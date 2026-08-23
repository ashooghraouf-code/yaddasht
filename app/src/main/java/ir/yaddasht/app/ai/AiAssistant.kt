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

enum class ApiFormat { OPENAI, GEMINI, POLLINATIONS }

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
    POLLINATIONS("Pollinations 🆓 (بدون کلید)", "https://text.pollinations.ai", "openai", ApiFormat.POLLINATIONS, true, false,
        "https://pollinations.ai", "",
        "نیاز به هیچ ثبت‌نام و کلیدی ندارد!\nفقط همین گزینه را انتخاب و «ذخیره» را بزن.\n✅ کاملاً رایگان و قابل استفاده در ایران"),
    OPENROUTER("OpenRouter 🌐 (مدل رایگان)", "https://openrouter.ai/api/v1", "meta-llama/llama-3.1-8b-instruct:free", ApiFormat.OPENAI, true, true,
        "https://openrouter.ai", "https://openrouter.ai/keys",
        "۱) وارد سایت شو (ایمیل/گوگل)\n۲) از لینک Keys کلید بساز\n✅ مدل‌های با پسوند :free کاملاً رایگان‌اند"),
    GROQ("Groq ⚡ (سریع، سهمیه رایگان)", "https://api.groq.com/openai/v1", "llama-3.1-8b-instant", ApiFormat.OPENAI, true, true,
        "https://groq.com", "https://console.groq.com/keys",
        "۱) در console.groq.com ثبت‌نام کن\n۲) کلید بساز\n✅ سهمیهٔ رایگان روزانه"),
    GITHUB("GitHub Models 🐙 (رایگان)", "https://models.inference.ai.azure.com", "gpt-4o-mini", ApiFormat.OPENAI, true, true,
        "https://github.com/marketplace/models", "https://github.com/settings/personal-access-tokens",
        "۱) از لینک بالا یک Personal Access Token بساز\n۲) توکن را به‌جای کلید API وارد کن\n✅ با اکانت گیت‌هاب رایگان"),
    DEEPSEEK("DeepSeek 🇨🇳 (API پولی!)", "https://api.deepseek.com", "deepseek-chat", ApiFormat.OPENAI, true, true,
        "https://www.deepseek.com", "https://platform.deepseek.com/api_keys",
        "⚠️ مهم: چتِ سایت دیپ‌سیک رایگان است ولی API آن نیاز به شارژ دلاری دارد.\nخطای Insufficient Balance به همین دلیل است، نه باگ اپ!"),
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
        val name = prefs(context).getString("provider", AiProvider.POLLINATIONS.name) ?: AiProvider.POLLINATIONS.name
        return AiProvider.entries.firstOrNull { it.name == name } ?: AiProvider.POLLINATIONS
    }
    fun apiKey(context: Context): String = prefs(context).getString("api_key", "") ?: ""
    fun baseUrl(context: Context): String { val s = prefs(context).getString("base_url", "") ?: ""; return if (s.isNotBlank()) s else provider(context).defaultBaseUrl }
    fun model(context: Context): String { val s = prefs(context).getString("model", "") ?: ""; return if (s.isNotBlank()) s else provider(context).defaultModel }
    fun isConfigured(context: Context): Boolean = provider(context) == AiProvider.POLLINATIONS || apiKey(context).isNotBlank()
    fun save(context: Context, p: AiProvider, key: String, url: String, model: String) {
        prefs(context).edit().putString("provider", p.name).putString("api_key", key.trim())
            .putString("base_url", url.trim()).putString("model", model.trim()).apply()
    }
}

object AiAssistant {
    data class AnalysisResult(val success: Boolean, val content: String, val error: String? = null)

    suspend fun analyzeNote(context: Context, title: String, body: String): AnalysisResult = withContext(Dispatchers.IO) {
        call(context, """شما دستیار هوشمند فارسی‌زبان اپ «چراغ راه» هستید. یادداشت زیر را تحلیل کن و با ایموجی و ساختار بنویس:
📌 خلاصه موضوع:
🔑 نکات کلیدی:
💡 پیشنهادها برای بهبود:

عنوان: $title
متن: $body""")
    }

