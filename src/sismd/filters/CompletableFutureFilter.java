package sismd.filters;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import sismd.utils.Utils;

/**
 * Asynchronous implementation using CompletableFuture.
 *
 * Phase 1 — supplyAsync launches one task per thread slice; each returns a partial histogram.
 *            allOf waits for all, then thenApply merges them without explicit blocking.
 * Phase 2 — cumulative computed synchronously (O(256)).
 * Phase 3 — runAsync launches equalization tasks; allOf.join() awaits completion.
 */
public class CompletableFutureFilter extends AbstractFilter {

    private final int numThreads;

    public CompletableFutureFilter(int numThreads) {
        this.numThreads = numThreads;
    }

    @Override
    public String getName() {
        return "CompletableFuture (" + numThreads + "T)";
    }

    @Override
    public Color[][] apply(Color[][] image) {
        Color[][] result  = Utils.copyImage(image);
        int rows          = result.length;
        int totalPixels   = rows * result[0].length;
        int actualThreads = Math.min(numThreads, rows);

        ExecutorService executor = Executors.newFixedThreadPool(actualThreads);
        try {
            // Phase 1: launch partial histogram computations asynchronously
            List<CompletableFuture<int[]>> histFutures = new ArrayList<>(actualThreads);
            for (int t = 0; t < actualThreads; t++) {
                final int start = t * rows / actualThreads;
                final int end   = (t + 1) * rows / actualThreads;
                histFutures.add(CompletableFuture.supplyAsync(
                    () -> SequentialFilter.computeHistogram(result, start, end),
                    executor
                ));
            }

            // Merge once all futures complete
            int[] hist = CompletableFuture
                .allOf(histFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    int[] merged = new int[256];
                    for (CompletableFuture<int[]> f : histFutures) {
                        // join() is non-blocking here — all futures are already done
                        int[] partial = f.join();
                        for (int i = 0; i < 256; i++) merged[i] += partial[i];
                    }
                    return merged;
                })
                .join();

            // Phase 2: cumulative (sequential)
            int[] cumulative = buildCumulativeHistogram(hist);

            // Phase 3: launch equalization tasks asynchronously
            List<CompletableFuture<Void>> eqFutures = new ArrayList<>(actualThreads);
            for (int t = 0; t < actualThreads; t++) {
                final int start = t * rows / actualThreads;
                final int end   = (t + 1) * rows / actualThreads;
                eqFutures.add(CompletableFuture.runAsync(
                    () -> SequentialFilter.applyEqualization(result, start, end, cumulative, totalPixels),
                    executor
                ));
            }
            CompletableFuture.allOf(eqFutures.toArray(new CompletableFuture[0])).join();

        } finally {
            executor.shutdown();
        }

        return result;
    }
}
