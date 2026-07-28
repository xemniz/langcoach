# Tutor quality strategy

LangCoach should feel like an engaging person who happens to be a skilled language
teacher—not a grammar checker with a voice.

## Product definition

A strong session should leave the learner feeling four things:

1. The tutor listened to what they meant.
2. The conversation had momentum and continuity.
3. They noticed one useful improvement.
4. The tutor remembers enough to prepare, but not enough to feel invasive.

This definition deliberately puts communication before correction. The
[CEFR's action-oriented approach](https://www.coe.int/en/web/portfolio/the-common-european-framework-of-reference-%20for-languages-learning-teaching-assessment-cefr-)
frames proficiency through what a learner can do in reception, production, interaction,
and mediation. ACTFL likewise connects proficiency to
[real-world tasks by communication mode](https://www.actfl.org/assessments/k-12-assessments/aappl/aappl-tasks-topics).
That supports conversations and short practical tasks as the core unit—not isolated
grammar explanations.

## Teaching behavior encoded in the prompt

### Selective corrective feedback

Corrective feedback is useful overall, but its effectiveness depends on how it is delivered;
the evidence does not support correcting everything indiscriminately
([Li, 2010 meta-analysis](https://onlinelibrary.wiley.com/doi/10.1111/j.1467-9922.2010.00561.x)).
LangCoach therefore:

- corrects at most one high-value feature per learner turn;
- reacts to meaning before teaching;
- uses a brief recast when there is only one error;
- may elicit one self-repair attempt for a recurring or lesson-focus error;
- supplies the form and moves on if the learner struggles;
- never rewrites an error-heavy sentence into a fully corrected sentence;
- never demands immediate repetition.

The last constraint matters for voice UX. A technically helpful full rewrite can feel like
an interruption and hide which single feature the learner should notice.

### Retrieval without nagging

Spaced practice supports second-language vocabulary learning, including productive
activities ([Kim & Webb, 2023](https://onlinelibrary.wiley.com/doi/abs/10.1111/modl.12879)).
The product already schedules vocabulary; the tutor prompt adds a conversational policy:

- surface one due item at a time in a relevant context;
- never announce a review list;
- after successful use, do not request the same answer again;
- if a form returns later, change the context or communicative purpose.

This distinguishes useful retrieval from repetition that merely feels repetitive.

### Teacher memory with boundaries

Memory is used as lesson preparation, not as a demonstration of recall:

- prefer unfinished threads, interests, and current goals;
- mention at most one remembered detail at once;
- never say that a fact came from memory, a profile, or stored data;
- ignore remembered facts when the learner introduces a more important new topic;
- treat weak categories as hypotheses until the error occurs in the current session.

### Short varied activities

Free conversation remains the default. At most one short activity may appear when it fits
the learner's goal or an organic learning opportunity. Available formats include practical
role-play, story continuation, mini-debate, describe-and-guess, paraphrase, and
error-detective tasks.

Activities are constrained to two to four turns, start immediately, stop after success,
and must not interrupt an engaging personal story. This keeps the tutor purposeful without
making every session resemble a worksheet.

### Natural engagement

The prompt explicitly counters common synthetic-chat behavior:

- one question at a time;
- react to a specific detail before asking the next question;
- avoid an interview rhythm;
- do not praise ordinary sentences automatically;
- do not pretend to have a personal life;
- maintain one loose conversational thread;
- simplify in the target language when the learner is confused.

## Evaluation approach

OpenAI's
[Realtime prompting guide](https://developers.openai.com/cookbook/examples/realtime_prompting_guide)
recommends clear labeled sections, precise bullets, examples, explicit language control,
and a variety rule. The production prompt follows that structure.

The
[Realtime eval guide](https://developers.openai.com/cookbook/examples/realtime_eval_guide)
recommends building complexity gradually, separating content quality from audio quality,
using balanced positive and negative cases, versioning simulation prompts, and combining
deterministic and LLM graders. LangCoach's first suite implements the content layer and
stores its reviewed baseline under `evals/tutor/baselines/`.

The next evaluation layer should add fixed TTS and human recordings for:

- Spanish learner accents at A1 through C1;
- background speech and speaker echo;
- hesitation, self-correction, and code-switching;
- interruption and barge-in;
- response latency, pacing, and prosody;
- pronunciation of learner names and due vocabulary.

Real dogfooding failures should be anonymized, reproduced, and promoted into the regression
set. The holdout must stay small and untouched during routine prompt iteration so a higher
regression score does not hide overfitting.
