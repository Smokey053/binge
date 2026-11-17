package com.binge.util;

import java.util.Locale;

/** Small formatting helpers shared by the UI. */
public final class Fmt {

    private Fmt() {
    }

    /** 83.4 -> "1:23", 3725 -> "1:02:05". Negative and NaN values are shown as 0:00. */
    public static String time(double seconds) {
        long total = Double.isNaN(seconds) || seconds < 0 ? 0 : (long) seconds;
        long h = total / 3600;
        long m = (total % 3600) / 60;
        long s = total % 60;
        return h > 0
                ? String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s)
                : String.format(Locale.ROOT, "%d:%02d", m, s);
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
