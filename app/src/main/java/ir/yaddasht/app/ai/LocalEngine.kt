package ir.yaddasht.app.ai

object LocalEngine {

    private const val ZWNJ = "‌"
    private const val FA_DIGITS = "۰۱۲۳۴۵۶۷۸۹"

    private fun toFaDigits(s: String): String = s.map { c ->
        when (c) {
            in '0'..'9' -> FA_DIGITS[c - '0']
            in '٠'..'٩' -> FA_DIGITS[c - '٠']
            else -> c
        }
    }.joinToString("")

    private fun normalize(s: String): String = s
        .replace('ي', 'ی').replace('ك', 'ک').replace('ة', 'ه')
        .replace('‌', ' ')
        .let { toFaDigits(it) }

    private val STOPWORDS = setOf(
        "و", "در", "به", "که", "از", "با", "برای", "این", "آن", "را", "تا", "یا", "هم", "هر", "نه",
        "می", "نمی", "است", "هست", "بود", "شد", "کن", "کرد", "کنم", "کنی", "کند", "کردم", "کردی",
        "ما", "شما", "او", "آنها", "من", "تو", "چرا", "چگونه", "چه", "کی", "کجا", "یک", "دو", "سه",
        "روز", "زمان", "بعد", "قبل", "بین", "روی", "زیر", "بالای", "پیش", "پس", "اگر", "ولی", "اما",
        "چون", "زیرا", "همه", "هیچ", "بعضی", "چند", "هرگز", "هنوز", "الان", "امروز", "فردا", "دیروز",
        "اینکه", "آنکه", "باشد", "باشه", "داره", "دارم", "داری", "دارد", "میکنم", "میکند", "شده",
        "شود", "کنید", "کنیم", "خواهد", "هنگام", "مانند", "بدون", "مثل", "دیگر"
    )

    private fun stem(w: String): String {
        var x = w
        for (s in listOf("ترین", "های", "ها", "تر", "ام", "ات", "مان", "تان", "اش", "ی"))
            if (x.length > s.length + 2 && x.endsWith(s)) { x = x.dropLast(s.length); break }
        return x
    }

    private fun words(text: String): List<String> =
        normalize(text).split(Regex("[\\s\\p{P}]+")).map { it.trim() }.filter { it.length >= 3 }

    fun keywords(text: String, max: Int = 8): List<String> {
        val freq = mutableMapOf<String, Int>()
        val display = mutableMapOf<String, String>()
        words(text).filter { it !in STOPWORDS }.forEach { w ->
            val r = stem(w)
            freq[r] = (freq[r] ?: 0) + 1
            display[r] = w
        }
        return freq.entries.sortedByDescending { it.value }.take(max).map { display[it.key] ?: it.key }
    }

    private fun sentences(text: String): List<String> =
        normalize(text).split(Regex("[.!؟?\\n]+")).map { it.trim() }.filter { it.length > 10 }

    fun stats(text: String): String {
        val w = words(text)
        val s = sentences(text)
        val unique = w.distinct().size
        val avg = if (s.isEmpty()) 0 else w.size / s.size
        val readMin = (w.size / 200) + 1
        return "• ${toFaDigits(w.size.toString())} کلمه، ${toFaDigits(s.size.toString())} جمله\n• واژه‌های یکتا: ${toFaDigits(unique.toString())} (${toFaDigits(((unique * 100) / (w.size.coerceAtLeast(1))).toString())}٪ تنوع)\n• میانگین طول جمله: ${toFaDigits(avg.toString())} کلمه\n• زمان مطالعهٔ تقریبی: ${toFaDigits(readMin.toString())} دقیقه"
    }

    private fun summarize(text: String, title: String = "", maxSentences: Int = 3): String {
        val sents = sentences(text)
        if (sents.isEmpty()) return "متن کافی برای خلاصه وجود ندارد."
        val kw = keywords(text, 12).map { stem(it) }.toSet()
        val tkw = words(title).map { stem(it) }.toSet()
        val scored = sents.mapIndexed { i, s ->
            val ws = words(s).map { stem(it) }
            var score = ws.count { it in kw }
            score += ws.count { it in tkw } * 2
            if (i == 0) score += 2
            if (i == sents.lastIndex) score += 1
            if (ws.size < 5 || ws.size > 60) score -= 2
            Triple(i, s, score)
        }
        return scored.sortedByDescending { it.third }.take(maxSentences).sortedBy { it.first }
            .joinToString("\n") { "• " + it.second }
    }

    private fun actions(text: String): List<String> {
        val markers = Regex("باید|بایست|لطفا|یادآوری|فراموش نشه|انجام بده|برو|بخر|تماس بگیر|ارسال کن|آماده کن|پرداخت کن|زنگ بزن|چک کن|بنویس|بساز")
        return sentences(text).filter { markers.containsMatchIn(it) }.take(6)
    }

