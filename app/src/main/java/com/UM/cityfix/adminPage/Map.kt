package com.UM.cityfix.adminPage

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    val db = Firebase.firestore
    var firebaseIssues by remember { mutableStateOf(listOf<MapIssue>()) }
    var selectedCategory by remember { mutableStateOf("All") }

    // FETCH DATA FROM FIREBASE
    LaunchedEffect(Unit) {
        db.collection("Issues").addSnapshotListener { value, error ->
            if (error != null) return@addSnapshotListener
            if (value != null) {
                firebaseIssues = value.documents.mapNotNull { doc ->
                    val latRaw = doc.get("latitude")
                    val lonRaw = doc.get("longitude")
                    val latVal = (latRaw as? Number)?.toDouble() ?: 0.0
                    val lonVal = (lonRaw as? Number)?.toDouble() ?: 0.0

                    if (latVal != 0.0 && lonVal != 0.0) {
                        MapIssue(
                            title = doc.getString("description") ?: "No Desc",
                            point = GeoPoint(latVal, lonVal),
                            category = doc.getString("category") ?: "",
                            description = doc.getString("description") ?: "",
                            urgency = doc.getString("urgency") ?: "Normal",
                            locationName = doc.getString("location") ?: "Unknown"
                        )
                    } else null
                }
            }
        }
    }

    Scaffold(
        bottomBar = { AdminBottomBar(navController = navController, currentRoute = "map") }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    // 1. Properly initialize OSM Configuration
                    org.osmdroid.config.Configuration.getInstance().userAgentValue = ctx.packageName

                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)

                        // 2. Use the lat/lng passed from IssueDetails!
                        controller.setZoom(17.0)
                        val startPoint = if (lat != 0.0) GeoPoint(lat, lng) else GeoPoint(6.742454, 125.358471)
                        controller.setCenter(startPoint)
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { mapView ->
                    mapView.overlays.clear()
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

                        val marker = Marker(mapView)
                        marker.position = issue.point
                        marker.title = issue.description
                        marker.snippet = "Location: ${issue.locationName}"

                        if (iconRes != null) {
                            marker.icon = createCustomMarker(context, iconRes, composebg, 70)
                        }
                        mapView.overlays.add(marker)
                    }
                    mapView.invalidate()
                }
            )

            MapSorter(
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MapPreview() {
}