package com.ricsdev.ucam

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.ricsdev.ucam.di.initKoin

fun main() = application {

    initKoin()


    Window(
        onCloseRequest = ::exitApplication,
        title = "uCam",
    ) {
        App()
    }
}