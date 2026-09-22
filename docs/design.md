# LangCoach — Design History

This doc captures the reasoning behind the app's architecture choices.
If you're tempted to suggest changing one of these, read the corresponding
section first.

## Why a voice tutor with on-device memory

The market is full of conversational AI language apps — Langua, TalkPal,
Speak, Praktika, YourTeacher.AI, Univext. Of these, **Langua is the
strongest** competitor: tracks vocab, brings difficult words back into
future conversations, has "call mode," ~$10–15/month.

Most others fail in one of two ways:

1. **No real spaced repetition.** They "track vocabulary" but surface it
   sometimes, not on a schedule.
2. **Weak grammar tracking.** Beginner errors get caught; intermediate /
   advanced (B2+) subtle mistakes don't.

**Our angle**: BYO-key + on-device memory. This wins on:

- **Privacy** — your mistake history never leaves your phone.
- **Cost** — user pays inference directly, ~$5/month real usage vs $15/mo
  subscription.
- **Transparency** — learners can review and edit the local notes and goals
  used to plan future lessons.
- **Audience** — independent and privacy-conscious language learners.

The learner-facing app stays focused on lessons and progress. Provider,
protocol, token, and processing details remain internal.

## Why OpenAI first (not multi-provider, not Anthropic)

OpenAI Realtime API is currently the best end-to-end voice experience:

- Single WebSocket, audio in / audio out.
- Native tool calling.
- ~500ms turn latency (vs 1.2–2s cascaded STT/LLM/TTS).
- Stable, well-documented.

Anthropic doesn't ship a comparable realtime audio API as of writing.
We'd have to cascade Whisper + Claude + ElevenLabs, which is more code
and worse latency. Once Anthropic ships realtime audio, we add it.

Gemini Live is comparable to OpenAI Realtime. Add it after OpenAI is
shipped and stable.

For reflection (post-session, no realtime constraint), any provider
works — but we keep one provider for MVP to reduce surface area.

## Why no backend in MVP

The classic SaaS model is "we hold the keys, you pay subscription." With
BYO-key, the user already paid OpenAI. Inserting a backend would mean:

- We'd need to proxy traffic (latency cost).
- We'd need privacy policy, GDPR, data retention rules.
- We'd need infra cost we can't recover.
- Users would correctly ask "why is my key going through your server?"

So: zero backend in MVP. Sync, multi-device, social features all need
backend later — but not for proving the product.

Trade-off: no telemetry, no remote configuration, and no server-side tutor
quality evaluation. We accept this for now.

## Why on-device memory specifically

Two reasons:

1. **Privacy story is real.** Vocab struggles, error patterns, learning
   trajectory — these are personal. A user's mistake log is more
   sensitive than their search history.
2. **It works fine.** SQLite + FSRS math + small classification calls
   are cheap. We don't need a server-side ML pipeline to pick due cards.

The only things that *need* to be remote are the LLM calls themselves,
and those go directly to OpenAI.

If we ever add cloud sync, it must be opt-in and end-to-end encrypted.

## Why AndroidX storage and lifecycle with simple navigation

JetBrains stack option (rejected): Decompose for navigation, SQLDelight
for DB, Voyager for screens.

The shared app uses Room and AndroidX ViewModel. Navigation is a small typed
back stack owned by the Compose UI; the current screen count does not justify
another navigation dependency.

Reasons:

- **Future Jetpack libs** land in AndroidX first, get KMP support
  added soon after. We want to be on the train, not catching up.
- **Low navigation complexity.** The app has a shallow hierarchy and no
  deep links, nested graphs, or shared-element transitions.

What we lose:

- **Decompose and Navigation 3** offer more rigorous lifecycle and graph
  handling. Reassess when the route hierarchy becomes materially deeper.
- **SQLDelight** has been KMP-stable longer than Room 3. Room 3 has been
  stable long enough by now.

If AndroidX KMP support degrades or the manual back stack becomes hard to
reason about, revisit the choice.

## Why FSRS (not SM-2, not custom)

FSRS is the modern spaced repetition algorithm. Anki migrated to it.
There's an open-source spec, well-documented math, KMP-portable in pure
Kotlin (~150 lines).

SM-2 is older and less accurate. A custom scheduler is reinventing the
wheel.

Post-lesson reflection records retrieval evidence and the FSRS scheduler
computes the next review interval.

## Why no on-device LLM (yet)

3–7B models on flagship phones run at 15–30 tok/s. Quality is genuinely
not enough for:

- B1+ grammar nuance
- Reliable tool calling
- Categorizing errors against ~300 taxonomy buckets

It looks great in demos and falls apart in real use. Maybe in 18 months
when 14B models run on phones with acceptable latency.

If we add on-device later, it'll be for the *small* tasks (lemmatization,
extraction), not the conversation itself.

## Why no LiveKit

LiveKit is excellent for production WebRTC voice AI. Used by Anthropic
and OpenAI in their own demos.

But: with OpenAI Realtime, we go *directly* from the device to OpenAI
over WebSocket. No SFU needed for a 1:1 user-to-agent call. LiveKit
adds a dependency, an SDK on each platform, and an infra layer we don't
need.

Add LiveKit later if we ever want:
- Multi-user practice sessions
- Phone integration
- Recording / replay

For 1:1 BYO-key voice tutoring, it's overkill.

## Open questions / known unknowns

1. **iOS bridge ergonomics for Realtime audio.** PCM streaming through
   `AVAudioEngine` works but has gotchas. Allocate ~2 days for this.
2. **FSRS Kotlin implementation.** Several community ports exist; verify
   one before depending on it, or vendor the algorithm directly.
3. **Error taxonomy size.** 300 categories per language is a guess.
   Might be 100, might be 800. Build for one language (Spanish) first,
   measure, then generalize.
4. **Realtime cost ceiling.** OpenAI Realtime is not cheap. Keep the
   internal UsageLedger accurate so future product decisions can use real usage data.
5. **Pronunciation feedback.** Out of scope for MVP. If we add it, it's
   a separate model (Azure Speech assessment, or Whisper + phoneme
   alignment). LLMs alone can't do this from a transcript.
