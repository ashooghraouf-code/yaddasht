package ir.yaddasht.app.widget

import android.app.Activity
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.Toast
import ir.yaddasht.app.R

class WidgetColorPickerActivity : Activity() {

    private val notePalette = intArrayOf(
        0xFFFFE082.toInt(), 0xFFFFB74D.toInt(), 0xFFFF8A65.toInt(), 0xFFF06292.toInt(),
        0xFFBA68C8.toInt(), 0xFF9575CD.toInt(), 0xFF81C784.toInt(), 0xFFAED581.toInt()
    )
    private val taskPalette = intArrayOf(
        0xFF80DEEA.toInt(), 0xFF4DD0E1.toInt(), 0xFF4FC3F7.toInt(), 0xFF64B5F6.toInt(),
        0xFF7986CB.toInt(), 0xFF9575CD.toInt(), 0xFFA1887F.toInt(), 0xFFE0E0E0.toInt()
    )
    private val noteNames = arrayOf("طلایی", "نارنجی", "مرجانی", "صورتی", "بنفش", "یاسی", "سبز", "لیمویی")
    private val taskNames = arrayOf("فیروزه‌ای", "آبی روشن", "آسمانی", "آبی", "نیلی", "ارغوانی", "قهوه‌ای", "نقره‌ای")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_widget_color_picker)

        val noteGrid = findViewById<GridLayout>(R.id.note_colors_grid)
        val taskGrid = findViewById<GridLayout>(R.id.task_colors_grid)
        val btnDone = findViewById<Button>(R.id.btn_done)

        buildGrid(noteGrid, notePalette, noteNames, isNote = true)
        buildGrid(taskGrid, taskPalette, taskNames, isNote = false)

        btnDone.setOnClickListener {
            NoteWidget.forceUpdate(this)
            TaskWidget.forceUpdate(this)
            Toast.makeText(this, "✅ ویجت‌ها به‌روز شدن", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun buildGrid(grid: GridLayout, colors: IntArray, names: Array<String>, isNote: Boolean) {
        val currentColor = if (isNote) WidgetPreferences.getNoteColor(this) else WidgetPreferences.getTaskColor(this)
        grid.removeAllViews()
        colors.forEachIndexed { i, color ->
            val size = (60 * resources.displayMetrics.density).toInt()
            val margin = (6 * resources.displayMetrics.density).toInt()
            val view = View(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = size; height = size
                    setMargins(margin, margin, margin, margin)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setColor(color)
                    cornerRadius = 16 * resources.displayMetrics.density
                    if (color == currentColor) setStroke((4 * resources.displayMetrics.density).toInt(), 0xFFFFFFFF.toInt())
                }
                contentDescription = names[i]
                setOnClickListener {
                    if (isNote) WidgetPreferences.setNoteColor(this@WidgetColorPickerActivity, color)
                    else WidgetPreferences.setTaskColor(this@WidgetColorPickerActivity, color)
                    buildGrid(grid, colors, names, isNote)
                    Toast.makeText(this@WidgetColorPickerActivity, "✓ ${names[i]} انتخاب شد", Toast.LENGTH_SHORT).show()
                }
            }
            grid.addView(view)
        }
    }
}
