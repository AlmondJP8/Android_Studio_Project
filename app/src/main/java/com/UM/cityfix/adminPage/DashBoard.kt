package com.UM.cityfix.adminPage

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.UM.cityfix.components.Header
import com.UM.cityfix.components.MainBG
import com.UM.cityfix.components.button
import com.UM.cityfix.ui.theme.appName
import com.UM.cityfix.ui.theme.buttonText
import com.google.firebase.auth.FirebaseAuth

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun DashBoard(navController: NavHostController? = null){
    val auth = FirebaseAuth.getInstance()

    Scaffold(
        bottomBar = { AdminBottomBar(navController = navController, currentRoute = "dashboard") }
    ) { innerPadding ->
        Column(Modifier.MainBG().padding(innerPadding)) {
            //header
            Row(
                Modifier.Header().fillMaxWidth().padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Community Board", style = appName)
                Spacer(Modifier.weight(1f))

                // LOGOUT BUTTON
                Text(
                    "Logout",
                    Modifier.button().clickable {
                        auth.signOut()
                        navController?.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    style = buttonText
                )
            }
        }
    }
}