package com.UM.cityfix.adminPage

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.UM.cityfix.R
import com.UM.cityfix.components.GridConfig2
import com.UM.cityfix.components.MainBG
import com.UM.cityfix.ui.theme.gridText
import com.google.firebase.firestore.FirebaseFirestore

data class DashboardItem(
    val title: String,
    val iconRes: Int,
    val route: String,
    val color: Color,
    val isBadgeVisible: Boolean = false
)

@Composable
fun ReportsPage(navController: NavController?) {
    val isPreview = LocalInspectionMode.current

    val db = FirebaseFirestore.getInstance()
    var activeCategories by remember { mutableStateOf(setOf<String>()) }
    var readCategories by remember { mutableStateOf(setOf<String>()) }

    // Use 'by' and 'value' properly for the state
    val navBackStackEntry by if (isPreview) {
        remember { mutableStateOf(null) }
    } else {
        navController?.currentBackStackEntryAsState() ?: remember { mutableStateOf(null) }
    }

    val currentRoute = navBackStackEntry?.destination?.route ?: "reports"

    // Listen to the database for ANY pending reports
    LaunchedEffect(Unit) {
        db.collection("Issues")
            .whereEqualTo("status", "Pending")
            .addSnapshotListener { snapshots, _ ->
                activeCategories = snapshots?.documents
                    ?.mapNotNull { it.getString("category")?.lowercase() }
                    ?.toSet() ?: emptySet()
            }
    }

    val dashboardItems = remember(activeCategories, readCategories) {
        listOf(
            DashboardItem("Power", R.drawable.pic_bolt, "power", Color(0xFFFFB300),
                activeCategories.contains("power") && !readCategories.contains("power")),
            DashboardItem("Water", R.drawable.pic_water, "water", Color(0xFF0288D1),
                activeCategories.contains("water") && !readCategories.contains("water")),
            DashboardItem("Lights", R.drawable.pic_light, "lights", Color(0xFFFBC02D),
                activeCategories.contains("lights") && !readCategories.contains("lights")),
            DashboardItem("Streets", R.drawable.pic_road, "road", Color(0xFF455A64),
                activeCategories.contains("roads") && !readCategories.contains("roads")),
            DashboardItem("Hazards", R.drawable.pic_hazard, "hazards", Color(0xFFE64A19),
                activeCategories.contains("hazards") && !readCategories.contains("hazards")),
            DashboardItem("Trees", R.drawable.pic_trees, "trees", Color(0xFF388E3C),
                activeCategories.contains("trees") && !readCategories.contains("trees")),
            DashboardItem("Waste", R.drawable.pic_waste, "waste", Color(0xFF8D6E63),
                activeCategories.contains("waste") && !readCategories.contains("waste"))
        )
    }
    Scaffold(bottomBar = { AdminBottomBar(navController = navController, currentRoute = currentRoute) }
    ) { innerPadding ->
        Column(modifier = Modifier.MainBG().fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally)
        {
            Text(
                text = "Reports",
                Modifier.padding(20.dp),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Column(
                modifier = Modifier
                    .padding(bottom = innerPadding.calculateBottomPadding()) // Respect the bottom bar
                    .padding(horizontal = 16.dp), // Keep sides clean
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // The Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(GridConfig2.COLUMNS),
                    contentPadding = PaddingValues(top = 10.dp, bottom = 20.dp),
                    verticalArrangement = GridConfig2.VERTICAL_SPACING,
                    horizontalArrangement = GridConfig2.HORIZONTAL_SPACING,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(dashboardItems) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .clickable { navController?.navigate(item.route) },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp))
                        {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    // Inside your items(dashboardItems) block:
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {

                                            // --- WRAP ICON IN BADGEDBOX ---
                                            BadgedBox(
                                                badge = {
                                                    if (item.isBadgeVisible) {
                                                        Badge(
                                                            containerColor = Color.Red,
                                                            modifier = Modifier.size(8.dp).offset(x = 2.dp, y = (-2).dp)
                                                        )
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = item.iconRes),
                                                    contentDescription = item.title,
                                                    modifier = Modifier.size(35.dp),
                                                    tint = Color.Unspecified
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(text = item.title, style = gridText, fontSize = 14.sp, color = item.color)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }
    }
}

@Preview(showBackground = true, name = "Report Page")
@Composable
fun PreviewReports(){
    ReportsPage(navController = null)
}