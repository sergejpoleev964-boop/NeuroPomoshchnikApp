package ru.neuropomoshchnik.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun VoiceScreen(
    status: String,
    voices: List<String>,
    selectedVoice: String,
    active: Boolean,
    onVoiceSelected: (String) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Голосовой разговор", style = MaterialTheme.typography.headlineSmall)
        Text(status, style = MaterialTheme.typography.bodyMedium)

        if (voices.isNotEmpty()) {
            Text("Голос помощника")
            voices.forEach { voice ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedVoice == voice,
                        onClick = { onVoiceSelected(voice) }
                    )
                    Text(voice)
                }
            }
        } else {
            Text("Русские голоса загружаются…")
        }

        Button(
            onClick = if (active) onStop else onStart,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (active) "Завершить разговор" else "Начать разговор")
        }

        OutlinedButton(onClick = onClose) {
            Text("Вернуться в чат")
        }
    }
}
