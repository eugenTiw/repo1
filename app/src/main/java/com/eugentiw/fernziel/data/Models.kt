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
    /** Tag, ab dem die Handlung verfolgt wird. Frühere Tage gelten nicht als verpasst. */
    val createdDay: Long = 0,
    /** Uhrzeit der freiwilligen Erinnerung in Minuten seit Mitternacht, oder null. */
    val reminderMinutes: Int? = null,
    /** Selbst festgelegte Stufen, von leicht nach schwer. Leer heißt: keine Level. */
    val levels: List<Level> = emptyList(),
    /** Tag, ab dem die Level zählen. Früherer Verlauf wird für den Aufstieg nicht gewertet. */
    val levelsSince: Long? = null,
)

/**
 * Eine Stufe einer Handlung, z. B. „Vor 8:00 aufstehen".
 * [length]: wie viele Zeiträume (Tage, Wochen oder Monate) am Stück nötig sind, um
 * aufzusteigen. Beim höchsten Level hat der Wert keine Bedeutung: dort bleibt man.
 */
data class Level(val title: String, val length: Int)

/**
 * Eigenes To-do für einen bestimmten Tag, unabhängig von Zielen.
 * [failed] heißt: ausdrücklich als nicht geschafft markiert. [done] und [failed] sind nie beide wahr.
 */
data class Todo(
    val id: Long = 0,
    val title: String,
    val day: Long,
    val done: Boolean = false,
    val failed: Boolean = false,
)

/** Verknüpft eine Handlung mit einem Tag (erledigt, nicht geschafft oder eingeplant). */
data class DayRef(val actionId: Long, val day: Long)
