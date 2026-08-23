package ir.yaddasht.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ir.yaddasht.app.ai.AiAssistant
import ir.yaddasht.app.ai.AiConfig
import ir.yaddasht.app.ai.AiProvider
import ir.yaddasht.app.ui.theme.Brick
import ir.yaddasht.app.ui.theme.DeepGreenSoft
import ir.yaddasht.app.ui.theme.InkSoft
import ir.yaddasht.app.ui.theme.LalezarFont
import ir.yaddasht.app.ui.theme.LineGreen
import ir.yaddasht.app.ui.theme.PaperWhite
import ir.yaddasht.app.ui.theme.Saffron
import ir.yaddasht.app.ui.theme.VazirFont

private val ReadableInk = Color(0xFF1C2A22)
private val AnswerBg = Color(0xFFFCF6E8)
private val QuestionBg = Color(0xFFFFE9B8)

private enum class AiMode(val label: String) { ANALYZE("📊 تحلیل"), REPORT("📋 گزارش"), CHAT("💬 پرسش‌وپاسخ") }

@Composable
fun AiAnalysisDialog(title: String, content: String, isLocked: Boolean, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var showGateway by remember { mutableStateOf(!AiConfig.isConfigured(context)) }
    var consented by remember { mutableStateOf(false) }

    when {
        showGateway -> AiGatewayDialog(
            onDismiss = { if (AiConfig.isConfigured(context)) showGateway = false else onDismiss() },
            onSaved = { showGateway = false })
        isLocked && !consented -> ConsentDialog(AiConfig.provider(context).displayName, onAllow = { consented = true }, onDeny = onDismiss)
        else -> AiMainDialog(title, content, onOpenGateway = { showGateway = true }, onDismiss = onDismiss)
    }
}

