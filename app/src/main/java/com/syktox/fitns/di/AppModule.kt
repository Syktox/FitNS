package com.syktox.fitns.di

import android.content.Context
import androidx.room.Room
import com.syktox.fitns.core.network.N8nApiService
import com.syktox.fitns.core.settings.DefaultN8nBaseUrl
import com.syktox.fitns.data.local.FitNsDatabase
import com.syktox.fitns.data.local.Migration1To2
import com.syktox.fitns.data.local.Migration2To3
import com.syktox.fitns.data.local.dao.BodyWeightDao
import com.syktox.fitns.data.local.dao.FoodDao
import com.syktox.fitns.data.local.dao.ProfileDao
import com.syktox.fitns.data.local.dao.SyncQueueDao
import com.syktox.fitns.data.local.dao.WorkoutDao
import com.syktox.fitns.data.repository.DataStoreSettingsRepository
import com.syktox.fitns.data.repository.LocalBodyWeightRepository
import com.syktox.fitns.data.repository.LocalNutritionRepository
import com.syktox.fitns.data.repository.LocalProfileRepository
import com.syktox.fitns.data.repository.LocalWorkoutRepository
import com.syktox.fitns.data.repository.RemoteN8nRepository
import com.syktox.fitns.domain.repository.N8nRepository
import com.syktox.fitns.domain.repository.BodyWeightRepository
import com.syktox.fitns.domain.repository.NutritionRepository
import com.syktox.fitns.domain.repository.ProfileRepository
import com.syktox.fitns.domain.repository.SettingsRepository
import com.syktox.fitns.domain.repository.WorkoutRepository
import com.syktox.fitns.domain.usecase.BodyWeightTrendCalculator
import com.syktox.fitns.domain.usecase.NutritionCalculator
import com.syktox.fitns.domain.usecase.RecommendationEngine
import com.syktox.fitns.domain.usecase.WorkoutProgressionCalculator
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
            .addMigrations(Migration1To2, Migration2To3)
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
