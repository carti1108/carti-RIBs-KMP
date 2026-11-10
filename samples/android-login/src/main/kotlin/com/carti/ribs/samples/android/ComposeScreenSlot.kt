package com.carti.ribs.samples.android

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.carti.ribs.ViewControllable

/** Compose로 그릴 수 있는 RIB 뷰. iOS의 `SwiftUIRenderable`에 해당한다(ADR-0006). */
interface ComposeRenderable {
    @Composable
    fun Content()
}

/**
 * RIB 화면 하나를 담는 자리. iOS의 `RIBScreenSlot`에 해당한다.
 * [hide]는 지금 보이는 화면일 때만 비워서, 새 화면을 먼저 보여 준 뒤 옛 화면을 숨겨도 안전하다.
 */
class ComposeScreenSlot {
    var current: ViewControllable? by mutableStateOf(null)
        private set

    fun show(view: ViewControllable) {
        require(view is ComposeRenderable) { "View must implement ComposeRenderable: $view" }
        current = view
    }

    fun hide(view: ViewControllable) {
        if (current === view) current = null
    }
}

/** 슬롯의 현재 화면을 그린다. 화면이 바뀌면 이전 화면의 Compose 상태를 남기지 않는다. */
@Composable
fun RibSlot(slot: ComposeScreenSlot) {
    val view = slot.current as? ComposeRenderable ?: return
    key(view) { view.Content() }
}
