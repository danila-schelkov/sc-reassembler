package com.vorono4ka;

import com.vorono4ka.swf.Export;
import com.vorono4ka.swf.SupercellSWF;
import com.vorono4ka.swf.exceptions.LoadingFaultException;
import com.vorono4ka.swf.exceptions.TextureFileNotFound;
import com.vorono4ka.swf.exceptions.UnableToFindObjectException;
import com.vorono4ka.swf.exceptions.UnsupportedCustomPropertyException;
import com.vorono4ka.swf.movieclips.MovieClipOriginal;
import com.vorono4ka.swf.textures.SWFTexture;

import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Main {
    private static final boolean TRACK_MEMORY = true;

    public static void main(String[] args) throws Exception {
        Pattern pattern = Pattern.compile(".*shelly.*");

        Predicate<String> matcher = pattern.asPredicate();

        Stream<Path> scFiles = Files.list(Paths.get("sc"));
        scFiles.filter((path -> path.toFile().isFile())).map(Path::toString).forEach(filepath -> {
            String basename = filepath.substring(filepath.indexOf('\\') + 1, filepath.lastIndexOf('.'));

            SupercellSWF swf = getSupercellSWF(filepath);
            SwfReassambler reassambler = new SwfReassambler();
            for (Export export : swf.getExports()) {
                if (!matcher.test(export.name())) {
                    continue;
                }

                MovieClipOriginal movieClip;
                try {
                    movieClip = swf.getOriginalMovieClip(export.id(), export.name());
                } catch (UnableToFindObjectException e) {
                    throw new RuntimeException(e);
                }

                reassambler.addMovieClip(movieClip, swf);
                reassambler.addExport(export.id(), export.name());
            }

            SupercellSWF reassambledSwf = reassambler.getSwf();
            if (reassambledSwf.getExports().isEmpty()) {
                return;
            }

            for (int i = 0; i < swf.getTextureCount(); i++) {
                SWFTexture texture = swf.getTexture(i);
                reassambledSwf.addTexture(texture);
            }

            System.out.println(reassambledSwf.getExports());
            reassambledSwf.save("sc/patched/" + basename + ".sc", Main::setProgress);
        });
    }

    private static SupercellSWF getSupercellSWF(String filepath) {
        SupercellSWF swf = new SupercellSWF();

        try {
            swf.load(filepath, filepath);
        } catch (LoadingFaultException | UnableToFindObjectException |
                 UnsupportedCustomPropertyException | TextureFileNotFound e) {
            throw new RuntimeException(e);
        }
        return swf;
    }

    private static void setProgress(long i, long max) {
        if (i % 1000 == 0) {
            System.out.printf("%d/%d %d\n", i, max, TRACK_MEMORY ? ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed() / 1024 / 1024 : 0);
        }
    }
}