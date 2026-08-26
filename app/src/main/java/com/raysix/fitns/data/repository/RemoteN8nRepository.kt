package com.raysix.fitns.data.repository

import com.raysix.fitns.core.model.AppError
import com.raysix.fitns.core.model.AppResult
import com.raysix.fitns.core.network.N8nApiService
import com.raysix.fitns.core.network.N8nServiceFactory
import com.raysix.fitns.core.network.BarcodeRequest
import com.raysix.fitns.core.network.ImageAnalysisItem
import com.raysix.fitns.core.network.ImageAnalysisRequest
import com.raysix.fitns.core.network.RemoteFoodProduct
import com.raysix.fitns.core.network.RemoteNutritionFacts
import com.raysix.fitns.domain.repository.N8nRepository
import com.raysix.fitns.domain.model.FoodProductLookup
import com.raysix.fitns.domain.model.MealAnalysisItem
import com.raysix.fitns.domain.model.MealAnalysisResult
import com.raysix.fitns.domain.model.NutritionFacts
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

class RemoteN8nRepository @Inject constructor(
    private val serviceFactory: N8nServiceFactory
) : N8nRepository {
    override suspend fun testConnection(baseUrl: String, bearerToken: String?): AppResult<Unit> {
        val normalizedBaseUrl = serviceFactory.normalizeBaseUrl(baseUrl)
            ?: return AppResult.Failure(AppError.Validation("Enter a valid HTTPS base URL."))

        return try {
            val response = serviceFor(normalizedBaseUrl).health(bearerToken.asAuthorizationHeader())
            when {
                response.isSuccessful -> AppResult.Success(Unit)
                response.code() == 401 || response.code() == 403 -> AppResult.Failure(AppError.Unauthorized)
                response.code() == 404 -> AppResult.Failure(AppError.NotFound)
                else -> AppResult.Failure(AppError.Remote(response.code(), "n8n rejected the health check."))
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: SocketTimeoutException) {
            AppResult.Failure(AppError.Timeout)
        } catch (_: IOException) {
            AppResult.Failure(AppError.Offline)
        } catch (error: HttpException) {
            AppResult.Failure(AppError.Remote(error.code(), error.message()))
        } catch (error: IllegalArgumentException) {
            AppResult.Failure(AppError.Validation(error.message ?: "Invalid base URL."))
        } catch (error: Exception) {
            AppResult.Failure(AppError.Unknown(error.message ?: "Unknown connection error."))
        }
    }

    override suspend fun findProductByBarcode(
        baseUrl: String,
        bearerToken: String?,
        barcode: String
    ): AppResult<FoodProductLookup> {
        val normalizedBaseUrl = serviceFactory.normalizeBaseUrl(baseUrl)
            ?: return AppResult.Failure(AppError.Validation("Enter a valid HTTPS base URL."))
        val cleanBarcode = barcode.trim()
        if (cleanBarcode.isBlank()) {
            return AppResult.Failure(AppError.Validation("Enter a barcode first."))
        }

        return try {
            val response = serviceFor(normalizedBaseUrl).findProductByBarcode(
                authorization = bearerToken.asAuthorizationHeader(),
                idempotencyKey = "barcode-$cleanBarcode-${UUID.randomUUID()}",
                request = BarcodeRequest(
                    barcode = cleanBarcode,
                    timestamp = Instant.now().toString()
                )
            )
            when {
                response.isSuccessful -> {
                    val body = response.body()
                    val product = body?.product
                    if (body?.found == true && product != null) {
                        AppResult.Success(product.toDomain())
                    } else {
                        AppResult.Failure(AppError.NotFound)
                    }
                }
                response.code() == 401 || response.code() == 403 -> AppResult.Failure(AppError.Unauthorized)
                response.code() == 404 -> AppResult.Failure(AppError.NotFound)
                else -> AppResult.Failure(AppError.Remote(response.code(), "n8n rejected the barcode lookup."))
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: SocketTimeoutException) {
            AppResult.Failure(AppError.Timeout)
        } catch (_: IOException) {
            AppResult.Failure(AppError.Offline)
        } catch (error: HttpException) {
            AppResult.Failure(AppError.Remote(error.code(), error.message()))
        } catch (error: IllegalArgumentException) {
            AppResult.Failure(AppError.Validation(error.message ?: "Invalid base URL."))
        } catch (error: Exception) {
            AppResult.Failure(AppError.Unknown(error.message ?: "Unknown barcode lookup error."))
        }
    }

    override suspend fun analyzeMealImage(
        baseUrl: String,
        bearerToken: String?,
        imageBase64: String,
        consentGranted: Boolean
    ): AppResult<MealAnalysisResult> {
        val normalizedBaseUrl = serviceFactory.normalizeBaseUrl(baseUrl)
            ?: return AppResult.Failure(AppError.Validation("Enter a valid HTTPS base URL."))
        if (!consentGranted) {
            return AppResult.Failure(AppError.Validation("Consent is required before the photo is uploaded."))
        }
        if (imageBase64.isBlank()) {
            return AppResult.Failure(AppError.Validation("Capture a photo of the meal first."))
        }

        return try {
            val response = serviceFor(normalizedBaseUrl).analyzeMealImage(
                authorization = bearerToken.asAuthorizationHeader(),
                idempotencyKey = "image-${UUID.randomUUID()}",
                request = ImageAnalysisRequest(
                    imageBase64 = imageBase64,
                    consentGranted = true,
                    timestamp = Instant.now().toString()
                )
            )
            when {
                response.isSuccessful -> {
                    val body = response.body()
                    if (body != null && body.items.isNotEmpty()) {
                        AppResult.Success(
                            MealAnalysisResult(
                                items = body.items.map { it.toDomain() },
                                total = body.total.toDomain(),
                                disclaimer = body.disclaimer
                            )
                        )
                    } else {
                        AppResult.Failure(AppError.NotFound)
                    }
                }
                response.code() == 401 || response.code() == 403 -> AppResult.Failure(AppError.Unauthorized)
                response.code() == 404 -> AppResult.Failure(AppError.NotFound)
                else -> AppResult.Failure(AppError.Remote(response.code(), "n8n rejected the image analysis."))
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: SocketTimeoutException) {
            AppResult.Failure(AppError.Timeout)
        } catch (_: IOException) {
            AppResult.Failure(AppError.Offline)
        } catch (error: HttpException) {
            AppResult.Failure(AppError.Remote(error.code(), error.message()))
        } catch (error: IllegalArgumentException) {
            AppResult.Failure(AppError.Validation(error.message ?: "Invalid base URL."))
        } catch (error: Exception) {
            AppResult.Failure(AppError.Unknown(error.message ?: "Unknown image analysis error."))
        }
    }

    private fun serviceFor(baseUrl: String): N8nApiService {
        return serviceFactory.serviceFor(baseUrl)
    }

    private fun String?.asAuthorizationHeader(): String? {
        val token = this?.trim().orEmpty()
        return if (token.isBlank()) null else "Bearer $token"
    }

    private fun RemoteFoodProduct.toDomain(): FoodProductLookup {
        return FoodProductLookup(
            barcode = barcode,
            name = name,
            brand = brand,
            servingSizeGrams = servingSizeGrams,
            nutritionPer100g = nutritionPer100g.toDomain()
        )
    }

    private fun ImageAnalysisItem.toDomain(): MealAnalysisItem {
        return MealAnalysisItem(
            name = name,
            estimatedGrams = estimatedGrams,
            confidence = confidence,
            nutrition = nutrition.toDomain()
        )
    }

    private fun RemoteNutritionFacts.toDomain(): NutritionFacts {
        return NutritionFacts(
            caloriesKcal = caloriesKcal,
            proteinGrams = proteinGrams,
            carbohydratesGrams = carbohydratesGrams,
            sugarGrams = sugarGrams ?: 0.0,
            fatGrams = fatGrams,
            saturatedFatGrams = saturatedFatGrams ?: 0.0,
            fiberGrams = fiberGrams ?: 0.0,
            saltGrams = saltGrams ?: 0.0,
            sodiumMilligrams = null
        )
    }
}
