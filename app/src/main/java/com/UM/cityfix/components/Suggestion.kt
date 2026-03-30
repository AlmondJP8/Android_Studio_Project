package com.UM.cityfix.components

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.ModeComment
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class Suggestion(
    val id: String = "",
    val author: String = "",
    val content: String = "",
    val likedBy: List<String> = emptyList(),
    val dislikedBy: List<String> = emptyList(),
    val commentCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

data class Comment(
    val id: String = "",
    val author: String = "",
    val text: String = "",
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
fun SuggestionItem(item: Suggestion, navController: NavHostController? = null) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: ""

    val isLiked = item.likedBy.contains(userId)
    val isDisliked = item.dislikedBy.contains(userId)

    Column(modifier = Modifier.tutorialBox().padding(5.dp)) {
        // Author and Time Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.author.split("@")[0],
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = formatTimeAgo(item.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(text = item.content, color = Color.White)
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
                navController?.navigate("comments/${item.id}")
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