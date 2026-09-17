package com.clinic.callmonitor.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.clinic.callmonitor.CallMonitorApp
import com.clinic.callmonitor.service.FileWatcherService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && CallMonitorApp.isConsentGiven(context)) {
            context.startService(Intent(context, FileWatcherService::class.java))
        }
    }
}
