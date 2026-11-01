package com.freelibrary.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freelibrary.app.data.repository.ProfileRepository
import com.freelibrary.app.data.repository.ProfileShelvesRepository
import com.freelibrary.app.data.repository.ProfileStatsRepository
import com.freelibrary.app.data.repository.ReadingGoalRepository
import com.freelibrary.app.domain.model.ProfileCounts
import com.freelibrary.app.domain.model.ProfileSection
import com.freelibrary.app.domain.model.ReadingSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Everything the Profile page shows. [avatarVersion] is null while the reader has no picture. */
data class ProfileState(
    val name: String? = null,
    val avatarVersion: Long? = null,
    val counts: ProfileCounts = ProfileCounts(0, 0),
    val sections: List<ProfileSection> = emptyList(),
    val reading: ReadingSummary = ReadingSummary(),
)

/** The Profile page: the reader's name and picture, their totals, their year and their three shelves. */
@HiltViewModel
class ProfileViewModel
    @Inject
    constructor(
        profileRepository: ProfileRepository,
        statsRepository: ProfileStatsRepository,
        shelvesRepository: ProfileShelvesRepository,
        private val goalRepository: ReadingGoalRepository,
    ) : ViewModel() {
        val state: StateFlow<ProfileState> =
            combine(
                profileRepository.profile,
                statsRepository.counts,
                shelvesRepository.sections,
                goalRepository.summary,
            ) { profile, counts, sections, reading ->
                ProfileState(
                    name = profile.name,
                    avatarVersion = profile.avatarVersion,
                    counts = counts,
                    sections = sections,
                    reading = reading,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ProfileState())

        /** Saves the yearly goal, or removes it when [goal] is null. */
        fun setYearlyGoal(goal: Int?) {
            viewModelScope.launch { goalRepository.setGoal(goal) }
        }

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }
