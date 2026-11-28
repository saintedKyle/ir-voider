/*
 * IR Voider — Android IR remote.
 * Copyright (C) 2026 poging kyle
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.kylem.remote;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Keeps the vault shut.
 *
 * <p>Every time the last activity stops — Home, Recents, another app taking over, the screen
 * going off — the session dies: the unlock flag is cleared and the whole task is finished and
 * removed from Recents in one call, so nothing is left for the task switcher to preview and the
 * next launch has to come back through the calculator.
 *
 * <p>The stop is checked after a short delay instead of immediately, because Android starts the
 * next activity before stopping the previous one and a same-task hand-off can momentarily read as
 * "nothing running". The delay is invisible to a person and removes that race.
 *
 * <p>Nothing is launched from here. Starting an activity from the background is blocked on
 * Android 10+, so the app tears itself down and lets the launcher (which is the calculator) be
 * the entry point, rather than trying to force the lock screen forward.
 */
public class VaultApp extends Application implements Application.ActivityLifecycleCallbacks {

    private static final long STOP_GRACE_MS = 250;

    private final List<WeakReference<Activity>> alive = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int started;

    private final Runnable lockCheck = new Runnable() {
        @Override
        public void run() {
            if (started == 0) lockDown();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);

        BroadcastReceiver screenOff = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Screen off means the phone is about to be in a pocket, not a moment from
                // returning: tear down now rather than after the grace period.
                lockDown();
            }
        };
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(screenOff, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(screenOff, filter);
        }
    }

    /** Clear the session and remove every screen, including the task-switcher card. */
    static void lockDown() {
        Vault.lock();
        VaultApp app = instance;
        if (app != null) app.tearDown();
    }

    private static volatile VaultApp instance;

    private void tearDown() {
        handler.removeCallbacks(lockCheck);
        Activity victim = null;
        for (WeakReference<Activity> ref : alive) {
            Activity a = ref.get();
            if (a != null && !a.isFinishing()) {
                victim = a;
                break;
            }
        }
        alive.clear();
        started = 0;
        if (victim != null) {
            // One call finishes the whole task and drops it from Recents.
            victim.finishAndRemoveTask();
        }
    }

    // --------------------------------------------------------- lifecycle hooks

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        instance = this;
    }

    @Override
    public void onActivityStarted(Activity activity) {
        started++;
        handler.removeCallbacks(lockCheck);
        alive.add(new WeakReference<>(activity));
    }

    @Override
    public void onActivityResumed(Activity activity) {
    }

    @Override
    public void onActivityPaused(Activity activity) {
    }

    @Override
    public void onActivityStopped(Activity activity) {
        started = Math.max(0, started - 1);
        if (started == 0) handler.postDelayed(lockCheck, STOP_GRACE_MS);
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        for (int i = alive.size() - 1; i >= 0; i--) {
            Activity a = alive.get(i).get();
            if (a == null || a == activity) alive.remove(i);
        }
    }
}
