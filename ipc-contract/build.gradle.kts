// 应用 Android 库约定插件，获得 compileSdk / minSdk / jvmTarget 等通用配置
plugins {
    id("convention.android-library")
    alias(libs.plugins.kotlin.parcelize) // Parcelable 代码生成，仅此模块需要
}

android {
    namespace = "com.cn.ipc.contract"

    buildFeatures {
        aidl = true // 启用 AIDL 编译支持，仅此模块需要
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
}
