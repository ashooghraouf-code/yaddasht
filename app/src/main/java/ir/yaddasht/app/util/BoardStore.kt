package ir.yaddasht.app.util

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Board(val id: Long, val name: String, val background: Int, val sizeIndex: Int = 1)

data class BoardItem(
    val noteId: Long,
    val boardId: Long,
    val x: Float,
    val y: Float,
    val rotation: Float,
    val scale: Float
)

data class BoardImage(
    val id: Long,
    val boardId: Long,
    val uri: String,
    val x: Float,
    val y: Float,
    val rotation: Float,
    val scale: Float
)

object BoardStore {
    private const val PREFS = "board_store"
    private const val KEY_BOARDS = "boards"
    private const val KEY_ITEMS = "items"
    private const val KEY_IMAGES = "images"

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun boards(c: Context): List<Board> {
        val json = prefs(c).getString(KEY_BOARDS, "") ?: ""
        if (json.isBlank()) {
            val default = listOf(Board(1L, "اصلی", 0, 1))
            saveBoards(c, default)
            return default
        }
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                Board(
                    o.getLong("id"),
                    o.getString("name"),
                    o.optInt("background", 0),
                    o.optInt("sizeIndex", 1)
                )
            }
        } catch (_: Exception) {
            listOf(Board(1L, "اصلی", 0, 1))
        }
    }

    fun saveBoards(c: Context, list: List<Board>) {
        val arr = JSONArray()
        list.forEach { b ->
            arr.put(JSONObject().apply {
                put("id", b.id)
                put("name", b.name)
                put("background", b.background)
                put("sizeIndex", b.sizeIndex)
            })
        }
        prefs(c).edit().putString(KEY_BOARDS, arr.toString()).apply()
    }

    fun addBoard(c: Context, name: String, background: Int = 0, sizeIndex: Int = 1): Board {
        val list = boards(c).toMutableList()
        val b = Board(System.currentTimeMillis(), name, background, sizeIndex)
        list.add(b)
        saveBoards(c, list)
        return b
    }

    fun setBackground(c: Context, boardId: Long, bg: Int) {
        saveBoards(c, boards(c).map { if (it.id == boardId) it.copy(background = bg) else it })
    }

    fun setBoardSize(c: Context, boardId: Long, sizeIndex: Int) {
        saveBoards(c, boards(c).map { if (it.id == boardId) it.copy(sizeIndex = sizeIndex.coerceIn(0, 3)) else it })
    }

    private fun allItems(c: Context): List<BoardItem> {
        val json = prefs(c).getString(KEY_ITEMS, "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                BoardItem(
                    o.getLong("noteId"),
                    o.getLong("boardId"),
                    o.optDouble("x", 0.0).toFloat(),
                    o.optDouble("y", 0.0).toFloat(),
                    o.optDouble("rotation", 0.0).toFloat(),
                    o.optDouble("scale", 1.0).toFloat()
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveItems(c: Context, list: List<BoardItem>) {
        val arr = JSONArray()
        list.forEach { i ->
            arr.put(JSONObject().apply {
                put("noteId", i.noteId)
                put("boardId", i.boardId)
                put("x", i.x.toDouble())
                put("y", i.y.toDouble())
                put("rotation", i.rotation.toDouble())
                put("scale", i.scale.toDouble())
            })
        }
        prefs(c).edit().putString(KEY_ITEMS, arr.toString()).apply()
    }

    fun items(c: Context, boardId: Long): List<BoardItem> = allItems(c).filter { it.boardId == boardId }

    fun addItem(c: Context, noteId: Long, boardId: Long) {
        val list = allItems(c).toMutableList()
        if (list.any { it.noteId == noteId && it.boardId == boardId }) return
        val count = list.count { it.boardId == boardId }
        val x = 20f + (count % 3) * 40f
        val y = 20f + (count / 3) * 50f
        val rot = listOf(-4f, 3f, -2f, 5f, 0f)[count % 5]
        list.add(BoardItem(noteId, boardId, x, y, rot, 1.0f))
        saveItems(c, list)
    }

    fun move(c: Context, noteId: Long, boardId: Long, x: Float, y: Float) {
        saveItems(c, allItems(c).map {
            if (it.noteId == noteId && it.boardId == boardId)
                it.copy(x = x.coerceAtLeast(0f), y = y.coerceAtLeast(0f))
            else it
        })
    }

    fun rotate(c: Context, noteId: Long, boardId: Long, rotation: Float) {
        saveItems(c, allItems(c).map {
            if (it.noteId == noteId && it.boardId == boardId) it.copy(rotation = rotation)
            else it
        })
    }

    fun setScale(c: Context, noteId: Long, boardId: Long, scale: Float) {
        saveItems(c, allItems(c).map {
            if (it.noteId == noteId && it.boardId == boardId)
                it.copy(scale = scale.coerceIn(0.3f, 3.0f))
            else it
        })
    }

    fun removeItem(c: Context, noteId: Long, boardId: Long) {
        saveItems(c, allItems(c).filterNot { it.noteId == noteId && it.boardId == boardId })
    }

    private fun allImages(c: Context): List<BoardImage> {
        val json = prefs(c).getString(KEY_IMAGES, "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                BoardImage(
                    o.getLong("id"),
                    o.getLong("boardId"),
                    o.getString("uri"),
                    o.optDouble("x", 0.0).toFloat(),
                    o.optDouble("y", 0.0).toFloat(),
                    o.optDouble("rotation", 0.0).toFloat(),
                    o.optDouble("scale", 1.0).toFloat()
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveImages(c: Context, list: List<BoardImage>) {
        val arr = JSONArray()
        list.forEach { i ->
            arr.put(JSONObject().apply {
                put("id", i.id)
                put("boardId", i.boardId)
                put("uri", i.uri)
                put("x", i.x.toDouble())
                put("y", i.y.toDouble())
                put("rotation", i.rotation.toDouble())
                put("scale", i.scale.toDouble())
            })
        }
        prefs(c).edit().putString(KEY_IMAGES, arr.toString()).apply()
    }

    fun images(c: Context, boardId: Long): List<BoardImage> = allImages(c).filter { it.boardId == boardId }

    fun addImage(c: Context, boardId: Long, uri: String): BoardImage {
        val list = allImages(c).toMutableList()
        val count = list.count { it.boardId == boardId }
        val x = 30f + (count % 3) * 30f
        val y = 30f + (count / 3) * 40f
        val img = BoardImage(System.currentTimeMillis(), boardId, uri, x, y, 0f, 1.0f)
        list.add(img)
        saveImages(c, list)
        return img
    }

    fun moveImage(c: Context, imageId: Long, boardId: Long, x: Float, y: Float) {
        saveImages(c, allImages(c).map {
            if (it.id == imageId && it.boardId == boardId)
                it.copy(x = x.coerceAtLeast(0f), y = y.coerceAtLeast(0f))
            else it
        })
    }

    fun rotateImage(c: Context, imageId: Long, boardId: Long, rotation: Float) {
        saveImages(c, allImages(c).map {
            if (it.id == imageId && it.boardId == boardId) it.copy(rotation = rotation)
            else it
        })
    }

    fun setImageScale(c: Context, imageId: Long, boardId: Long, scale: Float) {
        saveImages(c, allImages(c).map {
            if (it.id == imageId && it.boardId == boardId)
                it.copy(scale = scale.coerceIn(0.3f, 3.0f))
            else it
        })
    }

    fun removeImage(c: Context, imageId: Long, boardId: Long) {
        saveImages(c, allImages(c).filterNot { it.id == imageId && it.boardId == boardId })
    }

    fun canUndo(c: Context, boardId: Long): Boolean {
        val json = prefs(c).getString("undo:$boardId", "[]") ?: "[]"
        return try {
            JSONArray(json).length() > 0
        } catch (_: Exception) {
            false
        }
    }

    fun undo(c: Context, boardId: Long): Boolean {
        return false
    }
}
