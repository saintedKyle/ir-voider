/*
 * IR Voider — Android IR remote.
 * Copyright (C) 2026 poging kyle
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.kylem.remote;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Optional release check against the public GitHub releases feed. Nothing else in the
 * app touches the network, and every remote feature works with the radio off.
 */
public final class Updater {

    public static final String PREFS = "update";

    public static final class Release {
        public final String version;
        public final String apkUrl;
        public final String pageUrl;
        public final String notes;

        Release(String version, String apkUrl, String pageUrl, String notes) {
            this.version = version;
            this.apkUrl = apkUrl;
            this.pageUrl = pageUrl;
            this.notes = notes;
        }
    }

    public interface Listener {
        /** Called on the main thread. {@code error} is null when the check completed. */
        void onCheck(Release newer, String error);
    }

    private Updater() {
    }

    public static String currentVersion() {
        return BuildConfig.VERSION_NAME;
    }

    public static boolean enabled(Context c) {
        return prefs(c).getBoolean("enabled", true);
    }

    public static void setEnabled(Context c, boolean value) {
        prefs(c).edit().putBoolean("enabled", value).apply();
    }

    /** Version the user chose to stop being asked about, or null. */
    public static String skippedVersion(Context c) {
        return prefs(c).getString("skipped", null);
    }

    public static void skipVersion(Context c, String version) {
        prefs(c).edit().putString("skipped", version).apply();
    }

    public static void clearSkipped(Context c) {
        prefs(c).edit().remove("skipped").apply();
    }

    /** Latest version seen by a previous check, used to badge the menu without a new request. */
    public static String knownLatest(Context c) {
        return prefs(c).getString("latest", null);
    }

    public static boolean hasConnection(Context c) {
        ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        Network network = cm.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    /**
     * Runs one check off the main thread. Silent when the user turned updates off or is
     * offline; both are the common case and should not produce a toast.
     */
    public static void check(final Context c, final boolean force, final Listener listener) {
        final Handler main = new Handler(Looper.getMainLooper());
        if (!force && !enabled(c)) return;
        if (!force && !hasConnection(c)) return;
        new Thread(() -> {
            Release release;
            String error = null;
            try {
                release = fetch();
            } catch (Exception e) {
                release = null;
                error = e.getMessage() == null ? e.toString() : e.getMessage();
            }
            final Release found = release;
            final String failure = error;
            main.post(() -> {
                if (found != null) {
                    prefs(c).edit().putLong("last_check", System.currentTimeMillis())
                            .putString("latest", found.version).apply();
                }
                listener.onCheck(found, failure);
            });
        }, "ir-voider-update").start();
    }

    /** @return the release only when it is newer than this build and not skipped. */
    private static Release fetch() throws Exception {
        HttpURLConnection conn = (HttpURLConnection)
                new URL("https://api.github.com/repos/" + BuildConfig.UPDATE_REPO
                        + "/releases/latest").openConnection();
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setInstanceFollowRedirects(true);
        conn.setRequestProperty("User-Agent", "IR-Voider-Android");
        conn.setRequestProperty("Accept", "application/vnd.github+json");
        try {
            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new IllegalStateException("server replied " + code);
            }
            JSONObject json = new JSONObject(readAll(conn.getInputStream()));
            String tag = json.optString("tag_name", "");
            String version = tag.startsWith("v") ? tag.substring(1) : tag;
            String apk = null;
            JSONArray assets = json.optJSONArray("assets");
            if (assets != null) {
                for (int i = 0; i < assets.length(); i++) {
                    JSONObject a = assets.getJSONObject(i);
                    String name = a.optString("name", "");
                    if (name.endsWith(".apk")) {
                        apk = a.optString("browser_download_url", null);
                        break;
                    }
                }
            }
            return new Release(version, apk, json.optString("html_url", ""),
                    json.optString("body", ""));
        } finally {
            conn.disconnect();
        }
    }

    private static String readAll(InputStream in) throws Exception {
        try (InputStream stream = in) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = stream.read(buf)) > 0) out.write(buf, 0, n);
            return out.toString("UTF-8");
        }
    }

    /** True when {@code latest} is a higher dotted number than {@code current}. */
    public static boolean isNewer(String latest, String current) {
        String[] a = split(latest);
        String[] b = split(current);
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int x = i < a.length ? num(a[i]) : 0;
            int y = i < b.length ? num(b[i]) : 0;
            if (x != y) return x > y;
        }
        return false;
    }

    private static String[] split(String value) {
        return value == null ? new String[0] : value.trim().split("\\.");
    }

    private static int num(String part) {
        int end = 0;
        while (end < part.length() && Character.isDigit(part.charAt(end))) end++;
        if (end == 0) return 0;
        try {
            return Integer.parseInt(part.substring(0, end));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
