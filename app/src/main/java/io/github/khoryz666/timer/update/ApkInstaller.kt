package io.github.khoryz666.timer.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller

import android.os.Build
import android.provider.Settings
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

/** Hands a downloaded APK to the system installer, which asks the user to confirm. */
class ApkInstaller(private val context: Context) {

    private val _failures = MutableStateFlow<String?>(null)

    /** The latest install failure reported by the system, until [clearFailure]. */
    val failures: StateFlow<String?> = _failures.asStateFlow()

    val apkFile: File get() = File(context.cacheDir, "updates/Timer.apk")

    /** Whether the user has allowed this app to install apps ("Install unknown apps"). */
    fun canInstall(): Boolean = context.packageManager.canRequestPackageInstalls()

    /** The system screen where the user grants [canInstall]. */
    fun permissionSettingsIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, "package:${context.packageName}".toUri())

    suspend fun install(apk: File) = withContext(Dispatchers.IO) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
        }
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            session.openWrite("update.apk", 0, apk.length()).use { out ->
                apk.inputStream().use { it.copyTo(out) }
                session.fsync(out)
            }
            session.commit(resultIntent(sessionId).intentSender)
        }
    }

    internal fun reportFailure(message: String) {
        _failures.value = message
    }

    fun clearFailure() {
        _failures.value = null
    }

    private fun resultIntent(sessionId: Int): PendingIntent {
        // The installer fills in the status extras, so the intent must be mutable on Android 12+.
        val mutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        return PendingIntent.getBroadcast(
            context,
            sessionId,
            Intent(context, InstallResultReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or mutable,
        )
    }
}
