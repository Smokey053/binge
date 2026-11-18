package com.binge.audio;

/** Small synth that builds sounds in memory, so no audio files are needed. All
buffers are mono and scaled to [-1,1].
 */
public final class ToneSynth {

    public static final int SAMPLE_RATE = 44_100;

    private ToneSynth() {
    }

    /** A fading tone that slides in pitch from the startHz to endHz. 
    A quiet second harmonic adds a warmer, bell-like feel
     */
    public static double[] tone(double startHz, double endHz, double seconds, double gain) {
        int n = (int) (seconds * SAMPLE_RATE);
        double[] out = new double[n];
        double phase = 0;
        for (int i = 0; i < n; i++) {
            double t = (double) i / n;
            double hz = startHz + (endHz - startHz) * t;
            phase += 2 * Math.PI * hz / SAMPLE_RATE;
            double attack = Math.min(1, i / (0.004 * SAMPLE_RATE));
            double decay = Math.exp(-4.5 * t);
            out[i] = (Math.sin(phase) + 0.3 * Math.sin(2 * phase)) / 1.3 * attack * decay * gain;
        }
        return out;
    }

    /** Soft filtered-noise "swish" witha bell-shaped envelope, used for carousel scrolling */
    public static double[] swish(double seconds, double gain) {
        int n = (int) (seconds * SAMPLE_RATE);
        double[] out = new double[n];
        java.util.Random random = new java.util.Random(7);
        double low = 0;
        for (int i = 0; i < n; i++) {
            double t = (double) i / n;
            low += 0.18 * ((random.nextDouble() * 2 - 1) - low);
            out[i] = low * Math.sin(Math.PI * t) * gain * 2.2;
        }
        return out;
    }

    /**Stacks layer on top of base, starting  offsetSeconds in. 
    The result may be longer than both. */
    public static double[] mix(double[] base, double[] layer, double offsetSeconds) {
        int offset = (int) (offsetSeconds * SAMPLE_RATE);
        double[] out = new double[Math.max(base.length, offset + layer.length)];
        System.arraycopy(base, 0, out, 0, base.length);
        for (int i = 0; i < layer.length; i++) {
            out[offset + i] += layer[i];
        }
        return out;
    }

    /** Encodes the samples as a 16-bit little-endian PCM WAV file. */
    public static byte[] toWav(double[] samples) {
        int dataLength = samples.length * 2;
        byte[] wav = new byte[44 + dataLength];
        writeAscii(wav, 0, "RIFF");
        writeIntLe(wav, 4, 36 + dataLength);
        writeAscii(wav, 8, "WAVE");
        writeAscii(wav, 12, "fmt ");
        writeIntLe(wav, 16, 16);
        writeShortLe(wav, 20, 1);
        writeShortLe(wav, 22, 1);
        writeIntLe(wav, 24, SAMPLE_RATE);
        writeIntLe(wav, 28, SAMPLE_RATE * 2);
        writeShortLe(wav, 32, 2);
        writeShortLe(wav, 34, 16);
        writeAscii(wav, 36, "data");
        writeIntLe(wav, 40, dataLength);
        for (int i = 0; i < samples.length; i++) {
            double clamped = Math.max(-1, Math.min(1, samples[i]));
            writeShortLe(wav, 44 + i * 2, (int) Math.round(clamped * Short.MAX_VALUE));
        }
        return wav;
    }

    private static void writeAscii(byte[] target, int offset, String text) {
        for (int i = 0; i < text.length(); i++) {
            target[offset + i] = (byte) text.charAt(i);
        }
    }

    private static void writeIntLe(byte[] target, int offset, int value) {
        target[offset] = (byte) value;
        target[offset + 1] = (byte) (value >> 8);
        target[offset + 2] = (byte) (value >> 16);
        target[offset + 3] = (byte) (value >> 24);
    }

    private static void writeShortLe(byte[] target, int offset, int value) {
        target[offset] = (byte) value;
        target[offset + 1] = (byte) (value >> 8);
    }
}
