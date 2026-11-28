/*
 * IR Voider — Android IR remote.
 * Copyright (C) 2026 poging kyle
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.kylem.remote;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Process;

import java.io.File;

/**
 * Erases everything this app has written, then puts it back on the calculator screen.
 *
 * <p>What is deleted: every child of the app's private data directory (shared preferences, the
 * SQLite databases, files, caches, WebView state, {@code no_backup}) and the app's external
 * private directories. Whatever the vault held is gone before the screen redraws.
 *
 * <p>The process is killed afterwards on purpose. Android keeps a parsed copy of
 * {@code shared_prefs} in memory, so a deleted file can be written back by the still-running
 * process; ending the process is the only way to guarantee nothing is flushed back to disk, and
 * it also drops the unlocked session without touching the on-disk state we just erased.
 *
 * <p>An app cannot uninstall itself. The optional {@code uninstall} flag asks the system to show
 * its own uninstall prompt, which is the closest the platform allows.
 */
final class PanicWipe {

    /** Never touched: deleting this breaks the install until the app is reinstalled. */
    private static final String[] KEEP = {"lib"};

    private PanicWipe() {
    }

    static void wipe(Activity from, boolean uninstall) {
        Context app = from.getApplicationContext();

        // Lock first: if anything goes wrong below, the vault is still shut.
        Vault.lock();

        File data = new File(app.getApplicationInfo().dataDir);
        File[] children = data.listFiles();
        if (children != null) {
            for (File child : children) {
                if (isKept(child.getName())) continue;
                deleteTree(child);
            }
        }
        deleteTree(app.getCacheDir());
        deleteTree(app.getExternalFilesDir(null));
        deleteTree(app.getExternalCacheDir());

        Intent lock = new Intent(app, LockActivity.class);
        lock.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        app.startActivity(lock);

        if (uninstall) {
            try {
                Intent remove = new Intent(Intent.ACTION_DELETE,
                        Uri.parse("package:" + app.getPackageName()));
                remove.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                app.startActivity(remove);
            } catch (Exception ignored) {
                // Some ROMs refuse the uninstall intent; the wipe has already happened.
            }
        }

        from.finishAffinity();

        // Take the process down now, not later. Both startActivity calls above are synchronous
        // IPC to the system, so they are already queued; killing here means the relaunch is
        // serviced by a brand-new process that boots straight into the calculator, and nothing
        // cached in memory — parsed preferences above all — can be flushed back to the disk we
        // just wiped.
        Process.killProcess(Process.myPid());
    }

    private static boolean isKept(String name) {
        for (String keep : KEEP) {
            if (keep.equals(name)) return true;
        }
        return false;
    }

    private static void deleteTree(File file) {
        if (file == null || !file.exists()) return;
        File[] kids = file.listFiles();
        if (kids != null) {
            for (File kid : kids) deleteTree(kid);
        }
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }
}
