package com.paperdesk.launcher.data

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import com.paperdesk.launcher.bridge.OsDataService
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Sanskrit verse chants from GitaVani (nikhilsi/gitavani, MIT).
 * Local first: /sdcard/Vishwakarma/gita/BG{c}.{v}.mp3
 * then app assets audio/, then GitHub raw (cached to that folder).
 */
object GitaAudio {
    private const val MIN_BYTES = 2048L
    private const val REMOTE =
        "https://raw.githubusercontent.com/nikhilsi/gitavani/main/android/GitaVani/app/src/main/assets/audio/"

    private var player: MediaPlayer? = null
    private var audio: AudioManager? = null
    private val main = Handler(Looper.getMainLooper())
    var chapter: Int = 0
        private set
    var verse: Int = 0
        private set

    fun verseId(c: Int, v: Int): String = "BG$c.$v"

    fun fileFor(c: Int, v: Int): File = File(OsDataService.rootDir(), "gita/${verseId(c, v)}.mp3")

    fun isPlaying(): Boolean = player?.isPlaying == true

    fun isPlaying(c: Int, v: Int): Boolean = isPlaying() && chapter == c && verse == v

    fun stop() {
        val mp = player
        player = null
        chapter = 0
        verse = 0
        if (mp != null) {
            runCatching { mp.stop() }
            runCatching { mp.release() }
        }
        runCatching { audio?.abandonAudioFocus(null) }
    }

    fun ensureFile(context: Context, c: Int, v: Int): File? {
        val dest = fileFor(c, v)
        if (dest.exists() && dest.length() >= MIN_BYTES) return dest
        dest.parentFile?.mkdirs()
        if (copyAsset(context, c, v, dest)) return dest
        if (download(c, v, dest)) return dest
        return if (dest.exists() && dest.length() >= MIN_BYTES) dest else null
    }

    fun start(context: Context, file: File, c: Int, v: Int, onComplete: () -> Unit, onError: () -> Unit) {
        stop()
        val mp = MediaPlayer()
        try {
            val am = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audio = am
            am.mode = AudioManager.MODE_NORMAL
            am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
            hear(am)
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            val speaker = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
            if (speaker != null) mp.setPreferredDevice(speaker)
            mp.setVolume(1f, 1f)
            mp.setDataSource(file.absolutePath)
            mp.setOnCompletionListener {
                main.post {
                    if (player !== mp) return@post
                    stop()
                    onComplete()
                }
            }
            mp.setOnErrorListener { _, _, _ ->
                main.post {
                    if (player !== mp) return@post
                    stop()
                    onError()
                }
                true
            }
            mp.prepare()
            mp.start()
            player = mp
            chapter = c
            verse = v
        } catch (_: Exception) {
            runCatching { mp.release() }
            player = null
            chapter = 0
            verse = 0
            onError()
        }
    }

    private fun hear(am: AudioManager) {
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val cur = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (max > 0 && cur < (max + 3) / 4) {
            am.setStreamVolume(AudioManager.STREAM_MUSIC, (max * 3) / 5, 0)
        }
    }

    private fun copyAsset(context: Context, c: Int, v: Int, dest: File): Boolean {
        val name = "audio/${verseId(c, v)}.mp3"
        return try {
            val tmp = File(dest.parentFile, dest.name + ".part")
            context.assets.open(name).use { input ->
                FileOutputStream(tmp).use { input.copyTo(it) }
            }
            if (tmp.length() < MIN_BYTES) {
                tmp.delete()
                false
            } else {
                if (dest.exists()) dest.delete()
                tmp.renameTo(dest)
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun download(c: Int, v: Int, dest: File): Boolean {
        val tmp = File(dest.parentFile, dest.name + ".part")
        var conn: HttpURLConnection? = null
        return try {
            conn = URL(REMOTE + verseId(c, v) + ".mp3").openConnection() as HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 45000
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", "AcharyaOS")
            conn.setRequestProperty("Accept", "audio/mpeg")
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return false
            conn.inputStream.use { input ->
                FileOutputStream(tmp).use { input.copyTo(it) }
            }
            if (tmp.length() < MIN_BYTES) {
                tmp.delete()
                false
            } else {
                if (dest.exists()) dest.delete()
                tmp.renameTo(dest)
            }
        } catch (_: Exception) {
            runCatching { tmp.delete() }
            false
        } finally {
            conn?.disconnect()
        }
    }
}
