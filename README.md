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

## Product decisions

LangCoach is an Android-first, local-first product prototype built to validate one idea:
a useful voice tutor should carry learning forward without turning every conversation into
cloud account data.

- **A complete Android loop:** real-time voice practice, live transcription, structured
  reflection, vocabulary review, recurring-error tracking, and next-session planning.
- **Shared where it matters:** domain rules, persistence, model protocols, and most UI
  state live in KMP modules; latency-sensitive audio remains platform-specific.
- **Local by default:** there is no account system, analytics SDK, backend, or mandatory
  cloud sync. The learner can inspect and edit the memory stored on the device.
- **Evidence over magic:** session outcomes require transcript evidence, tutor behavior is
  versioned and evaluated, and estimated model cost stays visible.

The current vocabulary scheduler intentionally uses a small, testable FSRS-inspired model.
A production release would adopt a verified implementation and calibrate it with longitudinal
learning data. The iOS target establishes the shared architecture boundary; native audio and
Keychain integration are the remaining platform work.

## Roadmap

The next milestone is **cross-platform validation**, not feature accumulation:

1. Ship the native iOS audio and Keychain implementations against the existing shared core.
2. Expand real-time protocol and reflection-schema contract coverage.
3. Evaluate saved and live device audio for interruption, noise, latency, pacing, and prosody.
4. Replace the prototype scheduler with a verified FSRS implementation and calibration data.
5. Explore opt-in encrypted sync only if multi-device testing proves it improves the
   local-first experience.

## Stack

Kotlin · Kotlin Multiplatform · Compose Multiplatform · Room · Ktor · Koin ·
OpenAI Realtime · Android foreground services

## License

[MIT](LICENSE) © 2026 Nikolai Konorev
