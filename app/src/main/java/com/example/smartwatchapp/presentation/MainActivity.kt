package com.example.smartwatchapp.presentation

import android.app.RemoteInput
import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.material.Text
import androidx.wear.input.RemoteInputIntentHelper
import com.example.smartwatchapp.presentation.theme.SmartwatchAppTheme
import kotlinx.coroutines.delay

// =====================================================================
//  COLORES: un solo color primario (violeta) + tonos neutros claros.
//  Si quieres otro color para toda la app, cambia solo ColorPrimary.
// =====================================================================

val ColorPrimary = Color(0xFF6C4DF6)               // el único color de acento
val ColorInk = Color(0xFF1F1A4D)                   // texto principal
val ColorMuted = Color(0xFF7D79AB)                 // texto secundario
val ColorSurface = Color.White                     // tarjetas
val ColorBgTop = Color(0xFFFFFFFF)                 // fondo (arriba)
val ColorBgBottom = Color(0xFFEFEBFF)              // fondo (abajo)
val ColorTint = ColorPrimary.copy(alpha = 0.12f)   // violeta muy suave
val ColorBorder = ColorPrimary.copy(alpha = 0.22f) // bordes de las tarjetas

// =====================================================================
//  DATOS
// =====================================================================

// Un recordatorio: "vibrar cuando falten X minutos"
data class Reminder(val minutesLeft: Int)

// Una sección de la exposición con sus palabras clave
data class Section(val title: String, val keywords: List<String>)

// Un "paso" = una palabra clave junto con la información de su sección.
// Con todos los pasos formamos la secuencia por la que se desliza.
data class Step(
    val sectionNumber: Int,
    val sectionCount: Int,
    val sectionTitle: String,
    val keyword: String,
    val keywordNumber: Int,
    val keywordCount: Int
)

// Todas las pantallas de la app
enum class Screen {
    HOME, DURATION, VIBRATIONS, REMINDER_EDIT, ASK_NOTES,
    SECTION_COUNT, SECTIONS, SECTION_EDIT, TEXT_INPUT,
    PRESENTING, FINISHED
}

// Convierte las secciones en una lista plana de pasos
fun buildSteps(sections: List<Section>): List<Step> {
    val steps = mutableListOf<Step>()
    sections.forEachIndexed { sectionIndex, section ->
        section.keywords.forEachIndexed { keywordIndex, keyword ->
            steps.add(
                Step(
                    sectionNumber = sectionIndex + 1,
                    sectionCount = sections.size,
                    sectionTitle = section.title,
                    keyword = keyword,
                    keywordNumber = keywordIndex + 1,
                    keywordCount = section.keywords.size
                )
            )
        }
    }
    return steps
}

// =====================================================================
//  VIBRACIÓN
// =====================================================================

// Vibra dos veces seguidas (se nota bien en la muñeca)
fun vibrate(context: Context) {
    val vibrator = context.getSystemService(Vibrator::class.java) ?: return
    val pattern = longArrayOf(0, 400, 150, 400) // espera, vibra, pausa, vibra
    vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
}

// =====================================================================
//  ACTIVIDAD
// =====================================================================

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmartwatchAppTheme {
                App()
            }
        }
    }
}

// =====================================================================
//  FONDO: degradado claro con círculos decorativos suaves
// =====================================================================

@Composable
fun AppBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(ColorBgTop, ColorBgBottom)))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = ColorPrimary.copy(alpha = 0.10f),
                radius = size.width * 0.45f,
                center = Offset(size.width * 0.95f, size.height * 0.05f)
            )
            drawCircle(
                color = ColorPrimary.copy(alpha = 0.08f),
                radius = size.width * 0.38f,
                center = Offset(size.width * 0.05f, size.height * 0.95f)
            )
        }
        content()
    }
}

// =====================================================================
//  APP: guarda todos los datos y decide qué pantalla mostrar
// =====================================================================

