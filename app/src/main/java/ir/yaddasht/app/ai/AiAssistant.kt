package ir.yaddasht.app.ai

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

enum class ApiFormat { OPENAI, GEMINI, POLLINATIONS, LOCAL }

enum class AiProvider(
    val displayName: String,
    val defaultBaseUrl: String,
    val defaultModel: String,
    val format: ApiFormat,
    val availableInIran: Boolean,
    val needsKey: Boolean,
    val siteUrl: String,
    val keyUrl: String,
    val helpFa: String
) {
    OFFLINE("✍️ موتور ویرایشگر (پیشنهادی)", "", "", ApiFormat.LOCAL, true, false, "", "",
        "بدون اینترنت و بدون کلید.\nخلاصه، کلیدواژه، اقدام، لحن، آمار، پرسش‌وپاسخ و ویراستاری متن.\n✅ همیشه در دسترس؛ همیشه کار می‌کند."),
    GROQ("⚡ Groq (سریع، کلید لازم)", "https://api.groq.com/openai/v1", "llama-3.1-8b-instant", ApiFormat.OPENAI, true, true,
        "https://groq.com", "https://console.groq.com/keys",
        "۱) با ایمیل/گوگل در console.groq.com ثبت‌نام کن\n۲) از API Keys یک کلید بساز\n✅ سهمیهٔ رایگان روزانه؛ معمولاً از ایران کار می‌کند."),
    OPENROUTER("🌐 OpenRouter (مدل رایگان)", "https://openrouter.ai/api/v1", "meta-llama/llama-3.1-8b-instruct:free", ApiFormat.OPENAI, true, true,
        "https://openrouter.ai", "https://openrouter.ai/keys",
        "۱) با ایمیل/گوگل ثبت‌نام کن\n۲) کلید بساز\n✅ مدل‌های :free کاملاً رایگان و بدون نیاز به کارت."),
    POLLINATIONS("🆓 Pollinations (بدون کلید)", "https://text.pollinations.ai", "openai", ApiFormat.POLLINATIONS, true, false,
        "https://pollinations.ai", "",
        "بدون ثبت‌نام و بدون کلید.\n⚠️ گاهی توسط Cloudflare بلاک می‌شود؛ در آن صورت اپ خودکار به موتور آفلاین می‌رود.");
}

object AiConfig {
    private const val PREF = "ai_gateway_prefs"
    private fun prefs(context: Context): SharedPreferences = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
    fun provider(context: Context): AiProvider {
        val name = prefs(context).getString("provider", AiProvider.OFFLINE.name) ?: AiProvider.OFFLINE.name
        return AiProvider.entries.firstOrNull { it.name == name } ?: AiProvider.OFFLINE
    }
    fun apiKey(context: Context): String = prefs(context).getString("api_key", "") ?: ""
    fun baseUrl(context: Context): String { val s = prefs(context).getString("base_url", "") ?: ""; return if (s.isNotBlank()) s else provider(context).defaultBaseUrl }
    fun model(context: Context): String { val s = prefs(context).getString("model", "") ?: ""; return if (s.isNotBlank()) s else provider(context).defaultModel }
    fun isConfigured(context: Context): Boolean = !provider(context).needsKey || apiKey(context).isNotBlank()
    fun save(context: Context, p: AiProvider, key: String, url: String, model: String) {
        prefs(context).edit().putString("provider", p.name).putString("api_key", key.trim())
            .putString("base_url", url.trim()).putString("model", model.trim()).apply()
    }
}

object AiAssistant {
    data class AnalysisResult(val success: Boolean, val content: String, val error: String? = null)

    private fun isHtmlBlock(s: String): Boolean {
        val t = s.trim()
        return t.startsWith("<!DOCTYPE", true) || t.startsWith("<html", true) ||
                t.contains("Cloudflare", true) || t.contains("Attention Required", true) ||
                t.contains("Just a moment", true)
    }

    private fun isBadKey(msg: String?): Boolean =
        msg != null && (msg.contains("invalid_api_key", true) || msg.contains("Incorrect API key", true) ||
                msg.contains("401") || msg.contains("invalid request", true))

    suspend fun analyzeNote(context: Context, title: String, body: String): AnalysisResult = withContext(Dispatchers.IO) {
        if (AiConfig.provider(context) == AiProvider.OFFLINE) return@withContext AnalysisResult(true, LocalEngine.analyze(title, body))
        callWithFallback(context, """شما دستیار هوشمند فارسی‌زبان اپ «چراغ راه» هستید. یادداشت زیر را تحلیل کن و با ایموجی و ساختار بنویس:
📌 خلاصه موضوع:
🔑 نکات کلیدی:
🎬 اقدام‌ها:
💡 پیشنهادها:

عنوان: $title
متن: ${body.take(3000)}""") { LocalEngine.analyze(title, body) }
    }

