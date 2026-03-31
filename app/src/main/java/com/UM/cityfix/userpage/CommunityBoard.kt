package com.UM.cityfix.userpage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
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
        Column(Modifier.MainBG()) {
            //header
            Row(Modifier.Header().fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Community Board", style = appName)
                Spacer(Modifier.weight(1f))
            }
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // INPUT SECTION
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
                                        content = newSuggestionText
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

                // LIST SECTION
                items(suggestions) { item ->
                    SuggestionItem(item = item, navController = navController)
                }
            }
        }
    }
}