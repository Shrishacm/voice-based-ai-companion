package com.mithra.assistant.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mithra.assistant.service.MithraWakeService

private const val TAG = "VOICE_BOOT"

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Boot completed, starting wake service")
            val serviceIntent = Intent(context, MithraWakeService::class.java)
            context.startForegroundService(serviceIntent)
        }
    }
}
