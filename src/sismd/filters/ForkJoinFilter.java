package sismd.filters;

import java.awt.Color;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;
import sismd.utils.Utils;

/**
 * Fork/Join implementation using divide-and-conquer.
 */
public class ForkJoinFilter extends AbstractFilter {

    private static final int THRESHOLD = 32; // rows per leaf task

    @Override
    public String getName() {
        return "Fork/Join";
    }

    @Override
    public Color[][] apply(Color[][] image) {
        Color[][] result  = Utils.copyImage(image);
        int totalPixels   = result.length * result[0].length;

        try (ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors())) {
            // Phase 1: parallel histogram via divide-and-conquer
            int[] hist = pool.invoke(new HistogramTask(result, 0, result.length));

            // Phase 2: cumulative (sequential, O(256))
            int[] cumulative = buildCumulativeHistogram(hist);

            // Phase 3: parallel equalization via divide-and-conquer
            pool.invoke(new EqualizationTask(result, 0, result.length, cumulative, totalPixels));
        }

        return result;
    }

    // ------------------------------------------------------------------ //

    private static final class HistogramTask extends RecursiveTask<int[]> {
        private final Color[][] image;
        private final int start, end;

        HistogramTask(Color[][] image, int start, int end) {
            this.image = image;
            this.start = start;
            this.end   = end;
        }

        @Override
        protected int[] compute() {
            if (end - start <= THRESHOLD)
                return SequentialFilter.computeHistogram(image, start, end);

            int mid = (start + end) >>> 1;
            HistogramTask left  = new HistogramTask(image, start, mid);
            HistogramTask right = new HistogramTask(image, mid, end);

            left.fork();
            int[] rightResult = right.compute();
            int[] leftResult  = left.join();

            for (int i = 0; i < 256; i++) leftResult[i] += rightResult[i];
            return leftResult;
        }
    }

    // ------------------------------------------------------------------ //

    private static final class EqualizationTask extends RecursiveAction {
        private final Color[][] image;
        private final int start, end;
        private final int[] cumulative;
        private final int totalPixels;

        EqualizationTask(Color[][] image, int start, int end,
                         int[] cumulative, int totalPixels) {
            this.image       = image;
            this.start       = start;
            this.end         = end;
            this.cumulative  = cumulative;
            this.totalPixels = totalPixels;
        }

        @Override
        protected void compute() {
            if (end - start <= THRESHOLD) {
                SequentialFilter.applyEqualization(image, start, end, cumulative, totalPixels);
                return;
            }
            int mid = (start + end) >>> 1;
            invokeAll(
                new EqualizationTask(image, start, mid, cumulative, totalPixels),
                new EqualizationTask(image, mid,  end,  cumulative, totalPixels)
            );
        }
    }
}
