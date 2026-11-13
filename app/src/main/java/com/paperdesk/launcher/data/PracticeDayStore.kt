package com.paperdesk.launcher.data

import android.content.Context
import java.util.Calendar

/** Local daily practice flags feeding the Carbon dashboard week + streak. */
class PracticeDayStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun sandhya(date: String = PanchangaStore.dateKey()): Triple<Boolean, Boolean, Boolean> =
        Triple(
            get(date, SESSION_PRATAH),
            get(date, SESSION_MADHYAHNA),
            get(date, SESSION_SAYAM)
        )

    fun setSandhya(session: String, done: Boolean, date: String = PanchangaStore.dateKey()) {
        val s = normalizeSession(session)
        val ed = prefs.edit().putBoolean(key(date, s), done)
        if (done) ed.putBoolean(key(date, skipKey(s)), false)
        ed.apply()
        if (done) refreshBestStreak()
    }

    fun isSandhyaSessionDone(session: String, date: String = PanchangaStore.dateKey()): Boolean =
        get(date, normalizeSession(session))

    fun isSandhyaSkipped(session: String, date: String = PanchangaStore.dateKey()): Boolean =
        get(date, skipKey(normalizeSession(session))) && !isSandhyaSessionDone(session, date)

    /** Day actions for a Sandhya kala — complete and skipped are mutually exclusive. */
    fun setSandhyaDay(session: String, complete: Boolean, skipped: Boolean, date: String = PanchangaStore.dateKey()) {
        val s = normalizeSession(session)
        prefs.edit()
            .putBoolean(key(date, s), complete && !skipped)
            .putBoolean(key(date, skipKey(s)), skipped && !complete)
            .apply()
        if (complete && !skipped) refreshBestStreak()
    }

    fun sandhyaComplete(date: String = PanchangaStore.dateKey()): Boolean =
        sandhya(date).toList().all { it }

    fun stotra(id: String, date: String = PanchangaStore.dateKey()): Boolean = get(date, "stotra_$id")

    fun setStotra(id: String, done: Boolean, date: String = PanchangaStore.dateKey()) {
        prefs.edit().putBoolean(key(date, "stotra_$id"), done).apply()
    }

    fun japaComplete(date: String = PanchangaStore.dateKey()): Boolean = get(date, KEY_JAPA_COMPLETE)
    fun japaSkipped(date: String = PanchangaStore.dateKey()): Boolean = get(date, KEY_JAPA_SKIPPED)

    /** Day actions for the Japa session — mutually exclusive when set. */
    fun setJapaDay(complete: Boolean, skipped: Boolean, date: String = PanchangaStore.dateKey()) {
        prefs.edit()
            .putBoolean(key(date, KEY_JAPA_COMPLETE), complete)
            .putBoolean(key(date, KEY_JAPA_SKIPPED), skipped && !complete)
            .apply()
    }

    fun pujaComplete(date: String = PanchangaStore.dateKey()): Boolean = get(date, KEY_PUJA_COMPLETE)
    fun pujaSkipped(date: String = PanchangaStore.dateKey()): Boolean = get(date, KEY_PUJA_SKIPPED)

    /** Day actions for standalone Puja — mutually exclusive when set. */
    fun setPujaDay(complete: Boolean, skipped: Boolean, date: String = PanchangaStore.dateKey()) {
        prefs.edit()
            .putBoolean(key(date, KEY_PUJA_COMPLETE), complete)
            .putBoolean(key(date, KEY_PUJA_SKIPPED), skipped && !complete)
            .apply()
    }

    fun parayanaComplete(date: String = PanchangaStore.dateKey()): Boolean = get(date, KEY_PARAYANA_COMPLETE)
    fun parayanaSkipped(date: String = PanchangaStore.dateKey()): Boolean = get(date, KEY_PARAYANA_SKIPPED)

    /** Day actions for Nitya Parayana — mutually exclusive when set. */
    fun setParayanaDay(complete: Boolean, skipped: Boolean, date: String = PanchangaStore.dateKey()) {
        prefs.edit()
            .putBoolean(key(date, KEY_PARAYANA_COMPLETE), complete)
            .putBoolean(key(date, KEY_PARAYANA_SKIPPED), skipped && !complete)
            .apply()
    }

    fun currentStreak(): Int {
        var current = 0
        val cursor = Calendar.getInstance()
        if (!sandhyaComplete(PanchangaStore.dateKey(cursor))) {
            cursor.add(Calendar.DATE, -1)
        }
        while (sandhyaComplete(PanchangaStore.dateKey(cursor))) {
            current++
            cursor.add(Calendar.DATE, -1)
            if (current > 400) break
        }
        return current
    }

    fun bestStreak(): Int {
        val stored = prefs.getInt(KEY_BEST_STREAK, 0)
        val live = currentStreak()
        return maxOf(stored, live, computeBestStreak(400))
    }

    fun refreshBestStreak() {
        val best = maxOf(prefs.getInt(KEY_BEST_STREAK, 0), computeBestStreak(400), currentStreak())
        prefs.edit().putInt(KEY_BEST_STREAK, best).apply()
    }

    private fun computeBestStreak(days: Int): Int {
        var best = 0
        var run = 0
        val scan = Calendar.getInstance()
        repeat(days) {
            if (sandhyaComplete(PanchangaStore.dateKey(scan))) {
                run++
                best = maxOf(best, run)
            } else {
                run = 0
            }
            scan.add(Calendar.DATE, -1)
        }
        return best
    }

    private fun get(date: String, name: String) = prefs.getBoolean(key(date, name), false)
    private fun key(date: String, name: String) = "${date}_$name"
    private fun skipKey(session: String) = "sandhya_skip_$session"

    companion object {
        private const val PREFS = "madhvanidhi_practice_day"
        private const val KEY_BEST_STREAK = "best_sandhya_streak"
        private const val KEY_JAPA_COMPLETE = "japa_complete"
        private const val KEY_JAPA_SKIPPED = "japa_skipped"
        private const val KEY_PUJA_COMPLETE = "puja_complete"
        private const val KEY_PUJA_SKIPPED = "puja_skipped"
        private const val KEY_PARAYANA_COMPLETE = "parayana_complete"
        private const val KEY_PARAYANA_SKIPPED = "parayana_skipped"
        const val SESSION_PRATAH = "pratah"
        const val SESSION_MADHYAHNA = "madhyahna"
        const val SESSION_SAYAM = "sayam"

        fun normalizeSession(session: String): String = when (session) {
            SandhyaStore.MADHYA, "madhyahna", "madhyahna_sandhya" -> SESSION_MADHYAHNA
            SandhyaStore.PRATAH, "pratah_sandhya" -> SESSION_PRATAH
            SandhyaStore.SAYAM, "sayam_sandhya" -> SESSION_SAYAM
            else -> session
        }
    }
}
