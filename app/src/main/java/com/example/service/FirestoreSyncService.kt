package com.example.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.example.data.local.entity.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirestoreSyncService(private val context: Context) {

    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private fun reportError(operation: String, throwable: Throwable, onErrorCallback: ((String) -> Unit)? = null) {
        val errorMessage = "Network/Firestore Error during $operation: ${throwable.localizedMessage ?: "Operation failed"}"
        Log.e("FirestoreSyncService", errorMessage, throwable)

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            onErrorCallback?.invoke(errorMessage)
        }
    }

    suspend fun syncAttendanceToFirestore(
        attendance: AttendanceEntity,
        onErrorCallback: ((String) -> Unit)? = null
    ) {
        withContext(Dispatchers.IO) {
            runCatching {
                val data = mapOf(
                    "id" to attendance.id,
                    "employeeId" to attendance.employeeId,
                    "dateString" to attendance.dateString,
                    "status" to attendance.status,
                    "overtimeHours" to attendance.overtimeHours,
                    "remarks" to attendance.remarks,
                    "timestamp" to attendance.timestamp
                )
                db.collection("attendance")
                    .document("${attendance.employeeId}_${attendance.dateString}")
                    .set(data)
                    .addOnFailureListener { e ->
                        reportError("Attendance Sync", e, onErrorCallback)
                    }
            }.onFailure { e ->
                reportError("Attendance Sync", e, onErrorCallback)
            }
        }
    }

    suspend fun syncEmployeeToFirestore(
        employee: EmployeeEntity,
        onErrorCallback: ((String) -> Unit)? = null
    ) {
        withContext(Dispatchers.IO) {
            runCatching {
                val data = mapOf(
                    "id" to employee.id,
                    "employeeCode" to employee.employeeCode,
                    "name" to employee.name,
                    "phone" to employee.phone,
                    "designation" to employee.designation,
                    "dailyWage" to employee.dailyWage,
                    "overtimeRatePerHour" to employee.overtimeRatePerHour,
                    "isActive" to employee.isActive
                )
                db.collection("employees")
                    .document(employee.id.toString())
                    .set(data)
                    .addOnFailureListener { e ->
                        reportError("Employee Sync", e, onErrorCallback)
                    }
            }.onFailure { e ->
                reportError("Employee Sync", e, onErrorCallback)
            }
        }
    }

    suspend fun syncCompanyProfileToFirestore(
        profile: CompanyProfileEntity,
        onErrorCallback: ((String) -> Unit)? = null
    ) {
        withContext(Dispatchers.IO) {
            runCatching {
                val data = mapOf(
                    "companyName" to profile.companyName,
                    "contractorName" to profile.contractorName,
                    "phone" to profile.phone,
                    "location" to profile.location,
                    "currentUserRole" to profile.currentUserRole
                )
                db.collection("company_profile")
                    .document("main_profile")
                    .set(data)
                    .addOnFailureListener { e ->
                        reportError("Company Profile Sync", e, onErrorCallback)
                    }
            }.onFailure { e ->
                reportError("Company Profile Sync", e, onErrorCallback)
            }
        }
    }

    suspend fun simulateFirestorePunchInWrite(
        employeeName: String,
        dateString: String,
        status: String = "PRESENT",
        timestamp: Long = System.currentTimeMillis(),
        onSuccess: (() -> Unit)? = null,
        onErrorCallback: ((String) -> Unit)? = null
    ) {
        withContext(Dispatchers.IO) {
            runCatching {
                val timeFormatted = java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
                val data = hashMapOf(
                    "employeeName" to employeeName,
                    "dateString" to dateString,
                    "punchTime" to timeFormatted,
                    "status" to status,
                    "timestamp" to timestamp,
                    "operationType" to "Simulated_Firestore_Write"
                )
                db.collection("attendance_punches")
                    .document("sim_${timestamp}")
                    .set(data)
                    .addOnSuccessListener {
                        Log.d("FirestoreSyncService", "Simulated punch-in written to Firestore successfully: $data")
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(
                                context,
                                "Firestore Write Success: Punch-In recorded for $employeeName ($timeFormatted)",
                                Toast.LENGTH_SHORT
                            ).show()
                            onSuccess?.invoke()
                        }
                    }
                    .addOnFailureListener { e ->
                        reportError("Simulated Punch-In Write", e, onErrorCallback)
                    }
            }.onFailure { e ->
                reportError("Simulated Punch-In Write", e, onErrorCallback)
            }
        }
    }

    suspend fun savePaymentToFirestore(
        paymentData: Map<String, Any?>,
        paymentId: String = "pay_${System.currentTimeMillis()}",
        onSuccess: (() -> Unit)? = null,
        onErrorCallback: ((String) -> Unit)? = null
    ) {
        withContext(Dispatchers.IO) {
            val rawAmount = paymentData["amount"]
            val numericAmount = when (rawAmount) {
                is Number -> rawAmount.toDouble()
                is String -> rawAmount.toDoubleOrNull()
                else -> null
            }

            if (numericAmount == null || numericAmount.isNaN() || numericAmount.isInfinite() || numericAmount <= 0.0) {
                val errorMsg = "Validation Error: Only valid positive numeric amounts can be submitted to Firestore (Received: $rawAmount)"
                Log.e("FirestoreSyncService", errorMsg)
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                    onErrorCallback?.invoke(errorMsg)
                }
                return@withContext
            }

            runCatching {
                db.collection("payments")
                    .document(paymentId)
                    .set(paymentData)
                    .addOnSuccessListener {
                        Log.d("FirestoreSyncService", "Payment saved successfully to Firestore 'payments' collection: $paymentId")
                        Handler(Looper.getMainLooper()).post {
                            val type = paymentData["paymentType"] ?: "Payment"
                            val amt = paymentData["amount"] ?: ""
                            val emp = paymentData["employeeName"] ?: "Worker"
                            Toast.makeText(
                                context,
                                "Firestore Sync: $type ($amt) recorded for $emp in 'payments'",
                                Toast.LENGTH_SHORT
                            ).show()
                            onSuccess?.invoke()
                        }
                    }
                    .addOnFailureListener { e ->
                        reportError("Saving Payment to 'payments' collection", e, onErrorCallback)
                    }
            }.onFailure { e ->
                reportError("Saving Payment to 'payments' collection", e, onErrorCallback)
            }
        }
    }

    suspend fun fetchAllAttendanceFromFirestore(): List<AttendanceEntity> = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            db.collection("attendance")
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val list = mutableListOf<AttendanceEntity>()
                    for (doc in querySnapshot.documents) {
                        try {
                            val empId = doc.getLong("employeeId") ?: continue
                            val dateStr = doc.getString("dateString") ?: continue
                            val status = doc.getString("status") ?: "PRESENT"
                            val otHours = doc.getDouble("overtimeHours") ?: 0.0
                            val remarks = doc.getString("remarks") ?: ""
                            val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                            val id = doc.getLong("id") ?: 0L
                            list.add(
                                AttendanceEntity(
                                    id = id,
                                    employeeId = empId,
                                    dateString = dateStr,
                                    status = status,
                                    overtimeHours = otHours,
                                    remarks = remarks,
                                    timestamp = timestamp
                                )
                            )
                        } catch (e: Exception) {
                            Log.w("FirestoreSyncService", "Error parsing attendance doc: ${doc.id}", e)
                        }
                    }
                    continuation.resume(list)
                }
                .addOnFailureListener { error ->
                    Log.e("FirestoreSyncService", "Failed to fetch attendance from Firestore", error)
                    continuation.resume(emptyList())
                }
        }
    }

    suspend fun fetchAllPaymentsFromFirestore(): List<Map<String, Any?>> = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            db.collection("payments")
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val list = mutableListOf<Map<String, Any?>>()
                    for (doc in querySnapshot.documents) {
                        doc.data?.let { data ->
                            list.add(data)
                        }
                    }
                    continuation.resume(list)
                }
                .addOnFailureListener { error ->
                    Log.e("FirestoreSyncService", "Failed to fetch payments from Firestore", error)
                    continuation.resume(emptyList())
                }
        }
    }
}

