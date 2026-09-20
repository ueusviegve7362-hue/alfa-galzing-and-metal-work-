package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.AdvancePaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AdvancePaymentDao {
    @Query("SELECT * FROM advance_payments ORDER BY dateTimestamp DESC")
    fun getAllAdvances(): Flow<List<AdvancePaymentEntity>>

    @Query("SELECT * FROM advance_payments WHERE employeeId = :empId ORDER BY dateTimestamp DESC")
    fun getAdvancesForEmployee(empId: Long): Flow<List<AdvancePaymentEntity>>

    @Query("SELECT * FROM advance_payments WHERE monthYear = :monthYear")
    fun getAdvancesForMonth(monthYear: String): Flow<List<AdvancePaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdvance(advance: AdvancePaymentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<AdvancePaymentEntity>)

    @Delete
    suspend fun deleteAdvance(advance: AdvancePaymentEntity)
}
