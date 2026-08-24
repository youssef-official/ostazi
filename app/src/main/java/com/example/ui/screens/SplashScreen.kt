package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }

    // Infinite transition for continuous 3D orbiting ring rotation
    val infiniteTransition = rememberInfiniteTransition(label = "orbit_transition")

    // Primary fast continuous ring rotation (0° to 360°)
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_angle"
    )

    // Secondary counter-rotation for inner sparkling particles
    val counterRotationAngle by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "counter_rotation"
    )

    // Pulsing cyan glow intensity
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    // Entrance scale & alpha
    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.75f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(900),
        label = "alpha"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        // Stay on splash long enough to enjoy the orbiting ring multiple times
        delay(1000)
        onSplashFinished()
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF03132B), // Glowing Cyan-Dark Core
                            Color(0xFF020917), // Deep Midnight Navy
                            Color(0xFF000308)  // Pure Void Black
                        ),
                        center = Offset.Unspecified,
                        radius = 1200f
                    )
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onSplashFinished()
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .scale(scaleAnim)
                    .alpha(alphaAnim),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Main Logo & Continuous Animated Orbiting Ring Container
                Box(
                    modifier = Modifier
                        .size(310.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Ambient Cyan Glow Aura Behind Logo
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .graphicsLayer {
                                alpha = glowPulse * 0.4f
                            }
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF00E5FF).copy(alpha = 0.45f),
                                        Color(0xFF0284C7).copy(alpha = 0.2f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                    )

                    // 1. Outer 3D Orbiting Ring Canvas (Continuous Rotation around "أستاذي")
                    Canvas(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val center = Offset(canvasWidth / 2f, canvasHeight / 2f)

                        val ellipseRx = canvasWidth * 0.44f
                        val ellipseRy = canvasHeight * 0.22f

                        // A. Draw Orbiting Glow Ring 1 (Main Tilted Cyan Oval)
                        rotate(degrees = -18f, pivot = center) {
                            // Glowing Ellipse Track
                            drawOval(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        Color(0xFF38BDF8),
                                        Color(0xFF00E5FF),
                                        Color(0xFF0284C7),
                                        Color(0xFF60A5FA),
                                        Color(0xFF38BDF8)
                                    ),
                                    center = center
                                ),
                                topLeft = Offset(center.x - ellipseRx, center.y - ellipseRy),
                                size = Size(ellipseRx * 2, ellipseRy * 2),
                                style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round),
                                alpha = 0.85f * glowPulse
                            )

                            // Outer Soft Blur Glow Path
                            drawOval(
                                color = Color(0xFF00E5FF),
                                topLeft = Offset(center.x - ellipseRx - 2.dp.toPx(), center.y - ellipseRy - 2.dp.toPx()),
                                size = Size((ellipseRx + 2.dp.toPx()) * 2, (ellipseRy + 2.dp.toPx()) * 2),
                                style = Stroke(width = 7.dp.toPx()),
                                alpha = 0.25f * glowPulse
                            )

                            // B. Calculate Orbiting Light Orb Position along Ellipse based on rotationAngle
                            val angleRad = Math.toRadians(rotationAngle.toDouble())
                            val orbX = center.x + ellipseRx * cos(angleRad).toFloat()
                            val orbY = center.y + ellipseRy * sin(angleRad).toFloat()

                            // Orbiting Head Glow
                            drawCircle(
                                color = Color(0xFF00E5FF),
                                radius = 10.dp.toPx(),
                                center = Offset(orbX, orbY),
                                alpha = 0.4f
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 5.dp.toPx(),
                                center = Offset(orbX, orbY),
                                alpha = 0.95f
                            )

                            // Orbiting Light Sparkle Star (+)
                            val starSize = 8.dp.toPx()
                            drawLine(
                                color = Color.White,
                                start = Offset(orbX - starSize, orbY),
                                end = Offset(orbX + starSize, orbY),
                                strokeWidth = 2.dp.toPx()
                            )
                            drawLine(
                                color = Color.White,
                                start = Offset(orbX, orbY - starSize),
                                end = Offset(orbX, orbY + starSize),
                                strokeWidth = 2.dp.toPx()
                            )
                        }

                        // C. Secondary Counter-Rotating Sparkling Ring (Opposite Tilt & Motion)
                        rotate(degrees = 22f, pivot = center) {
                            val secRx = canvasWidth * 0.38f
                            val secRy = canvasHeight * 0.18f

                            drawOval(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF60A5FA).copy(alpha = 0.1f),
                                        Color(0xFF38BDF8).copy(alpha = 0.5f),
                                        Color(0xFF00E5FF).copy(alpha = 0.1f)
                                    )
                                ),
                                topLeft = Offset(center.x - secRx, center.y - secRy),
                                size = Size(secRx * 2, secRy * 2),
                                style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round),
                                alpha = 0.6f
                            )

                            // Counter-Orbiting Orb
                            val secAngleRad = Math.toRadians(counterRotationAngle.toDouble())
                            val secOrbX = center.x + secRx * cos(secAngleRad).toFloat()
                            val secOrbY = center.y + secRy * sin(secAngleRad).toFloat()

                            drawCircle(
                                color = Color(0xFF38BDF8),
                                radius = 4.dp.toPx(),
                                center = Offset(secOrbX, secOrbY),
                                alpha = 0.8f
                            )
                        }
                    }

                    // Brand lockup supplied by the product owner. The deep-black
                    // artwork blends into the midnight canvas while the blue glow
                    // keeps the mark legible on every device.
                    Image(
                        painter = painterResource(id = R.drawable.ostazy_splash_logo),
                        contentDescription = "شعار أستاذي",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(270.dp)
                            .graphicsLayer {
                                alpha = 0.98f
                                shadowElevation = 22.dp.toPx()
                            }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "منصة إدارة المعلم المتميزة",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8),
                    letterSpacing = 1.2.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(36.dp))

                // Smooth Rotating Loader Indicator
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .graphicsLayer {
                            rotationZ = rotationAngle * 1.5f
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFF38BDF8),
                                    Color(0xFF00E5FF)
                                )
                            ),
                            startAngle = 0f,
                            sweepAngle = 280f,
                            useCenter = false,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
            }
        }
    }
}
