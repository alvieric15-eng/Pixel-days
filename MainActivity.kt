package com.pixeldays.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.pixeldays.app.data.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private val Night = Color(0xFF171C2B)
private val Panel = Color(0xFF252F45)
private val Cream = Color(0xFFF5E8C7)
private val Gold = Color(0xFFF4D58D)
private val Pink = Color(0xFFE99BB0)
private val Mint = Color(0xFF9BD6B3)
private val Muted = Color(0xFFB8C2D9)

class MainActivity : ComponentActivity() {
    private val requestNotifications = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= 33) requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        val repo = PlannerRepository(this)
        setContent {
            PixelDaysTheme {
                PixelDaysScreen(
                    repo = repo,
                    onReminderInfo = { showReminderInfo() },
                    onWidgetRefresh = { }
                )
            }
        }
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel("pixel_reminders", "Pixel Days reminders", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }
    private fun showReminderInfo() {
        // Reminder scheduling UI is prepared for the next iteration; system notification permission is requested.
    }
}

@Composable
private fun PixelDaysTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(
        background = Night, surface = Panel, primary = Gold, secondary = Pink,
        onBackground = Cream, onSurface = Cream, onPrimary = Night
    ), content = content)
}

@Composable
private fun PixelDaysScreen(repo: PlannerRepository, onReminderInfo: () -> Unit, onWidgetRefresh: () -> Unit) {
    val scope = rememberCoroutineScope()
    val tasks by repo.tasks.collectAsState(initial = emptyList())
    val note by repo.latestNote.collectAsState(initial = null)
    var newTask by remember { mutableStateOf("") }
    var assignee by remember { mutableStateOf("Both") }
    var noteText by remember(note?.body) { mutableStateOf(note?.body.orEmpty()) }
    var roomCode by remember { mutableStateOf("") }
    var syncMessage by remember { mutableStateOf("Local mode · notes stay on this phone") }
    val date = remember { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date()) }

    Surface(Modifier.fillMaxSize(), color = Night) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
            contentPadding = PaddingValues(top = 28.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("PIXEL DAYS", color = Gold, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                        Text("Your little world.", color = Cream, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
                        Text(date.replaceFirstChar { it.uppercase() }, color = Muted, fontSize = 13.sp)
                    }
                    Icon(Icons.Default.Favorite, contentDescription = "Together", tint = Pink, modifier = Modifier.size(30.dp))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("OPEN TASKS", tasks.count { !it.isDone }.toString(), Modifier.weight(1f))
                    StatCard("DONE", tasks.count { it.isDone }.toString(), Modifier.weight(1f))
                }
            }
            item {
                SectionTitle("TODAY'S LITTLE MISSIONS", "✦")
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InputField(newTask, { newTask = it }, "Add something to do…", Modifier.weight(1f))
                    IconButton(onClick = {
                        if (newTask.isNotBlank()) {
                            scope.launch {
                                repo.addTask(Task(UUID.randomUUID().toString(), newTask.trim(), null, assignee))
                                newTask = ""
                            }
                        }
                    }) { Icon(Icons.Default.Add, contentDescription = "Add task", tint = Gold) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Me", "My wife", "Both").forEach { person ->
                        FilterChip(selected = assignee == person, onClick = { assignee = person },
                            label = { Text(person) }, colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Gold, selectedLabelColor = Night
                            ))
                    }
                }
            }
            if (tasks.isEmpty()) {
                item { PanelCard { Text("No missions yet. Add your first little task above ✨", color = Muted) } }
            } else {
                items(tasks, key = { it.id }) { task ->
                    PanelCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (task.isDone) Icons.Default.CheckCircle else Icons.Default.Circle,
                                contentDescription = if (task.isDone) "Completed" else "Open",
                                tint = if (task.isDone) Mint else Gold,
                                modifier = Modifier.clickable { scope.launch { repo.toggleTask(task) } }.size(23.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(task.title, color = if (task.isDone) Muted else Cream,
                                    fontWeight = FontWeight.SemiBold)
                                Text(task.assignee, color = Pink, fontSize = 11.sp)
                            }
                            IconButton(onClick = { scope.launch { repo.deleteTask(task) } }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Muted)
                            }
                        }
                    }
                }
            }
            item {
                SectionTitle("QUICK NOTE", "▧")
                PanelCard {
                    BasicTextField(
                        value = noteText, onValueChange = { noteText = it },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 82.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Cream, fontSize = 14.sp),
                        decorationBox = { inner ->
                            Box {
                                if (noteText.isEmpty()) Text("Shopping list, ideas, little reminders…", color = Muted)
                                inner()
                            }
                        }
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = {
                        scope.launch { repo.saveNote(Note("shared-note", noteText)) }
                    }, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Night)) {
                        Icon(Icons.Default.EditNote, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Save note")
                    }
                }
            }
            item {
                SectionTitle("OUR SHARED SPACE", "♡")
                PanelCard {
                    Text("Connect your two phones", color = Cream, fontWeight = FontWeight.Bold)
                    Text("Use the same room code on both phones. Cloud sync needs Firebase setup first.", color = Muted, fontSize = 12.sp)
                    InputField(roomCode, { roomCode = it.uppercase().filter(Char::isLetterOrDigit).take(12) }, "Create a room code (6–12 chars)")
                    Button(onClick = { repo.syncToRoom(roomCode) { syncMessage = it } },
                        colors = ButtonDefaults.buttonColors(containerColor = Panel, contentColor = Gold)) {
                        Text("Check connection")
                    }
                    Text(syncMessage, color = Muted, fontSize = 11.sp)
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Gold)
                    Spacer(Modifier.width(8.dp))
                    Text("Reminders & widget", color = Cream, fontWeight = FontWeight.Bold)
                }
                Text("Add the Pixel Days widget from your Android home-screen widget picker. Reminder scheduling is a planned next step.", color = Muted, fontSize = 12.sp)
            }
            item { Text("MADE FOR TWO  ♥", color = Gold, fontSize = 10.sp, letterSpacing = 3.sp, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
        }
    }
}

@Composable private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    PanelCard(modifier) {
        Text(label, color = Muted, fontSize = 10.sp, letterSpacing = 1.5.sp)
        Text(value, color = Cream, fontSize = 27.sp, fontWeight = FontWeight.Black)
    }
}
@Composable private fun SectionTitle(title: String, glyph: String) {
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, color = Cream, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, letterSpacing = 1.2.sp)
        Text(glyph, color = Gold)
    }
}
@Composable private fun PanelCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().background(Panel, RoundedCornerShape(16.dp)).padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
}
@Composable private fun InputField(value: String, onValueChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier) {
    BasicTextField(value, onValueChange, modifier = modifier
        .background(Color(0xFF303B53), RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 13.dp),
        textStyle = androidx.compose.ui.text.TextStyle(color = Cream, fontSize = 14.sp),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text(hint, color = Muted, fontSize = 13.sp)
                inner()
            }
        })
}
