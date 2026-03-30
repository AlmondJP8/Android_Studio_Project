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
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.ui.graphics.Color
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority

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
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val categories = listOf("Hazards", "Water", "Power Lines", "Roads", "Waste", "Street  Lights", "Trees")

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
        permissionLauncher.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA
        ))
    }

    // --- Submission Logic ---

    @SuppressLint("MissingPermission")
    fun saveToFirestore(imageUrl: String) {
        // Fetch location before final save
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            val lat = location?.latitude ?: 0.0
            val lng = location?.longitude ?: 0.0

            val report = hashMapOf(
                "title" to title,
                "description" to description,
                "locationName" to locationNameInput,
                "category" to selectedCategory,
                "imageUrl" to imageUrl,
                "latitude" to lat,
                "longitude" to lng,
                "status" to "Pending",
                "timestamp" to com.google.firebase.Timestamp.now()
            )

            // UPDATED: Path changed to "Issues"
            db.collection("Issues").add(report)
                .addOnSuccessListener {
                    isSubmitting = false
                    Toast.makeText(context, "Issue Reported Successfully!", Toast.LENGTH_SHORT).show()
                    onSuccess()
                }
                .addOnFailureListener {
                    isSubmitting = false
                    Toast.makeText(context, "Firestore Error: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    fun uploadToImgBB() {
        if (imageUri == null || title.isEmpty() || selectedCategory.isEmpty()) {
            Toast.makeText(context, "Please complete the form", Toast.LENGTH_SHORT).show()
            return
        }

        isSubmitting = true
        val apiKey = "479dc74f47bdcd5f6fda547fec117b2e"

        // 1. Prepare File
        val inputStream = context.contentResolver.openInputStream(imageUri!!)
        val tempFile = File.createTempFile("upload", ".jpg", context.cacheDir)
        val outputStream = FileOutputStream(tempFile)
        inputStream?.use { input -> outputStream.use { output -> input.copyTo(output) } }

        // 2. Prepare Request
        val client = OkHttpClient()
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("key", apiKey)
            .addFormDataPart("image", tempFile.name, tempFile.asRequestBody("image/jpeg".toMediaTypeOrNull()))
            .build()

        val request = Request.Builder()
            .url("https://api.imgbb.com/1/upload")
            .post(requestBody)
            .build()

        // 3. Execute
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                (context as? Activity)?.runOnUiThread {
                    isSubmitting = false
                    Toast.makeText(context, "Network Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val responseData = it.body?.string()
                    if (it.isSuccessful && responseData != null) {
                        val json = JSONObject(responseData)
                        val url = json.getJSONObject("data").getString("url")

                        // Switch back to Main Thread for UI/Firebase
                        (context as? Activity)?.runOnUiThread {
                            saveToFirestore(url)
                        }
                    } else {
                        (context as? Activity)?.runOnUiThread {
                            isSubmitting = false
                            Toast.makeText(context, "ImgBB Error: Check API Key", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        })
    }

    val gpsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // User clicked "OK", now we can proceed!
            uploadToImgBB()
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

            Text("Report an Urban Issue", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(25.dp))

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
                        // This only runs if GPS is confirmed ON
                        uploadToImgBB()
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
