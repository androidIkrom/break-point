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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPosition
import java.awt.Dimension
import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import java.awt.Toolkit

fun main() = application {
    AppLog.install()
    AppContainer.localRepository.init()
    DemoData.seedIfFresh()
    // Never WindowPlacement.Maximized: for an undecorated window Compose maximizes over the
    // taskbar. The window is sized to the usable screen area instead (see fillScreen below).
    val windowState = rememberWindowState(placement = WindowPlacement.Floating, position = WindowPosition(Alignment.Center), size = DpSize(1280.dp, 820.dp))
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
        /** Fills the screen of the window's monitor, leaving the taskbar visible. */
        fun fillScreen() {
            val usable = usableScreenBounds(window)
            windowState.placement = WindowPlacement.Floating
            windowState.position = WindowPosition(usable.x.dp, usable.y.dp)
            windowState.size = DpSize(usable.width.dp, usable.height.dp)
        }
        fun isFilled(): Boolean {
            val usable = usableScreenBounds(window)
            return windowState.size.width >= (usable.width - 2).dp && windowState.size.height >= (usable.height - 2).dp
        }

        // Layouts are designed down to this size; smaller windows would clip the admin table
        LaunchedEffect(Unit) {
            window.minimumSize = Dimension(1024, 700)
            // Compose writes the window's initial bounds back into the state when the window is
            // shown, so the fill must come after that or it is overwritten
            while (!window.isShowing) kotlinx.coroutines.delay(16)
            kotlinx.coroutines.delay(120)
            fillScreen()
        }
        // Win+Up, Aero Snap or a double-click on the title bar maximize an undecorated window over
        // the taskbar; turn that into a fill of the usable area instead
        DisposableEffect(Unit) {
            val listener = java.awt.event.WindowStateListener { event ->
                if (event.newState and java.awt.Frame.MAXIMIZED_BOTH != 0) {
                    window.extendedState = java.awt.Frame.NORMAL
                    fillScreen()
                }
            }
            window.addWindowStateListener(listener)
            onDispose { window.removeWindowStateListener(listener) }
        }

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
                                    icon = if (isFilled()) Icons.Default.FilterNone else Icons.Default.CropSquare,
                                    contentDescription = "Kattalashtirish",
                                    onClick = {
                                        if (isFilled()) {
                                            windowState.size = DpSize(1280.dp, 820.dp)
                                            windowState.position = WindowPosition(Alignment.Center)
                                        } else {
                                            // Recomputed each time: the window may have moved to another screen
                                            fillScreen()
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

/** The window's screen minus the taskbar (and any other docked bar), in the same units AWT uses for the window. */
private fun usableScreenBounds(window: java.awt.Window): Rectangle {
    val gc = window.graphicsConfiguration ?: return GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
    val bounds = gc.bounds
    val insets = Toolkit.getDefaultToolkit().getScreenInsets(gc)
    val usable = Rectangle(
        bounds.x + insets.left,
        bounds.y + insets.top,
        bounds.width - insets.left - insets.right,
        bounds.height - insets.top - insets.bottom
    )
    return usable
}
