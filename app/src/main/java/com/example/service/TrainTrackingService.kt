package com.example.service

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
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TrainTrackingService : Service() {

    companion object {
        const val TAG = "TrainTrackingService"
        const val CHANNEL_ID = "trensurb_live_tracking_channel"
        const val CHANNEL_NAME = "Acompanhamento ao Vivo Trensurb"
        const val NOTIFICATION_ID = 2002

        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"
        const val ACTION_UPDATE = "com.example.service.ACTION_UPDATE"

        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_TEXT = "extra_text"
        const val EXTRA_BIG_TEXT = "extra_big_text"

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        private var currentTitle: String = "Trensurb - Acompanhamento ao Vivo"
        private var currentText: String = "Monitorando estações e paradas..."
        private var currentBigText: String = "Rastreando localização do trem em tempo real."

        fun start(context: Context) {
            val intent = Intent(context, TrainTrackingService::class.java).apply {
                action = ACTION_START
            }
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start TrainTrackingService: ${e.message}")
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, TrainTrackingService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop TrainTrackingService: ${e.message}")
            }
        }

        fun updateLiveTrip(
            context: Context,
            title: String,
            text: String,
            bigText: String
        ) {
            currentTitle = title
            currentText = text
            currentBigText = bigText

            if (!_isServiceRunning.value) return

            val intent = Intent(context, TrainTrackingService::class.java).apply {
                action = ACTION_UPDATE
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_TEXT, text)
                putExtra(EXTRA_BIG_TEXT, bigText)
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Could not send update to service: ${e.message}")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                Log.d(TAG, "Stopping service via intent")
                _isServiceRunning.value = false
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_UPDATE -> {
                val title = intent.getStringExtra(EXTRA_TITLE) ?: currentTitle
                val text = intent.getStringExtra(EXTRA_TEXT) ?: currentText
                val bigText = intent.getStringExtra(EXTRA_BIG_TEXT) ?: currentBigText
                currentTitle = title
                currentText = text
                currentBigText = bigText
                updateNotificationDirectly()
                return START_STICKY
            }
            ACTION_START, null -> {
                Log.d(TAG, "Starting foreground service")
                _isServiceRunning.value = true
                val notification = buildLiveNotification(currentTitle, currentText, currentBigText)

                val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                } else {
                    0
                }

                try {
                    ServiceCompat.startForeground(
                        this,
                        NOTIFICATION_ID,
                        notification,
                        foregroundServiceType
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "ServiceCompat.startForeground failed: ${e.message}")
                    @Suppress("DEPRECATION")
                    startForeground(NOTIFICATION_ID, notification)
                }
                return START_STICKY
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        _isServiceRunning.value = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificação persistente visível na tela de bloqueio com estações e ETA do Trensurb"
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    private fun updateNotificationDirectly() {
        if (!_isServiceRunning.value) return
        try {
            val notification = buildLiveNotification(currentTitle, currentText, currentBigText)
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission not available: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update notification: ${e.message}")
        }
    }

    private fun buildLiveNotification(title: String, text: String, bigText: String): Notification {
        // Intent para abrir o app ao clicar na notificação
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            10,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent para parar o rastreamento direto na notificação
        val stopIntent = Intent(this, TrainTrackingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            11,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Garante exibição na tela de bloqueio
            .setContentIntent(openAppPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Parar Rastreamento", stopPendingIntent)
            .build()
    }
}
