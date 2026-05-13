package dev.a10101100.snipcraft.core.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SnipcraftDatabase =
        Room.databaseBuilder(context, SnipcraftDatabase::class.java, SnipcraftDatabase.DATABASE_NAME)
            .build()

    @Provides
    fun provideSnippetDao(db: SnipcraftDatabase): SnippetDao = db.snippetDao()

    @Provides
    fun provideFolderDao(db: SnipcraftDatabase): FolderDao = db.folderDao()

    @Provides
    fun provideCompatibilityRuleDao(db: SnipcraftDatabase): CompatibilityRuleDao = db.compatibilityRuleDao()
}
