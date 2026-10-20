package io.github.khoryz666.timer.tracking

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import io.github.khoryz666.timer.MainActivity
import io.github.khoryz666.timer.R
import io.github.khoryz666.timer.day.DayPromptPoster
import io.github.khoryz666.timer.day.DayType

class TrackingNotifications(private val context: Context) : TrackerNotices, DayPromptPoster {

    private val manager = context.getSystemService(NotificationManager::class.java)

    fun createChannels() {
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(TRACKING_CHANNEL, "Tracking", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "What's running, today's progress and quick switches"
                    setShowBadge(false)
                },
                NotificationChannel(DAY_PROMPT_CHANNEL, "Day type", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Asks once a day whether today is a Light, Class or Deep day"
                },
                NotificationChannel(NUDGE_CHANNEL, "Nudges", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Optional heads-up when Joy starts before any focus block"
                },
            )
        )
    }

    fun tracking(content: NotificationContent): Notification {
        val builder = NotificationCompat.Builder(context, TRACKING_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(content.title)
            .setContentText(content.text)
            .setSubText(content.dayType)
            .setContentIntent(openApp())
            // Android 14+ lets users swipe this away; the service puts it straight back.
            .setDeleteIntent(TrackingService.showIntent(context))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        if (content.lines.isNotEmpty()) {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(content.lines.joinToString("\n")))
        }
        if (content.runningSince != null) {
            builder.setCategory(NotificationCompat.CATEGORY_STOPWATCH)
                .setWhen(content.runningSince)
                .setShowWhen(true)
                .setUsesChronometer(true)
        } else {
            builder.setShowWhen(false)
        }
        content.actions.forEach { builder.addAction(0, it.label, TrackingService.actionIntent(context, it)) }
        return builder.build()
    }

    fun postTracking(content: NotificationContent) = manager.notify(TRACKING_ID, tracking(content))

    override fun postDayPrompt(inForce: DayType) {
        val builder = NotificationCompat.Builder(context, DAY_PROMPT_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("What kind of day is today?")
            .setContentText("Until you pick, today uses ${inForce.label} targets.")
            .setContentIntent(openApp())
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
        DayType.entries.forEach { builder.addAction(0, it.label, TrackingService.dayTypeIntent(context, it)) }
        manager.notify(DAY_PROMPT_ID, builder.build())
    }

    override fun dismissDayPrompt() = manager.cancel(DAY_PROMPT_ID)

    override fun nudgeJoyBeforeFocus() {
        val notification = NotificationCompat.Builder(context, NUDGE_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Joy before focus")
            .setContentText("No focus block yet today. Enjoy - this is just a heads-up.")
            .setContentIntent(openApp())
            .setAutoCancel(true)
            .build()
        manager.notify(NUDGE_ID, notification)
    }

    private fun openApp(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        const val TRACKING_ID = 1
        private const val NUDGE_ID = 2
        private const val DAY_PROMPT_ID = 3
        private const val TRACKING_CHANNEL = "tracking"
        private const val NUDGE_CHANNEL = "nudges"
        private const val DAY_PROMPT_CHANNEL = "day_type"
    }
}
