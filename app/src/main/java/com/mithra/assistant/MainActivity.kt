package com.mithra.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.mithra.assistant.llm.LlmRouter
import com.mithra.assistant.service.MithraWakeService
import com.mithra.assistant.service.SpeechSessionManager
import com.mithra.assistant.speech.MultilingualSTT
import com.mithra.assistant.speech.TTSEngine
import com.mithra.assistant.ui.DataManagementScreen
import com.mithra.assistant.ui.DataManagementViewModel
import com.mithra.assistant.ui.DebugScreen
import com.mithra.assistant.ui.MainViewModel

private const val TAG = "VOICE_MAIN"
private const val PERMISSION_REQUEST_CODE = 1001

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val dataViewModel: DataManagementViewModel by viewModels()
    private var sessionManager: SpeechSessionManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!checkPermissions()) {
            requestPermissions()
            return
        }

        setupUI()
        startWakeService()
        initializeSessionManager()
    }

    private fun checkPermissions(): Boolean {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                setupUI()
                startWakeService()
                initializeSessionManager()
            } else {
                Log.e(TAG, "Permissions denied")
            }
        }
    }

    private fun setupUI() {
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF0F1117),
                    surface = Color(0xFF1A1D27),
                    primary = Color(0xFF00C9B1),
                    onPrimary = Color(0xFF0F1117),
                    onBackground = Color(0xFFE8EAF6),
                    onSurface = Color(0xFFE8EAF6)
                )
            ) {
                MithraApp(viewModel, dataViewModel)
            }
        }
    }

    private fun startWakeService() {
        val intent = Intent(this, MithraWakeService::class.java)
        startForegroundService(intent)
        Log.d(TAG, "Wake service started")
    }

    private fun initializeSessionManager() {
        val app = application as MithraApplication
        val stt = MultilingualSTT(this)
        val tts = TTSEngine(this)
        val memoryRetriever = app.memoryRetriever
        val llmRouter = LlmRouter()

        sessionManager = SpeechSessionManager(this, stt, tts, memoryRetriever, llmRouter).apply {
            setStateListener { state -> viewModel.updateState(state) }
            setConversationCallback { input, response ->
                viewModel.setLastInput(input)
                viewModel.setLastResponse(response)
            }
            setPartialTranscriptionCallback { text -> viewModel.updateLiveTranscription(text) }
            start()
        }
    }

    override fun onDestroy() {
        sessionManager?.stop()
        super.onDestroy()
    }
}

// ─── Root Navigation ────────────────────────────────────────────────────────────

@Composable
fun MithraApp(viewModel: MainViewModel, dataViewModel: DataManagementViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = Color(0xFF0F1117),
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF1A1D27),
                contentColor = Color(0xFF00C9B1),
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.MicNone, contentDescription = "Assistant") },
                    label = { Text("Assistant") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF00C9B1),
                        selectedTextColor = Color(0xFF00C9B1),
                        unselectedIconColor = Color(0xFF9E9EB2),
                        unselectedTextColor = Color(0xFF9E9EB2),
                        indicatorColor = Color(0xFF00C9B1).copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.HealthAndSafety, contentDescription = "Health Data") },
                    label = { Text("Health Data") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF00C9B1),
                        selectedTextColor = Color(0xFF00C9B1),
                        unselectedIconColor = Color(0xFF9E9EB2),
                        unselectedTextColor = Color(0xFF9E9EB2),
                        indicatorColor = Color(0xFF00C9B1).copy(alpha = 0.15f)
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF0F1117))
        ) {
            when (selectedTab) {
                0 -> DebugScreen(viewModel = viewModel)
                1 -> DataManagementScreen(viewModel = dataViewModel)
            }
        }
    }
}
