// 应用 JVM 库约定插件，获得统一的 Java 源码兼容性配置
plugins {
    id("convention.jvm-library")
}

dependencies {
    implementation(kotlin("stdlib"))
}