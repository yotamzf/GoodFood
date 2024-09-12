package com.example.goodfoodapp.dal.services

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.File
import java.io.IOException
import org.json.JSONObject

class ImgurApiService(private val clientId: String) {

    // POST: Upload an image to Imgur
    fun uploadImage(imageFile: File, onSuccess: (String) -> Unit, onError: (String) -> Unit, retries: Int = 3) {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("image", imageFile.name, RequestBody.create("image/*".toMediaTypeOrNull(), imageFile))
            .build()

        val request = Request.Builder()
            .url("https://api.imgur.com/3/upload")
            .addHeader("Authorization", "Client-ID $clientId")
            .post(requestBody)
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (retries > 0) {
                    uploadImage(imageFile, onSuccess, onError, retries - 1) // Retry
                } else {
                    onError(e.message ?: "Error uploading image")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                val imageUrl = extractImageUrl(responseData)
                if (imageUrl != null) {
                    onSuccess(imageUrl)  // Use plain URL instead of obfuscating
                } else {
                    onError("Error parsing image URL")
                }
            }
        })
    }

    // Extract image URL from response JSON
    private fun extractImageUrl(responseData: String?): String? {
        return try {
            val jsonResponse = JSONObject(responseData)
            jsonResponse.getJSONObject("data").getString("link")
        } catch (e: Exception) {
            null
        }
    }
}
