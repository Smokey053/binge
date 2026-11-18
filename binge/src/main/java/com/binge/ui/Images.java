package com.binge.ui;

import com.binge.model.Movie;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.scene.image.Image;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Fetches poster and backdrop images from TMDB without bundling copyrighted images. */
public final class Images {

    private static final Credentials TMDB_CREDENTIALS = configuredTmdbCredentials();
    private static final String API_ROOT = "https://api.themoviedb.org/3";
    private static final String IMAGE_ROOT = "https://image.tmdb.org/t/p/";
    private static final String WIKIPEDIA_API = "https://en.wikipedia.org/w/api.php";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();
    private static final ConcurrentHashMap<String, CompletableFuture<Artwork>> ARTWORK = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Image> IMAGES = new ConcurrentHashMap<>();
    private static final AtomicBoolean MISSING_KEY_REPORTED = new AtomicBoolean();

    private Images() {
    }

    private static Credentials configuredTmdbCredentials() {
        String accessToken = System.getenv("TMDB_ACCESS_TOKEN");
        if (accessToken != null && !accessToken.isBlank()) {
            return new Credentials(accessToken.strip(), true);
        }
        String apiKey = System.getenv("TMDB_API_KEY");
        return apiKey == null || apiKey.isBlank() ? null : new Credentials(apiKey.strip(), false);
    }

    /** Loads a movie poster asynchronously; the callback always runs on the JavaFX thread. */
    public static void poster(Movie movie, Consumer<Image> target) {
        artwork(movie).thenApply(paths -> image(paths.poster(), "w500"))
                .thenAccept(image -> deliver(target, image));
    }

    /** Loads a wide hero image, falling back to the poster when TMDB has no backdrop. */
    public static void backdrop(Movie movie, Consumer<Image> target) {
        artwork(movie).thenApply(paths -> paths.backdrop() != null
                        ? image(paths.backdrop(), "w1280")
                        : image(paths.poster(), "w780"))
                .thenAccept(image -> deliver(target, image));
    }

    private static void deliver(Consumer<Image> target, Image image) {
        Platform.runLater(() -> target.accept(image));
    }

    private static CompletableFuture<Artwork> artwork(Movie movie) {
        return ARTWORK.computeIfAbsent(movie.id(), ignored -> {
            if (TMDB_CREDENTIALS == null) {
                if (MISSING_KEY_REPORTED.compareAndSet(false, true)) {
                    System.err.println("Binge: TMDB_ACCESS_TOKEN or TMDB_API_KEY is not set; using the public poster fallback.");
                }
                return requestWikipedia(movie);
            }
            return requestTmdb(movie)
                    .handle((artwork, error) -> {
                        if (error != null) {
                            System.err.println("Binge: could not load TMDB artwork for '" + movie.title()
                                    + "': " + rootMessage(error));
                            return Artwork.EMPTY;
                        }
                        return artwork;
                    })
                    .thenCompose(artwork -> artwork.poster() == null
                            ? requestWikipedia(movie)
                            : CompletableFuture.completedFuture(artwork));
        });
    }

    private static CompletableFuture<Artwork> requestTmdb(Movie movie) {
        String title = URLEncoder.encode(movie.title(), StandardCharsets.UTF_8);
        String authentication = TMDB_CREDENTIALS.bearer()
                ? ""
                : "&api_key=" + URLEncoder.encode(TMDB_CREDENTIALS.value(), StandardCharsets.UTF_8);
        URI uri = URI.create(API_ROOT + "/search/movie?include_adult=false&language=en-US&query=" + title
                + "&year=" + movie.year() + authentication);
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .GET();
        if (TMDB_CREDENTIALS.bearer()) {
            builder.header("Authorization", "Bearer " + TMDB_CREDENTIALS.value());
        }
        HttpRequest request = builder.build();
        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() / 100 != 2) {
                        throw new IllegalStateException("TMDB request failed with HTTP " + response.statusCode());
                    }
                    return parseArtwork(response.body());
                });
    }

    /** Provides poster artwork when a TMDB key is unavailable or the TMDB request has no match. */
    private static CompletableFuture<Artwork> requestWikipedia(Movie movie) {
        String query = URLEncoder.encode(movie.title() + " " + movie.year() + " film", StandardCharsets.UTF_8);
        URI uri = URI.create(WIKIPEDIA_API + "?action=query&generator=search&gsrsearch=" + query
                + "&gsrlimit=1&prop=pageimages&pithumbsize=780&format=json");
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .header("User-Agent", "Binge streaming demo/2.0")
                .GET()
                .build();
        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() / 100 != 2) {
                        throw new IllegalStateException("Wikipedia request failed with HTTP " + response.statusCode());
                    }
                    return parseWikipediaArtwork(response.body());
                })
                .exceptionally(error -> {
                    System.err.println("Binge: could not load fallback poster for '" + movie.title()
                            + "': " + rootMessage(error));
                    return Artwork.EMPTY;
                });
    }

    private static Artwork parseArtwork(String body) {
        try {
            JsonNode results = MAPPER.readTree(body).path("results");
            if (!results.isArray() || results.isEmpty()) {
                return Artwork.EMPTY;
            }
            JsonNode match = results.get(0);
            return new Artwork(text(match, "poster_path"), text(match, "backdrop_path"));
        } catch (Exception error) {
            throw new IllegalStateException("Invalid TMDB response", error);
        }
    }

    private static Artwork parseWikipediaArtwork(String body) {
        try {
            JsonNode pages = MAPPER.readTree(body).path("query").path("pages");
            if (!pages.isObject()) {
                return Artwork.EMPTY;
            }
            var entries = pages.elements();
            if (!entries.hasNext()) {
                return Artwork.EMPTY;
            }
            String poster = text(entries.next().path("thumbnail"), "source");
            return new Artwork(poster, poster);
        } catch (Exception error) {
            throw new IllegalStateException("Invalid Wikipedia response", error);
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && !value.isNull() ? value.asText() : null;
    }

    private static String rootMessage(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }

    private static Image image(String path, String size) {
        if (path == null) {
            return null;
        }
        String url = path.startsWith("http://") || path.startsWith("https://")
                ? path
                : IMAGE_ROOT + size + path;
        return IMAGES.computeIfAbsent(url, value -> new Image(value, true));
    }

    private record Artwork(String poster, String backdrop) {
        private static final Artwork EMPTY = new Artwork(null, null);
    }

    /** TMDB supports a current bearer token as well as the legacy v3 API-key query parameter. */
    private record Credentials(String value, boolean bearer) { }
}
