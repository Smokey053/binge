package com.binge.ui;

import com.binge.audio.Sound;
import com.binge.audio.SoundFx;

/**
 * Gives the custom controls a way to play sound effects without every control needing a
 * reference to the audio service. The application installs the service once at start-up.
 */
public final class UiSound {

    private static SoundFx service;

    private UiSound() {
    }

    public static void install(SoundFx soundFx) {
        service = soundFx;
    }

    public static void play(Sound sound) {
        if (service != null) {
            service.play(sound);
        }
    }
}
