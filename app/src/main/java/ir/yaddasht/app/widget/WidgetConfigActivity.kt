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
    private var selectedTaskColor: Int = 0xFF80DEEA.toInt()

    private val notePalette = intArrayOf(
        0xFFFFE082.toInt(), 0xFFFFB74D.toInt(), 0xFFFF8A65.toInt(), 0xFFF06292.toInt(),
        0xFFBA68C8.toInt(), 0xFF9575CD.toInt(), 0xFF81C784.toInt(), 0xFFAED581.toInt()
    )
    private val taskPalette = intArrayOf(
        0xFF80DEEA.toInt(), 0xFF4DD0E1.toInt(), 0xFF4FC3F7.toInt(), 0xFF64B5F6.toInt(),
        0xFF7986CB.toInt(), 0xFF9575CD.toInt(), 0xFFA1887F.toInt(), 0xFFE0E0E0.toInt()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        
        try {
            setContentView(R.layout.activity_widget_config)
            Toast.makeText(this, "✅ صفحه تنظیمات باز شد", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "❌ خطا در باز کردن layout: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // گرفتن widgetId
        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID, 
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        Toast.makeText(this, "Widget ID: $appWidgetId", Toast.LENGTH_SHORT).show()

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Toast.makeText(this, "❌ Widget ID نامعتبر است", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        try {
            val noteGrid = findViewById<GridLayout>(R.id.config_note_grid)
            val taskGrid = findViewById<GridLayout>(R.id.config_task_grid)
            val confirmBtn = findViewById<Button>(R.id.config_confirm_btn)

            buildGrid(noteGrid, notePalette, isNote = true)
            buildGrid(taskGrid, taskPalette, isNote = false)

            confirmBtn.setOnClickListener {
                try {
                    saveAndFinish()
                } catch (e: Exception) {
                    Toast.makeText(this, "❌ خطا در ذخیره: ${e.message}", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "❌ خطا در ساخت گرید: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun buildGrid(grid: GridLayout, colors: IntArray, isNote: Boolean) {
        try {
            val currentColor = if (isNote) selectedNoteColor else selectedTaskColor
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
                        if (color == currentColor) {
                            setStroke((3 * resources.displayMetrics.density).toInt(), 0xFFFFFFFF.toInt())
                        }
                    }
                    setOnClickListener {
                        if (isNote) selectedNoteColor = color else selectedTaskColor = color
                        buildGrid(grid, colors, isNote)
                    }
                }
                grid.addView(view)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "خطا در ساخت گرید: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveAndFinish() {
        Toast.makeText(this, "💾 در حال ذخیره رنگ‌ها...", Toast.LENGTH_SHORT).show()
        
        try {
            // ذخیره رنگ‌ها
            WidgetPreferences.setNoteColor(this, selectedNoteColor)
            WidgetPreferences.setTaskColor(this, selectedTaskColor)
            Toast.makeText(this, "✓ رنگ‌ها ذخیره شد", Toast.LENGTH_SHORT).show()

            // آپدیت ویجت
            val appWidgetManager = AppWidgetManager.getInstance(this)
            Toast.makeText(this, "🔄 در حال آپدیت ویجت...", Toast.LENGTH_SHORT).show()
            
            NoteWidget.updateAppWidget(this, appWidgetManager, appWidgetId)
            TaskWidget.updateAppWidget(this, appWidgetManager, appWidgetId)
            Toast.makeText(this, "✓ ویجت آپدیت شد", Toast.LENGTH_SHORT).show()

            // برگرداندن نتیجه
            val resultValue = Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            setResult(RESULT_OK, resultValue)
            Toast.makeText(this, "✅ ویجت ساخته شد!", Toast.LENGTH_SHORT).show()
            
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, " خطا: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
}

// ✅ WidgetPreferences object
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
