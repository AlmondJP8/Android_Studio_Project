package com.UM.cityfix.userpage

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import com.UM.cityfix.R
import com.UM.cityfix.components.uploadToCloudinary
import com.UM.cityfix.userpage.components.ProfileIssueItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.yalantis.ucrop.UCrop
import java.io.File

@Composable
fun ProfileScreen(navController: NavHostController) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid
    val context = LocalContext.current

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // State variables
    var profileImageUrl by remember { mutableStateOf("") }
    var reportCount by remember { mutableIntStateOf(0) }
    var userName by remember { mutableStateOf("City Fixer") }
    var isUploading by remember { mutableStateOf(false) }

    var userIssues by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    val resolvedCount = userIssues.count { it["status"]?.toString()?.lowercase() == "resolved" }

    // Fetch User Data on Load
    LaunchedEffect(userId) {
        if (userId == null) return@LaunchedEffect

        // 1. Fetch User Profile Info (Real-time)
        db.collection("users").document(userId).addSnapshotListener { snapshot, _ ->
            if (snapshot != null && snapshot.exists()) {
                val fName = snapshot.getString("firstName") ?: ""
                val lName = snapshot.getString("lastName") ?: ""
                if (fName.isNotEmpty()) userName = "$fName $lName"
                profileImageUrl = snapshot.getString("profilePicture") ?: ""
            }
        }

        // 2. Fetch Reports (Using lowercase "issues" to match your submission fix)
        db.collection("Issues")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { result, e ->
                if (result != null) {
                    reportCount = result.size()
                    userIssues = result.documents.mapNotNull { it.data }
                }
            }
    }

    // 1. UPDATED Launcher for the Crop Result
    val cropLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val resultUri = UCrop.getOutput(result.data!!)
            resultUri?.let { cropped ->
                isUploading = true

                // --- LOCAL COMPRESSION (Prevents Lag) ---
                val inputStream = context.contentResolver.openInputStream(cropped)
                val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)

                // 300x300 is plenty for a profile circle
                val resizedBitmap = android.graphics.Bitmap.createScaledBitmap(originalBitmap, 300, 300, true)
                val compressedFile = File(context.cacheDir, "profile_final.jpg")

                java.io.FileOutputStream(compressedFile).use { out ->
                    resizedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, out)
                }

                // --- CLOUDINARY UPLOAD ---
                uploadToCloudinary(Uri.fromFile(compressedFile), "Profile") { newUrl ->
                    if (newUrl.isNotEmpty()) {
                        saveProfilePhotoToFirestore(newUrl) { success ->
                            isUploading = false
                            // ...
                            if (success) {
                                profileImageUrl = newUrl
                                (context as? android.app.Activity)?.runOnUiThread {
                                    Toast.makeText(context, "Profile Updated!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    } else {
                        isUploading = false
                        (context as? android.app.Activity)?.runOnUiThread {
                            Toast.makeText(context, "Upload Failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    // 2. Launcher to Pick Image
    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { sourceUri ->
            val destinationUri = Uri.fromFile(File(context.cacheDir, "temp_profile.jpg"))

            val options = UCrop.Options().apply {
                val brandColor = android.graphics.Color.parseColor("#1976D2")
                setToolbarColor(brandColor)
                setStatusBarColor(brandColor)
                setToolbarWidgetColor(android.graphics.Color.WHITE)
                setToolbarTitle("Adjust Photo")
                setHideBottomControls(false)

                // 3. UI Look
                setCircleDimmedLayer(true)
                setShowCropFrame(false)
                setShowCropGrid(false)
            }

            val uCropIntent = UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(1f, 1f)
                .withOptions(options)
                .getIntent(context)
            cropLauncher.launch(uCropIntent)
        }
    }

    // --- UI Layout (Keep your existing Scaffold and StatCards) ---
    Scaffold(
        bottomBar = { UserNavBar(navController = navController)}
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8F9FA))
                .verticalScroll(scrollState)
        ) {
            // HEADER SECTION
            Box(
                modifier = Modifier.fillMaxWidth().height(230.dp).background(Color(0xFF1976D2)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(110.dp)) {
                        Box(
                            contentAlignment = Alignment.BottomEnd,
                            modifier = Modifier.clickable(enabled = !isUploading) { launcher.launch("image/*") }
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(profileImageUrl.ifEmpty { R.drawable.pic_user })
                                    .crossfade(true)
                                    .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                                    .build(),
                                contentDescription = "Profile Picture",
                                modifier = Modifier.size(100.dp).clip(CircleShape).background(Color.White),
                                contentScale = ContentScale.Crop,
                                placeholder = painterResource(R.drawable.pic_user),
                                error = painterResource(R.drawable.pic_user)
                            )
                            if (!isUploading) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    modifier = Modifier.size(30.dp).background(Color.White, CircleShape).padding(6.dp),
                                    tint = Color(0xFF1976D2)
                                )
                            }
                        }
                        if (isUploading) {
                            CircularProgressIndicator(modifier = Modifier.size(40.dp), color = Color.White, strokeWidth = 4.dp)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = userName, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Verified Citizen", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                }
            }

            // STATS ROW
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Reports", reportCount.toString(), Modifier.weight(1f))
                StatCard("Resolved", resolvedCount.toString(), Modifier.weight(1f))
            }

            // MENU LIST
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column {
                    ProfileMenuItem(Icons.Default.History, "My Report History") {
                        coroutineScope.launch { scrollState.animateScrollTo(scrollState.maxValue) }
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                    ProfileMenuItem(Icons.AutoMirrored.Filled.ExitToApp, "Logout", tint = Color.Red) {
                        auth.signOut()
                        navController.navigate("login") { popUpTo(0) { inclusive = true } }
                    }
                }
            }

            // RECENT REPORTS
            Spacer(modifier = Modifier.height(24.dp))
            Text("Recent Reports", modifier = Modifier.padding(horizontal = 24.dp), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            if (userIssues.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Text("No reports found", color = Color.Gray)
                }
            } else {
                // Inside your ProfileScreen loop:
                userIssues.forEach { issue ->
                    ProfileIssueItem(
                        title = issue["title"]?.toString() ?: "General Issue",
                        status = issue["status"]?.toString() ?: "Pending",
                        location = issue["locationName"]?.toString() ?: "No description",
                        imageUrl = issue["imageUrl"]?.toString() ?.replace("/upload/", "/upload/w_200,c_thumb/") ?: ""
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
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