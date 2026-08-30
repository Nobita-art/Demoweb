package com.flexboard.ime

import android.content.Context
import com.flexboard.data.db.AppDatabase
import com.flexboard.data.db.DictionaryWord
import com.flexboard.data.db.SavedSentence

/**
 * SuggestionEngine
 *
 * Returns up to 5 word suggestions for the current typing context. Two
 * complementary signals drive the ranking:
 *
 *  1. Prefix completions       — words from the dictionary that START with
 *     the user's current partial word (`prefix`). This is the standard
 *     "auto-complete" path and is what the UI shows the moment the user
 *     starts typing a new word.
 *
 *  2. Bigram next-word         — words that have been seen FOLLOWING the
 *     previous typed word (`prevWord`). This is the "next-word predictor"
 *     and lets the keyboard suggest "hoon" right after the user types
 *     "main", or "kar" after "kya". It also boosts prefix completions
 *     that match the current bigram (so "ho" → ["hoon","hota","ho",…]
 *     when the previous word was "main").
 *
 * Both English and Roman-Urdu (Urdu written in Latin script — the actual
 * way Pakistani / Indian users type Urdu in WhatsApp etc.) are seeded on
 * first launch with a starter vocabulary AND a curated set of common
 * bigrams so the predictor works from day one. Everything the user types
 * after that incrementally extends the same store via [learnWord] and
 * [learnSentence].
 */
object SuggestionEngine {

    // ===== Built-in starter vocabulary =====
    private val BUILTIN_EN = listOf(
        "the","be","to","of","and","a","in","that","have","I","it","for","not","on","with","he","as","you","do","at",
        "this","but","his","by","from","they","we","say","her","she","or","an","will","my","one","all","would","there",
        "their","what","so","up","out","if","about","who","get","which","go","me","when","make","can","like","time","no",
        "just","him","know","take","people","into","year","your","good","some","could","them","see","other","than","then",
        "now","look","only","come","its","over","think","also","back","after","use","two","how","our","work","first",
        "well","way","even","new","want","because","any","these","give","day","most","us","please","thanks","sorry","yes",
        "okay","sure","maybe","later","today","tomorrow","tonight","morning","evening","night"
    )

