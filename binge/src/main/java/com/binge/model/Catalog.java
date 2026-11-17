package com.binge.model;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/** The full movie library: genre categories plus the ids highlighted in the hero banner. */
public record Catalog(List<String> featured, List<Category> categories) {

    public Catalog {
        featured = featured == null ? List.of() : List.copyOf(featured);
        categories = categories == null ? List.of() : List.copyOf(categories);
    }

    public Stream<Movie> stream() {
        return categories.stream().flatMap(c -> c.movies().stream());
    }

    public List<Movie> allMovies() {
        return stream().toList();
    }

    public Optional<Movie> find(String id) {
        return stream().filter(m -> m.id().equals(id)).findFirst();
    }

    public Optional<Category> category(String id) {
        return categories.stream().filter(c -> c.id().equals(id)).findFirst();
    }

    /** Movies shown in the hero banner; falls back to the first movies when none are configured. */
    public List<Movie> featuredMovies() {
        List<Movie> picked = featured.stream().map(this::find).flatMap(Optional::stream).toList();
        return picked.isEmpty() ? stream().limit(5).toList() : picked;
    }

    /** The category a movie belongs to. */
    public Optional<Category> categoryOf(Movie movie) {
        return categories.stream().filter(c -> c.movies().contains(movie)).findFirst();
    }

    /** Movies from the same category, used for the "More like this" shelf. */
    public List<Movie> related(Movie movie) {
        return categoryOf(movie)
                .map(c -> c.movies().stream().filter(m -> !m.equals(movie)).toList())
                .orElse(List.of());
    }

    /** Case-insensitive search over title, director, genre, year and synopsis; best matches first. */
    public List<Movie> search(String query) {
        return stream()
                .filter(m -> m.score(query) > 0)
                .sorted(Comparator.comparingInt((Movie m) -> m.score(query)).reversed()
                        .thenComparing(Movie::title))
                .toList();
    }
}
