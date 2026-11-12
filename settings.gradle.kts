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
