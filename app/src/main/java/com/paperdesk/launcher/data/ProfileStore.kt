package com.paperdesk.launcher.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class Profile(
    val id: String,
    val name: String,
    val vedaShakha: String,
    val gotra: String,
    val pravara: String,
    val rashi: String,
    val nakshatra: String,
    val yearOfBirth: Int,
    val placeOfBirth: String
)

/**
 * Local-only sadhaka profiles. Stored with EncryptedSharedPreferences
 * (AES-256). No online sync.
 */
class ProfileStore(context: Context) {
    private val app = context.applicationContext
    private val prefs: SharedPreferences by lazy { encryptedPrefs() }

    private fun encryptedPrefs(): SharedPreferences {
        migratePlaintextIfNeeded()
        return createEncrypted()
    }

    private fun createEncrypted(): SharedPreferences {
        val masterKey = MasterKey.Builder(app)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            app,
            PREFS_ENCRYPTED,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun migratePlaintextIfNeeded() {
        val plain = app.getSharedPreferences(PREFS_PLAIN, Context.MODE_PRIVATE)
        val hasPlain = plain.contains(KEY_ITEMS) || plain.contains(KEY_ACTIVE) ||
            plain.contains("profiles") || plain.contains("active_id")
        if (!hasPlain) return
        val encrypted = createEncrypted()
        if (!encrypted.contains(KEY_ITEMS) && !encrypted.contains("profiles")) {
            val items = plain.getString(KEY_ITEMS, null)
                ?: plain.getString("profiles", null)
            val active = plain.getString(KEY_ACTIVE, null)
                ?: plain.getString("active_id", null)
            encrypted.edit()
                .putString(KEY_ITEMS, items)
                .putString(KEY_ACTIVE, active)
                .commit()
        }
        plain.edit().clear().commit()
    }

    fun profiles(): List<Profile> {
        val parsed = parse(prefs.getString(KEY_ITEMS, "[]") ?: "[]")
        if (parsed.isNotEmpty()) return parsed
        val first = defaultProfile("default")
        saveItems(listOf(first))
        prefs.edit().putString(KEY_ACTIVE, first.id).commit()
        return listOf(first)
    }

    fun all(): List<Profile> = profiles()

    fun active(): Profile {
        val all = profiles()
        val id = prefs.getString(KEY_ACTIVE, all.first().id)
        return all.firstOrNull { it.id == id } ?: all.first().also { setActive(it.id) }
    }

    fun setActive(id: String) {
        if (profiles().any { it.id == id }) prefs.edit().putString(KEY_ACTIVE, id).commit()
    }

    fun save(profile: Profile): Profile {
        val safe = profile.copy(
            id = profile.id.ifBlank { UUID.randomUUID().toString() },
            name = profile.name.trim().ifBlank { "ಸಾಧಕ" },
            vedaShakha = profile.vedaShakha.ifBlank { VEDA_SHAKHAS.first() },
            gotra = profile.gotra.ifBlank { GOTRAS.first() },
            pravara = profile.pravara.trim(),
            rashi = profile.rashi.ifBlank { RASHIS.first() },
            nakshatra = profile.nakshatra.ifBlank { NAKSHATRAS.first() },
            yearOfBirth = profile.yearOfBirth.coerceAtLeast(0),
            placeOfBirth = profile.placeOfBirth.trim()
        )
        val all = profiles().toMutableList()
        val index = all.indexOfFirst { it.id == safe.id }
        if (index >= 0) all[index] = safe else all.add(safe)
        saveItems(all)
        prefs.edit().putString(KEY_ACTIVE, safe.id).commit()
        return safe
    }

    fun upsert(profile: Profile) {
        save(profile)
    }

    fun exportJson(): JSONObject = JSONObject()
        .put("active", active().id)
        .put("items", JSONArray().also { arr -> profiles().forEach { arr.put(toJson(it)) } })

    fun importJson(obj: JSONObject) {
        val list = parse(obj.optJSONArray("items")?.toString() ?: "[]")
        if (list.isEmpty()) return
        saveItems(list)
        val requested = obj.optString("active")
        prefs.edit()
            .putString(KEY_ACTIVE, list.firstOrNull { it.id == requested }?.id ?: list.first().id)
            .commit()
    }

    private fun saveItems(items: List<Profile>) {
        prefs.edit()
            .putString(KEY_ITEMS, JSONArray().also { arr -> items.forEach { arr.put(toJson(it)) } }.toString())
            .commit()
    }

    private fun defaultProfile(id: String) = Profile(
        id = id,
        name = "ಸಾಧಕ",
        vedaShakha = VEDA_SHAKHAS.first(),
        gotra = GOTRAS.first(),
        pravara = "",
        rashi = RASHIS.first(),
        nakshatra = NAKSHATRAS.first(),
        yearOfBirth = 0,
        placeOfBirth = ""
    )

    private fun parse(raw: String): List<Profile> = runCatching {
        val arr = JSONArray(raw)
        (0 until arr.length()).mapNotNull { i ->
            arr.optJSONObject(i)?.let { obj ->
                val id = obj.optString("id")
                if (id.isBlank()) null else Profile(
                    id = id,
                    name = obj.optString("name", "ಸಾಧಕ"),
                    vedaShakha = obj.optString("vedaShakha", VEDA_SHAKHAS.first()),
                    gotra = obj.optString("gotra", GOTRAS.first()),
                    pravara = obj.optString("pravara", ""),
                    rashi = obj.optString("rashi", RASHIS.first()),
                    nakshatra = obj.optString("nakshatra", NAKSHATRAS.first()),
                    yearOfBirth = obj.optInt("yearOfBirth", 0),
                    placeOfBirth = obj.optString("placeOfBirth", "")
                )
            }
        }
    }.getOrElse { emptyList() }

    private fun toJson(p: Profile) = JSONObject()
        .put("id", p.id)
        .put("name", p.name)
        .put("vedaShakha", p.vedaShakha)
        .put("gotra", p.gotra)
        .put("pravara", p.pravara)
        .put("rashi", p.rashi)
        .put("nakshatra", p.nakshatra)
        .put("yearOfBirth", p.yearOfBirth)
        .put("placeOfBirth", p.placeOfBirth)

    companion object {
        private const val PREFS_PLAIN = "paperdesk_profiles"
        private const val PREFS_ENCRYPTED = "madhvanidhi_profiles_encrypted"
        private const val KEY_ITEMS = "items"
        private const val KEY_ACTIVE = "active"

        val VEDA_SHAKHAS = listOf(
            "ತಿಳಿಸಿಲ್ಲ",
            "ಋಗ್ವೇದ — ಆಶ್ವಲಾಯನ",
            "ಋಗ್ವೇದ — ಶಾಂಖಾಯನ",
            "ಕೃಷ್ಣ ಯಜುರ್ವೇದ — ತೈತ್ತಿರೀಯ",
            "ಕೃಷ್ಣ ಯಜುರ್ವೇದ — ಮೈತ್ರಾಯಣೀಯ",
            "ಶುಕ್ಲ ಯಜುರ್ವೇದ — ವಾಜಸನೇಯಿ (ಮಾಧ್ಯಂದಿನ)",
            "ಶುಕ್ಲ ಯಜುರ್ವೇದ — ಕಾಣ್ವ",
            "ಸಾಮವೇದ — ಕೌಥುಮ",
            "ಸಾಮವೇದ — ರಾಣಾಯನೀಯ",
            "ಸಾಮವೇದ — ಜೈಮಿನೀಯ",
            "ಅಥರ್ವವೇದ — ಶೌನಕ",
            "ಅಥರ್ವವೇದ — ಪೈಪ್ಪಲಾದ"
        )

        val GOTRAS = listOf(
            "ತಿಳಿಸಿಲ್ಲ",
            "ಭಾರದ್ವಾಜ",
            "ಕಶ್ಯಪ",
            "ವಸಿಷ್ಠ",
            "ವಿಶ್ವಾಮಿತ್ರ",
            "ಗೌತಮ",
            "ಅತ್ರಿ",
            "ಅಂಗಿರಸ",
            "ಅಗಸ್ತ್ಯ",
            "ಜಮದಗ್ನಿ",
            "ಕೌಂಡಿನ್ಯ",
            "ಹಾರೀತ",
            "ಶಾಂಡಿಲ್ಯ",
            "ಕೌಶಿಕ",
            "ಮೌದ್ಗಲ್ಯ",
            "ವತ್ಸ",
            "ಧನಂಜಯ",
            "ಪರಾಶರ",
            "ಭೃಗು",
            "ಸಂಕೃತಿ",
            "ಇತರೆ"
        )

        val RASHIS = listOf(
            "ತಿಳಿಸಿಲ್ಲ",
            "ಮೇಷ",
            "ವೃಷಭ",
            "ಮಿಥುನ",
            "ಕರ್ಕಾಟಕ",
            "ಸಿಂಹ",
            "ಕನ್ಯಾ",
            "ತುಲಾ",
            "ವೃಶ್ಚಿಕ",
            "ಧನು",
            "ಮಕರ",
            "ಕುಂಭ",
            "ಮೀನ"
        )

        val NAKSHATRAS = listOf(
            "ತಿಳಿಸಿಲ್ಲ",
            "ಅಶ್ವಿನಿ",
            "ಭರಣಿ",
            "ಕೃತ್ತಿಕಾ",
            "ರೋಹಿಣಿ",
            "ಮೃಗಶಿರ",
            "ಆರಿದ್ರಾ",
            "ಪುನರ್ವಸು",
            "ಪುಷ್ಯ",
            "ಆಶ್ಲೇಷಾ",
            "ಮಘಾ",
            "ಪೂರ್ವ ಫಲ್ಗುಣಿ",
            "ಉತ್ತರ ಫಲ್ಗುಣಿ",
            "ಹಸ್ತ",
            "ಚಿತ್ತಾ",
            "ಸ್ವಾತಿ",
            "ವಿಶಾಖಾ",
            "ಅನುರಾಧಾ",
            "ಜ್ಯೇಷ್ಠಾ",
            "ಮೂಲ",
            "ಪೂರ್ವಾಷಾಢಾ",
            "ಉತ್ತರಾಷಾಢಾ",
            "ಶ್ರವಣ",
            "ಧನಿಷ್ಠಾ",
            "ಶತಭಿಷಾ",
            "ಪೂರ್ವ ಭಾದ್ರಪದ",
            "ಉತ್ತರ ಭಾದ್ರಪದ",
            "ರೇವತಿ"
        )
    }
}
