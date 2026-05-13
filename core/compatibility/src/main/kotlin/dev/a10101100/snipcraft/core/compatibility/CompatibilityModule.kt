package dev.a10101100.snipcraft.core.compatibility

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CompatibilityModule {

    @Provides
    @Singleton
    fun provideCompatibilityResolver(): CompatibilityResolver = CompatibilityResolver()
}
