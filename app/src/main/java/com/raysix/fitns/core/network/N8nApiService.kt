package com.raysix.fitns.core.network

import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface N8nApiService {
    @GET("webhook/health")
    suspend fun health(@Header("Authorization") authorization: String?): Response<Unit>

    @POST("webhook/food/barcode")
    suspend fun findProductByBarcode(
        @Header("Authorization") authorization: String?,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: BarcodeRequest
    ): Response<BarcodeResponse>

    @POST("webhook/food/analyze-image")
    suspend fun analyzeMealImage(
        @Header("Authorization") authorization: String?,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: ImageAnalysisRequest
    ): Response<ImageAnalysisResponse>

    @POST("webhook/nutrition/sync")
    suspend fun syncNutrition(
        @Header("Authorization") authorization: String?,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body payload: RequestBody
    ): Response<Unit>

    @POST("webhook/workout/sync")
    suspend fun syncWorkout(
        @Header("Authorization") authorization: String?,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body payload: RequestBody
    ): Response<Unit>

    @POST("webhook/body-weight/sync")
    suspend fun syncBodyWeight(
        @Header("Authorization") authorization: String?,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body payload: RequestBody
    ): Response<Unit>
}

data class BarcodeRequest(
    val barcode: String,
    val timestamp: String
)

data class BarcodeResponse(
    val found: Boolean,
    val product: RemoteFoodProduct?
)

data class RemoteFoodProduct(
    val barcode: String?,
    val name: String,
    val brand: String?,
    val servingSizeGrams: Double?,
    val nutritionPer100g: RemoteNutritionFacts
)

data class RemoteNutritionFacts(
    val caloriesKcal: Double,
    val proteinGrams: Double,
    val carbohydratesGrams: Double,
    val sugarGrams: Double? = null,
    val fatGrams: Double,
    val saturatedFatGrams: Double? = null,
    val fiberGrams: Double? = null,
    val saltGrams: Double? = null
)

data class ImageAnalysisRequest(
    val imageBase64: String,
    val consentGranted: Boolean,
    val timestamp: String
)

data class ImageAnalysisResponse(
    val items: List<ImageAnalysisItem>,
    val total: RemoteNutritionFacts,
    val disclaimer: String
)

data class ImageAnalysisItem(
    val name: String,
    val estimatedGrams: Double,
    val confidence: Double,
    val nutrition: RemoteNutritionFacts
)
