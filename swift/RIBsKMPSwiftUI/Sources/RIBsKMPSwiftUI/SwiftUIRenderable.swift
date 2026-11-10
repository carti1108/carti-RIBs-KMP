import SwiftUI

/// RIB 뷰를 SwiftUI로 그릴 수 있는 객체.
///
/// 공유 코드는 불투명한 `ViewControllable`만 넘기므로(ADR-0004), 부모 화면은 그 값을 이 프로토콜로
/// 다운캐스트해 SwiftUI 뷰를 꺼낸다. UIKit의 `child as? UIViewController`와 같은 역할이다.
@MainActor
public protocol SwiftUIRenderable: AnyObject {
    func makeView() -> AnyView
}
