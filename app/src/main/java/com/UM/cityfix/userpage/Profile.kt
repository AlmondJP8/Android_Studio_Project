package com.UM.cityfix.userpage

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

@Composable
fun ProfilePlaceholderScreen() {
    val context = LocalContext.current

    // LaunchedEffect runs as soon as this screen is "shown"
    LaunchedEffect(Unit) {
        Toast.makeText(context, "Profile feature coming soon!", Toast.LENGTH_SHORT).show()
    }

    // Keep the screen empty or show a simple "Coming Soon" text
//    Box(modifier = Modifier.fillMaxSize()) {
//        // You can leave this empty so it looks like it's loading or just a blank scaffold
//    }
}