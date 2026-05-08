package sismd.filters;

import java.awt.Color;
import sismd.utils.Utils;

public class SequentialFilter extends AbstractFilter {

    @Override
    public String getName() {
        return "Sequential";
    }

    @Override
    public Color[][] apply(Color[][] image) {
        Color[][] result = Utils.copyImage(image);
        int totalPixels = result.length * result[0].length;

        int[] hist       = computeHistogram(result, 0, result.length);
        int[] cumulative = buildCumulativeHistogram(hist);
        applyEqualization(result, 0, result.length, cumulative, totalPixels);

        return result;
    }

    // Package-visible so parallel filters can reuse these as their leaf operations
    static int[] computeHistogram(Color[][] image, int startRow, int endRow) {
        int[] hist = new int[256];
        for (int i = startRow; i < endRow; i++)
            for (int j = 0; j < image[i].length; j++) {
                Color p = image[i][j];
                hist[computeLuminosity(p.getRed(), p.getGreen(), p.getBlue())]++;
            }
        return hist;
    }

    static void applyEqualization(Color[][] image, int startRow, int endRow,
                                  int[] cumulative, int totalPixels) {
        for (int i = startRow; i < endRow; i++)
            for (int j = 0; j < image[i].length; j++) {
                Color p = image[i][j];
                int lum    = computeLuminosity(p.getRed(), p.getGreen(), p.getBlue());
                int newLum = equalizedLuminosity(lum, cumulative, totalPixels);
                image[i][j] = new Color(newLum, newLum, newLum);
            }
    }
}
