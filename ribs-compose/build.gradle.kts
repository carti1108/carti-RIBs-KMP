plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

// Android Compose 어댑터. iOS의 swift/RIBsKMPSwiftUI에 해당한다(ADR-0006).
android {
    namespace = "com.carti.ribs.compose"
    compileSdk = 37
    defaultConfig {
        minSdk = 24
    }
    buildFeatures {
        compose = true
    }
}

kotlin {
    explicitApi()
}

dependencies {
    api(project(":ribs-core"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.activity.compose)
}
