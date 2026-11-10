package com.eugentiw.fernziel.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Lokale SQLite-Datenbank. Alle Daten liegen ausschließlich im privaten
 * App-Speicher des Geräts; es gibt keinerlei Netzwerkzugriff.
 */
class Db(context: Context) : SQLiteOpenHelper(context.applicationContext, "fernziel.db", null, 1) {

    companion object {
        const val COMPLETIONS = "completions"
        const val PLANS = "plans"
    }

    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE goals(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT NOT NULL, " +
                "category TEXT NOT NULL, " +
                "criterion TEXT NOT NULL DEFAULT '', " +
                "target_day INTEGER, " +
                "created_day INTEGER NOT NULL, " +
                "achieved_day INTEGER)"
        )
        db.execSQL(
            "CREATE TABLE actions(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "goal_id INTEGER NOT NULL REFERENCES goals(id) ON DELETE CASCADE, " +
                "title TEXT NOT NULL, " +
                "period TEXT NOT NULL, " +
                "times INTEGER NOT NULL, " +
                "position INTEGER NOT NULL DEFAULT 0)"
        )
        db.execSQL(
            "CREATE TABLE completions(" +
                "action_id INTEGER NOT NULL REFERENCES actions(id) ON DELETE CASCADE, " +
                "day INTEGER NOT NULL, " +
                "PRIMARY KEY(action_id, day))"
        )
        db.execSQL(
            "CREATE TABLE plans(" +
                "action_id INTEGER NOT NULL REFERENCES actions(id) ON DELETE CASCADE, " +
                "day INTEGER NOT NULL, " +
                "PRIMARY KEY(action_id, day))"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Version 1: noch keine Migrationen.
    }

    fun loadGoals(): List<Goal> {
        val out = ArrayList<Goal>()
        readableDatabase.rawQuery(
            "SELECT id, title, category, criterion, target_day, created_day, achieved_day " +
                "FROM goals ORDER BY created_day, id",
            null,
        ).use { c ->
            while (c.moveToNext()) {
                out.add(
                    Goal(
                        id = c.getLong(0),
                        title = c.getString(1),
                        category = categoryOf(c.getString(2)),
                        criterion = c.getString(3),
                        targetDay = if (c.isNull(4)) null else c.getLong(4),
                        createdDay = c.getLong(5),
                        achievedDay = if (c.isNull(6)) null else c.getLong(6),
                    )
                )
            }
        }
        return out
    }

    fun loadActions(): List<GoalAction> {
        val out = ArrayList<GoalAction>()
        readableDatabase.rawQuery(
            "SELECT id, goal_id, title, period, times, position " +
                "FROM actions ORDER BY goal_id, position, id",
            null,
        ).use { c ->
            while (c.moveToNext()) {
                out.add(
                    GoalAction(
                        id = c.getLong(0),
                        goalId = c.getLong(1),
                        title = c.getString(2),
                        period = periodOf(c.getString(3)),
                        times = c.getInt(4),
                        position = c.getInt(5),
                    )
                )
            }
        }
        return out
    }

    /** Lädt Erledigungen bzw. Planungen im Tagesbereich [fromDay, toDay]. */
    fun loadRefs(table: String, fromDay: Long, toDay: Long): Set<DayRef> {
        val out = HashSet<DayRef>()
        readableDatabase.rawQuery(
            "SELECT action_id, day FROM $table WHERE day BETWEEN $fromDay AND $toDay",
            null,
        ).use { c ->
            while (c.moveToNext()) {
                out.add(DayRef(c.getLong(0), c.getLong(1)))
            }
        }
        return out
    }

    /** Legt ein Ziel an oder aktualisiert es, samt seiner Handlungen. */
    fun writeGoal(goal: Goal, actions: List<GoalAction>): Long {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val values = ContentValues()
            values.put("title", goal.title)
            values.put("category", goal.category.name)
            values.put("criterion", goal.criterion)
            values.put("target_day", goal.targetDay)
            values.put("created_day", goal.createdDay)

            val goalId: Long
            if (goal.id == 0L) {
                goalId = db.insertOrThrow("goals", null, values)
            } else {
                goalId = goal.id
                db.update("goals", values, "id = ?", arrayOf(goalId.toString()))
            }

            val keep = actions.filter { it.id != 0L }.map { it.id }.toSet()
            val existing = ArrayList<Long>()
            db.rawQuery("SELECT id FROM actions WHERE goal_id = $goalId", null).use { c ->
                while (c.moveToNext()) existing.add(c.getLong(0))
            }
            for (id in existing) {
                if (id !in keep) db.delete("actions", "id = ?", arrayOf(id.toString()))
            }

            actions.forEachIndexed { index, action ->
                val v = ContentValues()
                v.put("goal_id", goalId)
                v.put("title", action.title)
                v.put("period", action.period.name)
                v.put("times", action.times)
                v.put("position", index)
                if (action.id == 0L) {
                    db.insertOrThrow("actions", null, v)
                } else {
                    db.update("actions", v, "id = ?", arrayOf(action.id.toString()))
                }
            }

            db.setTransactionSuccessful()
            return goalId
        } finally {
            db.endTransaction()
        }
    }

    fun removeGoal(goalId: Long) {
        writableDatabase.delete("goals", "id = ?", arrayOf(goalId.toString()))
    }

    fun markAchieved(goalId: Long, day: Long?) {
        val values = ContentValues()
        values.put("achieved_day", day)
        writableDatabase.update("goals", values, "id = ?", arrayOf(goalId.toString()))
    }

    /** Schaltet eine Erledigung bzw. Planung für einen Tag um. */
    fun toggleRef(table: String, actionId: Long, day: Long) {
        val db = writableDatabase
        val removed = db.delete(
            table,
            "action_id = ? AND day = ?",
            arrayOf(actionId.toString(), day.toString()),
        )
        if (removed == 0) {
            val v = ContentValues()
            v.put("action_id", actionId)
            v.put("day", day)
            db.insertWithOnConflict(table, null, v, SQLiteDatabase.CONFLICT_IGNORE)
        }
    }

    private fun categoryOf(name: String): Category =
        Category.entries.firstOrNull { it.name == name } ?: Category.OTHER

    private fun periodOf(name: String): Period =
        Period.entries.firstOrNull { it.name == name } ?: Period.DAILY
}
