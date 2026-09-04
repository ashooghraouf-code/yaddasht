آ
    private val shakeListener = object : SensorEventListener {
        override fun onSensorChanged(e: SensorEvent) {
            val x = e.values[0]; val y = e.values[1]; val z = e.values[2]
            val g = kotlin.math.sqrt(x * x + y * y + z * z)
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager

        setContent {
            // ✅ state برای trigger recomposition هنگام تغییر رنگ
            var themeVersion by remember { mutableIntStateOf(0) }

            YaddashtTheme {
                val context = LocalContext.current
                val dao = remember { AppDatabase.get(context.applicationContext).dao() }
                val taskDao = remember { AppDatabase.get(context.applicationContext).taskDao() }
                var authRequired by remember { mutableStateOf(false) }
                var authChecked by remember { mutableStateOf(false) }
                var authPassed by remember { mutableStateOf(false) }

                val keyguardLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                    if (result.resultCode == Activity.RESULT_OK) { authPassed = true; authRequired = false }
                }

                LaunchedEffect(Unit) {
                    val hasLocked = withContext(Dispatchers.IO) { dao.allNotesSync().any { NoteLock.isLocked(it.body) } }
                    if (hasLocked) {
                        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                        if (keyguardManager.isDeviceSecure) authRequired = true
                    }
                    authChecked = true
                }

                LaunchedEffect(authRequired) {
                    if (authRequired && !authPassed) {
                        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                        val intent = keyguardManager.createConfirmDeviceCredentialIntent("قفل چراغ راه 🔒", "با اثر انگشت یا رمز باز کن")
                        if (intent != null) keyguardLauncher.launch(intent) else { authRequired = false; authPassed = true }
                    }
                }

                if (authRequired && !authPassed) {
                    LockScreen {
                        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                        val intent = keyguardManager.createConfirmDeviceCredentialIntent("قفل چراغ راه 🔒", "با اثر انگشت یا رمز باز کن")
                        if (intent != null) keyguardLauncher.launch(intent)
                    }
                } else if (authChecked) {
                    if (intent.getBooleanExtra("open_widget_settings", false)) {
                        startActivity(Intent(this@MainActivity, WidgetColorPickerActivity::class.java))
                        intent.removeExtra("open_widget_settings")
                    }

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
                        is Screen.Home -> HomeScreen(
                            dao = dao, taskDao = taskDao,
                            onOpenNote = { screen = Screen.Editor(it) },
                            onNewNote = { screen = Screen.Editor(NEW_NOTE_ID) },
                            onOpenTask = { screen = Screen.TaskEditor(it) },
                            onThemeChanged = { themeVersion++ }
                        )
                        is Screen.Editor -> EditorScreen(dao = dao, noteId = s.noteId, onBack = { screen = Screen.Home }, onOpenDraw = { screen = Screen.Draw(it, false) })
                        is Screen.Draw -> DrawScreen(dao = dao, noteId = s.noteId, isTask = s.isTask, taskDao = taskDao, onBack = { screen = if (s.isTask) Screen.TaskEditor(s.noteId) else Screen.Editor(s.noteId) })
                        is Screen.TaskEditor -> TaskEditorScreen(taskDao = taskDao, taskId = s.taskId, onBack = { screen = Screen.Home }, onOpenDraw = { screen = Screen.Draw(it, true) })
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
        NoteWidget.forceUpdate(this)
        TaskWidget.forceUpdate(this)
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
            Button(onClick = onUnlock, colors = ButtonDefaults.buttonColors(containerColor = Saffron, contentColor = DeepGreen)) {
                Text("باز کردن 🔓", fontFamily = LalezarFont, fontSize = 16.sp)
            }
        }
    }
}
