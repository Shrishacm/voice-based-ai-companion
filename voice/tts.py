import argparse
from pathlib import Path


def speak_text(text: str, output_path: str = "reply.mp3") -> str:
    try:
        import pyttsx3

        engine = pyttsx3.init()
        engine.say(text)
        engine.runAndWait()
        return text
    except Exception:
        pass

    try:
        from gtts import gTTS
        from playsound import playsound

        output_file = Path(output_path)
        gTTS(text=text, lang="en").save(str(output_file))
        playsound(str(output_file))
        return str(output_file)
    except ImportError as exc:
        raise RuntimeError("Text-to-speech requires pyttsx3 or gTTS with playsound.") from exc


def main() -> None:
    parser = argparse.ArgumentParser(description="Speak a line of text aloud.")
    parser.add_argument("text")
    parser.add_argument("--output", default="reply.mp3")
    args = parser.parse_args()
    speak_text(args.text, output_path=args.output)


if __name__ == "__main__":
    main()
