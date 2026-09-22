package com.xemniz.langcoach.domain.session

import com.xemniz.langcoach.data.prefs.ProfilePrefs
import com.xemniz.langcoach.data.repo.UserModelRepo
import com.xemniz.langcoach.data.repo.PracticalGoalStore
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
    private val sessionPlanner: SessionPlanner,
    private val practicalGoals: PracticalGoalStore,
) {
    suspend fun prepareSession(): PreparedSession {
        val nativeLang = profilePrefs.nativeLang.first()
        val targetLang = profilePrefs.targetLang.first()
        val level = profilePrefs.level.first().name
        val due = getDueVocab().take(10)
        val weak = getWeakCategories(windowDays = 14, limit = 5)
        val recent = getRecentSessions(limit = 3).reversed()
        val userModel = userModelRepo.getContent()
        val confirmedGoal = practicalGoals.confirmed(targetLang)
        val tentativeGoal = practicalGoals.tentative(targetLang)

        val learnerMemory = userModel?.takeIf { it.isNotBlank() } ?: ""
        val recentSummaries = recent.mapNotNull { it.summary?.takeIf(String::isNotBlank) }
        val moment = currentMoment()
        val plan = sessionPlanner.create(
            SessionPlanningContext(
                dueVocabulary = due.map { PlanningVocabulary(it.word, it.lemma) },
                weakAreas = weak.map {
                    PlanningWeakArea(
                        code = it.category.code,
                        name = it.category.name,
                        recentCount = it.recentCount,
                    )
                },
                recentSessionSummaries = recentSummaries,
                recentObjectiveResults = recent.mapNotNull { session ->
                    val objectiveId = session.objectiveId ?: return@mapNotNull null
                    val outcome = session.objectiveOutcome
                        ?.let { runCatching { ObjectiveOutcome.valueOf(it) }.getOrNull() }
                        ?: return@mapNotNull null
                    PlanningObjectiveResult(
                        objectiveId = objectiveId,
                        outcome = outcome,
                        confidence = session.objectiveConfidence ?: 0.0,
                    )
                },
                learnerMemory = learnerMemory,
                currentMoment = moment,
                level = level,
                confirmedPracticalGoal = confirmedGoal?.description,
                tentativePracticalGoal = tentativeGoal?.description,
                priorAssignment = recent.lastOrNull()?.assignment?.takeIf(String::isNotBlank),
            ),
        )

        val vocabList = if (plan.kind == SessionObjectiveKind.VocabularyRetrieval) {
            plan.target ?: "(none selected for this session)"
        } else {
            "(none selected for this session)"
        }
        val selectedWeakArea = if (plan.kind == SessionObjectiveKind.ErrorPattern) {
            weak.firstOrNull { it.category.code == plan.target }
        } else {
            null
        }
        val weakList = selectedWeakArea?.let {
            "- ${it.category.name}: ${it.category.description}"
        } ?: "- (none selected for this session)"
        val recentList = if (recent.isEmpty()) "(none yet)"
            else recent.mapIndexedNotNull { i, s -> s.summary?.takeIf { it.isNotBlank() }?.let { "${i + 1}. $it" } }
                .joinToString("\n").ifBlank { "(none yet)" }

        val systemPrompt = TutorPrompt.build(
            TutorPromptContext(
                nativeLanguage = nativeLang,
                targetLanguage = targetLang,
                level = level,
                sessionPlan = plan.renderForPrompt(),
                learnerMemory = learnerMemory.ifBlank { "(no personal memory yet)" },
                dueVocabulary = vocabList,
                weakAreas = weakList,
                recentSessions = recentList,
                currentMoment = moment,
            ),
        )
        return PreparedSession(
            plan = plan,
            systemPrompt = systemPrompt,
            transcriptionLanguage = transcriptionLanguageCode(targetLang),
            nativeLanguage = nativeLang,
            targetLanguage = targetLang,
            level = level,
        )
    }

    suspend fun buildSystemPrompt(): String = prepareSession().systemPrompt

    /**
     * Returns the user's target language as an ISO 639-1 two-letter code, or null if it can't be
     * determined. Whisper accepts these codes ("es", "fr", etc.) as a hint to constrain language
     * detection. If [ProfilePrefs.targetLang] already looks like a code, it's returned lower-cased;
     * otherwise common English language names are mapped to codes.
     */
    suspend fun transcriptionLanguageCode(): String? {
        return transcriptionLanguageCode(profilePrefs.targetLang.first())
    }

    private fun transcriptionLanguageCode(language: String): String? {
        val raw = language.trim()
        if (raw.isEmpty()) return null
        if (raw.length == 2 && raw.all { it.isLetter() }) return raw.lowercase()
        return when (raw.lowercase()) {
            "english" -> "en"
            "spanish", "español", "espanol" -> "es"
            "french", "français", "francais" -> "fr"
            "german", "deutsch" -> "de"
            "italian", "italiano" -> "it"
            "portuguese", "português", "portugues" -> "pt"
            "russian", "русский" -> "ru"
            "japanese", "日本語" -> "ja"
            "chinese", "中文", "mandarin" -> "zh"
            else -> null
        }
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
