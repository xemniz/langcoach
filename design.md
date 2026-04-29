# LangCoach — Design History

This doc captures *why* we made the architecture choices in `CLAUDE.md`.
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
- **Transparency** — show the user exactly what's tracked, what each call
  costs, what the system prompt looks like.
- **Audience** — devs, polyglots, privacy-conscious learners. Smaller
  market, but underserved.

We are **not** trying to beat Langua on conversation polish. We are
making the version a developer would want to use.

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

Trade-off: no telemetry, no remote configuration, no server-side eval
of agent quality. We accept this for now.

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

## Why AndroidX everywhere instead of JetBrains alternatives

JetBrains stack option (rejected): Decompose for navigation, SQLDelight
for DB, Voyager for screens.

AndroidX stack (chosen): Navigation 3, Room 3, AndroidX ViewModel —
all KMP-stable as of late 2025 / early 2026.

Reasons:

- **Google alignment.** The user is a senior Android dev; AndroidX is
  what teammates and recruiters know.
- **Future Jetpack libs** land in AndroidX first, get KMP support
  added soon after. We want to be on the train, not catching up.
- **Same code Android engineers write at Google.** Just in `commonMain`.

What we lose:

- **Decompose** has more rigorous lifecycle handling for complex screens.
  Nav 3 is fine for our 5–7 screens; reassess if we hit a wall.
- **SQLDelight** has been KMP-stable longer than Room 3. Room 3 has been
  stable long enough by now.

If AndroidX KMP support degrades or we hit a real blocker, we revisit.
Until then, go with the official Google story.

## Why Koog (and how much of it)

Koog is JetBrains' Kotlin agent framework. It's KMP-native, integrates
with major LLM providers, and provides clean abstractions for tools and
agent loops.

We use Koog for **post-session work only**:
- Reflection (transcript → vocab + errors + summary)
- Error classification against the taxonomy
- Vocab extraction with lemmatization

We do **not** use Koog on the realtime voice hot path, because OpenAI
Realtime has its own server-side state machine and tool dispatch. Layering
Koog on top adds complexity without benefit.

## Why FSRS (not SM-2, not custom)

FSRS is the modern spaced repetition algorithm. Anki migrated to it.
There's an open-source spec, well-documented math, KMP-portable in pure
Kotlin (~150 lines).

SM-2 is older and less accurate. A custom scheduler is reinventing the
wheel.

The agent calls `mark_recalled(item_id, grade)` after a vocab use; the
FSRS scheduler computes the new interval; that's it.

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
4. **Realtime cost ceiling.** OpenAI Realtime is not cheap. Build the
   UsageLedger early so the user sees what they spend.
5. **Pronunciation feedback.** Out of scope for MVP. If we add it, it's
   a separate model (Azure Speech assessment, or Whisper + phoneme
   alignment). LLMs alone can't do this from a transcript.
