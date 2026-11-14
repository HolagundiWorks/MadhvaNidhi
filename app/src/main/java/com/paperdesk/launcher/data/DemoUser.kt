package com.paperdesk.launcher.data

import android.content.Context
import java.util.Calendar

/**
 * Ships a complete demo sadhaka profile plus practice / japa / puja history so
 * Dashboard, Sankalpa (Sandhya + Puja Krama), and Japa all resolve real values.
 */
object DemoUser {
    const val PROFILE_ID = "demo_sadhaka"
    private const val META_PREFS = "madhvanidhi_demo_user"
    private const val KEY_SEEDED = "seeded_v1"

    fun ensure(context: Context) {
        val app = context.applicationContext
        val meta = app.getSharedPreferences(META_PREFS, Context.MODE_PRIVATE)
        if (meta.getBoolean(KEY_SEEDED, false) &&
            ProfileStore(app).profiles().any { it.id == PROFILE_ID }
        ) {
            return
        }

        val profiles = ProfileStore(app)
        val demo = demoProfile()
        profiles.save(demo)
        profiles.setActive(PROFILE_ID)

        val settings = SettingsStore(app)
        settings.setDashboardJapaSlots(
            listOf("gayatri", "narayana", "rama", "hanuman", "raghavendra")
        )
        settings.setDashboardShortcuts(listOf("desk", "japa", "puja"))

        val practice = PracticeDayStore(app)
        seedPractice(practice)

        val japa = JapaStore(app, PROFILE_ID)
        seedJapa(japa)

        meta.edit().putBoolean(KEY_SEEDED, true).apply()
    }

    fun demoProfile(): Profile = Profile(
        id = PROFILE_ID,
        name = "ಶ್ರೀಧರ ರಾವ್",
        vedaShakha = "ಋಗ್ವೇದ — ಆಶ್ವಲಾಯನ",
        gotra = "ಭಾರದ್ವಾಜ",
        pravara = "ಆಂಗಿರಸ ಭಾರದ್ವಾಜ",
        rashi = "ಮಿಥುನ",
        nakshatra = "ಮೃಗಶಿರ",
        yearOfBirth = 1988,
        placeOfBirth = "ಮಣಿಪಾಲ · ಉಡುಪಿ",
        marital = "married",
        matha = "Sri Raghavendra Swamy Matha",
        dikshaMantras = listOf(
            "gayatri",
            "narayana",
            "rama",
            "hanuman",
            "raghavendra",
            "vasudeva"
        ),
        japaTargets = mapOf(
            "gayatri" to Pair(101L, 50_001L),
            "narayana" to Pair(101L, 10_001L),
            "rama" to Pair(101L, 5_001L),
            "hanuman" to Pair(11L, 5_001L),
            "raghavendra" to Pair(101L, 5_001L),
            "vasudeva" to Pair(11L, JapaGoals.SANKALPA_AUTO)
        )
    )

    private fun seedPractice(practice: PracticeDayStore) {
        val today = Calendar.getInstance()
        // Today: Sandhya 2/3, japa complete, puja pending, parayana complete.
        practice.setSandhya(PracticeDayStore.SESSION_PRATAH, true)
        practice.setSandhya(PracticeDayStore.SESSION_MADHYAHNA, true)
        practice.setSandhya(PracticeDayStore.SESSION_SAYAM, false)
        practice.setJapaDay(complete = true, skipped = false)
        practice.setPujaDay(complete = false, skipped = false)
        practice.setParayanaDay(complete = true, skipped = false)
        practice.setStotra("hari_vayu", true)
        practice.setStotra("rama_dwadasha", true)

        // Prior 20 days: full Sandhya streak material for badges + Puja every other day.
        val cursor = today.clone() as Calendar
        repeat(20) { day ->
            cursor.add(Calendar.DATE, -1)
            val key = PanchangaStore.dateKey(cursor)
            practice.setSandhya(PracticeDayStore.SESSION_PRATAH, true, key)
            practice.setSandhya(PracticeDayStore.SESSION_MADHYAHNA, true, key)
            practice.setSandhya(PracticeDayStore.SESSION_SAYAM, true, key)
            practice.setJapaDay(complete = true, skipped = false, date = key)
            practice.setPujaDay(complete = day % 2 == 0, skipped = false, date = key)
            practice.setParayanaDay(complete = day % 3 != 0, skipped = false, date = key)
        }
        practice.refreshBestStreak()
    }

    private fun seedJapa(japa: JapaStore) {
        japa.seedCounts("gayatri", daily = 64, cumulative = 12_540)
        japa.seedCounts("narayana", daily = 101, cumulative = 5_001)
        japa.seedCounts("rama", daily = 36, cumulative = 1_108)
        japa.seedCounts("hanuman", daily = 11, cumulative = 540)
        japa.seedCounts("raghavendra", daily = 21, cumulative = 2_208)
        japa.seedCounts("vasudeva", daily = 11, cumulative = 308)
        japa.setActiveMantraId("gayatri")
    }
}
