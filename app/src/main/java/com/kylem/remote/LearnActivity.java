/*
 * IR Voider — Android IR remote.
 * Copyright (C) 2026 poging kyle
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.kylem.remote;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LearnActivity extends Activity {

    private static final int PICK_FILE = 101;

    private static final int BG = 0xFF060A12;
    private static final int CARD = 0xFF121A2B;
    private static final int BTN = 0xFF1B2740;
    private static final int STROKE = 0xFF26344F;
    private static final int ACCENT = 0xFF22D3EE;
    private static final int TEXT = 0xFFF8FAFC;
    private static final int MUTED = 0xFF94A3B8;
    private static final int DARK = 0xFF04141B;

    private static final String[] BUTTON_ORDER = {
            "power", "power_off", "vol_up", "vol_down", "mute", "ch_up", "ch_down",
            "input", "exit", "info", "guide", "up", "down", "left", "right", "ok",
            "back", "home", "menu", "play_pause", "stop", "rewind", "forward",
            "prev", "next", "record", "subtitle", "audio", "aspect", "last", "fav",
            "apps", "netflix", "temp_up", "temp_down", "mode", "fan", "swing", "speed",
            "light", "timer", "sleep", "brightness_up", "brightness_down", "color",
            "red", "green", "yellow", "blue", "zoom_in", "zoom_out",
            "num_0", "num_1", "num_2", "num_3", "num_4", "num_5", "num_6", "num_7",
            "num_8", "num_9", "dash", "dot"
    };

    private static final Map<String, String> ALIASES = new LinkedHashMap<>();

    static {
        String[][] pairs = {
                {"power", "power"}, {"power_toggle", "power"}, {"power_on", "power"},
                {"on", "power"}, {"turn_on", "power"}, {"off", "power_off"},
                {"power_off", "power_off"}, {"turn_off", "power_off"},
                {"vol_up", "vol_up"}, {"volume_up", "vol_up"}, {"vol_+", "vol_up"},
                {"vol_dn", "vol_down"}, {"vol_down", "vol_down"},
                {"volume_down", "vol_down"}, {"vol_-", "vol_down"},
                {"mute", "mute"}, {"ch_next", "ch_up"}, {"ch_up", "ch_up"},
                {"ch_prev", "ch_down"}, {"ch_down", "ch_down"},
                {"input", "input"}, {"source", "input"},
                {"up", "up"}, {"down", "down"}, {"left", "left"}, {"right", "right"},
                {"ok", "ok"}, {"enter", "ok"}, {"select", "ok"},
                {"back", "back"}, {"return", "back"}, {"exit", "exit"},
                {"home", "home"}, {"menu", "menu"}, {"settings", "menu"},
                {"info", "info"}, {"guide", "guide"}, {"epg", "guide"},
                {"play", "play_pause"}, {"pause", "play_pause"}, {"play_pause", "play_pause"},
                {"stop", "stop"}, {"rewind", "rewind"}, {"fast_forward", "forward"},
                {"forward", "forward"}, {"prev", "prev"}, {"next", "next"},
                {"record", "record"}, {"subtitle", "subtitle"}, {"audio", "audio"},
                {"aspect", "aspect"}, {"last", "last"}, {"fav", "fav"},
                {"apps", "apps"}, {"netflix", "netflix"},
                {"temp_up", "temp_up"}, {"temp_plus", "temp_up"}, {"temp+", "temp_up"},
                {"temp_down", "temp_down"}, {"temp_minus", "temp_down"},
                {"temp-", "temp_down"}, {"mode", "mode"}, {"fan", "fan"},
                {"speed", "speed"}, {"fan_speed", "speed"},
                {"swing", "swing"}, {"swing_v", "swing"}, {"swing_h", "swing"},
                {"light", "light"}, {"lamp", "light"}, {"timer", "timer"},
                {"sleep", "sleep"}, {"brightness_up", "brightness_up"},
                {"brightness_down", "brightness_down"}, {"color", "color"},
                {"red", "red"}, {"green", "green"}, {"yellow", "yellow"}, {"blue", "blue"},
                {"zoom_in", "zoom_in"}, {"zoom_out", "zoom_out"},
                {"dash", "dash"}, {"dot", "dot"}
        };
        for (String[] p : pairs) ALIASES.put(p[0], p[1]);
        for (int d = 0; d < 10; d++) {
            ALIASES.put(String.valueOf(d), "num_" + d);
            ALIASES.put("num_" + d, "num_" + d);
        }
    }

    private EditText nameInput;
    private String category;

    static class Entry {
        String name;
        String type;
        int frequency = 38000;
        final List<Integer> data = new ArrayList<>();
    }

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
        category = DeviceStore.selectedCategory(this);
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
        tv.setLineSpacing(0f, 1.1f);
        return tv;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16), dp(16), dp(16), dp(16));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD);
        bg.setCornerRadius(dp(24));
        bg.setStroke(dp(1), STROKE);
        c.setBackground(bg);
        c.setElevation(dp(3));
        return c;
    }

    private Button button(String label, int color, int textColor) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(15);
        b.setTextColor(textColor);
        b.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        b.setStateListAnimator(null);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(16));
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), bg, null));
        return b;
    }

    private GradientDrawable appBackground() {
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF05070D, 0xFF0B1220, 0xFF111C31});
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
        top.addView(text("IR Voider", 23, TEXT, true), new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button done = button("Done", ACCENT, 0xFF06263A);
        done.setOnClickListener(v -> finish());
        top.addView(done, new LinearLayout.LayoutParams(dp(84), dp(42)));
        root.addView(top);
        TextView sub = text("Fetch remote — import a captured .ir file", 12, MUTED, false);
        sub.setPadding(0, dp(2), 0, dp(14));
        root.addView(sub);

        LinearLayout info = card();
        info.addView(text("HOW THIS WORKS", 11, ACCENT, true));
        info.addView(text(
                "This phone's IR hardware only transmits \u2014 it cannot receive, "
                        + "so it cannot capture your original remote directly.\n\n"
                        + "You can import a capture made with an IR receiver "
                        + "(TSOP38238 + ESP32/Arduino) or a Flipper Zero. The app "
                        + "accepts Flipper .ir files that contain raw timings.\n\n"
                        + "TVs, ACs and most common devices are already covered by "
                        + "the built-in database and the scanner.",
                13, MUTED, false));
        addCard(root, info);

        LinearLayout importCard = card();
        importCard.addView(text("IMPORT .IR FILE", 11, ACCENT, true));
        importCard.addView(text("It will be added as a \u201cLearned\u201d remote "
                + "in the current category: " + category, 12, MUTED, false));

        nameInput = new EditText(this);
        nameInput.setHint("Remote name (optional)");
        nameInput.setHintTextColor(0xFF64748B);
        nameInput.setTextColor(TEXT);
        nameInput.setBackgroundTintList(ColorStateList.valueOf(ACCENT));
        LinearLayout.LayoutParams inputLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        inputLp.topMargin = dp(10);
        importCard.addView(nameInput, inputLp);

        Button pick = button("Choose .ir file", ACCENT, 0xFF06263A);
        pick.setOnClickListener(v -> pickFile());
        LinearLayout.LayoutParams pickLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(50));
        pickLp.topMargin = dp(8);
        importCard.addView(pick, pickLp);
        importCard.addView(text(
                "Only raw codes are imported. Parsed protocol entries are skipped.",
                11, MUTED, false));
        addCard(root, importCard);

        LinearLayout capture = card();
        capture.addView(text("HOW TO CAPTURE", 11, ACCENT, true));
        capture.addView(text(
                "1. Wire a TSOP38238 receiver to an ESP32.\n"
                        + "2. Flash the IRremoteESP8266 \u201cIRrecvDumpV2\u201d example.\n"
                        + "3. Press a button on the original remote and copy the raw "
                        + "timings into a .ir file:\n\n"
                        + "name: Power\n"
                        + "type: raw\n"
                        + "frequency: 38000\n"
                        + "data: 9000 4500 560 560 \u2026\n\n"
                        + "4. Import that file here.",
                13, MUTED, false));
        addCard(root, capture);

        TextView footer = text("made by poging kyle", 12, 0xFF64748B, false);
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

    private void addCard(LinearLayout root, View card) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(14);
        root.addView(card, lp);
    }

    private void pickFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, PICK_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_FILE || resultCode != RESULT_OK || data == null) return;
        Uri uri = data.getData();
        if (uri == null) return;
        String fileName = displayName(uri);
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IllegalStateException("Cannot open file");
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                String line;
                while ((line = reader.readLine()) != null) sb.append(line).append('\n');
            }
            importText(sb.toString(), fileName);
        } catch (Exception e) {
            error("Import failed", e.toString());
        }
    }

    private String displayName(Uri uri) {
        String name = null;
        try (android.database.Cursor cursor = getContentResolver().query(uri, null,
                null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) name = cursor.getString(idx);
            }
        } catch (Exception ignored) {
        }
        if (name == null) name = uri.getLastPathSegment();
        if (name == null) name = "Learned remote";
        if (name.toLowerCase().endsWith(".ir")) name = name.substring(0, name.length() - 3);
        return name;
    }

    private void importText(String text, String fileName) {
        List<Entry> entries = parse(text);
        JSONObject buttons = new JSONObject();
        int raw = 0;
        int skipped = 0;
        for (Entry e : entries) {
            String key = normalize(e.name);
            if (key == null || e.data.size() < 4) {
                skipped++;
                continue;
            }
            try {
                JSONArray pattern = new JSONArray();
                for (int v : e.data) {
                    if (v > 0) pattern.put(v);
                }
                if (pattern.length() < 4) {
                    skipped++;
                    continue;
                }
                if (pattern.length() % 2 == 1) pattern.put(30000); // trailing gap
                JSONArray code = new JSONArray();
                code.put(e.frequency);
                code.put(pattern);
                if (!buttons.has(key)) buttons.put(key, code);
                raw++;
            } catch (Exception ex) {
                skipped++;
            }
        }
        if (!buttons.has("power")) {
            error("No usable codes", "No raw \u201cPower\u201d button was found. "
                    + "Parsed protocols must be exported as raw first.");
            return;
        }
        String name = nameInput.getText().toString().trim();
        if (name.isEmpty()) name = fileName;

        CustomStore.add(this, category, name, buttons);
        Intent result = new Intent();
        result.putExtra("category", category);
        result.putExtra("name", name);
        setResult(RESULT_OK, result);
        Toast.makeText(this, "Imported " + raw + " codes"
                + (skipped > 0 ? " (" + skipped + " skipped)" : ""), Toast.LENGTH_LONG).show();
        finish();
    }

    private List<Entry> parse(String text) {
        List<Entry> out = new ArrayList<>();
        Entry cur = null;
        for (String line : text.split("\\r?\\n")) {
            String s = line.trim();
            if (s.isEmpty() || s.startsWith("#")) continue;
            if (s.startsWith("name:")) {
                if (cur != null) out.add(cur);
                cur = new Entry();
                cur.name = s.substring(5).trim();
                continue;
            }
            if (cur == null || !s.contains(":")) continue;
            String k = s.substring(0, s.indexOf(':')).trim().toLowerCase();
            String v = s.substring(s.indexOf(':') + 1).trim();
            if (k.equals("type")) cur.type = v;
            else if (k.equals("frequency")) {
                try {
                    cur.frequency = (int) Float.parseFloat(v);
                } catch (Exception ignored) {
                }
            } else if (k.equals("data")) {
                cur.data.clear();
                for (String p : v.split("\\s+")) {
                    try {
                        int n = Integer.parseInt(p);
                        if (n > 0) cur.data.add(n);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        if (cur != null) out.add(cur);
        return out;
    }

    private String normalize(String name) {
        if (name == null) return null;
        String key = name.trim().toLowerCase().replace(' ', '_')
                .replaceAll("[^a-z0-9_+\\-]", "");
        String mapped = ALIASES.get(key);
        if (mapped == null) return null;
        for (String allowed : BUTTON_ORDER) {
            if (allowed.equals(mapped)) return mapped;
        }
        return null;
    }

    private void error(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
}
