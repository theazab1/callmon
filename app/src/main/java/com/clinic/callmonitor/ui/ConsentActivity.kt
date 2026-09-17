package com.clinic.callmonitor.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.clinic.callmonitor.CallMonitorApp
import com.clinic.callmonitor.R
import com.clinic.callmonitor.network.ApiClient
import com.clinic.callmonitor.network.ConsentPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConsentActivity : AppCompatActivity() {

    private lateinit var cbAgree: CheckBox
    private lateinit var etName: EditText
    private lateinit var etId: EditText
    private lateinit var btnAgree: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // لو الموظف وافق قبل كده، نتخطى الشاشة دي على طول
        if (CallMonitorApp.isConsentGiven(this)) {
            startActivity(Intent(this, PermissionsActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_consent)

        cbAgree = findViewById(R.id.cbEmployeeId)
        etName = findViewById(R.id.etEmployeeName)
        etId = findViewById(R.id.etEmployeeId)
        btnAgree = findViewById(R.id.btnAgree)
        val btnDecline = findViewById<Button>(R.id.btnDecline)

        fun refreshButtonState() {
            btnAgree.isEnabled = cbAgree.isChecked &&
                etName.text.toString().trim().isNotEmpty() &&
                etId.text.toString().trim().isNotEmpty()
        }

        cbAgree.setOnCheckedChangeListener { _, _ -> refreshButtonState() }
        etName.addTextChangedListener(simpleWatcher { refreshButtonState() })
        etId.addTextChangedListener(simpleWatcher { refreshButtonState() })

        btnAgree.setOnClickListener {
            val name = etName.text.toString().trim()
            val empId = etId.text.toString().trim()
            val deviceUuid = CallMonitorApp.prefs.getString(CallMonitorApp.KEY_DEVICE_UUID, "")!!

            btnAgree.isEnabled = false

            CoroutineScope(Dispatchers.IO).launch {
                // نسجل الموافقة على السيرفر كإثبات موقّع بتاريخ ووقت - مهم قانونيًا
                val recorded = try {
                    ApiClient.service.recordConsent(
                        ConsentPayload(
                            deviceUuid = deviceUuid,
                            employeeName = name,
                            employeeId = empId,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                    true
                } catch (e: Exception) {
                    false
                }

                withContext(Dispatchers.Main) {
                    // نحفظ الموافقة محليًا حتى لو فشل الاتصال بالنت وقتها -
                    // هنعيد المحاولة لاحقًا، لكن ما نمنعش الموظف من الاستمرار
                    CallMonitorApp.prefs.edit()
                        .putBoolean(CallMonitorApp.KEY_CONSENT_GIVEN, true)
                        .putString(CallMonitorApp.KEY_EMPLOYEE_NAME, name)
                        .putString(CallMonitorApp.KEY_EMPLOYEE_ID, empId)
                        .apply()

                    if (!recorded) {
                        Toast.makeText(
                            this@ConsentActivity,
                            "تم حفظ الموافقة محليًا، سيتم إرسالها للسيرفر عند توفر الإنترنت",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    startActivity(Intent(this@ConsentActivity, PermissionsActivity::class.java))
                    finish()
                }
            }
        }

        btnDecline.setOnClickListener {
            Toast.makeText(
                this,
                "تم إلغاء التثبيت. برجاء التواصل مع الإدارة.",
                Toast.LENGTH_LONG
            ).show()
            finishAffinity()
        }
    }

    private fun simpleWatcher(onChange: () -> Unit) = object : android.text.TextWatcher {
        override fun afterTextChanged(s: android.text.Editable?) = onChange()
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }
}
