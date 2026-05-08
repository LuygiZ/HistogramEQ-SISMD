package sismd.starter;

import java.awt.Color;
import java.io.IOException;
import sismd.utils.Utils;

/**
 * @author Jorge Coelho
 * @contact jmn@isep.ipp.pt
 * @version 1.0
 */
public class Filters {

    String file;
    Color image[][];

    Filters(String filename) {
        this.file = filename;
        image = Utils.loadImage(filename);
    }

    public void HistogramFilter(String outputFile, int value) throws IOException {
        Color[][] tmp = Utils.copyImage(image);
        int[] hist = new int[256];
        int total_pixels = tmp.length * tmp[0].length;
        System.out.println("Total pixels: " + total_pixels);

        for (int i = 0; i < tmp.length; i++) {
            for (int j = 0; j < tmp[i].length; j++) {
                Color pixel = tmp[i][j];
                int r = pixel.getRed();
                int g = pixel.getGreen();
                int b = pixel.getBlue();
                int lum = computeLuminosity(r, g, b);
                hist[lum]++;
            }
        }

        int[] cumulative = new int[256];
        cumulative[0] = hist[0];
        for (int i = 1; i < 256; i++)
            cumulative[i] = cumulative[i - 1] + hist[i];

        int cdfMin = 0;
        for (int i = 0; i < 256; i++) {
            if (cumulative[i] != 0) {
                cdfMin = cumulative[i];
                break;
            }
        }

        for (int i = 0; i < tmp.length; i++) {
            for (int j = 0; j < tmp[i].length; j++) {
                Color pixel = tmp[i][j];
                int r = pixel.getRed();
                int g = pixel.getGreen();
                int b = pixel.getBlue();
                int lum = computeLuminosity(r, g, b);
                double cdf = (double) cumulative[lum] / (double) (total_pixels - cdfMin);
                int newLum = (int) Math.round(255.0 * cdf);
                tmp[i][j] = new Color(newLum, newLum, newLum);
            }
        }
        Utils.writeImage(tmp, outputFile);
    }

    public int computeLuminosity(int r, int g, int b) {
        return (int) Math.round(0.299 * r + 0.587 * g + 0.114 * b);
    }
}
