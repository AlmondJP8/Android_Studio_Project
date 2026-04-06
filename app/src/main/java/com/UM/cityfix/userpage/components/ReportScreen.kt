package com.UM.cityfix.userpage.components

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.UM.cityfix.components.IssueItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyReportsScreen(navController: NavHostController) {
    val db = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    var reports by remember { mutableStateOf<List<IssueItem>>(emptyList()) }

    LaunchedEffect(userId) {
        if (userId == null) return@LaunchedEffect
        db.collection("Issues")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreError", "Listen failed.", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    // mapNotNull is the key here - it ignores nulls instead of crashing
                    reports = snapshot.documents.mapNotNull { doc ->
                        try {
                            // Manually mapping is often safer than toObject when types are tricky
                            IssueItem(
                                id = doc.id,
                                userId = doc.getString("userId") ?: "",
                                title = doc.getString("title") ?: "No Title",
                                status = doc.getString("status") ?: "Pending",
                                description = doc.getString("description") ?: "",
                                locationName = doc.getString("locationName") ?: "",
                                imageUrl = doc.getString("imageUrl") ?: "",
                                // Get the date correctly from the Firebase Timestamp
                                timestamp = doc.getTimestamp("timestamp")?.toDate()
                            )
                        } catch (e: Exception) {
                            Log.e("MappingError", "Failed to parse ${doc.id}: ${e.message}")
                            null
                        }
                    }
                }
            }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("My Reports") }) }) { padding ->
        if (reports.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No reports found for this user.")
            }
        } else {
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(reports) { Issue ->
                Card(modifier = Modifier.padding(8.dp).fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(Issue.title, fontWeight = FontWeight.Bold)
                            Text("Status: ${Issue.status}", color = Color.Gray)
                        }
                        Button(onClick = { navController.navigate("edit_report/${Issue.id}") }) {
                            Text("Edit")
                        }
                    }
                    }
                }
            }
        }
    }
}

@Composable
fun EditReportScreen(navController: NavHostController, issueId: String) {
    val db = FirebaseFirestore.getInstance()
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    // Fetch existing data
    LaunchedEffect(issueId) {
        db.collection("Issues").document(issueId).get().addOnSuccessListener { doc ->
            title = doc.getString("title") ?: ""
            description = doc.getString("description") ?: ""
        }
    }

    Column(Modifier.padding(16.dp)) {
        Text("Edit Report", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") })

        Button(
            onClick = {
                isSaving = true
                db.collection("Issues").document(issueId)
                    .update(mapOf("title" to title, "description" to description))
                    .addOnSuccessListener {
                        navController.popBackStack()
                        // Show success toast here
                    }
            },
            enabled = !isSaving
        ) {
            if (isSaving) CircularProgressIndicator() else Text("Update Report")
        }
    }
}