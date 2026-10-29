/*
 * IR Voider — Android IR remote.
 * Copyright (C) 2026 poging kyle
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.kylem.remote;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class DeviceStore {

    public static final String PREFS = "remote";

    public static class Device {
        public String id;
        public String name;
        public String type;
        public String category;
        public String brand;
        public String remote;
        public int hash;
    }

    private DeviceStore() {
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static List<Device> devices(Context c) {
        List<Device> out = new ArrayList<>();
        String raw = prefs(c).getString("devices", "[]");
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Device d = new Device();
                d.id = o.optString("id", UUID.randomUUID().toString());
                d.name = o.optString("name", "Device");
                d.type = o.optString("type", "TV");
                d.category = o.optString("category", "TV");
                d.brand = o.optString("brand", "");
                d.remote = o.optString("remote", "");
                d.hash = o.optInt("hash", 0);
                out.add(d);
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    public static void saveDevices(Context c, List<Device> list) {
        JSONArray arr = new JSONArray();
        for (Device d : list) {
            try {
                JSONObject o = new JSONObject();
                o.put("id", d.id);
                o.put("name", d.name);
                o.put("type", d.type);
                o.put("category", d.category);
                o.put("brand", d.brand);
                o.put("remote", d.remote);
                o.put("hash", d.hash);
                arr.put(o);
            } catch (Exception ignored) {
            }
        }
        prefs(c).edit().putString("devices", arr.toString()).apply();
    }

    public static void add(Context c, Device d) {
        List<Device> list = devices(c);
        list.add(d);
        saveDevices(c, list);
    }

    public static void remove(Context c, String id) {
        List<Device> list = devices(c);
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i).id.equals(id)) list.remove(i);
        }
        saveDevices(c, list);
    }

    public static Device findByRemote(Context c, String category, String brand,
                                      String remote, int hash) {
        for (Device d : devices(c)) {
            if (d.category.equals(category) && d.brand.equals(brand)
                    && d.remote.equals(remote) && d.hash == hash) return d;
        }
        return null;
    }

    /** Remember the selected remote; used by both manual picks and device configs. */
    public static void rememberSelection(Context c, String category, String brand,
                                         String remote, int hash) {
        prefs(c).edit()
                .putInt("version", 2)
                .putString("category", category)
                .putString("brand", brand)
                .putString("remote", remote)
                .putInt("powerHash", hash)
                .apply();
    }

    public static String selectedCategory(Context c) {
        return prefs(c).getString("category", "TV");
    }

    public static String selectedBrand(Context c) {
        return prefs(c).getString("brand", null);
    }

    public static String selectedRemote(Context c) {
        return prefs(c).getString("remote", null);
    }

    public static int selectedHash(Context c) {
        return prefs(c).getInt("powerHash", 0);
    }

    public static void clearSelection(Context c) {
        prefs(c).edit().remove("category").remove("brand")
                .remove("remote").remove("powerHash").apply();
    }

    public static int repeats(Context c) {
        return Math.max(1, Math.min(3, prefs(c).getInt("repeats", 2)));
    }

    public static void setRepeats(Context c, int value) {
        prefs(c).edit().putInt("repeats", Math.max(1, Math.min(3, value))).apply();
    }

    public static boolean boostAll(Context c) {
        return prefs(c).getBoolean("boostAll", false);
    }

    public static void setBoostAll(Context c, boolean value) {
        prefs(c).edit().putBoolean("boostAll", value).apply();
    }

    public static boolean forceMode(Context c) {
        return prefs(c).getBoolean("forceMode", false);
    }

    public static void setForceMode(Context c, boolean value) {
        prefs(c).edit().putBoolean("forceMode", value).apply();
    }

    public static boolean acPowerRepeat(Context c) {
        return prefs(c).getBoolean("acPowerRepeat", false);
    }

    public static void setAcPowerRepeat(Context c, boolean value) {
        prefs(c).edit().putBoolean("acPowerRepeat", value).apply();
    }

    public static int scanDelayMs(Context c) {
        return prefs(c).getInt("scanDelay", 2000);
    }

    public static void setScanDelayMs(Context c, int value) {
        prefs(c).edit().putInt("scanDelay", value).apply();
    }
}
