package com.example.data

import com.example.backup.BackupData
import com.example.backup.BackupStudentWrapper
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

class TeacherRepository(private val dao: AppDao) {

    // --- Groups ---
    val allGroups: Flow<List<GroupEntity>> = dao.getAllGroups()

    suspend fun getGroupById(id: Int): GroupEntity? = dao.getGroupById(id)

    suspend fun insertGroup(group: GroupEntity): Long = dao.insertGroup(group)

    suspend fun updateGroup(group: GroupEntity) = dao.updateGroup(group)

    suspend fun deleteGroup(group: GroupEntity) = dao.deleteGroup(group)

    suspend fun moveGroupUp(group: GroupEntity, currentList: List<GroupEntity>) {
        val index = currentList.indexOfFirst { it.id == group.id }
        if (index > 0) {
            val prevGroup = currentList[index - 1]
            dao.updateGroupOrder(group.id, prevGroup.sortOrder)
            dao.updateGroupOrder(prevGroup.id, group.sortOrder)
        }
    }

    suspend fun moveGroupDown(group: GroupEntity, currentList: List<GroupEntity>) {
        val index = currentList.indexOfFirst { it.id == group.id }
        if (index >= 0 && index < currentList.size - 1) {
            val nextGroup = currentList[index + 1]
            dao.updateGroupOrder(group.id, nextGroup.sortOrder)
            dao.updateGroupOrder(nextGroup.id, group.sortOrder)
        }
    }

    // --- Students ---
    val allStudents: Flow<List<StudentEntity>> = dao.getAllStudents()

    fun getStudentsByGroup(groupId: Int): Flow<List<StudentEntity>> = dao.getStudentsByGroup(groupId)

    suspend fun insertStudent(student: StudentEntity): Long = dao.insertStudent(student)

    suspend fun updateStudent(student: StudentEntity) = dao.updateStudent(student)

    suspend fun deleteStudent(student: StudentEntity) = dao.deleteStudent(student)

    // --- Attendance ---
    fun getAttendanceForDate(date: String): Flow<List<AttendanceRecordEntity>> =
        dao.getAttendanceForDate(date)

    fun getAttendanceForMonth(monthPrefix: String): Flow<List<AttendanceRecordEntity>> =
        dao.getAttendanceForMonth(monthPrefix)

    fun getAttendanceBetweenDates(startDate: String, endDate: String): Flow<List<AttendanceRecordEntity>> =
        dao.getAttendanceBetweenDates(startDate, endDate)

    suspend fun getAttendanceForMonthList(monthPrefix: String): List<AttendanceRecordEntity> =
        dao.getAttendanceForMonthList(monthPrefix)

    fun getAttendanceForStudent(studentId: Int): Flow<List<AttendanceRecordEntity>> =
        dao.getAttendanceForStudent(studentId)

    suspend fun setAttendanceAndHomework(
        studentId: Int,
        date: String,
        attendanceStatus: String,
        homeworkStatus: String,
        interactionStatus: String = "ممتاز",
        recitationGrade: String = "",
        recitationStatus: String = "ممتاز",
        notes: String? = null
    ) {
        val existing = dao.getAttendanceForStudentAndDate(studentId, date)
        val finalNotes = notes ?: existing?.notes ?: ""
        val record = existing?.copy(
            attendanceStatus = attendanceStatus,
            homeworkStatus = homeworkStatus,
            interactionStatus = interactionStatus,
            recitationGrade = recitationGrade,
            recitationStatus = recitationStatus,
            notes = finalNotes
        ) ?: AttendanceRecordEntity(
            studentId = studentId,
            date = date,
            attendanceStatus = attendanceStatus,
            homeworkStatus = homeworkStatus,
            interactionStatus = interactionStatus,
            recitationGrade = recitationGrade,
            recitationStatus = recitationStatus,
            notes = finalNotes
        )
        dao.upsertAttendance(record)
    }

