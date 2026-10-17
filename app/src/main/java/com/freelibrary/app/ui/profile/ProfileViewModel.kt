package com.freelibrary.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freelibrary.app.data.repository.ProfileRepository
import com.freelibrary.app.data.repository.ProfileShelvesRepository
import com.freelibrary.app.data.repository.ProfileStatsRepository
import com.freelibrary.app.domain.model.ProfileCounts
import com.freelibrary.app.domain.model.ProfileSection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Everything the Profile page shows. [avatarVersion] is null while the reader has no picture. */
data class ProfileState(
    val name: String? = null,
    val avatarVersion: Long? = null,
    val counts: ProfileCounts = ProfileCounts(0, 0),
    val sections: List<ProfileSection> = emptyList(),
)

/** The Profile page: the reader's name and picture, their totals and their three shelves. */
@HiltViewModel
class ProfileViewModel
    @Inject
    constructor(
        profileRepository: ProfileRepository,
        statsRepository: ProfileStatsRepository,
        shelvesRepository: ProfileShelvesRepository,
    ) : ViewModel() {
        val state: StateFlow<ProfileState> =
            combine(
                profileRepository.profile,
                statsRepository.counts,
                shelvesRepository.sections,
            ) { profile, counts, sections ->
                ProfileState(name = profile.name, avatarVersion = profile.avatarVersion, counts = counts, sections = sections)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ProfileState())

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }
