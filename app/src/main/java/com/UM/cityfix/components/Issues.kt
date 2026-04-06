package com.UM.cityfix.components

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Updated Data Class to include userId
data class IssueItem(
    val id: String = "",
    val userId: String = "",
    val authorName: String = "",
    val authorEmail: String = "",
    val title: String? = "",
    val description: String = "",
    val locationName: String = "",
    val status: String = "Pending",
    val timestamp: Long = 0L,
    val urgency: String = "Medium",
    val imageUrl: String = "",
    val category: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

fun formatTimestamp(milliseconds: Long): String {
    if (milliseconds == 0L) return "Pending..."
    val sdf = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
    val date = Date(milliseconds)
    return sdf.format(date)
}

@Composable
fun IssueTabTemplate(issues: List<IssueItem>, navController: NavController?, totalCount: Int, newCount: Int)
{
    var selectedFilter by remember { mutableStateOf("All") }

    Column {
        // Stats Section
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard("Total: $totalCount", Modifier.weight(1f))
            StatCard("New: $newCount", Modifier.weight(1f))
        }

        // Sorter (Make sure this component is defined in your project)
        IssueSorterBar(
            selectedCategory = selectedFilter,
            onCategorySelected = { selectedFilter = it }
        )

        // The Filtered List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val filteredList = if (selectedFilter == "All") issues
            else issues.filter { it.status == selectedFilter }

            items(filteredList) { item ->
                IssueCard(item) {
                    navController?.navigate("issueDetail/${item.id}")
                }
            }
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
fun StatCard(text: String, modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun IssueCard(item: IssueItem, onClick: () -> Unit) {
    val urgencyColor = when (item.urgency) {
        "Urgent" -> Color(0xFFD32F2F)
        "High" -> Color(0xFFFF9800)
        "Medium" -> Color(0xFFFBC02D)
        else -> Color.Gray
    }
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title
            Text(
                text = if (item.title.isNullOrBlank()) "No Title" else item.title!!,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(5.dp))

            // User Name Row (Fixed Syntax)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary // Makes the person icon blue
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = item.authorName.ifEmpty { "" }, // FIXED HERE
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Description and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                StatusBadge(item.status)
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Urgency and Time
            Row {
                Text(
                    text = item.urgency,
                    style = MaterialTheme.typography.bodySmall,
                    color = urgencyColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = " • ${formatTimestamp(item.timestamp)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Location: ${item.locationName}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.DarkGray
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssueDetails(id: String, navController: NavController) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    var issue by remember { mutableStateOf<IssueItem?>(null) }
    var selectedUrgency by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf("") } // NEW STATE

    // --- HELPER FUNCTIONS ---
    fun updateField(fieldName: String, newValue: String) {
        db.collection("Issues").document(id)
            .update(fieldName, newValue)
            .addOnSuccessListener {
                Toast.makeText(context, "$fieldName updated to $newValue", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Update failed", Toast.LENGTH_SHORT).show()
            }
    }

    LaunchedEffect(id) {
        db.collection("Issues").document(id).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                val item = IssueItem(
                    id = doc.id,
                    userId = doc.getString("userId") ?: "",
                    authorName = doc.getString("authorName") ?: "Anonymous Citizen",
                    authorEmail = doc.getString("authorEmail") ?: "No Email",
                    title = doc.getString("title") ?: "No Title",
                    description = doc.getString("description") ?: "No Description",
                    locationName = doc.getString("locationName") ?: "Unknown Location",
                    status = doc.getString("status") ?: "Pending",
                    urgency = doc.getString("urgency") ?: "Normal",
                    imageUrl = doc.getString("imageUrl") ?: "",
                    timestamp = doc.getTimestamp("timestamp")?.toDate()?.time ?: 0L,
                    latitude = doc.getDouble("latitude") ?: 0.0,
                    longitude = doc.getDouble("longitude") ?: 0.0
                )
                issue = item
                selectedUrgency = item.urgency
                selectedStatus = item.status // Initialize status state
            }
        }
    }

    Scaffold { padding ->
        issue?.let { item ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().height(250.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = item.title ?: "No Title",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Inside IssueDetails Column, below the Title
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = item.authorName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                Text(text = item.authorEmail, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        Text(
                            text = "Update Status",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val lat = item.latitude.toString()
                                    val lng = item.longitude.toString()
                                    navController.navigate("map?lat=$lat&lng=$lng")
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = "Location Pin",
                                tint = Color(0xFF1976D2),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = item.locationName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color(0xFF1976D2),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Tap to view on map",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Event, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = formatTimestamp(item.timestamp), style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), thickness = 0.5.dp)

                        Text(
                            text = "Description",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 24.sp
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Update Status",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val statusLevels = listOf("Pending", "Ongoing", "Resolved", "Blocked")
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            statusLevels.forEach { status ->
                                val isSelected = selectedStatus == status
                                val statusColor = when (status) {
                                    "Resolved" -> Color(0xFF388E3C) // Green
                                    "Ongoing" -> Color(0xFF1976D2)  // Blue
                                    "Blocked" -> Color(0xFFD32F2F)  // Red
                                    else -> Color(0xFF757575)       // Grey for Pending
                                }

                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedStatus = status
                                        updateField("status", status)
                                    },
                                    label = { Text(status) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = statusColor,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                        Text(
                            text = "Set Urgency",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val urgencyLevels = listOf("Normal", "Urgent", "Immediate")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            urgencyLevels.forEach { level ->
                                val isSelected = selectedUrgency == level
                                val chipColor = when (level) {
                                    "Immediate" -> Color(0xFFD32F2F)
                                    "Urgent" -> Color(0xFFF57C00)
                                    else -> Color(0xFF388E3C)
                                }

                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedUrgency = level
                                        updateField("urgency", level)
                                    },
                                    label = { Text(level) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = chipColor,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}