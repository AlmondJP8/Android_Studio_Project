package com.UM.cityfix.components

import android.net.Uri
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import android.widget.Toast

fun uploadProfileImageToImgBB(uri: Uri, context: android.content.Context, onResult: (String) -> Unit) {
    val client = OkHttpClient()
    val apiKey = "479dc74f47bdcd5f6fda547fec117b2e"

    try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bytes = inputStream?.readBytes()
        inputStream?.close()

        if (bytes == null) {
            onResult("")
            return
        }

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("image", "profile.jpg", bytes.toRequestBody("image/*".toMediaTypeOrNull()))
            .build()

        val request = Request.Builder()
            .url("https://api.imgbb.com/1/upload?key=$apiKey")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Show why it failed in a Toast
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(context, "Network Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                    onResult("")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                if (response.isSuccessful && responseData != null) {
                    val url = JSONObject(responseData).getJSONObject("data").getString("url")
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        onResult(url)
                    }
                } else {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                       Toast.makeText(context, "Server Error: ${response.code}", Toast.LENGTH_SHORT).show()
                        onResult("")
                    }
                }
            }
        })
    } catch (e: Exception) {
        Toast.makeText(context, "File Error: ${e.message}", Toast.LENGTH_SHORT).show()
        onResult("")
    }
}