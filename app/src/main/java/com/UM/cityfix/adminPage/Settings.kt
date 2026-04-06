package com.UM.cityfix.adminPage

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.UM.cityfix.R
import com.UM.cityfix.components.uploadToCloudinary
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.yalantis.ucrop.UCrop
import java.io.File

@Composable
fun Settings(navController: NavHostController) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid
    val context = LocalContext.current

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // State variables
    var profileImageUrl by remember { mutableStateOf("") }
    var globalReportCount by remember { mutableIntStateOf(0) } // Admin sees total reports
    var globalResolvedCount by remember { mutableIntStateOf(0) } // Admin sees total fixed
    var adminName by remember { mutableStateOf("Admin User") }
    var isUploading by remember { mutableStateOf(false) }

    // Fetch Admin Data & System Stats
    LaunchedEffect(userId) {
        if (userId == null) return@LaunchedEffect

        // 1. Fetch Admin Profile Info
        db.collection("users").document(userId).addSnapshotListener { snapshot, _ ->
            if (snapshot != null && snapshot.exists()) {
                val fName = snapshot.getString("firstName") ?: ""
                val lName = snapshot.getString("lastName") ?: ""
                adminName = if (fName.isNotEmpty()) "$fName $lName" else "System Admin"
                profileImageUrl = snapshot.getString("profilePicture") ?: ""
            }
        }

        // 2. Fetch System-Wide Stats (Total vs Resolved)
        db.collection("Issues").addSnapshotListener { result, _ ->
            if (result != null) {
                globalReportCount = result.size()
                globalResolvedCount = result.documents.count {
                    it.getString("status")?.lowercase() == "resolved"
                }
            }
        }
    }
    // 1. Launcher to handle the final Crop Result
    val cropLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val resultUri = UCrop.getOutput(result.data!!)
            resultUri?.let { cropped ->
                isUploading = true

                // Compression logic
                val inputStream = context.contentResolver.openInputStream(cropped)
                val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                val resizedBitmap = android.graphics.Bitmap.createScaledBitmap(originalBitmap, 300, 300, true)
                val compressedFile = File(context.cacheDir, "profile_final.jpg")

                java.io.FileOutputStream(compressedFile).use { out ->
                    resizedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, out)
                }

                // Upload to Cloudinary
                uploadToCloudinary(Uri.fromFile(compressedFile), "Admin_Profiles") { newUrl ->
                    if (newUrl.isNotEmpty()) {
                        saveProfilePhotoToFirestore(newUrl) { success ->
                            isUploading = false
                            if (success) profileImageUrl = newUrl
                        }
                    } else {
                        isUploading = false
                    }
                }
            }
        }
    }

// 2. Launcher to pick the image from Gallery and start UCrop
    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { sourceUri ->
            val destinationUri = Uri.fromFile(File(context.cacheDir, "temp_profile.jpg"))
            val options = UCrop.Options().apply {
                setToolbarColor(android.graphics.Color.parseColor("#1976D2"))
                setToolbarTitle("Adjust Photo")
                setCircleDimmedLayer(true)
            }
            val uCropIntent = UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(1f, 1f)
                .withOptions(options)
                .getIntent(context)

            // This launches the crop activity
            cropLauncher.launch(uCropIntent)
        }
    }

    Scaffold(
        bottomBar = { AdminBottomBar(navController = navController, currentRoute = "settings")}
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8F9FA))
                .verticalScroll(scrollState)
        ) {
            // HEADER SECTION (Admin Branded)
            Box(
                modifier = Modifier.fillMaxWidth().height(250.dp).background(Color(0xFF19212C)), // Darker Admin Theme
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(110.dp)) {
                        // Profile Image Logic
                        Box(
                            contentAlignment = Alignment.BottomEnd,
                            modifier = Modifier.clickable(enabled = !isUploading) { launcher.launch("image/*") }
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(profileImageUrl.ifEmpty { R.drawable.pic_user })
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Admin Photo",
                                modifier = Modifier.size(100.dp).clip(CircleShape).background(Color.White).padding(2.dp),
                                contentScale = ContentScale.Crop
                            )
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Change",
                                modifier = Modifier.size(28.dp).background(Color.White, CircleShape).padding(6.dp),
                                tint = Color.Black
                            )
                        }
                        if (isUploading) CircularProgressIndicator(color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = adminName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)

                    // ADMIN BADGE
                    Surface(
                        color = Color(0xFFFF9800),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = "SYSTEM ADMINISTRATOR",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            }

            // SYSTEM STATS ROW
            Text(
                "System Overview",
                Modifier.padding(start = 20.dp, top = 20.dp),
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Total Issues", globalReportCount.toString(), Modifier.weight(1f))
                StatCard("Resolved", globalResolvedCount.toString(), Modifier.weight(1f))
            }

            // ADMIN SETTINGS LIST
            Text("Management", Modifier.padding(start = 20.dp, bottom = 8.dp), fontWeight = FontWeight.Bold, color = Color.Gray)
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column {
                    ProfileMenuItem(Icons.Default.AdminPanelSettings, "System Logs") {
                        Toast.makeText(context, "Log access restricted", Toast.LENGTH_SHORT).show()
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)

                    ProfileMenuItem(Icons.Default.HelpCenter, "Admin Support") {
                        // Link to developer or documentation
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)

                    ProfileMenuItem(Icons.AutoMirrored.Filled.ExitToApp, "Sign Out", tint = Color.Red) {
                        auth.signOut()
                        navController.navigate("login") { popUpTo(0) { inclusive = true } }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier, shape = RoundedCornerShape(12.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
            Text(text = label, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun ProfileMenuItem(icon: ImageVector, text: String, tint: Color = Color.Black, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = tint)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.LightGray)
        }
    }
}

fun saveProfilePhotoToFirestore(url: String, onComplete: (Boolean) -> Unit) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val db = FirebaseFirestore.getInstance()

    db.collection("users").document(userId)
        .update("profilePicture", url)
        .addOnSuccessListener { onComplete(true) }
        .addOnFailureListener {
            db.collection("users").document(userId)
                .set(mapOf("profilePicture" to url), com.google.firebase.firestore.SetOptions.merge())
                .addOnCompleteListener { task -> onComplete(task.isSuccessful) }
        }
}