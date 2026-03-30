package com.UM.cityfix.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

// Set of Material typography styles to start with
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)

val appName = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Bold,
    textAlign = TextAlign.Center,
    color = Color.Black,
    fontSize = 20.sp,
    lineHeight = 24.sp,
    letterSpacing = 0.5.sp
)

val buttonText = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Bold,
    textAlign = TextAlign.Center,
    color = Color.White,
    fontSize = 15.sp,
    lineHeight = 24.sp,
    letterSpacing = 0.5.sp
)

val conttext = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.W800,
    color = Color.White,
    fontSize = 20.sp,
    lineHeight = 24.sp,
    letterSpacing = 0.5.sp,
    textAlign = TextAlign.Center
)

val gridText = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Bold,
    fontSize = 15.sp,
    color = Color.Black,
    lineHeight = 24.sp,
    letterSpacing = 0.5.sp
)