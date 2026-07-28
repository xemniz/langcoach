package com.xemniz.langcoach.domain.usecase

import com.xemniz.langcoach.data.repo.SessionRepo
import com.xemniz.langcoach.domain.session.SessionPlan
import kotlinx.datetime.Clock

class StartSession(private val sessionRepo: SessionRepo) {
    suspend operator fun invoke(
        plan: SessionPlan,
        atMillis: Long = Clock.System.now().toEpochMilliseconds(),
    ): Long = sessionRepo.create(
        startedAt = atMillis,
        planVersion = plan.version,
        objectiveId = plan.objectiveId,
        objectiveKind = plan.kind.name,
        objectiveDescription = plan.objective,
        objectiveTarget = plan.target,
        objectiveSuccessCriteria = plan.successCriteria,
    )
}
