package com.UM.cityfix.components

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.ModeComment
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.UM.cityfix.R
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class Suggestion(
    val id: String = "",
    val authorId: String = "",
    val author: String = "",
    val content: String = "",
    val imageUrl: String? = null,
    val likedBy: List<String> = emptyList(),
    val dislikedBy: List<String> = emptyList(),
    val commentCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

fun formatTimeAgo(timestamp: Long): String {
    return DateUtils.getRelativeTimeSpanString(
        timestamp,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS
    ).toString()
}

@Composable
fun SuggestionItem(item: Suggestion, onCommentClick: (String) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: ""
    var profilePicUrl by remember { mutableStateOf<String?>(null) }

    val isLiked = item.likedBy.contains(userId)
    val isDisliked = item.dislikedBy.contains(userId)

    LaunchedEffect(item.author) {
        db.collection("users")
            .whereEqualTo("email", item.author) // Search for the doc where email matches
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]
                    profilePicUrl = document.getString("profilePicture")
                }
            }
    }

    Column(modifier = Modifier.tutorialBox().padding(5.dp)) {
        // Author and Time Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = profilePicUrl, // This is the URL from Firestore
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape), // Makes the image round
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.pic_user)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f), // Takes up remaining horizontal space
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = item.author.split("@")[0],
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Text(
                text = formatTimeAgo(item.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), thickness = 0.9.dp)

        Text(text = item.content, color = Color.White)

        Spacer(modifier = Modifier.height(8.dp))

        // ADD THIS BLOCK:
        if (!item.imageUrl.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            AsyncImage(
                model = item.imageUrl,
                contentDescription = "Suggestion Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Interaction Row: Like, Dislike, and Comment
        Row(verticalAlignment = Alignment.CenterVertically) {
            // LIKE
            IconButton(onClick = {
                val docRef = db.collection("suggestions").document(item.id)
                if (isLiked) docRef.update("likedBy", com.google.firebase.firestore.FieldValue.arrayRemove(userId))
                else {
                    docRef.update("likedBy", com.google.firebase.firestore.FieldValue.arrayUnion(userId))
                    docRef.update("dislikedBy", com.google.firebase.firestore.FieldValue.arrayRemove(userId))
                }
            }) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.ThumbUp,
                    contentDescription = "Like",
                    tint = if (isLiked) Color(0xFF4191E3) else Color.White
                )
            }
            Text("${item.likedBy.size}", color = Color.White)

            Spacer(modifier = Modifier.width(10.dp))

            // DISLIKE
            IconButton(onClick = {
                val docRef = db.collection("suggestions").document(item.id)
                if (isDisliked) docRef.update("dislikedBy", com.google.firebase.firestore.FieldValue.arrayRemove(userId))
                else {
                    docRef.update("dislikedBy", com.google.firebase.firestore.FieldValue.arrayUnion(userId))
                    docRef.update("likedBy", com.google.firebase.firestore.FieldValue.arrayRemove(userId))
                }
            }) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.ThumbDown,
                    contentDescription = "Dislike",
                    tint = if (isDisliked) Color(0xFF4191E3) else Color.White
                )
            }
            Text("${item.dislikedBy.size}", color = Color.White)

            Spacer(modifier = Modifier.weight(1f))

            // COMMENT BUTTON
            IconButton(onClick = {
                onCommentClick(item.id) // This triggers the sheet in the parent
            }) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.ModeComment,
                    contentDescription = "Comment",
                    tint = Color.White
                )
            }
            Text("${item.commentCount}", color = Color.White)
        }
    }
}