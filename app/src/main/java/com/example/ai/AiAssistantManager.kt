package com.example.ai

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.AttendanceRecordEntity
import com.example.data.GroupEntity
import com.example.data.PaymentRecordEntity
import com.example.data.StudentEntity
import com.example.data.getTimeSlotForDay
import com.example.data.meetsOnDay
import com.example.ui.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val base64Image: String? = null,
    val imageUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val actionTaken: String? = null
)

enum class MessageSender {
    USER, AI
}

object AiAssistantManager {

    private const val TAG = "AiAssistantManager"
    private const val GEMINI_MODEL = "gemini-3.5-flash"
    private const val GEMINI_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/$GEMINI_MODEL:generateContent"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    /**
     * Process prompt from voice speech or text input.
     */
    suspend fun processQuery(
        prompt: String,
        base64Image: String? = null,
        viewModel: MainViewModel,
        students: List<StudentEntity>,
        groups: List<GroupEntity>,
        attendanceToday: List<AttendanceRecordEntity>,
        payments: List<PaymentRecordEntity>,
        teacherName: String,
        context: Context
    ): ChatMessage = withContext(Dispatchers.IO) {
        val trimmedPrompt = prompt.trim()

        // If it's a regular text prompt without image, process local actions first
        if (base64Image == null) {
            val localActionResult = handleActionCommands(trimmedPrompt, viewModel, students, groups, context)
            if (localActionResult != null) {
                return@withContext localActionResult
            }

            val dataQueryResult = handleDataQueries(trimmedPrompt, students, groups, attendanceToday, payments, teacherName)
            if (dataQueryResult != null) {
                return@withContext dataQueryResult
            }
        }

        // 3. Utilize Gemini 3.5 Flash for Advanced Educational / Image Processing
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Throwable) { "" }
        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val aiResponse = callGeminiApi(trimmedPrompt, base64Image, apiKey, students, groups, attendanceToday, payments, teacherName)
                if (aiResponse.isNotBlank()) {
                    return@withContext ChatMessage(
                        sender = MessageSender.AI,
                        text = aiResponse
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gemini API error: ${e.message}", e)
            }
        }

