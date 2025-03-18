package com.vorono4ka;

import com.vorono4ka.swf.*;
import com.vorono4ka.swf.exceptions.UnableToFindObjectException;
import com.vorono4ka.swf.movieclips.*;
import com.vorono4ka.swf.shapes.ShapeOriginal;
import com.vorono4ka.swf.textfields.TextFieldOriginal;

import java.util.*;

public class SwfReassembler {
    private final SupercellSWF reassembledSwf;

    private final Map<Integer, Integer> loadedObjectIds = new HashMap<>();
    private final Map<Matrix2x3, Integer> currentMatrices = new HashMap<>();
    private final Map<ColorTransform, Integer> currentColorTransforms = new HashMap<>();

    private ScMatrixBank currentMatrixBank;
    private short currentMatrixBankIndex;

    public SwfReassembler() {
        this.reassembledSwf = SupercellSWF.createEmpty();

        this.currentMatrixBank = reassembledSwf.getMatrixBank(0);
        this.currentMatrixBankIndex = 0;
    }

    public int addMovieClip(MovieClipOriginal movieClip, SupercellSWF swf) {
        if (loadedObjectIds.containsKey(movieClip.getId())) {
            return loadedObjectIds.get(movieClip.getId());
        }

        addChildrenRecursively(swf, movieClip);
        addMatrices(swf, movieClip);

        int newId = reassembledSwf.addObject(movieClip);
        loadedObjectIds.put(movieClip.getId(), newId);
        return newId;
    }

    private void addChildrenRecursively(SupercellSWF swf, MovieClipOriginal movieClip) {
        if (loadedObjectIds.containsKey(movieClip.getId())) {
            return;
        }

        List<MovieClipChild> newChildren = new ArrayList<>();
        List<MovieClipChild> children = movieClip.getChildren();

        for (MovieClipChild childInfo : children) {
            DisplayObjectOriginal child;
            try {
                child = swf.getOriginalDisplayObject(childInfo.id() & 0xFFFF, movieClip.getExportName());
            } catch (UnableToFindObjectException e) {
                throw new RuntimeException(e);
            }

            if (loadedObjectIds.containsKey(child.getId())) {
                newChildren.add(new MovieClipChild(loadedObjectIds.get(child.getId()), childInfo.blend(), childInfo.name()));
                continue;
            }

            if (child instanceof MovieClipOriginal movieClipOriginal) {
                addChildrenRecursively(swf, movieClipOriginal);
                addMatrices(swf, movieClipOriginal);
            }

            int newId = reassembledSwf.addObject(child);
            loadedObjectIds.put(child.getId(), newId);

            newChildren.add(new MovieClipChild(newId, childInfo.blend(), childInfo.name()));
        }

        movieClip.setChildren(newChildren);
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

    public void recalculateIds() {
        for (ShapeOriginal shape : reassembledSwf.getShapes()) {
            shape.setId(loadedObjectIds.get(shape.getId()));
        }
        for (TextFieldOriginal textField : reassembledSwf.getTextFields()) {
            textField.setId(loadedObjectIds.get(textField.getId()));
        }
        for (MovieClipModifierOriginal modifier : reassembledSwf.getMovieClipModifiers()) {
            modifier.setId(loadedObjectIds.get(modifier.getId()));
        }
        for (MovieClipOriginal movieClip : reassembledSwf.getMovieClips()) {
            movieClip.setId(loadedObjectIds.get(movieClip.getId()));
        }
    }
}
