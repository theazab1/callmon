package com.clinic.callmonitor.util

import android.content.Context
import com.clinic.callmonitor.CallMonitorApp
import java.io.File

/**
 * بنحتفظ بسجل محلي (اسم الملف + هل اترفع أو لأ) في SharedPreferences بسيط.
 * لو ملف اترفع بنجاح واختفى بعدين من مجلد الجهاز من غير ما إحنا نمسحه -
 * ده معناه إن حد مسحه يدويًا، ونبلغ السيرفر فورًا (شغل FileWatcherService).
 */
object RecordingStore {

    private const val PREFS_TRACK = "recording_tracker"

    fun getRecordingsDir(context: Context): File {
        val dir = File(context.filesDir, "call_recordings")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun markUploaded(file: File) {
        val prefs = CallMonitorApp.prefs
        val editor = prefs.edit()
        editor.putBoolean("uploaded_${file.name}", true)
        editor.putLong("size_${file.name}", file.length())
        editor.apply()
    }

    fun queueForRetry(file: File) {
        val prefs = CallMonitorApp.prefs
        val pending = prefs.getStringSet("pending_uploads", mutableSetOf())!!.toMutableSet()
        pending.add(file.absolutePath)
        prefs.edit().putStringSet("pending_uploads", pending).apply()
    }

    fun getPendingUploads(): Set<String> =
        CallMonitorApp.prefs.getStringSet("pending_uploads", mutableSetOf()) ?: emptySet()

    fun getTrackedUploadedFiles(context: Context): List<File> {
        val dir = getRecordingsDir(context)
        val prefs = CallMonitorApp.prefs
        return dir.listFiles()?.filter { prefs.getBoolean("uploaded_${it.name}", false) } ?: emptyList()
    }

    /** بيرجع أسماء الملفات اللي اترفعت بنجاح لكن مش موجودة دلوقتي على الجهاز = محذوفة يدويًا */
    fun findDeletedAfterUpload(context: Context): List<String> {
        val prefs = CallMonitorApp.prefs
        val dir = getRecordingsDir(context)
        val existingNames = dir.listFiles()?.map { it.name }?.toSet() ?: emptySet()
        val allEntries = prefs.all.keys.filter { it.startsWith("uploaded_") }
        val deleted = mutableListOf<String>()
        for (key in allEntries) {
            val fileName = key.removePrefix("uploaded_")
            val wasUploaded = prefs.getBoolean(key, false)
            val alreadyReported = prefs.getBoolean("deletion_reported_$fileName", false)
            if (wasUploaded && !existingNames.contains(fileName) && !alreadyReported) {
                deleted.add(fileName)
            }
        }
        return deleted
    }

    fun markDeletionReported(fileName: String) {
        CallMonitorApp.prefs.edit().putBoolean("deletion_reported_$fileName", true).apply()
    }
}
