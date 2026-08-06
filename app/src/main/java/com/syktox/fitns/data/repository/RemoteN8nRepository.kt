package com.syktox.fitns.data.repository

import com.syktox.fitns.core.model.AppError
import com.syktox.fitns.core.model.AppResult
import com.syktox.fitns.core.network.N8nApiService
import com.syktox.fitns.core.network.BarcodeRequest
import com.syktox.fitns.core.network.RemoteFoodProduct
import com.syktox.fitns.domain.repository.N8nRepository
import com.syktox.fitns.domain.model.FoodProductLookup
import com.syktox.fitns.domain.model.NutritionFacts
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.net.SocketTimeoutException
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class RemoteN8nRepository @Inject constructor(
    private val client: OkHttpClient,
    private val moshi: Moshi
) : N8nRepository {
    override suspend fun testConnection(baseUrl: String, bearerToken: String?): AppResult<Unit> {
        val normalizedBaseUrl = normalizeBaseUrl(baseUrl)
            ?: return AppResult.Failure(AppError.Validation("Enter a valid HTTPS base URL."))

        return try {
            val response = serviceFor(normalizedBaseUrl).health(bearerToken.asAuthorizationHeader())
            when {
                response.isSuccessful -> AppResult.Success(Unit)
                response.code() == 401 || response.code() == 403 -> AppResult.Failure(AppError.Unauthorized)
                response.code() == 404 -> AppResult.Failure(AppError.NotFound)
                else -> AppResult.Failure(AppError.Remote(response.code(), "n8n rejected the health check."))
            }
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
        val normalizedBaseUrl = normalizeBaseUrl(baseUrl)
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

    private fun serviceFor(baseUrl: String): N8nApiService {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(N8nApiService::class.java)
    }

    private fun normalizeBaseUrl(baseUrl: String): String? {
        val trimmed = baseUrl.trim()
        if (!trimmed.startsWith("https://")) return null
        return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
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
            nutritionPer100g = NutritionFacts(
                caloriesKcal = nutritionPer100g.caloriesKcal,
                proteinGrams = nutritionPer100g.proteinGrams,
                carbohydratesGrams = nutritionPer100g.carbohydratesGrams,
                sugarGrams = nutritionPer100g.sugarGrams ?: 0.0,
                fatGrams = nutritionPer100g.fatGrams,
                saturatedFatGrams = nutritionPer100g.saturatedFatGrams ?: 0.0,
                fiberGrams = nutritionPer100g.fiberGrams ?: 0.0,
                saltGrams = nutritionPer100g.saltGrams ?: 0.0,
                sodiumMilligrams = null
            )
        )
    }
}
