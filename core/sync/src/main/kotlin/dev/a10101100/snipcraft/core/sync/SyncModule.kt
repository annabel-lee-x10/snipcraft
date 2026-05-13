package dev.a10101100.snipcraft.core.sync

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SyncJson

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {

    @Binds
    @Singleton
    abstract fun bindSyncConfigStore(impl: CredentialStore): SyncConfigStore

    companion object {

        @Provides
        @Singleton
        fun provideOkHttpClient(): OkHttpClient {
            val logging = HttpLoggingInterceptor().apply {
                // Keep level NONE to never log credentials or request bodies
                level = HttpLoggingInterceptor.Level.NONE
            }
            return OkHttpClient.Builder()
                .addInterceptor(logging)
                .build()
        }

        @Provides
        @Singleton
        @SyncJson
        fun provideSyncJson(): Json = Json { ignoreUnknownKeys = true }
    }
}
