package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "employees")
data class EmployeeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val employeeCode: String,
    val name: String,
    val phone: String,
    val designation: String,
    val dailyWage: Double,
    val overtimeRatePerHour: Double = 100.0,
    val joiningDate: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)
