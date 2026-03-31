package com.UM.cityfix.components

import android.net.Uri
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException

fun uploadToCloudinary(fileUri: Uri, context: android.content.Context, onComplete: (String) -> Unit) {
    val client = OkHttpClient()

    // 1. Get the file from URI safely
    val file = File(context.cacheDir, "temp_upload_${System.currentTimeMillis()}.jpg")
    try {
        context.contentResolver.openInputStream(fileUri)?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    } catch (e: Exception) {
        onComplete("")
        return
    }

    // 2. Setup your Cloudinary credentials
    val cloudName = "dr4swrogh" // <--- UPDATE THIS
    val uploadPreset = "ml_default" // <--- UPDATE THIS

    val requestBody = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart("file", file.name, file.asRequestBody("image/jpeg".toMediaTypeOrNull()))
        .addFormDataPart("upload_preset", uploadPreset)
        .build()

    val request = Request.Builder()
        .url("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
        .post(requestBody)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            android.util.Log.e("Cloudinary", "Network Error: ${e.message}")
            onComplete("")
        }

        override fun onResponse(call: Call, response: Response) {
            val body = response.body?.string()
            if (response.isSuccessful && body != null) {
                val json = JSONObject(body)
                onComplete(json.getString("secure_url"))
            } else {
                // THIS WILL TELL YOU THE REAL ERROR IN LOGCAT
                android.util.Log.e("Cloudinary", "Upload Failed: $body")
                onComplete("")
            }
        }
    })
}