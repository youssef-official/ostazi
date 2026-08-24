import sys

with open("app/src/main/java/com/example/data/AppDatabase.kt", "r") as f:
    content = f.read()

content = content.replace(
    "import androidx.room.RoomDatabase",
    "import androidx.room.RoomDatabase\nimport androidx.room.migration.Migration"
)

old_entities = """entities = [
        GroupEntity::class,
        StudentEntity::class,
        AttendanceRecordEntity::class,
        ExamRecordEntity::class,
        PaymentRecordEntity::class
    ],
    version = 8,"""

new_entities = """entities = [
        GroupEntity::class,
        StudentEntity::class,
        AttendanceRecordEntity::class,
        ExamRecordEntity::class,
        PaymentRecordEntity::class,
        GroupExamEntity::class,
        StudentExamScoreEntity::class
    ],
    version = 9,"""

content = content.replace(old_entities, new_entities)

migration = """
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add new columns to attendance_records
                db.execSQL("ALTER TABLE attendance_records ADD COLUMN interactionStatus TEXT NOT NULL DEFAULT 'ممتاز'")
                db.execSQL("ALTER TABLE attendance_records ADD COLUMN recitationGrade TEXT NOT NULL DEFAULT ''")
                
                // Create group_exams table
                db.execSQL("CREATE TABLE IF NOT EXISTS `group_exams` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `groupId` INTEGER NOT NULL, `name` TEXT NOT NULL, `date` TEXT NOT NULL, `googleFormLink` TEXT NOT NULL, `maxScore` INTEGER NOT NULL, FOREIGN KEY(`groupId`) REFERENCES `groups`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_group_exams_groupId` ON `group_exams` (`groupId`)")
                
                // Create student_exam_scores table
                db.execSQL("CREATE TABLE IF NOT EXISTS `student_exam_scores` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `examId` INTEGER NOT NULL, `studentId` INTEGER NOT NULL, `score` TEXT NOT NULL, FOREIGN KEY(`examId`) REFERENCES `group_exams`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`studentId`) REFERENCES `students`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_student_exam_scores_examId_studentId` ON `student_exam_scores` (`examId`, `studentId`)")
            }
        }
"""

content = content.replace(
    ".fallbackToDestructiveMigration(true)",
    ".addMigrations(MIGRATION_8_9)\n                .fallbackToDestructiveMigration(true)"
)

content = content.replace(
    "companion object {",
    "companion object {" + migration
)

with open("app/src/main/java/com/example/data/AppDatabase.kt", "w") as f:
    f.write(content)

