package com.clinic.callmonitor.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * بعض الأجهزة (خصوصًا Samsung One UI الحديثة) بتمنع أي تطبيق تالت من
 * قراءة TelephonyManager state بدقة أو تسجيل الصوت وقت المكالمة تمامًا،
 * بغض النظر عن الصلاحيات. في الحالة دي، مفيش حل برمجي 100% مضمون -
 * الخيارات المتاحة فعليًا:
 *   1) تجربة أجهزة/ماركات مختلفة (Xiaomi/Realme غالبًا أكثر تساهلاً)
 *   2) استخدام تطبيق كمكالم افتراضي (Default Dialer) مع Call Screening API
 *      (يتطلب شغل إضافي وتقييد أكبر على أي مكالمة تعدي غير من خلاله)
 *   3) قبول إن بعض الأجهزة هتوفر سجل المكالمات (وقت/رقم/مدة) بس من غير
 *      تسجيل صوتي - وده أحسن من ولا حاجة.
 * الكلاس ده جاهز كهيكل تقدر تبني عليه منطق إضافي لو احتجت.
 */
class CallAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // مكان مخصص لمنطق إضافي لو احتجت اكتشاف حالة المكالمة من واجهة
        // تطبيق الهاتف نفسه على أجهزة معينة
    }

    override fun onInterrupt() {}
}
