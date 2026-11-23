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
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

/**
 * Coloured shortcut badges: a service mark on its own brand field, the way the app appears on a
 * TV launcher, instead of a monochrome outline.
 *
 * <p>Every mark is drawn here as geometry and type — no vendor artwork is bundled. They are
 * recognisable at 26 dp by field colour and silhouette, which is what the eye uses at that size
 * anyway, and they carry the service's own palette rather than the theme accent.
 */
final class BrandMark extends Drawable {

    private static final int WHITE = 0xFFFFFFFF;

    private final String key;
    private final int accent;
    private final int px;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint type = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rect = new RectF();

    private Canvas cv;

    private BrandMark(String key, int accent, int px) {
        this.key = key == null ? "" : key;
        this.accent = accent;
        this.px = px;
        fill.setStyle(Paint.Style.FILL);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        type.setTextAlign(Paint.Align.CENTER);
        type.setTypeface(Typeface.create("sans-serif-black", Typeface.NORMAL));
    }

    static Drawable of(Context c, String key, int accent, int sizeDp) {
        int px = Math.max(16, (int) Surface.dp(c, sizeDp));
        return new BrandMark(key, accent, px);
    }

    @Override
    public int getIntrinsicWidth() {
        return px;
    }

    @Override
    public int getIntrinsicHeight() {
        return px;
    }

    @Override
    public void draw(Canvas canvas) {
        float s = getBounds().width() / 24f;
        canvas.save();
        canvas.translate(getBounds().left, getBounds().top);
        canvas.scale(s, s);
        cv = canvas;
        drawMark();
        canvas.restore();
    }

    // --------------------------------------------------------------- fields

    /** Rounded-square brand field with a vertical gradient, plus an inner hairline. */
    private void field(int top, int bottom) {
        rect.set(0.6f, 0.6f, 23.4f, 23.4f);
        path.reset();
        path.addRoundRect(rect, 5.8f, 5.8f, Path.Direction.CW);
        fill.setShader(new LinearGradient(0, rect.top, 0, rect.bottom, top, bottom,
                Shader.TileMode.CLAMP));
        cv.drawPath(path, fill);
        fill.setShader(null);
        stroke.setStrokeWidth(0.7f);
        stroke.setColor(0x26FFFFFF);
        rect.set(1.4f, 1.4f, 22.6f, 22.6f);
        path.reset();
        path.addRoundRect(rect, 5.2f, 5.2f, Path.Direction.CW);
        cv.drawPath(path, stroke);
    }

    private void field(int flat) {
        field(flat, Surface.darken(flat, 0.22f));
    }

