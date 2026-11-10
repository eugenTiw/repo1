package com.eugentiw.fernziel.data

/** Lebensbereich eines Ziels. */
enum class Category { HEALTH, FINANCE, CAREER, EDUCATION, RELATIONSHIPS, PERSONAL, LEISURE, OTHER }

/** Zeitraum, auf den sich die Häufigkeit einer Handlung bezieht. */
enum class Period { DAILY, WEEKLY, MONTHLY }

/** Ein langfristiges Ziel. Tage werden als Epochentag gespeichert (LocalDate.toEpochDay()). */
data class Goal(
    val id: Long = 0,
    val title: String,
    val category: Category,
    val criterion: String = "",
    val targetDay: Long? = null,
    val createdDay: Long,
    val achievedDay: Long? = null,
)

/** Eine wiederkehrende Handlung, die zu einem Ziel führt. */
data class GoalAction(
    val id: Long = 0,
    val goalId: Long = 0,
    val title: String,
    val period: Period,
    val times: Int,
    val position: Int = 0,
)

/** Verknüpft eine Handlung mit einem Tag (erledigt oder eingeplant). */
data class DayRef(val actionId: Long, val day: Long)
