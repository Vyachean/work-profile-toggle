package io.github.vyachean.workprofiletoggle

import android.app.Activity
import android.os.Bundle

class QuietModeActionActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dependencies = WorkProfileAppDependencies(this)
        dependencies.actionDispatcher.dispatchShortcut(intent)
        finish()
        overridePendingTransition(0, 0)
    }
}
