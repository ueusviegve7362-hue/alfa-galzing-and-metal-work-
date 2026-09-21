package com.example.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.widget.Toast
import com.example.data.local.entity.AdvancePaymentEntity
import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.CompanyProfileEntity
import com.example.data.local.entity.EmployeeEntity
import com.example.data.local.entity.SalarySlipEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// ============================================================================
// GITHUB BACKEND CONFIGURATION
// Add a setup section or variables at the top of the script for:
// GITHUB_USERNAME, REPO_NAME, GITHUB_TOKEN
// ============================================================================
var GITHUB_USERNAME: String = "YOUR_GITHUB_USERNAME"
var REPO_NAME: String = "YOUR_REPO_NAME"
var GITHUB_TOKEN: String = "YOUR_GITHUB_PERSONAL_ACCESS_TOKEN"

/**
 * Data container representing the contents of data.json in the GitHub repository.
 */
data class GitHubDatabasePayload(
    val employees: List<EmployeeEntity> = emptyList(),
    val attendance: List<AttendanceEntity> = emptyList(),
    val advances: List<AdvancePaymentEntity> = emptyList(),
    val salarySlips: List<SalarySlipEntity> = emptyList(),
    val payments: List<Map<String, Any?>> = emptyList(),
    val companyProfile: CompanyProfileEntity? = null,
    val lastUpdatedTimestamp: Long = System.currentTimeMillis()
)

/**
 * Service to manage backend database operations using GitHub REST API.
 * Reads data from raw.githubusercontent.com and updates data.json via GitHub Contents API.
 */
class GitHubSyncService(private val context: Context) {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val prefs = context.getSharedPreferences("alfa_github_config", Context.MODE_PRIVATE)

    init {
        // Load saved values from persistent preferences if configured
        val savedUser = prefs.getString("github_username", "")
        if (!savedUser.isNullOrBlank()) {
            GITHUB_USERNAME = savedUser
        }
        val savedRepo = prefs.getString("github_repo", "")
        if (!savedRepo.isNullOrBlank()) {
            REPO_NAME = savedRepo
        }
        val savedToken = prefs.getString("github_token", "")
        if (!savedToken.isNullOrBlank()) {
            GITHUB_TOKEN = savedToken
        }
    }

    fun saveGitHubConfig(username: String, repo: String, token: String) {
        GITHUB_USERNAME = username.trim()
        REPO_NAME = repo.trim()
        GITHUB_TOKEN = token.trim()

        prefs.edit()
            .putString("github_username", GITHUB_USERNAME)
            .putString("github_repo", REPO_NAME)
            .putString("github_token", GITHUB_TOKEN)
            .apply()
    }

    fun getRawDataUrl(): String {
        return "https://raw.githubusercontent.com/$GITHUB_USERNAME/$REPO_NAME/main/data.json"
    }

    fun getContentsApiUrl(): String {
        return "https://api.github.com/repos/$GITHUB_USERNAME/$REPO_NAME/contents/data.json"
    }

    private fun isConfigured(): Boolean {
        return GITHUB_USERNAME.isNotBlank() &&
                GITHUB_USERNAME != "YOUR_GITHUB_USERNAME" &&
                REPO_NAME.isNotBlank() &&
                REPO_NAME != "YOUR_REPO_NAME"
    }

