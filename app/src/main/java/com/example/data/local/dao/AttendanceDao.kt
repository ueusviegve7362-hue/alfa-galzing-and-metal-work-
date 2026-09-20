package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.AttendanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance WHERE dateString = :dateStr")
    fun getAttendanceForDate(dateStr: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE employeeId = :empId ORDER BY timestamp DESC")
    fun getAttendanceForEmployee(empId: Long): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE dateString LIKE :monthPrefix || '%'")
    fun getAttendanceForMonth(monthPrefix: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE employeeId = :empId AND dateString = :dateStr LIMIT 1")
    suspend fun getRecordForEmpAndDate(empId: Long, dateStr: String): AttendanceEntity?

    @Query("SELECT * FROM attendance")
    fun getAllAttendanceRecords(): Flow<List<AttendanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAttendance(attendance: AttendanceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<AttendanceEntity>)

    @Delete
    suspend fun deleteAttendance(attendance: AttendanceEntity)
}
