package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "advance_payments")
data class AdvancePaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val employeeId: Long,
    val amount: Double,
    val dateTimestamp: Long = System.currentTimeMillis(),
    val note: String = "",
    val monthYear: String // e.g. "2026-08"
)
