package com.freelibrary.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freelibrary.app.data.repository.ProfileRepository
import com.freelibrary.app.data.repository.ProfileStatsRepository
import com.freelibrary.app.domain.model.ProfileStats
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
    val stats: ProfileStats = ProfileStats(0, 0, 0, 0, 0),
)

/** The Profile page: the reader's name and picture and the live counts of their books. */
@HiltViewModel
class ProfileViewModel
    @Inject
    constructor(
        profileRepository: ProfileRepository,
        statsRepository: ProfileStatsRepository,
    ) : ViewModel() {
        val state: StateFlow<ProfileState> =
            combine(profileRepository.profile, statsRepository.stats) { profile, stats ->
                ProfileState(name = profile.name, avatarVersion = profile.avatarVersion, stats = stats)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ProfileState())

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }
