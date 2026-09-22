package com.xemniz.langcoach.data.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import androidx.room3.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PracticalGoalDao {
    @Insert
    suspend fun insert(goal: PracticalGoal): Long

    @Update
    suspend fun update(goal: PracticalGoal)

    @Query("UPDATE practical_goals SET description = '', evidenceTurnId = '', evidenceText = '', status = 'Rejected', updatedAt = :updatedAt WHERE id = :id")
    suspend fun scrub(id: Long, updatedAt: Long)

    @Query("SELECT * FROM practical_goals WHERE id = :id")
    suspend fun byId(id: Long): PracticalGoal?

    @Query("SELECT * FROM practical_goals WHERE sourceSessionId = :sourceSessionId ORDER BY id DESC LIMIT 1")
    suspend fun bySourceSessionId(sourceSessionId: Long): PracticalGoal?

    @Query("SELECT * FROM practical_goals WHERE targetLang = :targetLang AND status != 'Rejected' ORDER BY updatedAt DESC")
    suspend fun active(targetLang: String): List<PracticalGoal>

    @Query("SELECT * FROM practical_goals WHERE targetLang = :targetLang AND status = 'Tentative' ORDER BY updatedAt DESC LIMIT 1")
    suspend fun tentative(targetLang: String): PracticalGoal?

    @Query("SELECT * FROM practical_goals WHERE targetLang = :targetLang AND status = 'Confirmed' ORDER BY updatedAt DESC LIMIT 1")
    suspend fun confirmed(targetLang: String): PracticalGoal?

    @Query("UPDATE practical_goals SET status = 'Deferred', updatedAt = :updatedAt WHERE targetLang = :targetLang AND status = 'Confirmed' AND id != :exceptId")
    suspend fun deferOtherConfirmed(targetLang: String, exceptId: Long, updatedAt: Long)

    @Query("UPDATE practical_goals SET status = :status, updatedAt = :updatedAt WHERE id = :id AND status = 'Tentative'")
    suspend fun updateTentativeStatus(
        id: Long,
        status: PracticalGoalStatus,
        updatedAt: Long,
    ): Int

    @Query("UPDATE practical_goals SET status = 'Confirmed', updatedAt = :updatedAt WHERE id = :id AND status != 'Rejected' AND description != ''")
    suspend fun confirmIfActive(id: Long, updatedAt: Long): Int

    @Query("UPDATE practical_goals SET description = :description, updatedAt = :updatedAt WHERE id = :id AND status != 'Rejected'")
    suspend fun editIfActive(id: Long, description: String, updatedAt: Long): Int

    @Transaction
    suspend fun confirm(id: Long, targetLang: String, updatedAt: Long): Int {
        val changed = confirmIfActive(id, updatedAt)
        if (changed == 1) deferOtherConfirmed(targetLang, id, updatedAt)
        return changed
    }

    @Transaction
    suspend fun confirmTentative(id: Long, targetLang: String, updatedAt: Long): Int {
        val changed = updateTentativeStatus(id, PracticalGoalStatus.Confirmed, updatedAt)
        if (changed == 1) deferOtherConfirmed(targetLang, id, updatedAt)
        return changed
    }

    @Query("SELECT * FROM practical_goals ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<PracticalGoal>>

    @Query("SELECT * FROM practical_goals ORDER BY updatedAt DESC")
    suspend fun all(): List<PracticalGoal>
}
