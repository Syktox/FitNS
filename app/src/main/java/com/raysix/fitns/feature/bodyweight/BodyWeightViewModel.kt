package com.raysix.fitns.feature.bodyweight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raysix.fitns.core.model.AppError
import com.raysix.fitns.core.model.AppResult
import com.raysix.fitns.domain.model.BodyWeightLogEntry
import com.raysix.fitns.domain.repository.BodyWeightRepository
import com.raysix.fitns.domain.repository.ProfileRepository
import com.raysix.fitns.domain.usecase.BodyWeightTrendCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BodyWeightUiState(
    val entries: List<BodyWeightLogEntry> = emptyList(),
    val sevenDayAverageKg: Double? = null,
    val progress: BodyWeightProgress = BodyWeightProgress(),
    val errorMessage: String? = null
)

data class BodyWeightProgress(
    val currentKg: Double? = null,
    val targetKg: Double? = null,
    val totalChangeKg: Double? = null,
    val thirtyDayChangeKg: Double? = null,
    val remainingToTargetKg: Double? = null,
    val summary: String = "Log your first weigh-in to start tracking progress."
)

@HiltViewModel
class BodyWeightViewModel @Inject constructor(
    private val bodyWeightRepository: BodyWeightRepository,
    private val profileRepository: ProfileRepository,
    private val trendCalculator: BodyWeightTrendCalculator
) : ViewModel() {
    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<BodyWeightUiState> = combine(
        bodyWeightRepository.observeHistory(),
        profileRepository.observeProfile(),
        errorMessage
    ) { entries, profile, error ->
        val chronologicalWeights = entries.asReversed().map { it.weightKg }
        val latestAverage = if (chronologicalWeights.isEmpty()) {
            null
        } else {
            trendCalculator.movingAverage(chronologicalWeights, windowSize = 7).last()
        }
        BodyWeightUiState(
            entries = entries,
            sevenDayAverageKg = latestAverage,
            progress = entries.toProgress(profile.targetWeightKg),
            errorMessage = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BodyWeightUiState()
    )

    fun addEntry(weightKg: Double, notes: String) {
        viewModelScope.launch {
            val result = bodyWeightRepository.addEntry(
                BodyWeightLogEntry(
                    weightKg = weightKg,
                    notes = notes
                )
            )
            errorMessage.value = when (result) {
                is AppResult.Success -> null
                is AppResult.Failure -> result.error.toMessage()
            }
        }
    }

    fun deleteEntry(entry: BodyWeightLogEntry) {
        viewModelScope.launch {
            val result = bodyWeightRepository.deleteEntry(entry)
            errorMessage.value = when (result) {
                is AppResult.Success -> null
                is AppResult.Failure -> "Weight entry could not be deleted."
            }
        }
    }

    private fun AppError.toMessage(): String {
        return when (this) {
            is AppError.Validation -> message
            else -> "Weight could not be saved."
        }
    }

    private fun List<BodyWeightLogEntry>.toProgress(targetKg: Double?): BodyWeightProgress {
        if (isEmpty()) return BodyWeightProgress(targetKg = targetKg)
        val current = first().weightKg
        val oldest = last().weightKg
        val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24L * 60L * 60L * 1000L
        val thirtyDayBaseline = lastOrNull { it.measuredAt >= thirtyDaysAgo }?.weightKg ?: oldest
        val remaining = targetKg?.let { target -> current - target }
        val summary = when {
            targetKg == null -> "Set a target weight in Profile for goal progress."
            kotlin.math.abs(remaining ?: 0.0) <= 0.3 -> "You are essentially at your target weight."
            remaining != null && remaining > 0 -> "${remaining.formatSignedMagnitude()} kg left to lose."
            remaining != null -> "${kotlin.math.abs(remaining).formatSignedMagnitude()} kg left to gain."
            else -> "Keep tracking the trend, not single-day noise."
        }
        return BodyWeightProgress(
            currentKg = current,
            targetKg = targetKg,
            totalChangeKg = current - oldest,
            thirtyDayChangeKg = current - thirtyDayBaseline,
            remainingToTargetKg = remaining,
            summary = summary
        )
    }

    private fun Double.formatSignedMagnitude(): String {
        return ((kotlin.math.abs(this) * 10.0).toInt() / 10.0).toString()
    }
}
