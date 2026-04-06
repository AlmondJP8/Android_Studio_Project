package com.UM.cityfix.adminPage

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.UM.cityfix.components.IssueItem
import com.UM.cityfix.components.MainBG
import com.google.firebase.firestore.FirebaseFirestore

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun DashBoard(navController: NavHostController? = null) {
    val db = FirebaseFirestore.getInstance()

    // States for the Dashboard Stats
    var totalIssues by remember { mutableIntStateOf(0) }
    var pendingIssues by remember { mutableIntStateOf(0) }
    var resolvedIssues by remember { mutableIntStateOf(0) }
    var ongoingIssues by remember {mutableIntStateOf(0)}
    var blockedIssues by remember {mutableIntStateOf(0)}
    var allIssues by remember { mutableStateOf(listOf<IssueItem>()) }

    // Fetch counts from Firestore
    LaunchedEffect(Unit) {
        db.collection("Issues").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                totalIssues = snapshot.size()
                pendingIssues = snapshot.documents.count { it.getString("status") == "Pending" }
                ongoingIssues = snapshot.documents.count { it.getString("status") == "Ongoing" }
                resolvedIssues = snapshot.documents.count { it.getString("status") == "Resolved" }
                blockedIssues = snapshot.documents.count { it.getString("status") == "Blocked" }
            }
        }
    }

    Scaffold(
        bottomBar = { AdminBottomBar(navController = navController, currentRoute = "dashboard") }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .MainBG()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Header
            Column(modifier = Modifier.padding(16.dp)) {

                // 2. Welcome Message
                Text(
                    text = "Welcome back, Admin",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "System overview and reports",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 3. Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardStatCard("Total", totalIssues.toString(), Color(0xFF1976D2), Modifier.weight(1f))
                    DashboardStatCard("Pending", pendingIssues.toString(), Color(0xFF757575), Modifier.weight(1f))
                    DashboardStatCard("Ongoing", ongoingIssues.toString(), Color(0xFF2196F3), Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(10.dp))

                // ROW 2: The Action-Required / Finalized Statuses
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardStatCard("Fixed", resolvedIssues.toString(), Color(0xFF388E3C), Modifier.weight(1f))
                    DashboardStatCard("Blocked", blockedIssues.toString(), Color(0xFFD32F2F), Modifier.weight(1f))

                }
            }
                Spacer(modifier = Modifier.height(10.dp))
                LaunchedEffect(Unit) {
                    db.collection("Issues").addSnapshotListener { snapshot, _ ->
                        if (snapshot != null) {
                            allIssues = snapshot.documents.map { doc ->
                                IssueItem(
                                    category = doc.getString("category") ?: "",
                                    status = doc.getString("status") ?: "Pending"
                                    // ... other fields if needed ...
                                )
                            }
                        }
                    }
                }
                CategoryChart(issues = allIssues, navController = navController)
            }
        }
    }

@Composable
fun DashboardStatCard(label: String, count: String, color: Color, modifier: Modifier) {
    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally // Centers the data
        ) {
            Text(
                text = count,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                maxLines = 1
            )
        }
    }
}

@Composable
fun CategoryChart(issues: List<IssueItem>, navController: NavController?) {
    // 1. Group issues by category and count them
    val categoryCounts = issues.groupBy { it.category }
        .mapValues { it.value.size }
        .toList()
        .sortedByDescending { it.second } // Highest count first

    val maxCount = categoryCounts.maxOfOrNull { it.second }?.toFloat() ?: 1f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .padding(10.dp)
    ) {
        Text(
            text = "Issues by Category",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                categoryCounts.forEach { (category, count) ->
                    val categoryName = if (category.isEmpty()) "Uncategorized" else category

                    // 2. Individual Bar Row
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // Optional: Navigate to that specific category
                                navController?.navigate("${categoryName.lowercase()}_issues")
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = categoryName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(text = count.toString(), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // 3. The Actual Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE0E0E0)) // Background of the bar
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = count / maxCount) // Dynamic width
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(
                                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                            colors = listOf(Color(0xFF1976D2), Color(0xFF64B5F6))
                                        )
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}