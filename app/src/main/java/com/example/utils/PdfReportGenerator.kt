package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.AttendanceRecordEntity
import com.example.data.GroupEntity
import com.example.data.PaymentRecordEntity
import com.example.data.StudentEntity
import com.example.ui.screens.SessionDateItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    /**
     * Generates a printable A4 PDF for Group Monthly Attendance with multi-page support and clean column layout.
     */
    fun generateGroupAttendancePdf(
        context: Context,
        group: GroupEntity,
        students: List<StudentEntity>,
        sessionDates: List<SessionDateItem>,
        attendanceRecords: List<AttendanceRecordEntity>,
        periodSubtitle: String
    ): File? {
        return try {
            val pdfDoc = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842

            val maxDatesToShow = sessionDates.take(14)
            val numDates = maxDatesToShow.size

            val colNumWidth = 22f
            val colNameWidth = 110f
            val colNoteWidth = 70f
            // Cap date column width between 28f and 36f to prevent ugly stretching
            val dateColWidth = if (numDates > 0) ((pageWidth - 40f - colNumWidth - colNameWidth - colNoteWidth) / numDates).coerceIn(24f, 32f) else 32f
            val tableActualWidth = colNumWidth + colNameWidth + colNoteWidth + (numDates * dateColWidth)

            // Center table horizontally on page
            val tableLeft = ((pageWidth - tableActualWidth) / 2f).coerceAtLeast(20f)
            val tableRight = tableLeft + tableActualWidth

            val studentsPerPageFirst = 20
            val studentsPerPageLater = 26
            val studentChunks = if (students.isEmpty()) {
                listOf(emptyList())
            } else {
                val chunks = mutableListOf<List<StudentEntity>>()
                var idx = 0
                while (idx < students.size) {
                    val limit = if (chunks.isEmpty()) studentsPerPageFirst else studentsPerPageLater
                    val nextIdx = (idx + limit).coerceAtMost(students.size)
                    chunks.add(students.subList(idx, nextIdx))
                    idx = nextIdx
                }
                chunks
            }

            val totalPages = studentChunks.size
            var studentNumberOffset = 1

            for (pageIndex in studentChunks.indices) {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
                val page = pdfDoc.startPage(pageInfo)
                val canvas = page.canvas
                canvas.drawColor(Color.WHITE)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)

                var currentY = 32f

                if (pageIndex == 0) {
                    // --- 1. Top Header Card (Dark Indigo Purple) ---
                    val headerTop = 32f
                    val headerBottom = 118f
                    val headerRect = RectF(tableLeft, headerTop, tableRight, headerBottom)

                    paint.color = Color.parseColor("#1E1035") // Deep Purple
                    canvas.drawRoundRect(headerRect, 14f, 14f, paint)

                    // Subtitle top inside header
                    paint.color = Color.parseColor("#B3A5D4")
                    paint.textSize = 9.5f
                    paint.textAlign = Paint.Align.CENTER
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    canvas.drawText("تقرير شهري للمجموعات", (pageWidth / 2).toFloat(), headerTop + 22f, paint)

                    // Title: كشف حضور [Group Name]
                    paint.color = Color.WHITE
                    paint.textSize = 16f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("كشف حضور ${group.name}", (pageWidth / 2).toFloat(), headerTop + 48f, paint)

                    // Details line
                    val detailsText = "${group.subject} • ${students.size} طالب • ${sessionDates.size} أيام مسجلة ($periodSubtitle)"
                    paint.color = Color.parseColor("#DDD6FE")
                    paint.textSize = 9.5f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    canvas.drawText(detailsText, (pageWidth / 2).toFloat(), headerTop + 68f, paint)

                    currentY = 132f
                } else {
                    // Compact header for subsequent pages
                    val headerRect = RectF(tableLeft, 30f, tableRight, 62f)
                    paint.color = Color.parseColor("#1E1035")
                    canvas.drawRoundRect(headerRect, 8f, 8f, paint)

                    paint.color = Color.WHITE
                    paint.textSize = 12f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    paint.textAlign = Paint.Align.CENTER
                    canvas.drawText("كشف حضور ${group.name} ($periodSubtitle) - تابع", (pageWidth / 2).toFloat(), 50f, paint)

                    currentY = 74f
                }

                // --- 2. Table Header ---
                val headerRowHeight = 32f
                val headerRowRect = RectF(tableLeft, currentY, tableRight, currentY + headerRowHeight)
                paint.color = Color.parseColor("#EDE9FE")
                canvas.drawRoundRect(headerRowRect, 6f, 6f, paint)

                paint.color = Color.parseColor("#4C1D95")
                paint.textSize = 9f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textAlign = Paint.Align.CENTER

                // # (Number col)
                val numColX = tableRight - (colNumWidth / 2f)
                canvas.drawText("#", numColX, currentY + 20f, paint)

                // Student Name col
                val nameColX = tableRight - colNumWidth - (colNameWidth / 2f)
                canvas.drawText("اسم الطالب", nameColX, currentY + 20f, paint)

                // Notes col
                val noteColX = tableLeft + (colNoteWidth / 2f)
                canvas.drawText("الملاحظات", noteColX, currentY + 20f, paint)

                // Date columns (right to left)
                for (i in maxDatesToShow.indices) {
                    val dateItem = maxDatesToShow[i]
                    val colX = tableRight - colNumWidth - colNameWidth - (i * dateColWidth) - (dateColWidth / 2f)
                    
                    val shortDate = try {
                        val parts = dateItem.dateString.split("-")
                        if (parts.size == 3) "${parts[1]}/${parts[2]}" else dateItem.displayDate
                    } catch (e: Exception) {
                        dateItem.displayDate
                    }
                    
                    paint.textSize = 8f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    paint.color = Color.parseColor("#3730A3")
                    canvas.drawText(shortDate, colX, currentY + 14f, paint)

                    paint.textSize = 7f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    paint.color = Color.parseColor("#6B7280")
                    canvas.drawText(dateItem.dayName, colX, currentY + 25f, paint)
                }

                currentY += headerRowHeight

                // --- 3. Table Rows ---
                val rowHeight = 22f
                val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#E5E7EB")
                    strokeWidth = 0.8f
                }

                val pageStudents = studentChunks[pageIndex]
                if (pageStudents.isEmpty()) {
                    paint.color = Color.parseColor("#6B7280")
                    paint.textSize = 11f
                    paint.textAlign = Paint.Align.CENTER
                    canvas.drawText("لا يوجد طلاب مسجلين في هذه المجموعة", (pageWidth / 2).toFloat(), currentY + 30f, paint)
                } else {
                    pageStudents.forEachIndexed { sIdx, student ->
                        val currentNum = studentNumberOffset + sIdx
                        val rowRect = RectF(tableLeft, currentY, tableRight, currentY + rowHeight)
                        
                        if (sIdx % 2 == 1) {
                            paint.color = Color.parseColor("#F9FAFB")
                            canvas.drawRect(rowRect, paint)
                        }

                        // Row bottom divider
                        canvas.drawLine(tableLeft, currentY + rowHeight, tableRight, currentY + rowHeight, linePaint)

                        // Student Number
                        paint.color = Color.parseColor("#374151")
                        paint.textSize = 8.5f
                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        paint.textAlign = Paint.Align.CENTER
                        canvas.drawText("$currentNum", numColX, currentY + 15f, paint)

                        // Student Name
                        paint.color = Color.parseColor("#111827")
                        paint.textSize = 8.5f
                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                        paint.textAlign = Paint.Align.RIGHT
                        val nameDrawX = tableRight - colNumWidth - 6f
                        
                        var displayName = student.fullName.trim()
                        if (displayName.length > 20) {
                            displayName = displayName.take(18) + ".."
                        }
                        canvas.drawText(displayName, nameDrawX, currentY + 15f, paint)

                        // Notes
                        paint.textAlign = Paint.Align.LEFT
                        paint.color = Color.parseColor("#64748B")
                        paint.textSize = 7f
                        val studentRecords = attendanceRecords.filter { it.studentId == student.id }
                        val latestNote = studentRecords.sortedByDescending { it.date }.firstOrNull { it.notes.isNotBlank() }?.notes ?: ""
                        var noteToDraw = latestNote
                        if (noteToDraw.length > 18) noteToDraw = noteToDraw.take(15) + "..."
                        canvas.drawText(noteToDraw, tableLeft + 4f, currentY + 14f, paint)

                        // Attendance badges per date
                        for (dIdx in maxDatesToShow.indices) {
                            val dateItem = maxDatesToShow[dIdx]
                            val colX = tableRight - colNumWidth - colNameWidth - (dIdx * dateColWidth) - (dateColWidth / 2f)

                            val record = attendanceRecords.find { 
                                it.studentId == student.id && (it.date == dateItem.dateString || it.date.trim() == dateItem.dateString.trim())
                            }
                            val status = record?.attendanceStatus?.trim() ?: ""

                            val badgeW = (dateColWidth - 4f).coerceAtMost(30f).coerceAtLeast(18f)
                            val badgeH = 14f
                            val badgeRect = RectF(colX - (badgeW / 2), currentY + 4f, colX + (badgeW / 2), currentY + 4f + badgeH)

                            paint.textAlign = Paint.Align.CENTER
                            when (status) {
                                "PRESENT", "حاضر", "حضر" -> {
                                    paint.color = Color.parseColor("#DCFCE7")
                                    canvas.drawRoundRect(badgeRect, 3f, 3f, paint)
                                    paint.color = Color.parseColor("#15803D")
                                    paint.textSize = 7f
                                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                                    canvas.drawText("حاضر", colX, currentY + 14f, paint)
                                }
                                "LATE", "متأخر" -> {
                                    paint.color = Color.parseColor("#FEF3C7")
                                    canvas.drawRoundRect(badgeRect, 3f, 3f, paint)
                                    paint.color = Color.parseColor("#B45309")
                                    paint.textSize = 7f
                                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                                    canvas.drawText("متأخر", colX, currentY + 14f, paint)
                                }
                                "ABSENT", "غائب", "غ" -> {
                                    paint.color = Color.parseColor("#FEE2E2")
                                    canvas.drawRoundRect(badgeRect, 3f, 3f, paint)
                                    paint.color = Color.parseColor("#B91C1C")
                                    paint.textSize = 7f
                                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                                    canvas.drawText("غائب", colX, currentY + 14f, paint)
                                }
                                "EXCUSED", "مستأذن", "بعذر" -> {
                                    paint.color = Color.parseColor("#EFF6FF")
                                    canvas.drawRoundRect(badgeRect, 3f, 3f, paint)
                                    paint.color = Color.parseColor("#1D4ED8")
                                    paint.textSize = 7f
                                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                                    canvas.drawText("بعذر", colX, currentY + 14f, paint)
                                }
                                else -> {
                                    if (status.contains("الغاء") || status.contains("إلغاء") || status.contains("ملغية")) {
                                        paint.color = Color.parseColor("#F3F4F6")
                                        canvas.drawRoundRect(badgeRect, 3f, 3f, paint)
                                        paint.color = Color.parseColor("#4B5563")
                                        paint.textSize = 6.5f
                                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                                        canvas.drawText("ملغية", colX, currentY + 13f, paint)
                                    } else {
                                        paint.color = Color.parseColor("#9CA3AF")
                                        paint.textSize = 8f
                                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                                        canvas.drawText("—", colX, currentY + 14f, paint)
                                    }
                                }
                            }
                        }

                        currentY += rowHeight
                    }
                    studentNumberOffset += pageStudents.size
                }

                // --- 4. Legend & Footer ---
                val footerY = (pageHeight - 42).toFloat()
                paint.textSize = 8f
                paint.textAlign = Paint.Align.CENTER
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                val fullLegend = "حاضر (أخضر)     متأخر (برتقالي)     غائب (أحمر)     بعذر (أزرق)     — لم يسجل"
                paint.color = Color.parseColor("#4B5563")
                canvas.drawText(fullLegend, (pageWidth / 2).toFloat(), footerY, paint)

                paint.color = Color.parseColor("#9CA3AF")
                paint.textSize = 7.5f
                paint.textAlign = Paint.Align.LEFT
                canvas.drawText("صفحة ${pageIndex + 1} من $totalPages", tableLeft, footerY + 18f, paint)

                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText("أستاذي • تقرير حضور جاهز للطباعة", tableRight, footerY + 18f, paint)

                pdfDoc.finishPage(page)
            }

            // Save to internal cache
            val safeSubtitle = periodSubtitle.replace(" ", "_").replace("/", "-").replace(":", "-")
            val fileName = "تقرير_حضور_${group.name.replace(" ", "_")}_${safeSubtitle}.pdf"
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { out ->
                pdfDoc.writeTo(out)
            }
            pdfDoc.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generates a printable A4 PDF for Financial Records (السجل المالي) matching Image 2.
     */
    fun generateFinancePdf(
        context: Context,
        selectedGroupName: String,
        monthName: String,
        year: Int,
        students: List<StudentEntity>,
        groups: List<GroupEntity>,
        paymentsList: List<PaymentRecordEntity>,
        totalCollected: Double,
        totalExpected: Double,
        totalRemaining: Double,
        unpaidCount: Int
    ): File? {
        return try {
            val pdfDoc = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDoc.startPage(pageInfo)
            val canvas = page.canvas

            canvas.drawColor(Color.WHITE)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            // --- 1. Top Header Card (Dark Slate Blue) ---
            val headerTop = 36f
            val headerLeft = 36f
            val headerRight = (pageWidth - 36).toFloat()
            val headerBottom = 120f
            val headerRect = RectF(headerLeft, headerTop, headerRight, headerBottom)

            paint.color = Color.parseColor("#0F172A") // Dark Slate
            canvas.drawRoundRect(headerRect, 16f, 16f, paint)

            paint.color = Color.parseColor("#94A3B8")
            paint.textSize = 10f
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("أستاذي • التقرير المالي الشامل", (pageWidth / 2).toFloat(), headerTop + 24f, paint)

            paint.color = Color.WHITE
            paint.textSize = 18f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("السجل المالي والتحصيل", (pageWidth / 2).toFloat(), headerTop + 48f, paint)

            paint.color = Color.parseColor("#E2E8F0")
            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val subText = "$selectedGroupName • $monthName $year • إجمالي ${students.size} طالب"
            canvas.drawText(subText, (pageWidth / 2).toFloat(), headerTop + 70f, paint)

            // --- 2. 4 Stat Cards in 2x2 or 4x1 row (matching Image 2) ---
            val statsTop = 132f
            val cardGap = 8f
            val totalStatsWidth = headerRight - headerLeft
            val cardWidth = (totalStatsWidth - (cardGap * 3)) / 4f
            val cardHeight = 44f

            val statCards = listOf(
                Triple("المحصل", "${totalCollected.toInt()} ج.م", "#059669"),
                Triple("إجمالي المتوقع", "${totalExpected.toInt()} ج.م", "#2563EB"),
                Triple("المتبقي", "${totalRemaining.toInt()} ج.م", "#D97706"),
                Triple("عليهم مستحقات", "$unpaidCount طالب", "#DC2626")
            )

            for (i in statCards.indices) {
                val item = statCards[i]
                val cLeft = headerLeft + i * (cardWidth + cardGap)
                val cRight = cLeft + cardWidth
                val cRect = RectF(cLeft, statsTop, cRight, statsTop + cardHeight)

                paint.color = Color.parseColor("#F8FAFC")
                canvas.drawRoundRect(cRect, 8f, 8f, paint)

                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1f
                paint.color = Color.parseColor("#E2E8F0")
                canvas.drawRoundRect(cRect, 8f, 8f, paint)
                paint.style = Paint.Style.FILL

                // Label
                paint.color = Color.parseColor("#64748B")
                paint.textSize = 7.5f
                paint.textAlign = Paint.Align.CENTER
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText(item.first, (cLeft + cRight) / 2f, statsTop + 15f, paint)

                // Value
                paint.color = Color.parseColor(item.third)
                paint.textSize = 10f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(item.second, (cLeft + cRight) / 2f, statsTop + 33f, paint)
            }

            // --- 3. Financial Records Table ---
            val tableTop = 190f
            val tableLeft = 36f
            val tableRight = (pageWidth - 36).toFloat()
            val tableWidth = tableRight - tableLeft

            val colNumW = 22f
            val colNameW = 120f
            val colGroupW = 85f
            val colFeeW = 60f
            val colPaidW = 60f
            val colRemW = 60f
            val colStatusW = 65f
            val colDateW = tableWidth - (colNumW + colNameW + colGroupW + colFeeW + colPaidW + colRemW + colStatusW)

            // Table Header Bar
            val headerRowHeight = 28f
            val tableHeaderRect = RectF(tableLeft, tableTop, tableRight, tableTop + headerRowHeight)
            paint.color = Color.parseColor("#F1F5F9")
            canvas.drawRoundRect(tableHeaderRect, 6f, 6f, paint)

            paint.color = Color.parseColor("#334155")
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textAlign = Paint.Align.CENTER

            var colX = tableRight
            val headers = listOf(
                Pair("#", colNumW),
                Pair("اسم الطالب", colNameW),
                Pair("المجموعة", colGroupW),
                Pair("الاشتراك", colFeeW),
                Pair("المدفوع", colPaidW),
                Pair("المتبقي", colRemW),
                Pair("الحالة", colStatusW),
                Pair("تاريخ الدفع", colDateW)
            )

            headers.forEach { (title, width) ->
                val center = colX - (width / 2)
                canvas.drawText(title, center, tableTop + 18f, paint)
                colX -= width
            }

            // Table Rows
            val rowHeight = 20f
            var currentY = tableTop + headerRowHeight

            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#F1F5F9")
                strokeWidth = 1f
            }

            students.forEachIndexed { index, student ->
                val grp = groups.find { it.id == student.groupId }
                val fee = grp?.monthlyFee ?: 0.0
                val p = paymentsList.find { it.studentId == student.id }
                val status = p?.paymentStatus ?: "UNPAID"
                val remaining = when (status) {
                    "PAID" -> 0.0
                    "PARTIAL" -> p?.remainingAmount ?: 0.0
                    "EXEMPT" -> 0.0
                    else -> fee
                }
                val paid = when (status) {
                    "PAID" -> fee
                    "PARTIAL" -> (fee - remaining).coerceAtLeast(0.0)
                    "EXEMPT" -> 0.0
                    else -> 0.0
                }

                if (index % 2 == 1) {
                    paint.color = Color.parseColor("#F8FAFC")
                    canvas.drawRect(RectF(tableLeft, currentY, tableRight, currentY + rowHeight), paint)
                }

                canvas.drawLine(tableLeft, currentY + rowHeight, tableRight, currentY + rowHeight, linePaint)

                var cX = tableRight

                // 1. #
                paint.color = Color.parseColor("#475569")
                paint.textSize = 8.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText("${index + 1}", cX - (colNumW / 2), currentY + 14f, paint)
                cX -= colNumW

                // 2. Name
                paint.color = Color.parseColor("#0F172A")
                paint.textAlign = Paint.Align.RIGHT
                var sName = student.fullName
                if (sName.length > 20) sName = sName.take(18) + ".."
                canvas.drawText(sName, cX - 6f, currentY + 14f, paint)
                cX -= colNameW

                // 3. Group
                paint.color = Color.parseColor("#475569")
                paint.textAlign = Paint.Align.CENTER
                val gName = grp?.name?.take(14) ?: "-"
                canvas.drawText(gName, cX - (colGroupW / 2), currentY + 14f, paint)
                cX -= colGroupW

                // 4. Fee
                paint.color = Color.parseColor("#1E293B")
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText("${fee.toInt()} ج.م", cX - (colFeeW / 2), currentY + 14f, paint)
                cX -= colFeeW

                // 5. Paid
                paint.color = Color.parseColor("#059669")
                canvas.drawText("${paid.toInt()} ج.م", cX - (colPaidW / 2), currentY + 14f, paint)
                cX -= colPaidW

                // 6. Remaining
                paint.color = if (remaining > 0) Color.parseColor("#DC2626") else Color.parseColor("#64748B")
                canvas.drawText("${remaining.toInt()} ج.م", cX - (colRemW / 2), currentY + 14f, paint)
                cX -= colRemW

                // 7. Status
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 8f
                when (status) {
                    "PAID" -> {
                        paint.color = Color.parseColor("#059669")
                        canvas.drawText("مدفوع كامل", cX - (colStatusW / 2), currentY + 14f, paint)
                    }
                    "PARTIAL" -> {
                        paint.color = Color.parseColor("#D97706")
                        canvas.drawText("دفع جزئي", cX - (colStatusW / 2), currentY + 14f, paint)
                    }
                    "EXEMPT" -> {
                        paint.color = Color.parseColor("#7C3AED")
                        canvas.drawText("معفي", cX - (colStatusW / 2), currentY + 14f, paint)
                    }
                    else -> {
                        paint.color = Color.parseColor("#DC2626")
                        canvas.drawText("غير مسدد", cX - (colStatusW / 2), currentY + 14f, paint)
                    }
                }
                cX -= colStatusW

                // 8. Payment Date
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.textSize = 8f
                paint.color = Color.parseColor("#64748B")
                val pDate = p?.paymentDate?.ifEmpty { null } ?: "-"
                canvas.drawText(pDate, cX - (colDateW / 2), currentY + 14f, paint)

                currentY += rowHeight
            }

            // --- 4. Footer & Signature ---
            val footerY = (pageHeight - 50).toFloat()

            paint.color = Color.parseColor("#94A3B8")
            paint.textSize = 7.5f
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("صفحة 1 من 1", tableLeft, footerY + 20f, paint)

            paint.textAlign = Paint.Align.RIGHT
            val genDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ENGLISH).format(Date())
            canvas.drawText("أستاذي • تم التصدير بتاريخ $genDate", tableRight, footerY + 20f, paint)

            pdfDoc.finishPage(page)

            val fileName = "السجل_المالي_${selectedGroupName.replace(" ", "_")}_${monthName}_${year}.pdf"
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { out ->
                pdfDoc.writeTo(out)
            }
            pdfDoc.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Share PDF file via WhatsApp / Share Sheet.
     */
    fun sharePdf(context: Context, file: File, title: String) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, title))
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر مشاركة الملف: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Open PDF with any installed PDF viewer (Google Drive, Adobe, etc.).
     */
    fun openPdf(context: Context, file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            }
            context.startActivity(Intent.createChooser(intent, "عرض ملف PDF"))
        } catch (e: Exception) {
            Toast.makeText(context, "لا يوجد قارئ PDF مثبت على الجهاز", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Print PDF using Android PrintManager.
     */
    fun printPdf(context: Context, file: File, jobName: String) {
        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (printManager != null) {
                val printAdapter = object : PrintDocumentAdapter() {
                    override fun onLayout(
                        oldAttributes: PrintAttributes?,
                        newAttributes: PrintAttributes?,
                        cancellationSignal: android.os.CancellationSignal?,
                        callback: LayoutResultCallback?,
                        extras: android.os.Bundle?
                    ) {
                        if (cancellationSignal?.isCanceled == true) {
                            callback?.onLayoutCancelled()
                            return
                        }
                        val info = android.print.PrintDocumentInfo.Builder(jobName)
                            .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                            .setPageCount(1)
                            .build()
                        callback?.onLayoutFinished(info, true)
                    }

                    override fun onWrite(
                        pages: Array<out android.print.PageRange>?,
                        destination: android.os.ParcelFileDescriptor?,
                        cancellationSignal: android.os.CancellationSignal?,
                        callback: WriteResultCallback?
                    ) {
                        try {
                            val input = java.io.FileInputStream(file)
                            val output = java.io.FileOutputStream(destination?.fileDescriptor)
                            input.copyTo(output)
                            input.close()
                            output.close()
                            callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                        } catch (e: Exception) {
                            callback?.onWriteFailed(e.message)
                        }
                    }
                }
                printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
            }
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر بدء الطباعة: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
