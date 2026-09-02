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
            val isCompleted = intent.getBooleanExtra("is_completed", false)
            
            if (taskId > 0) {
                CoroutineScope(Dispatchers.IO).launch {
                    AppDatabase.get(context).taskDao().markCompleted(taskId, isCompleted)
                    TaskWidget.forceUpdate(context)
                }
            }
        }
    }
}
