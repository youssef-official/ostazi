package com.example.ui.theme

object AppStrings {
    fun isEn(lang: String): Boolean = lang == "en"

    // Bottom Navigation
    fun tabHome(lang: String) = if (isEn(lang)) "Home" else "الرئيسية"
    fun tabGroups(lang: String) = if (isEn(lang)) "Groups" else "المجموعات"
    fun tabStudents(lang: String) = if (isEn(lang)) "Students" else "الطلاب"
    fun tabTimetable(lang: String) = if (isEn(lang)) "Timetable" else "الجدول"
    fun tabPayments(lang: String) = if (isEn(lang)) "Finance" else "المالية"
    fun tabSession(lang: String) = if (isEn(lang)) "Session" else "بالحصة"
    fun settings(lang: String) = if (isEn(lang)) "Settings" else "الإعدادات"

    // App Header & Top Bar
    fun appTitle(lang: String) = if (isEn(lang)) "Ostazy Plus - Teacher App" else "أستاذي + - دفتر المعلم"
    fun cloudSync(lang: String) = if (isEn(lang)) "Cloud Backup" else "النسخ السحابي"
    fun vipTitle(lang: String) = if (isEn(lang)) "VIP Features" else "قائمة المميزات"

    // Dashboard
    fun welcomeTeacher(lang: String, name: String) = if (isEn(lang)) "Welcome, $name" else "مرحباً بك، $name"
    fun todaysClasses(lang: String) = if (isEn(lang)) "Today's Schedule" else "حصص اليوم"
    fun totalStudents(lang: String) = if (isEn(lang)) "Total Students" else "إجمالي الطلاب"
    fun totalGroups(lang: String) = if (isEn(lang)) "Groups Count" else "عدد المجموعات"
    fun attendanceRate(lang: String) = if (isEn(lang)) "Attendance Rate" else "نسبة الحضور"
    fun monthlyRevenue(lang: String) = if (isEn(lang)) "Monthly Income" else "إيرادات الشهر"
    fun quickActions(lang: String) = if (isEn(lang)) "Quick Actions" else "إجراءات سريعة"
    fun addStudentBtn(lang: String) = if (isEn(lang)) "+ Add Student" else "+ إضافة طالب"
    fun addGroupBtn(lang: String) = if (isEn(lang)) "+ Add Group" else "+ إضافة مجموعة"

    // Groups Screen
    fun groupManagement(lang: String) = if (isEn(lang)) "Group Management" else "إدارة المجموعات"
    fun centerCategory(lang: String) = if (isEn(lang)) "Center" else "السنتر"
    fun onlineCategory(lang: String) = if (isEn(lang)) "Online" else "أونلاين"
    fun privateCategory(lang: String) = if (isEn(lang)) "Private" else "خاص"
    fun addCenter(lang: String) = if (isEn(lang)) "+ Add Center" else "+ إضافة سنتر"
    fun addOnline(lang: String) = if (isEn(lang)) "+ Add Online" else "+ إضافة أونلاين"
    fun addPrivate(lang: String) = if (isEn(lang)) "+ Add Private" else "+ إضافة خاص"
    fun allGroups(lang: String) = if (isEn(lang)) "All" else "الكل"
    fun groupSubtitle(lang: String) = if (isEn(lang)) "View and manage center, online and private groups" else "عرض وإدارة مجموعات السنتر والأونلاين والدروس الخاصة"
    fun groupsCount(lang: String, count: Int) = if (isEn(lang)) "$count groups" else "$count مجموعة"
    fun unnamedGroup(lang: String) = if (isEn(lang)) "Unnamed group" else "مجموعة بدون اسم"
    fun subject(lang: String) = if (isEn(lang)) "Subject" else "المادة"
    fun general(lang: String) = if (isEn(lang)) "General" else "عام"
    fun center(lang: String) = if (isEn(lang)) "Center" else "سنتر"
    fun online(lang: String) = if (isEn(lang)) "Online" else "أونلاين"
    fun privateLesson(lang: String) = if (isEn(lang)) "Private" else "درس خاص"
    fun studentCount(lang: String) = if (isEn(lang)) "Students" else "عدد الطلاب"
    fun paid(lang: String) = if (isEn(lang)) "Paid" else "تم الدفع"
    fun notPaid(lang: String) = if (isEn(lang)) "Unpaid" else "لم يدفع"
    fun sessionsMonthly(lang: String, count: Int) = if (isEn(lang)) "$count sessions/month" else "$count حصة شهرياً"

    // Security dialogs
    fun identityTitle(lang: String) = if (isEn(lang)) "Confirm identity" else "تأكيد الهوية"
    fun identitySubtitle(lang: String) = if (isEn(lang)) "Use your fingerprint to access this section" else "استخدم بصمة الإصبع للوصول إلى هذا القسم"
    fun cancel(lang: String) = if (isEn(lang)) "Cancel" else "إلغاء"
    fun financeLock(lang: String) = if (isEn(lang)) "Finance lock" else "قفل قسم المالية"
    fun enterFinancePin(lang: String) = if (isEn(lang)) "Enter your PIN to open finance and student accounts." else "يرجى إدخال الرقم السري لفتح قسم المالية وحسابات الطلاب."
    fun pinLabel(lang: String) = if (isEn(lang)) "5-digit PIN" else "الرقم السري (5 أرقام)"
    fun wrongPin(lang: String) = if (isEn(lang)) "Incorrect PIN. Try again." else "الرقم السري غير صحيح، حاول مرة أخرى"
    fun confirmOpen(lang: String) = if (isEn(lang)) "Confirm and open" else "تأكيد وفتح"

    // Students Screen
    fun studentList(lang: String) = if (isEn(lang)) "Student List" else "قائمة الطلاب"
    fun searchPlaceholder(lang: String) = if (isEn(lang)) "Search by student name or phone..." else "ابحث باسم الطالب أو الرقم..."
    fun searchBtn(lang: String) = if (isEn(lang)) "Search" else "بحث"
    fun filterAll(lang: String) = if (isEn(lang)) "All Students" else "جميع الطلاب"

    // Timetable Screen
    fun weeklyTimetable(lang: String) = if (isEn(lang)) "Weekly Timetable" else "الجدول الأسبوعي"

    // Financial Screen
    fun financeTitle(lang: String) = if (isEn(lang)) "Finance & Payments" else "إدارة المالية والدفعات"
    fun paidStatus(lang: String) = if (isEn(lang)) "Paid" else "تم الدفع"
    fun unpaidStatus(lang: String) = if (isEn(lang)) "Unpaid" else "لم يدفع"
    fun partialStatus(lang: String) = if (isEn(lang)) "Partial" else "دفع جزئي"
}
