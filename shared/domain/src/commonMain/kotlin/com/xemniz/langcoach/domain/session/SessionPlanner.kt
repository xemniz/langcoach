package com.xemniz.langcoach.domain.session

/**
 * Pure, deterministic policy for selecting one primary session objective.
 *
 * Repeated error patterns outrank vocabulary retrieval. A one-off weak area does not: due
 * vocabulary gets the priority until the error has appeared in at least two recent sessions.
 */
class SessionPlanner {
    fun create(context: SessionPlanningContext): SessionPlan {
        val completedObjectiveId = context.recentObjectiveResults
            .lastOrNull()
            ?.takeIf {
                it.outcome == ObjectiveOutcome.AchievedIndependently &&
                    it.confidence >= INDEPENDENT_SUCCESS_CONFIDENCE
            }
            ?.objectiveId
        val eligibleWeakAreas = context.weakAreas.filterNot {
            "error:${it.code}" == completedObjectiveId
        }
        val eligibleVocabulary = context.dueVocabulary.filterNot {
            "vocabulary:${it.lemma.lowercase()}" == completedObjectiveId
        }
        val recurringWeakArea = eligibleWeakAreas.firstOrNull { it.recentCount >= 2 }
        val objective = when {
            recurringWeakArea != null -> errorPlan(recurringWeakArea, context)
            eligibleVocabulary.isNotEmpty() -> vocabularyPlan(eligibleVocabulary.first(), context)
            eligibleWeakAreas.isNotEmpty() -> errorPlan(eligibleWeakAreas.first(), context)
            else -> conversationPlan(context)
        }
        return objective
    }

    private fun errorPlan(
        weakArea: PlanningWeakArea,
        context: SessionPlanningContext,
    ) = SessionPlan(
        objectiveId = "error:${weakArea.code}",
        kind = SessionObjectiveKind.ErrorPattern,
        objective = "Create one natural opportunity to use ${weakArea.name.lowercase()} accurately.",
        target = weakArea.code,
        openingHint = openingHint(context),
        successCriteria =
            "The learner uses the target feature accurately in their own communicative sentence. " +
                "Immediate repetition of a tutor-supplied answer is only achievement with help.",
    )

    private fun vocabularyPlan(
        vocabulary: PlanningVocabulary,
        context: SessionPlanningContext,
    ) = SessionPlan(
        objectiveId = "vocabulary:${vocabulary.lemma.lowercase()}",
        kind = SessionObjectiveKind.VocabularyRetrieval,
        objective = "Create one natural opportunity for the learner to retrieve “${vocabulary.word}”.",
        target = vocabulary.word,
        openingHint = openingHint(context),
        successCriteria =
            "The learner uses “${vocabulary.word}” appropriately without the tutor supplying it " +
                "in the immediately preceding turn.",
    )

    private fun conversationPlan(context: SessionPlanningContext) = SessionPlan(
        objectiveId = "fluency:sustained-thread",
        kind = SessionObjectiveKind.ConversationFluency,
        objective = "Help the learner sustain one meaningful conversational thread.",
        target = null,
        openingHint = openingHint(context),
        successCriteria =
            "The learner develops the same topic across at least three substantive turns without " +
                "the tutor turning the exchange into an interview.",
    )

    private fun openingHint(context: SessionPlanningContext): String {
        val recent = context.recentSessionSummaries.lastOrNull()
        return when {
            !recent.isNullOrBlank() ->
                "Continue one specific unfinished or promising thread from this recent session: $recent"
            context.learnerMemory.isNotBlank() ->
                "Use at most one relevant learner detail to open naturally."
            else -> "Use the current moment (${context.currentMoment}) as a concrete opening."
        }
    }

    private companion object {
        const val INDEPENDENT_SUCCESS_CONFIDENCE = 0.8
    }
}

data class SessionPlanningContext(
    val dueVocabulary: List<PlanningVocabulary>,
    val weakAreas: List<PlanningWeakArea>,
    val recentSessionSummaries: List<String>,
    val recentObjectiveResults: List<PlanningObjectiveResult>,
    val learnerMemory: String,
    val currentMoment: String,
)

data class PlanningVocabulary(
    val word: String,
    val lemma: String,
)

data class PlanningWeakArea(
    val code: String,
    val name: String,
    val recentCount: Int,
)

data class PlanningObjectiveResult(
    val objectiveId: String,
    val outcome: ObjectiveOutcome,
    val confidence: Double,
)
