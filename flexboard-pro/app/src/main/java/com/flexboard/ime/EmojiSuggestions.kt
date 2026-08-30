package com.flexboard.ime

/**
 * EmojiSuggestions
 *
 * Maps everyday English / Roman-Urdu keywords to a small ranked list of
 * emojis. The suggestion bar consults this map every time the user types
 * (or finishes typing) a word: if the word matches a known keyword, the
 * keyboard surfaces 1-2 relevant emoji chips alongside the regular text
 * suggestions, so tapping is enough to drop the emoji into the message.
 *
 * Lookup is case-insensitive and exact (no fuzzy matching — that would
 * pollute the bar with off-topic glyphs while the user is mid-word).
 * Each entry is intentionally small (max 2 emojis); the goal is one or
 * two strong matches, not an emoji picker.
 */
object EmojiSuggestions {

    private val MAP: Map<String, List<String>> = buildMap {
        // ----- emotions / feelings -----
        listOf("love", "pyar", "pyaar", "mohabbat").forEach { put(it, listOf("\u2764\uFE0F", "\uD83D\uDC95")) }
        listOf("kiss", "chumma").forEach { put(it, listOf("\uD83D\uDC8B", "\uD83D\uDE18")) }
        listOf("hug", "hugs").forEach { put(it, listOf("\uD83E\uDD17", "\u2764\uFE0F")) }
        listOf("happy", "khush", "khushi").forEach { put(it, listOf("\uD83D\uDE0A", "\uD83D\uDE04")) }
        listOf("smile", "muskan", "muskura").forEach { put(it, listOf("\uD83D\uDE0A", "\uD83D\uDE01")) }
        listOf("laugh", "hasi", "hansi", "hasna", "hansna").forEach { put(it, listOf("\uD83D\uDE02", "\uD83E\uDD23")) }
        listOf("haha", "hahaha", "lol", "lmao", "rofl").forEach { put(it, listOf("\uD83D\uDE02", "\uD83E\uDD23")) }
        listOf("sad", "udas", "dukhi", "ghum").forEach { put(it, listOf("\uD83D\uDE22", "\uD83D\uDE1E")) }
        listOf("cry", "crying", "rona", "ronay", "roya").forEach { put(it, listOf("\uD83D\uDE2D", "\uD83D\uDE22")) }
        listOf("angry", "gussa", "gusse", "naraz").forEach { put(it, listOf("\uD83D\uDE20", "\uD83D\uDE21")) }
        listOf("mad", "pagal", "crazy").forEach { put(it, listOf("\uD83E\uDD2A", "\uD83E\uDD2F")) }
        listOf("scared", "dar", "darna", "khauf").forEach { put(it, listOf("\uD83D\uDE28", "\uD83D\uDE31")) }
        listOf("shocked", "hairan", "hairat").forEach { put(it, listOf("\uD83D\uDE32", "\uD83D\uDE2E")) }
        listOf("wow", "omg").forEach { put(it, listOf("\uD83D\uDE2E", "\uD83E\uDD29")) }
        listOf("think", "thinking", "soch", "sochna").forEach { put(it, listOf("\uD83E\uDD14", "\uD83D\uDCAD")) }
        listOf("worry", "worried", "fikar", "pareshan", "tension").forEach { put(it, listOf("\uD83D\uDE1F", "\uD83D\uDE30")) }
        listOf("shy", "sharam", "sharma").forEach { put(it, listOf("\uD83D\uDE33", "\uD83D\uDE48")) }
        listOf("proud", "fakhar").forEach { put(it, listOf("\uD83D\uDE0E", "\uD83D\uDCAA")) }
        listOf("cool").forEach { put(it, listOf("\uD83D\uDE0E", "\uD83D\uDC4C")) }
        listOf("tired", "thaka", "thaki", "thak").forEach { put(it, listOf("\uD83D\uDE29", "\uD83D\uDE2A")) }
        listOf("sleep", "sleeping", "sona", "neend", "soya").forEach { put(it, listOf("\uD83D\uDE34", "\uD83D\uDCA4")) }
        listOf("bored", "boring").forEach { put(it, listOf("\uD83D\uDE10", "\uD83E\uDD71")) }
        listOf("sick", "bimar", "ill").forEach { put(it, listOf("\uD83E\uDD12", "\uD83E\uDD27")) }
        listOf("kiss", "kisses").forEach { put(it, listOf("\uD83D\uDC8B", "\uD83D\uDE18")) }

        // ----- greetings / manners -----
        listOf("hi", "hello", "hey", "salam", "assalam", "salaam").forEach { put(it, listOf("\uD83D\uDC4B", "\uD83D\uDE0A")) }
        listOf("bye", "alvida", "goodbye").forEach { put(it, listOf("\uD83D\uDC4B", "\uD83E\uDEE1")) }
        listOf("thanks", "thank", "shukriya", "shukria").forEach { put(it, listOf("\uD83D\uDE4F", "\u2764\uFE0F")) }
        listOf("sorry", "maaf", "maafi").forEach { put(it, listOf("\uD83D\uDE4F", "\uD83D\uDE14")) }
        listOf("please", "plz", "pls").forEach { put(it, listOf("\uD83D\uDE4F")) }
        listOf("welcome", "khushamdeed").forEach { put(it, listOf("\uD83E\uDD17", "\uD83C\uDF39")) }
        listOf("ok", "okay", "theek", "thik").forEach { put(it, listOf("\uD83D\uDC4C", "\u2705")) }
        listOf("yes", "haan", "han", "ji", "yep").forEach { put(it, listOf("\u2705", "\uD83D\uDC4D")) }
        listOf("no", "nahi", "nahin", "nope").forEach { put(it, listOf("\u274C", "\uD83D\uDE45")) }
        listOf("right", "sahi", "correct").forEach { put(it, listOf("\u2705")) }
        listOf("wrong", "ghalat", "galat").forEach { put(it, listOf("\u274C")) }
        listOf("good", "achha", "accha", "acha", "nice", "great").forEach { put(it, listOf("\uD83D\uDC4D", "\uD83D\uDE0A")) }
        listOf("bad", "bura").forEach { put(it, listOf("\uD83D\uDC4E", "\uD83D\uDE1E")) }
        listOf("best", "top").forEach { put(it, listOf("\uD83C\uDFC6", "\u2B50")) }
        listOf("done", "complete", "finished").forEach { put(it, listOf("\u2705", "\uD83D\uDC4C")) }

        // ----- romance -----
        listOf("heart", "dil").forEach { put(it, listOf("\u2764\uFE0F", "\uD83D\uDC96")) }
        listOf("rose", "gulab").forEach { put(it, listOf("\uD83C\uDF39")) }
        listOf("flower", "phool", "phul").forEach { put(it, listOf("\uD83C\uDF38", "\uD83C\uDF3A")) }
        listOf("ring").forEach { put(it, listOf("\uD83D\uDC8D")) }
        listOf("married", "shadi", "wedding").forEach { put(it, listOf("\uD83D\uDC92", "\uD83D\uDC70")) }
        listOf("date", "dating").forEach { put(it, listOf("\uD83C\uDF39", "\u2764\uFE0F")) }

        // ----- family / people -----
        listOf("mom", "mama", "ammi", "mother").forEach { put(it, listOf("\uD83E\uDD31", "\uD83D\uDC69")) }
        listOf("dad", "papa", "abbu", "father", "baba").forEach { put(it, listOf("\uD83D\uDC68", "\uD83E\uDDD4")) }
        listOf("baby").forEach { put(it, listOf("\uD83D\uDC76")) }
        listOf("brother", "bhai", "bro").forEach { put(it, listOf("\uD83D\uDC6C", "\uD83E\uDD1D")) }
        listOf("sister", "behan", "bhen", "sis").forEach { put(it, listOf("\uD83D\uDC6D")) }
        listOf("friend", "dost", "yaar", "yar", "buddy").forEach { put(it, listOf("\uD83D\uDC6B", "\uD83E\uDD1D")) }
        listOf("family").forEach { put(it, listOf("\uD83D\uDC6A")) }
        listOf("boy", "ladka").forEach { put(it, listOf("\uD83D\uDC66")) }
        listOf("girl", "ladki").forEach { put(it, listOf("\uD83D\uDC67")) }
        listOf("man", "aadmi").forEach { put(it, listOf("\uD83D\uDC68")) }
        listOf("woman", "aurat").forEach { put(it, listOf("\uD83D\uDC69")) }

        // ----- food / drink -----
        listOf("chai", "tea").forEach { put(it, listOf("\u2615")) }
        listOf("coffee").forEach { put(it, listOf("\u2615")) }
        listOf("water", "pani").forEach { put(it, listOf("\uD83D\uDCA7", "\uD83D\uDEB0")) }
        listOf("milk", "doodh").forEach { put(it, listOf("\uD83E\uDD5B")) }
        listOf("food", "khana", "khaana").forEach { put(it, listOf("\uD83C\uDF7D\uFE0F", "\uD83C\uDF5B")) }
        listOf("breakfast", "nashta").forEach { put(it, listOf("\uD83C\uDF73", "\u2615")) }
        listOf("lunch").forEach { put(it, listOf("\uD83C\uDF71")) }
        listOf("dinner").forEach { put(it, listOf("\uD83C\uDF7D\uFE0F")) }
        listOf("pizza").forEach { put(it, listOf("\uD83C\uDF55")) }
        listOf("burger").forEach { put(it, listOf("\uD83C\uDF54")) }
        listOf("biryani", "rice", "chawal").forEach { put(it, listOf("\uD83C\uDF5A")) }
        listOf("bread", "roti", "naan").forEach { put(it, listOf("\uD83C\uDF5E", "\uD83E\uDED3")) }
        listOf("cake").forEach { put(it, listOf("\uD83C\uDF82")) }
        listOf("ice", "icecream").forEach { put(it, listOf("\uD83C\uDF66")) }
        listOf("chicken", "murgi", "murgha").forEach { put(it, listOf("\uD83C\uDF57", "\uD83D\uDC14")) }
        listOf("fruit").forEach { put(it, listOf("\uD83C\uDF4E")) }
        listOf("apple", "seb").forEach { put(it, listOf("\uD83C\uDF4E")) }
        listOf("banana", "kela").forEach { put(it, listOf("\uD83C\uDF4C")) }
        listOf("mango", "aam").forEach { put(it, listOf("\uD83E\uDD6D")) }
        listOf("orange", "santra").forEach { put(it, listOf("\uD83C\uDF4A")) }
        listOf("juice").forEach { put(it, listOf("\uD83E\uDDC3")) }
        listOf("egg", "anda").forEach { put(it, listOf("\uD83E\uDD5A")) }
        listOf("sweet", "mitha").forEach { put(it, listOf("\uD83C\uDF6C", "\uD83C\uDF6D")) }
        listOf("hungry", "bhuk", "bhook").forEach { put(it, listOf("\uD83C\uDF7D\uFE0F", "\uD83D\uDE0B")) }

        // ----- weather / nature -----
        listOf("sun", "dhoop", "suraj").forEach { put(it, listOf("\u2600\uFE0F")) }
        listOf("moon", "chaand", "chand").forEach { put(it, listOf("\uD83C\uDF19")) }
        listOf("star", "sitara", "tara").forEach { put(it, listOf("\u2B50", "\uD83C\uDF1F")) }
        listOf("cloud", "baadal", "badal").forEach { put(it, listOf("\u2601\uFE0F")) }
        listOf("rain", "baarish", "barish").forEach { put(it, listOf("\uD83C\uDF27\uFE0F", "\u2614")) }
        listOf("snow", "baraf", "barf").forEach { put(it, listOf("\u2744\uFE0F", "\u2603\uFE0F")) }
        listOf("wind", "hawa").forEach { put(it, listOf("\uD83D\uDCA8")) }
        listOf("fire", "aag").forEach { put(it, listOf("\uD83D\uDD25", "\uD83D\uDE92")) }
        listOf("tree", "darakht").forEach { put(it, listOf("\uD83C\uDF33")) }
        listOf("rainbow").forEach { put(it, listOf("\uD83C\uDF08")) }
        listOf("lightning", "bijli").forEach { put(it, listOf("\u26A1", "\uD83C\uDF29\uFE0F")) }
        listOf("earth", "world", "dunya").forEach { put(it, listOf("\uD83C\uDF0D")) }
        listOf("sky", "asmaan", "aasman").forEach { put(it, listOf("\uD83C\uDF0C")) }

        // ----- animals -----
        listOf("dog", "kutta").forEach { put(it, listOf("\uD83D\uDC36")) }
        listOf("cat", "billi").forEach { put(it, listOf("\uD83D\uDC31")) }
        listOf("bird", "parinda", "chiriya").forEach { put(it, listOf("\uD83D\uDC26")) }
        listOf("fish", "machli").forEach { put(it, listOf("\uD83D\uDC1F")) }
        listOf("horse", "ghora").forEach { put(it, listOf("\uD83D\uDC0E")) }
        listOf("cow", "gaye", "gai").forEach { put(it, listOf("\uD83D\uDC2E")) }
        listOf("lion", "sher").forEach { put(it, listOf("\uD83E\uDD81")) }
        listOf("tiger").forEach { put(it, listOf("\uD83D\uDC2F")) }
        listOf("snake", "saanp", "samp").forEach { put(it, listOf("\uD83D\uDC0D")) }
        listOf("monkey", "bandar").forEach { put(it, listOf("\uD83D\uDC35", "\uD83D\uDE48")) }
        listOf("elephant", "haathi").forEach { put(it, listOf("\uD83D\uDC18")) }
        listOf("rabbit", "khargosh").forEach { put(it, listOf("\uD83D\uDC30")) }

        // ----- objects / vehicles -----
        listOf("phone", "mobile", "fone").forEach { put(it, listOf("\uD83D\uDCF1")) }
        listOf("camera").forEach { put(it, listOf("\uD83D\uDCF7")) }
        listOf("book", "kitab", "kitaab").forEach { put(it, listOf("\uD83D\uDCDA", "\uD83D\uDCD6")) }
        listOf("pen", "qalam", "kalam").forEach { put(it, listOf("\uD83D\uDD8A\uFE0F")) }
        listOf("car", "gaadi", "gari").forEach { put(it, listOf("\uD83D\uDE97")) }
        listOf("bike", "bicycle", "cycle").forEach { put(it, listOf("\uD83D\uDEB2")) }
        listOf("motorcycle", "motorbike").forEach { put(it, listOf("\uD83C\uDFCD\uFE0F")) }
        listOf("bus").forEach { put(it, listOf("\uD83D\uDE8C")) }
        listOf("train", "rail").forEach { put(it, listOf("\uD83D\uDE86")) }
        listOf("plane", "airplane", "flight").forEach { put(it, listOf("\u2708\uFE0F")) }
        listOf("ship", "boat").forEach { put(it, listOf("\uD83D\uDEA2", "\u26F5")) }
        listOf("house", "ghar", "home").forEach { put(it, listOf("\uD83C\uDFE0")) }
        listOf("school", "madrasa").forEach { put(it, listOf("\uD83C\uDFEB")) }
        listOf("office").forEach { put(it, listOf("\uD83C\uDFE2")) }
        listOf("hospital").forEach { put(it, listOf("\uD83C\uDFE5")) }
        listOf("ambulance").forEach { put(it, listOf("\uD83D\uDE91")) }
        listOf("police").forEach { put(it, listOf("\uD83D\uDE93", "\uD83D\uDC6E")) }
        listOf("gun", "bandook", "pistol").forEach { put(it, listOf("\uD83D\uDD2B")) }
        listOf("bomb", "dhamaka").forEach { put(it, listOf("\uD83D\uDCA3", "\uD83D\uDCA5")) }
        listOf("money", "paisa", "paise", "cash").forEach { put(it, listOf("\uD83D\uDCB0", "\uD83D\uDCB5")) }
        listOf("gift", "tohfa").forEach { put(it, listOf("\uD83C\uDF81")) }
        listOf("key", "chabi").forEach { put(it, listOf("\uD83D\uDD11")) }
        listOf("lock", "tala").forEach { put(it, listOf("\uD83D\uDD12")) }
        listOf("clock", "ghadi").forEach { put(it, listOf("\u23F0", "\uD83D\uDD50")) }
        listOf("watch").forEach { put(it, listOf("\u231A")) }

        // ----- activities / sports -----
        listOf("music", "gaana", "song", "geet").forEach { put(it, listOf("\uD83C\uDFB5", "\uD83C\uDFB6")) }
        listOf("dance", "naach", "nach").forEach { put(it, listOf("\uD83D\uDC83", "\uD83D\uDD7A")) }
        listOf("game", "khel").forEach { put(it, listOf("\uD83C\uDFAE")) }
        listOf("football", "soccer").forEach { put(it, listOf("\u26BD")) }
        listOf("cricket").forEach { put(it, listOf("\uD83C\uDFCF")) }
        listOf("basketball").forEach { put(it, listOf("\uD83C\uDFC0")) }
        listOf("party", "celebration", "celebrate").forEach { put(it, listOf("\uD83C\uDF89", "\uD83E\uDD73")) }
        listOf("birthday", "salgirah", "bday").forEach { put(it, listOf("\uD83C\uDF82", "\uD83C\uDF89")) }
        listOf("gym", "exercise", "workout").forEach { put(it, listOf("\uD83D\uDCAA", "\uD83C\uDFCB\uFE0F")) }
        listOf("run", "running", "daur", "bhag").forEach { put(it, listOf("\uD83C\uDFC3")) }
        listOf("walk", "walking").forEach { put(it, listOf("\uD83D\uDEB6")) }
        listOf("swim", "swimming").forEach { put(it, listOf("\uD83C\uDFCA")) }
        listOf("read", "reading", "parh").forEach { put(it, listOf("\uD83D\uDCD6")) }
        listOf("write", "writing", "likhna", "likh").forEach { put(it, listOf("\u270D\uFE0F", "\uD83D\uDCDD")) }

        // ----- time -----
        listOf("morning", "subah").forEach { put(it, listOf("\uD83C\uDF05", "\u2600\uFE0F")) }
        listOf("evening", "sham").forEach { put(it, listOf("\uD83C\uDF06")) }
        listOf("night", "raat").forEach { put(it, listOf("\uD83C\uDF19", "\uD83C\uDF03")) }
        listOf("today", "aaj").forEach { put(it, listOf("\uD83D\uDCC5")) }
        listOf("tomorrow", "kal").forEach { put(it, listOf("\uD83D\uDCC5", "\u23ED\uFE0F")) }
        listOf("yesterday").forEach { put(it, listOf("\uD83D\uDCC6")) }
        listOf("time", "waqt").forEach { put(it, listOf("\u23F0", "\u231A")) }
        listOf("wait", "ruko", "ruk").forEach { put(it, listOf("\u23F3", "\u270B")) }
        listOf("fast", "speed", "tez").forEach { put(it, listOf("\u26A1", "\uD83C\uDFCE\uFE0F")) }
        listOf("slow", "sust").forEach { put(it, listOf("\uD83D\uDC22")) }

        // ----- tech / work -----
        listOf("email", "mail").forEach { put(it, listOf("\uD83D\uDCE7", "\u2709\uFE0F")) }
        listOf("message", "msg").forEach { put(it, listOf("\uD83D\uDCAC", "\uD83D\uDCE8")) }
        listOf("call", "calling").forEach { put(it, listOf("\uD83D\uDCDE", "\uD83D\uDCF1")) }
        listOf("work", "kaam", "kam").forEach { put(it, listOf("\uD83D\uDCBC")) }
        listOf("meeting").forEach { put(it, listOf("\uD83D\uDDD3\uFE0F", "\uD83E\uDD1D")) }
        listOf("idea").forEach { put(it, listOf("\uD83D\uDCA1")) }
        listOf("computer", "laptop", "pc").forEach { put(it, listOf("\uD83D\uDCBB")) }
        listOf("tv", "television").forEach { put(it, listOf("\uD83D\uDCFA")) }
        listOf("internet", "wifi").forEach { put(it, listOf("\uD83D\uDCF6", "\uD83D\uDCE1")) }

        // ----- religious / islamic -----
        listOf("allah").forEach { put(it, listOf("\uD83E\uDD32")) }
        listOf("inshallah", "insha").forEach { put(it, listOf("\uD83E\uDD32")) }
        listOf("mashallah", "masha").forEach { put(it, listOf("\uD83E\uDD32", "\u2764\uFE0F")) }
        listOf("alhamdulillah").forEach { put(it, listOf("\uD83E\uDD32", "\uD83D\uDE4F")) }
        listOf("dua", "prayer").forEach { put(it, listOf("\uD83E\uDD32", "\uD83D\uDE4F")) }
        listOf("masjid", "mosque").forEach { put(it, listOf("\uD83D\uDD4C")) }
        listOf("namaz").forEach { put(it, listOf("\uD83D\uDD4C", "\uD83E\uDD32")) }
        listOf("eid").forEach { put(it, listOf("\uD83C\uDF19", "\uD83C\uDF89")) }
        listOf("ramzan", "ramadan", "ramazan").forEach { put(it, listOf("\uD83C\uDF19")) }

        // ----- reactions / slang -----
        listOf("100", "hundred").forEach { put(it, listOf("\uD83D\uDCAF")) }
        listOf("power", "strong", "strength", "taqat").forEach { put(it, listOf("\uD83D\uDCAA")) }
        listOf("win", "winner", "jeet", "jeetna").forEach { put(it, listOf("\uD83C\uDFC6", "\uD83C\uDF89")) }
        listOf("loss", "loser", "haar").forEach { put(it, listOf("\uD83D\uDE1E", "\uD83D\uDC4E")) }
        listOf("fail").forEach { put(it, listOf("\uD83D\uDE1E", "\u274C")) }
        listOf("magic").forEach { put(it, listOf("\u2728", "\uD83E\uDE84")) }
        listOf("explode", "explosion").forEach { put(it, listOf("\uD83D\uDCA5", "\uD83D\uDCA3")) }
        listOf("eye", "ankh").forEach { put(it, listOf("\uD83D\uDC40", "\uD83D\uDC41\uFE0F")) }
        listOf("hand", "haath").forEach { put(it, listOf("\u270B", "\uD83D\uDC4B")) }
        listOf("clap", "clapping").forEach { put(it, listOf("\uD83D\uDC4F")) }
        listOf("warning", "danger").forEach { put(it, listOf("\u26A0\uFE0F", "\uD83D\uDEA8")) }
        listOf("info").forEach { put(it, listOf("\u2139\uFE0F")) }
        listOf("new").forEach { put(it, listOf("\uD83C\uDD95")) }
        listOf("alert").forEach { put(it, listOf("\uD83D\uDEA8")) }
        listOf("trophy").forEach { put(it, listOf("\uD83C\uDFC6")) }
        listOf("medal").forEach { put(it, listOf("\uD83E\uDD47", "\uD83C\uDFC5")) }

        // ----- misc -----
        listOf("hot", "garam").forEach { put(it, listOf("\uD83D\uDD25", "\uD83E\uDD75")) }
        listOf("cold", "thanda").forEach { put(it, listOf("\u2744\uFE0F", "\uD83E\uDD76")) }
        listOf("doctor").forEach { put(it, listOf("\uD83D\uDC68\u200D\u2695\uFE0F", "\uD83D\uDC8A")) }
        listOf("medicine", "dawai", "dawa").forEach { put(it, listOf("\uD83D\uDC8A", "\uD83D\uDC89")) }
        listOf("blood", "khoon").forEach { put(it, listOf("\uD83E\uDE78", "\u2764\uFE0F")) }
        listOf("ghost", "bhoot").forEach { put(it, listOf("\uD83D\uDC7B")) }
        listOf("alien").forEach { put(it, listOf("\uD83D\uDC7D")) }
        listOf("robot").forEach { put(it, listOf("\uD83E\uDD16")) }
        listOf("king", "badshah").forEach { put(it, listOf("\uD83D\uDC51", "\uD83E\uDD34")) }
        listOf("queen", "malka").forEach { put(it, listOf("\uD83D\uDC51", "\uD83D\uDC78")) }
        listOf("princess").forEach { put(it, listOf("\uD83D\uDC78")) }
        listOf("prince").forEach { put(it, listOf("\uD83E\uDD34")) }

        // ----- country flags -----
        listOf("pakistan", "pak").forEach { put(it, listOf("\uD83C\uDDF5\uD83C\uDDF0")) }
        listOf("india", "hind").forEach { put(it, listOf("\uD83C\uDDEE\uD83C\uDDF3")) }
    }

    /**
     * Returns up to [max] emoji suggestions for [keyword]. The match is
     * lower-cased and exact — no fuzzy / prefix matching, otherwise the
     * suggestion bar would surface unrelated emojis while the user is
     * still mid-word. Returns an empty list when nothing matches.
     */
    fun forKeyword(keyword: String, max: Int = 2): List<String> {
        if (keyword.isBlank()) return emptyList()
        val list = MAP[keyword.lowercase()] ?: return emptyList()
        return if (list.size <= max) list else list.take(max)
    }
}
