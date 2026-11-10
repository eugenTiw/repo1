package com.eugentiw.fernziel.reminders

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.eugentiw.fernziel.MainActivity
import com.eugentiw.fernziel.R
import com.eugentiw.fernziel.data.DayRef
import com.eugentiw.fernziel.data.Db
import com.eugentiw.fernziel.data.Goal
import com.eugentiw.fernziel.data.GoalAction
import com.eugentiw.fernziel.data.Period
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Lokale Erinnerungen. Es wird immer nur ein einziger Alarm für den nächsten fälligen
 * Zeitpunkt gesetzt; beim Auslösen werden die fälligen Benachrichtigungen angezeigt und
 * der nächste Alarm geplant. Alles geschieht auf dem Gerät, ohne Netzwerk.
 *
 * Der Alarm ist bewusst ungenau (keine Sonderberechtigung nötig) und kann
 * daher einige Minuten nach der gewählten Uhrzeit eintreffen.
 */
object Reminders {

    private const val CHANNEL = "reminders"
    private const val PREFS = "reminders"
    private const val KEY_LAST_CHECK = "last_check"
    private const val ACTION_FIRE = "com.eugentiw.fernziel.REMINDER"

    /** Ältere verpasste Erinnerungen (z. B. nach langem Ausschalten) werden nicht nachgereicht. */
    private const val MAX_DELAY_MILLIS = 12L * 60L * 60L * 1000L

    /**
     * Aus der App heraus nach jeder Änderung: plant den nächsten Alarm neu.
     * [actions] sind die Handlungen aller noch nicht erreichten Ziele.
     */
    fun sync(context: Context, actions: List<GoalAction>) {
        val now = System.currentTimeMillis()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putLong(KEY_LAST_CHECK, now).apply()
        scheduleNext(context, actions, now)
    }

    /** Beim Auslösen des Alarms, nach einem Neustart oder einer Zeitumstellung. */
    fun onAlarm(context: Context) {
        val db = Db(context)
        try {
            val goals: Map<Long, Goal> = db.loadGoals().associateBy { it.id }
            val actions = db.loadActions().filter { action ->
                val goal = goals[action.goalId]
                action.reminderMinutes != null && goal != null && goal.achievedDay == null
            }

            val nowMillis = System.currentTimeMillis()
            val zone = ZoneId.systemDefault()
            val now = LocalDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), zone)
            val today = now.toLocalDate().toEpochDay()
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val lastCheck = prefs.getLong(KEY_LAST_CHECK, nowMillis)
            val doneToday = db.loadRefs(Db.COMPLETIONS, today, today)

            for (action in actions) {
                val minutes = action.reminderMinutes ?: continue
                val goal = goals[action.goalId] ?: continue
                val occurrence = ReminderTimes.lastOccurrence(action.period, minutes, now)
                    .atZone(zone).toInstant().toEpochMilli()
                val due = occurrence > lastCheck && nowMillis - occurrence < MAX_DELAY_MILLIS
                if (!due) continue
                // Tägliche Handlung schon abgehakt: keine Erinnerung nötig.
                if (action.period == Period.DAILY && doneToday.contains(DayRef(action.id, today))) continue
                show(context, action, goal)
            }

            prefs.edit().putLong(KEY_LAST_CHECK, nowMillis).apply()
            scheduleNext(context, actions, nowMillis)
        } finally {
            db.close()
        }
    }

    private fun alarmIntent(context: Context): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(ACTION_FIRE)
        return PendingIntent.getBroadcast(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun scheduleNext(context: Context, actions: List<GoalAction>, nowMillis: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = alarmIntent(context)
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), zone)

        var next: Long? = null
        for (action in actions) {
            val minutes = action.reminderMinutes ?: continue
            val time = ReminderTimes.nextOccurrence(action.period, minutes, now)
                .atZone(zone).toInstant().toEpochMilli()
            val current = next
            if (current == null || time < current) {
                next = time
            }
        }

        val trigger = next
        if (trigger == null) {
            alarmManager.cancel(pending)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
        }
    }

    private fun show(context: Context, action: GoalAction, goal: Goal) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL,
                context.getString(R.string.channel_reminders),
                NotificationManager.IMPORTANCE_DEFAULT,
            )
        )

        val title: String = when (action.period) {
            Period.DAILY -> action.title
            Period.WEEKLY -> context.getString(R.string.notif_week_title)
            Period.MONTHLY -> context.getString(R.string.notif_month_title)
        }
        val text: String = when (action.period) {
            Period.DAILY -> context.getString(R.string.notif_daily_text, goal.title)
            Period.WEEKLY -> context.getString(R.string.notif_week_text, action.title, action.times)
            Period.MONTHLY -> context.getString(R.string.notif_month_text, action.title, action.times)
        }

        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_target)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()

        // Ohne erteilte Benachrichtigungs-Berechtigung verwirft das System den Aufruf stillschweigend.
        manager.notify(action.id.toInt(), notification)
    }
}

/** Empfängt den Alarm sowie Neustart, App-Update und Zeitumstellung und plant neu. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            Reminders.onAlarm(context.applicationContext)
        } catch (e: Exception) {
            // Eine fehlgeschlagene Erinnerung darf die App nicht zum Absturz bringen.
        }
    }
}
