package ntmt.schedule.data

data class Lesson(val n: String?, val a: String?, val p: String?)

data class Catalog(
    val updateDt: String,
    val semesterId: Int,
    val semesterTitle: String,
    val groups: List<String>,
    val weekTypeNumerator: Boolean,
    val todayName: String,
    val todayIso: String,
    val isoWeek: Int,
)

typealias GroupWeeks = List<Map<String, Map<String, List<Lesson>>>>
