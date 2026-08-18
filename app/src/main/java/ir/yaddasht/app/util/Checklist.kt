package ir.yaddasht.app.util

object Checklist {
    private const val UNCHECKED = "☐ "
    private const val CHECKED = "☑ "

    fun isChecklist(body: String): Boolean {
        if (NoteLock.isLocked(body)) return false
        val lines = body.lineSequence().filter { it.isNotBlank() }.toList()
        return lines.isNotEmpty() && lines.all { it.startsWith(UNCHECKED) || it.startsWith(CHECKED) }
    }

    fun toChecklist(body: String): String = body.lines().joinToString("\n") { l ->
        when {
            l.isBlank() -> ""
            l.startsWith(UNCHECKED) || l.startsWith(CHECKED) -> l
            else -> UNCHECKED + l.trim()
        }
    }.trimEnd('\n')

    fun fromChecklist(body: String): String = body.lines().joinToString("\n") { l ->
        when {
            l.startsWith(UNCHECKED) -> l.removePrefix(UNCHECKED)
            l.startsWith(CHECKED) -> l.removePrefix(CHECKED)
            else -> l
        }
    }

    fun progress(body: String): Pair<Int, Int> {
        val tasks = body.lines().filter { it.startsWith(UNCHECKED) || it.startsWith(CHECKED) }
        return tasks.count { it.startsWith(CHECKED) } to tasks.size
    }

    fun toggleLine(body: String, index: Int): String = body.lines().mapIndexed { i, l ->
        if (i != index) l
        else when {
            l.startsWith(UNCHECKED) -> CHECKED + l.removePrefix(UNCHECKED)
            l.startsWith(CHECKED) -> UNCHECKED + l.removePrefix(CHECKED)
            else -> l
        }
    }.joinToString("\n")
}