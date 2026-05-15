import java.util.Properties

plugins {
    id("snipcraft.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("snipcraft.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}

tasks.register("validateReleaseKeystore") {
    doFirst {
        if (!keystorePropsFile.exists()) {
            throw GradleException(
                "\n\n" +
                "  ❌  keystore.properties not found at project root.\n\n" +
                "  Before running bundleRelease or assembleRelease:\n" +
                "    1. Generate the release keystore (see docs/RELEASE_KEYSTORE.md)\n" +
                "    2. Create keystore.properties at project root with storeFile, storePassword,\n" +
                "       keyAlias, and keyPassword filled in.\n\n" +
                "  This keystore CANNOT be regenerated once the app is live on Play Store.\n" +
                "  Back up the .keystore file and passwords off-machine immediately.\n"
            )
        }
    }
}

tasks.matching { it.name == "bundleRelease" || it.name == "assembleRelease" }.configureEach {
    dependsOn("validateReleaseKeystore")
}

android {
    namespace = "dev.a10101100.snipcraft"

    defaultConfig {
        applicationId = "dev.a10101100.snipcraft"
        versionCode = 2
        versionName = "0.1.1"
    }

    signingConfigs {
        if (keystorePropsFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps["storeFile"] as String)
                storePassword = keystoreProps["storePassword"] as String
                keyAlias = keystoreProps["keyAlias"] as String
                keyPassword = keystoreProps["keyPassword"] as String
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (keystorePropsFile.exists())
                signingConfigs.getByName("release")
            else
                null
        }
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:accessibility"))
    implementation(project(":core:compatibility"))
    implementation(project(":core:database"))
    implementation(project(":core:sync"))
    implementation(project(":core:designsystem"))
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:library"))
    implementation(project(":feature:editor"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:diagnostics"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.compose.activity)
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)

    api(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    implementation(libs.timber)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.workmanager.ktx)
    implementation(libs.hilt.work)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
}
