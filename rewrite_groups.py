import re

with open("app/src/main/java/com/example/ui/screens/GroupsScreen.kt", "r") as f:
    content = f.read()

# Replace onSave signature in GroupsScreen
content = content.replace(
    "onSave = { name, subject, day1, day2, time, fee, groupType ->",
    "onSave = { name, subject, day1, day2, day3, time1, time2, time3, fee, groupType ->"
)

content = content.replace(
    "viewModel.addGroup(name, subject, day1, day2, time, fee, groupType, context)",
    "viewModel.addGroup(name, subject, day1, day2, day3, time1, time2, time3, fee, groupType, context)"
)

content = content.replace(
    """                    val updatedGroup = groupToEdit.copy(
                        name = name,
                        subject = subject,
                        day1 = day1,
                        day2 = if (day2 == "بدون يوم ثاني") null else day2,
                        timeSlot = time,
                        monthlyFee = fee,
                        groupType = groupType
                    )""",
    """                    val updatedGroup = groupToEdit.copy(
                        name = name,
                        subject = subject,
                        day1 = day1,
                        day2 = if (day2 == "بدون") null else day2,
                        day3 = if (day3 == "بدون") null else day3,
                        timeSlot = time1,
                        timeSlot2 = time2,
                        timeSlot3 = time3,
                        monthlyFee = fee,
                        groupType = groupType
                    )"""
)

# Now rewrite the dialog. We'll find it by searching for "fun GroupFormDialog(" and replacing the whole function.
pattern = r"fun GroupFormDialog\([\s\S]*?^}"
import re
match = re.search(pattern, content, re.MULTILINE)
if match:
    new_dialog = """fun GroupFormDialog(
    group: GroupEntity?,
    initialType: String = "CENTER",
    onDismiss: () -> Unit,
    onSave: (name: String, subject: String, day1: String, day2: String?, day3: String?, time1: String, time2: String?, time3: String?, fee: Double, groupType: String) -> Unit
) {
    var name by remember { mutableStateOf(group?.name ?: "") }
    var selectedGroupType by remember { mutableStateOf(group?.groupType ?: initialType) }
    var selectedSubject by remember { mutableStateOf(group?.subject ?: SUBJECT_LIST[0]) }
    
    var selectedDay1 by remember { mutableStateOf(group?.day1 ?: DAYS_LIST[0]) }
    var selectedDay2 by remember { mutableStateOf(group?.day2 ?: "بدون") }
    var selectedDay3 by remember { mutableStateOf(group?.day3 ?: "بدون") }
    
    var selectedTime1 by remember { mutableStateOf(group?.timeSlot ?: TIME_SLOTS[16]) }
    var selectedTime2 by remember { mutableStateOf(group?.timeSlot2 ?: TIME_SLOTS[16]) }
    var selectedTime3 by remember { mutableStateOf(group?.timeSlot3 ?: TIME_SLOTS[16]) }
    
    var feeText by remember { mutableStateOf(group?.monthlyFee?.toInt()?.toString() ?: "150") }

    val daysOptions = listOf("بدون") + DAYS_LIST

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (group == null) "إضافة مجموعة" else "تعديل مجموعة", fontWeight = FontWeight.Bold, color = SkyOnSurface) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("نوع المجموعة:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = SkyOnSurface)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = selectedGroupType == "CENTER", onClick = { selectedGroupType = "CENTER" }, label = { Text("🏬 سنتر") }, modifier = Modifier.weight(1f))
                    FilterChip(selected = selectedGroupType == "ONLINE", onClick = { selectedGroupType = "ONLINE" }, label = { Text("🌐 أونلاين") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم المجموعة") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                
                // Days and Times Selection (Side-by-side days, below them times)
                // The user said: "يوم 3 مربعات جمب بعض اليوم و3 مربعات تحت كل موعد لاختيار ساعة وموعد الدرس"
                // Let's do a Grid-like layout using Rows.
                
                Text("الأيام والمواعيد (اختر اليوم ثم الوقت تحته):", fontWeight = FontWeight.Bold, color = SkyOnSurface, fontSize = 14.sp)
                
                // Days Row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Day 1
                    Column(modifier = Modifier.weight(1f)) {
                        Text("يوم 1", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        DropdownMenuSelector(selectedDay1, DAYS_LIST) { selectedDay1 = it }
                    }
                    // Day 2
                    Column(modifier = Modifier.weight(1f)) {
                        Text("يوم 2", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        DropdownMenuSelector(selectedDay2, daysOptions) { selectedDay2 = it }
                    }
                    // Day 3
                    Column(modifier = Modifier.weight(1f)) {
                        Text("يوم 3", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        DropdownMenuSelector(selectedDay3, daysOptions) { selectedDay3 = it }
                    }
                }
                
                // Times Row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Time 1
                    Column(modifier = Modifier.weight(1f)) {
                        Text("وقت 1", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        DropdownMenuSelector(selectedTime1, TIME_SLOTS) { selectedTime1 = it }
                    }
                    // Time 2
                    Column(modifier = Modifier.weight(1f)) {
                        if (selectedDay2 != "بدون") {
                            Text("وقت 2", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            DropdownMenuSelector(selectedTime2, TIME_SLOTS) { selectedTime2 = it }
                        }
                    }
                    // Time 3
                    Column(modifier = Modifier.weight(1f)) {
                        if (selectedDay3 != "بدون") {
                            Text("وقت 3", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            DropdownMenuSelector(selectedTime3, TIME_SLOTS) { selectedTime3 = it }
                        }
                    }
                }

                OutlinedTextField(value = feeText, onValueChange = { feeText = it }, label = { Text("الاشتراك الشهرى") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isBlank()) return@Button
                val fee = feeText.toDoubleOrNull() ?: 150.0
                onSave(name, selectedSubject, selectedDay1, selectedDay2, selectedDay3, selectedTime1, selectedTime2, selectedTime3, fee, selectedGroupType)
            }) { Text("حفظ", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownMenuSelector(selected: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(text = { Text(opt, fontSize = 12.sp) }, onClick = { onSelect(opt); expanded = false })
            }
        }
    }
}
"""
    content = content[:match.start()] + new_dialog + content[match.end():]
else:
    print("Failed to match GroupFormDialog")

with open("app/src/main/java/com/example/ui/screens/GroupsScreen.kt", "w") as f:
    f.write(content)
