package sismd.filters;

public abstract class AbstractFilter implements HistogramEqualizer {

    // Weighted luminosity per ITU-R BT.601 (same formula as professor's Filters.java)
    protected static int computeLuminosity(int r, int g, int b) {
        return (int) Math.round(0.299 * r + 0.587 * g + 0.114 * b);
    }

    protected static int[] buildCumulativeHistogram(int[] hist) {
        int[] cumulative = new int[256];
        cumulative[0] = hist[0];
        for (int i = 1; i < 256; i++)
            cumulative[i] = cumulative[i - 1] + hist[i];
        return cumulative;
    }

    // newLum = floor(255 * cumulativeHist[L] / totalPixels)  — formula from spec
    protected static int equalizedLuminosity(int lum, int[] cumulative, int totalPixels) {
        return Math.min(255, (int) Math.floor(255.0 * cumulative[lum] / totalPixels));
    }
}