    suspend fun reportNote(context: Context, title: String, body: String): AnalysisResult = withContext(Dispatchers.IO) {
        if (AiConfig.provider(context) == AiProvider.OFFLINE) return@withContext AnalysisResult(true, LocalEngine.report(title, body))
        callWithFallback(context, """یک گزارش تحلیلی کامل و ساختاریافته به فارسی برای یادداشت زیر بنویس:
🧭 چشم‌انداز کلی:
📈 نکات و داده‌های مهم:
⚠️ ابهام‌ها و کمبودها:
🗺️ پیشنهادهای عملی:
🎯 اولویت‌بندی اقدام‌ها:

عنوان: $title
متن: ${body.take(3000)}""") { LocalEngine.report(title, body) }
    }

    suspend fun askAboutNote(context: Context, title: String, body: String, history: List<Pair<String, String>>, question: String): AnalysisResult = withContext(Dispatchers.IO) {
        if (AiConfig.provider(context) == AiProvider.OFFLINE) return@withContext AnalysisResult(true, LocalEngine.answer(question, body))
        val hist = if (history.isEmpty()) "" else history.takeLast(6).joinToString("\n") { "پرسش: ${it.first}\nپاسخ: ${it.second.take(800)}" }
        callWithFallback(context, """تو دستیار هوشمند اپ «چراغ راه» هستی. با توجه به متن یادداشت و گفتگوی قبلی، به پرسش جدید پاسخ فارسی و مفید بده.
عنوان یادداشت: $title
متن یادداشت: ${body.take(2500)}
گفتگوی قبلی:
$hist
پرسش جدید: $question""") { LocalEngine.answer(question, body) }
    }

    private fun isNetworkError(msg: String?): Boolean {
        if (msg == null) return false
        return msg.contains("Unable to resolve host", true) || msg.contains("No address associated", true) ||
                msg.contains("failed to connect", true) || msg.contains("ConnectException", true) ||
                msg.contains("SocketTimeoutException", true) || msg.contains("Network is unreachable", true) ||
                msg.contains("Connection refused", true)
    }

    private fun isBlockedOrPaid(msg: String?): Boolean =
        msg != null && (isNetworkError(msg) || isHtmlBlock(msg) || isBadKey(msg) || msg.contains("402") ||
                msg.contains("PAYMENT_REQUIRED", true) || msg.contains("balance", true) ||
                msg.contains("CLOUDFLARE", true))

    private suspend fun callWithFallback(context: Context, prompt: String, offline: () -> String): AnalysisResult {
        val primary = AiConfig.provider(context)
        val first = attempt(context, primary, prompt)
        if (first.success) return first
        if (isBlockedOrPaid(first.error)) {
            if (primary != AiProvider.POLLINATIONS && !isBadKey(first.error)) {
                val second = attempt(context, AiProvider.POLLINATIONS, prompt)
                if (second.success) return second.copy(content = "🔄 سرویس انتخابی در دسترس نبود؛ از «Pollinations 🆓» استفاده شد.\n\n" + second.content)
            }
            val note = if (isBadKey(first.error)) "🔑 کلید API نامعتبر بود؛ پاسخ توسط «موتور ویرایشگر ✍️» تولید شد:\n\n"
                       else "📴 سرویس آنلاین در دسترس نبود؛ پاسخ توسط «موتور ویرایشگر ✍️» تولید شد:\n\n"
            return AnalysisResult(true, note + offline())
        }
        return first
    }

