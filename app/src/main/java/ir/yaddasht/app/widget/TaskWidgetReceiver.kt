package ir.yaddasht.app.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ir.yaddasht.app.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskWidgetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "TOGGLE_TASK") {
            val taskId = intent.getLongExtra("task_id", -1L)
            
            if (taskId > 0) {
                CoroutineScope(Dispatchers.IO).launch {
                    // ✅ فقط یک پارامتر (مطابق TaskDao شما)
                    AppDatabase.get(context).taskDao().markCompleted(taskId)
                    TaskWidget.forceUpdate(context)
                }
            }
        }
    }
}
