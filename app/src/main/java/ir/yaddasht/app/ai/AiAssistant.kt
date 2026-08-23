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

enum class ApiFormat { OPENAI, GEMINI }

enum class AiProvider(
    val displayName: String,
    val defaultBaseUrl: String,
    val defaultModel: String,
    val format: ApiFormat,
    val availableInIran: Boolean,
    val siteUrl: String,
    val keyUrl: String,
    val helpFa: String
) {
    OPENROUTER("OpenRouter 🌐 (پیش‌فرض رایگان)", "https://openrouter.ai/api/v1", "meta-llama/llama-3.1-8b-instruct:free", ApiFormat.OPENAI, true,
        "https://openrouter.ai", "https://openrouter.ai/keys",
        "۱) وارد سایت شوید (با ایمیل یا گوگل)\n۲) از لینک Keys یک کلید بسازید\n✅ مدل‌های با پسوند :free کاملاً رایگان‌اند و نیاز به شارژ ندارند"),
    GROQ("Groq ⚡ (خیلی سریع)", "https://api.groq.com/openai/v1", "llama-3.1-8b-instant", ApiFormat.OPENAI, true,
        "https://groq.com", "https://console.groq.com/keys",
        "۱) در console.groq.com ثبت‌نام کنید\n۲) از بخش API Keys کلید بسازید\n✅ سهمیهٔ رایگان روزانه دارد"),
    DEEPSEEK("DeepSeek 🇨🇳 (نیاز به شارژ)", "https://api.deepseek.com", "deepseek-chat", ApiFormat.OPENAI, true,
        "https://www.deepseek.com", "https://platform.deepseek.com/api_keys",
        "۱) در platform.deepseek.com ثبت‌نام کنید\n۲) کلید بسازید و حساب را شارژ کنید\n⚠️ بدون اعتبار، خطای Insufficient Balance می‌دهد"),
    OPENAI("OpenAI (ChatGPT) 🇺🇸", "https://api.openai.com/v1", "gpt-4o-mini", ApiFormat.OPENAI, false,
        "https://openai.com", "https://platform.openai.com/api-keys",
        "۱) در platform.openai.com حساب بسازید\n۲) کلید API بگیرید\n⚠️ ایران را تحریم کرده؛ استفاده فقط طبق قوانین خود سرویس و با مسئولیت کاربر"),
    GEMINI("Google Gemini 🇺🇸", "https://generativelanguage.googleapis.com/v1beta", "gemini-1.5-flash", ApiFormat.GEMINI, false,
        "https://ai.google.dev", "https://aistudio.google.com/app/apikey",
        "۱) با حساب گوگل وارد AI Studio شوید\n۲) Get API Key بزنید\n⚠️ خدمات گوگل برای ایران در دسترس نیست؛ مسئولیت با کاربر است"),
    CUSTOM("🔧 سرویس سفارشی (سازگار با OpenAI)", "", "", ApiFormat.OPENAI, true, "", "",
        "آدرس پایه، نام مدل و کلید را دستی وارد کنید؛ برای هر سرور/سرویس سازگار با OpenAI");
}

object AiConfig {
    private const val PREF = "ai_gateway_prefs"
    private fun prefs(context: Context): SharedPreferences = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
    fun provider(context: Context): AiProvider {
        val name = prefs(context).getString("provider", AiProvider.OPENROUTER.name) ?: AiProvider.OPENROUTER.name
        return AiProvider.entries.firstOrNull { it.name == name } ?: AiProvider.OPENROUTER
    }
    fun apiKey(context: Context): String = prefs(context).getString("api_key", "") ?: ""
    fun baseUrl(context: Context): String { val s = prefs(context).getString("base_url", "") ?: ""; return if (s.isNotBlank()) s else provider(context).defaultBaseUrl }
    fun model(context: Context): String { val s = prefs(context).getString("model", "") ?: ""; return if (s.isNotBlank()) s else provider(context).defaultModel }
    fun isConfigured(context: Context): Boolean = apiKey(context).isNotBlank()
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
        call(context, """تو دستیار هوشمند اپ «چراغ راه» هستی. با توجه به متن یادداشت و ادامهٔ گفتگوی قبلی، به پرسش جدید کاربر پاسخ فارسی و مفید بده. هیچ محدودیتی در تعداد پرسش‌ها وجود ندارد.
عنوان یادداشت: $title
متن یادداشت: $body
گفتگوی قبلی:
$hist
پرسش جدید: $question""")
    }

    private fun call(context: Context, prompt: String): AnalysisResult {
        if (!AiConfig.isConfigured(context)) return AnalysisResult(false, "", "ابتدا کلید API را در درگاه هوش مصنوعی تنظیم کنید")
        return try {
            val provider = AiConfig.provider(context)
            val key = AiConfig.apiKey(context)
            val base = AiConfig.baseUrl(context).trimEnd('/')
            val model = AiConfig.model(context)
            val (urlStr, jsonBody, auth) = when (provider.format) {
                ApiFormat.OPENAI -> Triple("$base/chat/completions", JSONObject().apply {
                    put("model", model); put("temperature", 0.7); put("max_tokens", 2000)
                    put("messages", JSONArray().apply { put(JSONObject().apply { put("role", "user"); put("content", prompt) }) })
                }.toString(), true)
                ApiFormat.GEMINI -> Triple("$base/models/$model:generateContent?key=$key", JSONObject().apply {
                    put("contents", JSONArray().apply { put(JSONObject().apply { put("parts", JSONArray().apply { put(JSONObject().apply { put("text", prompt) }) }) }) })
                }.toString(), false)
            }
            val conn = URL(urlStr).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            if (auth) conn.setRequestProperty("Authorization", "Bearer $key")
            conn.connectTimeout = 30000; conn.readTimeout = 60000; conn.doOutput = true
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(jsonBody); it.flush() }
            if (conn.responseCode != 200) {
                val err = conn.errorStream?.let { BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { r -> r.readText() } } ?: "HTTP ${conn.responseCode}"
                val friendly = when {
                    err.contains("Insufficient Balance", true) || err.contains("balance", true) ->
                        "💳 اعتبار کلید شما تمام شده است. از دکمهٔ ⚙️ یک سرویس کاملاً رایگان مثل OpenRouter یا Groq انتخاب کنید."
                    err.contains("401") -> "کلید API معتبر نیست (401)"
                    err.contains("403") || err.contains("not available") -> "این سرویس در منطقهٔ شما در دسترس نیست (403)"
                    err.contains("429") -> "سهمیهٔ رایگان موقتاً تمام شده؛ چند دقیقه صبر کنید (429)"
                    else -> err
                }
                return AnalysisResult(false, "", friendly)
            }
            val json = JSONObject(BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() })
            val content = when (provider.format) {
                ApiFormat.OPENAI -> json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
                ApiFormat.GEMINI -> json.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
            }
            AnalysisResult(true, content)
        } catch (e: Exception) {
            AnalysisResult(false, "", e.message ?: "خطای شبکه؛ اینترنت را بررسی کنید")
        }
    }
}
