package com.UM.cityfix.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.UM.cityfix.Greeting
import com.UM.cityfix.Login
import com.UM.cityfix.Signup
import com.UM.cityfix.adminPage.DashBoard
import com.UM.cityfix.adminPage.Issues.Hazards
import com.UM.cityfix.adminPage.Issues.Power
import com.UM.cityfix.adminPage.Issues.Roads
import com.UM.cityfix.adminPage.Issues.StreetLights
import com.UM.cityfix.adminPage.Issues.Trees
import com.UM.cityfix.adminPage.Issues.Waste
import com.UM.cityfix.adminPage.Issues.Water
import com.UM.cityfix.adminPage.MapScreen
import com.UM.cityfix.adminPage.ReportsPage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.UM.cityfix.userpage.CommunityBoard
import com.UM.cityfix.userpage.ProfileScreen
import com.UM.cityfix.userpage.UserNavBar
import com.UM.cityfix.userpage.submission

@Composable
fun AppNav(navController: NavHostController) {
    // Check if a user is already signed in to decide where to start
    val auth = FirebaseAuth.getInstance()
    val startDest = if (auth.currentUser != null) "role_check" else "start"

    NavHost(navController = navController, startDestination = startDest) {

        composable("start") { Greeting(navController) }
        composable("login") { Login(navController) }
        composable("signup") { Signup(navController) }

        composable("role_check") { RoleCheckScreen(navController) }
        composable ("usrnav") { UserNavBar(navController) }

        composable("communitypage") { CommunityBoard(navController) }
        composable("submission") { submission(navController, onSuccess = { navController.popBackStack() }) }
        composable("setting") { ProfileScreen(navController = navController) }

        composable("comments/{suggestionId}", arguments = listOf(navArgument("suggestionId")
        { type = NavType.StringType })) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("suggestionId") ?: ""
            CommentScreen(navController = navController, suggestionId = id)
        }

        composable("dashboard") { DashBoard(navController) }
        composable("reports") { ReportsPage(navController) }
        composable(
            route = "map?lat={lat}&lng={lng}",
            arguments = listOf(
                navArgument("lat") { defaultValue = "0.0" },
                navArgument("lng") { defaultValue = "0.0" }
            )
        ) { backStackEntry ->
            val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull() ?: 0.0
            val lng = backStackEntry.arguments?.getString("lng")?.toDoubleOrNull() ?: 0.0
            MapScreen(lat = lat, lng = lng, navController = navController)
        }

        composable(
            route = "issueDetail/{issueId}",
            arguments = listOf(navArgument("issueId") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("issueId") ?: ""
            IssueDetails(id = id, navController = navController)
        }

        // Category Routes
        composable("power") { Power(navController) }
        composable("water") { Water(navController) }
        composable("lights") { StreetLights(navController) }
        composable("road") { Roads(navController) }
        composable("hazards") { Hazards(navController) }
        composable("trees") { Trees(navController) }
        composable("waste") { Waste(navController) }
    }
}

@Composable
fun RoleCheckScreen(navController: NavHostController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    val role = document.getString("role")
                    if (role == "admin") {
                        navController.navigate("dashboard") {
                            popUpTo("role_check") { inclusive = true }
                        }
                    } else {
                        navController.navigate("communitypage") {
                            popUpTo("role_check") { inclusive = true }
                        }
                    }
                }
                .addOnFailureListener {
                    navController.navigate("login")
                }
        } else { navController.navigate("login") }
    }

    androidx.compose.foundation.layout.Box(
        modifier = Modifier.MainBG(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        // A loading spinner so the screen isn't just blank while checking the database
        androidx.compose.material3.CircularProgressIndicator(
            color = androidx.compose.ui.graphics.Color(0xFF1976D2)
        )
    }
}