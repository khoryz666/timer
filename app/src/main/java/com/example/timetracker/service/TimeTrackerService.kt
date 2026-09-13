package com.example.timetracker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.timetracker.MainActivity
import com.example.timetracker.data.ActiveState
import com.example.timetracker.data.TrackerRepository
import com.example.timetracker.util.TimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class TimeTrackerService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private lateinit var repository: TrackerRepository

    companion object {
        const val CHANNEL_ID = "TimeTrackerChannel"
        const val NOTIFICATION_ID = 1

        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"
        const val ACTION_WORK = "ACTION_WORK"
        const val ACTION_SELF = "ACTION_SELF"
        const val ACTION_SLEEP = "ACTION_SLEEP"

        /** Test seam: overridden in tests to hand the service an in-memory repository. */
        internal var repositoryFactory: (Context) -> TrackerRepository = { TrackerRepository(it) }
    }

    override fun onCreate() {
        super.onCreate()
        repository = repositoryFactory(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForegroundService()
            }
            ACTION_STOP_SERVICE -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_WORK -> updateState(ActiveState.WORK)
            ACTION_SELF -> updateState(ActiveState.SELF)
            ACTION_SLEEP -> updateState(ActiveState.SLEEP)
        }
        return START_STICKY
    }

    private fun updateState(newState: ActiveState) {
        serviceScope.launch {
            // Goes through the shared repository so switching via the notification
            // persists the outgoing state's elapsed time exactly like the in-app buttons do.
            repository.switchState(newState)
        }
    }

    private fun startForegroundService() {
        startForeground(NOTIFICATION_ID, buildNotification("Loading...", ActiveState.IDLE))

        serviceScope.launch {
            repository.snapshotFlow().collect { snapshot ->
                val baseDuration = when (snapshot.activeState) {
                    ActiveState.WORK -> snapshot.workDurationMs
                    ActiveState.SELF -> snapshot.selfDurationMs
                    ActiveState.SLEEP -> snapshot.sleepDurationMs
                    ActiveState.IDLE -> 0L
                }
                updateNotification(snapshot.activeState, baseDuration + snapshot.activelyTickingMs)
            }
        }
    }

    private fun updateNotification(state: ActiveState, elapsedMs: Long) {
        val timeString = TimeFormatter.formatHMS(elapsedMs)
        val stateName = when (state) {
            ActiveState.IDLE -> "Idle"
            ActiveState.WORK -> "Work"
            ActiveState.SELF -> "Self"
            ActiveState.SLEEP -> "Sleep"
        }

        val text = if (state == ActiveState.IDLE) "Ready to track time" else "$stateName: $timeString"

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification(text, state))
    }

    private fun buildNotification(text: String, currentState: ActiveState): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_today)
            .setContentTitle("Time Tracker")
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setOngoing(true)

        if (currentState != ActiveState.WORK) {
            builder.addAction(0, "Work", getPendingIntentForAction(ACTION_WORK))
        }
        if (currentState != ActiveState.SELF) {
            builder.addAction(0, "Self", getPendingIntentForAction(ACTION_SELF))
        }
        if (currentState != ActiveState.SLEEP) {
            builder.addAction(0, "Sleep", getPendingIntentForAction(ACTION_SLEEP))
        }

        return builder.build()
    }

    private fun getPendingIntentForAction(action: String): PendingIntent {
        val intent = Intent(this, TimeTrackerService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(this, action.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Time Tracker Running"
            val descriptionText = "Displays the active timer"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
