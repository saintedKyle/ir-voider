/*
 * IR Voider — Android IR remote.
 * Copyright (C) 2026 poging kyle
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.kylem.remote;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.widget.Button;

/**
 * One key on a controls page: a frosted-glass plate carrying a vector glyph and the key's name.
 *
 * <p>Labels sit beside the glyph, not under it, so a page reads as a list of functions with real
 * icons instead of grey rectangles with words in them. The class stays a Button so the existing
 * hold-to-repeat and key-map code keeps working unchanged.
 */
final class KeyTile extends Button {

    /** Label-only plate for digits and letters, where a glyph would just repeat the text. */
    KeyTile(Context c, Palette p, int accent, String label, float labelSize) {
        this(c, p, accent, null, label, labelSize, false, false);
        setGravity(Gravity.CENTER);
    }

    KeyTile(Context c, Palette p, int accent, String key, String label, float labelSize,
            boolean iconOnly, boolean hero) {
        super(c);
        setAllCaps(false);
        setText(iconOnly ? "" : label);
        setTextSize(labelSize);
        setTextColor(p.text);
        setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        setStateListAnimator(null);
        setIncludeFontPadding(false);
        setGravity(iconOnly ? Gravity.CENTER : (Gravity.CENTER_VERTICAL | Gravity.START));
        setSingleLine(true);
        setEllipsize(android.text.TextUtils.TruncateAt.END);
        setMinWidth(0);
        setMinimumWidth(0);
        setMinHeight(0);
        setMinimumHeight(0);

        if (key != null) {
            int glyph = iconOnly || hero ? accent : Surface.blend(p.text, accent, 0.55f);
            Drawable icon = KeyIcon.of(c, key, glyph, iconOnly ? 30 : (hero ? 26 : 22));
            if (iconOnly) {
                setCompoundDrawablesRelativeWithIntrinsicBounds(null, icon, null, null);
            } else {
                setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, null, null);
                setCompoundDrawablePadding((int) Surface.dp(c, 12));
            }
        }
        int side = (int) Surface.dp(c, 16);
        setPadding(side, 0, side, 0);

        Drawable plate = Glass.plate(c, p, accent, hero);
        setBackground(new RippleDrawable(ColorStateList.valueOf(
                hero ? Surface.blend(p.ripple(), accent, 0.35f) : p.ripple()), plate, null));
        Surface.elevate(this, hero ? 24 : 21, hero ? 6 : 3);
        Surface.sinkOnPress(this, hero ? 6 : 3);
    }
}
