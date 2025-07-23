package com.example.data

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File

object DataSyncManager {
    private const val TAG = "DataSyncManager"

    fun getMainRootBackupDir(context: Context): File {
        val extDir = Environment.getExternalStorageDirectory()
        if (extDir != null && extDir.exists()) {
            val dir = File(extDir, "SoulVault_Backup")
            try {
                dir.mkdirs()
                if (dir.exists() && dir.canWrite()) {
                    return dir
                }
            } catch (e: Exception) {
                Log.e(TAG, "Cannot write to main external storage root", e)
            }
        }
        
        // plan B, C, D...
        val fallbacks = listOf(
            "/sdcard/SoulVault_Backup",
            "/storage/emulated/0/SoulVault_Backup",
            "/storage/emulated/0/Download/SoulVault_Backup",
            "/storage/emulated/0/Documents/SoulVault_Backup"
        )
        for (path in fallbacks) {
            try {
                val dir = File(path)
                dir.mkdirs()
                if (dir.exists() && dir.canWrite()) {
                    return dir
                }
            } catch (e: Exception) {
                // continue
            }
        }
        
        // last resort — internal cache
        val internalFallback = File(context.cacheDir, "SoulVault_Backup_Fallback")
        internalFallback.mkdirs()
        return internalFallback
    }

