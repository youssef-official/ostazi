import sys

with open("app/src/main/java/com/example/data/TeacherRepository.kt", "r") as f:
    content = f.read()

old_set = """    suspend fun setAttendanceAndHomework(
        studentId: Int,
        date: String,
        attendanceStatus: String,
        homeworkStatus: String,
        recitationStatus: String = "ممتاز"
    ) {
        val existing = dao.getAttendanceForStudentAndDate(studentId, date)
        val record = existing?.copy(
            attendanceStatus = attendanceStatus,
            homeworkStatus = homeworkStatus,
            recitationStatus = recitationStatus
        ) ?: AttendanceRecordEntity(
            studentId = studentId,
            date = date,
            attendanceStatus = attendanceStatus,
            homeworkStatus = homeworkStatus,
            recitationStatus = recitationStatus
        )
        dao.upsertAttendance(record)
    }"""

new_set = """    suspend fun setAttendanceAndHomework(
        studentId: Int,
        date: String,
        attendanceStatus: String,
        homeworkStatus: String,
        interactionStatus: String = "ممتاز",
        recitationGrade: String = "",
        recitationStatus: String = "ممتاز" // Keep for legacy
    ) {
        val existing = dao.getAttendanceForStudentAndDate(studentId, date)
        val record = existing?.copy(
            attendanceStatus = attendanceStatus,
            homeworkStatus = homeworkStatus,
            interactionStatus = interactionStatus,
            recitationGrade = recitationGrade,
            recitationStatus = recitationStatus
        ) ?: AttendanceRecordEntity(
            studentId = studentId,
            date = date,
            attendanceStatus = attendanceStatus,
            homeworkStatus = homeworkStatus,
            interactionStatus = interactionStatus,
            recitationGrade = recitationGrade,
            recitationStatus = recitationStatus
        )
        dao.upsertAttendance(record)
    }"""

content = content.replace(old_set, new_set)

with open("app/src/main/java/com/example/data/TeacherRepository.kt", "w") as f:
    f.write(content)

