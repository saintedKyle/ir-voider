/*
 * IR Voider — Android IR remote.
 * Copyright (C) 2026 poging kyle
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.kylem.remote;

import android.content.Context;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;

/**
 * The ten orange-family themes. Every preset keeps its neutrals warm and its accent somewhere on
 * the ember-to-bronze arc, so switching themes never leaves the app's identity.
 */
public final class Palette {

    public static final int DEFAULT = 0;

    public static final Palette[] PRESETS = {
            new Palette("Ember Signal",
                    0xFF0A0908, 0xFF120E0B, 0xFF14110F, 0xFF1A1715, 0xFF2C2824,
                    0xFFE8753D, 0xFFB07D52, 0xFFE9E0D4, 0xFF8B8176,
                    0xFFE06B55, 0xFFB7C48A, 0xFF0A0908),
            new Palette("Void Amber",
                    0xFF090807, 0xFF120F0A, 0xFF14120E, 0xFF1B1813, 0xFF2E2A21,
                    0xFFE8A33D, 0xFFB58443, 0xFFE9E2D0, 0xFF8B8370,
                    0xFFE08A55, 0xFFAEC48A, 0xFF0A0908),
            new Palette("Rust Signal",
                    0xFF0B0706, 0xFF150D0A, 0xFF17100E, 0xFF1D1412, 0xFF33231E,
                    0xFFC4562B, 0xFF8E4526, 0xFFEBD9CE, 0xFF8A7166,
                    0xFFE06B55, 0xFFB7C48A, 0xFF0B0706),
            new Palette("Copper Plate",
                    0xFF0A0807, 0xFF141009, 0xFF16110D, 0xFF1C1611, 0xFF32261C,
                    0xFFB07D52, 0xFFD9A268, 0xFFE9DECF, 0xFF8B7B67,
                    0xFFD96B4B, 0xFFB7BE8A, 0xFF0A0807),
            new Palette("Solar Flare",
                    0xFF060505, 0xFF100D0B, 0xFF121010, 0xFF191616, 0xFF2E2A28,
                    0xFFFF8A3D, 0xFFFFB067, 0xFFF2E9DE, 0xFF8E8478,
                    0xFFF06B4A, 0xFFB7C48A, 0xFF060505),
            new Palette("Ashen Coal",
                    0xFF0C0B0A, 0xFF141312, 0xFF161514, 0xFF1D1B1A, 0xFF302D2A,
                    0xFFC97A4B, 0xFF9A6A45, 0xFFD9D3CA, 0xFF857F77,
                    0xFFD4694F, 0xFFA9B582, 0xFF0C0B0A),
            new Palette("Hearth Glow",
                    0xFF0D0805, 0xFF1A1109, 0xFF191109, 0xFF211710, 0xFF3A2A1C,
                    0xFFF08C3A, 0xFFC9702E, 0xFFF0E2CE, 0xFF9A8368,
                    0xFFE07055, 0xFFBCC48A, 0xFF0D0805),
            new Palette("Bronze Dusk",
                    0xFF0A0A08, 0xFF131210, 0xFF15140F, 0xFF1C1B15, 0xFF302E22,
                    0xFFC89A54, 0xFFE0B472, 0xFFE7E2D0, 0xFF8C8776,
                    0xFFD97052, 0xFFB2C086, 0xFF0A0A08),
            new Palette("OLED Void",
                    0xFF000000, 0xFF0A0705, 0xFF0E0C0A, 0xFF151210, 0xFF262220,
                    0xFFE8753D, 0xFFB07D52, 0xFFE9E0D4, 0xFF7E766D,
                    0xFFE06B55, 0xFFB7C48A, 0xFF000000),
            new Palette("Bone Ember",
                    0xFFE9E0D4, 0xFFDCD0BE, 0xFFF2EBE0, 0xFFE4DACB, 0xFFC9BCA8,
                    0xFFD4562A, 0xFFA9682F, 0xFF1A1715, 0xFF6A625A,
                    0xFFB23A22, 0xFF5F7A3A, 0xFFF5EFE6),
    };

    public final String name;
    public final int bg;
    public final int bgBottom;
    public final int card;
    public final int btn;
    public final int stroke;
    public final int accent;
    public final int accent2;
    public final int text;
    public final int muted;
    public final int warn;
    public final int ok;
    /** Legible text placed directly on {@link #accent}. */
    public final int onAccent;
    /** True when surfaces are light, so rim light has to come from a dark edge instead. */
    public final boolean light;

    private Palette(String name, int bg, int bgBottom, int card, int btn, int stroke,
                    int accent, int accent2, int text, int muted,
                    int warn, int ok, int onAccent) {
        this.name = name;
        this.bg = bg;
        this.bgBottom = bgBottom;
        this.card = card;
        this.btn = btn;
        this.stroke = stroke;
        this.accent = accent;
        this.accent2 = accent2;
        this.text = text;
        this.muted = muted;
        this.warn = warn;
        this.ok = ok;
        this.onAccent = onAccent;
        this.light = luminance(text) < 0.5f;
    }

    public static Palette current(Context c) {
        return of(DeviceStore.themeIndex(c));
    }

    public static Palette of(int index) {
        return PRESETS[Math.max(0, Math.min(PRESETS.length - 1, index))];
    }

    static float luminance(int argb) {
        return (0.2126f * ((argb >> 16) & 0xFF) + 0.7152f * ((argb >> 8) & 0xFF)
                + 0.0722f * (argb & 0xFF)) / 255f;
    }

    /**
     * Top sheen, always white: light still arrives from above on a bone-coloured plate. The wash is
     * damped there because a near-white surface has almost no headroom left to gain.
     */
    public int gloss(float strength) {
        float s = light ? strength * 0.55f : strength;
        return 0x00FFFFFF | ((int) (255 * s) << 24);
    }

    /** Edge that separates a plate from its neighbour: brighter on dark themes, darker on light. */
    public int rim() {
        return light ? Surface.darken(stroke, 0.18f) : Surface.lighten(stroke, 0.22f);
    }

    public int ripple() {
        return light ? 0x22000000 : 0x22FFFFFF;
    }

    /** Dark clock/battery glyphs when the theme is light, so they stay readable. */
    static void applyStatusIcons(Window window, boolean lightTheme) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (lightTheme && window.getInsetsController() != null) {
                window.getInsetsController().setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
            }
            return;
        }
        int flags = window.getDecorView().getSystemUiVisibility();
        window.getDecorView().setSystemUiVisibility(lightTheme
                ? flags | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                : flags & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    }
}
