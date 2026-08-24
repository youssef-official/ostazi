package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import kotlin.math.sin

/**
 * A beautiful straight horizontal progress bar card representing the monthly financials.
 * Inside the progress bar, there is an animated sloshing water/wave effect with floating bubbles.
 * Underneath the progress line, Expected, Collected, and Remaining amounts are displayed side-by-side.
 */
@Composable
fun DashboardFinancialWaterLineCard(
    totalCollected: Double,
    totalExpected: Double,
    totalRemaining: Double,
    monthName: String,
    year: Int? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val progress = remember(totalCollected, totalExpected) {
        if (totalExpected > 0) (totalCollected / totalExpected).toFloat().coerceIn(0f, 1f) else 0f
    }
    val percentage = (progress * 100).toInt()

    // Animators for waves and bubbles
    val infiniteTransition = rememberInfiniteTransition(label = "lineWaterTransition")

    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "lineWavePhase"
    )

    val bubbleAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "lineBubbleAnim"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outline.copy(alpha = .65f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Percentage Tag
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFECFDF5),
                    border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                ) {
                    Text(
                        text = "$percentage% مكتمل",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF059669),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Month Info Aligned Right
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "السجل المالي للشهر الحالي ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "$monthName ${year ?: ""}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Animated Straight Water Progress Line
            val progressTrackColor = MaterialTheme.colorScheme.surfaceVariant
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(22.dp)
                ) {
                    val w = size.width
                    val h = size.height

                    // 1. Draw Background Track (straight line)
                    val trackPath = Path().apply {
                        addRoundRect(
                            androidx.compose.ui.geometry.RoundRect(
                                left = 0f,
                                top = 0f,
                                right = w,
                                bottom = h,
                                cornerRadius = CornerRadius(h / 2, h / 2)
                            )
                        )
                    }
                    drawPath(trackPath, progressTrackColor)

                    // 2. Draw Moving Water Liquid (if progress > 0)
                    if (progress > 0f) {
                        val fillW = w * progress
                        val waterPath = Path().apply {
                            addRoundRect(
                                androidx.compose.ui.geometry.RoundRect(
                                    left = 0f,
                                    top = 0f,
                                    right = fillW,
                                    bottom = h,
                                    cornerRadius = CornerRadius(h / 2, h / 2)
                                )
                            )
                        }

                        clipPath(waterPath) {
                            // Back Wave
                            val wavePath1 = Path().apply {
                                moveTo(0f, h)
                                lineTo(0f, h * 0.4f)
                                var x = 0f
                                while (x <= fillW) {
                                    val y = h * 0.35f + sin((x / w * 4 * Math.PI + wavePhase).toDouble()).toFloat() * 3f
                                    lineTo(x, y)
                                    x += 5f
                                }
                                lineTo(fillW, h)
                                close()
                            }
                            drawPath(
                                path = wavePath1,
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFF86EFAC), Color(0xFF16A36A))
                                )
                            )

                            // Front Wave
                            val wavePath2 = Path().apply {
                                moveTo(0f, h)
                                lineTo(0f, h * 0.5f)
                                var x = 0f
                                while (x <= fillW) {
                                    val y = h * 0.45f + sin((x / w * 3.5 * Math.PI - wavePhase).toDouble()).toFloat() * 2.5f
                                    lineTo(x, y)
                                    x += 5f
                                }
                                lineTo(fillW, h)
                                close()
                            }
                            drawPath(
                                path = wavePath2,
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFF34D399), Color(0xFF047857))
                                )
                            )

                            // Rising Bubbles inside the line progress bar
                            val bubbleX1 = fillW * ((bubbleAnim + 0.1f) % 1f)
                            val bubbleX2 = fillW * ((bubbleAnim + 0.6f) % 1f)
                            drawCircle(
                                color = Color.White.copy(alpha = 0.6f),
                                radius = 2.5f,
                                center = Offset(bubbleX1, h * 0.6f)
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = 0.5f),
                                radius = 2.0f,
                                center = Offset(bubbleX2, h * 0.4f)
                            )
                        }
                    }

                    // 3. Draw Track Outline for high premium feel
                    drawPath(
                        path = trackPath,
                        color = Color(0x3094A3B8),
                        style = Stroke(width = 1.5f)
                    )
                }

                // Floating revenue marker rides smoothly on the progress line at the leading fill edge
                val iconOffset = if (progress > 0f) progress else 0f
                Box(
                    modifier = Modifier
                        .fillMaxWidth(iconOffset.coerceIn(0.08f, 1f))
                        .align(Alignment.CenterStart),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Box(
                        modifier = Modifier
                            .offset(y = (-4).dp)
                            .shadow(2.dp, CircleShape)
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                            .size(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WaterDrop,
                            contentDescription = null,
                            tint = Color(0xFFD49A2A),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            // Statistics side-by-side Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Expected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "المتوقع", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.height(2.dp))
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(text = "${totalExpected.toInt()}", fontSize = 12.5.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(text = "ج.م", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                }

                // 2. Collected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "المحصل", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(modifier = Modifier.height(2.dp))
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(text = "${totalCollected.toInt()}", fontSize = 12.5.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                Text(text = "ج.م", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }
                }

                // 3. Remaining
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "المتبقي", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(modifier = Modifier.height(2.dp))
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(text = "${totalRemaining.toInt()}", fontSize = 12.5.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onErrorContainer)
                                Text(text = "ج.م", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }
            }
        }
    }
}
