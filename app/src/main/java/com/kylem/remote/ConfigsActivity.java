/*
 * IR Voider — Android IR remote.
 * Copyright (C) 2026 poging kyle
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.kylem.remote;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.UUID;

public class ConfigsActivity extends Activity {

    private static final int BG = 0xFF0A0908;
    private static final int CARD = 0xFF14110F;
    private static final int BTN = 0xFF1A1715;
    private static final int STROKE = 0xFF2C2824;
    private static final int ACCENT = 0xFFE8753D;
    private static final int TEXT = 0xFFE9E0D4;
    private static final int MUTED = 0xFF8B8176;
    private static final int DARK = 0xFF0A0908;

    private static final String[] TYPES = {
            "TV", "Aircon", "LED", "Projector", "Fan", "Soundbar", "Other"
    };

    private EditText nameInput;
    private Spinner typeSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
        buildUi();
    }

    private int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(value);
        tv.setTextSize(size);
        tv.setTextColor(color);
        if (bold) {
            tv.setTypeface(size >= 16
                    ? Typeface.create("sans-serif-black", Typeface.NORMAL)
                    : Typeface.create("sans-serif-medium", Typeface.BOLD));
        }
        return tv;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16), dp(16), dp(16), dp(16));
        c.setBackground(Surface.raised(this, 28, CARD, STROKE, 0x18FFFFFF));
        Surface.elevate(c, 28, 5);
        return c;
    }

    private void addCard(LinearLayout root, View card) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(14);
        root.addView(card, lp);
    }

    private Button button(String label, int color, int textColor) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setTextColor(textColor);
        b.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        b.setStateListAnimator(null);
        boolean hot = color == ACCENT;
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x22E9E0D4),
                Surface.raised(this, 20, color,
                        hot ? Surface.darken(ACCENT, 0.42f) : Surface.lighten(STROKE, 0.22f),
                        hot ? 0x3CFFFFFF : 0x24FFFFFF), null));
        Surface.elevate(b, 20, hot ? 5 : 2);
        Surface.sinkOnPress(b, hot ? 5 : 2);
        return b;
    }

    private LinearLayout row(String label, View view) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(0, dp(4), 0, dp(4));
        TextView tv = text(label, 14, MUTED, false);
        tv.setWidth(dp(96));
        r.addView(tv);
        r.addView(view, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return r;
    }

    private GradientDrawable appBackground() {
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{BG, 0xFF120E0B});
        bg.setShape(GradientDrawable.RECTANGLE);
        return bg;
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackground(appBackground());
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(24), dp(16), dp(28));
        scroll.addView(root);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("IR Voider", 24, TEXT, true);
        top.addView(title, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button done = button("Done", ACCENT, DARK);
        done.setOnClickListener(v -> finish());
        top.addView(done, new LinearLayout.LayoutParams(dp(84), dp(42)));
        root.addView(top);
        TextView sub = text("Devices & settings — name each remote, switch devices, tune IR.",
                12, MUTED, false);
        sub.setPadding(0, dp(4), 0, dp(14));
        root.addView(sub);

        // Saved devices
        LinearLayout devicesCard = card();
        devicesCard.addView(text("YOUR DEVICES", 11, ACCENT, true));
        List<DeviceStore.Device> devices = DeviceStore.devices(this);
        if (devices.isEmpty()) {
            TextView empty = text("No saved devices yet. Pick a working remote, "
                    + "then add it below.", 13, MUTED, false);
            empty.setPadding(0, dp(8), 0, 0);
            devicesCard.addView(empty);
        } else {
            for (DeviceStore.Device d : devices) {
                devicesCard.addView(deviceRow(d));
            }
        }
        addCard(root, devicesCard);

        // Add current
        LinearLayout addCard = card();
        addCard.addView(text("ADD CURRENT REMOTE", 11, ACCENT, true));
        String brand = DeviceStore.selectedBrand(this);
        String remote = DeviceStore.selectedRemote(this);
        String currentLabel = brand == null || remote == null
                ? "No remote selected yet"
                : brand + " \u2014 " + remote.replace('_', ' ');
        TextView cur = text("Current: " + currentLabel, 13, TEXT, false);
        cur.setPadding(0, dp(6), 0, dp(10));
        addCard.addView(cur);

        nameInput = new EditText(this);
        nameInput.setHint("Name, e.g. Living Room TV");
        nameInput.setHintTextColor(MUTED);
        nameInput.setTextColor(TEXT);
        nameInput.setInputType(InputType.TYPE_CLASS_TEXT);
        nameInput.setBackgroundTintList(ColorStateList.valueOf(ACCENT));
        addCard.addView(nameInput, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));

        typeSpinner = new Spinner(this);
        ArrayAdapter<String> ta = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, TYPES);
        ta.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(ta);
        addCard.addView(row("Type", typeSpinner));

        Button add = button("Save as device", ACCENT, DARK);
        add.setOnClickListener(v -> addDevice());
        LinearLayout.LayoutParams addLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(50));
        addLp.topMargin = dp(8);
        addCard.addView(add, addLp);
        addCard.addView(text(
                "Type is a label. Aircon/LED still use TV-style codes; "
                        + "full aircon state frames are not included.",
                11, MUTED, false));
        addCard(root, addCard);

        // Settings
        LinearLayout settings = card();
        settings.addView(text("IR & SCAN", 11, ACCENT, true));

        Spinner boost = new Spinner(this);
        String[] boostLabels = {"Standard \u2014 1 repeat", "Strong \u2014 2 repeats",
                "Max \u2014 3 repeats"};
        ArrayAdapter<String> ba = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, boostLabels);
        ba.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        boost.setAdapter(ba);
        boost.setSelection(DeviceStore.repeats(this) - 1);
        boost.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view,
                                       int position, long id) {
                DeviceStore.setRepeats(ConfigsActivity.this, position + 1);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
        settings.addView(row("IR boost", boost));
        settings.addView(text(
                "Repeats volume/channel presses to improve reliability. Power, OK "
                        + "and navigation are always sent once, so they cannot "
                        + "double-fire. Software cannot increase the LED's physical "
                        + "power or range.",
                11, MUTED, false));


        CheckBox irOverride = new CheckBox(this);
        irOverride.setText("Try IR anyway (phone reports NO IR)");
        irOverride.setTextColor(TEXT);
        irOverride.setButtonTintList(ColorStateList.valueOf(ACCENT));
        irOverride.setChecked(DeviceStore.irOverride(this));
        irOverride.setOnCheckedChangeListener((buttonView, isChecked) ->
                DeviceStore.setIrOverride(ConfigsActivity.this, isChecked));
        settings.addView(irOverride);
        settings.addView(text(
                "Only for phones that have an IR blaster but Android reports NO IR. "
                        + "It cannot create IR hardware; if the phone has no emitter, nothing happens.",
                11, MUTED, false));

        CheckBox forceMode = new CheckBox(this);
        forceMode.setText("Force mode (3x repeats, 25 ms gaps)");
        forceMode.setTextColor(TEXT);
        forceMode.setButtonTintList(ColorStateList.valueOf(ACCENT));
        forceMode.setChecked(DeviceStore.forceMode(this));
        forceMode.setOnCheckedChangeListener((buttonView, isChecked) ->
                DeviceStore.setForceMode(ConfigsActivity.this, isChecked));
        settings.addView(forceMode);
        settings.addView(text(
                "Force repeats frames harder and faster. It does not amplify the "
                        + "phone's IR LED; range is still limited by hardware.",
                11, MUTED, false));

        CheckBox boostAll = new CheckBox(this);
        boostAll.setText("Boost all buttons (except POWER)");
        boostAll.setTextColor(TEXT);
        boostAll.setButtonTintList(ColorStateList.valueOf(ACCENT));
        boostAll.setChecked(DeviceStore.boostAll(this));
        boostAll.setOnCheckedChangeListener((buttonView, isChecked) ->
                DeviceStore.setBoostAll(ConfigsActivity.this, isChecked));
        LinearLayout.LayoutParams boostLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        boostLp.topMargin = dp(6);
        settings.addView(boostAll, boostLp);
        settings.addView(text(
                "Repeats every button except POWER. This can double-trigger "
                        + "toggles or OK on sensitive devices; turn it off if that happens.",
                11, MUTED, false));
        CheckBox acRepeat = new CheckBox(this);
        acRepeat.setText("Repeat aircon POWER (try 2x)");
        acRepeat.setTextColor(TEXT);
        acRepeat.setButtonTintList(ColorStateList.valueOf(ACCENT));
        acRepeat.setChecked(DeviceStore.acPowerRepeat(this));
        acRepeat.setOnCheckedChangeListener((buttonView, isChecked) ->
                DeviceStore.setAcPowerRepeat(ConfigsActivity.this, isChecked));
        LinearLayout.LayoutParams acLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        acLp.topMargin = dp(4);
        settings.addView(acRepeat, acLp);
        settings.addView(text(
                "Some air conditioners ignore a single frame. This repeats POWER "
                        + "using the IR boost count; it may toggle twice on other ACs.",
                11, MUTED, false));
        addCard(root, settings);

        // Learn mode honesty card
        LinearLayout learn = card();
        learn.addView(text("LEARN FROM ORIGINAL REMOTE", 11, ACCENT, true));
        learn.addView(text(
                "This phone's IR hardware only transmits. It has no IR receiver, "
                        + "so the app cannot capture codes from your original remote.\n\n"
                        + "To make a digital copy you need a small IR receiver "
                        + "(e.g. TSOP38238 + ESP32). Capture the code there, then "
                        + "add it to this database. For most TVs the built-in "
                        + "Flipper-IRDB codes and the scanner already cover it.",
                13, MUTED, false));
        addCard(root, learn);

        TextView credit = text(
                "Codes: Flipper-IRDB (CC0). No internet permission.", 11,
                MUTED, false);
        credit.setPadding(dp(4), dp(4), 0, 0);
        root.addView(credit);

        TextView footer = text("made by poging kyle", 12, MUTED, false);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(26), 0, dp(6));
        root.addView(footer);

        scroll.setOnApplyWindowInsetsListener((v, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                Insets bars = insets.getInsets(WindowInsets.Type.systemBars()
                        | WindowInsets.Type.displayCutout());
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            } else {
                v.setPadding(insets.getSystemWindowInsetLeft(),
                        insets.getSystemWindowInsetTop(),
                        insets.getSystemWindowInsetRight(),
                        insets.getSystemWindowInsetBottom());
            }
            return insets;
        });
        scroll.requestApplyInsets();

        setContentView(scroll);
    }

    private View deviceRow(DeviceStore.Device d) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, dp(10), 0, dp(10));

        TextView name = text(d.name, 16, TEXT, true);
        box.addView(name);
        TextView meta = text(d.type + " \u2022 " + d.brand + " \u2014 "
                + d.remote.replace('_', ' '), 12, MUTED, false);
        box.addView(meta);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(8), 0, 0);

        Button open = button("Open", BTN, TEXT);
        open.setOnClickListener(v -> {
            DeviceStore.rememberSelection(this, d.category, d.brand, d.remote, d.hash);
            Toast.makeText(this, "Opened " + d.name, Toast.LENGTH_SHORT).show();
            finish();
        });
        Button rename = button("Rename", BTN, TEXT);
        rename.setOnClickListener(v -> renameDevice(d));
        Button delete = button("Delete", 0xFF2B1411, 0xFFE06B55);
        delete.setOnClickListener(v -> deleteDevice(d));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(42), 1f);
        lp.setMargins(0, 0, dp(6), 0);
        actions.addView(open, lp);
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, dp(42), 1f);
        lp2.setMargins(dp(3), 0, dp(3), 0);
        actions.addView(rename, lp2);
        LinearLayout.LayoutParams lp3 = new LinearLayout.LayoutParams(0, dp(42), 1f);
        lp3.setMargins(dp(6), 0, 0, 0);
        actions.addView(delete, lp3);
        box.addView(actions);
        return box;
    }

    private void addDevice() {
        String brand = DeviceStore.selectedBrand(this);
        String remote = DeviceStore.selectedRemote(this);
        if (brand == null || remote == null) {
            Toast.makeText(this, "Pick a remote on the main screen first.",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        String name = nameInput.getText().toString().trim();
        if (name.isEmpty()) name = brand + " " + remote.replace('_', ' ');

        DeviceStore.Device d = new DeviceStore.Device();
        d.id = UUID.randomUUID().toString();
        d.name = name;
        d.type = TYPES[typeSpinner.getSelectedItemPosition()];
        d.category = DeviceStore.selectedCategory(this);
        d.brand = brand;
        d.remote = remote;
        d.hash = DeviceStore.selectedHash(this);
        DeviceStore.add(this, d);
        Toast.makeText(this, "Saved \u201c" + name + "\u201d", Toast.LENGTH_SHORT).show();
        recreate();
    }

    private void renameDevice(DeviceStore.Device d) {
        EditText input = new EditText(this);
        input.setText(d.name);
        input.setSelection(d.name.length());
        input.setTextColor(TEXT);
        input.setHintTextColor(MUTED);
        new AlertDialog.Builder(this)
                .setTitle("Rename device")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) return;
                    List<DeviceStore.Device> list = DeviceStore.devices(this);
                    for (DeviceStore.Device x : list) {
                        if (x.id.equals(d.id)) x.name = name;
                    }
                    DeviceStore.saveDevices(this, list);
                    recreate();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteDevice(DeviceStore.Device d) {
        new AlertDialog.Builder(this)
                .setTitle("Delete \u201c" + d.name + "\u201d?")
                .setMessage("The remote itself is not deleted; only this saved entry.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    DeviceStore.remove(this, d.id);
                    recreate();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
