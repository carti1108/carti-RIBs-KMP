package com.carti.ribs.compose

import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import com.carti.ribs.Router

/**
 * 시스템 뒤로 가기를 루트 [Router.handleBackPress]로 보낸다. Android 원본 `RibActivity.onBackPressed`와 같은 흐름이다.
 * 트리가 처리하지 않으면(false) 이 콜백을 잠시 끄고 디스패처를 다시 불러 시스템 기본 동작(앱 닫기 등)을 따른다.
 */
public class RibBackPressedCallback(
    private val dispatcher: OnBackPressedDispatcher,
    private val handleBackPress: () -> Boolean,
) : OnBackPressedCallback(true) {
    override fun handleOnBackPressed() {
        if (handleBackPress()) return
        isEnabled = false
        dispatcher.onBackPressed()
        isEnabled = true
    }
}