    /**
     * Roman-Urdu (Urdu typed in Latin/English letters). Picked from the
     * most common WhatsApp-style words — covers pronouns, verbs, common
     * adjectives, time/place words, kinship terms, basic numbers, and
     * polite phrases. ~180 entries — small enough to seed instantly,
     * large enough to handle most casual chat.
     */
    private val BUILTIN_UR_LATIN = listOf(
        // pronouns / demonstratives
        "main","tum","aap","hum","wo","ye","yeh","iss","uss","is","us","sab","koi","kuch",
        "mera","meri","mere","tera","teri","tere","tumhara","tumhari","tumhare","uska","uski","uske",
        "hamara","humara","hamari","humari","hamare","humare","unka","unki","unke",
        "mujhe","mujhko","tumhe","tumko","aapko","humko","unko","isko","usko",
        // particles / postpositions
        "ka","ki","ke","ko","se","me","mein","par","pe","tak","liye","wala","wali","wale",
        // be / have
        "hai","hain","tha","thi","they","ho","hoon","hota","hoti","hote","hogi","hoga","honge",
        // common verbs (stem + tense forms)
        "kar","karna","karta","karti","karte","kiya","kar lo","kar do","karo","karenge","karunga","karungi",
        "raha","rahi","rahe","raho","rha","rhi","rhe",
        "ja","jana","jata","jati","jate","jaa","jao","jaunga","jaungi","gaya","gayi","gaye",
        "aa","ana","aana","ata","ati","aaya","aayi","aaye","aaja","aao","aaunga","aaungi",
        "le","lo","liya","li","liye","lunga","lungi",
        "de","do","diya","di","diye","dunga","dungi",
        "dekh","dekha","dekhi","dekho","dekhna","dekhta","dekhti","dekhte",
        "sun","suna","suni","suno","sunna","sunta","sunti","sunte",
        "bol","bola","boli","bolo","bolna","bolta","bolti","bolte",
        "samajh","samjha","samjhi","samjho","samajhna",
        "chal","chala","chali","chalo","chalna","chalta","chalti",
        "rakh","rakha","rakhi","rakho","rakhna",
        "soch","socha","sochi","socho","sochna",
        "ban","bana","bani","banao","banaya","banana","banayi",
        "mil","mila","mili","mile","milo","milna","milte","milenge",
        "kha","khaya","khayi","khao","khana",
        "pi","piya","piyo","pina",
        // questions
        "kya","kia","kyun","kyon","kyu","kyunki","kahan","kahaan","yahan","wahan",
        "kab","kaisa","kaise","kaisi","kaun","kitna","kitni","kitne",
        // affirm / negate
        "haan","han","ji","nahi","nahin","na","mat","bilkul","zaroor","zarur",
        // adjectives / quality
        "achha","accha","acha","theek","thik","sahi","ghalat","galat","sach","jhoot","jhoota",
        "bohot","bahut","thora","zara","thoda","thodi","thoday","zyada","kam","poora","poori",
        // time / place
        "ab","abhi","kal","aaj","subah","sham","raat","din","phir","fir","baad","pehle","pehley",
        // people
        "bhai","behan","bhen","ammi","abbu","papa","mama","dost","yaar","yar","sab log",
        // everyday nouns
        "ghar","school","office","kaam","kam","khana","pani","chai","doodh","paisa","paise",
        "khush","udas","pyar","mohabbat","dil","baat","baatein","kahani",
        // connectors
        "magar","lekin","agar","warna","phir bhi","aur","ya","ke baad","ke pehle",
        // memory / want
        "yaad","bhool","bhoolna","chahiye","chahiyae","zaruri","matlab","yani",
        // greetings / polite
        "shukriya","shukria","welcome","sorry","sahab","sahib",
        "salam","assalam","walaikum","khuda","allah","inshallah","mashallah",
        "wese","waise","hal","hala",
        // numbers
        "ek","do","teen","char","panch","saat","aath","das","dus","bees","sau","hazar"
    )

