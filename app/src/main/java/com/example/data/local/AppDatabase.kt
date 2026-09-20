package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.*
import com.example.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Database(
    entities = [
        EmployeeEntity::class,
        AttendanceEntity::class,
        AdvancePaymentEntity::class,
        SalarySlipEntity::class,
        CompanyProfileEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun employeeDao(): EmployeeDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun advancePaymentDao(): AdvancePaymentDao
    abstract fun salarySlipDao(): SalarySlipDao
    abstract fun companyProfileDao(): CompanyProfileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "alfa_glazing_db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(context.applicationContext))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                runCatching {
                    val database = getInstance(context)
                    seedInitialData(database)
                }.onFailure { e ->
                    android.util.Log.e("AppDatabase", "Error seeding database", e)
                }
            }
        }

        private suspend fun seedInitialData(db: AppDatabase) {
            // Seed Company Profile
            db.companyProfileDao().insertOrUpdateProfile(
                CompanyProfileEntity(
                    id = 1,
                    companyName = "ALFA GLAZING",
                    contractorName = "Alfa Glazing Operations Manager",
                    phone = "+91 98765 43210",
                    location = "Site #42 - Commercial Tower Project",
                    currencySymbol = "₹",
                    defaultOvertimeRate = 100.0,
                    isDarkMode = false,
                    currentUserRole = "ADMIN",
                    adminSecurityPin = "805281",
                    securityPin = "805281",
                    loggedInMobileNumber = "+91 9811100001",
                    loggedInEmail = "admin@alfaglazing.com",
                    isGeofenceEnabled = true,
                    jobSiteLatitude = 28.6139,
                    jobSiteLongitude = 77.2090,
                    geofenceRadiusMeters = 200,
                    jobSiteAddressName = "Site #42 - Commercial Tower Project"
                )
            )

            // Seed 30 Employees
            val employeesList = listOf(
                EmployeeEntity(id = 1, employeeCode = "AG-001", name = "Ramesh Kumar", phone = "+91 9811100001", designation = "Master Glazier", dailyWage = 950.0, overtimeRatePerHour = 120.0),
                EmployeeEntity(id = 2, employeeCode = "AG-002", name = "Suresh Sharma", phone = "+91 9811100002", designation = "Aluminum Fabricator", dailyWage = 850.0, overtimeRatePerHour = 100.0),
                EmployeeEntity(id = 3, employeeCode = "AG-003", name = "Abdul Karim", phone = "+91 9811100003", designation = "Glass Fitter", dailyWage = 800.0, overtimeRatePerHour = 100.0),
                EmployeeEntity(id = 4, employeeCode = "AG-004", name = "Rajesh Verma", phone = "+91 9811100004", designation = "Structural Glazier", dailyWage = 900.0, overtimeRatePerHour = 110.0),
                EmployeeEntity(id = 5, employeeCode = "AG-005", name = "Mohammad Rashid", phone = "+91 9811100005", designation = "Crane Operator", dailyWage = 1000.0, overtimeRatePerHour = 130.0),
                EmployeeEntity(id = 6, employeeCode = "AG-006", name = "Amit Patel", phone = "+91 9811100006", designation = "Senior Helper", dailyWage = 650.0, overtimeRatePerHour = 80.0),
                EmployeeEntity(id = 7, employeeCode = "AG-007", name = "Vikas Singh", phone = "+91 9811100007", designation = "Aluminum Fabricator", dailyWage = 850.0, overtimeRatePerHour = 100.0),
                EmployeeEntity(id = 8, employeeCode = "AG-008", name = "Deepak Gupta", phone = "+91 9811100008", designation = "Glass Fitter", dailyWage = 800.0, overtimeRatePerHour = 100.0),
                EmployeeEntity(id = 9, employeeCode = "AG-009", name = "Imran Khan", phone = "+91 9811100009", designation = "Master Glazier", dailyWage = 950.0, overtimeRatePerHour = 120.0),
                EmployeeEntity(id = 10, employeeCode = "AG-010", name = "Sunil Yadav", phone = "+91 9811100010", designation = "Site Supervisor", dailyWage = 1100.0, overtimeRatePerHour = 150.0),
                EmployeeEntity(id = 11, employeeCode = "AG-011", name = "Rahul Mishra", phone = "+91 9811100011", designation = "Glass Fitter", dailyWage = 800.0, overtimeRatePerHour = 100.0),
                EmployeeEntity(id = 12, employeeCode = "AG-012", name = "Manoj Kumar", phone = "+91 9811100012", designation = "Site Helper", dailyWage = 600.0, overtimeRatePerHour = 75.0),
                EmployeeEntity(id = 13, employeeCode = "AG-013", name = "Vijay Shah", phone = "+91 9811100013", designation = "Aluminum Fabricator", dailyWage = 850.0, overtimeRatePerHour = 100.0),
                EmployeeEntity(id = 14, employeeCode = "AG-014", name = "Sanjay Prasad", phone = "+91 9811100014", designation = "Assistant Glazier", dailyWage = 750.0, overtimeRatePerHour = 90.0),
                EmployeeEntity(id = 15, employeeCode = "AG-015", name = "Anil Chaudhari", phone = "+91 9811100015", designation = "Glass Cutter", dailyWage = 850.0, overtimeRatePerHour = 100.0),
                EmployeeEntity(id = 16, employeeCode = "AG-016", name = "Tariq Mahmood", phone = "+91 9811100016", designation = "Structural Glazier", dailyWage = 900.0, overtimeRatePerHour = 110.0),
                EmployeeEntity(id = 17, employeeCode = "AG-017", name = "Dinesh Carpenter", phone = "+91 9811100017", designation = "Frame Specialist", dailyWage = 850.0, overtimeRatePerHour = 100.0),
                EmployeeEntity(id = 18, employeeCode = "AG-018", name = "Ajay Thapa", phone = "+91 9811100018", designation = "Senior Helper", dailyWage = 650.0, overtimeRatePerHour = 80.0),
                EmployeeEntity(id = 19, employeeCode = "AG-019", name = "Santosh Das", phone = "+91 9811100019", designation = "Glass Fitter", dailyWage = 800.0, overtimeRatePerHour = 100.0),
                EmployeeEntity(id = 20, employeeCode = "AG-020", name = "Gopal Reddy", phone = "+91 9811100020", designation = "Safety Inspector", dailyWage = 1050.0, overtimeRatePerHour = 140.0),
                EmployeeEntity(id = 21, employeeCode = "AG-021", name = "Mahesh Shinde", phone = "+91 9811100021", designation = "Sealant Specialist", dailyWage = 800.0, overtimeRatePerHour = 100.0),
                EmployeeEntity(id = 22, employeeCode = "AG-022", name = "Prakash Naik", phone = "+91 9811100022", designation = "Site Helper", dailyWage = 600.0, overtimeRatePerHour = 75.0),
                EmployeeEntity(id = 23, employeeCode = "AG-023", name = "Salim Sheikh", phone = "+91 9811100023", designation = "Aluminum Fabricator", dailyWage = 850.0, overtimeRatePerHour = 100.0),
                EmployeeEntity(id = 24, employeeCode = "AG-024", name = "Pankaj Tiwari", phone = "+91 9811100024", designation = "Assistant Glazier", dailyWage = 750.0, overtimeRatePerHour = 90.0),
                EmployeeEntity(id = 25, employeeCode = "AG-025", name = "Rakesh Yadav", phone = "+91 9811100025", designation = "Glass Fitter", dailyWage = 800.0, overtimeRatePerHour = 100.0),
                EmployeeEntity(id = 26, employeeCode = "AG-026", name = "Mukesh Bhatia", phone = "+91 9811100026", designation = "Crane Operator", dailyWage = 1000.0, overtimeRatePerHour = 130.0),
                EmployeeEntity(id = 27, employeeCode = "AG-027", name = "Ashok Meena", phone = "+91 9811100027", designation = "Site Helper", dailyWage = 600.0, overtimeRatePerHour = 75.0),
                EmployeeEntity(id = 28, employeeCode = "AG-028", name = "Nitin Deshmukh", phone = "+91 9811100028", designation = "Master Glazier", dailyWage = 950.0, overtimeRatePerHour = 120.0),
                EmployeeEntity(id = 29, employeeCode = "AG-029", name = "Bilal Ahmad", phone = "+91 9811100029", designation = "Structural Glazier", dailyWage = 900.0, overtimeRatePerHour = 110.0),
                EmployeeEntity(id = 30, employeeCode = "AG-030", name = "Arvind Saini", phone = "+91 9811100030", designation = "Site Supervisor", dailyWage = 1100.0, overtimeRatePerHour = 150.0)
            )

            db.employeeDao().insertAll(employeesList)

            // Seed sample attendance for the last 5 days
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val calendar = Calendar.getInstance()
            val nowMs = System.currentTimeMillis()

            val attendanceList = mutableListOf<AttendanceEntity>()
            val statuses = listOf("PRESENT", "PRESENT", "PRESENT", "HALF_DAY", "PRESENT", "PRESENT", "ABSENT")

            for (dayOffset in 0..6) {
                calendar.timeInMillis = nowMs - (dayOffset * 86400000L)
                val dateStr = sdf.format(calendar.time)

                employeesList.forEachIndexed { idx, emp ->
                    val status = statuses[(idx + dayOffset) % statuses.size]
                    val ot = if (status == "PRESENT" && idx % 3 == 0) 2.0 else 0.0
                    attendanceList.add(
                        AttendanceEntity(
                            employeeId = emp.id,
                            dateString = dateStr,
                            status = status,
                            overtimeHours = ot,
                            remarks = if (ot > 0) "Glazing facade OT" else "",
                            timestamp = calendar.timeInMillis
                        )
                    )
                }
            }
            db.attendanceDao().insertAll(attendanceList)

            // Seed sample advance payments
            val monthYearStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
            val advances = listOf(
                AdvancePaymentEntity(employeeId = 3, amount = 2000.0, note = "Personal emergency advance", monthYear = monthYearStr),
                AdvancePaymentEntity(employeeId = 6, amount = 1000.0, note = "Medical cash advance", monthYear = monthYearStr),
                AdvancePaymentEntity(employeeId = 12, amount = 1500.0, note = "Festival travel advance", monthYear = monthYearStr)
            )
            db.advancePaymentDao().insertAll(advances)
        }
    }
}
