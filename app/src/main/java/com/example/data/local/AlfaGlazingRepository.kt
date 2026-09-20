package com.example.data.local

import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

class AlfaGlazingRepository(private val database: AppDatabase) {

    val employeesFlow: Flow<List<EmployeeEntity>> = database.employeeDao().getAllActiveEmployees()
    val allEmployeesFlow: Flow<List<EmployeeEntity>> = database.employeeDao().getAllEmployeesList()
    val companyProfileFlow: Flow<CompanyProfileEntity?> = database.companyProfileDao().getCompanyProfile()
    val geofenceService: GeofenceRepositoryService = GeofenceRepositoryService(companyProfileFlow)
    val allAttendanceFlow: Flow<List<AttendanceEntity>> = database.attendanceDao().getAllAttendanceRecords()
    val allAdvancesFlow: Flow<List<AdvancePaymentEntity>> = database.advancePaymentDao().getAllAdvances()
    val allSalarySlipsFlow: Flow<List<SalarySlipEntity>> = database.salarySlipDao().getAllSalarySlips()

    fun getAttendanceForDate(dateStr: String): Flow<List<AttendanceEntity>> {
        return database.attendanceDao().getAttendanceForDate(dateStr)
    }

    fun getAttendanceForMonth(monthPrefix: String): Flow<List<AttendanceEntity>> {
        return database.attendanceDao().getAttendanceForMonth(monthPrefix)
    }

    fun getAttendanceForEmployee(empId: Long): Flow<List<AttendanceEntity>> {
        return database.attendanceDao().getAttendanceForEmployee(empId)
    }

    fun getAdvancesForMonth(monthYear: String): Flow<List<AdvancePaymentEntity>> {
        return database.advancePaymentDao().getAdvancesForMonth(monthYear)
    }

    fun getSalarySlipsForMonth(monthYear: String): Flow<List<SalarySlipEntity>> {
        return database.salarySlipDao().getSalarySlipsForMonth(monthYear)
    }

    suspend fun getEmployeeById(empId: Long): EmployeeEntity? {
        return database.employeeDao().getEmployeeById(empId)
    }

    suspend fun getEmployeeByPhone(phoneDigits: String): EmployeeEntity? {
        return database.employeeDao().getEmployeeByPhone(phoneDigits)
    }

    suspend fun insertOrUpdateEmployee(employee: EmployeeEntity): Long {
        return database.employeeDao().insertEmployee(employee)
    }

    suspend fun updateEmployee(employee: EmployeeEntity) {
        database.employeeDao().updateEmployee(employee)
    }

    suspend fun deleteEmployee(employee: EmployeeEntity) {
        database.employeeDao().deleteEmployee(employee)
    }

    suspend fun markAttendance(attendance: AttendanceEntity): Long {
        return database.attendanceDao().insertOrUpdateAttendance(attendance)
    }

    suspend fun markBulkAttendance(list: List<AttendanceEntity>) {
        database.attendanceDao().insertAll(list)
    }

    suspend fun addAdvancePayment(advance: AdvancePaymentEntity): Long {
        return database.advancePaymentDao().insertAdvance(advance)
    }

    suspend fun saveSalarySlip(slip: SalarySlipEntity): Long {
        return database.salarySlipDao().insertSalarySlip(slip)
    }

    suspend fun saveSalarySlipsBulk(slips: List<SalarySlipEntity>) {
        database.salarySlipDao().insertAll(slips)
    }

    suspend fun updateCompanyProfile(profile: CompanyProfileEntity) {
        database.companyProfileDao().insertOrUpdateProfile(profile)
    }
}
