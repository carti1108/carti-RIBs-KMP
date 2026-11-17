/*
 * RIB 스캐폴딩. Mac·Windows 모두에서 추가 설치 없이 돈다.
 *
 * ./gradlew newRib -Pname=Profile -Ppackage=com.example.app.profile \
 *     -PkotlinDir=app-shared/src/commonMain/kotlin \
 *     [-PswiftDir=ios/Sources -PswiftModule=AppShared] [-PcomposeDir=android/src/main/kotlin -PcomposePackage=com.example.android]
 *
 * 만드는 파일(이미 있으면 실패하고 아무것도 덮어쓰지 않는다):
 * - <kotlinDir>/<package 경로>/<Name>.kt: 뷰 계약, Dependency, Listener, Builder, Interactor, Router
 * - <kotlinDir의 commonMain → commonTest>/<package 경로>/<Name>InteractorTest.kt
 * - <swiftDir>/<Name>Screen.swift (선택), <composeDir>/<composePackage 경로>/<Name>Screen.kt (선택)
 */

val newRib = tasks.register("newRib") {
    group = "ribs"
    description = "Generates a RIB (Kotlin) with optional SwiftUI and Compose screens."
    val name = providers.gradleProperty("name")
    val pkg = providers.gradleProperty("package")
    val kotlinDir = providers.gradleProperty("kotlinDir")
    val swiftDir = providers.gradleProperty("swiftDir")
    val swiftModule = providers.gradleProperty("swiftModule")
    val composeDir = providers.gradleProperty("composeDir")
    val composePackage = providers.gradleProperty("composePackage")
    val root = layout.projectDirectory
    doLast {
        val ribName = name.orNull ?: error("-Pname is required (e.g. -Pname=Profile)")
        val packageName = pkg.orNull ?: error("-Ppackage is required (e.g. -Ppackage=com.example.profile)")
        val sourceDir = kotlinDir.orNull ?: error("-PkotlinDir is required (e.g. -PkotlinDir=shared/src/commonMain/kotlin)")
        require(Regex("[A-Z][A-Za-z0-9]*").matches(ribName)) { "-Pname must be PascalCase: $ribName" }
        require(Regex("[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*").matches(packageName)) { "Invalid -Ppackage: $packageName" }
        require("commonMain" in sourceDir) { "-PkotlinDir must point at a commonMain source root: $sourceDir" }

        val packagePath = packageName.replace('.', '/')
        val outputs = mutableMapOf(
            root.file("$sourceDir/$packagePath/$ribName.kt").asFile to ribKotlin(ribName, packageName),
            root.file("${sourceDir.replace("commonMain", "commonTest")}/$packagePath/${ribName}InteractorTest.kt").asFile
                to ribTest(ribName, packageName),
        )
        swiftDir.orNull?.let { dir ->
            val module = swiftModule.orNull ?: error("-PswiftModule (your Kotlin framework name) is required with -PswiftDir")
            outputs[root.file("$dir/${ribName}Screen.swift").asFile] = ribSwift(ribName, module)
        }
        composeDir.orNull?.let { dir ->
            val composePkg = composePackage.orNull ?: error("-PcomposePackage is required with -PcomposeDir")
            outputs[root.file("$dir/${composePkg.replace('.', '/')}/${ribName}Screen.kt").asFile] =
                ribCompose(ribName, packageName, composePkg)
        }

        val existing = outputs.keys.filter { it.exists() }
        require(existing.isEmpty()) { "Refusing to overwrite: ${existing.joinToString()}" }
        outputs.forEach { (file, content) ->
            file.parentFile.mkdirs()
            file.writeText(content)
            println("created ${file.relativeTo(root.asFile)}")
        }
        println("Next: implement ${ribName}Dependency in the parent Component (forward make${ribName}View() up to the platform root dependency), wire ${ribName}Builder in the parent Router.")
    }
}

