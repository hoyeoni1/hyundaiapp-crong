pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://nexus-playground.pleos.ai/repository/maven-releases/")
    }
}
rootProject.name = "CruiseBreaker"
include(":app")
// 개발 중에는 옆 폴더의 공통 저장소를 쓰고, 제출 전 subtree로 넣으면 ./core를 쓴다
include(":core")
project(":core").projectDir = file("core").takeIf { it.exists() } ?: file("../hyundaiapp-common/core")
