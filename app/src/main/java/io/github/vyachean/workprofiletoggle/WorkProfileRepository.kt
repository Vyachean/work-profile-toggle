package io.github.vyachean.workprofiletoggle

import android.content.SharedPreferences
import android.os.UserHandle
import android.os.UserManager

private const val PREF_SELECTED_PROFILE_SERIAL = "selected_profile_serial"

class WorkProfileRepository(
    private val userManager: UserManager,
    private val preferences: SharedPreferences,
    private val profileLabel: (ordinal: Int, serialNumber: Long) -> String,
    private val invalidSerialDiagnostic: String,
    private val formatFailure: (operation: String, error: Throwable) -> String,
) {
    private var userHandlesBySerialNumber: Map<Long, UserHandle> = emptyMap()

    fun discoverProfiles(): ProfileDiscovery {
        val profilesResult = runCatching { userManager.userProfiles }
        val profileEntries = profilesResult.getOrNull()
            ?.let { profiles -> createProfileEntries(profiles) }
            ?: emptyList()

        return ProfileDiscovery(
            profileEntries = profileEntries,
            error = profilesResult.exceptionOrNull(),
        )
    }

    fun resolveProfileSelection(profiles: List<ProfileEntry.Labeled>): ProfileSelection {
        val selectedSerialNumber = selectedProfileSerialNumber()
        val selectedProfile = selectedSerialNumber?.let { serialNumber ->
            profiles.firstOrNull { profileEntry -> profileEntry.profile.identifier.serialNumber == serialNumber }
        }

        if (selectedProfile != null) {
            return ProfileSelection(
                selected = selectedProfile,
                availableProfiles = profiles,
                missingSelectedProfile = false,
            )
        }

        if (selectedSerialNumber == null && profiles.size == 1) {
            val onlyProfile = profiles.single()
            saveSelectedProfile(onlyProfile)
            return ProfileSelection(
                selected = onlyProfile,
                availableProfiles = profiles,
                missingSelectedProfile = false,
            )
        }

        return ProfileSelection(
            selected = null,
            availableProfiles = profiles,
            missingSelectedProfile = selectedSerialNumber != null,
        )
    }

    fun saveSelectedProfile(profileEntry: ProfileEntry.Labeled) {
        preferences.edit()
            .putLong(PREF_SELECTED_PROFILE_SERIAL, profileEntry.profile.identifier.serialNumber)
            .apply()
    }

    fun clearSelectedProfile() {
        preferences.edit()
            .remove(PREF_SELECTED_PROFILE_SERIAL)
            .apply()
    }

    fun findUserHandle(serialNumber: Long): UserHandle? {
        userHandlesBySerialNumber[serialNumber]?.let { userHandle ->
            return userHandle
        }

        discoverProfiles()
        return userHandlesBySerialNumber[serialNumber]
    }

    private fun createProfileEntries(profiles: List<UserHandle>): List<ProfileEntry> {
        val handlesBySerialNumber = mutableMapOf<Long, UserHandle>()
        val diagnosticEntries = mutableListOf<ProfileEntry.Diagnostic>()

        profiles.forEach { userHandle ->
            runCatching { userManager.getSerialNumberForUser(userHandle) }
                .fold(
                    onSuccess = { serialNumber ->
                        when (serialNumber) {
                            INVALID_SERIAL_NUMBER -> {
                                diagnosticEntries += ProfileEntry.Diagnostic(
                                    userHandle = userHandle,
                                    serialDiagnostic = invalidSerialDiagnostic,
                                )
                            }
                            OWNER_PROFILE_SERIAL_NUMBER -> Unit
                            else -> handlesBySerialNumber.putIfAbsent(serialNumber, userHandle)
                        }
                    },
                    onFailure = { error ->
                        diagnosticEntries += ProfileEntry.Diagnostic(
                            userHandle = userHandle,
                            serialDiagnostic = formatFailure("getSerialNumberForUser", error),
                        )
                    },
                )
        }
        userHandlesBySerialNumber = handlesBySerialNumber.toMap()

        val labeledEntries = ProfileLabels.fromSerialNumbers(handlesBySerialNumber.keys, profileLabel)
            .map { profile ->
                val serialNumber = profile.identifier.serialNumber
                ProfileEntry.Labeled(
                    userHandle = requireNotNull(handlesBySerialNumber[serialNumber]) {
                        "Missing UserHandle for profile serial $serialNumber"
                    },
                    profile = profile,
                )
            }

        return labeledEntries + diagnosticEntries.sortedBy { it.userHandle.toString() }
    }

    private fun selectedProfileSerialNumber(): Long? {
        return if (preferences.contains(PREF_SELECTED_PROFILE_SERIAL)) {
            preferences.getLong(PREF_SELECTED_PROFILE_SERIAL, INVALID_SERIAL_NUMBER)
        } else {
            null
        }
    }
}

data class ProfileDiscovery(
    val profileEntries: List<ProfileEntry>,
    val error: Throwable?,
) {
    val labeledEntries: List<ProfileEntry.Labeled> = profileEntries.filterIsInstance<ProfileEntry.Labeled>()
    val profilesAvailable: Boolean = error == null
}

sealed class ProfileEntry {
    abstract val userHandle: UserHandle

    data class Labeled(
        override val userHandle: UserHandle,
        val profile: DiscoveredProfile,
    ) : ProfileEntry()

    data class Diagnostic(
        override val userHandle: UserHandle,
        val serialDiagnostic: String,
    ) : ProfileEntry()
}

data class ProfileSelection(
    val selected: ProfileEntry.Labeled?,
    val availableProfiles: List<ProfileEntry.Labeled>,
    val missingSelectedProfile: Boolean,
)
