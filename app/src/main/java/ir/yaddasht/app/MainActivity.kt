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
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import ir.yaddasht.app.ui.theme.*
import ir.yaddasht.app.util.NoteLock
import ir.yaddasht.app.widget.NoteWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

const val NEW_NOTE_ID = -1L

sealed class Screen {
    data object Home : Screen()
    data class Editor(val noteId: Long) : Screen()
    data class Draw(val noteId: Long) : Screen()
}

class MainActivity : FragmentActivity() {

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

    private fun showBiometric(onSuccess: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }
        })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("قفل چراغ راه 🔒")
            .setSubtitle("با اثر انگشت یا رمز دستگاه باز کن")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
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
                var authRequired by remember { mutableStateOf(false) }
                var authChecked by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    val hasLocked = withContext(Dispatchers.IO) {
                        dao.allNotesSync().any { NoteLock.isLocked(it.body) }
                    }
                    val canBio = BiometricManager.from(this@MainActivity).canAuthenticate(
                        BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
                    ) == BiometricManager.BIOMETRIC_SUCCESS
                    if (hasLocked && canBio) authRequired = true
                    authChecked = true
                }

                LaunchedEffect(authRequired) {
                    if (authRequired) showBiometric { authRequired = false }
                }

                if (authRequired) {
                    LockScreen { showBiometric { authRequired = false } }
                } else if (authChecked) {
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
            Button(onClick = onUnlock,
                colors = ButtonDefaults.buttonColors(containerColor = Saffron, contentColor = Ink)) {
                Text("باز کردن 🔓", fontFamily = LalezarFont, fontSize = 16.sp)
            }
        }
    }
}
