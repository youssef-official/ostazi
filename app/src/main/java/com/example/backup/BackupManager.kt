package com.example.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.*
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

object BackupManager {

    private const val FIRESTORE_COLLECTION = "teacher_backups"
    private const val FIRESTORE_DOC_ID = "default_teacher_data"

    // Convert BackupData to JSON String
    fun toJson(data: BackupData): String {
        val root = JSONObject()
        root.put("exportDate", data.exportDate)
        root.put("version", data.version)

        // Groups
        val groupsArray = JSONArray()
        data.groups.forEach { g ->
            val obj = JSONObject()
            obj.put("id", g.id)
            obj.put("name", g.name)
            obj.put("subject", g.subject)
            obj.put("day1", g.day1)
            obj.put("day2", g.day2 ?: "")
            obj.put("day3", g.day3 ?: "")
            obj.put("day4", g.day4 ?: "")
            obj.put("day5", g.day5 ?: "")
            obj.put("day6", g.day6 ?: "")
            obj.put("day7", g.day7 ?: "")
            obj.put("timeSlot", g.timeSlot)
            obj.put("timeSlot2", g.timeSlot2 ?: "")
            obj.put("timeSlot3", g.timeSlot3 ?: "")
            obj.put("timeSlot4", g.timeSlot4 ?: "")
            obj.put("timeSlot5", g.timeSlot5 ?: "")
            obj.put("timeSlot6", g.timeSlot6 ?: "")
            obj.put("timeSlot7", g.timeSlot7 ?: "")
            obj.put("monthlyFee", g.monthlyFee)
            obj.put("groupType", g.groupType)
            obj.put("paymentType", g.paymentType)
            obj.put("whatsappGroupUrl", g.whatsappGroupUrl)
            obj.put("sortOrder", g.sortOrder)
            groupsArray.put(obj)
        }
        root.put("groups", groupsArray)

        // Students
        val studentsArray = JSONArray()
        data.studentWrappers.forEach { sw ->
            val s = sw.student
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("fullName", s.fullName)
            obj.put("groupId", s.groupId)
            if (sw.rawGroupName.isNotBlank()) obj.put("rawGroupName", sw.rawGroupName)
            obj.put("parentPhone", s.parentPhone)
            obj.put("studentPhone", s.studentPhone)
            studentsArray.put(obj)
        }
        root.put("students", studentsArray)

        // Attendance
        val attArray = JSONArray()
        data.attendanceWrappers.forEach { aw ->
            val a = aw.attendance
            val obj = JSONObject()
            obj.put("id", a.id)
            obj.put("studentId", a.studentId)
            if (aw.rawStudentName.isNotBlank()) obj.put("rawStudentName", aw.rawStudentName)
            obj.put("date", a.date)
            obj.put("attendanceStatus", a.attendanceStatus)
            obj.put("homeworkStatus", a.homeworkStatus)
            obj.put("interactionStatus", a.interactionStatus)
            obj.put("recitationGrade", a.recitationGrade)
            obj.put("notes", a.notes)
            attArray.put(obj)
        }
        root.put("attendanceRecords", attArray)

        // Exams
        val examArray = JSONArray()
        data.examWrappers.forEach { ew ->
            val e = ew.exam
            val obj = JSONObject()
            obj.put("id", e.id)
            obj.put("studentId", e.studentId)
            if (ew.rawStudentName.isNotBlank()) obj.put("rawStudentName", ew.rawStudentName)
            obj.put("exam1", e.exam1 ?: JSONObject.NULL)
            obj.put("exam2", e.exam2 ?: JSONObject.NULL)
            obj.put("exam3", e.exam3 ?: JSONObject.NULL)
            obj.put("exam4", e.exam4 ?: JSONObject.NULL)
            obj.put("exam5", e.exam5 ?: JSONObject.NULL)
            examArray.put(obj)
        }
        root.put("examRecords", examArray)

        // Payments
        val payArray = JSONArray()
        data.paymentWrappers.forEach { pw ->
            val p = pw.payment
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("studentId", p.studentId)
            if (pw.rawStudentName.isNotBlank()) obj.put("rawStudentName", pw.rawStudentName)
            obj.put("monthYear", p.monthYear)
            obj.put("paymentStatus", p.paymentStatus)
            obj.put("remainingAmount", p.remainingAmount)
            obj.put("paymentDate", p.paymentDate)
            payArray.put(obj)
        }
        root.put("paymentRecords", payArray)

        // Individual Exams
        val indArray = JSONArray()
        data.individualExamWrappers.forEach { iw ->
            val ie = iw.exam
            val obj = JSONObject()
            obj.put("id", ie.id)
            obj.put("studentId", ie.studentId)
            if (iw.rawStudentName.isNotBlank()) obj.put("rawStudentName", iw.rawStudentName)
            obj.put("examName", ie.examName)
            obj.put("score", ie.score)
            obj.put("maxScore", ie.maxScore)
            obj.put("date", ie.date)
            obj.put("notes", ie.notes)
            indArray.put(obj)
        }
        root.put("student_individual_exams", indArray)

        return root.toString(2)
    }

