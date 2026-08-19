package ir.yaddasht.app.util

import android.content.Context

object Recovery {
    private const val PREFS = "yaddasht_recovery"
    private const val KEY = "rec_"

    fun genCode(): String = (100000..999999).random().toString()

    fun saveBackup(context: Context, noteId: Long, code: String, plain: String) {
        val enc = NoteLock.lock(plain, code)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY + noteId, enc).apply()
    }

    fun tryRecover(context: Context, noteId: Long, code: String): String? {
        val enc = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY + noteId, null) ?: return null
        return NoteLock.unlock(enc, code)
    }
}