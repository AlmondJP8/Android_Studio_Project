package com.UM.cityfix.adminPage

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUserManagementScreen(navController: NavHostController) {
    val db = FirebaseFirestore.getInstance()
    var userList by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }

    // Fetch all users
    LaunchedEffect(Unit) {
        db.collection("users").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                userList = snapshot.documents.map { it.data?.plus("id" to it.id) ?: emptyMap() }
            }
        }
    }

    val filteredUsers = userList.filter {
        val name = "${it["firstName"]} ${it["lastName"]}".lowercase()
        name.contains(searchQuery.lowercase()) || (it["email"] as? String)?.contains(searchQuery) == true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("User Management", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White, // Match your clean design
                    navigationIconContentColor = Color.Black,
                    titleContentColor = Color.Black
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(Color(0xFFF5F7F9))) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                // ... other properties
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Color(0xFF1976D2),
                    unfocusedBorderColor = Color.Gray,
                )
            )

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredUsers) { userData ->
                    UserRoleCard(userData)
                }
            }
        }
    }
}

@Composable
fun UserRoleCard(userData: Map<String, Any>) {
    val db = FirebaseFirestore.getInstance()
    val userId = userData["id"] as String
    val currentRole = userData["role"] as? String ?: "User"
    var expanded by remember { mutableStateOf(false) }

    val roleColor = when (currentRole) {
        "admin" -> Color(0xFFD32F2F) // Red for Admin
//        "Moderator" -> Color(0xFFF57C00) // Orange
        else -> Color(0xFF1976D2) // Blue for standard User
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User Initials Avatar
            Box(
                modifier = Modifier.size(50.dp).background(roleColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (userData["firstName"] as? String)?.take(1) ?: "U",
                    color = roleColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${userData["firstName"]} ${userData["lastName"]}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = userData["email"] as? String ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // Role Dropdown selector
            Box {
                Surface(
                    onClick = { expanded = true },
                    color = roleColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, roleColor.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(currentRole, color = roleColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = roleColor)
                    }
                }

                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    val roles = listOf("user", "admin")
                    roles.forEach { role ->
                        DropdownMenuItem(
                            text = { Text(role) },
                            onClick = {
                                db.collection("users").document(userId).update("role", role)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}