    /**
     * Curated common bigrams (prevWord → nextWord). Each row inserted as a
     * dictionary entry whose `nextWord` column lights up the next-word
     * predictor. Covers the most frequent two-word sequences in casual
     * Pakistani / Indian chat.
     */
    private val BUILTIN_UR_LATIN_BIGRAMS = listOf(
        "kya" to "haal", "kya" to "kar", "kya" to "ho", "kya" to "hua", "kya" to "baat",
        "main" to "hoon", "main" to "ne", "main" to "to", "main" to "bhi", "main" to "kar",
        "aap" to "kaise", "aap" to "ka", "aap" to "ki", "aap" to "ko", "aap" to "ne", "aap" to "kahan",
        "tum" to "kaise", "tum" to "kya", "tum" to "ne", "tum" to "ho", "tum" to "kahan",
        "kar" to "raha", "kar" to "rahi", "kar" to "rahe", "kar" to "do", "kar" to "lo", "kar" to "diya",
        "ja" to "raha", "ja" to "rahi", "ja" to "rahe", "ja" to "raho",
        "ho" to "raha", "ho" to "rahi", "ho" to "rahe", "ho" to "gaya", "ho" to "gayi", "ho" to "jaye",
        "kaise" to "ho", "kaisi" to "ho", "kaisa" to "hai",
        "kahan" to "ho", "kahan" to "ja", "kahan" to "se", "kahan" to "the",
        "abhi" to "tak", "abhi" to "aa", "abhi" to "kar", "abhi" to "tho",
        "shukriya" to "bhai", "shukria" to "bhai", "shukriya" to "yaar",
        "achha" to "theek", "accha" to "theek", "achha" to "phir",
        "theek" to "hai", "thik" to "hai", "theek" to "thaak",
        "bohot" to "achha", "bahut" to "achha", "bohot" to "shukria", "bahut" to "shukria",
        "phir" to "milte", "phir" to "se", "phir" to "kya", "phir" to "baat",
        "yaar" to "kya", "bhai" to "kya", "yaar" to "yeh",
        "ghar" to "aa", "ghar" to "ja", "ghar" to "me", "ghar" to "pe",
        "khana" to "kha", "chai" to "pi", "pani" to "pi",
        "samajh" to "gaya", "samajh" to "gayi", "samajh" to "aaya",
        "dekh" to "lo", "dekh" to "raha", "dekh" to "kar",
        "sun" to "raha", "sun" to "lo", "sun" to "rahi",
        "bol" to "raha", "bol" to "do", "bol" to "rahi",
        "yaad" to "hai", "yaad" to "rakh", "yaad" to "aaya",
        "chal" to "raha", "chalo" to "phir", "chalo" to "yaar",
        "le" to "lo", "le" to "kar", "le" to "aao",
        "de" to "do", "de" to "diya", "de" to "dunga",
        "mil" to "gaya", "mil" to "gayi", "mil" to "kar",
        "kal" to "milte", "kal" to "phir", "kal" to "subah",
        "aaj" to "kal", "aaj" to "phir", "aaj" to "raat",
        "salam" to "alaikum", "assalam" to "alaikum", "walaikum" to "salam",
        "inshallah" to "kal", "mashallah" to "bhai",
        "matlab" to "kya", "yani" to "kya",
        "magar" to "main", "lekin" to "main", "agar" to "tum"
    )

    private val BUILTIN_EN_BIGRAMS = listOf(
        "i" to "am", "i" to "will", "i" to "was", "i" to "have", "i" to "think",
        "you" to "are", "you" to "can", "you" to "have", "you" to "know",
        "we" to "are", "we" to "can", "we" to "should", "we" to "will",
        "they" to "are", "they" to "will", "they" to "have",
        "it" to "is", "it" to "was", "it" to "will",
        "thank" to "you", "thanks" to "for", "good" to "morning", "good" to "night",
        "see" to "you", "let" to "me", "let" to "us", "as" to "soon", "soon" to "as",
        "have" to "a", "a" to "good", "good" to "day", "good" to "evening",
        "what" to "are", "what" to "is", "what" to "do", "how" to "are", "how" to "is",
        "where" to "are", "when" to "will", "why" to "did",
        "going" to "to", "want" to "to", "need" to "to", "got" to "to",
        "please" to "let", "please" to "send"
    )

    suspend fun ensureSeeded(ctx: Context) {
        val dao = AppDatabase.get(ctx).dictionaryDao()
        // Cheap probe — if anything starting with 'a' exists, we've seeded before.
        val any = dao.suggest("a", 1)
        if (any.isNotEmpty()) return

        BUILTIN_EN.forEach { dao.insert(DictionaryWord(word = it, frequency = 5, language = "en")) }
        BUILTIN_UR_LATIN.forEach {
            dao.insert(DictionaryWord(word = it, frequency = 5, language = "ur-Latn"))
        }
        // Seed bigrams as separate dictionary rows whose `nextWord` is set.
        // The next-word query is `WHERE nextWord = :prevWord` — so we store
        // each bigram as a row with word=next, nextWord=prev. (Yes, the
        // column naming in the DAO is inverted from how it reads — kept as
        // is to avoid a schema migration. The column is effectively the
        // "predecessor" of `word`.)
        BUILTIN_EN_BIGRAMS.forEach { (prev, next) ->
            dao.insert(DictionaryWord(word = next, nextWord = prev, frequency = 4, language = "en"))
        }
        BUILTIN_UR_LATIN_BIGRAMS.forEach { (prev, next) ->
            dao.insert(DictionaryWord(word = next, nextWord = prev, frequency = 4, language = "ur-Latn"))
        }
    }

