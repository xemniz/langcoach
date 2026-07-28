package com.xemniz.langcoach.domain.session

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class TutorPromptTest {
    private val prompt = TutorPrompt.build(
        TutorPromptContext(
            nativeLanguage = "English",
            targetLanguage = "Spanish",
            level = "B1",
            sessionPlan = """
                Primary objective: Create one natural opportunity to retrieve “regatear”.
                Target: regatear
                Success evidence: The learner uses it without a supplied answer.
            """.trimIndent(),
            learnerMemory = "The learner is preparing for a product-design interview.",
            dueVocabulary = "regatear, lograr",
            weakAreas = "- Verb tense: confuses preterite and present.",
            recentSessions = "1. Discussed a weekend market trip.",
            currentMoment = "Friday evening",
        )
    )

    @Test
    fun promptHasStableVersion() {
        assertEquals("tutor-v1.1.0", TutorPrompt.VERSION)
    }

    @Test
    fun promptInjectsLearnerContextWithoutUnresolvedPlaceholders() {
        assertContains(prompt, "current level is B1")
        assertContains(prompt, "preparing for a product-design interview")
        assertContains(prompt, "regatear, lograr")
        assertContains(prompt, "Friday evening")
        assertContains(prompt, "Primary objective")
        assertFalse(prompt.contains("{{"))
    }

    @Test
    fun promptCapsCorrectionAndPreventsRepetitiveDrilling() {
        assertContains(prompt, "correct AT MOST ONE error")
        assertContains(prompt, "not one fully rewritten sentence")
        assertContains(prompt, "NEVER recast or rewrite the sentence")
        assertContains(prompt, "no more than three words")
        assertContains(prompt, "Never stack corrections")
        assertContains(prompt, "Never repeat an identical question or drill")
        assertContains(prompt, "At most once in a session")
        assertContains(prompt, "start with only an in-character line")
        assertContains(prompt, "Do not add another model phrase")
        assertContains(prompt, "success with help")
        assertContains(prompt, "Do not immediately ask for the same answer again")
    }

    @Test
    fun promptPrioritizesMeaningAndEngagement() {
        assertContains(prompt, "Respond to the meaning")
        assertContains(prompt, "emotionally meaningful story")
        assertContains(prompt, "Avoid an interview rhythm")
        assertContains(prompt, "Default to free conversation")
        assertContains(prompt, "do not force an exercise", ignoreCase = true)
        assertContains(prompt, "twelve words or fewer")
    }
}
