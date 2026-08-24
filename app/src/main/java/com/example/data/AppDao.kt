package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    // --- Groups ---
    @Query("SELECT * FROM groups ORDER BY sortOrder ASC, id ASC")
    fun getAllGroups(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE id = :id")
    suspend fun getGroupById(id: Int): GroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntity): Long

    @Update
    suspend fun updateGroup(group: GroupEntity)

    @Delete
    suspend fun deleteGroup(group: GroupEntity)

    @Query("UPDATE groups SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateGroupOrder(id: Int, sortOrder: Int)

    // --- Students ---
    @Query("SELECT * FROM students ORDER BY fullName ASC")
    fun getAllStudents(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE groupId = :groupId ORDER BY fullName ASC")
    fun getStudentsByGroup(groupId: Int): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE id = :id")
    suspend fun getStudentById(id: Int): StudentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity): Long

    @Update
    suspend fun updateStudent(student: StudentEntity)

    @Delete
    suspend fun deleteStudent(student: StudentEntity)

    // --- Attendance ---
    @Query("SELECT * FROM attendance_records WHERE date = :date")
    fun getAttendanceForDate(date: String): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE date LIKE :monthPrefix || '%'")
    fun getAttendanceForMonth(monthPrefix: String): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE date LIKE :monthPrefix || '%'")
    suspend fun getAttendanceForMonthList(monthPrefix: String): List<AttendanceRecordEntity>

    @Query("SELECT * FROM attendance_records WHERE date >= :startDate AND date <= :endDate")
    fun getAttendanceBetweenDates(startDate: String, endDate: String): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE date >= :startDate AND date <= :endDate")
    suspend fun getAttendanceBetweenDatesList(startDate: String, endDate: String): List<AttendanceRecordEntity>

    @Query("SELECT * FROM attendance_records WHERE studentId = :studentId")
    fun getAttendanceForStudent(studentId: Int): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE studentId = :studentId AND date = :date LIMIT 1")
    suspend fun getAttendanceForStudentAndDate(studentId: Int, date: String): AttendanceRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttendance(record: AttendanceRecordEntity)

    // --- Exams ---
    @Query("SELECT * FROM exam_records WHERE studentId = :studentId LIMIT 1")
    fun getExamForStudent(studentId: Int): Flow<ExamRecordEntity?>

    @Query("SELECT * FROM exam_records")
    fun getAllExams(): Flow<List<ExamRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExam(record: ExamRecordEntity)

    // --- Individual Student Exams ---
    @Query("SELECT * FROM student_individual_exams WHERE studentId = :studentId ORDER BY id DESC")
    fun getIndividualExamsForStudent(studentId: Int): Flow<List<StudentIndividualExamEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIndividualExam(exam: StudentIndividualExamEntity): Long

    @Delete
    suspend fun deleteIndividualExam(exam: StudentIndividualExamEntity)

    @Query("DELETE FROM student_individual_exams WHERE id = :id")
    suspend fun deleteIndividualExamById(id: Int)

    // --- Payments ---
    @Query("SELECT * FROM payment_records WHERE monthYear LIKE :monthYear || '%'")
    fun getPaymentsForMonth(monthYear: String): Flow<List<PaymentRecordEntity>>

    @Query("SELECT * FROM payment_records")
    fun getAllPayments(): Flow<List<PaymentRecordEntity>>

    @Query("SELECT * FROM payment_records WHERE studentId = :studentId")
    fun getAllPaymentsForStudent(studentId: Int): Flow<List<PaymentRecordEntity>>

    @Query("SELECT * FROM payment_records WHERE studentId = :studentId AND monthYear = :monthYear LIMIT 1")
    suspend fun getPaymentForStudentAndMonth(studentId: Int, monthYear: String): PaymentRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPayment(record: PaymentRecordEntity)

    // --- Backup & Restore Direct Queries ---
    @Query("SELECT * FROM groups")
    suspend fun getAllGroupsList(): List<GroupEntity>

    @Query("SELECT * FROM students")
    suspend fun getAllStudentsList(): List<StudentEntity>

    @Query("SELECT * FROM attendance_records")
    suspend fun getAllAttendanceList(): List<AttendanceRecordEntity>

    @Query("SELECT * FROM exam_records")
    suspend fun getAllExamsList(): List<ExamRecordEntity>

    @Query("SELECT * FROM payment_records")
    suspend fun getAllPaymentsList(): List<PaymentRecordEntity>

    @Query("SELECT * FROM student_individual_exams")
    suspend fun getAllIndividualExamsList(): List<StudentIndividualExamEntity>

    @Query("DELETE FROM groups")
    suspend fun deleteAllGroups()

    @Query("DELETE FROM students")
    suspend fun deleteAllStudents()

    @Query("DELETE FROM attendance_records")
    suspend fun deleteAllAttendance()

    @Query("DELETE FROM exam_records")
    suspend fun deleteAllExams()

    @Query("DELETE FROM payment_records")
    suspend fun deleteAllPayments()

    @Query("DELETE FROM student_individual_exams")
    suspend fun deleteAllIndividualExams()
}
