package com.ap.background.recorder.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ap.background.recorder.data.RecorderPreferences
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    prefs: RecorderPreferences,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val themeMode by prefs.themeModeFlow.collectAsStateWithLifecycle(initialValue = "SYSTEM")
    val isDynamicColor by prefs.dynamicColorFlow.collectAsStateWithLifecycle(initialValue = true)
    val isAmoledMode by prefs.amoledModeFlow.collectAsStateWithLifecycle(initialValue = false)
    val isBiometricEnabled by prefs.biometricEnabledFlow.collectAsStateWithLifecycle(initialValue = false)

    // Back Camera Settings
    var videoResolution by remember { mutableStateOf("1080p") }
    var videoFps by remember { mutableIntStateOf(30) }
    var photoMegapixel by remember { mutableIntStateOf(48) }

    // Front Camera Settings
    var frontVideoResolution by remember { mutableStateOf("1080p") }
    var frontVideoFps by remember { mutableIntStateOf(30) }
    var frontPhotoQuality by remember { mutableIntStateOf(32) }

    LaunchedEffect(Unit) {
        videoResolution = prefs.getVideoResolution()
        videoFps = prefs.getVideoFps()
        photoMegapixel = prefs.getPhotoMegapixel()
        
        frontVideoResolution = prefs.getFrontVideoResolution()
        frontVideoFps = prefs.getFrontVideoFps()
        frontPhotoQuality = prefs.getFrontPhotoQuality()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Theme Settings
            Text("App Theme", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            
            ThemeRadioGroup(
                selectedOption = themeMode,
                onOptionSelected = { scope.launch { prefs.setThemeMode(it) } }
            )

            SettingSwitch(
                label = "Dynamic Color (Material You)",
                checked = isDynamicColor,
                onCheckedChange = { scope.launch { prefs.setDynamicColor(it) } }
            )

            SettingSwitch(
                label = "AMOLED Dark Mode",
                checked = isAmoledMode,
                onCheckedChange = { scope.launch { prefs.setAmoledMode(it) } },
                enabled = themeMode == "DARK" || (themeMode == "SYSTEM" && isSystemInDark())
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // Back Camera Settings
            Text("Back Camera Settings", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            
            SettingOption("Video Resolution", videoResolution) {
                val next = if (videoResolution == "1080p") "4K" else "1080p"
                videoResolution = next
                scope.launch { prefs.setVideoResolution(next) }
            }
            SettingOption("Video FPS", "$videoFps FPS") {
                val next = if (videoFps == 30) 60 else 30
                videoFps = next
                scope.launch { prefs.setVideoFps(next) }
            }
            SettingOption("Photo Quality", "$photoMegapixel MP") {
                val next = if (photoMegapixel == 48) 12 else 48
                photoMegapixel = next
                scope.launch { prefs.setPhotoMegapixel(next) }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // Front Camera Settings
            Text("Front Camera Settings", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            
            SettingOption("Front Video Resolution", frontVideoResolution) {
                val next = if (frontVideoResolution == "1080p") "720p" else "1080p"
                frontVideoResolution = next
                scope.launch { prefs.setFrontVideoResolution(next) }
            }
            SettingOption("Front Video FPS", "$frontVideoFps FPS") {
                val next = if (frontVideoFps == 30) 24 else 30
                frontVideoFps = next
                scope.launch { prefs.setFrontVideoFps(next) }
            }
            SettingOption("Front Photo Quality", "$frontPhotoQuality MP") {
                val next = if (frontPhotoQuality == 32) 8 else 32
                frontPhotoQuality = next
                scope.launch { prefs.setFrontPhotoQuality(next) }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // Security Settings
            Text("Security", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            SettingSwitch(
                label = "Biometric Lock",
                checked = isBiometricEnabled,
                onCheckedChange = { scope.launch { prefs.setBiometricEnabled(it) } }
            )

            Spacer(modifier = Modifier.height(32.dp))
            
            // About Section
            Text("About", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text("Developer: AP", modifier = Modifier.padding(vertical = 4.dp))
            
            // Developer Email with mailto intent
            TextButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:ap0803apap@gmail.com")
                        putExtra(Intent.EXTRA_SUBJECT, "Background Recorder Feedback")
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Handle case where no email app is found
                    }
                },
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Email: ap0803apap@gmail.com", textDecoration = TextDecoration.Underline)
            }
            
            TextButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ap0803apap-sketch"))
                    context.startActivity(intent)
                },
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("GitHub: View Source Code", textDecoration = TextDecoration.Underline)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text("Version 1.0.0", style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
fun ThemeRadioGroup(selectedOption: String, onOptionSelected: (String) -> Unit) {
    val options = listOf("SYSTEM", "LIGHT", "DARK")
    Column(Modifier.selectableGroup()) {
        options.forEach { text ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .selectable(
                        selected = (text == selectedOption),
                        onClick = { onOptionSelected(text) },
                        role = Role.RadioButton
                    )
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = (text == selectedOption), onClick = null)
                Text(
                    text = text.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Text(label, modifier = Modifier.weight(1f), color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun SettingOption(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun isSystemInDark(): Boolean = androidx.compose.foundation.isSystemInDarkTheme()
