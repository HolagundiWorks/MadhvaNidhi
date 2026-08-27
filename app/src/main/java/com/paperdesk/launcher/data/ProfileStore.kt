package com.paperdesk.launcher.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class ProfileStore(private val context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    data class Profile(
        val id: String,
        val name: String,
        val vedaShakha: String,
        val gotra: String,
        val pravara: String,
        val nakshatra: String
    )

    fun profiles(): List<Profile> {
        val parsed = parse(prefs.getString("items", "[]") ?: "[]")
        if (parsed.isNotEmpty()) return parsed
        val first = Profile("default", "ಸಾಧಕ", VEDA_SHAKHAS.first(), GOTRAS.first(), "", NAKSHATRAS.first())
        saveItems(listOf(first))
        prefs.edit().putString("active", first.id).commit()
        return listOf(first)
    }

    fun active(): Profile {
        val all = profiles()
        val id = prefs.getString("active", all.first().id)
        return all.firstOrNull { it.id == id } ?: all.first().also { setActive(it.id) }
    }

    fun setActive(id: String) {
        if (profiles().any { it.id == id }) prefs.edit().putString("active", id).commit()
    }

    fun save(profile: Profile): Profile {
        val safe = profile.copy(
            id = profile.id.ifBlank { UUID.randomUUID().toString() },
            name = profile.name.trim().ifBlank { "ಸಾಧಕ" },
            vedaShakha = profile.vedaShakha.ifBlank { VEDA_SHAKHAS.first() },
            gotra = profile.gotra.ifBlank { GOTRAS.first() },
            pravara = profile.pravara.trim(),
            nakshatra = profile.nakshatra.ifBlank { NAKSHATRAS.first() }
        )
        val all = profiles().toMutableList()
        val index = all.indexOfFirst { it.id == safe.id }
        if (index >= 0) all[index] = safe else all.add(safe)
        saveItems(all)
        prefs.edit().putString("active", safe.id).commit()
        return safe
    }

    fun exportJson(): JSONObject = JSONObject()
        .put("active", active().id)
        .put("items", JSONArray().also { arr -> profiles().forEach { arr.put(toJson(it)) } })

    fun importJson(obj: JSONObject) {
        val list = parse(obj.optJSONArray("items")?.toString() ?: "[]")
        if (list.isEmpty()) return
        saveItems(list)
        val requested = obj.optString("active")
        prefs.edit().putString("active", list.firstOrNull { it.id == requested }?.id ?: list.first().id).commit()
    }

    private fun saveItems(items: List<Profile>) {
        prefs.edit().putString("items", JSONArray().also { arr -> items.forEach { arr.put(toJson(it)) } }.toString()).commit()
    }

    private fun parse(raw: String): List<Profile> = runCatching {
        val arr = JSONArray(raw)
        (0 until arr.length()).mapNotNull { i ->
            arr.optJSONObject(i)?.let { obj ->
                val id = obj.optString("id")
                if (id.isBlank()) null else Profile(
                    id,
                    obj.optString("name", "ಸಾಧಕ"),
                    obj.optString("vedaShakha", VEDA_SHAKHAS.first()),
                    obj.optString("gotra", GOTRAS.first()),
                    obj.optString("pravara", ""),
                    obj.optString("nakshatra", NAKSHATRAS.first())
                )
            }
        }
    }.getOrElse { emptyList() }

    private fun toJson(p: Profile) = JSONObject().put("id", p.id).put("name", p.name)
        .put("vedaShakha", p.vedaShakha).put("gotra", p.gotra).put("pravara", p.pravara).put("nakshatra", p.nakshatra)

    companion object {
        const val PREFS = "paperdesk_profiles"

        val VEDA_SHAKHAS = listOf(
            "ಋಗ್ವೇದ — ಆಶ್ವಲಾಯನ", "ಕೃಷ್ಣ ಯಜುರ್ವೇದ — ತೈತ್ತಿರೀಯ",
            "ಶುಕ್ಲ ಯಜುರ್ವೇದ", "ಸಾಮವೇದ", "ಅಥರ್ವವೇದ", "ತಿಳಿಸಿಲ್ಲ"
        )
        val GOTRAS = listOf(
            "ತಿಳಿಸಿಲ್ಲ", "ಅತ್ರಿ", "ಅಂಗೀರಸ", "ಭಾರದ್ವಾಜ", "ಕಶ್ಯಪ", "ಕೌಂಡಿನ್ಯ", "ಕೌಶಿಕ",
            "ಗೌತಮ", "ಜಮದಗ್ನಿ", "ಪರಾಶರ", "ವತ್ಸ", "ವಸಿಷ್ಠ", "ವಿಶ್ವಾಮಿತ್ರ", "ಶಾಂಡಿಲ್ಯ", "ಶ್ರೀವತ್ಸ", "ಹರಿತ"
        )
        val NAKSHATRAS = listOf(
            "ತಿಳಿಸಿಲ್ಲ", "ಅಶ್ವಿನಿ", "ಭರಣಿ", "ಕೃತ್ತಿಕಾ", "ರೋಹಿಣಿ", "ಮೃಗಶಿರ", "ಆರ್ದ್ರಾ", "ಪುನರ್ವಸು",
            "ಪುಷ್ಯ", "ಆಶ್ಲೇಷಾ", "ಮಘಾ", "ಪೂರ್ವ ಫಲ್ಗುಣಿ", "ಉತ್ತರ ಫಲ್ಗುಣಿ", "ಹಸ್ತ", "ಚಿತ್ರಾ", "ಸ್ವಾತಿ",
            "ವಿಶಾಖಾ", "ಅನುರಾಧಾ", "ಜ್ಯೇಷ್ಠಾ", "ಮೂಲ", "ಪೂರ್ವಾಷಾಢಾ", "ಉತ್ತರಾಷಾಢಾ", "ಶ್ರವಣ",
            "ಧನಿಷ್ಠಾ", "ಶತಭಿಷಾ", "ಪೂರ್ವಭಾದ್ರಪದಾ", "ಉತ್ತರಭಾದ್ರಪದಾ", "ರೇವತಿ"
        )
    }
}
