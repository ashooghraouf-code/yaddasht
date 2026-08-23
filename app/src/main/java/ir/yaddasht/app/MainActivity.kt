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
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import ir.yaddasht.app.data.AppDatabase
import ir.yaddasht.app.ui.screen.DrawScreen
import ir.yaddasht.app.ui.screen.EditorScreen
import ir.yaddasht.app.ui.screen.HomeScreen
import ir.yaddasht.app.ui.screen.TaskEditorScreen
import ir.yaddasht.app.ui.theme.DeepGreen
import ir.yaddasht.app.ui.theme.LalezarFont
import ir.yaddasht.app.ui.theme.MutedGreenText
import ir.yaddasht.app.ui.theme.PaperWhite
import ir.yaddasht.app.ui.theme.Saffron
import ir.yaddasht.app.ui.theme.YaddashtTheme
import ir.yaddasht.app.util.NoteLock
import ir.yaddasht.app.widget.NoteWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

const val NEW_NOTE_ID = -1L

sealed class Screen {
    data object Home : Screen()
    data class Editor(val noteId: Long) : Screen()
    data class Draw(val noteId: Long, val isTask: Boolean = false) : Screen()
    data class TaskEditor(val taskId: Long) : Screen()
    companion object {
        val SAVER: Saver<Screen, String> = Saver(
            save = { s ->
                when (s) {
                    is Home -> "home"
                    is Editor -> "e:${s.noteId}"
                    is Draw -> "d:${s.noteId}:${if (s.isTask) 1 else 0}"
                    is TaskEditor -> "t:${s.taskId}"
                }
            },
            restore = { str ->
                when {
                    str.startsWith("e:") -> Editor(str.removePrefix("e:").toLongOrNull() ?: NEW_NOTE_ID)
                    str.startsWith("d:") -> {
                        val parts = str.removePrefix("d:").split(":")
                        Draw(parts.getOrNull(0)?.toLongOrNull() ?: NEW_NOTE_ID, parts.getOrNull(1) == "1")
                    }
                    str.startsWith("t:") -> TaskEditor(str.removePrefix("t:").toLongOrNull() ?: 0L)
                    else -> Home
                }
            }
        )
    }
}

class MainActivity : FragmentActivity() {
    private var sensorManager: SensorManager? = null
    private var shakeHits = 0
    private var lastHitTime = 0L
    private var lastTrigger = 0L
    private var onShake: (() -> Unit)? = null

    private val shakeListener = object : SensorEventListener {
        override fun onSensorChanged(e: SensorEvent) {
            val x = e.values[0]; val y = e.values[1]; val z = e.values[2]
            val g = sqrt(x * x + y * y + z * z)
            val now = System.currentTimeMillis()
            if (g > 22) {
                if (now - lastHitTime > 700) shakeHits = 0
                lastHitTime = now
                shakeHits++
                if (shakeHits >= 4 && now - lastTrigger > 3000) {
                    lastTrigger = now; shakeHits = 0
                    (getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)?.let {
                        if (Build.VERSION.SDK_INT >= 26) it.vibrate(VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE))
                    }
                    onShake?.invoke()
                }
            }
        }
        override fun onAccuracyChanged(s: Sensor?, a: Int) {}
    }

    private fun showBiometric(onSuccess: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { super.onAuthenticationSucceeded(result); onSuccess() }
        })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("قفل چراغ راه 🔒").setSubtitle("با اثر انگشت یا رمز دستگاه باز کن")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()
        prompt.authenticate(info)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        setContent {
            YaddashtTheme {
                val dao = remember { AppDatabase.get(applicationContext).dao() }
                val taskDao = remember { AppDatabase.get(applicationContext).taskDao() }
                var authRequired by remember { mutableStateOf(false) }
                var authChecked by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    val hasLocked = withContext(Dispatchers.IO) { dao.allNotesSync().any { NoteLock.isLocked(it.body) } }
                    val canBio = BiometricManager.from(this@MainActivity).canAuthenticate(
                        BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
                    if (hasLocked && canBio) authRequired = true
                    authChecked = true
                }
                LaunchedEffect(authRequired) { if (authRequired) showBiometric { authRequired = false } }
                if (authRequired) {
                    LockScreen { showBiometric { authRequired = false } }
                } else if (authChecked) {
                    val openNoteId = remember { intent.getLongExtra("note_id", 0L) }
                    val isTaskExtra = remember { intent.getBooleanExtra("is_task", false) }
                    var screen by rememberSaveable(stateSaver = Screen.SAVER) {
                        mutableStateOf<Screen>(when {
                            isTaskExtra && openNoteId > 0 -> Screen.TaskEditor(openNoteId)
                            openNoteId == NEW_NOTE_ID -> Screen.Editor(NEW_NOTE_ID)
                            openNoteId > 0 -> Screen.Editor(openNoteId)
                            else -> Screen.Home
                        })
                    }
                    LaunchedEffect(screen) { if (screen is Screen.Home) NoteWidget.forceUpdate(this@MainActivity) }
                    DisposableEffect(Unit) {
                        onShake = { screen = Screen.Editor(NEW_NOTE_ID) }
                        onDispose { onShake = null }
                    }
                    when (val s = screen) {
                        is Screen.Home -> HomeScreen(dao = dao, taskDao = taskDao,
                            onOpenNote = { screen = Screen.Editor(it) },
                            onNewNote = { screen = Screen.Editor(NEW_NOTE_ID) },
                            onOpenTask = { screen = Screen.TaskEditor(it) })
                        is Screen.Editor -> EditorScreen(dao = dao, noteId = s.noteId,
                            onBack = { screen = Screen.Home }, onOpenDraw = { screen = Screen.Draw(it, false) })
                        is Screen.Draw -> DrawScreen(dao = dao, noteId = s.noteId, isTask = s.isTask, taskDao = taskDao,
                            onBack = { screen = if (s.isTask) Screen.TaskEditor(s.noteId) else Screen.Editor(s.noteId) })
                        is Screen.TaskEditor -> TaskEditorScreen(taskDao = taskDao, taskId = s.taskId,
                            onBack = { screen = Screen.Home }, onOpenDraw = { screen = Screen.Draw(it, true) })
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { sensorManager?.registerListener(shakeListener, it, SensorManager.SENSOR_DELAY_UI) }
    }
    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(shakeListener)
    }
}

@Composable
private fun LockScreen(onUnlock: () -> Unit) {
    Box(Modifier.fillMaxSize().background(DeepGreen), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🏮", fontSize = 60.sp)
            Spacer(Modifier.height(12.dp))
            Text("چراغ راه قفل است", fontFamily = LalezarFont, fontSize = 26.sp, color = PaperWhite)
            Spacer(Modifier.height(6.dp))
            Text("یادداشت محرمانه داری؛ اول خودت را ثابت کن!", fontSize = 12.sp, color = MutedGreenText)
            Spacer(Modifier.height(20.dp))
            Button(onClick = onUnlock, colors = ButtonDefaults.buttonColors(containerColor = Saffron, contentColor = Ink)) {
                Text("باز کردن 🔓", fontFamily = LalezarFont, fontSize = 16.sp)
            }
        }
    }
}
