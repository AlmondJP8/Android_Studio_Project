package com.UM.cityfix.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

fun uploadProfileImageToImgBB(uri: Uri, context: Context, onResult: (String) -> Unit) {
    val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    val apiKey = "479dc74f47bdcd5f6fda547fec117b2e"

    Thread {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) {
                Handler(Looper.getMainLooper()).post { onResult("") }
                return@Thread
            }

            // Compression to make upload faster and prevent timeouts
            val outputStream = ByteArrayOutputStream()
            originalBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val compressedBytes = outputStream.toByteArray()

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "image",
                    "profile.jpg",
                    compressedBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                )
                .build()

            val request = Request.Builder()
                .url("https://api.imgbb.com/1/upload?key=$apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Handler(Looper.getMainLooper()).post {
                        Log.e("UPLOAD_ERROR", "Network failure: ${e.message}")
                        onResult("")
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseData = response.body?.string()
                    if (response.isSuccessful && responseData != null) {
                        try {
                            val jsonResponse = JSONObject(responseData)
                            // Use display_url for a direct image link
                            val directImageUrl = jsonResponse.getJSONObject("data").getString("display_url")
                            Handler(Looper.getMainLooper()).post { onResult(directImageUrl) }
                        } catch (e: Exception) {
                            Handler(Looper.getMainLooper()).post { onResult("") }
                        }
                    } else {
                        Handler(Looper.getMainLooper()).post { onResult("") }
                    }
                }
            })
        } catch (e: Exception) {
            Handler(Looper.getMainLooper()).post { onResult("") }
        }
    }.start()
}