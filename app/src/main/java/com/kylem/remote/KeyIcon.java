/*
 * IR Voider — Android IR remote.
 * Copyright (C) 2026 poging kyle
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.kylem.remote;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

/**
 * Crisp vector glyph for every remote key, drawn in a 24×24 box and scaled to the tile.
 *
 * <p>The icons are original line drawings of the function each key performs (power, volume,
 * navigation, media transport, modes) plus typographic marks for digits and letters. No vendor
 * logo artwork is bundled: a TV brand's logo is a trademark, and a key that sends "Home" is not
 * the same thing as that brand's mark. Shortcuts are labelled with the service name instead.
 */
final class KeyIcon {

    private KeyIcon() {
    }

    static Drawable of(Context c, String key, int color, int sizeDp) {
        int px = Math.max(12, (int) Surface.dp(c, sizeDp));
        return new Glyph(key == null ? "" : key, color, px);
    }

    private static final class Glyph extends Drawable {

        private final String key;
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint solid = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint type = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final RectF rect = new RectF();
        private int size;

        Glyph(String key, int color, int size) {
            this.key = key;
            this.size = size;
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(1.7f);
            stroke.setStrokeCap(Paint.Cap.ROUND);
            stroke.setStrokeJoin(Paint.Join.ROUND);
            stroke.setColor(color);
            solid.setStyle(Paint.Style.FILL);
            solid.setColor(color);
            type.setColor(color);
            type.setTextAlign(Paint.Align.CENTER);
            type.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            type.setTextSize(17f);
        }

        @Override
        public int getIntrinsicWidth() {
            return size;
        }

        @Override
        public int getIntrinsicHeight() {
            return size;
        }

        @Override
        public void draw(Canvas canvas) {
            float s = getBounds().width() / 24f;
            canvas.save();
            canvas.translate(getBounds().left, getBounds().top);
            canvas.scale(s, s);
            drawKey(canvas);
            canvas.restore();
        }

        // ------------------------------------------------------------- helpers

        private void line(float x1, float y1, float x2, float y2) {
            path.reset();
            path.moveTo(x1, y1);
            path.lineTo(x2, y2);
            canvasDraw(path);
        }

        private void canvasDraw(Path p) {
            pending.drawPath(p, stroke);
        }

        private Canvas pending;

        private void circle(float cx, float cy, float r, boolean filled) {
            path.reset();
            path.addCircle(cx, cy, r, Path.Direction.CW);
            pending.drawPath(path, filled ? solid : stroke);
        }

        private void ring(float l, float t, float r, float b, float rad, boolean filled) {
            path.reset();
            rect.set(l, t, r, b);
            path.addRoundRect(rect, rad, rad, Path.Direction.CW);
            pending.drawPath(path, filled ? solid : stroke);
        }

        private void tri(float x1, float y1, float x2, float y2, float x3, float y3) {
            path.reset();
            path.moveTo(x1, y1);
            path.lineTo(x2, y2);
            path.lineTo(x3, y3);
            path.close();
            pending.drawPath(path, solid);
        }

        private void arrow(float fromX, float fromY, float toX, float toY) {
            line(fromX, fromY, toX, toY);
            float dx = toX - fromX;
            float dy = toY - fromY;
            double a = Math.atan2(dy, dx);
            float len = 4f;
            line(toX, toY,
                    (float) (toX - len * Math.cos(a - 0.6)),
                    (float) (toY - len * Math.sin(a - 0.6)));
            line(toX, toY,
                    (float) (toX - len * Math.cos(a + 0.6)),
                    (float) (toY - len * Math.sin(a + 0.6)));
        }

        private void glyph(String text, float sizeUnits) {
            type.setTextSize(sizeUnits);
            pending.drawText(text, 12f, 12f + sizeUnits * 0.34f, type);
        }

        // --------------------------------------------------------------- keys

