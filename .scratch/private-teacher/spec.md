# AI private teacher: scope for ticket drafting

Status: accepted and implemented; live three-lesson validation remains pending

## Product direction

Provide affordable, engaging, individual language teaching through hour-long lessons for learners from complete beginner to advanced. Infer practical goals through conversation. Personalise examples immediately; confirm naturally before an inferred goal redirects several lessons. Distinguish an interest from a goal, and an inference from the learner's agreement.

## Proposed first delivery

Extend the current voice tutor into a connected teaching experience: prepare a lesson, teach and practise a bounded objective, record evidence of performance, and use that record to prepare the next lesson. Three connected lessons are the proposed integration demonstration, not an established learning-effectiveness study.

Keep the existing mobile targets, editable language settings, user-provided API key, cloud voice service, and local application data as the implementation baseline. These are current capabilities, not a new decision that rules out later product changes. The initial tickets should work without choosing a commercial audience or forcing a preset learner goal.

## Accepted goal policy

- A learner does not need to pick a practical goal before starting to talk.
- Context may immediately influence examples and short practice activities.
- Inferred practical goals remain distinct from casual interests and confirmed priorities.
- Before redirecting several lessons, the teacher checks naturally with the learner.
- Agreement can change upcoming priorities; rejection or an unclear answer preserves the existing direction.

## Proposed teaching behavior for review

- A prepared lesson contains a clear objective, selected teaching material, practice, feedback, a further attempt, and an evidence-based recap.
- Timings are flexible within the hour. Ending early records only observed work; it does not fabricate completion.
- Begin with a bounded, authored material set. Content selection uses the active language, observed ability, and practical goal when one exists.
- Adjust support for complete beginners and task complexity for advanced learners. Unknown ability is distinct from assessed ability. Brief familiar-language explanations are part of this proposal, requiring an intentional change from the current target-language-only prompt.
- Keep supported practice, independent production, and later retention distinct. Ambiguous speech evidence does not establish success or failure.
- A short between-lesson assignment and a later independent check supply continuity across the proposed three-lesson demonstration.
- Show meaningful progress and estimated complete lesson cost. Engagement and delayed independent performance are separate observations.

## Existing behavior to extend

The app already has a live transcript, one planned conversation objective, source-quoted outcome validation, lesson summaries, vocabulary review, an editable memory screen, and cost accounting. The polished recap is currently a demo; the real completion path should expose actual saved outcomes.

The current conversational prompt deliberately limits structured exercises and immediate retry. Prepared teaching requires an intentional behavior change with updated evaluation expectations, while preserving correction accuracy, learner agency, and restraint during personal disclosures.

## Remaining decisions and external dependency

- The existing decision ticket [Decide what the coach may retain and what forgetting means](../learner-memory/issues/03-memory-policy.md) must settle durable source retention and deletion semantics before implementing new durable lesson processing. This specification does not pick a raw transcript retention duration or broaden personal data collection by assumption.
- Audience, initial target/explanation languages, and whether the full lesson may require screen interaction remain unanswered.
- The teaching-material and assignment proposals need review with the ticket breakdown. A shared board, general reading/writing tuition, billing, backend retrieval, and local model migration are outside this proposed first ticket batch. This does not rule them out of the product.
- Set operational budgets from measured baseline behavior and a user-selected acceptable cost. Do not invent a price or claim equivalence to a qualified teacher.

## Verification expectations

Every implementation slice includes its own relevant behavior tests and a reproducible demonstration. Important cases include retrying processing after restart without duplicate learning updates, language isolation, distinguishing interests from goals, accepting and declining course changes, uncertain transcripts, supported versus independent production, and correction/forgetting during pending processing.

The final connected-lesson demonstration exercises real extraction, storage, reopening, retrieval and teaching. Handwritten memory injected into a prompt alone does not verify continuity. Record model and prompt versions, relevant observations, failures, and cost; keep authored demonstrations distinct from actual learner outcomes.

## Publication state

This is a reviewable scope document. The proposed tickets are presented in the conversation for approval under to-tickets. No implementation issue has been published or marked ready-for-agent, and the existing learner-memory parent map is unchanged.