    /**
     * Fetches the latest data.json from raw.githubusercontent.com
     */
    suspend fun fetchLatestData(): GitHubDatabasePayload? = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            Log.d(TAG, "GitHub configuration not set. Skipping remote raw data fetch.")
            return@withContext null
        }

        val url = getRawDataUrl()
        val requestBuilder = Request.Builder()
            .url(url)
            .get()

        if (GITHUB_TOKEN.isNotBlank() && GITHUB_TOKEN != "YOUR_GITHUB_PERSONAL_ACCESS_TOKEN") {
            requestBuilder.addHeader("Authorization", "Bearer $GITHUB_TOKEN")
        }

        try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Failed to fetch raw data.json: HTTP ${response.code}")
                    return@withContext null
                }

                val bodyString = response.body?.string() ?: return@withContext null
                return@withContext parseJsonToPayload(bodyString)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching raw data from GitHub: ${e.message}", e)
            return@withContext null
        }
    }

    /**
     * Retrieves the current SHA of data.json from GitHub Contents API.
     * Needed for PUT requests when updating existing files.
     */
    private suspend fun getCurrentFileSha(): String? = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext null

        val url = getContentsApiUrl()
        val requestBuilder = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github.v3+json")
            .get()

        if (GITHUB_TOKEN.isNotBlank() && GITHUB_TOKEN != "YOUR_GITHUB_PERSONAL_ACCESS_TOKEN") {
            requestBuilder.addHeader("Authorization", "Bearer $GITHUB_TOKEN")
        }

        try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                if (response.isSuccessful) {
                    val json = JSONObject(response.body?.string() ?: "{}")
                    return@withContext json.optString("sha", null)
                } else if (response.code == 404) {
                    return@withContext null // File does not exist yet
                }
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not fetch current file SHA: ${e.message}")
            null
        }
    }

    /**
     * Writes updated data to data.json in the GitHub repository using PUT request.
     */
    suspend fun updateGitHubData(
        payload: GitHubDatabasePayload,
        commitMessage: String = "Update data.json from ALFA GLAZING app",
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            val msg = "GitHub backend not fully configured (set Username, Repo, and Token in Settings)"
            Log.w(TAG, msg)
            onError?.invoke(msg)
            return@withContext false
        }

        if (GITHUB_TOKEN.isBlank() || GITHUB_TOKEN == "YOUR_GITHUB_PERSONAL_ACCESS_TOKEN") {
            val msg = "GitHub Personal Access Token is missing. Configure it in Settings."
            Log.e(TAG, msg)
            showToast(msg)
            onError?.invoke(msg)
            return@withContext false
        }

        try {
            val jsonString = serializePayloadToJson(payload)
            val base64Content = Base64.encodeToString(
                jsonString.toByteArray(StandardCharsets.UTF_8),
                Base64.NO_WRAP
            )

            val currentSha = getCurrentFileSha()

            val requestJson = JSONObject().apply {
                put("message", commitMessage)
                put("content", base64Content)
                if (!currentSha.isNullOrBlank()) {
                    put("sha", currentSha)
                }
            }

            val requestBody = requestJson.toString()
                .toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url(getContentsApiUrl())
                .addHeader("Accept", "application/vnd.github.v3+json")
                .addHeader("Authorization", "Bearer $GITHUB_TOKEN")
                .put(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d(TAG, "Successfully synced data.json to GitHub! Code: ${response.code}")
                    showToast("GitHub Sync: data.json updated successfully!")
                    withContext(Dispatchers.Main) {
                        onSuccess?.invoke()
                    }
                    return@withContext true
                } else {
                    val errBody = response.body?.string() ?: ""
                    val errMsg = "GitHub Sync Failed (HTTP ${response.code}): $errBody"
                    Log.e(TAG, errMsg)
                    showToast("GitHub Sync Failed: HTTP ${response.code}")
                    withContext(Dispatchers.Main) {
                        onError?.invoke(errMsg)
                    }
                    return@withContext false
                }
            }
        } catch (e: Exception) {
            val errMsg = "Network exception during GitHub Sync: ${e.message}"
            Log.e(TAG, errMsg, e)
            showToast("GitHub Sync Error: ${e.localizedMessage}")
            withContext(Dispatchers.Main) {
                onError?.invoke(errMsg)
            }
            return@withContext false
        }
    }

    /**
     * Converts a database payload into formatted JSON string for data.json
     */
    fun serializePayloadToJson(payload: GitHubDatabasePayload): String {
        val root = JSONObject()

        // Employees Array
        val empArray = JSONArray()
        payload.employees.forEach { emp ->
            val obj = JSONObject().apply {
                put("id", emp.id)
                put("employeeCode", emp.employeeCode)
                put("name", emp.name)
                put("phone", emp.phone)
                put("designation", emp.designation)
                put("dailyWage", emp.dailyWage)
                put("overtimeRatePerHour", emp.overtimeRatePerHour)
                put("joiningDate", emp.joiningDate)
                put("isActive", emp.isActive)
            }
            empArray.put(obj)
        }
        root.put("employees", empArray)

        // Attendance Array
        val attArray = JSONArray()
        payload.attendance.forEach { att ->
            val obj = JSONObject().apply {
                put("id", att.id)
                put("employeeId", att.employeeId)
                put("dateString", att.dateString)
                put("status", att.status)
                put("overtimeHours", att.overtimeHours)
                put("remarks", att.remarks)
                put("timestamp", att.timestamp)
            }
            attArray.put(obj)
        }
        root.put("attendance", attArray)

        // Advance Payments Array
        val advArray = JSONArray()
        payload.advances.forEach { adv ->
            val obj = JSONObject().apply {
                put("id", adv.id)
                put("employeeId", adv.employeeId)
                put("amount", adv.amount)
                put("dateTimestamp", adv.dateTimestamp)
                put("note", adv.note)
                put("monthYear", adv.monthYear)
            }
            advArray.put(obj)
        }
        root.put("advances", advArray)

        // Salary Slips Array
        val slipArray = JSONArray()
        payload.salarySlips.forEach { slip ->
            val obj = JSONObject().apply {
                put("id", slip.id)
                put("employeeId", slip.employeeId)
                put("monthYear", slip.monthYear)
                put("totalPresentDays", slip.totalPresentDays)
                put("totalAbsentDays", slip.totalAbsentDays)
                put("totalOvertimeHours", slip.totalOvertimeHours)
                put("baseWageRate", slip.baseWageRate)
                put("totalEarnedWage", slip.totalEarnedWage)
                put("overtimeAmount", slip.overtimeAmount)
                put("bonusAmount", slip.bonusAmount)
                put("totalAdvanceDeducted", slip.totalAdvanceDeducted)
                put("netSalaryPaid", slip.netSalaryPaid)
                put("paymentStatus", slip.paymentStatus)
                put("paymentDate", slip.paymentDate)
                put("paymentMethod", slip.paymentMethod)
            }
            slipArray.put(obj)
        }
        root.put("salarySlips", slipArray)

        // Raw Payments history
        val payArray = JSONArray()
        payload.payments.forEach { pay ->
            payArray.put(JSONObject(pay))
        }
        root.put("payments", payArray)

        // Company Profile
        payload.companyProfile?.let { prof ->
            val profObj = JSONObject().apply {
                put("id", prof.id)
                put("companyName", prof.companyName)
                put("contractorName", prof.contractorName)
                put("phone", prof.phone)
                put("location", prof.location)
                put("currencySymbol", prof.currencySymbol)
                put("defaultOvertimeRate", prof.defaultOvertimeRate)
                put("securityPin", prof.securityPin)
                put("currentUserRole", prof.currentUserRole)
                put("jobSiteAddressName", prof.jobSiteAddressName)
            }
            root.put("companyProfile", profObj)
        }

        root.put("lastUpdatedTimestamp", System.currentTimeMillis())
        root.put("lastUpdatedFormatted", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))

        return root.toString(2)
    }

    /**
     * Parses JSON string into a structured GitHubDatabasePayload
     */
    fun parseJsonToPayload(jsonStr: String): GitHubDatabasePayload {
        val root = JSONObject(jsonStr)

        val employees = mutableListOf<EmployeeEntity>()
        val empArray = root.optJSONArray("employees")
        if (empArray != null) {
            for (i in 0 until empArray.length()) {
                val obj = empArray.getJSONObject(i)
                employees.add(
                    EmployeeEntity(
                        id = obj.optLong("id", 0L),
                        employeeCode = obj.optString("employeeCode", "AG-000"),
                        name = obj.optString("name", "Worker"),
                        phone = obj.optString("phone", ""),
                        designation = obj.optString("designation", "Worker"),
                        dailyWage = obj.optDouble("dailyWage", 600.0),
                        overtimeRatePerHour = obj.optDouble("overtimeRatePerHour", 100.0),
                        joiningDate = obj.optLong("joiningDate", System.currentTimeMillis()),
                        isActive = obj.optBoolean("isActive", true)
                    )
                )
            }
        }

        val attendance = mutableListOf<AttendanceEntity>()
        val attArray = root.optJSONArray("attendance")
        if (attArray != null) {
            for (i in 0 until attArray.length()) {
                val obj = attArray.getJSONObject(i)
                attendance.add(
                    AttendanceEntity(
                        id = obj.optLong("id", 0L),
                        employeeId = obj.optLong("employeeId", 0L),
                        dateString = obj.optString("dateString", ""),
                        status = obj.optString("status", "PRESENT"),
                        overtimeHours = obj.optDouble("overtimeHours", 0.0),
                        remarks = obj.optString("remarks", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        }

        val advances = mutableListOf<AdvancePaymentEntity>()
        val advArray = root.optJSONArray("advances")
        if (advArray != null) {
            for (i in 0 until advArray.length()) {
                val obj = advArray.getJSONObject(i)
                advances.add(
                    AdvancePaymentEntity(
                        id = obj.optLong("id", 0L),
                        employeeId = obj.optLong("employeeId", 0L),
                        amount = obj.optDouble("amount", 0.0),
                        dateTimestamp = obj.optLong("dateTimestamp", System.currentTimeMillis()),
                        note = obj.optString("note", ""),
                        monthYear = obj.optString("monthYear", "")
                    )
                )
            }
        }

        val salarySlips = mutableListOf<SalarySlipEntity>()
        val slipArray = root.optJSONArray("salarySlips")
        if (slipArray != null) {
            for (i in 0 until slipArray.length()) {
                val obj = slipArray.getJSONObject(i)
                salarySlips.add(
                    SalarySlipEntity(
                        id = obj.optLong("id", 0L),
                        employeeId = obj.optLong("employeeId", 0L),
                        monthYear = obj.optString("monthYear", ""),
                        totalPresentDays = obj.optDouble("totalPresentDays", 0.0),
                        totalAbsentDays = obj.optInt("totalAbsentDays", 0),
                        totalOvertimeHours = obj.optDouble("totalOvertimeHours", 0.0),
                        baseWageRate = obj.optDouble("baseWageRate", 0.0),
                        totalEarnedWage = obj.optDouble("totalEarnedWage", 0.0),
                        overtimeAmount = obj.optDouble("overtimeAmount", 0.0),
                        bonusAmount = obj.optDouble("bonusAmount", 0.0),
                        totalAdvanceDeducted = obj.optDouble("totalAdvanceDeducted", 0.0),
                        netSalaryPaid = obj.optDouble("netSalaryPaid", 0.0),
                        paymentStatus = obj.optString("paymentStatus", "PAID"),
                        paymentDate = obj.optLong("paymentDate", System.currentTimeMillis()),
                        paymentMethod = obj.optString("paymentMethod", "CASH")
                    )
                )
            }
        }

        val payments = mutableListOf<Map<String, Any?>>()
        val payArray = root.optJSONArray("payments")
        if (payArray != null) {
            for (i in 0 until payArray.length()) {
                val obj = payArray.getJSONObject(i)
                val map = mutableMapOf<String, Any?>()
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    map[key] = obj.opt(key)
                }
                payments.add(map)
            }
        }

        val profileObj = root.optJSONObject("companyProfile")
        val companyProfile = if (profileObj != null) {
            CompanyProfileEntity(
                id = profileObj.optInt("id", 1),
                companyName = profileObj.optString("companyName", "ALFA GLAZING"),
                contractorName = profileObj.optString("contractorName", "Contractor"),
                phone = profileObj.optString("phone", "+91 9876543210"),
                location = profileObj.optString("location", "Site Office"),
                currencySymbol = profileObj.optString("currencySymbol", "₹"),
                defaultOvertimeRate = profileObj.optDouble("defaultOvertimeRate", 100.0),
                securityPin = profileObj.optString("securityPin", "805281"),
                currentUserRole = profileObj.optString("currentUserRole", "ADMIN"),
                jobSiteAddressName = profileObj.optString("jobSiteAddressName", "Sector 62 Work Site")
            )
        } else null

        val lastUpdated = root.optLong("lastUpdatedTimestamp", System.currentTimeMillis())

        return GitHubDatabasePayload(
            employees = employees,
            attendance = attendance,
            advances = advances,
            salarySlips = salarySlips,
            payments = payments,
            companyProfile = companyProfile,
            lastUpdatedTimestamp = lastUpdated
        )
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val TAG = "GitHubSyncService"
    }
}
