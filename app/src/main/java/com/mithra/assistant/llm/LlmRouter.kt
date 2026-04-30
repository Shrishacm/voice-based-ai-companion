package com.mithra.assistant.llm

import android.util.Log

private const val TAG = "VOICE_LLM_ROUTER"

enum class RouterState {
    DETECT_LANGUAGE,
    ROUTE_TO_LLM,
    GENERATE_RESPONSE,
    FALLBACK,
    COMPLETE
}

enum class DetectedLanguage {
    KANNADA,
    ENGLISH,
    HINDI
}

data class RouterStateGraph(
    var currentState: RouterState = RouterState.DETECT_LANGUAGE,
    var detectedLanguage: DetectedLanguage = DetectedLanguage.ENGLISH,
    var userInput: String = "",
    var context: String = "",
    var response: String = ""
) {
    fun transitionTo(nextState: RouterState) {
        Log.d(TAG, "Router state: $currentState -> $nextState")
        currentState = nextState
    }
}

class LlmRouter {
    private val kannadaProvider = KannadaLlmProvider()
    private val englishProvider = EnglishLlmProvider()
    private val stateGraph = RouterStateGraph()

    fun reset() {
        stateGraph.currentState = RouterState.DETECT_LANGUAGE
        stateGraph.response = ""
    }

    suspend fun processInput(userInput: String, context: String): String {
        stateGraph.userInput = userInput
        stateGraph.context = context

        // Node 1: Detect Language
        stateGraph.transitionTo(RouterState.DETECT_LANGUAGE)
        stateGraph.detectedLanguage = detectLanguage(userInput)

        // Node 2: Route to appropriate LLM
        stateGraph.transitionTo(RouterState.ROUTE_TO_LLM)
        val selectedProvider = routeToProvider(stateGraph.detectedLanguage)

        // Node 3: Generate Response
        stateGraph.transitionTo(RouterState.GENERATE_RESPONSE)
        stateGraph.response = selectedProvider.generateResponse(userInput, context)

        // Node 4: Complete
        stateGraph.transitionTo(RouterState.COMPLETE)
        return stateGraph.response
    }

    private fun detectLanguage(text: String): DetectedLanguage {
        val kannadaChars = text.count { it in '\u0C80'..'\u0CFF' }
        val hindiChars = text.count { it in '\u0900'..'\u097F' }
        val totalChars = text.length.coerceAtLeast(1)

        val kannadaRatio = kannadaChars.toDouble() / totalChars
        val hindiRatio = hindiChars.toDouble() / totalChars

        return when {
            kannadaRatio > 0.3 -> DetectedLanguage.KANNADA
            hindiRatio > 0.3 -> {
                Log.d(TAG, "Hindi detected, routing to English LLM as fallback")
                DetectedLanguage.ENGLISH
            }
            else -> DetectedLanguage.ENGLISH
        }
    }

    private fun routeToProvider(language: DetectedLanguage): LlmProvider {
        return when (language) {
            DetectedLanguage.KANNADA -> {
                Log.d(TAG, "Routing to Kannada LLM")
                kannadaProvider
            }
            DetectedLanguage.ENGLISH -> {
                Log.d(TAG, "Routing to English LLM")
                englishProvider
            }
            DetectedLanguage.HINDI -> {
                Log.d(TAG, "Hindi detected, routing to English LLM as fallback")
                englishProvider
            }
        }
    }

    fun getActiveLanguage(): String {
        return when (stateGraph.detectedLanguage) {
            DetectedLanguage.KANNADA -> "kn-IN"
            DetectedLanguage.ENGLISH -> "en-IN"
            DetectedLanguage.HINDI -> "en-IN"
        }
    }

    fun getCurrentState(): RouterState = stateGraph.currentState
}
