package com.xemniz.langcoach.domain.usecase

import com.xemniz.langcoach.data.repo.SessionRepo
import com.xemniz.langcoach.domain.session.PreparedSession
import kotlinx.datetime.Clock

class StartSession(private val sessionRepo: SessionRepo) {
    suspend operator fun invoke(
        preparedSession: PreparedSession,
        atMillis: Long = Clock.System.now().toEpochMilliseconds(),
    ): Long {
        val plan = preparedSession.plan
        return sessionRepo.create(
            startedAt = atMillis,
            planVersion = plan.version,
            objectiveId = plan.objectiveId,
            objectiveKind = plan.kind.name,
            objectiveDescription = plan.objective,
            objectiveTarget = plan.target,
            objectiveSuccessCriteria = plan.successCriteria,
            tentativePracticalGoalId = preparedSession.tentativePracticalGoalId,
            nativeLang = preparedSession.nativeLanguage,
            targetLang = preparedSession.targetLanguage,
            level = preparedSession.level,
        )
    }
}
