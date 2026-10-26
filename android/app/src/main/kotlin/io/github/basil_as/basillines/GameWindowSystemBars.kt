package io.github.basil_as.basillines

import android.content.res.Configuration
import android.view.ViewTreeObserver
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/** Each focused window must request the same bars; activity flags do not apply to dialogs. */
internal fun applyGameWindowImmersive(window: Window, landscape: Boolean) {
    val controller = WindowCompat.getInsetsController(window, window.decorView)
    if (landscape) {
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    } else {
        controller.show(WindowInsetsCompat.Type.systemBars())
    }
}

/** Apply before the dialog's first draw and when it regains focus after another window. */
@Composable
internal fun GameDialogSystemBars() {
    val view = LocalView.current
    val window = (view.parent as? DialogWindowProvider)?.window ?: return
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    DisposableEffect(view, window, landscape) {
        val listener = ViewTreeObserver.OnWindowFocusChangeListener { focused ->
            if (focused) applyGameWindowImmersive(window, landscape)
        }
        view.viewTreeObserver.addOnWindowFocusChangeListener(listener)
        applyGameWindowImmersive(window, landscape)
        onDispose {
            // A pre-attach observer can be merged into the attached tree, so remove from the
            // current observer rather than retaining that earlier observer.
            if (view.viewTreeObserver.isAlive) {
                view.viewTreeObserver.removeOnWindowFocusChangeListener(listener)
            }
        }
    }
}
