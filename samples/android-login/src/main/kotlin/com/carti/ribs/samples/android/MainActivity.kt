package com.carti.ribs.samples.android

import com.carti.ribs.compose.ComposeRenderable
import com.carti.ribs.compose.ComposeScreenSlot
import com.carti.ribs.compose.RibSlot

import android.os.Bundle
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.carti.ribs.RibThreadingConfig
import com.carti.ribs.ViewHost
import com.carti.ribs.samples.login.DelayedAuthService
import com.carti.ribs.samples.login.LoginApp
import com.carti.ribs.samples.login.RootViewControllable

/**
 * RIB 트리를 소유하고 루트 뷰를 창 슬롯에 붙이는 `ViewHost`. iOS 샘플의 `AppLauncher`에 해당한다.
 * ViewModel에 두어 화면 회전처럼 Activity가 다시 만들어져도 트리를 유지하고, 화면을 완전히 떠날 때 내린다.
 */
class LoginViewModel(loginDelayMillis: Long) : ViewModel(), ViewHost<RootViewControllable> {
    val windowSlot = ComposeScreenSlot()
    private val app = LoginApp(ComposeViewFactory(), DelayedAuthService(loginDelayMillis))

    init {
        // 공유 코드의 생명주기 호출이 메인 스레드인지 검사한다(ADR-0007).
        RibThreadingConfig.mainThreadChecker = LooperMainThreadChecker
        app.start(this)
    }

    override fun show(view: RootViewControllable) = windowSlot.show(view)

    override fun hide(view: RootViewControllable) = windowSlot.hide(view)

    override fun onCleared() {
        app.stop()
    }
}

/** Looper로 메인 스레드 여부를 알려 준다. 어느 스레드에서 불려도 안전하다. */
object LooperMainThreadChecker : com.carti.ribs.MainThreadChecker {
    override fun isMainThread(): Boolean = Looper.myLooper() == Looper.getMainLooper()
}

class MainActivity : ComponentActivity() {
    private val viewModel: LoginViewModel by lazy {
        val delay = intent.getLongExtra(EXTRA_LOGIN_DELAY_MILLIS, DEFAULT_LOGIN_DELAY_MILLIS)
        ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
                    LoginViewModel(delay) as T
            },
        )[LoginViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface { RibSlot(viewModel.windowSlot) }
            }
        }
    }

    companion object {
        /** 테스트가 로딩 상태를 확실히 잡도록 응답 지연을 바꾸는 인텐트 값. */
        const val EXTRA_LOGIN_DELAY_MILLIS: String = "loginDelayMillis"

        /** 샘플 서버 응답 지연. 로딩 표시를 눈으로 확인할 수 있을 만큼만 둔다. */
        const val DEFAULT_LOGIN_DELAY_MILLIS: Long = 800
    }
}
