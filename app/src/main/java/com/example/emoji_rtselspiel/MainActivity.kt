package com.example.emoji_rtselspiel

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.emoji_rtselspiel.ui.theme.EmojiRätselspielTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt


class MainActivity : ComponentActivity() {

    // Launcher für die Berechtigungsanfrage
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.d("Permission", "Notification permission granted")
                // Hier könntest du die Überwachung starten, falls sie von der Berechtigung abhängt
            } else {
                Log.d("Permission", "Notification permission denied")
                // Erkläre dem Nutzer, warum die Berechtigung nützlich ist
            }
        }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // TIRAMISU ist API 33
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                Log.d("Permission", "Notification permission already granted")
                // Berechtigung bereits erteilt
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // Zeige eine Erklärung, warum du die Berechtigung brauchst
                // z.B. in einem Dialog, bevor du sie erneut anfragst.
                // Für dieses Beispiel fragen wir direkt an.
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                // Direkte Anfrage
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        askNotificationPermission()
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

    val context = LocalContext.current
    val articleTitle = "Rick Astley"  // Der Artikel, den du überwachen möchtest
    val lastKnownRevId = remember { mutableStateOf<Long?>(null) } // Speichert die letzte bekannte Revisions-ID
    val checkIntervalMillis = 90000L // Intervall für die Überprüfung (z.B. 90 Sekunden)

    LaunchedEffect(Unit) { // Einmal starten, wenn das Composable in die Komposition eintritt
        Log.d("WikiCheck", "Starting Wikipedia check coroutine for article: $articleTitle")
        while (true) {
            // Stelle sicher, dass du die Internet-Berechtigung hast (sollte durch Manifest abgedeckt sein)
            try {
                val newRevId: Long? = withContext(Dispatchers.IO) { // Netzwerkaufruf im IO-Dispatcher
                    // API-URL für die letzte Revision. `rvprop=ids|timestamp` um ID und Zeitstempel zu bekommen.
                    val urlString = "https://de.wikipedia.org/w/api.php?action=query&prop=revisions&titles=${articleTitle.replace(" ", "_")}&rvprop=ids|timestamp&formatversion=2&format=json"
                    val url = URL(urlString)
                    var connection: HttpURLConnection? = null
                    try {
                        connection = url.openConnection() as HttpURLConnection
                        connection.requestMethod = "GET"
                        connection.connectTimeout = 15000 // 15 Sekunden
                        connection.readTimeout = 15000  // 15 Sekunden

                        val responseCode = connection.responseCode
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            val inputStream = connection.inputStream
                            val response = inputStream.bufferedReader().use { it.readText() }
                            inputStream.close()

                            Log.d("WikiCheck", "API Response: $response")

                            val jsonResponse = JSONObject(response)
                            val pages = jsonResponse.getJSONObject("query").getJSONArray("pages")
                            if (pages.length() > 0) {
                                val page = pages.getJSONObject(0)
                                if (page.has("revisions")) {
                                    val revisions = page.getJSONArray("revisions")
                                    if (revisions.length() > 0) {
                                        revisions.getJSONObject(0).getLong("revid")
                                    } else {
                                        Log.w("WikiCheck", "No revisions found for $articleTitle")
                                        null
                                    }
                                } else {
                                    Log.w("WikiCheck", "'revisions' field missing for $articleTitle. Page may not exist or response format unexpected.")
                                    if (page.has("missing") && page.getBoolean("missing")) {
                                        Log.e("WikiCheck", "Article '$articleTitle' does not exist.")
                                    }
                                    null
                                }
                            } else {
                                Log.w("WikiCheck", "No pages found in API response for $articleTitle.")
                                null
                            }
                        } else {
                            Log.e("WikiCheck", "API request failed with code: $responseCode. Message: ${connection.responseMessage}")
                            null
                        }
                    } catch (e: Exception) {
                        Log.e("WikiCheck", "Error during API request or parsing: ${e.message}", e)
                        null
                    } finally {
                        connection?.disconnect()
                    }
                }

                if (newRevId != null) {
                    Log.d("WikiCheck", "Current revId for $articleTitle: $newRevId, Last known: ${lastKnownRevId.value}")
                    if (lastKnownRevId.value != null && newRevId != lastKnownRevId.value) {
                        Log.i("WikiCheck", "Article '$articleTitle' has been updated! New revId: $newRevId")
                        sendNotification(
                            title = "Wikipedia Update",
                            message = "Der Artikel '$articleTitle' wurde bearbeitet.",
                            context = context
                        )
                    }
                    lastKnownRevId.value = newRevId
                } else {
                    Log.w("WikiCheck", "Could not retrieve new revId.")
                }

            } catch (e: Exception) {
                // Genereller Fehler im Überprüfungs-Loop (sollte nicht oft passieren, wenn Fehler oben abgefangen werden)
                Log.e("WikiCheck", "Exception in Wikipedia check loop: ${e.message}", e)
            }
            delay(checkIntervalMillis) // Warte bis zur nächsten Überprüfung
        }
    }


    val subredditName = "AskReddit"//"Test1234456765432"
    val lastKnownPostIds = remember { mutableStateListOf<String>() } // Speichert IDs der schon gesehenen Posts
    var newPostsCounter by remember { mutableIntStateOf(0) } // Zählt neue Posts seit der letzten Benachrichtigung
    val postsNeededForNotification = 1
    val redditCheckIntervalMillis = 60000L // Intervall für Reddit-Überprüfung (z.B. 60 Sekunden - sei vorsichtig mit der Frequenz!)

    LaunchedEffect(Unit) { // Startet einmal, wenn das Composable in die Komposition eintritt
        Log.d("RedditCheck", "Starting Reddit check coroutine for r/$subredditName")

        // Initialisiere lastKnownPostIds mit den aktuellen Posts, um nicht sofort zu benachrichtigen
        val initialPosts = fetchNewRedditPosts(subredditName, context, lastKnownPostIds.toList())
        if (initialPosts.isNotEmpty()) {
            lastKnownPostIds.addAll(initialPosts.map { it.id })
            Log.d("RedditCheck", "Initialized with ${lastKnownPostIds.size} post IDs.")
        }


        while (true) {
            try {
                val newPostsFetched = fetchNewRedditPosts(subredditName, context, lastKnownPostIds.toList())

                if (newPostsFetched.isNotEmpty()) {
                    var brandNewPostsFoundThisCycle = 0
                    newPostsFetched.forEach { post ->
                        if (!lastKnownPostIds.contains(post.id)) {
                            lastKnownPostIds.add(post.id) // Füge neue ID zur Liste hinzu
                            newPostsCounter++
                            brandNewPostsFoundThisCycle++
                            Log.i("RedditCheck", "New post found in r/$subredditName: ${post.title} (ID: ${post.id}). New post count: $newPostsCounter")
                        }
                    }

                    if (brandNewPostsFoundThisCycle > 0) {
                        Log.d("RedditCheck", "$brandNewPostsFoundThisCycle absolutely new posts processed this cycle.")
                    }


                    if (newPostsCounter >= postsNeededForNotification) {
                        Log.i("RedditCheck", "$newPostsCounter new posts reached. Sending notification for r/$subredditName.")
                        sendNotification(
                            title = "Neue Memes!",
                            message = "$newPostsCounter neue Posts in r/$subredditName seit deiner letzten Benachrichtigung.",
                            context = context,
                            channelId = "reddit_updates" // Eigener Channel für Reddit
                        )
                        newPostsCounter = 0 // Zähler zurücksetzen
                        // Optional: lastKnownPostIds bereinigen, um die Liste nicht unendlich wachsen zu lassen
                        // z.B. nur die neuesten X IDs behalten. Für dieses Beispiel lassen wir es einfach.
                    }
                } else {
                    Log.d("RedditCheck", "No new posts fetched or all are already known for r/$subredditName.")
                }

            } catch (e: Exception) {
                Log.e("RedditCheck", "Exception in Reddit check loop: ${e.message}", e)
            }
            delay(redditCheckIntervalMillis)
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

fun sendNotification(title: String, message: String, context: Context, channelId: String = "wiki_updates") {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId,
            "Wikipedia Updates", // Channel-Name für den Nutzer sichtbar in den Einstellungen
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Benachrichtigungen über Änderungen an Wikipedia-Artikeln"
        }
        notificationManager.createNotificationChannel(channel)
    }

    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_info) // Standard-Icon, ersetze es durch dein eigenes
        .setContentTitle(title)
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true) // Benachrichtigung verschwindet nach Klick
        .build()

    // Stelle sicher, dass du die Berechtigung hast, bevor du versuchst zu benachrichtigen
    // (Obwohl die Benachrichtigung selbst nicht fehlschlägt, ist es gute Praxis)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
        notificationManager.notify(System.currentTimeMillis().toInt(), notification) // Eindeutige ID für jede Benachrichtigung
    } else {
        Log.w("Notification", "POST_NOTIFICATIONS permission not granted.")
    }
}
// Hilfsdatenklasse für Reddit-Posts
data class RedditPost(val id: String, val title: String, val createdUtc: Long)

