package com.binge.audio;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.media.AudioClip;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

/**
  Plays the UI sound effects. Sounds are generated once, and  saved to a temporary folder (JavaFX
  needs a URL) and played fast (with low latency). If there is no audio device,
  It turns itself off quietly instead of crashing.
 */
public final class SoundFx {

    private static final long HOVER_GAP_NANOS = 70_000_000L;

    private final BooleanProperty enabled = new SimpleBooleanProperty(true);
    private final Map<Sound, AudioClip> clips = new EnumMap<>(Sound.class);
    private Path folder;
    private boolean unavailable;
    private long lastHover;

    public BooleanProperty enabledProperty() {
        return enabled;
    }

    public void play(Sound sound) {
        if (!enabled.get() || unavailable) {
            return;
        }
        if (sound == Sound.HOVER) {
            long now = System.nanoTime();
            if (now - lastHover < HOVER_GAP_NANOS) {
                return;
            }
            lastHover = now;
        }
        try {
            AudioClip clip = clips.computeIfAbsent(sound, this::createClip);
            clip.play(sound.volume());
        } catch (RuntimeException e) {
            unavailable = true;
            System.err.println("Sound effects disabled: " + e.getMessage());
        }
    }

    /** Pre-builds all sounds so the first hover plays without lag */
    public void warmUp() {
        for (Sound sound : Sound.values()) {
            try {
                clips.computeIfAbsent(sound, this::createClip);
            } catch (RuntimeException e) {
                unavailable = true;
                return;
            }
        }
    }

    private AudioClip createClip(Sound sound) {
        try {
            if (folder == null) {
                folder = Files.createTempDirectory("binge-sfx");
                Runtime.getRuntime().addShutdownHook(new Thread(this::cleanUp));
            }
            Path file = folder.resolve(sound.name().toLowerCase() + ".wav");
            Files.write(file, sound.renderWav());
            return new AudioClip(file.toUri().toString());
        } catch (IOException e) {
            throw new IllegalStateException("Cannot write sound effect", e);
        }
    }

    private void cleanUp() {
        if (folder == null) {
            return;
        }
        try (var files = Files.list(folder)) {
            files.forEach(f -> f.toFile().delete());
            Files.deleteIfExists(folder);
        } catch (IOException ignored) {
            // temp files are harmless if they cannot be removed
        }
    }
}
