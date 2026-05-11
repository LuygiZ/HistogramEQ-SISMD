package sismd.benchmark;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import sismd.filters.CompletableFutureFilter;
import sismd.filters.ForkJoinFilter;
import sismd.filters.HistogramEqualizer;
import sismd.filters.MultithreadedFilter;
import sismd.filters.SequentialFilter;
import sismd.filters.ThreadPoolFilter;
import sismd.utils.ImagePaths;
import sismd.utils.Utils;

/**
 * Runs all histogram equalization implementations and prints a performance table.
 *
 * Usage:  java sismd.benchmark.Benchmark [image-path]
 *         java sismd.Main benchmark [image-path]   (preferred — via Main)
 *
 * Defaults to images/in/src.jpg. Output images go to images/out/.
 * Each implementation is warmed up WARMUP_RUNS times (JIT), then measured
 * MEASURE_RUNS times and averaged.
 */
public class Benchmark {

    private static final int WARMUP_RUNS  = 3;
    private static final int MEASURE_RUNS = 5;

    public static void main(String[] args) {
        String imagePath = args.length > 0 ? args[0] : ImagePaths.DEFAULT_INPUT;
        String stem      = ImagePaths.stem(imagePath);

        System.out.println("Loading image: " + imagePath);
        Color[][] image = Utils.loadImage(imagePath);
        System.out.printf("Image: %d x %d  (%,d pixels)%n%n",
            image.length, image[0].length, (long) image.length * image[0].length);

        int cores = Runtime.getRuntime().availableProcessors();
        System.out.println("Available processors: " + cores);
        System.out.println();

        List<Integer> threadCounts = deduplicated(new int[]{1, 2, 4, 8, cores}, cores * 2);
        List<BenchmarkResult> results = new ArrayList<>();

        // 1. Sequential baseline
        results.add(run(new SequentialFilter(), image));

        // 2. Multithreaded (no pool)
        for (int t : threadCounts)
            results.add(run(new MultithreadedFilter(t), image));

        // 3. Thread Pool
        for (int t : threadCounts)
            results.add(run(new ThreadPoolFilter(t), image));

        // 4. Fork/Join
        results.add(run(new ForkJoinFilter(), image));

        // 5. CompletableFuture
        for (int t : threadCounts)
            results.add(run(new CompletableFutureFilter(t), image));

        printTable(results);

        // Save correctness reference to images/out/
        String outPath = ImagePaths.output(stem + "_sequential.jpg");
        Utils.writeImage(new SequentialFilter().apply(image), outPath);
        System.out.println("\nCorrectness reference -> " + outPath);
    }

    // ------------------------------------------------------------------ //

    public static BenchmarkResult run(HistogramEqualizer filter, Color[][] image) {
        System.out.print("  Benchmarking: " + filter.getName() + " ... ");

        for (int i = 0; i < WARMUP_RUNS; i++)
            filter.apply(image);

        Runtime rt = Runtime.getRuntime();
        long totalTimeNs   = 0;
        long totalMemBytes = 0;

        for (int i = 0; i < MEASURE_RUNS; i++) {
            rt.gc();
            long memBefore = rt.totalMemory() - rt.freeMemory();
            long t0 = System.nanoTime();

            filter.apply(image);

            totalTimeNs   += System.nanoTime() - t0;
            totalMemBytes += Math.max(0, (rt.totalMemory() - rt.freeMemory()) - memBefore);
        }

        long avgMs  = totalTimeNs  / MEASURE_RUNS / 1_000_000;
        long avgMem = totalMemBytes / MEASURE_RUNS;

        System.out.printf("%d ms%n", avgMs);
        return new BenchmarkResult(filter.getName(), avgMs, avgMem);
    }

    private static void printTable(List<BenchmarkResult> results) {
        long seqMs = results.get(0).getAvgTimeMs();
        String sep = "-".repeat(78);

        System.out.println("\n" + sep);
        System.out.printf("| %-42s | %8s | %10s | %8s |%n",
            "Implementation", "Time(ms)", "Mem(MB)", "Speedup");
        System.out.println(sep);

        for (BenchmarkResult r : results) {
            double speedup = (seqMs > 0 && r.getAvgTimeMs() > 0)
                ? (double) seqMs / r.getAvgTimeMs() : 1.0;
            System.out.printf("| %-42s | %8d | %10.2f | %7.2fx |%n",
                r.getName(), r.getAvgTimeMs(), r.getAvgMemoryMB(), speedup);
        }
        System.out.println(sep);
    }

    private static List<Integer> deduplicated(int[] candidates, int cap) {
        List<Integer> result = new ArrayList<>();
        for (int v : candidates)
            if (!result.contains(v) && v <= cap)
                result.add(v);
        return result;
    }
}