// Hilfsfunktion zum Abrufen und Parsen von Reddit-Posts
// Diese Funktion wird in Dispatchers.IO ausgeführt, da sie Netzwerkoperationen enthält
suspend fun fetchNewRedditPosts(subreddit: String, context: Context, knownIds: List<String>): List<RedditPost> {
    return withContext(Dispatchers.IO) {
        val posts = mutableListOf<RedditPost>()
        // Wichtig: Verwende einen spezifischen User-Agent!
        val userAgent = "android:com.example.emojiraetselspiel:v1.0.0 (by /u/DEIN_REDDIT_USERNAME_FALLS_AUTHENTIFIZIERT_ODER_DEIN_PROJEKTNAME)"
        val urlString = "https://www.reddit.com/r/$subreddit/new.json?limit=25" // Limit, um nicht zu viele auf einmal zu laden
        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", userAgent)
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val inputStream = connection.inputStream
                val response = inputStream.bufferedReader().use { it.readText() }
                inputStream.close()

                // Log.d("RedditCheck", "API Response for r/$subreddit: $response") // Kann sehr lang sein

                val jsonResponse = JSONObject(response)
                val data = jsonResponse.optJSONObject("data")
                val children = data?.optJSONArray("children")

                children?.let {
                    for (i in 0 until it.length()) {
                        val postData = it.optJSONObject(i)?.optJSONObject("data")
                        postData?.let { pd ->
                            val id = pd.optString("id")
                            val title = pd.optString("title")
                            val createdUtc = pd.optLong("created_utc")
                            if (id.isNotBlank() && !knownIds.contains(id)) { // Nur hinzufügen, wenn noch nicht bekannt (effizienter im Caller)
                                posts.add(RedditPost(id, title, createdUtc))
                            } else if (id.isNotBlank() && knownIds.contains(id)) {
                                // Log.d("RedditCheck", "Post ID $id already known, skipping.")
                            }
                        }
                    }
                }
                // Sortiere nach Erstellungsdatum (neueste zuerst), falls die API es nicht schon so liefert für /new
                posts.sortByDescending { it.createdUtc }
                Log.d("RedditCheck", "Fetched ${posts.size} potential new posts from r/$subreddit.")

            } else {
                Log.e("RedditCheck", "Reddit API request for r/$subreddit failed. Code: $responseCode. Message: ${connection.responseMessage}")
            }
        } catch (e: Exception) {
            Log.e("RedditCheck", "Error during Reddit API request or parsing for r/$subreddit: ${e.message}", e)
        } finally {
            connection?.disconnect()
        }
        posts // Gib die Liste der potenziell neuen Posts zurück
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SpielApp() {
    Spiel(modifier = Modifier
        .fillMaxSize()
        .wrapContentSize(align = Alignment.Center))
}
