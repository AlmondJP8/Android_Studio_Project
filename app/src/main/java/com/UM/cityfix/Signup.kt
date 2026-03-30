package com.UM.cityfix

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.UM.cityfix.components.MainBG
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun Signup(navController: NavHostController? = null) {

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

// States for Input Fields
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }

    // Success Dialog logic
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = "Account Created!") },
            text = { Text("Welcome to CityFix! You can now log in to report urban issues.") },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    navController?.navigate("login") {
                        popUpTo("signup") { inclusive = true }
                    }
                }) {
                    Text("OK", color = Color(0xFF1976D2), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Column(
        modifier = Modifier.MainBG().fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.1f))

        Image(
            painter = painterResource(id = R.drawable.pic_logo),
            contentDescription = "Logo",
            modifier = Modifier.size(100.dp).clip(CircleShape)
        )

        Spacer(modifier = Modifier.height(16.dp))

        ElevatedCard(
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth(0.85f),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "CityFix",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1976D2)
                )
                Text(text = "Create an account to continue", color = Color.Gray)

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("First Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Last Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val trimmedEmail = email.trim()
                        val trimmedPassword = password.trim()

                        if (trimmedEmail.isNotEmpty() && trimmedPassword.isNotEmpty()) {
                            // 1. Create User in Firebase Auth
                            auth.createUserWithEmailAndPassword(trimmedEmail, trimmedPassword)
                                .addOnSuccessListener { result ->
                                    val userId = result.user?.uid

                                    // 2. Prepare user data for Firestore
                                    val userMap = hashMapOf(
                                        "firstName" to firstName,
                                        "lastName" to lastName,
                                        "email" to trimmedEmail,
                                        "createdAt" to System.currentTimeMillis(),
                                        "role" to "user"
                                    )

                                    // 3. Save to "users" collection using the UID as the Document ID
                                    if (userId != null) {
                                        db.collection("users").document(userId)
                                            .set(userMap)
                                            .addOnSuccessListener {
                                                showDialog = true // Trigger your success dialog
                                            }
                                            .addOnFailureListener { e ->
                                                // Handle Firestore error (e.g., show a Toast)
                                            }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    // Handle Auth error (e.g., email already exists)
                                }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) { Text("Sign Up", fontWeight = FontWeight.Bold, color = Color.White) }

                Spacer(modifier = Modifier.height(16.dp))

                Row {
                    Text("Already have an Account? ")
                    Text(
                        text = "Log In",
                        color = Color(0xFF1976D2),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { navController?.navigate("login") }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.weight(0.1f))
    }
}