@Composable
fun App() {
    var screen by remember { mutableStateOf(Screen.HOME) }

    // --- Configuración de la exposición ---
    var durationMinutes by remember { mutableIntStateOf(10) }
    var vibrationsOn by remember { mutableStateOf(true) }
    val reminders = remember { mutableStateListOf<Reminder>() }
    var editingReminder by remember { mutableIntStateOf(-1) } // -1 = nuevo

    // --- Notas ---
    var useNotes by remember { mutableStateOf(false) }
    var sectionCount by remember { mutableIntStateOf(3) }
    val sections = remember { mutableStateListOf<Section>() }
    var editingSection by remember { mutableIntStateOf(0) }

    // --- Pantalla genérica para escribir texto ---
    var textLabel by remember { mutableStateOf("") }
    var textInitial by remember { mutableStateOf("") }
    var textNumeric by remember { mutableStateOf(false) }
    var textReturnTo by remember { mutableStateOf(Screen.HOME) }
    var textOnSave by remember { mutableStateOf<(String) -> Unit>({}) }
    var textOnDelete by remember { mutableStateOf<(() -> Unit)?>(null) }

    // Abre la pantalla de texto. Al guardar, ejecuta onSave y vuelve a returnTo.
    fun askText(
        label: String,
        initial: String,
        returnTo: Screen,
        numeric: Boolean = false,
        onDelete: (() -> Unit)? = null,
        onSave: (String) -> Unit
    ) {
        textLabel = label
        textInitial = initial
        textNumeric = numeric
        textReturnTo = returnTo
        textOnSave = { text ->
            onSave(text)
            screen = returnTo
        }
        textOnDelete = onDelete?.let { delete ->
            {
                delete()
                screen = returnTo
            }
        }
        screen = Screen.TEXT_INPUT
    }

    // Botón "atrás" físico o del emulador
    BackHandler(enabled = screen != Screen.HOME) {
        screen = when (screen) {
            Screen.DURATION -> Screen.HOME
            Screen.VIBRATIONS -> Screen.DURATION
            Screen.REMINDER_EDIT -> Screen.VIBRATIONS
            Screen.ASK_NOTES -> Screen.VIBRATIONS
            Screen.SECTION_COUNT -> Screen.ASK_NOTES
            Screen.SECTIONS -> Screen.SECTION_COUNT
            Screen.SECTION_EDIT -> Screen.SECTIONS
            Screen.TEXT_INPUT -> textReturnTo
            Screen.PRESENTING -> Screen.PRESENTING // durante la exposición no se sale por accidente
            else -> Screen.HOME
        }
    }

    AppBackground {
        when (screen) {
            Screen.HOME -> HomeScreen(
                onStart = { screen = Screen.DURATION }
            )

            Screen.DURATION -> DurationScreen(
                minutes = durationMinutes,
                onChange = { durationMinutes = it },
                onOther = {
                    askText("¿Cuántos minutos?", "", Screen.DURATION, numeric = true) { text ->
                        text.toIntOrNull()?.let { durationMinutes = it.coerceIn(1, 180) }
                    }
                },
                onNext = { screen = Screen.VIBRATIONS },
                onBack = { screen = Screen.HOME }
            )

            Screen.VIBRATIONS -> VibrationsScreen(
                enabled = vibrationsOn,
                reminders = reminders,
                onToggle = { vibrationsOn = !vibrationsOn },
                onEdit = { index ->
                    editingReminder = index
                    screen = Screen.REMINDER_EDIT
                },
                onAdd = {
                    editingReminder = -1
                    screen = Screen.REMINDER_EDIT
                },
                onNext = { screen = Screen.ASK_NOTES },
                onBack = { screen = Screen.DURATION }
            )

            Screen.REMINDER_EDIT -> {
                val maxMinutes = (durationMinutes - 1).coerceAtLeast(1)
                val isNew = editingReminder !in reminders.indices
                ReminderEditScreen(
                    initial = if (isNew) 1 else reminders[editingReminder].minutesLeft,
                    max = maxMinutes,
                    isNew = isNew,
                    onSave = { minutes ->
                        if (isNew) {
                            reminders.add(Reminder(minutes))
                        } else {
                            reminders[editingReminder] = Reminder(minutes)
                        }
                        // Sin repetidos y del más lejano al más cercano al final
                        val cleaned = reminders
                            .distinctBy { it.minutesLeft }
                            .sortedByDescending { it.minutesLeft }
                        reminders.clear()
                        reminders.addAll(cleaned)
                        screen = Screen.VIBRATIONS
                    },
                    onDelete = {
                        if (!isNew) reminders.removeAt(editingReminder)
                        screen = Screen.VIBRATIONS
                    },
                    onBack = { screen = Screen.VIBRATIONS }
                )
            }

            Screen.ASK_NOTES -> AskNotesScreen(
                onYes = {
                    useNotes = true
                    screen = Screen.SECTION_COUNT
                },
                onNo = {
                    useNotes = false
                    screen = Screen.PRESENTING
                },
                onBack = { screen = Screen.VIBRATIONS }
            )

            Screen.SECTION_COUNT -> SectionCountScreen(
                count = sectionCount,
                onChange = { sectionCount = it },
                onNext = {
                    // Ajusta la cantidad de secciones sin borrar las que ya existen
                    while (sections.size < sectionCount) {
                        sections.add(Section("Sección ${sections.size + 1}", emptyList()))
                    }
                    while (sections.size > sectionCount) {
                        sections.removeAt(sections.size - 1)
                    }
                    screen = Screen.SECTIONS
                },
                onBack = { screen = Screen.ASK_NOTES }
            )

            Screen.SECTIONS -> SectionsScreen(
                sections = sections,
                onEdit = { index ->
                    editingSection = index
                    screen = Screen.SECTION_EDIT
                },
                onStart = {
                    useNotes = true
                    screen = Screen.PRESENTING
                },
                onBack = { screen = Screen.SECTION_COUNT }
            )

            Screen.SECTION_EDIT -> {
                if (editingSection in sections.indices) {
                    SectionEditScreen(
                        index = editingSection,
                        section = sections[editingSection],
                        onRename = {
                            askText(
                                "Nombre de la sección",
                                sections[editingSection].title,
                                Screen.SECTION_EDIT
                            ) { text ->
                                sections[editingSection] = sections[editingSection].copy(title = text)
                            }
                        },
                        onAddKeyword = {
                            askText("Nueva palabra clave", "", Screen.SECTION_EDIT) { text ->
                                val current = sections[editingSection]
                                sections[editingSection] = current.copy(keywords = current.keywords + text)
                            }
                        },
                        onEditKeyword = { keywordIndex ->
                            askText(
                                "Editar palabra clave",
                                sections[editingSection].keywords[keywordIndex],
                                Screen.SECTION_EDIT,
                                onDelete = {
                                    val current = sections[editingSection]
                                    sections[editingSection] = current.copy(
                                        keywords = current.keywords.filterIndexed { i, _ -> i != keywordIndex }
                                    )
                                }
                            ) { text ->
                                val current = sections[editingSection]
                                sections[editingSection] = current.copy(
                                    keywords = current.keywords.mapIndexed { i, word ->
                                        if (i == keywordIndex) text else word
                                    }
                                )
                            }
                        },
                        onDone = { screen = Screen.SECTIONS }
                    )
                }
            }

            Screen.TEXT_INPUT -> TextInputScreen(
                label = textLabel,
                initial = textInitial,
                numeric = textNumeric,
                onSave = textOnSave,
                onDelete = textOnDelete,
                onCancel = { screen = textReturnTo }
            )

            Screen.PRESENTING -> {
                // remember: se calcula una sola vez al entrar a la exposición
                val steps = remember { if (useNotes) buildSteps(sections) else emptyList() }
                val reminderMinutes = remember {
                    if (vibrationsOn) {
                        reminders.map { it.minutesLeft }.filter { it < durationMinutes }
                    } else {
                        emptyList()
                    }
                }
                PresentationScreen(
                    totalSeconds = durationMinutes * 60,
                    reminderMinutes = reminderMinutes,
                    vibrationsOn = vibrationsOn,
                    steps = steps,
                    onFinished = { screen = Screen.FINISHED },
                    onExit = { screen = Screen.HOME }
                )
            }

            Screen.FINISHED -> FinishedScreen(
                onHome = { screen = Screen.HOME }
            )
        }
    }
}