        // 4. Default Smart Offline Response
        return@withContext ChatMessage(
            sender = MessageSender.AI,
            text = generateSmartOfflineResponse(trimmedPrompt, students, groups, attendanceToday, payments, teacherName)
        )
    }

    private fun handleActionCommands(
        prompt: String,
        viewModel: MainViewModel,
        students: List<StudentEntity>,
        groups: List<GroupEntity>,
        context: Context
    ): ChatMessage? {
        val lower = prompt.lowercase()
        val today = viewModel.todayDateString

        // 1. ADD NEW STUDENT (إضافة طالب)
        if (lower.contains("اضف طالب") || lower.contains("أضف طالب") || lower.contains("ضيف طالب") || lower.contains("تسجيل طالب جديد")) {
            val words = prompt.split(" ", "،", ",", "\n").map { it.trim() }.filter { it.isNotBlank() }
            val studentNameCandidate = extractNameAfterKeyword(prompt, listOf("طالب", "الطالب", "اسم"))
            val targetGroup = findBestMatchingGroup(prompt, groups) ?: groups.firstOrNull()
            
            // Extract phone number if present
            val phoneRegex = Regex("(01[0125][0-9]{8})")
            val phones = phoneRegex.findAll(prompt).map { it.value }.toList()
            val parentPhone = phones.getOrNull(0) ?: ""
            val studentPhone = phones.getOrNull(1) ?: ""

            if (studentNameCandidate.isNotBlank() && targetGroup != null) {
                viewModel.addStudent(
                    fullName = studentNameCandidate,
                    groupId = targetGroup.id,
                    parentPhone = parentPhone,
                    studentPhone = studentPhone,
                    discountAmount = 0.0
                )
                return ChatMessage(
                    sender = MessageSender.AI,
                    text = "تمت إضافة الطالب الجديد بنجاح 🎓✨\n\n👤 *الاسم:* $studentNameCandidate\n📚 *المجموعة:* ${targetGroup.name}\n📱 *هاتف ولي الأمر:* ${if (parentPhone.isNotBlank()) parentPhone else "غير محدد"}",
                    actionTaken = "STUDENT_ADDED"
                )
            }
        }

        // 2. ADD NEW GROUP (إضافة مجموعة)
        if (lower.contains("اضف مجموعة") || lower.contains("أضف مجموعة") || lower.contains("ضيف مجموعة") || lower.contains("انشئ مجموعة") || lower.contains("إنشاء مجموعة")) {
            val groupNameCandidate = extractNameAfterKeyword(prompt, listOf("مجموعة", "المجموعة", "اسم"))
            val feeRegex = Regex("(\\d{2,4})\\s*(جنيه|ج|ج.م|شهريا)?")
            val feeMatch = feeRegex.find(prompt)?.groupValues?.get(1)?.toDoubleOrNull() ?: 150.0

            if (groupNameCandidate.isNotBlank()) {
                viewModel.addGroup(
                    name = groupNameCandidate,
                    subject = viewModel.teacherSubject.value,
                    day1 = "السبت",
                    day2 = "الثلاثاء",
                    day3 = null,
                    timeSlot = "04:00 م",
                    timeSlot2 = "04:00 م",
                    timeSlot3 = null,
                    fee = feeMatch,
                    groupType = "CENTER",
                    paymentType = "MONTHLY",
                    context = context
                )
                return ChatMessage(
                    sender = MessageSender.AI,
                    text = "تم إنشاء المجموعة الجديدة بنجاح 📚✨\n\n🏷️ *الاسم:* $groupNameCandidate\n📖 *المادة:* ${viewModel.teacherSubject.value}\n💵 *الاشتراك الشهري:* ${feeMatch.toInt()} ج.م\n🗓️ *المواعيد المبدئية:* السبت والثلاثاء 04:00 م (يمكنك تعديلها في شاشة المجموعات).",
                    actionTaken = "GROUP_ADDED"
                )
            }
        }

        // 3. RECORD PAYMENT (تسجيل دفع / تحصيل)
        if (lower.contains("دفع") || lower.contains("سدد") || lower.contains("تحصيل") || lower.contains("سجل دفع") || lower.contains("دفع مصاريف")) {
            val targetStudent = findBestMatchingStudent(prompt, students)
            if (targetStudent != null) {
                val group = groups.find { it.id == targetStudent.groupId }
                val fullFee = group?.monthlyFee ?: 100.0
                val amountRegex = Regex("(\\d{2,4})")
                val specifiedAmount = amountRegex.find(prompt)?.groupValues?.get(1)?.toDoubleOrNull()

                val isFullPaid = lower.contains("كامل") || lower.contains("كل") || specifiedAmount == null || specifiedAmount >= fullFee
                val paidAmount = if (isFullPaid) fullFee else specifiedAmount
                val remaining = if (isFullPaid) 0.0 else (fullFee - paidAmount).coerceAtLeast(0.0)
                val status = if (remaining <= 0.0) "PAID" else "PARTIAL"

                viewModel.setPaymentStatusWithDiscount(
                    studentId = targetStudent.id,
                    status = status,
                    discountAmount = targetStudent.discountAmount,
                    paidAmount = paidAmount,
                    remainingAmount = remaining,
                    paymentDate = today,
                    monthYear = viewModel.currentMonthYear
                )

                return ChatMessage(
                    sender = MessageSender.AI,
                    text = "تم تسجيل الدفع والتحصيل المالي بنجاح 💰✅\n\n👤 *الطالب:* ${targetStudent.fullName}\n💵 *المبلغ المدفوع:* ${paidAmount.toInt()} ج.م\n⏳ *المتبقي:* ${remaining.toInt()} ج.م\n📊 *الحالة:* ${if (status == "PAID") "خالص ومسدد بالكامل" else "سداد جزئي"}",
                    actionTaken = "PAYMENT_RECORDED"
                )
            }
        }

        // 4. RECITATION GRADE (تسميع)
        if (lower.contains("تسميع") || lower.contains("درجة تسميع") || lower.contains("سمع")) {
            val targetStudent = findBestMatchingStudent(prompt, students)
            if (targetStudent != null) {
                val gradeRegex = Regex("(\\d{1,3})")
                val gradeValue = gradeRegex.find(prompt)?.groupValues?.get(1) ?: "10"
                viewModel.setAttendanceAndHomework(
                    studentId = targetStudent.id,
                    date = today,
                    attendanceStatus = "حضر",
                    homeworkStatus = "كتب",
                    interactionStatus = "ممتاز",
                    recitationGrade = gradeValue
                )
                return ChatMessage(
                    sender = MessageSender.AI,
                    text = "تم توثيق درجة التسميع للطالب بنجاح 📖🌟\n\n👤 *الطالب:* ${targetStudent.fullName}\n🎯 *درجة التسميع:* $gradeValue / 10\n✅ تم تثبيت حضوره وكتابة الواجب.",
                    actionTaken = "RECITATION_RECORDED"
                )
            }
        }

        // 5. INTERACTION STATUS (تفاعل)
        if (lower.contains("تفاعل") || lower.contains("مشاركة")) {
            val targetStudent = findBestMatchingStudent(prompt, students)
            if (targetStudent != null) {
                val interaction = when {
                    lower.contains("ضعيف") || lower.contains("قليل") -> "ضعيف"
                    lower.contains("جيد") || lower.contains("متوسط") -> "جيد"
                    else -> "ممتاز"
                }
                viewModel.setAttendanceAndHomework(
                    studentId = targetStudent.id,
                    date = today,
                    attendanceStatus = "حضر",
                    homeworkStatus = "كتب",
                    interactionStatus = interaction
                )
                return ChatMessage(
                    sender = MessageSender.AI,
                    text = "تم تسجيل مستوى تفاعل الطالب بنجاح 🌟\n\n👤 *الطالب:* ${targetStudent.fullName}\n💡 *مستوى التفاعل:* $interaction\n✅ تم تسجيل الحضور وتحديث السجل.",
                    actionTaken = "INTERACTION_RECORDED"
                )
            }
        }

        // 6. ATTENDANCE & HOMEWORK
        val isPresence = lower.contains("سجل حضور") || lower.contains("حضور الطالب") || lower.contains("حضر ") || lower.contains("موجود") || lower.contains("عمل طالب حضور") || lower.contains("عمله حضور") || lower.contains("اعمل حضور")
        val isAbsence = lower.contains("سجل غياب") || lower.contains("غياب الطالب") || lower.contains("غاب ") || lower.contains("غايب") || lower.contains("عمل طالب غياب") || lower.contains("عمله غياب") || lower.contains("اعمل غياب")
        val isHomeworkDone = lower.contains("كتب الواجب") || lower.contains("عمل الواجب") || lower.contains("حل الواجب")
        val isHomeworkNotDone = lower.contains("لم يكتب الواجب") || lower.contains("معملش الواجب") || lower.contains("مكتبش الواجب") || lower.contains("محلش الواجب")

        if (isPresence || isAbsence || isHomeworkDone || isHomeworkNotDone) {
            val targetStudent = findBestMatchingStudent(prompt, students)
            if (targetStudent != null) {
                when {
                    isPresence -> {
                        viewModel.setAttendanceAndHomework(
                            studentId = targetStudent.id,
                            date = today,
                            attendanceStatus = "حضر",
                            homeworkStatus = "كتب"
                        )
                        return ChatMessage(
                            sender = MessageSender.AI,
                            text = "تم تسجيل حضور الطالب *${targetStudent.fullName}* بنجاح ✅\n🗓️ التاريخ: $today\n📚 تم تحديث سجل الحضور في المجموعة فوراً.",
                            actionTaken = "ATTENDANCE_RECORDED"
                        )
                    }
                    isAbsence -> {
                        viewModel.setAttendanceAndHomework(
                            studentId = targetStudent.id,
                            date = today,
                            attendanceStatus = "غاب",
                            homeworkStatus = "لم يكتب"
                        )
                        return ChatMessage(
                            sender = MessageSender.AI,
                            text = "تم تسجيل غياب الطالب *${targetStudent.fullName}* بنجاح 🚫\n🗓️ التاريخ: $today\n💡 يمكنك إرسال إشعار غياب لولي أمره من بطاقة الطالب.",
                            actionTaken = "ABSENCE_RECORDED"
                        )
                    }
                    isHomeworkDone -> {
                        viewModel.setAttendanceAndHomework(
                            studentId = targetStudent.id,
                            date = today,
                            attendanceStatus = "حضر",
                            homeworkStatus = "كتب"
                        )
                        return ChatMessage(
                            sender = MessageSender.AI,
                            text = "تم توثيق إنجاز الواجب للطالب *${targetStudent.fullName}* 📝✅",
                            actionTaken = "HOMEWORK_DONE"
                        )
                    }
                    isHomeworkNotDone -> {
                        viewModel.setAttendanceAndHomework(
                            studentId = targetStudent.id,
                            date = today,
                            attendanceStatus = "حضر",
                            homeworkStatus = "لم يكتب"
                        )
                        return ChatMessage(
                            sender = MessageSender.AI,
                            text = "تم تسجيل عدم كتابة الواجب للطالب *${targetStudent.fullName}* ⚠️",
                            actionTaken = "HOMEWORK_NOT_DONE"
                        )
                    }
                }
            } else {
                return ChatMessage(
                    sender = MessageSender.AI,
                    text = "عذراً يا أستاذي، لم أتمكن من العثور على اسم الطالب في قاعدة البيانات. يُرجى نطق أو كتابة الاسم كما هو مسجل (مثال: 'سجل حضور أحمد محمود')."
                )
            }
        }

        return null
    }

    private fun extractNameAfterKeyword(prompt: String, keywords: List<String>): String {
        val words = prompt.split(" ", "،", ",", "\n").map { it.trim() }.filter { it.isNotBlank() }
        for (i in words.indices) {
            if (keywords.any { it.equals(words[i], ignoreCase = true) } && i + 1 < words.size) {
                val candidateWords = mutableListOf<String>()
                for (j in (i + 1) until words.size) {
                    val w = words[j]
                    if (w in listOf("في", "مجموعة", "رقم", "هاتف", "ولي", "مادة", "اشتراك", "بسعر")) break
                    candidateWords.add(w)
                    if (candidateWords.size >= 4) break
                }
                if (candidateWords.isNotEmpty()) {
                    return candidateWords.joinToString(" ")
                }
            }
        }
        return ""
    }

    private fun findBestMatchingGroup(prompt: String, groups: List<GroupEntity>): GroupEntity? {
        for (g in groups) {
            if (prompt.contains(g.name, ignoreCase = true)) return g
        }
        return null
    }

    private fun findBestMatchingStudent(prompt: String, students: List<StudentEntity>): StudentEntity? {
        val words = prompt.split(" ", "،", ",", "ـ", "\n").map { it.trim() }.filter { it.length >= 3 }
        
        // Exact name match first
        for (st in students) {
            if (prompt.contains(st.fullName, ignoreCase = true)) return st
        }

        // Substring / first name + second name match
        for (st in students) {
            val nameParts = st.fullName.split(" ")
            val firstName = nameParts.firstOrNull() ?: ""
            val secondName = nameParts.getOrNull(1) ?: ""
            if (firstName.isNotBlank() && words.any { it.equals(firstName, ignoreCase = true) }) {
                if (secondName.isNotBlank() && words.any { it.equals(secondName, ignoreCase = true) }) {
                    return st
                }
            }
        }

        // Single name match fallback
        for (st in students) {
            val firstName = st.fullName.split(" ").firstOrNull() ?: ""
            if (firstName.length >= 3 && words.any { it.equals(firstName, ignoreCase = true) }) {
                return st
            }
        }

        return null
    }

    private fun handleDataQueries(
        prompt: String,
        students: List<StudentEntity>,
        groups: List<GroupEntity>,
        attendanceToday: List<AttendanceRecordEntity>,
        payments: List<PaymentRecordEntity>,
        teacherName: String
    ): ChatMessage? {
        val lower = prompt.lowercase()

        // 1. Finance Queries (إيرادات، فلوس، محصل، متبقي)
        if (lower.contains("ارادات") || lower.contains("إيرادات") || lower.contains("ارباح") || lower.contains("المحصل") || lower.contains("فلوس") || lower.contains("مالية") || lower.contains("دخل") || lower.contains("الحسابات")) {
            val totalExpected = groups.sumOf { grp ->
                val grpStudents = students.filter { it.groupId == grp.id }
                grpStudents.size * grp.monthlyFee
            }
            val totalCollected = payments.filter { it.paymentStatus == "PAID" || it.paymentStatus == "PARTIAL" }
                .sumOf { pay ->
                    val student = students.find { it.id == pay.studentId }
                    val grp = groups.find { it.id == student?.groupId }
                    val fee = grp?.monthlyFee ?: 0.0
                    if (pay.paymentStatus == "PAID") fee else (fee - pay.remainingAmount).coerceAtLeast(0.0)
                }
            val remaining = (totalExpected - totalCollected).coerceAtLeast(0.0)
            val unpaidStudentsCount = students.count { st ->
                val pay = payments.find { it.studentId == st.id }
                pay == null || pay.paymentStatus == "UNPAID" || pay.paymentStatus == "PARTIAL"
            }

            return ChatMessage(
                sender = MessageSender.AI,
                text = buildString {
                    append("💰 *التقرير المالي المباشر للشهر الحالي:*\n\n")
                    append("💵 *إجمالي المحصل الفعلي:* ${totalCollected.toInt()} ج.م\n")
                    append("📊 *إجمالي المتوقع:* ${totalExpected.toInt()} ج.م\n")
                    append("⏳ *المتبقي لدى الطلاب:* ${remaining.toInt()} ج.م\n")
                    append("👥 *عدد الطلاب الذين لم يكتمل سدادهم:* $unpaidStudentsCount طالب\n\n")
                    append("💡 يمكنك قول: 'سجل دفع [اسم الطالب]' لتسديد أي مبلغ صوتياً فوراً!")
                }
            )
        }

        // 2. Who is absent today?
        if (lower.contains("مين غاب") || lower.contains("من غاب") || lower.contains("الغياب اليوم") || lower.contains("عدد الغياب") || lower.contains("الطلاب الغائبين")) {
            val absentList = attendanceToday.filter { it.attendanceStatus == "غاب" }
            if (absentList.isEmpty()) {
                return ChatMessage(
                    sender = MessageSender.AI,
                    text = "🎉 ممتاز يا أستاذي! لا يوجد أي طالب مسجل كـ *غائب* اليوم حتى الآن، أو لم يتم أخذ الحضور بعد."
                )
            } else {
                val absentStudents = absentList.mapNotNull { att ->
                    students.find { it.id == att.studentId }
                }
                return ChatMessage(
                    sender = MessageSender.AI,
                    text = buildString {
                        append("🚫 *قائمة الطلاب الغائبين اليوم (${absentStudents.size} طالب):*\n\n")
                        absentStudents.forEachIndexed { idx, st ->
                            val grp = groups.find { it.id == st.groupId }
                            val phone = st.parentPhone.ifBlank { st.studentPhone }
                            append("${idx + 1}. *${st.fullName}* - ${grp?.name ?: "بدون مجموعة"}")
                            if (phone.isNotBlank()) append(" (📱 $phone)")
                            append("\n")
                        }
                        append("\n💡 يمكنك إرسال تنبيه واتساب مباشر لأولياء أمورهم من بطاقة كل طالب.")
                    }
                )
            }
        }

        // 3. Who is present today?
        if (lower.contains("مين حضر") || lower.contains("من حضر") || lower.contains("الحضور اليوم") || lower.contains("الحاضرين")) {
            val presentList = attendanceToday.filter { it.attendanceStatus == "حضر" }
            if (presentList.isEmpty()) {
                return ChatMessage(
                    sender = MessageSender.AI,
                    text = "📝 لم يتم رصد حضور أي طالب لليوم حتى الآن. يمكنك قول: 'سجل حضور [اسم الطالب]' لتسجيله صوتياً فوراً."
                )
            } else {
                val presentStudents = presentList.mapNotNull { att ->
                    students.find { it.id == att.studentId }
                }
                return ChatMessage(
                    sender = MessageSender.AI,
                    text = buildString {
                        append("✅ *قائمة الطلاب الحاضرين اليوم (${presentStudents.size} طالب):*\n\n")
                        presentStudents.forEachIndexed { idx, st ->
                            val grp = groups.find { it.id == st.groupId }
                            append("${idx + 1}. *${st.fullName}* - ${grp?.name ?: ""}\n")
                        }
                    }
                )
            }
        }

        // 4. Today's Classes / Schedule
        if (lower.contains("حصص اليوم") || lower.contains("مواعيد اليوم") || lower.contains("جدول اليوم") || lower.contains("عندي إيه النهاردة")) {
            val cal = java.util.Calendar.getInstance()
            val dayOfWeekInt = cal.get(java.util.Calendar.DAY_OF_WEEK)
            val dayName = when (dayOfWeekInt) {
                java.util.Calendar.SATURDAY -> "السبت"
                java.util.Calendar.SUNDAY -> "الأحد"
                java.util.Calendar.MONDAY -> "الاثنين"
                java.util.Calendar.TUESDAY -> "الثلاثاء"
                java.util.Calendar.WEDNESDAY -> "الأربعاء"
                java.util.Calendar.THURSDAY -> "الخميس"
                java.util.Calendar.FRIDAY -> "الجمعة"
                else -> ""
            }

            val todayGroups = groups.filter { it.meetsOnDay(dayName) }
            if (todayGroups.isEmpty()) {
                return ChatMessage(
                    sender = MessageSender.AI,
                    text = "🗓️ *جدول اليوم ($dayName):*\n\nلا توجد مجموعات مجدولة ليوم $dayName حسب جدول مواعيدك. يوم سعيد يا أستاذي! ☕"
                )
            } else {
                return ChatMessage(
                    sender = MessageSender.AI,
                    text = buildString {
                        append("📚 *جدول حصص ومجموعات اليوم ($dayName) - (${todayGroups.size} مجموعات):*\n\n")
                        todayGroups.forEachIndexed { idx, grp ->
                            val count = students.count { it.groupId == grp.id }
                            val slot = grp.getTimeSlotForDay(dayName)
                            append("${idx + 1}. 🎓 *${grp.name}*\n")
                            append("   ▫️ المادة: ${grp.subject}\n")
                            append("   ▫️ الموعد: $slot\n")
                            append("   ▫️ عدد الطلاب: $count طالب\n")
                            append("   ▫️ المكان: ${if (grp.groupType == "ONLINE") "أونلاين 🌐" else if (grp.groupType == "PRIVATE") "برايفت 🏠" else "سنتر 🏢"}\n\n")
                        }
                    }
                )
            }
        }

        // 5. Total counts / stats
        if (lower.contains("كم طالب") || lower.contains("عدد الطلاب") || lower.contains("كم مجموعة") || lower.contains("عدد المجموعات") || lower.contains("إحصائيات")) {
            return ChatMessage(
                sender = MessageSender.AI,
                text = buildString {
                    append("📊 *إحصائيات تطبيق أستاذي +:*\n\n")
                    append("👨‍🏫 *المعلم:* ${teacherName.ifBlank { "أستاذي" }}\n")
                    append("👥 *إجمالي الطلاب المسجلين:* ${students.size} طالب\n")
                    append("📚 *إجمالي المجموعات:* ${groups.size} مجموعة\n")
                    append("✅ *حضور اليوم:* ${attendanceToday.count { it.attendanceStatus == "حضر" }} طالب\n")
                    append("🚫 *غياب اليوم:* ${attendanceToday.count { it.attendanceStatus == "غاب" }} طالب\n")
                }
            )
        }

        return null
    }

    private suspend fun callGeminiApi(
        prompt: String,
        base64Image: String?,
        apiKey: String,
        students: List<StudentEntity>,
        groups: List<GroupEntity>,
        attendanceToday: List<AttendanceRecordEntity>,
        payments: List<PaymentRecordEntity>,
        teacherName: String
    ): String {
        val systemPrompt = """
            أنت المساعد الذكي الصوتي والكتابي المدمج في تطبيق المعلم "أستاذي +".
            تخاطب المعلم باحترام وود واحترافية تربوية (يا أستاذي / يا أستاذ ${teacherName.ifBlank { "الفاضل" }}).
            معلومات المعلم والمدرسة الحالية:
            - اسم المعلم: ${teacherName.ifBlank { "المعلم" }}
            - عدد الطلاب: ${students.size}
            - عدد المجموعات: ${groups.size}
            
            مهمتك:
            1. الإجابة بدقة وذكاء وفصاحة عربية واضحة.
            2. إذا طلب منك تحويل صورة لأسئلة امتحان لجوجل فورم، قم باستخراج النص وتنسيقه كنص جاهز للنسخ في نماذج جوجل.
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", systemPrompt) })
                })
            })
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt.ifBlank { "اشرح هذه الصورة" }) })
                        if (base64Image != null) {
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", "image/jpeg")
                                    put("data", base64Image)
                                })
                            })
                        }
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("topP", 0.95)
                put("topK", 40)
            })
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonBody.toString().toRequestBody(mediaType)
        val url = "$GEMINI_ENDPOINT?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val response = okHttpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            Log.e(TAG, "Gemini API failed code: ${response.code}, body: $responseBody")
            return ""
        }

        val jsonResponse = JSONObject(responseBody)
        val candidates = jsonResponse.optJSONArray("candidates")
        val firstCandidate = candidates?.optJSONObject(0)
        val content = firstCandidate?.optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        val text = parts?.optJSONObject(0)?.optString("text") ?: ""

        return text.trim()
    }

    private fun generateSmartOfflineResponse(
        prompt: String,
        students: List<StudentEntity>,
        groups: List<GroupEntity>,
        attendanceToday: List<AttendanceRecordEntity>,
        payments: List<PaymentRecordEntity>,
        teacherName: String
    ): String {
        return buildString {
            append("أهلاً بك يا أستاذي العزيز 🌺\n\n")
            append("أنا جاهز ومستعد لتنفيذ أي أمر صوتي أو كتابي داخل التطبيق فوراً:\n\n")
            append("🎙️ *أوامر الحضور والغياب:* قل 'سجل حضور [اسم الطالب]' أو 'سجل غياب [اسم الطالب]'\n")
            append("📝 *أوامر الواجب والتسميع:* قل 'سجل كتب الواجب [اسم الطالب]' أو 'سجل تسميع [اسم الطالب] 10 من 10'\n")
            append("💰 *المالية والتحصيل:* قل 'سجل دفع [اسم الطالب] 200 جنيه' أو اسأل 'كم الإيرادات؟'\n")
            append("🎓 *إضافة طلاب ومجموعات:* قل 'أضف طالب [الاسم] في مجموعة [المجموعة]'\n")
            append("📅 *الحصص والمواعيد:* اسأل 'حصص اليوم' أو 'مين غاب اليوم؟'")
        }
    }
}
