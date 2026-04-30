package com.mithra.assistant.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.VoiceChat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mithra.assistant.service.SessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val tag: String,
    val message: String
)

class MainViewModel : ViewModel() {

    private val _state = MutableStateFlow<SessionState>(SessionState.STANDBY)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private val _liveTranscription = MutableStateFlow("")
    val liveTranscription: StateFlow<String> = _liveTranscription.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val _lastInput = MutableStateFlow("")
    val lastInput: StateFlow<String> = _lastInput.asStateFlow()

    private val _lastResponse = MutableStateFlow("")
    val lastResponse: StateFlow<String> = _lastResponse.asStateFlow()

    fun updateState(newState: SessionState) {
        viewModelScope.launch {
            _state.value = newState
            addLog("SESSION", "State -> $newState")
        }
    }

    fun updateLiveTranscription(text: String) {
        _liveTranscription.value = text
    }

    fun clearLiveTranscription() {
        _liveTranscription.value = ""
    }

    fun setLastInput(text: String) {
        _lastInput.value = text
        _liveTranscription.value = ""
        addLog("STT", "User: $text")
    }

    fun setLastResponse(text: String) {
        _lastResponse.value = text
        addLog("TTS", "Assistant: $text")
    }

    fun addLog(tag: String, message: String) {
        viewModelScope.launch {
            _logs.value = _logs.value + LogEntry(tag = tag, message = message)
        }
    }
}

@Composable
fun DebugScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val lastInput by viewModel.lastInput.collectAsState()
    val lastResponse by viewModel.lastResponse.collectAsState()
    val liveTranscription by viewModel.liveTranscription.collectAsState()

    val isListening = state == SessionState.LISTENING
    val isProcessing = state in listOf(
        SessionState.TRANSCRIBING,
        SessionState.RETRIEVING_MEMORY,
        SessionState.VERIFYING_FACTS
    )
    val isSpeaking = state == SessionState.SPEAKING

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Mithra",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when {
                            isListening -> Icons.Default.Mic
                            isProcessing -> Icons.Default.Memory
                            isSpeaking -> Icons.Default.VoiceChat
                            state == SessionState.CLOSING_DETECTED -> Icons.Default.CheckCircle
                            else -> Icons.Default.MicOff
                        },
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = when {
                            isListening -> Color(0xFF2196F3)
                            isProcessing -> Color(0xFFFF9800)
                            isSpeaking -> Color(0xFF4CAF50)
                            state == SessionState.CLOSING_DETECTED -> Color(0xFF9C27B0)
                            else -> Color.Gray
                        }
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = when (state) {
                            SessionState.STANDBY -> "Standby — say \"Mithra\""
                            SessionState.WAKE_TRIGGERED -> "Wake triggered"
                            SessionState.LISTENING -> "Listening... (up to 20s)"
                            SessionState.TRANSCRIBING -> "Transcribing..."
                            SessionState.RETRIEVING_MEMORY -> "Retrieving memory..."
                            SessionState.VERIFYING_FACTS -> "Verifying facts..."
                            SessionState.SPEAKING -> "Speaking..."
                            SessionState.CLOSING_DETECTED -> "Closing detected — ending session"
                            SessionState.TIMEOUT -> "Session timeout"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                AnimatedVisibility(
                    visible = isListening && liveTranscription.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Text(
                            text = "Live Transcription",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = Color(0xFFE3F2FD),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Text(
                                text = liveTranscription,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = Color(0xFF1565C0)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF2196F3), shape = androidx.compose.foundation.shape.CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Listening in Kannada / Hindi / English",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedVisibility(
            visible = lastInput.isNotEmpty() || lastResponse.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    if (lastInput.isNotEmpty()) {
                        Row {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .align(Alignment.Top)
                                    .background(Color(0xFF2196F3), shape = androidx.compose.foundation.shape.CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "You: $lastInput",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    if (lastResponse.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .align(Alignment.Top)
                                    .background(Color(0xFF4CAF50), shape = androidx.compose.foundation.shape.CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Mithra: $lastResponse",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Debug Log",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(logs.takeLast(50).reversed()) { entry ->
                Text(
                    text = "[${entry.tag}] ${entry.message}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(2.dp))
            }
        }
    }
}
