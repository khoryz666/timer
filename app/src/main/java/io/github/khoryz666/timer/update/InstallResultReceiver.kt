package io.github.khoryz666.timer.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import androidx.core.content.IntentCompat
import io.github.khoryz666.timer.appGraph

/** Receives the system installer's verdict: show its confirmation screen, or report a failure. */
class InstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)
                    ?.let { context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
            PackageInstaller.STATUS_SUCCESS -> Unit // The app is replaced and restarted.
            PackageInstaller.STATUS_FAILURE_ABORTED -> context.appGraph.installer.reportFailure("The update was cancelled.")
            else -> {
                val detail = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                context.appGraph.installer.reportFailure("The update couldn't be installed" + (detail?.let { ": $it" } ?: "."))
            }
        }
    }
}
