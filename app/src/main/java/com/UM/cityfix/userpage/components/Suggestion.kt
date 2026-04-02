package com.UM.cityfix.userpage.components

import android.net.Uri
import android.text.format.DateUtils
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ModeComment
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.UM.cityfix.components.ExpandableText
import com.UM.cityfix.components.tutorialBox
import com.cloudinary.android.MediaManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
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

fun uploadToCloudinary(uri: Uri, content: String, authorEmail: String) {
    MediaManager.get().upload(uri)
        .unsigned("ml_default") // Ensure this matches your dashboard
        .option("folder", "Suggestion") // Saves specifically to Suggestion folder
        .callback(object : com.cloudinary.android.callback.UploadCallback {
            override fun onStart(requestId: String) {}
            override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}

            override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                val imageUrl = resultData["secure_url"].toString()
                saveSuggestionToFirestore(content, authorEmail, imageUrl)
            }

            override fun onError(requestId: String, error: com.cloudinary.android.callback.ErrorInfo) {
                Log.e("Cloudinary", "Upload failed: ${error.description}")
            }

            override fun onReschedule(requestId: String, error: com.cloudinary.android.callback.ErrorInfo) {}
        }).dispatch()
}

fun saveSuggestionToFirestore(content: String, author: String, imageUrl: String?) {
    val db = FirebaseFirestore.getInstance()
    val id = db.collection("suggestions").document().id

    val item = Suggestion(
        id = id,
        author = author,
        content = content,
        imageUrl = imageUrl,
        timestamp = System.currentTimeMillis()
    )

    db.collection("suggestions").document(id).set(item)
}

@Composable
fun SuggestionItem(item: Suggestion, onCommentClick: (String) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: ""

    var profilePicUrl by remember { mutableStateOf<String?>(null) }

    val isLiked = item.likedBy.contains(userId)
    val isDisliked = item.dislikedBy.contains(userId)

    // Fetch Profile Picture for the author
    LaunchedEffect(item.author) {
        db.collection("users")
            .whereEqualTo("email", item.author)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]
                    profilePicUrl = document.getString("profilePicture")
                }
            }
    }

    Column(modifier = Modifier.tutorialBox().padding(10.dp)) {
        // Author and Time Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = profilePicUrl,
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.pic_user)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.author.split("@")[0],
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = formatTimeAgo(item.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 10.dp),
            thickness = 0.5.dp,
            color = Color.White.copy(alpha = 0.2f)
        )

        // The Text Content
        ExpandableText(
            text = item.content,
            textColor = Color.White,
            clickableColor = Color.White.copy(alpha = 0.7f),
            minimizedMaxLines = 1 // Increased to 3 for better readability
        )

        // The Suggestion Image
        if (!item.imageUrl.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            AsyncImage(
                model = item.imageUrl,
                contentDescription = "Suggestion Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp) // Good for different photo sizes
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Interaction Row: Like, Dislike, and Comment
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LIKE
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    val docRef = db.collection("suggestions").document(item.id)
                    if (isLiked) docRef.update("likedBy", FieldValue.arrayRemove(userId))
                    else {
                        docRef.update("likedBy", FieldValue.arrayUnion(userId))
                        docRef.update("dislikedBy", FieldValue.arrayRemove(userId))
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = "Like",
                        tint = if (isLiked) Color(0xFF4191E3) else Color.White
                    )
                }
                Text("${item.likedBy.size}", color = Color.White)
            }

            Spacer(modifier = Modifier.width(12.dp))

            // DISLIKE
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    val docRef = db.collection("suggestions").document(item.id)
                    if (isDisliked) docRef.update("dislikedBy", FieldValue.arrayRemove(userId))
                    else {
                        docRef.update("dislikedBy", FieldValue.arrayUnion(userId))
                        docRef.update("likedBy", FieldValue.arrayRemove(userId))
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.ThumbDown,
                        contentDescription = "Dislike",
                        tint = if (isDisliked) Color(0xFF4191E3) else Color.White
                    )
                }
                Text("${item.dislikedBy.size}", color = Color.White)
            }

            Spacer(modifier = Modifier.weight(1f))

            // COMMENT BUTTON
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onCommentClick(item.id) }) {
                    Icon(
                        imageVector = Icons.Default.ModeComment,
                        contentDescription = "Comment",
                        tint = Color.White
                    )
                }
                Text("${item.commentCount}", color = Color.White)
            }
        }
    }
}