// =====================================================================
//  COMPONENTES REUTILIZABLES
// =====================================================================

// Lista vertical con scroll, pensada para pantallas redondas
@Composable
fun ListScreen(title: String? = null, content: ScalingLazyListScope.() -> Unit) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 24.dp)
    ) {
        if (title != null) {
            item {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorInk,
                    textAlign = TextAlign.Center
                )
            }
        }
        content()
    }
}

// Botón grande en forma de píldora:
// lleno (violeta con texto blanco) o suave (violeta claro con texto violeta)
@Composable
fun PillButton(text: String, onClick: () -> Unit, filled: Boolean = true) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clip(CircleShape)
            .background(if (filled) ColorPrimary else ColorTint)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (filled) Color.White else ColorPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

// Botón pequeño para opciones rápidas (5, 10, 15 min)
@Composable
fun SmallPill(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(CircleShape)
            .background(if (selected) ColorPrimary else ColorSurface)
            .border(1.dp, if (selected) ColorPrimary else ColorBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else ColorInk
        )
    }
}

// Botón redondo para + y −
@Composable
fun RoundButton(symbol: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(ColorSurface)
            .border(1.dp, ColorBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = symbol, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = ColorPrimary)
    }
}

// Selector numérico:  (−)  10 min  (+)
@Composable
fun Stepper(value: Int, unit: String, min: Int, max: Int, onChange: (Int) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        RoundButton("−") { if (value > min) onChange(value - 1) }
        Column(
            modifier = Modifier.width(56.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "$value", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = ColorInk)
            Text(text = unit, fontSize = 11.sp, color = ColorMuted)
        }
        RoundButton("+") { if (value < max) onChange(value + 1) }
    }
}

