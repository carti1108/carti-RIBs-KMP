import XCTest

/// 공유 코드(LoginShared)의 RIB 트리가 SwiftUI 화면을 실제로 전환하는지 확인한다.
final class LoginFlowUITests: XCTestCase {
    private let timeout: TimeInterval = 5
    private var app: XCUIApplication!

    @MainActor
    override func setUp() {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launch()
    }

    @MainActor
    func testBlankNameShowsError() {
        app.buttons["loginButton"].tap()

        XCTAssertTrue(app.staticTexts["errorLabel"].waitForExistence(timeout: timeout))
    }

    @MainActor
    func testLoginThenLogoutReturnsToEmptyLoggedOutScreen() {
        let field = app.textFields["nameField"]
        XCTAssertTrue(field.waitForExistence(timeout: timeout))
        field.tap()
        field.typeText("  carti ")
        app.buttons["loginButton"].tap()

        let welcome = app.staticTexts["welcomeLabel"]
        XCTAssertTrue(welcome.waitForExistence(timeout: timeout))
        XCTAssertEqual(welcome.label, "carti님, 환영합니다")

        app.buttons["logoutButton"].tap()

        XCTAssertTrue(field.waitForExistence(timeout: timeout))
        XCTAssertEqual(field.value as? String, "이름")
        XCTAssertFalse(app.staticTexts["errorLabel"].exists)
    }
}
