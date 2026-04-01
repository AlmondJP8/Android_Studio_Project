package com.UM.cityfix.userpage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.UM.cityfix.components.CommentSheetContent
import com.UM.cityfix.components.Header
import com.UM.cityfix.components.MainBG
import com.UM.cityfix.components.Suggestion
import com.UM.cityfix.components.SuggestionItem
import com.UM.cityfix.ui.theme.appName
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityBoard(navController: NavHostController? = null) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    val sheetState = rememberModalBottomSheetState()
    var showComments by remember { mutableStateOf(false) }
    var selectedSuggestionId by remember { mutableStateOf("") }
    var newSuggestionText by remember { mutableStateOf("") }
    val suggestions = remember { mutableStateListOf<Suggestion>() }

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
            Row(Modifier.Header().fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Community Board", style = appName)
                Spacer(Modifier.weight(1f))
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = newSuggestionText,
                        onValueChange = { newSuggestionText = it },
                        placeholder = { Text("Share an idea for the city...") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = {
                                if (newSuggestionText.isNotBlank()) {
                                    val id = db.collection("suggestions").document().id
                                    val item = Suggestion(
                                        id = id,
                                        author = auth.currentUser?.email ?: "Anonymous",
                                        content = newSuggestionText,
                                        timestamp = System.currentTimeMillis()
                                    )
                                    db.collection("suggestions").document(id).set(item)
                                    newSuggestionText = ""
                                }
                            }) {
                                Icon(Icons.Default.Send, contentDescription = "Post")
                            }
                        }
                    )
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
            sheetState = sheetState,
            // This removes the "automatic" padding so your sheet can go edge-to-edge
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color.LightGray) }
        ) {
            // Apply the paddings to the first child inside the sheet
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding() // Space for the bottom system bar
                    .imePadding()            // Magic that moves the sheet up for the keyboard
            ) {
                CommentSheetContent(suggestionId = selectedSuggestionId)
            }
        }
    }
}