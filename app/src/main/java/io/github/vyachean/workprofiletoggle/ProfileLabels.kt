package io.github.vyachean.workprofiletoggle

/**
 * Stable platform profile identifier that can be persisted and passed through shortcuts.
 *
 * Android exposes UserHandle instances, but core labeling and ordering rules should not depend on
 * Android framework classes so they can stay unit-testable.
 */
data class ProfileIdentifier(
    val serialNumber: Long,
)

data class DiscoveredProfile(
    val identifier: ProfileIdentifier,
    val label: String,
)

object ProfileLabels {
    fun fromSerialNumbers(serialNumbers: Iterable<Long>): List<DiscoveredProfile> {
        return serialNumbers
            .distinct()
            .sorted()
            .mapIndexed { index, serialNumber ->
                DiscoveredProfile(
                    identifier = ProfileIdentifier(serialNumber),
                    label = fallbackLabel(index, serialNumber),
                )
            }
    }

    private fun fallbackLabel(index: Int, serialNumber: Long): String {
        return "Profile ${index + 1} (serial $serialNumber)"
    }
}
