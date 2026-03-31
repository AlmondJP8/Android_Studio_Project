package com.UM.cityfix

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.UM.cityfix.components.MainBG
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun Login(navController: NavHostController? = null) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

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

                Text(text = "Log in to continue", color = Color.Gray)

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val trimmedEmail = email.trim()
                        val trimmedPassword = password.trim()

                        if (trimmedEmail.isNotEmpty() && trimmedPassword.isNotEmpty()) {
                            isLoading = true
                            auth.signInWithEmailAndPassword(trimmedEmail, trimmedPassword)
                                .addOnSuccessListener { result ->
                                    val uid = result.user?.uid
                                    if (uid != null) {
                                        // Fetch role to decide where to navigate
                                        db.collection("users").document(uid).get()
                                            .addOnSuccessListener { doc ->
                                                isLoading = false
                                                val role = doc.getString("role") ?: "user"
                                                val target = if (role == "admin") "dashboard" else "communitypage"
                                                
                                                navController?.navigate(target) {
                                                    popUpTo("login") { inclusive = true }
                                                }
                                            }
                                            .addOnFailureListener {
                                                isLoading = false
                                                // Fallback if user doc doesn't exist
                                                navController?.navigate("communitypage")
                                            }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    isLoading = false
                                    Toast.makeText(context, "Login Error: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                        } else {
                            Toast.makeText(context, "Please enter email and password", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Text("LOG IN", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row {
                    Text(text = "Don't have an account? ", color = Color.Gray)
                    Text(
                        text = "Sign Up",
                        color = Color(0xFF1976D2),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            navController?.navigate("signup")
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.weight(0.1f))
    }
}
