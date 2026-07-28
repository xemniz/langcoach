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
) {
    fun renderForPrompt(): String = buildString {
        append("Primary objective: ").append(objective).append('\n')
        target?.let { append("Target: ").append(it).append('\n') }
        append("Opening direction: ").append(openingHint).append('\n')
        append("Success evidence: ").append(successCriteria).append('\n')
        append(
            "Treat this as a light intention, not a script. Follow a more meaningful learner-led " +
                "direction when one appears.",
        )
    }

    companion object {
        const val VERSION = "session-plan-v1"
    }
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
}

fun ObjectiveOutcome.vocabularyReviewRating(): Int? = when (this) {
    ObjectiveOutcome.NotObserved -> null
    ObjectiveOutcome.Attempted -> 1
    ObjectiveOutcome.AchievedWithHelp -> 2
    ObjectiveOutcome.AchievedIndependently -> 3
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
)
