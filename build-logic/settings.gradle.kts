// build-logic 是一个独立的 Gradle 子项目，专门用于存放约定插件（Convention Plugins）。
// 它不参与主项目的模块依赖图，而是作为 pluginManagement 的本地插件源被主项目引用。
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    // 引用主项目的版本目录，使约定插件内部也能通过 libs.* 访问统一版本
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
