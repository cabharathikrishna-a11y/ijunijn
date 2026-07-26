package com.example.desktop

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.example.ui.MainAppContent

fun main() = application {
    val windowState = rememberWindowState(size = DpSize(1280.dp, 800.dp))
    
    Window(
        onCloseRequest = ::exitApplication,
        title = "Life OS Desktop",
        state = windowState
    ) {
        MainAppContent()
    }
}
