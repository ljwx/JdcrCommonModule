# JdcrCommonModule 升级与跨平台集成指南

本文记录了从“普通 Android 工程”演进到“Java 17 + Compose + Kotlin Multiplatform (KMP) + Compose Multiplatform”的全流程。包含每一步的关键配置、遇到的问题与解决思路，方便后续项目复用或排障。

---

## 1. 初始状态与目标
- **原始状态**：Gradle 7.x + AGP 7.x，Java 11，XML UI，为传统 Android 项目。
- **目标能力**：
  1. 升级到 Java 17、对齐最新 Gradle/AGP。
  2. Android 模块启用 Jetpack Compose。
  3. 新增 Kotlin Multiplatform `shared` 模块，并使用 Compose Multiplatform 编写共享 UI。
  4. 所有模块采用统一版本目录 (`libs.versions.toml`)，便于后续升级。

---

## 2. 升级 Java 17 与基础工具链
### 2.1 升级步骤
- 修改 `gradle.properties` 的 `org.gradle.java.home` / IDE Gradle JDK 指向 JDK 17。
- 将 `app/build.gradle.kts` 的 `compileOptions`、`kotlinOptions` 统一到 11 → 17。
- 更新 `shared` 模块（若存在）或新的模块模板中的 `minSdk/compileSdk`，确保满足 AGP 8.x 要求。

### 2.2 常见问题
| 问题 | 原因 | 解决方案 |
| --- | --- | --- |
| `AGP requires Java 17 to run` | Gradle/IDE 仍使用旧 JDK | 在 IDE 与命令行都指定 JDK 17 |
| `Unsupported class file major version` | 子模块仍以 Java 8/11 编译 | 逐个模块设置 `sourceCompatibility` / `jvmTarget` |

---

## 3. Compose (Android) 集成
### 3.1 关键配置
- 在 `app/build.gradle.kts`：
  ```kotlin
  buildFeatures {
      compose = true
      viewBinding = true // 保留 XML 支持
  }
  composeOptions {
      kotlinCompilerExtensionVersion = libs.versions.composeBom.get()
  }
  ```
- 依赖管理使用 `libs.versions.toml` + Compose BOM：
  ```kotlin
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.ui)
  implementation(libs.androidx.material3)
  implementation(libs.androidx.activity.compose)
  ```

### 3.2 典型报错与处理
| 报错 | 原因 | 处理 |
| --- | --- | --- |
| `org.jetbrains.kotlin.plugin.compose not found` | Kotlin 1.7.x 没有该插件 | 升级 Kotlin ≥ 1.9；改用 Compose Multiplatform 插件 |
| `Compose Compiler requires Kotlin version ...` | Compose Compiler 与 Kotlin 版本不兼容 | 通过 BOM + 官方版本矩阵对齐 |
| `NoSuchMethodError: MessageCollector.report$default` | Kotlin/Compose 版本混用 | 清理 Gradle 缓存，统一版本 |

---

## 4. 引入 Kotlin Multiplatform + Compose Multiplatform
### 4.1 基础结构
1. 在 `settings.gradle.kts` 新增 `include(":shared")`，并声明 JetBrains Compose 仓库：
   ```kotlin
   pluginManagement {
       repositories {
           google()
           mavenCentral()
           gradlePluginPortal()
           maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
       }
   }
   dependencyResolutionManagement {
       repositories {
           google()
           mavenCentral()
           maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
       }
   }
   ```
2. 根 `build.gradle` 仅声明插件 alias（不直接 apply），由子模块按需使用。
3. 新建 `shared` 模块并使用 Kotlin DSL (`build.gradle.kts`)：
   ```kotlin
   plugins {
       alias(libs.plugins.kotlin.multiplatform)
       alias(libs.plugins.android.kotlin.multiplatform.library)
       alias(libs.plugins.jetbrains.compose)
       alias(libs.plugins.kotlin.compose)
   }

   kotlin {
       androidLibrary {
           namespace = "com.jdcr.shared"
           compileSdk = 35
           minSdk = 21
       }
       iosX64(); iosArm64(); iosSimulatorArm64()
       sourceSets {
           commonMain {
               dependencies {
                   implementation(compose.runtime)
                   implementation(compose.foundation)
                   implementation(compose.material3)
                   implementation("co.touchlab:kermit:2.0.3")
               }
           }
           androidMain {
               dependencies {
                   implementation(compose.ui)
                   implementation(compose.preview)
                   implementation(libs.androidx.activity.compose)
               }
           }
       }
   }
   ```

