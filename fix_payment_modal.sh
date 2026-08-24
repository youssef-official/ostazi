sed -i '/Box(/,/}            }/c\
                    Box(\
                        modifier = Modifier\
                            .size(80.dp)\
                            .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF34D399), Color(0xFF059669))), CircleShape)\
                            .shadow(8.dp, CircleShape),\
                        contentAlignment = Alignment.Center\
                    ) {\
                        Icon(androidx.compose.material.icons.Icons.Default.Payments, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))\
                    }\
\
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {\
                        Text("تسجيل الدفع المالي", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color(0xFF1E293B))\
                        Spacer(modifier = Modifier.height(4.dp))\
                        Text(student.fullName, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))\
                        Surface(color = Color(0xFFFEF3C7), shape = RoundedCornerShape(8.dp), modifier = Modifier.padding(top = 8.dp)) {\
                            Text("قيمة الحصة: ${fee.toInt()} ج.م", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309), modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))\
                        }\
                    }\
\
                    Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)\
\
                    Button(\
                        onClick = {\
                            viewModel.setPaymentStatusWithDiscount(student.id, "PAID", 0.0, fee, 0.0, currentDateStr, currentDateStr)\
                            Toast.makeText(context, "تم تسجيل دفع كامل 🟢", Toast.LENGTH_SHORT).show()\
                            selectedStudentForOptions = null\
                        },\
                        modifier = Modifier.fillMaxWidth().height(56.dp),\
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),\
                        shape = RoundedCornerShape(16.dp),\
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)\
                    ) {\
                        Icon(androidx.compose.material.icons.Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))\
                        Spacer(modifier = Modifier.width(10.dp))\
                        Text("دفع كامل (${fee.toInt()} ج.م)", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)\
                    }\
\
                    Button(\
                        onClick = {\
                            viewModel.setPaymentStatusWithDiscount(student.id, "EXEMPT", 0.0, 0.0, 0.0, null, currentDateStr)\
                            Toast.makeText(context, "تم إعفاء الطالب من الدفع 🟣", Toast.LENGTH_SHORT).show()\
                            selectedStudentForOptions = null\
                        },\
                        modifier = Modifier.fillMaxWidth().height(56.dp),\
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),\
                        shape = RoundedCornerShape(16.dp),\
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)\
                    ) {\
                        Icon(androidx.compose.material.icons.Icons.Default.CardGiftcard, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))\
                        Spacer(modifier = Modifier.width(10.dp))\
                        Text("معفي من الدفع", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)\
                    }\
\
                    Button(\
                        onClick = {\
                            viewModel.setPaymentStatusWithDiscount(student.id, "UNPAID", 0.0, 0.0, fee, null, currentDateStr)\
                            Toast.makeText(context, "تم تعيين كـ لم يدفع 🔴", Toast.LENGTH_SHORT).show()\
                            selectedStudentForOptions = null\
                        },\
                        modifier = Modifier.fillMaxWidth().height(56.dp),\
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEF2F2)),\
                        shape = RoundedCornerShape(16.dp),\
                        border = BorderStroke(1.5.dp, Color(0xFFEF4444)),\
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)\
                    ) {\
                        Icon(androidx.compose.material.icons.Icons.Default.Cancel, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(24.dp))\
                        Spacer(modifier = Modifier.width(10.dp))\
                        Text("لم يدفع / غير مسدد", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFEF4444))\
                    }\
\
                    TextButton(\
                        onClick = { selectedStudentForOptions = null },\
                        modifier = Modifier.fillMaxWidth()\
                    ) {\
                        Text("إلغاء", color = Color(0xFF94A3B8), fontSize = 16.sp, fontWeight = FontWeight.Bold)\
                    }\
                }\
            }\
        }' app/src/main/java/com/example/ui/screens/PerSessionScreen.kt
