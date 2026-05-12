package sismd;

import java.awt.Color;
import java.io.File;
import java.util.Scanner;
import sismd.benchmark.Benchmark;
import sismd.filters.CompletableFutureFilter;
import sismd.filters.ForkJoinFilter;
import sismd.filters.HistogramEqualizer;
import sismd.filters.MultithreadedFilter;
import sismd.filters.SequentialFilter;
import sismd.filters.ThreadPoolFilter;
import sismd.utils.ImagePaths;
import sismd.utils.Utils;

/**
 * Central entry point for the SISMD Histogram Equalization project.
 *
 * Input images:  images/in/
 * Output images: images/out/
 *
 * Usage:
 *   java sismd.Main                                  -> interactive menu
 *   java sismd.Main benchmark [image]                -> full benchmark
 *   java sismd.Main apply <image> <output> [filter] [threads]
 */
public class Main {

    public static void main(String[] args) {
        if (args.length > 0) {
            runCommand(args);
        } else {
            runMenu();
        }
    }

    // ------------------------------------------------------------------ //
    //  Interactive menu
    // ------------------------------------------------------------------ //

    private static void runMenu() {
        try (Scanner sc = new Scanner(System.in)) {
            boolean running = true;

            while (running) {
                System.out.println();
                System.out.println("==============================================");
                System.out.println("  Histogram Equalization - SISMD 2025/26");
                System.out.println("==============================================");
                System.out.println("  Input  dir: " + ImagePaths.INPUT_DIR);
                System.out.println("  Output dir: " + ImagePaths.OUTPUT_DIR);
                System.out.println("----------------------------------------------");
                System.out.println("  1. Run full benchmark");
                System.out.println("  2. Apply a filter and save image");
                System.out.println("  3. Apply ALL filters and save images");
                System.out.println("  0. Exit");
                System.out.println("----------------------------------------------");
                System.out.print("  Choice: ");

                switch (sc.nextLine().trim()) {
                    case "1" -> menuBenchmark(sc);
                    case "2" -> menuApplySingle(sc);
                    case "3" -> menuApplyAll(sc);
                    case "0" -> running = false;
                    default  -> System.out.println("  Invalid option.");
                }
            }
        }
    }

    private static void menuBenchmark(Scanner sc) {
        String img = selectImage(sc);
        System.out.println();
        Benchmark.main(new String[]{ img });
    }

    private static void menuApplySingle(Scanner sc) {
        String img = selectImage(sc);
        String stem = ImagePaths.stem(img);

        System.out.println();
        System.out.println("  Implementations:");
        System.out.println("    1. Sequential");
        System.out.println("    2. Multithreaded (no pool)");
        System.out.println("    3. Thread Pool");
        System.out.println("    4. Fork/Join");
        System.out.println("    5. CompletableFuture");
        System.out.print("  Choice [1]: ");
        String choice = sc.nextLine().trim();
        if (choice.isEmpty()) choice = "1";

        int threads = 4;
        if (!choice.equals("1") && !choice.equals("4")) {
            threads = Integer.parseInt(prompt(sc, "Number of threads", "4"));
        }

        String defaultOut = ImagePaths.output(stem + "_output.jpg");
        String out = prompt(sc, "Output file", defaultOut);
        applyAndSave(buildFilter(choice, threads), img, out);
    }

    private static void menuApplyAll(Scanner sc) {
        String img    = selectImage(sc);
        int    threads = Integer.parseInt(prompt(sc, "Threads for parallel filters", "4"));
        String stem   = ImagePaths.stem(img);

        System.out.println();
        Color[][] image = Utils.loadImage(img);

        HistogramEqualizer[] filters = {
            new SequentialFilter(),
            new MultithreadedFilter(threads),
            new ThreadPoolFilter(threads),
            new ForkJoinFilter(),
            new CompletableFutureFilter(threads)
        };
        String[] suffixes = {
            "sequential", "multithreaded", "threadpool", "forkjoin", "completable"
        };

        for (int i = 0; i < filters.length; i++) {
            String outPath = ImagePaths.output(stem + "_" + suffixes[i] + ".jpg");
            long t0        = System.nanoTime();
            Color[][] r    = filters[i].apply(image);
            long ms        = (System.nanoTime() - t0) / 1_000_000;
            Utils.writeImage(r, outPath);
            System.out.printf("  [%3d ms]  %-38s -> %s%n", ms, filters[i].getName(), outPath);
        }
        System.out.println("\n  All outputs saved to " + ImagePaths.OUTPUT_DIR);
    }