// Fila tipo tarjeta blanca con borde suave
// (para recordatorios, secciones y palabras clave)
@Composable
fun InfoRow(
    title: String,
    subtitle: String? = null,
    accent: Color = ColorPrimary,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(ColorSurface)
            .border(1.dp, ColorBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(accent)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = ColorInk,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(text = subtitle, fontSize = 10.sp, color = ColorMuted, maxLines = 1)
            }
        }
    }
}

@Composable
fun HintText(text: String) {
    Text(text = text, fontSize = 11.sp, color = ColorMuted, textAlign = TextAlign.Center)
}

// Puntos que indican en qué palabra de la sección vas
@Composable
fun ProgressDots(current: Int, total: Int) {
    if (total <= 8) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(total) { i ->
                Box(
                    modifier = Modifier
                        .size(if (i == current) 8.dp else 5.dp)
                        .clip(CircleShape)
                        .background(if (i == current) ColorPrimary else ColorPrimary.copy(alpha = 0.25f))
                )
            }
        }
    } else {
        Text(text = "${current + 1}/$total", fontSize = 10.sp, color = ColorMuted)
    }
}

// =====================================================================
//  PANTALLAS DE CONFIGURACIÓN
// =====================================================================

@Composable
fun HomeScreen(onStart: () -> Unit) {
    ListScreen {
        item {
            Text(text = "Hola 👋", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = ColorInk)
        }
        item {
            Box(
                modifier = Modifier
                    .width(34.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(ColorPrimary)
            )
        }
        item {
            Text(
                text = "¿Listo para tu exposición?",
                fontSize = 13.sp,
                color = ColorMuted,
                textAlign = TextAlign.Center
            )
        }
        item { Spacer(modifier = Modifier.height(4.dp)) }
        item { PillButton("Comenzar exposición", onStart) }
    }
}

@Composable
fun DurationScreen(
    minutes: Int,
    onChange: (Int) -> Unit,
    onOther: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    ListScreen(title = "⏱️ ¿Cuánto durará?") {
        item { Stepper(minutes, "min", 1, 180, onChange) }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(5, 10, 15).forEach { preset ->
                    SmallPill(
                        text = "$preset",
                        selected = minutes == preset,
                        onClick = { onChange(preset) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        item { PillButton("Otra duración", onOther, filled = false) }
        item { PillButton("Siguiente", onNext) }
        item { PillButton("Atrás", onBack, filled = false) }
    }
}

@Composable
fun VibrationsScreen(
    enabled: Boolean,
    reminders: List<Reminder>,
    onToggle: () -> Unit,
    onEdit: (Int) -> Unit,
    onAdd: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    ListScreen(title = "📳 Vibraciones") {
        item {
            InfoRow(
                title = if (enabled) "Activadas" else "Desactivadas",
                subtitle = "Toca para cambiar",
                accent = if (enabled) ColorPrimary else ColorMuted,
                onClick = onToggle
            )
        }
        if (enabled) {
            if (reminders.isEmpty()) {
                item { HintText("Aún no hay recordatorios") }
            }
            reminders.forEachIndexed { index, reminder ->
                item {
                    InfoRow(
                        title = if (reminder.minutesLeft == 1) "Falta 1 min" else "Faltan ${reminder.minutesLeft} min",
                        subtitle = "Toca para editar",
                        onClick = { onEdit(index) }
                    )
                }
            }
            item { PillButton("+ Agregar", onAdd, filled = false) }
        }
        item { PillButton("Siguiente", onNext) }
        item { PillButton("Atrás", onBack, filled = false) }
    }
}

@Composable
fun ReminderEditScreen(
    initial: Int,
    max: Int,
    isNew: Boolean,
    onSave: (Int) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit
) {
    var minutes by remember { mutableIntStateOf(initial.coerceIn(1, max)) }

    ListScreen(title = "Vibrar cuando falten") {
        item { Stepper(minutes, if (minutes == 1) "minuto" else "minutos", 1, max) { minutes = it } }
        item { PillButton("Guardar", onClick = { onSave(minutes) }) }
        if (!isNew) {
            item { PillButton("Eliminar", onDelete, filled = false) }
        }
        item { PillButton("Cancelar", onBack, filled = false) }
    }
}

@Composable
fun AskNotesScreen(onYes: () -> Unit, onNo: () -> Unit, onBack: () -> Unit) {
    ListScreen(title = "📝 ¿Quieres utilizar notas?") {
        item { PillButton("Sí", onYes) }
        item { PillButton("No", onNo, filled = false) }
        item { PillButton("Atrás", onBack, filled = false) }
    }
}

@Composable
fun SectionCountScreen(
    count: Int,
    onChange: (Int) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    ListScreen(title = "¿Cuántas secciones?") {
        item { Stepper(count, if (count == 1) "sección" else "secciones", 1, 10, onChange) }
        item { PillButton("Siguiente", onNext) }
        item { PillButton("Atrás", onBack, filled = false) }
    }
}

@Composable
fun SectionsScreen(
    sections: List<Section>,
    onEdit: (Int) -> Unit,
    onStart: () -> Unit,
    onBack: () -> Unit
) {
    ListScreen(title = "Tus secciones") {
        item { HintText("Toca una para editarla") }
        sections.forEachIndexed { index, section ->
            item {
                val count = section.keywords.size
                InfoRow(
                    title = section.title,
                    subtitle = if (count == 1) "1 palabra clave" else "$count palabras clave",
                    accent = if (count > 0) ColorPrimary else ColorMuted,
                    onClick = { onEdit(index) }
                )
            }
        }
        item { PillButton("Comenzar exposición", onStart) }
        item { PillButton("Atrás", onBack, filled = false) }
    }
}

@Composable
fun SectionEditScreen(
    index: Int,
    section: Section,
    onRename: () -> Unit,
    onAddKeyword: () -> Unit,
    onEditKeyword: (Int) -> Unit,
    onDone: () -> Unit
) {
    ListScreen(title = "Sección ${index + 1}") {
        item {
            InfoRow(
                title = section.title,
                subtitle = "Toca para cambiar el nombre",
                onClick = onRename
            )
        }
        item { HintText("Palabras clave") }
        section.keywords.forEachIndexed { keywordIndex, word ->
            item {
                InfoRow(
                    title = word,
                    onClick = { onEditKeyword(keywordIndex) }
                )
            }
        }
        item { PillButton("+ Palabra clave", onAddKeyword, filled = false) }
        item { PillButton("Listo", onDone) }
    }
}

// Pantalla para escribir un texto (nombre de sección, palabra clave, minutos).
// Usa el teclado oficial de Wear OS, que devuelve el texto ya confirmado.
@Composable
fun TextInputScreen(
    label: String,
    initial: String,
    numeric: Boolean,
    onSave: (String) -> Unit,
    onDelete: (() -> Unit)?,
    onCancel: () -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    var keyboardError by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(20.dp)
    val save = { if (text.isNotBlank()) onSave(text.trim()) }

    // Aquí llega el texto cuando termina de escribir en el teclado
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (data != null) {
            val results = RemoteInput.getResultsFromIntent(data)
            val typed = results?.getCharSequence("texto")?.toString()
            if (typed != null) {
                text = if (numeric) typed.filter { it.isDigit() } else typed.trim()
            }
        }
    }

    // Abre el teclado del reloj
    fun openKeyboard() {
        try {
            val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
            val remoteInput = RemoteInput.Builder("texto").setLabel(label).build()
            RemoteInputIntentHelper.putRemoteInputsExtra(intent, listOf(remoteInput))
            launcher.launch(intent)
        } catch (e: Exception) {
            keyboardError = true
        }
    }

    // Si es algo nuevo (campo vacío), abre el teclado automáticamente
    LaunchedEffect(Unit) {
        if (initial.isEmpty()) openKeyboard()
    }

    ListScreen(title = label) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(ColorSurface)
                    .border(1.dp, ColorBorder, shape)
                    .clickable { openKeyboard() }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Text(
                    text = if (text.isEmpty()) "Toca para escribir" else text,
                    fontSize = 16.sp,
                    color = if (text.isEmpty()) ColorMuted else ColorInk
                )
            }
        }
        if (keyboardError) {
            item { HintText("No se pudo abrir el teclado") }
        }
        item { PillButton("Guardar", onClick = { save() }) }
        if (onDelete != null) {
            item { PillButton("Eliminar", onDelete, filled = false) }
        }
        item { PillButton("Cancelar", onCancel, filled = false) }
    }
}

