package ntmt.schedule.widget

/**
 * Перенос названия предмета в виджете: первая строка стоит рядом с плашкой,
 * остаток уходит на следующие строки. Разрыв только между словами; предлог,
 * союз или частица не остаются в конце строки; шифр и его номер («ПМ 03»,
 * «МДК 03.01») не разделяются; слово, которое не помещается целиком,
 * переносится по слогам.
 */
internal object WidgetWrap {
    private const val NBSP = ' '
    private const val VOWELS = "аеёиоуыэюяАЕЁИОУЫЭЮЯ"
    private const val NO_START = "ьъйыЬЪЙЫ"
    private val SHORT = setOf(
        "в", "во", "и", "а", "но", "да", "на", "по", "с", "со", "к", "ко", "о", "об", "у",
        "за", "из", "от", "до", "для", "при", "без", "над", "под", "про", "не", "ни",
        "или", "как", "что", "их", "его", "её", "ее",
    )

    /** Неразрывные пробелы там, где строку рвать нельзя. */
    fun glue(text: String): String {
        val words = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.size < 2) return words.joinToString("")
        val out = StringBuilder(words[0])
        for (i in 1 until words.size) {
            val prev = words[i - 1]
            val cur = words[i]
            val keep = prev.trim('(', '«', '"').lowercase() in SHORT ||
                (cur.first().isDigit() && prev.none { it.isDigit() } && prev.length <= 5) ||
                cur == "-" || cur == "–" || cur == "—"
            out.append(if (keep) NBSP else ' ').append(cur)
        }
        return out.toString()
    }

    fun split(title: String, avail: Float, min: Float, width: (String) -> Float): Pair<String, String> {
        val glued = glue(title)
        if (avail <= min) return "" to glued
        if (width(glued) <= avail) return glued to ""
        val chunks = glued.split(' ')
        var head = ""
        var used = 0
        for (chunk in chunks) {
            val next = if (head.isEmpty()) chunk else "$head $chunk"
            if (width(next) > avail) break
            head = next
            used++
        }
        if (used == chunks.size) return glued to ""
        val rest = chunks.drop(used)
        // Свободное место в первой строке заполняем частью следующего слова по слогам.
        val word = rest.first()
        if (NBSP !in word || used == 0) {
            val prefix = if (head.isEmpty()) "" else "$head "
            val first = word.substringBefore(NBSP)
            val cut = syllableCut(first) { width("$prefix$it-") <= avail }
            if (cut > 0 && (used == 0 || first.length >= 9)) {
                val tail = (listOf(word.substring(cut)) + rest.drop(1)).joinToString(" ")
                return "$prefix${first.substring(0, cut)}-" to tail
            }
        }
        return head to rest.joinToString(" ")
    }

    /** Самая длинная допустимая часть слова, после которой можно поставить перенос; 0 = нельзя. */
    private fun syllableCut(word: String, fits: (String) -> Boolean): Int {
        if (word.length < 5 || word.any { !it.isLetter() }) return 0
        for (i in word.length - 3 downTo 2) {
            val left = word.substring(0, i)
            val right = word.substring(i)
            if (left.none { it in VOWELS } || right.none { it in VOWELS }) continue
            if (right[0] in NO_START) continue
            val l = left.last()
            val ok = l in VOWELS ||                                   // «ма-тематика»
                (l !in VOWELS && right[0] !in VOWELS && left[left.length - 2] in VOWELS) // «матема-ти-ка», «програм-мирование»
            if (!ok) continue
            // Не отрываем согласную от следующей за ней гласной: «прог-раммирование» можно, «програ-ммирование» нет.
            if (l in VOWELS && right.length > 1 && right[0] !in VOWELS && right[1] !in VOWELS &&
                right[1] !in NO_START && right[0] == right[1]
            ) continue
            if (fits(left)) return i
        }
        return 0
    }
}
