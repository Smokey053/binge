package com.binge.audio;

import java.util.function.Supplier;

import static com.binge.audio.ToneSynth.mix;
import static com.binge.audio.ToneSynth.swish;
import static com.binge.audio.ToneSynth.tone;

/** The catalogue of UI sound effects. Each one is synthesised on demand by {@link ToneSynth}. */
public enum Sound {
    /** Soft tick when the pointer enters a thumbnail or control. */
    HOVER(0.35, () -> tone(1500, 2000, 0.045, 0.22)),
    /** Generic confirmation for navigation and chips. */
    CLICK(0.7, () -> mix(tone(520, 520, 0.09, 0.4), tone(780, 780, 0.12, 0.34), 0.04)),
    /** A rising whoosh when a trailer starts to load. */
    OPEN(0.8, () -> mix(mix(tone(220, 880, 0.35, 0.4), tone(660, 660, 0.4, 0.2), 0.12), swish(0.3, 0.12), 0)),
    /** Falling blip when the player closes. */
    CLOSE(0.7, () -> tone(660, 330, 0.22, 0.4)),
    PLAY(0.7, () -> mix(tone(523, 523, 0.09, 0.4), tone(784, 784, 0.16, 0.4), 0.07)),
    PAUSE(0.7, () -> mix(tone(784, 784, 0.09, 0.4), tone(523, 523, 0.16, 0.4), 0.07)),
    /** Added to My List. */
    ADD(0.7, () -> mix(tone(660, 660, 0.08, 0.4), tone(990, 990, 0.16, 0.4), 0.07)),
    /** Removed from My List. */
    REMOVE(0.7, () -> mix(tone(660, 660, 0.08, 0.4), tone(440, 440, 0.16, 0.4), 0.07)),
    /** Carousel page change. */
    SWIPE(0.6, () -> swish(0.2, 0.3)),
    ERROR(0.8, () -> mix(tone(180, 160, 0.15, 0.5), tone(160, 130, 0.22, 0.5), 0.17)),
    /** Deep "ta-dum" played with the start-up animation. */
    INTRO(0.9, () -> mix(tone(98, 98, 0.9, 0.7), tone(147, 147, 1.1, 0.7), 0.42));

    private final double volume;
    private final Supplier<double[]> synth;

    Sound(double volume, Supplier<double[]> synth) {
        this.volume = volume;
        this.synth = synth;
    }

    double volume() {
        return volume;
    }

    byte[] renderWav() {
        return ToneSynth.toWav(synth.get());
    }
}
