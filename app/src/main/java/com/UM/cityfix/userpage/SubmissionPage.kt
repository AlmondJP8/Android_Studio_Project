package com.UM.cityfix.userpage

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream
import android.graphics.Bitmap
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.ui.graphics.Color
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import com.UM.cityfix.components.uploadToCloudinary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun submission(navController: NavHostController? = null, onSuccess: () -> Unit) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Form States
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var locationNameInput by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var currentUserName by remember { mutableStateOf("Anonymous Citizen") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val categories = listOf("Hazards", "Water", "Power Lines", "Roads", "Waste", "Street Lights", "Trees")

    // --- NEW: Helper to convert Camera Bitmap to Uri ---
    fun bitmapToUri(bitmap: Bitmap): Uri {
        val file = File(context.cacheDir, "camera_capture_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return Uri.fromFile(file)
    }

    // --- Launchers ---
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> imageUri = uri }

    // 2. NEW: Camera Launcher
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) { imageUri = bitmapToUri(bitmap) }
    }

    // 3. Permission Launcher (Ensuring Camera is included)
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false

        if (!cameraGranted || !locationGranted) {
            Toast.makeText(context, "Camera and Location permissions are required", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA))
    }

    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val firstName = document.getString("firstName") ?: ""
                        val lastName = document.getString("lastName") ?: ""
                        currentUserName = "$firstName $lastName".trim()
                    }
                }
        }
        // Also keep your permission launcher call here
        permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA))
    }

    // --- Submission Logic ---
    @SuppressLint("MissingPermission")
    fun saveToFirestore(imageUrl: String) {
        // 1. Get the current Auth user
        val currentUser = FirebaseAuth.getInstance().currentUser

        // 2. Fetch the last known location
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            val lat = location?.latitude ?: 0.0
            val lng = location?.longitude ?: 0.0

            // 3. Prepare the data map
            val report = hashMapOf(
                "title" to title.trim(),
                "description" to description.trim(),
                "locationName" to locationNameInput.trim(),
                "category" to selectedCategory,
                "imageUrl" to imageUrl,
                "latitude" to lat,
                "longitude" to lng,
                "status" to "Pending",
                "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                "userId" to currentUser?.uid,
                "authorName" to currentUserName,
                "authorEmail" to (currentUser?.email ?: "No Email")
            )

            // 4. Save to the "Issues" collection
            db.collection("Issues")
                .add(report)
                .addOnSuccessListener {
                    isSubmitting = false
                    Toast.makeText(context, "Issue Reported Successfully!", Toast.LENGTH_SHORT).show()

                    // Reset form or navigate back
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    isSubmitting = false
                    Log.e("FirestoreError", "Error adding document", e)
                    Toast.makeText(context, "Submission failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
        }.addOnFailureListener { e ->
            isSubmitting = false
            Toast.makeText(context, "Could not retrieve location", Toast.LENGTH_SHORT).show()
        }
    }

    fun startCloudinaryUpload() {
        if (imageUri == null || title.isEmpty() || selectedCategory.isEmpty()) {
            Toast.makeText(context, "Please complete the form", Toast.LENGTH_SHORT).show()
            isSubmitting = false
            return
        }

        try {
            // Safe way to get Bitmap from Uri
            val inputStream = context.contentResolver.openInputStream(imageUri!!)
            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)

            val tempFile = File(context.cacheDir, "upload_ready.jpg")
            val out = FileOutputStream(tempFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, out) // Smaller = Faster
            out.close()

            uploadToCloudinary(Uri.fromFile(tempFile), "Reports") { secureUrl ->
                (context as? Activity)?.runOnUiThread {
                    if (secureUrl.isNotEmpty()) {
                        saveToFirestore(secureUrl)
                    } else {
                        isSubmitting = false
                        // Check Logcat for "Cloudinary" tag to see the specific error!
                        Toast.makeText(context, "Upload Failed. Check settings.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } catch (e: Exception) {
            isSubmitting = false
            Toast.makeText(context, "Error processing image", Toast.LENGTH_SHORT).show()
        }
    }

    val gpsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startCloudinaryUpload()
        } else {
            isSubmitting = false
            Toast.makeText(context, "Location must be on to submit", Toast.LENGTH_SHORT).show()
        }
    }

    fun checkLocationSettingsAndSubmit(onReady: () -> Unit) {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).build()
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client = LocationServices.getSettingsClient(context)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            onReady() // GPS is already on
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    // MODIFIED: Use the gpsLauncher instead of context as Activity
                    val intentSenderRequest = androidx.activity.result.IntentSenderRequest
                        .Builder(exception.resolution.intentSender).build()
                    gpsLauncher.launch(intentSenderRequest)
                } catch (sendEx: Exception) {
                    isSubmitting = false
                }
            } else {
                isSubmitting = false
                Toast.makeText(context, "GPS Error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- UI Layout ---
    Scaffold (bottomBar = { UserNavBar(navController = navController) }){ innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
        ) {

            Text("Report an Issue", Modifier.padding(start = 10.dp), style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(15.dp))

            // Text Inputs
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Issue Title") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = locationNameInput, onValueChange = { locationNameInput = it }, label = { Text("Location Name") }, modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(24.dp))

            // Category
            Text("Category", style = MaterialTheme.typography.titleMedium)
            ScrollableRow(categories, selectedCategory) { selectedCategory = it }

            Spacer(modifier = Modifier.height(24.dp))

            // Image Preview
            Text("Evidence", style = MaterialTheme.typography.titleMedium)
            if (imageUri != null) {
                Card(modifier = Modifier.fillMaxWidth().height(200.dp).padding(vertical = 8.dp)) {
                    Image(painter = rememberAsyncImagePainter(imageUri), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { cameraLauncher.launch() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Take Photo")
                }
                Button(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Gallery")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    isSubmitting = true
                    checkLocationSettingsAndSubmit {
                        startCloudinaryUpload()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Text("Submit Report")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScrollableRow(items: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            FilterChip(selected = (item == selected), onClick = { onSelect(item) }, label = { Text(item) })
        }
    }
}
