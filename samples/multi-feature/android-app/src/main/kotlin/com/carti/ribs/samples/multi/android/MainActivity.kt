package com.carti.ribs.samples.multi.android

import android.os.Bundle
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.ViewModel
import com.carti.ribs.MainThreadChecker
import com.carti.ribs.RibThreadingConfig
import com.carti.ribs.ViewHost
import com.carti.ribs.compose.ComposeScreenSlot
import com.carti.ribs.compose.RibBackPressedCallback
import com.carti.ribs.compose.RibSlot
import com.carti.ribs.samples.multi.root.MultiFeatureApp
import com.carti.ribs.samples.multi.root.RootViewControllable

/** RIB 트리를 소유하는 `ViewHost`. 화면 회전에도 트리를 유지하고 화면을 떠날 때 내린다. */
class AppViewModel : ViewModel(), ViewHost<RootViewControllable> {
    val windowSlot = ComposeScreenSlot()
    private val app = MultiFeatureApp(AppComponent())

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
    private val viewModel: AppViewModel by viewModels()

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
}
