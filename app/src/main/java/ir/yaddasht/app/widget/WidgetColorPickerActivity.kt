package ir.yaddasht.app.widget

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import ir.yaddasht.app.R

class WidgetColorPickerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val btn = Button(this).apply {
            text = "بازگشت و آپدیت ویجت‌ها"
            setOnClickListener {
                NoteWidget.forceUpdate(this@WidgetColorPickerActivity)
                TaskWidget.forceUpdate(this@WidgetColorPickerActivity)
                Toast.makeText(this@WidgetColorPickerActivity, "ویجت‌ها به‌روز شدند", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
        setContentView(btn)
    }
}
