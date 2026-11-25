package ru.neuropomoshchnik.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF17152B)
private val Purple = Color(0xFF7657F6)
private val Pink = Color(0xFFFF6FAE)
private val Teal = Color(0xFF45D6C5)
private val SurfaceDark = Color(0xFF24213D)

data class ChatMessage(val text: String, val fromUser: Boolean)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NeuroApp() }
    }
}

@Composable
fun NeuroApp() {
    var prompt by remember { mutableStateOf("") }
    var selectedTool by remember { mutableStateOf("Чат") }
    val messages = remember {
        mutableStateListOf(
            ChatMessage("Привет! Я НейроПомощник ✨\\nПока я работаю в демонстрационном режиме. Напиши вопрос или выбери инструмент ниже.", false)
        )
    }

    val gradient = Brush.linearGradient(listOf(Color(0xFF6A4CFF), Color(0xFFB84BEA), Color(0xFFFF70A8)))

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFB9A8FF),
            background = Ink,
            surface = SurfaceDark,
            onBackground = Color.White,
            onSurface = Color.White
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color(0xFF211A45), Ink, Color(0xFF151326)))
            )
        ) {
            Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
                Spacer(Modifier.height(22.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(48.dp).background(gradient, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(27.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("НейроПомощник", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Твой AI-помощник на каждый день", fontSize = 12.sp, color = Color(0xFFB8B3D2))
                    }
                    Surface(color = Color(0xFF24443F), shape = RoundedCornerShape(50)) {
                        Text("БЕТА", modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                            color = Teal, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(22.dp))
                Text("Чем могу помочь?", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Text("Задай вопрос или выбери готовый инструмент", color = Color(0xFFB8B3D2), fontSize = 13.sp)
                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ToolChip("Чат", Icons.Default.ChatBubbleOutline, selectedTool == "Чат") { selectedTool = "Чат" }
                    ToolChip("Тексты", Icons.Default.EditNote, selectedTool == "Тексты") { selectedTool = "Тексты" }
                    ToolChip("Перевод", Icons.Default.Translate, selectedTool == "Перевод") { selectedTool = "Перевод" }
                    ToolChip("Идеи", Icons.Default.Lightbulb, selectedTool == "Идеи") { selectedTool = "Идеи" }
                }

                Spacer(Modifier.height(14.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(messages) { msg ->
                        MessageBubble(msg)
                    }
                }

                Surface(
                    color = SurfaceDark,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        OutlinedTextField(
                            value = prompt,
                            onValueChange = { prompt = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Напиши сообщение…", color = Color(0xFF9994B4)) },
                            minLines = 1,
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                cursorColor = Pink
                            )
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Демо-режим • ответы пока примерные", color = Color(0xFF9F99B8), fontSize = 10.sp, modifier = Modifier.weight(1f))
                            Button(
                                onClick = {
                                    val clean = prompt.trim()
                                    if (clean.isNotEmpty()) {
                                        messages.add(ChatMessage(clean, true))
                                        messages.add(ChatMessage(demoReply(clean, selectedTool), false))
                                        prompt = ""
                                    }
                                },
                                enabled = prompt.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = Purple),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Icon(Icons.Default.Send, null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Отправить")
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Проверяй важную информацию: ответы демонстрационные.",
                    color = Color(0xFF77728F), fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp)
                )
            }
        }
    }
}

@Composable
fun ToolChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 11.sp) },
        leadingIcon = { Icon(icon, null, modifier = Modifier.size(15.dp)) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFF5945A8),
            selectedLabelColor = Color.White,
            selectedLeadingIconColor = Color.White,
            containerColor = Color(0xFF292541),
            labelColor = Color(0xFFD7D2EC),
            iconColor = Color(0xFFD7D2EC)
        ),
        border = null
    )
}

@Composable
fun MessageBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (message.fromUser) Color(0xFF5945A8) else SurfaceDark,
            shape = RoundedCornerShape(
                topStart = 18.dp, topEnd = 18.dp,
                bottomStart = if (message.fromUser) 18.dp else 5.dp,
                bottomEnd = if (message.fromUser) 5.dp else 18.dp
            ),
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            Text(message.text, color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(14.dp))
        }
    }
}

fun demoReply(input: String, tool: String): String {
    return when (tool) {
        "Тексты" -> "Демонстрация инструмента «Тексты» ✍️\\n\\nЯ могу помочь составить письмо, объявление или пост. В полноценной версии подключим AI, который создаст готовый текст по твоему запросу.\\n\\nТвой запрос: «$input»"
        "Перевод" -> "Демонстрация переводчика 🌍\\n\\nВ полноценной версии я смогу переводить текст между русским, английским, румынским и другими языками. Сейчас это пример ответа."
        "Идеи" -> "Давай разовьём эту идею 💡\\n\\nВ полноценной версии AI предложит несколько вариантов, оценит плюсы и поможет составить план. Сейчас это демонстрационный ответ на запрос: «$input»"
        else -> "Спасибо за вопрос! ✨\\n\\nЭто пока демонстрационный режим: настоящий AI ещё не подключён. Следующий этап — подключить сервер к AI API, чтобы ответы генерировались нейросетью.\\n\\nТвой запрос: «$input»"
    }
}
