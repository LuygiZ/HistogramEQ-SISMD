package sismd.utils;

import java.io.File;

public final class ImagePaths {

    public static final String INPUT_DIR  = "images/in/";
    public static final String OUTPUT_DIR = "images/out/";

    public static final String DEFAULT_INPUT = INPUT_DIR + "src.jpg";

    private ImagePaths() {}

    public static String input(String filename) {
        return INPUT_DIR + filename;
    }

    public static String output(String filename) {
        ensureOutputDir();
        return OUTPUT_DIR + filename;
    }

    /** Derives an output filename from an input path, e.g. "images/in/src.jpg" -> "src". */
    public static String stem(String inputPath) {
        String name = new File(inputPath).getName();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private static void ensureOutputDir() {
        File dir = new File(OUTPUT_DIR);
        if (!dir.exists()) dir.mkdirs();
    }
}
