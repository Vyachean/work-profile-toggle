package io.github.vyachean.workprofiletoggle

import android.content.SharedPreferences

private const val PREF_LAST_RESULT = "last_result"

internal class ActionResultStore(
    private val preferences: SharedPreferences,
    private val defaultResult: String,
) {
    fun restore(savedResult: String?): String {
        return savedResult
            ?: preferences.getString(PREF_LAST_RESULT, null)
            ?: defaultResult
    }

    fun save(result: String) {
        preferences.edit()
            .putString(PREF_LAST_RESULT, result)
            .apply()
    }
}
