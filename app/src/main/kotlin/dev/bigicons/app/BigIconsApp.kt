package dev.bigicons.app

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import dev.bigicons.app.widget.WidgetUpdater

class BigIconsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // ACTION_CONFIGURATION_CHANGED нельзя принимать из манифеста — слушаем, пока жив процесс
        ContextCompat.registerReceiver(this, object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                WidgetUpdater.updateAll(context)
            }
        }, IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED)
    }
}