        private void drawKey(Canvas canvas) {
            pending = canvas;
            String k = key;
            if (k.startsWith("letter_") && k.length() == 8) {
                glyph(k.substring(7).toUpperCase(java.util.Locale.US), 17f);
                return;
            }
            if (k.startsWith("num_") && k.length() == 5) {
                glyph(k.substring(4), 18f);
                return;
            }
            if (k.startsWith("ac_temp_")) {
                thermometer(true);
                return;
            }
            if (k.startsWith("ac_mode_")) {
                mode();
                return;
            }
            if (k.startsWith("ac_fan_")) {
                fan();
                return;
            }
            switch (k) {
                case "power":
                case "power_off":
                    power();
                    break;
                case "vol_up":
                    speaker();
                    plus(17f, 8f, 3f);
                    break;
                case "vol_down":
                    speaker();
                    plus(17f, 8f, 0f);
                    break;
                case "mute":
                    speaker();
                    line(14f, 8f, 20f, 16f);
                    line(20f, 8f, 14f, 16f);
                    break;
                case "ch_up":
                    chevron(0f, false);
                    line(8f, 18f, 16f, 18f);
                    break;
                case "ch_down":
                    chevron(0f, true);
                    line(8f, 6f, 16f, 6f);
                    break;
                case "up":
                    chevron(0f, false);
                    break;
                case "down":
                    chevron(0f, true);
                    break;
                case "left":
                    chevron(-90f, false);
                    break;
                case "right":
                    chevron(90f, false);
                    break;
                case "ok":
                    circle(12f, 12f, 8f, false);
                    circle(12f, 12f, 3f, true);
                    break;
                case "back":
                case "last":
                    arrow(20f, 12f, 5f, 12f);
                    break;
                case "home":
                    home();
                    break;
                case "menu":
                    line(5f, 8f, 19f, 8f);
                    line(5f, 12f, 19f, 12f);
                    line(5f, 16f, 19f, 16f);
                    break;
                case "exit":
                    ring(4f, 5f, 16f, 19f, 3f, false);
                    arrow(12f, 12f, 19f, 5f);
                    break;
                case "input":
                    ring(5f, 7f, 19f, 19f, 3f, false);
                    arrow(2f, 12f, 12f, 12f);
                    break;
                case "info":
                    circle(12f, 12f, 8.5f, false);
                    line(12f, 11f, 12f, 17f);
                    circle(12f, 7.4f, 1.1f, true);
                    break;
                case "guide":
                    ring(3.5f, 5f, 20.5f, 19f, 2.5f, false);
                    line(8f, 9f, 16f, 9f);
                    line(8f, 12.5f, 16f, 12.5f);
                    line(8f, 16f, 13f, 16f);
                    break;
                case "play_pause":
                    tri(7f, 5f, 7f, 19f, 16f, 12f);
                    line(19f, 6f, 19f, 18f);
                    break;
                case "stop":
                    ring(6f, 6f, 18f, 18f, 2f, true);
                    break;
                case "rewind":
                    tri(12f, 6f, 12f, 18f, 4f, 12f);
                    tri(21f, 6f, 21f, 18f, 13f, 12f);
                    break;
                case "forward":
                    tri(12f, 6f, 12f, 18f, 20f, 12f);
                    tri(3f, 6f, 3f, 18f, 11f, 12f);
                    break;
                case "prev":
                    line(5f, 6f, 5f, 18f);
                    tri(19f, 6f, 19f, 18f, 8f, 12f);
                    break;
                case "next":
                    line(19f, 6f, 19f, 18f);
                    tri(5f, 6f, 5f, 18f, 16f, 12f);
                    break;
                case "record":
                    circle(12f, 12f, 6f, true);
                    break;
                case "temp_up":
                    thermometer(true);
                    break;
                case "temp_down":
                    thermometer(false);
                    break;
                case "mode":
                    mode();
                    break;
                case "fan":
                case "speed":
                    fan();
                    break;
                case "swing":
                    swing();
                    break;
                case "light":
                    light();
                    break;
                case "timer":
                    clock();
                    break;
                case "sleep":
                    moon();
                    break;
                case "brightness_up":
                case "brightness_down":
                    sun();
                    break;
                case "color":
                    circle(8f, 9f, 3.2f, false);
                    circle(16f, 9f, 3.2f, false);
                    circle(12f, 16f, 3.2f, false);
                    break;
                case "red":
                    dot(0xFFE05A4A);
                    break;
                case "green":
                    dot(0xFF6FBF73);
                    break;
                case "yellow":
                    dot(0xFFE8C15A);
                    break;
                case "blue":
                    dot(0xFF5A8FE0);
                    break;
                case "zoom_in":
                    magnifier();
                    plus(16.5f, 16.5f, 2.6f);
                    break;
                case "zoom_out":
                    magnifier();
                    plus(16.5f, 16.5f, 0f);
                    break;
                case "subtitle":
                    ring(2.5f, 6f, 21.5f, 18f, 3f, false);
                    glyphAt("CC", 10f, 12f, 13.4f);
                    break;
                case "audio":
                    speaker();
                    line(15f, 7f, 20f, 17f);
                    break;
                case "aspect":
                    ring(3f, 5.5f, 21f, 18.5f, 2f, false);
                    line(7f, 9f, 10f, 9f);
                    line(7f, 9f, 7f, 12f);
                    line(17f, 15f, 14f, 15f);
                    line(17f, 15f, 17f, 12f);
                    break;
                case "fav":
                    star();
                    break;
                case "apps":
                    ring(4f, 4f, 10.5f, 10.5f, 2.2f, true);
                    ring(13.5f, 4f, 20f, 10.5f, 2.2f, true);
                    ring(4f, 13.5f, 10.5f, 20f, 2.2f, true);
                    ring(13.5f, 13.5f, 20f, 20f, 2.2f, true);
                    break;
                case "netflix":
                    ring(3f, 3f, 21f, 21f, 5f, false);
                    glyphAt("N", 15f, 12f, 17.4f);
                    break;
                case "youtube":
                    ring(2.5f, 5.5f, 21.5f, 18.5f, 5f, false);
                    tri(10f, 9f, 10f, 15f, 15f, 12f);
                    break;
                case "prime":
                case "disney":
                case "hbo":
                case "spotify":
                    ring(3f, 3f, 21f, 21f, 6f, false);
                    glyphAt(k.substring(0, 1).toUpperCase(java.util.Locale.US), 13f, 12f, 16.6f);
                    break;
                case "browser":
                case "web":
                case "copilot":
                    globe();
                    break;
                case "dash":
                    line(7f, 12f, 17f, 12f);
                    break;
                case "dot":
                    circle(12f, 16f, 1.8f, true);
                    break;
                case "space":
                    line(5f, 16f, 19f, 16f);
                    break;
                case "backspace":
                    arrow(20f, 12f, 9f, 12f);
                    line(9f, 7f, 4f, 12f);
                    line(9f, 17f, 4f, 12f);
                    break;
                case "enter":
                    arrow(5f, 15f, 5f, 9f);
                    line(5f, 9f, 16f, 9f);
                    break;
                default:
                    initials();
            }
        }

