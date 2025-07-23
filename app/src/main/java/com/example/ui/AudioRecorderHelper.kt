package com.example.ui

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

class AudioRecorderHelper(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null

    fun startRecording(): File? {
        try {
            stopRecording()
            
            val tempFile = File.createTempFile("voice_", ".3gp", context.cacheDir)
            currentOutputFile = tempFile

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(tempFile.absolutePath)
                prepare()
                start()
            }
            Log.d("AudioRecorderHelper", "Recording started: ${tempFile.absolutePath}")
            return tempFile
        } catch (e: Exception) {
            Log.e("AudioRecorderHelper", "Failed to start recording", e)
            currentOutputFile?.delete()
            currentOutputFile = null
            return null
        }
    }

    fun stopRecording(): File? {
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            val file = currentOutputFile
            currentOutputFile = null
            Log.d("AudioRecorderHelper", "Recording stopped: ${file?.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e("AudioRecorderHelper", "Failed to stop recording", e)
            mediaRecorder = null
            currentOutputFile = null
            null
        }
    }
}
