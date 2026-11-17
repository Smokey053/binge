package com.binge.model;

import java.util.List;

/** A labelled shelf of movies, e.g. "Sci-Fi". */
public record Category(String id, String name, String tagline, List<Movie> movies) {

    public Category {
        movies = movies == null ? List.of() : List.copyOf(movies);
    }
}
