package com.raysix.fitns.feature.onboarding

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raysix.fitns.core.auth.GoogleAuthController
import com.raysix.fitns.core.model.AppError
import com.raysix.fitns.core.model.AppResult
import com.raysix.fitns.domain.model.GoogleAccount
import com.raysix.fitns.domain.model.NutritionGoal
import com.raysix.fitns.domain.model.UserProfile
import com.raysix.fitns.domain.repository.ProfileRepository
import com.raysix.fitns.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val onboardingCompleted: Boolean? = null,
    val profile: UserProfile = UserProfile(),
    val nutritionGoal: NutritionGoal = NutritionGoal(
        caloriesKcal = 2300.0,
        proteinGrams = 150.0,
        carbohydrateGrams = 250.0,
        fatGrams = 75.0,
        fiberGrams = 30.0,
        waterMilliliters = 2500.0
    ),
    val googleAccount: GoogleAccount? = null,
    val saving: Boolean = false,
    val errorMessage: String? = null,
    val signInError: String? = null
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val settingsRepository: SettingsRepository,
    private val googleAuthController: GoogleAuthController
) : ViewModel() {

    val isGoogleConfigured: Boolean
        get() = googleAuthController.isConfigured

    private val saving = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)
    private val signInError = MutableStateFlow<String?>(null)

    val uiState: StateFlow<OnboardingUiState> = combine(
        settingsRepository.observeOnboardingCompleted(),
        profileRepository.observeProfile(),
        profileRepository.observeNutritionGoal(),
        settingsRepository.observeGoogleAccount(),
        saving,
        errorMessage,
        signInError
    ) { values ->
        OnboardingUiState(
            onboardingCompleted = values[0] as Boolean?,
            profile = values[1] as UserProfile,
            nutritionGoal = values[2] as NutritionGoal,
            googleAccount = values[3] as GoogleAccount?,
            saving = values[4] as Boolean,
            errorMessage = values[5] as String?,
            signInError = values[6] as String?
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = OnboardingUiState()
    )

    fun createSignInIntent(): Intent? = googleAuthController.createSignInIntent()

    fun handleSignInResult(data: Intent?): Boolean {
        val account = googleAuthController.handleSignInResult(data)
        return if (account != null) {
            onGoogleSignedIn(account)
            true
        } else {
            false
        }
    }

    fun onGoogleSignedIn(account: GoogleAccount) {
        viewModelScope.launch {
            settingsRepository.saveGoogleAccount(account)
        }
        signInError.value = null
    }

    fun onGoogleSignOut() {
        googleAuthController.signOut()
        viewModelScope.launch {
            settingsRepository.clearGoogleAccount()
        }
        signInError.value = null
    }

    fun setSignInError(message: String) {
        signInError.value = message
    }

    fun save(profile: UserProfile, goal: NutritionGoal, onComplete: () -> Unit) {
        viewModelScope.launch {
            saving.value = true
            errorMessage.value = null
            val profileResult = profileRepository.saveProfile(profile)
            val goalResult = profileRepository.saveNutritionGoal(goal)
            if (profileResult is AppResult.Failure) {
                errorMessage.value = profileResult.error.toMessage()
            } else if (goalResult is AppResult.Failure) {
                errorMessage.value = goalResult.error.toMessage()
            } else {
                settingsRepository.completeOnboarding()
                onComplete()
            }
            saving.value = false
        }
    }

    private fun AppError.toMessage(): String {
        return when (this) {
            is AppError.Validation -> message
            else -> "Profile could not be saved."
        }
    }
}
