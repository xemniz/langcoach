# 01: Discover and confirm practical goals

**What to build:** Let a learner begin speaking without choosing a goal first. The AI teacher may use an evidence-linked inferred practical goal to personalise examples in the current lesson, but only a goal the learner explicitly accepts may redirect future lessons. The learner can inspect the current inference, accept or reject it, and correct its wording.

**Blocked by:** None (can start immediately).

**Status:** completed

- [x] A learner statement can produce a tentative practical goal with its source turn; a casual interest is not silently treated as a goal.
- [x] Lesson preparation can use a tentative goal for contextual examples but exposes only an accepted goal as the course direction.
- [x] Acceptance, rejection, and deferral are recorded distinctly; rejection or no answer preserves the existing course direction.
- [x] The teacher naturally asks for confirmation before proposing a change that affects several lessons.
- [x] The learner can inspect, edit, accept, or reject a tentative goal from the coach-memory experience.
- [x] Goal extraction and updates are covered at the public goal-recording use case, and tutor behavior cases cover personalisation and confirmation.
- [x] Existing target-language, objective-planning, and memory behaviors continue to pass their tests.

## Notes

The storage model must keep practical goals separate from interests, freeform personal memory, and per-lesson objectives. A model inference must retain learner evidence and must not become accepted merely because its confidence is high.

The initial slice uses the existing mobile, local-storage, and OpenAI reflection architecture. Reliable resumption of interrupted reflection is tracked separately and is not silently bundled into this ticket.
