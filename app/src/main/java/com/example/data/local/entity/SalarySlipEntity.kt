package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "salary_slips")
data class SalarySlipEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val employeeId: Long,
    val monthYear: String, // e.g. "2026-08"
    val totalPresentDays: Double,
    val totalAbsentDays: Int,
    val totalOvertimeHours: Double,
    val baseWageRate: Double,
    val totalEarnedWage: Double,
    val overtimeAmount: Double,
    val bonusAmount: Double = 0.0,
    val totalAdvanceDeducted: Double = 0.0,
    val netSalaryPaid: Double,
    val paymentStatus: String = "PAID", // "PAID", "PENDING", "PARTIAL"
    val paymentDate: Long = System.currentTimeMillis(),
    val paymentMethod: String = "CASH" // "CASH", "BANK_TRANSFER", "UPI"
)
