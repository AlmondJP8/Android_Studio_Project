package com.UM.cityfix.adminPage

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.UM.cityfix.R
import com.UM.cityfix.components.MapSorter
import com.UM.cityfix.components.createCustomMarker
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

data class MapIssue(
    val title: String,
    val point: GeoPoint,
    val category: String,
    val description: String,
    val urgency: String,
    val locationName: String
)

@Composable
fun MapScreen(navController: NavController?, lat: Double, lng: Double) {
    // 1. ALL STATES MUST BE HERE AT THE TOP
    val context = LocalContext.current
    val db = Firebase.firestore // Initialize Firestore
    var firebaseIssues by remember { mutableStateOf(listOf<MapIssue>()) }
    var selectedCategory by remember { mutableStateOf("All") }
    var isMenuExpanded by remember { mutableStateOf(false) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    // ADD THIS FETCH LOGIC BACK IN
    LaunchedEffect(Unit) {
        db.collection("Issues").addSnapshotListener { value, error ->
            if (error != null) return@addSnapshotListener
            if (value != null) {
                firebaseIssues = value.documents.mapNotNull { doc ->
                    val latVal = doc.getDouble("latitude") ?: 0.0
                    val lonVal = doc.getDouble("longitude") ?: 0.0

                    if (latVal != 0.0 && lonVal != 0.0) {
                        MapIssue(
                            // This ensures the dropdown shows the actual Title field
                            title = doc.getString("title") ?: "Untitled Issue",
                            point = GeoPoint(latVal, lonVal),
                            category = doc.getString("category") ?: "",
                            description = doc.getString("description") ?: "",
                            urgency = doc.getString("urgency") ?: "Normal",
                            locationName = doc.getString("locationName") ?: "Unknown Location"
                        )
                    } else null
                }
            }
        }
    }

    val filteredIssues = remember(selectedCategory, firebaseIssues) {
        if (selectedCategory == "All") emptyList()
        else firebaseIssues.filter { it.category == selectedCategory }
    }
    Scaffold(
        bottomBar = { AdminBottomBar(navController = navController, currentRoute = "map") }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {

            AndroidView(
                factory = { ctx ->
                    org.osmdroid.config.Configuration.getInstance().userAgentValue = ctx.packageName
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(17.0)
                        val startPoint =
                            if (lat != 0.0) GeoPoint(lat, lng) else GeoPoint(6.742454, 125.358471)
                        controller.setCenter(startPoint)
                        mapViewRef = this
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    mapViewRef = view
                    view.overlays.clear()
                    val composebg = Color(0xC1FFFFFF).toArgb()

                    // Add markers for all issues
                    firebaseIssues.filter {
                        selectedCategory == "All" || it.category == selectedCategory
                    }.forEach { issue ->
                        val iconRes = when (issue.category) {
                            "Water" -> R.drawable.pic_water
                            "Power Lines" -> R.drawable.pic_bolt
                            "Roads" -> R.drawable.pic_road
                            "Street Lights" -> R.drawable.pic_light
                            "Hazards" -> R.drawable.pic_hazard
                            "Trees" -> R.drawable.pic_trees
                            "Waste" -> R.drawable.pic_waste
                            else -> null
                        }

                        val marker = Marker(view)
                        marker.position = issue.point
                        marker.title = issue.description
                        marker.snippet = "Location: ${issue.locationName}"

                        if (iconRes != null) {
                            marker.icon = createCustomMarker(context, iconRes, composebg, 70)
                        }
                        view.overlays.add(marker)
                    }
                    view.invalidate()
                }
            )

            Column(modifier = Modifier.align(Alignment.TopEnd).padding(top = 16.dp, end = 16.dp)) {

                MapSorter(
                    selectedCategory = selectedCategory,
                    onCategorySelected = {
                        selectedCategory = it
                        isMenuExpanded = false // Close menu when switching categories
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 3. THE BUBBLE: Only show if a category is picked AND an issue exists
                AnimatedVisibility(visible = filteredIssues.isNotEmpty()) {
                    Box {
                        CategoryJumpBubble(
                            category = selectedCategory,
                            count = filteredIssues.size,
                            onClick = { isMenuExpanded = true }
                        )


                        DropdownMenu(
                            expanded = isMenuExpanded,
                            onDismissRequest = { isMenuExpanded = false },
                            modifier = Modifier.width(250.dp).background(Color.White)
                        ) {
                            filteredIssues.forEach { issue ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                issue.title,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                issue.locationName,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Gray
                                            )
                                        }
                                    },
                                    onClick = {
                                        mapViewRef?.controller?.animateTo(issue.point)
                                        mapViewRef?.controller?.setZoom(19.0)
                                        isMenuExpanded = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = Color(0xFF1976D2)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun CategoryJumpBubble(category: String, count: Int, onClick: () -> Unit) { // Removed currentIndex
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp), // Changed to 12.dp for a cleaner dropdown anchor look
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))

            // Simplified label for the dropdown trigger
            Text(
                text = "$category Reports ($count)",
                style = MaterialTheme.typography.labelLarge
            )

            Spacer(modifier = Modifier.width(4.dp))
            // ArrowDropDown is more intuitive for a menu than ChevronRight
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MapPreview() {
}