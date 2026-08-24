package com.example.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.GroupEntity
import com.example.data.StudentEntity
import com.example.data.TeacherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExcelManager {

    /**
     * Exports students list to Excel CSV format with UTF-8 BOM so Microsoft Excel
     * and Google Sheets open Arabic text with perfect encoding.
     */
    suspend fun exportStudentsToExcel(
        context: Context,
        students: List<StudentEntity>,
        groups: List<GroupEntity>
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val groupMap = groups.associateBy { it.id }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
            val fileName = "Students_Export_$timeStamp.csv"
            val file = File(context.cacheDir, fileName)

            val stringBuilder = StringBuilder()
            // Write UTF-8 BOM for Microsoft Excel
            stringBuilder.append("\uFEFF")

            // Excel CSV Header
            stringBuilder.append("م,اسم الطالب,المجموعة,المادة,رقم ولي الأمر,رقم هاتف الطالب,قيمة الاشتراك\n")

            students.forEachIndexed { index, student ->
                val group = groupMap[student.groupId]
                val groupName = group?.name ?: "بدون مجموعة"
                val subject = group?.subject ?: "-"
                val fee = group?.monthlyFee ?: 0.0

                val safeName = escapeCsv(student.fullName)
                val safeGroup = escapeCsv(groupName)
                val safeSubject = escapeCsv(subject)
                val safeParentPhone = escapeCsv(student.parentPhone)
                val safeStudentPhone = escapeCsv(student.studentPhone)

                stringBuilder.append("${index + 1},$safeName,$safeGroup,$safeSubject,$safeParentPhone,$safeStudentPhone,$fee\n")
            }

            file.writeText(stringBuilder.toString(), Charsets.UTF_8)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            // Share Intent
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/comma-separated-values"
                putExtra(Intent.EXTRA_SUBJECT, "تصدير بيانات الطلاب - Excel")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(sendIntent, "فتح أو مشاركة ملف Excel").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)

            Pair(true, "تم تصدير ملف الإكسل بنجاح (${students.size} طالب) 📊")
        } catch (e: Exception) {
            Pair(false, "حدث خطأ أثناء تصدير الإكسل: ${e.localizedMessage}")
        }
    }

    /**
     * Imports students from CSV or Excel exported CSV file.
     */
    suspend fun importStudentsFromCsv(
        context: Context,
        uri: Uri,
        repository: TeacherRepository
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Pair(false, "تعذر قراءة الملف المختار")

            val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
            val lines = reader.readLines()
            reader.close()
            inputStream.close()

            if (lines.isEmpty()) {
                return@withContext Pair(false, "ملف الإكسل فارغ")
            }

            val existingGroups = repository.getAllGroupsList()
            var defaultGroup = existingGroups.firstOrNull()
            if (defaultGroup == null) {
                val newGroupId = repository.insertGroup(
                    GroupEntity(
                        name = "مجموعة عامة",
                        subject = "عام",
                        day1 = "السبت",
                        timeSlot = "08:00 ص",
                        monthlyFee = 150.0
                    )
                )
                defaultGroup = GroupEntity(
                    id = newGroupId.toInt(),
                    name = "مجموعة عامة",
                    subject = "عام",
                    day1 = "السبت",
                    timeSlot = "08:00 ص",
                    monthlyFee = 150.0
                )
            }

            var importedCount = 0
            var skippedCount = 0

            // Skip header if line 0 contains header terms
            val startIndex = if (lines[0].contains("اسم") || lines[0].contains("الطالب") || lines[0].contains("Name")) 1 else 0

            for (i in startIndex until lines.size) {
                val line = lines[i].trim()
                if (line.isBlank()) continue

                // Support comma and semicolon delimiters
                val parts = parseCsvLine(line)
                if (parts.isEmpty()) continue

                var name = ""
                var groupName = ""
                var parentPhone = ""
                var studentPhone = ""

                if (parts.size == 1) {
                    name = parts[0]
                } else if (parts.size == 2) {
                    name = parts[0]
                    parentPhone = parts[1]
                } else if (parts.size >= 3) {
                    // Could be [Index, Name, Group, ...] or [Name, Group, ParentPhone, ...]
                    val firstIsNum = parts[0].toIntOrNull() != null
                    if (firstIsNum && parts.size >= 4) {
                        name = parts[1]
                        groupName = parts[2]
                        parentPhone = parts.getOrNull(4) ?: parts.getOrNull(3) ?: ""
                        studentPhone = parts.getOrNull(5) ?: ""
                    } else {
                        name = parts[0]
                        groupName = parts[1]
                        parentPhone = parts.getOrNull(2) ?: ""
                        studentPhone = parts.getOrNull(3) ?: ""
                    }
                }

                if (name.isBlank()) {
                    skippedCount++
                    continue
                }

                // Determine Group ID
                var targetGroupId = defaultGroup.id
                if (groupName.isNotBlank()) {
                    val matchedGroup = existingGroups.find { it.name.trim().equals(groupName.trim(), ignoreCase = true) }
                    if (matchedGroup != null) {
                        targetGroupId = matchedGroup.id
                    } else {
                        // Create new group for this student
                        val newGId = repository.insertGroup(
                            GroupEntity(
                                name = groupName.trim(),
                                subject = "عام",
                                day1 = "السبت",
                                timeSlot = "08:00 ص",
                                monthlyFee = 150.0
                            )
                        )
                        targetGroupId = newGId.toInt()
                    }
                }

                repository.insertStudent(
                    StudentEntity(
                        id = 0,
                        fullName = name.trim(),
                        groupId = targetGroupId,
                        parentPhone = parentPhone.trim(),
                        studentPhone = studentPhone.trim()
                    )
                )
                importedCount++
            }

            if (importedCount > 0) {
                Pair(true, "تم استيراد $importedCount طالب بنجاح من ملف الإكسل! 🎉")
            } else {
                Pair(false, "لم يتم العثور على بيانات طلاب صالحة للاستيراد في الملف")
            }
        } catch (e: Exception) {
            Pair(false, "فشل استيراد الملف: ${e.localizedMessage}")
        }
    }

    private fun escapeCsv(text: String): String {
        var clean = text.replace("\"", "\"\"")
        if (clean.contains(",") || clean.contains("\n") || clean.contains("\"") || clean.contains(";")) {
            clean = "\"$clean\""
        }
        return clean
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val delimiter = if (line.contains(";") && !line.contains(",")) ';' else ','
        var inQuotes = false
        val currentField = StringBuilder()

        for (ch in line) {
            when {
                ch == '\"' -> inQuotes = !inQuotes
                ch == delimiter && !inQuotes -> {
                    result.add(currentField.toString().trim())
                    currentField.clear()
                }
                else -> currentField.append(ch)
            }
        }
        result.add(currentField.toString().trim())
        return result
    }
}
