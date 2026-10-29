package ntmt.schedule.data

data class Lesson(val n: String?, val a: String?, val p: String?, val g: String? = null)

data class Catalog(
    val updateDt: String,
    val semesterId: Int,
    val semesterTitle: String,
    val groups: List<String>,
    val weekTypeNumerator: Boolean,
    val todayName: String,
    val todayIso: String,
    val isoWeek: Int,
    /** Хэш содержимого: сайт может поменять пары, не меняя дату обновления */
    val hash: String = "",
) {
    /** Метка версии расписания: дата с сайта и хэш содержимого */
    val stamp: String get() = "$updateDt#$hash"
}

typealias GroupWeeks = List<Map<String, Map<String, List<Lesson>>>>
