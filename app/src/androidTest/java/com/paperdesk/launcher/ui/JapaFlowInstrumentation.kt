package com.paperdesk.launcher.ui

import android.app.Activity
import android.app.Instrumentation
import android.os.Bundle
import com.paperdesk.launcher.data.JapaStore
import com.paperdesk.launcher.data.SecurePrefs
import java.util.UUID

/** Uses an isolated profile keyspace; never changes the user's profile or counts. */
class JapaFlowInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    override fun onStart() {
        val id = "count-test-${UUID.randomUUID()}"
        val prefix = "profile_${id}_"
        val prefs = SecurePrefs.get(targetContext, JapaStore.PREFS_ENCRYPTED)
        val result = Bundle()
        var code = Activity.RESULT_OK
        try {
            val store = JapaStore(targetContext, id)
            store.seedCounts("gayatri", 64, 12540)
            val target = store.counts("gayatri").dailyTarget
            store.setProcedurePhase("gayatri", 1)
            check(store.add(1, "gayatri").let { it.daily == 65L && it.cumulative == 12541L })
            check(store.undo("gayatri").let { it.daily == 64L && it.cumulative == 12540L })
            check(store.lastCountDelta("gayatri") == 0L)
            check(store.undo("gayatri").daily == 64L) // cannot repeatedly undo the same action
            store.add(108, "gayatri")
            check(JapaStore(targetContext, id).counts("gayatri").let { it.daily == 172L && it.cumulative == 12648L })
            check(store.counts("gayatri").dailyTarget == target)
            check(JapaStore(targetContext, id).lastCountDelta("gayatri") == 108L)
            check(store.undo("gayatri").let { it.daily == 64L && it.cumulative == 12540L })
            store.setProcedurePhase("gayatri", 2)
            store.setProcedurePhase("gayatri", 3)
            check(store.counts("gayatri").daily == 64L) // completion never fills or resets counts
            check(JapaStore(targetContext, id).procedurePhase("gayatri") == 3)
            check(store.procedurePhase("narayana") == 0)
            check(store.counts("narayana").daily == 0L)
            prefs.edit().putString(prefix + "m_gayatri_day", "2000-01-01").commit()
            check(store.counts("gayatri").let { it.daily == 0L && it.cumulative == 12540L })
            check(store.procedurePhase("gayatri") == 0)
            check(store.lastCountDelta("gayatri") == 0L)
            result.putString("stream", "PASS: tap, batch, undo, persisted counts/stage, target unchanged, mantra isolation, completion preservation, day rollover")
        } catch (error: Throwable) {
            result.putString("stream", android.util.Log.getStackTraceString(error))
            code = Activity.RESULT_CANCELED
        } finally {
            val edit = prefs.edit()
            prefs.all.keys.filter { it.startsWith(prefix) }.forEach { edit.remove(it) }
            edit.commit()
        }
        finish(code, result)
    }
}
