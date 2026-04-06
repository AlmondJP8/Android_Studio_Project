package com.UM.cityfix.userpage

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.UM.cityfix.userpage.components.CommentSheetContent
import com.UM.cityfix.components.MainBG
import com.UM.cityfix.userpage.components.Suggestion
import com.UM.cityfix.userpage.components.SuggestionItem
import com.UM.cityfix.ui.theme.appName
import com.UM.cityfix.userpage.components.saveSuggestionToFirestore
import com.UM.cityfix.userpage.components.uploadToCloudinary
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityBoard(navController: NavHostController? = null) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showComments by remember { mutableStateOf(false) }
    var selectedSuggestionId by remember { mutableStateOf("") }
    var newSuggestionText by remember { mutableStateOf("") }
    val suggestions = remember { mutableStateListOf<Suggestion>() }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
//    var isUploading by remember { mutableStateOf(false) } // To show a loading state

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    LaunchedEffect(Unit) {
        db.collection("suggestions")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val list = snapshot.toObjects(Suggestion::class.java)
                    suggestions.clear()
                    suggestions.addAll(list)
                }
            }
    }

    Scaffold(
        bottomBar = { UserNavBar(navController = navController) }
    ) { innerPadding ->
        Column(Modifier.MainBG().padding(innerPadding)) {
            Text("DashBoard", Modifier.padding(start = 15.dp), style = MaterialTheme.typography.headlineMedium)

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .padding(8.dp)
                    ) {
                        // Image Preview (Stays on top if selected)
                        selectedImageUri?.let { uri ->
                            Box(modifier = Modifier.padding(bottom = 8.dp)) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                IconButton(
                                    onClick = { selectedImageUri = null },
                                    modifier = Modifier.align(Alignment.TopEnd)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. The Input Box (Takes up all middle space)
                            OutlinedTextField(
                                value = newSuggestionText,
                                onValueChange = { newSuggestionText = it },
                                placeholder = { Text("Share an idea...", color = Color.Gray) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(15.dp),
                                maxLines = 3,
                                textStyle = androidx.compose.ui.text.TextStyle(color = Color.Black)
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            // 2. Photo Picker Button
                            IconButton(onClick = { launcher.launch("image/*") }) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = "Add Photo",
                                    tint = if (selectedImageUri != null) Color(0xFF4191E3) else Color.Gray
                                )
                            }

                            // 3. The Send Button
                            IconButton(
                                onClick = {
                                    val authorEmail = auth.currentUser?.email ?: "Anonymous"

                                    if (selectedImageUri != null) {
                                        // Upload to Cloudinary first, then it handles Firestore
                                        uploadToCloudinary(selectedImageUri!!, newSuggestionText, authorEmail)
                                    } else {
                                        // No image? Just save text directly to Firestore
                                        saveSuggestionToFirestore(newSuggestionText, authorEmail, null)
                                    }

                                    newSuggestionText = ""
                                    selectedImageUri = null
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Post",
                                    tint = if (newSuggestionText.isNotBlank() || selectedImageUri != null)
                                        Color(0xFF4191E3) else Color.Gray
                                )
                            }
                        }
                    }
                }

                items(suggestions) { item ->
                    SuggestionItem(
                        item = item,
                        onCommentClick = { id ->
                            selectedSuggestionId = id
                            showComments = true
                        }
                    )
                }
            }
        }
    }

    // Inside CommunityBoard.kt
    if (showComments) {
        ModalBottomSheet(
            onDismissRequest = { showComments = false },
            sheetState = sheetState, // Ensure this has skipPartiallyExpanded = true
            contentWindowInsets = { WindowInsets(0) }, // Removes the gap at the bottom
            containerColor = Color.White,
        ) {
            CommentSheetContent(suggestionId = selectedSuggestionId)
        }
    }
}