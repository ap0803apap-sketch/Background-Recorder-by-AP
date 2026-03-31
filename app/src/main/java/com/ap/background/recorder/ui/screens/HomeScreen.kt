package com.ap.background.recorder.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ap.background.recorder.data.RecorderPreferences
import com.ap.background.recorder.services.RecordingService
import com.ap.background.recorder.utils.FileManager
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    prefs: RecorderPreferences,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fileManager = remember { FileManager(context) }
    
    val recordingMode by prefs.recordingModeFlow.collectAsStateWithLifecycle(initialValue = "VIDEO")
    val cameraSelection by prefs.cameraSelectionFlow.collectAsStateWithLifecycle(initialValue = "PRIMARY")
    val zoomLevel by prefs.zoomLevelFlow.collectAsStateWithLifecycle(initialValue = 1f)
    val focusMode by prefs.focusModeFlow.collectAsStateWithLifecycle(initialValue = "AUTO")
    val photoInterval by prefs.photoIntervalFlow.collectAsStateWithLifecycle(initialValue = 5)
    
    var recordings by remember { mutableStateOf(listOf<File>()) }
    var fileToDelete by remember { mutableStateOf<File?>(null) }
    var showHelpDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        recordings = fileManager.getAllRecordings()
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("How to Use") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    HelpItem("1. Start/Stop", "Use the floating button or the Stop button on the home screen.")
                    HelpItem("2. Background Trigger", "Go to Device Settings > Default Apps > Digital Assistant and select this app. Now long-pressing the power/home button will toggle recording silently.")
                    HelpItem("3. Quick Settings", "Add the 'Background Recorder' tile to your notification panel for one-tap recording.")
                    HelpItem("4. Silent Mode", "Assistant and Tile methods start recording without opening the app UI.")
                    HelpItem("5. Multiple Cameras", "Cycle between Primary, Secondary (Ultrawide), and Front cameras using the 'Cam' chip.")
                    HelpItem("6. Export", "Recordings are private. Use 'Save to Downloads' in the file menu to make them visible in your Gallery.")
                }
            },
            confirmButton = { TextButton(onClick = { showHelpDialog = false }) { Text("Got it") } }
        )
    }

    if (fileToDelete != null) {
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text("Confirm Delete") },
            text = { Text("Are you sure you want to delete ${fileToDelete?.name}?") },
            confirmButton = {
                TextButton(onClick = {
                    fileToDelete?.delete()
                    recordings = fileManager.getAllRecordings()
                    fileToDelete = null
                }) { Text("Delete", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Background Recorder") },
                actions = {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Help")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val action = when (recordingMode) {
                        "VIDEO" -> RecordingService.ACTION_START_VIDEO
                        "PHOTO" -> RecordingService.ACTION_START_PHOTO
                        "AUDIO" -> RecordingService.ACTION_START_AUDIO
                        else -> RecordingService.ACTION_START_VIDEO
                    }
                    val intent = Intent(context, RecordingService::class.java).apply {
                        this.action = action
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.RadioButtonChecked, contentDescription = "Start", tint = Color.White)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Control Panel
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Quick Controls", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        ControlChip("Mode: $recordingMode", Icons.Default.Videocam) {
                             val next = when(recordingMode) {
                                 "VIDEO" -> "PHOTO"
                                 "PHOTO" -> "AUDIO"
                                 else -> "VIDEO"
                             }
                             scope.launch { prefs.setRecordingMode(next) }
                        }
                        
                        ControlChip("Cam: $cameraSelection", Icons.Default.Camera) {
                            val next = when(cameraSelection) {
                                "PRIMARY" -> "SECONDARY"
                                "SECONDARY" -> "FRONT"
                                else -> "PRIMARY"
                            }
                            scope.launch { prefs.setCameraSelection(next) }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        ControlChip("Zoom: ${String.format("%.1f", zoomLevel)}x", Icons.Default.ZoomIn) {
                            val next = if (zoomLevel >= 10f) 1f else zoomLevel + 1.0f
                            scope.launch { prefs.setZoomLevel(next) }
                        }
                        ControlChip("Focus: $focusMode", Icons.Default.FilterCenterFocus) {
                            val next = if (focusMode == "AUTO") "MANUAL" else "AUTO"
                            scope.launch { prefs.setFocusMode(next) }
                        }
                    }

                    if (recordingMode == "PHOTO") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Photo Interval: ${photoInterval}s", 
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                        Slider(
                            value = photoInterval.toFloat(),
                            onValueChange = { scope.launch { prefs.setPhotoInterval(it.toInt()) } },
                            valueRange = 1f..60f,
                            steps = 59,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                    
                    Button(
                        onClick = {
                            val intent = Intent(context, RecordingService::class.java).apply {
                                action = RecordingService.ACTION_STOP
                            }
                            context.startService(intent)
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Stop Recording")
                    }
                }
            }

            Text(
                "Recent Recordings",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (recordings.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No recordings found", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(recordings) { file ->
                        RecordingItem(
                            file = file, 
                            onDelete = { fileToDelete = it },
                            onSave = {
                                val feature = when {
                                    file.name.contains("VID") -> "Video"
                                    file.name.contains("AUD") -> "Audio"
                                    else -> "Photo"
                                }
                                val uri = fileManager.saveToDownloads(file, feature)
                                scope.launch {
                                    if (uri != null) {
                                        snackbarHostState.showSnackbar("Saved to Downloads/Background Recorder")
                                    } else {
                                        snackbarHostState.showSnackbar("Failed to save file")
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HelpItem(title: String, description: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
        Text(description, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun ControlChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    FilterChip(
        selected = true,
        onClick = onClick,
        label = { Text(label, fontSize = 12.sp) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) }
    )
}

@Composable
fun RecordingItem(file: File, onDelete: (File) -> Unit, onSave: () -> Unit) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }
    var showMenu by remember { mutableStateOf(false) }
    
    ListItem(
        headlineContent = { Text(file.name) },
        supportingContent = { 
            val size = if (file.length() >= 1024 * 1024) String.format("%.1f MB", file.length() / (1024.0 * 1024.0)) else String.format("%.1f KB", file.length() / 1024.0)
            Text("$size • ${dateFormat.format(java.util.Date(file.lastModified()))}") 
        },
        leadingContent = {
            val icon = when {
                file.name.contains("VID") -> Icons.Default.Movie
                file.name.contains("AUD") -> Icons.Default.Mic
                else -> Icons.Default.Image
            }
            Icon(icon, contentDescription = null)
        },
        trailingContent = {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Play/Open") },
                        onClick = {
                            showMenu = false
                            openFile(context, file)
                        },
                        leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Save to Downloads") },
                        onClick = {
                            showMenu = false
                            onSave()
                        },
                        leadingIcon = { Icon(Icons.Default.Save, contentDescription = null) }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Delete", color = Color.Red) },
                        onClick = {
                            showMenu = false
                            onDelete(file)
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) }
                    )
                }
            }
        },
        modifier = Modifier.clickable { openFile(context, file) }
    )
}

private fun openFile(context: android.content.Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, getMimeType(file))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        // Handle error
    }
}

private fun getMimeType(file: File): String {
    return when (file.extension.lowercase()) {
        "mp4" -> "video/mp4"
        "m4a" -> "audio/mp4"
        "jpg", "jpeg" -> "image/jpeg"
        else -> "*/*"
    }
}