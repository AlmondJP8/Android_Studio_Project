package com.UM.cityfix.components

import android.widget.Toast
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
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

data class IssueItem(
    val id: String = "",
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
fun IssueTabTemplate(
    issues: List<IssueItem>,
    navController: NavController?,
    totalCount: Int, newCount: Int
) {
    var selectedFilter by remember { mutableStateOf("All") }

    // Stats Section
    Row(
        modifier = Modifier.padding(16.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard("Total: $totalCount", Modifier.weight(1f))
        StatCard("New: $newCount", Modifier.weight(1f))
    }

    // Sorter
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
            IssueCard(item){
                navController?.navigate("issueDetail/${item.id}")
            }
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
        "Urgent" -> Color(0xFFD32F2F)   // Strong Red
        "High" -> Color(0xFFFF9800)     // Orange
        "Medium" -> Color(0xFFFBC02D)   // Yellow/Amber
        else -> Color.Gray              // Low or Normal
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title Row
            Text(
                text = if (item.title.isNullOrBlank()) "No Title" else item.title!!,
                style = MaterialTheme.typography.titleLarge, // Slightly larger for titles
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface, // Proper theme color
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Description and Status Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 2, // Prevents long descriptions from breaking the UI
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                StatusBadge(item.status)
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Show the Urgency and Time
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

@Composable
fun IssueDetails(id: String, navController: NavController) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    var issue by remember { mutableStateOf<IssueItem?>(null) }
    var selectedUrgency by remember { mutableStateOf("") }

    fun updateUrgency(newUrgency: String) {
        db.collection("Issues").document(id)
            .update("urgency", newUrgency)
            .addOnSuccessListener {
                Toast.makeText(context, "Urgency updated to $newUrgency", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Update failed", Toast.LENGTH_SHORT).show()
            }
    }

    // Fetch the specific document using the ID passed from the Box click
    LaunchedEffect(id) {
        db.collection("Issues").document(id).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                val item = IssueItem(
                    id = doc.id,
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
            }
        }
    }


    Scaffold() { padding ->
        issue?.let { item ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // 1. Image Card
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

                // 2. Data Details Card
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        // Title
                        Text(
                            text = item.title ?: "No Title",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Location Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    issue?.let { item ->
                                        // Convert to string to prevent the navigation system from
                                        // misinterpreting the decimal points
                                        val lat = item.latitude.toString()
                                        val lng = item.longitude.toString()

                                        // This must match your NavHost: map?lat={lat}&lng={lng}
                                        navController.navigate("map?lat=$lat&lng=$lng")
                                    }
                                }
                                .padding(vertical = 8.dp) // Extra padding makes it easier to tap
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
                                    color = Color(0xFF1976D2), // Blue color hints that it is a link
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

                        // Date Row
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Event, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = formatTimestamp(item.timestamp), style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), thickness = 0.5.dp)

                        // Description Label
                        Text(
                            text = "Description",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        // Description Body
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 24.sp // Adds a bit of "breathing room" to the text
                        )
                    }

                    Column(modifier = Modifier.padding(20.dp)) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                        // --- URGENCY SELECTION ROW ---
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

                                // Color logic for buttons
                                val chipColor = when (level) {
                                    "Immediate" -> Color(0xFFD32F2F) // Red
                                    "Urgent" -> Color(0xFFF57C00)    // Orange
                                    else -> Color(0xFF388E3C)       // Green
                                }

                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedUrgency = level
                                        updateUrgency(level) // Call the function defined above
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