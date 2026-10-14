package com.example.data

import android.content.Context
import com.example.model.DiagramThemeMode

class UserPreferencesRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "trensurb_preferences"
        private const val KEY_DIAGRAM_THEME = "diagram_theme_mode"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
    }

    fun getThemeMode(): DiagramThemeMode {
        val raw = prefs.getString(KEY_DIAGRAM_THEME, DiagramThemeMode.DARK.name)
        return try {
            DiagramThemeMode.valueOf(raw ?: DiagramThemeMode.DARK.name)
        } catch (e: Exception) {
            DiagramThemeMode.DARK
        }
    }

    fun setThemeMode(mode: DiagramThemeMode) {
        prefs.edit().putString(KEY_DIAGRAM_THEME, mode.name).apply()
    }

    fun isSoundEnabled(): Boolean {
        return prefs.getBoolean(KEY_SOUND_ENABLED, true)
    }

    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
    }

    fun isVibrationEnabled(): Boolean {
        return prefs.getBoolean(KEY_VIBRATION_ENABLED, true)
    }

    fun setVibrationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIBRATION_ENABLED, enabled).apply()
    }
}
