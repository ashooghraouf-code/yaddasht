package ir.yaddasht.app

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import ir.yaddasht.app.data.AppDatabase
import ir.yaddasht.app.ui.theme.YaddashtTheme
import ir.yaddasht.app.ui.screen.DrawScreen
import ir.yaddasht.app.ui.screen.EditorScreen
import ir.yaddasht.app.ui.screen.HomeScreen
import ir.yaddasht.app.widget.NoteWidget
import kotlin.math.sqrt

const val NEW_NOTE_ID = -1L

sealed class Screen {
    data object Home : Screen()
    data class Editor(val noteId: Long) : Screen()
    data class Draw(val noteId: Long) : Screen()
}

class MainActivity : ComponentActivity() {

    private var sensorManager: SensorManager? = null
    private var lastShake = 0L
    private var onShake: (() -> Unit)? = null

    private val shakeListener = object : SensorEventListener {
        override fun onSensorChanged(e: SensorEvent) {
            val x = e.values[0]; val y = e.values[1]; val z = e.values[2]
            val g = sqrt(x * x + y * y + z * z)
            if (g > 21 && System.currentTimeMillis() - lastShake > 3000) {
                lastShake = System.currentTimeMillis()
                (getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)?.let {
                    if (Build.VERSION.SDK_INT >= 26)
                        it.vibrate(VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE))
                }
                onShake?.invoke()
            }
        }
        override fun onAccuracyChanged(s: Sensor?, a: Int) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager

        setContent {
            YaddashtTheme {
                val dao = remember { AppDatabase.get(applicationContext).dao() }
                val openNoteId = remember { intent.getLongExtra("note_id", 0L) }
                var screen by remember {
                    mutableStateOf<Screen>(when {
                        openNoteId == NEW_NOTE_ID -> Screen.Editor(NEW_NOTE_ID)
                        openNoteId > 0 -> Screen.Editor(openNoteId)
                        else -> Screen.Home
                    })
                }

                LaunchedEffect(screen) {
                    if (screen is Screen.Home) NoteWidget.forceUpdate(this@MainActivity)
                }

                DisposableEffect(Unit) {
                    onShake = { screen = Screen.Editor(NEW_NOTE_ID) }
                    onDispose { onShake = null }
                }

                when (val s = screen) {
                    is Screen.Home -> HomeScreen(
                        dao = dao,
                        onOpenNote = { screen = Screen.Editor(it) },
                        onNewNote = { screen = Screen.Editor(NEW_NOTE_ID) }
                    )
                    is Screen.Editor -> EditorScreen(
                        dao = dao,
                        noteId = s.noteId,
                        onBack = { screen = Screen.Home },
                        onOpenDraw = { screen = Screen.Draw(it) }
                    )
                    is Screen.Draw -> DrawScreen(
                        dao = dao,
                        noteId = s.noteId,
                        onBack = { screen = Screen.Editor(s.noteId) }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sensorManager?.registerListener(shakeListener, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(shakeListener)
    }
}
