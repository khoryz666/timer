package com.example.timetracker.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.timetracker.data.ActiveState
import com.example.timetracker.data.TrackerPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Resumes the foreground notification after a reboot if a category was left running. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val activeState = TrackerPreferences(context).activeStateFlow.first()
                if (activeState != ActiveState.IDLE) {
                    TimeTrackerService.start(context)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
