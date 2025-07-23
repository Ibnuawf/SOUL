package com.example.data

import android.content.Context
import android.util.Log
import com.example.security.CryptographyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class JournalRepository(private val context: Context) {
    private val database = SoulDatabase.getDatabase(context)
    private val dao = database.journalDao()

    val allEntriesFlow: Flow<List<JournalEntry>> = dao.getAllEntriesFlow()

    suspend fun getEntryByDate(dateString: String): JournalEntry? {
        return dao.getEntryByDate(dateString)
    }

    suspend fun insertEntry(entry: JournalEntry) {
        dao.insertEntry(entry)
    }

    // true = clock has been fucked with
    suspend fun checkAndUpdateLastKnownTime(currentTime: Long): Boolean = withContext(Dispatchers.IO) {
        val lastKnownSetting = dao.getSetting("last_known_time")
        val lastKnownTime = lastKnownSetting?.value?.toLongOrNull() ?: 0L
        
        if (currentTime < lastKnownTime) {
            Log.e("JournalRepository", "Clock rollback detected! Current: $currentTime, Last Known: $lastKnownTime")
            return@withContext true
        }

        // check for fast-forward shenanigans
        val lastSystemTimeSetting = dao.getSetting("last_system_time")?.value?.toLongOrNull() ?: 0L
        val lastElapsedSetting = dao.getSetting("last_elapsed_time")?.value?.toLongOrNull() ?: 0L
        val currentElapsed = android.os.SystemClock.elapsedRealtime()

        if (lastSystemTimeSetting > 0 && lastElapsedSetting > 0) {
            if (currentElapsed > lastElapsedSetting) {
                val deltaSystem = currentTime - lastSystemTimeSetting
                val deltaElapsed = currentElapsed - lastElapsedSetting
                // If system clock advanced by more than 5 minutes compared to actual elapsed real time, it's tampered
                if (deltaSystem - deltaElapsed > 5 * 60 * 1000L) {
                    Log.e("JournalRepository", "Clock advancement tampering detected! Delta system: $deltaSystem, Delta elapsed: $deltaElapsed")
                    return@withContext true
                }
            }
        }

        // any files from the future? probably tampering
        val filesDir = context.filesDir
        val files = filesDir.listFiles()
        if (files != null) {
            for (file in files) {
                if (file.lastModified() > currentTime + 60000) { // 1 min buffer
                    Log.e("JournalRepository", "Tampering detected: File ${file.name} is in the future: ${file.lastModified()}")
                    return@withContext true
                }
            }
        }
        
        if (currentTime > lastKnownTime) {
            dao.insertSetting(SystemSetting("last_known_time", currentTime.toString()))
        }

        dao.insertSetting(SystemSetting("last_system_time", currentTime.toString()))
        dao.insertSetting(SystemSetting("last_elapsed_time", currentElapsed.toString()))
        false
    }

    suspend fun getLastKnownTime(): Long {
        val setting = dao.getSetting("last_known_time")
        return setting?.value?.toLongOrNull() ?: 0L
    }

    // encrypt + persist a full day's entry atomically
    suspend fun saveTodayEntry(dateString: String, answers: JournalAnswers) = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val oneYearInMillis = 365L * 24 * 60 * 60 * 1000L
        val unlockTimestamp = timestamp + oneYearInMillis

        val filesToMove = mutableListOf<Pair<File, File>>()
        val finalMediaDir = File(context.filesDir, "encrypted_media").apply { mkdirs() }

        // relocate staged media to permanent encrypted dir
        val updatedAnswersList = answers.answers.map { originalQA ->
            var updatedAudioPath = originalQA.audioPath
            if (updatedAudioPath != null && updatedAudioPath.contains("staged_media")) {
                val stagedFile = File(updatedAudioPath)
                if (stagedFile.exists()) {
                    val finalFile = File(finalMediaDir, stagedFile.name)
                    filesToMove.add(Pair(stagedFile, finalFile))
                    updatedAudioPath = finalFile.absolutePath
                }
            }

            val updatedPhotoPaths = originalQA.photoPaths.map { path ->
                if (path.contains("staged_media")) {
                    val stagedFile = File(path)
                    if (stagedFile.exists()) {
                        val finalFile = File(finalMediaDir, stagedFile.name)
                        filesToMove.add(Pair(stagedFile, finalFile))
                        finalFile.absolutePath
                    } else {
                        path
                    }
                } else {
                    path
                }
            }

            originalQA.copy(
                audioPath = updatedAudioPath,
                photoPaths = updatedPhotoPaths
            )
        }

        val finalAnswers = JournalAnswers(answers = updatedAnswersList)
        val jsonString = JournalAnswers.toJson(finalAnswers)
        val encryptedBytes = CryptographyManager.encrypt(jsonString.toByteArray(Charsets.UTF_8))

        val entry = JournalEntry(
            dateString = dateString,
            timestamp = timestamp,
            unlockTimestamp = unlockTimestamp,
            isCompleted = true,
            encryptedAnswers = encryptedBytes
        )

        try {
            // atomic — entry + time update in one shot
            database.runInTransaction {
                dao.insertEntrySync(entry)
                dao.insertSettingSync(SystemSetting("last_known_time", timestamp.toString()))
                dao.insertSettingSync(SystemSetting("last_system_time", timestamp.toString()))
                dao.insertSettingSync(SystemSetting("last_elapsed_time", android.os.SystemClock.elapsedRealtime().toString()))
            }

            // only promote staged files if the db txn went through
            for ((stagedFile, finalFile) in filesToMove) {
                if (stagedFile.exists()) {
                    moveFile(stagedFile, finalFile)
                }
            }
        } catch (e: Exception) {
            Log.e("JournalRepository", "Transaction failed, deleting staged files to prevent orphans", e)
            // rollback — nuke any orphaned staged files
            for ((stagedFile, _) in filesToMove) {
                if (stagedFile.exists()) {
                    stagedFile.delete()
                }
            }
            throw e
        }

        // sync to both backup locations after successful save
        try {
            DataSyncManager.backupActiveData(context)
        } catch (e: Exception) {
            Log.e("JournalRepository", "Error saving database backup", e)
        }
    }

    private fun moveFile(source: File, dest: File) {
        if (!source.exists()) return
        dest.parentFile?.mkdirs()
        val success = source.renameTo(dest)
        if (!success) {
            try {
                source.inputStream().use { input ->
                    dest.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                source.delete()
            } catch (e: Exception) {
                Log.e("JournalRepository", "Failed to copy file in move fallback", e)
            }
        }
    }

    // decrypt — but only if the year's up
    fun decryptEntry(entry: JournalEntry, currentTime: Long = System.currentTimeMillis()): JournalAnswers {
        if (currentTime < entry.unlockTimestamp) {
            throw SecurityException("Entry is locked until ${getFormattedDate(entry.unlockTimestamp)}")
        }
        
        val decryptedBytes = CryptographyManager.decrypt(entry.encryptedAnswers)
        val jsonString = String(decryptedBytes, Charsets.UTF_8)
        return JournalAnswers.fromJson(jsonString) ?: JournalAnswers()
    }

    // dump an encrypted copy into staged
    suspend fun encryptFileToStaged(tempSourceFile: File): File = withContext(Dispatchers.IO) {
        val stagedDir = File(context.filesDir, "staged_media").apply { mkdirs() }
        val encryptedFile = File(stagedDir, "${UUID.randomUUID()}.enc")
        
        CryptographyManager.encryptFileStream(tempSourceFile, encryptedFile)
        
        // Securely delete plain file immediately
        if (tempSourceFile.exists()) {
            tempSourceFile.delete()
        }
        encryptedFile
    }

    // convenience wrapper — legacy compat
    suspend fun encryptFile(tempSourceFile: File): File {
        return encryptFileToStaged(tempSourceFile)
    }

    // decrypt to temp for playback — caller must clean up
    suspend fun decryptFileToTemp(encryptedFile: File): File = withContext(Dispatchers.IO) {
        val decryptedDir = File(context.cacheDir, "decrypted_temp").apply { mkdirs() }
        val tempFile = File(decryptedDir, "temp_${System.currentTimeMillis()}_${encryptedFile.name.removeSuffix(".enc")}")
        
        CryptographyManager.decryptFileStream(encryptedFile, tempFile)
        tempFile
    }

    // verify + repair data on startup
    suspend fun checkAndRepairDataIntegrity(force: Boolean = false) = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            val sevenDaysInMillis = 7L * 24 * 60 * 60 * 1000L
            val lastCheckStr = dao.getSetting("last_integrity_check_time")?.value
            val lastCheck = lastCheckStr?.toLongOrNull() ?: 0L

            if (!force && now - lastCheck < sevenDaysInMillis) {
                Log.i("JournalRepository", "Skipping routine startup integrity check (last ran less than 7 days ago)")
                return@withContext
            }

            Log.i("JournalRepository", "Starting optimized startup data integrity check and repair...")
            val allEntries = dao.getAllEntries()
            val referencedEncryptedFileNames = mutableSetOf<String>()

            // snapshot backup listings — O(1) lookups > O(N) stat spaghetti
            val androidBackupDir = File(context.getExternalFilesDir(null), "SoulVault_Data")
            val mainRootBackupDir = DataSyncManager.getMainRootBackupDir(context)
            val backupMediaA = File(androidBackupDir, "media")
            val backupMediaB = File(mainRootBackupDir, "media")

            val backupAFiles = backupMediaA.listFiles()?.associateBy { it.name } ?: emptyMap()
            val backupBFiles = backupMediaB.listFiles()?.associateBy { it.name } ?: emptyMap()

            // single pass — decrypt, map refs, heal gaps
            for (entry in allEntries) {
                try {
                    val decryptedBytes = CryptographyManager.decrypt(entry.encryptedAnswers)
                    val jsonString = String(decryptedBytes, Charsets.UTF_8)
                    val answers = JournalAnswers.fromJson(jsonString)
                    if (answers != null) {
                        for (answer in answers.answers) {
                            answer.audioPath?.let { path ->
                                val file = File(path)
                                val fileName = file.name
                                referencedEncryptedFileNames.add(fileName)
                                verifyAndRestoreFileOptimized(file, fileName, backupAFiles, backupBFiles)
                            }
                            for (path in answer.photoPaths) {
                                val file = File(path)
                                val fileName = file.name
                                referencedEncryptedFileNames.add(fileName)
                                verifyAndRestoreFileOptimized(file, fileName, backupAFiles, backupBFiles)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("JournalRepository", "Data integrity check: Failed to decrypt entry for ${entry.dateString}", e)
                }
            }

            // purge orphan encrypted files not referenced by any entry
            val activeMediaDir = File(context.filesDir, "encrypted_media")
            if (activeMediaDir.exists()) {
                val activeFiles = activeMediaDir.listFiles() ?: emptyArray()
                for (file in activeFiles) {
                    if (file.name !in referencedEncryptedFileNames) {
                        Log.w("JournalRepository", "Orphan encrypted media file detected: ${file.name}. Removing...")
                        file.delete()
                        try {
                            DataSyncManager.deleteBackupFile(context, file.name)
                        } catch (e: Exception) {
                            Log.e("JournalRepository", "Failed to delete backup of orphan file: ${file.name}", e)
                        }
                    }
                }
            }

            // stamp the check time so we don't re-run too soon
            dao.insertSetting(SystemSetting("last_integrity_check_time", now.toString()))
            Log.i("JournalRepository", "Startup data integrity check completed and settings updated.")
        } catch (e: Exception) {
            Log.e("JournalRepository", "Error during data integrity check", e)
        }
    }

    private fun verifyAndRestoreFileOptimized(
        file: File,
        fileName: String,
        backupAFiles: Map<String, File>,
        backupBFiles: Map<String, File>
    ) {
        if (!file.exists()) {
            Log.w("JournalRepository", "Missing media reference: $fileName. Attempting recovery...")
            val backup1 = backupAFiles[fileName]
            val backup2 = backupBFiles[fileName]
            val sourceBackup = backup1 ?: backup2

            if (sourceBackup != null && sourceBackup.exists()) {
                try {
                    file.parentFile?.mkdirs()
                    sourceBackup.inputStream().use { input ->
                        file.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    file.setLastModified(sourceBackup.lastModified())
                    Log.i("JournalRepository", "Successfully recovered missing media file: $fileName")
                } catch (e: Exception) {
                    Log.e("JournalRepository", "Failed to recover missing media file: $fileName", e)
                }
            } else {
                Log.e("JournalRepository", "Missing file recovery failed: $fileName (not found in backups)")
            }
        }
    }

    fun getFormattedDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun getFormattedDateWithDay(timestamp: Long): String {
        val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
