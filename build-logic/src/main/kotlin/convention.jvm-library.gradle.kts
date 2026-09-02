/**
 * 约定插件：纯 JVM 库模块通用配置
 *
 * 所有纯 JVM 模块（ipc-annotations、ipc-compiler）只需应用此插件，
 * 即可获得统一的 Java 源码兼容性配置，无需在每个模块中重复声明。
 *
 * 模块自身仍需声明：
 *   - 模块自身的 dependencies
 */
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.configure

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("convention.publish")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

extensions.configure<JavaPluginExtension> {
    // 从版本目录统一读取 JVM 目标版本，修改只需改 libs.versions.toml
    val target = JavaVersion.toVersion(libs.findVersion("jvmTarget").get().requiredVersion)
    sourceCompatibility = target
    targetCompatibility = target
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = libs.findVersion("jvmTarget").get().requiredVersion
    }
}
