package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.backup.BackupData
import com.example.backup.BackupManager
import com.example.data.*
import com.example.notification.AlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = TeacherRepository(db.appDao())

    val todayDateString: String = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())
    val todayDisplayDate: String = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("ar")).format(Date())
    val currentMonthYear: String = SimpleDateFormat("yyyy-MM", Locale.ENGLISH).format(Date())

    val todayArabicDayName: String = getTodayArabicDay()

    private val prefs = application.getSharedPreferences("teacher_prefs", Context.MODE_PRIVATE)

    private val _teacherName = MutableStateFlow(prefs.getString("teacher_name", "أ. أحمد محمود") ?: "أ. أحمد محمود")
    val teacherName: StateFlow<String> = _teacherName.asStateFlow()

    private val _teacherSubject = MutableStateFlow(prefs.getString("teacher_subject", "اللغة العربية") ?: "اللغة العربية")
    val teacherSubject: StateFlow<String> = _teacherSubject.asStateFlow()

    private val _teacherImageUri = MutableStateFlow(prefs.getString("teacher_image_uri", "") ?: "")
    val teacherImageUri: StateFlow<String> = _teacherImageUri.asStateFlow()

    private val _appLanguage = MutableStateFlow(prefs.getString("app_language", "en") ?: "en")
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    private val _themeMode = MutableStateFlow(prefs.getString("theme_mode", "SYSTEM") ?: "SYSTEM")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _savedManualReport = MutableStateFlow(prefs.getString("saved_manual_report", "السلام عليكم ورحمة الله وبركاته،\nتقرير اليوم لمجموعتنا المميزة:") ?: "السلام عليكم ورحمة الله وبركاته،\nتقرير اليوم لمجموعتنا المميزة:")
    val savedManualReport: StateFlow<String> = _savedManualReport.asStateFlow()

    fun saveManualReport(reportText: String) {
        prefs.edit().putString("saved_manual_report", reportText).apply()
        _savedManualReport.value = reportText
    }

    fun setAppLanguage(lang: String) {
        prefs.edit().putString("app_language", lang).apply()
        _appLanguage.value = lang
    }

    fun toggleAppLanguage() {
        val nextLang = if (_appLanguage.value == "en") "ar" else "en"
        setAppLanguage(nextLang)
    }

    fun setThemeMode(mode: String) {
        prefs.edit().putString("theme_mode", mode).apply()
        _themeMode.value = mode
    }

    fun toggleThemeMode() {
        val nextMode = when (_themeMode.value) {
            "LIGHT" -> "DARK"
            "DARK" -> "SYSTEM"
            else -> "LIGHT"
        }
        setThemeMode(nextMode)
    }

    private val _savedLogStartDate = MutableStateFlow(prefs.getLong("log_start_date", 0L))
    val savedLogStartDate: StateFlow<Long> = _savedLogStartDate.asStateFlow()

    private val _savedLogEndDate = MutableStateFlow(prefs.getLong("log_end_date", 0L))
    val savedLogEndDate: StateFlow<Long> = _savedLogEndDate.asStateFlow()

    fun saveLogDateRange(startMillis: Long, endMillis: Long) {
        prefs.edit()
            .putLong("log_start_date", startMillis)
            .putLong("log_end_date", endMillis)
            .apply()
        _savedLogStartDate.value = startMillis
        _savedLogEndDate.value = endMillis
    }

    fun saveTeacherProfile(name: String, subject: String, imageUri: String = _teacherImageUri.value) {
        prefs.edit()
            .putString("teacher_name", name)
            .putString("teacher_subject", subject)
            .putString("teacher_image_uri", imageUri)
            .apply()
        _teacherName.value = name
        _teacherSubject.value = subject
        _teacherImageUri.value = imageUri
    }

    data class TeacherProfileData(val name: String, val subject: String, val imageUri: String)

    val teacherProfile: StateFlow<TeacherProfileData> = combine(_teacherName, _teacherSubject, _teacherImageUri) { name, subject, img ->
        TeacherProfileData(name, subject, img)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, TeacherProfileData(_teacherName.value, _teacherSubject.value, _teacherImageUri.value))

    val groups: StateFlow<List<GroupEntity>> = repository.allGroups
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allStudents: StateFlow<List<StudentEntity>> = repository.allStudents
        .map { it.sortedBy { student -> student.fullName } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val students: StateFlow<List<StudentEntity>> = allStudents

    val attendanceForToday: StateFlow<List<AttendanceRecordEntity>> = repository.getAttendanceForDate(todayDateString)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allExams: StateFlow<List<ExamRecordEntity>> = repository.allExams
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val paymentsForMonth: StateFlow<List<PaymentRecordEntity>> = repository.getPaymentsForMonth(currentMonthYear)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allPayments: StateFlow<List<PaymentRecordEntity>> = repository.allPayments
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // --- Group Operations ---
    fun addGroup(
        name: String, subject: String,
        day1: String, day2: String?, day3: String?, day4: String? = null, day5: String? = null, day6: String? = null, day7: String? = null,
        timeSlot: String, timeSlot2: String?, timeSlot3: String?, timeSlot4: String? = null, timeSlot5: String? = null, timeSlot6: String? = null, timeSlot7: String? = null,
        fee: Double, groupType: String, paymentType: String = "MONTHLY", whatsappGroupUrl: String = "", notes: String = "", context: Context
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentList = groups.value
                val maxOrder = currentList.maxOfOrNull { it.sortOrder } ?: 0
                val newGroup = GroupEntity(
                    name = name.trim(),
                    subject = subject.trim(),
                    day1 = day1,
                    day2 = day2,
                    day3 = day3,
                    day4 = day4,
                    day5 = day5,
                    day6 = day6,
                    day7 = day7,
                    timeSlot = timeSlot,
                    timeSlot2 = timeSlot2,
                    timeSlot3 = timeSlot3,
                    timeSlot4 = timeSlot4,
                    timeSlot5 = timeSlot5,
                    timeSlot6 = timeSlot6,
                    timeSlot7 = timeSlot7,
                    monthlyFee = fee,
                    groupType = groupType,
                    paymentType = paymentType,
                    whatsappGroupUrl = whatsappGroupUrl.trim(),
                    sortOrder = maxOrder + 1,
                    notes = notes.trim()
                )
                val id = repository.insertGroup(newGroup).toInt()
                try {
                    AlarmScheduler.scheduleAlarmForGroup(context, newGroup.copy(id = id))
                } catch (_: Throwable) { }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    fun updateGroup(group: GroupEntity, context: Context) {
        viewModelScope.launch {
            repository.updateGroup(group)
            AlarmScheduler.scheduleAlarmForGroup(context, group)
        }
    }

    fun deleteGroup(group: GroupEntity) {
        viewModelScope.launch {
            repository.deleteGroup(group)
        }
    }

    fun moveGroupUp(group: GroupEntity) {
        viewModelScope.launch {
            repository.moveGroupUp(group, groups.value)
        }
    }

    fun moveGroupDown(group: GroupEntity) {
        viewModelScope.launch {
            repository.moveGroupDown(group, groups.value)
        }
    }

    // --- Student Operations ---
    fun addStudent(
        fullName: String,
        groupId: Int,
        parentPhone: String,
        studentPhone: String = "",
        discountAmount: Double = 0.0
    ) {
        viewModelScope.launch {
            val student = StudentEntity(
                fullName = fullName,
                groupId = groupId,
                parentPhone = parentPhone,
                studentPhone = studentPhone,
                discountAmount = discountAmount
            )
            val studentId = repository.insertStudent(student).toInt()
        }
    }

    fun updateStudent(student: StudentEntity) {
        viewModelScope.launch {
            repository.updateStudent(student)
        }
    }

    fun deleteStudent(student: StudentEntity) {
        viewModelScope.launch {
            repository.deleteStudent(student)
        }
    }

    // --- Attendance & Homework & Recitation & Interaction ---
    fun setAttendanceAndHomework(
        studentId: Int,
        date: String = todayDateString,
        attendanceStatus: String,
        homeworkStatus: String,
        interactionStatus: String = "ممتاز",
        recitationGrade: String = "",
        recitationStatus: String = "ممتاز",
        notes: String? = null
    ) {
        viewModelScope.launch {
            repository.setAttendanceAndHomework(
                studentId,
                date,
                attendanceStatus,
                homeworkStatus,
                interactionStatus,
                recitationGrade,
                recitationStatus,
                notes
            )
        }
    }

    fun setStudentNote(
        studentId: Int,
        date: String = todayDateString,
        note: String
    ) {
        viewModelScope.launch {
            repository.setStudentNote(studentId, date, note)
        }
    }

    fun batchSetAttendance(
        studentIds: List<Int>,
        date: String = todayDateString,
        attendanceStatus: String? = null,
        homeworkStatus: String? = null,
        interactionStatus: String? = null,
        recitationGrade: String? = null,
        notes: String? = null
    ) {
        viewModelScope.launch {
            repository.batchSetAttendance(
                studentIds,
                date,
                attendanceStatus,
                homeworkStatus,
                interactionStatus,
                recitationGrade,
                notes
            )
        }
    }

    fun setSessionPayment(
        studentId: Int,
        date: String = todayDateString,
        isPaid: Boolean,
        paidAmount: Double = 0.0,
        paymentStatus: String = if (isPaid) "PAID" else "UNPAID"
    ) {
        viewModelScope.launch {
            repository.setSessionPayment(studentId, date, isPaid, paidAmount, paymentStatus)
        }
    }

    fun batchSetSessionPayment(
        studentIds: List<Int>,
        date: String = todayDateString,
        isPaid: Boolean,
        defaultAmount: Double = 0.0
    ) {
        viewModelScope.launch {
            repository.batchSetSessionPayment(studentIds, date, isPaid, defaultAmount)
        }
    }

    fun getAttendanceForDateFlow(date: String): Flow<List<AttendanceRecordEntity>> {
        return repository.getAttendanceForDate(date)
    }

    fun getAttendanceForMonthFlow(monthPrefix: String): Flow<List<AttendanceRecordEntity>> {
        return repository.getAttendanceForMonth(monthPrefix)
    }

    fun getAttendanceBetweenDatesFlow(startDate: String, endDate: String): Flow<List<AttendanceRecordEntity>> {
        return repository.getAttendanceBetweenDates(startDate, endDate)
    }

    fun getPaymentsForMonthFlow(monthYear: String): Flow<List<PaymentRecordEntity>> {
        return repository.getPaymentsForMonth(monthYear)
    }

    // --- Exams ---
    fun saveExams(studentId: Int, exam1: String?, exam2: String?, exam3: String?, exam4: String? = null, exam5: String? = null) {
        viewModelScope.launch {
            repository.saveExams(studentId, exam1, exam2, exam3, exam4, exam5)
        }
    }

    fun getAttendanceForStudentFlow(studentId: Int): Flow<List<AttendanceRecordEntity>> {
        return repository.getAttendanceForStudent(studentId)
    }

    fun getPaymentsForStudentFlow(studentId: Int): Flow<List<PaymentRecordEntity>> {
        return repository.getAllPaymentsForStudent(studentId)
    }

    fun getExamForStudentFlow(studentId: Int): Flow<ExamRecordEntity?> {
        return repository.getExamForStudent(studentId)
    }

    // --- Unlimited Individual Student Exams (50+) ---
    fun getIndividualExamsForStudentFlow(studentId: Int): Flow<List<StudentIndividualExamEntity>> {
        return repository.getIndividualExamsForStudent(studentId)
    }

    fun addIndividualExam(
        studentId: Int,
        examName: String,
        score: String,
        maxScore: String = "100",
        date: String = todayDateString,
        notes: String = ""
    ) {
        viewModelScope.launch {
            repository.addIndividualExam(
                StudentIndividualExamEntity(
                    studentId = studentId,
                    examName = examName,
                    score = score,
                    maxScore = maxScore,
                    date = date,
                    notes = notes
                )
            )
        }
    }

    fun deleteIndividualExam(exam: StudentIndividualExamEntity) {
        viewModelScope.launch {
            repository.deleteIndividualExam(exam)
        }
    }

    fun updateIndividualExam(exam: StudentIndividualExamEntity) {
        viewModelScope.launch {
            repository.addIndividualExam(exam)
        }
    }

    // --- Payments ---
    fun setPaymentStatus(
        studentId: Int,
        status: String,
        remainingAmount: Double = 0.0,
        paymentDate: String? = null,
        monthYear: String = currentMonthYear
    ) {
        viewModelScope.launch {
            repository.setPaymentStatus(studentId, monthYear, status, remainingAmount, paymentDate)
        }
    }

    fun setPaymentStatusWithDiscount(
        studentId: Int,
        status: String,
        discountAmount: Double = 0.0,
        paidAmount: Double = 0.0,
        remainingAmount: Double = 0.0,
        paymentDate: String? = null,
        monthYear: String = currentMonthYear
    ) {
        viewModelScope.launch {
            repository.setPaymentStatusWithDiscount(
                studentId = studentId,
                monthYear = monthYear,
                status = status,
                discountAmount = discountAmount,
                paidAmount = paidAmount,
                remainingAmount = remainingAmount,
                paymentDate = paymentDate
            )
        }
    }

    fun setSessionDetails(
        studentId: Int,
        date: String = todayDateString,
        attendanceStatus: String,
        homeworkStatus: String,
        interactionStatus: String = "ممتاز",
        recitationGrade: String = "",
        notes: String = "",
        sessionPaid: Boolean = false,
        sessionPaidAmount: Double = 0.0,
        sessionPaymentStatus: String = "UNPAID"
    ) {
        viewModelScope.launch {
            repository.setSessionDetails(
                studentId = studentId,
                date = date,
                attendanceStatus = attendanceStatus,
                homeworkStatus = homeworkStatus,
                interactionStatus = interactionStatus,
                recitationGrade = recitationGrade,
                notes = notes,
                sessionPaid = sessionPaid,
                sessionPaidAmount = sessionPaidAmount,
                sessionPaymentStatus = sessionPaymentStatus
            )
        }
    }

    // --- Alarms ---
    fun triggerTestNotification(context: Context, group: GroupEntity) {
        AlarmScheduler.triggerTestNotification(context, group)
    }

    // --- Backup & Restore (Local File) ---
    fun exportBackupToUri(context: Context, targetUri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val data = createBackupSnapshot()
                val jsonStr = BackupManager.toJson(data)
                context.contentResolver.openOutputStream(targetUri)?.use { outputStream ->
                    outputStream.write(jsonStr.toByteArray(Charsets.UTF_8))
                }
                onResult(true, "تم حفظ ملف النسخة الاحتياطية بنجاح في مدير الملفات! 💾")
            } catch (e: Exception) {
                onResult(false, "فشل حفظ الملف: ${e.localizedMessage}")
            }
        }
    }

    fun exportBackupData(context: Context, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val data = createBackupSnapshot()
                val uri = BackupManager.exportBackupFile(context, data)
                if (uri != null) {
                    onResult(true, "تم تصدير ملف النسخة الاحتياطية بنجاح! 📂")
                } else {
                    onResult(false, "فشل تصدير ملف النسخة الاحتياطية.")
                }
            } catch (e: Exception) {
                onResult(false, "خطأ أثناء التصدير: ${e.localizedMessage}")
            }
        }
    }

    fun importBackupDataFromUri(context: Context, uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val backupData = BackupManager.importBackupFile(context, uri)
                if (backupData != null) {
                    repository.restoreAllData(backupData)
                    onResult(true, "تم استرجاع كافة البيانات والطلاب والمجموعات والمالية بنجاح! 🎉")
                } else {
                    onResult(false, "فشل قراءة أو تحليل ملف النسخة الاحتياطية.")
                }
            } catch (e: Exception) {
                onResult(false, "خطأ أثناء الاسترجاع: ${e.localizedMessage}")
            }
        }
    }

    // --- Firebase Firestore Sync ---
    fun uploadToFirestore(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val data = createBackupSnapshot()
                BackupManager.syncToFirestore(
                    data = data,
                    onSuccess = {
                        onResult(true, "تمت المزامنة ورفع كافة البيانات إلى سحابة Firestore بنجاح! ☁️")
                    },
                    onFailure = { errorMsg ->
                        onResult(false, "فشلت المزامنة السحابية: $errorMsg")
                    }
                )
            } catch (e: Exception) {
                onResult(false, "خطأ في الاتصال: ${e.localizedMessage}")
            }
        }
    }

    fun downloadFromFirestore(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            BackupManager.restoreFromFirestore(
                onSuccess = { backupData ->
                    viewModelScope.launch {
                        repository.restoreAllData(backupData)
                        onResult(true, "تم استرجاع وتنزيل كافة البيانات والمجموعات والمالية تلقائياً من سحابة Firestore! ☁️🎉")
                    }
                },
                onFailure = { errorMsg ->
                    onResult(false, "تعذر التنزيل من السحابة: $errorMsg")
                }
            )
        }
    }

    fun wipeAllData(onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.clearAllData()
                onComplete(true, "تم مسح كافة البيانات والسجلات بنجاح! 🗑️")
            } catch (e: Exception) {
                onComplete(false, "حدث خطأ أثناء مسح البيانات: ${e.localizedMessage}")
            }
        }
    }

    fun exportStudentsToExcel(context: Context, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val students = repository.getAllStudentsList()
            val groups = repository.getAllGroupsList()
            val result = com.example.backup.ExcelManager.exportStudentsToExcel(context, students, groups)
            onResult(result.first, result.second)
        }
    }

    fun importStudentsFromExcel(context: Context, uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = com.example.backup.ExcelManager.importStudentsFromCsv(context, uri, repository)
            onResult(result.first, result.second)
        }
    }

    private suspend fun createBackupSnapshot(): BackupData {
        return BackupData(
            exportDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).format(Date()),
            groups = repository.getAllGroupsList(),
            students = repository.getAllStudentsList(),
            attendanceRecords = repository.getAllAttendanceList(),
            examRecords = repository.getAllExamsList(),
            paymentRecords = repository.getAllPaymentsList(),
            individualExams = repository.getAllIndividualExamsList()
        )
    }

    private fun getTodayArabicDay(): String {
        val cal = Calendar.getInstance()
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> "السبت"
            Calendar.SUNDAY -> "الأحد"
            Calendar.MONDAY -> "الإثنين"
            Calendar.TUESDAY -> "الثلاثاء"
            Calendar.WEDNESDAY -> "الأربعاء"
            Calendar.THURSDAY -> "الخميس"
            Calendar.FRIDAY -> "الجمعة"
            else -> "السبت"
        }
    }
}
