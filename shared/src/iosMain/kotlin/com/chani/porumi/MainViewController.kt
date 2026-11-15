package com.chani.porumi

import androidx.compose.ui.window.ComposeUIViewController
import com.chani.porumi.di.initKoin

fun MainViewController() = ComposeUIViewController(
    configure = { initKoin() },
) { App() }
