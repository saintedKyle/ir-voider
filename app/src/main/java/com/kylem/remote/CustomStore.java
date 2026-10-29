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

public final class CustomStore {

    private static final String KEY = "customRemotes";

    private CustomStore() {
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(DeviceStore.PREFS, Context.MODE_PRIVATE);
    }

    public static JSONArray list(Context c) {
        try {
            return new JSONArray(prefs(c).getString(KEY, "[]"));
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    public static void add(Context c, String category, String name, JSONObject buttons) {
        JSONArray current = list(c);
        JSONArray next = new JSONArray();
        for (int i = 0; i < current.length(); i++) {
            JSONObject o = current.optJSONObject(i);
            if (o == null) continue;
            if (category.equals(o.optString("category"))
                    && name.equals(o.optString("name"))) {
                continue;
            }
            next.put(o);
        }
        try {
            JSONObject remote = new JSONObject();
            remote.put("category", category);
            remote.put("name", name);
            remote.put("buttons", buttons);
            next.put(remote);
        } catch (Exception ignored) {
        }
        prefs(c).edit().putString(KEY, next.toString()).apply();
    }

    public static void clear(Context c) {
        prefs(c).edit().remove(KEY).apply();
    }
}