    private void line(float x1, float y1, float x2, float y2, int color, float width) {
        stroke.setColor(color);
        stroke.setStrokeWidth(width);
        path.reset();
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);
        cv.drawPath(path, stroke);
    }

    private void solid(float x1, float y1, float x2, float y2, float rad, int color) {
        rect.set(x1, y1, x2, y2);
        path.reset();
        path.addRoundRect(rect, rad, rad, Path.Direction.CW);
        fill.setColor(color);
        fill.setShader(null);
        cv.drawPath(path, fill);
    }

    private void text(String value, float size, float cx, float baseline, int color,
                      float spacing) {
        type.setColor(color);
        type.setTextSize(size);
        type.setLetterSpacing(spacing);
        cv.drawText(value, cx, baseline, type);
    }

    // ----------------------------------------------------------------- marks

    private void drawMark() {
        switch (key) {
            case "netflix":
                netflix();
                break;
            case "youtube":
                youtube();
                break;
            case "prime":
                prime();
                break;
            case "disney":
                disney();
                break;
            case "hbo":
                max();
                break;
            case "spotify":
                spotify();
                break;
            case "apps":
                appGrid();
                break;
            case "browser":
            case "web":
            case "copilot":
                globe();
                break;
            default:
                appGrid();
        }
    }

    /** Black field, red N built from two stems and the full-width diagonal. */
    private void netflix() {
        field(0xFF15151B, 0xFF08080C);
        int red = 0xFFE50914;
        solid(6.2f, 4.6f, 9.2f, 19.6f, 0.5f, red);
        solid(14.8f, 4.6f, 17.8f, 19.6f, 0.5f, red);
        line(7.7f, 4.6f, 16.3f, 19.6f, red, 2.9f);
    }

    /** Red field with the white play wedge. */
    private void youtube() {
        field(0xFFFF0033, 0xFFC3001F);
        path.reset();
        path.moveTo(9.4f, 7.8f);
        path.lineTo(17.0f, 12f);
        path.lineTo(9.4f, 16.2f);
        path.close();
        fill.setShader(null);
        fill.setColor(WHITE);
        cv.drawPath(path, fill);
    }

    /** Cyan field, white "p" stem and the smile sweeping underneath. */
    private void prime() {
        field(0xFF00A8E1, 0xFF0B2A6B);
        stroke.setColor(WHITE);
        stroke.setStrokeWidth(2.0f);
        rect.set(6.0f, 5.4f, 13.4f, 12.8f);
        path.reset();
        path.addOval(rect, Path.Direction.CW);
        cv.drawPath(path, stroke);
        line(6.0f, 9.1f, 6.0f, 20.2f, WHITE, 2.0f);
        path.reset();
        path.moveTo(6.2f, 16.0f);
        path.quadTo(13.4f, 20.4f, 20.6f, 13.4f);
        path.lineTo(19.2f, 12.2f);
        path.quadTo(13.2f, 18.0f, 6.2f, 14.2f);
        path.close();
        fill.setShader(null);
        fill.setColor(WHITE);
        cv.drawPath(path, fill);
    }

    /** Deep royal field with the D+ wordmark. */
    private void disney() {
        field(0xFF07245C, 0xFF0C1B4D);
        text("D+", 10.2f, 12f, 15.9f, WHITE, -0.02f);
    }

    /** Blue-to-violet field with the MAX wordmark. */
    private void max() {
        field(0xFF0026E0, 0xFF8A1BE0);
        text("MAX", 6.4f, 12f, 14.4f, WHITE, 0.04f);
    }

    /** Green disc, three black arcs opening downward. */
    private void spotify() {
        rect.set(0.8f, 0.8f, 23.2f, 23.2f);
        path.reset();
        path.addOval(rect, Path.Direction.CW);
        fill.setColor(0xFF1DB954);
        fill.setShader(null);
        cv.drawPath(path, fill);

        int ink = 0xFF0B1410;
        float[][] arcs = {{15.4f, 1.55f}, {15.1f, 1.25f}, {14.8f, 1.05f}};
        float[] radii = {7.4f, 5.5f, 3.7f};
        for (int i = 0; i < radii.length; i++) {
            float r = radii[i];
            rect.set(12f - r, arcs[i][0] - r, 12f + r, arcs[i][0] + r);
            path.reset();
            path.addArc(rect, 202f, 136f);
            stroke.setColor(ink);
            stroke.setStrokeWidth(arcs[i][1]);
            cv.drawPath(path, stroke);
        }
    }

    /** Four app tiles in the theme accent, so the launcher key still fits the palette. */
    private void appGrid() {
        field(0xFF1A1C22, 0xFF0D0F14);
        int a = accent;
        int b = Surface.lighten(accent, 0.22f);
        solid(3.4f, 3.4f, 10.6f, 10.6f, 2.0f, a);
        solid(13.4f, 3.4f, 20.6f, 10.6f, 2.0f, b);
        solid(3.4f, 13.4f, 10.6f, 20.6f, 2.0f, b);
        solid(13.4f, 13.4f, 20.6f, 20.6f, 2.0f, a);
    }

    /** Blue field with a white globe for the browser shortcut. */
    private void globe() {
        field(0xFF2F6FEC, 0xFF15307E);
        stroke.setColor(WHITE);
        stroke.setStrokeWidth(1.7f);
        rect.set(4.2f, 4.2f, 19.8f, 19.8f);
        path.reset();
        path.addOval(rect, Path.Direction.CW);
        cv.drawPath(path, stroke);
        rect.set(9.0f, 4.2f, 15.0f, 19.8f);
        path.reset();
        path.addOval(rect, Path.Direction.CW);
        cv.drawPath(path, stroke);
        line(4.2f, 12f, 19.8f, 12f, WHITE, 1.7f);
    }

    @Override
    public void setAlpha(int alpha) {
        fill.setAlpha(alpha);
        stroke.setAlpha(alpha);
        type.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(android.graphics.ColorFilter colorFilter) {
        fill.setColorFilter(colorFilter);
        stroke.setColorFilter(colorFilter);
        type.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
