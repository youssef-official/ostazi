import sys

new_content = """@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupCardItem(
    group: GroupEntity,
    students: List<StudentEntity>,
    attendanceList: List<com.example.data.AttendanceRecordEntity>,
    teacherName: String,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStudentClick: (StudentEntity) -> Unit,
    onAttendanceChange: (studentId: Int, attendance: String, homework: String, recitation: String) -> Unit,
    onOpenGroupWhatsAppReports: () -> Unit = {},
    onScanBarcodeClick: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val totalIncome = students.size * group.monthlyFee.toInt()

    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color(0xFFF3F4F6))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Left: PDF Icon + Active Badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF3F4F6))
                            .clickable { onOpenGroupWhatsAppReports() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "PDF", tint = Color(0xFF4B5563), modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFECFDF5)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF10B981)))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("نشطة", color = Color(0xFF065F46), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                // Right: Icon + Title + Subject
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = group.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = group.subject, fontSize = 14.sp, color = Color(0xFF6B7280))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Box(modifier = Modifier.size(52.dp).background(Color(0xFFE0F2FE), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Groups, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(28.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2x2 Stats Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "السعة / النوع",
                    value = if (group.groupType == "ONLINE") "أونلاين" else if (group.groupType == "PRIVATE") "برايفت" else "سنتر",
                    icon = Icons.Default.PersonAddAlt1,
                    iconBgColor = Color(0xFFFFFBEB),
                    iconColor = Color(0xFFD97706)
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "الطلاب المسجلين",
                    value = "${students.size} طالب",
                    icon = Icons.Default.People,
                    iconBgColor = Color(0xFFEFF6FF),
                    iconColor = Color(0xFF2563EB)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "الدخل المحصل",
                    value = "$totalIncome ج.م",
                    icon = Icons.Default.AccountBalanceWallet,
                    iconBgColor = Color(0xFFFAF5FF),
                    iconColor = Color(0xFF9333EA)
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "سعر الاشتراك",
                    value = "${group.monthlyFee.toInt()} ج.م / شهر",
                    icon = Icons.Default.Payments,
                    iconBgColor = Color(0xFFF0FDF4),
                    iconColor = Color(0xFF16A34A)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Quick Actions Title
            Text("إجراءات سريعة", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
            Spacer(modifier = Modifier.height(18.dp))
            
            // Quick Actions Buttons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                QuickActionItem(title = "تعديل", icon = Icons.Default.Edit, bgColor = Color(0xFFFEF2F2), fgColor = Color(0xFFDC2626), onClick = onEdit)
                QuickActionItem(title = "إضافة طالب", icon = Icons.Default.PersonAddAlt1, bgColor = Color(0xFFFFFBEB), fgColor = Color(0xFFD97706), onClick = { expanded = !expanded })
                QuickActionItem(title = "تسجيل غياب", icon = Icons.Default.Checklist, bgColor = Color(0xFFEFF6FF), fgColor = Color(0xFF2563EB), onClick = { expanded = !expanded })
                QuickActionItem(title = "تسجيل سريع", icon = Icons.Default.QrCodeScanner, bgColor = Color(0xFFF0FDF4), fgColor = Color(0xFF16A34A), onClick = onScanBarcodeClick)
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Schedule Title
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row {
                    IconButton(onClick = onMoveUp, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up", tint = Color(0xFF9CA3AF))
                    }
                    IconButton(onClick = onMoveDown, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down", tint = Color(0xFF9CA3AF))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFDC2626))
                    }
                }
                Text("مواعيد الحصص", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
            }
            Spacer(modifier = Modifier.height(14.dp))

            val days = listOf(
                group.day1 to group.timeSlot,
                group.day2 to group.timeSlot2,
                group.day3 to group.timeSlot3,
                group.day4 to group.timeSlot4,
                group.day5 to group.timeSlot5,
                group.day6 to group.timeSlot6,
                group.day7 to group.timeSlot7
            ).filter { !it.first.isNullOrBlank() && it.first != "بدون" }.distinctBy { it.first }

            if (days.isEmpty()) {
                Text("مواعيد الحصص غير محددة", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
            } else {
                days.forEach { (day, time) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).background(Color(0xFFF9FAFB), RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Event, contentDescription = null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(24.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = Color(0xFFDBEAFE), shape = RoundedCornerShape(10.dp)) {
                                Text(time ?: "غير محدد", modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp), color = Color(0xFF1E40AF), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(18.dp))
                            Text(day ?: "", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF111827))
                        }
                    }
                }
            }
            
            // Expanded Student List for Group Attendance
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 18.dp)) {
                    HorizontalDivider(color = Color(0xFFE5E7EB))
                    Spacer(modifier = Modifier.height(14.dp))

                    if (students.isEmpty()) {
                        Text(
                            text = "لا يوجد طلاب مسجلين في هذه المجموعة حتى الآن.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9CA3AF),
                            modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                    } else {
                        students.forEach { student ->
                            val attRecord = attendanceList.find { it.studentId == student.id }
                            val attStatus = attRecord?.attendanceStatus ?: "حضر"
                            val hwStatus = attRecord?.homeworkStatus ?: "كتب الواجب"
                            val recStatus = attRecord?.recitationStatus ?: "ممتاز"

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                                border = BorderStroke(1.dp, Color(0xFFF3F4F6))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = student.fullName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF111827),
                                            modifier = Modifier
                                                .clickable { onStudentClick(student) }
                                                .padding(vertical = 4.dp)
                                        )

                                        // Quick WhatsApp Button
                                        IconButton(
                                            onClick = {
                                                val formattedDate = java.text.SimpleDateFormat("EEEE, d MMMM yyyy", java.util.Locale("ar")).format(java.util.Date())
                                                val msg = buildString {
                                                    append("✨ *تقرير المتابعة اليومي* ✨\\n\\n")
                                                    append("السلام عليكم ورحمة الله وبركاته 🌺\\n")
                                                    append("ولي أمر الطالب/ة المحترم: *${student.fullName}*\\n")
                                                    append("🗓️ *التاريخ:* $formattedDate\\n\\n")
                                                    append("• *حالة الحضور:* $attStatus\\n")
                                                    append("• *أداء الواجب:* $hwStatus\\n")
                                                    append("• *التسميع/الحفظ:* $recStatus\\n\\n")
                                                    append("مع أطيب التحيات والتقدير 🌸\\n")
                                                    if (teacherName.isNotBlank()) {
                                                        append("أ/ $teacherName")
                                                    }
                                                }
                                                val phone = student.parentPhone.replace(" ", "").replace("-", "")
                                                val formattedPhone = if (phone.startsWith("0")) "2$phone" else phone
                                                val uri = android.net.Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=${android.net.Uri.encode(msg)}")
                                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                                                try { context.startActivity(intent) } catch (e: Exception) { }
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "واتساب", tint = Color(0xFF25D366), modifier = Modifier.size(20.dp))
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Attendance Toggles
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("الاطمئنان:", style = MaterialTheme.typography.labelMedium, color = Color(0xFF6B7280))
                                        listOf("حضر", "غائب", "متأخر").forEach { st ->
                                            val isSel = attStatus == st
                                            val bg = when {
                                                isSel && st == "حضر" -> StatusPresent
                                                isSel && st == "غائب" -> StatusAbsent
                                                isSel && st == "متأخر" -> StatusLate
                                                else -> Color(0xFFF3F4F6)
                                            }
                                            val fg = if (isSel) Color.White else Color(0xFF4B5563)

                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(bg)
                                                    .clickable { onAttendanceChange(student.id, st, hwStatus, recStatus) }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = when(st) {
                                                        "حضر" -> "حضر ✔️"
                                                        "غائب" -> "غائب ❌"
                                                        else -> "متأخر ⏰"
                                                    },
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = fg
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
    }
}

@Composable
fun StatCard(modifier: Modifier = Modifier, title: String, value: String, icon: ImageVector, iconBgColor: Color, iconColor: Color) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFF3F4F6))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(iconBgColor), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = title, tint = iconColor, modifier = Modifier.size(24.dp))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(title, fontSize = 12.sp, color = Color(0xFF6B7280), fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(2.dp))
                Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
            }
        }
    }
}

@Composable
fun QuickActionItem(title: String, icon: ImageVector, bgColor: Color, fgColor: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick).padding(4.dp)) {
        Box(
            modifier = Modifier.size(56.dp).clip(CircleShape).background(bgColor).border(1.dp, fgColor.copy(alpha=0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = fgColor, modifier = Modifier.size(26.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF374151))
    }
}
"""

with open("app/src/main/java/com/example/ui/screens/GroupsScreen.kt", "r") as f:
    content = f.read()

start_str = "@OptIn(ExperimentalMaterial3Api::class)\n@Composable\nfun GroupCardItem("
end_str = "private fun String?.isNull_or_empty(): Boolean = this == null || this.isBlank() || this == \"بدون يوم ثاني\""

start_idx = content.find(start_str)
end_idx = content.find(end_str)

if start_idx != -1 and end_idx != -1:
    new_file = content[:start_idx] + new_content + "\n\n" + content[end_idx:]
    with open("app/src/main/java/com/example/ui/screens/GroupsScreen.kt", "w") as f:
        f.write(new_file)
    print("Success")
else:
    print(f"Failed to find indices. Start: {start_idx}, End: {end_idx}")

