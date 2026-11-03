package com.freelibrary.app.di

import com.freelibrary.app.ui.profile.avatar.AvatarDrafts
import com.freelibrary.app.ui.profile.avatar.FileAvatarDrafts
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AvatarModule {
    @Binds
    abstract fun bindAvatarDrafts(drafts: FileAvatarDrafts): AvatarDrafts
}
