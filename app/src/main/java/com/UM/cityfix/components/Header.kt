package com.UM.cityfix.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.UM.cityfix.ui.theme.appName

@Composable
fun AdminHeader(
    title: String,
    navController: NavController? = null,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null
) {
    Box(modifier = modifier.Header().fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 25.dp)
        ) {
            // 2. Conditional Back Arrow Logic
            if (onBackClick != null) {
                IconButton(onClick = onBackClick, modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.Black // Or your specific theme color
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(text = title, style = appName, modifier = Modifier.weight(1f, fill = false))

        }
    }
}

