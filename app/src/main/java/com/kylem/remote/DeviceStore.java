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

    /** One press inside a macro, plus the pause that follows it. */
    public static class MacroStep {
        public String key;
        public int delayMs;

        public MacroStep(String key, int delayMs) {
            this.key = key;
            this.delayMs = delayMs;
        }
    }

    /**
     * A named sequence. {@code loop} is 0 for once, -1 for forever, or the
     * number of passes. 1.6.5 wrote macros without the field, so 0 is the
     * only safe default when reading old data.
     */
    public static class Macro {
        public String id;
        public String name;
        public int loop;
        public final List<MacroStep> steps = new ArrayList<>();

        public Macro(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    /** Per-device timing that the Signal Lab proved on the real unit. */
    public static class Calibration {
        public int freq;
        public int repeats;
        public int gapMs;
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
        // One tap must look like one press on the device. Anything above 1 is opt-in.
        return Math.max(1, Math.min(3, prefs(c).getInt("repeats", 1)));
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

    public static boolean irOverride(Context c) {
        return prefs(c).getBoolean("irOverride", false);
    }

    public static void setIrOverride(Context c, boolean value) {
        prefs(c).edit().putBoolean("irOverride", value).apply();
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
        return prefs(c).getInt("scanDelay", 200);
    }

    public static String scanFrom(Context c) {
        return prefs(c).getString("scanFrom", "A");
    }

    public static void setScanFrom(Context c, String value) {
        prefs(c).edit().putString("scanFrom", value).apply();
    }

    public static String scanTo(Context c) {
        return prefs(c).getString("scanTo", "Z");
    }

    public static void setScanTo(Context c, String value) {
        prefs(c).edit().putString("scanTo", value).apply();
    }

    public static void setScanDelayMs(Context c, int value) {
        prefs(c).edit().putInt("scanDelay", value).apply();
    }

    public static int themeIndex(Context c) {
        return prefs(c).getInt("theme", Palette.DEFAULT);
    }

    public static void setThemeIndex(Context c, int value) {
        prefs(c).edit().putInt("theme", value).apply();
    }

    // --------------------------------------------------------------- macros

    /** The identity 1.6.5 writes to prefs, kept byte-identical for upgrades. */
    public static String macroDeviceKey(String category, String brand,
                                        String remote, int hash) {
        return category + "\u0001" + brand + "\u0001" + remote + "\u0001" + hash;
    }

    public static List<Macro> macros(Context c, String deviceKey) {
        List<Macro> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(prefs(c).getString("macros", "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                if (!deviceKey.equals(o.optString("device"))) continue;
                Macro m = new Macro(o.optString("id", UUID.randomUUID().toString()),
                        o.optString("name", "Macro"));
                m.loop = o.optInt("loop", 0);
                JSONArray steps = o.optJSONArray("steps");
                if (steps != null) {
                    for (int s = 0; s < steps.length(); s++) {
                        JSONObject so = steps.getJSONObject(s);
                        m.steps.add(new MacroStep(so.optString("key"),
                                Math.max(100, so.optInt("delay", 600))));
                    }
                }
                out.add(m);
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    public static void saveMacros(Context c, String deviceKey, List<Macro> list) {
        try {
            JSONArray old = new JSONArray(prefs(c).getString("macros", "[]"));
            JSONArray next = new JSONArray();
            for (int i = 0; i < old.length(); i++) {
                JSONObject o = old.getJSONObject(i);
                if (!deviceKey.equals(o.optString("device"))) next.put(o);
            }
            for (Macro m : list) {
                JSONObject o = new JSONObject();
                o.put("device", deviceKey);
                o.put("id", m.id);
                o.put("name", m.name);
                o.put("loop", m.loop);
                JSONArray steps = new JSONArray();
                for (MacroStep step : m.steps) {
                    JSONObject so = new JSONObject();
                    so.put("key", step.key);
                    so.put("delay", step.delayMs);
                    steps.put(so);
                }
                o.put("steps", steps);
                next.put(o);
            }
            prefs(c).edit().putString("macros", next.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    // ---------------------------------------------------------- calibration

    private static JSONObject jsonMap(Context c, String name) {
        try {
            return new JSONObject(prefs(c).getString(name, "{}"));
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    public static Calibration calibration(Context c, String deviceKey) {
        JSONObject o = jsonMap(c, "calibrations").optJSONObject(deviceKey);
        if (o == null) return null;
        Calibration cal = new Calibration();
        cal.freq = o.optInt("freq", 0);
        cal.repeats = Math.max(1, Math.min(3, o.optInt("repeats", 1)));
        cal.gapMs = Math.max(20, Math.min(2000, o.optInt("gap", 50)));
        return cal;
    }

    public static void saveCalibration(Context c, String deviceKey, Calibration cal) {
        try {
            JSONObject all = jsonMap(c, "calibrations");
            JSONObject o = new JSONObject();
            o.put("freq", cal.freq);
            o.put("repeats", cal.repeats);
            o.put("gap", cal.gapMs);
            all.put(deviceKey, o);
            prefs(c).edit().putString("calibrations", all.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    public static void clearCalibration(Context c, String deviceKey) {
        try {
            JSONObject all = jsonMap(c, "calibrations");
            all.remove(deviceKey);
            prefs(c).edit().putString("calibrations", all.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    // -------------------------------------------------------- aircon target

    /** Last absolute target: {celsius, mode 0..4, fan 0..3}. */
    public static int[] acState(Context c, String deviceKey,
                                int defaultTemp, int defaultMode, int defaultFan) {
        int temp = Math.max(17, Math.min(30, defaultTemp));
        int mode = Math.max(0, Math.min(4, defaultMode));
        int fan = Math.max(0, Math.min(3, defaultFan));
        JSONObject o = jsonMap(c, "acStates").optJSONObject(deviceKey);
        if (o != null) {
            temp = Math.max(17, Math.min(30, o.optInt("temp", temp)));
            mode = Math.max(0, Math.min(4, o.optInt("mode", mode)));
            fan = Math.max(0, Math.min(3, o.optInt("fan", fan)));
        }
        return new int[]{temp, mode, fan};
    }

    public static void saveAcState(Context c, String deviceKey,
                                   int temp, int mode, int fan) {
        try {
            JSONObject all = jsonMap(c, "acStates");
            JSONObject o = new JSONObject();
            o.put("temp", Math.max(17, Math.min(30, temp)));
            o.put("mode", Math.max(0, Math.min(4, mode)));
            o.put("fan", Math.max(0, Math.min(3, fan)));
            all.put(deviceKey, o);
            prefs(c).edit().putString("acStates", all.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    // -------------------------------------------------------- extra buttons

    /** Buttons added from sibling remotes; stored as [freq, [pattern]]. */
    public static JSONObject patches(Context c, String deviceKey) {
        JSONObject all = jsonMap(c, "patches");
        JSONObject o = all.optJSONObject(deviceKey);
        return o == null ? new JSONObject() : o;
    }

    public static void savePatches(Context c, String deviceKey, JSONObject buttons) {
        try {
            JSONObject all = jsonMap(c, "patches");
            if (buttons == null || buttons.length() == 0) all.remove(deviceKey);
            else all.put(deviceKey, buttons);
            prefs(c).edit().putString("patches", all.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    public static void putPatch(Context c, String deviceKey, String button,
                                int freq, int[] pattern) {
        try {
            JSONObject buttons = patches(c, deviceKey);
            JSONArray arr = new JSONArray();
            arr.put(freq);
            JSONArray pat = new JSONArray();
            for (int value : pattern) pat.put(value);
            arr.put(pat);
            buttons.put(button, arr);
            savePatches(c, deviceKey, buttons);
        } catch (Exception ignored) {
        }
    }

    public static void removePatch(Context c, String deviceKey, String button) {
        try {
            JSONObject buttons = patches(c, deviceKey);
            buttons.remove(button);
            savePatches(c, deviceKey, buttons);
        } catch (Exception ignored) {
        }
    }

    public static void clearPatches(Context c, String deviceKey) {
        savePatches(c, deviceKey, null);
    }

    /** Extra frames for a Power Doctor winner. Stored beside the button patch. */
    public static int powerOffRepeats(Context c, String deviceKey) {
        return Math.max(1, Math.min(3,
                patches(c, deviceKey).optInt("__powerOffRepeats", 1)));
    }

    public static void setPowerOffRepeats(Context c, String deviceKey, int value) {
        try {
            JSONObject buttons = patches(c, deviceKey);
            buttons.put("__powerOffRepeats", Math.max(1, Math.min(3, value)));
            savePatches(c, deviceKey, buttons);
        } catch (Exception ignored) {
        }
    }

    // ------------------------------------------------------------ shortcuts

    public static String webUrl(Context c) {
        return prefs(c).getString("webUrl", "https://www.youtube.com");
    }

    public static void setWebUrl(Context c, String url) {
        prefs(c).edit().putString("webUrl", url).apply();
    }

    // ---------------------------------------------------------- test storm

    public static int stormDelayMs(Context c) {
        return Math.max(120, Math.min(2000, prefs(c).getInt("stormDelay", 350)));
    }

    public static void setStormDelayMs(Context c, int value) {
        prefs(c).edit().putInt("stormDelay", Math.max(120, Math.min(2000, value))).apply();
    }

    public static boolean stormIncludePower(Context c) {
        return prefs(c).getBoolean("stormPower", false);
    }

    public static void setStormIncludePower(Context c, boolean value) {
        prefs(c).edit().putBoolean("stormPower", value).apply();
    }
}
