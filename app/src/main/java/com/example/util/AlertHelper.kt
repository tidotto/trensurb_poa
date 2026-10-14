package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

class AlertHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "trensurb_arrival_alerts"
        const val NOTIFICATION_ID = 1001
    }

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
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Alertas de Desembarque Trensurb"
            val descriptionText = "Notificações de aproximação e chegada à estação de destino no Trensurb."
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 150, 400, 150, 800)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * Dispara alerta sonoro, vibração intensa e notificação do sistema para chegada ao destino final.
     */
    fun playArrivalAlert(
        stationName: String,
        soundEnabled: Boolean = true,
        vibrationEnabled: Boolean = true
    ) {
        // 1. Vibração de alta intensidade (padrão de alerta de chegada)
        if (vibrationEnabled) {
            try {
                vibrator?.let { vib ->
                    if (vib.hasVibrator()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val timings = longArrayOf(0, 400, 150, 400, 150, 800)
                            val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
                            vib.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                        } else {
                            @Suppress("DEPRECATION")
                            vib.vibrate(longArrayOf(0, 400, 150, 400, 150, 800), -1)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AlertHelper", "Vibration failed: ${e.message}")
            }
        }

        // 2. Toque sonoro de atenção imediata
        if (soundEnabled) {
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 700)
            } catch (e: Exception) {
                Log.e("AlertHelper", "Tone playback failed: ${e.message}")
            }
        }

        // 3. Notificação do Sistema
        sendSystemNotification(
            title = "Atenção: Desembarque!",
            message = "Você chegou à estação $stationName. Prepare-se para desembarcar do trem."
        )
    }

    /**
     * Dispara alerta sonoro e vibração de aproximação (próxima parada é o destino).
     */
    fun playApproachingAlert(
        stationName: String,
        soundEnabled: Boolean = true,
        vibrationEnabled: Boolean = true
    ) {
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

        sendSystemNotification(
            title = "Próxima parada: $stationName",
            message = "O trem está se aproximando do seu destino. Fique atento ao desembarque!"
        )
    }

    /**
     * Chime simples de transição entre estações regulares.
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

    private fun sendSystemNotification(title: String, message: String) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            Log.w("AlertHelper", "Notification permission not granted: ${e.message}")
        } catch (e: Exception) {
            Log.e("AlertHelper", "System notification failed: ${e.message}")
        }
    }
}
