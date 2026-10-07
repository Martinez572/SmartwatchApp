package com.example.smartwatchapp.presentation

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.example.smartwatchapp.presentation.theme.SmartwatchAppTheme
import kotlinx.coroutines.delay

data class PresentationSection(
    val title: String,
    val notes: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmartwatchAppTheme {
                MainAppScreen(context = applicationContext)
            }
        }
    }
}

fun triggerVibration(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(400)
    }
}

@Composable
fun MainAppScreen(context: Context) {
    val listState = rememberScalingLazyListState()
    var currentScreen by remember { mutableStateOf("home") }

    var totalMinutes by remember { mutableStateOf(5) }
    var timeLeftSeconds by remember { mutableStateOf(300) }
    var isRunning by remember { mutableStateOf(false) }

    // Secciones totalmente personalizadas por el usuario
    val userSections = remember {
        mutableStateListOf(
            PresentationSection("MI INTRODUCCIÓN", "Saludar al público y explicar el objetivo principal."),
            PresentationSection("DESARROLLO TÉCNICO", "Detallar los puntos clave, datos y análisis."),
            PresentationSection("CONCLUSIÓN", "Cierre de la exposición y agradecimientos.")
        )
    }
    var currentSectionIndex by remember { mutableStateOf(0) }

    LaunchedEffect(isRunning, timeLeftSeconds) {
        if (isRunning && timeLeftSeconds > 0) {
            delay(1000L)
            timeLeftSeconds--
        } else if (timeLeftSeconds == 0 && isRunning) {
            isRunning = false
            triggerVibration(context)
        }
    }

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (currentScreen) {
                "home" -> {
                    item {
                        Text(
                            text = "Expositor Pro",
                            fontSize = 14.sp,
                            color = MaterialTheme.colors.primary,
                            textAlign = TextAlign.Center
                        )
                    }
                    item { Spacer(modifier = Modifier.height(4.dp)) }
                    item {
                        Button(
                            onClick = {
                                triggerVibration(context)
                                currentScreen = "setup_time"
                            },
                            modifier = Modifier.fillMaxWidth(0.85f).height(36.dp)
                        ) {
                            Text("Nueva Exposición", fontSize = 11.sp)
                        }
                    }
                }
                "setup_time" -> {
                    item {
                        Text(
                            text = "Duración Total",
                            fontSize = 12.sp,
                            color = MaterialTheme.colors.secondary
                        )
                    }
                    item { Spacer(modifier = Modifier.height(2.dp)) }
                    item {
                        Text(
                            text = "${totalMinutes} min",
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    item { Spacer(modifier = Modifier.height(4.dp)) }
                    item {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { if (totalMinutes > 1) totalMinutes-- },
                                modifier = Modifier.size(32.dp)
                            ) { Text("-", fontSize = 12.sp) }
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = { totalMinutes++ },
                                modifier = Modifier.size(32.dp)
                            ) { Text("+", fontSize = 12.sp) }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(6.dp)) }
                    item {
                        Button(
                            onClick = { currentScreen = "setup_sections" },
                            modifier = Modifier.fillMaxWidth(0.85f).height(32.dp)
                        ) {
                            Text("Siguiente: Secciones", fontSize = 10.sp)
                        }
                    }
                }
                "setup_sections" -> {
                    item {
                        Text(
                            text = "Tus Secciones",
                            fontSize = 11.sp,
                            color = MaterialTheme.colors.primary
                        )
                    }
                    item { Spacer(modifier = Modifier.height(2.dp)) }
                    item {
                        Text(
                            text = "Total: ${userSections.size} listas para exponer",
                            fontSize = 9.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                    item { Spacer(modifier = Modifier.height(4.dp)) }

                    items(userSections.size) { index ->
                        Text(
                            text = "• ${userSections[index].title}",
                            fontSize = 10.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }

                    item { Spacer(modifier = Modifier.height(6.dp)) }
                    item {
                        Button(
                            onClick = {
                                triggerVibration(context)
                                timeLeftSeconds = totalMinutes * 60
                                currentSectionIndex = 0
                                isRunning = true
                                currentScreen = "timer"
                            },
                            modifier = Modifier.fillMaxWidth(0.85f).height(36.dp),
                            colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
                        ) {
                            Text("¡Iniciar Exposición!", fontSize = 10.sp)
                        }
                    }
                }
                "timer" -> {
                    val minutes = timeLeftSeconds / 60
                    val seconds = timeLeftSeconds % 60
                    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

                    item {
                        Text(
                            text = "$timeFormatted MIN",
                            fontSize = 14.sp,
                            color = if (timeLeftSeconds <= 30) Color.Red else Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                    item { Spacer(modifier = Modifier.height(2.dp)) }

                    val currentSection = userSections.getOrElse(currentSectionIndex) { userSections[0] }

                    item {
                        Card(
                            onClick = {
                                if (currentSectionIndex < userSections.size - 1) {
                                    currentSectionIndex++
                                } else {
                                    currentSectionIndex = 0
                                }
                                triggerVibration(context)
                            },
                            modifier = Modifier.fillMaxWidth(0.95f)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "TÍTULO",
                                    fontSize = 8.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = currentSection.title,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colors.primary,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "NOTAS",
                                    fontSize = 8.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = currentSection.notes,
                                    fontSize = 10.sp,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "(${currentSectionIndex + 1}/${userSections.size}) - Toca para siguiente",
                                    fontSize = 7.sp,
                                    color = Color.LightGray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(4.dp)) }

                    item {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { isRunning = !isRunning },
                                modifier = Modifier.width(70.dp).height(28.dp)
                            ) {
                                Text(if (isRunning) "Pausa" else "Seguir", fontSize = 9.sp)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Button(
                                onClick = {
                                    isRunning = false
                                    currentScreen = "home"
                                },
                                modifier = Modifier.width(70.dp).height(28.dp),
                                colors = ButtonDefaults.buttonColors(backgroundColor = Color.DarkGray)
                            ) {
                                Text("Salir", fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}