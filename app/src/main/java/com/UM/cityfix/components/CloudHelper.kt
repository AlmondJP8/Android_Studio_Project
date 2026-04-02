package com.UM.cityfix.components

import android.net.Uri
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback

/**
 * Generic Helper to upload any image to a specific Cloudinary folder.
 * @param uri The local URI of the image (usually the cropped/compressed file)
 * @param folderName The folder in Cloudinary (e.g., "Profile", "Suggestion", "Issues")
 * @param onComplete Returns the Secure URL if successful, or an empty string if failed.
 */
fun uploadToCloudinary(
    uri: Uri,
    folderName: String,
    onComplete: (String) -> Unit
) {
    MediaManager.get().upload(uri)
        .unsigned("ml_default") // Ensure this matches your Cloudinary dashboard
        .option("folder", folderName)         // This organizes your Media Library
        .callback(object : UploadCallback {
            override fun onStart(requestId: String) {
                Log.d("Cloudinary", "Upload started for folder: $folderName")
            }

            override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                // You can track progress here if you want to update a progress bar
            }

            override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                val url = resultData["secure_url"].toString()
                Log.d("Cloudinary", "Upload Success: $url")
                onComplete(url)
            }

            override fun onError(requestId: String, error: ErrorInfo) {
                Log.e("Cloudinary", "Upload Error: ${error.description}")
                onComplete("") // Return empty to signal failure
            }

            override fun onReschedule(requestId: String, error: ErrorInfo) {
                Log.e("Cloudinary", "Upload Rescheduled")
            }
        }).dispatch()
}