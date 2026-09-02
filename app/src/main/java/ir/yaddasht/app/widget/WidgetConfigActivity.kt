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

        // دریافت ID ویجت از لانچر
        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        // ساخت یک دکمه ساده برای تأیید
        val btn = Button(this).apply {
            text = "✅ تأیید و ساخت ویجت"
            textSize = 18f
            setOnClickListener {
                try {
                    // ۱. ذخیره یک تنظیمات تستی (برای اطمینان از کارکرد SharedPreferences)
                    getSharedPreferences("widget_prefs_test", MODE_PRIVATE).edit()
                        .putString("status", "configured").apply()

                    // ۲. ارسال سیگنال موفقیت به لانچر اندروید (حیاتی‌ترین خط کد)
                    val resultValue = Intent().apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    }
                    setResult(RESULT_OK, resultValue)
                    
                    Toast.makeText(this@WidgetConfigActivity, "در حال افزودن به صفحه...", Toast.LENGTH_SHORT).show()
                    
                    // ۳. بستن اکتیویتی
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this@WidgetConfigActivity, "خطا: ${e.message}", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
            }
        }

        setContentView(btn)
    }
}
