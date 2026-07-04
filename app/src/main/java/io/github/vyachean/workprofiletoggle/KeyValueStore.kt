package io.github.vyachean.workprofiletoggle

import android.content.SharedPreferences

internal interface KeyValueStore {
    fun getBoolean(key: String, defaultValue: Boolean): Boolean
    fun getString(key: String): String?
    fun getStringSet(key: String): Set<String>?
    fun edit(update: KeyValueStoreEditor.() -> Unit)
}

internal interface KeyValueStoreEditor {
    fun putBoolean(key: String, value: Boolean)
    fun putString(key: String, value: String?)
    fun putStringSet(key: String, values: Set<String>)
    fun remove(key: String)
}

internal class SharedPreferencesKeyValueStore(
    private val preferences: SharedPreferences,
) : KeyValueStore {
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return preferences.getBoolean(key, defaultValue)
    }

    override fun getString(key: String): String? {
        return preferences.getString(key, null)
    }

    override fun getStringSet(key: String): Set<String>? {
        return preferences.getStringSet(key, null)?.toSet()
    }

    override fun edit(update: KeyValueStoreEditor.() -> Unit) {
        val editor = SharedPreferencesKeyValueStoreEditor(preferences.edit())
        editor.update()
        editor.apply()
    }

    private class SharedPreferencesKeyValueStoreEditor(
        private val editor: SharedPreferences.Editor,
    ) : KeyValueStoreEditor {
        override fun putBoolean(key: String, value: Boolean) {
            editor.putBoolean(key, value)
        }

        override fun putString(key: String, value: String?) {
            editor.putString(key, value)
        }

        override fun putStringSet(key: String, values: Set<String>) {
            editor.putStringSet(key, values)
        }

        override fun remove(key: String) {
            editor.remove(key)
        }

        fun apply() {
            editor.apply()
        }
    }
}
