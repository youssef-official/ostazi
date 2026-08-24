package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val OstaziFontFamily = FontFamily.SansSerif

// Calm, compact Material type scale matching the supplied Arabic reference.
val Typography =
  Typography(
    titleLarge = TextStyle(
      fontFamily = OstaziFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = 24.sp,
      lineHeight = 32.sp,
      letterSpacing = (-0.15).sp
    ),
    titleMedium = TextStyle(
      fontFamily = OstaziFontFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = 18.sp,
      lineHeight = 26.sp
    ),
    bodyMedium = TextStyle(
      fontFamily = OstaziFontFamily,
      fontWeight = FontWeight.Normal,
      fontSize = 14.sp,
      lineHeight = 22.sp
    ),
    labelLarge = TextStyle(
      fontFamily = OstaziFontFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = 14.sp,
      lineHeight = 20.sp
    ),
    labelSmall = TextStyle(
      fontFamily = OstaziFontFamily,
      fontWeight = FontWeight.Medium,
      fontSize = 11.sp,
      lineHeight = 16.sp
    ),
    bodyLarge =
      TextStyle(
        fontFamily = OstaziFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
      ),
    bodySmall = TextStyle(
      fontFamily = OstaziFontFamily,
      fontWeight = FontWeight.Normal,
      fontSize = 12.sp,
      lineHeight = 18.sp
    ),
    titleSmall = TextStyle(
      fontFamily = OstaziFontFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = 15.sp,
      lineHeight = 21.sp
    )
  )
