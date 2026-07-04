package io.github.vyachean.workprofiletoggle

internal class InMemoryKeyValueStore : KeyValueStore {
    private val values = mutableMapOf<String, Any?>()

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return values[key] as? Boolean ?: defaultValue
    }

    override fun getString(key: String): String? {
        return values[key] as? String
    }

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String): Set<String>? {
        return values[key] as? Set<String>
    }

    override fun edit(update: KeyValueStoreEditor.() -> Unit) {
        InMemoryKeyValueStoreEditor().apply {
            update()
            applyChanges()
        }
    }

    private inner class InMemoryKeyValueStoreEditor : KeyValueStoreEditor {
        private val pendingValues = mutableMapOf<String, Any?>()
        private val removedKeys = mutableSetOf<String>()

        override fun putBoolean(key: String, value: Boolean) {
            pendingValues[key] = value
            removedKeys.remove(key)
        }

        override fun putString(key: String, value: String?) {
            pendingValues[key] = value
            removedKeys.remove(key)
        }

        override fun putStringSet(key: String, values: Set<String>) {
            pendingValues[key] = values
            removedKeys.remove(key)
        }

        override fun remove(key: String) {
            removedKeys.add(key)
            pendingValues.remove(key)
        }

        fun applyChanges() {
            removedKeys.forEach { key -> values.remove(key) }
            pendingValues.forEach { (key, value) ->
                if (value == null) {
                    values.remove(key)
                } else {
                    values[key] = value
                }
            }
        }
    }
}
