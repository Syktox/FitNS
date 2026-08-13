package com.raysix.fitns.di

import android.content.Context
import androidx.room.Room
import com.raysix.fitns.core.network.N8nApiService
import com.raysix.fitns.core.settings.DefaultN8nBaseUrl
import com.raysix.fitns.data.local.FitNsDatabase
import com.raysix.fitns.data.local.Migration1To2
import com.raysix.fitns.data.local.Migration2To3
import com.raysix.fitns.data.local.Migration3To4
import com.raysix.fitns.data.local.Migration4To5
import com.raysix.fitns.data.local.dao.BodyWeightDao
import com.raysix.fitns.data.local.dao.FoodDao
import com.raysix.fitns.data.local.dao.ProfileDao
import com.raysix.fitns.data.local.dao.SyncQueueDao
import com.raysix.fitns.data.local.dao.WorkoutDao
import com.raysix.fitns.data.repository.DataStoreSettingsRepository
import com.raysix.fitns.data.repository.LocalBodyWeightRepository
import com.raysix.fitns.data.repository.LocalNutritionRepository
import com.raysix.fitns.data.repository.LocalProfileRepository
import com.raysix.fitns.data.repository.LocalWorkoutRepository
import com.raysix.fitns.data.repository.RemoteN8nRepository
import com.raysix.fitns.domain.repository.N8nRepository
import com.raysix.fitns.domain.repository.BodyWeightRepository
import com.raysix.fitns.domain.repository.NutritionRepository
import com.raysix.fitns.domain.repository.ProfileRepository
import com.raysix.fitns.domain.repository.SettingsRepository
import com.raysix.fitns.domain.repository.WorkoutRepository
import com.raysix.fitns.domain.usecase.BodyWeightTrendCalculator
import com.raysix.fitns.domain.usecase.LabelNutritionParser
import com.raysix.fitns.domain.usecase.NutritionCalculator
import com.raysix.fitns.domain.usecase.RecommendationEngine
import com.raysix.fitns.domain.usecase.WorkoutProgressionCalculator
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FitNsDatabase {
        return Room.databaseBuilder(context, FitNsDatabase::class.java, "fitns.db")
            .addMigrations(Migration1To2, Migration2To3, Migration3To4, Migration4To5)
            .build()
    }

    @Provides
    fun provideFoodDao(database: FitNsDatabase): FoodDao = database.foodDao()

    @Provides
    fun provideWorkoutDao(database: FitNsDatabase): WorkoutDao = database.workoutDao()

    @Provides
    fun provideSyncQueueDao(database: FitNsDatabase): SyncQueueDao = database.syncQueueDao()

    @Provides
    fun provideBodyWeightDao(database: FitNsDatabase): BodyWeightDao = database.bodyWeightDao()

    @Provides
    fun provideProfileDao(database: FitNsDatabase): ProfileDao = database.profileDao()

    @Provides
    fun provideNutritionCalculator(): NutritionCalculator = NutritionCalculator()

    @Provides
    fun provideWorkoutProgressionCalculator(): WorkoutProgressionCalculator = WorkoutProgressionCalculator()

    @Provides
    fun provideLabelNutritionParser(): LabelNutritionParser = LabelNutritionParser()

    @Provides
    fun provideBodyWeightTrendCalculator(): BodyWeightTrendCalculator = BodyWeightTrendCalculator()

    @Provides
    fun provideRecommendationEngine(bodyWeightTrendCalculator: BodyWeightTrendCalculator): RecommendationEngine {
        return RecommendationEngine(bodyWeightTrendCalculator)
    }

    @Provides
    @Singleton
    fun provideNutritionRepository(repository: LocalNutritionRepository): NutritionRepository = repository

    @Provides
    @Singleton
    fun provideWorkoutRepository(repository: LocalWorkoutRepository): WorkoutRepository = repository

    @Provides
    @Singleton
    fun provideBodyWeightRepository(repository: LocalBodyWeightRepository): BodyWeightRepository = repository

    @Provides
    @Singleton
    fun provideProfileRepository(repository: LocalProfileRepository): ProfileRepository = repository

    @Provides
    @Singleton
    fun provideSettingsRepository(repository: DataStoreSettingsRepository): SettingsRepository = repository

    @Provides
    @Singleton
    fun provideN8nRepository(repository: RemoteN8nRepository): N8nRepository = repository

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    @Provides
    @Singleton
    fun provideN8nApiService(client: OkHttpClient, moshi: Moshi): N8nApiService {
        return Retrofit.Builder()
            .baseUrl(DefaultN8nBaseUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(N8nApiService::class.java)
    }
}
