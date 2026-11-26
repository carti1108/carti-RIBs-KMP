// 소스가 하나도 없으면 Kotlin/Native 링크 태스크가 NO-SOURCE로 건너뛰어져 AppShared.xcframework가 생기지 않는다.
// 이 모듈의 내용은 build.gradle.kts의 export 설정이고, 이 파일은 링크가 돌게 하는 자리만 채운다.
package com.carti.ribs.samples.multi.ios
