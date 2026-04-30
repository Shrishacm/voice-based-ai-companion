package com.mithra.assistant.network

import android.util.Log
import com.mithra.assistant.data.local.dao.MemoryDao
import com.mithra.assistant.data.local.entity.MemoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

private const val TAG = "VOICE_MEMORY"

class MemoryRetriever(private val memoryDao: MemoryDao) {

    suspend fun storeMemory(content: String, category: String = "general") {
        val embedding = generateEmbedding(content)
        val entity = MemoryEntity(
            content = content,
            embedding = embedding,
            category = category
        )
        memoryDao.insert(entity)
        Log.d(TAG, "Stored memory [$category]: ${content.take(80)}")
    }

    suspend fun retrieveRelevant(query: String, topK: Int = 5): List<MemoryEntity> = withContext(Dispatchers.IO) {
        val allMemories = memoryDao.getAllMemories()
        val queryEmbedding = parseEmbedding(generateEmbedding(query))

        allMemories
            .map { memory ->
                val similarity = cosineSimilarity(queryEmbedding, parseEmbedding(memory.embedding))
                memory to similarity
            }
            .sortedByDescending { it.second }
            .take(topK)
            .map { it.first }
    }

    suspend fun retrieveByCategory(category: String): List<MemoryEntity> = withContext(Dispatchers.IO) {
        memoryDao.getMemoriesByCategory(category)
    }

    suspend fun detectIntent(query: String): QueryIntentResult = withContext(Dispatchers.IO) {
        val lowerQuery = query.lowercase()
        val hasKannada = query.any { it in '\u0C80'..'\u0CFF' }

        val healthKeywordsEn = listOf("health", "medicine", "doctor", "hospital", "disease", "sugar", "diabetes", "blood", "pressure", "thyroid", "asthma", "allergy", "pain", "surgery", "checkup", "appointment", "medication", "tablet", "dose", "bp", "heart", "cardiac", "stent", "knee", "arthritis", "joint", "eye", "cataract", "vision")
        val healthKeywordsKn = listOf("ಆರೋಗ್ಯ", "ಔಷಧ", "ವೈದ್ಯ", "ಆಸ್ಪತ್ರೆ", "ಕಾಯಿಲೆ", "ಸಕ್ಕರೆ", "ಮಧುಮೇಹ", "ರಕ್ತ", "ಒತ್ತಡ", "ಥೈರಾಯ್ಡ್", "ಅಸ್ತಮಾ", "ಅಲರ್ಜಿ", "ನೋವು", "ಶಸ್ತ್ರಚಿಕಿತ್ಸೆ", "ಪರೀಕ್ಷೆ", "ಮೆಡಿಕಲ್", "ಹಾರ್ಟ್")
        val familyKeywordsEn = listOf("family", "wife", "husband", "daughter", "son", "mother", "father", "mom", "dad", "brother", "sister", "member", "age", "blood group", "who")
        val familyKeywordsKn = listOf("ಕುಟುಂಬ", "ಹೆಂಡತಿ", "ಗಂಡ", "ಮಗಳು", "ಮಗ", "ತಾಯಿ", "ತಂದೆ", "ಅಮ್ಮ", "ಅಪ್ಪ", "ಸಹೋದರ", "ಸಹೋದರಿ", "ಸದಸ್ಯ", "ವಯಸ್ಸು", "ರಕ್ತ", "ಯಾರು")
        val appointmentKeywordsEn = listOf("appointment", "schedule", "date", "when", "next visit", "doctor visit", "scheduled")
        val appointmentKeywordsKn = listOf("ಅಪಾಯಿಂಟ್ಮೆಂಟ್", "ದಿನಾಂಕ", "ಯಾವಾಗ", "ಭೇಟಿ", "ಡಾಕ್ಟರ್ ಭೇಟಿ")
        val emergencyKeywordsEn = listOf("emergency", "ambulance", "helpline", "insurance", "emergency number", "hospital number")
        val emergencyKeywordsKn = listOf("ತುರ್ತು", "ಆ್ಯಂಬುಲೆನ್ಸ್", "ವಿಮೆ", "ಹೆಲ್ಪ್‌ಲೈನ್", "ಆಸ್ಪತ್ರೆ ಸಂಖ್ಯೆ")
        val allergyKeywordsEn = listOf("allerg", "allergic", "allergy")
        val allergyKeywordsKn = listOf("ಅಲರ್ಜಿ", "ಒಗ್ಗದ")
        val greetingKeywordsEn = listOf("hello", "hi", "hey", "good morning", "good evening", "namaste")
        val greetingKeywordsKn = listOf("ನಮಸ್ಕಾರ", "ಹಲೋ", "ನಮಸ್ತೆ", "ಹೈ")
        val thankKeywordsEn = listOf("thank", "thanks", "dhanyavadagalu")
        val thankKeywordsKn = listOf("ಧನ್ಯವಾದ", "ಥ್ಯಾಂಕ್ಸ್", "ಥ್ಯಾಂಕ್ಯೂ")
        val reminderKeywordsEn = listOf("reminder", "remind", "alarm", "set alarm", "set reminder", "medicine time", "medication time", "pill reminder")
        val reminderKeywordsKn = listOf("ನೆನಪಿಸು", "ರಿಮೈಂಡರ್", "ಎಚ್ಚರಿಕೆ", "ಔಷಧ ಸಮಯ")
        val reminderKeywordsHi = listOf("रिमाइंडर", "याद दिला", "अलार्म", "दवा का समय")

        val intent = when {
            greetingKeywordsEn.any { lowerQuery.contains(it) } || greetingKeywordsKn.any { lowerQuery.contains(it) } -> QueryIntent.GREETING
            thankKeywordsEn.any { lowerQuery.contains(it) } || thankKeywordsKn.any { lowerQuery.contains(it) } -> QueryIntent.THANK
            reminderKeywordsEn.any { keyword -> lowerQuery.contains(keyword) } || reminderKeywordsKn.any { lowerQuery.contains(it) } || reminderKeywordsHi.any { lowerQuery.contains(it) } -> QueryIntent.REMINDER
            allergyKeywordsEn.any { keyword -> lowerQuery.contains(keyword) } || allergyKeywordsKn.any { lowerQuery.contains(it) } -> QueryIntent.ALLERGY
            healthKeywordsEn.any { keyword -> lowerQuery.contains(keyword) } || healthKeywordsKn.any { lowerQuery.contains(it) } -> QueryIntent.HEALTH
            familyKeywordsEn.any { keyword -> lowerQuery.contains(keyword) } || familyKeywordsKn.any { lowerQuery.contains(it) } -> QueryIntent.FAMILY
            appointmentKeywordsEn.any { keyword -> lowerQuery.contains(keyword) } || appointmentKeywordsKn.any { lowerQuery.contains(it) } -> QueryIntent.APPOINTMENT
            emergencyKeywordsEn.any { keyword -> lowerQuery.contains(keyword) } || emergencyKeywordsKn.any { lowerQuery.contains(it) } -> QueryIntent.EMERGENCY
            else -> QueryIntent.GENERAL
        }

        QueryIntentResult(intent, hasKannada)
    }

