# LangCoach — Project Context

> This file is read by Claude Code at the start of every session.
> Keep it short, factual, and current. When something changes, update this file.

## What we're building

A Kotlin Multiplatform mobile app (Android + iOS) that acts as a conversational
language tutor over voice. The user brings their own OpenAI API key. The app
maintains a memory layer of vocabulary (with FSRS spaced repetition) and
grammar errors (categorical taxonomy) on-device, so the tutor adapts to the
learner across sessions.

**Differentiator** vs Langua / TalkPal / Speak: BYO-key, on-device memory,
privacy-first, real spaced repetition driving conversation topics.

## Architecture (high level)

- KMP with Compose Multiplatform UI (shared Compose, native shells).
- Google-aligned stack: AndroidX everywhere it's KMP-available.
- Single LLM provider for MVP: **OpenAI** — Realtime API for voice,
  Chat Completions for reflection / classification.
- All memory and orchestration **on-device**. No backend in MVP.
- **Koog** as the agent framework for the reflection / extraction flows only.
  Realtime voice goes direct to OpenAI WebSocket (Koog not on hot path).

## Stack

Versions are pinned in `gradle/libs.versions.toml` — **always read it before
guessing a version**.

- Kotlin 2.3.x with K2 compiler
- AGP 8.9+
- Compose Multiplatform 1.10.x with AndroidX ViewModel KMP
- AndroidX Navigation 3 (Android-only for now; KMP support TBD)
- Room 3 alpha (`androidx.room3` package, KSP-only, suspend / Flow DAOs)
- Ktor Client 3.x with WebSockets — Darwin engine on iOS, OkHttp on Android
- DataStore (KMP) for non-secret preferences
- Koin 4 for DI; `koin-compose-viewmodel` for ViewModels
- kotlinx.coroutines, kotlinx.serialization, kotlinx.datetime
- Koog for agent orchestration (post-session work only)

## Module layout

```
composeApp/        shared Compose UI (screens, theme, navigation)
shared/core        models, Result types, common utils
shared/data        Room 3, Ktor, DataStore, repos, secure storage
shared/domain      use cases, FSRS scheduler, error taxonomy
shared/llm         OpenAI client (Realtime + Chat Completions), Koog wiring
shared/voice       audio capture / playback expect/actual
androidApp/        MainActivity, app init
iosApp/            SwiftUI shell, KMP bridge
```

Five shared modules. Resist further splitting until build times demand it.

## Memory model (the heart of the product)

Four memory layers, all on-device in Room 3:

1. **Profile** — slow, structured (level, native lang, target lang, prefs)
2. **Vocabulary** — FSRS-scheduled (word, lemma, context, due_at, stability)
3. **Errors** — categorical taxonomy + per-instance log
4. **Sessions** — episodic summaries (post-session reflection writes these)

**Session start**: agent fetches due vocab, weak error categories, and recent
session summaries; composes system prompt.

**Session end**: reflection job parses transcript, extracts new vocab, marks
reviewed items, logs errors against taxonomy, writes a 200-word summary.

## OpenAI integration

- **Realtime**: `wss://api.openai.com/v1/realtime`
  PCM16, 24 kHz, mono. Native tool calling. Used for live conversation.
- **Chat Completions**: `https://api.openai.com/v1/chat/completions`
  Used for reflection (post-session) and error classification.

API key is stored via `expect class SecureStorage`:

- Android — `EncryptedSharedPreferences` backed by Keystore master key
- iOS — Keychain (`kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`)

**Never** log the key, never serialize to plain prefs, never send to analytics.
Verify with a test call (`/v1/models`) before saving on the settings screen.

## Conventions

- All shared business logic must compile in `commonMain`. Platform code lives
  only in `androidMain` / `iosMain` via `expect`/`actual`.
- ViewModels in `commonMain` using AndroidX ViewModel KMP.
- DAOs return `suspend` or `Flow<T>`. No blocking calls.
- One feature = one package, **not** one module. Modularize later.
- MVI for screens with non-trivial state (Call, Session). Sealed interfaces
  for state and intent.
- Cost tracking is a feature, not an afterthought: every OpenAI call writes
  to a `UsageLedger` row with token counts and estimated USD cents.
- No emojis, asterisk-actions, or markdown in agent's spoken output (TTS
  reads them literally — ugly).

## What NOT to do

- **No** SQLDelight, Decompose, or Voyager. We chose AndroidX equivalents
  for Google alignment.
- **No** backend. MVP is fully client-side, BYO-key.
- **No** on-device LLM. Maybe in Phase 3.
- **No** LiveKit. We go direct to OpenAI Realtime to avoid the dependency.
- **No** analytics SDKs that touch the API key, transcripts, or vocab data.

## Reference docs (fetch when API details matter)

- OpenAI Realtime: https://platform.openai.com/docs/guides/realtime
- OpenAI tool calling: https://platform.openai.com/docs/guides/function-calling
- Room 3 KMP: https://developer.android.com/kotlin/multiplatform/room
- Compose Multiplatform: https://www.jetbrains.com/help/kotlin-multiplatform-dev/
- Navigation 3: https://developer.android.com/guide/navigation/navigation-3
- Koog: https://docs.koog.ai/
- FSRS algorithm: https://github.com/open-spaced-repetition/fsrs4anki/wiki/ABC-of-FSRS

## Working agreements with Claude Code

1. **Always read `libs.versions.toml` before adding a dependency.** Don't guess
   versions from training data. If the version isn't there, ask before adding.
2. **Verify builds.** After meaningful changes, run `./gradlew build` (or the
   relevant module task). Don't hand back broken code.
3. **Stop at milestone boundaries** so the user can commit. Don't barrel
   through three milestones in one go.
4. **No tests yet** — MVP skeleton first. We'll add tests once interfaces
   stabilize (probably week 3).
5. **Ask before adding new modules or libraries.** Stack drift is the enemy.
6. **iOS bridge friction is expected.** Run
   `./gradlew :shared:llm:assembleXCFramework` (and similar) early to catch
   interop issues. Sealed classes and Flow are the usual suspects.

## Current milestone

**Week 1 — Skeleton.** Project compiles on both targets. `composeApp` shows
a placeholder Home screen via Navigation 3. `shared/data` has Room 3 wired
with one entity (`VocabItem`). `SecureStorage` has working `expect`/`actual`
on both platforms. No business logic yet.

## Design history

See `docs/design.md` for the reasoning behind architecture choices —
why OpenAI over multi-provider, why AndroidX over JetBrains alternatives,
why on-device memory, why no backend. Read it if you need to understand
**why** we picked X over Y before suggesting changing it.
