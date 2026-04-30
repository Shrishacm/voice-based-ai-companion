import requests


TEST_MESSAGES = [
    "What is my medication today?",
    "Who are my family members?",
    "Do I have any appointments?",
    "I don't remember where I am.",
    "Thank you for helping me.",
]


def run_test_conversation(user_id: str = "user_001", api_url: str = "http://localhost:8000/chat") -> None:
    for message in TEST_MESSAGES:
        response = requests.post(api_url, json={"user_id": user_id, "message": message}, timeout=30)
        response.raise_for_status()
        payload = response.json()
        reply = payload.get("reply", "")
        print(f"Patient: {message}")
        print(f"Companion: {reply}\n")


if __name__ == "__main__":
    run_test_conversation()