    suspend fun setStudentNote(
        studentId: Int,
        date: String,
        note: String
    ) {
        val existing = dao.getAttendanceForStudentAndDate(studentId, date)
        val record = existing?.copy(notes = note) ?: AttendanceRecordEntity(
            studentId = studentId,
            date = date,
            notes = note
        )
        dao.upsertAttendance(record)
    }

    suspend fun batchSetAttendance(
        studentIds: List<Int>,
        date: String,
        attendanceStatus: String? = null,
        homeworkStatus: String? = null,
        interactionStatus: String? = null,
        recitationGrade: String? = null,
        notes: String? = null
    ) {
        studentIds.forEach { sId ->
            val existing = dao.getAttendanceForStudentAndDate(sId, date)
            val record = existing?.copy(
                attendanceStatus = attendanceStatus ?: existing.attendanceStatus,
                homeworkStatus = homeworkStatus ?: existing.homeworkStatus,
                interactionStatus = interactionStatus ?: existing.interactionStatus,
                recitationGrade = recitationGrade ?: existing.recitationGrade,
                notes = notes ?: existing.notes
            ) ?: AttendanceRecordEntity(
                studentId = sId,
                date = date,
                attendanceStatus = attendanceStatus ?: "حضر",
                homeworkStatus = homeworkStatus ?: "كتب",
                interactionStatus = interactionStatus ?: "ممتاز",
                recitationGrade = recitationGrade ?: "10/10",
                notes = notes ?: ""
            )
            dao.upsertAttendance(record)
        }
    }

    suspend fun setSessionPayment(
        studentId: Int,
        date: String,
        isPaid: Boolean,
        paidAmount: Double = 0.0,
        paymentStatus: String = if (isPaid) "PAID" else "UNPAID"
    ) {
        val existing = dao.getAttendanceForStudentAndDate(studentId, date)
        val record = existing?.copy(
            sessionPaid = isPaid,
            sessionPaidAmount = paidAmount,
            sessionPaymentStatus = paymentStatus
        ) ?: AttendanceRecordEntity(
            studentId = studentId,
            date = date,
            sessionPaid = isPaid,
            sessionPaidAmount = paidAmount,
            sessionPaymentStatus = paymentStatus
        )
        dao.upsertAttendance(record)
    }

    suspend fun batchSetSessionPayment(
        studentIds: List<Int>,
        date: String,
        isPaid: Boolean,
        defaultAmount: Double = 0.0
    ) {
        studentIds.forEach { sId ->
            val existing = dao.getAttendanceForStudentAndDate(sId, date)
            val record = existing?.copy(
                sessionPaid = isPaid,
                sessionPaidAmount = if (isPaid) defaultAmount else 0.0,
                sessionPaymentStatus = if (isPaid) "PAID" else "UNPAID"
            ) ?: AttendanceRecordEntity(
                studentId = sId,
                date = date,
                sessionPaid = isPaid,
                sessionPaidAmount = if (isPaid) defaultAmount else 0.0,
                sessionPaymentStatus = if (isPaid) "PAID" else "UNPAID"
            )
            dao.upsertAttendance(record)
        }
    }

    // --- Exams ---
    fun getExamForStudent(studentId: Int): Flow<ExamRecordEntity?> = dao.getExamForStudent(studentId)

    val allExams: Flow<List<ExamRecordEntity>> = dao.getAllExams()

    // --- Individual Student Exams (Unlimited 50+) ---
    fun getIndividualExamsForStudent(studentId: Int): Flow<List<StudentIndividualExamEntity>> =
        dao.getIndividualExamsForStudent(studentId)

    suspend fun addIndividualExam(exam: StudentIndividualExamEntity): Long =
        dao.insertIndividualExam(exam)

    suspend fun deleteIndividualExam(exam: StudentIndividualExamEntity) =
        dao.deleteIndividualExam(exam)

    suspend fun deleteIndividualExamById(id: Int) =
        dao.deleteIndividualExamById(id)

