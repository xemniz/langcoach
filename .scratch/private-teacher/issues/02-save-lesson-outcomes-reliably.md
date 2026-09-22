# 02: Save lesson outcomes reliably

**What to build:** Preserve each lesson's language, level, transcript, processing state, and usage so post-lesson work can resume after interruption without silently losing the learner's record.

**Blocked by:** None (can start immediately).

**Status:** completed

- [x] Ending a lesson saves its context and turns before background reflection begins.
- [x] Pending and failed lessons can be retried, with visible processing state and no duplicate learning records.
- [x] Changing profile settings after a lesson does not reinterpret that lesson.

Raw turns are retained only while reflection is pending or failed. Successful completion atomically stores the derived lesson record and deletes the raw transcript.
