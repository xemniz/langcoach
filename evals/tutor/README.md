# Tutor quality evals

This folder turns tutor behavior into a repeatable product test instead of a subjective
prompt review.

## Version contract

- Production prompt: `TutorPrompt.VERSION` in
  `shared/domain/src/commonMain/kotlin/com/xemniz/langcoach/domain/session/TutorPrompt.kt`
- Evaluation set: `tutor-eval-v1.2.0.json`
- Previous reviewed live baseline: `baselines/tutor-v1.0.0.json`

The runner refuses to execute when either the dataset's expected prompt version or its
SHA-256 differs from the production prompt. This catches prompt edits that forgot a version
bump. Bump the prompt version when behavior changes intentionally, copy the eval set when
its cases or grading contract change, and keep the old baseline for comparison.

## What it measures

The initial gold set contains 12 scripted Spanish A2-to-B2 episodes:

- a prepared opening that uses memory without sounding invasive;
- one high-value correction and a multiple-error restraint stress test;
- a correct story that must not trigger an invented correction;
- a recurring error that must not become a repetitive drill;
- natural due-vocabulary retrieval;
- an emotional personal story where conversation outranks exercise;
- a short practical role-play;
- a later opinion exercise that must vary the recent role-play format;
- comprehension repair through simpler target-language speech;
- multi-turn resistance to automatic praise;
- a negative memory case where irrelevant facts must stay unused.

Eight cases form the regression set and four are holdouts. The dataset deliberately
contains both positive and negative examples so optimizing for "teaching" does not turn
the tutor into a constant corrector.

## How it works

1. The runner reads the canonical prompt directly from the Kotlin production source.
2. It replays fixed, multi-turn learner messages against the real Realtime model in text
   mode, preserving the model and prompt behavior while removing microphone variance.
3. Deterministic checks catch repeated questions, multiple questions in a turn, and banned
   generic openings.
4. A structured LLM grader scores engagement, teaching value, correction restraint,
   memory use, variety, level fit, and continuity.
5. The full transcript, grader evidence, model IDs, versions, and timestamp are saved.

Run the complete suite:

```bash
python3 evals/tutor/run_eval.py \
  --output evals/tutor/results/$(date -u +%Y%m%dT%H%M%SZ).json
```

Run a focused regression while iterating:

```bash
python3 evals/tutor/run_eval.py --case error_flood_choose_one
```

The runner reads `OPENAI_API_KEY` from the environment or the existing uncommitted
`local.properties`. It never writes the key to results.

## Pass gate

- Every case score is at least 3.5 out of 5.
- The suite average is at least 4.0.
- No hard case-specific violation occurs.
- Every assistant turn asks at most one answerable question.
- No identical question or forbidden generic opening is emitted.

The previous `tutor-v1.0.0` live baseline scored **4.83/5**, passed **12/12** cases,
and produced **zero hard violations** with `gpt-realtime-2.1`. The new
`tutor-v1.1.0` session-plan prompt must establish its own reviewed baseline before release.

## Boundaries

This suite measures conversational content. It does not claim to measure pronunciation,
prosody, latency, interruption handling, VAD, noisy audio, or Android routing. Those need
separate saved-audio and physical-device layers. The intended maturity path is:

1. **Crawl:** this deterministic text/multi-turn content suite.
2. **Walk:** fixed TTS and real recorded audio, including noise and hesitation.
3. **Run:** full physical-device sessions with manual audio review and promoted regression
   cases from actual failures.
