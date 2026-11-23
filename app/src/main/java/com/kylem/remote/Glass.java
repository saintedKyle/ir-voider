/*
 * IR Voider — Android IR remote.
 * Copyright (C) 2026 poging kyle
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.kylem.remote;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/**
 * Frosted-glass plates for the controls pages.
 *
 * <p>A plate is a translucent body with a lit top edge, a soft underside and a rim that takes the
 * device accent, so a button reads as glass sitting on the card rather than a flat rounded box.
 * The rim is a gradient — near-white where the light lands, accent-tinted where it falls away —
 * because a single stroke colour looks like a sticker, not an edge.
 *
 * <p>All gradients are dithered. At these alphas an undithered white-to-transparent ramp bands
 * into visible stripes on an OLED panel, which is what makes a glass button look cheap.
 */
final class Glass {

    private Glass() {
    }

    /** Key plate used by every button on the controls pages. */
    static Drawable plate(Context c, Palette p, int accent, boolean hero) {
        return new Plate(c, p, accent, hero);
    }

    private static final class Plate extends Drawable {

        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        private final Paint gloss = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF box = new RectF();
        private final Path shape = new Path();
        private final float radius;
        private final float edge;
        private final boolean light;

        /** Accent mixed toward white; returns an opaque colour. */
        private final int glow;

        Plate(Context c, Palette p, int accent, boolean hero) {
            radius = Surface.dp(c, hero ? 24 : 20);
            edge = Math.max(1.2f, Surface.dp(c, hero ? 1.6f : 1.35f));
            this.light = p.light;
            this.glow = Surface.blend(accent, 0xFFFFFFFF, light ? 0.15f : 0.5f);
            fill.setStyle(Paint.Style.FILL);
            rim.setStyle(Paint.Style.STROKE);
            rim.setStrokeWidth(edge);
            gloss.setStyle(Paint.Style.STROKE);
            gloss.setStrokeWidth(Math.max(1f, Surface.dp(c, 1)));
        }

        /** Replace a colour's alpha, keeping its hue. */
        private static int alpha(int argb, int a) {
            return (argb & 0x00FFFFFF) | ((a & 0xFF) << 24);
        }

        @Override
        public void draw(Canvas canvas) {
            RectF b = box;
            b.set(getBounds());
            b.inset(edge / 2f, edge / 2f);
            shape.reset();
            shape.addRoundRect(b, radius, radius, Path.Direction.CW);

            int white = 0xFFFFFFFF;
            int ink = 0xFF000000;

            // Body: light enters at the top, the accent bleeds through the middle, the bottom
            // of the plate falls into shade. One ramp, so there is nothing to band against.
            fill.setShader(new LinearGradient(0, b.top, 0, b.bottom,
                    new int[]{alpha(white, light ? 0x9C : 0x28),
                            alpha(Surface.blend(white, glow, 0.35f), light ? 0x30 : 0x18),
                            alpha(white, light ? 0x0C : 0x06)},
                    new float[]{0f, 0.52f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawPath(shape, fill);

            // Contact shade along the underside, so the plate sits on the card instead of
            // floating above it.
            fill.setShader(new LinearGradient(0, b.top, 0, b.bottom,
                    new int[]{0x00000000, 0x00000000, alpha(ink, light ? 0x18 : 0x3C)},
                    new float[]{0f, 0.6f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawPath(shape, fill);
            fill.setShader(null);

            // Rim: bright where the light lands at the top, accent in the shadow at the bottom.
            rim.setShader(new LinearGradient(0, b.top, 0, b.bottom,
                    new int[]{alpha(glow, light ? 0xE6 : 0xC0),
                            alpha(Surface.blend(glow, ink, light ? 0.25f : 0.45f),
                                    light ? 0x8A : 0x78)},
                    new float[]{0f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawPath(shape, rim);
            rim.setShader(null);

            // Specular line just inside the top edge: the last cue that says glass.
            float inset = Math.max(2.2f, edge * 1.7f);
            RectF g = new RectF(b.left + inset, b.top + inset,
                    b.right - inset, b.bottom - inset);
            gloss.setShader(new LinearGradient(0, g.top, 0, g.top + (b.height() * 0.38f),
                    new int[]{alpha(white, light ? 0x66 : 0x3C), alpha(white, 0x00)},
                    new float[]{0f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(g, Math.max(1f, radius - inset),
                    Math.max(1f, radius - inset), gloss);
            gloss.setShader(null);
        }

        @Override
        public void setAlpha(int a) {
            fill.setAlpha(a);
            rim.setAlpha(a);
            gloss.setAlpha(a);
        }

        @Override
        public void setColorFilter(android.graphics.ColorFilter colorFilter) {
            fill.setColorFilter(colorFilter);
            rim.setColorFilter(colorFilter);
            gloss.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
