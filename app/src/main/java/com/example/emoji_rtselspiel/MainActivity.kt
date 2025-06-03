package com.example.emoji_rtselspiel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emoji_rtselspiel.ui.theme.EmojiRätselspielTheme
import kotlinx.coroutines.launch
import androidx.compose.animation.core.Animatable // Make sure this is imported
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue // For property delegation if used elsewhere
import androidx.compose.runtime.mutableStateOf // You'll still need this for other states
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue // For property delegation if used elsewhere
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.ui.unit.IntOffset


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EmojiRätselspielTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SpielApp()
                }
                }
            }
        }
    }

@Composable
fun Spiel(modifier: Modifier = Modifier){
    var text = remember { mutableStateOf("") }
    var showCorrectDialog = remember { mutableStateOf(false) }
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
    var antwort = stringResource(antwortResource)

    var jiggleTrigger by remember { mutableStateOf(false) }
    val offsetX = remember { Animatable(0f) }

    LaunchedEffect(jiggleTrigger) { // Key is the integer count
        if (jiggleTrigger) { // Only run if triggered (not on initial composition or if reset to 0)
            launch {
                offsetX.animateTo(targetValue = -10f, animationSpec = tween(durationMillis = 50))
                offsetX.animateTo(targetValue = 10f, animationSpec = tween(durationMillis = 50))
                offsetX.animateTo(targetValue = -10f, animationSpec = tween(durationMillis = 50))
                offsetX.animateTo(targetValue = 10f, animationSpec = tween(durationMillis = 50))
                offsetX.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 50))
                jiggleTrigger = false
            }

        }
    }
    Column(modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Errate das Sprichwort!",
            fontSize = 50.sp,
            lineHeight = 50.sp,
            textAlign = TextAlign.Center)
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(textResource),
                modifier = Modifier
                    .offset{
                        IntOffset(offsetX.value.roundToInt(), 0)
                           },
                fontSize = 40.sp
            )
        }
        TextField(
            value = text.value, // The current text to display
            onValueChange = { newText -> // Callback when the text changes
                text.value = newText
            },
            label = { Text("Deine Antwort") }, // Optional label for the TextField
            modifier = Modifier.padding(top = 16.dp) // Add some space above the TextField
        )
       Button(onClick = {
           if (checkAnswer(text.value, antwort)) {
               showCorrectDialog.value = true // Show the dialog
           } else {
               text.value = "" // Clear input on wrong answer
               jiggleTrigger = true
           }
       }
       ) {
           Text("Submit")
       }
        if (showCorrectDialog.value) {
            AlertDialog(
                onDismissRequest = {
                    // This is called when the user clicks outside the dialog or presses the back button.
                    showCorrectDialog.value = false
                    // emojis = (1..2).random() // Or go to next: (emojis % MAX_RIDDLES) + 1
                    text.value = "" // Clear the text field for the next riddle
                },
                title = {
                    Text(text = "Richtig!")
                },
                text = {
                    Text(text = "Das war die korrekte Antwort.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showCorrectDialog.value = false
                            // Logic to go to the next riddle:
                            // Example: emojis = if (emojis < 2) emojis + 1 else 1 // Assuming 2 riddles for now
                            emojis.value = (1..10).random() // Or simply pick another random one as per your original logic
                            text.value = "" // Clear the text field for the next riddle
                        }
                    ) {
                        Text("Nächstes Rätsel")
                    }
                }
            )
        }
    }
}



fun checkAnswer(answer: String, correctAnswer: String): Boolean {
    return answer == correctAnswer
}

@Preview(showBackground = true,
        showSystemUi = true)
@Composable
fun SpielApp(){
    Spiel(modifier = Modifier
        .fillMaxSize()
        .wrapContentSize(Alignment.Center))
}