package sismd.utils;

import java.io.File;
import java.util.Arrays;

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

    /** Returns all image files in INPUT_DIR sorted by name. */
    public static File[] listInputImages() {
        File dir = new File(INPUT_DIR);
        File[] files = dir.listFiles(f -> f.isFile() && f.getName().matches(".*\\.(jpg|jpeg|png|bmp)"));
        if (files == null) return new File[0];
        Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        return files;
    }

    private static void ensureOutputDir() {
        File dir = new File(OUTPUT_DIR);
        if (!dir.exists()) dir.mkdirs();
    }
}
