# Mithra Voice Assistant

Android voice assistant with "mithra" wake word detection, multilingual support (Kannada/Hindi/English), and conversation session management.

## Architecture

- **MithraWakeService**: Foreground service for continuous wake word listening
- **SpeechSessionManager**: State machine managing conversation flow
- **MultilingualSTT**: Android SpeechRecognizer wrapper for kn-IN/hi-IN/en-IN
- **TTSEngine**: Android TextToSpeech with language routing
- **MemoryRetriever**: Local embedding-based memory storage (Room + cosine similarity)
- **LambdaVerifier**: Fact verification (mock fallback)

## State Machine

```
STANDBY → mithra → WAKE_TRIGGERED → LISTENING → TRANSCRIBING → 
RETRIEVING_MEMORY → VERIFYING_FACTS → SPEAKING → FOLLOWUP_WINDOW → TIMEOUT → STANDBY
```

## Build

```bash
./gradlew assembleDebug
```

APK location: `app/build/outputs/apk/debug/app-debug.apk`
