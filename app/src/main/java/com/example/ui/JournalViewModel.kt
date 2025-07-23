package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.receiver.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class JournalViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = JournalRepository(application)

    val allEntries: StateFlow<List<JournalEntry>> = repository.allEntriesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isRollbackDetected = MutableStateFlow(false)
    val isRollbackDetected = _isRollbackDetected.asStateFlow()

    private val _todayCompleted = MutableStateFlow(false)
    val todayCompleted = _todayCompleted.asStateFlow()

    private val _oneYearAgoEntry = MutableStateFlow<JournalEntry?>(null)
    val oneYearAgoEntry = _oneYearAgoEntry.asStateFlow()

    private val _oneYearAgoDecryptedAnswers = MutableStateFlow<JournalAnswers?>(null)
    val oneYearAgoDecryptedAnswers = _oneYearAgoDecryptedAnswers.asStateFlow()

    private val _reviewingOneYearAgo = MutableStateFlow(false)
    val reviewingOneYearAgo = _reviewingOneYearAgo.asStateFlow()

    private val _activeAnswers = MutableStateFlow(JournalAnswers())
    val activeAnswers = _activeAnswers.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex = _currentQuestionIndex.asStateFlow()

    private val tempDecryptedFiles = java.util.Collections.synchronizedList(mutableListOf<File>())

    init {
        checkStatus()
    }

    fun checkStatus() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            
            // 1. did someone rewind the clock?
            val isRollback = repository.checkAndUpdateLastKnownTime(now)
            _isRollbackDetected.value = isRollback
            if (isRollback) return@launch

            // 2. already wrote today?
            val todayDateString = getTodayDateString()
            val todayEntry = repository.getEntryByDate(todayDateString)
            _todayCompleted.value = todayEntry?.isCompleted == true

            // 3. past-me left something for today
            val oneYearAgoDateString = getOneYearAgoDateString()
            val entryOneYearAgo = repository.getEntryByDate(oneYearAgoDateString)
            if (entryOneYearAgo != null && entryOneYearAgo.isCompleted) {
                _oneYearAgoEntry.value = entryOneYearAgo
                if (todayEntry?.isCompleted != true) {
                    _reviewingOneYearAgo.value = true
                    try {
                        _oneYearAgoDecryptedAnswers.value = repository.decryptEntry(entryOneYearAgo, now)
                    } catch (e: Exception) {
                        Log.e("JournalViewModel", "Failed to decrypt one year ago entry", e)
                    }
                }
            } else {
                _oneYearAgoEntry.value = null
                _reviewingOneYearAgo.value = false
                _oneYearAgoDecryptedAnswers.value = null
            }

            // 4. sweep old junk + integrity check — IO thread so UI doesn't choke
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    // nuke stale staged files from aborted sessions
                    val stagedDir = File(getApplication<android.app.Application>().filesDir, "staged_media")
                    if (stagedDir.exists()) {
                        stagedDir.deleteRecursively()
                    }
                    cleanupAllTempFiles()
                    repository.checkAndRepairDataIntegrity(force = false)
                } catch (e: Exception) {
                    Log.e("JournalViewModel", "Failed to run background startup integrity checks and cleanups", e)
                }
            }
        }
    }

    fun onAnswerTextChange(index: Int, text: String) {
        val current = _activeAnswers.value
        val updatedAnswers = current.answers.toMutableList()
        updatedAnswers[index] = updatedAnswers[index].copy(text = text)
        _activeAnswers.value = current.copy(answers = updatedAnswers)
    }

    suspend fun addAudio(index: Int, tempFile: File) {
        try {
            val encryptedFile = repository.encryptFile(tempFile)
            val current = _activeAnswers.value
            val updatedAnswers = current.answers.toMutableList()
            
            updatedAnswers[index].audioPath?.let { oldPath ->
                val file = File(oldPath)
                if (file.exists()) file.delete()
            }

            updatedAnswers[index] = updatedAnswers[index].copy(audioPath = encryptedFile.absolutePath)
            _activeAnswers.value = current.copy(answers = updatedAnswers)
        } catch (e: Exception) {
            Log.e("JournalViewModel", "Failed to add audio", e)
        }
    }

    suspend fun addPhoto(index: Int, tempFile: File) {
        try {
            val encryptedFile = repository.encryptFile(tempFile)
            val current = _activeAnswers.value
            val updatedAnswers = current.answers.toMutableList()
            val currentPhotos = updatedAnswers[index].photoPaths.toMutableList()
            currentPhotos.add(encryptedFile.absolutePath)
            
            updatedAnswers[index] = updatedAnswers[index].copy(photoPaths = currentPhotos)
            _activeAnswers.value = current.copy(answers = updatedAnswers)
        } catch (e: Exception) {
            Log.e("JournalViewModel", "Failed to add photo", e)
        }
    }



    fun deleteAttachment(index: Int, type: String, path: String) {
        viewModelScope.launch {
            try {
                val file = File(path)
                val fileName = file.name
                if (file.exists()) {
                    file.delete()
                }
                
                // Also delete from backups
                try {
                    DataSyncManager.deleteBackupFile(getApplication(), fileName)
                } catch (e: Exception) {
                    Log.e("JournalViewModel", "Failed to delete backup file", e)
                }
                
                val current = _activeAnswers.value
                val updatedAnswers = current.answers.toMutableList()
                
                when (type) {
                    "AUDIO" -> {
                        updatedAnswers[index] = updatedAnswers[index].copy(audioPath = null)
                    }
                    "PHOTO" -> {
                        val currentPhotos = updatedAnswers[index].photoPaths.toMutableList()
                        currentPhotos.remove(path)
                        updatedAnswers[index] = updatedAnswers[index].copy(photoPaths = currentPhotos)
                    }
                }
                _activeAnswers.value = current.copy(answers = updatedAnswers)
            } catch (e: Exception) {
                Log.e("JournalViewModel", "Failed to delete attachment", e)
            }
        }
    }

    fun setQuestionIndex(index: Int) {
        _currentQuestionIndex.value = index.coerceIn(0, 4)
    }

    fun nextQuestion() {
        _currentQuestionIndex.value = (_currentQuestionIndex.value + 1).coerceAtMost(4)
    }

    fun prevQuestion() {
        _currentQuestionIndex.value = (_currentQuestionIndex.value - 1).coerceAtLeast(0)
    }

    fun completeReview() {
        _reviewingOneYearAgo.value = false
    }

    fun lockAndSaveToday() {
        viewModelScope.launch {
            try {
                val todayDateString = getTodayDateString()
                repository.saveTodayEntry(todayDateString, _activeAnswers.value)
                
                _activeAnswers.value = JournalAnswers()
                _currentQuestionIndex.value = 0
                _todayCompleted.value = true

                ReminderScheduler.scheduleNextAlarm(getApplication())
            } catch (e: Exception) {
                Log.e("JournalViewModel", "Failed to save entry", e)
            }
        }
    }

    fun decryptEntry(entry: JournalEntry): JournalAnswers? {
        return try {
            repository.decryptEntry(entry, System.currentTimeMillis())
        } catch (e: Exception) {
            Log.e("JournalViewModel", "Failed to decrypt entry", e)
            null
        }
    }

    suspend fun decryptFileForPlayback(encryptedPath: String): File? {
        return try {
            val encryptedFile = File(encryptedPath)
            if (!encryptedFile.exists()) return null
            val decryptedFile = repository.decryptFileToTemp(encryptedFile)
            tempDecryptedFiles.add(decryptedFile)
            decryptedFile
        } catch (e: Exception) {
            Log.e("JournalViewModel", "Failed to decrypt file for playback", e)
            null
        }
    }

    fun deleteDecryptedTempFile(file: File) {
        if (file.exists()) {
            file.delete()
        }
        tempDecryptedFiles.remove(file)
    }

    fun cleanupAllTempFiles() {
        val iterator = tempDecryptedFiles.iterator()
        while (iterator.hasNext()) {
            val file = iterator.next()
            if (file.exists()) {
                file.delete()
            }
            iterator.remove()
        }
        try {
            val decryptedDir = File(getApplication<android.app.Application>().cacheDir, "decrypted_temp")
            if (decryptedDir.exists()) {
                decryptedDir.deleteRecursively()
            }
        } catch (e: Exception) {
            Log.e("JournalViewModel", "Failed to clear decrypted temp dir", e)
        }
    }

    fun getFormattedDate(timestamp: Long): String {
        return repository.getFormattedDate(timestamp)
    }

    fun getFormattedDateWithDay(timestamp: Long): String {
        return repository.getFormattedDateWithDay(timestamp)
    }

    fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    fun getOneYearAgoDateString(): String {
        val oneYearAgoCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -365)
        }
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(oneYearAgoCal.time)
    }

    override fun onCleared() {
        super.onCleared()
        cleanupAllTempFiles()
    }
}
