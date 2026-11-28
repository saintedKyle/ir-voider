/*
 * IR Voider — Android IR remote.
 * Copyright (C) 2026 poging kyle
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.kylem.remote;

/**
 * Process-wide lock state for the calculator front.
 *
 * <p>The flag lives in memory only, never on disk: a process restart, a reboot or a task kill
 * all start the app locked, and nothing about the vault is written anywhere for a forensic
 * read of {@code shared_prefs} to find.
 */
final class Vault {

    /** The unlock sequence: these digits, then {@code =}. */
    static final String PIN = "54321";

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
}
