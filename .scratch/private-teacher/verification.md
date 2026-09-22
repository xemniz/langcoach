# Connected private-teacher verification

## Automated evidence

- `SessionPlannerTest.threeConnectedLessonsConfirmPracticeAndMoveOnAfterLaterRetention` exercises an authored three-lesson sequence: tentative practical-goal personalisation, confirmed course direction with an assignment, and progression after later independent retention evidence.
- `RecordPracticalGoalTest` covers source-backed inference, explicit acceptance, retry idempotence, rejection during in-flight processing, and permanent removal.
- `ProcessPendingLessonsTest` verifies that every pending lesson is attempted even when one record fails.
- `ObjectiveEvaluationPolicyTest` and the planner tests keep uncertain, helped, independent, and retained-later outcomes distinct.
- `TutorPromptTest` pins the flexible prepared-lesson arc, meaningful retry, level repair, and learner-requested adaptation.
- `./gradlew allTests` passed on Android and iOS simulator targets on 2026-09-22.
- The live `tutor-v1.2.0` content suite ran against `gpt-realtime-2.1` with `gpt-4.1-mini` judging: 12/12 cases passed, the suite score was 4.74/5, and there were zero hard violations. This measures scripted tutor responses, not the persistence lifecycle or learner outcomes.

## Runtime records

Each completed lesson now retains its original language and level, ordered transcript turns, processing state, source evidence, recap, strength, next step, assignment, objective result, and token usage. Realtime and lesson-processing token entries are replaced idempotently per session and endpoint. The current product still records `costCents = 0`; no current model price or learner-approved budget was established, so this implementation does not invent a monetary estimate.

## Limits of the result

The three-lesson test is a deterministic authored fixture. It verifies policy continuity and regression behavior, not language acquisition or real learner progress. No three-hour learner study or scripted on-device voice sequence was run. A live acceptance run still needs to measure extraction quality, audio behavior, end-to-end persistence after process interruption, latency, retries, and actual billed cost. Transcript retention remains on-device and durable because reliable replay was explicitly implemented, while the broader retention-duration policy in the learner-memory track remains open.
