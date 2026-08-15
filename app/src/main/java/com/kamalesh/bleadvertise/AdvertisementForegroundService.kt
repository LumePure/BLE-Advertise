package com.kamalesh.bleadvertise

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class AdvertisementForegroundService : Service() {

    companion object {
        private const val CHANNEL_ID = "ble_advertise_channel"
        private const val NOTIFICATION_ID = 1
        private const val PREFS_NAME = "ble_advertise_prefs"
        private const val KEY_DEVICE_ID = "device_id"

        const val ACTION_START = "com.kamalesh.bleadvertise.action.START"
        const val ACTION_STOP = "com.kamalesh.bleadvertise.action.STOP"
        const val EXTRA_DEVICE_ID = "device_id"

        @Volatile
        var isRunning: Boolean = false
            private set

        @Volatile
        var currentDeviceId: String? = null
            private set

        fun start(context: Context, deviceId: String) {
            val intent = Intent(context, AdvertisementForegroundService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_DEVICE_ID, deviceId)
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, AdvertisementForegroundService::class.java)
                .setAction(ACTION_STOP)
            context.startService(intent)
        }
    }

    private var advertiser: BleAdvertiser? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        advertiser = BleAdvertiser(this).also { ble ->
            ble.listener = object : BleAdvertiser.Listener {
                override fun onAdvertisingStarted() {
                    updateNotification(getString(R.string.advertising_in_progress))
                }

                override fun onAdvertisingStopped() {
                    updateNotification(getString(R.string.not_advertising))
                }

                override fun onAdvertisingFailed(reason: String) {
                    updateNotification("Error: $reason")
                }
            }
        }

        val savedId = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(KEY_DEVICE_ID, null)
        if (savedId != null) {
            currentDeviceId = savedId
            isRunning = true
            DeviceProfiles.getById(savedId)?.let { advertiser?.start(it.payload) }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val deviceId = intent.getStringExtra(EXTRA_DEVICE_ID)
                val profile = deviceId?.let { DeviceProfiles.getById(it) }
                if (profile == null) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                currentDeviceId = deviceId
                isRunning = true
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .edit().putString(KEY_DEVICE_ID, deviceId).apply()
                startForegroundCompat(buildNotification(getString(R.string.advertising_in_progress)))
                advertiser?.stop()
                advertiser?.start(profile.payload)
            }
            ACTION_STOP -> {
                currentDeviceId = null
                isRunning = false
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .edit().remove(KEY_DEVICE_ID).apply()
                advertiser?.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> {
                val savedId = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(KEY_DEVICE_ID, null)
                if (savedId != null) {
                    isRunning = true
                    startForegroundCompat(buildNotification(getString(R.string.advertising_in_progress)))
                } else {
                    stopSelf()
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        advertiser?.stop()
        advertiser = null
        isRunning = false
        super.onDestroy()
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "BLE advertising",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, AdvertisementForegroundService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_stat_ble)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .addAction(0, "Stop", stopIntent)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }
}
