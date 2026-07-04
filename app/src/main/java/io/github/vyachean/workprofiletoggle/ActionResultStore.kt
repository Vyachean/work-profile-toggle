package io.github.vyachean.workprofiletoggle

private const val PREF_LAST_RESULT = "last_result"

internal class ActionResultStore(
    private val keyValueStore: KeyValueStore,
    private val defaultResult: String,
) {
    fun restore(savedResult: String?): String {
        return savedResult
            ?: keyValueStore.getString(PREF_LAST_RESULT)
            ?: defaultResult
    }

    fun save(result: String) {
        keyValueStore.edit {
            putString(PREF_LAST_RESULT, result)
        }
    }
}
