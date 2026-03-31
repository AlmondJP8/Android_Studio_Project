package com.UM.cityfix.userpage.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.UM.cityfix.R

@Composable
fun ProfileIssueItem(
    title: String,
    status: String,
    location: String,
    imageUrl: String = "" // Added imageUrl parameter
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Image Thumbnail (Cloudinary URL)
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl.ifEmpty { R.drawable.pic_user }) // Use a default icon if empty
                    .crossfade(true)
                    .build(),
                contentDescription = "Issue Image",
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF0F0F0)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 2. Text Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF333333),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = location,
                    color = Color.Gray,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 3. Dynamic Status Label
            val (backgroundColor, textColor) = when (status.lowercase()) {
                "resolved" -> Color(0xFFE3F2FD) to Color(0xFF1976D2) // Blue
                "pending" -> Color(0xFFFFF3E0) to Color(0xFFE65100)  // Orange
                else -> Color(0xFFF5F5F5) to Color(0xFF757575)       // Gray
            }

            Surface(
                color = backgroundColor,
                shape = RoundedCornerShape(20.dp) // Pills look more modern
            ) {
                Text(
                    text = status.uppercase(),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    color = textColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}