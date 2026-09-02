package ir.yaddasht.app.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
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

    private val notePalette = intArrayOf(0xFFFFE082.toInt(), 0xFFFFB74D.toInt(), 0xFFFF8A65.toInt(), 0xFFF06292.toInt(), 0xFFBA68C8.toInt(), 0xFF9575CD.toInt(), 0xFF81C784.toInt(), 0xFFAED581.toInt())
    private val taskPalette = intArrayOf(0xFF80DEEA.toInt(), 0xFF4DD0E1.toInt(), 0xFF4FC3F7.toInt(), 0xFF64B5F6.toInt(), 0xFF7986CB.toInt(), 0xFF9575CD.toInt(), 0xFFA1887F.toInt(), 0xFFE0E0E0.toInt())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        try {
            setContentView(R.layout.activity_widget_config)
        } catch (e: Exception) {
            finish()
            return
        }

        appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }

        val noteGrid = findViewById<GridLayout>(R.id.config_note_grid)
        val taskGrid = findViewById<GridLayout>(R.id.config_task_grid)
        val confirmBtn = findViewById<Button>(R.id.config_confirm_btn)

        buildGrid(noteGrid, notePalette, isNote = true)
        buildGrid(taskGrid, taskPalette, isNote = false)

        confirmBtn.setOnClickListener {
            try {
                WidgetPreferences.setNoteColor(this, selectedNoteColor)
                WidgetPreferences.setTaskColor(this, selectedTaskColor)

                val resultValue = Intent().apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                setResult(RESULT_OK, resultValue)
                
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ finish() }, 300)
            } catch (e: Exception) {
                e.printStackTrace()
                finish()
            }
        }
    }

    private fun buildGrid(grid: GridLayout, colors: IntArray, isNote: Boolean) {
        val currentColor = if (isNote) selectedNoteColor else selectedTaskColor
        grid.removeAllViews()
        colors.forEachIndexed { _, color ->
            val size = (56 * resources.displayMetrics.density).toInt()
            val margin = (4 * resources.displayMetrics.density).toInt()
            val view = View(this).apply {
                layoutParams = GridLayout.LayoutParams().apply { width = size; height = size; setMargins(margin, margin, margin, margin) }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE; setColor(color); cornerRadius = 14 * resources.displayMetrics.density
                    if (color == currentColor) setStroke((3 * resources.displayMetrics.density).toInt(), 0xFFFFFFFF.toInt())
                }
                setOnClickListener { 
                    if (isNote) selectedNoteColor = color else selectedTaskColor = color
                    buildGrid(grid, colors, isNote) 
                }
            }
            grid.addView(view)
        }
    }
}