    /**
     * Return up to 5 ranked suggestions for the current typing context.
     *
     * Three lanes feed the result, in priority order:
     *   1. If the user is mid-word (prefix non-blank) AND we know the
     *      previous word, suggest words that BOTH start with `prefix`
     *      AND have been seen following `prevWord`. These are the most
     *      relevant suggestions and float to the top.
     *   2. Plain prefix completions (frequency-ordered). Always shown
     *      when the user is mid-word so the bar is never empty.
     *   3. If the user just typed a space (prefix blank) AND we know the
     *      previous word, return next-word predictions on their own —
     *      this is what powers "type a word, get the next word for free".
     */
    suspend fun suggest(ctx: Context, prefix: String, prevWord: String?): List<String> {
        val dao = AppDatabase.get(ctx).dictionaryDao()
        val result = LinkedHashSet<String>()
        val pfx = prefix.lowercase()
        val prev = prevWord?.lowercase()

        if (pfx.isBlank()) {
            // Pure next-word lane: user just hit space.
            if (prev != null) {
                dao.nextWordFor(prev, 5).forEach { result.add(it.word) }
            }
            return result.take(5).toList()
        }

        // Lane 1: bigram-aware completions (prev + prefix together).
        if (prev != null) {
            dao.nextWordFor(prev, 25).forEach {
                if (it.word.startsWith(pfx) && it.word != pfx) result.add(it.word)
            }
        }
        // Lane 2: regular prefix completions.
        dao.suggest(pfx, 10).forEach {
            if (it.word != pfx) result.add(it.word)
        }
        return result.take(5).toList()
    }

    suspend fun learnWord(ctx: Context, word: String) {
        if (word.isBlank() || word.length > 30) return
        val dao = AppDatabase.get(ctx).dictionaryDao()
        val w = word.lowercase()
        dao.insert(DictionaryWord(word = w))
        dao.bump(w)
    }

    /**
     * Persist the sentence (so the Saved Sentences screen can show it)
     * AND walk through every adjacent (prev, next) pair to grow the
     * bigram store. The latter is what makes "next-word" suggestions
     * keep getting smarter as the user keeps typing in their own style.
     */
    suspend fun learnSentence(ctx: Context, sentence: String) {
        val s = sentence.trim()
        if (s.isEmpty()) return
        val db = AppDatabase.get(ctx)
        val dao = db.dictionaryDao()

        // ----- Bigram learning -----
        val tokens = s.split(Regex("\\s+"))
            .map { it.lowercase().filter { c -> c.isLetterOrDigit() || c == '\'' } }
            .filter { it.isNotEmpty() && it.length <= 30 }
        for (i in 0 until (tokens.size - 1)) {
            val cur = tokens[i]
            val nxt = tokens[i + 1]
            // word=next, nextWord=prev — see ensureSeeded for the rationale.
            runCatching {
                dao.insert(DictionaryWord(word = nxt, nextWord = cur, frequency = 1))
                dao.bump(nxt)
            }
        }

        // ----- Sentence save (existing behavior) -----
        val prefs = com.flexboard.utils.SettingsStore.prefs(ctx)
        val minLen = prefs.getInt(com.flexboard.utils.SettingsStore.KEY_AUTO_SAVE_MIN_LEN, 12)
        if (s.length < minLen) return
        val enabled = prefs.getBoolean(com.flexboard.utils.SettingsStore.KEY_AUTO_SAVE_SENTENCE, true)
        if (!enabled) return
        val sdao = db.savedSentenceDao()
        val existing = sdao.find(s)
        if (existing == null) sdao.insert(SavedSentence(sentence = s))
        else sdao.bump(existing.id)
    }
}
