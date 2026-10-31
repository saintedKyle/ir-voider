/*
 * IR Voider — Android IR remote.
 * Copyright (C) 2026 poging kyle
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.kylem.remote;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.hardware.ConsumerIrManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ViewFlipper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends Activity {

    private Palette palette;
    private int BG, CARD, BTN, STROKE, ACCENT, ACCENT2, TEXT, MUTED, RED, GREEN, ON_ACCENT;
    private int RIPPLE, ACTIVE;

    /** Copies the chosen theme onto the short names the screens already speak. */
    private void loadPalette() {
        palette = Palette.current(this);
        BG = palette.bg;
        CARD = palette.card;
        BTN = palette.btn;
        STROKE = palette.stroke;
        ACCENT = palette.accent;
        ACCENT2 = palette.accent2;
        TEXT = palette.text;
        MUTED = palette.muted;
        RED = palette.warn;
        GREEN = palette.ok;
        ON_ACCENT = palette.onAccent;
        RIPPLE = palette.ripple();
        ACTIVE = Surface.blend(CARD, ACCENT, 0.10f);
    }

    private static final String[] BUTTON_ORDER = {
            "power", "power_off", "vol_up", "vol_down", "mute", "ch_up", "ch_down",
            "input", "up", "down", "left", "right", "ok", "back", "home", "menu",
            "exit", "info", "guide", "play_pause", "stop", "rewind", "forward",
            "prev", "next", "record", "temp_up", "temp_down", "mode", "fan", "swing",
            "speed", "light", "timer", "sleep", "brightness_up", "brightness_down",
            "color", "red", "green", "yellow", "blue", "zoom_in", "zoom_out",
            "subtitle", "audio", "aspect", "last", "fav", "apps", "netflix",
            "num_0", "num_1", "num_2", "num_3", "num_4", "num_5", "num_6", "num_7",
            "num_8", "num_9", "dash", "dot"
    };
    private static final String[] BUTTON_LABELS = {
            "POWER", "Power Off", "Vol +", "Vol −", "Mute", "CH +", "CH −",
            "Input", "▲", "▼", "◀", "▶", "OK", "Back", "Home",
            "Menu", "Exit", "Info", "Guide", "Play/Pause", "Stop", "Rewind", "Forward",
            "Prev", "Next", "Record", "Temp +", "Temp −", "Mode", "Fan", "Swing",
            "Speed", "Light", "Timer", "Sleep", "Bright +", "Bright −", "Color",
            "Red", "Green", "Yellow", "Blue", "Zoom +", "Zoom −", "Subtitle",
            "Audio", "Aspect", "Last", "Fav", "Apps", "Netflix", "0", "1", "2", "3",
            "4", "5", "6", "7", "8", "9", "Dash", "Dot"
    };
    private static final String[] DPAD = {"up", "down", "left", "right", "ok"};
    private static final String[] BASIC_KEYS = {"vol_up", "vol_down", "mute", "ch_up", "ch_down", "input"};
    private static final String[] NAV_EXTRA = {"back", "home", "menu", "exit", "info", "guide"};
    private static final String[] MEDIA_KEYS = {"play_pause", "stop", "rewind", "forward",
            "prev", "next", "record", "subtitle", "audio", "aspect", "last", "fav",
            "apps", "netflix"};
    private static final String[] NUMBER_KEYS = {"num_1", "num_2", "num_3", "num_4",
            "num_5", "num_6", "num_7", "num_8", "num_9", "num_0", "dash", "dot"};
    private static final String[] COLOR_KEYS = {"red", "green", "yellow", "blue"};
    private static final int REQ_LEARN = 77;

    static class Code {
        int freq;
        int[] pattern;
    }

    static class Remote {
        String category;
        String brand;
        String name;
        final Map<String, Code> buttons = new LinkedHashMap<>();
    }

    static class Brand {
        String name;
        final List<Remote> remotes = new ArrayList<>();
    }

    static class Category {
        String name;
        final List<Brand> brands = new ArrayList<>();
        int remoteCount;
    }

    static class Candidate {
        Remote remote;
        Code code;
        String label;
    }

    private ConsumerIrManager ir;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService irExecutor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean irBusy = new AtomicBoolean(false);

    private final List<Category> categories = new ArrayList<>();
    private final List<Candidate> candidates = new ArrayList<>();
    private final Map<String, Button> buttons = new LinkedHashMap<>();
    private Category category;
    private List<Brand> brands = new ArrayList<>();

    private ScrollView scrollView;
    private LinearLayout controlsCard;
    private SwipeFlipper controlsPager;
    private TextView pageDots;
    private int pageCount;
    private Spinner brandSpinner;
    private Spinner remoteSpinner;
    private final Button[] speedButtons = new Button[4];
    private CheckBox allBrands;
    private TextView status;
    private TextView irChip;
    private TextView deviceNameText;
    private TextView modelText;
    private TextView categoryLabel;
    private Dialog scannerDialog;
    private Button findButton;
    private ProgressBar progress;
    private TextView scanCount;
    private TextView scanName;
    private TextView scanHistoryView;
    private Button pauseButton;
    private final java.util.ArrayDeque<String> scanHistory = new java.util.ArrayDeque<>();

    private Remote current;
    private boolean adjusting;
    private boolean bootstrapping;
    private boolean running;
    private boolean scanPaused;
    private int nextCandidate;
    private int scanDelayMs = 2000;
    private String repeatKey;
    private TextView scanTimer;
    private long scanStartedAt;
    private long pausedTotal;
    private long pauseStartedAt;
    private boolean updateChecked;
    private BroadcastReceiver jackPlug;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private final Runnable timerTick = new Runnable() {
        @Override
        public void run() {
            showScanElapsed();
            timerHandler.postDelayed(this, 500);
        }
    };

    private final Runnable repeatRunnable = new Runnable() {
        @Override
        public void run() {
            if (repeatKey == null) return;
            send(repeatKey);
            handler.postDelayed(this, 260);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadPalette();
        setTheme(palette.light ? R.style.AppTheme_Light : R.style.AppTheme);
        ir = (ConsumerIrManager) getSystemService(Context.CONSUMER_IR_SERVICE);
        getWindow().setBackgroundDrawable(new ColorDrawable(BG));
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
        Palette.applyStatusIcons(getWindow(), palette.light);

        TextView loading = text("Loading IR database\u2026", 18, MUTED, false);
        loading.setGravity(Gravity.CENTER);
        loading.setBackgroundColor(BG);
        loading.setPadding(dp(48), dp(220), dp(48), dp(48));
        setContentView(loading);

        new Thread(() -> {
            String error = null;
            try {
                loadDatabase();
            } catch (Exception e) {
                error = e.toString();
            }
            final String err = error;
            runOnUiThread(() -> {
                if (err != null) {
                    loading.setText("Could not load remote database:\n" + err);
                } else {
                    buildUi();
                    maybeCheckForUpdate();
                }
            });
        }).start();
    }

    // ------------------------------------------------------------------ data

    private String readAsset(String name) throws Exception {
        try (InputStream in = getAssets().open(name)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toString("UTF-8");
        }
    }

    private void loadDatabase() throws Exception {
        JSONObject root = new JSONObject(readAsset("remotes.json"));
        JSONArray cats = root.getJSONArray("categories");
        for (int i = 0; i < cats.length(); i++) {
            JSONObject co = cats.getJSONObject(i);
            Category cat = new Category();
            cat.name = co.getString("name");
            JSONArray brandArray = co.getJSONArray("brands");
            for (int j = 0; j < brandArray.length(); j++) {
                JSONObject bo = brandArray.getJSONObject(j);
                Brand brand = new Brand();
                brand.name = bo.getString("name");
                JSONArray remotes = bo.getJSONArray("remotes");
                for (int k = 0; k < remotes.length(); k++) {
                    JSONObject ro = remotes.getJSONObject(k);
                    Remote remote = new Remote();
                    remote.category = cat.name;
                    remote.brand = brand.name;
                    remote.name = ro.getString("name");
                    JSONObject btns = ro.getJSONObject("buttons");
                    for (int b = 0; b < BUTTON_ORDER.length; b++) {
                        String key = BUTTON_ORDER[b];
                        if (!btns.has(key)) continue;
                        JSONArray arr = btns.getJSONArray(key);
                        Code code = new Code();
                        code.freq = arr.getInt(0);
                        JSONArray pat = arr.getJSONArray(1);
                        code.pattern = new int[pat.length()];
                        for (int p = 0; p < pat.length(); p++) code.pattern[p] = pat.getInt(p);
                        remote.buttons.put(key, code);
                    }
                    if (!remote.buttons.containsKey("power")) continue;
                    brand.remotes.add(remote);
                    cat.remoteCount++;
                }
                if (!brand.remotes.isEmpty()) cat.brands.add(brand);
            }
            if (!cat.brands.isEmpty()) categories.add(cat);
        }
        if (categories.isEmpty()) throw new IllegalStateException("No categories in database");
        appendCustomRemotes();

        int index = findCategory(DeviceStore.selectedCategory(this));
        category = categories.get(index < 0 ? 0 : index);
        brands = category.brands;
    }

    private int findCategory(String name) {
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).name.equals(name)) return i;
        }
        return -1;
    }

    // -------------------------------------------------------------------- ui

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
        tv.setLineSpacing(0f, 1.08f);
        return tv;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16), dp(16), dp(16), dp(16));
        c.setBackground(Surface.raised(this, 28, CARD, STROKE, palette.gloss(0.09f)));
        Surface.elevate(c, 28, 5);
        return c;
    }

    private void addCard(LinearLayout root, View card) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(14);
        root.addView(card, lp);
    }

    private Button styledButton(String label, int color, float textSize) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(textSize);
        b.setTextColor(TEXT);
        b.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        b.setStateListAnimator(null);
        Drawable plate = Surface.raised(this, 20, color,
                palette.rim(), palette.gloss(0.14f));
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(RIPPLE), plate, null));
        b.setPadding(dp(8), dp(8), dp(8), dp(8));
        Surface.elevate(b, 20, 2);
        Surface.sinkOnPress(b, 2);
        return b;
    }

    private Button highlightButton(String label, float textSize) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(textSize);
        b.setTextColor(ON_ACCENT);
        b.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        b.setStateListAnimator(null);
        Drawable plate = Surface.raised(this, 22, ACCENT,
                Surface.darken(ACCENT, 0.42f), palette.gloss(0.24f));
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33000000), plate, null));
        b.setPadding(dp(8), dp(8), dp(8), dp(8));
        Surface.elevate(b, 22, 5);
        Surface.sinkOnPress(b, 5);
        return b;
    }

    private Drawable pill(int fill, int strokeColor) {
        return Surface.raised(this, 50, fill, strokeColor, palette.gloss(0.12f));
    }

    private GradientDrawable appBackground() {
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{BG, palette.bgBottom});
        bg.setShape(GradientDrawable.RECTANGLE);
        return bg;
    }

    private void buildUi() {
        scanDelayMs = DeviceStore.scanDelayMs(this);
        scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(Color.TRANSPARENT);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(22), dp(16), dp(28));
        scrollView.addView(root);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        Button more = styledButton("\u2261", BTN, 24);
        more.setContentDescription("More");
        more.setOnClickListener(v -> showMore());
        top.addView(more, new LinearLayout.LayoutParams(dp(42), dp(42)));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ic_launcher);
        LinearLayout.LayoutParams logoLp = new LinearLayout.LayoutParams(dp(36), dp(36));
        logoLp.setMargins(dp(8), 0, dp(8), 0);
        top.addView(logo, logoLp);

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.addView(text("IR Voider", 22, TEXT, true));
        categoryLabel = text("", 10, ACCENT, false);
        categoryLabel.setBackground(pill(CARD, STROKE));
        categoryLabel.setPadding(dp(9), dp(3), dp(9), dp(3));
        LinearLayout.LayoutParams catLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        catLp.topMargin = dp(3);
        titles.addView(categoryLabel, catLp);
        top.addView(titles, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button configs = styledButton("\u2699", BTN, 18);
        configs.setContentDescription("Configs");
        configs.setOnClickListener(v ->
                startActivity(new Intent(this, ConfigsActivity.class)));
        top.addView(configs, new LinearLayout.LayoutParams(dp(42), dp(42)));
        root.addView(top);

        status = text("", 13, MUTED, false);
        status.setPadding(0, dp(10), 0, dp(12));
        root.addView(status);

        LinearLayout currentCard = card();
        LinearLayout nowRow = new LinearLayout(this);
        nowRow.setOrientation(LinearLayout.HORIZONTAL);
        nowRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView nowLabel = text("NOW USING", 10, ACCENT, true);
        nowLabel.setLetterSpacing(0.12f);
        nowRow.addView(nowLabel, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        irChip = text("", 9, TEXT, true);
        irChip.setLetterSpacing(0.08f);
        irChip.setPadding(dp(8), dp(3), dp(8), dp(3));
        nowRow.addView(irChip);
        refreshIrChip();
        currentCard.addView(nowRow);
        deviceNameText = text("Not selected", 20, TEXT, true);
        deviceNameText.setPadding(0, dp(4), 0, 0);
        currentCard.addView(deviceNameText);
        modelText = text("", 12, MUTED, false);
        modelText.setPadding(0, dp(2), 0, dp(2));
        currentCard.addView(modelText);
        currentCard.addView(text("Tap \u2630 to change category, or \u2699 to name and save devices.",
                12, MUTED, false));
        addCard(root, currentCard);

        controlsCard = card();
        addCard(root, controlsCard);

        LinearLayout scanCard = card();
        scanCard.addView(text("Find a working code", 17, TEXT, true));
        brandSpinner = new Spinner(this);
        remoteSpinner = new Spinner(this);
        brandSpinner.setBackgroundTintList(ColorStateList.valueOf(ACCENT));
        remoteSpinner.setBackgroundTintList(ColorStateList.valueOf(ACCENT));
        LinearLayout brandRow = spinnerRow("Brand", brandSpinner);
        Button searchBrand = styledButton("Search", BTN, 12);
        searchBrand.setPadding(0, 0, 0, 0);
        searchBrand.setOnClickListener(v -> showBrandSearch());
        LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(dp(74), dp(38));
        searchLp.setMargins(dp(6), 0, 0, 0);
        brandRow.addView(searchBrand, searchLp);
        scanCard.addView(brandRow);
        scanCard.addView(spinnerRow("Model", remoteSpinner));
        TextView hint = text(
                "Point the phone\u2019s top edge at the device. The scan sends one "
                        + "power code at a time; stop when it reacts.",
                12, MUTED, false);
        hint.setPadding(0, dp(8), 0, dp(8));
        scanCard.addView(hint);

        allBrands = new CheckBox(this);
        allBrands.setText("Search every brand in this category");
        allBrands.setTextColor(TEXT);
        allBrands.setButtonTintList(ColorStateList.valueOf(ACCENT));
        scanCard.addView(allBrands);

        LinearLayout speedRow = new LinearLayout(this);
        speedRow.setOrientation(LinearLayout.HORIZONTAL);
        speedRow.setGravity(Gravity.CENTER_VERTICAL);
        speedRow.setPadding(0, dp(4), 0, dp(4));
        TextView speedLabel = text("Speed", 14, MUTED, false);
        speedLabel.setWidth(dp(58));
        speedRow.addView(speedLabel);
        String[] speedLabels = {"1 s", "1.5 s", "2 s", "3 s"};
        for (int i = 0; i < speedButtons.length; i++) {
            final int index = i;
            Button b = styledButton(speedLabels[i], BTN, 13);
            b.setOnClickListener(v -> setScanSpeed(index, true));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(42), 1f);
            lp.setMargins(dp(3), 0, dp(3), 0);
            speedRow.addView(b, lp);
            speedButtons[i] = b;
        }
        scanCard.addView(speedRow);
        setScanSpeed(scanDelayMs <= 1100 ? 0 : scanDelayMs <= 1600 ? 1
                : scanDelayMs <= 2200 ? 2 : 3, false);

        findButton = highlightButton("Find code", 16);
        findButton.setOnClickListener(v -> startAutoFind());
        scanCard.addView(findButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));

        addCard(root, scanCard);

        TextView credit = text(
                "Codes: Flipper-IRDB (CC0). IR only \u2014 no network calls unless "
                        + "you turn on update checks.",
                11, MUTED, false);
        credit.setPadding(dp(4), dp(4), 0, 0);
        root.addView(credit);

        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackground(appBackground());
        shell.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        View divider = new View(this);
        divider.setBackgroundColor(Surface.blend(BG, TEXT, 0.13f));
        shell.addView(divider, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        TextView footer = text("made by poging kyle", 11, MUTED, false);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(12), 0, dp(12));
        shell.addView(footer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        shell.setOnApplyWindowInsetsListener((v, insets) -> {
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
        shell.requestApplyInsets();

        setContentView(shell);

        bootstrapping = true;
        setupSpinners();
        applySavedSelection(true);
        bootstrapping = false;
        updateCategoryLabel();
        renderControls();
        updateDeviceLabels();
    }

    private void addRow(LinearLayout parent, String... keys) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        for (String key : keys) {
            if (key == null) {
                Space s = new Space(this);
                row.addView(s, new LinearLayout.LayoutParams(0, 1, 1f));
                continue;
            }
            Button b = styledButton(labelFor(key), BTN, 15);
            if (isRepeatable(key)) {
                attachRepeat(b, key);
            } else {
                b.setOnClickListener(v -> send(key));
            }
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(54), 1f);
            lp.setMargins(dp(3), dp(3), dp(3), dp(3));
            row.addView(b, lp);
            buttons.put(key, b);
        }
        parent.addView(row);
    }

    private boolean isRepeatable(String key) {
        return key.equals("vol_up") || key.equals("vol_down")
                || key.equals("ch_up") || key.equals("ch_down")
                || key.equals("temp_up") || key.equals("temp_down")
                || key.equals("brightness_up") || key.equals("brightness_down");
    }

    private void attachRepeat(Button b, String key) {
        b.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    repeatKey = key;
                    send(key);
                    handler.postDelayed(repeatRunnable, 450);
                    v.setPressed(true);
                    Surface.sink(v, true, 2);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    repeatKey = null;
                    handler.removeCallbacks(repeatRunnable);
                    v.setPressed(false);
                    Surface.sink(v, false, 2);
                    return true;
                default:
                    return false;
            }
        });
    }

    private LinearLayout spinnerRow(String label, Spinner spinner) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(2), 0, dp(2));
        TextView tv = text(label, 14, MUTED, false);
        tv.setWidth(dp(58));
        row.addView(tv);
        row.addView(spinner, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    private void setScanSpeed(int index, boolean persist) {
        scanDelayMs = index == 0 ? 1000 : index == 1 ? 1500 : index == 2 ? 2000 : 3000;
        if (persist) DeviceStore.setScanDelayMs(this, scanDelayMs);
        for (int i = 0; i < speedButtons.length; i++) {
            Button b = speedButtons[i];
            if (b == null) continue;
            boolean active = i == index;
            Drawable plate = Surface.raised(this, 12,
                    active ? ACCENT : Surface.darken(BTN, palette.light ? 0.12f : 0.35f),
                    active ? Surface.darken(ACCENT, 0.42f) : STROKE,
                    palette.gloss(active ? 0.22f : 0.08f));
            b.setBackground(new RippleDrawable(ColorStateList.valueOf(RIPPLE), plate, null));
            Surface.elevate(b, 12, active ? 4 : 0);
            b.setTextColor(active ? ON_ACCENT : MUTED);
            b.setTypeface(Typeface.create("sans-serif-medium",
                    active ? Typeface.BOLD : Typeface.NORMAL));
        }
    }

    private String labelFor(String key) {
        for (int i = 0; i < BUTTON_ORDER.length; i++) {
            if (BUTTON_ORDER[i].equals(key)) return BUTTON_LABELS[i];
        }
        return key;
    }

    // ------------------------------------------------------------- selection

    private void setupSpinners() {
        rebuildBrandSpinner();
        brandSpinner.setOnItemSelectedListener(new SimpleItemListener() {
            @Override
            public void onItemSelected(int position) {
                if (adjusting) return;
                if (position < 0 || position >= brands.size()) return;
                if (current != null && brands.get(position).name.equals(current.brand)) return;
                populateRemotes(position);
            }
        });
        remoteSpinner.setOnItemSelectedListener(new SimpleItemListener() {
            @Override
            public void onItemSelected(int position) {
                if (adjusting) return;
                int bi = brandSpinner.getSelectedItemPosition();
                if (bi < 0 || bi >= brands.size()) return;
                Brand brand = brands.get(bi);
                if (position < 0 || position >= brand.remotes.size()) return;
                Remote r = brand.remotes.get(position);
                if (r == current) return;
                selectRemote(r);
            }
        });
        if (!brands.isEmpty()) populateRemotes(0);
    }

    private void rebuildBrandSpinner() {
        List<String> names = new ArrayList<>();
        for (Brand b : brands) names.add(b.name);
        ArrayAdapter<String> ba = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, names);
        ba.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adjusting = true;
        brandSpinner.setAdapter(ba);
        adjusting = false;
    }

    private void populateRemotes(int brandIndex) {
        if (brandIndex < 0 || brandIndex >= brands.size()) return;
        Brand brand = brands.get(brandIndex);
        adjusting = true;
        setRemoteAdapter(brand, brand.remotes.get(0));
        adjusting = false;
        selectRemote(brand.remotes.get(0));
    }

    private void setRemoteAdapter(Brand brand, Remote selected) {
        List<String> names = new ArrayList<>();
        int selectedIndex = 0;
        for (int i = 0; i < brand.remotes.size(); i++) {
            Remote r = brand.remotes.get(i);
            names.add(pretty(r.name));
            if (r == selected) selectedIndex = i;
        }
        ArrayAdapter<String> ra = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, names);
        ra.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        remoteSpinner.setAdapter(ra);
        remoteSpinner.setSelection(selectedIndex);
    }

    private void applyRemote(Remote r, boolean saved) {
        adjusting = true;
        if (!r.category.equals(category.name)) {
            int ci = findCategory(r.category);
            if (ci >= 0) {
                category = categories.get(ci);
                brands = category.brands;
                rebuildBrandSpinner();
                updateCategoryLabel();
            }
        }
        int bi = brandIndex(r.brand);
        if (bi >= 0) {
            brandSpinner.setSelection(bi);
            setRemoteAdapter(brands.get(bi), r);
        }
        current = r;
        renderControls();
        updateDeviceLabels();
        status.setText(saved ? "Saved. Press POWER to test." : "Ready.");
        handler.postDelayed(() -> adjusting = false, 400);
    }

    private int brandIndex(String name) {
        for (int i = 0; i < brands.size(); i++) {
            if (brands.get(i).name.equals(name)) return i;
        }
        return -1;
    }

    private void selectRemote(Remote r) {
        current = r;
        renderControls();
        updateDeviceLabels();
        status.setText("Power is a toggle \u2014 the same button turns it on and off.");
        if (!bootstrapping) saveCurrent(r);
    }

    private void saveCurrent(Remote r) {
        Code power = r.buttons.get("power");
        DeviceStore.rememberSelection(this, r.category, r.brand, r.name,
                power == null ? 0 : Arrays.hashCode(power.pattern));
    }

    private void updateDeviceLabels() {
        if (deviceNameText == null) return;
        if (current == null) {
            deviceNameText.setText("Not selected");
            if (modelText != null) modelText.setText("");
            return;
        }
        Code power = current.buttons.get("power");
        int hash = power == null ? 0 : Arrays.hashCode(power.pattern);
        DeviceStore.Device d = DeviceStore.findByRemote(this, current.category,
                current.brand, current.name, hash);
        if (d != null) {
            deviceNameText.setText(d.name);
            modelText.setText(d.type + " \u2022 " + current.category + " \u2022 "
                    + current.brand + " \u2014 " + pretty(current.name));
        } else {
            deviceNameText.setText(current.brand + " \u2014 " + pretty(current.name));
            modelText.setText(current.category + " \u2022 Tap \u2699 to name this device");
        }
    }

    private void showBrandSearch() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(18), dp(18), dp(18), dp(18));
        panel.setBackground(Surface.raised(this, 28, CARD, palette.rim(), palette.gloss(0.10f)));
        Surface.elevate(panel, 28, 12);

        panel.addView(text("Find a brand", 22, TEXT, true));
        EditText input = new EditText(this);
        input.setHint("Search brand");
        input.setHintTextColor(MUTED);
        input.setTextColor(TEXT);
        input.setSingleLine(true);
        input.setBackgroundTintList(ColorStateList.valueOf(ACCENT));
        LinearLayout.LayoutParams inputLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        inputLp.topMargin = dp(10);
        panel.addView(input, inputLp);

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        LinearLayout.LayoutParams listLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        listLp.topMargin = dp(8);
        panel.addView(scroll, listLp);

        Runnable fill = () -> {
            list.removeAllViews();
            String q = input.getText().toString().trim().toLowerCase();
            int shown = 0;
            for (int i = 0; i < brands.size(); i++) {
                Brand b = brands.get(i);
                if (!q.isEmpty() && !b.name.toLowerCase().contains(q)) continue;
                shown++;
                boolean active = current != null && b.name.equals(current.brand);
                TextView row = text(b.name, 15, active ? ACCENT : TEXT, active);
                row.setPadding(dp(10), dp(13), dp(10), dp(13));
                row.setBackground(active ? Surface.raised(this, 12, ACTIVE,
                        Surface.blend(CARD, ACCENT, 0.5f), palette.gloss(0.16f)) : null);
                final int index = i;
                row.setOnClickListener(v -> {
                    brandSpinner.setSelection(index);
                    dialog.dismiss();
                });
                list.addView(row);
            }
            if (shown == 0) {
                list.addView(text("No matching brand.", 13, MUTED, false));
            }
        };
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { fill.run(); }
        });
        fill.run();

        Button cancel = styledButton("Cancel", BTN, 14);
        cancel.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams cancelLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        cancelLp.topMargin = dp(8);
        panel.addView(cancel, cancelLp);

        dialog.setContentView(panel);
        dialog.show();
        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(w.getAttributes());
            lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.92f);
            lp.height = (int) (getResources().getDisplayMetrics().heightPixels * 0.72f);
            w.setAttributes(lp);
        }
    }

    private void updateCategoryLabel() {
        if (categoryLabel != null && category != null) {
            categoryLabel.setText(category.name + " \u2022 " + category.remoteCount
                    + " remotes \u2022 \u2261 for more");
        }
    }

    private Remote findRemote(String brand, String name, int hash) {
        for (Brand b : brands) {
            if (!b.name.equals(brand)) continue;
            Remote fallback = null;
            for (Remote r : b.remotes) {
                if (!r.name.equals(name)) continue;
                if (fallback == null) fallback = r;
                Code p = r.buttons.get("power");
                if (hash != 0 && p != null && Arrays.hashCode(p.pattern) == hash) return r;
            }
            return fallback;
        }
        return null;
    }

    private void applySavedSelection(boolean saved) {
        SharedPreferences p = getSharedPreferences(DeviceStore.PREFS, MODE_PRIVATE);
        if (p.getInt("version", 1) < 2) {
            p.edit().clear().putInt("version", 2).apply();
            return;
        }
        p.edit().putInt("version", 2).apply();

        String catName = DeviceStore.selectedCategory(this);
        int ci = findCategory(catName);
        if (ci >= 0 && !categories.get(ci).name.equals(category.name)) {
            category = categories.get(ci);
            brands = category.brands;
            rebuildBrandSpinner();
            updateCategoryLabel();
        }
        String brand = DeviceStore.selectedBrand(this);
        String name = DeviceStore.selectedRemote(this);
        if (brand == null || name == null) {
            if (!brands.isEmpty()) populateRemotes(0);
            return;
        }
        Remote r = findRemote(brand, name, DeviceStore.selectedHash(this));
        if (r != null) {
            applyRemote(r, saved);
        } else if (!brands.isEmpty()) {
            populateRemotes(0);
        }
    }

    private void syncFromPrefs() {
        if (categories.isEmpty()) return;
        String catName = DeviceStore.selectedCategory(this);
        int ci = findCategory(catName);
        if (ci >= 0) {
            Category c = categories.get(ci);
            if (!c.name.equals(category.name)) {
                category = c;
                brands = category.brands;
                rebuildBrandSpinner();
                updateCategoryLabel();
            }
        }
        String brand = DeviceStore.selectedBrand(this);
        String name = DeviceStore.selectedRemote(this);
        if (brand == null || name == null) {
            updateDeviceLabels();
            return;
        }
        Remote r = findRemote(brand, name, DeviceStore.selectedHash(this));
        if (r != null && r != current) {
            applyRemote(r, false);
        } else {
            updateDeviceLabels();
        }
    }

    // ------------------------------------------------------------------- ir

    private void send(String key) {
        getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        if (current == null) {
            status.setText("Pick a device remote first.");
            return;
        }
        Code code = current.buttons.get(key);
        if (code == null) {
            status.setText("This remote has no \u201c" + labelFor(key) + "\u201d code.");
            return;
        }
        boolean repeat;
        if ("power".equals(key)) {
            repeat = "Air Conditioner".equals(current.category)
                    && DeviceStore.acPowerRepeat(this);
        } else {
            repeat = isRepeatable(key) || DeviceStore.boostAll(this);
        }
        transmit(code, repeat);
        status.setText("Sent " + labelFor(key) + ".");
    }

    private static final int EMITTER_NONE = 0;
    private static final int EMITTER_JACK = 1;
    private static final int EMITTER_BUILT = 2;

    /**
     * Which emitter the phone can actually evidence. Turning the jack mode on is a
     * deliberate choice, so it wins over a built-in emitter; otherwise the built-in
     * report wins, and with neither the chip says NO IR and means it.
     */
    private int emitterState() {
        if (JackBlaster.active(this)) return EMITTER_JACK;
        if (ir != null && ir.hasIrEmitter()) return EMITTER_BUILT;
        return EMITTER_NONE;
    }

    /**
     * The chip states only what a sensor proves: an emitter the OS reports, or a
     * plug in the port. Nothing here can confirm that light left an LED, so the
     * jack path is never labelled ready.
     */
    private void refreshIrChip() {
        if (irChip == null) return;
        int state = emitterState();
        int tint = state == EMITTER_BUILT ? ACCENT
                : state == EMITTER_JACK ? Surface.blend(ACCENT, RED, 0.5f) : RED;
        irChip.setText(state == EMITTER_BUILT ? "IR READY"
                : state == EMITTER_JACK ? "JACK BLASTER" : "NO IR");
        irChip.setTextColor(Surface.blend(tint, TEXT, 0.35f));
        irChip.setBackground(pill(Surface.blend(CARD, tint, 0.18f),
                Surface.blend(CARD, tint, 0.45f)));
        boolean setup = state != EMITTER_BUILT;
        irChip.setClickable(setup);
        irChip.setOnClickListener(setup ? v -> showJackDialog() : null);
    }

    private String noEmitterMessage() {
        if (JackBlaster.enabled(this)) {
            return "The port reports nothing plugged in. Push the blaster home, "
                    + "or switch on Force in More \u2192 Jack blaster.";
        }
        return "No IR emitter reported. Set up a jack blaster in More \u2192 Jack "
                + "blaster, or enable IR override to try anyway.";
    }

    private void transmit(Code code, boolean allowRepeat) {
        int repeats = DeviceStore.repeats(this);
        if (allowRepeat && DeviceStore.forceMode(this)) repeats = Math.max(3, repeats);
        transmit(code, allowRepeat ? repeats : 1);
    }

    private void transmit(Code code, int repeats) {
        final boolean jack = emitterState() == EMITTER_JACK;
        if (!jack) {
            if (ir == null) {
                status.setText("No IR service on this phone.");
                return;
            }
            if (!ir.hasIrEmitter() && !DeviceStore.irOverride(this)) {
                status.setText(noEmitterMessage());
                return;
            }
        }
        if (irExecutor.isShutdown()) return;
        if (!irBusy.compareAndSet(false, true)) return;
        final int count = Math.max(1, Math.min(3, repeats));
        try {
            irExecutor.execute(() -> {
            String failure = null;
            try {
                for (int i = 0; i < count; i++) {
                    if (jack) {
                        JackBlaster.Result sent = JackBlaster.send(this, code);
                        if (!sent.ok) {
                            failure = sent.detail;
                            break;
                        }
                    } else {
                        ir.transmit(code.freq, code.pattern);
                    }
                    if (i + 1 < count) Thread.sleep(DeviceStore.forceMode(this) ? 25 : 50);
                }
            } catch (InterruptedException ignored) {
            } catch (Exception e) {
                failure = e.getMessage() == null
                        ? e.getClass().getSimpleName() : e.getMessage();
            } finally {
                irBusy.set(false);
                final String why = failure;
                if (why != null) {
                    runOnUiThread(() -> status.setText(
                            (jack ? "Jack send failed: " : "IR send failed: ") + why));
                }
            }
            });
        } catch (RejectedExecutionException e) {
            irBusy.set(false);
        }
    }

    private void sendPowerRepeat() {
        if (current == null) return;
        Code code = current.buttons.get("power");
        if (code == null) {
            status.setText("No POWER code for this remote.");
            return;
        }
        getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        transmit(code, 2);
        status.setText("Sent POWER ×2.");
    }

    private void showScannerDialog() {
        scannerDialog = new Dialog(this);
        scannerDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(24), dp(56), dp(24), dp(32));

        TextView kicker = text("SCANNING CODES", 10, MUTED, true);
        kicker.setLetterSpacing(0.16f);
        root.addView(kicker);
        TextView headline = text("Point at the device", 30, TEXT, true);
        headline.setPadding(0, dp(8), 0, dp(4));
        root.addView(headline);
        TextView categoryText = text(current.category + "  \u2022  "
                + candidates.size() + " codes", 13, MUTED, false);
        root.addView(categoryText);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(1);
        progress.setProgress(0);
        LayerDrawable bar = new LayerDrawable(new Drawable[]{
                Surface.recessed(this, 8, Surface.darken(CARD, 0.55f), STROKE),
                Surface.raised(this, 8, ACCENT, Surface.darken(ACCENT, 0.4f),
                        palette.gloss(0.25f))});
        bar.setId(0, android.R.id.background);
        bar.setId(1, android.R.id.progress);
        progress.setProgressDrawable(bar);
        LinearLayout.LayoutParams progressLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(8));
        progressLp.topMargin = dp(28);
        root.addView(progress, progressLp);

        scanCount = text("", 13, MUTED, false);
        scanCount.setPadding(0, dp(12), 0, 0);
        root.addView(scanCount);
        scanName = text("", 26, TEXT, true);
        scanName.setPadding(0, dp(4), 0, dp(10));
        root.addView(scanName);
        scanTimer = text("", 13, ACCENT, true);
        scanTimer.setPadding(0, 0, 0, dp(8));
        root.addView(scanTimer);
        root.addView(text("Watch the device. When it reacts, tap It worked.",
                12, MUTED, false));

        LinearLayout historyBox = new LinearLayout(this);
        historyBox.setOrientation(LinearLayout.VERTICAL);
        historyBox.setPadding(dp(14), dp(10), dp(14), dp(10));
        historyBox.setBackground(Surface.recessed(this, 16, Surface.darken(CARD, 0.55f), STROKE));
        TextView historyLabel = text("LAST 5 SCANNED", 9, MUTED, true);
        historyLabel.setLetterSpacing(0.12f);
        historyBox.addView(historyLabel);
        scanHistoryView = text("Nothing scanned yet.", 11, MUTED, false);
        scanHistoryView.setPadding(0, dp(5), 0, 0);
        historyBox.addView(scanHistoryView);
        LinearLayout.LayoutParams historyLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        historyLp.topMargin = dp(16);
        root.addView(historyBox, historyLp);

        root.addView(new Space(this), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        Button worked = highlightButton("It worked", 18);
        worked.setOnClickListener(v -> saveCandidate(nextCandidate - 1));
        root.addView(worked, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(70)));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        Button previous = styledButton("Previous", BTN, 15);
        previous.setOnClickListener(v -> rewindOne());
        pauseButton = styledButton("Pause", BTN, 15);
        pauseButton.setOnClickListener(v -> pauseScan());
        Button skip = styledButton("Skip 5", BTN, 15);
        skip.setOnClickListener(v -> skipFive());
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, dp(54), 1f);
        p1.setMargins(0, dp(10), dp(4), 0);
        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(0, dp(54), 1f);
        p2.setMargins(dp(4), dp(10), dp(4), 0);
        LinearLayout.LayoutParams p3 = new LinearLayout.LayoutParams(0, dp(54), 1f);
        p3.setMargins(dp(4), dp(10), 0, 0);
        row.addView(previous, p1);
        row.addView(pauseButton, p2);
        row.addView(skip, p3);
        root.addView(row);
        Button stop = styledButton("Stop", Surface.blend(CARD, RED, 0.22f), 15);
        stop.setTextColor(RED);
        stop.setOnClickListener(v -> stopAutoFind("Stopped."));
        LinearLayout.LayoutParams stopLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(50));
        stopLp.topMargin = dp(8);
        root.addView(stop, stopLp);
        root.addView(text("Pause to stop sending. Previous replays; Skip 5 jumps ahead.",
                11, MUTED, false));

        scanHistory.clear();
        scanHistoryView.setText("Nothing scanned yet.");
        scannerDialog.setContentView(root);
        scannerDialog.setCanceledOnTouchOutside(false);
        scannerDialog.setOnCancelListener(d -> stopAutoFind("Stopped."));
        scannerDialog.show();
        Window window = scannerDialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(BG));
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT);
        }
    }

    private void dismissScanner() {
        timerHandler.removeCallbacks(timerTick);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        scanTimer = null;
        if (scannerDialog != null) {
            scannerDialog.dismiss();
            scannerDialog = null;
        }
    }

    private void showScanElapsed() {
        if (scanTimer == null) return;
        long now = SystemClock.elapsedRealtime();
        long active = now - scanStartedAt - pausedTotal;
        if (scanPaused && pauseStartedAt > 0) active -= now - pauseStartedAt;
        int left = Math.max(0, candidates.size() - nextCandidate);
        String tail = scanPaused ? "paused"
                : "\u2248 " + clock((long) left * scanDelayMs / 1000L) + " left";
        scanTimer.setText(clock(active / 1000L) + " elapsed  \u00b7  " + tail);
    }

    private static String clock(long seconds) {
        long s = Math.max(0, seconds);
        if (s < 3600) return String.format(java.util.Locale.US, "%d:%02d", s / 60, s % 60);
        return String.format(java.util.Locale.US, "%d:%02d:%02d",
                s / 3600, (s / 60) % 60, s % 60);
    }

    private void recordScanHistory(Candidate c) {
        if (scanHistoryView == null) return;
        String line = nextCandidate + ". " + c.remote.brand + " — "
                + pretty(c.remote.name) + c.label;
        scanHistory.addFirst(line);
        while (scanHistory.size() > 5) scanHistory.removeLast();
        StringBuilder sb = new StringBuilder();
        for (String item : scanHistory) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(item);
        }
        scanHistoryView.setText(sb.toString());
    }

    private void pauseScan() {
        if (!running) return;
        scanPaused = !scanPaused;
        if (scanPaused) {
            pauseStartedAt = SystemClock.elapsedRealtime();
        } else {
            resumeClock();
        }
        handler.removeCallbacksAndMessages(null);
        if (pauseButton != null) pauseButton.setText(scanPaused ? "Resume" : "Pause");
        if (!scanPaused) handler.postDelayed(this::tickAutoFind, 250);
    }

    private void resumeClock() {
        if (pauseStartedAt == 0) return;
        pausedTotal += SystemClock.elapsedRealtime() - pauseStartedAt;
        pauseStartedAt = 0;
    }

    private void skipFive() {
        if (!running) return;
        scanPaused = false;
        resumeClock();
        if (pauseButton != null) pauseButton.setText("Pause");
        handler.removeCallbacksAndMessages(null);
        nextCandidate = Math.min(candidates.size(), nextCandidate + 5);
        if (nextCandidate >= candidates.size()) {
            stopAutoFind("Skipped to the end.");
            return;
        }
        tickAutoFind();
    }

    // ------------------------------------------------------------- auto find

    private void startAutoFind() {
        if (running) return;
        boolean allowed = emitterState() != EMITTER_NONE
                || (ir != null && DeviceStore.irOverride(this));
        if (!allowed) {
            status.setText(noEmitterMessage());
            return;
        }
        candidates.clear();
        Map<String, Candidate> unique = new LinkedHashMap<>();
        if (allBrands.isChecked()) {
            for (Brand b : brands) {
                for (Remote r : b.remotes) addCandidate(r, unique);
            }
        } else {
            int bi = brandSpinner.getSelectedItemPosition();
            if (bi < 0 || bi >= brands.size()) return;
            for (Remote r : brands.get(bi).remotes) addCandidate(r, unique);
        }
        candidates.addAll(unique.values());
        if (candidates.isEmpty()) {
            status.setText("No codes available.");
            return;
        }
        if (allBrands.isChecked()) {
            findButton.setEnabled(false);
            new AlertDialog.Builder(this)
                    .setTitle("Scan every brand?")
                    .setMessage("This sends power codes for many brands and can reach "
                            + "nearby devices. Point directly at your device and keep "
                            + "the phone close.")
                    .setPositiveButton("Scan", (dialog, which) -> beginScan())
                    .setNegativeButton("Cancel", (dialog, which) -> findButton.setEnabled(true))
                    .setOnCancelListener(dialog -> findButton.setEnabled(true))
                    .show();
        } else {
            beginScan();
        }
    }

    private void beginScan() {
        if (running) return;
        running = true;
        scanPaused = false;
        scanStartedAt = SystemClock.elapsedRealtime();
        pausedTotal = 0;
        pauseStartedAt = 0;
        nextCandidate = 0;
        findButton.setEnabled(false);
        allBrands.setEnabled(false);
        brandSpinner.setEnabled(false);
        remoteSpinner.setEnabled(false);
        for (Button b : speedButtons) { b.setEnabled(false); b.setAlpha(0.4f); }
        for (Button b : buttons.values()) {
            b.setEnabled(false);
            b.setAlpha(0.3f);
        }
        showScannerDialog();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        timerHandler.post(timerTick);
        progress.setMax(candidates.size());
        progress.setProgress(0);
        scanCount.setText("Scanning 0 / " + candidates.size());
        scanName.setText("Get ready\u2026");
        handler.postDelayed(this::tickAutoFind, 800);
    }

    private void addCandidate(Remote r, Map<String, Candidate> unique) {
        Code power = r.buttons.get("power");
        if (power == null) return;
        addCandidate(r, power, "", unique);
        Code off = r.buttons.get("power_off");
        if (off != null && "Air Conditioner".equals(r.category)
                && !(off.freq == power.freq && Arrays.equals(off.pattern, power.pattern))) {
            addCandidate(r, off, "  (Power Off)", unique);
        }
    }

    private void addCandidate(Remote r, Code code, String label,
                              Map<String, Candidate> unique) {
        String key = code.freq + ":" + Arrays.hashCode(code.pattern);
        Candidate existing = unique.get(key);
        if (existing == null) {
            Candidate c = new Candidate();
            c.remote = r;
            c.code = code;
            c.label = label;
            unique.put(key, c);
        } else if (r.buttons.size() > existing.remote.buttons.size()) {
            existing.remote = r;
            existing.code = code;
            existing.label = label;
        }
    }

    private void tickAutoFind() {
        if (!running || scanPaused) return;
        if (nextCandidate >= candidates.size()) {
            stopAutoFind("Tried all " + candidates.size() + " codes with no reaction.");
            return;
        }
        Candidate c = candidates.get(nextCandidate);
        if (irBusy.get()) {
            // A built-in emitter answers in milliseconds; a jack tone can take
            // seconds. Wait it out instead of burning a code it never saw.
            handler.postDelayed(this::tickAutoFind, 120);
            return;
        }
        nextCandidate++;
        transmit(c.code, "Air Conditioner".equals(current.category)
                && DeviceStore.acPowerRepeat(this));
        if (scannerDialog != null && scannerDialog.getWindow() != null) {
            scannerDialog.getWindow().getDecorView()
                    .performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        }
        progress.setProgress(nextCandidate);
        scanCount.setText("Scanning " + nextCandidate + " / " + candidates.size());
        scanName.setText(c.remote.brand + " \u2014 " + pretty(c.remote.name));
        recordScanHistory(c);
        handler.postDelayed(this::tickAutoFind, scanDelayMs);
    }

    private void rewindOne() {
        if (!running) return;
        int index = nextCandidate - 2;
        if (index < 0) {
            status.setText("Already at the first code.");
            return;
        }
        handler.removeCallbacksAndMessages(null);
        nextCandidate = index + 1;
        Candidate c = candidates.get(index);
        transmit(c.code, "Air Conditioner".equals(current.category)
                && DeviceStore.acPowerRepeat(this));
        progress.setProgress(nextCandidate);
        scanCount.setText("Replaying " + nextCandidate + " / " + candidates.size());
        scanName.setText(c.remote.brand + " \u2014 " + pretty(c.remote.name));
        status.setText("Replaying the previous code.");
        handler.postDelayed(this::tickAutoFind, scanDelayMs);
    }

    private void saveCandidate(int index) {
        if (!running) return;
        if (index < 0 || index >= candidates.size()) {
            status.setText("No earlier code to save.");
            return;
        }
        Candidate c = candidates.get(index);
        running = false;
        scanPaused = false;
        handler.removeCallbacksAndMessages(null);
        findButton.setEnabled(true);
        allBrands.setEnabled(true);
        brandSpinner.setEnabled(true);
        remoteSpinner.setEnabled(true);
        for (Button b : speedButtons) { b.setEnabled(true); b.setAlpha(1f); }
        dismissScanner();
        saveCurrent(c.remote);
        applyRemote(c.remote, true);
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_UP));
    }

    private void stopAutoFind(String message) {
        running = false;
        scanPaused = false;
        handler.removeCallbacksAndMessages(null);
        findButton.setEnabled(true);
        allBrands.setEnabled(true);
        brandSpinner.setEnabled(true);
        remoteSpinner.setEnabled(true);
        for (Button b : speedButtons) { b.setEnabled(true); b.setAlpha(1f); }
        dismissScanner();
        renderControls();
        status.setText(message);
    }

    // --------------------------------------------------------------- controls

    static class Page {
        String title;
        List<String> keys;

        Page(String title, List<String> keys) {
            this.title = title;
            this.keys = keys;
        }
    }

    private void renderControls() {
        controlsCard.removeAllViews();
        buttons.clear();
        controlsPager = null;
        pageDots = null;
        pageCount = 0;
        if (current == null) {
            controlsCard.addView(text("Select a remote to begin.", 13, MUTED, false));
            return;
        }
        TextView controlsLabel = text("CONTROLS", 10, MUTED, true);
        controlsLabel.setLetterSpacing(0.14f);
        controlsCard.addView(controlsLabel);

        if (current.buttons.containsKey("power")) {
            Button power = highlightButton("POWER", 20);
            power.setOnClickListener(v -> send("power"));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(76));
            lp.topMargin = dp(10);
            controlsCard.addView(power, lp);
            buttons.put("power", power);

            if ("Air Conditioner".equals(current.category)) {
                Button power2 = styledButton("POWER ×2", BTN, 14);
                power2.setOnClickListener(v -> sendPowerRepeat());
                LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
                p2.topMargin = dp(8);
                controlsCard.addView(power2, p2);
                buttons.put("__power2", power2);
                if (current.buttons.containsKey("power_off")) {
                    Button powerOff = styledButton("POWER OFF", BTN, 14);
                    powerOff.setTextColor(RED);
                    powerOff.setOnClickListener(v -> send("power_off"));
                    LinearLayout.LayoutParams po = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
                    po.topMargin = dp(8);
                    controlsCard.addView(powerOff, po);
                    buttons.put("power_off", powerOff);
                }
                controlsCard.addView(text(
                        "Some ACs use separate On/Off codes; try POWER OFF too. "
                                + "POWER ×2 helps units that ignore one frame.",
                        11, MUTED, false));
            }
        }

        List<Page> pages = buildPages();
        pageCount = pages.size();
        if (pageCount == 0) {
            String message = "Air Conditioner".equals(current.category)
                    ? "Built-in aircon profiles expose Power only. Temperature, mode and "
                    + "fan need a protocol-specific profile or an imported raw remote."
                    : "No mapped buttons for this remote.";
            controlsCard.addView(text(message, 12, MUTED, false));
            return;
        }

        if (pageCount == 1) {
            LinearLayout page = new LinearLayout(this);
            page.setOrientation(LinearLayout.VERTICAL);
            addPageView(page, pages.get(0));
            controlsCard.addView(page);
        } else {
            controlsPager = new SwipeFlipper(this);
            for (Page page : pages) {
                LinearLayout view = new LinearLayout(this);
                view.setOrientation(LinearLayout.VERTICAL);
                addPageView(view, page);
                controlsPager.addView(view, new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            controlsPager.detector = new GestureDetector(this,
                    new GestureDetector.SimpleOnGestureListener() {
                        @Override
                        public boolean onFling(MotionEvent e1, MotionEvent e2,
                                               float velocityX, float velocityY) {
                            if (e1 == null || e2 == null) return false;
                            float dx = e2.getX() - e1.getX();
                            if (Math.abs(dx) > 110 && Math.abs(velocityX) > 100) {
                                goPage(dx < 0 ? 1 : -1);
                                return true;
                            }
                            return false;
                        }
                    });
            controlsCard.addView(controlsPager, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            addPageIndicator();
            animatePager(true);
            controlsPager.setDisplayedChild(0);
            updatePageDots();
            controlsCard.addView(text("Swipe left or right for more buttons.",
                    11, MUTED, false));
        }

        if ("Air Conditioner".equals(current.category)) {
            controlsCard.addView(text(
                    "Aircon codes are included per button. Temperature/mode buttons "
                            + "may set a captured state rather than step; if your model "
                            + "is missing, its IR protocol is not in this database.",
                    11, MUTED, false));
        }
        if (hasRepeatable(pages)) {
            controlsCard.addView(text("Hold Vol / CH / Temp / Bright to repeat.",
                    11, MUTED, false));
        }
    }

    private List<Page> buildPages() {
        List<Page> pages = new ArrayList<>();
        java.util.HashSet<String> used = new java.util.HashSet<>();

        List<String> basics = new ArrayList<>();
        for (String key : categoryPrimary()) {
            if (current.buttons.containsKey(key) && used.add(key)) basics.add(key);
        }
        for (String key : BASIC_KEYS) {
            if (current.buttons.containsKey(key) && used.add(key)) basics.add(key);
        }
        addPage(pages, "BASICS", basics);

        List<String> nav = new ArrayList<>();
        boolean allDpad = true;
        for (String key : DPAD) if (!current.buttons.containsKey(key)) allDpad = false;
        if (allDpad) {
            for (String key : DPAD) {
                if (used.add(key)) nav.add(key);
            }
        }
        for (String key : NAV_EXTRA) {
            if (current.buttons.containsKey(key) && used.add(key)) nav.add(key);
        }
        addPage(pages, "NAVIGATION", nav);

        List<String> media = new ArrayList<>();
        for (String key : MEDIA_KEYS) {
            if (current.buttons.containsKey(key) && used.add(key)) media.add(key);
        }
        addPage(pages, "MEDIA", media);

        List<String> numbers = new ArrayList<>();
        for (String key : NUMBER_KEYS) {
            if (current.buttons.containsKey(key) && used.add(key)) numbers.add(key);
        }
        addPage(pages, "NUMBERS", numbers);

        List<String> colors = new ArrayList<>();
        for (String key : COLOR_KEYS) {
            if (current.buttons.containsKey(key) && used.add(key)) colors.add(key);
        }
        addPage(pages, "COLORS", colors);

        List<String> extras = new ArrayList<>();
        for (String key : BUTTON_ORDER) {
            if (key.equals("power")) continue;
            if (current.buttons.containsKey(key) && used.add(key)) extras.add(key);
        }
        addPage(pages, "EXTRAS", extras);
        return pages;
    }

    private void addPage(List<Page> pages, String title, List<String> keys) {
        if (keys.isEmpty()) return;
        int chunks = (keys.size() + 11) / 12;
        for (int c = 0; c < chunks; c++) {
            int from = c * 12;
            int to = Math.min(keys.size(), from + 12);
            String pageTitle = chunks == 1 ? title : title + " " + (c + 1) + "/" + chunks;
            pages.add(new Page(pageTitle, new ArrayList<>(keys.subList(from, to))));
        }
    }

    private String[] categoryPrimary() {
        String name = current.category;
        if ("Air Conditioner".equals(name)) {
            return new String[]{"temp_up", "temp_down", "mode", "fan", "swing", "light", "timer", "sleep"};
        }
        if ("Fan".equals(name)) {
            return new String[]{"speed", "swing", "light", "timer", "mode", "fan"};
        }
        if ("LED Light".equals(name)) {
            return new String[]{"brightness_up", "brightness_down", "color", "mode",
                    "speed", "light", "timer"};
        }
        if ("Heater".equals(name) || "Fireplace".equals(name)) {
            return new String[]{"temp_up", "temp_down", "mode", "swing", "light", "timer"};
        }
        if ("Humidifier".equals(name) || "Air Purifier".equals(name)) {
            return new String[]{"speed", "mode", "light", "timer"};
        }
        if ("Projector".equals(name)) {
            return new String[]{"vol_up", "vol_down", "mute", "input", "menu", "zoom_in", "zoom_out"};
        }
        if ("Soundbar".equals(name) || "Audio / Receiver".equals(name)
                || "Speaker".equals(name)) {
            return new String[]{"vol_up", "vol_down", "mute", "input", "play_pause"};
        }
        return new String[]{"vol_up", "vol_down", "mute", "ch_up", "ch_down", "input"};
    }

    private void addPageView(LinearLayout parent, Page page) {
        TextView title = text(page.title, 10, ACCENT, true);
        title.setLetterSpacing(0.12f);
        title.setPadding(0, dp(6), 0, dp(4));
        parent.addView(title);
        boolean isNav = page.title.startsWith("NAVIGATION");
        List<String> rest = new ArrayList<>();
        if (isNav) {
            boolean hasDpad = true;
            for (String key : DPAD) if (!page.keys.contains(key)) hasDpad = false;
            if (hasDpad) {
                addRow(parent, null, "up", null);
                addRow(parent, "left", "ok", "right");
                addRow(parent, null, "down", null);
                for (String key : page.keys) if (!isDpad(key)) rest.add(key);
            } else {
                rest.addAll(page.keys);
            }
        } else {
            rest.addAll(page.keys);
        }
        addGrid(parent, rest);
    }

    private void addGrid(LinearLayout parent, List<String> keys) {
        for (int i = 0; i < keys.size(); i += 3) {
            String a = keys.get(i);
            String b = i + 1 < keys.size() ? keys.get(i + 1) : null;
            String c = i + 2 < keys.size() ? keys.get(i + 2) : null;
            addRow(parent, a, b, c);
        }
    }

    private boolean hasRepeatable(List<Page> pages) {
        for (Page page : pages) {
            for (String key : page.keys) if (isRepeatable(key)) return true;
        }
        return false;
    }

    private void addPageIndicator() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(6), 0, dp(2));
        Button prev = styledButton("\u2039", BTN, 18);
        prev.setOnClickListener(v -> goPage(-1));
        Button next = styledButton("\u203A", BTN, 18);
        next.setOnClickListener(v -> goPage(1));
        pageDots = text("", 11, ACCENT, true);
        pageDots.setGravity(Gravity.CENTER);
        row.addView(prev, new LinearLayout.LayoutParams(dp(42), dp(38)));
        row.addView(pageDots, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(next, new LinearLayout.LayoutParams(dp(42), dp(38)));
        controlsCard.addView(row);
        buttons.put("__page_prev", prev);
        buttons.put("__page_next", next);
    }

    private void goPage(int delta) {
        if (controlsPager == null || pageCount < 2) return;
        int currentPage = controlsPager.getDisplayedChild();
        int next = (currentPage + delta + pageCount) % pageCount;
        animatePager(delta > 0);
        controlsPager.setDisplayedChild(next);
        updatePageDots();
    }

    private void animatePager(boolean forward) {
        if (controlsPager == null) return;
        Animation in = new TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, forward ? 1f : -1f,
                Animation.RELATIVE_TO_PARENT, 0f,
                Animation.RELATIVE_TO_PARENT, 0f,
                Animation.RELATIVE_TO_PARENT, 0f);
        in.setDuration(220);
        Animation out = new TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, 0f,
                Animation.RELATIVE_TO_PARENT, forward ? -1f : 1f,
                Animation.RELATIVE_TO_PARENT, 0f,
                Animation.RELATIVE_TO_PARENT, 0f);
        out.setDuration(220);
        controlsPager.setInAnimation(in);
        controlsPager.setOutAnimation(out);
    }

    private void updatePageDots() {
        if (pageDots == null || controlsPager == null) return;
        int currentPage = controlsPager.getDisplayedChild();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pageCount; i++) {
            sb.append(i == currentPage ? "\u25CF" : "\u25CB");
            if (i < pageCount - 1) sb.append("  ");
        }
        sb.append("   ").append(currentPage + 1).append("/").append(pageCount);
        pageDots.setText(sb.toString());
    }

    private boolean isDpad(String key) {
        for (String d : DPAD) if (d.equals(key)) return true;
        return false;
    }

    private static class SwipeFlipper extends ViewFlipper {
        GestureDetector detector;

        SwipeFlipper(Context context) {
            super(context);
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent ev) {
            if (detector != null) detector.onTouchEvent(ev);
            return super.dispatchTouchEvent(ev);
        }
    }

    // ------------------------------------------------------------------ more

    private void showMore() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.TRANSPARENT);
        scroll.setFillViewport(true);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(18), dp(12), dp(18), dp(24));
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                Surface.lift(CARD));
        bg.setCornerRadii(new float[]{dp(28), dp(28), dp(28), dp(28), 0, 0, 0, 0});
        bg.setStroke(dp(1), palette.rim());
        panel.setBackground(bg);
        Surface.elevate(panel, 28, 14);

        View handle = new View(this);
        GradientDrawable hb = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Surface.blend(STROKE, TEXT, 0.55f), Surface.blend(STROKE, BG, 0.3f)});
        hb.setCornerRadius(dp(3));
        handle.setBackground(hb);
        LinearLayout.LayoutParams hl = new LinearLayout.LayoutParams(dp(44), dp(5));
        hl.gravity = Gravity.CENTER_HORIZONTAL;
        hl.bottomMargin = dp(14);
        panel.addView(handle, hl);

        panel.addView(text("More", 26, TEXT, true));
        TextView sub = text("Pick a device type, manage devices, fetch remotes.",
                12, MUTED, false);
        sub.setPadding(0, dp(2), 0, dp(14));
        panel.addView(sub);

        TextView catHeader = text("CATEGORIES", 10, ACCENT, true);
        catHeader.setLetterSpacing(0.14f);
        catHeader.setPadding(0, 0, 0, dp(8));
        panel.addView(catHeader);

        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < categories.size(); i += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams chipLp = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            chipLp.setMargins(0, 0, dp(4), dp(8));
            row.addView(categoryChip(categories.get(i), dialog, i), chipLp);
            if (i + 1 < categories.size()) {
                LinearLayout.LayoutParams chipLp2 = new LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                chipLp2.setMargins(dp(4), 0, 0, dp(8));
                row.addView(categoryChip(categories.get(i + 1), dialog, i + 1), chipLp2);
            } else {
                Space spacer = new Space(this);
                LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(0, 1, 1f);
                sp.setMargins(dp(4), 0, 0, dp(8));
                row.addView(spacer, sp);
            }
            grid.addView(row);
        }
        panel.addView(grid);

        TextView toolsHeader = text("TOOLS", 10, ACCENT, true);
        toolsHeader.setLetterSpacing(0.14f);
        toolsHeader.setPadding(0, dp(10), 0, dp(8));
        panel.addView(toolsHeader);

        panel.addView(toolRow("Devices", "Name and switch saved devices", v -> {
            dialog.dismiss();
            startActivity(new Intent(this, ConfigsActivity.class));
        }));
        panel.addView(toolRow("Fetch original remote",
                "Import a captured .ir file", v -> {
                    dialog.dismiss();
                    startActivityForResult(
                            new Intent(this, LearnActivity.class), REQ_LEARN);
                }));
        panel.addView(toolRow("IR & scan settings", "Boost and scan speed", v -> {
            dialog.dismiss();
            startActivity(new Intent(this, ConfigsActivity.class));
        }));
        panel.addView(toolRow("Diagnostics", "IR status, phone and database info", v -> {
            dialog.dismiss();
            showDiagnostics();
        }));
        panel.addView(toolRow("About", "Sources, limits and how it works", v -> {
            dialog.dismiss();
            showAbout();
        }));

        TextView updateHeader = text("UPDATES", 10, ACCENT, true);
        updateHeader.setLetterSpacing(0.14f);
        updateHeader.setPadding(0, dp(10), 0, dp(8));
        panel.addView(updateHeader);
        panel.addView(toolRow("Check for updates",
                "You are on " + Updater.currentVersion() + " \u00b7 GitHub releases",
                v -> {
                    dialog.dismiss();
                    checkForUpdatesNow();
                }));
        panel.addView(switchRow("Auto update check",
                "Asks once per app open while on Wi-Fi or mobile data",
                Updater.enabled(this),
                on -> {
                    Updater.setEnabled(this, on);
                    status.setText(on ? "Update checks are on."
                            : "Update checks are off. The app makes no network calls.");
                }));

        TextView jackHeader = text("JACK BLASTER", 10, ACCENT, true);
        jackHeader.setLetterSpacing(0.14f);
        jackHeader.setPadding(0, dp(10), 0, dp(8));
        panel.addView(jackHeader);
        panel.addView(toolRow("DIY IR blaster on the 3.5mm plug",
                JackBlaster.plugPresent(this)
                        ? "Something is in the port \u00b7 2 LEDs back-to-back needed"
                        : "Port reads empty \u00b7 force it if your plug has no mic line",
                v -> {
                    dialog.dismiss();
                    showJackDialog();
                }));

        TextView themeHeader = text("APPEARANCE", 10, ACCENT, true);
        themeHeader.setLetterSpacing(0.14f);
        themeHeader.setPadding(0, dp(10), 0, dp(8));
        panel.addView(themeHeader);
        TextView themeNote = text("Ten orange-family themes. Switching one keeps every "
                + "saved device and setting.", 11, MUTED, false);
        themeNote.setPadding(0, 0, 0, dp(8));
        panel.addView(themeNote);
        panel.addView(themeGrid(dialog));

        Button close = styledButton("Close", BTN, 14);
        close.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        closeLp.topMargin = dp(10);
        panel.addView(close, closeLp);

        scroll.addView(panel);
        dialog.setContentView(scroll);
        dialog.show();
        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setGravity(Gravity.BOTTOM);
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            w.setDimAmount(0.6f);
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(w.getAttributes());
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = (int) (getResources().getDisplayMetrics().heightPixels * 0.9f);
            lp.gravity = Gravity.BOTTOM;
            w.setAttributes(lp);
        }
    }

    private View themeGrid(Dialog host) {
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < Palette.PRESETS.length; i += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams a = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            a.setMargins(0, 0, dp(4), dp(6));
            row.addView(themeCell(i, host), a);
            LinearLayout.LayoutParams b = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            b.setMargins(dp(4), 0, 0, dp(6));
            if (i + 1 < Palette.PRESETS.length) {
                row.addView(themeCell(i + 1, host), b);
            } else {
                row.addView(new Space(this), b);
            }
            grid.addView(row);
        }
        return grid;
    }

    private View themeCell(int index, Dialog host) {
        Palette p = Palette.of(index);
        boolean active = index == DeviceStore.themeIndex(this);
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.HORIZONTAL);
        cell.setGravity(Gravity.CENTER_VERTICAL);
        cell.setPadding(dp(9), dp(9), dp(9), dp(9));
        cell.setBackground(Surface.raised(this, 14, active ? ACTIVE : CARD,
                active ? p.accent : STROKE, palette.gloss(active ? 0.18f : 0.08f)));
        Surface.elevate(cell, 14, active ? 4 : 2);

        View swatch = new View(this);
        swatch.setBackground(miniUI(p));
        cell.addView(swatch, new LinearLayout.LayoutParams(dp(30), dp(26)));

        TextView name = text(p.name, 12, active ? ACCENT : TEXT, active);
        name.setSingleLine(true);
        name.setPadding(dp(9), 0, 0, 0);
        cell.addView(name, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        cell.setOnClickListener(v -> {
            if (DeviceStore.themeIndex(this) == index) {
                host.dismiss();
                return;
            }
            DeviceStore.setThemeIndex(this, index);
            host.dismiss();
            recreate();
        });
        Surface.sinkOnPress(cell, active ? 4 : 2);
        return cell;
    }

    /** A three-line mock of the remote screen wearing the theme it is offering. */
    private LayerDrawable miniUI(Palette p) {
        GradientDrawable sheet = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                Surface.lift(p.card));
        sheet.setCornerRadius(dp(7));
        sheet.setStroke(Math.max(1, dp(1)), p.stroke);

        GradientDrawable bar = new GradientDrawable();
        bar.setColor(p.accent);
        bar.setCornerRadius(dp(2));

        GradientDrawable line = new GradientDrawable();
        line.setColor(p.muted);
        line.setCornerRadius(dp(1));

        GradientDrawable dot = new GradientDrawable();
        dot.setColor(p.text);
        dot.setCornerRadius(dp(1));

        LayerDrawable mini = new LayerDrawable(
                new Drawable[]{sheet, bar, line, dot});
        int g = dp(4);
        mini.setLayerInset(1, g, dp(15), g, dp(5));
        mini.setLayerInset(2, g, g, dp(14), dp(17));
        mini.setLayerInset(3, dp(20), g, g, dp(17));
        return mini;
    }

    private View categoryChip(Category c, Dialog dialog, int index) {
        LinearLayout chip = new LinearLayout(this);
        chip.setOrientation(LinearLayout.VERTICAL);
        chip.setPadding(dp(12), dp(11), dp(12), dp(11));
        boolean active = c.name.equals(category.name);
        chip.setBackground(Surface.raised(this, 16, active ? ACTIVE : CARD,
                active ? ACCENT : STROKE, palette.gloss(active ? 0.20f : 0.09f)));
        Surface.elevate(chip, 16, active ? 5 : 2);
        TextView name = text(c.name, 13, active ? ACCENT : TEXT, true);
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        chip.addView(name);
        TextView count = text(c.remoteCount + " codes", 10, MUTED, false);
        count.setPadding(0, dp(2), 0, 0);
        chip.addView(count);
        chip.setOnClickListener(v -> {
            dialog.dismiss();
            switchCategory(index);
        });
        return chip;
    }

    private View toolRow(String title, String subtitle, View.OnClickListener click) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        row.setBackground(Surface.raised(this, 16, BTN, STROKE, palette.gloss(0.11f)));
        Surface.elevate(row, 16, 3);
        Surface.sinkOnPress(row, 3);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(8);
        row.setLayoutParams(lp);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.addView(text(title, 14, TEXT, true));
        texts.addView(text(subtitle, 11, MUTED, false));
        row.addView(texts, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView chevron = text("›", 24, MUTED, false);
        row.addView(chevron);
        row.setOnClickListener(click);
        return row;
    }

    private interface ToggleHandler {
        void onChange(boolean value);
    }

    private View switchRow(String title, String subtitle, boolean on,
                           final ToggleHandler onChange) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        row.setBackground(Surface.raised(this, 16, BTN, STROKE, palette.gloss(0.11f)));
        Surface.elevate(row, 16, 3);
        Surface.sinkOnPress(row, 3);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(8);
        row.setLayoutParams(lp);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.addView(text(title, 14, TEXT, true));
        texts.addView(text(subtitle, 11, MUTED, false));
        row.addView(texts, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        CheckBox box = new CheckBox(this);
        box.setChecked(on);
        box.setClickable(false);
        box.setFocusable(false);
        box.setButtonTintList(ColorStateList.valueOf(ACCENT));
        row.addView(box);
        row.setOnClickListener(v -> {
            boolean next = !box.isChecked();
            box.setChecked(next);
            onChange.onChange(next);
        });
        return row;
    }

    // ---------------------------------------------------------------- updates

    /** One quiet check per app open. The dialog only appears when something newer exists. */
    private void maybeCheckForUpdate() {
        if (updateChecked || !Updater.enabled(this)) return;
        updateChecked = true;
        Updater.check(this, false, (release, error) -> {
            if (release == null) return;
            if (!Updater.isNewer(release.version, Updater.currentVersion())) return;
            if (release.version.equals(Updater.skippedVersion(this))) return;
            showUpdateDialog(release);
        });
    }

    private void checkForUpdatesNow() {
        Updater.clearSkipped(this);
        status.setText("Checking GitHub for a newer version\u2026");
        Updater.check(this, true, (release, error) -> {
            if (error != null) {
                status.setText("Update check failed: " + error);
                return;
            }
            if (release == null
                    || !Updater.isNewer(release.version, Updater.currentVersion())) {
                status.setText("You are on the newest version ("
                        + Updater.currentVersion() + ").");
                return;
            }
            showUpdateDialog(release);
        });
    }

    private void showUpdateDialog(Updater.Release release) {
        StringBuilder message = new StringBuilder("Version ").append(release.version)
                .append(" is out. You are on ").append(Updater.currentVersion())
                .append(".\n\nDownloading from GitHub needs a browser; the app never "
                        + "installs anything by itself.");
        String notes = release.notes == null ? "" : release.notes.trim();
        if (!notes.isEmpty()) {
            if (notes.length() > 700) notes = notes.substring(0, 700) + "\u2026";
            message.append("\n\n").append(notes);
        }
        final String link = release.apkUrl != null && !release.apkUrl.isEmpty()
                ? release.apkUrl : release.pageUrl;
        new AlertDialog.Builder(this)
                .setTitle("Update available")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Download", (d, w) -> openLink(link))
                .setNeutralButton("Skip this version",
                        (d, w) -> Updater.skipVersion(this, release.version))
                .setNegativeButton("Not now", null)
                .show();
    }

    private void openLink(String url) {
        if (url == null || url.isEmpty()) return;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            status.setText("No app on this phone can open that link.");
        }
    }

    private void switchCategory(int index) {
        if (index < 0 || index >= categories.size()) return;
        category = categories.get(index);
        brands = category.brands;
        current = null;
        adjusting = true;
        rebuildBrandSpinner();
        adjusting = false;
        updateCategoryLabel();
        if (!brands.isEmpty()) {
            populateRemotes(0);
            if (current != null) saveCurrent(current);
        }
        updateDeviceLabels();
    }

    private void appendCustomRemotes() {
        JSONArray arr = CustomStore.list(this);
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            String catName = o.optString("category");
            int ci = findCategory(catName);
            if (ci < 0) continue;
            Category cat = categories.get(ci);
            Brand learned = null;
            for (Brand b : cat.brands) {
                if (b.name.equals("Learned")) learned = b;
            }
            String name = o.optString("name", "Learned remote");
            if (learned != null) {
                boolean exists = false;
                for (Remote r : learned.remotes) if (r.name.equals(name)) exists = true;
                if (exists) continue;
            }
            Remote remote = new Remote();
            remote.category = cat.name;
            remote.brand = learned.name;
            remote.name = name;
            JSONObject btns = o.optJSONObject("buttons");
            if (btns == null) continue;
            for (String key : BUTTON_ORDER) {
                if (!btns.has(key)) continue;
                try {
                    JSONArray arrCode = btns.getJSONArray(key);
                    Code code = new Code();
                    code.freq = arrCode.getInt(0);
                    JSONArray pat = arrCode.getJSONArray(1);
                    code.pattern = new int[pat.length()];
                    for (int p = 0; p < pat.length(); p++) code.pattern[p] = pat.getInt(p);
                    remote.buttons.put(key, code);
                } catch (Exception ignored) {
                }
            }
            if (!remote.buttons.containsKey("power")) continue;
            if (learned == null) {
                learned = new Brand();
                learned.name = "Learned";
                cat.brands.add(learned);
            }
            learned.remotes.add(remote);
            cat.remoteCount++;
        }
    }

    private void removeCustomBrands() {
        for (Category c : categories) {
            for (int i = c.brands.size() - 1; i >= 0; i--) {
                if (c.brands.get(i).name.equals("Learned")) c.brands.remove(i);
            }
            int n = 0;
            for (Brand b : c.brands) n += b.remotes.size();
            c.remoteCount = n;
        }
    }

    private void reloadCustomRemotes() {
        boolean wasLearned = current != null && current.brand.equals("Learned");
        removeCustomBrands();
        appendCustomRemotes();
        if (wasLearned) current = null;
        adjusting = true;
        rebuildBrandSpinner();
        adjusting = false;
        updateCategoryLabel();
        if (current == null && !brands.isEmpty()) populateRemotes(0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_LEARN || resultCode != RESULT_OK || data == null) return;
        reloadCustomRemotes();
        String catName = data.getStringExtra("category");
        String name = data.getStringExtra("name");
        int ci = findCategory(catName == null ? "" : catName);
        if (ci >= 0 && !categories.get(ci).name.equals(category.name)) {
            category = categories.get(ci);
            brands = category.brands;
            adjusting = true;
            rebuildBrandSpinner();
            adjusting = false;
            updateCategoryLabel();
        }
        int bi = brandIndex("Learned");
        if (bi >= 0) {
            Brand learned = brands.get(bi);
            Remote found = null;
            for (Remote r : learned.remotes) {
                if (r.name.equals(name)) found = r;
            }
            adjusting = true;
            brandSpinner.setSelection(bi);
            if (found != null) setRemoteAdapter(learned, found);
            adjusting = false;
            if (found != null) {
                selectRemote(found);
                scrollView.post(() -> scrollView.fullScroll(View.FOCUS_UP));
            }
        }
        status.setText("Imported “" + name + "”.");
    }

    private String jackVerdict;

    /** Rebuilt wholesale so the shaded button styling survives a change of level. */
    private void buildDriveRow(LinearLayout row, TextView readout, TextView verdict) {
        row.removeAllViews();
        int chosen = JackBlaster.gainPct(this);
        int[] levels = {25, 50, 75, 100};
        for (int i = 0; i < levels.length; i++) {
            final int level = levels[i];
            Button b = styledButton(level + "%", level == chosen ? ACTIVE : BTN, 13);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(46), 1f);
            lp.setMargins(dp(i == 0 ? 0 : 3), 0, dp(3), 0);
            b.setOnClickListener(v -> {
                JackBlaster.setGainPct(this, level);
                buildDriveRow(row, readout, verdict);
                updateJackReadout(readout, verdict);
            });
            row.addView(b, lp);
        }
    }

    private void updateJackReadout(TextView out, TextView verdict) {
        String state = JackBlaster.describe(this);
        if (!JackBlaster.enabled(this)) {
            state += "\nSending: jack mode is OFF \u2014 the phone still uses its own emitter.";
        } else if (emitterState() == EMITTER_BUILT) {
            state += "\nSending: built-in emitter (jack mode is on but the port is empty).";
        } else {
            state += "\nSending: through the jack.";
        }
        int volume = JackBlaster.mediaVolumePct(this);
        if (volume < 100) {
            state += "\n\u26a0 Media volume is at " + volume + "%. The drive level is capped by it.";
        }
        out.setText(state);

        if (jackVerdict == null) {
            verdict.setText("Never measured. Run the test tone and this line will "
                    + "say whether the sound actually reached the port.");
            verdict.setTextColor(MUTED);
        } else if (jackVerdict.startsWith("OK")) {
            verdict.setText(jackVerdict);
            verdict.setTextColor(ACCENT);
        } else {
            verdict.setText(jackVerdict);
            verdict.setTextColor(RED);
        }
    }

    private void showJackDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(24), dp(52), dp(24), dp(28));
        scroll.addView(panel);

        TextView kicker = text("DIY BLASTER", 10, ACCENT, true);
        kicker.setLetterSpacing(0.16f);
        panel.addView(kicker);
        TextView headline = text("Send IR through the jack", 28, TEXT, true);
        headline.setPadding(0, dp(8), 0, dp(6));
        panel.addView(headline);
        panel.addView(text("A 48 kHz audio output cannot make a 38 kHz tone \u2014 its "
                + "ceiling is 24 kHz. So this sends 19 kHz and lets the hardware do the "
                + "doubling: two IR LEDs wired back-to-back, each lighting on the half of "
                + "the wave the other blocks, which is 38 kHz of light to a TV.",
                12, MUTED, false));

        final LinearLayout live = card();
        live.addView(text("STATUS", 10, ACCENT, true));
        final TextView readout = text("", 12, TEXT, false);
        readout.setPadding(0, dp(6), 0, 0);
        live.addView(readout);
        final TextView verdict = text("", 12, ACCENT, true);
        verdict.setPadding(0, dp(10), 0, 0);
        live.addView(verdict);
        addCard(panel, live);

        LinearLayout testRow = new LinearLayout(this);
        testRow.setOrientation(LinearLayout.HORIZONTAL);
        Button tone = styledButton("Test tone 1.2 s", BTN, 13);
        Button power = styledButton("Send POWER", BTN, 13);
        LinearLayout.LayoutParams half = new LinearLayout.LayoutParams(0, dp(50), 1f);
        half.rightMargin = dp(4);
        LinearLayout.LayoutParams other = new LinearLayout.LayoutParams(0, dp(50), 1f);
        other.leftMargin = dp(4);
        testRow.addView(tone, half);
        testRow.addView(power, other);
        LinearLayout.LayoutParams testLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        testLp.bottomMargin = dp(10);
        panel.addView(testRow, testLp);

        Runnable emit = () -> {
            if (irExecutor.isShutdown() || !irBusy.compareAndSet(false, true)) return;
            tone.setEnabled(false);
            power.setEnabled(false);
            try {
                irExecutor.execute(() -> {
                    JackBlaster.Result r = JackBlaster.testTone(this, 1200);
                    jackVerdict = r.ok ? "OK \u2014 measured route: " + r.routed
                            + (r.detail == null ? "" : " \u00b7 " + r.detail)
                            : "FAILED \u2014 " + (r.detail == null ? r.routed : r.detail);
                    irBusy.set(false);
                    runOnUiThread(() -> {
                        tone.setEnabled(true);
                        power.setEnabled(true);
                        updateJackReadout(readout, verdict);
                        refreshIrChip();
                    });
                });
            } catch (RejectedExecutionException e) {
                irBusy.set(false);
                tone.setEnabled(true);
                power.setEnabled(true);
            }
        };
        tone.setOnClickListener(v -> emit.run());
        power.setOnClickListener(v -> {
            Code code = null;
            if (current != null) {
                code = current.buttons.get("power");
                if (code == null && !current.buttons.isEmpty()) {
                    code = current.buttons.values().iterator().next();
                }
            }
            if (code == null) {
                status.setText("Pick a remote first, then press Send POWER.");
                return;
            }
            transmit(code, 2);
            status.setText("Sent a code through the jack \u2014 watch the status chip.");
        });

        panel.addView(switchRow("Send through the jack",
                "Use the DIY blaster instead of the phone's own emitter",
                JackBlaster.enabled(this),
                on -> {
                    JackBlaster.setEnabled(this, on);
                    refreshIrChip();
                    updateJackReadout(readout, verdict);
                }));
        panel.addView(switchRow("Force, even if the port reads empty",
                "A plug with only LEDs may not trip the jack sensor",
                JackBlaster.forced(this),
                on -> {
                    JackBlaster.setForced(this, on);
                    refreshIrChip();
                    updateJackReadout(readout, verdict);
                }));
        panel.addView(switchRow("Opposite-phase stereo",
                "Works for both wirings and doubles the voltage across tip and ring",
                JackBlaster.invertedStereo(this),
                on -> {
                    JackBlaster.setInvertedStereo(this, on);
                    updateJackReadout(readout, verdict);
                }));

        TextView driveHeader = text("DRIVE LEVEL", 10, ACCENT, true);
        driveHeader.setLetterSpacing(0.14f);
        driveHeader.setPadding(0, dp(12), 0, dp(8));
        panel.addView(driveHeader);
        LinearLayout driveRow = new LinearLayout(this);
        driveRow.setOrientation(LinearLayout.HORIZONTAL);
        buildDriveRow(driveRow, readout, verdict);
        panel.addView(driveRow);
        panel.addView(text("Start at 50% if your blaster has no resistor. Full drive "
                + "pushes tens of milliamps through the phone's audio chip.",
                11, MUTED, false));

        TextView gearHeader = text("WHAT YOUR BLASTER NEEDS", 10, ACCENT, true);
        gearHeader.setLetterSpacing(0.14f);
        gearHeader.setPadding(0, dp(14), 0, dp(8));
        panel.addView(gearHeader);
        panel.addView(text("\u2022 Two IR LEDs wired back-to-back, not one. A single LED "
                + "only lights on half the wave, so it emits 19 kHz and the TV ignores it.\n"
                + "\u2022 A 47\u2013100 \u03a9 resistor in series. Without one the audio "
                + "amplifier is shorted to about 1.3 V.\n"
                + "\u2022 Media volume at 100%. The tone cannot be made louder than the "
                + "phone's own volume allows.\n"
                + "\u2022 Expect 1\u20132 m, pointed straight at the receiver. This is a "
                + "weak blaster, not a stronger one.\n"
                + "\u2022 The phone must wait for each tone to finish, so a scan of long "
                + "air-conditioner codes runs slower than the built-in emitter.",
                12, MUTED, false));

        Button close = styledButton("Close", BTN, 14);
        close.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        closeLp.topMargin = dp(14);
        panel.addView(close, closeLp);

        dialog.setOnDismissListener(d -> refreshIrChip());
        dialog.setContentView(scroll);
        dialog.show();
        updateJackReadout(readout, verdict);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(BG));
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT);
        }
    }

    private void showDiagnostics() {
        boolean ready = ir != null && ir.hasIrEmitter();
        boolean override = DeviceStore.irOverride(this);
        int source = emitterState();
        int remotes = 0;
        for (Category c : categories) remotes += c.remoteCount;
        String currentText = current == null ? "none"
                : current.category + " • " + current.brand + " — "
                + pretty(current.name);
        String sending = source == EMITTER_JACK ? "the jack blaster"
                : source == EMITTER_BUILT ? "the built-in emitter"
                : "nothing (no emitter" + (override ? ", override on)" : ")");
        String message = "IR emitter: " + (ready ? "READY" : "NOT AVAILABLE") + "\n"
                + "Sending by: " + sending + "\n"
                + "Phone: " + Build.MANUFACTURER + " " + Build.MODEL + "\n"
                + "Android: " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")\n\n"
                + "Database: " + categories.size() + " categories, " + remotes + " remotes\n"
                + "App: IR Voider " + Updater.currentVersion()
                + " (build " + BuildConfig.VERSION_CODE + ")\n"
                + "Update check: " + (Updater.enabled(this) ? "on" : "off")
                + (Updater.knownLatest(this) == null ? ""
                        : " \u00b7 latest seen " + Updater.knownLatest(this)) + "\n"
                + "Current: " + currentText + "\n"
                + (JackBlaster.enabled(this)
                        ? "\nJack blaster\n" + JackBlaster.describe(this) + "\n" : "\n")
                + "Tips:\n"
                + "\u2022 Point the emitter straight at the device receiver: the top "
                + "edge, or the plug if you send through the jack.\n"
                + "\u2022 Try 0.5\u20131 m, then move back.\n"
                + "\u2022 Enable Force mode for 3x repeats with 25 ms gaps.\n"
                + "\u2022 ACs often need Repeat aircon POWER; protocols vary by model.\n"
                + "\u2022 Fans/LEDs that use RF cannot be controlled by a phone.";
        new AlertDialog.Builder(this)
                .setTitle("Diagnostics")
                .setMessage(message)
                .setPositiveButton("Close", null)
                .show();
    }

    private void showAbout() {
        new AlertDialog.Builder(this)
                .setTitle("About")
                .setMessage("IR Voider " + Updater.currentVersion()
                        + " \u2014 IR only. No account, no ads, no tracking.\n\n"
                        + "Codes from Flipper-IRDB (CC0). Protocol encoding based on "
                        + "Flipper Zero firmware and IRremoteESP8266.\n\n"
                        + "The phone's IR LED power cannot be amplified in software. "
                        + "Boost repeats frames to improve reliability.\n\n"
                        + "The internet permission is used only by the optional update "
                        + "check in More \u2192 Updates. With it switched off this app "
                        + "never opens a connection.")
                .setPositiveButton("Close", null)
                .show();
    }

    // -------------------------------------------------------------- lifecycle

    @Override
    protected void onResume() {
        super.onResume();
        if (!categories.isEmpty()) syncFromPrefs();
        if (jackPlug == null) {
            jackPlug = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    refreshIrChip();
                }
            };
            // A system-only broadcast, so no export flag is needed for it.
            registerReceiver(jackPlug,
                    new IntentFilter(android.media.AudioManager.ACTION_HEADSET_PLUG));
        }
        refreshIrChip();
    }

    @Override
    protected void onPause() {
        repeatKey = null;
        handler.removeCallbacks(repeatRunnable);
        if (jackPlug != null) {
            unregisterReceiver(jackPlug);
            jackPlug = null;
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        running = false;
        repeatKey = null;
        handler.removeCallbacksAndMessages(null);
        dismissScanner();
        irExecutor.shutdownNow();
        super.onDestroy();
    }

    private String pretty(String name) {
        return name.replace('_', ' ');
    }

    private abstract static class SimpleItemListener
            implements android.widget.AdapterView.OnItemSelectedListener {
        public abstract void onItemSelected(int position);

        @Override
        public final void onItemSelected(android.widget.AdapterView<?> parent,
                                         View view, int position, long id) {
            onItemSelected(position);
        }

        @Override
        public final void onNothingSelected(android.widget.AdapterView<?> parent) {
        }
    }
}