    // ------------------------------------------------------------------ //
    //  Direct command-line mode
    // ------------------------------------------------------------------ //

    private static void runCommand(String[] args) {
        switch (args[0].toLowerCase()) {
            case "benchmark" -> {
                String img = args.length > 1 ? args[1] : ImagePaths.DEFAULT_INPUT;
                Benchmark.main(new String[]{ img });
            }
            case "apply" -> {
                if (args.length < 3) {
                    System.out.println("Usage: Main apply <image> <output> [sequential|multithreaded|threadpool|forkjoin|completable] [threads]");
                    return;
                }
                String img  = args[1];
                String out  = args[2];
                String type = args.length > 3 ? args[3] : "sequential";
                int threads = args.length > 4 ? Integer.parseInt(args[4]) : 4;
                applyAndSave(buildFilter(type, threads), img, out);
            }
            default -> System.out.println("Unknown command '" + args[0] + "'. Use 'benchmark' or 'apply'.");
        }
    }

    // ------------------------------------------------------------------ //
    //  Helpers
    // ------------------------------------------------------------------ //

    private static void applyAndSave(HistogramEqualizer filter, String imgPath, String outPath) {
        System.out.println("\n  Loading: " + imgPath);
        Color[][] image = Utils.loadImage(imgPath);
        System.out.print("  Running: " + filter.getName() + " ... ");
        long t0     = System.nanoTime();
        Color[][] r = filter.apply(image);
        long ms     = (System.nanoTime() - t0) / 1_000_000;
        Utils.writeImage(r, outPath);
        System.out.printf("done (%d ms)%n", ms);
        System.out.println("  Saved:   " + outPath);
    }

    private static HistogramEqualizer buildFilter(String choice, int threads) {
        return switch (choice) {
            case "2", "multithreaded" -> new MultithreadedFilter(threads);
            case "3", "threadpool"    -> new ThreadPoolFilter(threads);
            case "4", "forkjoin"      -> new ForkJoinFilter();
            case "5", "completable"   -> new CompletableFutureFilter(threads);
            default                   -> new SequentialFilter();
        };
    }

    private static String selectImage(Scanner sc) {
        File[] images = ImagePaths.listInputImages();
        if (images.length == 0) {
            System.out.println("  No images found in " + ImagePaths.INPUT_DIR);
            return ImagePaths.DEFAULT_INPUT;
        }

        String defaultName = new File(ImagePaths.DEFAULT_INPUT).getName();
        int defaultIdx = 1;

        System.out.println();
        System.out.println("  Available images:");
        System.out.printf("  %-4s  %-28s  %13s  %15s%n", "#", "File", "Resolution", "Pixels");
        System.out.println("  " + "-".repeat(68));
        for (int i = 0; i < images.length; i++) {
            int[] dim = Utils.readDimensions(images[i].getPath());
            long pixels = (long) dim[0] * dim[1];
            boolean isDefault = images[i].getName().equalsIgnoreCase(defaultName);
            if (isDefault) defaultIdx = i + 1;
            System.out.printf("  %-4d  %-28s  %6dx%-6d  %,15d%s%n",
                i + 1, images[i].getName(), dim[0], dim[1], pixels, isDefault ? "  *" : "");
        }
        System.out.printf("%n  Select image [%d]: ", defaultIdx);
        String line = sc.nextLine().trim();
        if (line.isEmpty()) return images[defaultIdx - 1].getPath();
        try {
            int idx = Integer.parseInt(line);
            if (idx >= 1 && idx <= images.length) return images[idx - 1].getPath();
        } catch (NumberFormatException ignored) {}
        return line; // treat raw input as a custom path
    }

    private static String prompt(Scanner sc, String label, String defaultVal) {
        System.out.printf("  %s [%s]: ", label, defaultVal);
        String val = sc.nextLine().trim();
        return val.isEmpty() ? defaultVal : val;
    }
}