    private fun times(text: String): List<String> {
        val out = mutableListOf<String>()
        Regex("(امروز|فردا|پس‌?فردا|دیروز|هفتهٔ? بعد|هفتهٔ? دیگه|ماه بعد|شب|صبح|عصر|ظهر|ساعت [۰-۹0-9]+|[۰-۹0-9]+ روز دیگه)").findAll(normalize(text))
            .forEach { out.add(it.value) }
        return out.distinct().take(8)
    }

    private fun sentiment(text: String): String {
        val pos = setOf("خوب", "عالی", "خوش", "موفق", "زیبا", "دوست", "علاقه", "رشد", "پیشرفت", "برنده", "شاد", "امید", "انرژی", "مثبت", "تایید", "سود", "جایزه", "تشکر", "ممنون", "عالیه", "خوبه")
        val neg = setOf("بد", "افتضاح", "مشکل", "خطر", "نگران", "استرس", "غم", "ناراحت", "شکست", "باخت", "درد", "بیمار", "مریض", "خسته", "ناامید", "منفی", "دیر", "دیرکرد", "فراموش", "جریمه", "بدهی", "زیان", "بده")
        val w = words(text)
        val p = w.count { it in pos }
        val n = w.count { it in neg }
        return when {
            p > n -> "🙂 مثبت (${toFaDigits(p.toString())} نشانهٔ مثبت در برابر ${toFaDigits(n.toString())} منفی)"
            n > p -> "😟 منفی/نگران (${toFaDigits(n.toString())} نشانهٔ منفی در برابر ${toFaDigits(p.toString())} مثبت)"
            else -> "😐 خنثی (لحن اطلاع‌رسانی)"
        }
    }

    fun analyze(title: String, body: String): String {
        if (body.isBlank()) return "یادداشت خالی است؛ چیزی برای تحلیل نیست."
        val act = actions(body)
        val tm = times(body)
        return """📌 خلاصه موضوع:
${summarize(body, title, 2)}

🔑 نکات کلیدی:
${keywords(body).joinToString("، ")}

🎬 اقدام‌های یافت‌شده:
${if (act.isEmpty()) "• مورد اقدام صریحی یافت نشد." else act.joinToString("\n") { "• " + it }}

⏳ زمان‌های ذکرشده:
${if (tm.isEmpty()) "• اشارهٔ زمانی خاصی نیست." else tm.joinToString("، ")}

🎭 لحن متن:
${sentiment(body)}

💡 پیشنهادها:
${suggestions(body)}"""
    }

    fun report(title: String, body: String): String {
        if (body.isBlank()) return "یادداشت خالی است؛ چیزی برای گزارش نیست."
        return """🧭 چشم‌انداز کلی:
${summarize(body, title, 2)}

📈 آمار و داده‌ها:
${stats(body)}

🔑 کلیدواژه‌ها:
${keywords(body, 10).joinToString("، ")}

🎬 اقدام‌ها:
${if (actions(body).isEmpty()) "• -" else actions(body).joinToString("\n") { "• " + it }}

⏳ زمان‌ها:
${times(body).joinToString("، ").ifBlank { "• -" }}

🎭 لحن:
${sentiment(body)}

⚠️ ابهام‌ها و کمبودها:
${if (body.length < 100) "• متن کوتاه است؛ جزئیات بیشتری بنویس." else "• ساختار متن قابل قبول است؛ جملات بلند را کوتاه‌تر کن."}

🎯 اولویت‌بندی:
• ابتدا «${keywords(body, 1).firstOrNull() ?: "موضوع اصلی"}» را پیگیری کن."""
    }

    fun answer(question: String, body: String): String {
        val q = normalize(question)
        if (q.contains("خلاصه")) return "📌 " + summarize(body, "", 3)
        if (q.contains("کلیدواژه") || q.contains("موضوع")) return "🔑 " + keywords(body).joinToString("، ")
        if (q.contains("چند") || q.contains("تعداد") || q.contains("آمار")) return "📈\n" + stats(body)
        if (q.contains("اقدام") || q.contains("کار باید") || q.contains("برنامه")) {
            val a = actions(body)
            return if (a.isEmpty()) "اقدام صریحی در متن نیست." else "🎬\n" + a.joinToString("\n") { "• " + it }
        }
        if (q.contains("زمان") || q.contains("کی") || q.contains("چه روزی") || q.contains("مهلت")) {
            val t = times(body)
            return if (t.isEmpty()) "اشارهٔ زمانی در متن نیست." else "⏳ " + t.joinToString("، ")
        }
        if (q.contains("لحن") || q.contains("احساس")) return "🎭 " + sentiment(body)
        val qk = keywords(question, 6)
        if (qk.isEmpty()) return "پرسش کلیدواژهٔ قابل‌جستجو ندارد؛ ساده‌تر بپرس."
        val best = sentences(body).map { s -> Pair(s, qk.count { k -> s.contains(k) }) }
            .sortedByDescending { it.second }.take(2).filter { it.second > 0 }
        return if (best.isNotEmpty())
            "بر اساس متن یادداشت:\n" + best.joinToString("\n") { "«" + it.first + "»" } +
                    "\n\nکلیدواژه‌های یافت‌شده: " + qk.joinToString("، ")
        else "در متن این یادداشت پاسخ مستقیمی پیدا نشد.\nکلیدواژه‌های پرسش: ${qk.joinToString("، ")}\n💡 متن را کامل‌تر کن یا از سرویس آنلاین استفاده کن."
    }