// =====================================================================
//  PANTALLA DURANTE LA EXPOSICIÓN
// =====================================================================

@Composable
fun PresentationScreen(
    totalSeconds: Int,
    reminderMinutes: List<Int>,
    vibrationsOn: Boolean,
    steps: List<Step>,
    onFinished: () -> Unit,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current

    var remainingMs by remember { mutableLongStateOf(totalSeconds * 1000L) }
    var stepIndex by remember { mutableIntStateOf(0) }

    // Mantiene la pantalla encendida mientras exponemos
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    // CRONÓMETRO + VIBRACIONES
    // Se calcula con la hora real del sistema, así no se atrasa.
    LaunchedEffect(Unit) {
        val endTime = SystemClock.elapsedRealtime() + totalSeconds * 1000L
        val alreadyVibrated = mutableSetOf<Int>() // recordatorios ya ejecutados

        while (true) {
            val ms = (endTime - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
            remainingMs = ms
            val secondsLeft = ((ms + 999) / 1000).toInt()

            // ¿Ya faltan X minutos o menos? Vibra una sola vez por recordatorio.
            for (minutes in reminderMinutes) {
                if (secondsLeft <= minutes * 60 && minutes !in alreadyVibrated) {
                    alreadyVibrated.add(minutes)
                    vibrate(context)
                }
            }

            if (ms == 0L) {
                if (vibrationsOn) vibrate(context)
                onFinished()
                return@LaunchedEffect
            }
            delay(200)
        }
    }

    val secondsLeft = ((remainingMs + 999) / 1000).toInt()
    val timeText = "%02d:%02d".format(secondsLeft / 60, secondsLeft % 60)
    val progress = remainingMs / (totalSeconds * 1000f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            // GESTOS: derecha = siguiente palabra, izquierda = anterior
            .pointerInput(steps) {
                var dragTotal = 0f
                detectHorizontalDragGestures(
                    onDragStart = { dragTotal = 0f },
                    onDragEnd = {
                        if (dragTotal > 50f && stepIndex < steps.size - 1) {
                            stepIndex++
                        } else if (dragTotal < -50f && stepIndex > 0) {
                            stepIndex--
                        }
                    },
                    onHorizontalDrag = { _, dragAmount -> dragTotal += dragAmount }
                )
            }
    ) {
        // Anillo de progreso alrededor de la pantalla
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 6.dp.toPx()
            val inset = strokeWidth / 2 + 2.dp.toPx()
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            drawArc(
                color = ColorPrimary.copy(alpha = 0.14f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokeWidth)
            )
            drawArc(
                color = ColorPrimary,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. TIEMPO RESTANTE (arriba), dentro de una píldora suave
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .background(ColorTint)
                    .padding(horizontal = 14.dp, vertical = 2.dp)
            ) {
                Text(
                    text = timeText,
                    fontSize = if (secondsLeft >= 6000) 26.sp else 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (secondsLeft <= 60) ColorPrimary else ColorInk
                )
            }

            // 2 y 3. SECCIÓN Y PALABRA CLAVE (centro)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (steps.isEmpty()) {
                    Text(text = "Sin notas", fontSize = 13.sp, color = ColorMuted)
                } else {
                    AnimatedContent(
                        targetState = stepIndex,
                        contentAlignment = Alignment.Center,
                        transitionSpec = {
                            val forward = targetState > initialState
                            (slideInHorizontally(initialOffsetX = { if (forward) -it / 2 else it / 2 }) + fadeIn()) togetherWith
                                    (slideOutHorizontally(targetOffsetX = { if (forward) it / 2 else -it / 2 }) + fadeOut())
                        },
                        label = "keyword"
                    ) { index ->
                        StepContent(steps[index])
                    }
                }
            }

            // Puntos de progreso dentro de la sección
            if (steps.isNotEmpty()) {
                ProgressDots(
                    current = steps[stepIndex].keywordNumber - 1,
                    total = steps[stepIndex].keywordCount
                )
            }
            Spacer(modifier = Modifier.height(6.dp))

            // Salir: mantener pulsado (para no salir por accidente)
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(ColorSurface)
                    .border(1.dp, ColorBorder, CircleShape)
                    .pointerInput(Unit) {
                        detectTapGestures(onLongPress = { onExit() })
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "✕", fontSize = 11.sp, color = ColorMuted)
            }
        }
    }
}

