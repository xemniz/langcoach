package com.xemniz.langcoach.domain.session

/**
 * Pure, deterministic policy for selecting one primary session objective.
 *
 * Repeated error patterns outrank vocabulary retrieval. A one-off weak area does not: due
 * vocabulary gets the priority until the error has appeared in at least two recent sessions.
 */
class SessionPlanner {
    fun create(context: SessionPlanningContext): SessionPlan {
        val completedObjectiveIds = context.recentObjectiveResults
            .filter {
                it.outcome in setOf(
                    ObjectiveOutcome.AchievedIndependently,
                    ObjectiveOutcome.RetainedLater,
                ) &&
                    it.confidence >= INDEPENDENT_SUCCESS_CONFIDENCE
            }
            .mapTo(mutableSetOf()) { it.objectiveId }
        val eligibleWeakAreas = context.weakAreas.filterNot {
            "error:${it.code}" in completedObjectiveIds
        }
        val eligibleVocabulary = context.dueVocabulary.filterNot {
            "vocabulary:${it.lemma.lowercase()}" in completedObjectiveIds
        }
        val recurringWeakArea = eligibleWeakAreas.firstOrNull { it.recentCount >= 2 }
        val objective = when {
            recurringWeakArea != null -> errorPlan(recurringWeakArea, context)
            eligibleVocabulary.isNotEmpty() -> vocabularyPlan(eligibleVocabulary.first(), context)
            eligibleWeakAreas.isNotEmpty() -> errorPlan(eligibleWeakAreas.first(), context)
            else -> conversationPlan(context)
        }
        return objective.copy(
            lessonFormat = lessonFormat(context, objective.kind),
            material = preparedMaterial(context, objective),
            supportGuidance = supportGuidance(context.level),
            courseDirection = context.confirmedPracticalGoal,
            goalConfirmationPrompt = context.tentativePracticalGoal?.let {
                "The learner may want to $it. Use it for relevant examples now, then briefly ask whether future lessons should work toward it."
            },
            priorAssignment = context.priorAssignment,
        )
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

    private fun lessonFormat(context: SessionPlanningContext, kind: SessionObjectiveKind): LessonFormat =
        when {
            context.level == "Unknown" -> LessonFormat.TestTeachTest
            context.level in setOf("A0", "A1", "A2") -> LessonFormat.PresentationPracticeProduction
            kind == SessionObjectiveKind.ConversationFluency -> LessonFormat.TaskBased
            else -> LessonFormat.TestTeachTest
        }

    private fun preparedMaterial(context: SessionPlanningContext, objective: SessionPlan): String {
        val goal = context.tentativePracticalGoal ?: context.confirmedPracticalGoal
        val situation = goal?.let { "the real-world goal “$it”" }
            ?: "the learner's opening topic"
        return when (objective.kind) {
            SessionObjectiveKind.ErrorPattern ->
                "Task card — Situation: $situation. Learner task: describe one decision in two " +
                    "connected sentences and give the reason. Target to observe: ${objective.target}. " +
                    "Feedback card: contrast only the learner's form with its correction. Transfer " +
                    "card: give the same message to a different person and change the reason."
            SessionObjectiveKind.VocabularyRetrieval ->
                "Task card — Situation: $situation. Learner task: explain what is needed and why, " +
                    "using “${objective.target}”. Cue card, reveal in order only if needed: meaning, " +
                    "then first sound. Transfer card: change the person and place; ask for the same " +
                    "meaning again without a cue."
            SessionObjectiveKind.ConversationFluency ->
                "Task card — Situation: $situation. Learner task: state what happened, why it matters, " +
                    "and what should happen next. Complication card: the other person disagrees with " +
                    "one reason. Closing card: the learner gives a two-sentence recap and decision."
        }
    }

    private fun supportGuidance(level: String): String = when (level) {
        "Unknown" ->
            "Begin with a low-pressure diagnostic exchange, avoid labelling the learner's ability, and adjust language and support from demonstrated performance."
        "A0", "A1" ->
            "Use very short target-language turns, visible or spoken models, progressive hints, and a brief familiar-language explanation when target-language repair fails."
        "A2", "B1" ->
            "Let the learner attempt first when feasible, then provide focused explanation, hints, and a changed-context retry."
        else ->
            "Use demanding authentic turns, precision, register, nuance, and complications; explain only the gaps demonstrated in this lesson."
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
    val level: String = "A2",
    val confirmedPracticalGoal: String? = null,
    val tentativePracticalGoal: String? = null,
    val priorAssignment: String? = null,
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
