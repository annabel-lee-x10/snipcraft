package dev.a10101100.snipcraft.feature.diagnostics

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DiagnosticsModule {
    @Provides
    @Singleton
    fun provideChecker(@ApplicationContext context: Context): DiagnosticsChecker =
        AndroidDiagnosticsChecker(context)
}
