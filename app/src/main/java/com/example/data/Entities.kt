package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val subject: String,
    val day1: String,
    val day2: String? = null,
    val day3: String? = null,
    val day4: String? = null,
    val day5: String? = null,
    val day6: String? = null,
    val day7: String? = null,
    val timeSlot: String, // Treat as timeSlot1
    val timeSlot2: String? = null,
    val timeSlot3: String? = null,
    val timeSlot4: String? = null,
    val timeSlot5: String? = null,
    val timeSlot6: String? = null,
    val timeSlot7: String? = null,
    val monthlyFee: Double,
    val groupType: String = "CENTER", // "CENTER" (سنتر), "ONLINE" (أونلاين), or "PRIVATE" (خاص)
    val paymentType: String = "MONTHLY", // "MONTHLY" (دفع شهري) or "PER_SESSION" (دفع بالحصة)
    val whatsappGroupUrl: String = "",
    val sortOrder: Int = 0,
    val notes: String = ""
)

fun normalizeArabicDay(day: String?): String {
    if (day.isNullOrBlank()) return ""
    val clean = day.trim()
    if (clean == "بدون" || clean.startsWith("بدون")) return ""
    return clean
        .replace("أ", "ا")
        .replace("إ", "ا")
        .replace("آ", "ا")
        .replace("ة", "ه")
        .replace("ى", "ي")
}

fun GroupEntity.meetsOnDay(dayName: String): Boolean {
    val normTarget = normalizeArabicDay(dayName)
    if (normTarget.isEmpty()) return false

    val d1 = normalizeArabicDay(day1)
    val d2 = normalizeArabicDay(day2)
    val d3 = normalizeArabicDay(day3)
    val d4 = normalizeArabicDay(day4)
    val d5 = normalizeArabicDay(day5)
    val d6 = normalizeArabicDay(day6)
    val d7 = normalizeArabicDay(day7)

    return d1 == normTarget || d2 == normTarget || d3 == normTarget ||
           d4 == normTarget || d5 == normTarget || d6 == normTarget || d7 == normTarget
}

fun GroupEntity.getTimeSlotForDay(dayName: String): String {
    val normTarget = normalizeArabicDay(dayName)
    if (normTarget.isEmpty()) return timeSlot

    val d1 = normalizeArabicDay(day1)
    val d2 = normalizeArabicDay(day2)
    val d3 = normalizeArabicDay(day3)
    val d4 = normalizeArabicDay(day4)
    val d5 = normalizeArabicDay(day5)
    val d6 = normalizeArabicDay(day6)
    val d7 = normalizeArabicDay(day7)

    return when {
        d1 == normTarget -> timeSlot
        d2 == normTarget && !timeSlot2.isNullOrBlank() -> timeSlot2!!
        d3 == normTarget && !timeSlot3.isNullOrBlank() -> timeSlot3!!
        d4 == normTarget && !timeSlot4.isNullOrBlank() -> timeSlot4!!
        d5 == normTarget && !timeSlot5.isNullOrBlank() -> timeSlot5!!
        d6 == normTarget && !timeSlot6.isNullOrBlank() -> timeSlot6!!
        d7 == normTarget && !timeSlot7.isNullOrBlank() -> timeSlot7!!
        else -> timeSlot
    }
}

@Entity(
    tableName = "students",
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["groupId"])]
)
data class StudentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fullName: String,
    val groupId: Int,
    val parentPhone: String,
    val studentPhone: String = "",
    val discountAmount: Double = 0.0
)

@Entity(
    tableName = "attendance_records",
    indices = [Index(value = ["studentId", "date"], unique = true)]
)
data class AttendanceRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: Int,
    val date: String,
    val attendanceStatus: String = "", // "", "حضر", "غائب", "متأخر"
    val homeworkStatus: String = "كتب", // "كتب", "لم يكتب", "متأخر", "لا يوجد"
    val interactionStatus: String = "ممتاز", // "ممتاز", "جيد", "ضعيف"
    val recitationGrade: String = "", // e.g. "9/10"
    val recitationStatus: String = "ممتاز", // Deprecated/legacy field
    val notes: String = "", // Notes on student for this session
    val sessionPaid: Boolean = false,
    val sessionPaidAmount: Double = 0.0,
    val sessionPaymentStatus: String = "UNPAID" // "PAID", "UNPAID", "PARTIAL", "EXEMPT"
)

@Entity(
    tableName = "exam_records",
    indices = [Index(value = ["studentId"], unique = true)]
)
data class ExamRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: Int,
    val exam1: String? = null,
    val exam2: String? = null,
    val exam3: String? = null,
    val exam4: String? = null,
    val exam5: String? = null
)

@Entity(
    tableName = "payment_records",
    indices = [Index(value = ["studentId", "monthYear"], unique = true)]
)
data class PaymentRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: Int,
    val monthYear: String, // e.g. "2026-08"
    val paymentStatus: String = "UNPAID", // "PAID", "UNPAID", "PARTIAL", "EXEMPT"
    val remainingAmount: Double = 0.0,
    val paymentDate: String = "", // e.g. "2026-08-07"
    val discountAmount: Double = 0.0,
    val paidAmount: Double = 0.0
)

@Entity(
    tableName = "group_exams",
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["groupId"])]
)
data class GroupExamEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val groupId: Int,
    val name: String,
    val date: String,
    val googleFormLink: String = "",
    val maxScore: Int = 100
)

@Entity(
    tableName = "student_exam_scores",
    foreignKeys = [
        ForeignKey(
            entity = GroupExamEntity::class,
            parentColumns = ["id"],
            childColumns = ["examId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["examId", "studentId"], unique = true),
        Index(value = ["studentId"])
    ]
)
data class StudentExamScoreEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val examId: Int,
    val studentId: Int,
    val score: String = "" // "85", "غائب"
)

@Entity(
    tableName = "student_individual_exams",
    foreignKeys = [
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["studentId"])]
)
data class StudentIndividualExamEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: Int,
    val examName: String,
    val score: String,
    val maxScore: String = "100",
    val date: String,
    val notes: String = ""
)
