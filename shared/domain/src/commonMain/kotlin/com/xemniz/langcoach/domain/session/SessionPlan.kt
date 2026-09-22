package com.xemniz.langcoach.domain.session

/**
 * One primary learning objective for a conversation.
 *
 * The plan is intentionally compact: it guides the tutor without turning the conversation into a
 * scripted lesson. [target] is the exact language feature or vocabulary item being observed.
 */
data class SessionPlan(
    val version: String = VERSION,
    val objectiveId: String,
    val kind: SessionObjectiveKind,
    val objective: String,
    val target: String?,
    val openingHint: String,
    val successCriteria: String,
    val lessonFormat: LessonFormat = LessonFormat.TestTeachTest,
    val material: String = "Use a short, relevant conversational example.",
    val supportGuidance: String = "Adjust support from observed performance.",
    val courseDirection: String? = null,
    val goalConfirmationPrompt: String? = null,
    val priorAssignment: String? = null,
) {
    fun renderForPrompt(): String = buildString {
        append("Primary objective: ").append(objective).append('\n')
        target?.let { append("Target: ").append(it).append('\n') }
        append("Opening direction: ").append(openingHint).append('\n')
        append("Success evidence: ").append(successCriteria).append('\n')
        append("Lesson format: ").append(lessonFormat.name).append('\n')
        append("Prepared material: ").append(material).append('\n')
        append("Support: ").append(supportGuidance).append('\n')
        courseDirection?.let { append("Confirmed course direction: ").append(it).append('\n') }
        goalConfirmationPrompt?.let { append("Tentative goal to confirm naturally: ").append(it).append('\n') }
        priorAssignment?.let { append("Previous assignment to check: ").append(it).append('\n') }
        append("Treat the stages as a flexible teaching arc. Follow meaningful learner input while returning to the objective.")
    }

    companion object {
        const val VERSION = "session-plan-v1"
    }
}

enum class LessonFormat {
    PresentationPracticeProduction,
    TestTeachTest,
    TaskBased,
}

enum class SessionObjectiveKind {
    ErrorPattern,
    VocabularyRetrieval,
    ConversationFluency,
}

enum class ObjectiveOutcome {
    NotObserved,
    Attempted,
    AchievedWithHelp,
    AchievedIndependently,
    RetainedLater,
}

fun ObjectiveOutcome.vocabularyReviewRating(): Int? = when (this) {
    ObjectiveOutcome.NotObserved -> null
    ObjectiveOutcome.Attempted -> 1
    ObjectiveOutcome.AchievedWithHelp -> 2
    ObjectiveOutcome.AchievedIndependently -> 3
    ObjectiveOutcome.RetainedLater -> 3
}

data class ObjectiveEvaluation(
    val outcome: ObjectiveOutcome,
    val evidenceTurnId: String?,
    val evidenceText: String?,
    val confidence: Double,
)

data class PreparedSession(
    val plan: SessionPlan,
    val systemPrompt: String,
    val transcriptionLanguage: String?,
    val nativeLanguage: String,
    val targetLanguage: String,
    val level: String,
    val tentativePracticalGoalId: Long? = null,
)
