plugins {
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    jvmToolchain(21)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

dependencies {
    // JUnit Platform launcher required by Gradle 9 when using useJUnitPlatform()
    "testRuntimeOnly"("org.junit.platform:junit-platform-launcher:1.12.2")
}
