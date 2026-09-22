# LangCoach

**A local-first voice tutor that makes every conversation inform the next one.**

LangCoach is a Kotlin Multiplatform app for real-time language practice on Android and
iOS. It turns each conversation into vocabulary, recurring-error insights, and context
for the next lesson.

## Highlights

- Real-time voice sessions with live, speaker-aware transcripts
- Post-session reflection, corrections, vocabulary review, and next-session planning
- Local Room database with editable tutor notes and learner-controlled goals
- Shared Compose UI, domain logic, persistence, and model protocols
- Native Android audio with foreground-service support
- Native iOS audio through AVAudioEngine and secure Keychain storage
- Versioned tutor prompts and a 12-case quality evaluation suite

## Architecture

The KMP modules separate core, data, domain, LLM, voice, and UI concerns. Ktor handles
model protocols, Koin provides dependency injection, and platform-specific code owns
audio capture, playback, and secure storage.

```mermaid
flowchart LR
    UI["Compose UI"] --> Session["Session orchestrator"]
    Session --> Realtime["OpenAI Realtime"]
    Realtime --> Audio["Platform audio"]
    Realtime --> Reflection["Post-session reflection"]
    Reflection --> Memory["Local learning memory"]
    Memory --> Session
```

More detail is available in the [architecture notes](docs/design.md) and
[tutor evaluation](docs/tutor-quality.md).

## Run

Open the project in Android Studio and run `androidApp`. On iOS, open
`iosApp/iosApp.xcodeproj` in Xcode and run the `iosApp` scheme.

Choose your languages and level in **Settings**, then add an OpenAI API key for voice
lessons. The key remains in platform secure storage and is sent directly to OpenAI.

Completed voice-session transcripts remain in the local database. With a debuggable
Android build connected and authorized over USB, download the latest transcript as
JSON and Markdown with:

```bash
./scripts/export_android_transcripts.py
```

Pass `--all` to export every retained session. Exports are written to
`exports/transcripts/`, which is excluded from Git because transcripts contain private
conversation data.

```bash
./gradlew :androidApp:assembleDebug \
  :composeApp:testDebugUnitTest \
  :shared:domain:testDebugUnitTest
```

## Stack

Kotlin · Kotlin Multiplatform · Compose Multiplatform · Room · Ktor · Koin ·
OpenAI Realtime · AVAudioEngine

## License

[MIT](LICENSE) © 2026 Nikolai Konorev
