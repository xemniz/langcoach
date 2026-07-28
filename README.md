# LangCoach

**A privacy-conscious voice tutor that makes every conversation inform the next one.**

LangCoach is an Android-first Kotlin Multiplatform project for real-time language
practice. It listens, speaks, and—after a session—turns the conversation into useful
memory: vocabulary to revisit, recurring errors, and context for the next lesson.

The portfolio demo is deterministic and requires no API key. The real voice path connects
directly to OpenAI Realtime using a key stored on the device.

## Product walkthrough

<p align="center">
  <img src="docs/media/home.png" width="30%" alt="LangCoach home screen with today's practice and learning-loop metrics">
  <img src="docs/media/demo-call.png" width="30%" alt="Deterministic Spanish coaching conversation with a live transcript">
  <img src="docs/media/session-recap.png" width="30%" alt="Session recap showing feedback, a correction, saved vocabulary, and the next focus">
</p>

The offline demo shows the complete loop: a focused conversation, an in-context
correction, and a recap that turns the session into vocabulary and a concrete next focus.

## Why this project exists

Most conversational tutors forget the learner between calls or hide their learning model
behind a subscription backend. LangCoach explores a different trade-off:

- **Continuity:** recent sessions, due vocabulary, and weak grammar areas shape the next call.
- **Privacy:** the learning profile stays in the local Room database.
- **Transparency:** the learner can inspect and edit coach memory and see estimated API cost.
- **Direct ownership:** BYO-key traffic goes from the device to the model provider.

## The learning loop

1. Select one structured session objective from due vocabulary, recurring errors, and recent outcomes.
2. Stream microphone audio and model audio over a real-time WebSocket session.
3. Maintain a live, speaker-aware transcript.
4. Reflect on the ordered conversation to classify objective evidence, corrections, and vocabulary.
5. Validate the evidence, schedule future review, and use the result to shape the next conversation.

```mermaid
flowchart LR
    UI["Compose UI"] --> Session["Session orchestrator"]
    Session --> Realtime["OpenAI Realtime"]
    Realtime --> Audio["Platform audio"]
    Realtime --> Reflection["Post-session reflection"]
    Reflection --> Memory["Local learning memory"]
    Memory --> Room["Room database"]
    Memory --> Session
```

## Technical highlights

- Kotlin Multiplatform modules separate core, data, domain, LLM, voice, and UI concerns.
- Compose Multiplatform shares the product UI and presentation logic.
- Room stores session summaries, vocabulary, error categories, usage, and editable coach memory.
- Ktor handles chat and real-time model protocols.
- Koin wires shared and platform-specific implementations.
- Android audio uses low-level capture/playback for PCM streaming.
- A foreground service keeps an active voice session resilient.
- Focused common tests cover transcript reduction and vocabulary scheduling behavior.
- A persisted session plan and evidence-backed objective outcome close the learning loop.
- A versioned tutor prompt and live 12-case quality suite make coaching changes measurable.

See [docs/design.md](docs/design.md) for the reasoning behind the architecture and
[docs/demo-script.md](docs/demo-script.md) for the interview walkthrough. The teaching
strategy and evidence are documented in [docs/tutor-quality.md](docs/tutor-quality.md).

## Run the portfolio demo

1. Open the project in Android Studio.
2. Run the `androidApp` configuration.
3. Tap **Play portfolio demo**.
4. Let the scripted conversation complete, then open the session recap.

No network connection or API key is required for this path.

## Run a real voice session

Open **Settings**, add an OpenAI API key, choose the native and target languages, then
start a conversation. The key is stored using Android secure storage and is not committed
to the repository.

```bash
./gradlew :androidApp:assembleDebug \
  :composeApp:testDebugUnitTest \
  :shared:domain:testDebugUnitTest
```

Run the real-model tutor quality suite:

```bash
python3 evals/tutor/run_eval.py \
  --output evals/tutor/results/$(date -u +%Y%m%dT%H%M%SZ).json
```

The previous `tutor-v1.0.0` baseline scored **4.83/5 across 12/12 passing cases**
with zero hard violations. The new session-plan prompt is versioned separately and must
establish a fresh reviewed baseline. See
[evals/tutor/README.md](evals/tutor/README.md) for the rubric, scenarios, and limits of
transcript-level evaluation.

## Current scope

This is an **Android-first portfolio MVP**, not a released consumer product.

- Android voice sessions are implemented.
- Shared KMP architecture and iOS targets compile incrementally.
- iOS audio capture and Keychain storage remain explicit follow-up work.
- The vocabulary scheduler is a small prototype inspired by FSRS concepts; production
  adoption would use a verified implementation and calibration data.
- There is intentionally no backend, account system, telemetry, or cloud sync.

These boundaries are deliberate and documented rather than hidden behind placeholder claims.

## What I would build next

1. Complete the native iOS audio and secure-storage implementations.
2. Add contract tests for real-time wire events and reflection schemas.
3. Add saved-audio and physical-device tutor evals for prosody, noise, and interruption.
4. Replace prototype scheduling with a verified FSRS library.
5. Add opt-in encrypted sync only after validating the local-first experience.

## Stack

Kotlin · Kotlin Multiplatform · Compose Multiplatform · Room · Ktor · Koin ·
OpenAI Realtime · Android foreground services

## License

[MIT](LICENSE) © 2026 Nikolai Konorev
