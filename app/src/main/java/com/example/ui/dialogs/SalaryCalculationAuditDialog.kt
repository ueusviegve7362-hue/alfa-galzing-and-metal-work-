package com.example.ui.dialogs

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entity.AttendanceEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.AlfaGlazingUiState
import com.example.ui.viewmodel.AlfaGlazingViewModel
import com.example.ui.viewmodel.EmployeeSalarySummary
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalaryCalculationAuditDialog(
    summary: EmployeeSalarySummary,
    monthStr: String,
    uiState: AlfaGlazingUiState,
    viewModel: AlfaGlazingViewModel,
    onDismiss: () -> Unit,
    onUpdateWageRate: () -> Unit,
    onRecordAdvance: () -> Unit,
    onMarkPaid: () -> Unit
) {
    val emp = summary.employee
    val currency = uiState.companyProfile.currencySymbol
    val isPaid = summary.paymentStatus == "PAID"

    // Parse month string for display
    val monthTitle = remember(monthStr) {
        try {
            val d = SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(monthStr)
            if (d != null) SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(d) else monthStr
        } catch (e: Exception) {
            monthStr
        }
    }

    val displayDateSdf = SimpleDateFormat("dd MMM (EEE)", Locale.getDefault())
    val parseDateSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun formatDate(dStr: String): String {
        return try {
            val d = parseDateSdf.parse(dStr)
            if (d != null) displayDateSdf.format(d) else dStr
        } catch (e: Exception) {
            dStr
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(GlazingBluePrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_alfa_logo_cropped_1786308824043),
                        contentDescription = "ALFA GLAZING Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Salary Calculation Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GlazingBluePrimary
                    )
                    Text(
                        text = "${emp.name} (${emp.employeeCode}) • $monthTitle",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${emp.designation} • Rate: $currency${emp.dailyWage.toInt()}/day",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Formula Explainer Banner
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Calculate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Automated Calculation Formula",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Net Salary = (Billable Days × Daily Wage) + (OT Hours × OT Rate) - Advances",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Step-by-Step Calculation Breakdown
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Step-by-Step Math",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )

                            // Step 1: Base Wages
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "1. Earned Base Wages",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${summary.presentDays} billable days × $currency${summary.dailyWage.toInt()}/day",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "$currency${summary.earnedBasicWage.toInt()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PresentGreen
                                )
                            }

                            // Step 2: Overtime
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "2. Overtime Earnings",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${summary.overtimeHours} hrs × $currency${emp.overtimeRatePerHour.toInt()}/hr",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "+ $currency${summary.overtimePay.toInt()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = OvertimeAmber
                                )
                            }

                            // Gross Subtotal
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Gross Total Earned",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$currency${summary.grossSalary.toInt()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Step 3: Advance Deductions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "3. Advance Deductions",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Advances issued in $monthTitle",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "- $currency${summary.totalAdvanceDeducted.toInt()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AbsentRed
                                )
                            }

                            if (summary.bonusAmount > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Performance Bonus",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "+ $currency${summary.bonusAmount.toInt()}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = PresentGreen
                                    )
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            // Final Net Salary
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "FINAL NET PAYABLE",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Status: ${if (isPaid) "PAID" else "PENDING"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isPaid) PresentGreen else OvertimeAmber,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "$currency${summary.netSalaryPayable.toInt()}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Attendance Days Distribution
                item {
                    Text(
                        text = "Attendance Summary (${summary.totalDaysInMonth} Days in Month)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Present
                        Surface(
                            color = PresentGreen.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Present", fontSize = 11.sp, color = PresentGreen, fontWeight = FontWeight.Bold)
                                Text("${summary.fullPresentDays}d", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PresentGreen)
                            }
                        }
                        // Half Days
                        Surface(
                            color = OvertimeAmber.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Half Day", fontSize = 11.sp, color = OvertimeAmber, fontWeight = FontWeight.Bold)
                                Text("${summary.halfDays}d (${summary.halfDays * 0.5}d)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OvertimeAmber)
                            }
                        }
                        // Absent
                        Surface(
                            color = AbsentRed.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Absent", fontSize = 11.sp, color = AbsentRed, fontWeight = FontWeight.Bold)
                                Text("${summary.absentDays}d", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AbsentRed)
                            }
                        }
                        // Leave
                        Surface(
                            color = Color.Blue.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Leave", fontSize = 11.sp, color = Color.Blue, fontWeight = FontWeight.Bold)
                                Text("${summary.leaveDays}d", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Blue)
                            }
                        }
                    }
                }

                // Daily Attendance Audit Logs List
                item {
                    Text(
                        text = "Daily Attendance Log (${summary.attendanceRecords.size} records logged)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (summary.attendanceRecords.isEmpty()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No daily attendance marked for this worker in $monthTitle yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                } else {
                    items(summary.attendanceRecords, key = { it.id }) { att ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = formatDate(att.dateString),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (att.remarks.isNotBlank()) {
                                        Text(
                                            text = att.remarks,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (att.overtimeHours > 0) {
                                        Surface(
                                            color = OvertimeAmber.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "+${att.overtimeHours}h OT",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = OvertimeAmber,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    val (statusBg, statusFg, statusText, dayVal) = when (att.status) {
                                        "PRESENT" -> Tuple4(PresentGreen.copy(alpha = 0.15f), PresentGreen, "PRESENT", "1.0 Day ($currency${summary.dailyWage.toInt()})")
                                        "HALF_DAY" -> Tuple4(OvertimeAmber.copy(alpha = 0.15f), OvertimeAmber, "HALF DAY", "0.5 Day ($currency${(summary.dailyWage / 2).toInt()})")
                                        "ABSENT" -> Tuple4(AbsentRed.copy(alpha = 0.15f), AbsentRed, "ABSENT", "0 Day ($currency 0)")
                                        "LEAVE" -> Tuple4(Color.Blue.copy(alpha = 0.15f), Color.Blue, "LEAVE", "Leave")
                                        else -> Tuple4(Color.Gray.copy(alpha = 0.15f), Color.DarkGray, att.status, "")
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Surface(color = statusBg, shape = RoundedCornerShape(4.dp)) {
                                            Text(
                                                text = statusText,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = statusFg,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = dayVal,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (uiState.isUserAdmin) {
                    OutlinedButton(
                        onClick = onUpdateWageRate,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.AttachMoney, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Wage Rate", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onRecordAdvance,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PriceChange, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Advance", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onMarkPaid,
                        colors = ButtonDefaults.buttonColors(containerColor = if (isPaid) PresentGreen else GlazingBluePrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(if (isPaid) Icons.Default.CheckCircle else Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isPaid) "Paid" else "Mark Paid", fontSize = 12.sp)
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

private data class Tuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
