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
 * The ten themes. Each one carries its own neutrals, so a blue accent sits on blue-black
 * surfaces rather than on a warm card with a cold button. Ember Signal stays the default.
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
            new Palette("Terrace Green",
                    0xFF070B08, 0xFF0C130E, 0xFF0E1510, 0xFF141D17, 0xFF243528,
                    0xFF5FC97A, 0xFF3E9E62, 0xFFDCE9DC, 0xFF75897A,
                    0xFFE0705A, 0xFF9BD48F, 0xFF07130B),
            new Palette("Teal Current",
                    0xFF060B0C, 0xFF0A1214, 0xFF0D1518, 0xFF131E22, 0xFF21363B,
                    0xFF31C7B8, 0xFF2A9E97, 0xFFD8E9E8, 0xFF70898C,
                    0xFFE0705A, 0xFF8FD4A0, 0xFF04100F),
            new Palette("Cobalt Pulse",
                    0xFF070911, 0xFF0B1020, 0xFF0E1322, 0xFF141B2E, 0xFF24304F,
                    0xFF4C8DF6, 0xFF7FA8E8, 0xFFDDE4F5, 0xFF7783A0,
                    0xFFE06B6B, 0xFF8FD48F, 0xFF060A14),
            new Palette("Ultraviolet",
                    0xFF0A0713, 0xFF110C1F, 0xFF140F24, 0xFF1B1530, 0xFF2E2450,
                    0xFFA47CF5, 0xFF7E5BD0, 0xFFE6DFF7, 0xFF857CA0,
                    0xFFE06B8F, 0xFF9BD48F, 0xFF0B0716),
            new Palette("Rose Signal",
                    0xFF0E070B, 0xFF170B12, 0xFF1A0E15, 0xFF23131C, 0xFF3B2030,
                    0xFFF2649B, 0xFFC25A86, 0xFFEDDCE3, 0xFF9A7A87,
                    0xFFE0605A, 0xFF9BD48F, 0xFF140409),
            new Palette("Smoke Signal",
                    0xFF0A0A0B, 0xFF121214, 0xFF151517, 0xFF1C1C1F, 0xFF2F2F34,
                    0xFFC3C9D2, 0xFF8C93A0, 0xFFE7E8EA, 0xFF84868C,
                    0xFFE06B55, 0xFF9BD48F, 0xFF0A0A0B),
            new Palette("OLED Void",
                    0xFF000000, 0xFF0A0705, 0xFF0E0C0A, 0xFF151210, 0xFF262220,
                    0xFFE8753D, 0xFFB07D52, 0xFFE9E0D4, 0xFF7E766D,
                    0xFFE06B55, 0xFFB7C48A, 0xFF000000),
            new Palette("Bone Frost",
                    0xFFE7EBF2, 0xFFD8DEE9, 0xFFF1F4F9, 0xFFDFE5EF, 0xFFC0C8D6,
                    0xFF2F6FD0, 0xFF5D8BD8, 0xFF141821, 0xFF5E6674,
                    0xFFC23A2E, 0xFF3F7A3A, 0xFFF4F7FC),
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
