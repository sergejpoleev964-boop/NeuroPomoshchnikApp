package ru.neuropomoshchnik.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun VoiceModeDialog(onClose: () -> Unit) {
    val context = LocalContext.current
    val status = remember { mutableStateOf("Подготовка голоса…") }
    val voices = remember { mutableStateListOf<String>() }
    val selectedVoice = remember { mutableStateOf("") }
    val active = remember { mutableStateOf(false) }
    val controllerRef = remember { mutableStateOf<VoiceController?>(null) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    val controller = remember(context) {
        VoiceController(
            context = context,
            status = { message ->
                mainHandler.post { status.value = message }
            },
            recognized = { phrase ->
                mainHandler.post { status.value = "Отправляю вопрос…" }
                Thread {
                    val answer = try {
                        val connection = URL(
                            "https://neuropomoshchnikapp.onrender.com/chat"
                        ).openConnection() as HttpURLConnection

                        try {
                            connection.requestMethod = "POST"
                            connection.connectTimeout = 20000
                            connection.readTimeout = 60000
                            connection.doOutput = true
                            connection.setRequestProperty(
                                "Content-Type",
                                "application/json; charset=utf-8"
                            )
                            connection.outputStream.use {
                                it.write(
                                    JSONObject().put("message", phrase)
                                        .toString()
                                        .toByteArray(Charsets.UTF_8)
                                )
                            }

                            val responseCode = connection.responseCode
                            val stream = if (responseCode in 200..299) {
                                connection.inputStream
                            } else {
                                connection.errorStream
                            }
                            val body = stream?.bufferedReader()?.use { it.readText() }
                                .orEmpty()

                            if (responseCode in 200..299) {
                                JSONObject(body).optString("answer")
                                    .ifBlank { "Сервер не вернул ответ." }
                            } else {
                                "Ошибка сервера $responseCode. $body"
                            }
                        } finally {
                            connection.disconnect()
                        }
                    } catch (e: Exception) {
                        "Ошибка соединения: ${e.message ?: "неизвестная ошибка"}"
                    }

                    mainHandler.post {
                        if (active.value) {
                            controllerRef.value?.speak(answer, selectedVoice.value)
                        }
                    }
                }.start()
            },
            finished = {
                if (active.value) controllerRef.value?.listen()
            },
            voicesReady = { available ->
                voices.clear()
                voices.addAll(available)
                if (selectedVoice.value !in available) {
                    selectedVoice.value = available.firstOrNull().orEmpty()
                }
            }
        ).also {
            controllerRef.value = it
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            active.value = true
            controller.listen()
        } else {
            status.value = "Для разговора нужно разрешение на микрофон"
        }
    }

    DisposableEffect(controller) {
        onDispose {
            active.value = false
            controller.stop()
            controller.release()
        }
    }

    Dialog(onDismissRequest = onClose) {
        Surface {
            VoiceScreen(
                status = status.value,
                voices = voices.toList(),
                selectedVoice = selectedVoice.value,
                active = active.value,
                onVoiceSelected = { selectedVoice.value = it },
                onStart = {
                    if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                        == PackageManager.PERMISSION_GRANTED
                    ) {
                        active.value = true
                        controller.listen()
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onStop = {
                    active.value = false
                    controller.stop()
                    status.value = "Разговор завершён"
                },
                onClose = onClose
            )
        }
    }
}
