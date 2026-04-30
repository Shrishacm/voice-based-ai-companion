package com.mithra.assistant.llm

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

private const val TAG = "VOICE_LLM"
private const val GROQ_BASE_URL = "https://api.groq.com/"
private const val GROQ_API_KEY = "YOUR_GROQ_API_KEY_HERE"
abstract class GroqLlmProvider(
    private val systemPrompt: String,
    private val modelName: String,
    private val language: String
) : LlmProvider {

    private val apiService: GroqApiService by lazy {
        val client = okhttp3.OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(GROQ_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GroqApiService::class.java)
    }

    override suspend fun generateResponse(userInput: String, context: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val messages = listOf(
                    GroqMessage("system", systemPrompt),
                    GroqMessage("system", "Context from patient records:\n$context"),
                    GroqMessage("user", userInput)
                )

                val request = GroqRequest(
                    messages = messages,
                    model = modelName,
                    temperature = 0.5f,
                    max_tokens = 512
                )

                val response = apiService.createChatCompletion(
                    authorization = "Bearer $GROQ_API_KEY",
                    request = request
                ).execute()

                if (response.isSuccessful && response.body() != null) {
                    val reply = response.body()!!.choices.firstOrNull()?.message?.content
                        ?: "I'm sorry, I couldn't generate a response."
                    Log.d(TAG, "LLM ($language) response: ${reply.take(100)}")
                    reply
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Log.e(TAG, "LLM ($language) API error: ${response.code()} - $errorBody")
                    getFallbackResponse(userInput)
                }
            } catch (e: Exception) {
                Log.e(TAG, "LLM ($language) request failed", e)
                getFallbackResponse(userInput)
            }
        }
    }

    override fun getModelName(): String = modelName
    override fun getLanguage(): String = language

    abstract fun getFallbackResponse(userInput: String): String
}
