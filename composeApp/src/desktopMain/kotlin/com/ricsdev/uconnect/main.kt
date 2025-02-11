package com.ricsdev.uconnect

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.ricsdev.uconnect.di.initKoin

fun main() = application {

    initKoin()


    Window(
        state = rememberWindowState(width = 850.dp, height = 650.dp),
        onCloseRequest = ::exitApplication,
        title = "uConnect",
    ) {
        App()
    }
}