    suspend fun saveExams(studentId: Int, exam1: String?, exam2: String?, exam3: String?, exam4: String? = null, exam5: String? = null) {
        dao.upsertExam(
            ExamRecordEntity(
                studentId = studentId,
                exam1 = exam1,
                exam2 = exam2,
                exam3 = exam3,
                exam4 = exam4,
                exam5 = exam5
            )
        )
    }

    // --- Payments ---
    fun getPaymentsForMonth(monthYear: String): Flow<List<PaymentRecordEntity>> =
        dao.getPaymentsForMonth(monthYear)

    val allPayments: Flow<List<PaymentRecordEntity>> = dao.getAllPayments()

    fun getAllPaymentsForStudent(studentId: Int): Flow<List<PaymentRecordEntity>> =
        dao.getAllPaymentsForStudent(studentId)

    suspend fun setPaymentStatus(
        studentId: Int,
        monthYear: String,
        status: String,
        remainingAmount: Double = 0.0,
        paymentDate: String? = null
    ) {
        val existing = dao.getPaymentForStudentAndMonth(studentId, monthYear)
        val currentDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val finalDate = paymentDate ?: if (status != "UNPAID") {
            if (!existing?.paymentDate.isNullOrEmpty()) existing.paymentDate else currentDateStr
        } else {
            ""
        }
        val record = existing?.copy(
            paymentStatus = status,
            remainingAmount = remainingAmount,
            paymentDate = finalDate
        ) ?: PaymentRecordEntity(
            studentId = studentId,
            monthYear = monthYear,
            paymentStatus = status,
            remainingAmount = remainingAmount,
            paymentDate = finalDate
        )
        dao.upsertPayment(record)
    }

    suspend fun setPaymentStatusWithDiscount(
        studentId: Int,
        monthYear: String,
        status: String,
        discountAmount: Double = 0.0,
        paidAmount: Double = 0.0,
        remainingAmount: Double = 0.0,
        paymentDate: String? = null
    ) {
        val existing = dao.getPaymentForStudentAndMonth(studentId, monthYear)
        val currentDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val finalDate = paymentDate ?: if (status != "UNPAID") {
            if (!existing?.paymentDate.isNullOrEmpty()) existing.paymentDate else currentDateStr
        } else {
            ""
        }
        val record = existing?.copy(
            paymentStatus = status,
            discountAmount = discountAmount,
            paidAmount = paidAmount,
            remainingAmount = remainingAmount,
            paymentDate = finalDate
        ) ?: PaymentRecordEntity(
            studentId = studentId,
            monthYear = monthYear,
            paymentStatus = status,
            discountAmount = discountAmount,
            paidAmount = paidAmount,
            remainingAmount = remainingAmount,
            paymentDate = finalDate
        )
        dao.upsertPayment(record)
    }

    suspend fun setSessionDetails(
        studentId: Int,
        date: String,
        attendanceStatus: String,
        homeworkStatus: String,
        interactionStatus: String = "ممتاز",
        recitationGrade: String = "",
        notes: String = "",
        sessionPaid: Boolean = false,
        sessionPaidAmount: Double = 0.0,
        sessionPaymentStatus: String = "UNPAID"
    ) {
        val existing = dao.getAttendanceForStudentAndDate(studentId, date)
        val record = existing?.copy(
            attendanceStatus = attendanceStatus,
            homeworkStatus = homeworkStatus,
            interactionStatus = interactionStatus,
            recitationGrade = recitationGrade,
            notes = notes,
            sessionPaid = sessionPaid,
            sessionPaidAmount = sessionPaidAmount,
            sessionPaymentStatus = sessionPaymentStatus
        ) ?: AttendanceRecordEntity(
            studentId = studentId,
            date = date,
            attendanceStatus = attendanceStatus,
            homeworkStatus = homeworkStatus,
            interactionStatus = interactionStatus,
            recitationGrade = recitationGrade,
            recitationStatus = "ممتاز",
            notes = notes,
            sessionPaid = sessionPaid,
            sessionPaidAmount = sessionPaidAmount,
            sessionPaymentStatus = sessionPaymentStatus
        )
        dao.upsertAttendance(record)
    }

