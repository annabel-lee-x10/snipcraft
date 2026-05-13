import com.android.build.api.dsl.LibraryExtension

plugins {
    id("snipcraft.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
}

extensions.configure<LibraryExtension> {
    buildFeatures {
        compose = true
    }
}
