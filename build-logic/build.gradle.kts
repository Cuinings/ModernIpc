// build-logic 子项目的构建文件。
// 在此声明约定插件所需的编译依赖（AGP、Kotlin Gradle Plugin），
// 使 src/main/kotlin/ 下的 *.gradle.kts 文件能够使用这些 API。
plugins {
    `kotlin-dsl` // 启用 Kotlin DSL 支持，使 .gradle.kts 文件可作为预编译脚本插件
}

dependencies {
    // 约定插件内部需要调用 Android Library API → 依赖 AGP
    implementation(libs.android.gradlePlugin)
    // 约定插件内部需要调用 Kotlin Gradle Plugin API
    implementation(libs.kotlin.gradlePlugin)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "17"
    }
}
tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = "17"
    targetCompatibility = "17"
}
