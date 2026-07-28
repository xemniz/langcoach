package com.xemniz.langcoach.domain.reflection

data class SessionTranscript(
    val turns: List<TranscriptTurn>,
) {
    fun render(): String = turns.joinToString("\n\n") { turn ->
        "${turn.id} ${turn.speaker.label}:\n${turn.text}"
    }

    val learnerText: String
        get() = turns
            .filter { it.speaker == TranscriptSpeaker.Learner }
            .joinToString("\n") { it.text }

    val tutorText: String
        get() = turns
            .filter { it.speaker == TranscriptSpeaker.Tutor }
            .joinToString("\n") { it.text }
}

data class TranscriptTurn(
    val id: String,
    val speaker: TranscriptSpeaker,
    val text: String,
)

enum class TranscriptSpeaker(internal val label: String) {
    Learner("LEARNER"),
    Tutor("TUTOR"),
}
