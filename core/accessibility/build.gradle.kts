plugins {
    id("snipcraft.android.library")
    id("snipcraft.android.hilt")
}

android {
    namespace = "dev.a10101100.snipcraft.core.accessibility"

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(project(":core:engine"))
    implementation(project(":core:variables"))
    implementation(project(":core:compatibility"))
    implementation(project(":core:data"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.workmanager.ktx)

    // JUnit 5 for pure unit tests
    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    // JUnit 4 + vintage engine for Robolectric tests
    testImplementation(libs.junit4)
    testRuntimeOnly(libs.junit.vintage.engine)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.workmanager.testing)
    testImplementation(libs.kotlinx.coroutines.test)
}
