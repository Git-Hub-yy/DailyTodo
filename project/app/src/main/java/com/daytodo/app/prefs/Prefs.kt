package com.daytodo.app.prefs

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

class Prefs(context: Context) {

    private val sp = context.applicationContext
        .getSharedPreferences("daytodo_prefs", Context.MODE_PRIVATE)

    var themeMode: Int
        get() = sp.getInt(KEY_THEME, THEME_SYSTEM)
        set(value) = sp.edit().putInt(KEY_THEME, value).apply()

    val nightMode: Int
        get() = when (themeMode) {
            THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }

    var widgetDim: Int
        get() = sp.getInt(KEY_DIM, 45)
        set(value) = sp.edit().putInt(KEY_DIM, value).apply()

    var widgetTextColor: Int
        get() = sp.getInt(KEY_TEXT_COLOR, TEXT_LIGHT)
        set(value) = sp.edit().putInt(KEY_TEXT_COLOR, value).apply()

    var widgetBgPath: String?
        get() = sp.getString(KEY_BG, null)
        set(value) = sp.edit().putString(KEY_BG, value).apply()

    var widgetShowDone: Boolean
        get() = sp.getBoolean(KEY_SHOW_DONE, false)
        set(value) = sp.edit().putBoolean(KEY_SHOW_DONE, value).apply()

    companion object {
        const val THEME_SYSTEM = 0
        const val THEME_LIGHT = 1
        const val THEME_DARK = 2

        const val TEXT_AUTO = 0
        const val TEXT_LIGHT = 1
        const val TEXT_DARK = 2

        private const val KEY_THEME = "theme_mode"
        private const val KEY_DIM = "widget_dim"
        private const val KEY_TEXT_COLOR = "widget_text_color"
        private const val KEY_BG = "widget_bg_path"
        private const val KEY_SHOW_DONE = "widget_show_done"
    }
}
