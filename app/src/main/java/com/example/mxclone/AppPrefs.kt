package com.example.mxclone

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

class AppPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("mx_clone_prefs", Context.MODE_PRIVATE)

    // 0 = Follow System, 1 = Light, 2 = Dark
    var themeMode: Int
        get() = prefs.getInt("theme_mode", 0)
        set(value) = prefs.edit().putInt("theme_mode", value).apply()

    var rememberBrightness: Boolean
        get() = prefs.getBoolean("remember_brightness", true)
        set(value) = prefs.edit().putBoolean("remember_brightness", value).apply()

    var savedBrightness: Float
        get() = prefs.getFloat("saved_brightness", -1f)
        set(value) = prefs.edit().putFloat("saved_brightness", value).apply()

    var rememberAspectRatio: Boolean
        get() = prefs.getBoolean("remember_aspect", true)
        set(value) = prefs.edit().putBoolean("remember_aspect", value).apply()

    var savedAspectRatio: Int
        get() = prefs.getInt("saved_aspect", 0)
        set(value) = prefs.edit().putInt("saved_aspect", value).apply()

    var seekSeconds: Int
        get() = prefs.getInt("seek_seconds", 10)
        set(value) = prefs.edit().putInt("seek_seconds", value).apply()

    var autoPip: Boolean
        get() = prefs.getBoolean("auto_pip", false)
        set(value) = prefs.edit().putBoolean("auto_pip", value).apply()

    var gestureControl: Boolean
        get() = prefs.getBoolean("gesture_control", true)
        set(value) = prefs.edit().putBoolean("gesture_control", value).apply()

    var showFloatingPlay: Boolean
        get() = prefs.getBoolean("show_floating_play", true)
        set(value) = prefs.edit().putBoolean("show_floating_play", value).apply()

    var lastVideoUri: String?
        get() = prefs.getString("last_video_uri", null)
        set(value) = prefs.edit().putString("last_video_uri", value).apply()

    var lastVideoTitle: String?
        get() = prefs.getString("last_video_title", null)
        set(value) = prefs.edit().putString("last_video_title", value).apply()

    fun applyTheme() {
        when (themeMode) {
            1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
}
