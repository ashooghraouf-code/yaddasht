package ir.yaddasht.app.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.Toast
import androidx.core.content.edit
import ir.yaddasht.app.R

class WidgetConfigActivity : Activity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var selectedNoteColor: Int = 0xFFFFE082.toInt()

    private val notePalette = intArrayOf(
        0xFFFFE082.toInt(), 0xFFFFB74D.toInt(), 0xFFFF8A65.toInt(), 0xFFF06292.toInt(),
        0xFFBA68C8.toInt(), 0xFF9575CD.toInt(), 0xFF81C784.toInt(), 0xFFAED581.toInt()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        
        try {
            setContentView(R.layout.activity_widget_config)
        } catch (e: Exception) {
            Toast.makeText(this, "❌ خطا در layout: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID, 
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Toast.makeText(this, "❌ Widget ID نامعتبر است", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val noteGrid = findViewById<GridLayout>(R.id.config_note_grid)
        val confirmBtn = findViewById<Button>(R.id.config_confirm_btn)

        // مخفی کردن گرید وظایف برای تست ساده‌تر
        findViewById<GridLayout>(R.id.config_task_grid)?.visibility = View.GONE

        buildGrid(noteGrid, notePalette)

        confirmBtn.setOnClickListener {
            try {
                // ۱. ذخیره رنگ
                val prefs = getSharedPreferences("widget_prefs_v2", Context.MODE_PRIVATE)
                prefs.edit { putInt("note_bg_color", selectedNoteColor) }
                Toast.makeText(this, "✓ رنگ ذخیره شد", Toast.LENGTH_SHORT).show()

                // ۲. آپدیت ویجت
                val appWidgetManager = AppWidgetManager.getInstance(this)
                Toast.makeText(this, "🔄 در حال ساخت ویجت...", Toast.LENGTH_SHORT).show()
                
                NoteWidget.updateAppWidget(this, appWidgetManager, appWidgetId)
                Toast.makeText(this, "✓ ویجت آپدیت شد", Toast.LENGTH_SHORT).show()

                // ۳. بازگرداندن نتیجه به سیستم عامل اندروید
                val resultValue = Intent().apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                setResult(RESULT_OK, resultValue)
                Toast.makeText(this, "✅ ویجت ساخته شد! به صفحه اصلی بروید", Toast.LENGTH_LONG).show()
                
                finish()
            } catch (e: Exception) {
                Toast.makeText(this, "❌ خطا: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }

    private fun buildGrid(grid: GridLayout, colors: IntArray) {
        grid.removeAllViews()
        colors.forEachIndexed { i, color ->
            val size = (56 * resources.displayMetrics.density).toInt()
            val margin = (4 * resources.displayMetrics.density).toInt()

            val view = View(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = size; height = size
                    setMargins(margin, margin, margin, margin)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setColor(color)
                    cornerRadius = 14 * resources.displayMetrics.density
                    if (color == selectedNoteColor) {
                        setStroke((3 * resources.displayMetrics.density).toInt(), 0xFFFFFFFF.toInt())
                    }
                }
                setOnClickListener {
                    selectedNoteColor = color
                    buildGrid(grid, colors)
                }
            }
            grid.addView(view)
        }
    }
}

object WidgetPreferences {
    private const val PREFS = "widget_prefs_v2"
    private const val KEY_NOTE_BG = "note_bg_color"
    private const val KEY_TASK_BG = "task_bg_color"

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getNoteColor(c: Context): Int = prefs(c).getInt(KEY_NOTE_BG, 0xFFFFE082.toInt())
    fun setNoteColor(c: Context, color: Int) = prefs(c).edit { putInt(KEY_NOTE_BG, color) }

    fun getTaskColor(c: Context): Int = prefs(c).getInt(KEY_TASK_BG, 0xFF80DEEA.toInt())
    fun setTaskColor(c: Context, color: Int) = prefs(c).edit { putInt(KEY_TASK_BG, color) }
}