    // Date & String Normalization Helpers
    fun normalizeMonthYear(raw: String): String {
        if (raw.isBlank()) return SimpleDateFormat("yyyy-MM", Locale.ENGLISH).format(Date())
        val clean = raw.trim()
        val yyyyMm = Regex("^(\\d{4})[-/](\\d{1,2})").find(clean)
        if (yyyyMm != null) {
            val (y, m) = yyyyMm.destructured
            return String.format(Locale.ENGLISH, "%04d-%02d", y.toInt(), m.toInt())
        }
        val mmYyyy = Regex("^(\\d{1,2})[-/](\\d{4})").find(clean)
        if (mmYyyy != null) {
            val (m, y) = mmYyyy.destructured
            return String.format(Locale.ENGLISH, "%04d-%02d", y.toInt(), m.toInt())
        }
        return clean
    }

    fun normalizeDate(raw: String): String {
        if (raw.isBlank()) return SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())
        val clean = raw.trim()
        val ymd = Regex("^(\\d{4})[-/](\\d{1,2})[-/](\\d{1,2})").find(clean)
        if (ymd != null) {
            val (y, m, d) = ymd.destructured
            return String.format(Locale.ENGLISH, "%04d-%02d-%02d", y.toInt(), m.toInt(), d.toInt())
        }
        val dmy = Regex("^(\\d{1,2})[-/](\\d{1,2})[-/](\\d{4})").find(clean)
        if (dmy != null) {
            val (d, m, y) = dmy.destructured
            return String.format(Locale.ENGLISH, "%04d-%02d-%02d", y.toInt(), m.toInt(), d.toInt())
        }
        return clean
    }

    fun normalizePaymentStatus(raw: String, isPaidBool: Boolean? = null): String {
        if (isPaidBool == true) return "PAID"
        val clean = raw.trim().uppercase(Locale.ENGLISH)
        return when {
            clean == "PAID" || clean == "TRUE" || clean == "1" ||
            clean.contains("تم") || clean.contains("مدفوع") || clean.contains("دفع") || clean.contains("خالص") -> "PAID"
            clean == "PARTIAL" || clean.contains("جزئ") || clean.contains("متبق") -> "PARTIAL"
            else -> "UNPAID"
        }
    }

    fun normalizeAttendanceStatus(raw: String): String {
        val clean = raw.trim().uppercase(Locale.ENGLISH)
        return when {
            clean == "حضر" || clean == "PRESENT" || clean == "TRUE" || clean == "1" || clean.contains("حاضر") -> "حضر"
            clean == "غائب" || clean == "ABSENT" || clean == "FALSE" || clean == "0" || clean.contains("غياب") -> "غائب"
            clean == "متأخر" || clean == "LATE" || clean.contains("تاخير") || clean.contains("تأخير") -> "متأخر"
            clean.contains("عذر") || clean == "EXCUSED" -> "بعذر"
            else -> "حضر"
        }
    }

    fun normalizeHomeworkStatus(raw: String): String {
        val clean = raw.trim().uppercase(Locale.ENGLISH)
        return when {
            clean == "كتب" || clean == "DONE" || clean == "TRUE" || clean == "1" || clean.contains("تم") || clean.contains("حل") || clean.contains("واجب") -> "كتب"
            clean == "لم يكتب" || clean == "NOT_DONE" || clean == "FALSE" || clean == "0" || clean.contains("لم") -> "لم يكتب"
            clean == "متأخر" || clean.contains("تاخير") -> "متأخر"
            clean == "لا يوجد" || clean.contains("لا") -> "لا يوجد"
            else -> "كتب"
        }
    }

    // Parse JSON String to BackupData (Universal Smart JSON Parser)
    fun fromJson(jsonStr: String): BackupData {
        val trimmed = jsonStr.trim()
        val groups = mutableListOf<GroupEntity>()
        val studentWrappers = mutableListOf<BackupStudentWrapper>()
        val attendanceWrappers = mutableListOf<BackupAttendanceWrapper>()
        val examWrappers = mutableListOf<BackupExamWrapper>()
        val paymentWrappers = mutableListOf<BackupPaymentWrapper>()
        val individualExamWrappers = mutableListOf<BackupIndividualExamWrapper>()

        var exportDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).format(Date())
        var version = 1

        try {
            if (trimmed.startsWith("[")) {
                // Root is JSON Array
                val rootArray = JSONArray(trimmed)
                for (i in 0 until rootArray.length()) {
                    val obj = rootArray.optJSONObject(i) ?: continue
                    val sName = optFirstString(obj, "fullName", "name", "studentName", "student_name", "full_name", "title", "label", "اسم_الطالب", "اسم الطالب", "الطالب", "الاسم")
                    if (sName.isNotBlank()) {
                        val pPhone = optFirstString(obj, "parentPhone", "parent_phone", "phone", "mobile", "parentMobile", "parent_mobile", "phone1", "هاتف_الولي", "رقم_الولي", "رقم ولي الأمر")
                        val sPhone = optFirstString(obj, "studentPhone", "student_phone", "phone2", "mobile2", "رقم_الطالب")
                        val gId = optFirstInt(obj, "groupId", "group_id", "class_id", "classId", fallback = 0)
                        val rawGName = optFirstString(obj, "groupName", "group_name", "group", "class", "className", "class_name", "المجموعة", "اسم_المجموعة", "اسم المجموعة", "مجموعة", "rawGroupName")
                        val sId = optFirstInt(obj, "id", "studentId", "student_id", fallback = i + 1)
                        studentWrappers.add(
                            BackupStudentWrapper(
                                student = StudentEntity(id = sId, fullName = sName, groupId = gId, parentPhone = pPhone, studentPhone = sPhone),
                                rawGroupName = rawGName
                            )
                        )
                    } else {
                        val gName = optFirstString(obj, "name", "groupName", "group_name", "title", "label", "className", "اسم_المجموعة", "اسم المجموعة", "المجموعة")
                        if (gName.isNotBlank()) {
                            val gSub = optFirstString(obj, "subject", "subjectName", "subject_name", "course", "مادة", "المادة", fallback = "مادة عامة")
                            val fee = optFirstDouble(obj, "monthlyFee", "monthly_fee", "fee", "price", "amount", "cost", "الاشتراك", "الرسوم", fallback = 150.0)
                            val gTypeStr = optFirstString(obj, "groupType", "group_type", "type", "نوع_المجموعة", fallback = "CENTER")
                            val gType = when {
                                gTypeStr.contains("أونلاين") || gTypeStr.contains("ONLINE") -> "ONLINE"
                                gTypeStr.contains("خاص") || gTypeStr.contains("PRIVATE") -> "PRIVATE"
                                else -> "CENTER"
                            }
                            val gId = optFirstInt(obj, "id", "groupId", "group_id", fallback = i + 1)
                            val d1 = optFirstString(obj, "day1", "day", fallback = "السبت")
                            val tSlot = optFirstString(obj, "timeSlot", "time", "time_slot", fallback = "04:00 م")
                            groups.add(
                                GroupEntity(
                                    id = gId,
                                    name = gName,
                                    subject = gSub,
                                    day1 = d1,
                                    timeSlot = tSlot,
                                    monthlyFee = fee,
                                    groupType = gType,
                                    sortOrder = i + 1
                                )
                            )
                        }
                    }
                }
            } else {
                // Root is JSONObject
                var root = JSONObject(trimmed)
                val nestedObjKeys = listOf("data", "backup", "payload", "content", "teacher_data", "database", "export", "app_data")
                for (k in nestedObjKeys) {
                    val sub = root.optJSONObject(k)
                    if (sub != null) {
                        root = sub
                        break
                    }
                }

                exportDate = root.optString("exportDate", root.optString("date", exportDate))
                version = root.optInt("version", 1)

                // 1. Find Groups Array
                val groupsArr = findJsonArray(
                    root,
                    "groups", "groupList", "groupsList", "classes", "classList", "items", "categories", "subjects", "sections", "المجموعات", "مجموعات", "الصفوف", "صفوف", "الفصول", "فصول"
                )
                if (groupsArr != null) {
                    for (i in 0 until groupsArr.length()) {
                        val obj = groupsArr.optJSONObject(i) ?: continue
                        val gName = optFirstString(obj, "name", "groupName", "group_name", "title", "label", "className", "class_name", "اسم_المجموعة", "اسم المجموعة", "المجموعة")
                        if (gName.isNotBlank()) {
                            val gSub = optFirstString(obj, "subject", "subjectName", "subject_name", "course", "مادة", "المادة", fallback = "مادة عامة")
                            val fee = optFirstDouble(obj, "monthlyFee", "monthly_fee", "fee", "price", "amount", "cost", "الاشتراك", "الرسوم", "السعر", fallback = 150.0)
                            val gTypeStr = optFirstString(obj, "groupType", "group_type", "type", "نوع_المجموعة", fallback = "")
                            val combinedTypeStr = (gTypeStr + " " + gName).uppercase()
                            val gType = when {
                                combinedTypeStr.contains("أونلاين") || combinedTypeStr.contains("أون لاين") || combinedTypeStr.contains("ONLINE") -> "ONLINE"
                                combinedTypeStr.contains("خاص") || combinedTypeStr.contains("برايفت") || combinedTypeStr.contains("PRIVATE") -> "PRIVATE"
                                else -> "CENTER"
                            }
                            val pTypeStr = optFirstString(obj, "paymentType", "payment_type", "نوع_الدفع", fallback = "MONTHLY")
                            val pType = if (pTypeStr.contains("حصة") || pTypeStr.contains("SESSION")) "PER_SESSION" else "MONTHLY"
                            val gId = optFirstInt(obj, "id", "groupId", "group_id", fallback = i + 1)
                            
                            var d1 = optFirstString(obj, "day1", "day", fallback = "السبت")
                            var d2 = if (obj.isNull("day2")) null else obj.optString("day2")
                            var d3 = if (obj.isNull("day3")) null else obj.optString("day3")
                            var d4 = if (obj.isNull("day4")) null else obj.optString("day4")
                            var d5 = if (obj.isNull("day5")) null else obj.optString("day5")
                            var d6 = if (obj.isNull("day6")) null else obj.optString("day6")
                            var d7 = if (obj.isNull("day7")) null else obj.optString("day7")

                            var tSlot = optFirstString(obj, "timeSlot", "time", "time_slot", "timeSlot1", fallback = "04:00 م")
                            var tSlot2 = if (obj.isNull("timeSlot2")) null else obj.optString("timeSlot2")
                            var tSlot3 = if (obj.isNull("timeSlot3")) null else obj.optString("timeSlot3")
                            var tSlot4 = if (obj.isNull("timeSlot4")) null else obj.optString("timeSlot4")
                            var tSlot5 = if (obj.isNull("timeSlot5")) null else obj.optString("timeSlot5")
                            var tSlot6 = if (obj.isNull("timeSlot6")) null else obj.optString("timeSlot6")
                            var tSlot7 = if (obj.isNull("timeSlot7")) null else obj.optString("timeSlot7")

                            val daysArr = findJsonArray(obj, "days", "daysList", "schedule", "الأيام", "المواعيد")
                            if (daysArr != null && daysArr.length() > 0) {
                                for (j in 0 until daysArr.length()) {
                                    val item = daysArr.opt(j)
                                    var dayStr = ""
                                    var timeStr = ""
                                    
                                    if (item is JSONObject) {
                                        dayStr = optFirstString(item, "day", "name", "dayName", fallback = "")
                                        timeStr = optFirstString(item, "time", "timeSlot", fallback = "")
                                    } else {
                                        dayStr = item?.toString() ?: ""
                                    }
                                    
                                    if (dayStr.isNotBlank()) {
                                        when (j) {
                                            0 -> { d1 = dayStr; if (timeStr.isNotBlank()) tSlot = timeStr }
                                            1 -> { d2 = dayStr; if (timeStr.isNotBlank()) tSlot2 = timeStr }
                                            2 -> { d3 = dayStr; if (timeStr.isNotBlank()) tSlot3 = timeStr }
                                            3 -> { d4 = dayStr; if (timeStr.isNotBlank()) tSlot4 = timeStr }
                                            4 -> { d5 = dayStr; if (timeStr.isNotBlank()) tSlot5 = timeStr }
                                            5 -> { d6 = dayStr; if (timeStr.isNotBlank()) tSlot6 = timeStr }
                                            6 -> { d7 = dayStr; if (timeStr.isNotBlank()) tSlot7 = timeStr }
                                        }
                                    }
                                }
                            }

                            groups.add(
                                GroupEntity(
                                    id = gId,
                                    name = gName,
                                    subject = gSub,
                                    day1 = d1,
                                    day2 = d2,
                                    day3 = d3,
                                    day4 = d4,
                                    day5 = d5,
                                    day6 = d6,
                                    day7 = d7,
                                    timeSlot = tSlot,
                                    timeSlot2 = tSlot2,
                                    timeSlot3 = tSlot3,
                                    timeSlot4 = tSlot4,
                                    timeSlot5 = tSlot5,
                                    timeSlot6 = tSlot6,
                                    timeSlot7 = tSlot7,
                                    monthlyFee = fee,
                                    groupType = gType,
                                    paymentType = pType,
                                    whatsappGroupUrl = obj.optString("whatsappGroupUrl", obj.optString("whatsapp", "")),
                                    sortOrder = obj.optInt("sortOrder", i + 1)
                                )
                            )
                        }
                    }
                }

                // 2. Find Students Array
                val studentsArr = findJsonArray(
                    root,
                    "students", "studentList", "studentsList", "pupils", "data", "items", "users", "contacts", "records", "list", "الطلاب", "طلاب", "تلاميذ", "التلاميذ", "أسماء_الطلاب"
                )
                if (studentsArr != null) {
                    for (i in 0 until studentsArr.length()) {
                        val obj = studentsArr.optJSONObject(i) ?: continue
                        val sName = optFirstString(obj, "fullName", "name", "studentName", "student_name", "student_fullname", "full_name", "title", "label", "اسم_الطالب", "اسم الطالب", "الطالب", "الاسم")
                        if (sName.isNotBlank()) {
                            val pPhone = optFirstString(obj, "parentPhone", "parent_phone", "phone", "mobile", "parentMobile", "parent_mobile", "studentPhone", "student_phone", "phone1", "هاتف_الولي", "رقم_الولي", "هاتف ولي الأمر", "رقم ولي الأمر")
                            val sPhone = optFirstString(obj, "studentPhone", "student_phone", "phone2", "mobile2", "رقم_الطالب", "هاتف_الطالب")
                            val gId = optFirstInt(obj, "groupId", "group_id", "class_id", "classId", fallback = 0)
                            val rawGName = optFirstString(obj, "groupName", "group_name", "group", "class", "className", "class_name", "المجموعة", "اسم_المجموعة", "اسم المجموعة", "مجموعة", "rawGroupName")
                            val sId = optFirstInt(obj, "id", "studentId", "student_id", fallback = i + 1)
                            studentWrappers.add(
                                BackupStudentWrapper(
                                    student = StudentEntity(id = sId, fullName = sName, groupId = gId, parentPhone = pPhone, studentPhone = sPhone),
                                    rawGroupName = rawGName
                                )
                            )
                        }
                    }
                }

                if (studentWrappers.isEmpty()) {
                    scanDeepForStudentWrappers(root, studentWrappers)
                }

                // 3. Find Attendance Records Array
                val attArr = findJsonArray(
                    root,
                    "attendanceRecords", "attendance", "attendanceList", "records", "الحضور", "سجل_الحضور", "سجل الحضور", "حضور"
                )
                if (attArr != null) {
                    for (i in 0 until attArr.length()) {
                        val obj = attArr.optJSONObject(i) ?: continue
                        val aId = optFirstInt(obj, "id", fallback = i + 1)
                        val stId = optFirstInt(obj, "studentId", "student_id", "student", fallback = 0)
                        val rawStName = optFirstString(obj, "rawStudentName", "studentName", "student_name", "fullName", "name", "اسم_الطالب", "الطالب")
                        val rawDt = optFirstString(obj, "date", "created_at", "attendanceDate", "تاريخ", fallback = "")
                        val normDt = normalizeDate(rawDt)
                        
                        val rawAttSt = optFirstString(obj, "attendanceStatus", "status", "presence", "حالة_الحضور", "حضور", fallback = "حضر")
                        val normAttSt = normalizeAttendanceStatus(rawAttSt)

                        val rawHwSt = optFirstString(obj, "homeworkStatus", "homework", "الواجب", "حالة_الواجب", fallback = "كتب")
                        val normHwSt = normalizeHomeworkStatus(rawHwSt)

                        val recSt = optFirstString(obj, "interactionStatus", "recitationStatus", "recitation", "interaction", "المشاركة", fallback = "ممتاز")
                        val recGrade = optFirstString(obj, "recitationGrade", "grade", "score", "درجة_التسميع", fallback = "")
                        val notesStr = optFirstString(obj, "notes", "note", "comment", "ملاحظات", fallback = "")

                        attendanceWrappers.add(
                            BackupAttendanceWrapper(
                                attendance = AttendanceRecordEntity(
                                    id = aId,
                                    studentId = stId,
                                    date = normDt,
                                    attendanceStatus = normAttSt,
                                    homeworkStatus = normHwSt,
                                    interactionStatus = recSt,
                                    recitationGrade = recGrade,
                                    recitationStatus = recSt,
                                    notes = notesStr
                                ),
                                rawStudentName = rawStName
                            )
                        )
                    }
                }

                // 4. Find Exam Records Array
                val examArr = findJsonArray(
                    root,
                    "examRecords", "exams", "examsList", "grades", "scores", "الامتحانات", "الاختبارات", "درجات", "الدرجات"
                )
                if (examArr != null) {
                    for (i in 0 until examArr.length()) {
                        val obj = examArr.optJSONObject(i) ?: continue
                        val eId = optFirstInt(obj, "id", fallback = i + 1)
                        val stId = optFirstInt(obj, "studentId", "student_id", "student", fallback = 0)
                        val rawStName = optFirstString(obj, "rawStudentName", "studentName", "student_name", "fullName", "name", "اسم_الطالب", "الطالب")
                        val e1 = if (obj.isNull("exam1")) null else obj.optString("exam1")
                        val e2 = if (obj.isNull("exam2")) null else obj.optString("exam2")
                        val e3 = if (obj.isNull("exam3")) null else obj.optString("exam3")
                        val e4 = if (obj.isNull("exam4")) null else obj.optString("exam4")
                        val e5 = if (obj.isNull("exam5")) null else obj.optString("exam5")

                        examWrappers.add(
                            BackupExamWrapper(
                                exam = ExamRecordEntity(id = eId, studentId = stId, exam1 = e1, exam2 = e2, exam3 = e3, exam4 = e4, exam5 = e5),
                                rawStudentName = rawStName
                            )
                        )
                    }
                }

                // 5. Find Payment Records Array (FINANCE / WHO PAID & WHO DIDN'T)
                val payArr = findJsonArray(
                    root,
                    "paymentRecords", "payments", "paymentsList", "fees", "finances", "المالية", "المصروفات", "المدفوعات", "سجل_المدفوعات", "دفع", "الدفع"
                )
                if (payArr != null) {
                    for (i in 0 until payArr.length()) {
                        val obj = payArr.optJSONObject(i) ?: continue
                        val pId = optFirstInt(obj, "id", fallback = i + 1)
                        val stId = optFirstInt(obj, "studentId", "student_id", "student", fallback = 0)
                        val rawStName = optFirstString(obj, "rawStudentName", "studentName", "student_name", "fullName", "name", "اسم_الطالب", "الطالب")
                        
                        val rawMYr = optFirstString(obj, "monthYear", "month", "date", "period", "الشهر", "تاريخ", fallback = "")
                        val normMYr = normalizeMonthYear(rawMYr)

                        val rawPSt = optFirstString(obj, "paymentStatus", "status", "paid", "isPaid", "حالة_الدفع", "تم_الدفع", fallback = "UNPAID")
                        val isPaidBool = if (obj.has("isPaid")) obj.optBoolean("isPaid") else if (obj.has("paid") && obj.get("paid") is Boolean) obj.optBoolean("paid") else null
                        val normPSt = normalizePaymentStatus(rawPSt, isPaidBool)

                        val rem = optFirstDouble(obj, "remainingAmount", "remaining", "rest", "المتبقي", "متبقي", fallback = 0.0)
                        val rawPDt = optFirstString(obj, "paymentDate", "date", "created_at", "تاريخ_الدفع", fallback = "")
                        val normPDt = if (rawPDt.isBlank()) "" else normalizeDate(rawPDt)

                        paymentWrappers.add(
                            BackupPaymentWrapper(
                                payment = PaymentRecordEntity(
                                    id = pId,
                                    studentId = stId,
                                    monthYear = normMYr,
                                    paymentStatus = normPSt,
                                    remainingAmount = rem,
                                    paymentDate = normPDt
                                ),
                                rawStudentName = rawStName
                            )
                        )
                    }
                }

                // 6. Find Individual Exam Records Array
                val indExamArr = findJsonArray(
                    root,
                    "student_individual_exams", "individualExams", "individual_exams", "examsHistory", "امتحانات_فرودية"
                )
                if (indExamArr != null) {
                    for (i in 0 until indExamArr.length()) {
                        val obj = indExamArr.optJSONObject(i) ?: continue
                        val ieId = optFirstInt(obj, "id", fallback = i + 1)
                        val stId = optFirstInt(obj, "studentId", "student_id", "student", fallback = 0)
                        val rawStName = optFirstString(obj, "rawStudentName", "studentName", "student_name", "fullName", "name", "اسم_الطالب")
                        val exName = optFirstString(obj, "examName", "name", "title", "اسم_الامتحان", fallback = "اختبار")
                        val scoreStr = optFirstString(obj, "score", "grade", "الدرجة", fallback = "0")
                        val maxScoreStr = optFirstString(obj, "maxScore", "outOf", "الدرجة_النهائية", fallback = "100")
                        val dtStr = normalizeDate(optFirstString(obj, "date", "تاريخ", fallback = ""))
                        val notesStr = optFirstString(obj, "notes", "ملاحظات", fallback = "")

                        individualExamWrappers.add(
                            BackupIndividualExamWrapper(
                                exam = StudentIndividualExamEntity(
                                    id = ieId,
                                    studentId = stId,
                                    examName = exName,
                                    score = scoreStr,
                                    maxScore = maxScoreStr,
                                    date = dtStr,
                                    notes = notesStr
                                ),
                                rawStudentName = rawStName
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return BackupData(
            exportDate = exportDate,
            version = version,
            groups = groups,
            studentWrappers = studentWrappers,
            attendanceWrappers = attendanceWrappers,
            examWrappers = examWrappers,
            paymentWrappers = paymentWrappers,
            individualExamWrappers = individualExamWrappers
        )
    }

    // Helper extraction methods
    private fun findJsonArray(obj: JSONObject, vararg keys: String): JSONArray? {
        for (k in keys) {
            if (obj.has(k)) {
                val arr = obj.optJSONArray(k)
                if (arr != null) return arr
            }
        }
        return null
    }

    private fun optFirstString(obj: JSONObject, vararg keys: String, fallback: String = ""): String {
        for (k in keys) {
            if (obj.has(k) && !obj.isNull(k)) {
                val str = obj.optString(k, "").trim()
                if (str.isNotBlank()) return str
            }
        }
        return fallback
    }

    private fun optFirstInt(obj: JSONObject, vararg keys: String, fallback: Int = 0): Int {
        for (k in keys) {
            if (obj.has(k) && !obj.isNull(k)) {
                val v = obj.optInt(k, -1)
                if (v >= 0) return v
            }
        }
        return fallback
    }

    private fun optFirstDouble(obj: JSONObject, vararg keys: String, fallback: Double = 0.0): Double {
        for (k in keys) {
            if (obj.has(k) && !obj.isNull(k)) {
                val v = obj.optDouble(k, -1.0)
                if (v >= 0) return v
            }
        }
        return fallback
    }

    private fun scanDeepForStudentWrappers(
        jsonObj: JSONObject, 
        outWrappers: MutableList<BackupStudentWrapper>,
        parentGroupId: Int = 1,
        parentGroupName: String = ""
    ) {
        val keys = jsonObj.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val arr = jsonObj.optJSONArray(k)
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val childObj = arr.optJSONObject(i)
                    if (childObj != null) {
                        // Check if this object represents a group itself to pass down its id/name
                        val possibleGroupId = optFirstInt(childObj, "id", "groupId", "group_id", fallback = parentGroupId)
                        val possibleGroupName = optFirstString(childObj, "name", "groupName", "title", fallback = parentGroupName)

                        val sName = optFirstString(childObj, "fullName", "studentName", "student_name", "student_fullname", "full_name", "اسم_الطالب", "اسم الطالب", "الطالب", "الاسم", "name", "title", "label")
                        
                        // Heuristic: If it has "students" or "days" array, it's probably a group, not a student.
                        val isLikelyGroup = childObj.has("students") || childObj.has("days") || childObj.has("schedule")
                        
                        if (!isLikelyGroup && sName.isNotBlank() && outWrappers.none { it.student.fullName == sName }) {
                            val pPhone = optFirstString(childObj, "parentPhone", "parent_phone", "phone", "mobile", "parentMobile", "parent_mobile", "studentPhone", "student_phone", "phone1", "هاتف_الولي", "رقم_الولي")
                            val sPhone = optFirstString(childObj, "studentPhone", "student_phone", "phone2", "mobile2", "رقم_الطالب")
                            val gId = optFirstInt(childObj, "groupId", "group_id", "class_id", "classId", fallback = possibleGroupId)
                            val rawGName = optFirstString(childObj, "groupName", "group_name", "group", "class", "المجموعة", "اسم_المجموعة", "مجموعة", fallback = possibleGroupName)
                            val sId = optFirstInt(childObj, "id", "studentId", "student_id", fallback = outWrappers.size + 1)
                            outWrappers.add(
                                BackupStudentWrapper(
                                    student = StudentEntity(id = sId, fullName = sName, groupId = gId, parentPhone = pPhone, studentPhone = sPhone),
                                    rawGroupName = rawGName
                                )
                            )
                        }
                        
                        // Always scan inside for nested students (e.g. students array inside a group)
                        scanDeepForStudentWrappers(childObj, outWrappers, possibleGroupId, possibleGroupName)
                    }
                }
            } else {
                val subObj = jsonObj.optJSONObject(k)
                if (subObj != null) {
                    scanDeepForStudentWrappers(subObj, outWrappers, parentGroupId, parentGroupName)
                }
            }
        }
    }

    // Export Backup File and share via Intent
    fun exportBackupFile(context: Context, data: BackupData): Uri? {
        return try {
            val dateStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
            val fileName = "TeacherAssistant_Backup_$dateStamp.json"
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { out ->
                out.write(toJson(data).toByteArray(Charsets.UTF_8))
            }
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "حفظ أو مشاركة النسخة الاحتياطية"))
            contentUri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Read Backup Data from Uri
    fun importBackupFile(context: Context, uri: Uri): BackupData? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val reader = InputStreamReader(inputStream, Charsets.UTF_8)
            val jsonStr = reader.readText()
            reader.close()
            fromJson(jsonStr)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Firebase Firestore Cloud Sync
    fun syncToFirestore(
        data: BackupData,
        userId: String? = null,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        try {
            val docId = userId ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: FIRESTORE_DOC_ID
            val firestore = FirebaseFirestore.getInstance()
            val jsonMap = mapOf(
                "jsonContent" to toJson(data),
                "lastUpdated" to data.exportDate,
                "version" to data.version,
                "userId" to docId
            )

            firestore.collection(FIRESTORE_COLLECTION)
                .document(docId)
                .set(jsonMap)
                .addOnSuccessListener {
                    onSuccess()
                }
                .addOnFailureListener { ex ->
                    onFailure(ex.localizedMessage ?: "حدث خطأ أثناء الاتصال بسحابة Firestore")
                }
        } catch (e: Throwable) {
            onFailure("خدمة Firebase غير متوفرة في هذا الجهاز: ${e.localizedMessage}")
        }
    }

    fun restoreFromFirestore(
        userId: String? = null,
        onSuccess: (BackupData) -> Unit,
        onFailure: (String) -> Unit
    ) {
        try {
            val docId = userId ?: try { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid } catch (_: Throwable) { null } ?: FIRESTORE_DOC_ID
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection(FIRESTORE_COLLECTION)
                .document(docId)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc != null && doc.exists()) {
                        val jsonContent = doc.getString("jsonContent")
                        if (!jsonContent.isNullOrBlank()) {
                            try {
                                val backupData = fromJson(jsonContent)
                                onSuccess(backupData)
                            } catch (e: Exception) {
                                onFailure("فشل في فك تشفير البيانات من السحابة: ${e.localizedMessage}")
                            }
                        } else {
                            onFailure("لم يتم العثور على نسخة احتياطية سحابية مخزنة.")
                        }
                    } else {
                        onFailure("لا توجد بيانات مسجلة في حساب Firestore بعد لهذا المستخدم.")
                    }
                }
                .addOnFailureListener { ex ->
                    onFailure(ex.localizedMessage ?: "فشل الاتصال بسحابة Firestore")
                }
        } catch (e: Throwable) {
            onFailure("خدمة Firebase غير متوفرة في هذا الجهاز: ${e.localizedMessage}")
        }
    }
}