    // --- Full Database Backup & Restore Helpers ---
    suspend fun getAllGroupsList(): List<GroupEntity> = dao.getAllGroupsList()
    suspend fun getAllStudentsList(): List<StudentEntity> = dao.getAllStudentsList()
    suspend fun getAllAttendanceList(): List<AttendanceRecordEntity> = dao.getAllAttendanceList()
    suspend fun getAllExamsList(): List<ExamRecordEntity> = dao.getAllExamsList()
    suspend fun getAllPaymentsList(): List<PaymentRecordEntity> = dao.getAllPaymentsList()
    suspend fun getAllIndividualExamsList(): List<StudentIndividualExamEntity> = dao.getAllIndividualExamsList()

    suspend fun restoreAllData(
        groups: List<GroupEntity>,
        students: List<StudentEntity>,
        attendance: List<AttendanceRecordEntity>,
        exams: List<ExamRecordEntity>,
        payments: List<PaymentRecordEntity>
    ) {
        restoreAllData(
            BackupData(
                exportDate = "",
                version = 1,
                groups = groups,
                students = students,
                attendanceRecords = attendance,
                examRecords = exams,
                paymentRecords = payments
            )
        )
    }

    suspend fun clearAllData() {
        dao.deleteAllAttendance()
        dao.deleteAllExams()
        dao.deleteAllPayments()
        dao.deleteAllIndividualExams()
        dao.deleteAllStudents()
        dao.deleteAllGroups()
    }

