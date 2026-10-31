package com.kylem.remote;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioProfile;
import android.media.AudioTrack;
import android.os.Build;
import java.util.ArrayList;
import java.util.List;

/**
 * Drives a DIY blaster wired to the 3.5mm jack.
 *
 * A TV's receiver expects a 38 kHz light pulse train, but a phone's audio output
 * cannot generate 38 kHz at all: at a 48 kHz sample rate the ceiling is 24 kHz.
 * Two IR LEDs wired back-to-back solve it — each one conducts on the half of the
 * waveform the other blocks, so a 19 kHz tone becomes 38 kHz of light.
 */
public class JackBlaster {

    /** Author the samples at whatever rate the mixer really runs at. Requesting
     *  96 kHz from a 48 kHz mixer gets the tone resampled, and the resampler's
     *  lowpass drags 19 kHz down on its way past. */
    public static final int RATE_AUTO = 0;
    private static final int[] FALLBACK_RATES = {48000, 44100, 96000};

    /** Never sit on the very edge of what the output can reach: the analog stage
     *  is already pulling the tone down by then. */
    private static final double USABLE = 0.46;

    public static class Result {
        public final boolean ok;
        public final int rate;
        /** What the phone reported the sound was routed to, measured while playing. */
        public final String routed;
        public final String detail;

        Result(boolean ok, int rate, String routed, String detail) {
            this.ok = ok;
            this.rate = rate;
            this.routed = routed;
            this.detail = detail;
        }
    }

