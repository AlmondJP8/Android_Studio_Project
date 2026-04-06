package com.UM.cityfix.adminPage.Issues

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import com.UM.cityfix.components.AdminHeader
import com.UM.cityfix.components.IssueItem
import com.UM.cityfix.components.IssueTabTemplate
import com.UM.cityfix.components.MainBG
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore

// --- THE RECYCLABLE TEMPLATE ---
@Composable
fun Trees(navController: NavController?) {
    val db = com.google.firebase.Firebase.firestore
    var TreeData by remember { mutableStateOf(listOf<IssueItem>()) }
    var lastCheckTime by remember { mutableLongStateOf(0L) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        // Get the last time admin viewed stats
        db.collection("AdminView").document("usage_stats")
            .get().addOnSuccessListener { doc ->
                lastCheckTime = doc.getTimestamp("viewstats")?.toDate()?.time ?: 0L
            }

        // Listen to "Issues" collection
        db.collection("Issues")
            .whereEqualTo("category", "Trees")
            .addSnapshotListener { value, error ->
                if (error == null && value != null) {
                    TreeData = value.documents.map { doc ->
                        val rawName = doc.getString("authorName")
                        IssueItem(
                            id = doc.id,
                            title = doc.getString("title") ?: "No Title",
                            authorName = rawName ?: "Anonymous",
                            description = doc.getString("description") ?: "No Description",
                            locationName = doc.getString("locationName") ?: "Unknown Location",
                            status = doc.getString("status") ?: "Pending",
                            urgency = doc.getString("urgency") ?: "Normal",
                            timestamp = doc.getTimestamp("timestamp")?.toDate(),
                            imageUrl = doc.getString("imageUrl") ?: "",
                            // --- ADD THESE TWO LINES ---
                            latitude = doc.getDouble("latitude") ?: 0.0,
                            longitude = doc.getDouble("longitude") ?: 0.0
                        )
                    }
                }
                isLoading = false
            }
    }

    DisposableEffect(Unit) {
        onDispose {
            val now = com.google.firebase.Timestamp.now()
            db.collection("AdminView").document("usage_stats")
                .set(mapOf("viewstats" to now), SetOptions.merge())
        }
    }
    // We use ?.time to get the Long value, and ?: 0L to handle nulls
    val newIssuesCount = TreeData.count { (it.timestamp?.time ?: 0L) > lastCheckTime }

    Scaffold(
        topBar = { AdminHeader(title = "Trees Reports", navController = navController, onBackClick = { navController?.popBackStack() }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .MainBG()
        ) {
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                // Pass the data to the template
                IssueTabTemplate(
                    totalCount = TreeData.size,
                    newCount = newIssuesCount,
                    issues = TreeData,
                    navController = navController
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TreePreview() {
    Trees(navController = null)
}