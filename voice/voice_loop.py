import argparse
import os
import time

import requests

try:
    from .stt import record_audio, transcribe_audio
    from .tts import speak_text
except ImportError:
    from stt import record_audio, transcribe_audio
    from tts import speak_text


DEFAULT_API_URL = os.getenv("VOICE_COMPANION_API_URL", "http://localhost:8000/chat")
DEFAULT_WAKE_WORDS = ("hello", "hey mitra")


def send_chat_message(user_id: str, message: str, api_url: str = DEFAULT_API_URL) -> str:
    response = requests.post(
        api_url,
        json={"user_id": user_id, "message": message},
        timeout=30,
    )
    response.raise_for_status()
    payload = response.json()
    return payload.get("reply", "I could not generate a reply just now.")


def should_process_message(message: str, require_wake_word: bool) -> bool:
    if not require_wake_word:
        return True
    lowered = message.lower()
    return any(wake_word in lowered for wake_word in DEFAULT_WAKE_WORDS)


def run_voice_loop(
    user_id: str = "user_001",
    api_url: str = DEFAULT_API_URL,
    duration: int = 5,
    model_name: str = "base",
    require_wake_word: bool = False,
) -> None:
    silent_turns = 0

    print("Voice companion loop started. Press Ctrl+C to stop.")
    while True:
        audio_path = record_audio(output_path="temp.wav", duration=duration)
        message = transcribe_audio(audio_path, model_name=model_name)
        print(f"Patient: {message}")

        if len(message.split()) <= 1:
            silent_turns += 1
            if silent_turns < 3:
                print("Short or empty input detected. Listening again.")
                continue
            message = "[patient_silent]"
            silent_turns = 0
        else:
            silent_turns = 0

        if not should_process_message(message, require_wake_word):
            print("Wake word not detected. Skipping this turn.")
            continue

        reply = send_chat_message(user_id=user_id, message=message, api_url=api_url)
        print(f"Companion: {reply}")
        speak_text(reply)
        time.sleep(0.5)


def main() -> None:
    parser = argparse.ArgumentParser(description="Run the end-to-end voice companion loop.")
    parser.add_argument("--user-id", default="user_001")
    parser.add_argument("--api-url", default=DEFAULT_API_URL)
    parser.add_argument("--duration", type=int, default=5)
    parser.add_argument("--model", default="base")
    parser.add_argument("--require-wake-word", action="store_true")
    args = parser.parse_args()

    try:
        run_voice_loop(
            user_id=args.user_id,
            api_url=args.api_url,
            duration=args.duration,
            model_name=args.model,
            require_wake_word=args.require_wake_word,
        )
    except KeyboardInterrupt:
        print("Voice companion loop stopped.")


if __name__ == "__main__":
    main()