    // ------------------------------------------------------------ preferences

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences("jack", Context.MODE_PRIVATE);
    }

    public static boolean enabled(Context c) {
        return p(c).getBoolean("enabled", false);
    }

    public static void setEnabled(Context c, boolean value) {
        p(c).edit().putBoolean("enabled", value).apply();
    }

    /**
     * A plug with only LEDs behind it does not always short the jack's detect
     * switch, so the phone can report an empty port while something is in it.
     */
    public static boolean forced(Context c) {
        return p(c).getBoolean("forced", false);
    }

    public static void setForced(Context c, boolean value) {
        p(c).edit().putBoolean("forced", value).apply();
    }

    /** Drive level as a percentage of full scale. Half is the safe starting point
     *  for a blaster with no series resistor. */
    public static int gainPct(Context c) {
        return p(c).getInt("gain", 50);
    }

    public static void setGainPct(Context c, int value) {
        p(c).edit().putInt("gain", Math.max(10, Math.min(100, value))).apply();
    }

    /** Opposite-phase left and right works with every wiring: one LED pair to
     *  ground, or the pair between tip and ring for double the voltage. */
    public static boolean invertedStereo(Context c) {
        return p(c).getBoolean("invert", true);
    }

    public static void setInvertedStereo(Context c, boolean value) {
        p(c).edit().putBoolean("invert", value).apply();
    }

    public static int preferredRate(Context c) {
        return p(c).getInt("rate", RATE_AUTO);
    }

    public static void setPreferredRate(Context c, int value) {
        p(c).edit().putInt("rate", value).apply();
    }

    // ------------------------------------------------------------- detection

    private static AudioManager am(Context c) {
        return (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
    }

    private static boolean isWiredOutput(int type) {
        return type == AudioDeviceInfo.TYPE_WIRED_HEADSET
                || type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES;
    }

    /** The device entry the jack is reporting, or null when it looks empty. */
    public static AudioDeviceInfo jackDevice(Context c) {
        AudioManager am = am(c);
        if (am == null) return null;
        AudioDeviceInfo[] outs = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
        if (outs == null) return null;
        for (AudioDeviceInfo d : outs) {
            if (d != null && (isWiredOutput(d.getType())
                    || d.getType() == AudioDeviceInfo.TYPE_USB_HEADSET)) {
                return d;
            }
        }
        return null;
    }

    public static boolean plugPresent(Context c) {
        return jackDevice(c) != null;
    }

    public static String typeName(int type) {
        if (type == AudioDeviceInfo.TYPE_WIRED_HEADSET) return "3.5mm port";
        if (type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES) return "3.5mm port";
        if (type == AudioDeviceInfo.TYPE_USB_HEADSET) return "USB audio";
        if (type == AudioDeviceInfo.TYPE_USB_DEVICE) return "USB audio";
        if (type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) return "the phone speaker";
        if (type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP) return "Bluetooth";
        return "device type " + type;
    }

    public static boolean active(Context c) {
        return enabled(c) && (plugPresent(c) || forced(c));
    }

    public static int mediaVolumePct(Context c) {
        AudioManager am = am(c);
        if (am == null) return 100;
        int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        if (max <= 0) return 100;
        return Math.round(100f * am.getStreamVolume(AudioManager.STREAM_MUSIC) / max);
    }

    private static int mixerRate(Context c) {
        AudioManager am = am(c);
        if (am == null) return 0;
        String value = am.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
        if (value == null) return 0;
        return Integer.parseInt(value.trim());
    }

    /**
     * Rates this output can take. getMinBufferSize is not a capability probe — it
     * only rejects invalid arguments and the framework happily resamples anything
     * else — so ask the device itself when the platform lets us.
     */
    private static boolean supported(Context c, int rate) {
        AudioDeviceInfo jack = jackDevice(c);
        if (jack != null && Build.VERSION.SDK_INT >= 31) {
            List<AudioProfile> profiles = jack.getAudioProfiles();
            if (profiles != null && !profiles.isEmpty()) {
                for (AudioProfile profile : profiles) {
                    if (profile == null) continue;
                    int[] rates = profile.getSampleRates();
                    if (rates == null) continue;
                    for (int r : rates) {
                        if (r == rate) return true;
                    }
                }
                return false;
            }
        }
        return AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT) > 0;
    }

    /** The rate to author at, or -1 with the reason in {@code why}. */
    public static int chooseRate(Context c, int carrierHz, StringBuilder why) {
        List<Integer> order = new ArrayList<>();
        int mixer = mixerRate(c);
        if (mixer > 0) order.add(mixer);
        for (int rate : FALLBACK_RATES) {
            if (!order.contains(rate)) order.add(rate);
        }
        int pref = preferredRate(c);
        for (int rate : order) {
            if (pref != RATE_AUTO && rate != pref) continue;
            if (!supported(c, rate)) {
                if (why != null) {
                    why.append(rate).append(" Hz is not offered by this output. ");
                }
                continue;
            }
            if (carrierHz / 2.0 > rate * USABLE) {
                if (why != null) {
                    why.append(rate).append(" Hz cannot reach a ").append(carrierHz / 1000)
                            .append(" kHz carrier. ");
                }
                continue;
            }
            return rate;
        }
        if (why != null && why.length() == 0) why.append("No usable sample rate. ");
        return -1;
    }

    // ------------------------------------------------------------- synthesis

    /**
     * Render a code into interleaved 16-bit stereo. Each mark restarts at phase
     * zero and rounds to a whole number of samples, so burst lengths stay exact to
     * about one sample (21 microseconds). The half cycle left at the end of a mark
     * costs nothing: the jack is AC coupled either way.
     */
    static short[] render(MainActivity.Code code, int rate, int gainPct,
                          boolean invertRight) {
        double tone = code.freq / 2.0;
        double amp = Short.MAX_VALUE * (gainPct / 100.0);

        long frames = 0;
        for (int us : code.pattern) frames += Math.round(us * rate / 1_000_000.0);
        if (frames > 6_000_000L) return null;

        short[] out = new short[(int) frames * 2];
        int at = 0;
        boolean mark = true;
        for (int us : code.pattern) {
            int count = (int) Math.round(us * rate / 1_000_000.0);
            if (mark) {
                for (int i = 0; i < count; i++) {
                    short v = (short) Math.round(
                            amp * Math.sin(2 * Math.PI * i * tone / rate));
                    out[at++] = v;
                    out[at++] = invertRight ? (short) -v : v;
                }
            } else {
                at += count * 2;
            }
            mark = !mark;
        }
        return out;
    }

    // ---------------------------------------------------------------- output

    private static AudioTrack open(int rate, int frames, AudioDeviceInfo jack) {
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        AudioFormat format = new AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(rate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .build();
        int min = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT);
        AudioTrack track = new AudioTrack.Builder()
                .setAudioAttributes(attrs)
                .setAudioFormat(format)
                .setBufferSizeInBytes(Math.max(Math.max(min, 0) * 2, frames * 4))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                .build();
        if (jack != null) track.setPreferredDevice(jack);
        track.setVolume(1f);
        return track;
    }

    /** Blocks until the sound has left the phone, and records where it went. */
    private static Result play(Context c, short[] data, int rate) {
        AudioTrack track = null;
        try {
            track = open(rate, data.length / 2, jackDevice(c));
            track.play();
            track.write(data, 0, data.length);
            long frames = data.length / 2;
            String routed = null;
            int waited = 0;
            while (waited++ < 900) {
                if (routed == null) {
                    AudioDeviceInfo device = track.getRoutedDevice();
                    if (device != null) routed = typeName(device.getType());
                }
                if (track.getPlaybackHeadPosition() >= frames) break;
                Thread.sleep(10);
            }
            return new Result(true, rate, routed, null);
        } catch (Exception e) {
            return new Result(false, rate, null,
                    e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        } finally {
            if (track != null) {
                try {
                    track.stop();
                } catch (IllegalStateException ignored) {
                }
                track.release();
            }
        }
    }

    /** One IR code, out of the jack. */
    public static Result send(Context c, MainActivity.Code code) {
        StringBuilder why = new StringBuilder();
        int rate = chooseRate(c, code.freq, why);
        if (rate < 0) return new Result(false, 0, null, why.toString());
        short[] data = render(code, rate, gainPct(c), invertedStereo(c));
        if (data == null) return new Result(false, rate, null, "Code too long to render.");
        return label(play(c, data, rate), c);
    }

    /**
     * A held test tone, chopped into 20 ms bursts on purpose. A receiver's
     * auto-gain mutes itself after about a second of unbroken carrier, so one
     * long tone can look dead while the blaster is working fine.
     */
    public static Result testTone(Context c, int ms) {
        int carrier = 38000;
        StringBuilder why = new StringBuilder();
        int rate = chooseRate(c, carrier, why);
        if (rate < 0) return new Result(false, 0, null, why.toString());

        double tone = carrier / 2.0;
        double amp = Short.MAX_VALUE * (gainPct(c) / 100.0);
        boolean invert = invertedStereo(c);
        int on = (int) (0.020 * rate), off = (int) (0.010 * rate);
        int total = (int) (ms / 1000.0 * rate);
        short[] data = new short[total * 2];

        int at = 0, phase = 0;
        boolean emitting = true;
        while (at < total) {
            int chunk = emitting ? on : off;
            for (int i = 0; i < chunk && at < total; i++) {
                short v = 0;
                if (emitting) {
                    v = (short) Math.round(amp * Math.sin(2 * Math.PI * phase++ * tone / rate));
                } else {
                    phase = 0;
                }
                data[at * 2] = v;
                data[at * 2 + 1] = invert ? (short) -v : v;
                at++;
            }
            emitting = !emitting;
        }
        return label(play(c, data, rate), c);
    }

    /** Turns the measured route into a verdict the user can act on. */
    private static Result label(Result r, Context c) {
        if (!r.ok || r.routed == null) return r;
        if (r.routed.startsWith("3.5mm") || r.routed.startsWith("USB")) {
            return new Result(true, r.rate, r.routed, "Sound reached the port.");
        }
        return new Result(false, r.rate, r.routed,
                "The phone played it out of " + r.routed + " instead of the port, "
                        + "so nothing reached your LEDs.");
    }

    // ------------------------------------------------------------ reporting

    public static String describe(Context c) {
        int rate = chooseRate(c, 38000, null);
        AudioDeviceInfo jack = jackDevice(c);
        StringBuilder b = new StringBuilder();
        b.append("Plug: ").append(jack == null ? "port reports nothing in it"
                : typeName(jack.getType()) + " connected");
        if (forced(c) && jack == null) b.append(" (sending anyway)");
        b.append("\nJack sending: ").append(enabled(c) ? "on" : "off");
        b.append("\nSample rate: ").append(rate < 0 ? "none usable" : rate + " Hz");
        b.append("\nTone: 19 kHz \u2192 38 kHz of light through a 2-LED pair");
        b.append("\nDrive: ").append(gainPct(c)).append("% of full scale");
        b.append("\nStereo: ").append(invertedStereo(c) ? "opposite phase" : "mirrored");
        b.append("\nMedia volume: ").append(mediaVolumePct(c)).append("%");
        return b.toString();
    }
}
