import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.20" // Add this line
}

kotlin {
    // Android configuration
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }
    }

    // iOS configuration
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    // Desktop configuration
    jvm("desktop"){
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }
    }

    // Web configuration
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        moduleName = "composeApp"
        browser {
            val rootDirPath = project.rootDir.path
            val projectDirPath = project.projectDir.path
            commonWebpackConfig {
                outputFileName = "composeApp.js"
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    static = (static ?: mutableListOf()).apply {
                        add(rootDirPath)
                        add(projectDirPath)
                    }
                }
            }
        }
        binaries.executable()
    }

//    sourceSets.all {
//        languageSettings.enableLanguageFeature("ExpectActualClasses")
//    }

    sourceSets {
        // Common dependencies for all platforms
        val commonMain by getting {
            dependencies {
                // Compose dependencies
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)

                // AndroidX dependencies
                implementation(libs.androidx.lifecycle.viewmodel)
                implementation(libs.androidx.lifecycle.runtime.compose)
                implementation(libs.androidx.material3)
                implementation(libs.androidx.materialIconsExtended)

                // Navigation and UI components
                implementation(libs.adaptive)
                implementation(libs.adaptive.layout)
                implementation(libs.adaptive.navigation)
                implementation(libs.material3.adaptive.navigation.suite)
                implementation(libs.material3.window.size.classe)
                implementation(libs.navigation.compose)

                // Core dependencies
                implementation(libs.qr.kit)
                implementation(libs.koin.compose)
                implementation(libs.koin.compose.viewmodel)
                api(libs.koin.core)
                implementation(libs.bundles.ktor)


                implementation(libs.kotlinx.serialization.json)
//                implementation(libs.logback.classic)
            }
        }

        // Android-specific dependencies
        val androidMain by getting {
            dependencies {
                implementation(compose.preview)
                implementation(libs.androidx.activity.compose)
                implementation(libs.koin.android)
                implementation(libs.koin.androidx.compose)
                implementation(libs.ktor.client.cio)
                implementation(libs.ktor.client.okhttp)

                // Camera dependencies
                implementation(libs.androidx.camera.camera2)
                implementation(libs.androidx.camera.lifecycle)
                implementation(libs.androidx.camera.video)
                implementation(libs.androidx.camera.view)
                implementation(libs.androidx.camera.mlkit.vision)
                implementation(libs.androidx.camera.extensions)
            }
        }

        // Desktop-specific dependencies
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.bundles.ktor.server)
//                implementation(libs.javacv.platform)

                // Platform-specific Skiko configuration
                val osName = System.getProperty("os.name")
                val targetOs = when {
                    osName.startsWith("Win") -> "windows"
                    osName.startsWith("Linux") -> "linux"
                    else -> error("Unsupported OS: $osName")
                }

                val targetArch = when (val osArch = System.getProperty("os.arch")) {
                    "x86_64", "amd64" -> "x64"
                    "aarch64" -> "arm64"
                    else -> error("Unsupported arch: $osArch")
                }

                val skikoVersion = "0.8.22.1"
                implementation("org.jetbrains.skiko:skiko-awt-runtime-$targetOs-$targetArch:$skikoVersion")
            }
        }
    }
}

// Android configuration block
android {

    namespace = "com.ricsdev.ucam"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.ricsdev.ucam"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/INDEX.LIST"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// Root-level dependencies
dependencies {
    implementation(libs.androidx.camera.lifecycle)
    debugImplementation(compose.uiTooling)
}

// Desktop application configuration
compose.desktop {
    application {
        mainClass = "com.ricsdev.ucam.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.ricsdev.ucam"
            packageVersion = "1.0.0"
        }
    }
}