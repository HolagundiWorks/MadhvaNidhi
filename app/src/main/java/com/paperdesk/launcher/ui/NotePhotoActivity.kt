package com.paperdesk.launcher.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.paperdesk.launcher.DeskLock
import com.paperdesk.launcher.data.DeskPhoto
import com.paperdesk.launcher.data.NoteItem
import com.paperdesk.launcher.data.NoteStore
import com.paperdesk.launcher.data.ProjectStore
import java.util.UUID

class NotePhotoActivity : AppCompatActivity() {

    private lateinit var dest: java.io.File
    private lateinit var projectId: String
    private lateinit var noteId: String

    private val askCam = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) shoot() else finish()
    }

    private val takePic = registerForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok && dest.exists() && dest.length() > 0L) {
            val store = NoteStore(this)
            val projects = ProjectStore(this)
            store.upsert(
                NoteItem(
                    id = noteId,
                    projectId = projectId,
                    title = DeskPhoto.label(),
                    photo = dest.absolutePath
                )
            )
            projects.touch(projectId)
        } else {
            DeskPhoto.delete(dest.absolutePath)
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (DeskLock.bounce(this)) return
        projectId = intent.getStringExtra(EntryUi.EXTRA_PROJECT_ID).orEmpty()
        if (projectId.isBlank()) {
            finish()
            return
        }
        noteId = UUID.randomUUID().toString()
        dest = DeskPhoto.file(this, projectId, noteId)
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            shoot()
        } else {
            askCam.launch(Manifest.permission.CAMERA)
        }
    }

    private fun shoot() {
        val uri = runCatching { DeskPhoto.uri(this, dest) }.getOrNull()
        if (uri == null) {
            finish()
            return
        }
        takePic.launch(uri)
    }
}
