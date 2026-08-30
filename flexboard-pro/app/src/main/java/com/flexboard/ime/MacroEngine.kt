package com.flexboard.ime

import android.content.Context
import com.flexboard.data.db.AppDatabase
import com.flexboard.data.db.Macro
import kotlinx.coroutines.runBlocking

object MacroEngine {
    @Volatile private var cache: List<Macro> = emptyList()
    @Volatile private var loaded = false

    fun preload(ctx: Context) {
        if (loaded) return
        loaded = true
        runBlocking {
            val dao = AppDatabase.get(ctx).macroDao()
            cache = dao.allOnce()
            if (cache.isEmpty()) {
                val seed = listOf(
                    Macro(shortcode = "addr1", expansion = "House #123, Street 4, City"),
                    Macro(shortcode = "mail1", expansion = "myemail@example.com"),
                    Macro(shortcode = "ph1", expansion = "+92-300-0000000"),
                    Macro(shortcode = ":smile:", expansion = "🙂"),
                    Macro(shortcode = ":love:", expansion = "❤")
                )
                seed.forEach { dao.upsert(it) }
                cache = dao.allOnce()
            }
        }
    }

    fun reload(ctx: Context) {
        runBlocking { cache = AppDatabase.get(ctx).macroDao().allOnce() }
    }

    /** Returns expansion if the trailing token before space matches a macro shortcode. */
    fun checkExpansion(beforeSpaceText: String): Pair<String, String>? {
        val token = beforeSpaceText.takeLastWhile { !it.isWhitespace() }
        if (token.isEmpty()) return null
        for (m in cache) {
            val match = if (m.caseSensitive) token == m.shortcode
            else token.equals(m.shortcode, ignoreCase = true)
            if (match) {
                val expansion = if (m.caseSensitive) m.expansion
                else if (token.isNotEmpty() && token[0].isUpperCase()) m.expansion.replaceFirstChar { it.uppercase() }
                else m.expansion
                return token to expansion
            }
        }
        return null
    }
}