    // ✍️ ویراستار پیشرفته — نسخهٔ اصلاح‌شده (بدون حذف حرف و بدون چسباندن کلمات)
    fun editText(text: String): String {
        if (text.isBlank()) return "متنی برای ویرایش وجود ندارد."
        val fixes = mutableListOf<String>()
        var t = text.replace('ي', 'ی').replace('ك', 'ک').replace('ة', 'ه')
        t = toFaDigits(t)

        // ۱) نیم‌فاصلهٔ می/نمی — فقط وقتی «می/نمی» یک کلمهٔ مستقل است (نه داخل کلمه مثل «کمی»)
        val miRegex = Regex("(?<![\\p{L}])(می|نمی) +(?=\\p{L})")
        if (miRegex.containsMatchIn(t)) {
            t = t.replace(miRegex, "$1" + ZWNJ)
            fixes.add("نیم‌فاصلهٔ «می/نمی»")
        }

        // ۲) «هٔ» — فقط بعد از حروف بی‌صدا (نه بعد از ا/و/ه تا «ماه ی» خراب نشود)
        val heRegex = Regex("([^\\sاوهی]ه) ی (?=\\p{L})")
        if (heRegex.containsMatchIn(t)) {
            t = t.replace(heRegex, "$1ٔ ")
            fixes.add("نشانهٔ «هٔ»")
        }

        // ۳) حذف کلمهٔ تکراری — فقط کلمه‌های ۳ حرفی و بیشتر (تکرارهای مجاز مثل «کم کم» دست نمی‌خورند)
        val dupRegex = Regex("(\\S{3,}) \\1")
        if (dupRegex.containsMatchIn(t)) {
            t = t.replace(dupRegex, "$1")
            fixes.add("حذف کلمهٔ تکراری")
        }

        // ۴) فاصله‌ها و علائم — اعداد اعشاری (۳.۵) و لینک‌ها دست‌نخورده می‌مانند
        t = t.replace(Regex("[ \t]+"), " ")
            .replace(Regex(" +([.,،؛:!؟?])"), "$1")
            .replace(Regex("([.,،؛:!؟?])([^\\s.,،؛:!؟?۰-۹0-9])"), "$1 $2")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()

        // ۵) نقطهٔ پایان
        if (t.isNotEmpty() && !Regex("[.،؛:!؟?]").containsMatchIn(t.takeLast(1))) {
            t += "."
            fixes.add("نقطهٔ پایان")
        }

        return """✍️ متن ویراسته:

$t

🛠 اصلاحات انجام‌شده (${toFaDigits(fixes.size.toString())} مورد):
${if (fixes.isEmpty()) "• متن از قبل تمیز بود 👌" else fixes.distinct().joinToString("\n") { "• " + it }}"""
    }

    private fun suggestions(text: String): String {
        val list = mutableListOf<String>()
        if (words(text).size > 300) list.add("یادداشت بلند است؛ آن را بخش‌بندی کن یا از حالت تمرکز ✒️ استفاده کن.")
        if (text.contains("؟") || text.contains("?")) list.add("پرسش‌هایی داخل متن هست؛ برایشان وظیفه یا یادآور بساز.")
        if (times(text).isNotEmpty()) list.add("به زمان اشاره شده؛ یک یادآور ⏰ تنظیم کن تا فراموش نشود.")
        if (text.contains("☐") || text.contains("☑")) list.add("چک‌لیست تشخیص داده شد؛ موارد انجام‌نشده را اولویت‌بندی کن.")
        if (actions(text).isNotEmpty()) list.add("اقدام‌های صریح داری؛ آن‌ها را به تب وظایف منتقل کن.")
        if (list.isEmpty()) list.add("روزانه یک بازبینی کوتاه برای این یادداشت زمان‌بندی کن.")
        return list.joinToString("\n") { "• " + it }
    }
}
