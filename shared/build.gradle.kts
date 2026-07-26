import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    id("com.android.kotlin.multiplatform.library")
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
kotlin {
    androidLibrary {
        namespace = "com.example.shared"
        compileSdk = 35
        minSdk = 24
    }
    
    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "shared.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)

                implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
                implementation("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.8.2")

                implementation("io.ktor:ktor-client-core:2.3.11")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.11")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.11")
                implementation("io.ktor:ktor-client-logging:2.3.11")

                implementation("androidx.datastore:datastore-preferences-core:1.1.1")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
            }
        }

        val androidMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-okhttp:2.3.11")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation("io.ktor:ktor-client-cio:2.3.11")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.1")
            }
        }

        val wasmJsMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-js:2.3.11")
            }
        }
    }
}

compose.resources {
    packageOfResClass = "com.example.shared.resources"
}

compose.desktop {
    application {
        mainClass = "com.example.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Exe, TargetFormat.Msi)
            packageName = "LifeOS"
            packageVersion = "1.0.0"
            description = "Life OS Cross-Platform Application"
            vendor = "Life OS Team"
            
            windows {
                menu = true
                shortcut = true
                dirChooser = true
            }
        }
    }
}
