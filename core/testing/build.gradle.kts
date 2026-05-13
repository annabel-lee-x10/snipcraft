plugins {
    id("snipcraft.android.library")
}

android {
    namespace = "dev.a10101100.snipcraft.core.testing"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(libs.kotlinx.coroutines.test)
    implementation(libs.junit5.api)
    implementation(libs.mockk)
    implementation(libs.turbine)
    implementation(libs.robolectric)
    implementation(libs.androidx.test.core)
}
