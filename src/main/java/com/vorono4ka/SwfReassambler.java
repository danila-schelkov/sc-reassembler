package com.vorono4ka;

import com.vorono4ka.swf.*;
import com.vorono4ka.swf.exceptions.UnableToFindObjectException;
import com.vorono4ka.swf.movieclips.MovieClipFrame;
import com.vorono4ka.swf.movieclips.MovieClipFrameElement;
import com.vorono4ka.swf.movieclips.MovieClipOriginal;

import java.util.*;

public class SwfReassambler {
    private final SupercellSWF reassembledSwf;

    private final Set<Integer> loadedObjectIds = new HashSet<>();
    private final Map<Matrix2x3, Integer> currentMatrices = new HashMap<>();
    private final Map<ColorTransform, Integer> currentColorTransforms = new HashMap<>();

    private ScMatrixBank currentMatrixBank;
    private short currentMatrixBankIndex;

    public SwfReassambler() {
        this.reassembledSwf = SupercellSWF.createEmpty();

        this.currentMatrixBank = reassembledSwf.getMatrixBank(0);
        this.currentMatrixBankIndex = 0;
    }

    public void addMovieClip(MovieClipOriginal movieClip, SupercellSWF swf) {
        if (loadedObjectIds.contains(movieClip.getId())) {
            return;
        }

        addChildrenRecursively(swf, movieClip);
        addMatrices(swf, movieClip);

        loadedObjectIds.add(movieClip.getId());
        reassembledSwf.addObject(movieClip);
    }

    private void addChildrenRecursively(SupercellSWF swf, MovieClipOriginal movieClip) {
        if (loadedObjectIds.contains(movieClip.getId())) {
            return;
        }

        DisplayObjectOriginal[] timelineChildren;
        try {
            timelineChildren = movieClip.createTimelineChildren(swf);
        } catch (UnableToFindObjectException e) {
            throw new RuntimeException(e);
        }

        for (DisplayObjectOriginal child : timelineChildren) {
            if (loadedObjectIds.contains(child.getId())) {
                continue;
            }

            if (child instanceof MovieClipOriginal movieClipOriginal) {
                addChildrenRecursively(swf, movieClipOriginal);
                addMatrices(swf, movieClipOriginal);
            }

            reassembledSwf.addObject(child);
            loadedObjectIds.add(child.getId());
        }
    }

    private void addMatrices(SupercellSWF swf, MovieClipOriginal movieClip) {
        ScMatrixBank matrixBank = swf.getMatrixBank(movieClip.getMatrixBankIndex());

        List<Matrix2x3> matrices = new ArrayList<>();
        List<ColorTransform> colorTransforms = new ArrayList<>();

        for (MovieClipFrame frame : movieClip.getFrames()) {
            List<MovieClipFrameElement> elements = frame.getElements();
            for (MovieClipFrameElement element : elements) {
                if (element.matrixIndex() != 0xFFFF) {
                    matrices.add(matrixBank.getMatrix(element.matrixIndex()));
                }

                if (element.colorTransformIndex() != 0xFFFF) {
                    colorTransforms.add(matrixBank.getColorTransform(element.colorTransformIndex()));
                }
            }
        }

        List<Matrix2x3> absentMatrices = new ArrayList<>(matrices);
        absentMatrices.removeIf(currentMatrices::containsKey);

        List<ColorTransform> absentColorTransforms = new ArrayList<>(colorTransforms);
        absentColorTransforms.removeIf(currentColorTransforms::containsKey);

        boolean notEnoughSpaceForMatrix = 0xFFFF - currentMatrixBank.getMatrixCount() < absentMatrices.size();
        boolean notEnoughSpaceForColors = 0xFFFF - currentMatrixBank.getColorTransformCount() < absentColorTransforms.size();
        if (notEnoughSpaceForMatrix || notEnoughSpaceForColors) {
            currentMatrixBankIndex = (short) reassembledSwf.getMatrixBankCount();
            currentMatrixBank = new ScMatrixBank();

            currentMatrices.clear();
            currentColorTransforms.clear();

            reassembledSwf.addMatrixBank(currentMatrixBank);
            absentColorTransforms = colorTransforms;
            absentMatrices = matrices;
        }

        for (Matrix2x3 matrix : absentMatrices) {
            if (currentMatrices.containsKey(matrix)) {
                continue;
            }

            currentMatrices.put(matrix, currentMatrixBank.getMatrixCount());
            currentMatrixBank.addMatrix(matrix);
        }

        for (ColorTransform colorTransform : absentColorTransforms) {
            if (currentColorTransforms.containsKey(colorTransform)) {
                continue;
            }

            currentColorTransforms.put(colorTransform, currentMatrixBank.getColorTransformCount());
            currentMatrixBank.addColorTransform(colorTransform);
        }

        for (MovieClipFrame frame : movieClip.getFrames()) {
            List<MovieClipFrameElement> newElements = new ArrayList<>();

            for (MovieClipFrameElement element : frame.getElements()) {
                int newMatrixIndex = 0xFFFF;
                if (element.matrixIndex() != 0xFFFF) {
                    Matrix2x3 matrix = matrixBank.getMatrix(element.matrixIndex());
                    newMatrixIndex = currentMatrices.get(matrix);
                }

                int newColorIndex = 0xFFFF;
                if (element.colorTransformIndex() != 0xFFFF) {
                    ColorTransform colorTransform = matrixBank.getColorTransform(element.colorTransformIndex());
                    newColorIndex = currentColorTransforms.get(colorTransform);
                }

                newElements.add(new MovieClipFrameElement(element.childIndex(), newMatrixIndex, newColorIndex));
            }

            frame.setElements(newElements);
        }

        movieClip.setMatrixBankIndex(currentMatrixBankIndex);
    }

    public void addExport(int movieClipId, String name) {
        this.reassembledSwf.addExport(movieClipId, name);
    }

    public SupercellSWF getSwf() {
        return reassembledSwf;
    }
}
