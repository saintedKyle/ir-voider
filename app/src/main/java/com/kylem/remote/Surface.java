package com.kylem.remote;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewOutlineProvider;

/**
 * Shaded warm-dark surfaces. Light is assumed to fall from above, so a plate gets a lit top rim,
 * a body gradient and a contact shade at the bottom instead of one flat fill.
 */
final class Surface {

    private Surface() {
    }

    static float dp(Context c, float v) {
        return v * c.getResources().getDisplayMetrics().density;
    }

    static int lighten(int argb, float t) {
        return blend(argb, 0xFFFFFFFF, t);
    }

    static int darken(int argb, float t) {
        return blend(argb, 0xFF000000, t);
    }

    /** Tint one colour into another, for accents bleeding into warm neutrals. */
    static int blend(int from, int to, float t) {
        int r = (int) (Color.red(from) + (Color.red(to) - Color.red(from)) * t + 0.5f);
        int g = (int) (Color.green(from) + (Color.green(to) - Color.green(from)) * t + 0.5f);
        int b = (int) (Color.blue(from) + (Color.blue(to) - Color.blue(from)) * t + 0.5f);
        return (from & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    /** Raised plate from one flat hue: lit face, base, shaded underside. */
    static LayerDrawable raised(Context c, float radiusDp, int base, int rim, int gloss) {
        return plate(c, radiusDp, lift(base), rim, gloss);
    }

    /**
     * Top-lit stops for shapes that need their own corner geometry. The lift scales with how bright
     * the fill already is: a flat 18% wash turns a near-black key mid-grey, but reads as glass on
     * ember. Light surfaces get a gentler underside so they never go muddy.
     */
    static int[] lift(int base) {
        float l = Palette.luminance(base);
        return new int[]{lighten(base, 0.06f + 0.16f * l), base,
                darken(base, l > 0.7f ? 0.34f - (l - 0.7f) : 0.34f)};
    }

    /** Sunken track: dark at the top lip, light along the bottom edge. */
    static GradientDrawable recessed(Context c, float radiusDp, int base, int rim) {
        GradientDrawable body = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{darken(base, 0.35f), base, lighten(base, 0.09f)});
        body.setCornerRadius(dp(c, radiusDp));
        body.setStroke(Math.max(1, (int) dp(c, 1)), rim);
        return body;
    }

    private static LayerDrawable plate(Context c, float radiusDp, int[] stops, int rim, int gloss) {
        float radius = dp(c, radiusDp);
        int edge = Math.max(1, (int) dp(c, 1));

        GradientDrawable body = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, stops);
        body.setCornerRadius(radius);
        body.setStroke(edge, rim);

        GradientDrawable topLight = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{gloss, 0x00000000, 0x00000000});
        topLight.setCornerRadius(radius - edge);

        GradientDrawable bottomShade = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{0x2E000000, 0x00000000, 0x00000000});
        bottomShade.setCornerRadius(radius - edge);

        LayerDrawable plate = new LayerDrawable(new Drawable[]{body, topLight, bottomShade});
        plate.setLayerInset(1, edge, edge, edge, 0);
        plate.setLayerInset(2, edge, 0, edge, edge);
        return plate;
    }

    /** Shadow cut to the rounded silhouette; the default outline follows the layer stack. */
    static void elevate(View v, float radiusDp, float elevationDp) {
        final float radius = dp(v.getContext(), radiusDp);
        v.setElevation(dp(v.getContext(), elevationDp));
        v.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
            }
        });
    }

    static void sink(View v, boolean pressed, float elevationDp) {
        float raised = dp(v.getContext(), elevationDp);
        v.setElevation(pressed ? raised * 0.2f : raised);
        v.animate()
                .scaleX(pressed ? 0.965f : 1f)
                .scaleY(pressed ? 0.965f : 1f)
                .setDuration(pressed ? 90 : 160)
                .start();
    }

    @SuppressLint("ClickableViewAccessibility")
    static void sinkOnPress(View v, float elevationDp) {
        v.setOnTouchListener((view, event) -> {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                sink(view, true, elevationDp);
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                sink(view, false, elevationDp);
            }
            return false;
        });
    }
}
