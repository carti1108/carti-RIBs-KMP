plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.kmp.library)
}

// 할 일 데이터 계층. todo-api의 TodoRepository를 구현한다.
// 이 모듈을 아는 곳은 조립 모듈(app-root)뿐이다. 피처 impl은 의존하지 않는다(ADR-0012).
kotlin {
    jvm()
    android {
        namespace = "com.carti.ribs.samples.multi.todo.data"
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
            api(project(":samples:multi-feature:feature:todo-api"))
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