    private fun copyFile(source: File, dest: File) {
        if (!source.exists()) return
        try {
            dest.parentFile?.mkdirs()
            source.inputStream().use { input ->
                dest.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            dest.setLastModified(source.lastModified())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy ${source.absolutePath} to ${dest.absolutePath}", e)
        }
    }

    private fun copyDirectoryContents(srcDir: File, destDir: File) {
        if (!srcDir.exists() || !srcDir.isDirectory) return
        destDir.mkdirs()
        val files = srcDir.listFiles() ?: return
        for (file in files) {
            if (file.isFile) {
                copyFile(file, File(destDir, file.name))
            }
        }
    }

    // bidirectional sync — whatever's newer or missing gets copied
    fun syncBackups(context: Context) {
        val androidBackupDir = File(context.getExternalFilesDir(null), "SoulVault_Data")
        val mainRootBackupDir = getMainRootBackupDir(context)

        val androidDbDir = File(androidBackupDir, "databases")
        val androidMediaDir = File(androidBackupDir, "media")

        val mainDbDir = File(mainRootBackupDir, "databases")
        val mainMediaDir = File(mainRootBackupDir, "media")

        // make sure they exist before we poke at 'em
        try { androidDbDir.mkdirs() } catch (e: Exception) {}
        try { androidMediaDir.mkdirs() } catch (e: Exception) {}
        try { mainDbDir.mkdirs() } catch (e: Exception) {}
        try { mainMediaDir.mkdirs() } catch (e: Exception) {}

        // 1. databases
        val androidHasDb = androidDbDir.exists() && File(androidDbDir, "soul_database").exists()
        val mainHasDb = mainDbDir.exists() && File(mainDbDir, "soul_database").exists()

        if (androidHasDb && !mainHasDb) {
            Log.d(TAG, "Main root backup is missing database, copying from Android folder...")
            copyDirectoryContents(androidDbDir, mainDbDir)
        } else if (!androidHasDb && mainHasDb) {
            Log.d(TAG, "Android folder backup is missing database, copying from Main root...")
            copyDirectoryContents(mainDbDir, androidDbDir)
        } else if (androidHasDb && mainHasDb) {
            val androidDbFile = File(androidDbDir, "soul_database")
            val mainDbFile = File(mainDbDir, "soul_database")
            if (androidDbFile.lastModified() > mainDbFile.lastModified() + 1000) {
                Log.d(TAG, "Android folder database backup is newer, updating Main root...")
                copyDirectoryContents(androidDbDir, mainDbDir)
            } else if (mainDbFile.lastModified() > androidDbFile.lastModified() + 1000) {
                Log.d(TAG, "Main root database backup is newer, updating Android folder...")
                copyDirectoryContents(mainDbDir, androidDbDir)
            }
        }

        // 2. encrypted media — merge both ways
        if (androidMediaDir.exists() && mainMediaDir.exists()) {
            val filesA = androidMediaDir.listFiles() ?: emptyArray()
            val filesB = mainMediaDir.listFiles() ?: emptyArray()

            val namesA = filesA.map { it.name }.toSet()
            val namesB = filesB.map { it.name }.toSet()

            for (fileA in filesA) {
                if (fileA.name !in namesB) {
                    copyFile(fileA, File(mainMediaDir, fileA.name))
                }
            }

            for (fileB in filesB) {
                if (fileB.name !in namesA) {
                    copyFile(fileB, File(androidMediaDir, fileB.name))
                }
            }
        }
    }

    // pull backed-up data back into active storage
    fun restoreActiveDataFromBackup(context: Context) {
        val androidBackupDir = File(context.getExternalFilesDir(null), "SoulVault_Data")
        val mainRootBackupDir = getMainRootBackupDir(context)

        val androidDbDir = File(androidBackupDir, "databases")
        val androidMediaDir = File(androidBackupDir, "media")

        val mainDbDir = File(mainRootBackupDir, "databases")
        val mainMediaDir = File(mainRootBackupDir, "media")

        // pick the healthiest db source
        val bestDbDir = when {
            androidDbDir.exists() && File(androidDbDir, "soul_database").exists() -> androidDbDir
            mainDbDir.exists() && File(mainDbDir, "soul_database").exists() -> mainDbDir
            else -> null
        }

        val bestMediaDir = when {
            androidMediaDir.exists() && (androidMediaDir.listFiles()?.isNotEmpty() == true) -> androidMediaDir
            mainMediaDir.exists() && (mainMediaDir.listFiles()?.isNotEmpty() == true) -> mainMediaDir
            else -> null
        }

        // 1. db restore
        val activeDbFile = context.getDatabasePath("soul_database")
        if (bestDbDir != null) {
            val backupDbFile = File(bestDbDir, "soul_database")
            val activeDbExists = activeDbFile.exists()

            if (!activeDbExists || backupDbFile.lastModified() > activeDbFile.lastModified() + 1000) {
                Log.d(TAG, "Restoring active database from backup sources...")
                activeDbFile.parentFile?.mkdirs()
                val dbFileNames = listOf("soul_database", "soul_database-wal", "soul_database-shm")
                for (fileName in dbFileNames) {
                    val src = File(bestDbDir, fileName)
                    val dest = File(activeDbFile.parentFile, fileName)
                    if (src.exists()) {
                        copyFile(src, dest)
                    } else if (dest.exists()) {
                        dest.delete()
                    }
                }
            }
        }

        // 2. media restore
        val activeMediaDir = File(context.filesDir, "encrypted_media").apply { mkdirs() }
        if (bestMediaDir != null) {
            val backupMediaFiles = bestMediaDir.listFiles() ?: emptyArray()
            val activeMediaFiles = activeMediaDir.listFiles() ?: emptyArray()
            val activeMediaNames = activeMediaFiles.map { it.name }.toSet()

            for (bFile in backupMediaFiles) {
                if (bFile.name !in activeMediaNames) {
                    Log.d(TAG, "Restoring missing active media file: ${bFile.name}")
                    copyFile(bFile, File(activeMediaDir, bFile.name))
                }
            }
        }
    }

    // Startup utility to sync and restore data
    fun syncAndRestore(context: Context) {
        syncBackups(context)
        restoreActiveDataFromBackup(context)
    }

    // copy active data to both backup locations
    fun backupActiveData(context: Context) {
        val androidBackupDir = File(context.getExternalFilesDir(null), "SoulVault_Data")
        val mainRootBackupDir = getMainRootBackupDir(context)

        val androidDbDir = File(androidBackupDir, "databases").apply { mkdirs() }
        val androidMediaDir = File(androidBackupDir, "media").apply { mkdirs() }

        val mainDbDir = File(mainRootBackupDir, "databases").apply { mkdirs() }
        val mainMediaDir = File(mainRootBackupDir, "media").apply { mkdirs() }

        // 1. db files
        val activeDbFile = context.getDatabasePath("soul_database")
        if (activeDbFile.exists()) {
            val dbFileNames = listOf("soul_database", "soul_database-wal", "soul_database-shm")
            for (fileName in dbFileNames) {
                val activeSrc = File(activeDbFile.parentFile, fileName)
                if (activeSrc.exists()) {
                    copyFile(activeSrc, File(androidDbDir, fileName))
                    copyFile(activeSrc, File(mainDbDir, fileName))
                }
            }
        }

        // 2. media files
        val activeMediaDir = File(context.filesDir, "encrypted_media")
        if (activeMediaDir.exists()) {
            val mediaFiles = activeMediaDir.listFiles() ?: emptyArray()
            
            // cache dest file metadata — O(1) beats stat() spam
            val androidBackupFiles = androidMediaDir.listFiles()?.associate { it.name to it.lastModified() } ?: emptyMap()
            val mainBackupFiles = mainMediaDir.listFiles()?.associate { it.name to it.lastModified() } ?: emptyMap()
            
            for (mediaFile in mediaFiles) {
                val fileName = mediaFile.name
                val activeLastModified = mediaFile.lastModified()
                
                val destAndroidLastModified = androidBackupFiles[fileName]
                if (destAndroidLastModified == null || activeLastModified > destAndroidLastModified + 1000) {
                    copyFile(mediaFile, File(androidMediaDir, fileName))
                }
                
                val destMainLastModified = mainBackupFiles[fileName]
                if (destMainLastModified == null || activeLastModified > destMainLastModified + 1000) {
                    copyFile(mediaFile, File(mainMediaDir, fileName))
                }
            }
        }
    }

    // mirror the deletion in both backup locations
    fun deleteBackupFile(context: Context, fileName: String) {
        val androidBackupDir = File(context.getExternalFilesDir(null), "SoulVault_Data")
        val mainRootBackupDir = getMainRootBackupDir(context)

        val androidMediaFile = File(androidBackupDir, "media/$fileName")
        val mainMediaFile = File(mainRootBackupDir, "media/$fileName")

        if (androidMediaFile.exists()) {
            androidMediaFile.delete()
        }
        if (mainMediaFile.exists()) {
            mainMediaFile.delete()
        }
    }
}