        private void glyphAt(String text, float sizeUnits, float cx, float baseline) {
            type.setTextSize(sizeUnits);
            pending.drawText(text, cx, baseline, type);
        }

        private void initials() {
            String s = key.replace("_", " ").trim();
            if (s.isEmpty()) {
                circle(12f, 12f, 6f, false);
                return;
            }
            glyphAt(s.substring(0, 1).toUpperCase(java.util.Locale.US), 17f, 12f, 18f);
        }

        private void power() {
            path.reset();
            rect.set(4.5f, 5.5f, 19.5f, 20f);
            path.addArc(rect, -62f, 304f);
            pending.drawPath(path, stroke);
            line(12f, 2.5f, 12f, 11f);
        }

        private void speaker() {
            path.reset();
            path.moveTo(3f, 9.5f);
            path.lineTo(7f, 9.5f);
            path.lineTo(11f, 5.5f);
            path.lineTo(11f, 18.5f);
            path.lineTo(7f, 14.5f);
            path.lineTo(3f, 14.5f);
            path.close();
            pending.drawPath(path, solid);
        }

        private void plus(float cx, float cy, float r) {
            if (r <= 0f) {
                line(cx - 3.4f, cy, cx + 3.4f, cy);
                return;
            }
            line(cx - 3.4f, cy, cx + 3.4f, cy);
            line(cx, cy - 3.4f, cx, cy + 3.4f);
        }

        private void chevron(float rotate, boolean down) {
            path.reset();
            path.moveTo(5f, down ? 10f : 14f);
            path.lineTo(12f, down ? 16f : 8f);
            path.lineTo(19f, down ? 10f : 14f);
            if (rotate != 0f) {
                android.graphics.Matrix m = new android.graphics.Matrix();
                m.setRotate(rotate, 12f, 12f);
                path.transform(m);
            }
            pending.drawPath(path, stroke);
        }

