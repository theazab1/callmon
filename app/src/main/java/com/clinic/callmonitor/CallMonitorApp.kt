package com.clinic.callmonitor

import android.app.Application
import android.content.Context
import android.content.SharedPreferences

class CallMonitorApp : Application() {

    companion object {
        lateinit var prefs: SharedPreferences
            private set

        const val PREFS_NAME = "call_monitor_prefs"
        const val KEY_CONSENT_GIVEN = "consent_given"
        const val KEY_EMPLOYEE_NAME = "employee_name"
        const val KEY_EMPLOYEE_ID = "employee_id"
        const val KEY_DEVICE_UUID = "device_uuid"
        const val KEY_SERVER_URL = "server_url"
        const val KEY_API_TOKEN = "api_token"

        fun isConsentGiven(context: Context): Boolean =
            prefs.getBoolean(KEY_CONSENT_GIVEN, false)
    }

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        if (!prefs.contains(KEY_DEVICE_UUID)) {
            prefs.edit().putString(KEY_DEVICE_UUID, java.util.UUID.randomUUID().toString()).apply()
        }
        // عنوان السيرفر الافتراضي - يتغير من شاشة الإعدادات أو يوضع وقت البناء
        if (!prefs.contains(KEY_SERVER_URL)) {
            prefs.edit().putString(KEY_SERVER_URL, "https://callmon.wellio.org/api/").apply()
        }
    }
}
