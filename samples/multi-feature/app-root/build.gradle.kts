plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
}

// 루트 RIB과 플랫폼 진입점. 피처 impl과 데이터 계층을 아는 유일한 모듈이다. 피처끼리는 서로의 -api만 안다.
kotlin {
    jvm()
    android {
        namespace = "com.carti.ribs.samples.multi.root"
        compileSdk = 37
        minSdk = 24
    }
    js {
        nodejs()
        browser()
    }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(project(":samples:multi-feature:feature:login-api"))
            api(project(":samples:multi-feature:feature:signup-api"))
            api(project(":samples:multi-feature:feature:todo-api"))
            api(project(":samples:multi-feature:feature:settings-api"))
            implementation(project(":samples:multi-feature:feature:login-impl"))
            implementation(project(":samples:multi-feature:feature:signup-impl"))
            implementation(project(":samples:multi-feature:feature:todo-impl"))
            implementation(project(":samples:multi-feature:feature:settings-impl"))
            implementation(project(":samples:multi-feature:data:todo-data"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
