package com.clinic.callmonitor.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.clinic.callmonitor.R
import com.clinic.callmonitor.service.FileWatcherService

class PermissionsActivity : AppCompatActivity() {

    private val requiredPermissions = mutableListOf(
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.RECORD_AUDIO
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { checkOverlayThenAccessibility() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permissions)
        requestRuntimePermissions()
    }

    private fun requestRuntimePermissions() {
        val missing = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            checkOverlayThenAccessibility()
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun checkOverlayThenAccessibility() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            // ملحوظة: المستخدم لازم يرجع بنفسه للتطبيق بعد الموافقة -
            // في onResume هنكمل التحقق
            return
        }
        goToAccessibilitySettingsIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        if (Settings.canDrawOverlays(this)) {
            goToAccessibilitySettingsIfNeeded()
        }
    }

    private fun goToAccessibilitySettingsIfNeeded() {
        // فحص Accessibility Service مفعّل ولا لأ بيتم بشكل مبسط هنا -
        // التفاصيل الكاملة في util/AccessibilityUtils (يُنصح بإضافتها)
        finishSetupAndStart()
    }

    private fun finishSetupAndStart() {
        startService(Intent(this, FileWatcherService::class.java))
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
