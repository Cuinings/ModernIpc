// 应用 JVM 库约定插件，获得统一的 Java 源码兼容性配置
plugins {
    id("convention.jvm-library")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":ipc-annotations"))
    // KSP Symbol Processing API，用于编写 KSP 处理器
    implementation(libs.ksp.api)
    // KotlinPoet 用于生成优雅的 Kotlin 代码
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)
}