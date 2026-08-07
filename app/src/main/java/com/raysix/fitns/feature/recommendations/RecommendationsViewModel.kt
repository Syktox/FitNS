package com.raysix.fitns.feature.recommendations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raysix.fitns.domain.model.RecommendationItem
import com.raysix.fitns.domain.repository.BodyWeightRepository
import com.raysix.fitns.domain.repository.NutritionRepository
import com.raysix.fitns.domain.repository.WorkoutRepository
import com.raysix.fitns.domain.usecase.RecommendationEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class RecommendationsUiState(
    val recommendations: List<RecommendationItem> = emptyList()
)

@HiltViewModel
class RecommendationsViewModel @Inject constructor(
    nutritionRepository: NutritionRepository,
    bodyWeightRepository: BodyWeightRepository,
    workoutRepository: WorkoutRepository,
    recommendationEngine: RecommendationEngine
) : ViewModel() {
    val uiState: StateFlow<RecommendationsUiState> = combine(
        nutritionRepository.observeToday(),
        bodyWeightRepository.observeHistory(),
        workoutRepository.observeHistory()
    ) { dashboard, bodyWeights, workouts ->
        RecommendationsUiState(
            recommendations = recommendationEngine.generate(
                dashboard = dashboard,
                bodyWeights = bodyWeights,
                workouts = workouts
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RecommendationsUiState()
    )
}

