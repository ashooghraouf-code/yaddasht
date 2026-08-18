package ir.yaddasht.app.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.yaddasht.app.data.AppDatabase
import ir.yaddasht.app.data.Note
import ir.yaddasht.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WidgetConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }
        setResult(RESULT_CANCELED)

        setContent {
            YaddashtTheme {
                ConfigScreen(widgetId, onDone = { finish() })
            }
        }
    }
}

@Composable
private fun ConfigScreen(widgetId: Int, onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { AppDatabase.get(context).dao() }
    var notes by remember { mutableStateOf<List<Note>>(emptyList()) }
    var selectedNote by remember { mutableStateOf<Long>(-1L) }
    var selectedColor by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        notes = withContext(Dispatchers.IO) { dao.allNotesSync() }
        if (notes.isNotEmpty()) selectedNote = notes.first().id
    }

    Column(Modifier.fillMaxSize().background(DeepGreen).padding(16.dp)) {
        Text("ویجت جدید 🧩", fontFamily = LalezarFont, fontSize = 26.sp, color = PaperWhite)
        Spacer(Modifier.height(4.dp))
        Text("کدوم یادداشت روی صفحهٔ خانگی باشه؟", fontSize = 13.sp, color = MutedGreenText)
        Spacer(Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PaperColors.forEachIndexed { i, c ->
                Box(Modifier.size(34.dp).clip(CircleShape).background(c)
                    .border(if (selectedColor == i) 3.dp else 1.dp,
                        if (selectedColor == i) Saffron else Color.Black.copy(alpha = .2f), CircleShape)
                    .clickable { selectedColor = i })
            }
        }
        Spacer(Modifier.height(14.dp))

        LazyColumn(Modifier.weight(1f)) {
            items(notes) { n ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selectedNote == n.id) Saffron.copy(alpha = .25f) else DeepGreenSoft)
                    .border(1.dp, if (selectedNote == n.id) Saffron else LineGreen, RoundedCornerShape(14.dp))
                    .clickable { selectedNote = n.id }
                    .padding(12.dp)) {
                    Text(if (selectedNote == n.id) "✅ " else "🗒️ ", fontSize = 16.sp)
                    Text(n.title.ifBlank { "بدون عنوان" }, color = PaperWhite, fontSize = 14.sp)
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        Button(onClick = {
            scope.launch {
                withContext(Dispatchers.IO) {
                    WidgetPrefs.setNote(context, widgetId, selectedNote)
                    WidgetPrefs.setColor(context, widgetId, selectedColor)
                }
                NoteWidget.updateSingle(context, widgetId)
                val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                (context as android.app.Activity).setResult(android.app.Activity.RESULT_OK, result)
                onDone()
            }
        }, colors = ButtonDefaults.buttonColors(containerColor = Saffron, contentColor = Ink),
            modifier = Modifier.fillMaxWidth()) {
            Text("بچسبون به صفحه! 📌", fontFamily = LalezarFont, fontSize = 16.sp)
        }
    }
}