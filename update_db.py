import sys

with open("app/src/main/java/com/example/data/Entities.kt", "r") as f:
    content = f.read()

# Update AttendanceRecordEntity
old_attendance = """data class AttendanceRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: Int,
    val date: String,
    val attendanceStatus: String = "حضر", // "حضر", "غائب", "متأخر"
    val homeworkStatus: String = "كتب الواجب", // "كتب الواجب", "لم يكتب الواجب", "متأخر"
    val recitationStatus: String = "ممتاز" // "ممتاز", "جيد", "ضعيف"
)"""

new_attendance = """data class AttendanceRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: Int,
    val date: String,
    val attendanceStatus: String = "حضر", // "حضر", "غائب", "متأخر"
    val homeworkStatus: String = "كتب", // "كتب", "لم يكتب", "متأخر", "لا يوجد"
    val interactionStatus: String = "ممتاز", // "ممتاز", "جيد", "ضعيف"
    val recitationGrade: String = "", // e.g. "9/10"
    val recitationStatus: String = "ممتاز" // Deprecated/legacy field
)"""

content = content.replace(old_attendance, new_attendance)

# Add new Exam Entities
new_exam_entities = """
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
    indices = [Index(value = ["examId", "studentId"], unique = true)]
)
data class StudentExamScoreEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val examId: Int,
    val studentId: Int,
    val score: String = "" // "85", "غائب"
)
"""

if "GroupExamEntity" not in content:
    content += new_exam_entities

with open("app/src/main/java/com/example/data/Entities.kt", "w") as f:
    f.write(content)

