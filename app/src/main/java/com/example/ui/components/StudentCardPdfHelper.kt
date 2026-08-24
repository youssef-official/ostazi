package com.example.ui.components

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
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
import com.example.data.GroupEntity
import com.example.data.StudentEntity
import com.example.utils.BarcodeUtils
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StudentCardPdfHelper {

    fun generateBatchStudentCardsPdf(
        context: Context,
        students: List<StudentEntity>,
        groups: List<GroupEntity>,
        teacherName: String,
        teacherSubject: String,
        groupNameFilter: String = "جميع الطلاب"
    ) {
        if (students.isEmpty()) {
            Toast.makeText(context, "لا يوجد طلاب لإنشاء الكروت لهم", Toast.LENGTH_SHORT).show()
            return
        }

        var document: PdfDocument? = null
        try {
            document = PdfDocument()
            val pageWidth = 595 // A4 width in points
            val pageHeight = 842 // A4 height in points
            
            // Card dimensions (standard credit card size approx in points)
            val cardWidth = 260f
            val cardHeight = 160f
            val margin = 25f
            val spacing = 20f
            
            val cardsPerRow = 2
            val cardsPerPage = 8 // 4 rows of 2 cards
            
            var currentStudentIndex = 0
            var pageNum = 1
            
            while (currentStudentIndex < students.size) {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                val page = document.startPage(pageInfo)
                val canvas: Canvas = page.canvas
                val paint = Paint().apply { isAntiAlias = true }
                
                // Draw Title
                paint.color = android.graphics.Color.BLACK
                paint.textSize = 14f
                paint.isFakeBoldText = true
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText("كروت تعريف الطلاب - $groupNameFilter (صفحة $pageNum)", pageWidth / 2f, 40f, paint)
                
                for (i in 0 until cardsPerPage) {
                    if (currentStudentIndex >= students.size) break
                    
                    val student = students[currentStudentIndex]
                    val group = groups.find { it.id == student.groupId }
                    
                    val row = i / cardsPerRow
                    val col = i % cardsPerRow
                    
                    val startX = margin + col * (cardWidth + spacing)
                    val startY = 60f + row * (cardHeight + spacing)
                    
                    drawStudentCard(canvas, paint, student, group, teacherName, teacherSubject, startX, startY, cardWidth, cardHeight)
                    
                    currentStudentIndex++
                }
                
                document.finishPage(page)
                pageNum++
            }
            
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "StudentCards_${groupNameFilter.replace(" ", "_")}_$timestamp.pdf"
            
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

            Toast.makeText(context, "تم حفظ ملف كروت الطلاب بنجاح ", Toast.LENGTH_SHORT).show()

            // Open or share the PDF file
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
                context.startActivity(Intent.createChooser(intent, "عرض ملف الكروت PDF"))
            } catch (_: Exception) {
                Toast.makeText(context, "تم الحفظ في: ${file.name}", Toast.LENGTH_LONG).show()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "خطأ في إنشاء ملف الكروت: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            try {
                document?.close()
            } catch (_: Exception) {}
        }
    }

    private fun drawStudentCard(
        canvas: Canvas,
        paint: Paint,
        student: StudentEntity,
        group: GroupEntity?,
        teacherName: String,
        teacherSubject: String,
        x: Float,
        y: Float,
        width: Float,
        height: Float
    ) {
        val navy = android.graphics.Color.rgb(10, 31, 68)
        val blue = android.graphics.Color.rgb(35, 83, 155)
        val gold = android.graphics.Color.rgb(211, 158, 54)
        val paper = android.graphics.Color.rgb(248, 250, 253)
        val ink = android.graphics.Color.rgb(19, 34, 56)

        // Premium cool-paper card
        val cardRect = RectF(x, y, x + width, y + height)
        paint.color = paper
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(cardRect, 12f, 12f, paint)

        paint.color = navy
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.8f
        canvas.drawRoundRect(cardRect, 12f, 12f, paint)

        // Branded header and gold accent
        paint.style = Paint.Style.FILL
        paint.color = navy
        canvas.drawRoundRect(RectF(x + 1f, y + 1f, x + width - 1f, y + 43f), 11f, 11f, paint)
        paint.color = gold
        canvas.drawRect(x + 1f, y + 39f, x + width - 1f, y + 43f, paint)

        paint.color = android.graphics.Color.WHITE
        paint.textSize = 11f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("بطاقة الطالب الذكية", x + width / 2f, y + 27f, paint)

        // QR framed in brand blue
        val qrSize = 66f
        val qrX = x + 16f
        val qrY = y + 57f
        paint.color = android.graphics.Color.rgb(231, 239, 251)
        canvas.drawRoundRect(RectF(qrX - 5f, qrY - 5f, qrX + qrSize + 5f, qrY + qrSize + 5f), 9f, 9f, paint)
        paint.color = blue
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(RectF(qrX - 5f, qrY - 5f, qrX + qrSize + 5f, qrY + qrSize + 5f), 9f, 9f, paint)
        paint.style = Paint.Style.FILL
        val qrBitmap = BarcodeUtils.generateQrCodeBitmap("STUDENT_${student.id}", 140, 140)
        if (qrBitmap != null) {
            val qrRect = RectF(qrX, qrY, qrX + qrSize, qrY + qrSize)
            canvas.drawBitmap(qrBitmap, null, qrRect, null)
        }
        
        // Student information with clear hierarchy
        paint.color = ink
        paint.textAlign = Paint.Align.RIGHT
        paint.textSize = 10.5f
        paint.isFakeBoldText = true
        canvas.drawText(student.fullName, x + width - 15f, y + 66f, paint)
        
        paint.isFakeBoldText = false
        paint.textSize = 8f
        paint.color = android.graphics.Color.rgb(72, 89, 111)
        canvas.drawText("كود الطالب: ${student.id}", x + width - 15f, y + 83f, paint)
        canvas.drawText("المجموعة: ${group?.name ?: "غير محدد"}", x + width - 15f, y + 99f, paint)
        canvas.drawText("المعلم: $teacherName", x + width - 15f, y + 115f, paint)
        canvas.drawText("المادة: $teacherSubject", x + width - 15f, y + 131f, paint)

        paint.color = gold
        paint.strokeWidth = 0.5f
        canvas.drawLine(x + 14f, y + height - 18f, x + width - 14f, y + height - 18f, paint)
        
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 6f
        paint.color = navy
        canvas.drawText("امسح الكود لتسجيل الحضور والوصول لبيانات الطالب", x + width / 2f, y + height - 7f, paint)
    }
}
