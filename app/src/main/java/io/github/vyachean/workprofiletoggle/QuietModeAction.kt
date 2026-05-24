package io.github.vyachean.workprofiletoggle

enum class QuietModeAction(
    val intentAction: String,
) {
    Enable("io.github.vyachean.workprofiletoggle.action.ENABLE_QUIET_MODE"),
    Disable("io.github.vyachean.workprofiletoggle.action.DISABLE_QUIET_MODE"),
    Toggle("io.github.vyachean.workprofiletoggle.action.TOGGLE_QUIET_MODE"),
    ;

    companion object {
        fun fromIntentAction(intentAction: String?): QuietModeAction? {
            return entries.firstOrNull { action -> action.intentAction == intentAction }
        }
    }
}
