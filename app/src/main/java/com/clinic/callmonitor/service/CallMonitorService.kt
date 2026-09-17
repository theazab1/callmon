package com.clinic.callmonitor.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.clinic.callmonitor.CallMonitorApp
import com.clinic.callmonitor.R
import com.clinic.callmonitor.network.ApiClient
import com.clinic.callmonitor.network.CallLogEntry
import com.clinic.callmonitor.util.RecordingStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CallMonitorService : Service() {

    companion object {
        const val ACTION_CALL_STARTED = "ACTION_CALL_STARTED"
        const val ACTION_CALL_ENDED = "ACTION_CALL_ENDED"
        const val EXTRA_PHONE_NUMBER = "EXTRA_PHONE_NUMBER"
        const val NOTIF_CHANNEL_ID = "call_monitor_channel"
        const val NOTIF_ID = 101
    }

    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var callStartTime: Long = 0L
    private var currentNumber: String = ""
    private var reminderView: TextView? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification("جاري تشغيل خدمة مراقبة المكالمات"))

        when (intent?.action) {
            ACTION_CALL_STARTED -> {
                currentNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER) ?: ""
                startRecording()
                showReminderOverlay()
            }
            ACTION_CALL_ENDED -> {
                stopRecordingAndUpload()
                hideReminderOverlay()
            }
        }
        return START_STICKY
    }

    private fun startRecording() {
        callStartTime = System.currentTimeMillis()
        val employeeId = CallMonitorApp.prefs.getString(CallMonitorApp.KEY_EMPLOYEE_ID, "unknown")
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "call_${employeeId}_${ts}.m4a"
        currentFile = File(RecordingStore.getRecordingsDir(this), fileName)

        recorder = MediaRecorder().apply {
            try {
                // ملحوظة: MediaRecorder.AudioSource.VOICE_CALL غير مسموح على
                // أغلب أجهزة أندرويد 10+. VOICE_COMMUNICATION هو الأقرب المتاح
                // فعليًا (بيسجل صوت الميكروفون بشكل أساسي، جودة الطرف التاني
                // بتختلف حسب الجهاز). لو عندك أجهزة معينة بتدعم VOICE_CALL
                // جرّبها بدل، وإلا استخدم الافتراضي ده.
                setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(currentFile!!.absolutePath)
                prepare()
                start()
            } catch (e: Exception) {
                // بعض الأجهزة بترفض التسجيل تمامًا أثناء مكالمة - نسجل الفشل
                // كحدث عشان تظهر عندك في الأدمن بدل ما تفتكر إن في تلاعب
                logRecordingFailure(e.message ?: "unknown error")
            }
        }
    }

    private fun stopRecordingAndUpload() {
        val durationSeconds = ((System.currentTimeMillis() - callStartTime) / 1000).toInt()
        try {
            recorder?.stop()
        } catch (_: Exception) { }
        recorder?.release()
        recorder = null

        val fileToUpload = currentFile
        val employeeId = CallMonitorApp.prefs.getString(CallMonitorApp.KEY_EMPLOYEE_ID, "unknown")!!
        val deviceUuid = CallMonitorApp.prefs.getString(CallMonitorApp.KEY_DEVICE_UUID, "")!!

        // نسجل بيانات المكالمة (السجل) دايمًا حتى لو ملف الصوت فشل
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ApiClient.service.uploadCallLog(
                    CallLogEntry(
                        deviceUuid = deviceUuid,
                        employeeId = employeeId,
                        phoneNumber = currentNumber,
                        callType = "UNKNOWN", // تفصيليًا هيتحدد من CallLog Provider - انظر util/CallLogSync
                        startTimestamp = callStartTime,
                        durationSeconds = durationSeconds,
                        recordingFileName = fileToUpload?.name
                    )
                )
            } catch (_: Exception) { }

            if (fileToUpload != null && fileToUpload.exists() && fileToUpload.length() > 0) {
                uploadRecordingFile(fileToUpload, deviceUuid, employeeId)
            }
        }
    }

    private suspend fun uploadRecordingFile(file: File, deviceUuid: String, employeeId: String) {
        try {
            val reqFile = file.asRequestBody("audio/mp4".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", file.name, reqFile)
            val resp = ApiClient.service.uploadRecording(
                deviceUuid.toRequestBody("text/plain".toMediaTypeOrNull()),
                employeeId.toRequestBody("text/plain".toMediaTypeOrNull()),
                file.name.toRequestBody("text/plain".toMediaTypeOrNull()),
                filePart
            )
            if (resp.isSuccessful) {
                // نحذف النسخة المحلية بعد تأكيد نجاح الرفع فقط - عشان الموظف
                // ميقدرش "يمسح قبل ما يترفع" وتضيع النسخة
                RecordingStore.markUploaded(file)
            } else {
                RecordingStore.queueForRetry(file)
            }
        } catch (e: Exception) {
            RecordingStore.queueForRetry(file)
        }
    }

    private fun logRecordingFailure(reason: String) {
        // TODO: إرسال حدث "فشل تسجيل" للسيرفر عشان الأدمن يعرف إن الجهاز ده
        // بيمنع التسجيل بدل ما يفسرها غلط كحذف متعمد
    }

    private fun showReminderOverlay() {
        if (!Settings.canDrawOverlays(this)) return
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        reminderView = TextView(this).apply {
            text = "ذكّر العميل: \"هذه المكالمة قد تُسجل لأغراض الجودة\""
            setBackgroundColor(0xCC222222.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(24, 16, 24, 16)
            textSize = 14f
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
            y = 100
        }
        try {
            wm.addView(reminderView, params)
        } catch (_: Exception) { }
    }

    private fun hideReminderOverlay() {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        reminderView?.let {
            try { wm.removeView(it) } catch (_: Exception) { }
        }
        reminderView = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIF_CHANNEL_ID,
                "مراقبة المكالمات",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification =
        NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
            .setContentTitle("تطبيق العيادة نشط")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .build()

    override fun onBind(intent: Intent?): IBinder? = null
}
