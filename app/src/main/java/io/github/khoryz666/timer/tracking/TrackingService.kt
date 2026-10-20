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
import io.github.khoryz666.timer.day.DayType
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
        scope.launch {
            graph.clock.ticks(DAY_PROMPT_CHECK_MS).collect { graph.dayPrompter.postIfDue() }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!enterForeground()) return START_NOT_STICKY
        when (intent?.action) {
            ACTION_SWITCH -> intent.target()?.let { scope.launch { graph.tracker.switchTo(it) } }
            ACTION_STOP -> scope.launch { graph.tracker.stop() }
            ACTION_SET_DAY_TYPE -> intent.dayType()?.let { scope.launch { graph.tracker.setDayType(it) } }
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
        private const val DAY_PROMPT_CHECK_MS = 60_000L
        private const val ACTION_SHOW = "show"
        private const val ACTION_SWITCH = "switch"
        private const val ACTION_STOP = "stop"
        private const val ACTION_SET_DAY_TYPE = "setDayType"
        private const val EXTRA_PILLAR = "pillar"
        private const val EXTRA_PROJECT_ID = "projectId"
        private const val EXTRA_DAY_TYPE = "dayType"

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, TrackingService::class.java))
        }

        internal fun showIntent(context: Context): PendingIntent = servicePendingIntent(context, 0, Intent(ACTION_SHOW))

        // Distinct request codes, so each intent keeps its own extras.
        internal fun actionIntent(context: Context, action: QuickAction): PendingIntent = when (action) {
            QuickAction.Stop -> servicePendingIntent(context, 1, Intent(ACTION_STOP))
            is QuickAction.Switch -> {
                val target = action.target
                val intent = Intent(ACTION_SWITCH).putExtra(EXTRA_PILLAR, target.pillar.name)
                if (target is TrackTarget.Project) {
                    servicePendingIntent(context, 1000 + target.projectId.toInt(), intent.putExtra(EXTRA_PROJECT_ID, target.projectId))
                } else {
                    servicePendingIntent(context, 2 + target.pillar.ordinal, intent)
                }
            }
        }

        internal fun dayTypeIntent(context: Context, type: DayType): PendingIntent =
            servicePendingIntent(context, 10 + type.ordinal, Intent(ACTION_SET_DAY_TYPE).putExtra(EXTRA_DAY_TYPE, type.name))

        private fun servicePendingIntent(context: Context, requestCode: Int, intent: Intent): PendingIntent =
            PendingIntent.getService(
                context,
                requestCode,
                intent.setClass(context, TrackingService::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        private fun Intent.target(): TrackTarget? {
            val pillar = getStringExtra(EXTRA_PILLAR)?.let { name -> Pillar.entries.find { it.name == name } } ?: return null
            if (pillar == Pillar.PROJECT && !hasExtra(EXTRA_PROJECT_ID)) return null
            return TrackTarget.of(pillar, getLongExtra(EXTRA_PROJECT_ID, 0))
        }

        private fun Intent.dayType(): DayType? = getStringExtra(EXTRA_DAY_TYPE)?.let { name -> DayType.entries.find { it.name == name } }
    }
}