    suspend fun reportNote(context: Context, title: String, body: String): AnalysisResult = withContext(Dispatchers.IO) {
        call(context, """یک گزارش تحلیلی کامل و ساختاریافته به فارسی برای یادداشت زیر بنویس:
🧭 چشم‌انداز کلی:
📈 نکات و داده‌های مهم:
⚠️ ابهام‌ها و کمبودها:
🗺️ پیشنهادهای عملی:
🎯 اولویت‌بندی اقدام‌ها:

عنوان: $title
متن: $body""")
    }

    suspend fun askAboutNote(context: Context, title: String, body: String, history: List<Pair<String, String>>, question: String): AnalysisResult = withContext(Dispatchers.IO) {
        val hist = if (history.isEmpty()) "" else history.joinToString("\n") { "پرسش: ${it.first}\nپاسخ: ${it.second}" }
        call(context, """تو دستیار هوشمند اپ «چراغ راه» هستی. با توجه به متن یادداشت و گفتگوی قبلی، به پرسش جدید پاسخ فارسی و مفید بده. هیچ محدودیتی در تعداد پرسش‌ها نیست.
عنوان یادداشت: $title
متن یادداشت: $body
گفتگوی قبلی:
$hist
پرسش جدید: $question""")
    }

    private fun openaiBody(prompt: String, model: String): String = JSONObject().apply {
        put("model", model); put("temperature", 0.7); put("max_tokens", 2000)
        put("messages", JSONArray().apply { put(JSONObject().apply { put("role", "user"); put("content", prompt) }) })
    }.toString()

    private fun geminiBody(prompt: String): String = JSONObject().apply {
        put("contents", JSONArray().apply { put(JSONObject().apply { put("parts", JSONArray().apply { put(JSONObject().apply { put("text", prompt) }) }) }) })
    }.toString()

    private fun call(context: Context, prompt: String): AnalysisResult {
        if (!AiConfig.isConfigured(context)) return AnalysisResult(false, "", "ابتدا از دکمهٔ ⚙️ یک سرویس‌دهنده انتخاب و ذخیره کن")
        return try {
            val provider = AiConfig.provider(context)
            val key = AiConfig.apiKey(context)
            val base = AiConfig.baseUrl(context).trimEnd('/')
            val model = AiConfig.model(context)
            val (urlStr, jsonBody, auth) = when (provider.format) {
                ApiFormat.OPENAI -> Triple("$base/chat/completions", openaiBody(prompt, model), true)
                ApiFormat.GEMINI -> Triple("$base/models/$model:generateContent?key=$key", geminiBody(prompt), false)
                ApiFormat.POLLINATIONS -> Triple("$base/openai", openaiBody(prompt, model), false)
            }
            val conn = URL(urlStr).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "*/*")
            if (auth && key.isNotBlank()) conn.setRequestProperty("Authorization", "Bearer $key")
            conn.connectTimeout = 30000; conn.readTimeout = 90000; conn.doOutput = true
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(jsonBody); it.flush() }
            if (conn.responseCode != 200) {
                val err = conn.errorStream?.let { BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { r -> r.readText() } } ?: "HTTP ${conn.responseCode}"
                val friendly = when {
                    err.contains("Insufficient Balance", true) || err.contains("balance", true) ->
                        "💳 این سرویس پولی است و اعتبار ندارد. از ⚙️ گزینهٔ رایگان Pollinations یا OpenRouter را انتخاب کن."
                    err.contains("401") -> "کلید API معتبر نیست (401)"
                    err.contains("403") || err.contains("not available", true) -> "این سرویس در منطقهٔ شما در دسترس نیست (403)"
                    err.contains("429") -> "صف شلوغ است؛ چند ثانیه صبر کن و دوباره امتحان کن (429)"
                    else -> err
                }
                return AnalysisResult(false, "", friendly)
            }
            val raw = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
            val content = when (provider.format) {
                ApiFormat.GEMINI -> JSONObject(raw).getJSONArray("candidates").getJSONObject(0)
                    .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
                else -> try {
                    JSONObject(raw).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
                } catch (e: Exception) { raw }
            }
            AnalysisResult(true, content.trim())
        } catch (e: Exception) {
            AnalysisResult(false, "", e.message ?: "خطای شبکه؛ اینترنت را بررسی کن")
        }
    }
}
