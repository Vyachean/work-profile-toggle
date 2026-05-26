package io.github.vyachean.workprofiletoggle

import android.os.UserHandle
import android.os.UserManager

internal class ProfileRepository(
    private val userManager: UserManager,
) {
    fun loadProfiles(
        labelFactory: (ordinal: Int, serialNumber: Long) -> String,
        invalidSerialDiagnostic: String,
        serialFailureDiagnostic: (Throwable) -> String,
    ): ProfileSnapshot {
        val handlesBySerialNumber = mutableMapOf<Long, UserHandle>()
        val diagnosticEntries = mutableListOf<ProfileEntry.Diagnostic>()

        userManager.userProfiles.forEach { userHandle ->
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
                            serialDiagnostic = serialFailureDiagnostic(error),
                        )
                    },
                )
        }

        val labeledEntries = ProfileLabels.fromSerialNumbers(
            serialNumbers = handlesBySerialNumber.keys,
            labelFactory = labelFactory,
        ).map { profile ->
            val serialNumber = profile.identifier.serialNumber
            ProfileEntry.Labeled(
                userHandle = requireNotNull(handlesBySerialNumber[serialNumber]) {
                    "Missing UserHandle for profile serial $serialNumber"
                },
                profile = profile,
            )
        }

        return ProfileSnapshot(
            entries = labeledEntries + diagnosticEntries.sortedBy { it.userHandle.toString() },
            handlesBySerialNumber = handlesBySerialNumber.toMap(),
        )
    }
}

internal data class ProfileSnapshot(
    val entries: List<ProfileEntry>,
    val handlesBySerialNumber: Map<Long, UserHandle>,
)

internal sealed class ProfileEntry {
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
