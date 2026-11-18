package com.binge.model;

import java.util.List;
import java.util.Locale;

/** One movie in the catalog. Instances cannot change and are read straight from {catalog.json}*/
public record Movie(
        String id,
        String title,
        int year,
        String director,
        List<String> genres,
        String synopsis,
        String trailerId) {

    public Movie {
        genres = genres == null ? List.of() : List.copyOf(genres);
    }

    /** Link to the trailer on youtube.com, used when embedded player is blocked. */
    public String watchUrl() {
        return "https://www.youtube.com/watch?v=" + trailerId;
    }

    /** Genres joined like "Action • Crime • Drama" */
    public String genreLine() {
        return String.join("  •  ", genres);
    }

    /** Year and Director joined like "2008  •  Christopher Nolan" */
    public String metaLine() {
        return year + "  •  " + director;
    }

    /** How well this movie matches a search query; 0 means no match, higher is better. */
    public int score(String query) {
        String q = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            return 0;
        }
        String name = title.toLowerCase(Locale.ROOT);
        if (name.startsWith(q)) {
            return 100;
        }
        if (name.contains(q)) {
            return 80;
        }
        if (director.toLowerCase(Locale.ROOT).contains(q)) {
            return 60;
        }
        if (genres.stream().anyMatch(g -> g.toLowerCase(Locale.ROOT).contains(q))) {
            return 50;
        }
        if (String.valueOf(year).contains(q)) {
            return 40;
        }
        return synopsis.toLowerCase(Locale.ROOT).contains(q) ? 20 : 0;
    }
}
