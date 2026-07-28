package com.xemniz.langcoach.domain.session

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SessionPlannerTest {
    private val planner = SessionPlanner()

    @Test
    fun recurringErrorOutranksDueVocabulary() {
        val plan = planner.create(
            context(
                dueVocabulary = listOf(PlanningVocabulary("regatear", "regatear")),
                weakAreas = listOf(PlanningWeakArea("VERB_TENSE", "Verb tense", recentCount = 3)),
            ),
        )

        assertEquals(SessionObjectiveKind.ErrorPattern, plan.kind)
        assertEquals("error:VERB_TENSE", plan.objectiveId)
        assertContains(plan.successCriteria, "Immediate repetition")
    }

    @Test
    fun dueVocabularyOutranksOneOffError() {
        val plan = planner.create(
            context(
                dueVocabulary = listOf(PlanningVocabulary("regatear", "regatear")),
                weakAreas = listOf(PlanningWeakArea("VERB_TENSE", "Verb tense", recentCount = 1)),
            ),
        )

        assertEquals(SessionObjectiveKind.VocabularyRetrieval, plan.kind)
        assertEquals("vocabulary:regatear", plan.objectiveId)
        assertEquals("regatear", plan.target)
    }

    @Test
    fun noLearningSignalsCreatesConversationObjective() {
        val plan = planner.create(context())

        assertEquals(SessionObjectiveKind.ConversationFluency, plan.kind)
        assertEquals("fluency:sustained-thread", plan.objectiveId)
        assertNull(plan.target)
        assertContains(plan.openingHint, "Saturday morning")
    }

    @Test
    fun recentSessionProvidesOpeningContinuity() {
        val plan = planner.create(
            context(
                recentSessionSummaries = listOf("The learner was preparing for an interview."),
            ),
        )

        assertContains(plan.openingHint, "preparing for an interview")
    }

    @Test
    fun independentlyAchievedObjectiveIsNotImmediatelySelectedAgain() {
        val plan = planner.create(
            context(
                weakAreas = listOf(
                    PlanningWeakArea("VERB_TENSE", "Verb tense", recentCount = 3),
                    PlanningWeakArea("PREPOSITION", "Preposition", recentCount = 2),
                ),
                recentObjectiveResults = listOf(
                    PlanningObjectiveResult(
                        objectiveId = "error:VERB_TENSE",
                        outcome = ObjectiveOutcome.AchievedIndependently,
                        confidence = 0.95,
                    ),
                ),
            ),
        )

        assertEquals("error:PREPOSITION", plan.objectiveId)
    }

    private fun context(
        dueVocabulary: List<PlanningVocabulary> = emptyList(),
        weakAreas: List<PlanningWeakArea> = emptyList(),
        recentSessionSummaries: List<String> = emptyList(),
        recentObjectiveResults: List<PlanningObjectiveResult> = emptyList(),
    ) = SessionPlanningContext(
        dueVocabulary = dueVocabulary,
        weakAreas = weakAreas,
        recentSessionSummaries = recentSessionSummaries,
        recentObjectiveResults = recentObjectiveResults,
        learnerMemory = "",
        currentMoment = "Saturday morning",
    )
}
