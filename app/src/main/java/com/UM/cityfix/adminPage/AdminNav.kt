package com.UM.cityfix.adminPage


import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.UM.cityfix.R
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun AdminBottomBar(navController: NavController?, currentRoute: String?) {
    var hasNewReports by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    // 1. Rename this to activeRoute to avoid conflict with the parameter 'currentRoute'
    val navBackStackEntry by navController?.currentBackStackEntryAsState() ?: remember { mutableStateOf(null) }
    val activeRoute = navBackStackEntry?.destination?.route ?: currentRoute

    val sharedPrefs = remember { context.getSharedPreferences("AdminPrefs", Context.MODE_PRIVATE) }

    val itemColors = NavigationBarItemDefaults.colors(
        indicatorColor = Color.Transparent,
        selectedIconColor = Color(0xFF1976D2),
        selectedTextColor = Color(0xFF1976D2),
        unselectedIconColor = Color.Black,
        unselectedTextColor = Color.Black
    )

    // Use activeRoute here
    DisposableEffect(activeRoute) {
        if (activeRoute == "reports") {
            hasNewReports = false
            sharedPrefs.edit().putLong("last_checked", System.currentTimeMillis()).apply()
        }

        val listenerRegistration = db.collection("Issues")
            .whereEqualTo("status", "Pending")
            .addSnapshotListener { snapshots, e ->
                // Check activeRoute inside the listener
                if (e != null || activeRoute == "reports") {
                    hasNewReports = false
                    return@addSnapshotListener
                }

                val lastChecked = sharedPrefs.getLong("last_checked", 0L)
                val hasNewerReport = snapshots?.documents?.any { doc ->
                    val timestamp = doc.getTimestamp("timestamp")?.toDate()?.time ?: 0L
                    timestamp > lastChecked
                } ?: false

                hasNewReports = hasNewerReport
            }

        onDispose {
            listenerRegistration.remove()
        }
    }

    Column {
        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))

        NavigationBar(
            modifier = Modifier.height(90.dp),
            containerColor = Color.White,
            tonalElevation = 0.dp
        ) {
            val navItems = listOf(
                Triple("dashboard", "Dashboard", R.drawable.pic_dashboard),
                Triple("reports", "Reports", R.drawable.pic_reports),
                Triple("map", "Map", R.drawable.pic_map),
                Triple("setting", "Settings", R.drawable.pic_cog)
            )

            navItems.forEach { (route, label, iconRes) ->
                NavigationBarItem(
                    selected = activeRoute == route,
                    onClick = {
                        if (route == "reports") hasNewReports = false

                        if (activeRoute != route) {
                            navController?.navigate(route) {
                                // Added null-safety for the graph to prevent crashes
                                navController.graph.startDestinationRoute?.let { start ->
                                    popUpTo(start) { saveState = true }
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    label = { Text(label, fontSize = 14.sp) },
                    icon = {
                        if (route == "reports") {
                            BadgedBox(
                                badge = {
                                    if (hasNewReports) {
                                        Badge(
                                            containerColor = Color.Red,
                                            modifier = Modifier.size(10.dp).offset(x = 5.dp, y = (-4).dp)
                                        )
                                    }
                                }
                            ) {
                                NavigationIcon(iconRes)
                            }
                        } else {
                            NavigationIcon(iconRes)
                        }
                    },
                    colors = itemColors
                )
            }
        }
    }
}

@Composable
fun NavigationIcon(id: Int) {
    Icon(
        painter = painterResource(id = id),
        contentDescription = null,
        modifier = Modifier.size(25.dp)
    )
}