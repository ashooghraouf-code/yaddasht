package ir.yaddasht.app.ai

object LocalEngine {

    private val STOPWORDS = setOf(
        "و", "در", "به", "که", "از", "با", "برای", "این", "آن", "را", "تا", "یا", "هم", "هر", "نه",
        "می", "نمی", "است", "هست", "بود", "شد", "کن", "کرد", "کنم", "کنی", "کند", "کردم", "کردی",
        "ما", "شما", "او", "آنها", "من", "تو", "چرا", "چگونه", "چه", "کی", "کجا", "یک", "دو", "سه",
        "روز", "زمان", "بعد", "قبل", "بین", "روی", "زیر", "بالای", "پیش", "پس", "اگر", "ولی", "اما",
        "چون", "زیرا", "همه", "هیچ", "بعضی", "چند", "هرگز", "هنوز", "الان", "امروز", "فردا", "دیروز",
        "اینکه", "آنکه", "باشد", "باشه", "هستش", "داره", "دارم", "داري", "داری", "دارد", "میکنم", "میکند"
    )

    private fun normalize(s: String): String = s
        .replace('ي', 'ی').replace('ك', 'ک').replace('ة', 'ه')
        .replace("‌", " ")

    private fun words(text: String): List<String> =
        normalize(text).split(Regex("[\\s\\p{P}]+")).map { it.trim() }.filter { it.length >= 3 }

    fun keywords(text: String, max: Int = 8): List<String> {
        val freq = mutableMapOf<String, Int>()
        words(text).filter { it !in STOPWORDS }.forEach { freq[it] = (freq[it] ?: 0) + 1 }
        return freq.entries.sortedByDescending { it.value }.take(max).map { it.key }
    }

    private fun sentences(text: String): List<String> =
        normalize(text).split(Regex("[.!؟?\\n]+")).map { it.trim() }.filter { it.length > 10 }

    private fun summarize(text: String, maxSentences: Int = 3): String {
        val sents = sentences(text)
        if (sents.isEmpty()) return "متن کافی برای خلاصه وجود ندارد."
        val kw = keywords(text, 10).toSet()
        val scored = sents.mapIndexed { i, s ->
            val score = words(s).count { it in kw } + (if (i == 0) 2 else 0)
            Triple(i, s, score)
        }
        return scored.sortedByDescending { it.third }.take(maxSentences)
            .sortedBy { it.first }.joinToString("\n") { "• " + it.second }
    }

    private fun suggestions(text: String): String {
        val list = mutableListOf<String>()
        val wc = words(text).size
        if (wc > 300) list.add("یادداشت بلند است؛ آن را به بخش‌های کوچک‌تر تقسیم کن یا از حالت تمرکز ✒️ استفاده کن.")
        if (text.contains("؟") || text.contains("?")) list.add("پرسش‌هایی داخل متن هست؛ می‌توانی برایشان وظیفه یا یادآور بسازی.")
        if (Regex("فردا|امروز|ساعت|صبح|عصر|هفته").containsMatchIn(text)) list.add("متن به زمان اشاره دارد؛ یک یادآور ⏰ تنظیم کن تا فراموش نشود.")
        if (text.contains("☐") || text.contains("☑")) list.add("چک‌لیست تشخیص داده شد؛ موارد انجام‌نشده را اولویت‌بندی کن.")
        if (list.isEmpty()) list.add("روزانه یک بازبینی کوتاه برای این یادداشت زمان‌بندی کن.")
        return list.joinToString("\n") { "• " + it }
    }

    fun analyze(title: String, body: String): String {
        if (body.isBlank()) return "یادداشت خالی است؛ چیزی برای تحلیل نیست."
        return """📌 خلاصه موضوع:
${summarize(body, 2)}

🔑 نکات کلیدی:
${keywords(body).joinToString("، ")}

💡 پیشنهادها:
${suggestions(body)}"""
    }

    fun report(title: String, body: String): String {
        if (body.isBlank()) return "یادداشت خالی است؛ چیزی برای گزارش نیست."
        val wc = words(body).size
        val sc = sentences(body).size
        return """🧭 چشم‌انداز کلی:
${summarize(body, 2)}

📈 داده‌های متن:
• ${wc} کلمه و ${sc} جمله
• بلندترین جمله: ${(sentences(body).maxByOrNull { it.length } ?: "-").take(80)}

🔑 کلیدواژه‌ها:
${keywords(body, 10).joinToString("، ")}

⚠️ ابهام‌ها و کمبودها:
${if (body.length < 100) "• متن کوتاه است؛ جزئیات بیشتری بنویس تا تحلیل دقیق‌تر شود." else "• مورد ساختاری خاصی یافت نشد."}

🗺️ پیشنهادهای عملی:
${suggestions(body)}

🎯 اولویت‌بندی:
• ابتدا موارد مرتبط با «${keywords(body, 1).firstOrNull() ?: "موضوع"}» را پیگیری کن."""
    }

    fun answer(question: String, body: String): String {
        val qk = keywords(question, 6)
        if (qk.isEmpty()) return "پرسش کلیدواژهٔ قابل‌جستجو ندارد؛ ساده‌تر بپرس."
        val sents = sentences(body)
        val best = sents.map { s -> Pair(s, qk.count { k -> s.contains(k) }) }.maxByOrNull { it.second }
        return if (best != null && best.second > 0)
            "بر اساس متن یادداشت:\n«${best.first}»\n\nکلیدواژه‌های یافت‌شده: ${qk.filter { k -> best.first.contains(k) }.joinToString("، ")}"
        else
            "در متن این یادداشت پاسخ مستقیمی برای پرسش شما پیدا نشد.\nکلیدواژه‌های پرسش: ${qk.joinToString("، ")}\n💡 متن یادداشت را کامل‌تر کن یا از سرویس‌های آنلاین برای پاسخ عمیق‌تر استفاده کن."
    }
}
