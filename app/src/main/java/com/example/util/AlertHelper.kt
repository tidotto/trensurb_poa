package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.receiver.AlarmDismissReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AlertHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "trensurb_arrival_alerts"
        const val NOTIFICATION_ID = 1001

        private val _isAlarmActive = MutableStateFlow(false)
        val isAlarmActive: StateFlow<Boolean> = _isAlarmActive.asStateFlow()

        private var activeInstance: AlertHelper? = null

        fun stopActiveAlarm(context: Context) {
            activeInstance?.dismissAlert()
                ?: run {
                    _isAlarmActive.value = false
                    try {
                        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
                    } catch (e: Exception) {
                        Log.e("AlertHelper", "Error cancelling notification: ${e.message}")
                    }
                }
        }
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var alarmLoopJob: Job? = null

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    init {
        activeInstance = this
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Alertas de Desembarque Trensurb"
            val descriptionText = "Notificações sonoras e táteis de chegada à estação de destino no Trensurb."
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 450, 150, 450, 150, 800)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * Acende a tela do dispositivo bloqueado durante o alerta de desembarque.
     */
    fun wakeUpScreen() {
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            @Suppress("DEPRECATION")
            val wakeLock = powerManager?.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                "trensurb:screen_alert_wakelock"
            )
            wakeLock?.acquire(4500L)
        } catch (e: Exception) {
            Log.e("AlertHelper", "Could not wake up screen: ${e.message}")
        }
    }

    /**
     * Dispara alarme de chegada em loop contínuo de alta intensidade (até desarme do usuário ou 30s timeout).
     */
    fun playArrivalAlert(
        stationName: String,
        soundEnabled: Boolean = true,
        vibrationEnabled: Boolean = true
    ) {
        dismissAlert() // Limpa loop anterior se houver
        _isAlarmActive.value = true

        alarmLoopJob = coroutineScope.launch {
            val maxIterations = 12 // ~30 segundos máximos de alarme se o usuário não tocar
            for (i in 1..maxIterations) {
                if (!_isAlarmActive.value) break

                // 1. Acende a tela a cada pulso
                wakeUpScreen()

                // 2. Pulso de vibração intensa
                if (vibrationEnabled) {
                    executeArrivalVibration()
                }

                // 3. Toque sonoro de atenção máxima (canal de alarme)
                if (soundEnabled) {
                    executeAlarmTone()
                }

                // 4. Notificação com botão de desarme na tela de bloqueio
                sendArrivalSystemNotification(
                    title = "Atenção: Desembarque!",
                    message = "Você chegou à estação $stationName. Toque para desligar o alarme."
                )

                delay(2500L)
            }
            _isAlarmActive.value = false
        }
    }

    /**
     * Desliga imediatamente o alarme ativo, vibração e notificação.
     */
    fun dismissAlert() {
        _isAlarmActive.value = false
        alarmLoopJob?.cancel()
        alarmLoopJob = null

        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.e("AlertHelper", "Error stopping vibrator: ${e.message}")
        }

        try {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        } catch (e: Exception) {
            Log.e("AlertHelper", "Error cancelling notification: ${e.message}")
        }
    }

    private fun executeArrivalVibration() {
        try {
            vibrator?.let { vib ->
                if (vib.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val timings = longArrayOf(0, 450, 150, 450, 150, 800)
                        val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
                        vib.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                    } else {
                        @Suppress("DEPRECATION")
                        vib.vibrate(longArrayOf(0, 450, 150, 450, 150, 800), -1)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AlertHelper", "Vibration failed: ${e.message}")
        }
    }

    private fun executeAlarmTone() {
        try {
            // Tenta STREAM_ALARM para prioridade máxima no bolso
            val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 700)
        } catch (e: Exception) {
            try {
                val fallbackGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
                fallbackGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 700)
            } catch (ex: Exception) {
                Log.e("AlertHelper", "Tone playback failed: ${ex.message}")
            }
        }
    }

    /**
     * Dispara alerta preventivo de aproximação (próxima parada é o destino).
     */
    fun playApproachingAlert(
        stationName: String,
        soundEnabled: Boolean = true,
        vibrationEnabled: Boolean = true
    ) {
        wakeUpScreen()

        if (vibrationEnabled) {
            try {
                vibrator?.let { vib ->
                    if (vib.hasVibrator()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val timings = longArrayOf(0, 300, 120, 300)
                            val amplitudes = intArrayOf(0, 220, 0, 220)
                            vib.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                        } else {
                            @Suppress("DEPRECATION")
                            vib.vibrate(longArrayOf(0, 300, 120, 300), -1)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AlertHelper", "Approaching vibration failed: ${e.message}")
            }
        }

        if (soundEnabled) {
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 400)
            } catch (e: Exception) {
                Log.e("AlertHelper", "Approaching tone failed: ${e.message}")
            }
        }

        sendApproachingNotification(
            title = "Próxima parada: $stationName",
            message = "O trem está se aproximando do seu destino. Fique atento ao desembarque!"
        )
    }

    /**
     * Chime sutil entre estações intermediárias regulares.
     */
    fun playStationApproachingChime(soundEnabled: Boolean = true, vibrationEnabled: Boolean = true) {
        if (vibrationEnabled) {
            try {
                vibrator?.let { vib ->
                    if (vib.hasVibrator()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vib.vibrate(VibrationEffect.createOneShot(180, VibrationEffect.DEFAULT_AMPLITUDE))
                        } else {
                            @Suppress("DEPRECATION")
                            vib.vibrate(180)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AlertHelper", "Chime vibration failed: ${e.message}")
            }
        }

        if (soundEnabled) {
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 65)
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 250)
            } catch (e: Exception) {
                Log.e("AlertHelper", "Chime tone failed: ${e.message}")
            }
        }
    }

    private fun sendArrivalSystemNotification(title: String, message: String) {
        try {
            val contentIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val contentPendingIntent = PendingIntent.getActivity(
                context,
                0,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Ação de desarme seguro direto na notificação da tela de bloqueio
            val dismissIntent = Intent(context, AlarmDismissReceiver::class.java).apply {
                action = AlarmDismissReceiver.ACTION_DISMISS_ALARM
            }
            val dismissPendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setContentIntent(contentPendingIntent)
                .addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "Desembarquei / Desligar Alarme",
                    dismissPendingIntent
                )

            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            Log.w("AlertHelper", "Notification permission not granted: ${e.message}")
        } catch (e: Exception) {
            Log.e("AlertHelper", "System notification failed: ${e.message}")
        }
    }

    private fun sendApproachingNotification(title: String, message: String) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                2,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            Log.w("AlertHelper", "Notification permission not granted: ${e.message}")
        } catch (e: Exception) {
            Log.e("AlertHelper", "Approaching notification failed: ${e.message}")
        }
    }
}
