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
 * The rim is a gradient (accent at the corners, white across the lit edge) because a single stroke
 * colour looks like a sticker, not an edge.
 */
final class Glass {

    private Glass() {
    }

    /** Key plate used by every button on the controls pages. */
    static Drawable plate(Context c, Palette p, int accent, boolean hero) {
        return new Plate(c, p, accent, hero);
    }

    private static final class Plate extends Drawable {

        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint gloss = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF box = new RectF();
        private final Path shape = new Path();
        private final float radius;
        private final float edge;
        private final boolean light;
        private final int accent;

        Plate(Context c, Palette p, int accent, boolean hero) {
            radius = Surface.dp(c, hero ? 24 : 21);
            edge = Math.max(1f, Surface.dp(c, hero ? 1.6f : 1.3f));
            this.light = p.light;
            this.accent = accent;
            fill.setStyle(Paint.Style.FILL);
            rim.setStyle(Paint.Style.STROKE);
            rim.setStrokeWidth(edge);
            gloss.setStyle(Paint.Style.STROKE);
            gloss.setStrokeWidth(Math.max(1f, edge * 0.8f));
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

            int top = 0xFFFFFFFF;
            int topColor = alpha(top, light ? 0x8A : 0x22);
            int midColor = alpha(top, light ? 0x3D : 0x12);
            int lowColor = alpha(top, light ? 0x14 : 0x0A);
            int rimEdge = alpha(Surface.blend(accent, 0xFFFFFFFF, light ? 0.10f : 0.42f),
                    light ? 0xB0 : 0x90);
            int rimSoft = alpha(top, light ? 0x38 : 0x2A);

            // Body: glass catches more light at the top than at the bottom.
            fill.setShader(new LinearGradient(0, b.top, 0, b.bottom,
                    new int[]{topColor, midColor, lowColor},
                    new float[]{0f, 0.46f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawPath(shape, fill);
            fill.setShader(null);

            // Contact shade at the underside and a faint inner shade at the top, both as one
            // full-size gradient so the corners stay rounded without clipping the canvas.
            fill.setShader(new LinearGradient(0, b.top, 0, b.bottom,
                    new int[]{light ? 0x1A000000 : 0x0C000000, 0x00000000,
                            0x00000000, light ? 0x14000000 : 0x38000000},
                    new float[]{0f, 0.17f, 0.72f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawPath(shape, fill);
            fill.setShader(null);

            // Rim: accent in the corners, near-white across the lit edge.
            rim.setShader(new LinearGradient(b.left, b.top, b.right, b.bottom,
                    new int[]{rimEdge, rimSoft, rimEdge},
                    new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawPath(shape, rim);
            rim.setShader(null);

            // Specular line just inside the top edge.
            float inset = edge * 1.9f;
            RectF g = new RectF(b.left + inset, b.top + inset,
                    b.right - inset, b.bottom - inset);
            gloss.setShader(new LinearGradient(0, g.top, 0, g.centerY(),
                    new int[]{rimSoft, alpha(top, 0x00)},
                    new float[]{0f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(g, Math.max(1f, radius - inset),
                    Math.max(1f, radius - inset), gloss);
            gloss.setShader(null);
        }

        @Override
        public void setAlpha(int a) {
            fill.setAlpha(a);
        }

        @Override
        public void setColorFilter(android.graphics.ColorFilter colorFilter) {
            fill.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
