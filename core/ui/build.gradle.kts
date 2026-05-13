plugins {
    id("snipcraft.android.library.compose")
}

android {
    namespace = "dev.a10101100.snipcraft.core.ui"
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:domain"))
    api(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
}
