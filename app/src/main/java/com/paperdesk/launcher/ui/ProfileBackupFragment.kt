package com.paperdesk.launcher.ui

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.ProfileStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class ProfileBackupFragment : Fragment(R.layout.page_profile_backup) {
    private val create = registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        uri?.let { transfer(it, true) }
    }
    private val open = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) MaterialAlertDialogBuilder(requireContext())
            .setMessage("Replace local profiles with the profiles in this backup?")
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton("Restore") { _, _ -> transfer(uri, false) }.show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.exportProfiles).setOnClickListener {
            if (hasPassphrase(requireConfirm = true)) create.launch("MadhvaNidhi-profiles.mnprofiles")
        }
        view.findViewById<View>(R.id.importProfiles).setOnClickListener {
            // Restore only needs the known passphrase — no confirm field.
            if (hasPassphrase(requireConfirm = false)) {
                open.launch(arrayOf("application/octet-stream", "application/*", "*/*"))
            }
        }
    }

    private fun hasPassphrase(requireConfirm: Boolean): Boolean {
        val root = requireView()
        val input = root.findViewById<EditText>(R.id.backupPassword)
        val confirm = root.findViewById<EditText>(R.id.backupPasswordConfirm)
        input.error = null
        confirm.error = null
        if (input.length() < 4) {
            input.error = "Enter at least four characters"
            return false
        }
        if (requireConfirm) {
            val a = input.text?.toString().orEmpty()
            val b = confirm.text?.toString().orEmpty()
            if (a != b) {
                confirm.error = "Passphrases do not match"
                return false
            }
        }
        return true
    }

    private fun transfer(uri: Uri, exporting: Boolean) {
        if (!hasPassphrase(requireConfirm = exporting)) return
        val root = requireView()
        val password = root.findViewById<EditText>(R.id.backupPassword).text.toString().toCharArray()
        val context = requireContext().applicationContext
        val status = root.findViewById<TextView>(R.id.backupStatus)
        val buttons = listOf(root.findViewById<View>(R.id.exportProfiles), root.findViewById<View>(R.id.importProfiles))
        buttons.forEach { it.isEnabled = false }
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val resolver = context.contentResolver
                    val store = ProfileStore(context)
                    val random = SecureRandom()
                    val salt = ByteArray(16)
                    val nonce = ByteArray(12)
                    val magic = "MNPROF01".toByteArray()
                    fun cipher(mode: Int): Cipher {
                        val spec = PBEKeySpec(password, salt, 310000, 256)
                        val key = try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded }
                            finally { spec.clearPassword() }
                        return Cipher.getInstance("AES/GCM/NoPadding").apply {
                            init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
                            updateAAD(magic)
                            key.fill(0)
                        }
                    }
                    if (exporting) {
                        random.nextBytes(salt)
                        random.nextBytes(nonce)
                        val encrypted = cipher(Cipher.ENCRYPT_MODE).doFinal(store.exportJson().put("songFavorites", com.paperdesk.launcher.data.SongFavoritesStore.exportAll(context)).toString().toByteArray())
                        requireNotNull(resolver.openOutputStream(uri, "wt")).use { it.write(magic + salt + nonce + encrypted) }
                    } else {
                        val bytes = requireNotNull(resolver.openInputStream(uri)).use { stream ->
                            val output = java.io.ByteArrayOutputStream()
                            val buffer = ByteArray(8192)
                            while (true) {
                                val count = stream.read(buffer)
                                if (count < 0) break
                                require(output.size() + count <= 8 * 1024 * 1024)
                                output.write(buffer, 0, count)
                            }
                            output.toByteArray()
                        }
                        require(bytes.size >= 52 && bytes.copyOfRange(0, 8).contentEquals(magic))
                        bytes.copyInto(salt, 0, 8, 24)
                        bytes.copyInto(nonce, 0, 24, 36)
                        val profile = JSONObject(String(cipher(Cipher.DECRYPT_MODE).doFinal(bytes, 36, bytes.size - 36), Charsets.UTF_8))
                        require(profile.getJSONArray("items").length() > 0)
                        store.importJson(profile)
                        profile.optJSONObject("songFavorites")?.let { com.paperdesk.launcher.data.SongFavoritesStore.importAll(context,it) }
                    }
                }
                status.text = if (exporting) "Profile backup created" else "Profiles restored"
            } catch (error: kotlinx.coroutines.CancellationException) {
                throw error
            } catch (_: Exception) {
                status.text = if (exporting) "Could not write backup" else "Check the backup file and passphrase"
            } finally {
                password.fill(' ')
                buttons.forEach { it.isEnabled = true }
            }
        }
    }
}
