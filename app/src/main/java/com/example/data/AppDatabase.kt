package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        GroupEntity::class,
        StudentEntity::class,
        AttendanceRecordEntity::class,
        ExamRecordEntity::class,
        PaymentRecordEntity::class,
        GroupExamEntity::class,
        StudentExamScoreEntity::class,
        StudentIndividualExamEntity::class
    ],
    version = 15,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appDao(): AppDao

    companion object {
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

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE attendance_records ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exam_records ADD COLUMN exam4 TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE exam_records ADD COLUMN exam5 TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `student_individual_exams` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `studentId` INTEGER NOT NULL, `examName` TEXT NOT NULL, `score` TEXT NOT NULL, `maxScore` TEXT NOT NULL DEFAULT '100', `date` TEXT NOT NULL, `notes` TEXT NOT NULL DEFAULT '', FOREIGN KEY(`studentId`) REFERENCES `students`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_student_individual_exams_studentId` ON `student_individual_exams` (`studentId`)")
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE groups ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE students ADD COLUMN discountAmount REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE payment_records ADD COLUMN discountAmount REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE payment_records ADD COLUMN paidAmount REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE attendance_records ADD COLUMN sessionPaid INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE attendance_records ADD COLUMN sessionPaidAmount REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE attendance_records ADD COLUMN sessionPaymentStatus TEXT NOT NULL DEFAULT 'UNPAID'")
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_student_exam_scores_studentId` ON `student_exam_scores` (`studentId`)")
                } catch (_: Exception) {}
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                try {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "teacher_assistant_db"
                    )
                    .addMigrations(MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15)
                    .fallbackToDestructiveMigration(true)
                    .addCallback(DatabaseCallback(context.applicationContext))
                    .build()
                    INSTANCE = instance
                    instance
                } catch (e: Exception) {
                    val fallbackInstance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "teacher_assistant_db"
                    )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    INSTANCE = fallbackInstance
                    fallbackInstance
                }
            }
        }

        private class DatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // App starts completely clean with no pre-registered groups or students
            }
        }
    }
}
