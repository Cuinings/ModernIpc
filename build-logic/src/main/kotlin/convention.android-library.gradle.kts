/**
 * 约定插件：Android 库模块通用配置
 *
 * 所有 Android 库模块（ipc-contract、ipc-api、ipc-runtime-*）只需应用此插件，
 * 即可获得统一的 compileSdk、minSdk、compileOptions、kotlinOptions 配置，
 * 无需在每个模块中重复声明。
 *
 * 模块自身仍需声明：
 *   - android.namespace（模块唯一包名）
 *   - 模块特有的 buildFeatures（如 aidl）
 *   - 模块自身的 dependencies
 */
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("convention.publish")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

extensions.configure<LibraryExtension> {
    // 从版本目录统一读取 SDK 版本，修改只需改 libs.versions.toml
    compileSdk = libs.findVersion("compileSdk").get().requiredVersion.toInt()

    defaultConfig {
        minSdk = libs.findVersion("minSdk").get().requiredVersion.toInt()
    }

    compileOptions {
        val target = JavaVersion.toVersion(libs.findVersion("jvmTarget").get().requiredVersion)
        sourceCompatibility = target
        targetCompatibility = target
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = libs.findVersion("jvmTarget").get().requiredVersion
    }
}
