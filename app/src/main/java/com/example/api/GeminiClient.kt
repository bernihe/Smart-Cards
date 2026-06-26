package com.example.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    data class CardDetails(
        val title: String,
        val address: String,
        val description: String,
        val estimatedPrices: String,
        val alternatives: String
    )

    suspend fun generateCardDetails(name: String, folderName: String): Result<CardDetails> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "GEMINI_API_KEY") {
            return@withContext Result.failure(Exception("Gemini API Key is not set. Please add it via the Secrets panel in Google AI Studio."))
        }

        val prompt = """
            Create a highly detailed, visually descriptive card info about: "$name"
            Category/Context: "$folderName"
            
            Based on this name and context, fetch/generate accurate details:
            1. Title: The formal/neat name.
            2. Address: A precise physical address if it is a physical location. If it is an idea, a project, or abstract, write a brief, realistic execution context or starting location (e.g. "Backyard / Local Home Improvement Store").
            3. Description: A professional, beautiful, and informative brief summary.
            4. Estimated Prices: Accurate entry ticket prices, dinner costs, budget estimates, or project setup/materials cost estimates.
            5. Alternatives: 2-3 similar alternatives, alternative locations, or related starting steps.
            
            Return the response strictly in JSON format as specified.
        """.trimIndent()

        try {
            // Build the standard Gemini request JSON
            val requestJson = JSONObject()
            
            // Contents
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            val partObj = JSONObject()
            partObj.put("text", prompt)
            partsArray.put(partObj)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            requestJson.put("contents", contentsArray)

            // Tools (Search and Maps Grounding)
            val toolsArray = JSONArray()
            val toolSearch = JSONObject()
            toolSearch.put("googleSearch", JSONObject())
            toolsArray.put(toolSearch)
            
            // Adding googleMaps tool as well as requested by metadata
            val toolMaps = JSONObject()
            toolMaps.put("googleMaps", JSONObject())
            toolsArray.put(toolMaps)
            
            requestJson.put("tools", toolsArray)

            // GenerationConfig with schema
            val generationConfig = JSONObject()
            generationConfig.put("responseMimeType", "application/json")
            
            val responseSchema = JSONObject()
            responseSchema.put("type", "OBJECT")
            
            val properties = JSONObject()
            
            val titleProp = JSONObject().put("type", "STRING")
            val addressProp = JSONObject().put("type", "STRING").put("description", "Physical address, or starting context if it's an idea.")
            val descProp = JSONObject().put("type", "STRING").put("description", "A beautiful, rich and informative summary.")
            val priceProp = JSONObject().put("type", "STRING").put("description", "Estimated ticket prices, budget, or costs.")
            val altProp = JSONObject().put("type", "STRING").put("description", "Alternatives, other places, or related steps.")
            
            properties.put("title", titleProp)
            properties.put("address", addressProp)
            properties.put("description", descProp)
            properties.put("estimatedPrices", priceProp)
            properties.put("alternatives", altProp)
            
            responseSchema.put("properties", properties)
            
            val required = JSONArray()
            required.put("title")
            required.put("address")
            required.put("description")
            required.put("estimatedPrices")
            required.put("alternatives")
            
            responseSchema.put("required", required)
            generationConfig.put("responseSchema", responseSchema)
            
            requestJson.put("generationConfig", generationConfig)

            // Log payload for debugging
            Log.d(TAG, "Request payload: ${requestJson.toString(2)}")

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            
            val url = "$BASE_URL?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string()
                Log.d(TAG, "Response code: ${response.code}")
                Log.d(TAG, "Response body: $bodyString")

                if (!response.isSuccessful || bodyString == null) {
                    return@withContext Result.failure(Exception("API request failed with code ${response.code}: ${response.message}"))
                }

                val jsonResponse = JSONObject(bodyString)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    return@withContext Result.failure(Exception("AI did not generate any candidates. Please try again with a more specific name."))
                }

                val candidate = candidates.getJSONObject(0)
                val content = candidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts == null || parts.length() == 0) {
                    return@withContext Result.failure(Exception("AI response empty. Please check your query and try again."))
                }

                val responseText = parts.getJSONObject(0).optString("text")
                if (responseText.isEmpty()) {
                    return@withContext Result.failure(Exception("AI returned empty text. Please try again."))
                }

                // Parse the response schema JSON
                val resultJson = JSONObject(responseText)
                val parsedDetails = CardDetails(
                    title = resultJson.optString("title", name),
                    address = resultJson.optString("address", "N/A"),
                    description = resultJson.optString("description", "No description generated."),
                    estimatedPrices = resultJson.optString("estimatedPrices", "Free / Unknown"),
                    alternatives = resultJson.optString("alternatives", "No alternatives provided.")
                )

                Result.success(parsedDetails)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during generation", e)
            Result.failure(e)
        }
    }
}
