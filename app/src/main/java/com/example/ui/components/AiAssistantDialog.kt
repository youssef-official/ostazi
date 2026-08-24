package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.AiAssistantManager
import com.example.ai.ChatMessage
import com.example.ai.MessageSender
import com.example.ai.VoiceAssistantHelper
import com.example.ui.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun AiAssistantDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val students by viewModel.allStudents.collectAsStateWithLifecycle()
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val attendanceToday by viewModel.attendanceForToday.collectAsStateWithLifecycle()
    val payments by viewModel.paymentsForMonth.collectAsStateWithLifecycle()
    val teacherName by viewModel.teacherName.collectAsStateWithLifecycle(initialValue = "")

    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isTtsAutoPlayEnabled by remember { mutableStateOf(true) }
    var voiceErrorMsg by remember { mutableStateOf<String?>(null) }

    // Voice Engine
    val voiceHelper = remember { VoiceAssistantHelper(context) }
    val isListening by voiceHelper.isListening.collectAsState()
    val isSpeaking by voiceHelper.isSpeaking.collectAsState()
    val liveRmsDb by voiceHelper.liveRmsDb.collectAsState()
    val spokenText by voiceHelper.spokenText.collectAsState()

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                sender = MessageSender.AI,
                text = "مرحباً يا أستاذي! أنا مساعدك الذكي الصوتي والكتابي \n\nتحدث معي مباشرة بالصوت وسأنفذ طلبك فوراً:\n• 'سجل حضور [اسم الطالب]'\n• 'سجل غياب [اسم الطالب]'\n• 'سجل كتب الواجب / لم يكتب'\n• 'سجل تسميع 10 من 10 لـ [اسم]'\n• 'سجل دفع مصاريف [اسم الطالب]'\n• 'كم الإيرادات والمتبقي؟'\n• 'مين غاب اليوم؟' / 'حصص اليوم'\n• 'أضف طالب جديد'\n\nاضغط على أيقونة الميكروفون بالأسفل وتحدث معي! "
            )
        )
    }

    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var selectedImageBase64 by remember { mutableStateOf<String?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                if (bytes != null) {
                    selectedImageBase64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                }
                inputStream?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendPrompt(textToSend: String) {
        if ((textToSend.isBlank() && selectedImageUri == null) || isLoading) return
        val userMsg = ChatMessage(
            sender = MessageSender.USER,
            text = textToSend,
            base64Image = selectedImageBase64,
            imageUrl = selectedImageUri?.toString()
        )
        messages.add(userMsg)
        
        val promptText = textToSend
        val imageBase64ToSend = selectedImageBase64
        
        inputText = ""
        selectedImageUri = null
        selectedImageBase64 = null
        voiceErrorMsg = null
        isLoading = true

        scope.launch {
            val response = AiAssistantManager.processQuery(
                prompt = promptText,
                base64Image = imageBase64ToSend,
                viewModel = viewModel,
                students = students,
                groups = groups,
                attendanceToday = attendanceToday,
                payments = payments,
                teacherName = teacherName,
                context = context
            )
            messages.add(response)
            isLoading = false
            listState.animateScrollToItem(messages.size - 1)

            // Auto-speak response if enabled
            if (isTtsAutoPlayEnabled) {
                voiceHelper.speak(response.text)
            }
        }
    }

    // Permission launcher for microphone
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            voiceHelper.startListening()
        } else {
            voiceErrorMsg = "يتطلب التحدث الصوتي إذن الميكروفون"
        }
    }

    // Setup speech callbacks
    DisposableEffect(Unit) {
        voiceHelper.onSpeechRecognized = { recognizedText ->
            inputText = recognizedText
            sendPrompt(recognizedText)
        }
        voiceHelper.onErrorOccurred = { error ->
            voiceErrorMsg = error
        }

        onDispose {
            voiceHelper.stopListening()
            voiceHelper.stopSpeaking()
            voiceHelper.destroy()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val quickPrompts = listOf(
        "سجل حضور طالب",
        "مين غاب اليوم؟",
        "كم الإيرادات والمتبقي؟",
        "حصص ومواعيد اليوم",
        "سجل كتب الواجب",
        "سجل دفع مصاريف",
    )

    // Pulse animation when listening
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.28f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "micPulse"
    )

    Dialog(
        onDismissRequest = {
            voiceHelper.stopListening()
            voiceHelper.stopSpeaking()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        PremiumDialogDirectionGuard()
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // 1. Top Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                voiceHelper.stopListening()
                                voiceHelper.stopSpeaking()
                                onDismiss()
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Icon(Icons.Outlined.Close, contentDescription = "إغلاق", tint = Color(0xFF64748B), modifier = Modifier.size(20.dp))
                        }

                        // Toggle TTS Voice readout
                        IconButton(
                            onClick = {
                                isTtsAutoPlayEnabled = !isTtsAutoPlayEnabled
                                if (!isTtsAutoPlayEnabled) {
                                    voiceHelper.stopSpeaking()
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .background(if (isTtsAutoPlayEnabled) Color(0xFFEDE9FE) else MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Icon(
                                if (isTtsAutoPlayEnabled) Icons.Outlined.VolumeUp else Icons.Outlined.VolumeOff,
                                contentDescription = "قراءة صوتية",
                                tint = if (isTtsAutoPlayEnabled) Color(0xFF7C3AED) else Color(0xFF94A3B8),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        if (messages.size > 1) {
                            IconButton(
                                onClick = {
                                    voiceHelper.stopSpeaking()
                                    messages.clear()
                                    messages.add(
                                        ChatMessage(
                                            sender = MessageSender.AI,
                                            text = "تم مسح المحادثة. جاهز لأوامرك الصوتية والكتابية يا أستاذي! "
                                        )
                                    )
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Outlined.DeleteSweep, contentDescription = "مسح المحادثة", tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "المساعد الصوتي الذكي",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )
                                Icon(Icons.Outlined.Mic, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(16.dp))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (isListening) Color(0xFFEF4444) else if (isSpeaking) Color(0xFF3B82F6) else Color(0xFF10B981))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isListening) "جاري الاستماع لصوتك..." else if (isSpeaking) "يتحدث الآن " else "متصل وجاهز للأوامر",
                                    fontSize = 10.5.sp,
                                    color = if (isListening) Color(0xFFDC2626) else if (isSpeaking) Color(0xFF2563EB) else Color(0xFF059669),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF7C3AED), Color(0xFF2563EB))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 2. Quick Suggestions Row
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(quickPrompts) { prompt ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, Color(0xFFE9D5FF)),
                            modifier = Modifier.clickable {
                                val cleanQuery = prompt.replace("", "").trim()
                                if (cleanQuery == "سجل حضور طالب") {
                                    sendPrompt("سجل حضور")
                                } else {
                                    sendPrompt(cleanQuery)
                                }
                            }
                        ) {
                            Text(
                                text = prompt,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7E22CE),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 3. Chat Messages List
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        ChatBubble(
                            message = msg,
                            onPlayTts = { voiceHelper.speak(msg.text) },
                            isSpeakingNow = isSpeaking
                        )
                    }

                    if (isLoading) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF7C3AED)
                                )
                                Text("جاري معالجة طلبك وتنفيذه فوراً...", fontSize = 12.sp, color = Color(0xFF64748B))
                            }
                        }
                    }
                }

                // Error alert if any
                if (voiceErrorMsg != null) {
                    Text(
                        text = voiceErrorMsg ?: "",
                        color = Color(0xFFDC2626),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 4. Live Voice Listening Indicator & Visualizer
                AnimatedVisibility(visible = isListening) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        border = BorderStroke(1.dp, Color(0xFFFECACA)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFDC2626))
                                )
                                Text(
                                    text = if (spokenText.isNotBlank()) "«$spokenText»" else "استمع إليك الآن... تحدث بأمرك",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF991B1B)
                                )
                            }

                            // Live sound bars simulation
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                                val barHeight = (10 + (liveRmsDb.coerceAtLeast(0f) * 3)).dp.coerceIn(8.dp, 28.dp)
                                Box(modifier = Modifier.width(3.dp).height(barHeight).background(Color(0xFFDC2626), CircleShape))
                                Box(modifier = Modifier.width(3.dp).height((barHeight.value * 0.7f).dp).background(Color(0xFFDC2626), CircleShape))
                                Box(modifier = Modifier.width(3.dp).height((barHeight.value * 1.2f).dp).background(Color(0xFFDC2626), CircleShape))
                                Box(modifier = Modifier.width(3.dp).height(barHeight).background(Color(0xFFDC2626), CircleShape))
                            }
                        }
                    }
                }

                // Selected Image Preview
                if (selectedImageUri != null) {
                    Box(
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    ) {
                        coil.compose.AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Selected Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        IconButton(
                            onClick = { 
                                selectedImageUri = null
                                selectedImageBase64 = null
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(24.dp)
                                .padding(2.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Outlined.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                // 5. Input Controls Row (Speech Microphone + Text Field + Send)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Big Voice Mic Button
                    Box(contentAlignment = Alignment.Center) {
                        IconButton(
                            onClick = {
                                if (isListening) {
                                    voiceHelper.stopListening()
                                } else {
                                    val hasPermission = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED

                                    if (hasPermission) {
                                        voiceHelper.startListening()
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .scale(if (isListening) pulseScale else 1f)
                                .clip(CircleShape)
                                .background(
                                    if (isListening) androidx.compose.ui.graphics.SolidColor(Color(0xFFDC2626)) else Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFF9333EA)))
                                )
                        ) {
                            Icon(
                                if (isListening) Icons.Outlined.MicOff else Icons.Outlined.Mic,
                                contentDescription = "تحدث بالصوت",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    
                    IconButton(
                        onClick = {
                            imagePickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            Icons.Outlined.Image,
                            contentDescription = "Attach Image",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("اكتب أو اضغط المايك للتحدث...", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF7C3AED),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )

                    IconButton(
                        onClick = { sendPrompt(inputText) },
                        enabled = (inputText.isNotBlank() || selectedImageUri != null) && !isLoading,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                if ((inputText.isNotBlank() || selectedImageUri != null) && !isLoading) Color(0xFF7C3AED) else MaterialTheme.colorScheme.surfaceVariant
                            )
                    ) {
                        Icon(
                            Icons.Outlined.Send,
                            contentDescription = "إرسال",
                            tint = if ((inputText.isNotBlank() || selectedImageUri != null) && !isLoading) Color.White else Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    onPlayTts: () -> Unit,
    isSpeakingNow: Boolean
) {
    val isUser = message.sender == MessageSender.USER

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF7C3AED)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.SmartToy, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            border = if (isUser) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth(0.88f)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (message.imageUrl != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        coil.compose.AsyncImage(
                            model = message.imageUrl,
                            contentDescription = "Image sent",
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = message.text,
                        fontSize = 13.sp,
                        color = if (isUser) Color.White else Color(0xFF1E293B),
                        lineHeight = 20.sp,
                        modifier = Modifier.weight(1f)
                    )

                    if (!isUser) {
                        IconButton(
                            onClick = onPlayTts,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Outlined.VolumeUp,
                                contentDescription = "قراءة الرد",
                                tint = Color(0xFF7C3AED),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                if (message.actionTaken != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFDCFCE7),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(14.dp))
                            Text(
                                text = "تم تنفيذ الإجراء وتحديث البيانات بنجاح ",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D)
                            )
                        }
                    }
                }
            }
        }
    }
}
