package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.receiver.ReminderScheduler
import com.example.ui.SoulApp
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    // fire up the daily nagger on first boot
    ReminderScheduler.scheduleNextAlarm(this)

    setContent {
      MyApplicationTheme {
        SoulApp()
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    if (!isChangingConfigurations) {
      try {
        val decryptedDir = java.io.File(cacheDir, "decrypted_temp")
        if (decryptedDir.exists()) {
          decryptedDir.deleteRecursively()
        }
        val stagedDir = java.io.File(filesDir, "staged_media")
        if (stagedDir.exists()) {
          stagedDir.deleteRecursively()
        }
      } catch (e: Exception) {
        // shush — cleanup failures aren't worth crashing over
      }
    }
  }
}
