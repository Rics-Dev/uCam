package com.ricsdev.ucam

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.ricsdev.ucam.di.initKoin

fun main() = application {

    initKoin()


    Window(
        state = rememberWindowState(width = 850.dp, height = 650.dp),
        onCloseRequest = ::exitApplication,
        title = "uCam",
    ) {
        App()
    }
}