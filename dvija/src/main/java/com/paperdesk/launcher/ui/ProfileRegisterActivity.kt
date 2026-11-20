package com.paperdesk.launcher.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.paperdesk.launcher.DeskTheme
import com.paperdesk.launcher.DeskWindow
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.Profile
import com.paperdesk.launcher.data.ProfileStore
import java.util.Calendar
import java.util.UUID

class ProfileRegisterActivity : AppCompatActivity() {
    private lateinit var store: ProfileStore
    private lateinit var editing: Profile

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(AppTextSize.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        DeskTheme.apply(this)
        super.onCreate(savedInstanceState)
        // Continuous scroll — do not wrap with ScreenPages (that hid fields behind 1/N paging).
        setContentView(R.layout.activity_profile_register)
        DeskWindow.padCutout(findViewById(android.R.id.content))
        DeskWindow.immersive(this)
        MaterialLayouts.bind(findViewById(android.R.id.content))

        store = ProfileStore(this)
        editing = store.active()

        findViewById<android.view.View>(R.id.profileBack).setOnClickListener { finish() }

        findViewById<TextInputEditText>(R.id.profileName).setText(editing.name)
        findViewById<TextInputEditText>(R.id.profilePravara).setText(editing.pravara)
        findViewById<TextInputEditText>(R.id.profilePlaceOfBirth).setText(editing.placeOfBirth)
        findViewById<TextInputEditText>(R.id.profileYearOfBirth).setText(
            if (editing.yearOfBirth > 0) editing.yearOfBirth.toString() else ""
        )

        bindChoice(R.id.profileVeda, ProfileStore.VEDA_SHAKHAS, editing.vedaShakha)
        bindChoice(R.id.profileGotra, ProfileStore.GOTRAS, editing.gotra)
        bindChoice(R.id.profileRashi, ProfileStore.RASHIS, editing.rashi)
        bindChoice(R.id.profileNakshatra, ProfileStore.NAKSHATRAS, editing.nakshatra)

        findViewById<android.view.View>(R.id.profileSave).setOnClickListener { save() }
    }

    private fun bindChoice(viewId: Int, choices: List<String>, current: String) {
        val view = findViewById<MaterialAutoCompleteTextView>(viewId)
        val index = choices.indexOf(current).let { if (it >= 0) it else 0 }
        MaterialChoices.bind(view, choices, index)
    }

    private fun choiceValue(viewId: Int, choices: List<String>): String {
        val view = findViewById<MaterialAutoCompleteTextView>(viewId)
        val typed = view.text?.toString()?.trim().orEmpty()
        if (typed.isNotEmpty() && choices.contains(typed)) return typed
        return choices.getOrElse(MaterialChoices.position(view)) { choices.first() }
    }

    private fun save() {
        val name = findViewById<TextInputEditText>(R.id.profileName).text?.toString()?.trim().orEmpty()
        if (name.isBlank()) {
            Toast.makeText(this, R.string.profile_name_required, Toast.LENGTH_SHORT).show()
            findViewById<TextInputEditText>(R.id.profileName).requestFocus()
            return
        }
        val yearRaw = findViewById<TextInputEditText>(R.id.profileYearOfBirth).text?.toString()?.trim().orEmpty()
        val year = yearRaw.toIntOrNull() ?: 0
        val maxYear = Calendar.getInstance().get(Calendar.YEAR)
        if (yearRaw.isNotEmpty() && (year < 1900 || year > maxYear)) {
            Toast.makeText(this, getString(R.string.profile_year_invalid, maxYear), Toast.LENGTH_SHORT).show()
            findViewById<TextInputEditText>(R.id.profileYearOfBirth).requestFocus()
            return
        }
        store.save(
            Profile(
                id = editing.id.ifBlank { UUID.randomUUID().toString() },
                name = name,
                vedaShakha = choiceValue(R.id.profileVeda, ProfileStore.VEDA_SHAKHAS),
                gotra = choiceValue(R.id.profileGotra, ProfileStore.GOTRAS),
                pravara = findViewById<TextInputEditText>(R.id.profilePravara).text?.toString()?.trim().orEmpty(),
                rashi = choiceValue(R.id.profileRashi, ProfileStore.RASHIS),
                nakshatra = choiceValue(R.id.profileNakshatra, ProfileStore.NAKSHATRAS),
                yearOfBirth = year,
                placeOfBirth = findViewById<TextInputEditText>(R.id.profilePlaceOfBirth).text?.toString()?.trim().orEmpty()
            )
        )
        Toast.makeText(this, R.string.profile_saved, Toast.LENGTH_SHORT).show()
        setResult(RESULT_OK)
        finish()
    }

    companion object {
        fun intent(context: android.content.Context): Intent =
            Intent(context, ProfileRegisterActivity::class.java)
    }
}