fun ribKotlin(n: String, pkg: String): String = """
package $pkg

import com.carti.ribs.Builder
import com.carti.ribs.Dependency
import com.carti.ribs.Presentable
import com.carti.ribs.PresentableInteractor
import com.carti.ribs.ViewControllable
import com.carti.ribs.ViewableRouter

/** 뷰 → 공유 코드: 사용자 입력. */
interface ${n}PresentableListener {
    fun didTapClose()
}

/** Interactor → 뷰: 플랫폼이 구현하는 뷰 계약(ADR-0006). 메인 스레드에서만 부른다(ADR-0007). */
interface ${n}Presentable : Presentable {
    /** 활성 구간에만 설정된다. 플랫폼은 약한 참조로 둔다. */
    var listener: ${n}PresentableListener?
    fun showTitle(title: String)
}

/** Router → 뷰. 자식 화면을 붙일 때 메서드를 추가한다. */
interface ${n}ViewControllable : ViewControllable

/** 플랫폼 뷰 하나가 구현하는 묶음. Interactor는 [${n}Presentable]로, Router는 [${n}ViewControllable]로만 본다. */
interface ${n}ViewContract : ${n}Presentable, ${n}ViewControllable

/** 부모가 제공해야 하는 것. */
interface ${n}Dependency : Dependency {
    fun make${n}View(): ${n}ViewContract
}

/** 자식 → 부모: 사용자 의도로 표현한다. 라우팅은 부모가 정한다. */
interface ${n}Listener {
    fun didRequestClose$n()
}

class ${n}Builder(dependency: ${n}Dependency) : Builder<${n}Dependency>(dependency) {
    fun build(listener: ${n}Listener): ${n}Router {
        val view = dependency.make${n}View()
        return ${n}Router(${n}Interactor(view, listener), view)
    }
}

class ${n}Interactor(
    presenter: ${n}Presentable,
    private val listener: ${n}Listener,
) : PresentableInteractor<${n}Presentable>(presenter), ${n}PresentableListener {
    /** Router가 만들어질 때 자신을 넣는다(RIBs-iOS 템플릿과 같음). */
    var router: ${n}Router? = null

    override fun didBecomeActive() {
        presenter.listener = this
        presenter.showTitle("$n")
    }

    override fun willResignActive() {
        presenter.listener = null
    }

    override fun didTapClose() = listener.didRequestClose$n()
}

class ${n}Router(interactor: ${n}Interactor, view: ${n}ViewControllable) :
    ViewableRouter<${n}Interactor, ${n}ViewControllable>(interactor, view) {
    init {
        interactor.router = this
    }
}
""".trimStart()

fun ribTest(n: String, pkg: String): String = """
package $pkg

import com.carti.ribs.RibCoroutinesConfig
import com.carti.ribs.RibDispatchersProvider
import com.carti.ribs.mount
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

// commonTest에 kotlinx-coroutines-test 의존성이 필요하다.
@OptIn(ExperimentalCoroutinesApi::class)
class ${n}InteractorTest {
    @BeforeTest
    fun setUp() {
        RibCoroutinesConfig.dispatchers = SingleTestDispatcher(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        RibCoroutinesConfig.reset()
    }

    @Test
    fun activateShowsTitleAndDeactivateClearsListener() {
        val view = Fake${n}View()
        val router = ${n}Builder(FakeDependency(view)).build(FakeListener())

        val mount = router.mount()
        assertEquals("$n", view.title)

        mount.unmount()
        assertNull(view.listener)
    }

    @Test
    fun closeTapIsForwardedToParent() {
        val view = Fake${n}View()
        val listener = FakeListener()
        ${n}Builder(FakeDependency(view)).build(listener).mount()

        view.listener?.didTapClose()

        assertTrue(listener.closeRequested)
    }

    private class Fake${n}View : ${n}ViewContract {
        override var listener: ${n}PresentableListener? = null
        var title: String? = null
        override fun showTitle(title: String) { this.title = title }
    }

    private class FakeDependency(private val view: ${n}ViewContract) : ${n}Dependency {
        override fun make${n}View(): ${n}ViewContract = view
    }

    private class FakeListener : ${n}Listener {
        var closeRequested = false
        override fun didRequestClose$n() { closeRequested = true }
    }

    private class SingleTestDispatcher(dispatcher: CoroutineDispatcher) : RibDispatchersProvider {
        override val Main: CoroutineDispatcher = dispatcher
        override val MainImmediate: CoroutineDispatcher = dispatcher
        override val Default: CoroutineDispatcher = dispatcher
        override val Unconfined: CoroutineDispatcher = dispatcher
    }
}
""".trimStart()

fun ribSwift(n: String, module: String): String = """
import $module
import RIBsKMPSwiftUI
import SwiftUI

/// ${n} RIB의 뷰. 상태는 공유 코드가 넘기고, 입력은 `listener`로 돌려준다.
@MainActor
@Observable
final class ${n}Screen: NSObject, @preconcurrency ${n}ViewContract, SwiftUIRenderable {
    @ObservationIgnored weak var listener: (any ${n}PresentableListener)?
    private(set) var title = ""

    func showTitle(title: String) {
        self.title = title
    }

    func makeView() -> AnyView {
        AnyView(${n}View(screen: self))
    }
}

private struct ${n}View: View {
    let screen: ${n}Screen

    var body: some View {
        VStack(spacing: 16) {
            Text(screen.title).font(.title2.bold())
            Button("닫기") { screen.listener?.didTapClose() }
        }
        .padding(24)
    }
}
""".trimStart()

fun ribCompose(n: String, pkg: String, composePkg: String): String = """
package $composePkg

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.carti.ribs.compose.ComposeRenderable
import $pkg.${n}ViewContract
import $pkg.${n}PresentableListener

/** ${n} RIB의 뷰. */
class ${n}Screen : ${n}ViewContract, ComposeRenderable {
    override var listener: ${n}PresentableListener? = null
    private var title: String by mutableStateOf("")

    override fun showTitle(title: String) {
        this.title = title
    }

    @Composable
    override fun Content() {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(title)
            Button(onClick = { listener?.didTapClose() }) { Text("닫기") }
        }
    }
}
""".trimStart()