        private void home() {
            path.reset();
            path.moveTo(3.5f, 11.5f);
            path.lineTo(12f, 4f);
            path.lineTo(20.5f, 11.5f);
            pending.drawPath(path, stroke);
            path.reset();
            path.moveTo(6f, 10.5f);
            path.lineTo(6f, 20f);
            path.lineTo(18f, 20f);
            path.lineTo(18f, 10.5f);
            pending.drawPath(path, stroke);
        }

        private void thermometer(boolean up) {
            ring(9.5f, 3f, 14.5f, 15f, 2.5f, false);
            circle(12f, 17.4f, 3.4f, true);
            line(12f, 8f, 12f, 14f);
            line(15.5f, 6f, 17.5f, 6f);
            line(15.5f, 9.5f, 17.5f, 9.5f);
            arrow(up ? 19.5f : 19.5f, up ? 17f : 19f, 19.5f, up ? 12.5f : 23.5f);
        }

        private void mode() {
            path.reset();
            rect.set(4f, 4f, 20f, 20f);
            path.addArc(rect, -40f, 250f);
            pending.drawPath(path, stroke);
            tri(17.5f, 2.5f, 21.5f, 6.5f, 16f, 7.5f);
        }

        private void fan() {
            for (int i = 0; i < 3; i++) {
                path.reset();
                android.graphics.Matrix m = new android.graphics.Matrix();
                m.setRotate(i * 120f, 12f, 12f);
                path.moveTo(12f, 12f);
                path.cubicTo(14.5f, 9f, 18.5f, 8f, 19.5f, 10.5f);
                path.cubicTo(20.5f, 13f, 15.5f, 13.5f, 12f, 12f);
                path.transform(m);
                pending.drawPath(path, stroke);
            }
            circle(12f, 12f, 1.6f, true);
        }

        private void swing() {
            path.reset();
            rect.set(3f, 8f, 21f, 24f);
            path.addArc(rect, 200f, 140f);
            pending.drawPath(path, stroke);
            arrow(4f, 15f, 20f, 15f);
        }

        private void light() {
            path.reset();
            rect.set(6.5f, 2.5f, 17.5f, 14f);
            path.addOval(rect, Path.Direction.CW);
            pending.drawPath(path, stroke);
            line(9f, 16.5f, 15f, 16.5f);
            line(9.5f, 19.5f, 14.5f, 19.5f);
        }

        private void clock() {
            circle(12f, 12f, 8f, false);
            line(12f, 7f, 12f, 12f);
            line(12f, 12f, 16f, 14f);
        }

        private void moon() {
            path.reset();
            path.addArc(2f, 2f, 22f, 22f, 45f, 270f);
            pending.drawPath(path, stroke);
        }

        private void sun() {
            circle(12f, 12f, 4.6f, false);
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * i / 4;
                line((float) (12 + 7.6 * Math.cos(a)), (float) (12 + 7.6 * Math.sin(a)),
                        (float) (12 + 10 * Math.cos(a)), (float) (12 + 10 * Math.sin(a)));
            }
        }

        private void magnifier() {
            circle(10.5f, 10.5f, 6.5f, false);
            line(15.5f, 15.5f, 20f, 20f);
        }

        private void star() {
            path.reset();
            for (int i = 0; i < 10; i++) {
                double a = -Math.PI / 2 + i * Math.PI / 5;
                double r = i % 2 == 0 ? 8.5 : 3.8;
                float x = (float) (12 + r * Math.cos(a));
                float y = (float) (12 + r * Math.sin(a));
                if (i == 0) path.moveTo(x, y);
                else path.lineTo(x, y);
            }
            path.close();
            pending.drawPath(path, stroke);
        }

        private void globe() {
            circle(12f, 12f, 8.5f, false);
            path.reset();
            rect.set(6.5f, 3.5f, 17.5f, 20.5f);
            path.addOval(rect, Path.Direction.CW);
            pending.drawPath(path, stroke);
            line(3.5f, 12f, 20.5f, 12f);
        }

        private void dot(int color) {
            solid.setColor(color);
            circle(12f, 12f, 6.5f, true);
            solid.setColor(type.getColor());
        }

        @Override
        public void setAlpha(int alpha) {
            stroke.setAlpha(alpha);
            solid.setAlpha(alpha);
            type.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(android.graphics.ColorFilter colorFilter) {
            stroke.setColorFilter(colorFilter);
            solid.setColorFilter(colorFilter);
            type.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
