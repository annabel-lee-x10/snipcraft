plugins {
    id("snipcraft.android.library")
}

android {
    namespace = "dev.a10101100.snipcraft.core.common"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.timber)
    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
}
