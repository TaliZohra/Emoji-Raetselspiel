package com.example.emoji_rtselspiel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emoji_rtselspiel.ui.theme.EmojiRätselspielTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EmojiRätselspielTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SpielApp()
                }
            }
        }
    }
}

@Composable
fun Spiel(modifier: Modifier = Modifier) {
    var text = remember { mutableStateOf("") }
    var correctCount = remember { mutableStateOf(0) }
    var solvedRiddles = remember { mutableStateListOf<Int>() }
    var showGratulationsDialog = remember { mutableStateOf(false) }

    var emojis = remember { mutableStateOf(1) }
    val textResource = when (emojis.value) {
        1 -> R.string.Raetsel_1
        2 -> R.string.Raetsel_2
        3 -> R.string.Raetsel_3
        4 -> R.string.Raetsel_4
        5 -> R.string.Raetsel_5
        6 -> R.string.Raetsel_6
        7 -> R.string.Raetsel_7
        8 -> R.string.Raetsel_8
        9 -> R.string.Raetsel_9
        10 -> R.string.Raetsel_10
        else -> R.string.Raetsel_1
    }

    val antwortResource = when (emojis.value) {
        1 -> R.string.Antwort_1
        2 -> R.string.Antwort_2
        3 -> R.string.Antwort_3
        4 -> R.string.Antwort_4
        5 -> R.string.Antwort_5
        6 -> R.string.Antwort_6
        7 -> R.string.Antwort_7
        8 -> R.string.Antwort_8
        9 -> R.string.Antwort_9
        10 -> R.string.Antwort_10
        else -> R.string.Antwort_1
    }

    val antwort = stringResource(antwortResource)

    val scope = rememberCoroutineScope()
    var jiggleTrigger by remember { mutableStateOf(false) }
    val offsetX = remember { Animatable(0f) }

    LaunchedEffect(jiggleTrigger) {
        if (jiggleTrigger) {
            scope.launch {
                offsetX.animateTo(-10f, tween(50))
                offsetX.animateTo(10f, tween(50))
                offsetX.animateTo(-10f, tween(50))
                offsetX.animateTo(10f, tween(50))
                offsetX.animateTo(0f, tween(50))
                jiggleTrigger = false
            }
        }
    }

    var showCorrectFlash by remember { mutableStateOf(false) }
    if (showCorrectFlash) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(250)
            showCorrectFlash = false
        }
    }

    Box(
        modifier = modifier
            .background(if (showCorrectFlash) Color(0xFFA5D6A7) else MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopEnd

    ) {
        Text("Richtige Antworten: ${correctCount.value}",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            )

        Column(
            modifier = Modifier
                .padding(top = 50.dp, start = 24.dp, end = 24.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                "Errate das Wort!",
                fontSize = 40.sp,
                lineHeight = 44.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(textResource),
                modifier = Modifier
                    .offset { IntOffset(offsetX.value.roundToInt(), 0) },
                fontSize = 32.sp
            )

            TextField(
                value = text.value,
                onValueChange = { newText -> text.value = newText },
                label = { Text("Deine Antwort") },
                modifier = Modifier.fillMaxWidth(0.85f)
            )

            Button(onClick = {
                if (checkAnswer(text.value, antwort)) {
                    if (!solvedRiddles.contains(emojis.value)) {
                        solvedRiddles.add(emojis.value)
                        correctCount.value++
                    }
                    if (solvedRiddles.size >= 10) {
                        showGratulationsDialog.value = true
                    } else {
                        do {
                            emojis.value = (1..10).random()
                        } while (solvedRiddles.contains(emojis.value))
                        text.value = ""
                        showCorrectFlash = true
                    }
                } else {
                    text.value = ""
                    jiggleTrigger = true
                }
            }) {
                Text("Submit")
            }
        }
    }

    if (showGratulationsDialog.value) {
        AlertDialog(
            onDismissRequest = {
                showGratulationsDialog.value = false
            },
            title = { Text("Gratulation!") },
            text = { Text("Du hast alle Rätsel gelöst!") },
            confirmButton = {
                TextButton(onClick = {
                    showGratulationsDialog.value = false
                    solvedRiddles.clear()
                    correctCount.value = 0
                    emojis.value = (1..10).random()
                    text.value = ""
                }) {
                    Text("Neu starten")
                }
            }
        )
    }
}

fun checkAnswer(answer: String, correctAnswer: String): Boolean {
    return answer == correctAnswer
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SpielApp() {
    Spiel(modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(align = Alignment.Center))
}
