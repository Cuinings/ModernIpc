import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

plugins {
    `maven-publish`
}

// 统一的发布配置：组名和版本号
val sdkGroupId = "com.modernipc"
val sdkVersion = "1.0.0-SNAPSHOT"

afterEvaluate {
    extensions.configure<PublishingExtension> {
        repositories {
            maven {
                name = "ProjectLocal"
                url = uri("${project.rootProject.projectDir}/local-maven")
            }
            
            // 新增 GitHub Packages 远程仓库配置
            maven {
                name = "GitHubPackages"
                // 优先读取 GitHub Actions 环境变量中的仓库名 (例如: "Octocat/ModernIpc")
                val githubRepo = System.getenv("GITHUB_REPOSITORY") ?: "YourGithubName/ModernIpc"
                url = uri("https://maven.pkg.github.com/$githubRepo")
                credentials {
                    // 读取 Actions 自动注入的环境变量，或本地 gradle.properties 中配置的属性
                    username = System.getenv("GITHUB_ACTOR") ?: project.findProperty("gpr.user") as String? ?: ""
                    password = System.getenv("GITHUB_TOKEN") ?: project.findProperty("gpr.key") as String? ?: ""
                }
            }
        }
        
        publications {
            val isAndroid = project.plugins.hasPlugin("com.android.library")
            val isJvm = project.plugins.hasPlugin("org.jetbrains.kotlin.jvm")

            if (isAndroid) {
                create<MavenPublication>("release") {
                    from(components["release"])
                    groupId = sdkGroupId
                    artifactId = project.name
                    version = sdkVersion
                }
            } else if (isJvm) {
                create<MavenPublication>("java") {
                    from(components["java"])
                    groupId = sdkGroupId
                    artifactId = project.name
                    version = sdkVersion
                }
            }
        }
    }
}
