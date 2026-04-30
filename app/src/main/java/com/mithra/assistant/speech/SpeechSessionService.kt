package com.mithra.assistant.speech

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.mithra.assistant.MithraApplication
import com.mithra.assistant.llm.LlmRouter
import com.mithra.assistant.service.SpeechSessionManager

private const val TAG = "VOICE_SESSION_SVC"

class SpeechSessionService : Service() {

    private var sessionManager: SpeechSessionManager? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "SpeechSessionService created")

        val app = application as MithraApplication
        val stt = MultilingualSTT(this)
        val tts = TTSEngine(this)
        val memoryRetriever = app.memoryRetriever
        val llmRouter = LlmRouter()

        sessionManager = SpeechSessionManager(this, stt, tts, memoryRetriever, llmRouter).apply {
            start()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        sessionManager?.stop()
        super.onDestroy()
    }
}
