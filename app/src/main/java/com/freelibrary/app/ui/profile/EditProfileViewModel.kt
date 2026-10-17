package com.freelibrary.app.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freelibrary.app.data.TimeProvider
import com.freelibrary.app.data.profile.AvatarStore
import com.freelibrary.app.data.repository.ProfileRepository
import com.freelibrary.app.domain.model.MAX_PROFILE_NAME_LENGTH
import com.freelibrary.app.domain.model.UserProfile
import com.freelibrary.app.domain.model.cleanProfileName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/** What the reader has decided about the picture; nothing is applied until Save. */
sealed interface PictureDraft {
    /** Leave the saved picture (or the lack of one) as it is. */
    data object Keep : PictureDraft

    data object Remove : PictureDraft

    data class Replace(
        val source: Uri,
    ) : PictureDraft
}

enum class EditProfileError {
    /** The chosen picture could not be read as an image. */
    PICTURE_UNREADABLE,

    /** Anything else that went wrong while saving. */
    SAVE_FAILED,
}

/** The editor's form. [dirty] is true once something differs from what is saved. */
data class EditProfileState(
    val loaded: Boolean = false,
    val name: String = "",
    val birthdayUtcMillis: Long? = null,
    val savedAvatarVersion: Long? = null,
    val picture: PictureDraft = PictureDraft.Keep,
    val dirty: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: EditProfileError? = null,
) {
    /** Whether a picture is on show: a newly chosen one, or the saved one unless it is being removed. */
    val hasPicture: Boolean
        get() =
            when (picture) {
                PictureDraft.Keep -> savedAvatarVersion != null
                PictureDraft.Remove -> false
                is PictureDraft.Replace -> true
            }
}

/**
 * The profile editor. Every change stays in a draft until [save], which writes the picture and
 * then the details; if the picture cannot be read nothing at all is saved.
 */
@HiltViewModel
class EditProfileViewModel
    @Inject
    constructor(
        private val profileRepository: ProfileRepository,
        private val avatarStore: AvatarStore,
        private val timeProvider: TimeProvider,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(EditProfileState())
        val state: StateFlow<EditProfileState> = mutableState.asStateFlow()

        private var initial = UserProfile()

        init {
            viewModelScope.launch {
                val profile = profileRepository.profile.first()
                initial = profile
                mutableState.value =
                    EditProfileState(
                        loaded = true,
                        name = profile.name.orEmpty(),
                        birthdayUtcMillis = profile.birthdayUtcMillis,
                        savedAvatarVersion = profile.avatarVersion,
                    )
            }
        }

        fun onNameChanged(name: String) = change { it.copy(name = name.take(MAX_PROFILE_NAME_LENGTH)) }

        /** A day at midnight UTC, or null to clear it. A day in the future is ignored. */
        fun onBirthdayPicked(utcMillis: Long?) {
            if (utcMillis != null && utcMillis > timeProvider.nowMillis()) return
            change { it.copy(birthdayUtcMillis = utcMillis) }
        }

        fun onPictureChosen(source: Uri) = change { it.copy(picture = PictureDraft.Replace(source)) }

        /** Removes the saved picture, or just forgets a newly chosen one. */
        fun onPictureRemoved() =
            change { current ->
                current.copy(picture = if (current.savedAvatarVersion != null) PictureDraft.Remove else PictureDraft.Keep)
            }

        fun errorShown() = mutableState.update { it.copy(error = null) }

        fun save() {
            val current = mutableState.value
            if (!current.loaded || current.saving || !current.dirty) return
            mutableState.update { it.copy(saving = true) }
            viewModelScope.launch {
                val failure =
                    try {
                        persist(current)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        EditProfileError.SAVE_FAILED
                    }
                mutableState.update {
                    if (failure == null) {
                        it.copy(
                            saving = false,
                            saved = true,
                        )
                    } else {
                        it.copy(saving = false, error = failure)
                    }
                }
            }
        }

        private suspend fun persist(draft: EditProfileState): EditProfileError? {
            when (val picture = draft.picture) {
                PictureDraft.Keep -> Unit
                PictureDraft.Remove -> {
                    avatarStore.remove()
                    profileRepository.setAvatarVersion(null)
                }
                is PictureDraft.Replace -> {
                    if (!avatarStore.save(picture.source)) return EditProfileError.PICTURE_UNREADABLE
                    profileRepository.setAvatarVersion(timeProvider.nowMillis())
                }
            }
            profileRepository.saveDetails(draft.name, draft.birthdayUtcMillis)
            return null
        }

        private fun change(transform: (EditProfileState) -> EditProfileState) {
            mutableState.update { current ->
                val changed = transform(current)
                changed.copy(dirty = isDirty(changed))
            }
        }

        private fun isDirty(state: EditProfileState): Boolean =
            cleanProfileName(state.name) != initial.name ||
                state.birthdayUtcMillis != initial.birthdayUtcMillis ||
                state.picture != PictureDraft.Keep
    }
