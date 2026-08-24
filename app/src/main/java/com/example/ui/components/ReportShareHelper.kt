package com.example.ui.components

import com.example.ui.MainViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

enum class ReportChannel(
    val title: String,
    val subtitle: String,
    val brandColor: Color,
    val iconVector: ImageVector
) {
    WHATSAPP(
        title = "واتساب عادي",
        subtitle = "محادثة مباشرة لولي الأمر عبر WhatsApp",
        brandColor = Color(0xFF25D366),
        iconVector = Icons.Outlined.Chat
    ),
    WHATSAPP_BUSINESS(
        title = "واتساب الأعمال",
        subtitle = "محادثة مباشرة عبر WhatsApp Business",
        brandColor = Color(0xFF075E54),
        iconVector = Icons.Outlined.BusinessCenter
    ),
    TELEGRAM(
        title = "تليجرام",
        subtitle = "إرسال تقرير ومحادثة عبر Telegram",
        brandColor = Color(0xFF0088CC),
        iconVector = Icons.Outlined.Send
    ),
    SMS(
        title = "رسالة نصية SMS",
        subtitle = "إرسال رسالة قصيرة للهاتف مباشرة",
        brandColor = Color(0xFFEA580C),
        iconVector = Icons.Outlined.Sms
    ),
    MESSENGER(
        title = "ماسينجر (Messenger)",
        subtitle = "إرسال ومحادثة عبر Facebook Messenger",
        brandColor = Color(0xFF0084FF),
        iconVector = Icons.Outlined.Forum
    )
}

object ReportSender {
    fun cleanPhone(phone: String): String {
        return phone.replace(" ", "").replace("-", "").replace("+", "")
    }

    fun formatPhoneForWhatsApp(phone: String): String {
        val clean = cleanPhone(phone)
        return when {
            clean.isEmpty() -> ""
            clean.startsWith("20") -> clean
            clean.startsWith("01") -> "2$clean"
            clean.startsWith("0") -> "2$clean"
            else -> "20$clean"
        }
    }

    fun send(context: Context, channel: ReportChannel, phone: String, message: String) {
        val cleanP = cleanPhone(phone)
        val formattedWaPhone = formatPhoneForWhatsApp(phone)

        when (channel) {
            ReportChannel.WHATSAPP -> {
                if (formattedWaPhone.isNotBlank()) {
                    val url = "https://api.whatsapp.com/send?phone=$formattedWaPhone&text=${Uri.encode(message)}"
                    val uri = Uri.parse(url)
                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                        setPackage("com.whatsapp")
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        val fallbackIntent = Intent(Intent.ACTION_VIEW, uri)
                        try {
                            context.startActivity(fallbackIntent)
                        } catch (e2: Exception) {
                            Toast.makeText(context, "تعذر فتح تطبيق واتساب", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, message)
                        setPackage("com.whatsapp")
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        val chooser = Intent.createChooser(intent, "مشاركة التقرير عبر واتساب")
                        context.startActivity(chooser)
                    }
                }
            }

            ReportChannel.WHATSAPP_BUSINESS -> {
                if (formattedWaPhone.isNotBlank()) {
                    val url = "https://api.whatsapp.com/send?phone=$formattedWaPhone&text=${Uri.encode(message)}"
                    val uri = Uri.parse(url)
                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                        setPackage("com.whatsapp.w4b")
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        val fallbackIntent = Intent(Intent.ACTION_VIEW, uri)
                        try {
                            context.startActivity(fallbackIntent)
                        } catch (e2: Exception) {
                            Toast.makeText(context, "تطبيق واتساب الأعمال غير مثبت", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, message)
                        setPackage("com.whatsapp.w4b")
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        val chooser = Intent.createChooser(intent, "مشاركة التقرير عبر واتساب للأعمال")
                        context.startActivity(chooser)
                    }
                }
            }

            ReportChannel.TELEGRAM -> {
                // Copy text to clipboard so it can be pasted easily in Telegram chat
                copyToClipboard(context, message, "تقرير الطالب")
                val uri = if (formattedWaPhone.isNotBlank()) {
                    Uri.parse("https://t.me/+$formattedWaPhone")
                } else {
                    Uri.parse("https://t.me/share/url?url=&text=${Uri.encode(message)}")
                }
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage("org.telegram.messenger")
                }
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    val fallbackIntent = Intent(Intent.ACTION_VIEW, uri)
                    try {
                        context.startActivity(fallbackIntent)
                    } catch (e2: Exception) {
                        Toast.makeText(context, "تطبيق تليجرام غير مثبت", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            ReportChannel.SMS -> {
                if (cleanP.isBlank()) {
                    Toast.makeText(context, "لا يوجد رقم هاتف مسجل للطالب", Toast.LENGTH_SHORT).show()
                    return
                }
                val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("smsto:$cleanP")
                    putExtra("sms_body", message)
                }
                try {
                    context.startActivity(smsIntent)
                } catch (e: Exception) {
                    Toast.makeText(context, "تعذر فتح تطبيق الرسائل القصيرة", Toast.LENGTH_SHORT).show()
                }
            }

            ReportChannel.MESSENGER -> {
                copyToClipboard(context, message, "تقرير الطالب")
                val messengerIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, message)
                    setPackage("com.facebook.orca")
                }
                try {
                    context.startActivity(messengerIntent)
                } catch (e: Exception) {
                    val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://m.me/"))
                    try {
                        context.startActivity(fallbackIntent)
                    } catch (e2: Exception) {
                        shareGeneral(context, message)
                    }
                }
            }
        }
    }

    fun shareGeneral(context: Context, message: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        val shareIntent = Intent.createChooser(sendIntent, "مشاركة التقرير عبر...")
        try {
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح قائمة المشاركة", Toast.LENGTH_SHORT).show()
        }
    }

    fun copyToClipboard(context: Context, message: String, title: String = "تقرير الطالب") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(title, message)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "تم نسخ التقرير إلى الحافظة بنجاح ", Toast.LENGTH_SHORT).show()
    }

    fun openWhatsAppGroupLink(context: Context, groupUrl: String, message: String = "", targetPackage: String? = null) {
        if (message.isNotBlank()) {
            copyToClipboard(context, message, "تقرير المجموعة")
        }
        val cleanUrl = groupUrl.trim()
        if (cleanUrl.isNotBlank()) {
            val uri = if (cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")) {
                Uri.parse(cleanUrl)
            } else {
                Uri.parse("https://$cleanUrl")
            }
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                if (!targetPackage.isNullOrBlank()) {
                    setPackage(targetPackage)
                }
            }
            try {
                context.startActivity(intent)
                if (message.isNotBlank()) {
                    Toast.makeText(context, "تم نسخ تقرير المجموعة لحافظة الهاتف لتقوم بلصقه داخل الجروب ", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "جاري فتح رابط جروب الواتساب... ", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                val fallbackIntent = Intent(Intent.ACTION_VIEW, uri)
                try {
                    context.startActivity(fallbackIntent)
                } catch (e2: Exception) {
                    Toast.makeText(context, "تعذر فتح رابط الجروب المسجل، يرجى التأكد من صحة الرابط", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            // No link registered yet: open general WhatsApp share
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                if (!targetPackage.isNullOrBlank()) {
                    setPackage(targetPackage)
                }
            }
            try {
                context.startActivity(sendIntent)
                Toast.makeText(context, "لم تقم بتسجيل رابط الجروب لهذه المجموعة بعد. جاري فتح تطبيق الواتساب اختيار الجروب... ", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                shareGeneral(context, message)
            }
        }
    }
}

fun insertNoteBeforeTeacher(message: String, note: String): String {
    if (note.isBlank()) return message
    val noteBlock = "\n*ملاحظة:* $note\n"
    val targets = listOf("\nمع تحيات", "\n*مع تحيات", "\nمع أطيب", "\n*مع أطيب")
    for (target in targets) {
        val idx = message.indexOf(target)
        if (idx != -1) {
            return message.substring(0, idx) + noteBlock + message.substring(idx)
        }
    }
    return message + noteBlock
}

@Composable
fun GroupWhatsAppChannelDialog(
    groupName: String,
    reportMessage: String,
    groupUrl: String,
    onSaveGroupUrl: ((String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var editableMessage by remember { mutableStateOf(reportMessage) }
    var noteText by remember { mutableStateOf("") }
    var currentUrl by remember { mutableStateOf(groupUrl) }
    var showUrlEditField by remember { mutableStateOf(groupUrl.isBlank()) }

    Dialog(onDismissRequest = onDismiss) {
        PremiumDialogDirectionGuard()
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .72f)),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Outlined.Close, contentDescription = "إغلاق", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "إرسال تقرير إلى جروب الواتساب ",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "المجموعة: $groupName",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Editable Message Section (Styled cleanly like a real messaging app)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "نص الرسالة (يمكنك التعديل بحرية):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${editableMessage.length} حرف",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = editableMessage,
                        onValueChange = { editableMessage = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 12.sp,
                            textAlign = TextAlign.End,
                            textDirection = androidx.compose.ui.text.style.TextDirection.Rtl,
                            lineHeight = 18.sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedBorderColor = Color(0xFF2563EB),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Write note field
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { newNote ->
                        noteText = newNote
                        editableMessage = insertNoteBeforeTeacher(reportMessage, newNote)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("كتابة ملاحظة إضافية للتقرير ", fontSize = 12.sp) },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 12.sp,
                        textAlign = TextAlign.End,
                        textDirection = androidx.compose.ui.text.style.TextDirection.Rtl
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF16A34A),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // WhatsApp Group Invite Link Section
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (currentUrl.isNotBlank()) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
                    border = BorderStroke(1.dp, if (currentUrl.isNotBlank()) Color(0xFF86EFAC) else Color(0xFFFDE68A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { showUrlEditField = !showUrlEditField },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (showUrlEditField) "إخفاء" else "تعديل رابط الجروب", fontSize = 10.5.sp)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (currentUrl.isNotBlank()) "رابط الجروب مسجل ومربوط " else "يرجى إضافة رابط الجروب للفتح الفوري ",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (currentUrl.isNotBlank()) Color(0xFF166534) else Color(0xFF92400E)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (currentUrl.isNotBlank()) Icons.Outlined.Link else Icons.Outlined.LinkOff,
                                    contentDescription = null,
                                    tint = if (currentUrl.isNotBlank()) Color(0xFF166534) else Color(0xFF92400E),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        if (showUrlEditField) {
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = currentUrl,
                                onValueChange = {
                                    currentUrl = it
                                    onSaveGroupUrl?.invoke(it)
                                },
                                placeholder = { Text("https://chat.whatsapp.com/...", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                            Text(
                                text = " نصيحة: انسخ رابط الدعوة من إعدادات الجروب في واتساب والصقه هنا لتنتقل للجروب فوراً بضغطة واحدة.",
                                fontSize = 9.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // WhatsApp App Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Option 1: WhatsApp
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.2.dp, Color(0xFF25D366)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                ReportSender.openWhatsAppGroupLink(context, currentUrl, editableMessage, "com.whatsapp")
                                onDismiss()
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.ArrowBackIosNew, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("فتح جروب واتساب العادي ", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text("ينتقل مباشرة للجروب وينسخ التقرير للصقه", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Box(
                                    modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF25D366).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Outlined.Chat, contentDescription = null, tint = Color(0xFF25D366), modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }

                    // Option 2: WhatsApp Business
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.2.dp, Color(0xFF075E54)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                ReportSender.openWhatsAppGroupLink(context, currentUrl, editableMessage, "com.whatsapp.w4b")
                                onDismiss()
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.ArrowBackIosNew, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("فتح جروب واتساب الأعمال ", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text("ينتقل مباشرة للجروب داخل تطبيق WhatsApp Business", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Box(
                                    modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF075E54).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Outlined.Business, contentDescription = null, tint = Color(0xFF075E54), modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            ReportSender.copyToClipboard(context, editableMessage, "تقرير $groupName")
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("نسخ الرسالة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            ReportSender.shareGeneral(context, editableMessage)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A))
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مشاركة لتطبيقات أخرى", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ReportChannelSelectionDialog(
    recipientName: String,
    phoneNumber: String,
    reportMessage: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var editableMessage by remember { mutableStateOf(reportMessage) }
    var noteText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        PremiumDialogDirectionGuard()
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .72f)),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Outlined.Close, contentDescription = "إغلاق", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "تعديل وإرسال التقرير ",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "إلى: $recipientName",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (phoneNumber.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            text = " $phoneNumber",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = editableMessage,
                    onValueChange = { editableMessage = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 12.sp,
                        textAlign = TextAlign.End,
                        textDirection = androidx.compose.ui.text.style.TextDirection.Rtl
                    ),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2563EB),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Write note field
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { newNote ->
                        noteText = newNote
                        editableMessage = insertNoteBeforeTeacher(reportMessage, newNote)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("كتابة ملاحظة إضافية للتقرير ", fontSize = 12.sp) },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 12.sp,
                        textAlign = TextAlign.End,
                        textDirection = androidx.compose.ui.text.style.TextDirection.Rtl
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF16A34A),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Channels List
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReportChannel.values().forEach { channel ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    ReportSender.send(context, channel, phoneNumber, editableMessage)
                                    onDismiss()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ArrowBackIosNew,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = channel.title,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = channel.subtitle,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(channel.brandColor.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = channel.iconVector,
                                            contentDescription = null,
                                            tint = channel.brandColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom actions: Share General & Copy
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            ReportSender.copyToClipboard(context, reportMessage, "تقرير $recipientName")
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                    ) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("نسخ النص", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            ReportSender.shareGeneral(context, reportMessage)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A))
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مشاركة أخرى", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ManualReportDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val savedReport by viewModel.savedManualReport.collectAsState()
    var editableMessage by remember(savedReport) { mutableStateOf(savedReport) }
    var targetPhone by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        PremiumDialogDirectionGuard()
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .72f)),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Outlined.Close, contentDescription = "إغلاق", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "تقرير يدوي مخصص ",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "اكتب واحفظ التقرير للإرسال السريع",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Report text
                OutlinedTextField(
                    value = editableMessage,
                    onValueChange = { editableMessage = it },
                    placeholder = { Text("اكتب نص التقرير اليدوي هنا...", fontSize = 12.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 12.sp,
                        textAlign = TextAlign.End,
                        textDirection = androidx.compose.ui.text.style.TextDirection.Rtl
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Save button
                Button(
                    onClick = {
                        viewModel.saveManualReport(editableMessage)
                        Toast.makeText(context, "تم حفظ نص التقرير بنجاح! ", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                ) {
                    Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("حفظ التقرير في الذاكرة ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // Optional phone input
                OutlinedTextField(
                    value = targetPhone,
                    onValueChange = { targetPhone = it },
                    label = { Text("رقم هاتف المستلم (اختياري) ", fontSize = 12.sp) },
                    placeholder = { Text("01xxxxxxxxx", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 12.sp,
                        textAlign = TextAlign.End
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Send Channels Title
                Text(
                    text = "إرسال ومشاركة عبر:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.align(Alignment.End)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Channels List
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ReportChannel.values().forEach { channel ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    ReportSender.send(context, channel, targetPhone, editableMessage)
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ArrowBackIosNew,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(12.dp)
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = channel.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(channel.brandColor.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = channel.iconVector,
                                            contentDescription = null,
                                            tint = channel.brandColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
