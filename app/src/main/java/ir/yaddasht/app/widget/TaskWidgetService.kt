package ir.yaddasht.app.widget

import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import ir.yaddasht.app.R
import ir.yaddasht.app.data.AppDatabase
import ir.yaddasht.app.data.Priority
import ir.yaddasht.app.data.Task
import ir.yaddasht.app.util.FaDate
import ir.yaddasht.app.util.fa
import kotlinx.coroutines.runBlocking
import java.util.Calendar

class TaskWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return TaskWidgetFactory(applicationContext)
    }
}

private class TaskWidgetFactory(
    private val context: android.content.Context
) : RemoteViewsService.RemoteViewsFactory {

    private var tasks: List<Task> = emptyList()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        runBlocking {
            tasks = try {
                AppDatabase.get(context.applicationContext).taskDao().getUpcomingTasksSync()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    override fun onDestroy() { tasks = emptyList() }
    override fun getCount(): Int = tasks.size
    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = if (position < tasks.size) tasks[position].id else position.toLong()
    override fun hasStableIds(): Boolean = true

    override fun getViewAt(position: Int): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.task_widget_item)
        if (position < 0 || position >= tasks.size) return rv
        val task = tasks[position]

        rv.setTextViewText(R.id.task_item_title, task.title.ifBlank { "بدون عنوان" })

        if (task.dueDate > 0) {
            val (jy, jm, jd) = FaDate.jalali(task.dueDate)
            val c = Calendar.getInstance().apply { timeInMillis = task.dueDate }
            val h = c.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
            val m = c.get(Calendar.MINUTE).toString().padStart(2, '0')
            rv.setTextViewText(R.id.task_item_due, "${jd.fa()} ${FaDate.monthName(jm)} - $h:$m".fa())
            rv.setViewVisibility(R.id.task_item_due, View.VISIBLE)
        } else {
            rv.setViewVisibility(R.id.task_item_due, View.GONE)
        }

        rv.setImageViewResource(
            R.id.task_item_check,
            if (task.isCompleted) android.R.drawable.checkbox_on_background
            else android.R.drawable.checkbox_off_background
        )

        val mark = when (task.priority) {
            Priority.HIGH -> "🔴"
            Priority.NORMAL -> "🟡"
            Priority.LOW -> "🟢"
        }
        rv.setTextViewText(R.id.task_item_priority, if (!task.isCompleted) mark else "✓")

        val fillIn = Intent().apply {
            putExtra(TaskWidget.EXTRA_TASK_ID, task.id)
        }
        rv.setOnClickFillInIntent(R.id.task_item_root, fillIn)
        return rv
    }
}
