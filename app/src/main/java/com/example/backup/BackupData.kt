package com.example.backup

import com.example.data.*

data class BackupStudentWrapper(
    val student: StudentEntity,
    val rawGroupName: String = ""
)

data class BackupAttendanceWrapper(
    val attendance: AttendanceRecordEntity,
    val rawStudentName: String = ""
)

data class BackupExamWrapper(
    val exam: ExamRecordEntity,
    val rawStudentName: String = ""
)

data class BackupPaymentWrapper(
    val payment: PaymentRecordEntity,
    val rawStudentName: String = ""
)

data class BackupIndividualExamWrapper(
    val exam: StudentIndividualExamEntity,
    val rawStudentName: String = ""
)

data class BackupData(
    val exportDate: String = "",
    val version: Int = 1,
    val groups: List<GroupEntity> = emptyList(),
    val studentWrappers: List<BackupStudentWrapper> = emptyList(),
    val attendanceWrappers: List<BackupAttendanceWrapper> = emptyList(),
    val examWrappers: List<BackupExamWrapper> = emptyList(),
    val paymentWrappers: List<BackupPaymentWrapper> = emptyList(),
    val individualExamWrappers: List<BackupIndividualExamWrapper> = emptyList(),
    
    val rawStudents: List<StudentEntity> = emptyList(),
    val rawAttendanceRecords: List<AttendanceRecordEntity> = emptyList(),
    val rawExamRecords: List<ExamRecordEntity> = emptyList(),
    val rawPaymentRecords: List<PaymentRecordEntity> = emptyList(),
    val rawIndividualExams: List<StudentIndividualExamEntity> = emptyList()
) {
    constructor(
        exportDate: String = "",
        version: Int = 1,
        groups: List<GroupEntity> = emptyList(),
        students: List<StudentEntity> = emptyList(),
        attendanceRecords: List<AttendanceRecordEntity> = emptyList(),
        examRecords: List<ExamRecordEntity> = emptyList(),
        paymentRecords: List<PaymentRecordEntity> = emptyList(),
        individualExams: List<StudentIndividualExamEntity> = emptyList()
    ) : this(
        exportDate = exportDate,
        version = version,
        groups = groups,
        studentWrappers = students.map { s ->
            val gName = groups.find { it.id == s.groupId }?.name ?: ""
            BackupStudentWrapper(s, gName)
        },
        attendanceWrappers = attendanceRecords.map { a ->
            val sName = students.find { it.id == a.studentId }?.fullName ?: ""
            BackupAttendanceWrapper(a, sName)
        },
        examWrappers = examRecords.map { e ->
            val sName = students.find { it.id == e.studentId }?.fullName ?: ""
            BackupExamWrapper(e, sName)
        },
        paymentWrappers = paymentRecords.map { p ->
            val sName = students.find { it.id == p.studentId }?.fullName ?: ""
            BackupPaymentWrapper(p, sName)
        },
        individualExamWrappers = individualExams.map { ie ->
            val sName = students.find { it.id == ie.studentId }?.fullName ?: ""
            BackupIndividualExamWrapper(ie, sName)
        },
        rawStudents = students,
        rawAttendanceRecords = attendanceRecords,
        rawExamRecords = examRecords,
        rawPaymentRecords = paymentRecords,
        rawIndividualExams = individualExams
    )

    val students: List<StudentEntity>
        get() = if (studentWrappers.isNotEmpty()) studentWrappers.map { it.student } else rawStudents

    val attendanceRecords: List<AttendanceRecordEntity>
        get() = if (attendanceWrappers.isNotEmpty()) attendanceWrappers.map { it.attendance } else rawAttendanceRecords

    val examRecords: List<ExamRecordEntity>
        get() = if (examWrappers.isNotEmpty()) examWrappers.map { it.exam } else rawExamRecords

    val paymentRecords: List<PaymentRecordEntity>
        get() = if (paymentWrappers.isNotEmpty()) paymentWrappers.map { it.payment } else rawPaymentRecords

    val individualExams: List<StudentIndividualExamEntity>
        get() = if (individualExamWrappers.isNotEmpty()) individualExamWrappers.map { it.exam } else rawIndividualExams
}

