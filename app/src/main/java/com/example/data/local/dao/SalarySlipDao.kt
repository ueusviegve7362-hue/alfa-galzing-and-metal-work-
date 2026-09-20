package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.SalarySlipEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SalarySlipDao {
    @Query("SELECT * FROM salary_slips ORDER BY paymentDate DESC")
    fun getAllSalarySlips(): Flow<List<SalarySlipEntity>>

    @Query("SELECT * FROM salary_slips WHERE monthYear = :monthYear")
    fun getSalarySlipsForMonth(monthYear: String): Flow<List<SalarySlipEntity>>

    @Query("SELECT * FROM salary_slips WHERE employeeId = :empId ORDER BY paymentDate DESC")
    fun getSalarySlipsForEmployee(empId: Long): Flow<List<SalarySlipEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSalarySlip(slip: SalarySlipEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<SalarySlipEntity>)

    @Update
    suspend fun updateSalarySlip(slip: SalarySlipEntity)

    @Delete
    suspend fun deleteSalarySlip(slip: SalarySlipEntity)
}
