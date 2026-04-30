package com.mithra.assistant.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

private const val TAG = "VOICE_TTS"

class TTSEngine(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var pendingText: String? = null
    private var pendingLanguage: String? = null

    init {
        initializeTTS()
    }

    private fun initializeTTS() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                Log.d(TAG, "TTS initialized successfully")
                pendingText?.let { text ->
                    val lang = pendingLanguage ?: "en-IN"
                    speak(text, lang)
                    pendingText = null
                    pendingLanguage = null
                }
            } else {
                Log.e(TAG, "TTS initialization failed: $status")
            }
        }

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d(TAG, "TTS started")
            }

            override fun onDone(utteranceId: String?) {
                Log.d(TAG, "TTS done")
                onDoneCallback?.invoke()
            }

            override fun onError(utteranceId: String?) {
                Log.e(TAG, "TTS error")
            }
        })
    }

    private var onDoneCallback: (() -> Unit)? = null

    fun speak(text: String, language: String, onDone: (() -> Unit)? = null) {
        if (!isInitialized) {
            pendingText = text
            pendingLanguage = language
            onDoneCallback = onDone
            return
        }

        onDoneCallback = onDone

        val locale = languageToLocale(language)
        tts?.language = locale

        val result = tts?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "utterance_${System.currentTimeMillis()}"
        )

        Log.d(TAG, "Speaking [$language]: $text (result: $result)")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
    }

    private fun languageToLocale(language: String): Locale {
        return when (language) {
            "kn-IN" -> Locale("kan", "IN")
            "hi-IN" -> Locale("hin", "IN")
            else -> Locale("en", "IN")
        }
    }
}
