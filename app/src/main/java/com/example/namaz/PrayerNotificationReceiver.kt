package com.example.namaz

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

class PrayerNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra("PRAYER_NAME") ?: "Namaz"
        val prayerTime = intent.getStringExtra("PRAYER_TIME") ?: ""

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "namaz_reminder_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Prayer Time Reminders (Namaz)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for 5 daily prayers"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_focus_launcher)
            .setContentTitle("🕌 Time for $prayerName Prayer ($prayerTime)")
            .setContentText("Step away from work to pray and renew your focus.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(prayerName.hashCode(), notification)
    }

    companion object {
        fun schedulePrayerAlarm(
            context: Context,
            prayerName: String,
            timeFormatted: String,
            timestampMillis: Long
        ) {
            if (timestampMillis <= System.currentTimeMillis()) return

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, PrayerNotificationReceiver::class.java).apply {
                putExtra("PRAYER_NAME", prayerName)
                putExtra("PRAYER_TIME", timeFormatted)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                prayerName.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        timestampMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        timestampMillis,
                        pendingIntent
                    )
                }
            } catch (_: Exception) {
            }
        }

        fun cancelPrayerAlarm(context: Context, prayerName: String) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, PrayerNotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                prayerName.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }
}
