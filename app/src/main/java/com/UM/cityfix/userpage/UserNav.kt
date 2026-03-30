package com.UM.cityfix.userpage

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import com.UM.cityfix.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun UserNavBar(navController: NavHostController?) {
    val navBackStackEntry by navController?.currentBackStackEntryAsState() ?: remember { mutableStateOf(null) }
    val activeRoute = navBackStackEntry?.destination?.route

    val itemColors = NavigationBarItemDefaults.colors(
        indicatorColor = Color(0xFFE3F2FD),
        selectedIconColor = Color(0xFF1976D2),
        selectedTextColor = Color(0xFF1976D2),
        unselectedIconColor = Color.Black,
        unselectedTextColor = Color.Black
    )

    Column {
        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.3f))

        NavigationBar(
            modifier = Modifier.height(90.dp),
            containerColor = Color.White,
            tonalElevation = 0.dp
        ) {
            val navItems = listOf(
                Triple("communitypage", "Community", R.drawable.pic_community),
                Triple("submission", "Submit Ticket", R.drawable.pic_ticket),
                Triple("setting", "Profile", R.drawable.pic_user)
            )

            navItems.forEach { (route, label, iconRes) ->
                NavigationBarItem(
                    selected = activeRoute == route,
                    onClick = {
                        if (activeRoute != route) {
                            navController?.let { controller -> // Safely use the controller
                                controller.navigate(route) {
                                    // Pop up to the start destination to avoid building up a large stack
                                    controller.graph.startDestinationRoute?.let { start ->
                                        popUpTo(start) { saveState = true }
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    },
                    label = { Text(label, fontSize = 12.sp) },
                    icon = { Navigationicon(iconRes) },
                    colors = itemColors
                )
            }
        }
    }
}

@Composable
fun Navigationicon(id: Int) {
    Icon(
        painter = painterResource(id = id),
        contentDescription = null,
        modifier = Modifier.size(25.dp)
    )
}