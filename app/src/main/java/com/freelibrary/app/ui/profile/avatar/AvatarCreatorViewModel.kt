package com.freelibrary.app.ui.profile.avatar

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freelibrary.app.data.profile.AvatarDesignStore
import com.freelibrary.app.domain.model.AvatarDesign
import com.freelibrary.app.domain.model.AvatarSlot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random

/**
 * What the creator shows. [picture] is set once the avatar has been written as a picture; the screen takes it and hands it to the
 * profile editor. [failed] is set when writing did not work, until the screen has told the reader.
 */
data class AvatarCreatorState(
    val design: AvatarDesign,
    val tab: AvatarTab = AvatarTab.HEAD,
    val saving: Boolean = false,
    val picture: Uri? = null,
    val failed: Boolean = false,
)

/** The avatar creator: the choices live here, and Save turns them into a picture. */
@HiltViewModel
class AvatarCreatorViewModel
    @Inject
    constructor(
        private val designStore: AvatarDesignStore,
        private val drafts: AvatarDrafts,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(AvatarCreatorState(designStore.load() ?: AvatarDesign.random(Random.Default)))
        val state: StateFlow<AvatarCreatorState> = mutableState.asStateFlow()

        fun selectTab(tab: AvatarTab) = mutableState.update { it.copy(tab = tab) }

        fun pick(
            slot: AvatarSlot,
            choice: Int,
        ) = mutableState.update { it.copy(design = it.design.with(slot, choice)) }

        fun randomize() = mutableState.update { it.copy(design = AvatarDesign.random(Random.Default)) }

        fun failureShown() = mutableState.update { it.copy(failed = false) }

        fun save() {
            val current = mutableState.value
            if (current.saving || current.picture != null) return
            mutableState.update { it.copy(saving = true) }
            viewModelScope.launch {
                try {
                    val picture = drafts.write(current.design)
                    designStore.save(current.design)
                    mutableState.update { it.copy(saving = false, picture = picture) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    mutableState.update { it.copy(saving = false, failed = true) }
                }
            }
        }
    }
