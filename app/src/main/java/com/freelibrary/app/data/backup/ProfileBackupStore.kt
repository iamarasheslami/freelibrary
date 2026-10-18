package com.freelibrary.app.data.backup

import com.freelibrary.app.data.TimeProvider
import com.freelibrary.app.data.profile.AvatarStore
import com.freelibrary.app.data.repository.ProfileRepository
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/** The largest picture a backup may carry in; the app itself never writes more than a few hundred KB. */
private const val MAX_AVATAR_BYTES = 2L * 1024 * 1024

/** Moves the reader's profile in and out of a backup, checking everything it takes in. */
@Singleton
class ProfileBackupStore
    @Inject
    constructor(
        private val profileRepository: ProfileRepository,
        private val avatarStore: AvatarStore,
        private val timeProvider: TimeProvider,
    ) {
        /** The profile to write, or null when the reader has set nothing up. */
        suspend fun read(): ProfileBackup? {
            val profile = profileRepository.profile.first()
            val hasPicture = avatarStore.file() != null
            if (profile.name == null && profile.birthdayUtcMillis == null && !hasPicture) return null
            return ProfileBackup(profile.name, profile.birthdayUtcMillis, hasPicture)
        }

        /** The picture file to put in the zip, if there is one. */
        fun pictureFile(): File? = avatarStore.file()

        /** Restores name and birthday; a part the backup does not have keeps what the phone has. */
        suspend fun restoreDetails(backup: ProfileBackup) {
            val current = profileRepository.profile.first()
            val birthday = backup.birthdayUtcMillis?.takeIf { it in 0..timeProvider.nowMillis() }
            profileRepository.saveDetails(backup.name ?: current.name, birthday ?: current.birthdayUtcMillis)
        }

        /** Restores the picture. False when the zip entry is not a usable picture; the phone's own picture then stays. */
        suspend fun restorePicture(input: InputStream): Boolean {
            if (!avatarStore.saveFinished(input, MAX_AVATAR_BYTES)) return false
            profileRepository.setAvatarVersion(timeProvider.nowMillis())
            return true
        }
    }
