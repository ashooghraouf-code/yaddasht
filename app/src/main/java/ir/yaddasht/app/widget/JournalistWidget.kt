package ir.yaddasht.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import ir.yaddasht.app.R

class JournalistWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.journalist_widget_layout)

            val bgColor = WidgetPreferences.getJournalistColor(context)
            try { views.setInt(R.id.journalist_widget_root, "setBackgroundColor", bgColor) } catch (_: Exception) {}

            // ✅ مستقیم به JournalistActionActivity - بدون باز کردن اپ اصلی
            val cameraIntent = Intent(context, JournalistActionActivity::class.java).apply {
                action = JournalistActionActivity.ACTION_CAMERA
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val cameraPending = PendingIntent.getActivity(
                context, 0, cameraIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.journalist_camera_btn, cameraPending)

            val micIntent = Intent(context, JournalistActionActivity::class.java).apply {
                action = JournalistActionActivity.ACTION_MIC
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val micPending = PendingIntent.getActivity(
                context, 1, micIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.journalist_mic_btn, micPending)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun forceUpdate(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, JournalistWidget::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (appWidgetId in allWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }
}
