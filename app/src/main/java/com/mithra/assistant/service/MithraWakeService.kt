package com.mithra.assistant.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.app.Service
import android.os.Bundle
import com.mithra.assistant.MainActivity
import com.mithra.assistant.R

private const val TAG = "VOICE_WAKE"
private const val CHANNEL_ID = "mithra_wake_channel"
private const val NOTIFICATION_ID = 1
private const val DEBOUNCE_MS = 2000L

class MithraWakeService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var lastWakeDetectedAt = 0L
    private var serviceJob: Job? = null

    private val wakeVariants = setOf(
        "mithra", "mitra", "मित्र", "ಮಿತ್ರ", "ಮಿಥ್ರ", "mithraa", "mitraa"
    )

    private val sessionStartedReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            Log.d(TAG, "Session started — stopping wake STT")
            stopListening()
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "MithraWakeService created")
        createNotificationChannel()
        startForegroundService()
        registerReceiver(sessionStartedReceiver, IntentFilter("com.mithra.SESSION_STARTED"), Context.RECEIVER_NOT_EXPORTED)
        initializeSpeechRecognizer()
    }

    private fun startListeningLoop() {
        serviceJob = serviceScope.launch {
            while (true) {
                if (!isListening) {
                    startListening()
                }
                delay(1000)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Wake service started, beginning listening loop")
        startListeningLoop()
        return START_STICKY
    }

    override fun onBind(intent: Intent?) = null

    override fun onDestroy() {
        Log.d(TAG, "Wake service destroyed")
        serviceJob?.cancel()
        stopListening()
        speechRecognizer?.destroy()
        serviceScope.cancel()
        try {
            unregisterReceiver(sessionStartedReceiver)
        } catch (e: Exception) {
            Log.w(TAG, "Receiver already unregistered")
        }
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.channel_description)
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun startForegroundService() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Mithra")
            .setContentText("Listening for wake word...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun initializeSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.e(TAG, "Speech recognition not available")
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                }

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    isListening = false
                }

                override fun onError(error: Int) {
                    Log.w(TAG, "Recognition error: $error, restarting in 500ms")
                    isListening = false
                    serviceScope.launch {
                        delay(500)
                        startListening()
                    }
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    matches?.firstOrNull()?.let { text ->
                        processWakeCandidate(text)
                    }
                    serviceScope.launch {
                        delay(300)
                        startListening()
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        startListening()
    }

    private fun startListening() {
        if (isListening) return

        val recognizer = speechRecognizer ?: return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "kn-IN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("hi-IN", "en-IN"))
        }

        try {
            recognizer.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start listening", e)
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            isListening = false
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping listener", e)
        }
    }

    private fun processWakeCandidate(text: String) {
        val normalized = text.lowercase().trim()

        val isWakeWord = wakeVariants.any { variant ->
            normalized.contains(variant) || variant.contains(normalized)
        }

        if (!isWakeWord) return

        val now = System.currentTimeMillis()
        if (now - lastWakeDetectedAt < DEBOUNCE_MS) {
            Log.d(TAG, "Wake word debounced: $text")
            return
        }

        lastWakeDetectedAt = now
        Log.d(TAG, "Wake word detected: $text")

        stopListening()

        val appIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(appIntent)

        val intent = Intent("com.mithra.WAKE_TRIGGERED").apply {
            setPackage(packageName)
            putExtra("wake_text", text)
        }
        sendBroadcast(intent)

        serviceScope.launch {
            delay(DEBOUNCE_MS)
            startListening()
        }
    }
}
