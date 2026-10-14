package io.github.khoryz666.timer.tracking

import android.annotation.SuppressLint
import android.app.ForegroundServiceStartNotAllowedException
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import io.github.khoryz666.timer.appGraph
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Keeps the tracking notification up all day and handles its quick actions. It holds no
 * tracking state of its own - a restart (START_STICKY, boot, app update) just re-reads the
 * database - so being killed never loses or corrupts time.
 */
class TrackingService : Service() {

    private val scope = MainScope()
    private val graph by lazy { appGraph }
    private var content = NotificationContent.Loading

    override fun onCreate() {
        super.onCreate()
        graph.notifications.createChannels()
        scope.launch {
            graph.tracker.today(TICK_MS)
                .map(NotificationContent::of)
                .distinctUntilChanged()
                .collect {
                    content = it
                    graph.notifications.postTracking(it)
                }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!enterForeground()) return START_NOT_STICKY
        when (intent?.action) {
            ACTION_SWITCH -> scope.launch { graph.tracker.switchTo(intent.target()) }
            ACTION_STOP -> scope.launch { graph.tracker.stop() }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null

    @SuppressLint("InlinedApi") // ServiceCompat ignores the service type below API 34.
    private fun enterForeground(): Boolean = try {
        ServiceCompat.startForeground(
            this,
            TrackingNotifications.TRACKING_ID,
            graph.notifications.tracking(content),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )
        true
    } catch (e: IllegalStateException) {
        // Background-start restrictions (Android 12+); the app or the next boot starts us again.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && e is ForegroundServiceStartNotAllowedException) {
            stopSelf()
            false
        } else {
            throw e
        }
    }

    companion object {
        private const val TICK_MS = 30_000L
        private const val ACTION_SHOW = "show"
        private const val ACTION_SWITCH = "switch"
        private const val ACTION_STOP = "stop"
        private const val EXTRA_PROJECT_ID = "projectId"

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, TrackingService::class.java))
        }

        internal fun showIntent(context: Context): PendingIntent = servicePendingIntent(context, 0, Intent(ACTION_SHOW))

        internal fun actionIntent(context: Context, action: QuickAction): PendingIntent = when (action) {
            QuickAction.Stop -> servicePendingIntent(context, 1, Intent(ACTION_STOP))
            is QuickAction.Switch -> when (val target = action.target) {
                TrackTarget.Joy -> servicePendingIntent(context, 2, Intent(ACTION_SWITCH))
                is TrackTarget.Engine -> servicePendingIntent(
                    context,
                    // Distinct request codes so each project's intent keeps its own extras.
                    1000 + target.projectId.toInt(),
                    Intent(ACTION_SWITCH).putExtra(EXTRA_PROJECT_ID, target.projectId),
                )
            }
        }

        private fun servicePendingIntent(context: Context, requestCode: Int, intent: Intent): PendingIntent =
            PendingIntent.getService(
                context,
                requestCode,
                intent.setClass(context, TrackingService::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        private fun Intent.target(): TrackTarget =
            if (hasExtra(EXTRA_PROJECT_ID)) TrackTarget.Engine(getLongExtra(EXTRA_PROJECT_ID, 0)) else TrackTarget.Joy
    }
}
