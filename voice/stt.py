import argparse
import wave
from pathlib import Path


MODEL_CACHE = {}


def record_audio(output_path: str = "temp.wav", duration: int = 5, sample_rate: int = 16000) -> str:
    output_file = Path(output_path)

    try:
        import pyaudio  # type: ignore

        audio = pyaudio.PyAudio()
        stream = audio.open(
            format=pyaudio.paInt16,
            channels=1,
            rate=sample_rate,
            input=True,
            frames_per_buffer=1024,
        )

        frames = []
        for _ in range(0, int(sample_rate / 1024 * duration)):
            frames.append(stream.read(1024, exception_on_overflow=False))

        stream.stop_stream()
        stream.close()
        sample_width = audio.get_sample_size(pyaudio.paInt16)
        audio.terminate()

        with wave.open(str(output_file), "wb") as wav_file:
            wav_file.setnchannels(1)
            wav_file.setsampwidth(sample_width)
            wav_file.setframerate(sample_rate)
            wav_file.writeframes(b"".join(frames))

        return str(output_file)
    except ImportError:
        pass

    try:
        import numpy as np
        import sounddevice as sd

        recording = sd.rec(int(duration * sample_rate), samplerate=sample_rate, channels=1, dtype="int16")
        sd.wait()

        with wave.open(str(output_file), "wb") as wav_file:
            wav_file.setnchannels(1)
            wav_file.setsampwidth(2)
            wav_file.setframerate(sample_rate)
            wav_file.writeframes(np.asarray(recording).tobytes())

        return str(output_file)
    except ImportError as exc:
        raise RuntimeError(
            "Microphone recording requires either pyaudio or sounddevice to be installed."
        ) from exc


def _get_model(model_name: str = "base"):
    if model_name not in MODEL_CACHE:
        try:
            import whisper
        except ImportError as exc:
            raise RuntimeError("Speech transcription requires the openai-whisper package.") from exc
        MODEL_CACHE[model_name] = whisper.load_model(model_name)
    return MODEL_CACHE[model_name]


def transcribe_audio(audio_path: str, model_name: str = "base") -> str:
    model = _get_model(model_name)
    result = model.transcribe(audio_path)
    return result.get("text", "").strip()


def transcribe_from_microphone(duration: int = 5, output_path: str = "temp.wav", model_name: str = "base") -> str:
    audio_path = record_audio(output_path=output_path, duration=duration)
    return transcribe_audio(audio_path, model_name=model_name)


def main() -> None:
    parser = argparse.ArgumentParser(description="Record microphone input and transcribe it with Whisper.")
    parser.add_argument("--duration", type=int, default=5)
    parser.add_argument("--output", default="temp.wav")
    parser.add_argument("--model", default="base")
    args = parser.parse_args()

    print("Recording...")
    transcript = transcribe_from_microphone(duration=args.duration, output_path=args.output, model_name=args.model)
    print(transcript)


if __name__ == "__main__":
    main()
