rootProject.name = "carti-ribs-kmp"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}

include(":ribs-core")
include(":ribs-compose")
include(":samples:login-shared")
include(":samples:android-login")
include(":ribs-web")
include(":samples:web-login")

// 피처 4개 모듈화 샘플(samples/multi-feature/README.md)
listOf("login", "signup", "todo", "settings").forEach { feature ->
    include(":samples:multi-feature:feature:$feature-api")
    include(":samples:multi-feature:feature:$feature-impl")
}
include(":samples:multi-feature:app-root")
include(":samples:multi-feature:export:ios-app")
include(":samples:multi-feature:export:web-app")
include(":samples:multi-feature:android-app")
