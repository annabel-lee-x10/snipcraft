package dev.a10101100.snipcraft.core.accessibility

import android.content.ClipboardManager
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.a10101100.snipcraft.core.variables.ClipboardVariableResolver
import dev.a10101100.snipcraft.core.variables.DateVariableResolver
import dev.a10101100.snipcraft.core.variables.TimeVariableResolver
import dev.a10101100.snipcraft.core.variables.VariableEngine
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object VariableEngineModule {

    @Provides
    @Singleton
    fun provideVariableEngine(@ApplicationContext context: Context): VariableEngine {
        val clipboardResolver = ClipboardVariableResolver {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
        }
        return VariableEngine(
            resolvers = listOf(DateVariableResolver(), TimeVariableResolver(), clipboardResolver)
        )
    }
}
