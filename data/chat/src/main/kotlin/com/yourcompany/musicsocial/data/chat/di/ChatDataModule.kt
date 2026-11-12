package com.yourcompany.musicsocial.data.chat.di

import com.yourcompany.musicsocial.core.domain.repository.ChatRepository
import com.yourcompany.musicsocial.data.chat.datasource.ChatRemoteDataSource
import com.yourcompany.musicsocial.data.chat.datasource.SupabaseChatRemoteDataSource
import com.yourcompany.musicsocial.data.chat.repository.ChatRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ChatDataModule {

    @Binds
    abstract fun bindChatRemoteDataSource(impl: SupabaseChatRemoteDataSource): ChatRemoteDataSource

    @Binds
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository
}
