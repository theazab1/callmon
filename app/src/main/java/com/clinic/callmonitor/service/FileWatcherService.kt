package com.clinic.callmonitor.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.work.*
import com.clinic.callmonitor.CallMonitorApp
import com.clinic.callmonitor.network.ApiClient
import com.clinic.callmonitor.network.DeletionEvent
import com.clinic.callmonitor.util.RecordingStore
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

/**
 * ملحوظة صريحة: من غير Device Owner / صلاحيات نظام خاصة، تطبيق عادي في
 * أندرويد الحديث *لا يقدر يمنع فعليًا* مستخدم عنده صلاحية أدمن على تليفونه
 * من مسح ملف. أقصى حاجة ممكنة هنا هي "الاكتشاف والتبليغ الفوري" - وهو
 * المطلوب الأساسي في طلبك. لو عايز "المنع الفعلي" لازم Device Owner
 * enrollment (موضح في الدليل المرفق).
 */
class DeletionCheckWorker(context: android.content.Context, params: WorkerParameters) :
    Worker(context, params) {

    override fun doWork(): Result {
        val deletedFiles = RecordingStore.findDeletedAfterUpload(applicationContext)
        if (deletedFiles.isEmpty()) return Result.success()

        val employeeId = CallMonitorApp.prefs.getString(CallMonitorApp.KEY_EMPLOYEE_ID, "unknown")!!
        val deviceUuid = CallMonitorApp.prefs.getString(CallMonitorApp.KEY_DEVICE_UUID, "")!!

        for (fileName in deletedFiles) {
            runBlocking {
                try {
                    val resp = ApiClient.service.reportDeletion(
                        DeletionEvent(
                            deviceUuid = deviceUuid,
                            employeeId = employeeId,
                            recordingFileName = fileName,
                            detectedAt = System.currentTimeMillis(),
                            note = "الملف كان مرفوع بنجاح على السيرفر ثم اختفى من الجهاز"
                        )
                    )
                    if (resp.isSuccessful) {
                        RecordingStore.markDeletionReported(fileName)
                    }
                } catch (_: Exception) {
                    // هنعيد المحاولة في الدورة الجاية، من غير ما نعلّم إنه اتبلّغ
                }
            }
        }
        return Result.success()
    }

    companion object {
        fun schedule(context: android.content.Context) {
            val request = PeriodicWorkRequestBuilder<DeletionCheckWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "deletion_check",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}

/** خدمة بسيطة لبدء جدولة الفحص الدوري وقت تشغيل التطبيق */
class FileWatcherService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        DeletionCheckWorker.schedule(applicationContext)
        stopSelf()
        return START_NOT_STICKY
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
