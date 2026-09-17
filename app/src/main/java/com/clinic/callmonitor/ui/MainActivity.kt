package com.clinic.callmonitor.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.clinic.callmonitor.CallMonitorApp
import com.clinic.callmonitor.R
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

/**
 * شاشة الموظف البسيطة التي تعرض حالته وتسمح له بإدخال التوكن 
 * الذي تم إصداره من السيرفر.
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val name = CallMonitorApp.prefs.getString(CallMonitorApp.KEY_EMPLOYEE_NAME, "")
        val currentToken = CallMonitorApp.prefs.getString(CallMonitorApp.KEY_API_TOKEN, "")
        
        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val etApiToken = findViewById<EditText>(R.id.etApiToken)
        val btnSaveToken = findViewById<Button>(R.id.btnSaveToken)

        tvStatus.text = "مرحبًا $name\nالتطبيق يعمل في الخلفية لمراقبة الجودة."
        
        if (!currentToken.isNullOrEmpty()) {
            etApiToken.setText(currentToken)
        }

        btnSaveToken.setOnClickListener {
            val newToken = etApiToken.text.toString().trim()
            if (newToken.isNotEmpty()) {
                CallMonitorApp.prefs.edit()
                    .putString(CallMonitorApp.KEY_API_TOKEN, newToken)
                    .apply()
                Toast.makeText(this, "تم حفظ التوكن بنجاح!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "يرجى إدخال التوكن أولاً", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
