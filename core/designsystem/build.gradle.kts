plugins {
    id("snipcraft.android.library.compose")
}

android {
    namespace = "dev.a10101100.snipcraft.core.designsystem"
}

dependencies {
    api(platform(libs.compose.bom))
    api(libs.compose.material3)
    api(libs.compose.ui)
    api(libs.compose.ui.graphics)
    api(libs.compose.ui.tooling.preview)
    api(libs.compose.foundation)
    api(libs.compose.material.icons.extended)
    debugApi(libs.compose.ui.tooling)
}
