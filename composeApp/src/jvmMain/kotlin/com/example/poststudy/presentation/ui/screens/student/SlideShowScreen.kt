package com.example.poststudy.presentation.ui.screens.student

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.poststudy.presentation.ui.components.BackButton
import com.example.poststudy.presentation.theme.AppDesign
import com.example.poststudy.presentation.ui.components.HelpIcon
import com.example.poststudy.presentation.ui.components.PostStudyDialog
import kotlinx.coroutines.delay
import java.awt.image.BufferedImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlideShowScreen(
    slides: List<BufferedImage>,
    slideTimerSeconds: Int,
    // True when a test follows the slides; changes the wording of the finish dialog
    hasTestAfter: Boolean,
    onFinished: () -> Unit,
    onBack: () -> Unit
) {
    var currentSlideIndex by remember { mutableStateOf(0) }
    var timeLeftSeconds by remember { mutableStateOf(slideTimerSeconds) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showBackDialog by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }


    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        if (slideTimerSeconds > 0) {
            while (timeLeftSeconds > 0) {
                delay(1000)
                timeLeftSeconds--
            }
            onFinished()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppDesign.BackgroundGradient)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            modifier = Modifier
                .focusRequester(focusRequester)
                .onPreviewKeyEvent {
                    if (it.type == KeyEventType.KeyDown) {
                        when (it.key) {
                            Key.DirectionRight -> {
                                if (currentSlideIndex < slides.size - 1) currentSlideIndex++ else showExitDialog = true
                                true
                            }
                            Key.DirectionLeft -> {
                                if (currentSlideIndex > 0) currentSlideIndex--
                                true
                            }
                            Key.Enter -> {
                                if (currentSlideIndex == slides.size - 1) showExitDialog = true
                                true
                            }
                            Key.Escape -> {
                                showBackDialog = true
                                true
                            }
                            else -> false
                        }
                    } else false
                },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "O'rganish: ${currentSlideIndex + 1} / ${slides.size}-slayd",
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF065F46)
                        )
                    },
                    navigationIcon = {
                        BackButton(onClick = { showBackDialog = true })
                    },
                    actions = {
                        HelpIcon(
                            title = "Taqdimotni ko'rish",
                            helpText = "Slaydlarni ko'rib chiqing va o'rganing. Navigatsiya uchun tugmalardan yoki klaviaturadagi O'ng/Chap strelkalardan foydalanishingiz mumkin.",
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        if (slideTimerSeconds > 0) {
                            Surface(
                                color = Color(0xFF6366F1),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.padding(end = 16.dp)
                            ) {
                                val minutes = timeLeftSeconds / 60
                                val seconds = timeLeftSeconds % 60
                                Text(
                                    text = "%d:%02d".format(minutes, seconds),
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (slides.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Black)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Image(
                                bitmap = slides[currentSlideIndex].toComposeImageBitmap(),
                                contentDescription = "Slide ${currentSlideIndex + 1}",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { if (currentSlideIndex > 0) currentSlideIndex-- },
                        enabled = currentSlideIndex > 0,
                        modifier = Modifier.height(60.dp).width(180.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF6366F1)),
                        border = BorderStroke(2.dp, Color(0xFF6366F1))
                    ) {
                        Text("Oldingi", fontWeight = FontWeight.Bold)
                    }

                    LinearProgressIndicator(
                        progress = { (currentSlideIndex + 1).toFloat() / slides.size },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 48.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = Color(0xFF6366F1),
                        trackColor = Color(0xFF6366F1).copy(alpha = 0.15f)
                    )

                    Button(
                        onClick = {
                            if (currentSlideIndex < slides.size - 1) {
                                currentSlideIndex++
                            } else {
                                showExitDialog = true
                            }
                        },
                        modifier = Modifier.height(60.dp).width(180.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                    ) {
                        Text(
                            text = if (currentSlideIndex < slides.size - 1) "Keyingisi" else "O'rganishni yakunlash",
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }

    if (showExitDialog) {
        PostStudyDialog(
            onDismissRequest = { showExitDialog = false },
            title = if (hasTestAfter) "O'rganish yakunlansinmi?" else "Taqdimotni tugatish?",
            text = if (hasTestAfter)
                "O'quv mashg'ulotini muddatidan oldin tugatib, testni boshlashga ishonchingiz komilmi?"
            else
                "Taqdimotni ko'rib bo'ldingizmi? Orqaga qaytishni xohlaysizmi?",
            confirmText = "Ha, yakunlash",
            onConfirm = {
                showExitDialog = false
                onFinished()
            }
        )
    }

    if (showBackDialog) {
        PostStudyDialog(
            onDismissRequest = { showBackDialog = false },
            title = "Sessiyadan chiqish?",
            text = "Chiqishga ishonchingiz komilmi? Sizning natijalaringiz saqlanmaydi va siz tanlov ekraniga qaytasiz.",
            confirmText = "Ha, chiqish",
            confirmColor = Color(0xFFEF4444), // Rose/Red
            onConfirm = {
                showBackDialog = false
                onBack()
            }
        )
    }
}