// Lo que se ve en el centro: sección + palabra clave actual
@Composable
fun StepContent(step: Step) {
    val keywordSize = when {
        step.keyword.length <= 10 -> 26.sp
        step.keyword.length <= 22 -> 20.sp
        else -> 16.sp
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Etiqueta "SECCIÓN 2 DE 3" dentro de una píldora
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(ColorTint)
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = "SECCIÓN ${step.sectionNumber} DE ${step.sectionCount}",
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = ColorPrimary
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = step.sectionTitle,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = ColorPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = step.keyword,
            fontSize = keywordSize,
            fontWeight = FontWeight.Bold,
            color = ColorInk,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// =====================================================================
//  PANTALLA FINAL
// =====================================================================

@Composable
fun FinishedScreen(onHome: () -> Unit) {
    ListScreen {
        item {
            // Círculo con un halo suave alrededor
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(ColorTint),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(ColorPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "✓", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
        item {
            Text(
                text = "Exposición finalizada",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ColorInk,
                textAlign = TextAlign.Center
            )
        }
        item {
            Text(
                text = "¡Buen trabajo! 🎉",
                fontSize = 14.sp,
                color = ColorPrimary,
                textAlign = TextAlign.Center
            )
        }
        item { Spacer(modifier = Modifier.height(4.dp)) }
        item { PillButton("Inicio", onHome) }
    }
}