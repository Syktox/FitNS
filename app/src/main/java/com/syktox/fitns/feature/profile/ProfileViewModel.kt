package com.syktox.fitns.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.syktox.fitns.core.model.AppError
import com.syktox.fitns.core.model.AppResult
import com.syktox.fitns.domain.model.NutritionGoal
import com.syktox.fitns.domain.model.UserProfile
import com.syktox.fitns.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val profile: UserProfile = UserProfile(),
    val nutritionGoal: NutritionGoal = NutritionGoal(
        caloriesKcal = 2300.0,
        proteinGrams = 150.0,
        carbohydrateGrams = 250.0,
        fatGrams = 75.0,
        fiberGrams = 30.0,
        waterMilliliters = 2500.0
    ),
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {
    private val statusMessage = MutableStateFlow<String?>(null)
    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ProfileUiState> = combine(
        profileRepository.observeProfile(),
        profileRepository.observeNutritionGoal(),
        statusMessage,
        errorMessage
    ) { profile, goal, status, error ->
        ProfileUiState(
            profile = profile,
            nutritionGoal = goal,
            statusMessage = status,
            errorMessage = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileUiState()
    )

    fun save(profile: UserProfile, goal: NutritionGoal) {
        viewModelScope.launch {
            val profileResult = profileRepository.saveProfile(profile)
            val goalResult = profileRepository.saveNutritionGoal(goal)
            val failure = listOf(profileResult, goalResult).filterIsInstance<AppResult.Failure>().firstOrNull()
            if (failure == null) {
                errorMessage.value = null
                statusMessage.value = "Profile and goals saved"
            } else {
                statusMessage.value = null
                errorMessage.value = failure.error.toMessage()
            }
        }
    }

    private fun AppError.toMessage(): String {
        return when (this) {
            is AppError.Validation -> message
            else -> "Profile could not be saved."
        }
    }
}
