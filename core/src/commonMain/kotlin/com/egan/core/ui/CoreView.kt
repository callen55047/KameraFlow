package com.egan.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

abstract class CoreView {
    abstract val viewModel: CoreViewModel

    @Composable
    abstract fun content()

    @Composable
    fun render() {
        LaunchedEffect(Unit) {
            viewModel.onViewRendered()
        }

        val lifecycleOwner = LocalLifecycleOwner.current
        val lifecycleState = remember { mutableStateOf(Lifecycle.Event.ON_ANY) }

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                lifecycleState.value = event
            }
            lifecycleOwner.lifecycle.addObserver(observer)

            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                viewModel.onViewDestroyed()
                println("Current lifecycle state: ${lifecycleState.value}")
            }
        }

        content()
    }
}

abstract class CoreViewModel {
    abstract fun onViewRendered()
    abstract fun onViewDestroyed()
}