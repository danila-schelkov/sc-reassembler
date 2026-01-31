package dev.donutquine;

import dev.donutquine.swf.Export;
import dev.donutquine.swf.SupercellSWF;
import dev.donutquine.swf.exceptions.LoadingFaultException;
import dev.donutquine.swf.exceptions.TextureFileNotFound;
import dev.donutquine.swf.exceptions.UnableToFindObjectException;
import dev.donutquine.swf.exceptions.UnsupportedCustomPropertyException;
import dev.donutquine.swf.movieclips.MovieClipOriginal;
import dev.donutquine.swf.textures.SWFTexture;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
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

        boolean preferLowres = ns.getBoolean("lowres");

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

        scFiles.forEach(filepath -> handleFile(filepath, preferLowres));
        scFiles.close();
    }

    private static void handleFile(Path filepath, boolean preferLowres) {
        String filename = filepath.getFileName().toString();
        String basename = filename.substring(0, filename.lastIndexOf('.'));

        SupercellSWF swf = getSupercellSWF(filepath.toString(), preferLowres);

        SwfReassembler reassembler = new SwfReassembler();
        for (Export export : swf.getExports()) {
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
            System.err.println("No matched exports found");
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
