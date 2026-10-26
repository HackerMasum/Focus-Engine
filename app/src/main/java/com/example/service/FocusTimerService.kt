package com.example.service

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
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.R

class FocusTimerService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_UPDATE
        val remainingSec = intent?.getIntExtra(EXTRA_REMAINING_SECONDS, 0) ?: 0
        val isRunning = intent?.getBooleanExtra(EXTRA_IS_RUNNING, true) ?: true
        val isBreak = intent?.getBooleanExtra(EXTRA_IS_BREAK, false) ?: false
        val taskTitle = intent?.getStringExtra(EXTRA_TASK_TITLE) ?: ""

        when (action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                val notification = buildNotification(remainingSec, isRunning, isBreak, taskTitle)
                val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    0
                }
                ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, fgsType)
            }
        }

        return START_NOT_STICKY
    }

    private fun buildNotification(
        remainingSec: Int,
        isRunning: Boolean,
        isBreak: Boolean,
        taskTitle: String
    ): android.app.Notification {
        val min = remainingSec / 60
        val sec = remainingSec % 60
        val timeFormatted = String.format("%02d:%02d", min, sec)

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isBreak) "☕ Break Time ($timeFormatted)" else "🎯 Focus Flow ($timeFormatted)"
        val content = if (taskTitle.isNotBlank()) {
            "Working on: $taskTitle"
        } else {
            if (isRunning) "Deep concentration in progress..." else "Timer Paused"
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_focus_launcher)
            .setContentTitle(title)
            .setContentText(content)
            .setOngoing(isRunning)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // Action: Stop
        val stopIntent = Intent(this, FocusTimerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "FocusFlow Active Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live remaining focus time and session controls"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "focus_timer_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_UPDATE = "com.example.service.ACTION_UPDATE"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"

        const val EXTRA_REMAINING_SECONDS = "remaining_seconds"
        const val EXTRA_IS_RUNNING = "is_running"
        const val EXTRA_IS_BREAK = "is_break"
        const val EXTRA_TASK_TITLE = "task_title"

        fun updateService(
            context: Context,
            remainingSeconds: Int,
            isRunning: Boolean,
            isBreak: Boolean,
            taskTitle: String?
        ) {
            val intent = Intent(context, FocusTimerService::class.java).apply {
                action = ACTION_UPDATE
                putExtra(EXTRA_REMAINING_SECONDS, remainingSeconds)
                putExtra(EXTRA_IS_RUNNING, isRunning)
                putExtra(EXTRA_IS_BREAK, isBreak)
                putExtra(EXTRA_TASK_TITLE, taskTitle ?: "")
            }
            try {
                if (isRunning) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, FocusTimerService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {
            }
        }
    }
}
