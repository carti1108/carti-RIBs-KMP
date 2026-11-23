import XCTest

/// 피처 4개를 지나는 흐름. Android `MultiFeatureFlowTest`, 웹 `App.test.tsx`와 같은 시나리오다.
final class MultiFeatureFlowUITests: XCTestCase {
    private var app: XCUIApplication!

    override func setUp() {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launch()
    }

    func testBlankLoginShowsError() {
        app.buttons["loginButton"].tap()

        XCTAssertEqual(app.staticTexts["errorLabel"].label, "이름을 입력하세요")
    }

    func testSignupThenTodoThenSettingsThenLogout() {
        app.buttons["signupLink"].tap()
        let field = app.textFields["nameField"]
        XCTAssertTrue(field.waitForExistence(timeout: 2))
        field.tap()
        field.typeText("carti")
        app.buttons["signupButton"].tap()

        XCTAssertTrue(app.staticTexts["welcomeLabel"].waitForExistence(timeout: 2))
        XCTAssertEqual(app.staticTexts["welcomeLabel"].label, "carti님의 할 일")
        app.buttons["todoRow-1"].tap()

        app.buttons["settingsButton"].tap()
        XCTAssertEqual(app.staticTexts["settingsUserLabel"].label, "carti(으)로 로그인됨")
        app.buttons["closeButton"].tap()

        // 설정에 가려져 있던 동안에도 할 일 RIB은 붙어 있었으므로 체크가 남는다.
        XCTAssertEqual(app.buttons["todoRow-1"].value as? String, "done")

        app.buttons["settingsButton"].tap()
        app.buttons["logoutButton"].tap()
        XCTAssertTrue(app.buttons["loginButton"].waitForExistence(timeout: 2))
    }
}
