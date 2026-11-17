/*
 * IR Voider — Android IR remote.
 * Copyright (C) 2026 poging kyle
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.kylem.remote;

/**
 * Midea R05D aircon frames.
 *
 * <p>A message is two identical 48-bit packets:
 * {@code L  B2 4D  B ~B  C ~C  S  L  B2 4D  B ~B  C ~C}.</p>
 *
 * <p>{@code B} holds the fan in bits 7..5 (bits 4..0 stay set): auto 101,
 * low 100, medium 010, high 001, fixed 000. {@code C} holds a Gray-coded
 * temperature in bits 7..4 and the mode in bits 3..2: cool 00, dry 01,
 * heat 11, auto 10. Fan-only reuses dry's mode bits with a 1110
 * temperature nibble; the fixed 0x7B / 0xE0 pair is the Off frame.
 * The AirMax and Beko captures are byte-identical, which is why 1.6.x could
 * not offer temperature there: the profile only ever captured the 17&nbsp;°C
 * cool frame. The frames below are built from that capture, so they use the
 * exact header and bit timing this unit already answers.</p>
 */
final class R05D {
    private static final int[] TEMP_CODE = {
            0x0, 0x1, 0x3, 0x2, 0x6, 0x7, 0x5, 0x4,
            0xC, 0xD, 0x9, 0x8, 0xA, 0xB
    };
    private static final int MIN_TEMP = 17;
    private static final int MAX_TEMP = 30;

    private R05D() {
    }

    /** True when the power code is a 48-bit R05D state packet sent twice. */
    static boolean supported(MainActivity.Code power) {
        return bytes(power) != null;
    }

    /** Decoded six data bytes, or null when this is not R05D. */
    static int[] bytes(MainActivity.Code code) {
        if (code == null || code.pattern == null || code.pattern.length != 200) return null;
        int[] pattern = code.pattern;
        if (pattern[0] < 3500 || pattern[1] < 3500) return null;
        int[] out = new int[6];
        for (int byteIndex = 0; byteIndex < 6; byteIndex++) {
            int value = 0;
            for (int bit = 0; bit < 8; bit++) {
                int space = pattern[3 + 2 * (byteIndex * 8 + bit)];
                value = (value << 1) | (space > 1000 ? 1 : 0);
            }
            out[byteIndex] = value;
        }
        if (out[1] != ((~out[0]) & 0xFF)) return null;
        if (out[3] != ((~out[2]) & 0xFF)) return null;
        if (out[5] != ((~out[4]) & 0xFF)) return null;
        if (out[0] != 0xB2 || out[1] != 0x4D) return null;
        return out;
    }

    /** Temperature index 0..13 for 17..30 °C, or -1 when outside the range. */
    static int tempIndex(int celsius) {
        return celsius < MIN_TEMP || celsius > MAX_TEMP ? -1 : celsius - MIN_TEMP;
    }

    static int tempIndexFromByte(int cByte) {
        int nibble = (cByte >> 4) & 0x0F;
        for (int i = 0; i < TEMP_CODE.length; i++) {
            if (TEMP_CODE[i] == nibble) return i;
        }
        return tempIndex(24);
    }

    static int celsiusFromIndex(int index) {
        return MIN_TEMP + Math.max(0, Math.min(TEMP_CODE.length - 1, index));
    }

    static int fanBits(MainActivity.Code power) {
        int[] data = bytes(power);
        return data == null ? 5 : (data[2] >> 5) & 0x07;
    }

    static int modeBits(MainActivity.Code power) {
        int[] data = bytes(power);
        return data == null ? 0 : (data[4] >> 2) & 0x03;
    }

    static int tempIndex(MainActivity.Code power) {
        int[] data = bytes(power);
        return data == null ? tempIndex(24) : tempIndexFromByte(data[4]);
    }

    /** A state frame with the given fan bits, temperature index and mode bits. */
    static MainActivity.Code state(MainActivity.Code template, int fan,
                                   int tempIndex, int mode, boolean fanOnly) {
        int b = ((fan & 0x07) << 5) | 0x1F;
        int tempNibble = fanOnly ? 0x0E
                : TEMP_CODE[Math.max(0, Math.min(TEMP_CODE.length - 1, tempIndex))];
        int c = ((tempNibble & 0x0F) << 4) | ((mode & 0x03) << 2);
        return frame(template, b, c);
    }

    /** Build both copies of a message from the captured header and bit timing. */
    static MainActivity.Code frame(MainActivity.Code template, int b, int c) {
        if (template == null || template.pattern == null
                || template.pattern.length != 200) return null;
        int shortSpace = 0;
        int longSpace = 0;
        for (int i = 3; i < 99; i += 2) {
            int space = template.pattern[i];
            if (space < 300 || space > 2500) continue;
            if (shortSpace == 0 || space < shortSpace) shortSpace = space;
            if (longSpace == 0 || space > longSpace) longSpace = space;
        }
        if (shortSpace == 0 || longSpace == 0 || shortSpace == longSpace) {
            shortSpace = 481;
            longSpace = 1553;
        }
        int[] data = {0xB2, 0x4D, b, (~b) & 0xFF, c, (~c) & 0xFF};
        int[] pattern = template.pattern.clone();
        for (int copy = 0; copy < 2; copy++) {
            int base = copy == 0 ? 2 : 102;
            for (int bit = 0; bit < 48; bit++) {
                int value = (data[bit / 8] >> (7 - (bit % 8))) & 1;
                pattern[base + 2 * bit + 1] = value == 1 ? longSpace : shortSpace;
            }
        }
        MainActivity.Code out = new MainActivity.Code();
        out.freq = template.freq;
        out.pattern = pattern;
        return out;
    }
}
