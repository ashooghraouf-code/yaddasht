package ir.yaddasht.app.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import ir.yaddasht.app.R

class WidgetConfigActivity : Activity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var selectedColor: Int = 0
    private var widgetType: WidgetType = WidgetType.NOTE

    enum class WidgetType { NOTE, TASK, JOURNALIST }

    private val notePalette = intArrayOf(
        0xFFFFE082.toInt(), 0xFFFFB74D.toInt(), 0xFFFF8A65.toInt(), 0xFFF06292.toInt(),
        0xFFBA68C8.toInt(), 0xFF9575CD.toInt(), 0xFF81C784.toInt(), 0xFFAED581.toInt()
    )
    private val taskPalette = intArrayOf(
        0xFF80DEEA.toInt(), 0xFF4DD0E1.toInt(), 0xFF4FC3F7.toInt(), 0xFF64B5F6.toInt(),
        0xFF7986CB.toInt(), 0xFF9575CD.toInt(), 0xFFA1887F.toInt(), 0xFFE0E0E0.toInt()
    )
    private val journalistPalette = intArrayOf(
        0xFFFF5252.toInt(), 0xFFFF1744.toInt(), 0xFFFF6D00.toInt(), 0xFFFFAB00.toInt(),
        0xFFD50000.toInt(), 0xFFFF3D00.toInt(), 0xFFBF360C.toInt(), 0xFF212121.toInt()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        try { setContentView(R.layout.activity_widget_config) } catch (e: Exception) { finish(); return }

        appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }

        val providerName = AppWidgetManager.getInstance(this).getAppWidgetInfo(appWidgetId)?.provider?.className ?: ""
        widgetType = when {
            providerName.contains("NoteWidget", ignoreCase = true) -> WidgetType.NOTE
            providerName.contains("TaskWidget", ignoreCase = true) -> WidgetType.TASK
            providerName.contains("JournalistWidget", ignoreCase = true) -> WidgetType.JOURNALIST
            else -> WidgetType.NOTE
        }

        selectedColor = when (widgetType) {
            WidgetType.NOTE -> WidgetPreferences.getNoteColor(this)
            WidgetType.TASK -> WidgetPreferences.getTaskColor(this)
            WidgetType.JOURNALIST -> WidgetPreferences.getJournalistColor(this)
        }

        val noteGrid = findViewById<GridLayout>(R.id.config_note_grid)
        val taskGrid = findViewById<GridLayout>(R.id.config_task_grid)
        val confirmBtn = findViewById<Button>(R.id.config_confirm_btn)

        when (widgetType) {
            WidgetType.NOTE -> {
                noteGrid.visibility = View.VISIBLE
                taskGrid.visibility = View.GONE
                buildGrid(noteGrid, notePalette)
            }
            WidgetType.TASK -> {
                noteGrid.visibility = View.GONE
                taskGrid.visibility = View.VISIBLE
                buildGrid(taskGrid, taskPalette)
            }
            WidgetType.JOURNALIST -> {
                noteGrid.visibility = View.GONE
                taskGrid.visibility = View.VISIBLE
                buildGrid(taskGrid, journalistPalette)
            }
        }

        confirmBtn.setOnClickListener {
            when (widgetType) {
                WidgetType.NOTE -> WidgetPreferences.setNoteColor(this, selectedColor)
                WidgetType.TASK -> WidgetPreferences.setTaskColor(this, selectedColor)
                WidgetType.JOURNALIST -> WidgetPreferences.setJournalistColor(this, selectedColor)
            }

            val resultValue = Intent().apply { putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId) }
            setResult(RESULT_OK, resultValue)

            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ finish() }, 200)
        }
    }

    private fun buildGrid(grid: GridLayout, colors: IntArray) {
        grid.removeAllViews()
        colors.forEach { color ->
            val size = (56 * resources.displayMetrics.density).toInt()
            val margin = (4 * resources.displayMetrics.density).toInt()
            val view = View(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = size; height = size; setMargins(margin, margin, margin, margin)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setColor(color)
                    cornerRadius = 14 * resources.displayMetrics.density
                    if (color == selectedColor) setStroke((3 * resources.displayMetrics.density).toInt(), 0xFFFFFFFF.toInt())
                }
                setOnClickListener {
                    selectedColor = color
                    buildGrid(grid, colors)
                }
            }
            grid.addView(view)
        }
    }
}
