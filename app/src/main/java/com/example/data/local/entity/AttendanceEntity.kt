package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance")
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val employeeId: Long,
    val dateString: String, // e.g. "2026-08-09"
    val status: String, // "PRESENT", "ABSENT", "HALF_DAY", "LEAVE"
    val overtimeHours: Double = 0.0,
    val remarks: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
