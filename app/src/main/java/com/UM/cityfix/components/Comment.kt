package com.UM.cityfix.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

@Composable
fun CommentScreen(navController: NavHostController, suggestionId: String) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    var commentText by remember { mutableStateOf("") }
    val comments = remember { mutableStateListOf<Comment>() }

    // Listen for comments in the sub-collection
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

    Scaffold(
        topBar = { /* Add a simple "Back" TopAppBar here */ }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // List of comments
            LazyColumn(modifier = Modifier.weight(1f).padding(16.dp)) {
                items(comments) { comment ->
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(comment.author, fontWeight = FontWeight.Bold, color = Color(0xFF1976D2), fontSize = 12.sp)
                        Text(comment.text)
                        HorizontalDivider(modifier = Modifier.padding(top = 8.dp), thickness = 0.5.dp)
                    }
                }
            }

            // Input area at the bottom
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Write a comment...") }
                )
                IconButton(onClick = {
                    if (commentText.isNotBlank()) {
                        val commentId = db.collection("suggestions").document(suggestionId).collection("comments").document().id
                        val newComment = Comment(
                            id = commentId,
                            author = auth.currentUser?.email ?: "Anonymous",
                            text = commentText
                        )
                        // 1. Save the comment
                        db.collection("suggestions").document(suggestionId)
                            .collection("comments").document(commentId).set(newComment)

                        // 2. Increment the comment count on the main suggestion
                        db.collection("suggestions").document(suggestionId)
                            .update("commentCount", FieldValue.increment(1))

                        commentText = ""
                    }
                }) {
                    Icon(Icons.Default.Send, contentDescription = "Send")
                }
            }
        }
    }
}