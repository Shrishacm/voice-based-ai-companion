import os
from datetime import datetime
from pathlib import Path

import requests

try:
    import google.generativeai as genai
except ImportError:
    genai = None

try:
    from dotenv import load_dotenv
    env_path = Path(__file__).parent / ".env"
    load_dotenv(env_path)
except:
    pass


class LLMHandler:
    def __init__(self):
        self.provider = os.getenv("LLM_PROVIDER", "gemini")
        self.api_url = os.getenv("LLM_API_URL", "")
        self.api_key = os.getenv("LLM_API_KEY", "")
        self.model_name = os.getenv("LLM_MODEL", "gemini-1.5-flash")
        self.auth_header = os.getenv("LLM_AUTH_HEADER", "Authorization")
        self.auth_scheme = os.getenv("LLM_AUTH_SCHEME", "Bearer")
        
        self.gemini_api_key = os.getenv("GEMINI_API_KEY", "").strip()
        self._gemini_model = None
        if self.gemini_api_key and genai is not None:
            try:
                genai.configure(api_key=self.gemini_api_key)
                self._gemini_model = genai.GenerativeModel("gemini-2.5-flash")
            except Exception as e:
                print(f"Gemini init error: {e}")
                self._gemini_model = None

    def build_system_prompt(self, patient_memory: dict) -> str:
        patient_name = patient_memory.get("patient_name", "Patient")
        age = patient_memory.get("age", 0)
        medications = patient_memory.get("medications", [])
        family_members = patient_memory.get("family_members", [])
        appointments = patient_memory.get("appointments", [])

        system_prompt = f"""You are a warm AI companion for an elderly patient with early dementia.
Your role is to provide calm conversation, gentle reminders, and emotional reassurance.

Current patient information:
- Name: {patient_name}
- Age: {age}

Medications (always be gentle about reminding):
{chr(10).join(f"- {med}" for med in medications)}

Important family members:
{chr(10).join(f"- {fm['name']} ({fm['relationship']})" for fm in family_members)}

Upcoming appointments:
{chr(10).join(f"- {ap['date']}: {ap['description']}" for ap in appointments)}

Guidelines:
- Speak warmly and simply
- Use short sentences
- Be patient and understanding
- Gently remind about medications if relevant
- Mention family members positively
- Do not argue or correct confused memories - gently redirect if needed
- Keep answers concise and voice-friendly
- Always be kind and supportive"""

        return system_prompt

    def chat(self, patient_memory: dict, user_message: str, conversation_history: list = None) -> str:
        system_prompt = self.build_system_prompt(patient_memory)
        history = conversation_history or []

        if self._gemini_model is not None:
            try:
                return self._chat_gemini(system_prompt, user_message, history)
            except Exception as e:
                print(f"Gemini error: {e}")

        if self.provider == "openai-compatible" and self.api_url and self.api_key:
            try:
                return self._chat_openai_compatible(system_prompt, user_message, history)
            except Exception:
                pass

        return self._mock_reply(patient_memory, user_message)

    def _chat_gemini(self, system_prompt: str, user_message: str, conversation_history: list) -> str:
        history_text = ""
        for turn in conversation_history[-8:]:
            history_text += f"{turn['role']}: {turn['content']}\n"

        full_prompt = f"""{system_prompt}

Conversation History:
{history_text}

User: {user_message}
Assistant:"""

        response = self._gemini_model.generate_content(full_prompt)
        return response.text.strip()

    def _chat_openai_compatible(self, system_prompt: str, user_message: str, conversation_history: list) -> str:
        headers = {"Content-Type": "application/json"}
        if self.api_key:
            headers[self.auth_header] = f"{self.auth_scheme} {self.api_key}".strip()

        messages = [{"role": "system", "content": system_prompt}]
        for message in conversation_history:
            messages.append({
                "role": message["role"],
                "content": message["content"],
            })
        messages.append({"role": "user", "content": user_message})

        payload = {
            "model": self.model_name,
            "messages": messages,
            "temperature": 0.7,
            "max_tokens": 300,
        }

        response = requests.post(self.api_url, headers=headers, json=payload, timeout=30)
        response.raise_for_status()
        data = response.json()
        content = data["choices"][0]["message"]["content"]
        if isinstance(content, list):
            return " ".join(part.get("text", "") for part in content if isinstance(part, dict)).strip()
        return str(content).strip()

    def _mock_reply(self, patient_memory: dict, user_message: str) -> str:
        patient_name = patient_memory.get("patient_name", "there")
        medications = patient_memory.get("medications", [])
        family_members = patient_memory.get("family_members", [])
        appointments = patient_memory.get("appointments", [])
        lowered = user_message.lower().strip()

        if lowered == "[patient_silent]":
            return f"I'm here with you, {patient_name}. If you need help, say hello and I will respond."

        if any(keyword in lowered for keyword in ["medicine", "medication", "tablet", "pill"]):
            if medications:
                return f"{patient_name}, your medications today are {', '.join(medications)}. Would you like me to repeat them slowly?"
            return f"{patient_name}, I do not have any medications saved yet."

        if any(keyword in lowered for keyword in ["appointment", "doctor", "visit", "meeting"]):
            if appointments:
                upcoming = sorted(appointments, key=lambda item: item.get("date", ""))[0]
                return (
                    f"{patient_name}, your next appointment is {upcoming.get('description', 'an appointment')} "
                    f"on {upcoming.get('date', 'a scheduled date')}."
                )
            return f"{patient_name}, I do not see any upcoming appointments saved right now."

        if any(keyword in lowered for keyword in ["family", "daughter", "son", "priya", "arjun", "who is coming"]):
            if family_members:
                names = ", ".join(f"{member['name']} your {member['relationship']}" for member in family_members)
                return f"{patient_name}, your family includes {names}. They care about you very much."
            return f"{patient_name}, I do not have any family names saved yet."

        if any(keyword in lowered for keyword in ["where am i", "i don't remember", "confused", "lost"]):
            return (
                f"You are safe, {patient_name}. Take a slow breath with me. "
                "I can help you remember your family, medicines, or today's plans."
            )

        if appointments:
            next_appointment = sorted(appointments, key=lambda item: item.get("date", ""))[0]
            return (
                f"Hello {patient_name}. I'm here with you. "
                f"Your next appointment is {next_appointment.get('description', 'planned soon')} on {next_appointment.get('date', 'a scheduled date')}."
            )

        today = datetime.utcnow().strftime("%A")
        return f"Hello {patient_name}. It is {today}. I am here to keep you company and help with your routine."


def get_llm_handler() -> LLMHandler:
    return LLMHandler()