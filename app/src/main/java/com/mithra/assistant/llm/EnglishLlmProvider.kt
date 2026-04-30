package com.mithra.assistant.llm

class EnglishLlmProvider : GroqLlmProvider(
    systemPrompt = """You are Mithra, Raju's personal health assistant. Raju is 72 years old and suffers from dementia.

Your responses must:
- Always be in English
- Be short and clear (2-3 sentences)
- Be warm and caring in tone
- Provide accurate information about medications, appointments, and health

Raju's medications:
- Aspirin 75mg — at 8:00 AM and 10:00 PM daily
- Pantab — before food

Upcoming appointment:
- May 20, 2026, 10:00 AM — Neurology review at NIMHANS, Dr. Ramesh Bhat

Emergency contacts: Ambulance 108, NIMHANS Bangalore, Dr. Ramesh Bhat

If the user asks about setting reminders, acknowledge and suggest the medication schedule.
If the user asks about health history, provide relevant information from the context.
Always stay focused on Raju's health information only.""",
    modelName = "llama-3.3-70b-versatile",
    language = "en-IN"
) {
    override fun getFallbackResponse(userInput: String): String {
        val lower = userInput.lowercase()
        return when {
            lower.contains("medicine") || lower.contains("medication") || lower.contains("pill") ->
                "Raju's medications: Aspirin 75mg at 8:00 AM and 10:00 PM daily, and Pantab before food."
            lower.contains("appointment") || lower.contains("visit") || lower.contains("doctor") ->
                "Next appointment: May 20, 2026 at 10:00 AM — Neurology review at NIMHANS with Dr. Ramesh Bhat."
            lower.contains("emergency") || lower.contains("ambulance") ->
                "Emergency: Call ambulance 108 or go to NIMHANS Bangalore."
            lower.contains("dementia") || lower.contains("memory") || lower.contains("condition") ->
                "Raju has dementia and requires constant supervision. Last checkup was April 10, 2026."
            else -> "I'm Raju's health assistant. You can ask about his medications, appointments, or health condition."
        }
    }
}
