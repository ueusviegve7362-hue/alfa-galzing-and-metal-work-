package com.example.ui.dialogs

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entity.EmployeeEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.AlfaGlazingUiState
import com.example.ui.viewmodel.AlfaGlazingViewModel
import com.example.ui.viewmodel.EmployeeMonthSalaryHistory
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeSalaryHistoryDialog(
    employee: EmployeeEntity,
    uiState: AlfaGlazingUiState,
    viewModel: AlfaGlazingViewModel,
    onDismiss: () -> Unit
) {
    var showExportDialog by remember { mutableStateOf(false) }
    val historyList = remember(employee.id, uiState.allAttendance, uiState.allAdvances, uiState.salarySlips) {
        viewModel.getSalaryHistoryForEmployee(employee.id)
    }

    val currency = uiState.companyProfile.currencySymbol
    val totalGrossEarned = historyList.sumOf { it.summary.earnedBasicWage + it.summary.overtimePay }
    val totalAdvancesDeducted = historyList.sumOf { it.summary.totalAdvanceDeducted }
    val totalNetPayable = historyList.sumOf { it.summary.netSalaryPayable }
    val paidMonthsCount = historyList.count { it.summary.paymentStatus == "PAID" }

    val monthParseSdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    val monthDisplaySdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val dateDisplaySdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    fun formatMonthString(raw: String): String {
        return try {
            val date = monthParseSdf.parse(raw)
            if (date != null) monthDisplaySdf.format(date) else raw
        } catch (e: Exception) {
            raw
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
                        .size(44.dp)
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
                        text = "Salary & Payment History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GlazingBluePrimary
                    )
                    Text(
                        text = "${employee.name} (${employee.employeeCode})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Designation: ${employee.designation} • Rate: $currency${employee.dailyWage.toInt()}/day",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Lifetime Summary Overview Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Cumulative Wages Overview",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "$paidMonthsCount / ${historyList.size} Months Paid",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Total Gross Earned", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$currency${totalGrossEarned.toInt()}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Column {
                                Text("Total Advances", fontSize = 11.sp, color = AbsentRed)
                                Text("- $currency${totalAdvancesDeducted.toInt()}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AbsentRed)
                            }
                            Column {
                                Text("Net Earned Payable", fontSize = 11.sp, color = PresentGreen)
                                Text("$currency${totalNetPayable.toInt()}", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = PresentGreen)
                            }
                        }
                    }
                }

                // Month by Month History List
                if (historyList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No past salary records found for this worker.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = "Monthly Payment Breakdowns (${historyList.size} Periods):",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(historyList, key = { it.monthYear }) { historyItem ->
                            val s = historyItem.summary
                            val isPaid = s.paymentStatus == "PAID"
                            val formattedMonth = formatMonthString(historyItem.monthYear)

                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(1.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    // Header: Month + Status Badge
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.CalendarToday,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = GlazingBluePrimary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = formattedMonth,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }

                                        Surface(
                                            color = if (isPaid) PresentGreen.copy(alpha = 0.15f) else OvertimeAmber.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    if (isPaid) Icons.Default.CheckCircle else Icons.Default.HourglassEmpty,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(12.dp),
                                                    tint = if (isPaid) PresentGreen else OvertimeAmber
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isPaid) "PAID" else "PENDING",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isPaid) PresentGreen else OvertimeAmber
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Metrics Grid
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Days Worked", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("${s.presentDays} Days", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        }
                                        Column {
                                            Text("Base Wages", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("$currency${s.earnedBasicWage.toInt()}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        }
                                        Column {
                                            Text("OT Pay (${s.overtimeHours}h)", fontSize = 11.sp, color = OvertimeAmber)
                                            Text("$currency${s.overtimePay.toInt()}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = OvertimeAmber)
                                        }
                                        Column {
                                            Text("Advance", fontSize = 11.sp, color = AbsentRed)
                                            Text("- $currency${s.totalAdvanceDeducted.toInt()}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = AbsentRed)
                                        }
                                    }

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Net Payable Salary:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                text = "$currency${s.netSalaryPayable.toInt()}",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 15.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        if (isPaid && historyItem.paymentDateTimestamp != null) {
                                            Text(
                                                text = "Paid on ${dateDisplaySdf.format(Date(historyItem.paymentDateTimestamp))}",
                                                fontSize = 11.sp,
                                                color = PresentGreen,
                                                fontWeight = FontWeight.Medium
                                            )
                                        } else if (uiState.isUserAdmin) {
                                            Button(
                                                onClick = {
                                                    viewModel.markSalaryAsPaid(s)
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text("Mark Paid", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { showExportDialog = true },
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share History Statement")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )

    if (showExportDialog) {
        val sb = StringBuilder()
        sb.append("📋 ALFA GLAZING - WORKER SALARY & PAYMENT HISTORY STATEMENT\n")
        sb.append("---------------------------------------------------\n")
        sb.append("Employee: ${employee.name} (${employee.employeeCode})\n")
        sb.append("Designation: ${employee.designation}\n")
        sb.append("Phone: ${employee.phone}\n")
        sb.append("Daily Wage Rate: $currency${employee.dailyWage.toInt()} / day\n")
        sb.append("Overtime Rate: $currency${employee.overtimeRatePerHour.toInt()} / hr\n")
        sb.append("---------------------------------------------------\n")
        sb.append("CUMULATIVE FINANCIAL OVERVIEW:\n")
        sb.append(" - Total Gross Wages Earned: $currency${totalGrossEarned.toInt()}\n")
        sb.append(" - Total Cash Advances Deducted: $currency${totalAdvancesDeducted.toInt()}\n")
        sb.append(" - Net Total Wages Earned/Paid: $currency${totalNetPayable.toInt()}\n")
        sb.append(" - Months Active: ${historyList.size} | Paid Months: $paidMonthsCount\n")
        sb.append("---------------------------------------------------\n\n")
        sb.append("MONTH-BY-MONTH BREAKDOWN:\n\n")

        historyList.forEachIndexed { idx, h ->
            val formattedMonth = formatMonthString(h.monthYear)
            val s = h.summary
            sb.append("${idx + 1}. Month: $formattedMonth [Status: ${s.paymentStatus}]\n")
            sb.append("   - Days Worked: ${s.presentDays} days (Absent: ${s.absentDays}, Half: ${s.halfDays})\n")
            sb.append("   - Overtime: ${s.overtimeHours} hrs (Earned: $currency${s.overtimePay.toInt()})\n")
            sb.append("   - Basic Earned: $currency${s.earnedBasicWage.toInt()}\n")
            sb.append("   - Advance Deducted: $currency${s.totalAdvanceDeducted.toInt()}\n")
            sb.append("   -> NET MONTHLY PAYABLE: $currency${s.netSalaryPayable.toInt()}\n")
            if (h.paymentDateTimestamp != null) {
                sb.append("   - Payment Date: ${dateDisplaySdf.format(Date(h.paymentDateTimestamp))}\n")
            }
            sb.append("\n")
        }

        ExportReportDialog(
            title = "Salary History (${employee.name})",
            reportText = sb.toString(),
            onDismiss = { showExportDialog = false }
        )
    }
}
