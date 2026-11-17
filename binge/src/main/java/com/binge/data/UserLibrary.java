package com.binge.data;

import com.binge.model.Movie;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Per-user state that survives restarts: the "My List" shelf, recently watched trailers and
 * the sound / theme preferences. Everything is stored as one small JSON file.
 */
public final class UserLibrary {

    private static final int MAX_RECENT = 10;

    /** On-disk representation. */
    public record Snapshot(List<String> myList, List<String> recent, boolean soundEnabled, boolean lightTheme) {
        public Snapshot {
            myList = myList == null ? List.of() : List.copyOf(myList);
            recent = recent == null ? List.of() : List.copyOf(recent);
        }
    }

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final ObservableList<String> myList = FXCollections.observableArrayList();
    private final ObservableList<String> recent = FXCollections.observableArrayList();
    private final BooleanProperty soundEnabled = new SimpleBooleanProperty(true);
    private final BooleanProperty lightTheme = new SimpleBooleanProperty(false);
    private final Path file;
    private boolean loading;

    public UserLibrary(Path file) {
        this.file = file;
        load();
        myList.addListener((ListChangeListener<String>) c -> save());
        recent.addListener((ListChangeListener<String>) c -> save());
        soundEnabled.addListener((o, a, b) -> save());
        lightTheme.addListener((o, a, b) -> save());
    }

    /** Library stored in {@code ~/.binge/library.json}. */
    public static UserLibrary atDefaultLocation() {
        return new UserLibrary(Path.of(System.getProperty("user.home"), ".binge", "library.json"));
    }

    public ObservableList<String> myList() {
        return myList;
    }

    public ObservableList<String> recent() {
        return recent;
    }

    public BooleanProperty soundEnabledProperty() {
        return soundEnabled;
    }

    public BooleanProperty lightThemeProperty() {
        return lightTheme;
    }

    public boolean isInMyList(Movie movie) {
        return myList.contains(movie.id());
    }

    /** A binding that stays in sync with the My List shelf; used by cards and buttons. */
    public BooleanBinding inMyListBinding(Movie movie) {
        return Bindings.createBooleanBinding(() -> myList.contains(movie.id()), myList);
    }

    /** Adds the movie when absent, removes it otherwise. Returns {@code true} if it is now in the list. */
    public boolean toggleMyList(Movie movie) {
        if (myList.remove(movie.id())) {
            return false;
        }
        myList.add(0, movie.id());
        return true;
    }

    /** Records a trailer view; the newest entry always comes first and duplicates are collapsed. */
    public void addRecent(Movie movie) {
        loading = true;
        try {
            recent.remove(movie.id());
            recent.add(0, movie.id());
            if (recent.size() > MAX_RECENT) {
                recent.remove(MAX_RECENT, recent.size());
            }
        } finally {
            loading = false;
        }
        save();
    }

    private void load() {
        if (!Files.isRegularFile(file)) {
            return;
        }
        loading = true;
        try {
            Snapshot snapshot = mapper.readValue(file.toFile(), Snapshot.class);
            myList.setAll(snapshot.myList());
            recent.setAll(snapshot.recent());
            soundEnabled.set(snapshot.soundEnabled());
            lightTheme.set(snapshot.lightTheme());
        } catch (IOException | RuntimeException e) {
            System.err.println("Ignoring unreadable library file " + file + ": " + e.getMessage());
        } finally {
            loading = false;
        }
    }

    private void save() {
        if (loading) {
            return;
        }
        try {
            Files.createDirectories(file.getParent());
            Path temp = file.resolveSibling(file.getFileName() + ".tmp");
            mapper.writerWithDefaultPrettyPrinter().writeValue(temp.toFile(),
                    new Snapshot(new ArrayList<>(myList), new ArrayList<>(recent), soundEnabled.get(), lightTheme.get()));
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("Could not save library: " + e.getMessage());
        }
    }
}
