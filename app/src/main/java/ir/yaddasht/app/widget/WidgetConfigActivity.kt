package ir.yaddasht.app.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import ir.yaddasht.app.R

class WidgetConfigActivity : Activity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID) 
            ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val btn = Button(this).apply {
            text = "✅ تأیید و ساخت ویجت"
            textSize = 18f
            setOnClickListener {
                try {
                    // ذخیره رنگ پیش‌فرض برای اطمینان از کارکرد
                    WidgetPreferences.setNoteColor(this@WidgetConfigActivity, 0xFFFFE082.toInt())
                    WidgetPreferences.setTaskColor(this@WidgetConfigActivity, 0xFF80DEEA.toInt())

                    val resultValue = Intent().apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    }
                    setResult(RESULT_OK, resultValue)
                    Toast.makeText(this@WidgetConfigActivity, "در حال افزودن...", Toast.LENGTH_SHORT).show()
                    
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ finish() }, 300)
                } catch (e: Exception) {
                    e.printStackTrace()
                    finish()
                }
            }
        }
        setContentView(btn)
    }
}
