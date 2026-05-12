package sismd.filters;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import sismd.utils.Utils;

/**
 * Thread pool implementation using ExecutorService.
 */
public class ThreadPoolFilter extends AbstractFilter {

    private final int numThreads;

    public ThreadPoolFilter(int numThreads) {
        this.numThreads = numThreads;
    }

    @Override
    public String getName() {
        return "Thread Pool (" + numThreads + "T)";
    }

    @Override
    public Color[][] apply(Color[][] image) {
        Color[][] result  = Utils.copyImage(image);
        int rows          = result.length;
        int totalPixels   = rows * result[0].length;
        int actualThreads = Math.min(numThreads, rows);
        ExecutorService pool = Executors.newFixedThreadPool(actualThreads);

        try {
            // Phase 1: submit histogram tasks, collect Future<int[]>
            List<Future<int[]>> histFutures = new ArrayList<>(actualThreads);
            for (int t = 0; t < actualThreads; t++) {
                final int start = t * rows / actualThreads;
                final int end   = (t + 1) * rows / actualThreads;
                histFutures.add(pool.submit(() ->
                    SequentialFilter.computeHistogram(result, start, end)
                ));
            }

            // Collect and merge
            int[] hist = new int[256];
            for (Future<int[]> f : histFutures) {
                int[] partial = f.get();
                for (int i = 0; i < 256; i++) hist[i] += partial[i];
            }

            // Phase 2: cumulative (sequential)
            int[] cumulative = buildCumulativeHistogram(hist);

            // Phase 3: submit equalization tasks
            List<Future<?>> eqFutures = new ArrayList<>(actualThreads);
            for (int t = 0; t < actualThreads; t++) {
                final int start = t * rows / actualThreads;
                final int end   = (t + 1) * rows / actualThreads;
                eqFutures.add(pool.submit(() ->
                    SequentialFilter.applyEqualization(result, start, end, cumulative, totalPixels)
                ));
            }
            for (Future<?> f : eqFutures) f.get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Task execution failed", e);
        } finally {
            pool.shutdown();
        }

        return result;
    }
}
