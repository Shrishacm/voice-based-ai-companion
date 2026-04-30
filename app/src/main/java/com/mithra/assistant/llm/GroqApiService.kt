package com.mithra.assistant.llm

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

data class GroqMessage(
    val role: String,
    val content: String
)

data class GroqRequest(
    val messages: List<GroqMessage>,
    val model: String,
    val temperature: Float = 0.7f,
    val max_tokens: Int = 512
)

data class GroqResponse(
    val choices: List<Choice>,
    val usage: Usage?
)

data class Choice(
    val message: GroqMessage
)

data class Usage(
    val total_tokens: Int
)

interface GroqApiService {
    @Headers("Content-Type: application/json")
    @POST("openai/v1/chat/completions")
    fun createChatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: GroqRequest
    ): Call<GroqResponse>
}
