# Define the AI private-teacher pilot

Status: drafting ticket proposal

## Destination under discussion

A build-ready specification for an affordable AI private teacher, illustrated by connected lessons and explicit quality criteria. This destination and a three-lesson pilot were proposed by the assistant; the user has requested an interview to sharpen the design, not yet accepted every recommendation.

## Confirmed user direction

- Reproduce established private language teaching, with an affordable AI teacher.
- A lesson takes an hour.
- The product should accommodate learners at any language level.
- The experience should be engaging.
- Use grill-with-docs to sharpen the design and document settled terms and decisions.
- Infer the learner's practical goal from conversation. A preset practical goal should not define the pilot's learners.
- Personalise examples from conversational context immediately. Briefly confirm with the learner before an inferred goal redirects several lessons; an incidental remark must remain easy to move past.

## Round one: open decisions

1. Who is the first version for: the owner personally, a small invited pilot, or paying public users?
   - Recommendation: the owner plus a small invited group whose lessons can be observed with their agreement.
2. Which target language and learner familiar language(s) define the first pilot?
   - Partially answered: the practical goal emerges from conversation; it is not a preset pilot requirement. Target and explanation languages remain unanswered.
   - Recommendation still open: one target language, with beginner, intermediate, and advanced examples. The eventual any-level ambition remains the user's direction.
3. Can a lesson require looking at and interacting with the screen, or must it work entirely hands-free?
   - Recommendation: voice-led lessons with a shared board for examples, short texts, and learner writing; include stretches of audio-only activity.

## Settled branch: inferring the practical goal

- Decision: infer goals conversationally and personalise examples immediately; ask a natural confirmation before redirecting several lessons.
- Example: mentioning a job interview might justify interview preparation, but may also be incidental conversation.
- The learner's "ok" accepted the recommended confirmation boundary. A passing interest alone does not establish a new practical goal, and a goal inference does not itself authorise a course change.
- Audience, target/explanation languages, and screen interaction remain open; the user's partial answer does not select defaults for them.

## Dependent decisions to revisit

- Product access and payment: the existing user-provided API key model versus managed access, after the pilot audience is chosen.
- Course scope, source materials, assessment and skill coverage, after the first learners and lesson environment are specified.
- Teacher initiative, learner choice, correction and adaptation policies within a lesson.
- Student records, evidence retention, corrections and forgetting; coordinate with the existing learner-memory map.
- Quality, learner outcomes, return behavior, and acceptable cost per completed lesson.
- Implementation scope and architecture after teaching behavior and operating constraints are settled.

## Documentation

- [Domain glossary](../../CONTEXT.md) contains only terms already grounded in the user's stated direction.
- [Improve LangCoach learner memory](../learner-memory/map.md) remains a separate open effort; its unresolved choices are not silently resolved by this interview.
- No architecture decision is accepted yet. Create an ADR only when a consequential tradeoff has been settled with the user.

## Answers

2026-09-22 — User answered the practical-goal part of question two: "practical goal should be implied from the talk". Recorded as conversation-based goal inference. Other first-round choices and the confirmation policy remain open.

2026-09-22 — User replied "ok" to personalising examples immediately and briefly confirming before redirecting several lessons, then invoked to-tickets. The confirmation policy is settled. The next artifact is a proposed breakdown for review, not published ready-for-agent tickets. Unanswered audience, languages, and screen-interaction choices remain open.
