// app 模块：Android UI 壳层，消费 shared KMP 模块
plugins {
    alias(libs.plugins.android.application) // AGP 8.9.1 - 与 Kotlin 2.0 兼容
    alias(libs.plugins.kotlin.android) // Kotlin Android 插件（K2）
    alias(libs.plugins.kotlin.compose) // Compose 编译器插件，匹配 Kotlin 2.0.21
}

android {
    namespace = "com.jdcr.app" // Android 包名
    compileSdk = 35 // 对齐 shared 模块，支持最新 API

    defaultConfig {
        applicationId = "com.jdcr.app"
        minSdk = 23 // Compose Multiplatform Android 侧最低支持21
        targetSdk = 34 // 当前发布 target，兼容 Play 要求
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11 // 与老项目 Java 11 需求保持一致
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11" // Kotlin 编译目标，与 Java 编译选项一致
    }
    buildFeatures {
        compose = true // Jetpack Compose UI
        viewBinding = true // 保留 ViewBinding，兼容 XML 代码
    }
}

dependencies {
    implementation(project(":shared")) // 引入 KMP 共享逻辑与 UI

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat) // AppCompat 兼容控件，如 Toolbar
    implementation(libs.material) // 传统 XML Material 控件
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose) // Activity 与 Compose 联动
    implementation(libs.androidx.navigation.fragment.ktx) // Fragment 宿主导航
    implementation(libs.androidx.navigation.ui.ktx) // ActionBar/BottomNav 与 NavController 适配

    implementation(platform(libs.androidx.compose.bom)) // Android Compose 版本统一
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3) // Material3 Compose 组件

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}