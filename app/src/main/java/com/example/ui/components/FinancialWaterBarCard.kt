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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import kotlin.math.sin

/**
 * Premium glass bottle financial card with animated water, bubbles and right alignment.
 */
@Composable
fun FinancialWaterBarCard(
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

    // Smooth wave animations
    val infiniteTransition = rememberInfiniteTransition(label = "jarWaterTransition")
    
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    val wavePhaseBack by infiniteTransition.animateFloat(
        initialValue = (Math.PI).toFloat(),
        targetValue = (3f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhaseBack"
    )

    val bubbleAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bubbleAnim"
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
        // Force LTR Row so the Bottle is ALWAYS on the RIGHT side, and Statistics on the LEFT side!
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                
                // -------------------------------------------------------------
                // LEFT SIDE: STATISTICS (ALIGNED RIGHT inside itself for Arabic text)
                // -------------------------------------------------------------
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.End // Aligned right for beautiful Arabic reading
                ) {
                    // Header Information
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "السجل المالي للمجموعة ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$monthName ${year ?: ""}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Stats rows
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // 1. Expected Amount (المتوقع)
                        StatRowItem(
                            title = "المبلغ المتوقع",
                            amount = totalExpected,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            textColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        // 2. Collected Amount (المحصل)
                        StatRowItem(
                            title = "المبلغ المحصل",
                            amount = totalCollected,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            textColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            badge = "$percentage%"
                        )

                        // 3. Remaining Amount (المتبقي)
                        StatRowItem(
                            title = "المبلغ المتبقي",
                            amount = totalRemaining,
                            color = MaterialTheme.colorScheme.errorContainer,
                            textColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                // -------------------------------------------------------------
                // RIGHT SIDE: SLIM GLASS BOTTLE WITH ANIMATED WATER & BUBBLES
                // -------------------------------------------------------------
                Box(
                    modifier = Modifier
                        .width(70.dp)
                        .height(110.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        // 1. Wooden Cork Stopper (Top) - Refined Gold Cap
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFFD97706), Color(0xFFF59E0B), Color(0xFFD97706))
                            ),
                            topLeft = Offset(w * 0.38f, 0f),
                            size = Size(w * 0.24f, 8f),
                            cornerRadius = CornerRadius(2.5f, 2.5f)
                        )

                        // 2. Glass Neck Ring
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFFCBD5E1), Color(0xFFF8FAFC), Color(0xFFCBD5E1))
                            ),
                            topLeft = Offset(w * 0.30f, 8f),
                            size = Size(w * 0.40f, 6f),
                            cornerRadius = CornerRadius(3f, 3f)
                        )

                        // 3. Elegant Rounded Glass Jar Body Path (Capsule-like)
                        val bottlePath = Path().apply {
                            addRoundRect(
                                RoundRect(
                                    left = w * 0.10f,
                                    top = 14f,
                                    right = w * 0.90f,
                                    bottom = h,
                                    cornerRadius = CornerRadius(16f, 16f)
                                )
                            )
                        }

                        // Glass Back Shadow Depth
                        drawPath(
                            path = bottlePath,
                            color = Color(0x0A94A3B8)
                        )

                        // 4. Animated Liquid Waves inside Slim Bottle
                        val maxFillHeight = h - 20f
                        val currentFillHeight = maxFillHeight * progress
                        if (currentFillHeight > 0f) {
                            val baseWaterY = h - currentFillHeight

                            clipPath(bottlePath) {
                                // A. Back Wave (Slower, slightly lighter blue/green)
                                val wavePathBack = Path().apply {
                                    moveTo(0f, h)
                                    lineTo(0f, baseWaterY)
                                    var x = 0f
                                    while (x <= w) {
                                        val waveAmp = (2.5f * progress).coerceAtMost(4f)
                                        val y = baseWaterY + sin((x / w * 2.8 * Math.PI + wavePhaseBack).toDouble()).toFloat() * waveAmp
                                        lineTo(x, y)
                                        x += 4f
                                    }
                                    lineTo(w, h)
                                    close()
                                }
                                drawPath(
                                    path = wavePathBack,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0x8834D399), // green/teal back wave
                                            Color(0xBB059669),
                                            Color(0xDD064E3B)
                                        ),
                                        startY = baseWaterY - 3f,
                                        endY = h
                                    )
                                )

                                // B. Front Wave (Faster, vibrant green)
                                val wavePathFront = Path().apply {
                                    moveTo(0f, h)
                                    lineTo(0f, baseWaterY)
                                    var x = 0f
                                    while (x <= w) {
                                        val waveAmp = (3.5f * progress).coerceAtMost(5f)
                                        val y = baseWaterY + sin((x / w * 2.4 * Math.PI + wavePhase).toDouble()).toFloat() * waveAmp
                                        lineTo(x, y)
                                        x += 4f
                                    }
                                    lineTo(w, h)
                                    close()
                                }
                                drawPath(
                                    path = wavePathFront,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xCC10B981), // vibrant emerald top
                                            Color(0xFF059669),
                                            Color(0xFF064E3B)
                                        ),
                                        startY = baseWaterY - 1f,
                                        endY = h
                                    )
                                )

                                // C. Floating Bubbles (Bubbles rise up and fade out)
                                val bubble1Y = h - (currentFillHeight * ((bubbleAnim + 0.15f) % 1f))
                                val bubble2Y = h - (currentFillHeight * ((bubbleAnim + 0.55f) % 1f))
                                val bubble3Y = h - (currentFillHeight * ((bubbleAnim + 0.85f) % 1f))

                                if (bubble1Y > baseWaterY) {
                                    drawCircle(
                                        color = Color(0x55FFFFFF),
                                        radius = 2.5f,
                                        center = Offset(w * 0.30f, bubble1Y)
                                    )
                                }
                                if (bubble2Y > baseWaterY) {
                                    drawCircle(
                                        color = Color(0x77FFFFFF),
                                        radius = 4f,
                                        center = Offset(w * 0.70f, bubble2Y)
                                    )
                                    drawCircle(
                                        color = Color.White,
                                        radius = 1f,
                                        center = Offset(w * 0.68f, bubble2Y - 1f)
                                    )
                                }
                                if (bubble3Y > baseWaterY) {
                                    drawCircle(
                                        color = Color(0x33FFFFFF),
                                        radius = 2f,
                                        center = Offset(w * 0.50f, bubble3Y)
                                    )
                                }
                            }
                        }

                        // 5. High-End Vertical Gloss Highlight on Glass Left
                        drawPath(
                            path = Path().apply {
                                moveTo(w * 0.18f, 20f)
                                quadraticTo(w * 0.14f, h / 2, w * 0.18f, h - 14f)
                            },
                            color = Color.White.copy(alpha = 0.22f),
                            style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                        )

                        // 6. Outer Glass Outline Border
                        drawPath(
                            path = bottlePath,
                            color = Color(0x2594A3B8),
                            style = Stroke(width = 2f)
                        )
                    }

                    // Percentage floating indicator in center of jar
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(top = 10.dp)
                            .shadow(2.dp, CircleShape)
                            .background(Color.White.copy(alpha = 0.94f), CircleShape)
                            .size(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$percentage%",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF047857)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRowItem(
    title: String,
    amount: Double,
    color: Color,
    textColor: Color,
    badge: String? = null
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Force RTL layout inside the StatRowItem to support proper Arabic text direction!
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    // Small dot
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(textColor)
                    )
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569)
                    )
                    
                    if (badge != null) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = textColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = badge,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = textColor,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                            )
                        }
                    }
                }
                
                // Bulletproof currency view: always ensures number on left and "ج.م" on right in LTR container
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "${amount.toInt()}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = textColor
                        )
                        Text(
                            text = "ج.م",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }
                }
            }
        }
    }
}
