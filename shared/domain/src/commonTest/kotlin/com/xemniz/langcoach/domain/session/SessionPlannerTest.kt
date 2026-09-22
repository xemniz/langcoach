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

    @Test
    fun retainedObjectiveIsNotImmediatelySelectedAgain() {
        val plan = planner.create(
            context(
                weakAreas = listOf(
                    PlanningWeakArea("VERB_TENSE", "Verb tense", recentCount = 3),
                    PlanningWeakArea("PREPOSITION", "Preposition", recentCount = 2),
                ),
                recentObjectiveResults = listOf(
                    PlanningObjectiveResult(
                        objectiveId = "error:VERB_TENSE",
                        outcome = ObjectiveOutcome.RetainedLater,
                        confidence = 0.95,
                    ),
                ),
            ),
        )

        assertEquals("error:PREPOSITION", plan.objectiveId)
    }

    @Test
    fun tentativePracticalGoalPersonalisesLessonButDoesNotBecomeCourseDirection() {
        val plan = planner.create(
            context(
                confirmedPracticalGoal = "Speak with neighbours",
                tentativePracticalGoal = "Prepare for a job interview",
            ),
        )

        assertEquals("Speak with neighbours", plan.courseDirection)
        assertContains(plan.goalConfirmationPrompt.orEmpty(), "job interview")
        assertContains(plan.material, "job interview")
    }

    @Test
    fun threeConnectedLessonsConfirmPracticeAndMoveOnAfterLaterRetention() {
        val discoveryLesson = planner.create(
            context(
                level = "A0",
                tentativePracticalGoal = "introduce myself to new colleagues",
            ),
        )
        assertEquals(LessonFormat.PresentationPracticeProduction, discoveryLesson.lessonFormat)
        assertContains(discoveryLesson.goalConfirmationPrompt.orEmpty(), "new colleagues")

        val supportedPracticeLesson = planner.create(
            context(
                weakAreas = listOf(PlanningWeakArea("INTRO", "Introductions", recentCount = 2)),
                confirmedPracticalGoal = "introduce myself to new colleagues",
                priorAssignment = "Record a 30-second introduction",
            ),
        )
        assertEquals("error:INTRO", supportedPracticeLesson.objectiveId)
        assertEquals(
            "Record a 30-second introduction",
            supportedPracticeLesson.priorAssignment,
        )

        val laterIndependentCheck = planner.create(
            context(
                weakAreas = listOf(
                    PlanningWeakArea("INTRO", "Introductions", recentCount = 3),
                    PlanningWeakArea("QUESTIONS", "Follow-up questions", recentCount = 2),
                ),
                recentObjectiveResults = listOf(
                    PlanningObjectiveResult(
                        objectiveId = "error:INTRO",
                        outcome = ObjectiveOutcome.RetainedLater,
                        confidence = 0.93,
                    ),
                ),
                confirmedPracticalGoal = "introduce myself to new colleagues",
            ),
        )
        assertEquals("error:QUESTIONS", laterIndependentCheck.objectiveId)
        assertEquals("introduce myself to new colleagues", laterIndependentCheck.courseDirection)
    }

    @Test
    fun unknownAbilityStartsWithDiagnosisInsteadOfAssumingBeginnerOrAdvanced() {
        val plan = planner.create(context(level = "Unknown"))

        assertEquals(LessonFormat.TestTeachTest, plan.lessonFormat)
        assertContains(plan.supportGuidance, "diagnostic")
        assertContains(plan.supportGuidance, "demonstrated performance")
    }

    private fun context(
        dueVocabulary: List<PlanningVocabulary> = emptyList(),
        weakAreas: List<PlanningWeakArea> = emptyList(),
        recentSessionSummaries: List<String> = emptyList(),
        recentObjectiveResults: List<PlanningObjectiveResult> = emptyList(),
        confirmedPracticalGoal: String? = null,
        tentativePracticalGoal: String? = null,
        level: String = "A2",
        priorAssignment: String? = null,
    ) = SessionPlanningContext(
        dueVocabulary = dueVocabulary,
        weakAreas = weakAreas,
        recentSessionSummaries = recentSessionSummaries,
        recentObjectiveResults = recentObjectiveResults,
        learnerMemory = "",
        currentMoment = "Saturday morning",
        level = level,
        confirmedPracticalGoal = confirmedPracticalGoal,
        tentativePracticalGoal = tentativePracticalGoal,
        priorAssignment = priorAssignment,
    )
}
