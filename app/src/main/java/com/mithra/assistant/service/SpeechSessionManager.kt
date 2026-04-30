package com.mithra.assistant.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.mithra.assistant.llm.LlmRouter
import com.mithra.assistant.network.MemoryRetriever
import com.mithra.assistant.network.QueryIntent
import com.mithra.assistant.speech.MultilingualSTT
import com.mithra.assistant.speech.TTSEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "VOICE_SESSION"
private const val NO_INPUT_TIMEOUT_MS = 20000L
private const val POST_CLOSING_WINDOW_MS = 120000L

enum class SessionState {
    STANDBY,
    WAKE_TRIGGERED,
    LISTENING,
    TRANSCRIBING,
    RETRIEVING_MEMORY,
    VERIFYING_FACTS,
    SPEAKING,
    CLOSING_DETECTED,
    TIMEOUT
}

class SpeechSessionManager(
    private val context: Context,
    private val stt: MultilingualSTT,
    private val tts: TTSEngine,
    private val memoryRetriever: MemoryRetriever,
    private val llmRouter: LlmRouter
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var currentState: SessionState = SessionState.STANDBY
    private var sessionJob: Job? = null
    private var sessionId: String = generateSessionId()
    private var detectedLanguage: String = "en-IN"
    private var conversationHistory = mutableListOf<String>()
    private var isConversationActive = false
    private var isAfterSoftClosing = false
    private var consecutiveErrors = 0

    private var stateListener: ((SessionState) -> Unit)? = null
    private var conversationCallback: ((String, String) -> Unit)? = null
    private var partialTranscriptionCallback: ((String) -> Unit)? = null

    fun setPartialTranscriptionCallback(callback: (String) -> Unit) {
        partialTranscriptionCallback = callback
    }

    private val wakeReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val wakeText = intent?.getStringExtra("wake_text")
            Log.d(TAG, "Wake received: $wakeText, active=$isConversationActive, state=$currentState")
            if (!isConversationActive) {
                startSession()
            } else {
                Log.d(TAG, "Wake ignored — conversation already active")
            }
        }
    }

    fun setStateListener(listener: (SessionState) -> Unit) {
        stateListener = listener
    }

    fun setConversationCallback(callback: (String, String) -> Unit) {
        conversationCallback = callback
    }

    fun start() {
        val filter = IntentFilter("com.mithra.WAKE_TRIGGERED")
        context.registerReceiver(wakeReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        Log.d(TAG, "SpeechSessionManager started, waiting for wake word")
    }

    fun stop() {
        try {
            context.unregisterReceiver(wakeReceiver)
        } catch (e: Exception) {
            Log.w(TAG, "Receiver already unregistered")
        }
        sessionJob?.cancel()
        scope.cancel()
        stt.cancel()
        tts.stop()
        isConversationActive = false
        Log.d(TAG, "SpeechSessionManager stopped")
    }

    fun getCurrentState(): SessionState = currentState

    fun getSessionId(): String = sessionId

    private fun setState(state: SessionState) {
        currentState = state
        Log.d(TAG, "State -> $state")
        stateListener?.invoke(state)
    }

    private fun startSession() {
        sessionJob?.cancel()
        sessionId = generateSessionId()
        isConversationActive = true
        conversationHistory.clear()
        consecutiveErrors = 0
        setState(SessionState.WAKE_TRIGGERED)

        val wakeIntent = Intent("com.mithra.SESSION_STARTED")
        wakeIntent.setPackage(context.packageName)
        context.sendBroadcast(wakeIntent)

        val wakeResponse = when (detectedLanguage) {
            "kn-IN" -> "ಹೌದು, ಹೇಳಿ?"
            "hi-IN" -> "जी हाँ, बताइए?"
            else -> "Yes?"
        }
        tts.speak(wakeResponse, detectedLanguage)

        sessionJob = scope.launch {
            delay(1500)
            beginListeningTurn()
        }
    }

    private fun beginListeningTurn() {
        if (!isConversationActive) {
            Log.d(TAG, "beginListeningTurn skipped — conversation ended")
            return
        }

        setState(SessionState.LISTENING)

        val timeout = if (isAfterSoftClosing) {
            Log.d(TAG, "Using post-closing window: ${POST_CLOSING_WINDOW_MS / 1000}s")
            POST_CLOSING_WINDOW_MS
        } else {
            NO_INPUT_TIMEOUT_MS
        }

        val languages = listOf("kn-IN", "hi-IN", "en-IN")

        stt.startListening(
            languages = languages,
            silenceTimeoutMs = timeout,
            onResults = { text ->
                Log.d(TAG, "STT final result: '$text'")
                consecutiveErrors = 0
                partialTranscriptionCallback?.invoke("")

                if (text.isNotBlank()) {
                    processUserInput(text)
                } else {
                    Log.d(TAG, "Silence detected (${NO_INPUT_TIMEOUT_MS / 1000}s), re-listening...")
                    scope.launch {
                        delay(500)
                        beginListeningTurn()
                    }
                }
            },
            onError = { error ->
                Log.w(TAG, "STT error: $error (consecutive: ${consecutiveErrors + 1})")
                consecutiveErrors++
                partialTranscriptionCallback?.invoke("")

                if (consecutiveErrors >= 3) {
                    Log.w(TAG, "Too many consecutive errors, ending session")
                    endSession()
                } else {
                    Log.d(TAG, "Retrying STT...")
                    scope.launch {
                        delay(1000)
                        beginListeningTurn()
                    }
                }
            },
            onPartialResult = { text ->
                Log.d(TAG, "STT partial: $text")
                partialTranscriptionCallback?.invoke(text)
            }
        )
    }

    private fun processUserInput(userText: String) {
        scope.launch {
            isAfterSoftClosing = false
            setState(SessionState.TRANSCRIBING)

            val lang = detectLanguage(userText)
            detectedLanguage = lang

            conversationHistory.add(userText)

            val isGoodbye = detectGoodbyePhrase(userText)
            val isSoftClosing = detectSoftClosingPhrase(userText)

            if (isGoodbye) {
                setState(SessionState.CLOSING_DETECTED)

                val goodbye = when (detectedLanguage) {
                    "kn-IN" -> "ಗುಡ್‌ಬೈ! ಜಾಗ್ರತೆ."
                    "hi-IN" -> "अलविदा! अपना ख्याल रखना।"
                    else -> "Goodbye! Take care."
                }

                setState(SessionState.SPEAKING)
                tts.speak(goodbye, detectedLanguage) {
                    scope.launch {
                        delay(1000)
                        endSession()
                    }
                }

                conversationCallback?.invoke(userText, goodbye)
                saveConversationToMemory(userText, goodbye, detectedLanguage)
            } else if (isSoftClosing) {
                setState(SessionState.CLOSING_DETECTED)

                val ackResponse = when (detectedLanguage) {
                    "kn-IN" -> "ಸ್ವಾಗತ! ಇನ್ನೇನಾದರೂ ಕೇಳಿ. ಎರಡು ನಿಮಿಷ ಕಾಯುತ್ತೇನೆ."
                    "hi-IN" -> "स्वागत है! और कुछ पूछें। दो मिनट सुनूंगा।"
                    else -> "You're welcome! I'll listen for two more minutes."
                }

                isAfterSoftClosing = true

                setState(SessionState.SPEAKING)
                tts.speak(ackResponse, detectedLanguage) {
                    scope.launch {
                        delay(800)
                        beginListeningTurn()
                    }
                }

                conversationCallback?.invoke(userText, ackResponse)
                saveConversationToMemory(userText, ackResponse, detectedLanguage)
            } else {
                try {
                    setState(SessionState.RETRIEVING_MEMORY)
                    val intentResult = memoryRetriever.detectIntent(userText)
                    val retrievedContext = memoryRetriever.buildContextForQuery(userText)
                    Log.d(TAG, "Intent: ${intentResult.intent}, Context: ${retrievedContext.take(200)}")

                    setState(SessionState.VERIFYING_FACTS)
                    val response = generateResponse(userText, retrievedContext, intentResult.intent, detectedLanguage)

                    setState(SessionState.SPEAKING)
                    tts.speak(response, detectedLanguage) {
                        scope.launch {
                            delay(1000)
                            beginListeningTurn()
                        }
                    }

                    conversationCallback?.invoke(userText, response)
                    saveConversationToMemory(userText, response, detectedLanguage)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to generate or speak LLM response, using fallback", e)
                    val fallback = when (detectedLanguage) {
                        "kn-IN" -> "ಕ್ಷಮಿಸಿ, ಈಗ ಉತ್ತರಿಸಲು ಸಾಧ್ಯವಾಗಲಿಲ್ಲ. ದಯವಿಟ್ಟು ಮತ್ತೆ ಪ್ರಯತ್ನಿಸಿ."
                        "hi-IN" -> "माफ़ करें, अभी जवाब देने में असमर्थ हूँ। कृपया फिर प्रयास करें।"
                        else -> "I'm sorry, I couldn't process your request right now. Please try again."
                    }
                    setState(SessionState.SPEAKING)
                    tts.speak(fallback, detectedLanguage) {
                        scope.launch {
                            delay(1000)
                            beginListeningTurn()
                        }
                    }
                    conversationCallback?.invoke(userText, fallback)
                }
            }
        }
    }

    private fun endSession() {
        isConversationActive = false
        setState(SessionState.TIMEOUT)
        scope.launch {
            delay(1000)
            setState(SessionState.STANDBY)
            Log.d(TAG, "Session ended, returning to standby")
        }
    }

    private fun detectGoodbyePhrase(text: String): Boolean {
        val lower = text.lowercase().trim()

        val goodbyeEn = listOf(
            "goodbye", "bye", "bye bye", "see you", "see ya", "good night",
            "goodnight", "take care", "catch you later"
        )

        val goodbyeKn = listOf(
            "ಬೈ", "ಗುಡ್ ಬೈ", "ಗುಡ್‌ಬೈ", "ಶುಭ ರಾತ್ರಿ", "ಸಿ ಯು",
            "ಹೋಗಿ ಬರ್ತೇನೆ", "ಇನ್ನು ಬರ್ತಿನಿ"
        )

        val goodbyeHi = listOf(
            "अलविदा", "बाय", "बाय बाय", "नमस्ते", "फिर मिलेंगे",
            "खुद का ख्याल रखना", "शुभ रात्रि"
        )

        return goodbyeEn.any { lower.contains(it) } ||
                goodbyeKn.any { lower.contains(it) } ||
                goodbyeHi.any { lower.contains(it) }
    }

    private fun detectSoftClosingPhrase(text: String): Boolean {
        val lower = text.lowercase().trim()

        val softEn = listOf(
            "thank you", "thanks", "thankyou", "thank u", "thx",
            "okay", "ok", "alright", "alrighty", "got it",
            "that's all", "thats all", "nothing else", "no thanks",
            "done", "finished", "perfect", "great thanks",
            "dhanyavadagalu", "dhanyavada"
        )

        val softKn = listOf(
            "ಧನ್ಯವಾದ", "ಥ್ಯಾಂಕ್ಸ್", "ಥ್ಯಾಂಕ್ಯೂ", "ಧನ್ಯವಾದಗಳು",
            "ಸರಿ", "ಓಕೆ", "ಆಗಲಿ", "ಹೌದು ಸರಿ",
            "ಎಲ್ಲಾ ಮುಗಿದಿದೆ", "ಇನ್ನೇನೂ ಬೇಡ", "ಸರಿ ಧನ್ಯವಾದ",
            "ಆಯ್ತು", "ಆಯ್ತು ಧನ್ಯವಾದ"
        )

        val softHi = listOf(
            "धन्यवाद", "शुक्रिया", "थैंक्स", "थैंक यू",
            "ठीक है", "ओके", "अच्छा", "सही है",
            "बस इतना ही", "और कुछ नहीं", "कोई बात नहीं",
            "हो गया", "खत्म"
        )

        return softEn.any { lower.contains(it) } ||
                softKn.any { lower.contains(it) } ||
                softHi.any { lower.contains(it) }
    }

    private fun detectLanguage(text: String): String {
        val kannadaChars = text.count { it in '\u0C80'..'\u0CFF' }
        val hindiChars = text.count { it in '\u0900'..'\u097F' }

        return when {
            kannadaChars > hindiChars && kannadaChars > text.length * 0.3 -> "kn-IN"
            hindiChars > text.length * 0.3 -> "hi-IN"
            else -> "en-IN"
        }
    }

    private suspend fun generateResponse(
        userText: String,
        context: String,
        intent: QueryIntent,
        language: String
    ): String {
        return llmRouter.processInput(userText, context)
    }

    private suspend fun saveConversationToMemory(userText: String, response: String, language: String) {
        memoryRetriever.storeMemory(
            content = "User asked: $userText. Assistant responded: $response",
            category = "conversation"
        )
    }

    private fun generateSessionId(): String {
        return "session_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
}
