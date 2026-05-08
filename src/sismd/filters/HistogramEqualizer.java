package sismd.filters;

import java.awt.Color;

public interface HistogramEqualizer {
    Color[][] apply(Color[][] image);
    String getName();
}
