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
    OFFLINE("موتور ویرایشگر ✍️ (آفلاین داخلی)", "", "", ApiFormat.LOCAL, true, false, "", "",
        "بدون اینترنت و بدون کلید!\nخلاصه‌سازی، کلیدواژه، اقدام‌ها، زمان‌ها، لحن، آمار، پرسش‌وپاسخ و «ویراستاری متن» روی خود گوشی.\n✅ پیشنهاد ما با توجه به قطعی/مسدودیت شبکه."),
    QWEN("Qwen کوئن 🇨🇳 (نسخهٔ بین‌المللی)", "https://dashscope-intl.aliyuncs.com/compatible-mode/v1", "qwen-turbo", ApiFormat.OPENAI, true, true,
        "https://www.alibabacloud.com/product/modelstudio", "https://modelstudio.console.alibabacloud.com/",
        "⚠️ کنسول چین بسته است؛ از نسخهٔ بین‌المللی استفاده کن:\n۱) وارد کنسول Alibaba Cloud ModelStudio (لینک بالا) شو\n۲) از بخش API-Key کلید بساز\n۳) اپ به‌صورت پیش‌فرض از endpoint بین‌المللی (intl) استفاده می‌کند"),
    POLLINATIONS("Pollinations 🆓 (بدون کلید)", "https://text.pollinations.ai", "openai", ApiFormat.POLLINATIONS, true, false,
        "https://pollinations.ai", "",
        "نیاز به ثبت‌نام و کلید ندارد؛ اپ از مسیر ناشناسِ رایگان استفاده می‌کند."),
    MISTRAL("Mistral 🇫🇷 (طرح رایگان)", "https://api.mistral.ai/v1", "mistral-small-latest", ApiFormat.OPENAI, true, true,
        "https://mistral.ai", "https://console.mistral.ai/api-keys",
        "۱) در console.mistral.ai ثبت‌نام کن\n۲) کلید بساز\n✅ طرح آزمایشی رایگان دارد"),
    GROQ("Groq ⚡ (سریع، سهمیه رایگان)", "https://api.groq.com/openai/v1", "llama-3.1-8b-instant", ApiFormat.OPENAI, true, true,
        "https://groq.com", "https://console.groq.com/keys",
        "۱) در console.groq.com ثبت‌نام کن\n۲) کلید بساز\n✅ سهمیهٔ رایگان روزانه"),
    CEREBRAS("Cerebras ⚡", "https://api.cerebras.ai/v1", "llama3.1-8b", ApiFormat.OPENAI, true, true,
        "https://www.cerebras.ai", "https://cloud.cerebras.ai",
        "۱) در cloud.cerebras.ai ثبت‌نام کن\n۲) کلید بساز\n✅ طرح رایگان دارد"),
    HUGGINGFACE("HuggingFace 🤗 (رایگان)", "https://api-inference.huggingface.co/v1", "microsoft/Phi-3.5-mini-instruct", ApiFormat.OPENAI, true, true,
        "https://huggingface.co", "https://huggingface.co/settings/tokens",
        "۱) در huggingface.co ثبت‌نام کن\n۲) از Settings/Tokens یک توکن بساز\n✅ سهمیهٔ رایگان"),
    OPENROUTER("OpenRouter 🌐 (مدل رایگان)", "https://openrouter.ai/api/v1", "meta-llama/llama-3.1-8b-instruct:free", ApiFormat.OPENAI, true, true,
        "https://openrouter.ai", "https://openrouter.ai/keys",
        "۱) وارد سایت شو (ایمیل/گوگل)\n۲) از لینک Keys کلید بساز\n✅ مدل‌های با پسوند :free کاملاً رایگان‌اند"),
    GITHUB("GitHub Models 🐙 (معمولاً مسدود در ایران)", "https://models.inference.ai.azure.com", "gpt-4o-mini", ApiFormat.OPENAI, false, true,
        "https://github.com/marketplace/models", "https://github.com/settings/personal-access-tokens",
        "⚠️ دامنهٔ azure.com در بسیاری از شبکه‌های ایران مسدود است؛ فقط با DNS غیرایرانی."),
    DEEPSEEK("DeepSeek 🇨 (API پولی!)", "https://api.deepseek.com", "deepseek-chat", ApiFormat.OPENAI, true, true,
        "https://www.deepseek.com", "https://platform.deepseek.com/api_keys",
        "⚠️ چتِ سایت رایگان است ولی API نیاز به شارژ دلاری دارد (خطای Insufficient Balance)."),
    OPENAI("OpenAI (ChatGPT) 🇺🇸", "https://api.openai.com/v1", "gpt-4o-mini", ApiFormat.OPENAI, false, true,
        "https://openai.com", "https://platform.openai.com/api-keys",
        "۱) حساب بساز و کلید بگیر\n⚠️ ایران تحریم است؛ مسئولیت با کاربر"),
    GEMINI("Google Gemini 🇺🇸", "https://generativelanguage.googleapis.com/v1beta", "gemini-1.5-flash", ApiFormat.GEMINI, false, true,
        "https://ai.google.dev", "https://aistudio.google.com/app/apikey",
        "۱) وارد AI Studio شو و کلید بگیر\n⚠️ گوگل برای ایران در دسترس نیست؛ مسئولیت با کاربر"),
    CUSTOM("🔧 سرویس سفارشی", "", "", ApiFormat.OPENAI, true, true, "", "",
        "آدرس پایه، نام مدل و کلید را دستی وارد کن.");
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
        msg != null && (isNetworkError(msg) || msg.contains("402") || msg.contains("PAYMENT_REQUIRED", true) || msg.contains("balance", true))

    private suspend fun callWithFallback(context: Context, prompt: String, offline: () -> String): AnalysisResult {
        val primary = AiConfig.provider(context)
        val first = attempt(context, primary, prompt)
        if (first.success) return first
        if (isBlockedOrPaid(first.error)) {
            if (primary != AiProvider.POLLINATIONS) {
                val second = attempt(context, AiProvider.POLLINATIONS, prompt)
                if (second.success) return second.copy(content = "🔄 سرویس انتخابی در دسترس نبود؛ به‌صورت خودکار از «Pollinations 🆓» استفاده شد.\n\n" + second.content)
            }
            return AnalysisResult(true, "📴 اینترنت/سرویس‌های آنلاین در دسترس نبودند؛ پاسخ توسط «موتور ویرایشگر ✍️» تولید شد:\n\n" + offline())
        }
        return first
    }

    private fun readAll(conn: HttpURLConnection): String =
        BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }

    private fun readErr(conn: HttpURLConnection): String =
        conn.errorStream?.let { BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { r -> r.readText() } } ?: "HTTP ${conn.responseCode}"

    private fun friendly(err: String): String = when {
        err.contains("Insufficient Balance", true) || err.contains("balance", true) ->
            "💳 این سرویس پولی است و اعتبار ندارد. از ⚙️ گزینهٔ «موتور ویرایشگر ✍️»، Qwen یا Pollinations را انتخاب کن."
        err.contains("PAYMENT_REQUIRED", true) || err.contains("402") -> "💳 این مسیر پولی شده؛ اپ از مسیر ناشناس/آفلاین استفاده می‌کند."
        err.contains("401") -> "کلید API معتبر نیست (401)"
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
                conn.connectTimeout = 20000; conn.readTimeout = 90000; conn.doOutput = true
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body); it.flush() }
                if (conn.responseCode == 200) return AnalysisResult(true, parseContent(readAll(conn)).trim())
                val getUrl = "$base/" + URLEncoder.encode(prompt.take(1800), "UTF-8")
                val c2 = URL(getUrl).openConnection() as HttpURLConnection
                c2.requestMethod = "GET"; c2.connectTimeout = 20000; c2.readTimeout = 90000
                if (c2.responseCode == 200) return AnalysisResult(true, readAll(c2).trim())
                return AnalysisResult(false, "", friendly(readErr(c2)))
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
            if (conn.responseCode != 200) return AnalysisResult(false, "", friendly(readErr(conn)))
            val raw = readAll(conn)
            val content = if (provider.format == ApiFormat.GEMINI)
                JSONObject(raw).getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
            else parseContent(raw)
            AnalysisResult(true, content.trim())
        } catch (e: Exception) {
            AnalysisResult(false, "", e.message ?: "خطای شبکه؛ اینترنت را بررسی کن")
        }
    }
}