    suspend fun restoreAllData(backupData: BackupData) {
        dao.deleteAllAttendance()
        dao.deleteAllExams()
        dao.deleteAllPayments()
        dao.deleteAllIndividualExams()
        dao.deleteAllStudents()
        dao.deleteAllGroups()

        val oldToNewGroupId = mutableMapOf<Int, Int>()
        val groupNameToId = mutableMapOf<String, Int>()

        val effectiveGroups = backupData.groups.toMutableList()
        val effectiveStudentWrappers = if (backupData.studentWrappers.isNotEmpty()) {
            backupData.studentWrappers
        } else {
            backupData.students.map { BackupStudentWrapper(it) }
        }

        effectiveStudentWrappers.forEach { sw ->
            val gName = sw.rawGroupName.trim()
            if (gName.isNotEmpty() && effectiveGroups.none { it.name.trim().equals(gName, ignoreCase = true) }) {
                effectiveGroups.add(
                    GroupEntity(
                        id = 0,
                        name = gName,
                        subject = "مادة عامة",
                        day1 = "السبت",
                        timeSlot = "04:00 م",
                        monthlyFee = 150.0,
                        groupType = "CENTER",
                        sortOrder = effectiveGroups.size + 1
                    )
                )
            }
        }

        if (effectiveGroups.isEmpty() && effectiveStudentWrappers.isNotEmpty()) {
            effectiveGroups.add(
                GroupEntity(
                    id = 0,
                    name = "المجموعة الأولى (مستوردة)",
                    subject = "عام",
                    day1 = "السبت",
                    timeSlot = "04:00 م",
                    monthlyFee = 150.0,
                    groupType = "CENTER",
                    sortOrder = 1
                )
            )
        }

        effectiveGroups.forEachIndexed { index, g ->
            val oldId = g.id
            val groupToInsert = g.copy(id = 0)
            val insertedId = dao.insertGroup(groupToInsert).toInt()
            val newId = if (insertedId > 0) insertedId else oldId
            if (oldId > 0) {
                oldToNewGroupId[oldId] = newId
            }
            // Also map 1-based index
            oldToNewGroupId[index + 1] = newId

            if (g.name.isNotBlank()) {
                val cleanKey = g.name.trim().lowercase(Locale.ROOT)
                groupNameToId[cleanKey] = newId
            }
        }

        val allInsertedGroups = dao.getAllGroupsList()
        val fallbackGroupId = allInsertedGroups.firstOrNull()?.id ?: 1

        val oldToNewStudentId = mutableMapOf<Int, Int>()
        val studentNameToId = mutableMapOf<String, Int>()

        effectiveStudentWrappers.forEach { sw ->
            val s = sw.student
            val oldId = s.id
            val rawGName = sw.rawGroupName.trim().lowercase(Locale.ROOT)

            val targetGroupId = oldToNewGroupId[s.groupId]
                ?: (if (rawGName.isNotEmpty()) groupNameToId[rawGName] else null)
                ?: (if (rawGName.isNotEmpty()) groupNameToId.entries.firstOrNull { it.key.contains(rawGName) || rawGName.contains(it.key) }?.value else null)
                ?: (if (s.groupId > 0 && s.groupId <= effectiveGroups.size) {
                    val groupNameAtIndex = effectiveGroups.getOrNull(s.groupId - 1)?.name?.trim()?.lowercase(Locale.ROOT)
                    groupNameToId[groupNameAtIndex]
                } else null)
                ?: if (allInsertedGroups.any { it.id == s.groupId }) s.groupId else fallbackGroupId

            val newStudent = s.copy(id = 0, groupId = targetGroupId)
            val insertedStudentId = dao.insertStudent(newStudent).toInt()
            val newStudentId = if (insertedStudentId > 0) insertedStudentId else oldId

            if (oldId > 0) {
                oldToNewStudentId[oldId] = newStudentId
            }
            if (s.fullName.isNotBlank()) {
                studentNameToId[s.fullName.trim().lowercase(Locale.ROOT)] = newStudentId
            }
        }

        val allInsertedStudents = dao.getAllStudentsList()
        val firstStudentId = allInsertedStudents.firstOrNull()?.id ?: 0

        fun resolveStudentId(oldStudentId: Int, rawStudentName: String): Int {
            if (oldStudentId > 0 && oldToNewStudentId.containsKey(oldStudentId)) {
                return oldToNewStudentId[oldStudentId]!!
            }
            if (allInsertedStudents.any { it.id == oldStudentId }) {
                return oldStudentId
            }
            val cleanName = rawStudentName.trim().lowercase(Locale.ROOT)
            if (cleanName.isNotEmpty() && studentNameToId.containsKey(cleanName)) {
                return studentNameToId[cleanName]!!
            }
            if (cleanName.isNotEmpty()) {
                val match = allInsertedStudents.firstOrNull {
                    it.fullName.trim().lowercase(Locale.ROOT).contains(cleanName) || cleanName.contains(it.fullName.trim().lowercase(Locale.ROOT))
                }
                if (match != null) return match.id
            }
            return if (oldStudentId > 0) oldStudentId else firstStudentId
        }

        backupData.attendanceWrappers.forEach { aw ->
            val att = aw.attendance
            val targetStId = resolveStudentId(att.studentId, aw.rawStudentName)
            if (targetStId > 0) {
                dao.upsertAttendance(att.copy(id = 0, studentId = targetStId))
            }
        }

        backupData.examWrappers.forEach { ew ->
            val ex = ew.exam
            val targetStId = resolveStudentId(ex.studentId, ew.rawStudentName)
            if (targetStId > 0) {
                dao.upsertExam(ex.copy(id = 0, studentId = targetStId))
            }
        }

        backupData.paymentWrappers.forEach { pw ->
            val pay = pw.payment
            val targetStId = resolveStudentId(pay.studentId, pw.rawStudentName)
            if (targetStId > 0) {
                dao.upsertPayment(pay.copy(id = 0, studentId = targetStId))
            }
        }

        backupData.individualExamWrappers.forEach { iw ->
            val indEx = iw.exam
            val targetStId = resolveStudentId(indEx.studentId, iw.rawStudentName)
            if (targetStId > 0) {
                dao.insertIndividualExam(indEx.copy(id = 0, studentId = targetStId))
            }
        }
    }
}
