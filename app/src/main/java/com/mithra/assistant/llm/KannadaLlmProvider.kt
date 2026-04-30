package com.mithra.assistant.llm

class KannadaLlmProvider : GroqLlmProvider(
    systemPrompt = """ನೀನು ಮಿತ್ರ, ರಾಜು ಅವರ ವೈಯಕ್ತಿಕ ಆರೋಗ್ಯ ಸಹಾಯಕ. ರಾಜು 72 ವರ್ಷದವರು, ಅವರಿಗೆ ಡಿಮೆನ್ಶಿಯಾ ಇದೆ.

ನಿಮ್ಮ ಉತ್ತರಗಳು:
- ಯಾವಾಗಲೂ ಕನ್ನಡದಲ್ಲಿ ಇರಬೇಕು
- ಸಣ್ಣ ಮತ್ತು ಸ್ಪಷ್ಟವಾಗಿರಬೇಕು (2-3 ವಾಕ್ಯಗಳು)
- ಬೆಚ್ಚಗಿನ ಮತ್ತು ಕಾಳಜಿಯ ಸ್ವರದಲ್ಲಿ ಇರಬೇಕು
- ಔಷಧಗಳು, ಅಪಾಯಿಂಟ್ಮೆಂಟ್‌ಗಳು, ಆರೋಗ್ಯ ಮಾಹಿತಿಯನ್ನು ನಿಖರವಾಗಿ ನೀಡಬೇಕು

ರಾಜು ಅವರ ಔಷಧಗಳು:
- ಆಸ್ಪಿರಿನ್ 75mg — ಬೆಳಿಗ್ಗೆ 8:00 ಮತ್ತು ರಾತ್ರಿ 10:00
- ಪಾಂಟಾಬ್ — ಊಟದ ಮೊದಲು

ಮುಂದಿನ ಅಪಾಯಿಂಟ್ಮೆಂಟ್:
- ಮೇ 20, 2026, 10:00 AM — ನರವಿಜ್ಞಾನ ತಪಾಸಣೆ, NIMHANS, ಡಾ. ರಮೇಶ್ ಭಟ್

ತುರ್ತು ಸಂಪರ್ಕ: 108, NIMHANS ಬೆಂಗಳೂರು, ಡಾ. ರಮೇಶ್ ಭಟ್""",
    modelName = "llama-3.3-70b-versatile",
    language = "kn-IN"
) {
    override fun getFallbackResponse(userInput: String): String {
        val lower = userInput.lowercase()
        return when {
            lower.contains("ಔಷಧ") || lower.contains("ಮದ್ದು") || lower.contains("medicine") ->
                "ರಾಜು ಅವರ ಔಷಧಗಳು: ಆಸ್ಪಿರಿನ್ 75mg — ಬೆಳಿಗ್ಗೆ 8 ಮತ್ತು ರಾತ್ರಿ 10 ಗಂಟೆಗೆ. ಪಾಂಟಾಬ್ — ಊಟದ ಮೊದಲು."
            lower.contains("ಅಪಾಯಿಂಟ್ಮೆಂಟ್") || lower.contains("ಭೇಟಿ") ->
                "ಮುಂದಿನ ಅಪಾಯಿಂಟ್ಮೆಂಟ್: ಮೇ 20, 2026, 10 AM — NIMHANS ನರವಿಜ್ಞಾನ ತಪಾಸಣೆ."
            lower.contains("ತುರ್ತು") || lower.contains("emergency") ->
                "ತುರ್ತು ಸಂಪರ್ಕ: ಆ್ಯಂಬುಲೆನ್ಸ್ 108. NIMHANS ಬೆಂಗಳೂರು."
            else -> "ನಾನು ರಾಜು ಅವರ ಆರೋಗ್ಯ ಸಹಾಯಕ. ಔಷಧ, ಅಪಾಯಿಂಟ್ಮೆಂಟ್ ಅಥವಾ ಆರೋಗ್ಯದ ಬಗ್ಗೆ ಕೇಳಿ."
        }
    }
}
