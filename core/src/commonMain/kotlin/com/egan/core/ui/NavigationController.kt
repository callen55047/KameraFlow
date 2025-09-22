package com.egan.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.egan.core.patterns.IEventFlow
import com.egan.core.patterns.IEventParcel
import com.egan.core.patterns.Injection.using
import com.egan.core.platform.backgroundScope
import com.egan.core.ui.views.ECoreView
import com.egan.core.ui.views.WelcomeView
import kotlinx.coroutines.launch


@Composable
fun ComposeNavigation(startingView: ECoreView) {
    val allViews = remember { mutableStateOf(ECoreView.entries) }
    val currentView = remember { mutableStateOf(startingView) }
    val eventFlow = using<IEventFlow>()

    fun setCurrentView(view: ECoreView) {
        currentView.value = view
    }

    LaunchedEffect(Unit) {
        backgroundScope().launch {
            eventFlow.subscribeTo<IEventParcel.NavBack>().collect {
                val prevIndex = allViews.value.indexOfFirst { it == currentView.value } - 1
                setCurrentView(allViews.value[prevIndex])
            }
        }

        backgroundScope().launch {
            eventFlow.subscribeTo<IEventParcel.NavForward>().collect {
                val nextIndex = allViews.value.indexOfFirst { it == currentView.value } + 1
                setCurrentView(allViews.value[nextIndex])
            }
        }
    }

    // allows SDK to override specific views
//    val overrideView = overrides?.firstOrNull { it.type === currentView.value }

    Box(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        // main view display
        when (currentView.value) {
            ECoreView.INIT -> WelcomeView().render()
            ECoreView.WELCOME -> WelcomeView().render()
//            ECoreView.CAPTURE -> CaptureView().render()
//            ECoreView.REVIEW -> ReviewView().render()
            else -> Box {}
        }

        // Card view overlay components
    }
}
