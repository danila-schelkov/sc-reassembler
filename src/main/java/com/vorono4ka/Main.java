package com.vorono4ka;

import com.vorono4ka.swf.Export;
import com.vorono4ka.swf.SupercellSWF;
import com.vorono4ka.swf.exceptions.LoadingFaultException;
import com.vorono4ka.swf.exceptions.TextureFileNotFound;
import com.vorono4ka.swf.exceptions.UnableToFindObjectException;
import com.vorono4ka.swf.exceptions.UnsupportedCustomPropertyException;
import com.vorono4ka.swf.movieclips.MovieClipOriginal;
import com.vorono4ka.swf.textures.SWFTexture;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Main {
    private static final boolean TRACK_MEMORY = true;

    public static void main(String[] args) throws Exception {
        ArgumentParser parser = ArgumentParsers.newFor("SC Downgrade").build()
            .defaultHelp(true)
            .description("Reassemble SC file to SC of version 1, containing only needed export names.");

        parser.addArgument("-f", "--files").nargs("*").help("Files to be reassembled");
        parser.addArgument("-d", "--directory").help("Directory of files to be reassembled");
        parser.addArgument("-l", "--lowres").action(Arguments.storeTrue()).help("Prefer low resolution even if highres exists");
        parser.addArgument("-e", "--exports").help("File containing all export names separated by a line break");
//        parser.addArgument("-r", "--regex").action(Arguments.storeFalse()).help("Should convert export names to regular expressions");

        if (args.length == 0) {
            parser.printUsage();
            return;
        }

        Namespace ns = null;
        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }

        String exportsFilename = ns.getString("exports");
        if (exportsFilename == null) {
            throw new RuntimeException("Missing exports file");
        }

//        boolean useRegex = ns.getBoolean("regex");
        boolean preferLowres = ns.getBoolean("lowres");
        boolean useRegex = false;

        List<String> exports = Files.readAllLines(Paths.get(exportsFilename));
        List<Predicate<String>> matchers = new ArrayList<>();
        for (String export : exports) {
            String pattern = export;
            if (!useRegex) {
                pattern = Pattern.quote(pattern);
            }

            matchers.add(Pattern.compile(pattern).asPredicate());
        }

        Stream<Path> scFiles;
        String directory = ns.getString("directory");
        if (directory != null) {
            scFiles = Files.list(Paths.get(directory));
        } else {
            List<String> files = ns.getList("files");
            if (files == null) {
                throw new RuntimeException("Missing files");
            }

            scFiles = files.stream().map(Paths::get);
        }

        scFiles = scFiles.filter(path -> path.toFile().isFile());

        scFiles.forEach(filepath -> handleFile(filepath, matchers, preferLowres));
        scFiles.close();
    }

    private static void handleFile(Path filepath, List<Predicate<String>> matcher, boolean preferLowres) {
        String filename = filepath.getFileName().toString();
        String basename = filename.substring(0, filename.lastIndexOf('.'));

        SupercellSWF swf = getSupercellSWF(filepath.toString(), preferLowres);
        SwfReassembler reassembler = new SwfReassembler();
        for (Export export : swf.getExports()) {
            if (matcher.stream().noneMatch(stringPredicate -> stringPredicate.test(export.name()))) {
                continue;
            }

            MovieClipOriginal movieClip;
            try {
                movieClip = swf.getOriginalMovieClip(export.id(), export.name());
            } catch (UnableToFindObjectException e) {
                throw new RuntimeException(e);
            }

            int newId = reassembler.addMovieClip(movieClip, swf);

            reassembler.addExport(newId, export.name());
        }

        SupercellSWF reassembledSwf = reassembler.getSwf();
        if (reassembledSwf.getExports().isEmpty()) {
            System.out.println("No matched exports found");
            return;
        }

        for (int i = 0; i < swf.getTextureCount(); i++) {
            SWFTexture texture = swf.getTexture(i);
            reassembledSwf.addTexture(texture);
        }

        reassembler.recalculateIds();

        Path directory = filepath.toAbsolutePath().getParent();
        Path reassembled = directory.resolve("reassembled");
        reassembled.toFile().mkdirs();
        String outputFilepath = reassembled.resolve(basename + ".sc").toString();

        reassembledSwf.save(outputFilepath, Main::setProgress);
        System.out.printf("Saved as %s\n", outputFilepath);
    }

    private static void handleFile1(Path filepath, boolean preferLowres) {
        String filename = filepath.getFileName().toString();
        String basename = filename.substring(0, filename.lastIndexOf('.'));

        SupercellSWF swf = getSupercellSWF(filepath.toString(), preferLowres);

        Path directory = filepath.toAbsolutePath().getParent();
        Path reassembled = directory.resolve("downgraded");
        reassembled.toFile().mkdirs();
        String outputFilepath = reassembled.resolve(basename + ".sc").toString();

        swf.save(outputFilepath, Main::setProgress);
        System.out.printf("Saved as %s\n", outputFilepath);
    }

    private static SupercellSWF getSupercellSWF(String filepath, boolean preferLowres) {
        SupercellSWF swf = new SupercellSWF();

        try {
            swf.load(filepath, filepath, preferLowres);
        } catch (LoadingFaultException | UnableToFindObjectException |
                 UnsupportedCustomPropertyException | TextureFileNotFound e) {
            throw new RuntimeException(e);
        }
        return swf;
    }

    private static void setProgress(long i, long max) {
        if (i % 1000 == 0) {
            System.out.printf("Progress: %d/%d\tMemory used: %dMB\r", i, max, TRACK_MEMORY ? ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed() / 1024 / 1024 : 0);
        }

        if (i == max) {
            System.out.printf("Progress: %d/%d\n", max, max);
        }
    }
}