package com.example.ui

import android.media.MediaPlayer
import android.util.Log
import java.io.File

class AudioPlayerHelper {
    private var mediaPlayer: MediaPlayer? = null
    var onCompletionListener: (() -> Unit)? = null

    fun play(file: File, onStart: (duration: Int) -> Unit) {
        try {
            stop()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                setOnPreparedListener {
                    onStart(duration)
                    start()
                }
                setOnCompletionListener {
                    stop()
                    onCompletionListener?.invoke()
                }
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerHelper", "Failed to play audio", e)
        }
    }

    fun stop() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e("AudioPlayerHelper", "Failed to stop playback", e)
        }
    }

    val isPlaying: Boolean
        get() = mediaPlayer?.isPlaying == true

    val currentPosition: Int
        get() = mediaPlayer?.currentPosition ?: 0
}
