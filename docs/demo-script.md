# Interview demo script

The demo is designed to take under three minutes and to work without network access.

## Hiring-manager version

**Opening (20 seconds)**

> LangCoach is a voice tutor that remembers a learner’s vocabulary and recurring mistakes
> locally. Each conversation shapes the next one, without requiring a subscription backend.

**Product loop (90 seconds)**

1. On Home, point out today’s language, level, and lesson focus.
2. Tap **Play portfolio demo**.
3. Let the transcript show the learner making a past-tense mistake and the coach correcting it.
4. Open the recap.
5. Point out the correction, extracted vocabulary, next-session focus, and estimated cost.
6. Return Home and open **Private coach memory** to show transparency and local ownership.

**Close (20 seconds)**

> The product insight is that conversation alone is easy to demo; continuity is the hard part.
> The interesting engineering work is turning a bidirectional audio stream into durable,
> inspectable learning state.

## Developer version

Lead with the same product loop, then discuss:

- Why the audio path is platform-specific while orchestration and transcript state are shared.
- How real-time events become speaker-aware UI state.
- Why post-session reflection runs outside the latency-sensitive call path.
- Why Room is the source of truth for the learner model.
- The privacy and operational trade-offs of BYO-key with no backend.
- Explicit limitations: iOS platform work, scheduler validation, and evaluation coverage.

## Questions to be ready for

- How are API keys protected?
- What happens when a WebSocket reconnects?
- How do you prevent transcript deltas from duplicating text?
- How would you evaluate correction quality?
- Why KMP instead of two native applications?
- What belongs in common code and what should stay platform-specific?
- What changes when this becomes a multi-device product?

## Demo safety

- Use the deterministic portfolio path unless the interviewer specifically requests the live API.
- Keep a short screen recording as a backup.
- Do not paste a real API key on a projected screen.
- Before the interview, run the debug build and the common tests from a clean checkout.
