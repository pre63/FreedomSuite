package org.freedomsuite.core.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun AccountSetupScreen(
    title: String,
    subtitle: String,
    isLoading: Boolean,
    errorMessage: String?,
    connectButtonText: String = "Connect",
    suggestManualSetup: Boolean = false,
    onConfigure: (email: String, password: String, manual: ManualMailSettings?) -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showManual by remember(suggestManualSetup) { mutableStateOf(suggestManualSetup) }
    var imapHost by remember { mutableStateOf("") }
    var imapPort by remember { mutableStateOf("993") }
    var smtpHost by remember { mutableStateOf("") }
    var smtpPort by remember { mutableStateOf("465") }

    FreedomScaffold(title = title, subtitle = subtitle) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email address") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("App password") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )
        if (!showManual) {
            TextButton(
                onClick = { showManual = true },
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                Text("Enter server settings manually")
            }
        }
        if (showManual) {
            OutlinedTextField(
                value = imapHost,
                onValueChange = { imapHost = it },
                label = { Text("IMAP server") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                singleLine = true,
            )
            OutlinedTextField(
                value = imapPort,
                onValueChange = { imapPort = it.filter { ch -> ch.isDigit() } },
                label = { Text("IMAP port") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            OutlinedTextField(
                value = smtpHost,
                onValueChange = { smtpHost = it },
                label = { Text("SMTP server") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                singleLine = true,
            )
            OutlinedTextField(
                value = smtpPort,
                onValueChange = { smtpPort = it.filter { ch -> ch.isDigit() } },
                label = { Text("SMTP port") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        Button(
            onClick = {
                if (!email.contains("@") || password.isBlank()) return@Button
                val manual = if (showManual && imapHost.isNotBlank() && smtpHost.isNotBlank()) {
                    ManualMailSettings(
                        imapHost = imapHost.trim(),
                        imapPort = imapPort.toIntOrNull() ?: 993,
                        smtpHost = smtpHost.trim(),
                        smtpPort = smtpPort.toIntOrNull() ?: 465,
                    )
                } else {
                    null
                }
                onConfigure(email.trim(), password, manual)
            },
            enabled = !isLoading && email.contains("@") && password.isNotBlank() &&
                (!showManual || (imapHost.isNotBlank() && smtpHost.isNotBlank())),
            modifier = Modifier.padding(top = 16.dp),
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
            }
            Text(connectButtonText)
        }
    }
}
