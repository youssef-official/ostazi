package com.example.ui.components

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.AttendanceRecordEntity
import com.example.data.ExamRecordEntity
import com.example.data.GroupEntity
import com.example.data.PaymentRecordEntity
import com.example.data.StudentEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StudentPdfReportHelper {

    fun generateAndOpenStudentMonthlyReport(
        context: Context,
        student: StudentEntity,
        group: GroupEntity?,
        attendanceList: List<AttendanceRecordEntity>,
        examRecord: ExamRecordEntity?,
        paymentRecord: PaymentRecordEntity?,
        teacherName: String,
        monthName: String,
        year: Int
    ) {
        var document: PdfDocument? = null
        try {
            document = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = document.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val paint = Paint().apply { isAntiAlias = true }

            // --- 1. Top Decorative Header Banner ---
            paint.color = android.graphics.Color.rgb(30, 58, 138) // Deep Blue
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 60f, paint)

            paint.color = android.graphics.Color.WHITE
            paint.textSize = 18f
            paint.isFakeBoldText = true
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("التقرير الإحصائي الشهري الشامل لمتابعة الطالب", pageWidth / 2f, 36f, paint)

            // Sub-header date & teacher
            paint.color = android.graphics.Color.rgb(241, 245, 249)
            paint.textSize = 10f
            paint.isFakeBoldText = false
            val currentDate = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date())
            canvas.drawText("شهر: $monthName $year   |   تاريخ الإصدار: $currentDate", pageWidth / 2f, 52f, paint)

            var y = 75f

            // --- 2. Student & Group Info Card ---
            val infoCardRect = RectF(25f, y, pageWidth - 25f, y + 68f)
            paint.color = android.graphics.Color.rgb(248, 250, 252)
            canvas.drawRoundRect(infoCardRect, 10f, 10f, paint)

            paint.color = android.graphics.Color.rgb(203, 213, 225)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            canvas.drawRoundRect(infoCardRect, 10f, 10f, paint)
            paint.style = Paint.Style.FILL

            // Info rows
            paint.textAlign = Paint.Align.RIGHT
            paint.textSize = 11f

            // Row 1
            paint.isFakeBoldText = true
            paint.color = android.graphics.Color.rgb(15, 23, 42)
            canvas.drawText("طالب: ${student.fullName}", pageWidth - 35f, y + 20f, paint)
            canvas.drawText("المجموعة: ${group?.name ?: "غير محدد"}", 320f, y + 20f, paint)
            canvas.drawText("المادة: ${group?.subject ?: "عام"}", 160f, y + 20f, paint)

            // Row 2
            paint.isFakeBoldText = false
            paint.color = android.graphics.Color.rgb(71, 85, 105)
            canvas.drawText("هاتف ولي الأمر: ${student.parentPhone.ifBlank { "-" }}", pageWidth - 35f, y + 42f, paint)
            canvas.drawText("المعلم: ${teacherName.ifBlank { "إدارة المنصة" }}", 320f, y + 42f, paint)
            val groupTypeStr = when(group?.groupType) {
                "ONLINE" -> "أونلاين"
                "PRIVATE" -> "درس خاص"
                else -> "سنتر"
            }
            canvas.drawText("نوع المجموعة: $groupTypeStr", 160f, y + 42f, paint)

            y += 80f

            // --- 3. Attendance & Performance Summary Stat Badges ---
            val totalMonthlySessions = attendanceList.size
            val attendedCount = attendanceList.count { it.attendanceStatus == "حضر" || it.attendanceStatus == "حاضر" }
            val absentCount = attendanceList.count { it.attendanceStatus == "غائب" }
            val lateCount = attendanceList.count { it.attendanceStatus == "متأخر" }
            val cancelledCount = attendanceList.count { it.attendanceStatus.contains("الغاء") || it.attendanceStatus.contains("إلغاء") || it.attendanceStatus.contains("ملغية") }
            
            val activeSessions = totalMonthlySessions - cancelledCount
            val attendancePercent = if (activeSessions > 0) (attendedCount * 100) / activeSessions else 100

            // 4 Mini Stat Boxes
            val boxWidth = (pageWidth - 50f - 24f) / 4f
            val statLabels = listOf(
                Pair("إجمالي الحصص", "$totalMonthlySessions حصص"),
                Pair("نسبة الحضور", "$attendancePercent%"),
                Pair("حضور / تأخير", "$attendedCount / $lateCount"),
                Pair("الحصص الملغية", "$cancelledCount حِصص")
            )

            for (i in 0..3) {
                val boxX = 25f + i * (boxWidth + 8f)
                val boxRect = RectF(boxX, y, boxX + boxWidth, y + 44f)

                paint.color = when (i) {
                    0 -> android.graphics.Color.rgb(238, 242, 255) // Indigo tint
                    1 -> android.graphics.Color.rgb(236, 253, 245) // Emerald tint
                    2 -> android.graphics.Color.rgb(254, 253, 232) // Yellow tint
                    else -> android.graphics.Color.rgb(254, 242, 242) // Red tint
                }
                canvas.drawRoundRect(boxRect, 8f, 8f, paint)

                paint.textAlign = Paint.Align.CENTER
                paint.color = android.graphics.Color.rgb(71, 85, 105)
                paint.textSize = 8.5f
                paint.isFakeBoldText = false
                canvas.drawText(statLabels[i].first, boxX + boxWidth / 2f, y + 16f, paint)

                paint.color = when (i) {
                    0 -> android.graphics.Color.rgb(67, 56, 202)
                    1 -> android.graphics.Color.rgb(5, 150, 105)
                    2 -> android.graphics.Color.rgb(217, 119, 6)
                    else -> android.graphics.Color.rgb(220, 38, 38)
                }
                paint.textSize = 10.5f
                paint.isFakeBoldText = true
                canvas.drawText(statLabels[i].second, boxX + boxWidth / 2f, y + 34f, paint)
            }
            
            // Extract and format month filter
            val monthIndex = when (monthName) {
                "يناير" -> 0
                "فبراير" -> 1
                "مارس" -> 2
                "أبريل" -> 3
                "مايو" -> 4
                "يونيو" -> 5
                "يوليو" -> 6
                "أغسطس" -> 7
                "سبتمبر" -> 8
                "أكتوبر" -> 9
                "نوفمبر" -> 10
                "ديسمبر" -> 11
                else -> {
                    val found = listOf(
                        "يناير" to 0, "فبراير" to 1, "مارس" to 2, "أبريل" to 3,
                        "مايو" to 4, "يونيو" to 5, "يوليو" to 6, "أغسطس" to 7,
                        "سبتمبر" to 8, "أكتوبر" to 9, "نوفمبر" to 10, "ديسمبر" to 11
                    ).find { it.first == monthName }
                    found?.second ?: java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)
                }
            }
            val monthPrefix = String.format(Locale.US, "%04d-%02d", year, monthIndex + 1)
            val filteredRecords = attendanceList.filter { it.date.startsWith(monthPrefix) }.sortedBy { it.date }
            val recordsToShow = if (filteredRecords.isNotEmpty()) filteredRecords else attendanceList.sortedBy { it.date }

            class SessionRow(
                val sessionNum: Int,
                val dateDisplay: String,
                val dayName: String,
                val attendance: String,
                val homework: String,
                val interaction: String,
                val recitation: String,
                val notes: String
            )

            val sessions = recordsToShow.mapIndexed { index, record ->
                val dateParts = record.date.split("-")
                val displayDate = if (dateParts.size == 3) "${dateParts[1]}/${dateParts[2]}" else record.date
                val arabicDay = getArabicDayName(record.date)
                
                val hwStr = when (record.homeworkStatus) {
                    "كتب", "كتب الواجب" -> "كتب ✔️"
                    "متأخر" -> "متأخر ⏰"
                    "لا يوجد" -> "لا يوجد "
                    else -> record.homeworkStatus.ifBlank { "-" }
                }
                
                val interStr = when (record.interactionStatus) {
                    "ممتاز" -> "ممتاز ⭐"
                    "جيد" -> "جيد 👍"
                    else -> record.interactionStatus.ifBlank { "-" }
                }

                val recStr = if (record.recitationGrade.isNotBlank()) {
                    record.recitationGrade
                } else {
                    "-"
                }
                
                val attStr = when (record.attendanceStatus) {
                    "حضر", "حاضر" -> "حاضر ✔️"
                    "حضر", "حاضر" -> "حاضر "
                    "متأخر" -> "متأخر ⏰"
                    "تم الغاء حصة اليوم", "ملغية", "ملغية " -> "ملغية "
                    else -> record.attendanceStatus.ifBlank { "-" }
                }

                SessionRow(
                    sessionNum = index + 1,
                    dateDisplay = displayDate,
                    dayName = arabicDay,
                    attendance = attStr,
                    homework = hwStr,
                    interaction = interStr,
                    recitation = recStr,
                    notes = record.notes
                )
            }
            
            y += 56f
 
             // --- 4. Sessions Detailed Table Header ---
             paint.textAlign = Paint.Align.RIGHT
             paint.color = android.graphics.Color.rgb(15, 23, 42)
             paint.textSize = 12f
             paint.isFakeBoldText = true
             canvas.drawText(" سجل الحصص والمتابعة اليومية:", pageWidth - 25f, y, paint)
 
             y += 8f
             val tableTop = y
             val colXSession = pageWidth - 20f
             val colXDate = pageWidth - 70f
             val colXDay = pageWidth - 140f
             val colXAtt = pageWidth - 200f
             val colXHw = pageWidth - 260f
             val colXInter = pageWidth - 315f
             val colXRec = pageWidth - 375f
             val colXNote = pageWidth - 425f
 
             // Table Header Bar
             paint.color = android.graphics.Color.rgb(30, 58, 138)
             val headerRect = RectF(25f, y, pageWidth - 25f, y + 22f)
             canvas.drawRoundRect(headerRect, 6f, 6f, paint)
 
             paint.color = android.graphics.Color.WHITE
             paint.textSize = 9.5f
             paint.textAlign = Paint.Align.RIGHT
             paint.isFakeBoldText = true
             canvas.drawText("الحصة", colXSession, y + 15f, paint)
             canvas.drawText("التاريخ", colXDate, y + 15f, paint)
             canvas.drawText("اليوم", colXDay, y + 15f, paint)
             canvas.drawText("الحضور", colXAtt, y + 15f, paint)
             canvas.drawText("الواجب", colXHw, y + 15f, paint)
             canvas.drawText("التفاعل", colXInter, y + 15f, paint)
             canvas.drawText("التسميع", colXRec, y + 15f, paint)
             canvas.drawText("ملاحظات المعلم", colXNote, y + 15f, paint)
 
             y += 24f
 
             // Table Rows
             paint.textSize = 9f
             if (sessions.isEmpty()) {
                 val rowRect = RectF(25f, y, pageWidth - 25f, y + 30f)
                 paint.color = android.graphics.Color.rgb(248, 250, 252)
                 canvas.drawRect(rowRect, paint)
                 
                 paint.color = android.graphics.Color.rgb(100, 116, 139)
                 paint.textAlign = Paint.Align.CENTER
                 paint.isFakeBoldText = false
                 canvas.drawText("لا توجد حصص مسجلة في هذا الشهر حتى الآن.", pageWidth / 2f, y + 18f, paint)
                 
                 y += 30f
             } else {
                 sessions.take(12).forEachIndexed { index, session ->
                     val rowRect = RectF(25f, y, pageWidth - 25f, y + 20f)
                     if (index % 2 == 1) {
                         paint.color = android.graphics.Color.rgb(248, 250, 252)
                         canvas.drawRect(rowRect, paint)
                     }
 
                     paint.textAlign = Paint.Align.RIGHT
                     paint.isFakeBoldText = true
                     paint.color = android.graphics.Color.rgb(30, 58, 138)
                     canvas.drawText("حصة ${session.sessionNum}", colXSession, y + 14f, paint)
 
                     paint.isFakeBoldText = false
                     paint.color = android.graphics.Color.rgb(51, 65, 85)
                     canvas.drawText(session.dateDisplay, colXDate, y + 14f, paint)
                     canvas.drawText(session.dayName, colXDay, y + 14f, paint)
 
                     // Attendance Status Color
                     paint.color = when {
                         session.attendance.contains("حاضر") || session.attendance.contains("حضر") -> android.graphics.Color.rgb(22, 163, 74)
                         session.attendance.contains("غائب") -> android.graphics.Color.rgb(220, 38, 38)
                         session.attendance.contains("متأخر") -> android.graphics.Color.rgb(217, 119, 6)
                         session.attendance.contains("ملغية") || session.attendance.contains("الغاء") || session.attendance.contains("إلغاء") -> android.graphics.Color.rgb(156, 163, 175)
                         else -> android.graphics.Color.rgb(100, 116, 139)
                     }
                     paint.isFakeBoldText = true
                     canvas.drawText(session.attendance, colXAtt, y + 14f, paint)
 
                     paint.isFakeBoldText = false
                     paint.color = android.graphics.Color.rgb(51, 65, 85)
                     canvas.drawText(session.homework, colXHw, y + 14f, paint)
                     canvas.drawText(session.interaction, colXInter, y + 14f, paint)
                     canvas.drawText(session.recitation, colXRec, y + 14f, paint)
                     
                     val shortNote = if (session.notes.length > 18) session.notes.take(16) + ".." else session.notes
                     canvas.drawText(shortNote.ifBlank { "-" }, colXNote, y + 14f, paint)
 
                     // Row bottom divider
                     paint.color = android.graphics.Color.rgb(241, 245, 249)
                     paint.strokeWidth = 0.5f
                     canvas.drawLine(25f, y + 20f, pageWidth - 25f, y + 20f, paint)
 
                     y += 20f
                 }
             }
 
             y += 10f

            // --- 5. Exam Performance Section ---
            paint.textAlign = Paint.Align.RIGHT
            paint.color = android.graphics.Color.rgb(15, 23, 42)
            paint.textSize = 12f
            paint.isFakeBoldText = true
            canvas.drawText(" نتائج وتقييمات الاختبارات:", pageWidth - 25f, y, paint)

            y += 8f
            val examCardRect = RectF(25f, y, pageWidth - 25f, y + 74f)
            paint.color = android.graphics.Color.rgb(255, 251, 235) // Amber background
            canvas.drawRoundRect(examCardRect, 8f, 8f, paint)

            paint.color = android.graphics.Color.rgb(253, 230, 138)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            canvas.drawRoundRect(examCardRect, 8f, 8f, paint)
            paint.style = Paint.Style.FILL

            val e1 = examRecord?.exam1?.ifBlank { null } ?: "لم يُؤدَ"
            val e2 = examRecord?.exam2?.ifBlank { null } ?: "لم يُؤدَ"
            val e3 = examRecord?.exam3?.ifBlank { null } ?: "لم يُؤدَ"
            val e4 = examRecord?.exam4?.ifBlank { null } ?: "لم يُؤدَ"
            val e5 = examRecord?.exam5?.ifBlank { null } ?: "لم يُؤدَ"

            paint.textSize = 10f
            paint.isFakeBoldText = true
            paint.color = android.graphics.Color.rgb(180, 83, 9)
            canvas.drawText("الاختبار الأول: $e1", pageWidth - 35f, y + 22f, paint)
            canvas.drawText("الاختبار الثاني: $e2", 370f, y + 22f, paint)
            canvas.drawText("الاختبار الثالث: $e3", 200f, y + 22f, paint)
            
            canvas.drawText("الاختبار الرابع: $e4", pageWidth - 35f, y + 42f, paint)
            canvas.drawText("الاختبار الخامس (النهائي): $e5", 370f, y + 42f, paint)

            paint.textSize = 9f
            paint.isFakeBoldText = false
            paint.color = android.graphics.Color.rgb(113, 63, 18)
            val overallNote = "ملاحظة التقييم: يتم تقييم مستوى الطالب بناء على الحضور اليومي والامتحانات الدورية والواجبات."
            canvas.drawText(overallNote, pageWidth - 35f, y + 64f, paint)

            y += 86f

            // --- 6. Financial / Subscription Status Section ---
            paint.textAlign = Paint.Align.RIGHT
            paint.color = android.graphics.Color.rgb(15, 23, 42)
            paint.textSize = 12f
            paint.isFakeBoldText = true
            canvas.drawText(" الموقف المالي والاشتراك الشهري:", pageWidth - 25f, y, paint)

            y += 8f
            val fee = group?.monthlyFee ?: 0.0
            val pStatus = paymentRecord?.paymentStatus ?: "UNPAID"
            val pDate = paymentRecord?.paymentDate ?: "-"
            val pRem = paymentRecord?.remainingAmount ?: 0.0

            val finCardRect = RectF(25f, y, pageWidth - 25f, y + 46f)
            paint.color = when (pStatus) {
                "PAID" -> android.graphics.Color.rgb(240, 253, 244)
                "PARTIAL" -> android.graphics.Color.rgb(255, 251, 235)
                "EXEMPT" -> android.graphics.Color.rgb(250, 245, 255)
                else -> android.graphics.Color.rgb(254, 242, 242)
            }
            canvas.drawRoundRect(finCardRect, 8f, 8f, paint)

            paint.textSize = 10f
            paint.isFakeBoldText = true
            paint.color = android.graphics.Color.rgb(15, 23, 42)
            canvas.drawText("قيمة الاشتراك الشهري: ${fee.toInt()} ج.م", pageWidth - 35f, y + 18f, paint)

            val statusText = when (pStatus) {
                "PAID" -> "حالة السداد: تم الدفع بالكامل  (بتاريخ: $pDate)"
                "PARTIAL" -> "حالة السداد: دفع جزئي (متبقي ${pRem.toInt()} ج.م)"
                "EXEMPT" -> "حالة السداد: طالب معفي"
                else -> "حالة السداد: لم يتم الدفع"
            }
            paint.color = when (pStatus) {
                "PAID" -> android.graphics.Color.rgb(22, 163, 74)
                "PARTIAL" -> android.graphics.Color.rgb(217, 119, 6)
                "EXEMPT" -> android.graphics.Color.rgb(147, 51, 234)
                else -> android.graphics.Color.rgb(220, 38, 38)
            }
            canvas.drawText(statusText, pageWidth - 35f, y + 36f, paint)

            y += 58f

            // --- 7. Bottom Signatures & Footer ---
            paint.color = android.graphics.Color.rgb(203, 213, 225)
            paint.strokeWidth = 1f
            canvas.drawLine(25f, y, pageWidth - 25f, y, paint)

            y += 20f
            paint.color = android.graphics.Color.rgb(71, 85, 105)
            paint.textSize = 10f
            paint.isFakeBoldText = true
            canvas.drawText("توقيع المعلم / المشرف: ____________________", pageWidth - 35f, y, paint)
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("خاتم وإعتماد الإدارة: ____________________", 35f, y, paint)

            y += 22f
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = 8.5f
            paint.isFakeBoldText = false
            paint.color = android.graphics.Color.rgb(148, 163, 184)
            canvas.drawText("تم إنشاء هذا التقرير تلقائياً عبر تطبيق إدارة الدروس والمجموعات • جميع الحقوق محفوظة", pageWidth / 2f, y, paint)

            document.finishPage(page)

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val sanitizedStudentName = student.fullName.replace(Regex("[^a-zA-Z0-9_\\u0600-\\u06FF]"), "_")
            val fileName = "StudentReport_${sanitizedStudentName}_$timestamp.pdf"

            val pdfDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.cacheDir
            if (!pdfDir.exists()) pdfDir.mkdirs()
            val file = File(pdfDir, fileName)

            FileOutputStream(file).use { out ->
                document.writeTo(out)
            }
            document.close()
            document = null

            // Copy to MediaStore on Android Q+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    val resolver = context.contentResolver
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { outputStream ->
                            file.inputStream().use { inputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            Toast.makeText(context, "تم حفظ تقرير PDF للطالب بنجاح ", Toast.LENGTH_SHORT).show()

            // Open or share the PDF file safely using FileProvider
            try {
                val contentUri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(contentUri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(intent, "عرض تقرير الطالب PDF"))
            } catch (_: Exception) {
                Toast.makeText(context, "تم الحفظ في: ${file.name}", Toast.LENGTH_LONG).show()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "خطأ في استخراج PDF: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            try {
                document?.close()
            } catch (_: Exception) {}
        }
    }

    private fun getArabicDayName(dateStr: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = sdf.parse(dateStr)
            val dayFormat = SimpleDateFormat("EEEE", Locale("ar"))
            dayFormat.format(date)
        } catch (e: Exception) {
            "-"
        }
    }
}
