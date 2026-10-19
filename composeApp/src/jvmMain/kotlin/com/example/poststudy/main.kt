package com.example.poststudy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.FilterNone
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.jetbrains.compose.resources.painterResource
import poststudy.composeapp.generated.resources.Res
import poststudy.composeapp.generated.resources.icon
import com.example.poststudy.data.local.DemoData
import com.example.poststudy.data.session.AppServer
import com.example.poststudy.data.system.AppLog
import com.example.poststudy.di.AppContainer
import com.example.poststudy.presentation.App
import com.example.poststudy.presentation.theme.PostStudyTheme
import com.example.poststudy.presentation.theme.Signal
import com.example.poststudy.presentation.ui.components.SignalIconButton
import java.awt.Dimension

fun main() = application {
    AppLog.install()
    AppContainer.localRepository.init()
    DemoData.seedIfFresh()
    val windowState = rememberWindowState(placement = WindowPlacement.Maximized)
    val serverRunning by AppContainer.networkRepository.serverRunning.collectAsState()
    var windowVisible by remember { mutableStateOf(true) }

    val quit: () -> Unit = {
        AppServer.stop()
        exitApplication()
    }
    // With background mode on, a running server outlives the window and lives in the system tray
    val requestClose: () -> Unit = {
        if (AppContainer.networkRepository.serverRunning.value &&
            AppContainer.localRepository.isBackgroundModeEnabled()
        ) {
            windowVisible = false
        } else {
            quit()
        }
    }

    if (!windowVisible) {
        Tray(
            icon = painterResource(Res.drawable.icon),
            tooltip = if (serverRunning) "BreakPoint — tarmoq ishlamoqda" else "BreakPoint",
            onAction = { windowVisible = true },
            menu = {
                Item("Oynani ochish", onClick = { windowVisible = true })
                Item(if (serverRunning) "Tarmoqni to'xtatish va chiqish" else "Chiqish", onClick = quit)
            }
        )
    }

    Window(
        visible = windowVisible,
        onCloseRequest = requestClose,
        title = "BreakPoint",
        state = windowState,
        undecorated = true, // This makes the window match our UI perfectly
        icon = painterResource(Res.drawable.icon)
    ) {
        // Layouts are designed down to this size; smaller windows would clip the admin table
        LaunchedEffect(Unit) { window.minimumSize = Dimension(1024, 700) }

        PostStudyTheme {
            Column(Modifier.fillMaxSize()) {
                WindowDraggableArea {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(40.dp).background(Signal.GradientTitleBar),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(start = 16.dp, end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "BreakPoint",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Signal.OnDarkMuted,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "v${AppInfo.version}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Signal.OnDarkFaint
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                SignalIconButton(
                                    icon = Icons.Default.Remove,
                                    contentDescription = "Kichraytirish",
                                    onClick = { windowState.isMinimized = true },
                                    tint = Signal.OnDarkMuted,
                                    container = Color.Transparent,
                                    borderColor = null,
                                    size = 32.dp,
                                    iconSize = 16.dp,
                                    onDark = true
                                )
                                SignalIconButton(
                                    icon = if (windowState.placement == WindowPlacement.Maximized) Icons.Default.FilterNone else Icons.Default.CropSquare,
                                    contentDescription = "Kattalashtirish",
                                    onClick = {
                                        windowState.placement = if (windowState.placement == WindowPlacement.Maximized) {
                                            WindowPlacement.Floating
                                        } else {
                                            WindowPlacement.Maximized
                                        }
                                    },
                                    tint = Signal.OnDarkMuted,
                                    container = Color.Transparent,
                                    borderColor = null,
                                    size = 32.dp,
                                    iconSize = 14.dp,
                                    onDark = true
                                )
                                SignalIconButton(
                                    icon = Icons.Default.Close,
                                    contentDescription = "Yopish",
                                    onClick = requestClose,
                                    tint = Signal.OnDarkMuted,
                                    container = Color.Transparent,
                                    borderColor = null,
                                    size = 32.dp,
                                    iconSize = 16.dp,
                                    onDark = true
                                )
                            }
                        }
                    }
                }

                Box(Modifier.weight(1f)) {
                    App()
                }
            }
        }
    }
}
