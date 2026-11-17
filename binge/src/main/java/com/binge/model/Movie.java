package com.binge.model;

import java.util.List;
import java.util.Locale;

/**
 * A single movie in the catalog. Instances are immutable and are read straight from {@code catalog.json}.
 *
 * @param id        stable slug used for persistence
 * @param trailerId YouTube video id of the official trailer (played through the embedded player)
 */
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

    /** Link to the trailer on youtube.com, used as a fallback when embedding is blocked. */
    public String watchUrl() {
        return "https://www.youtube.com/watch?v=" + trailerId;
    }

    /** "Action • Crime • Drama" */
    public String genreLine() {
        return String.join("  •  ", genres);
    }

    /** "2008  •  Christopher Nolan" */
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
