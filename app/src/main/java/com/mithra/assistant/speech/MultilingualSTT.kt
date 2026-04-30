package com.mithra.assistant.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "VOICE_STT"

class MultilingualSTT(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var currentCallbackActive = false

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private var onResultsCallback: ((String) -> Unit)? = null
    private var onErrorCallback: ((Int) -> Unit)? = null
    private var onPartialResultCallback: ((String) -> Unit)? = null

    init {
        createRecognizer()
    }

    private fun createRecognizer() {
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                    _partialText.value = ""
                    Log.d(TAG, "Ready for speech")
                }

                override fun onBeginningOfSpeech() {
                    Log.d(TAG, "Speech beginning")
                }

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    Log.d(TAG, "Speech ended")
                    isListening = false
                }

                override fun onError(error: Int) {
                    Log.w(TAG, "Recognition error: $error")
                    isListening = false
                    _partialText.value = ""
                    currentCallbackActive = false
                    onErrorCallback?.invoke(error)
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val bestMatch = matches?.firstOrNull() ?: ""
                    Log.d(TAG, "Final result: $bestMatch")
                    isListening = false
                    currentCallbackActive = false
                    onResultsCallback?.invoke(bestMatch)
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    partial?.firstOrNull()?.let { text ->
                        Log.d(TAG, "Partial: $text")
                        _partialText.value = text
                        onPartialResultCallback?.invoke(text)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    fun startListening(
        languages: List<String> = listOf("kn-IN", "hi-IN", "en-IN"),
        silenceTimeoutMs: Long = 15000,
        onResults: (String) -> Unit,
        onError: (Int) -> Unit,
        onPartialResult: (String) -> Unit = {}
    ) {
        onResultsCallback = onResults
        onErrorCallback = onError
        onPartialResultCallback = onPartialResult
        currentCallbackActive = true

        if (isListening) {
            stopListening()
        }

        val recognizer = speechRecognizer
        if (recognizer == null || !SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "Speech recognition not available, recreating")
            createRecognizer()
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languages.first())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", languages.toTypedArray())
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, silenceTimeoutMs)
        }

        try {
            speechRecognizer?.startListening(intent)
            Log.d(TAG, "STT started listening with timeout=${silenceTimeoutMs}ms")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start listening: ${e.message}")
            currentCallbackActive = false
            onError(-2)
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            isListening = false
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping: ${e.message}")
        }
    }

    fun cancel() {
        try {
            currentCallbackActive = false
            speechRecognizer?.cancel()
            isListening = false
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling: ${e.message}")
        }
    }

    fun destroy() {
        cancel()
        onResultsCallback = null
        onErrorCallback = null
        onPartialResultCallback = null
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