    suspend fun buildContextForQuery(query: String): String = withContext(Dispatchers.IO) {
        val intentResult = detectIntent(query)
        val relevantMemories = retrieveRelevant(query, topK = 8)
        val healthRecords = memoryDao.getMemoriesByCategory("health")
        val familyRecords = memoryDao.getMemoriesByCategory("family")
        val appointmentRecords = memoryDao.getMemoriesByCategory("appointment")
        val emergencyRecords = memoryDao.getMemoriesByCategory("emergency")

        val contextParts = mutableListOf<String>()

        when (intentResult.intent) {
            QueryIntent.HEALTH, QueryIntent.ALLERGY -> {
                contextParts.addAll(healthRecords.map { it.content })
            }
            QueryIntent.FAMILY -> {
                contextParts.addAll(familyRecords.map { it.content })
            }
            QueryIntent.APPOINTMENT -> {
                contextParts.addAll(appointmentRecords.map { it.content })
            }
            QueryIntent.EMERGENCY -> {
                contextParts.addAll(emergencyRecords.map { it.content })
            }
            QueryIntent.GREETING, QueryIntent.THANK -> {
                contextParts.addAll(healthRecords.take(2).map { it.content })
                contextParts.addAll(familyRecords.take(2).map { it.content })
            }
            QueryIntent.REMINDER -> {
                contextParts.addAll(healthRecords.filter { it.content.contains("medication") || it.content.contains("medicine") || it.content.contains("Aspirin") || it.content.contains("Pantab") }.map { it.content })
            }
            QueryIntent.GENERAL -> {
                contextParts.addAll(relevantMemories.map { it.content })
            }
        }

        Log.d(TAG, "Built context with ${contextParts.size} records for query: $query")
        contextParts.joinToString("\n")
    }

    private fun generateEmbedding(text: String): String {
        val words = text.lowercase().split("\\s+".toRegex())
        val vector = FloatArray(128) { 0f }

        words.forEach { word ->
            val hash = word.hashCode()
            val position = kotlin.math.abs(hash) % 128
            vector[position] += 1f
        }

        val magnitude = sqrt(vector.sumOf { it.toDouble() * it.toDouble() }).toFloat()
        if (magnitude > 0) {
            for (i in vector.indices) {
                vector[i] /= magnitude
            }
        }

        return vector.joinToString(",") { it.toString() }
    }

    private fun parseEmbedding(embedding: String): FloatArray {
        return embedding.split(",").mapNotNull { it.toFloatOrNull() }.toFloatArray()
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) return 0f

        var dotProduct = 0f
        var normA = 0f
        var normB = 0f

        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }

        val denominator = sqrt(normA.toDouble()) * sqrt(normB.toDouble())
        return if (denominator == 0.0) 0f else (dotProduct / denominator).toFloat()
    }
}

enum class QueryIntent {
    HEALTH, FAMILY, APPOINTMENT, EMERGENCY, ALLERGY, GREETING, THANK, REMINDER, GENERAL
}

data class QueryIntentResult(
    val intent: QueryIntent,
    val hasKannada: Boolean
)
