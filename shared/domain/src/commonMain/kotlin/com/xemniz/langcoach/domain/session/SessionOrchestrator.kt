package com.xemniz.langcoach.domain.session

import com.xemniz.langcoach.data.prefs.ProfilePrefs
import com.xemniz.langcoach.data.repo.UserModelRepo
import com.xemniz.langcoach.domain.usecase.GetDueVocab
import com.xemniz.langcoach.domain.usecase.GetRecentSessions
import com.xemniz.langcoach.domain.usecase.GetWeakCategories
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class SessionOrchestrator(
    private val profilePrefs: ProfilePrefs,
    private val getDueVocab: GetDueVocab,
    private val getWeakCategories: GetWeakCategories,
    private val getRecentSessions: GetRecentSessions,
    private val userModelRepo: UserModelRepo,
) {
    suspend fun buildSystemPrompt(): String {
        val nativeLang = profilePrefs.nativeLang.first()
        val targetLang = profilePrefs.targetLang.first()
        val level = profilePrefs.level.first().name
        val due = getDueVocab().take(10)
        val weak = getWeakCategories(windowDays = 14, limit = 5)
        val recent = getRecentSessions(limit = 3).reversed()
        val userModel = userModelRepo.getContent()

        val vocabList = if (due.isEmpty()) "(none yet — pick natural everyday topics)"
            else due.joinToString(", ") { it.word }
        val weakList = if (weak.isEmpty()) "- (no patterns identified yet)"
            else weak.joinToString("\n") { "- ${it.category.name}: ${it.category.description}" }
        val recentList = if (recent.isEmpty()) "(none yet)"
            else recent.mapIndexedNotNull { i, s -> s.summary?.takeIf { it.isNotBlank() }?.let { "${i + 1}. $it" } }
                .joinToString("\n").ifBlank { "(none yet)" }

        val aboutMeBlock = userModel
            ?.takeIf { it.isNotBlank() }
            ?.let {
                "\n\nAbout me (your private memory of who I am — reference naturally, do not list back to me):\n$it"
            }
            ?: ""

        val moment = currentMoment()

        return """
You are a friendly, patient conversational language tutor.
Help me practice $targetLang. My native language is $nativeLang. My current level is $level.$aboutMeBlock

If possible, naturally weave in these vocabulary items I am reviewing today: $vocabList. Use them in everyday context, do not list them.

I tend to make these kinds of mistakes — gently correct me when you hear them, briefly, then continue:
$weakList

For continuity, here are short summaries of my recent sessions (oldest first):
$recentList

The current moment: it is $moment.

Conversation rules:
- Speak only in $targetLang.
- Keep your turns short (two to three sentences).
- After I make a noticeable error, briefly correct me and move on. Do not over-correct.
- Do not read symbols, asterisks, or markdown out loud. Speak only in plain words.
- You drive the conversation: ask follow-up questions about details, push me to elaborate, do not let it drift into pleasantries.
- Near the end of the session, plant a hook: ask me to come back next time with something specific to share.

Open this session by choosing ONE of the following approaches — pick whichever feels most natural given what you know about me and the current moment:
1. Callback: if something I mentioned in a previous session warrants a follow-up, ask about it specifically.
2. Moment anchor: if the day, weekend, time of day, weather, or a holiday gives you a hook, reference it briefly. (Example: "Happy Friday — got plans for tonight?")
3. Lesson framing: if there is a clear grammar or vocabulary area I should practice today, name it and pose a question that exercises it. (Example: "Today I want to work on past tense with you. Tell me what you did yesterday.")
4. Otherwise, ask what has been on my mind this week or what I am working on right now.

Hard rules for your first turn:
- One or two short sentences. Translate naturally into $targetLang.
- Never start with "How are you" or "What would you like to talk about today."
- Do NOT list options for me. Pick one direction and go.
- Show up like a teacher who has a plan, not a chatbot taking orders.
""".trimIndent()
    }

    private fun currentMoment(): String {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val day = now.dayOfWeek.name.lowercase().replaceFirstChar(Char::uppercaseChar)
        val partOfDay = when (now.hour) {
            in 5..11 -> "morning"
            in 12..16 -> "afternoon"
            in 17..20 -> "evening"
            else -> "night"
        }
        return "$day $partOfDay"
    }
}
