package io.github.vyachean.workprofiletoggle

/**
 * Stable platform profile identifier that can be persisted and passed through shortcuts.
 *
 * Android exposes UserHandle instances, but core labeling and ordering rules should not depend on
 * Android framework classes so they can stay unit-testable.
 */
@JvmInline
value class ProfileIdentifier(
    val serialNumber: Long,
)

data class DiscoveredProfile(
    val identifier: ProfileIdentifier,
    val label: String,
)

object ProfileLabels {
    fun fromSerialNumbers(
        serialNumbers: Iterable<Long>,
        labelFactory: (ordinal: Int, serialNumber: Long) -> String,
    ): List<DiscoveredProfile> {
        return serialNumbers
            .distinct()
            .sorted()
            .mapIndexed { index, serialNumber ->
                val ordinal = index + 1
                DiscoveredProfile(
                    identifier = ProfileIdentifier(serialNumber),
                    label = labelFactory(ordinal, serialNumber),
                )
            }
    }
}
