package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.namaz.PrayerTimeCalculator

class FocusAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val calculator = PrayerTimeCalculator()
        val prayers = calculator.calculateAllWaktItems()
        val nextPrayer = prayers.find { it.isNext }

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_focus)

            if (nextPrayer != null) {
                views.setTextViewText(
                    R.id.widget_namaz_badge,
                    "🕌 ${nextPrayer.prayer.displayName} ${nextPrayer.startTimeFormatted}"
                )
            } else {
                views.setTextViewText(R.id.widget_namaz_badge, "🕌 Namaz Tracked")
            }

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            views.setOnClickPendingIntent(R.id.widget_action_button, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
