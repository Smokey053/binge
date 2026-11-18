package com.binge.player;

import java.util.Locale;

/** One reading from the JavaScript bridge in {player.html}, sent as 
{api|playerstate|time|duration|loadedFraction|errorCode}*/
record PlayerSnapshot(String api, int playerState, double time, double duration, double loaded, int error) {

    /** Returns {@code null} when the text is missing or malformed (e.g. the page is still loading). */
    static PlayerSnapshot parse(Object raw) {
        if (!(raw instanceof String text)) {
            return null;
        }
        String[] parts = text.split("\\|");
        if (parts.length < 6) {
            return null;
        }
        try {
            return new PlayerSnapshot(
                    parts[0].toLowerCase(Locale.ROOT),
                    (int) Double.parseDouble(parts[1]),
                    number(parts[2]),
                    number(parts[3]),
                    number(parts[4]),
                    (int) Double.parseDouble(parts[5]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    boolean apiReady() {
        return "ready".equals(api);
    }

    private static double number(String text) {
        double value = Double.parseDouble(text);
        return Double.isFinite(value) ? value : 0;
    }
}