@Composable
private fun ConsentDialog(providerName: String, onAllow: () -> Unit, onDeny: () -> Unit) {
    AlertDialog(onDismissRequest = onDeny,
        title = { Text("🔒 یادداشت محرمانه", fontFamily = LalezarFont, fontSize = 20.sp, color = ReadableInk) },
        text = {
            Column {
                Text("این یادداشت قفل است. متن آن تنها پس از اجازهٔ شما برای پردازش به سرویس «$providerName» ارسال می‌شود.",
                    fontSize = 14.sp, color = ReadableInk, lineHeight = 22.sp)
                Spacer(Modifier.height(8.dp))
                Text("⚠️ این پرسش هر بار نمایش داده می‌شود و هیچ اجازه‌ای ذخیره نمی‌گردد.",
                    fontSize = 12.sp, color = Brick, fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = { TextButton(onClick = onAllow) { Text("✅ اجازه می‌دهم", color = Saffron, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDeny) { Text("انصراف", color = ReadableInk) } })
}

@Composable
private fun AiMainDialog(title: String, content: String, onOpenGateway: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var mode by remember { mutableStateOf(AiMode.ANALYZE) }
    var result by remember { mutableStateOf<AiAssistant.AnalysisResult?>(null) }
    var loading by remember { mutableStateOf(true) }
    var history by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    var question by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(mode) {
        if (mode != AiMode.CHAT) {
            loading = true
            result = if (mode == AiMode.ANALYZE) AiAssistant.analyzeNote(context, title, content)
            else AiAssistant.reportNote(context, title, content)
            loading = false
        }
    }
    LaunchedEffect(pending) {
        val q = pending ?: return@LaunchedEffect
        val r = AiAssistant.askAboutNote(context, title, content, history, q)
        history = history + (q to (if (r.success) r.content else "❌ ${r.error}"))
        pending = null
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = PaperWhite, modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp)) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🤖", fontSize = 22.sp)
                    Spacer(Modifier.width(6.dp))
                    Text("دستیار هوشمند", fontFamily = LalezarFont, fontSize = 18.sp, color = ReadableInk, modifier = Modifier.weight(1f))
                    IconButton(onClick = onOpenGateway, modifier = Modifier.size(30.dp)) { Icon(Icons.Filled.Settings, "درگاه", tint = Saffron, modifier = Modifier.size(17.dp)) }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) { Icon(Icons.Filled.Close, "بستن", tint = InkSoft, modifier = Modifier.size(17.dp)) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AiMode.entries.forEach { m ->
                        Surface(onClick = { mode = m }, shape = RoundedCornerShape(10.dp),
                            color = if (mode == m) Saffron else DeepGreenSoft.copy(alpha = .25f)) {
                            Text(m.label, Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                fontSize = 13.sp, color = ReadableInk, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = LineGreen.copy(alpha = .4f))
                Spacer(Modifier.height(8.dp))
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    if (mode != AiMode.CHAT) {
                        if (loading) Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = Saffron); Spacer(Modifier.height(10.dp))
                                Text("در حال پردازش…", fontSize = 13.sp, color = ReadableInk)
                            }
                        } else {
                            val r = result
                            if (r != null && r.success) {
                                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(AnswerBg)
                                    .border(1.dp, LineGreen.copy(alpha = .5f), RoundedCornerShape(12.dp)).padding(12.dp)) {
                                    Text(r.content, fontSize = 14.sp, color = ReadableInk, lineHeight = 26.sp, fontFamily = VazirFont)
                                }
                            } else {
                                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFFDEAE5))
                                    .border(1.dp, Brick.copy(alpha = .5f), RoundedCornerShape(12.dp)).padding(12.dp)) {
                                    Text("❌ ${r?.error ?: "خطا"}", fontSize = 13.sp, color = Color(0xFFB3341E), lineHeight = 22.sp)
                                }
                            }
                        }
                    } else {
                        if (history.isEmpty()) Text("پرسش خود را دربارهٔ این یادداشت بنویس؛ پاسخ با توجه به متن یادداشت داده می‌شود. 💬",
                            fontSize = 13.sp, color = ReadableInk.copy(alpha = .8f), lineHeight = 22.sp)
                        history.forEach { (q, a) ->
                            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(QuestionBg).padding(10.dp)) {
                                Text("پرسش: $q", fontSize = 13.sp, color = ReadableInk, fontWeight = FontWeight.Bold, lineHeight = 22.sp)
                            }
                            Spacer(Modifier.height(4.dp))
                            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(AnswerBg)
                                .border(1.dp, LineGreen.copy(alpha = .5f), RoundedCornerShape(10.dp)).padding(10.dp)) {
                                Text(a, fontSize = 14.sp, color = ReadableInk, lineHeight = 26.sp, fontFamily = VazirFont)
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        if (pending != null) Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(color = Saffron, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(6.dp)); Text("در حال پاسخ…", fontSize = 12.sp, color = ReadableInk)
                        }
                    }
                }
                if (mode == AiMode.CHAT) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = question, onValueChange = { question = it },
                            placeholder = { Text("پرسش شما…", fontSize = 13.sp) },
                            singleLine = true, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(6.dp))
                        IconButton(onClick = { if (question.isNotBlank()) { pending = question.trim(); question = "" } },
                            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(Saffron)) {
                            Icon(Icons.Filled.Send, "ارسال", tint = ReadableInk, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AiGatewayDialog(onDismiss: () -> Unit, onSaved: () -> Unit) {
    val context = LocalContext.current
    var provider by remember { mutableStateOf(AiConfig.provider(context)) }
    var key by remember { mutableStateOf(AiConfig.apiKey(context)) }
    var url by remember { mutableStateOf(AiConfig.baseUrl(context)) }
    var model by remember { mutableStateOf(AiConfig.model(context)) }

    val canSave = when (provider) {
        AiProvider.POLLINATIONS -> true
        AiProvider.CUSTOM -> key.isNotBlank() && url.isNotBlank()
        else -> key.isNotBlank()
    }

    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("🌐 درگاه هوش مصنوعی", fontFamily = LalezarFont, fontSize = 20.sp, color = ReadableInk) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState())) {
                Text("۱) سرویس‌دهنده:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ReadableInk)
                AiProvider.entries.forEach { p ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .clickable { provider = p; url = p.defaultBaseUrl; model = p.defaultModel }
                        .padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = provider == p, onClick = { provider = p; url = p.defaultBaseUrl; model = p.defaultModel },
                            colors = RadioButtonDefaults.colors(selectedColor = Saffron))
                        Spacer(Modifier.width(4.dp))
                        Column {
                            Text(p.displayName, fontSize = 14.sp, color = ReadableInk, fontWeight = if (provider == p) FontWeight.Bold else FontWeight.Normal)
                            Text(when {
                                !p.needsKey -> "🆓 رایگان، بدون کلید"
                                p.availableInIran -> "✅ در دسترس برای ایران"
                                else -> "⚠️ ایران را تحریم کرده است"
                            }, fontSize = 11.sp, color = Brick, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(AnswerBg)
                    .border(1.dp, LineGreen.copy(alpha = .5f), RoundedCornerShape(12.dp)).padding(10.dp)) {
                    Column {
                        Text("📘 آموزش قدم‌به‌قدم:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ReadableInk)
                        Spacer(Modifier.height(4.dp))
                        Text(provider.helpFa, fontSize = 12.sp, color = ReadableInk, lineHeight = 20.sp)
                        if (provider.keyUrl.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text("🔗 لینک دریافت کلید (نگه‌دار و کپی کن):", fontSize = 11.sp, color = ReadableInk.copy(alpha = .8f))
                            SelectionContainer { Text(provider.keyUrl, fontSize = 12.sp, color = Color(0xFF8A5A00), fontWeight = FontWeight.Bold) }
                            Text("🌐 سایت: ${provider.siteUrl}", fontSize = 11.sp, color = ReadableInk.copy(alpha = .8f))
                        }
                    }
                }
                if (provider.needsKey) {
                    Spacer(Modifier.height(8.dp))
                    Text("۲) کلید API:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ReadableInk)
                    OutlinedTextField(value = key, onValueChange = { key = it }, label = { Text("API Key") },
                        leadingIcon = { Icon(Icons.Filled.Key, null, tint = Saffron, modifier = Modifier.size(18.dp)) },
                        singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                if (provider == AiProvider.CUSTOM) {
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("Base URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("نام مدل") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(8.dp))
                Text("⚖️ مسئولیت رعایت قوانین هر سرویس‌دهنده و مقررات منطقه‌ای با کاربر است.",
                    fontSize = 11.sp, color = Brick, fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = {
            TextButton(enabled = canSave, onClick = { AiConfig.save(context, provider, key, url, model); onSaved() }) {
                Text("ذخیره ✅", color = if (canSave) Saffron else Brick, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف", color = ReadableInk) } })
}
