package com.UM.cityfix.userpage.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.UM.cityfix.R
import com.UM.cityfix.components.ExpandableText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

data class Comment(
    val id: String = "",
    val authorId: String = "",
    val author: String = "",
    val text: String = "",
    val timestamp: Long = 0L
)

@Composable
fun CommentScreen(navController: NavHostController, suggestionId: String) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    // States
    var commentText by remember { mutableStateOf("") }
    val comments = remember { mutableStateListOf<Comment>() }

    // Real-time listener for comments
    LaunchedEffect(suggestionId) {
        db.collection("suggestions").document(suggestionId)
            .collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    comments.clear()
                    comments.addAll(snapshot.toObjects(Comment::class.java))
                }
            }
    }

    Scaffold() { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // 1. The Scrollable Comment List
            LazyColumn(
                modifier = Modifier
                    .weight(1f) // Takes up all space between top and input box
                    .padding(horizontal = 16.dp)
            ) {
                items(comments) { comment ->
                    CommentItem(comment = comment)
                }
            }

            // 2. The Bottom Input Bar
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Write a comment...") }
                )

                IconButton(onClick = {
                    if (commentText.isNotBlank()) {
                        val currentUser = auth.currentUser
                        val commentRef = db.collection("suggestions")
                            .document(suggestionId)
                            .collection("comments")
                            .document()

                        val newComment = hashMapOf(
                            "id" to commentRef.id,
                            "authorId" to (currentUser?.uid ?: ""),
                            "author" to (currentUser?.email ?: "Anonymous"),
                            "text" to commentText.trim(),
                            "timestamp" to System.currentTimeMillis()
                        )

                        commentRef.set(newComment).addOnSuccessListener {
                            commentText = "" // Clear input
                            // Update the count on the main suggestion document
                            db.collection("suggestions").document(suggestionId)
                                .update("commentCount", FieldValue.increment(1))
                        }.addOnFailureListener { e ->
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                })
                {
                    Icon(Icons.Default.Send, contentDescription = "Send")
                }
            }
        }
    }
}

@Composable
fun CommentItem(comment: Comment) {
    val db = FirebaseFirestore.getInstance()
    var profilePicUrl by remember { mutableStateOf<String?>(null) }
    var displayName by remember { mutableStateOf(comment.author.split("@")[0]) }

    LaunchedEffect(comment.authorId) {
        if (comment.authorId.isNotEmpty()) {
            db.collection("users").document(comment.authorId)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        profilePicUrl = doc.getString("profilePicture")
                        val fName = doc.getString("firstName") ?: ""
                        if (fName.isNotEmpty()) displayName = fName
                    }
                }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(Color(0x11FDFDFD)),
        verticalAlignment = Alignment.Top
    ) {
        AsyncImage(
            model = profilePicUrl,
            contentDescription = "Profile Picture",
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(id = R.drawable.pic_user),
            error = painterResource(id = R.drawable.pic_user)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Header: Name and Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = comment.author.split("@")[0],
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4191E3), // Matches your Like button blue
                    fontSize = 13.sp
                )

                // --- ADDED TIMESTAMP HERE ---
                Text(
                    text = formatTimeAgo(comment.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray, // Or Color.White.copy(alpha = 0.6f) for dark mode
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // The Comment Content
            ExpandableText(
                text = comment.text,
                textColor = Color.Black,
                clickableColor = Color.Black,
                minimizedMaxLines = 1
            )

            HorizontalDivider(
                modifier = Modifier.padding(top = 10.dp),
                thickness = 0.5.dp,
                color = Color.Gray.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
fun CommentSheetContent(suggestionId: String) {
    val context = LocalContext.current // Added
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance() // Added
    var commentText by remember { mutableStateOf("") }
    val comments = remember { mutableStateListOf<Comment>() }
    val listState = rememberLazyListState()

    // Real-time listener
    LaunchedEffect(suggestionId) {
        db.collection("suggestions").document(suggestionId)
            .collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    comments.clear()
                    comments.addAll(snapshot.toObjects(Comment::class.java))
                }
            }
    }

    // AUTO-SCROLL FIX: Scrolls to bottom when keyboard opens or new comments arrive
    LaunchedEffect(comments.size) {
        if (comments.isNotEmpty()) {
            listState.animateScrollToItem(comments.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxHeight(0.7f) // Increased slightly for better view
            .fillMaxWidth()
            .background(Color.White)
            .navigationBarsPadding()
            .imePadding()
    ) {
        Text("Comments", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

        LazyColumn(
            state = listState, // Attach the listState here!
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            items(comments) { comment -> CommentItem(comment) }
        }

        // The Input Box - Ensure this is OUTSIDE the LazyColumn but INSIDE the Column
        Surface(modifier = Modifier.fillMaxWidth(), tonalElevation = 8.dp, shadowElevation = 8.dp) {
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Write a comment...") },
                    shape = RoundedCornerShape(24.dp) // Makes it look more like a chat bubble
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (commentText.isNotBlank()) {
                            val user = auth.currentUser
                            val ref = db.collection("suggestions").document(suggestionId).collection("comments").document()

                            val data = hashMapOf(
                                "id" to ref.id,
                                "authorId" to (user?.uid ?: ""),
                                "author" to (user?.email ?: "Anonymous"),
                                "text" to commentText.trim(),
                                "timestamp" to System.currentTimeMillis()
                            )

                            ref.set(data).addOnSuccessListener {
                                commentText = ""
                                db.collection("suggestions").document(suggestionId)
                                    .update("commentCount", FieldValue.increment(1))
                            }
                        }
                    },
                    enabled = commentText.isNotBlank()
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = if(commentText.isNotBlank()) Color(0xFF4191E3) else Color.Gray)
                }
            }
        }
    }
}