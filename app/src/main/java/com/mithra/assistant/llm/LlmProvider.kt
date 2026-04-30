package com.mithra.assistant.llm

interface LlmProvider {
    suspend fun generateResponse(userInput: String, context: String): String
    fun getModelName(): String
    fun getLanguage(): String
}
