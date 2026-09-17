package com.clinic.callmonitor.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.clinic.callmonitor.CallMonitorApp
import com.clinic.callmonitor.service.CallMonitorService

/**
 * ملحوظة مهمة: أندرويد ما بيدّيش أي تطبيق قدرة على "حقن" رسالة صوتية
 * جوه صوت المكالمة نفسها للطرفين. الحل العملي هنا هو تذكير بصري (Overlay)
 * يظهر للموظف فور بدء المكالمة يذكّره إنه يقول جملة التنبيه بصوته -
 * ده شرح ليه محتاج تدرب الموظفين يقولوها فعليًا في أول ثانيتين من المكالمة.
 * البديل الاحترافي الحقيقي هو IVR على مستوى شركة الاتصالات/السنترال
 * (خارج نطاق تطبيق الموبايل ده تمامًا).
 */
class CallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (!CallMonitorApp.isConsentGiven(context)) return // لا تسجيل بدون موافقة مسجلة

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING,
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                val serviceIntent = Intent(context, CallMonitorService::class.java).apply {
                    action = CallMonitorService.ACTION_CALL_STARTED
                    putExtra(CallMonitorService.EXTRA_PHONE_NUMBER, incomingNumber ?: "")
                }
                context.startForegroundService(serviceIntent)
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                val serviceIntent = Intent(context, CallMonitorService::class.java).apply {
                    action = CallMonitorService.ACTION_CALL_ENDED
                }
                context.startForegroundService(serviceIntent)
            }
        }
    }
}
