/*
 * IR Voider — Android IR remote.
 * Copyright (C) 2026 poging kyle
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.kylem.remote;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The launcher screen: an ordinary four-function calculator.
 *
 * <p>It has to be a calculator that genuinely works — if it were a fake keypad with a hidden
 * "unlock" button, anyone glancing at the phone would know something was off, and the user would
 * have no calculator. Every key behaves normally; {@code 54321} then {@code =} is the only input
 * that does something else, and it leaves no visible trace when it happens.
 */
public class LockActivity extends Activity {

    private static final int BG = 0xFF0B0D11;
    private static final int KEY_NUM = 0xFF22262E;
    private static final int KEY_FN = 0xFF2C323C;
    private static final int KEY_OP = 0xFFE8753D;
    private static final int TEXT = 0xFFF2F4F8;
    private static final int MUTED = 0xFF8D95A3;
    private static final int INK = 0xFF160E08;
    private static final int MAX_LEN = 18;

    private final StringBuilder input = new StringBuilder();
    private TextView display;
    private TextView history;
    /** True while the display holds a computed result that the next digit should replace. */
    private boolean fresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Nothing about this app may appear in a screenshot, a screen recording, or the
        // task-switcher thumbnail — including the calculator itself.
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);
        if (Build.VERSION.SDK_INT >= 33) {
            setRecentsScreenshotEnabled(false);
        }
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(BG));
        build();
    }

    private void build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        int pad = dp(18);
        root.setPadding(pad, dp(28), pad, dp(22));

        history = new TextView(this);
        history.setText("");
        history.setTextSize(15);
        history.setTextColor(MUTED);
        history.setGravity(Gravity.END);
        history.setSingleLine(true);
        root.addView(history, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        display = new TextView(this);
        display.setText("0");
        display.setTextColor(TEXT);
        display.setGravity(Gravity.END | Gravity.BOTTOM);
        display.setSingleLine(true);
        display.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        if (Build.VERSION.SDK_INT >= 26) {
            display.setAutoSizeTextTypeUniformWithConfiguration(20, 52, 2,
                    TypedValue.COMPLEX_UNIT_SP);
        } else {
            display.setTextSize(40);
        }
        LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        dl.topMargin = dp(6);
        root.addView(display, dl);

        String[][] rows = {
                {"AC", "\u232B", "%", "\u00F7"},
                {"7", "8", "9", "\u00D7"},
                {"4", "5", "6", "\u2212"},
                {"1", "2", "3", "+"},
                {"0", ".", "="},
        };
        for (String[] row : rows) {
            root.addView(keyRow(row));
        }

        setContentView(root);
        render();
    }

    private View keyRow(String[] keys) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(64));
        rl.topMargin = dp(10);
        row.setLayoutParams(rl);

        for (String label : keys) {
            Button b = key(label);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    "0".equals(label) ? 2f : 1f);
            lp.setMargins(dp(5), 0, dp(5), 0);
            row.addView(b, lp);
        }
        return row;
    }

    private Button key(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        b.setTextSize(24);
        b.setGravity(Gravity.CENTER);
        b.setPadding(0, 0, 0, 0);
        b.setMinHeight(0);
        b.setMinWidth(0);
        b.setStateListAnimator(null);

        boolean op = "\u00F7".equals(label) || "\u00D7".equals(label)
                || "\u2212".equals(label) || "+".equals(label) || "=".equals(label);
        boolean fn = "AC".equals(label) || "\u232B".equals(label) || "%".equals(label);
        int bg = op ? KEY_OP : fn ? KEY_FN : KEY_NUM;
        b.setTextColor(op ? INK : TEXT);
        b.setBackground(ripple(bg));
        b.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            press(label);
        });
        return b;
    }

    private RippleDrawable ripple(int color) {
        GradientDrawable body = new GradientDrawable();
        body.setShape(GradientDrawable.RECTANGLE);
        body.setCornerRadius(dp(24));
        body.setColor(color);
        body.setStroke(dp(1), blend(color, 0xFFFFFFFF, 0.10f));
        return new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), body, null);
    }

    private static int blend(int base, int over, float amount) {
        float w = 1f - amount;
        int r = (int) (Color.red(base) * w + Color.red(over) * amount);
        int g = (int) (Color.green(base) * w + Color.green(over) * amount);
        int b = (int) (Color.blue(base) * w + Color.blue(over) * amount);
        return Color.rgb(r, g, b);
    }

    private void press(String label) {
        switch (label) {
            case "AC":
                input.setLength(0);
                history.setText("");
                fresh = false;
                break;
            case "\u232B":
                if (input.length() > 0) input.setLength(input.length() - 1);
                fresh = false;
                break;
            case "=":
                equals();
                return;
            case "%":
                percent();
                break;
            default:
                if (isOperator(label)) {
                    operator(label);
                } else {
                    digit(label);
                }
                break;
        }
        render();
    }

    private static boolean isOperator(String label) {
        return "\u00F7".equals(label) || "\u00D7".equals(label)
                || "\u2212".equals(label) || "+".equals(label);
    }

    /** The display uses the typographic minus, so every scan has to accept both forms. */
    private static boolean isOpChar(char c) {
        return c == '+' || c == '-' || c == '\u2212' || c == '\u00D7' || c == '\u00F7';
    }

    private void digit(String label) {
        boolean point = ".".equals(label);
        // A digit after "=" begins a new expression instead of extending the result.
        if (fresh) {
            input.setLength(0);
            history.setText("");
            fresh = false;
        }
        // Never show a leading zero the user did not type on purpose.
        if (!point && input.length() == 1 && input.charAt(0) == '0') input.setLength(0);
        if (input.length() >= MAX_LEN) return;
        if (point) {
            // "5+" then "." must read 5+0.… ; a number never takes two decimal points.
            int lastOp = -1;
            for (int i = 0; i < input.length(); i++) {
                if (isOpChar(input.charAt(i))) lastOp = i;
            }
            if (input.indexOf(".", lastOp + 1) >= 0) return;
            if (input.length() == 0 || lastOp == input.length() - 1) input.append('0');
        }
        input.append(label);
    }

    private void operator(String label) {
        history.setText("");
        fresh = false;
        if (input.length() == 0) input.append('0');
        if (isOpChar(input.charAt(input.length() - 1))) {
            input.setLength(input.length() - 1);
        }
        if (input.length() >= MAX_LEN) return;
        input.append(label);
    }

    private void percent() {
        if (input.length() == 0) return;
        double v = evaluate(input.toString());
        if (Double.isNaN(v)) return;
        setInput(fmt(v / 100));
        fresh = true;
    }

    private void equals() {
        String expr = input.toString();
        // The one hidden path. Exact digits, nothing else, no animation, no sound.
        if (Vault.PIN.equals(expr)) {
            Vault.unlock();
            Intent main = new Intent(this, MainActivity.class);
            main.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(main);
            finish();
            overridePendingTransition(0, 0);
            return;
        }
        if (expr.isEmpty()) return;
        double v = evaluate(expr);
        if (Double.isNaN(v)) {
            history.setText("");
            input.setLength(0);
            display.setText("Error");
            return;
        }
        history.setText(expr);
        setInput(fmt(v));
        fresh = true;
        render();
    }

    private void setInput(String value) {
        input.setLength(0);
        input.append(value);
    }

    /** Plain two-pass evaluator: × ÷ first, then + −. No scripting, no reflection. */
    private static double evaluate(String raw) {
        String s = raw.replace('\u2212', '-');
        List<Double> nums = new ArrayList<>();
        List<Character> ops = new ArrayList<>();
        int i = 0;
        boolean expectNumber = true;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '+' || c == '-' || c == '\u00D7' || c == '\u00F7') {
                if (expectNumber) {
                    // Allow a leading minus only.
                    if (c == '-' && nums.isEmpty() && ops.isEmpty()) {
                        nums.add(0.0);
                        ops.add('-');
                        i++;
                        continue;
                    }
                    return Double.NaN;
                }
                ops.add(c);
                expectNumber = true;
                i++;
            } else if (c == '.' || (c >= '0' && c <= '9')) {
                int start = i;
                int dots = 0;
                while (i < s.length() && (s.charAt(i) == '.' || (s.charAt(i) >= '0' && s.charAt(i) <= '9'))) {
                    if (s.charAt(i) == '.' && ++dots > 1) return Double.NaN;
                    i++;
                }
                try {
                    nums.add(Double.parseDouble(s.substring(start, i)));
                } catch (NumberFormatException e) {
                    return Double.NaN;
                }
                expectNumber = false;
            } else {
                return Double.NaN;
            }
        }
        if (nums.isEmpty() || nums.size() != ops.size() + 1) return Double.NaN;

        // Pass one: × and ÷.
        List<Double> n2 = new ArrayList<>();
        n2.add(nums.get(0));
        for (int k = 0; k < ops.size(); k++) {
            char op = ops.get(k);
            double rhs = nums.get(k + 1);
            if (op == '\u00D7') {
                n2.set(n2.size() - 1, n2.get(n2.size() - 1) * rhs);
            } else if (op == '\u00F7') {
                if (rhs == 0) return Double.NaN;
                n2.set(n2.size() - 1, n2.get(n2.size() - 1) / rhs);
            } else {
                n2.add(rhs);
            }
        }
        // Pass two: + and −, left to right.
        double acc = n2.get(0);
        int idx = 1;
        for (int k = 0; k < ops.size(); k++) {
            char op = ops.get(k);
            if (op == '\u00D7' || op == '\u00F7') continue;
            double rhs = n2.get(idx++);
            acc = op == '+' ? acc + rhs : acc - rhs;
        }
        return acc;
    }

    private static String fmt(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return "Error";
        double r = Math.rint(v);
        if (Math.abs(v - r) < 1e-10 && Math.abs(v) < 1e15) {
            return String.valueOf((long) r);
        }
        String s = String.format(Locale.US, "%.8f", v);
        while (s.endsWith("0")) s = s.substring(0, s.length() - 1);
        if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
        return s;
    }

    private void render() {
        String text = input.length() == 0 ? "0" : input.toString();
        display.setText(text);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Leaving the calculator clears whatever was typed, so the PIN is not waiting on screen
        // when the phone is picked up again.
        input.setLength(0);
        if (history != null) history.setText("");
        if (display != null) display.setText("0");
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
