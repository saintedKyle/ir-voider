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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.hardware.ConsumerIrManager;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
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
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.widget.ViewFlipper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
        brandAccent = ACCENT;
    }

    private static final String[] BUTTON_ORDER = {
            "power", "power_off", "vol_up", "vol_down", "mute", "ch_up", "ch_down",
            "input", "up", "down", "left", "right", "ok", "back", "home", "menu",
            "exit", "info", "guide", "play_pause", "stop", "rewind", "forward",
            "prev", "next", "record", "temp_up", "temp_down", "mode", "fan", "swing",
            "speed", "light", "timer", "sleep", "brightness_up", "brightness_down",
            "color", "red", "green", "yellow", "blue", "zoom_in", "zoom_out",
            "subtitle", "audio", "aspect", "last", "fav", "apps", "netflix",
            "youtube", "prime", "disney", "hbo", "spotify", "browser", "copilot", "web",
            "num_0", "num_1", "num_2", "num_3", "num_4", "num_5", "num_6", "num_7",
            "num_8", "num_9", "dash", "dot",
            "letter_a", "letter_b", "letter_c", "letter_d", "letter_e", "letter_f", "letter_g", "letter_h", "letter_i", "letter_j", "letter_k", "letter_l", "letter_m",
            "letter_n", "letter_o", "letter_p", "letter_q", "letter_r", "letter_s", "letter_t", "letter_u", "letter_v", "letter_w", "letter_x", "letter_y", "letter_z",
            "space", "backspace", "enter"
    };
    private static final String[] BUTTON_LABELS = {
            "POWER", "Power Off", "Vol +", "Vol −", "Mute", "CH +", "CH −",
            "Input", "▲", "▼", "◀", "▶", "OK", "Back", "Home",
            "Menu", "Exit", "Info", "Guide", "Play/Pause", "Stop", "Rewind", "Forward",
            "Prev", "Next", "Record", "Temp +", "Temp −", "Mode", "Fan", "Swing",
            "Speed", "Light", "Timer", "Sleep", "Bright +", "Bright −", "Color",
            "Red", "Green", "Yellow", "Blue", "Zoom +", "Zoom −", "Subtitle",
            "Audio", "Aspect", "Last", "Fav", "Apps", "Netflix",
            "YouTube", "Prime Video", "Disney+", "HBO Max", "Spotify", "Browser",
            "Copilot", "Web",
            "0", "1", "2", "3",
            "4", "5", "6", "7", "8", "9", "Dash", "Dot",
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z",
            "Space", "Backspace", "Enter"
    };
    private static final String[] DPAD = {"up", "down", "left", "right", "ok"};
    private static final String[] KEYBOARD_KEYS = {
            "letter_a", "letter_b", "letter_c", "letter_d", "letter_e", "letter_f",
            "letter_g", "letter_h", "letter_i", "letter_j", "letter_k", "letter_l",
            "letter_m", "letter_n", "letter_o", "letter_p", "letter_q", "letter_r",
            "letter_s", "letter_t", "letter_u", "letter_v", "letter_w", "letter_x",
            "letter_y", "letter_z", "space", "backspace", "enter"
    };
    private static final String[][] PAIRS = {
            {"vol_up", "vol_down"}, {"ch_up", "ch_down"}, {"temp_up", "temp_down"},
            {"brightness_up", "brightness_down"}, {"zoom_in", "zoom_out"},
            {"rewind", "forward"}, {"prev", "next"}
    };
    private static final int[] SCAN_SPEEDS = {200, 300, 500, 1000, 2000};
    private static final String[] SPEED_LABELS = {"0.2 s", "0.3 s", "0.5 s", "1 s", "2 s"};
    private static final int TEXT_GAP_MS = 170;
    private static final String[] BASIC_KEYS = {"vol_up", "vol_down", "mute", "ch_up", "ch_down", "input"};
    private static final String[] NAV_EXTRA = {"back", "home", "menu", "exit", "info", "guide"};
    private static final String[] MEDIA_KEYS = {"play_pause", "stop", "rewind", "forward",
            "prev", "next", "record", "subtitle", "audio", "aspect", "last", "fav"};
    private static final String[] NUMBER_KEYS = {"num_1", "num_2", "num_3", "num_4",
            "num_5", "num_6", "num_7", "num_8", "num_9", "num_0", "dash", "dot"};
    private static final String[] COLOR_KEYS = {"red", "green", "yellow", "blue"};
    private static final String[] APP_KEYS = {"netflix", "youtube", "prime", "disney",
            "hbo", "spotify", "apps", "browser"};
    private static final int REQ_LEARN = 77;

    static class Code {
        int freq;
        int[] pattern;
    }

    static class Remote {
        String category;
        String brand;
        String name;
        boolean unverified;
        final Map<String, Code> buttons = new LinkedHashMap<>();
    }

    static class Brand {
        String name;
        final List<Remote> remotes = new ArrayList<>();
    }

    static class Category {
        String name;
        final List<String> files = new ArrayList<>();
        final List<Brand> brands = new ArrayList<>();
        boolean loaded;
        int baseCount;
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
    /** Taps already queued for the emitter. Rapid presses line up instead of being dropped. */
    private final AtomicInteger irInFlight = new AtomicInteger(0);

    private final List<Category> categories = new ArrayList<>();
    private final List<Candidate> candidates = new ArrayList<>();
    private final Map<String, Button> buttons = new LinkedHashMap<>();
    private Category category;
    private List<Brand> brands = new ArrayList<>();
    private boolean loadingCodes;
    private int pendingCategory = -1;
    private Runnable pendingAfter;
    private int dbRemotes;
    private int dbCodes;
    private int dbUnverified;

    private ScrollView scrollView;
    private LinearLayout controlsCard;
    private SwipeFlipper controlsPager;
    private TextView pageDots;
    private int pageCount;
    private final List<String> pageTitles = new ArrayList<>();
    private Spinner brandSpinner;
    private Spinner remoteSpinner;
    private final Button[] speedButtons = new Button[5];
    private Spinner rangeFromSpinner;
    private Spinner rangeToSpinner;
    private TextView rangeCount;
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
    private int scanDelayMs = 200;
    private String repeatKey;
    private TextView scanTimer;
    private long scanStartedAt;
    private long pausedTotal;
    private long pauseStartedAt;
    private boolean updateChecked;
    private BroadcastReceiver jackPlug;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());

    private LinearLayout macroContent;
    private Dialog macroDialog;
    private final AtomicBoolean macroCancelled = new AtomicBoolean(false);
    private final AtomicBoolean stormCancelled = new AtomicBoolean(false);
    private Dialog stormDialog;
    private int brandAccent = 0;
    private final Random stormRandom = new Random();

    private TextView retroBarView, retroStatusView, retroLogView, retroCursorView;
    private long retroStart;
    private int retroPercent;
    private Runnable retroFinish;
    private MediaPlayer shutdownPlayer;
    private int shutdownGeneration;
    private Runnable shutdownFallback;

    private static final String[] RETRO_LINES = {
            "MOUNTING REMOTE TAPES...",
            "READING 7,612 REMOTES...",
            "INDEXING 131,411 CODES...",
            "CALIBRATING 38 kHz CARRIER...",
            "INSERT COIN TO CONTINUE"
    };

    /** Fake CRT progress: races to 94%, then waits for the real load. */
    private final Runnable retroTick = new Runnable() {
        @Override
        public void run() {
            if (retroBarView == null) return;
            long elapsed = SystemClock.uptimeMillis() - retroStart;
            int next = (int) Math.min(94, 6 + elapsed / 18);
            if (next > retroPercent) retroPercent = next;
            retroBarView.setText(retroBar());
            int phase = (int) ((elapsed / 800) % RETRO_LINES.length);
            if (retroStatusView != null) retroStatusView.setText(RETRO_LINES[phase]);
            if (retroCursorView != null) {
                retroCursorView.setVisibility((elapsed / 360) % 2 == 0
                        ? View.VISIBLE : View.INVISIBLE);
            }
            handler.postDelayed(this, 110);
        }
    };

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
        // MainActivity is neither exported nor the launcher, so the calculator is the only way
        // in. Anything else — a restore, a stale task, a stray intent — lands back on it.
        if (!Vault.isUnlocked()) {
            startActivity(new Intent(this, LockActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
            return;
        }
        // No screenshots, no screen recording, no task-switcher thumbnail of the vault.
        getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE,
                android.view.WindowManager.LayoutParams.FLAG_SECURE);
        if (Build.VERSION.SDK_INT >= 33) setRecentsScreenshotEnabled(false);

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

        showRetroLoader();

        new Thread(() -> {
            String error = null;
            try {
                loadDatabase();
            } catch (Exception e) {
                error = e.toString();
            }
            final String err = error;
            runOnUiThread(() -> finishRetroLoad(err));
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
        if (root.optInt("format", 1) != 2) {
            throw new IllegalStateException("Database asset is outdated; reinstall");
        }
        JSONObject totals = root.optJSONObject("totals");
        if (totals != null) {
            dbRemotes = totals.optInt("remotes");
            dbCodes = totals.optInt("codes");
            dbUnverified = totals.optInt("unverified");
        }
        JSONArray cats = root.getJSONArray("categories");
        for (int i = 0; i < cats.length(); i++) {
            JSONObject co = cats.getJSONObject(i);
            Category cat = new Category();
            cat.name = co.getString("name");
            cat.baseCount = co.optInt("remotes");
            cat.remoteCount = cat.baseCount;
            JSONArray files = co.optJSONArray("files");
            if (files != null) {
                for (int f = 0; f < files.length(); f++) cat.files.add(files.getString(f));
            }
            if (!cat.files.isEmpty()) categories.add(cat);
        }
        if (categories.isEmpty()) throw new IllegalStateException("No categories in database");

        int index = findCategory(DeviceStore.selectedCategory(this));
        category = categories.get(index < 0 ? 0 : index);
        brands = category.brands;
        hydrate(category);
        appendCustomRemotes();
    }

    /** Read one category's shard files. Only ever called off the main thread. */
    private void hydrate(Category cat) throws Exception {
        if (cat.loaded) return;
        for (String file : cat.files) {
            JSONObject co = new JSONObject(readAsset(file));
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
                    remote.unverified = ro.optInt("src", 0) != 0;
                    JSONObject btns = ro.getJSONObject("buttons");
                    // 1.7 lists every key the profile carries. The old fixed
                    // BUTTON_ORDER silently dropped anything added later, which
                    // is exactly how useful buttons went missing.
                    Iterator<String> keys = btns.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        try {
                            JSONArray arr = btns.getJSONArray(key);
                            Code code = new Code();
                            code.freq = arr.getInt(0);
                            JSONArray pat = arr.getJSONArray(1);
                            code.pattern = new int[pat.length()];
                            for (int p = 0; p < pat.length(); p++) {
                                code.pattern[p] = pat.getInt(p);
                            }
                            remote.buttons.put(key, code);
                        } catch (Exception ignored) {
                        }
                    }
                    if (remote.buttons.isEmpty()) continue;
                    brand.remotes.add(remote);
                }
                if (!brand.remotes.isEmpty()) cat.brands.add(brand);
            }
        }
        cat.loaded = true;
    }

    /**
     * Point the UI at a category, loading its codes first when needed. `after`
     * always runs with `brands` filled for the shown category.
     */
    private void useCategory(int index, Runnable after) {
        if (index < 0 || index >= categories.size()) {
            if (after != null) after.run();
            return;
        }
        final Category next = categories.get(index);
        if (next.loaded) {
            applyCategory(next);
            if (after != null) after.run();
            return;
        }
        if (loadingCodes) {
            // Remember the newest request; a tap during a load must not be dropped.
            pendingCategory = index;
            pendingAfter = after;
            status.setText("Loading " + next.name + " codes\u2026");
            return;
        }
        loadingCodes = true;
        status.setText("Loading " + next.name + " codes\u2026");
        new Thread(() -> {
            String error = null;
            try {
                hydrate(next);
            } catch (Exception e) {
                error = e.toString();
            }
            final String err = error;
            runOnUiThread(() -> {
                loadingCodes = false;
                if (err != null) {
                    status.setText("Could not load codes:\n" + err);
                    return;
                }
                applyCategory(next);
                if (after != null) after.run();
                int p = pendingCategory;
                Runnable pa = pendingAfter;
                pendingCategory = -1;
                pendingAfter = null;
                if (p >= 0 && p != index) useCategory(p, pa);
            });
        }).start();
    }

    private void applyCategory(Category c) {
        category = c;
        brands = c.brands;
        adjusting = true;
        rebuildBrandSpinner();
        adjusting = false;
        updateCategoryLabel();
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
        return highlightButton(label, textSize, ACCENT);
    }

    private Button highlightButton(String label, float textSize, int accent) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(textSize);
        b.setTextColor(ON_ACCENT);
        b.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        b.setStateListAnimator(null);
        Drawable plate = Surface.raised(this, 22, accent,
                Surface.darken(accent, 0.42f), palette.gloss(0.24f));
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

    // ------------------------------------------------------------ retro boot

    private TextView mono(String value, float size, int color, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(value);
        tv.setTextSize(size);
        tv.setTextColor(color);
        tv.setTypeface(Typeface.MONOSPACE, bold ? Typeface.BOLD : Typeface.NORMAL);
        tv.setLineSpacing(0f, 1.05f);
        return tv;
    }

    private String retroBar() {
        int filled = Math.max(0, Math.min(10, retroPercent / 10));
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < 10; i++) out.append(i < filled ? "\u25A0" : "\u00B7");
        out.append("] ");
        if (retroPercent >= 100) out.append("100%");
        else out.append(String.format(Locale.US, "%3d%%", retroPercent));
        return out.toString();
    }

    private void showRetroLoader() {
        retroStart = SystemClock.uptimeMillis();
        retroPercent = 0;
        // The boot screen is always dark amber, whatever theme is active.
        Palette.applyStatusIcons(getWindow(), false);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(7, 5, 4));
        root.addView(new ScanlineView(this), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setPadding(dp(26), dp(34), dp(26), dp(26));

        Space top = new Space(this);
        content.addView(top, new LinearLayout.LayoutParams(1, 0, 1f));

        TextView brand = mono("IR VOIDER", 34, ACCENT, true);
        brand.setLetterSpacing(0.20f);
        brand.setGravity(Gravity.CENTER);
        content.addView(brand);

        TextView sub = mono("INFRARED REMOTE SYSTEM // 198X", 9, MUTED, false);
        sub.setGravity(Gravity.CENTER);
        sub.setLetterSpacing(0.12f);
        sub.setPadding(0, dp(4), 0, dp(26));
        content.addView(sub);

        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setPadding(dp(16), dp(18), dp(16), dp(18));
        GradientDrawable box = new GradientDrawable();
        box.setColor(Color.rgb(12, 9, 5));
        box.setCornerRadius(dp(10));
        box.setStroke(dp(1), Surface.blend(ACCENT, BG, 0.35f));
        screen.setBackground(box);

        TextView label = mono("LOADING IR DATABASE", 10, ACCENT, true);
        label.setLetterSpacing(0.18f);
        screen.addView(label);

        retroBarView = mono(retroBar(), 17, TEXT, true);
        retroBarView.setPadding(0, dp(12), 0, dp(6));
        screen.addView(retroBarView);

        LinearLayout statusRow = new LinearLayout(this);
        statusRow.setOrientation(LinearLayout.HORIZONTAL);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);
        retroStatusView = mono(RETRO_LINES[0], 10, MUTED, false);
        statusRow.addView(retroStatusView, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        retroCursorView = mono("\u2588", 12, ACCENT, true);
        statusRow.addView(retroCursorView);
        screen.addView(statusRow);

        retroLogView = mono("> 7,612 REMOTES / 131,411 CODES\n> 2,562 BRANDS / 41 CATEGORIES",
                9, Surface.blend(MUTED, ACCENT, 0.25f), false);
        retroLogView.setPadding(0, dp(14), 0, 0);
        retroLogView.setLineSpacing(dp(2), 1f);
        screen.addView(retroLogView);

        content.addView(screen, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Space bottom = new Space(this);
        content.addView(bottom, new LinearLayout.LayoutParams(1, 0, 1f));

        TextView footer = mono("\u00A9 198X POGING KYLE  \u00B7  GPL-3.0", 8, MUTED, false);
        footer.setGravity(Gravity.CENTER);
        content.addView(footer);

        root.addView(content, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // A slow cathode beam, because a still loading screen is no fun.
        View beam = new View(this);
        GradientDrawable beamBg = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x00FFB000, 0x55FFB000, 0x00FFB000});
        beam.setBackground(beamBg);
        FrameLayout.LayoutParams beamLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(2));
        beamLp.gravity = Gravity.TOP;
        root.addView(beam, beamLp);
        TranslateAnimation sweep = new TranslateAnimation(0, 0, 0,
                getResources().getDisplayMetrics().heightPixels);
        sweep.setDuration(2800);
        sweep.setRepeatCount(Animation.INFINITE);
        sweep.setRepeatMode(Animation.RESTART);
        sweep.setInterpolator(new LinearInterpolator());
        beam.startAnimation(sweep);

        setContentView(root);
        handler.post(retroTick);
    }

    private void finishRetroLoad(String error) {
        long wait = Math.max(0, 900 - (SystemClock.uptimeMillis() - retroStart));
        retroFinish = () -> {
            handler.removeCallbacks(retroTick);
            if (isFinishing() || isDestroyed()) return;
            if (error != null) {
                if (retroBarView != null) retroBarView.setText("[//////////] ERR");
                if (retroStatusView != null) {
                    retroStatusView.setTextColor(RED);
                    retroStatusView.setText("BOOT FAILURE");
                }
                if (retroLogView != null) {
                    retroLogView.setText("Could not load remote database:\n" + error);
                }
                if (retroCursorView != null) retroCursorView.setVisibility(View.GONE);
                return;
            }
            retroPercent = 100;
            if (retroBarView != null) retroBarView.setText(retroBar());
            if (retroStatusView != null) retroStatusView.setText("READY.");
            if (retroCursorView != null) retroCursorView.setVisibility(View.VISIBLE);
            // Let the full bar and READY sit on screen for a beat.
            handler.postDelayed(() -> {
                if (isFinishing() || isDestroyed()) return;
                Palette.applyStatusIcons(getWindow(), palette.light);
                buildUi();
                maybeCheckForUpdate();
            }, 320);
        };
        handler.postDelayed(retroFinish, wait);
    }

    /** Horizontal CRT scanlines over the boot screen. */
    private static class ScanlineView extends View {
        private final Paint paint = new Paint();
        private final int step;

        ScanlineView(Context c) {
            super(c);
            paint.setColor(0x12FFB000);
            step = Math.max(2, (int) (3 * c.getResources().getDisplayMetrics().density));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int w = getWidth();
            for (int y = 0; y < getHeight(); y += step) {
                canvas.drawRect(0, y, w, y + 1, paint);
            }
        }
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
        brandRow.addView(searchButton("Search", this::showBrandSearch));
        scanCard.addView(brandRow);
        LinearLayout modelRow = spinnerRow("Model", remoteSpinner);
        modelRow.addView(searchButton("Search", this::showRemoteSearch));
        scanCard.addView(modelRow);
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
        scanCard.addView(buildRangeRow());

        LinearLayout speedRow = new LinearLayout(this);
        speedRow.setOrientation(LinearLayout.HORIZONTAL);
        speedRow.setGravity(Gravity.CENTER_VERTICAL);
        speedRow.setPadding(0, dp(4), 0, dp(4));
        TextView speedLabel = text("Speed", 14, MUTED, false);
        speedLabel.setWidth(dp(58));
        speedRow.addView(speedLabel);
        String[] speedLabels = SPEED_LABELS;
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
        setScanSpeed(speedIndexFor(scanDelayMs), false);

        findButton = highlightButton("Find code", 16);
        findButton.setOnClickListener(v -> startAutoFind());
        scanCard.addView(findButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));

        addCard(root, scanCard);

        TextView credit = text(
                "Codes: Flipper-IRDB (CC0), including its converted public remote "
                        + "dumps. Codes marked \u201cuntested source\u201d were never "
                        + "bench-checked here. IR only \u2014 no network calls unless you "
                        + "turn on update checks.",
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
            Button b = keyTile(key, labelFor(key), false, dp(76), 15);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(76), 1f);
            lp.setMargins(dp(4), dp(4), dp(4), dp(4));
            row.addView(b, lp);
        }
        parent.addView(row);
    }

    /**
     * Held buttons keep sending, but never the power family: holding a toggle
     * would strobe the device on and off.
     */
    private boolean holdable(String key) {
        return !"power".equals(key) && !"power_off".equals(key);
    }

    private void attachRepeat(Button b, String key) {
        b.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    repeatKey = key;
                    send(key);
                    handler.postDelayed(repeatRunnable, 380);
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
        index = Math.max(0, Math.min(SCAN_SPEEDS.length - 1, index));
        scanDelayMs = SCAN_SPEEDS[index];
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

    private int speedIndexFor(int delay) {
        int best = 0;
        int gap = Integer.MAX_VALUE;
        for (int i = 0; i < SCAN_SPEEDS.length; i++) {
            int d = Math.abs(SCAN_SPEEDS[i] - delay);
            if (d < gap) {
                gap = d;
                best = i;
            }
        }
        return best;
    }

    /** Brand-initial range for an all-brand scan: F to R skips the rest. */
    private LinearLayout buildRangeRow() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(4), 0, 0);
        TextView label = text("Range", 14, MUTED, false);
        label.setWidth(dp(58));
        row.addView(label);

        String[] letters = new String[26];
        for (int i = 0; i < 26; i++) letters[i] = String.valueOf((char) ('A' + i));
        rangeFromSpinner = letterSpinner(letters, DeviceStore.scanFrom(this));
        rangeToSpinner = letterSpinner(letters, DeviceStore.scanTo(this));
        row.addView(rangeFromSpinner, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView dash = text("\u2013", 14, MUTED, false);
        dash.setPadding(dp(4), 0, dp(4), 0);
        row.addView(dash);
        row.addView(rangeToSpinner, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        box.addView(row);

        rangeCount = text("", 10, MUTED, false);
        rangeCount.setPadding(dp(58), 0, 0, dp(2));
        box.addView(rangeCount);

        SimpleItemListener listener = new SimpleItemListener() {
            @Override
            public void onItemSelected(int position) {
                if (rangeFromSpinner == null || rangeToSpinner == null) return;
                DeviceStore.setScanFrom(MainActivity.this, String.valueOf(
                        (char) ('A' + rangeFromSpinner.getSelectedItemPosition())));
                DeviceStore.setScanTo(MainActivity.this, String.valueOf(
                        (char) ('A' + rangeToSpinner.getSelectedItemPosition())));
                updateRangeCount();
            }
        };
        rangeFromSpinner.setOnItemSelectedListener(listener);
        rangeToSpinner.setOnItemSelectedListener(listener);
        updateRangeCount();
        return box;
    }

    private Spinner letterSpinner(String[] letters, String selected) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, letters);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setBackgroundTintList(ColorStateList.valueOf(ACCENT));
        int index = 0;
        for (int i = 0; i < letters.length; i++) {
            if (letters[i].equals(selected)) index = i;
        }
        spinner.setSelection(index);
        return spinner;
    }

    private char rangeFrom() {
        if (rangeFromSpinner == null) return 'A';
        int p = Math.max(0, Math.min(25, rangeFromSpinner.getSelectedItemPosition()));
        return (char) ('A' + p);
    }

    private char rangeTo() {
        if (rangeToSpinner == null) return 'Z';
        int p = Math.max(0, Math.min(25, rangeToSpinner.getSelectedItemPosition()));
        return (char) ('A' + p);
    }

    private boolean brandInRange(String brand) {
        char from = rangeFrom();
        char to = rangeTo();
        if (from > to) {
            char swap = from;
            from = to;
            to = swap;
        }
        String pretty = prettyBrand(brand);
        if (pretty.isEmpty()) return false;
        char c = Character.toUpperCase(pretty.charAt(0));
        if (c < 'A' || c > 'Z') return from == 'A';
        return c >= from && c <= to;
    }

    private void updateRangeCount() {
        if (rangeCount == null) return;
        char from = rangeFrom();
        char to = rangeTo();
        if (from > to) {
            char swap = from;
            from = to;
            to = swap;
        }
        int count = 0;
        for (Brand b : brands) {
            if (!brandInRange(b.name)) continue;
            for (Remote r : b.remotes) {
                if (r.buttons.containsKey("power")) count++;
            }
        }
        rangeCount.setText(String.valueOf(from) + " \u2013 " + to + " \u00b7 " + count
                + " power codes \u00b7 used when scanning every brand");
    }

    private String labelFor(String key) {
        for (int i = 0; i < BUTTON_ORDER.length; i++) {
            if (BUTTON_ORDER[i].equals(key)) return BUTTON_LABELS[i];
        }
        if (key != null && key.startsWith("ac_temp_")) {
            return key.substring(8) + "\u00b0C";
        }
        if (key != null && key.startsWith("ac_mode_")) {
            return "Mode " + acWord(key.substring(8));
        }
        if (key != null && key.startsWith("ac_fan_")) {
            return "Fan " + acWord(key.substring(7));
        }
        String pretty = key == null ? "" : key.replace('_', ' ').trim();
        if (pretty.isEmpty()) return "";
        return Character.toUpperCase(pretty.charAt(0)) + pretty.substring(1);
    }

    private String acWord(String value) {
        if ("med".equals(value)) return "Medium";
        if (value == null || value.isEmpty()) return "";
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    /**
     * A brand tint for the plates that already carry the theme accent. It is
     * mixed with the active theme, so contrast stays inside the palette.
     */
    private int brandAccentFor(String brand) {
        int raw = brandColor(brand);
        return raw == 0 ? ACCENT : Surface.blend(ACCENT, raw, 0.42f);
    }

    private int brandColor(String brand) {
        if (brand == null) return 0;
        String b = brand.toLowerCase(Locale.US);
        if (b.startsWith("lg")) return 0xFFA50034;
        if (b.startsWith("tcl")) return 0xFFE2231A;
        if (b.startsWith("samsung")) return 0xFF1428A0;
        if (b.startsWith("sony")) return 0xFF0084C6;
        if (b.startsWith("panasonic")) return 0xFF0B4EA2;
        if (b.startsWith("philips")) return 0xFF0B5ED7;
        if (b.startsWith("hisense")) return 0xFF00A651;
        if (b.startsWith("sharp")) return 0xFFE60012;
        if (b.startsWith("toshiba")) return 0xFFE60012;
        if (b.startsWith("jvc")) return 0xFFE60012;
        if (b.startsWith("vizio")) return 0xFFE60012;
        if (b.startsWith("xiaomi") || b.startsWith("mi ")) return 0xFFFF6900;
        if (b.startsWith("huawei")) return 0xFFE60012;
        if (b.startsWith("roku")) return 0xFF6F1AB1;
        if (b.startsWith("google")) return 0xFF4285F4;
        if (b.startsWith("amazon")) return 0xFFFF9900;
        if (b.startsWith("apple")) return 0xFF8E8E93;
        if (b.startsWith("haier")) return 0xFF1B5FAA;
        if (b.startsWith("airmax") || b.startsWith("air max")) return 0xFF00A0E9;
        if (b.startsWith("midea")) return 0xFF00A3E0;
        if (b.startsWith("carrier")) return 0xFF0072CE;
        if (b.startsWith("daikin")) return 0xFF0096D6;
        if (b.startsWith("gree")) return 0xFFF39800;
        if (b.startsWith("nokia")) return 0xFF124191;
        if (b.startsWith("motorola")) return 0xFF5C2D91;
        if (b.startsWith("bose")) return 0xFFB08D57;
        if (b.startsWith("learned")) return GREEN;
        return 0;
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
        for (Brand b : brands) names.add(prettyBrand(b.name));
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
        if (!r.category.equals(category.name)) {
            useCategory(findCategory(r.category), () -> finishApplyRemote(r, saved));
            return;
        }
        finishApplyRemote(r, saved);
    }

    private void finishApplyRemote(Remote r, boolean saved) {
        adjusting = true;
        int bi = brandIndex(r.brand);
        if (bi >= 0) {
            brandSpinner.setSelection(bi);
            setRemoteAdapter(brands.get(bi), r);
        }
        installAcSet(r);
        applyPatches(r);
        current = r;
        renderControls();
        updateDeviceLabels();
        status.setText(saved ? "Saved. Press POWER to test." : codeOrigin(r));
        handler.postDelayed(() -> adjusting = false, 400);
    }

    private int brandIndex(String name) {
        for (int i = 0; i < brands.size(); i++) {
            if (brands.get(i).name.equals(name)) return i;
        }
        return -1;
    }

    private void selectRemote(Remote r) {
        installAcSet(r);
        applyPatches(r);
        current = r;
        renderControls();
        updateDeviceLabels();
        status.setText(powerDirection(r));
        if (!bootstrapping) saveCurrent(r);
    }

    private String powerDirection(Remote r) {
        if (!r.buttons.containsKey("power_off")) {
            return "Power is a toggle \u2014 the same button turns it on and off.";
        }
        return "Air Conditioner".equals(r.category)
                ? "This aircon has separate On and Off codes \u2014 each button is one "
                        + "direction."
                : "This profile also has an explicit POWER OFF code.";
    }

    private void saveCurrent(Remote r) {
        Code power = r.buttons.get("power");
        DeviceStore.rememberSelection(this, r.category, r.brand, r.name,
                power == null ? 0 : Arrays.hashCode(power.pattern));
    }

    private String macroKeyFor(Remote r) {
        if (r == null) return "";
        Code power = r.buttons.get("power");
        return DeviceStore.macroDeviceKey(r.category, r.brand, r.name,
                power == null ? 0 : Arrays.hashCode(power.pattern));
    }

    private String macroKey() {
        return current == null ? "" : macroKeyFor(current);
    }

    /** Merge buttons added from sibling profiles into the live remote. */
    private void applyPatches(Remote r) {
        if (r == null) return;
        JSONObject patch = DeviceStore.patches(this, macroKeyFor(r));
        Iterator<String> keys = patch.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (key.startsWith("__")) continue;
            try {
                JSONArray arr = patch.getJSONArray(key);
                Code code = new Code();
                code.freq = arr.getInt(0);
                JSONArray pat = arr.getJSONArray(1);
                code.pattern = new int[pat.length()];
                for (int i = 0; i < pat.length(); i++) code.pattern[i] = pat.getInt(i);
                r.buttons.put(key, code);
            } catch (Exception ignored) {
            }
        }
        mergeSiblingKeys(r);
    }

    /**
     * Fill in the buttons this model's capture never included by borrowing the same key from
     * another remote of the same brand in this category.
     *
     * <p>The database is a set of real captures from real remotes, and many entries stop at
     * Power/Volume/CH. A brand's newer sets usually do carry the missing keys, so within one brand
     * the union is the closest thing to "every button this model has" that does not invent an IR
     * code. Borrowed keys never overwrite a code the selected model already has.
     *
     * <p>Generated {@code ac_*} state frames are deliberately skipped: they are rebuilt from one
     * indoor unit's own capture and mean nothing on a different unit.
     */
    private void mergeSiblingKeys(Remote r) {
        if (r == null || r.brand == null || brands == null) return;
        for (Brand b : brands) {
            if (!b.name.equalsIgnoreCase(r.brand)) continue;
            for (Remote sibling : b.remotes) {
                if (sibling == r) continue;
                for (Map.Entry<String, Code> entry : sibling.buttons.entrySet()) {
                    String key = entry.getKey();
                    if (key.startsWith("ac_") || key.startsWith("__")) continue;
                    if (!r.buttons.containsKey(key)) r.buttons.put(key, entry.getValue());
                }
            }
        }
    }

    /**
     * AirMax and Beko only shipped a 17 °C cool frame plus Off. Both are R05D,
     * which carries mode and temperature in every full-state frame, so build
     * the missing values from that same capture. Each key is a real frame, so
     * macros and Test Storm can use it too.
     */
    private void installAcSet(Remote r) {
        if (r == null) return;
        Code power = r.buttons.get("power");
        if (!R05D.supported(power)) return;
        int[] state = DeviceStore.acState(this, macroKeyFor(r),
                R05D.celsiusFromIndex(R05D.tempIndex(power)),
                acModeIndex(R05D.modeBits(power)),
                acFanIndex(R05D.fanBits(power)));
        int temp = state[0];
        int mode = state[1];
        int fan = state[2];
        for (int celsius = 17; celsius <= 30; celsius++) {
            Code code = R05D.state(power, acFanBitsForMode(mode, fan),
                    R05D.tempIndex(celsius), acModeBits(mode), mode == 2);
            if (code != null) r.buttons.put("ac_temp_" + celsius, code);
        }
        r.buttons.put("ac_mode_cool", R05D.state(power, acFanBitsForMode(0, fan),
                R05D.tempIndex(temp), acModeBits(0), false));
        r.buttons.put("ac_mode_dry", R05D.state(power, acFanBitsForMode(1, fan),
                R05D.tempIndex(temp), acModeBits(1), false));
        r.buttons.put("ac_mode_fan", R05D.state(power, acFanBitsForMode(2, fan),
                R05D.tempIndex(temp), acModeBits(2), true));
        r.buttons.put("ac_mode_heat", R05D.state(power, acFanBitsForMode(3, fan),
                R05D.tempIndex(temp), acModeBits(3), false));
        r.buttons.put("ac_mode_auto", R05D.state(power, acFanBitsForMode(4, fan),
                R05D.tempIndex(temp), acModeBits(4), false));
        for (int i = 0; i < 4; i++) {
            String[] names = {"ac_fan_auto", "ac_fan_low", "ac_fan_med", "ac_fan_high"};
            r.buttons.put(names[i], R05D.state(power, acFanBitsForMode(mode, i),
                    R05D.tempIndex(temp), acModeBits(mode), mode == 2));
        }
    }

    private int acModeIndex(int bits) {
        if (bits == 1) return 1;
        if (bits == 2) return 4;
        if (bits == 3) return 3;
        return 0;
    }

    private int acModeBits(int index) {
        if (index == 1) return 1;
        if (index == 2) return 1;
        if (index == 3) return 3;
        if (index == 4) return 2;
        return 0;
    }

    private int acFanIndex(int bits) {
        if (bits == 5) return 0;
        if (bits == 4) return 1;
        if (bits == 2) return 2;
        if (bits == 1) return 3;
        return 0;
    }

    private int acFanBits(int index) {
        if (index == 0) return 5;
        if (index == 2) return 2;
        if (index == 3) return 1;
        return 4;
    }

    /** R05D fixes the fan in dry and auto mode; honour that in generated frames. */
    private int acFanBitsForMode(int mode, int fan) {
        if (mode == 1 || mode == 4) return 0;
        return acFanBits(fan);
    }

    /**
     * Build one absolute R05D frame from the last target this app sent, then
     * apply the tapped key. That keeps "24 °C, then Fan High" at 24 °C instead
     * of snapping back to the temperature inside the original capture.
     */
    private Code acStateCode(String key, Remote remote) {
        if (remote == null || key == null || !key.startsWith("ac_")) return null;
        Code power = remote.buttons.get("power");
        if (!R05D.supported(power)) return null;
        int[] state = DeviceStore.acState(this, macroKeyFor(remote),
                R05D.celsiusFromIndex(R05D.tempIndex(power)),
                acModeIndex(R05D.modeBits(power)),
                acFanIndex(R05D.fanBits(power)));
        int temp = state[0];
        int mode = state[1];
        int fan = state[2];
        if (key.startsWith("ac_temp_")) {
            try {
                temp = Integer.parseInt(key.substring(8));
            } catch (NumberFormatException ignored) {
                return null;
            }
            if (R05D.tempIndex(temp) < 0) return null;
        } else if (key.startsWith("ac_mode_")) {
            String value = key.substring(8);
            if ("cool".equals(value)) mode = 0;
            else if ("dry".equals(value)) mode = 1;
            else if ("fan".equals(value)) mode = 2;
            else if ("heat".equals(value)) mode = 3;
            else if ("auto".equals(value)) mode = 4;
            else return null;
        } else if (key.startsWith("ac_fan_")) {
            String value = key.substring(7);
            if ("auto".equals(value)) fan = 0;
            else if ("low".equals(value)) fan = 1;
            else if ("med".equals(value)) fan = 2;
            else if ("high".equals(value)) fan = 3;
            else return null;
        } else {
            return null;
        }
        DeviceStore.saveAcState(this, macroKeyFor(remote), temp, mode, fan);
        return R05D.state(power, acFanBitsForMode(mode, fan),
                R05D.tempIndex(temp), acModeBits(mode), mode == 2);
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
                    + prettyBrand(current.brand) + " \u2014 " + pretty(current.name));
        } else {
            deviceNameText.setText(prettyBrand(current.brand) + " \u2014 " + pretty(current.name));
            modelText.setText(current.category + " \u2022 Tap \u2699 to name this device");
        }
    }

    private static final class SearchSheet {
        final Dialog dialog;
        final EditText input;
        final LinearLayout list;

        SearchSheet(Dialog dialog, EditText input, LinearLayout list) {
            this.dialog = dialog;
            this.input = input;
            this.list = list;
        }

        void onTyped(Runnable r) {
            input.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
                @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
                @Override public void afterTextChanged(Editable s) { r.run(); }
            });
        }
    }

    /** Title, one text field, a scrolling result list and Cancel — sized for a thumb. */
    private SearchSheet buildSearchSheet(String title, String hint) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(18), dp(18), dp(18), dp(18));
        panel.setBackground(Surface.raised(this, 28, CARD, palette.rim(), palette.gloss(0.10f)));
        Surface.elevate(panel, 28, 12);

        panel.addView(text(title, 22, TEXT, true));
        EditText input = new EditText(this);
        input.setHint(hint);
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
        return new SearchSheet(dialog, input, list);
    }

    private Button searchButton(String label, Runnable onTap) {
        Button b = styledButton(label, BTN, 12);
        b.setPadding(0, 0, 0, 0);
        b.setOnClickListener(v -> onTap.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(74), dp(38));
        lp.setMargins(dp(6), 0, 0, 0);
        b.setLayoutParams(lp);
        return b;
    }

    private void showBrandSearch() {
        SearchSheet sheet = buildSearchSheet("Find a brand", "Type to filter brands");
        Runnable fill = () -> {
            sheet.list.removeAllViews();
            String q = sheet.input.getText().toString().trim().toLowerCase();
            int shown = 0;
            for (int i = 0; i < brands.size(); i++) {
                Brand b = brands.get(i);
                if (!q.isEmpty() && !prettyBrand(b.name).toLowerCase().contains(q)) continue;
                shown++;
                boolean active = current != null && b.name.equals(current.brand);
                TextView row = text(prettyBrand(b.name), 15, active ? ACCENT : TEXT, active);
                row.setPadding(dp(10), dp(13), dp(10), dp(13));
                row.setBackground(active ? Surface.raised(this, 12, ACTIVE,
                        Surface.blend(CARD, ACCENT, 0.5f), palette.gloss(0.16f)) : null);
                final int index = i;
                row.setOnClickListener(v -> {
                    brandSpinner.setSelection(index);
                    sheet.dialog.dismiss();
                });
                sheet.list.addView(row);
            }
            if (shown == 0) {
                sheet.list.addView(text("No matching brand.", 13, MUTED, false));
            }
        };
        sheet.onTyped(fill);
        fill.run();
    }

    /** Every remote in this device type, typed away in one field. */
    private void showRemoteSearch() {
        SearchSheet sheet = buildSearchSheet("Find a model",
                "Type a model number, e.g. HWE08");
        Runnable fill = () -> {
            sheet.list.removeAllViews();
            String q = sheet.input.getText().toString().trim().toLowerCase();
            int shown = 0;
            int matched = 0;
            for (int bi = 0; bi < brands.size(); bi++) {
                Brand b = brands.get(bi);
                for (int ri = 0; ri < b.remotes.size(); ri++) {
                    Remote r = b.remotes.get(ri);
                    String label = prettyBrand(b.name) + " \u2014 " + pretty(r.name);
                    if (!q.isEmpty() && !label.toLowerCase().contains(q)) continue;
                    matched++;
                    if (shown >= 200) continue;
                    shown++;
                    boolean active = r == current;
                    TextView row = text(label, 13, active ? ACCENT : TEXT, active);
                    row.setPadding(dp(10), dp(12), dp(10), dp(12));
                    row.setBackground(active ? Surface.raised(this, 12, ACTIVE,
                            Surface.blend(CARD, ACCENT, 0.5f), palette.gloss(0.16f)) : null);
                    final int brandIndex = bi;
                    final int remoteIndex = ri;
                    row.setOnClickListener(v -> {
                        pickRemote(brandIndex, remoteIndex);
                        sheet.dialog.dismiss();
                    });
                    sheet.list.addView(row);
                }
            }
            if (matched == 0) {
                sheet.list.addView(text("No matching model in this device type.",
                        13, MUTED, false));
            } else if (shown < matched) {
                sheet.list.addView(text("Showing the first 200 of " + matched
                        + " \u2014 keep typing to narrow.", 12, MUTED, false));
            }
        };
        sheet.onTyped(fill);
        fill.run();
    }

    private void pickRemote(int brandIndex, int remoteIndex) {
        if (brandIndex < 0 || brandIndex >= brands.size()) return;
        Brand b = brands.get(brandIndex);
        if (remoteIndex < 0 || remoteIndex >= b.remotes.size()) return;
        Remote r = b.remotes.get(remoteIndex);
        // applyRemote owns the spinner sync; doing it here would race the late
        // onItemSelected callback, which jumps back to the brand's first remote.
        applyRemote(r, false);
        if (current != r) return;
        status.setText(powerDirection(r));
        if (!bootstrapping) saveCurrent(r);
    }

    private void updateCategoryLabel() {
        if (categoryLabel != null && category != null) {
            categoryLabel.setText(category.name + " \u2022 " + category.remoteCount
                    + " remotes \u2022 \u2261 for more");
        }
        updateRangeCount();
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
            useCategory(ci, () -> restoreSelectedRemote(saved));
            return;
        }
        restoreSelectedRemote(saved);
    }

    private void restoreSelectedRemote(boolean saved) {
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
        if (ci >= 0 && !categories.get(ci).name.equals(category.name)) {
            useCategory(ci, this::syncFromPrefs);
            return;
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
        Code code = key.startsWith("ac_") ? acStateCode(key, current)
                : current.buttons.get(key);
        if (code == null) {
            status.setText("This remote has no \u201c" + labelFor(key) + "\u201d code.");
            return;
        }
        boolean isPower = "power".equals(key) || "power_off".equals(key);
        boolean repeat = isPower
                ? "Air Conditioner".equals(current.category)
                        && DeviceStore.acPowerRepeat(this)
                : DeviceStore.boostAll(this);
        // A Signal Lab calibration is an explicit choice, so its frame count is
        // honoured on ordinary taps too. Power stays single unless asked for x2,
        // otherwise a repeat would toggle the device straight back.
        if (!isPower && DeviceStore.calibration(this, macroKey()) != null) {
            repeat = true;
        }
        final boolean repeats = repeat;
        final int fixed = "power_off".equals(key)
                ? DeviceStore.powerOffRepeats(this, macroKey()) : 1;
        final String sent = key.startsWith("ac_")
                ? "Sent " + labelFor(key) + " \u00b7 target " + acTargetLabel() + "."
                : "Sent " + labelFor(key) + ".";
        final Runnable emit = () -> {
            if ("power_off".equals(key)) {
                if (fixed > 1) transmit(code, fixed);
                else transmit(code, repeats);
            } else {
                transmit(code, repeats);
            }
            status.setText(sent);
        };

        // A shutdown clip belongs to the TV's own Power Off button. It plays on the
        // phone first, then the IR goes out, so the picture survives the fanfare.
        // Most TV captures only carry a Power toggle, so that toggle counts too.
        boolean tvOff = "power_off".equals(key)
                || ("power".equals(key) && !current.buttons.containsKey("power_off"));
        if (tvOff && "TV".equals(current.category)
                && DeviceStore.shutdownSound(this)) {
            String sound = DeviceStore.shutdownSoundUrl(this);
            if (!sound.isEmpty()) {
                playShutdownSound(sound, emit);
                status.setText("Playing the shutdown sound, then sending Power Off\u2026");
                return;
            }
        }
        emit.run();
    }

    // --------------------------------------------------------- shutdown sound

    /**
     * Plays the chosen clip through the phone, then sends the Off code. A network
     * clip can stall or error, so watchdogs send the IR anyway instead of leaving
     * the TV on. IR itself is one-way; this only controls the order of the two.
     */
    private void playShutdownSound(String url, Runnable then) {
        cancelShutdownSound();
        final int gen = shutdownGeneration;
        final AtomicBoolean fired = new AtomicBoolean(false);
        final Runnable fire = () -> {
            if (fired.compareAndSet(false, true)) {
                handler.removeCallbacks(shutdownFallback);
                shutdownFallback = null;
                then.run();
            }
        };
        try {
            MediaPlayer player = new MediaPlayer();
            shutdownPlayer = player;
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            player.setOnPreparedListener(mp -> {
                if (gen != shutdownGeneration) return;
                handler.removeCallbacks(shutdownFallback);
                shutdownFallback = () -> {
                    if (gen != shutdownGeneration) return;
                    releaseShutdownPlayer();
                    fire.run();
                };
                handler.postDelayed(shutdownFallback, 15000);
                mp.start();
            });
            player.setOnCompletionListener(mp -> {
                if (gen != shutdownGeneration) return;
                releaseShutdownPlayer();
                fire.run();
            });
            player.setOnErrorListener((mp, what, extra) -> {
                if (gen != shutdownGeneration) return true;
                releaseShutdownPlayer();
                fire.run();
                return true;
            });
            player.setDataSource(url);
            player.prepareAsync();
        } catch (Exception e) {
            releaseShutdownPlayer();
            fire.run();
            return;
        }
        // Four and a half seconds to start; then the Off code goes regardless.
        shutdownFallback = () -> {
            if (gen != shutdownGeneration) return;
            releaseShutdownPlayer();
            fire.run();
        };
        handler.postDelayed(shutdownFallback, 4500);
    }

    private void releaseShutdownPlayer() {
        if (shutdownPlayer == null) return;
        try {
            shutdownPlayer.reset();
        } catch (Exception ignored) {
        }
        try {
            shutdownPlayer.release();
        } catch (Exception ignored) {
        }
        shutdownPlayer = null;
    }

    private void cancelShutdownSound() {
        shutdownGeneration++;
        handler.removeCallbacks(shutdownFallback);
        shutdownFallback = null;
        releaseShutdownPlayer();
    }

    private String acTargetLabel() {
        if (current == null) return "";
        Code power = current.buttons.get("power");
        if (!R05D.supported(power)) return "";
        int[] state = DeviceStore.acState(this, macroKeyFor(current),
                R05D.celsiusFromIndex(R05D.tempIndex(power)),
                acModeIndex(R05D.modeBits(power)),
                acFanIndex(R05D.fanBits(power)));
        String[] modes = {"Cool", "Dry", "Fan", "Heat", "Auto"};
        String[] fans = {"Auto", "Low", "Medium", "High"};
        return state[0] + "\u00b0C \u00b7 " + modes[state[1]] + " \u00b7 Fan "
                + fans[state[2]];
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

    /**
     * Two aircon state packets 50 ms apart read as one corrupted burst, so a repeat
     * gets a gap the unit can separate into two presses. Force mode keeps the tight
     * timing some built-in emitters need.
     */
    private int frameGapMs() {
        if (DeviceStore.forceMode(this)) return 25;
        return current != null && "Air Conditioner".equals(current.category) ? 300 : 50;
    }

    /** Snap a requested carrier to a range the phone says it can emit. */
    private int carrierFor(int freq) {
        if (ir == null) return freq;
        try {
            ConsumerIrManager.CarrierFrequencyRange[] ranges = ir.getCarrierFrequencies();
            if (ranges == null || ranges.length == 0) return freq;
            int best = freq;
            int bestDistance = Integer.MAX_VALUE;
            for (ConsumerIrManager.CarrierFrequencyRange range : ranges) {
                if (freq >= range.getMinFrequency() && freq <= range.getMaxFrequency()) {
                    return freq;
                }
                int cand = Math.max(range.getMinFrequency(),
                        Math.min(range.getMaxFrequency(), freq));
                int distance = Math.abs(cand - freq);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = cand;
                }
            }
            return best;
        } catch (Exception e) {
            return freq;
        }
    }

    /** One or more frames on the IR thread, for macro and Test Storm loops. */
    private void sendCodeBlocking(Code code, boolean jack, String deviceKey,
                                  int repeats) throws Exception {
        DeviceStore.Calibration cal = DeviceStore.calibration(this, deviceKey);
        int freq = cal != null && cal.freq > 0 ? cal.freq : code.freq;
        int gap = cal != null ? cal.gapMs : frameGapMs();
        int count = Math.max(1, Math.min(3, repeats));
        for (int i = 0; i < count; i++) {
            if (jack) {
                JackBlaster.Result sent = JackBlaster.send(this, code);
                if (!sent.ok) throw new Exception(sent.detail);
            } else {
                ir.transmit(carrierFor(freq), code.pattern);
            }
            if (i + 1 < count) Thread.sleep(gap);
        }
    }

    /** Sleep in slices so a stop button lands within 50 ms. */
    private void sleepWhile(int ms, AtomicBoolean cancelled) throws InterruptedException {
        int left = Math.max(0, ms);
        while (left > 0 && !cancelled.get()) {
            int slice = Math.min(50, left);
            Thread.sleep(slice);
            left -= slice;
        }
    }

    private void transmit(Code code, boolean allowRepeat) {
        transmit(code, allowRepeat, null);
    }

    private boolean transmit(Code code, boolean allowRepeat, Runnable onDone) {
        int repeats = DeviceStore.repeats(this);
        if (allowRepeat && DeviceStore.forceMode(this)) repeats = Math.max(3, repeats);
        DeviceStore.Calibration cal = DeviceStore.calibration(this, macroKey());
        if (cal != null) repeats = allowRepeat ? cal.repeats : 1;
        return transmit(code, allowRepeat ? repeats : 1, onDone);
    }

    private void transmit(Code code, int repeats) {
        transmit(code, repeats, null);
    }

    private boolean transmit(Code code, int repeats, Runnable onDone) {
        final boolean jack = emitterState() == EMITTER_JACK;
        if (!jack) {
            if (ir == null) {
                status.setText("No IR service on this phone.");
                return false;
            }
            if (!ir.hasIrEmitter() && !DeviceStore.irOverride(this)) {
                status.setText(noEmitterMessage());
                return false;
            }
        }
        if (irExecutor.isShutdown()) return false;
        // A single-threaded emitter is fine, but refusing taps while one frame is in
        // flight made fast presses feel late and dropped. Queue a few instead.
        if (irInFlight.get() >= 5) return false;
        irInFlight.incrementAndGet();
        final int count = Math.max(1, Math.min(3, repeats));
        DeviceStore.Calibration cal = DeviceStore.calibration(this, macroKey());
        final int gapMs = cal != null ? cal.gapMs : frameGapMs();
        final int sendFreq = carrierFor(cal != null && cal.freq > 0 ? cal.freq : code.freq);
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
                        ir.transmit(sendFreq, code.pattern);
                    }
                    if (i + 1 < count) Thread.sleep(gapMs);
                }
            } catch (InterruptedException ignored) {
            } catch (Exception e) {
                failure = e.getMessage() == null
                        ? e.getClass().getSimpleName() : e.getMessage();
            } finally {
                irInFlight.decrementAndGet();
                final String why = failure;
                if (why != null) {
                    runOnUiThread(() -> status.setText(
                            (jack ? "Jack send failed: " : "IR send failed: ") + why));
                }
                if (onDone != null) runOnUiThread(onDone);
            }
            });
            return true;
        } catch (RejectedExecutionException e) {
            irInFlight.decrementAndGet();
            return false;
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

    /** Phone keyboard -> the IR keys this profile actually carries. */
    private void showTypeDialog() {
        if (current == null) {
            status.setText("Pick a device remote first.");
            return;
        }
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(20), dp(20), dp(20), dp(20));
        panel.setBackground(Surface.raised(this, 28, CARD, palette.rim(),
                palette.gloss(0.10f)));
        Surface.elevate(panel, 28, 12);

        panel.addView(text("Type to TV", 24, TEXT, true));
        TextView note = text(typeSupport(), 12, MUTED, false);
        note.setPadding(0, dp(6), 0, dp(10));
        panel.addView(note);

        EditText input = new EditText(this);
        input.setHint("Channel number, PIN or text");
        input.setHintTextColor(MUTED);
        input.setTextColor(TEXT);
        input.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setMinLines(2);
        input.setGravity(Gravity.TOP | Gravity.START);
        input.setBackgroundTintList(ColorStateList.valueOf(ACCENT));
        panel.addView(input, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(96)));

        TextView preview = text("", 11, MUTED, false);
        preview.setPadding(0, dp(6), 0, 0);
        panel.addView(preview);
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                int ok = 0;
                int skipped = 0;
                for (char ch : s.toString().toCharArray()) {
                    if (Character.isWhitespace(ch)) continue;
                    String key = typeKeyFor(ch);
                    if (key != null && current.buttons.containsKey(key)) ok++;
                    else skipped++;
                }
                preview.setText("Sends " + ok + " key" + (ok == 1 ? "" : "s")
                        + (skipped > 0 ? " \u00b7 " + skipped + " character"
                        + (skipped == 1 ? "" : "s") + " not in this profile" : ""));
            }
        });

        Button send = highlightButton("Send to device", 16);
        send.setOnClickListener(v -> {
            String value = input.getText().toString();
            dialog.dismiss();
            sendTyped(value);
        });
        LinearLayout.LayoutParams sendLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        sendLp.topMargin = dp(12);
        panel.addView(send, sendLp);

        Button close = styledButton("Close", BTN, 14);
        close.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        closeLp.topMargin = dp(8);
        panel.addView(close, closeLp);

        dialog.setContentView(panel);
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(window.getAttributes());
            lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.92f);
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(lp);
        }
        input.requestFocus();
    }

    private String typeSupport() {
        if (current == null) return "";
        boolean digits = current.buttons.containsKey("num_0");
        boolean letters = current.buttons.containsKey("letter_a");
        StringBuilder sb = new StringBuilder("Each character is one IR key press. ");
        if (letters) {
            sb.append("This profile has letter keys, so text types directly.");
        } else {
            sb.append("This profile has no letter keys \u2014 digits, dash and dot "
                    + "send; letters are skipped. TVs accept letters through their "
                    + "on-screen keyboard, not a universal IR text code.");
        }
        if (!digits) sb.append(" It has no digit keys either.");
        return sb.toString();
    }

    private String typeKeyFor(char ch) {
        if (ch >= '0' && ch <= '9') return "num_" + ch;
        if (ch >= 'a' && ch <= 'z') return "letter_" + ch;
        if (ch >= 'A' && ch <= 'Z') return "letter_" + Character.toLowerCase(ch);
        if (ch == '-') return "dash";
        if (ch == '.') return "dot";
        if (ch == ' ') return "space";
        if (ch == '\n') return "enter";
        if (ch == '\b') return "backspace";
        return null;
    }

    /** One queued burst, so a whole word never drops keys on a busy emitter. */
    private void sendTyped(String text) {
        if (current == null) return;
        boolean jack = emitterState() == EMITTER_JACK;
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
        List<Code> codes = new ArrayList<>();
        int skipped = 0;
        for (char ch : text.toCharArray()) {
            if (ch == '\r') continue;
            String key = typeKeyFor(ch);
            Code code = key == null ? null : current.buttons.get(key);
            if (code == null) {
                if (!Character.isWhitespace(ch)) skipped++;
                continue;
            }
            codes.add(code);
        }
        if (codes.isEmpty()) {
            status.setText("Nothing in that text matches this remote's keys.");
            return;
        }
        if (irExecutor.isShutdown() || !irBusy.compareAndSet(false, true)) {
            status.setText("IR is busy \u2014 wait a moment and send again.");
            return;
        }
        final boolean throughJack = jack;
        final int missed = skipped;
        final int count = codes.size();
        try {
            irExecutor.execute(() -> {
                String failure = null;
                try {
                    for (int i = 0; i < codes.size(); i++) {
                        Code code = codes.get(i);
                        if (throughJack) {
                            JackBlaster.Result sent = JackBlaster.send(this, code);
                            if (!sent.ok) {
                                failure = sent.detail;
                                break;
                            }
                        } else {
                            ir.transmit(code.freq, code.pattern);
                        }
                        if (i + 1 < codes.size()) Thread.sleep(TEXT_GAP_MS);
                    }
                } catch (InterruptedException ignored) {
                } catch (Exception e) {
                    failure = e.getMessage() == null
                            ? e.getClass().getSimpleName() : e.getMessage();
                } finally {
                    irBusy.set(false);
                    final String why = failure;
                    runOnUiThread(() -> {
                        if (why != null) {
                            status.setText("Typing stopped: " + why);
                        } else {
                            status.setText("Typed " + count + " key"
                                    + (count == 1 ? "" : "s")
                                    + (missed > 0 ? " \u00b7 " + missed + " skipped" : "")
                                    + ".");
                        }
                    });
                }
            });
        } catch (RejectedExecutionException e) {
            irBusy.set(false);
        }
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
        String line = nextCandidate + ". " + prettyBrand(c.remote.brand) + " — "
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
            List<Brand> sorted = new ArrayList<>(brands);
            sorted.sort((a, b) -> prettyBrand(a.name).compareToIgnoreCase(prettyBrand(b.name)));
            for (Brand b : sorted) {
                if (!brandInRange(b.name)) continue;
                for (Remote r : b.remotes) addCandidate(r, unique);
            }
        } else {
            int bi = brandSpinner.getSelectedItemPosition();
            if (bi < 0 || bi >= brands.size()) return;
            for (Remote r : brands.get(bi).remotes) addCandidate(r, unique);
        }
        candidates.addAll(unique.values());
        if (candidates.isEmpty()) {
            status.setText(allBrands.isChecked()
                    ? "No power codes for brands " + rangeFrom() + "\u2013" + rangeTo()
                            + " in this device type."
                    : "No codes available.");
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
            // A manual tap or the previous frame is still on the wire. Retry
            // quickly instead of burning a code the emitter never sent.
            handler.postDelayed(this::tickAutoFind, 25);
            return;
        }
        final long sendStartedAt = SystemClock.elapsedRealtime();
        boolean started = transmit(c.code, "Air Conditioner".equals(current.category)
                && DeviceStore.acPowerRepeat(this), () -> {
                    if (!running || scanPaused) return;
                    long elapsed = SystemClock.elapsedRealtime() - sendStartedAt;
                    int floor = emitterState() == EMITTER_JACK ? 120 : 60;
                    int wait = Math.max(floor, scanDelayMs - (int) elapsed);
                    handler.postDelayed(this::tickAutoFind, wait);
                });
        if (!started) {
            // The emitter was busy; leave this candidate unspent and retry.
            handler.postDelayed(this::tickAutoFind, 25);
            return;
        }
        nextCandidate++;
        if (scannerDialog != null && scannerDialog.getWindow() != null) {
            scannerDialog.getWindow().getDecorView()
                    .performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        }
        progress.setProgress(nextCandidate);
        scanCount.setText("Scanning " + nextCandidate + " / " + candidates.size());
        scanName.setText(prettyBrand(c.remote.brand) + " \u2014 " + pretty(c.remote.name)
                + (c.remote.unverified ? "  \u00b7 untested source" : ""));
        recordScanHistory(c);
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
        scanName.setText(prettyBrand(c.remote.brand) + " \u2014 " + pretty(c.remote.name)
                + (c.remote.unverified ? "  \u00b7 untested source" : ""));
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
        pageTitles.clear();
        if (current == null) {
            controlsCard.addView(text("Select a remote to begin.", 13, MUTED, false));
            return;
        }
        brandAccent = brandAccentFor(current.brand);
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);

        TextView controlsLabel = text("CONTROLS", 10, brandAccent, true);
        controlsLabel.setLetterSpacing(0.14f);
        head.addView(controlsLabel, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button macrosButton = styledButton("MACROS", BTN, 11);
        macrosButton.setContentDescription("Macros for this remote");
        macrosButton.setOnClickListener(v -> showMacroPage());
        LinearLayout.LayoutParams mb = new LinearLayout.LayoutParams(dp(70), dp(34));
        mb.setMargins(0, 0, dp(6), 0);
        head.addView(macrosButton, mb);

        Button stormButton = styledButton("TEST", BTN, 11);
        stormButton.setContentDescription("Test Storm");
        stormButton.setOnClickListener(v -> showStormDialog());
        head.addView(stormButton, new LinearLayout.LayoutParams(dp(54), dp(34)));
        controlsCard.addView(head);

        View brandBar = new View(this);
        brandBar.setBackground(pill(brandAccent, Surface.darken(brandAccent, 0.30f)));
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(dp(46), dp(3));
        barLp.topMargin = dp(7);
        controlsCard.addView(brandBar, barLp);

        if (current.buttons.containsKey("power")) {
            boolean hasOff = current.buttons.containsKey("power_off");
            boolean aircon = "Air Conditioner".equals(current.category);
            // Aircon captures pair `power` with `power_off` as the two directions; a TV
            // usually pairs a toggle with standby, so only the AC gets the ON label.
            Button power = highlightButton(hasOff && aircon ? "POWER ON" : "POWER",
                    20, brandAccent);
            power.setOnClickListener(v -> send("power"));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(76));
            lp.topMargin = dp(10);
            controlsCard.addView(power, lp);
            buttons.put("power", power);

            if (hasOff) {
                Button powerOff = styledButton("POWER OFF", BTN, 17);
                powerOff.setTextColor(RED);
                powerOff.setOnClickListener(v -> send("power_off"));
                LinearLayout.LayoutParams po = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(60));
                po.topMargin = dp(8);
                controlsCard.addView(powerOff, po);
                buttons.put("power_off", powerOff);
            }
            if (aircon) {
                Button power2 = styledButton("POWER ×2", BTN, 14);
                power2.setOnClickListener(v -> sendPowerRepeat());
                LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
                p2.topMargin = dp(8);
                controlsCard.addView(power2, p2);
                buttons.put("__power2", power2);
                controlsCard.addView(text(hasOff
                        ? "This model has separate On and Off codes, so each button is "
                        + "one direction. If the unit ignores a single frame, POWER ×2 "
                        + "sends it twice with a real gap between."
                        : "Power is one toggle code here. If the unit ignores a single "
                                + "frame, try POWER ×2.",
                        11, MUTED, false));

                LinearLayout acTools = new LinearLayout(this);
                acTools.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams at = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
                at.topMargin = dp(8);
                Button doctor = styledButton("POWER DOCTOR", BTN, 12);
                doctor.setTextColor(brandAccent);
                doctor.setOnClickListener(v -> showPowerDoctor());
                Button addCodes = styledButton("ADD CODES", BTN, 12);
                addCodes.setOnClickListener(v -> showAddKey());
                LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(0, dp(46), 1f);
                dl.setMargins(0, 0, dp(3), 0);
                LinearLayout.LayoutParams al = new LinearLayout.LayoutParams(0, dp(46), 1f);
                al.setMargins(dp(3), 0, 0, 0);
                acTools.addView(doctor, dl);
                acTools.addView(addCodes, al);
                controlsCard.addView(acTools, at);

                if (!R05D.supported(current.buttons.get("power"))
                        && !current.buttons.containsKey("temp_up")
                        && !current.buttons.containsKey("temp_down")) {
                    TextView temp = text("This profile has no temperature codes captured. "
                            + "ADD CODES can pull a temperature button from another "
                            + current.brand + " profile in this category; a full .ir "
                            + "remote also works.", 11, MUTED, false);
                    temp.setPadding(0, dp(8), 0, 0);
                    controlsCard.addView(temp);
                }
                if (R05D.supported(current.buttons.get("power"))) {
                    TextView r05d = text("R05D state rebuild ready \u2014 swipe the "
                            + "controls pages to AIRCON SET for 17\u201330 \u00b0C, "
                            + "mode and fan frames generated from this unit's own "
                            + "captured state.", 11, brandAccent, false);
                    r05d.setPadding(0, dp(8), 0, 0);
                    controlsCard.addView(r05d);
                }
            }
        }

        if (!"Air Conditioner".equals(current.category)) {
            TextView shortcutLabel = text("SHORTCUTS", 10, brandAccent, true);
            shortcutLabel.setLetterSpacing(0.14f);
            shortcutLabel.setPadding(0, dp(14), 0, dp(6));
            controlsCard.addView(shortcutLabel);
            controlsCard.addView(shortcutGrid());
            TextView shortcutNote = text("Accent buttons send the TV's own app key. A grey "
                    + "one has no IR code on this model — IR cannot carry a URL, so it asks "
                    + "before opening on the phone. Hold Web to change its address.", 11, MUTED,
                    false);
            shortcutNote.setPadding(0, dp(6), 0, 0);
            controlsCard.addView(shortcutNote);
        }

        Button typeButton = styledButton("\u2328  Type to TV", BTN, 15);
        typeButton.setOnClickListener(v -> showTypeDialog());
        LinearLayout.LayoutParams typeLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(54));
        typeLp.topMargin = dp(10);
        controlsCard.addView(typeButton, typeLp);

        List<Page> pages = buildPages();
        pageCount = pages.size();
        pageTitles.clear();
        for (Page page : pages) pageTitles.add(page.title);
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
            TextView swipeHint = text("Swipe the buttons left or right. Every code this "
                    + "model can send lives on these pages \u2014 the page strip below "
                    + "tells you where you are.", 11, MUTED, false);
            swipeHint.setPadding(0, dp(4), 0, 0);
            controlsCard.addView(swipeHint);
        }

        if ("Air Conditioner".equals(current.category)) {
            boolean r05d = R05D.supported(current.buttons.get("power"));
            controlsCard.addView(text(r05d
                    ? "AIRCON SET sends an absolute target, not a step: tap 24 \u00b0C and "
                    + "the unit is told 24 \u00b0C in one full state frame. Use POWER OFF "
                    + "(or Power Doctor) to shut it down."
                    : "Aircon codes are included per button. Temperature/mode buttons "
                    + "may set a captured state rather than step; if your model is "
                    + "missing, its IR protocol is not in this database.",
                    11, MUTED, false));
        }
        if (hasHoldable(pages)) {
            controlsCard.addView(text("One tap sends one code. Hold a button to keep "
                    + "sending it.", 11, MUTED, false));
        }
    }

    /** Every service the shortcut row knows about, in the order it is drawn. */
    private static final String[] SHORTCUTS = {"netflix", "youtube", "prime", "disney",
            "hbo", "spotify", "apps", "web"};

    /**
     * Shortcut row. Only entries that can reach the TV are drawn, with a coloured service badge,
     * and the caption says exactly what each one will do. Services with no code anywhere in the
     * database (YouTube, Prime Video, Disney+, Max, Spotify — the DB carries only {@code netflix}
     * and {@code apps}) never get a tile that cannot fire; they live behind the phone button.
     */
    private View shortcutGrid() {
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);

        List<String[]> items = new ArrayList<>();
        List<String> phoneOnly = new ArrayList<>();
        int viaMenu = 0;
        for (String kind : SHORTCUTS) {
            String route = shortcutRoute(kind);
            if ("none".equals(route)) {
                if (!"web".equals(kind) && !"apps".equals(kind)) phoneOnly.add(shortcutName(kind));
                continue;
            }
            if ("menu".equals(route)) viaMenu++;
            items.add(new String[]{shortcutName(kind), kind, route});
        }

        // Two per row: a 26 dp badge plus a name like "Prime Video" does not fit three across
        // on a 360 dp screen, and an ellipsised service name looks like a broken button.
        for (int i = 0; i < items.size(); i += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int j = 0; j < 2; j++) {
                int n = i + j;
                if (n >= items.size()) {
                    row.addView(new Space(this), new LinearLayout.LayoutParams(0, 1, 1f));
                    continue;
                }
                final String kind = items.get(n)[1];
                boolean direct = "key".equals(items.get(n)[2]);
                int accent = direct ? tileAccent()
                        : Surface.blend(tileAccent(), TEXT, 0.35f);
                Button b = new KeyTile(this, palette, accent,
                        BrandMark.of(this, kind, accent, 24), items.get(n)[0], 12.5f);
                b.setOnClickListener(v -> activateShortcut(kind));
                b.setContentDescription(items.get(n)[0] + (direct
                        ? " shortcut, sent to the TV over IR"
                        : " shortcut, opens this TV's app menu over IR"));
                if ("web".equals(kind)) {
                    b.setOnLongClickListener(v -> {
                        editWebUrl();
                        return true;
                    });
                }
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(60), 1f);
                lp.setMargins(dp(5), dp(5), dp(5), dp(5));
                row.addView(b, lp);
            }
            grid.addView(row);
        }

        if (items.isEmpty() && phoneOnly.isEmpty()) {
            grid.addView(text("This profile has no app keys at all.", 11, MUTED, false));
        } else if (items.isEmpty()) {
            grid.addView(text("No app keys on this " + (current == null ? "model"
                    : current.brand) + " profile \u2014 these services cannot be launched on "
                    + "the TV by any remote code.", 11, MUTED, false));
        } else if (viaMenu > 0) {
            grid.addView(text("A button in the accent sends the service's own key. A dimmer "
                    + "one opens the TV's app menu, where the app already lives.", 11, MUTED,
                    false));
        }
        if (!phoneOnly.isEmpty()) {
            grid.addView(phoneAppsButton());
        }
        return grid;
    }

    /** The phone fallback, kept off the TV grid so no tile ever fails silently. */
    /** Phone-only services stay reachable here, one tap from the chooser. */
    private View phoneAppsButton() {
        Button b = styledButton("More apps on this phone", BTN, 12);
        b.setContentDescription("Open a streaming service on this phone");
        b.setOnClickListener(v -> {
            String[] kinds = {"netflix", "youtube", "prime", "disney", "hbo", "spotify",
                    "web"};
            String[] labels = new String[kinds.length];
            for (int i = 0; i < kinds.length; i++) labels[i] = shortcutName(kinds[i]);
            new AlertDialog.Builder(this)
                    .setTitle("Open on this phone")
                    .setItems(labels, (d, which) -> openOnPhone(kinds[which]))
                    .setNegativeButton("Cancel", null)
                    .show();
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        lp.topMargin = dp(8);
        b.setLayoutParams(lp);
        return b;
    }

    /**
     * What a shortcut can do, in three honest states:
     *
     * <ul>
     *   <li>{@code key} — this profile carries the service's own IR key, from its own capture or
     *       merged in from a sibling model of the same brand.</li>
     *   <li>{@code menu} — no key of its own, but the model has an app launcher, so the TV can
     *       still be driven to the place the app lives. That is the most any code can do: IR is
     *       one-way and cannot carry a URL or a package name.</li>
     *   <li>{@code none} — nothing this remote can send; the service only exists on the phone.</li>
     * </ul>
     */
    private String shortcutRoute(String kind) {
        if (current == null) return "none";
        if ("web".equals(kind)) {
            return current.buttons.containsKey("browser") ? "key" : "none";
        }
        if (current.buttons.containsKey(kind)) return "key";
        if (!"apps".equals(kind) && current.buttons.containsKey("apps")) return "menu";
        return "none";
    }

    private String shortcutName(String kind) {
        switch (kind) {
            case "netflix":
                return "Netflix";
            case "youtube":
                return "YouTube";
            case "prime":
                return "Prime Video";
            case "disney":
                return "Disney+";
            case "hbo":
                return "Max";
            case "spotify":
                return "Spotify";
            case "apps":
                return "Apps";
            case "web":
                return "Web";
            default:
                return labelFor(kind);
        }
    }

    private String shortcutUrl(String kind) {
        switch (kind) {
            case "netflix":
                return "https://www.netflix.com";
            case "youtube":
                return "https://www.youtube.com";
            case "prime":
                return "https://www.primevideo.com";
            case "disney":
                return "https://www.disneyplus.com";
            case "hbo":
                return "https://www.max.com";
            case "spotify":
                return "https://open.spotify.com";
            case "copilot":
                return "https://copilot.microsoft.com";
            case "web":
                return DeviceStore.webUrl(this);
            default:
                return null;
        }
    }

    /** A shortcut always does the best thing it can, and says which one it did. */
    private void activateShortcut(String kind) {
        if (current == null) {
            status.setText("Pick a device remote first.");
            return;
        }
        String route = shortcutRoute(kind);
        String name = shortcutName(kind);
        if ("key".equals(route)) {
            send("web".equals(kind) ? "browser" : kind);
            return;
        }
        if ("menu".equals(route)) {
            send("apps");
            status.setText(name + " has no key on this model \u2014 opened the TV's app menu.");
            return;
        }
        openOnPhone(kind);
    }

    /** Confirm before leaving the app: a phone link is never a substitute for an IR press. */
    private void openOnPhone(String kind) {
        String url = shortcutUrl(kind);
        if (url == null) return;
        String name = shortcutName(kind);
        String brand = current == null ? "This" : current.brand;
        new AlertDialog.Builder(this)
                .setTitle("Open " + name + " on this phone?")
                .setMessage("The " + brand + " profile has no " + name + " IR key, and IR is "
                        + "one-way \u2014 it cannot carry a web address or launch an app on the "
                        + "TV. The link opens here instead.")
                .setPositiveButton("Open on phone", (d, w) -> openInChrome(url, name))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Last resort for a shortcut with no IR code on the selected model: open it on the phone.
     * Chrome is addressed by package first; any browser is the fallback. IR cannot carry a URL,
     * so this is never a substitute for sending a key to the TV.
     */
    private void openInChrome(String url, String name) {
        if (url == null || url.isEmpty()) return;
        Intent view = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        view.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Intent chrome = new Intent(view);
        chrome.setPackage("com.android.chrome");
        try {
            startActivity(chrome);
            status.setText("Opened " + name + " in Chrome on this phone.");
            return;
        } catch (Exception ignored) {
        }
        try {
            startActivity(view);
            status.setText("Opened " + name + " on this phone.");
        } catch (Exception e) {
            status.setText("No browser on this phone can open " + name + ".");
        }
    }

    private void editWebUrl() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(20), dp(20), dp(20), dp(20));
        panel.setBackground(Surface.raised(this, 28, CARD, palette.rim(),
                palette.gloss(0.10f)));
        Surface.elevate(panel, 28, 12);

        panel.addView(text("Web shortcut", 24, TEXT, true));
        TextView note = text("The Web button opens this address on the phone. "
                + "A TV cannot receive a URL over IR.", 12, MUTED, false);
        note.setPadding(0, dp(6), 0, dp(10));
        panel.addView(note);

        EditText input = new EditText(this);
        input.setText(DeviceStore.webUrl(this));
        input.setHint("https://example.com");
        input.setHintTextColor(MUTED);
        input.setTextColor(TEXT);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input.setBackgroundTintList(ColorStateList.valueOf(ACCENT));
        panel.addView(input, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));

        LinearLayout quick = new LinearLayout(this);
        quick.setOrientation(LinearLayout.HORIZONTAL);
        String[][] links = {{"YouTube", "https://www.youtube.com"},
                {"Netflix", "https://www.netflix.com"},
                {"Prime", "https://www.primevideo.com"}};
        for (int i = 0; i < links.length; i++) {
            final String preset = links[i][1];
            Button b = styledButton(links[i][0], BTN, 12);
            b.setOnClickListener(v -> input.setText(preset));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(42), 1f);
            lp.setMargins(dp(i == 0 ? 0 : 3), dp(8), dp(i == 2 ? 0 : 3), 0);
            quick.addView(b, lp);
        }
        panel.addView(quick);

        Button save = highlightButton("Save address", 15);
        save.setOnClickListener(v -> {
            String url = input.getText().toString().trim();
            if (url.isEmpty()) {
                input.setError("Enter an address");
                return;
            }
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }
            DeviceStore.setWebUrl(this, url);
            status.setText("Web shortcut set to " + url);
            dialog.dismiss();
        });
        LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(50));
        sl.topMargin = dp(14);
        panel.addView(save, sl);

        Button cancel = styledButton("Cancel", BTN, 13);
        cancel.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        cl.topMargin = dp(8);
        panel.addView(cancel, cl);

        dialog.setContentView(panel);
        dialog.show();
    }

    private void showShutdownSoundDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout panel = sheetPanel("TV shutdown sound",
                "A short clip plays on this phone, then Power Off goes to the TV.");

        panel.addView(switchRow("Play before Power Off",
                "TV profile only \u00b7 sound comes from this phone",
                DeviceStore.shutdownSound(this), on -> {
                    DeviceStore.setShutdownSound(this, on);
                    status.setText(on ? "Shutdown sound is on."
                            : "Shutdown sound is off.");
                }));

        TextView note = text("Paste a direct audio link \u2014 the file itself, not a "
                + "YouTube or web page. Media volume controls how loud it is. A slow "
                + "or broken link never blocks the Off code. On a TV profile with one "
                + "Power toggle, the clip plays on that tap too, because IR cannot "
                + "tell on from off.", 11, MUTED, false);
        note.setPadding(0, dp(4), 0, dp(10));
        panel.addView(note);

        EditText input = new EditText(this);
        input.setText(DeviceStore.shutdownSoundUrl(this));
        input.setHint("https://example.com/shutdown.mp3");
        input.setHintTextColor(MUTED);
        input.setTextColor(TEXT);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input.setBackgroundTintList(ColorStateList.valueOf(ACCENT));
        panel.addView(input, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button test = styledButton("Test sound", BTN, 13);
        test.setOnClickListener(v -> {
            String url = audioUrl(input);
            if (url == null) return;
            status.setText("Playing a test clip on this phone\u2026");
            playShutdownSound(url, () -> status.setText("Test clip finished."));
        });
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(0, dp(46), 1f);
        tl.setMargins(0, dp(12), dp(4), 0);
        actions.addView(test, tl);

        Button save = highlightButton("Save", 14);
        save.setOnClickListener(v -> {
            String raw = input.getText().toString().trim();
            if (raw.isEmpty()) {
                if (DeviceStore.shutdownSound(this)) {
                    input.setError("Add a sound URL or turn the toggle off");
                    return;
                }
                DeviceStore.setShutdownSoundUrl(this, "");
                status.setText("Shutdown sound URL cleared.");
                dialog.dismiss();
                return;
            }
            String url = audioUrl(input);
            if (url == null) return;
            DeviceStore.setShutdownSoundUrl(this, url);
            input.setText(url);
            status.setText(DeviceStore.shutdownSound(this)
                    ? "Shutdown sound saved. It plays before Power Off."
                    : "Shutdown sound saved. Turn the toggle on to use it.");
            dialog.dismiss();
        });
        LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(0, dp(46), 1f);
        sl.setMargins(dp(4), dp(12), 0, 0);
        actions.addView(save, sl);
        panel.addView(actions);

        Button cancel = styledButton("Close", BTN, 13);
        cancel.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        cl.topMargin = dp(8);
        panel.addView(cancel, cl);

        showSheet(dialog, panel, 0.82f);
    }

    /** @return a normalised https audio URL, or null after flagging the field. */
    private String audioUrl(EditText input) {
        String url = input.getText().toString().trim();
        if (url.isEmpty()) {
            input.setError("Paste an audio link");
            return null;
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        if (!url.startsWith("https://")) {
            input.setError("Use an https:// link");
            return null;
        }
        String path = url.toLowerCase(Locale.US);
        int cut = path.indexOf('?');
        if (cut >= 0) path = path.substring(0, cut);
        cut = path.indexOf('#');
        if (cut >= 0) path = path.substring(0, cut);
        if (!(path.endsWith(".mp3") || path.endsWith(".m4a") || path.endsWith(".aac")
                || path.endsWith(".ogg") || path.endsWith(".wav")
                || path.endsWith(".opus") || path.endsWith(".flac"))) {
            input.setError("Link must end at an audio file: .mp3, .m4a, .ogg or .wav");
            return null;
        }
        return url;
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

        List<String> apps = new ArrayList<>();
        for (String key : APP_KEYS) {
            if (current.buttons.containsKey(key) && used.add(key)) apps.add(key);
        }
        addPage(pages, "APPS", apps);

        List<String> acSet = new ArrayList<>();
        for (String key : current.buttons.keySet()) {
            if (key.startsWith("ac_") && used.add(key)) acSet.add(key);
        }
        addPage(pages, "AIRCON SET", acSet);

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

        List<String> keyboard = new ArrayList<>();
        for (String key : KEYBOARD_KEYS) {
            if (current.buttons.containsKey(key) && used.add(key)) keyboard.add(key);
        }
        addPage(pages, "KEYBOARD", keyboard);

        List<String> extras = new ArrayList<>();
        for (String key : BUTTON_ORDER) {
            // Both power buttons live in the header; a second copy in EXTRAS is clutter.
            if (key.equals("power") || key.equals("power_off")) continue;
            if (key.startsWith("ac_")) continue;
            if (current.buttons.containsKey(key) && used.add(key)) extras.add(key);
        }
        // Never drop a key just because this build has not heard of it.
        for (String key : current.buttons.keySet()) {
            if (key.equals("power") || key.equals("power_off")) continue;
            if (key.startsWith("ac_")) continue;
            if (used.add(key)) extras.add(key);
        }
        addPage(pages, "EXTRAS", extras);
        return pages;
    }

    private void addPage(List<Page> pages, String title, List<String> keys) {
        if (keys.isEmpty()) return;
        // Big icon tiles need room: six per page, except the number pad and the letter
        // grid, which stay compact so the whole keypad is on one page.
        int per = "NUMBERS".equals(title) || "KEYBOARD".equals(title) ? 12 : 6;
        int chunks = (keys.size() + per - 1) / per;
        for (int c = 0; c < chunks; c++) {
            int from = c * per;
            int to = Math.min(keys.size(), from + per);
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
        TextView title = text(page.title, 13, brandAccent == 0 ? ACCENT : brandAccent, true);
        title.setLetterSpacing(0.10f);
        title.setPadding(0, dp(12), 0, dp(8));
        parent.addView(title);
        if (page.title.startsWith("NAVIGATION")) {
            boolean hasDpad = true;
            for (String key : DPAD) if (!page.keys.contains(key)) hasDpad = false;
            if (hasDpad) {
                addDpad(parent);
                List<String> rest = new ArrayList<>();
                for (String key : page.keys) if (!isDpad(key)) rest.add(key);
                if (!rest.isEmpty()) addGrid(parent, rest);
                return;
            }
        }
        if (page.title.startsWith("NUMBERS")) {
            addNumberPad(parent, page.keys);
            return;
        }
        if (page.title.startsWith("KEYBOARD")) {
            addCompactGrid(parent, page.keys);
            return;
        }
        addGrid(parent, page.keys);
    }

    /** The D-pad as a real ring of large round keys instead of a row of rectangles. */
    private void addDpad(LinearLayout parent) {
        addDpadRow(parent, null, "up", null);
        addDpadRow(parent, "left", "ok", "right");
        addDpadRow(parent, null, "down", null);
    }

    private void addDpadRow(LinearLayout parent, String a, String b, String c) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        String[] keys = {a, b, c};
        for (String key : keys) {
            if (key == null) {
                row.addView(new Space(this), new LinearLayout.LayoutParams(0, 1, 1f));
                continue;
            }
            boolean ok = "ok".equals(key);
            Button tile = keyTile(key, labelFor(key), !ok, dp(66), 15);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(66), 1f);
            lp.setMargins(dp(4), dp(4), dp(4), dp(4));
            row.addView(tile, lp);
        }
        parent.addView(row);
    }

    private void addNumberPad(LinearLayout parent, List<String> keys) {
        for (int i = 0; i < keys.size(); i += 3) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int j = 0; j < 3; j++) {
                int n = i + j;
                if (n >= keys.size()) {
                    row.addView(new Space(this), new LinearLayout.LayoutParams(0, 1, 1f));
                    continue;
                }
                String key = keys.get(n);
                Button b = new KeyTile(this, palette, tileAccent(), labelFor(key), 22);
                b.setContentDescription(labelFor(key));
                b.setOnClickListener(v -> send(key));
                buttons.put(key, b);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(68), 1f);
                lp.setMargins(dp(4), dp(4), dp(4), dp(4));
                row.addView(b, lp);
            }
            parent.addView(row);
        }
    }

    /** Letter grid: label-only plates, four across, so the keyboard fits a page. */
    private void addCompactGrid(LinearLayout parent, List<String> keys) {
        for (int i = 0; i < keys.size(); i += 4) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int j = 0; j < 4; j++) {
                int n = i + j;
                if (n >= keys.size()) {
                    row.addView(new Space(this), new LinearLayout.LayoutParams(0, 1, 1f));
                    continue;
                }
                String key = keys.get(n);
                Button b = new KeyTile(this, palette, tileAccent(), labelFor(key), 16);
                b.setContentDescription(labelFor(key));
                if (holdable(key)) attachRepeat(b, key);
                else b.setOnClickListener(v -> send(key));
                buttons.put(key, b);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(54), 1f);
                lp.setMargins(dp(4), dp(4), dp(4), dp(4));
                row.addView(b, lp);
            }
            parent.addView(row);
        }
    }

    private int tileAccent() {
        return brandAccent == 0 ? ACCENT : brandAccent;
    }

    /** Build one controls tile and wire it to the emitter (or hold-to-repeat). */
    private Button keyTile(String key, String label, boolean iconOnly, int heightDp,
                           float textSize) {
        Button b = new KeyTile(this, palette, tileAccent(), key, label, textSize, iconOnly,
                false);
        b.setContentDescription(label);
        if (holdable(key)) {
            attachRepeat(b, key);
        } else {
            b.setOnClickListener(v -> send(key));
        }
        buttons.put(key, b);
        return b;
    }

    /** Two big tiles per row: an icon and a readable name need the width. */
    private void addGrid(LinearLayout parent, List<String> keys) {
        int i = 0;
        while (i < keys.size()) {
            int count = Math.min(2, keys.size() - i);
            addFlexRow(parent, keys.subList(i, i + count));
            i += count;
        }
    }

    private static boolean isPair(String a, String b) {
        for (String[] pair : PAIRS) {
            if (pair[0].equals(a) && pair[1].equals(b)) return true;
        }
        return false;
    }

    /** Tiles share the row width instead of leaving a gap at the end. */
    private void addFlexRow(LinearLayout parent, List<String> keys) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        for (String key : keys) {
            Button b = keyTile(key, labelFor(key), false, dp(76), 14);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(76), 1f);
            lp.setMargins(dp(5), dp(5), dp(5), dp(5));
            row.addView(b, lp);
        }
        parent.addView(row);
    }

    private boolean hasHoldable(List<Page> pages) {
        for (Page page : pages) {
            for (String key : page.keys) if (holdable(key)) return true;
        }
        return false;
    }

    private void addPageIndicator() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(6), 0, dp(2));
        Button prev = styledButton("\u2039", BTN, 22);
        prev.setContentDescription("Previous buttons page");
        prev.setOnClickListener(v -> goPage(-1));
        Button next = styledButton("\u203A", BTN, 22);
        next.setContentDescription("Next buttons page");
        next.setOnClickListener(v -> goPage(1));
        pageDots = text("", 11, ACCENT, true);
        pageDots.setGravity(Gravity.CENTER);
        row.addView(prev, new LinearLayout.LayoutParams(dp(52), dp(46)));
        row.addView(pageDots, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(next, new LinearLayout.LayoutParams(dp(52), dp(46)));
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
            if (i < pageCount - 1) sb.append(" ");
        }
        String name = currentPage < pageTitles.size() ? pageTitles.get(currentPage) : "";
        pageDots.setText(sb + "   " + name + "   " + (currentPage + 1) + "/" + pageCount);
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

    // --------------------------------------------------------------- macros

    private int buttonRank(String key) {
        for (int i = 0; i < BUTTON_ORDER.length; i++) {
            if (BUTTON_ORDER[i].equals(key)) return i;
        }
        return BUTTON_ORDER.length;
    }

    private void showMacroPage() {
        if (current == null) {
            status.setText("Pick a device remote first.");
            return;
        }
        if (macroDialog != null && macroDialog.isShowing()) macroDialog.dismiss();
        macroDialog = new Dialog(this);
        macroDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackgroundColor(BG);
        panel.setPadding(dp(20), dp(26), dp(20), dp(16));

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        Button back = styledButton("\u2039", BTN, 20);
        back.setContentDescription("Close macros");
        back.setOnClickListener(v -> macroDialog.dismiss());
        head.addView(back, new LinearLayout.LayoutParams(dp(42), dp(42)));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(dp(10), 0, 0, 0);
        titles.addView(text("Macros", 26, TEXT, true));
        titles.addView(text(prettyBrand(current.brand) + " \u2014 " + pretty(current.name),
                12, MUTED, false));
        head.addView(titles, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button add = highlightButton("+ New", 13, brandAccent);
        add.setOnClickListener(v -> openMacroEditor(null));
        head.addView(add, new LinearLayout.LayoutParams(dp(78), dp(42)));
        panel.addView(head);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        macroContent = new LinearLayout(this);
        macroContent.setOrientation(LinearLayout.VERTICAL);
        macroContent.setPadding(0, dp(10), 0, dp(12));
        scroll.addView(macroContent, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        panel.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        macroDialog.setContentView(panel);
        macroDialog.show();
        Window w = macroDialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(w.getAttributes());
            lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.96f);
            lp.height = (int) (getResources().getDisplayMetrics().heightPixels * 0.90f);
            w.setAttributes(lp);
        }
        renderMacroList();
    }

    private void renderMacroList() {
        if (macroContent == null) return;
        macroContent.removeAllViews();
        List<DeviceStore.Macro> macros = DeviceStore.macros(this, macroKey());
        TextView header = text("SAVED MACROS \u00b7 " + macros.size(), 10, brandAccent, true);
        header.setLetterSpacing(0.14f);
        header.setPadding(0, dp(4), 0, dp(8));
        macroContent.addView(header);

        if (macros.isEmpty()) {
            LinearLayout empty = card();
            empty.addView(text("No macros yet", 18, TEXT, true));
            TextView note = text("A macro sends a sequence with a pause between each "
                    + "press. Build one for \u201cWatch TV\u201d, \u201cMovie night\u201d "
                    + "or \u201cGood night\u201d.", 12, MUTED, false);
            note.setPadding(0, dp(4), 0, 0);
            empty.addView(note);
            macroContent.addView(empty);
        } else {
            for (DeviceStore.Macro macro : macros) macroContent.addView(macroRow(macro));
        }

        Button create = styledButton("+  New macro", BTN, 15);
        create.setOnClickListener(v -> openMacroEditor(null));
        LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        cl.topMargin = dp(10);
        macroContent.addView(create, cl);

        TextView note = text("Macros are stored on this phone with the remote profile. "
                + "Each step sends one IR code; the pause gives the device time to "
                + "react. A macro can repeat forever and stops the moment you tap Stop.",
                11, MUTED, false);
        note.setPadding(dp(4), dp(12), dp(4), 0);
        macroContent.addView(note);
    }

    private View macroRow(final DeviceStore.Macro macro) {
        LinearLayout box = card();
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(text(macro.name, 18, TEXT, true), new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        String loopSuffix = macro.loop < 0 ? " \u00b7 loop \u221e"
                : macro.loop > 1 ? " \u00b7 loop \u00d7" + macro.loop : "";
        top.addView(text(macro.steps.size() + (macro.steps.size() == 1 ? " step" : " steps")
                + loopSuffix, 11, MUTED, false));
        box.addView(top);

        StringBuilder summary = new StringBuilder();
        for (int i = 0; i < macro.steps.size() && i < 4; i++) {
            if (i > 0) summary.append("  \u203a  ");
            summary.append(labelFor(macro.steps.get(i).key));
        }
        if (macro.steps.size() > 4) summary.append("  \u203a  \u2026");
        TextView steps = text(summary.toString(), 11, MUTED, false);
        steps.setPadding(0, dp(4), 0, dp(8));
        box.addView(steps);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button run = highlightButton("Run", 12, brandAccent);
        run.setOnClickListener(v -> runMacro(macro));
        Button loop = styledButton("Loop \u221e", BTN, 12);
        loop.setContentDescription("Run this macro forever");
        loop.setOnClickListener(v -> runMacro(macro, -1));
        Button edit = styledButton("Edit", BTN, 12);
        edit.setOnClickListener(v -> openMacroEditor(macro));
        Button delete = styledButton("Delete", Surface.blend(CARD, RED, 0.20f), 12);
        delete.setTextColor(RED);
        delete.setOnClickListener(v -> confirmDeleteMacro(macro));
        actions.addView(run, new LinearLayout.LayoutParams(0, dp(42), 1f));
        LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(0, dp(42), 1f);
        ll.setMargins(dp(5), 0, dp(5), 0);
        actions.addView(loop, ll);
        LinearLayout.LayoutParams el = new LinearLayout.LayoutParams(0, dp(42), 1f);
        el.setMargins(0, 0, dp(5), 0);
        actions.addView(edit, el);
        actions.addView(delete, new LinearLayout.LayoutParams(0, dp(42), 1f));
        box.addView(actions);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(10);
        box.setLayoutParams(lp);
        return box;
    }

    private void confirmDeleteMacro(final DeviceStore.Macro macro) {
        new AlertDialog.Builder(this)
                .setTitle("Delete macro?")
                .setMessage(macro.name)
                .setPositiveButton("Delete", (dialog, which) -> {
                    List<DeviceStore.Macro> macros = DeviceStore.macros(this, macroKey());
                    for (int i = macros.size() - 1; i >= 0; i--) {
                        if (macros.get(i).id.equals(macro.id)) macros.remove(i);
                    }
                    DeviceStore.saveMacros(this, macroKey(), macros);
                    renderMacroList();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openMacroEditor(DeviceStore.Macro source) {
        if (macroContent == null || current == null) return;
        final DeviceStore.Macro macro = new DeviceStore.Macro(
                source == null ? UUID.randomUUID().toString() : source.id,
                source == null ? "" : source.name);
        macro.loop = source == null ? 0 : source.loop;
        if (source != null) {
            for (DeviceStore.MacroStep step : source.steps) {
                macro.steps.add(new DeviceStore.MacroStep(step.key, step.delayMs));
            }
        }

        macroContent.removeAllViews();
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        Button back = styledButton("\u2039", BTN, 20);
        back.setContentDescription("Back to macros");
        back.setOnClickListener(v -> renderMacroList());
        head.addView(back, new LinearLayout.LayoutParams(dp(42), dp(42)));
        TextView title = text(source == null ? "New macro" : "Edit macro", 22, TEXT, true);
        title.setPadding(dp(10), 0, 0, 0);
        head.addView(title, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        macroContent.addView(head);

        TextView nameHeader = text("NAME", 10, brandAccent, true);
        nameHeader.setLetterSpacing(0.14f);
        nameHeader.setPadding(0, dp(14), 0, dp(4));
        macroContent.addView(nameHeader);

        final EditText name = new EditText(this);
        name.setText(macro.name);
        name.setHint("Watch TV");
        name.setHintTextColor(MUTED);
        name.setTextColor(TEXT);
        name.setSingleLine(true);
        name.setBackgroundTintList(ColorStateList.valueOf(ACCENT));
        macroContent.addView(name, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));

        TextView stepsHeader = text("STEPS", 10, brandAccent, true);
        stepsHeader.setLetterSpacing(0.14f);
        stepsHeader.setPadding(0, dp(16), 0, dp(6));
        macroContent.addView(stepsHeader);

        final LinearLayout stepList = new LinearLayout(this);
        stepList.setOrientation(LinearLayout.VERTICAL);
        macroContent.addView(stepList);
        renderMacroSteps(macro, stepList);

        Button addStep = styledButton("+  Add a button", BTN, 14);
        addStep.setOnClickListener(v -> showStepPicker(macro, stepList));
        LinearLayout.LayoutParams al = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        al.topMargin = dp(8);
        macroContent.addView(addStep, al);

        TextView repeatHeader = text("REPEAT", 10, brandAccent, true);
        repeatHeader.setLetterSpacing(0.14f);
        repeatHeader.setPadding(0, dp(16), 0, dp(6));
        macroContent.addView(repeatHeader);
        final LinearLayout loopRow = new LinearLayout(this);
        loopRow.setOrientation(LinearLayout.HORIZONTAL);
        macroContent.addView(loopRow);
        renderLoopChips(loopRow, macro);

        Button save = highlightButton("Save macro", 15, brandAccent);
        save.setOnClickListener(v -> {
            String text = name.getText().toString().trim();
            if (text.isEmpty()) {
                name.setError("Give this macro a name");
                name.requestFocus();
                return;
            }
            if (macro.steps.isEmpty()) {
                Toast.makeText(this, "Add at least one step first.", Toast.LENGTH_SHORT).show();
                return;
            }
            macro.name = text;
            List<DeviceStore.Macro> macros = DeviceStore.macros(this, macroKey());
            boolean replaced = false;
            for (int i = 0; i < macros.size(); i++) {
                if (macros.get(i).id.equals(macro.id)) {
                    macros.set(i, macro);
                    replaced = true;
                    break;
                }
            }
            if (!replaced) macros.add(macro);
            DeviceStore.saveMacros(this, macroKey(), macros);
            Toast.makeText(this, "Saved \u201c" + macro.name + "\u201d",
                    Toast.LENGTH_SHORT).show();
            renderMacroList();
        });
        LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        sl.topMargin = dp(16);
        macroContent.addView(save, sl);

        Button cancel = styledButton("Cancel", BTN, 13);
        cancel.setOnClickListener(v -> renderMacroList());
        LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        cl.topMargin = dp(8);
        macroContent.addView(cancel, cl);
    }

    private void renderLoopChips(LinearLayout row, DeviceStore.Macro macro) {
        row.removeAllViews();
        final int[] values = {0, 2, 5, -1};
        final String[] labels = {"Once", "\u00d72", "\u00d75", "\u221e"};
        for (int i = 0; i < values.length; i++) {
            final int value = values[i];
            Button b = styledButton(labels[i], macro.loop == value ? ACTIVE : BTN, 12);
            if (macro.loop == value) b.setTextColor(brandAccent);
            b.setOnClickListener(v -> {
                macro.loop = value;
                renderLoopChips(row, macro);
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(44), 1f);
            lp.setMargins(dp(i == 0 ? 0 : 3), 0, dp(i == 3 ? 0 : 3), 0);
            row.addView(b, lp);
        }
    }

    private void renderMacroSteps(final DeviceStore.Macro macro,
                                  final LinearLayout parent) {
        parent.removeAllViews();
        if (macro.steps.isEmpty()) {
            TextView empty = text("No steps yet. Add the buttons in the order you "
                    + "want them sent.", 12, MUTED, false);
            empty.setPadding(dp(2), dp(4), 0, dp(6));
            parent.addView(empty);
            return;
        }
        for (int i = 0; i < macro.steps.size(); i++) {
            final int index = i;
            final DeviceStore.MacroStep step = macro.steps.get(i);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(dp(12), dp(8), dp(12), dp(8));
            row.setBackground(Surface.raised(this, 16, BTN, STROKE,
                    palette.gloss(0.08f)));
            LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rl.bottomMargin = dp(8);
            parent.addView(row, rl);

            LinearLayout top = new LinearLayout(this);
            top.setOrientation(LinearLayout.HORIZONTAL);
            top.setGravity(Gravity.CENTER_VERTICAL);
            top.addView(text((i + 1) + ". " + labelFor(step.key), 14, TEXT, true),
                    new LinearLayout.LayoutParams(0,
                            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            Button up = styledButton("\u2191", BTN, 13);
            up.setContentDescription("Move step up");
            up.setOnClickListener(v -> {
                if (index > 0) {
                    Collections.swap(macro.steps, index, index - 1);
                    renderMacroSteps(macro, parent);
                }
            });
            Button down = styledButton("\u2193", BTN, 13);
            down.setContentDescription("Move step down");
            down.setOnClickListener(v -> {
                if (index < macro.steps.size() - 1) {
                    Collections.swap(macro.steps, index, index + 1);
                    renderMacroSteps(macro, parent);
                }
            });
            Button remove = styledButton("\u00d7", Surface.blend(CARD, RED, 0.18f), 16);
            remove.setTextColor(RED);
            remove.setContentDescription("Remove step");
            remove.setOnClickListener(v -> {
                macro.steps.remove(index);
                renderMacroSteps(macro, parent);
            });
            top.addView(up, new LinearLayout.LayoutParams(dp(32), dp(32)));
            top.addView(down, new LinearLayout.LayoutParams(dp(32), dp(32)));
            top.addView(remove, new LinearLayout.LayoutParams(dp(32), dp(32)));
            row.addView(top);

            LinearLayout pause = new LinearLayout(this);
            pause.setOrientation(LinearLayout.HORIZONTAL);
            pause.setGravity(Gravity.CENTER_VERTICAL);
            pause.setPadding(0, dp(4), 0, 0);
            TextView label = text("Pause after", 11, MUTED, false);
            label.setWidth(dp(84));
            pause.addView(label);
            final int[] delays = {200, 400, 600, 1000, 1500, 2500};
            Spinner spinner = new Spinner(this);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item,
                    new String[]{"0.2 s", "0.4 s", "0.6 s", "1 s", "1.5 s", "2.5 s"});
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
            int best = 0;
            for (int d = 0; d < delays.length; d++) {
                if (Math.abs(delays[d] - step.delayMs)
                        < Math.abs(delays[best] - step.delayMs)) best = d;
            }
            spinner.setSelection(best);
            spinner.setOnItemSelectedListener(new SimpleItemListener() {
                @Override
                public void onItemSelected(int position) {
                    step.delayMs = delays[position];
                }
            });
            pause.addView(spinner, new LinearLayout.LayoutParams(0, dp(44), 1f));
            row.addView(pause);
        }
    }

    private void showStepPicker(final DeviceStore.Macro macro,
                                final LinearLayout stepList) {
        if (current == null) return;
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(18), dp(18), dp(18), dp(16));
        panel.setBackground(Surface.raised(this, 28, CARD, palette.rim(),
                palette.gloss(0.10f)));
        Surface.elevate(panel, 28, 12);
        panel.addView(text("Add a button", 22, TEXT, true));
        TextView note = text("Everything this remote carries is listed here \u2014 "
                + "including keys added from sibling models.", 11, MUTED, false);
        note.setPadding(0, dp(2), 0, dp(8));
        panel.addView(note);

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        panel.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        List<String> keys = new ArrayList<>(current.buttons.keySet());
        Collections.sort(keys, (a, b) -> {
            int rank = Integer.compare(buttonRank(a), buttonRank(b));
            return rank != 0 ? rank : labelFor(a).compareToIgnoreCase(labelFor(b));
        });
        for (final String key : keys) {
            TextView row = text(labelFor(key), 15, TEXT, false);
            row.setPadding(dp(10), dp(12), dp(10), dp(12));
            row.setOnClickListener(v -> {
                macro.steps.add(new DeviceStore.MacroStep(key, 600));
                dialog.dismiss();
                renderMacroSteps(macro, stepList);
            });
            list.addView(row);
        }

        Button cancel = styledButton("Cancel", BTN, 13);
        cancel.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        cl.topMargin = dp(8);
        panel.addView(cancel, cl);

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

    private void runMacro(final DeviceStore.Macro macro) {
        runMacro(macro, macro.loop);
    }

    private void runMacro(final DeviceStore.Macro macro, final int loopMode) {
        if (current == null || macro.steps.isEmpty()) return;
        final Remote remote = current;
        final List<DeviceStore.MacroStep> steps = new ArrayList<>(macro.steps);
        final String deviceKey = macroKeyFor(remote);
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
        if (irExecutor.isShutdown() || !irBusy.compareAndSet(false, true)) {
            status.setText("IR is busy \u2014 wait a moment and run it again.");
            return;
        }
        macroCancelled.set(false);

        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(22), dp(22), dp(22), dp(18));
        panel.setBackground(Surface.raised(this, 28, CARD, palette.rim(),
                palette.gloss(0.10f)));
        Surface.elevate(panel, 28, 12);

        panel.addView(text("RUNNING MACRO", 10, brandAccent, true));
        TextView title = text(macro.name, 24, TEXT, true);
        title.setPadding(0, dp(2), 0, dp(6));
        panel.addView(title);
        final TextView detail = text(loopMode < 0
                ? "Starting\u2026 looping until you stop it."
                : "Starting\u2026", 12, MUTED, false);
        panel.addView(detail);

        final ProgressBar progress = new ProgressBar(this, null,
                android.R.attr.progressBarStyleHorizontal);
        progress.setMax(steps.size());
        progress.setProgress(0);
        LinearLayout.LayoutParams pl = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(8));
        pl.topMargin = dp(12);
        panel.addView(progress, pl);

        Button stop = styledButton("Stop macro", BTN, 13);
        stop.setOnClickListener(v -> {
            macroCancelled.set(true);
            dialog.dismiss();
            status.setText("Macro stopping\u2026");
        });
        LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        sl.topMargin = dp(14);
        panel.addView(stop, sl);
        dialog.setOnCancelListener(d -> macroCancelled.set(true));
        dialog.setContentView(panel);
        dialog.show();
        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(w.getAttributes());
            lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.88f);
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            w.setAttributes(lp);
        }

        irExecutor.execute(() -> runMacroLoop(macro, loopMode, remote, deviceKey,
                steps, jack, dialog, detail, progress));
    }

    private void runMacroLoop(DeviceStore.Macro macro, int loopMode, Remote remote,
                              String deviceKey, List<DeviceStore.MacroStep> steps,
                              boolean jack, Dialog dialog, TextView detail,
                              ProgressBar progress) {
        String failure = null;
        int presses = 0;
        int loops = 0;
        boolean stopped = false;
        DeviceStore.Calibration cal = DeviceStore.calibration(this, deviceKey);
        int macroRepeats = cal != null ? cal.repeats
                : (DeviceStore.boostAll(this) ? 2 : 1);
        try {
            boolean forever = loopMode < 0;
            int target = loopMode <= 0 ? 1 : loopMode;
            while (!macroCancelled.get() && (forever || loops < target)) {
                loops++;
                for (int i = 0; i < steps.size() && !macroCancelled.get(); i++) {
                    DeviceStore.MacroStep step = steps.get(i);
                    final int shownLoop = loops;
                    final int shownStep = i + 1;
                    final int shownPresses = presses;
                    runOnUiThread(() -> {
                        if (!dialog.isShowing()) return;
                        String loopText = forever ? "loop \u221e"
                                : "loop " + shownLoop + "/" + target;
                        detail.setText(loopText + " \u00b7 press " + shownPresses
                                + " \u00b7 " + shownStep + "/" + steps.size()
                                + "  " + labelFor(step.key));
                        progress.setProgress(shownStep - 1);
                    });
                    Code code = step.key.startsWith("ac_")
                            ? acStateCode(step.key, remote)
                            : remote.buttons.get(step.key);
                    if (code != null) {
                        // Never multi-fire a power code inside a sequence: a repeated
                        // toggle frame would turn the device straight back off.
                        boolean powerStep = "power".equals(step.key)
                                || "power_off".equals(step.key);
                        sendCodeBlocking(code, jack, deviceKey,
                                powerStep ? 1 : macroRepeats);
                        presses++;
                    }
                    sleepWhile(step.delayMs, macroCancelled);
                }
                if (!forever && loops >= target) break;
                sleepWhile(400, macroCancelled);
            }
        } catch (Exception e) {
            failure = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        } finally {
            stopped = macroCancelled.get();
            irBusy.set(false);
            final String why = failure;
            final int sent = presses;
            final int rounds = loops;
            final boolean wasStopped = stopped;
            runOnUiThread(() -> {
                if (dialog.isShowing()) dialog.dismiss();
                if (why != null) {
                    status.setText("Macro stopped: " + why);
                } else if (wasStopped) {
                    status.setText("Macro stopped after " + sent + " press"
                            + (sent == 1 ? "" : "es") + ".");
                } else {
                    status.setText("Ran \u201c" + macro.name + "\u201d \u00b7 "
                            + sent + " press" + (sent == 1 ? "" : "es")
                            + (rounds > 1 ? " \u00b7 " + rounds + " loops" : "") + ".");
                }
            });
        }
    }

    // ----------------------------------------------------------- test storm

    private void showStormDialog() {
        if (current == null) {
            status.setText("Pick a device remote first.");
            return;
        }
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(20), dp(20), dp(20), dp(18));
        panel.setBackground(Surface.raised(this, 28, CARD, palette.rim(),
                palette.gloss(0.10f)));
        Surface.elevate(panel, 28, 12);

        panel.addView(text("Test Storm", 24, TEXT, true));
        TextView note = text("Fires every button on this remote in a shuffled order, "
                + "pass after pass, so you can see which ones the device answers. It "
                + "keeps going until you stop it.", 12, MUTED, false);
        note.setPadding(0, dp(4), 0, dp(10));
        panel.addView(note);

        TextView speedHeader = text("SPEED BETWEEN PRESSES", 10, brandAccent, true);
        speedHeader.setLetterSpacing(0.12f);
        panel.addView(speedHeader);

        final int[] speeds = {150, 250, 350, 800};
        final String[] labels = {"Fast", "0.25 s", "0.35 s", "0.8 s"};
        LinearLayout speedRow = new LinearLayout(this);
        speedRow.setOrientation(LinearLayout.HORIZONTAL);
        speedRow.setPadding(0, dp(4), 0, dp(8));
        for (int i = 0; i < speeds.length; i++) {
            final int delay = speeds[i];
            Button b = styledButton(labels[i],
                    DeviceStore.stormDelayMs(this) == delay ? ACTIVE : BTN, 12);
            if (DeviceStore.stormDelayMs(this) == delay) b.setTextColor(brandAccent);
            b.setOnClickListener(v -> {
                DeviceStore.setStormDelayMs(this, delay);
                for (int j = 0; j < speedRow.getChildCount(); j++) {
                    Button chip = (Button) speedRow.getChildAt(j);
                    chip.setBackground(Surface.raised(this, 20, BTN, palette.rim(),
                            palette.gloss(0.14f)));
                    chip.setTextColor(TEXT);
                }
                b.setBackground(Surface.raised(this, 20, ACTIVE, STROKE,
                        palette.gloss(0.18f)));
                b.setTextColor(brandAccent);
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(44), 1f);
            lp.setMargins(dp(i == 0 ? 0 : 3), 0, dp(i == 3 ? 0 : 3), 0);
            speedRow.addView(b, lp);
        }
        panel.addView(speedRow);

        final CheckBox power = new CheckBox(this);
        power.setText("Include POWER and POWER OFF");
        power.setTextColor(TEXT);
        power.setButtonTintList(ColorStateList.valueOf(ACCENT));
        power.setChecked(DeviceStore.stormIncludePower(this));
        power.setOnCheckedChangeListener((buttonView, checked) ->
                DeviceStore.setStormIncludePower(this, checked));
        panel.addView(power);

        TextView warning = text("Power is left out by default so the device stays on "
                + "while you test the rest. Some aircon frames take a second to send; "
                + "the storm waits for each one.", 11, MUTED, false);
        warning.setPadding(dp(4), dp(0), dp(4), dp(10));
        panel.addView(warning);

        Button start = highlightButton("Start storm", 16, brandAccent);
        start.setOnClickListener(v -> {
            dialog.dismiss();
            startStorm();
        });
        panel.addView(start, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(54)));

        Button cancel = styledButton("Cancel", BTN, 13);
        cancel.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        cl.topMargin = dp(8);
        panel.addView(cancel, cl);

        dialog.setContentView(panel);
        dialog.show();
        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(w.getAttributes());
            lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.92f);
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            w.setAttributes(lp);
        }
    }

    private void startStorm() {
        if (current == null) return;
        final Remote remote = current;
        boolean includePower = DeviceStore.stormIncludePower(this);
        final List<String> keys = new ArrayList<>();
        for (String key : remote.buttons.keySet()) {
            if (!includePower && ("power".equals(key) || "power_off".equals(key))) continue;
            keys.add(key);
        }
        if (keys.isEmpty()) {
            status.setText("This remote has no buttons to storm.");
            return;
        }
        final String deviceKey = macroKeyFor(remote);
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
        if (irExecutor.isShutdown() || !irBusy.compareAndSet(false, true)) {
            status.setText("IR is busy \u2014 wait a moment and start again.");
            return;
        }
        stormCancelled.set(false);

        stormDialog = new Dialog(this);
        stormDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(22), dp(22), dp(22), dp(18));
        panel.setBackground(Surface.raised(this, 28, CARD, palette.rim(),
                palette.gloss(0.10f)));
        Surface.elevate(panel, 28, 12);

        panel.addView(text("TEST STORM RUNNING", 10, brandAccent, true));
        final TextView count = text("0", 46, TEXT, true);
        count.setPadding(0, dp(2), 0, 0);
        panel.addView(count);
        final TextView now = text("Starting\u2026", 13, MUTED, false);
        panel.addView(now);
        final TextView pressLog = text("", 11, MUTED, false);
        pressLog.setPadding(0, dp(8), 0, 0);
        panel.addView(pressLog);

        final Button stop = styledButton("STOP STORM", Surface.blend(CARD, RED, 0.22f), 17);
        stop.setTextColor(RED);
        stop.setOnClickListener(v -> {
            stormCancelled.set(true);
            if (stormDialog != null && stormDialog.isShowing()) stormDialog.dismiss();
            status.setText("Test Storm stopping\u2026");
        });
        LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(64));
        sl.topMargin = dp(16);
        panel.addView(stop, sl);
        stormDialog.setOnCancelListener(d -> stormCancelled.set(true));
        stormDialog.setContentView(panel);
        stormDialog.show();
        Window w = stormDialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(w.getAttributes());
            lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90f);
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            w.setAttributes(lp);
        }

        irExecutor.execute(() -> runStorm(remote, deviceKey, keys, jack, count, now, pressLog));
    }

    private void runStorm(Remote remote, String deviceKey, List<String> keys,
                          boolean jack, TextView count, TextView now, TextView pressLog) {
        int presses = 0;
        int pass = 0;
        List<String> deck = new ArrayList<>(keys);
        Collections.shuffle(deck, stormRandom);
        String last = null;
        StringBuilder log = new StringBuilder();
        try {
            while (!stormCancelled.get()) {
                // Work through a shuffled deck so every button is exercised once
                // per pass, then reshuffle. Random picks alone can miss keys.
                if (deck.isEmpty()) {
                    pass++;
                    deck = new ArrayList<>(keys);
                    Collections.shuffle(deck, stormRandom);
                    if (keys.size() > 1 && deck.get(0).equals(last)) {
                        Collections.swap(deck, 0, 1);
                    }
                }
                String key = deck.remove(0);
                Code code = key.startsWith("ac_")
                        ? acStateCode(key, remote)
                        : remote.buttons.get(key);
                if (code == null) continue;
                last = key;
                sendCodeBlocking(code, jack, deviceKey, 1);
                presses++;
                final int shown = presses;
                final int shownPass = pass + 1;
                final String shownKey = labelFor(key);
                runOnUiThread(() -> {
                    count.setText(String.valueOf(shown));
                    now.setText("Now: " + shownKey + " \u00b7 pass " + shownPass);
                    log.append(shown == 1 ? "" : " \u00b7 ").append(shownKey);
                    if (log.length() > 120) {
                        int cut = log.indexOf(" \u00b7 ", log.length() - 80);
                        if (cut > 0) log.delete(0, cut + 3);
                    }
                    pressLog.setText(log.toString());
                });
                int delay = DeviceStore.stormDelayMs(this);
                sleepWhile(delay + stormRandom.nextInt(80), stormCancelled);
            }
        } catch (Exception e) {
            final String why = e.getMessage() == null
                    ? e.getClass().getSimpleName() : e.getMessage();
            runOnUiThread(() -> status.setText("Test Storm stopped: " + why));
        } finally {
            boolean stopped = stormCancelled.get();
            irBusy.set(false);
            final int sent = presses;
            runOnUiThread(() -> {
                if (stormDialog != null && stormDialog.isShowing()) stormDialog.dismiss();
                if (stopped) {
                    status.setText("Test Storm stopped after " + sent + " press"
                            + (sent == 1 ? "" : "es") + ".");
                }
            });
        }
    }

    // ------------------------------------------------------------ signal lab

    private List<Integer> supportedFrequencies() {
        List<Integer> out = new ArrayList<>();
        int[] candidates = {33000, 36000, 38000, 40000, 56000};
        if (ir != null) {
            try {
                ConsumerIrManager.CarrierFrequencyRange[] ranges = ir.getCarrierFrequencies();
                if (ranges != null && ranges.length > 0) {
                    for (int candidate : candidates) {
                        for (ConsumerIrManager.CarrierFrequencyRange range : ranges) {
                            if (candidate >= range.getMinFrequency()
                                    && candidate <= range.getMaxFrequency()) {
                                if (!out.contains(candidate)) out.add(candidate);
                                break;
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        if (out.isEmpty()) {
            out.add(36000);
            out.add(38000);
            out.add(40000);
        }
        return out;
    }

    private boolean sendRawTest(Code code, int freq, int repeats, int gapMs) {
        boolean jack = emitterState() == EMITTER_JACK;
        if (!jack) {
            if (ir == null) {
                status.setText("No IR service on this phone.");
                return false;
            }
            if (!ir.hasIrEmitter() && !DeviceStore.irOverride(this)) {
                status.setText(noEmitterMessage());
                return false;
            }
        }
        if (irExecutor.isShutdown() || !irBusy.compareAndSet(false, true)) {
            status.setText("IR is busy \u2014 wait a moment and try again.");
            return false;
        }
        final int count = Math.max(1, Math.min(3, repeats));
        final int gap = Math.max(20, gapMs);
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
                        ir.transmit(carrierFor(freq), code.pattern);
                    }
                    if (i + 1 < count) Thread.sleep(gap);
                }
            } catch (Exception e) {
                failure = e.getMessage() == null
                        ? e.getClass().getSimpleName() : e.getMessage();
            } finally {
                irBusy.set(false);
                final String why = failure;
                if (why != null) runOnUiThread(() ->
                        status.setText("Test send failed: " + why));
            }
        });
        return true;
    }

    private void showSignalLab() {
        if (current == null) {
            status.setText("Pick a device remote first.");
            return;
        }
        final List<String> keys = new ArrayList<>(current.buttons.keySet());
        if (keys.isEmpty()) {
            status.setText("This remote has no codes to test.");
            return;
        }
        Collections.sort(keys, (a, b) -> {
            int rank = Integer.compare(buttonRank(a), buttonRank(b));
            return rank != 0 ? rank : labelFor(a).compareToIgnoreCase(labelFor(b));
        });
        final List<Integer> freqs = supportedFrequencies();

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(20), dp(20), dp(20), dp(18));
        panel.setBackground(Surface.raised(this, 28, CARD, palette.rim(),
                palette.gloss(0.10f)));
        Surface.elevate(panel, 28, 12);

        panel.addView(text("Signal Lab", 24, TEXT, true));
        TextView note = text("IR is one-way: the phone cannot hear whether the device "
                + "answered. Send the same button at different timings, then save the "
                + "one that worked. The setting follows this remote profile.", 12, MUTED, false);
        note.setPadding(0, dp(4), 0, dp(10));
        panel.addView(note);

        final int[] sel = {freqs.contains(38000) ? 38000 : freqs.get(0), 1, 50};
        final String[] testKey = {keys.get(0)};

        Spinner keySpinner = new Spinner(this);
        List<String> keyLabels = new ArrayList<>();
        for (String key : keys) keyLabels.add(labelFor(key));
        ArrayAdapter<String> ka = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, keyLabels);
        ka.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        keySpinner.setAdapter(ka);
        inDialogRow(panel, "Button", keySpinner);

        Spinner freqSpinner = new Spinner(this);
        List<String> freqLabels = new ArrayList<>();
        int freqIndex = 0;
        for (int i = 0; i < freqs.size(); i++) {
            freqLabels.add(freqs.get(i) / 1000 + " kHz");
            if (freqs.get(i) == sel[0]) freqIndex = i;
        }
        ArrayAdapter<String> fa = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, freqLabels);
        fa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        freqSpinner.setAdapter(fa);
        freqSpinner.setSelection(freqIndex);
        inDialogRow(panel, "Carrier", freqSpinner);

        Spinner repeatsSpinner = new Spinner(this);
        ArrayAdapter<String> ra = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"1 frame", "2 frames", "3 frames"});
        ra.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        repeatsSpinner.setAdapter(ra);
        inDialogRow(panel, "Repeat", repeatsSpinner);

        Spinner gapSpinner = new Spinner(this);
        final int[] gaps = {25, 50, 120, 300};
        ArrayAdapter<String> ga = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"25 ms", "50 ms", "120 ms", "300 ms"});
        ga.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        gapSpinner.setAdapter(ga);
        gapSpinner.setSelection(1);
        inDialogRow(panel, "Gap", gapSpinner);

        final TextView summary = text("", 11, MUTED, false);
        summary.setPadding(0, dp(6), 0, dp(8));
        panel.addView(summary);
        updateSignalSummary(summary, testKey[0], sel);

        keySpinner.setOnItemSelectedListener(new SimpleItemListener() {
            @Override
            public void onItemSelected(int position) {
                testKey[0] = keys.get(position);
                updateSignalSummary(summary, testKey[0], sel);
            }
        });
        freqSpinner.setOnItemSelectedListener(new SimpleItemListener() {
            @Override
            public void onItemSelected(int position) {
                sel[0] = freqs.get(position);
                updateSignalSummary(summary, testKey[0], sel);
            }
        });
        repeatsSpinner.setOnItemSelectedListener(new SimpleItemListener() {
            @Override
            public void onItemSelected(int position) {
                sel[1] = position + 1;
                updateSignalSummary(summary, testKey[0], sel);
            }
        });
        gapSpinner.setOnItemSelectedListener(new SimpleItemListener() {
            @Override
            public void onItemSelected(int position) {
                sel[2] = gaps[position];
                updateSignalSummary(summary, testKey[0], sel);
            }
        });

        DeviceStore.Calibration existing = DeviceStore.calibration(this, macroKey());
        TextView saved = text(existing == null
                ? "No calibration saved for this remote."
                : "Saved: " + existing.freq / 1000 + " kHz \u00b7 "
                + existing.repeats + " frame" + (existing.repeats == 1 ? "" : "s")
                + " \u00b7 " + existing.gapMs + " ms gap",
                11, existing == null ? MUTED : GREEN, false);
        saved.setPadding(0, 0, 0, dp(8));
        panel.addView(saved);

        if (emitterState() == EMITTER_JACK) {
            TextView jackNote = text("Jack blaster is active. Timing is generated in "
                    + "software, so carrier and gap changes do not apply until you switch "
                    + "back to the built-in emitter.", 11, MUTED, false);
            jackNote.setPadding(0, 0, 0, dp(8));
            panel.addView(jackNote);
        }

        Button test = highlightButton("Send test", 15, brandAccent);
        test.setOnClickListener(v -> {
            Code code = testKey[0].startsWith("ac_")
                    ? acStateCode(testKey[0], current)
                    : current.buttons.get(testKey[0]);
            if (code == null) return;
            if (sendRawTest(code, sel[0], sel[1], sel[2])) {
                status.setText("Testing " + labelFor(testKey[0]) + " at "
                        + sel[0] / 1000 + " kHz \u00b7 " + sel[1] + " frame"
                        + (sel[1] == 1 ? "" : "s") + ".");
            }
        });
        panel.addView(test, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));

        Button save = styledButton("It worked \u2014 save this timing", BTN, 14);
        save.setOnClickListener(v -> {
            DeviceStore.Calibration cal = new DeviceStore.Calibration();
            cal.freq = sel[0];
            cal.repeats = sel[1];
            cal.gapMs = sel[2];
            DeviceStore.saveCalibration(this, macroKey(), cal);
            status.setText("Saved " + sel[0] / 1000 + " kHz \u00b7 " + sel[1]
                    + " frame" + (sel[1] == 1 ? "" : "s") + " for this remote.");
            dialog.dismiss();
        });
        LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        sl.topMargin = dp(8);
        panel.addView(save, sl);

        if (existing != null) {
            Button clear = styledButton("Clear saved timing", BTN, 12);
            clear.setTextColor(RED);
            clear.setOnClickListener(v -> {
                DeviceStore.clearCalibration(this, macroKey());
                status.setText("Calibration cleared. Defaults are back.");
                dialog.dismiss();
            });
            LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(42));
            cl.topMargin = dp(8);
            panel.addView(clear, cl);
        }

        Button close = styledButton("Close", BTN, 13);
        close.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(42));
        cl.topMargin = dp(8);
        panel.addView(close, cl);

        dialog.setContentView(panel);
        dialog.show();
        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(w.getAttributes());
            lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.92f);
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            w.setAttributes(lp);
        }
    }

    private void updateSignalSummary(TextView out, String key, int[] sel) {
        out.setText("Test code: " + labelFor(key) + " \u00b7 " + sel[0] / 1000
                + " kHz \u00b7 " + sel[1] + " frame" + (sel[1] == 1 ? "" : "s")
                + " \u00b7 " + sel[2] + " ms gap");
    }

    private void inDialogRow(LinearLayout panel, String label, Spinner spinner) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(3), 0, dp(3));
        TextView name = text(label, 13, MUTED, false);
        name.setWidth(dp(72));
        row.addView(name);
        spinner.setBackgroundTintList(ColorStateList.valueOf(ACCENT));
        row.addView(spinner, new LinearLayout.LayoutParams(0, dp(46), 1f));
        panel.addView(row);
    }

    // ---------------------------------------------------------- power doctor

    private static class PowerCandidate {
        String label;
        Code code;
        int repeats;
    }

    private List<PowerCandidate> powerCandidates() {
        List<PowerCandidate> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        addPowerCandidate(out, seen, "This profile \u00b7 POWER OFF",
                current.buttons.get("power_off"), 1);
        addPowerCandidate(out, seen, "This profile \u00b7 POWER OFF \u00d72",
                current.buttons.get("power_off"), 2);
        addPowerCandidate(out, seen, "This profile \u00b7 POWER OFF \u00d73",
                current.buttons.get("power_off"), 3);
        addPowerCandidate(out, seen, "This profile \u00b7 POWER",
                current.buttons.get("power"), 1);
        addPowerCandidate(out, seen, "This profile \u00b7 POWER \u00d72",
                current.buttons.get("power"), 2);

        List<Brand> ordered = new ArrayList<>();
        for (Brand b : brands) {
            if (b.name.equals(current.brand)) ordered.add(0, b);
            else ordered.add(b);
        }
        for (Brand b : ordered) {
            for (Remote r : b.remotes) {
                if (r == current) continue;
                if (out.size() >= 28) break;
                addPowerCandidate(out, seen, prettyBrand(b.name) + " \u2014 "
                                + pretty(r.name) + " \u00b7 OFF",
                        r.buttons.get("power_off"), 1);
                addPowerCandidate(out, seen, prettyBrand(b.name) + " \u2014 "
                                + pretty(r.name) + " \u00b7 POWER",
                        r.buttons.get("power"), 1);
            }
        }
        return out;
    }

    private void addPowerCandidate(List<PowerCandidate> out, Set<String> seen,
                                   String label, Code code, int repeats) {
        if (code == null || code.pattern == null) return;
        String signature = code.freq + ":" + Arrays.hashCode(code.pattern) + ":" + repeats;
        if (!seen.add(signature)) return;
        PowerCandidate c = new PowerCandidate();
        c.label = label;
        c.code = code;
        c.repeats = repeats;
        out.add(c);
    }

    private void showPowerDoctor() {
        if (current == null) {
            status.setText("Pick a device remote first.");
            return;
        }
        final List<PowerCandidate> candidates = powerCandidates();
        if (candidates.isEmpty()) {
            status.setText("No power codes to test in this category.");
            return;
        }

        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(20), dp(20), dp(20), dp(18));
        panel.setBackground(Surface.raised(this, 28, CARD, palette.rim(),
                palette.gloss(0.10f)));
        Surface.elevate(panel, 28, 12);

        panel.addView(text("Power Doctor", 24, TEXT, true));
        TextView note = text("For aircon units where Off only lowers the temperature. "
                + "Send a candidate. If the unit turns off, save it as this remote's "
                + "POWER OFF. If it just changes the temperature, that was a state "
                + "frame \u2014 try the next one.", 12, MUTED, false);
        note.setPadding(0, dp(4), 0, dp(10));
        panel.addView(note);

        final TextView counter = text("", 10, brandAccent, true);
        counter.setLetterSpacing(0.10f);
        panel.addView(counter);
        final TextView candidateName = text("", 18, TEXT, true);
        candidateName.setPadding(0, dp(2), 0, dp(10));
        panel.addView(candidateName);

        final int[] index = {0};
        final Runnable show = () -> {
            PowerCandidate c = candidates.get(index[0]);
            counter.setText("CANDIDATE " + (index[0] + 1) + " OF " + candidates.size()
                    + (c.repeats > 1 ? " \u00b7 \u00d7" + c.repeats : ""));
            candidateName.setText(c.label);
        };
        show.run();

        Button send = highlightButton("Send candidate", 15, brandAccent);
        send.setOnClickListener(v -> {
            PowerCandidate c = candidates.get(index[0]);
            if (sendRawTest(c.code, c.code.freq, c.repeats, frameGapMs())) {
                status.setText("Sent candidate " + (index[0] + 1) + " \u00b7 " + c.label);
            }
        });
        panel.addView(send, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));

        Button save = styledButton("This turned it off \u2014 save", BTN, 14);
        save.setTextColor(GREEN);
        save.setOnClickListener(v -> {
            PowerCandidate c = candidates.get(index[0]);
            String key = macroKey();
            DeviceStore.putPatch(this, key, "power_off", c.code.freq, c.code.pattern);
            DeviceStore.setPowerOffRepeats(this, key, c.repeats);
            current.buttons.put("power_off", c.code);
            status.setText("Saved POWER OFF from " + c.label + ".");
            dialog.dismiss();
            renderControls();
        });
        LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(50));
        sl.topMargin = dp(8);
        panel.addView(save, sl);

        Button next = styledButton("Next candidate", BTN, 13);
        next.setOnClickListener(v -> {
            index[0] = (index[0] + 1) % candidates.size();
            show.run();
        });
        LinearLayout.LayoutParams nl = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        nl.topMargin = dp(8);
        panel.addView(next, nl);

        Button close = styledButton("Close", BTN, 13);
        close.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(42));
        cl.topMargin = dp(8);
        panel.addView(close, cl);

        dialog.setContentView(panel);
        dialog.show();
        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(w.getAttributes());
            lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.92f);
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            w.setAttributes(lp);
        }
    }

    // -------------------------------------------------------------- add keys

    private void showAddKey() {
        if (current == null) {
            status.setText("Pick a device remote first.");
            return;
        }
        List<Brand> ordered = new ArrayList<>();
        for (Brand b : brands) {
            if (b.name.equals(current.brand)) ordered.add(0, b);
            else ordered.add(b);
        }
        final Map<String, Remote> sources = new LinkedHashMap<>();
        for (Brand b : ordered) {
            for (Remote r : b.remotes) {
                if (r == current) continue;
                for (String key : r.buttons.keySet()) {
                    if (key.startsWith("__")) continue;
                    if ("power".equals(key) || "power_off".equals(key)) continue;
                    if (current.buttons.containsKey(key)) continue;
                    if (!sources.containsKey(key)) sources.put(key, r);
                }
            }
        }
        if (sources.isEmpty()) {
            status.setText("This remote already carries every extra button in "
                    + current.category + ".");
            return;
        }

        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(18), dp(18), dp(18), dp(16));
        panel.setBackground(Surface.raised(this, 28, CARD, palette.rim(),
                palette.gloss(0.10f)));
        Surface.elevate(panel, 28, 12);

        panel.addView(text("Add a missing button", 22, TEXT, true));
        TextView note = text("Copied from another " + current.category + " profile. "
                + "It is a real captured code, but not guaranteed to match your exact "
                + "model \u2014 test it before relying on it.", 11, MUTED, false);
        note.setPadding(0, dp(2), 0, dp(8));
        panel.addView(note);

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        panel.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        List<String> keys = new ArrayList<>(sources.keySet());
        Collections.sort(keys, (a, b) -> {
            int rank = Integer.compare(buttonRank(a), buttonRank(b));
            return rank != 0 ? rank : labelFor(a).compareToIgnoreCase(labelFor(b));
        });
        for (final String key : keys) {
            final Remote source = sources.get(key);
            final Code code = source.buttons.get(key);
            if (code == null) continue;
            list.addView(toolRow(labelFor(key),
                    prettyBrand(source.brand) + " \u2014 " + pretty(source.name),
                    v -> {
                        DeviceStore.putPatch(this, macroKey(), key, code.freq, code.pattern);
                        try {
                            JSONObject patch = DeviceStore.patches(this, macroKey());
                            JSONArray added = patch.optJSONArray("__added");
                            if (added == null) added = new JSONArray();
                            boolean has = false;
                            for (int i = 0; i < added.length(); i++) {
                                if (key.equals(added.optString(i))) has = true;
                            }
                            if (!has) added.put(key);
                            patch.put("__added", added);
                            DeviceStore.savePatches(this, macroKey(), patch);
                        } catch (Exception ignored) {
                        }
                        current.buttons.put(key, code);
                        status.setText("Added " + labelFor(key) + " from "
                                + prettyBrand(source.brand) + ".");
                        dialog.dismiss();
                        renderControls();
                    }));
        }

        try {
            JSONObject patch = DeviceStore.patches(this, macroKey());
            JSONArray added = patch.optJSONArray("__added");
            if (added != null && added.length() > 0) {
                panel.addView(toolRow("Remove added buttons",
                        added.length() + " button" + (added.length() == 1 ? "" : "s")
                                + " added by you", v -> {
                            try {
                                JSONObject p = DeviceStore.patches(this, macroKey());
                                JSONArray list2 = p.optJSONArray("__added");
                                if (list2 != null) {
                                    for (int i = 0; i < list2.length(); i++) {
                                        String key = list2.optString(i);
                                        current.buttons.remove(key);
                                        p.remove(key);
                                    }
                                }
                                p.remove("__added");
                                if (p.length() == 0) DeviceStore.clearPatches(this, macroKey());
                                else DeviceStore.savePatches(this, macroKey(), p);
                            } catch (Exception ignored) {
                            }
                            status.setText("Removed the buttons you added.");
                            dialog.dismiss();
                            renderControls();
                        }));
            }
        } catch (Exception ignored) {
        }

        Button close = styledButton("Close", BTN, 13);
        close.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        cl.topMargin = dp(8);
        panel.addView(close, cl);

        dialog.setContentView(panel);
        dialog.show();
        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(w.getAttributes());
            lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.94f);
            lp.height = (int) (getResources().getDisplayMetrics().heightPixels * 0.80f);
            w.setAttributes(lp);
        }
    }

    // ------------------------------------------------------------------ more

    private void showMore() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout panel = sheetPanel("More",
                "Grouped so the common things sit one tap away.");

        panel.addView(sectionLabel("REMOTE & DEVICE"));
        panel.addView(tileRow(
                tile("Device type", category.name + " \u00b7 tap to change",
                        v -> {
                            dialog.dismiss();
                            showCategoryPicker();
                        }),
                tile("Devices", "Switch, name, delete",
                        v -> {
                            dialog.dismiss();
                            startActivity(new Intent(this, ConfigsActivity.class));
                        })));
        panel.addView(tileRow(
                tile("Fetch remote", "Import a .ir capture",
                        v -> {
                            dialog.dismiss();
                            startActivityForResult(
                                    new Intent(this, LearnActivity.class), REQ_LEARN);
                        }),
                tile("Web shortcut", "Chrome link for Web",
                        v -> {
                            dialog.dismiss();
                            editWebUrl();
                        })));
        panel.addView(tileRow(
                tile("Shutdown sound", DeviceStore.shutdownSound(this)
                                ? "On \u00b7 plays before TV Power Off"
                                : "Off \u00b7 tap to set",
                        v -> {
                            dialog.dismiss();
                            showShutdownSoundDialog();
                        }),
                tile("Appearance", Palette.of(DeviceStore.themeIndex(this)).name,
                        v -> {
                            dialog.dismiss();
                            showThemePicker();
                        })));

        panel.addView(sectionLabel("TOOLS"));
        panel.addView(tileRow(
                tile("Macros", "Sequences \u00b7 loop or repeat",
                        v -> {
                            dialog.dismiss();
                            showMacroPage();
                        }),
                tile("Test Storm", "Sweep every button",
                        v -> {
                            dialog.dismiss();
                            showStormDialog();
                        })));
        panel.addView(tileRow(
                tile("Signal Lab", "Carrier, frames and gap",
                        v -> {
                            dialog.dismiss();
                            showSignalLab();
                        }),
                tile("Add a missing button", "Borrow a key from a sibling",
                        v -> {
                            dialog.dismiss();
                            showAddKey();
                        })));
        if ("Air Conditioner".equals(current == null ? "" : current.category)) {
            panel.addView(tileRow(
                    tile("Power Doctor", "Fix an aircon Off code",
                            v -> {
                                dialog.dismiss();
                                showPowerDoctor();
                            }),
                    null));
        }

        panel.addView(sectionLabel("APP"));
        panel.addView(tileRow(
                tile("IR & scan", "Boost and scan speed",
                        v -> {
                            dialog.dismiss();
                            startActivity(new Intent(this, ConfigsActivity.class));
                        }),
                tile("Jack blaster", "3.5 mm DIY emitter",
                        v -> {
                            dialog.dismiss();
                            showJackDialog();
                        })));
        panel.addView(tileRow(
                tile("Diagnostics", "IR status and phone info",
                        v -> {
                            dialog.dismiss();
                            showDiagnostics();
                        }),
                tile("Updates", "v" + Updater.currentVersion() + " \u00b7 check",
                        v -> {
                            dialog.dismiss();
                            showUpdatesDialog();
                        })));
        panel.addView(sectionLabel("SECURITY"));
        panel.addView(tileRow(
                tile("Unlock PIN", Vault.hasCustomPin(this) ? "Custom PIN set"
                                : "Default " + Vault.DEFAULT_PIN,
                        v -> {
                            dialog.dismiss();
                            showUnlockPinDialog();
                        }),
                tile("Panic wipe", "Erase everything now",
                        v -> {
                            dialog.dismiss();
                            showPanicWipe();
                        })));
        panel.addView(tileRow(
                tile("About", "Sources, limits, licence",
                        v -> {
                            dialog.dismiss();
                            showAbout();
                        }),
                null));

        addClose(panel, dialog);
        showSheet(dialog, panel, 0.86f);
    }

    // ----------------------------------------------------------- more helpers

    /** The bottom-sheet card shared by More, its pickers and the sound dialog. */
    private LinearLayout sheetPanel(String title, String subtitle) {
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

        panel.addView(text(title, 26, TEXT, true));
        TextView sub = text(subtitle, 12, MUTED, false);
        sub.setPadding(0, dp(2), 0, dp(10));
        panel.addView(sub);
        return panel;
    }

    private TextView sectionLabel(String value) {
        TextView header = text(value, 10, ACCENT, true);
        header.setLetterSpacing(0.14f);
        header.setPadding(0, dp(8), 0, dp(8));
        return header;
    }

    /** A compact two-line tile. Keeps the More sheet to a handful of rows. */
    private View tile(String title, String subtitle, View.OnClickListener click) {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setPadding(dp(12), dp(9), dp(12), dp(9));
        t.setBackground(Surface.raised(this, 16, CARD, STROKE, palette.gloss(0.09f)));
        Surface.elevate(t, 16, 2);
        Surface.sinkOnPress(t, 2);

        TextView name = text(title, 13, TEXT, true);
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        t.addView(name);

        TextView sub = text(subtitle, 10, MUTED, false);
        sub.setSingleLine(true);
        sub.setEllipsize(android.text.TextUtils.TruncateAt.END);
        sub.setPadding(0, dp(2), 0, 0);
        t.addView(sub);

        t.setOnClickListener(click);
        return t;
    }

    private View tileRow(View left, View right) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(58), 1f);
        lp.setMargins(0, 0, dp(4), dp(8));
        row.addView(left, lp);
        if (right != null) {
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(0, dp(58), 1f);
            rp.setMargins(dp(4), 0, 0, dp(8));
            row.addView(right, rp);
        } else {
            Space spacer = new Space(this);
            LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(0, 1, 1f);
            sp.setMargins(dp(4), 0, 0, dp(8));
            row.addView(spacer, sp);
        }
        return row;
    }

    private void addClose(LinearLayout panel, Dialog dialog) {
        Button close = styledButton("Close", BTN, 14);
        close.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        lp.topMargin = dp(10);
        panel.addView(close, lp);
    }

    private void showSheet(Dialog dialog, View panel, float heightFraction) {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.TRANSPARENT);
        scroll.setFillViewport(true);
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
            lp.height = (int) (getResources().getDisplayMetrics().heightPixels * heightFraction);
            lp.gravity = Gravity.BOTTOM;
            w.setAttributes(lp);
        }
    }

    private void showCategoryPicker() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout panel = sheetPanel("Device type",
                categories.size() + " categories \u00b7 current: " + category.name);
        for (int i = 0; i < categories.size(); i += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams a = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            a.setMargins(0, 0, dp(4), dp(8));
            row.addView(categoryChip(categories.get(i), dialog, i), a);
            if (i + 1 < categories.size()) {
                LinearLayout.LayoutParams b = new LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                b.setMargins(dp(4), 0, 0, dp(8));
                row.addView(categoryChip(categories.get(i + 1), dialog, i + 1), b);
            } else {
                Space spacer = new Space(this);
                LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(0, 1, 1f);
                sp.setMargins(dp(4), 0, 0, dp(8));
                row.addView(spacer, sp);
            }
            panel.addView(row);
        }
        addClose(panel, dialog);
        showSheet(dialog, panel, 0.86f);
    }

    private void showThemePicker() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout panel = sheetPanel("Appearance",
                "Ten orange-family themes. Saved devices and settings stay put.");
        panel.addView(themeGrid(dialog));
        addClose(panel, dialog);
        showSheet(dialog, panel, 0.82f);
    }

    private void showUpdatesDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout panel = sheetPanel("Updates",
                "You are on " + Updater.currentVersion() + " \u00b7 GitHub releases");
        String latest = Updater.knownLatest(this);
        panel.addView(toolRow("Check for updates",
                latest == null || latest.isEmpty()
                        ? "Ask GitHub for the newest release"
                        : "Last seen: " + latest + " \u00b7 tap to check again",
                v -> {
                    dialog.dismiss();
                    checkForUpdatesNow();
                }));
        panel.addView(switchRow("Auto update check",
                "One quiet check per app open",
                Updater.enabled(this), on -> {
                    Updater.setEnabled(this, on);
                    status.setText(on ? "Update checks are on."
                            : "Update checks are off. The app makes no network calls.");
                }));
        addClose(panel, dialog);
        showSheet(dialog, panel, 0.58f);
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
        current = null;
        useCategory(index, () -> {
            if (!brands.isEmpty()) {
                populateRemotes(0);
                if (current != null) saveCurrent(current);
            }
            updateDeviceLabels();
        });
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
            if (!cat.loaded) {
                try {
                    hydrate(cat);
                } catch (Exception ignored) {
                    continue;
                }
            }
            Brand learned = null;
            for (Brand b : cat.brands) {
                if (b.name.equals("Learned")) learned = b;
            }
            String name = o.optString("name", "Learned remote");
            if (learned != null) {
                boolean exists = false;
                for (Remote r : learned.remotes) if (r.name.equals(name)) exists = true;
                if (exists) continue;
            } else {
                learned = new Brand();
                learned.name = "Learned";
                cat.brands.add(0, learned);
            }
            Remote remote = new Remote();
            remote.category = cat.name;
            remote.brand = learned.name;
            remote.name = name;
            JSONObject btns = o.optJSONObject("buttons");
            if (btns == null) continue;
            Iterator<String> learnedKeys = btns.keys();
            while (learnedKeys.hasNext()) {
                String key = learnedKeys.next();
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
            if (remote.buttons.isEmpty()) continue;
            learned.remotes.add(remote);
        }
        refreshCounts();
    }

    /** Shown count is always the published shard count plus anything learned. */
    private void refreshCounts() {
        for (Category c : categories) {
            int learned = 0;
            for (Brand b : c.brands) {
                if (b.name.equals("Learned")) learned += b.remotes.size();
            }
            c.remoteCount = c.baseCount + learned;
        }
    }

    private void removeCustomBrands() {
        for (Category c : categories) {
            for (int i = c.brands.size() - 1; i >= 0; i--) {
                if (c.brands.get(i).name.equals("Learned")) c.brands.remove(i);
            }
        }
        refreshCounts();
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
        Runnable showLearned = () -> {
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
        };
        if (ci >= 0 && !categories.get(ci).name.equals(category.name)) {
            useCategory(ci, showLearned);
        } else {
            showLearned.run();
        }
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
                : current.category + " • " + prettyBrand(current.brand) + " — "
                + pretty(current.name);
        String sending = source == EMITTER_JACK ? "the jack blaster"
                : source == EMITTER_BUILT ? "the built-in emitter"
                : "nothing (no emitter" + (override ? ", override on)" : ")");
        String message = "IR emitter: " + (ready ? "READY" : "NOT AVAILABLE") + "\n"
                + "Sending by: " + sending + "\n"
                + "Phone: " + Build.MANUFACTURER + " " + Build.MODEL + "\n"
                + "Android: " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")\n\n"
                + "Database: " + categories.size() + " categories, " + remotes
                + " remotes, " + dbCodes + " codes\n"
                + "Untested-source remotes: " + dbUnverified
                + " (labelled while scanning)\n"
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

    /** Current PIN state, with the two things that can be done to it. */
    private void showUnlockPinDialog() {
        boolean custom = Vault.hasCustomPin(this);
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Unlock PIN")
                .setMessage(custom
                        ? "A custom PIN is set. Only a salted PBKDF2 hash of it is stored, so the "
                        + "app cannot show it back to you.\n\nOn the calculator, type the PIN and "
                        + "press = to open the vault."
                        : "The default PIN is " + Vault.DEFAULT_PIN + ". On the calculator, type "
                        + "it and press = to open the vault.\n\nA forgotten custom PIN can only be "
                        + "cleared by wiping the app or reinstalling it, which restores the "
                        + "default.")
                .setPositiveButton("Change", (d, w) -> showPinEntryDialog())
                .setNegativeButton("Cancel", null);
        if (custom) {
            builder.setNeutralButton("Use default", (d, w) -> {
                Vault.setPin(this, null);
                status.setText("Unlock PIN reset to " + Vault.DEFAULT_PIN + ".");
            });
        }
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE,
                android.view.WindowManager.LayoutParams.FLAG_SECURE);
    }

    /** Two masked fields; nothing is stored until both match and pass validation. */
    private void showPinEntryDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(6), dp(20), 0);
        EditText first = pinField("New PIN");
        EditText again = pinField("Repeat PIN");
        box.addView(first);
        box.addView(again);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("New unlock PIN")
                .setMessage(Vault.MIN_LEN + " to " + Vault.MAX_LEN
                        + " digits, and it cannot start with 0 — the calculator drops a leading "
                        + "zero, so such a PIN could never be typed.")
                .setView(box)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String pin = first.getText().toString().trim();
                    String problem = Vault.problemWith(pin);
                    if (problem != null) {
                        first.setError(problem);
                        return;
                    }
                    if (!pin.equals(again.getText().toString().trim())) {
                        again.setError("Does not match");
                        return;
                    }
                    Vault.setPin(this, pin);
                    status.setText("Unlock PIN changed. Type it, then = on the calculator.");
                    dialog.dismiss();
                }));
        dialog.show();
        dialog.getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE,
                android.view.WindowManager.LayoutParams.FLAG_SECURE);
    }

    private EditText pinField(String hint) {
        EditText field = new EditText(this);
        field.setHint(hint);
        field.setInputType(InputType.TYPE_CLASS_NUMBER
                | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        field.setSingleLine(true);
        field.setTextSize(18);
        field.setTextColor(TEXT);
        field.setHintTextColor(MUTED);
        return field;
    }

    /**
     * The panic switch. One tap from More, one confirmation, then the app erases itself back to
     * a clean calculator; the uninstall path hands the rest to the system prompt, because an app
     * cannot uninstall itself.
     */
    private void showPanicWipe() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Wipe everything?")
                .setMessage("This erases every saved remote, macro, setting, log and cached "
                        + "file in this app, closes the vault and returns to the calculator. "
                        + "It cannot be undone.")
                .setPositiveButton("Wipe now", (d, w) -> PanicWipe.wipe(this, false))
                .setNeutralButton("Wipe and uninstall", (d, w) -> PanicWipe.wipe(this, true))
                .setNegativeButton("Cancel", null)
                .create();
        dialog.show();
        // Dialogs are their own window, so FLAG_SECURE has to be repeated here.
        dialog.getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE,
                android.view.WindowManager.LayoutParams.FLAG_SECURE);
        int red = 0xFFE8453C;
        if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(red);
        }
        if (dialog.getButton(AlertDialog.BUTTON_NEUTRAL) != null) {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(red);
        }
    }

    private void showAbout() {
        new AlertDialog.Builder(this)
                .setTitle("About")
                .setMessage("IR Voider " + Updater.currentVersion()
                        + " \u2014 IR only. No account, no ads, no tracking.\n\n"
                        + "Codes from Flipper-IRDB (CC0). Protocol encoding based on "
                        + "Flipper Zero firmware and IRremoteESP8266.\n\n"
                        + "The phone's IR LED power cannot be amplified in software. "
                        + "One tap always sends a single frame; the repeat count only "
                        + "applies when Boost or Repeat aircon POWER is switched on.\n\n"
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
        // Never keep firing IR from the background: an infinite macro or Test
        // Storm would otherwise run until the battery dies.
        macroCancelled.set(true);
        stormCancelled.set(true);
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
        macroCancelled.set(true);
        stormCancelled.set(true);
        repeatKey = null;
        cancelShutdownSound();
        handler.removeCallbacksAndMessages(null);
        if (stormDialog != null && stormDialog.isShowing()) stormDialog.dismiss();
        dismissScanner();
        irExecutor.shutdownNow();
        super.onDestroy();
    }

    private String pretty(String name) {
        return name.replace('_', ' ');
    }

    /** Brand names arrive as directory slugs — `General_Electric`, `Sony 2`. */
    static String prettyBrand(String name) {
        if ("Learned".equals(name)) return name;
        return name.replace('_', ' ').replaceAll(" \\d+$", "").trim();
    }

    /**
     * Where a code came from. The converted collection is publicly shared but
     * never bench-tested, so the UI says so instead of implying it works.
     */
    private String codeOrigin(Remote r) {
        if (r == null) return "Ready.";
        if ("Learned".equals(r.brand)) return "Ready. From your own capture.";
        return r.unverified
                ? "Ready. Community capture \u2014 untested by this app."
                : "Ready.";
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
