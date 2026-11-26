package com.carti.ribs.samples.multi.android

import android.os.Bundle
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.carti.ribs.MainThreadChecker
import com.carti.ribs.RibThreadingConfig
import com.carti.ribs.ViewHost
import com.carti.ribs.compose.ComposeScreenSlot
import com.carti.ribs.compose.RibBackPressedCallback
import com.carti.ribs.compose.RibSlot
import com.carti.ribs.samples.multi.root.MultiFeatureApp
import com.carti.ribs.samples.multi.root.RootViewControllable

/**
 * RIB 트리를 소유하는 `ViewHost`. 화면 회전에도 트리를 유지하고 화면을 떠날 때 내린다.
 * [todoBaseUrl]이 있으면 그 서버로 실제 요청을 보내고, 없으면 앱 안의 샘플 서버를 쓴다.
 */
class AppViewModel(todoBaseUrl: String?) : ViewModel(), ViewHost<RootViewControllable> {
    val windowSlot = ComposeScreenSlot()
    private val app = if (todoBaseUrl == null) {
        MultiFeatureApp(AppComponent())
    } else {
        MultiFeatureApp(AppComponent(), todoBaseUrl)
    }

    init {
        RibThreadingConfig.mainThreadChecker = LooperMainThreadChecker
        app.start(this)
    }

    fun handleBackPress(): Boolean = app.handleBackPress()

    override fun show(view: RootViewControllable) = windowSlot.show(view)

    override fun hide(view: RootViewControllable) = windowSlot.hide(view)

    override fun onCleared() {
        app.stop()
    }
}

object LooperMainThreadChecker : MainThreadChecker {
    override fun isMainThread(): Boolean = Looper.myLooper() == Looper.getMainLooper()
}

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels {
        viewModelFactory { initializer { AppViewModel(intent.getStringExtra(EXTRA_TODO_BASE_URL)) } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(
            this,
            RibBackPressedCallback(onBackPressedDispatcher, viewModel::handleBackPress),
        )
        setContent {
            MaterialTheme {
                Surface { RibSlot(viewModel.windowSlot) }
            }
        }
    }

    companion object {
        /** 할 일 서버 주소(예: `http://10.0.2.2:8080/api`). 없으면 샘플 서버를 쓴다. */
        const val EXTRA_TODO_BASE_URL: String = "todoBaseUrl"
    }
}
