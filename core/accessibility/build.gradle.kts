plugins {
    id("snipcraft.android.library")
    id("snipcraft.android.hilt")
}

android {
    namespace = "dev.a10101100.snipcraft.core.accessibility"
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
    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
}
