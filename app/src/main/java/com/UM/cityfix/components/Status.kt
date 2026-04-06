package com.UM.cityfix.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

@Composable
fun StatusBadge(status: String) {
    val (color, label) = when (status) {
        "Pending" -> Color(0xFFFFB74D) to "PENDING"
        "Ongoing" -> Color(0xFF1976D2) to "ONGOING"
        "Resolved" -> Color(0xFF81C784) to "FIXED"
        "Blocked" -> Color(0xFFD32F2F) to "BLOCKED"
        else -> Color.Gray to "UNKNOWN"
    }

    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = TextStyle(color = color, fontWeight = FontWeight.Bold, fontSize = 10.sp)
        )
    }
}


fun createCustomMarker(context: Context, iconResId: Int, backgroundColor: Int, sizePx: Int
): Drawable {
    // 1. Create the blank canvas
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // 2. Draw the background shape (e.g., a circle)
    paint.color = backgroundColor
    val rect = RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat())
    canvas.drawOval(rect, paint)

    // 3. Draw the PNG icon on top
    val icon = ContextCompat.getDrawable(context, iconResId)
    if (icon != null) {
        // Calculate padding (e.g., 20% from the edge)
        val padding = sizePx / 5
        icon.setBounds(padding, padding, sizePx - padding, sizePx - padding)
        icon.draw(canvas)
    }
    return BitmapDrawable(context.resources, bitmap)
}