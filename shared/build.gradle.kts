// shared 模块：Kotlin Multiplatform + Compose Multiplatform 核心配置
plugins {
    alias(libs.plugins.kotlin.multiplatform) // 注册 KMP 核心能力，提供 commonMain/iosMain 等 SourceSet
    alias(libs.plugins.android.kotlin.multiplatform.library) // Android Library 目标扩展，生成 AAR
    alias(libs.plugins.jetbrains.compose) // Compose Multiplatform Gradle 插件，提供 compose.* 依赖别名
    alias(libs.plugins.kotlin.compose) // Kotlin 2.0+ 所需的 Compose 编译器插件
}

kotlin {

// Target declarations - add or remove as needed below. These define
// which platforms this KMP module supports.
// See: https://kotlinlang.org/docs/multiplatform-discover-project.html#targets
    androidLibrary {
        namespace = "com.jdcr.shared" // Android AAR 输出的命名空间
        compileSdk = 35 // 与 app 模块保持一致，支持最新 API
        minSdk = 23 // Compose Multiplatform Android 侧最低要求 21

        withHostTestBuilder {
        }

        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

// For iOS targets, this is also where you should
// configure native binary output. For more information, see:
// https://kotlinlang.org/docs/multiplatform-build-native-binaries.html#build-xcframeworks

// A step-by-step guide on how to include this library in an XCode
// project can be found here:
// https://developer.android.com/kotlin/multiplatform/migrate
    val xcfName = "sharedKit" // iOS framework 输出名，可按业务改

    iosX64 {
        binaries.framework {
            baseName = xcfName
        }
    }

    iosArm64 {
        binaries.framework {
            baseName = xcfName
        }
    }

    iosSimulatorArm64 {
        binaries.framework {
            baseName = xcfName
        }
    }

// Source set declarations.
// Declaring a target automatically creates a source set with the same name. By default, the
// Kotlin Gradle Plugin creates additional source sets that depend on each other, since it is
// common to share sources between related targets.
// See: https://kotlinlang.org/docs/multiplatform-hierarchy.html
    sourceSets {
        commonMain {
            dependencies {
                implementation("co.touchlab:kermit:2.0.3") // KMP 日志库，示例用途
                implementation(compose.runtime) // Compose Multiplatform 核心 runtime
                implementation(compose.foundation) // 通用布局/基础组件
                implementation(compose.material3) // Material3 跨平台组件（仅依赖 commonMain）
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test) // KMP 测试框架
            }
        }

        androidMain {
            dependencies {
                // 可在此添加 only-Android 依赖，例如窗口控件或 AndroidX bridge
                implementation(compose.ui) // Android 平台 Compose UI artifact
                implementation(compose.preview) // 预览调试支持
                implementation(libs.androidx.activity.compose) // Activity 与 Compose Bridge
            }
        }

        getByName("androidDeviceTest") {
            dependencies {
                implementation(libs.androidx.runner)
                implementation(libs.androidx.core)
                implementation(libs.androidx.junit)
            }
        }

        iosMain {
            dependencies {
                // 如需 iOS 专属库（Swift interop 等）可在此补充
            }
        }
    }

}