### 4.2 需要注意的插件组合
| 插件 | 用途 | 说明 |
| --- | --- | --- |
| `org.jetbrains.kotlin.multiplatform` | 提供 KMP SourceSet 支持 | Kotlin 2.0.21 的核心插件 |
| `com.android.kotlin.multiplatform.library` | 让 KMP 模块生成 Android AAR | AGP 8.9.1 提供 |
| `org.jetbrains.compose` | Compose Multiplatform Gradle 支持 | 提供 `compose.*` 依赖别名 |
| `org.jetbrains.kotlin.plugin.compose` | Kotlin 2.0+ 必须的 Compose 编译器插件 | 和 `org.jetbrains.compose` 同时使用 |

### 4.3 Compose Multiplatform 常见问题
| 报错 | 原因 | 解决 |
| --- | --- | --- |
| `Since Kotlin 2.0.0-RC2 you must apply org.jetbrains.kotlin.plugin.compose` | 升级到 Compose 1.7.0 后强制要求 | 在 root `plugins` 与 `shared/build.gradle.kts` 同时声明 alias 并 apply |
| `Unresolved reference: compose` in commonMain | 依赖没有添加到 `commonMain` | 使用 `implementation(compose.runtime/foundation/material3)` |
| `androidLibrary defaultConfig unresolved` | KMP DSL 与传统 DSL 不同 | 在 `androidLibrary` 内直接写 `compileSdk/minSdk`，不要再写 `defaultConfig` |

### 4.4 与 Android 模块联动
- `app` 模块引入 `implementation(project(":shared"))`，即可使用 KMP 提供的 Composable：
  ```kotlin
  // NotificationsFragment.kt
  SharedScreen()
  ```
- 保持 KMP 与 Android Compose 的版本一致，可通过 `libs.androidx.compose.bom` + Compose Multiplatform 统一管理。

---

## 5. 资源与 UI 调整
- 主题迁移：从 `Theme.AppCompat.Light.DarkActionBar` → `Theme.Material3.DayNight.NoActionBar`。
- Toolbar 处理：`activity_main.xml` 增加 `MaterialToolbar`，在 `MainActivity` 中使用 `setupWithNavController` 绑定标题/返回箭头。
- Navigation：补充 `androidx.navigation.fragment.ktx` 和 `androidx.navigation.ui.ktx` 依赖，解决 XML 中的 `navGraph`/`defaultNavHost` 属性找不到的问题。

---

## 6. 版本目录 (`libs.versions.toml`) 示例
- 所有版本集中维护，并给出注释说明每个版本的选择理由。
- 复制到其他项目时，只需同步 `settings.gradle.kts` 的仓库声明即可直接复用。

---

## 7. 构建与验证
### 7.1 构建命令
```bash
./gradlew clean :shared:assemble :app:assembleDebug
```

### 7.2 验证点
- Android 模块编译通过，Compose Preview 正常。
- `shared` 模块可以生成 Android AAR；若接入 iOS，可继续用 `./gradlew :shared:createXCFramework`（Compose MPP 官方文档）。

---

## 8. 经验总结
1. **版本矩阵先行**：Compose MPP 与 Kotlin 版本强绑定，升级前先查官方支持表。
2. **插件别 apply 多次**：root 只做 alias，子模块按需 `apply`，避免重复加载导致 `plugin already on classpath` 错误。
3. **仓库配置一次到位**：`settings.gradle.kts` 中添加 JetBrains Compose 仓库，避免“找不到插件/依赖”的反复折腾。
4. **保留注释与模板**：通过详细注释记录依赖、插件、版本选择原因，后续迁移/调试更高效。

---

> **提示**：若要兼容更老的项目，可额外维护“兼容版”配置（例如 Kotlin 1.9.22 + Compose MPP 1.6.11 + AGP 8.5.0），但需放弃最新 Compose 特性。上述指南基于“最新稳定”栈，适合全新或已升级的工程。
