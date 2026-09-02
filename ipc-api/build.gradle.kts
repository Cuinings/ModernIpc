plugins {
    id("convention.android-library")
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.cn.ipc.api"
}

dependencies {
    implementation(project(":ipc-contract"))
    implementation(project(":ipc-annotations"))
    implementation(project(":ipc-runtime-client"))
    implementation(project(":ipc-runtime-server"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
    
    ksp(project(":ipc-compiler"))
}