package com.ap.background.recorder.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TermsScreen(
    onAccept: () -> Unit
) {
    val scrollState = rememberScrollState()
    var hasScrolledToBottom by remember { mutableStateOf(false) }
    var isChecked by remember { mutableStateOf(false) }

    // Check if scrolled to bottom
    LaunchedEffect(scrollState.value, scrollState.maxValue) {
        if (scrollState.maxValue > 0 && scrollState.value >= scrollState.maxValue - 10) {
            hasScrolledToBottom = true
        }
    }

    Scaffold(
        bottomBar = {
            Surface(tonalElevation = 8.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { isChecked = it },
                            enabled = hasScrolledToBottom
                        )
                        Text(
                            text = if (hasScrolledToBottom) "I agree to the Terms & Conditions" else "Please scroll to bottom to agree",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (hasScrolledToBottom) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                        )
                    }
                    Button(
                        onClick = onAccept,
                        enabled = isChecked && hasScrolledToBottom,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Accept & Continue")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            Text(
                "📄 Terms & Conditions",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("License: CC BY-NC 4.0", style = MaterialTheme.typography.bodySmall)
            Text("Last Updated: Oct 2023", style = MaterialTheme.typography.bodySmall)
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            TermsSection("1. License Agreement", "This Application and its source code are licensed under the Creative Commons Attribution-NonCommercial 4.0 International License (CC BY-NC 4.0). By using this software, you agree to these terms.")
            
            TermsSection("2. Non-Commercial Use Only", "This software is strictly for non-commercial purposes. You may use, modify and share for personal/educational use, but you CANNOT sell or monetize it.")

            TermsSection("3. Attribution Requirement", "If you use or modify this project, you MUST give proper credit to the original developer (AP) and reference this license.")

            TermsSection("4. Ethical Use Policy ⚠️", "STRICTLY for ethical and legal use. No unauthorized surveillance, spying, or recording without consent. Follow all local privacy laws.")

            TermsSection("5. User Responsibility", "You are solely responsible for how you use this app and for ensuring legal compliance in your jurisdiction.")

            TermsSection("6. Disclaimer of Liability", "Provided \"AS IS\". The developer is NOT responsible for misuse, illegal use, privacy violations, or any legal consequences.")

            TermsSection("7. No Warranty", "No guarantee of functionality, security, or accuracy. Use at your own risk.")

            TermsSection("8. Third-Party Modifications", "If you fork this project, you are responsible for your version and must follow CC BY-NC 4.0 terms.")

            TermsSection("11. Governing Law", "These terms are governed by the laws of India.")
            
            TermsSection("12. Contact", "Developer: AP\nEmail: ap0803apap@gmail.com")

            Spacer(modifier = Modifier.height(32.dp))
            Text(
                "🚨 IMPORTANT",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                "Consent is required before recording any individual. Developer is NOT responsible for misuse.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun TermsSection(title: String, content: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(content, style = MaterialTheme.typography.bodyMedium)
    }
}