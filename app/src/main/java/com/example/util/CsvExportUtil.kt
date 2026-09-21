package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.EmployeeEntity
import com.example.ui.viewmodel.EmployeeSalarySummary
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExportUtil {

    private fun escapeCsv(value: Any?): String {
        if (value == null) return "\"\""
        val str = value.toString()
        val escaped = str.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    /**
     * Generates CSV content for Monthly Attendance Report.
     * Displays Employee Info, Month, and detailed counts of Present, Half Day, Absent, Leave,
     * and Overtime Hours, plus Day-by-Day status breakdown.
     */
    fun generateMonthlyAttendanceCsv(
        monthStr: String,
        companyName: String,
        employees: List<EmployeeEntity>,
        attendanceList: List<AttendanceEntity>
    ): String {
        val sb = StringBuilder()
        // Header info
        sb.append(escapeCsv("ALFA GLAZING - MONTHLY ATTENDANCE REPORT")).append("\n")
        sb.append(escapeCsv("Company: $companyName")).append(",").append(escapeCsv("Month: $monthStr")).append("\n")
        sb.append(escapeCsv("Generated At: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")).append("\n\n")

        // Table Columns
        sb.append(escapeCsv("Sr No")).append(",")
            .append(escapeCsv("Employee Code")).append(",")
            .append(escapeCsv("Employee Name")).append(",")
            .append(escapeCsv("Designation")).append(",")
            .append(escapeCsv("Phone")).append(",")
            .append(escapeCsv("Present Days")).append(",")
            .append(escapeCsv("Half Days")).append(",")
            .append(escapeCsv("Absent Days")).append(",")
            .append(escapeCsv("Leave Days")).append(",")
            .append(escapeCsv("Total Effective Days")).append(",")
            .append(escapeCsv("Total OT Hours")).append("\n")

        // Group attendance by employee
        val monthAttendance = attendanceList.filter { it.dateString.startsWith(monthStr) }
        val attByEmp = monthAttendance.groupBy { it.employeeId }

        employees.forEachIndexed { index, emp ->
            val empAtt = attByEmp[emp.id] ?: emptyList()
            var presentDays = 0
            var halfDays = 0
            var absentDays = 0
            var leaveDays = 0
            var otHours = 0.0

            empAtt.forEach { rec ->
                when (rec.status.uppercase(Locale.getDefault())) {
                    "PRESENT" -> presentDays++
                    "HALF_DAY" -> halfDays++
                    "ABSENT" -> absentDays++
                    "LEAVE" -> leaveDays++
                }
                otHours += rec.overtimeHours
            }

            val totalEffectiveDays = presentDays + (halfDays * 0.5)

            sb.append(escapeCsv(index + 1)).append(",")
                .append(escapeCsv(emp.employeeCode)).append(",")
                .append(escapeCsv(emp.name)).append(",")
                .append(escapeCsv(emp.designation)).append(",")
                .append(escapeCsv(emp.phone)).append(",")
                .append(escapeCsv(presentDays)).append(",")
                .append(escapeCsv(halfDays)).append(",")
                .append(escapeCsv(absentDays)).append(",")
                .append(escapeCsv(leaveDays)).append(",")
                .append(escapeCsv(totalEffectiveDays)).append(",")
                .append(escapeCsv(otHours)).append("\n")
        }

        // Section 2: Detailed Day-by-Day Attendance Log
        sb.append("\n\n").append(escapeCsv("--- DAILY ATTENDANCE DETAILED LOG ($monthStr) ---")).append("\n")
        sb.append(escapeCsv("Date")).append(",")
            .append(escapeCsv("Employee Code")).append(",")
            .append(escapeCsv("Employee Name")).append(",")
            .append(escapeCsv("Status")).append(",")
            .append(escapeCsv("OT Hours")).append(",")
            .append(escapeCsv("Recorded Time")).append(",")
            .append(escapeCsv("Remarks")).append("\n")

        val sortedRecords = monthAttendance.sortedWith(compareBy({ it.dateString }, { it.employeeId }))
        val empMap = employees.associateBy { it.id }
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

        sortedRecords.forEach { rec ->
            val emp = empMap[rec.employeeId]
            val recordedTime = try {
                timeFormat.format(Date(rec.timestamp))
            } catch (e: Exception) {
                ""
            }

            sb.append(escapeCsv(rec.dateString)).append(",")
                .append(escapeCsv(emp?.employeeCode ?: "EMP#${rec.employeeId}")).append(",")
                .append(escapeCsv(emp?.name ?: "Unknown")).append(",")
                .append(escapeCsv(rec.status)).append(",")
                .append(escapeCsv(rec.overtimeHours)).append(",")
                .append(escapeCsv(recordedTime)).append(",")
                .append(escapeCsv(rec.remarks)).append("\n")
        }

        return sb.toString()
    }

    /**
     * Generates CSV content for Monthly Salary & Payroll Report.
     */
    fun generateMonthlySalaryCsv(
        monthStr: String,
        companyName: String,
        currencySymbol: String,
        salarySummaries: List<EmployeeSalarySummary>
    ): String {
        val sb = StringBuilder()
        // Header info
        sb.append(escapeCsv("ALFA GLAZING - MONTHLY SALARY & PAYROLL REPORT")).append("\n")
        sb.append(escapeCsv("Company: $companyName")).append(",").append(escapeCsv("Month: $monthStr")).append("\n")
        sb.append(escapeCsv("Currency: $currencySymbol")).append(",")
            .append(escapeCsv("Generated At: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")).append("\n\n")

        // Table Columns
        sb.append(escapeCsv("Sr No")).append(",")
            .append(escapeCsv("Employee Code")).append(",")
            .append(escapeCsv("Employee Name")).append(",")
            .append(escapeCsv("Designation")).append(",")
            .append(escapeCsv("Daily Wage Rate ($currencySymbol)")).append(",")
            .append(escapeCsv("OT Rate/Hour ($currencySymbol)")).append(",")
            .append(escapeCsv("Days Worked")).append(",")
            .append(escapeCsv("OT Hours")).append(",")
            .append(escapeCsv("Earned Basic Wage ($currencySymbol)")).append(",")
            .append(escapeCsv("Overtime Pay ($currencySymbol)")).append(",")
            .append(escapeCsv("Bonus ($currencySymbol)")).append(",")
            .append(escapeCsv("Gross Salary ($currencySymbol)")).append(",")
            .append(escapeCsv("Advance Deducted ($currencySymbol)")).append(",")
            .append(escapeCsv("Net Payable ($currencySymbol)")).append(",")
            .append(escapeCsv("Payment Status")).append("\n")

        var grandTotalGross = 0.0
        var grandTotalAdvance = 0.0
        var grandTotalNet = 0.0

        salarySummaries.forEachIndexed { index, s ->
            val gross = s.earnedBasicWage + s.overtimePay + s.bonusAmount
            grandTotalGross += gross
            grandTotalAdvance += s.totalAdvanceDeducted
            grandTotalNet += s.netSalaryPayable

            sb.append(escapeCsv(index + 1)).append(",")
                .append(escapeCsv(s.employee.employeeCode)).append(",")
                .append(escapeCsv(s.employee.name)).append(",")
                .append(escapeCsv(s.employee.designation)).append(",")
                .append(escapeCsv(s.dailyWage)).append(",")
                .append(escapeCsv(s.employee.overtimeRatePerHour)).append(",")
                .append(escapeCsv(s.presentDays)).append(",")
                .append(escapeCsv(s.overtimeHours)).append(",")
                .append(escapeCsv(s.earnedBasicWage)).append(",")
                .append(escapeCsv(s.overtimePay)).append(",")
                .append(escapeCsv(s.bonusAmount)).append(",")
                .append(escapeCsv(gross)).append(",")
                .append(escapeCsv(s.totalAdvanceDeducted)).append(",")
                .append(escapeCsv(s.netSalaryPayable)).append(",")
                .append(escapeCsv(s.paymentStatus)).append("\n")
        }

        // Totals Row
        sb.append(escapeCsv("TOTALS")).append(",")
            .append(escapeCsv("")).append(",")
            .append(escapeCsv("Total Workers: ${salarySummaries.size}")).append(",")
            .append(escapeCsv("")).append(",")
            .append(escapeCsv("")).append(",")
            .append(escapeCsv("")).append(",")
            .append(escapeCsv("")).append(",")
            .append(escapeCsv("")).append(",")
            .append(escapeCsv("")).append(",")
            .append(escapeCsv("")).append(",")
            .append(escapeCsv("")).append(",")
            .append(escapeCsv(grandTotalGross)).append(",")
            .append(escapeCsv(grandTotalAdvance)).append(",")
            .append(escapeCsv(grandTotalNet)).append(",")
            .append(escapeCsv("")).append("\n")

        return sb.toString()
    }

    /**
     * Saves CSV file to device storage (Downloads or App Documents directory)
     * and returns the file location / URI result for opening or sharing.
     */
    fun saveCsvToStorage(
        context: Context,
        fileName: String,
        csvContent: String
    ): Pair<Uri?, String> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // MediaStore Downloads for Android 10+ (Scoped Storage safe, no permission needed)
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/AlfaGlazing")
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream: OutputStream ->
                        outputStream.write(csvContent.toByteArray(Charsets.UTF_8))
                    }
                    Pair(uri, "Saved to Downloads/AlfaGlazing/$fileName")
                } else {
                    // Fallback to app-specific external files
                    saveToAppSpecificStorage(context, fileName, csvContent)
                }
            } else {
                saveToAppSpecificStorage(context, fileName, csvContent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to internal storage / cache with FileProvider
            saveToAppSpecificStorage(context, fileName, csvContent)
        }
    }

    private fun saveToAppSpecificStorage(
        context: Context,
        fileName: String,
        csvContent: String
    ): Pair<Uri?, String> {
        return try {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            val file = File(dir, fileName)
            FileOutputStream(file).use { fos ->
                fos.write(csvContent.toByteArray(Charsets.UTF_8))
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            Pair(uri, "Saved to ${file.absolutePath}")
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(null, "Failed to save: ${e.localizedMessage}")
        }
    }

    /**
     * Creates an Intent to share or open the generated CSV file.
     */
    fun shareCsvFile(
        context: Context,
        uri: Uri,
        title: String
    ) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(sendIntent, "Share $title via")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /**
     * Creates an Intent to open or view the CSV file in an external viewer (e.g., Google Sheets, Excel).
     */
    fun openCsvFile(
        context: Context,
        uri: Uri
    ) {
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "text/csv")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(viewIntent, "Open CSV with")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
