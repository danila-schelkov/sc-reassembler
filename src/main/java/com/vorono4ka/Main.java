package com.vorono4ka;

import com.vorono4ka.swf.SupercellSWF;
import com.vorono4ka.swf.exceptions.LoadingFaultException;
import com.vorono4ka.swf.exceptions.TextureFileNotFound;
import com.vorono4ka.swf.exceptions.UnableToFindObjectException;
import com.vorono4ka.swf.exceptions.UnsupportedCustomPropertyException;

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
        System.out.println(matcher.test("emoji_shelly_blahblah"));

        Stream<Path> scFiles = Files.list(Paths.get("sc"));
        scFiles.filter((path -> path.toFile().isFile())).forEach(path -> {
            String filepath = path.toString();
            String basename = filepath.substring(filepath.indexOf('\\') + 1, filepath.lastIndexOf('.'));

            SupercellSWF swf = getSupercellSWF(filepath);

            SupercellSWF reassembledSwf = new SupercellSWF();


//            for (int i = 0; i < swf.getTextureCount(); i++) {
//                SWFTexture texture = swf.getTexture(i);
//                byte[] textureCompressed = Zstandard.compress(texture.getKtxData());
//                try {
//                    Files.write(Paths.get(basename + "_" + i + ".zktx"), textureCompressed);
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//            }

            reassembledSwf.save("sc/patched/" + basename + ".sc", Main::setProgress);
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