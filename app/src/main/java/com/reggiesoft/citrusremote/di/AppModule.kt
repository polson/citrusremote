package com.reggiesoft.citrusremote.di

import com.reggiesoft.citrusremote.data.python.AppleTvRemoteService
import com.reggiesoft.citrusremote.data.python.ChaquopyAppleTvRemoteService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    abstract fun bindAppleTvRemoteService(
        serviceImpl: ChaquopyAppleTvRemoteService
    ): AppleTvRemoteService
}