    private fun readAll(conn: HttpURLConnection): String =
        BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }

    private fun readErr(conn: HttpURLConnection): String =
        conn.errorStream?.let { BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { r -> r.readText() } } ?: "HTTP ${conn.responseCode}"

    private fun friendly(err: String): String = when {
        isBadKey(err) -> "🔑 کلید API نامعتبر است؛ در ⚙️ کلید درست وارد کن یا «موتور ویرایشگر ✍️» را انتخاب کن."
        isHtmlBlock(err) -> "🚧 این سرویس توسط Cloudflare مسدود شده؛ اپ خودکار از موتور آفلاین استفاده می‌کند."
        err.contains("Insufficient Balance", true) || err.contains("balance", true) ->
            "💳 اعتبار این سرویس تمام شده؛ از «موتور ویرایشگر ✍️» استفاده کن."
        err.contains("PAYMENT_REQUIRED", true) || err.contains("402") -> "💳 این مسیر پولی شده؛ اپ از موتور آفلاین استفاده می‌کند."
        err.contains("403") || err.contains("not available", true) -> "این سرویس در منطقهٔ شما در دسترس نیست (403)"
        err.contains("429") -> "صف شلوغ است؛ چند ثانیه صبر کن (429)"
        else -> err
    }

    private fun parseContent(raw: String): String = try {
        JSONObject(raw).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
    } catch (e: Exception) { raw }

    private fun attempt(context: Context, provider: AiProvider, prompt: String): AnalysisResult {
        if (provider.needsKey && AiConfig.provider(context) != provider)
            return AnalysisResult(false, "", "برای ${provider.displayName} کلید لازم است")
        return try {
            val configured = AiConfig.provider(context) == provider
            val key = if (configured) AiConfig.apiKey(context) else ""
            val base = (if (configured) AiConfig.baseUrl(context) else provider.defaultBaseUrl).trimEnd('/')
            val model = if (configured) AiConfig.model(context) else provider.defaultModel

            if (provider.format == ApiFormat.POLLINATIONS) {
                val body = JSONObject().apply {
                    put("model", model)
                    put("messages", JSONArray().apply { put(JSONObject().apply { put("role", "user"); put("content", prompt) }) })
                }.toString()
                val conn = URL("$base/").openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("User-Agent", "YaddashtApp/1.0")
                conn.connectTimeout = 20000; conn.readTimeout = 90000; conn.doOutput = true
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body); it.flush() }
                if (conn.responseCode == 200) {
                    val c = parseContent(readAll(conn))
                    if (isHtmlBlock(c)) return AnalysisResult(false, "", "CLOUDFLARE")
                    return AnalysisResult(true, c.trim())
                }
                val errRaw = readErr(conn)
                if (isHtmlBlock(errRaw)) return AnalysisResult(false, "", "CLOUDFLARE")
                val getUrl = "$base/" + URLEncoder.encode(prompt.take(1800), "UTF-8")
                val c2 = URL(getUrl).openConnection() as HttpURLConnection
                c2.requestMethod = "GET"
                c2.setRequestProperty("User-Agent", "YaddashtApp/1.0")
                c2.connectTimeout = 20000; c2.readTimeout = 90000
                if (c2.responseCode == 200) {
                    val c = readAll(c2)
                    if (isHtmlBlock(c)) return AnalysisResult(false, "", "CLOUDFLARE")
                    return AnalysisResult(true, c.trim())
                }
                val err2 = readErr(c2)
                return AnalysisResult(false, "", if (isHtmlBlock(err2)) "CLOUDFLARE" else friendly(err2))
            }

            val (urlStr, jsonBody, auth) = when (provider.format) {
                ApiFormat.OPENAI -> Triple("$base/chat/completions", JSONObject().apply {
                    put("model", model); put("temperature", 0.7); put("max_tokens", 2000)
                    put("messages", JSONArray().apply { put(JSONObject().apply { put("role", "user"); put("content", prompt) }) })
                }.toString(), true)
                ApiFormat.GEMINI -> Triple("$base/models/$model:generateContent?key=$key", JSONObject().apply {
                    put("contents", JSONArray().apply { put(JSONObject().apply { put("parts", JSONArray().apply { put(JSONObject().apply { put("text", prompt) }) }) }) })
                }.toString(), false)
                else -> Triple("", "", false)
            }
            val conn = URL(urlStr).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "*/*")
            if (auth && key.isNotBlank()) conn.setRequestProperty("Authorization", "Bearer $key")
            conn.connectTimeout = 20000; conn.readTimeout = 90000; conn.doOutput = true
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(jsonBody); it.flush() }
            if (conn.responseCode != 200) {
                val err = readErr(conn)
                return AnalysisResult(false, "", when {
                    isHtmlBlock(err) -> "CLOUDFLARE"
                    isBadKey(err) -> err
                    else -> friendly(err)
                })
            }
            val raw = readAll(conn)
            if (isHtmlBlock(raw)) return AnalysisResult(false, "", "CLOUDFLARE")
            val content = if (provider.format == ApiFormat.GEMINI)
                JSONObject(raw).getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
            else parseContent(raw)
            AnalysisResult(true, content.trim())
        } catch (e: Exception) {
            AnalysisResult(false, "", e.message ?: "خطای شبکه؛ اینترنت را بررسی کن")
        }
    }
}
