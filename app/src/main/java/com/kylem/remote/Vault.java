/*
 * IR Voider — Android IR remote.
 * Copyright (C) 2026 poging kyle
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.kylem.remote;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.security.SecureRandom;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * The lock: an in-memory unlock flag, and the PIN that sets it.
 *
 * <p>The flag never touches disk, so a process restart, a reboot or a task kill always starts
 * locked. The PIN is only stored as a salted PBKDF2 hash — a plaintext code in
 * {@code shared_prefs} would be readable by any backup tool, and the app cannot show it back to
 * the user either, which is the point.
 *
 * <p>Wiping the app (Panic wipe, or uninstalling and reinstalling) deletes the hash, so the
 * default {@value #DEFAULT_PIN} works again. That is the only way back in if the PIN is
 * forgotten.
 */
final class Vault {

    static final String DEFAULT_PIN = "54321";
    static final int MIN_LEN = 4;
    static final int MAX_LEN = 10;

    private static final String PREFS = "vault";
    private static final String KEY_SALT = "pin_salt";
    private static final String KEY_HASH = "pin_hash";
    private static final int ITERATIONS = 12000;
    private static final int KEY_BITS = 128;

    private static volatile boolean unlocked;

    private Vault() {
    }

    static boolean isUnlocked() {
        return unlocked;
    }

    static void unlock() {
        unlocked = true;
    }

    static void lock() {
        unlocked = false;
    }

    // ------------------------------------------------------------------- PIN

    static boolean hasCustomPin(Context c) {
        return prefs(c).contains(KEY_HASH);
    }

    /** Does the typed sequence open the vault? Falls back to the default when none is set. */
    static boolean matches(Context c, String typed) {
        if (typed == null) return false;
        // Cheap shape check first: this runs on every "=", and only a plain run of digits of a
        // plausible length could ever be a PIN, so ordinary arithmetic never pays for the hash.
        if (typed.length() < MIN_LEN || typed.length() > MAX_LEN) return false;
        for (int i = 0; i < typed.length(); i++) {
            char ch = typed.charAt(i);
            if (ch < '0' || ch > '9') return false;
        }
        SharedPreferences p = prefs(c);
        String stored = p.getString(KEY_HASH, null);
        if (stored == null) return DEFAULT_PIN.equals(typed);
        String salt = p.getString(KEY_SALT, "");
        if (salt == null) return false;
        try {
            byte[] raw = Base64.decode(salt, Base64.NO_WRAP);
            return stored.equals(hash(typed, raw));
        } catch (Exception e) {
            return false;
        }
    }

    /** Passing {@code null} removes the custom PIN and restores the default. */
    static void setPin(Context c, String pin) {
        SharedPreferences p = prefs(c);
        if (pin == null) {
            p.edit().remove(KEY_SALT).remove(KEY_HASH).apply();
            return;
        }
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        p.edit()
                .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
                .putString(KEY_HASH, hash(pin, salt))
                .apply();
    }

    /** Why this PIN cannot be used, or {@code null} if it can. */
    static String problemWith(String pin) {
        if (pin == null || pin.isEmpty()) return "Enter a PIN";
        for (int i = 0; i < pin.length(); i++) {
            char c = pin.charAt(i);
            if (c < '0' || c > '9') return "Digits only";
        }
        if (pin.length() < MIN_LEN) return "At least " + MIN_LEN + " digits";
        if (pin.length() > MAX_LEN) return "At most " + MAX_LEN + " digits";
        // The calculator drops a leading zero, so such a PIN could never be typed.
        if (pin.charAt(0) == '0') return "Cannot start with 0";
        return null;
    }

    private static String hash(String pin, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_BITS);
            byte[] out = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
                    .generateSecret(spec).getEncoded();
            spec.clearPassword();
            return Base64.encodeToString(out, Base64.NO_WRAP);
        } catch (Exception e) {
            return "";
        }
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
