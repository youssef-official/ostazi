package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object CertificateGenerator {

    /**
     * Generates a high-definition appreciation certificate Bitmap matching the uploaded design
     */
    fun createCertificateBitmap(
        context: Context,
        studentName: String,
        teacherName: String,
        subjectOrExamName: String,
        scoreText: String = "",
        dateText: String,
        customPraise: String = ""
    ): Bitmap {
        val width = 1600
        val height = 1100
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val navy = Color.rgb(9, 31, 68)
        val royalBlue = Color.rgb(35, 83, 155)
        val gold = Color.rgb(194, 143, 42)
        val ivory = Color.rgb(250, 246, 236)
        val ink = Color.rgb(44, 57, 76)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }

        // A clean print-first paper with a restrained double-line frame.
        canvas.drawColor(ivory)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 24f
        paint.color = navy
        canvas.drawRect(28f, 28f, width - 28f, height - 28f, paint)
        paint.strokeWidth = 8f
        paint.color = gold
        canvas.drawRect(52f, 52f, width - 52f, height - 52f, paint)
        paint.strokeWidth = 2f
        paint.color = navy
        canvas.drawRect(74f, 74f, width - 74f, height - 74f, paint)

        // Minimal corner ornaments and a centered achievement seal.
        paint.style = Paint.Style.FILL
        paint.color = gold
        listOf(
            RectF(92f, 92f, 180f, 180f), RectF(width - 180f, 92f, width - 92f, 180f),
            RectF(92f, height - 180f, 180f, height - 92f), RectF(width - 180f, height - 180f, width - 92f, height - 92f)
        ).forEach { rect ->
            canvas.drawCircle(rect.centerX(), rect.centerY(), 22f, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f
            canvas.drawCircle(rect.centerX(), rect.centerY(), 31f, paint)
            paint.style = Paint.Style.FILL
        }
        canvas.drawCircle(width / 2f, 124f, 55f, paint)
        paint.color = ivory
        canvas.drawCircle(width / 2f, 124f, 43f, paint)
        paint.color = navy
        canvas.drawCircle(width / 2f, 124f, 8f, paint)
        canvas.drawRect(width / 2f - 5f, 124f, width / 2f + 5f, 158f, paint)

        drawCenteredFit(canvas, "شهادة تقدير", width / 2f, 272f, width * .78f, 100f, navy, true)
        drawCenteredFit(canvas, "تُمنح هذه الشهادة بكل فخر واعتزاز إلى", width / 2f, 356f, width * .75f, 40f, ink, false)
        drawCenteredFit(canvas, teacherName.ifBlank { "الأستاذ / المعلم" }, width / 2f, 438f, width * .68f, 62f, gold, true)
        paint.color = gold
        paint.strokeWidth = 3f
        canvas.drawLine(width * .32f, 472f, width * .68f, 472f, paint)
        drawCenteredFit(canvas, "الطالب / الطالبة المتميز(ة)", width / 2f, 538f, width * .75f, 38f, ink, false)
        drawCenteredFit(canvas, studentName, width / 2f, 638f, width * .78f, 78f, royalBlue, true)
        drawCenteredFit(canvas, "تقديراً لتميزه وأدائه المشرف في مادة $subjectOrExamName", width / 2f, 720f, width * .8f, 34f, ink, false)
        if (customPraise.isNotBlank()) {
            drawCenteredFit(canvas, customPraise, width / 2f, 774f, width * .8f, 30f, ink, false)
        }
        if (scoreText.isNotBlank()) {
            drawCenteredFit(canvas, "الدرجة: $scoreText", width / 2f, 828f, width * .5f, 34f, gold, true)
        }

        paint.color = Color.rgb(100, 116, 139)
        paint.textSize = 24f
        paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("تاريخ الإصدار: $dateText", 150f, 970f, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("توقيع المعلم: _________________", width - 150f, 970f, paint)
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 20f
        paint.color = navy
        canvas.drawText("أستاذي  •  منصة إدارة المعلم", width / 2f, 1030f, paint)
        return bitmap
    }

    private fun drawCenteredFit(
        canvas: Canvas,
        text: String,
        centerX: Float,
        baseline: Float,
        maxWidth: Float,
        initialSize: Float,
        color: Int,
        bold: Boolean
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
            textSize = initialSize
        }
        while (paint.measureText(text) > maxWidth && paint.textSize > 18f) {
            paint.textSize *= .92f
        }
        canvas.drawText(text, centerX, baseline, paint)
    }

    /**
     * Saves the certificate and initiates a sharing intent.
     */
    fun saveBitmapToCache(context: Context, bitmap: Bitmap, filename: String): Uri? {
        return try {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, filename)
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()
            FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun shareCertificateImage(context: Context, imageUri: Uri, phoneNumber: String, caption: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            putExtra(Intent.EXTRA_TEXT, caption)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            
            if (phoneNumber.isNotBlank()) {
                setPackage("com.whatsapp")
                val cleanPhone = phoneNumber.replace(Regex("[^0-9]"), "")
                val formattedPhone = if (cleanPhone.startsWith("0")) "2$cleanPhone" else cleanPhone
                putExtra("jid", "$formattedPhone@s.whatsapp.net")
            }
        }

        try {
            if (phoneNumber.isNotBlank()) {
                context.startActivity(shareIntent)
            } else {
                context.startActivity(Intent.createChooser(shareIntent, "مشاركة شهادة التقدير"))
            }
        } catch (e: Exception) {
            shareIntent.setPackage(null)
            context.startActivity(Intent.createChooser(shareIntent, "مشاركة شهادة التقدير"))
        }
    }
}
