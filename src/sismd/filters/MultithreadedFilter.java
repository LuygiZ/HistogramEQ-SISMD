package sismd.filters;

import java.awt.Color;
import sismd.utils.Utils;

/**
 * Multithreaded implementation without thread pools.
 */
public class MultithreadedFilter extends AbstractFilter {

    private final int numThreads;

    public MultithreadedFilter(int numThreads) {
        this.numThreads = numThreads;
    }

    @Override
    public String getName() {
        return "Multithreaded - no pool (" + numThreads + "T)";
    }

    @Override
    public Color[][] apply(Color[][] image) {
        Color[][] result = Utils.copyImage(image);
        int rows          = result.length;
        int totalPixels   = rows * result[0].length;
        int actualThreads = Math.min(numThreads, rows);

        Thread[]   threads      = new Thread[actualThreads];
        int[][]    partialHists = new int[actualThreads][256];

        // Parallel histogram — each thread owns its partial array
        for (int t = 0; t < actualThreads; t++) {
            final int id    = t;
            final int start = id * rows / actualThreads;
            final int end   = (id + 1) * rows / actualThreads;
            threads[t] = new Thread(() ->
                partialHists[id] = SequentialFilter.computeHistogram(result, start, end)
            );
            threads[t].start();
        }
        joinAll(threads);

        // Merge partial histograms (256 iterations — trivially fast)
        int[] hist = new int[256];
        for (int[] partial : partialHists)
            for (int i = 0; i < 256; i++)
                hist[i] += partial[i];

        // Phase 2: cumulative (sequential)
        int[] cumulative = buildCumulativeHistogram(hist);

        // Phase 3: parallel equalization — disjoint rows, no synchronization needed
        for (int t = 0; t < actualThreads; t++) {
            final int start = t * rows / actualThreads;
            final int end   = (t + 1) * rows / actualThreads;
            threads[t] = new Thread(() ->
                SequentialFilter.applyEqualization(result, start, end, cumulative, totalPixels)
            );
            threads[t].start();
        }
        joinAll(threads);

        return result;
    }

    private static void joinAll(Thread[] threads) {
        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Thread interrupted", e);
            }
        }
    }
}
