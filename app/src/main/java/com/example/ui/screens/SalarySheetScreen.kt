package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.example.ui.dialogs.EmployeeSalaryHistoryDialog
import com.example.ui.dialogs.ExportReportDialog
import com.example.ui.dialogs.RoleStatusBanner
import com.example.ui.dialogs.RoleSwitchDialog
import com.example.ui.dialogs.SalaryCalculationAuditDialog
import com.example.ui.dialogs.UpdateWageRateDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.AlfaGlazingUiState
import com.example.ui.viewmodel.AlfaGlazingViewModel
import com.example.ui.viewmodel.EmployeeSalarySummary
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalarySheetScreen(
    uiState: AlfaGlazingUiState,
    viewModel: AlfaGlazingViewModel,
    onOpenRecordAdvance: (Long) -> Unit = {},
    onOpenRecordPayment: (employeeId: Long?, defaultType: String) -> Unit = { _, _ -> }
) {
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("ALL") } // "ALL", "PENDING", "PAID"
    var showExportDialog by remember { mutableStateOf(false) }
    var showRoleDialog by remember { mutableStateOf(false) }
    var showBatchPayDialog by remember { mutableStateOf(false) }
    var selectedPayslipEmp by remember { mutableStateOf<EmployeeSalarySummary?>(null) }
    var auditCalculationEmp by remember { mutableStateOf<EmployeeSalarySummary?>(null) }
    var singlePayslipToShare by remember { mutableStateOf<EmployeeSalarySummary?>(null) }
    var selectedHistoryEmp by remember { mutableStateOf<EmployeeEntity?>(null) }
    var wageRateEmpToUpdate by remember { mutableStateOf<EmployeeEntity?>(null) }
    var showManageWageRatesDialog by remember { mutableStateOf(false) }

    val monthStr = uiState.selectedMonth
    val currency = uiState.companyProfile.currencySymbol

    val salarySummaries = viewModel.generateSalarySummaryForMonth(monthStr)
    val filteredSummaries = salarySummaries.filter { summary ->
        val matchesSearch = summary.employee.name.contains(searchQuery, ignoreCase = true) ||
                summary.employee.employeeCode.contains(searchQuery, ignoreCase = true) ||
                summary.employee.designation.contains(searchQuery, ignoreCase = true)
        val matchesStatus = when (statusFilter) {
            "PENDING" -> summary.paymentStatus != "PAID"
            "PAID" -> summary.paymentStatus == "PAID"
            else -> true
        }
        matchesSearch && matchesStatus
    }

    val totalGrossEarned = salarySummaries.sumOf { it.earnedBasicWage + it.overtimePay }
    val totalAdvancesDeducted = salarySummaries.sumOf { it.totalAdvanceDeducted }
    val totalNetPayable = salarySummaries.sumOf { it.netSalaryPayable }
    val paidCount = salarySummaries.count { it.paymentStatus == "PAID" }
    val pendingCount = salarySummaries.size - paidCount

    // Helper to change month
    val monthSdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    val cal = Calendar.getInstance()
    try {
        val parsed = monthSdf.parse(monthStr)
        if (parsed != null) cal.time = parsed
    } catch (e: Exception) {
        e.printStackTrace()
    }

    if (showRoleDialog) {
        RoleSwitchDialog(
            currentRole = uiState.companyProfile.currentUserRole,
            viewModel = viewModel,
            onDismiss = { showRoleDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        RoleStatusBanner(
            isUserAdmin = uiState.isUserAdmin,
            loggedInMobile = uiState.companyProfile.loggedInMobileNumber,
            loggedInEmail = uiState.companyProfile.loggedInEmail,
            onSwitchRoleClick = { showRoleDialog = true }
        )
        // Month Selector Bar
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(0.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            cal.add(Calendar.MONTH, -1)
                            viewModel.setSelectedMonth(monthSdf.format(cal.time))
                        }
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(GlazingBluePrimary.copy(alpha = 0.15f))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Payments,
                            contentDescription = null,
                            tint = GlazingBluePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Salary Sheet: $monthStr",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = GlazingBluePrimary
                        )
                    }

                    IconButton(
                        onClick = {
                            cal.add(Calendar.MONTH, 1)
                            viewModel.setSelectedMonth(monthSdf.format(cal.time))
                        }
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Contractor Financial Summary Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Total Monthly Net Payroll",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$currency${totalNetPayable.toInt()}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (uiState.isUserAdmin) {
                                    Button(
                                        onClick = { onOpenRecordPayment(null, "SALARY") },
                                        colors = ButtonDefaults.buttonColors(containerColor = GlazingBluePrimary),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("record_payment_salary_screen_btn")
                                    ) {
                                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Record Payment", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = { showManageWageRatesDialog = true },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("manage_wages_salary_screen_btn")
                                    ) {
                                        Icon(Icons.Default.AttachMoney, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("Wage Form", fontSize = 12.sp)
                                    }
                                }

                                Button(
                                    onClick = { showExportDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("export_salary_sheet_btn")
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Export", fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Gross Earned: $currency${totalGrossEarned.toInt()}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Advances Deducted: $currency${totalAdvancesDeducted.toInt()}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = AbsentRed
                            )
                            Text(
                                text = "Paid: $paidCount | Pending: $pendingCount",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = OvertimeAmber
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Automated Calculation Engine Banner
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Calculate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Automated Calculation Engine",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = { viewModel.recalculateMonthlySalaries(monthStr) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Recalculate", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }

                                if (uiState.isUserAdmin && pendingCount > 0) {
                                    Button(
                                        onClick = { showBatchPayDialog = true },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PresentGreen)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Batch Pay All", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Formula: (Billable Days × Daily Rate) + (OT Hours × OT Rate) - Advances",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search worker by name, code or designation...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("salary_search_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Status Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = statusFilter == "ALL",
                        onClick = { statusFilter = "ALL" },
                        label = { Text("All (${salarySummaries.size})", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = statusFilter == "PENDING",
                        onClick = { statusFilter = "PENDING" },
                        label = { Text("Pending ($pendingCount)", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OvertimeAmber.copy(alpha = 0.2f),
                            selectedLabelColor = OvertimeAmber
                        )
                    )
                    FilterChip(
                        selected = statusFilter == "PAID",
                        onClick = { statusFilter = "PAID" },
                        label = { Text("Paid ($paidCount)", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PresentGreen.copy(alpha = 0.2f),
                            selectedLabelColor = PresentGreen
                        )
                    )
                }
            }
        }

        // Salary Sheet Register Cards List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredSummaries, key = { it.employee.id }) { item ->
                WorkerSalaryCard(
                    summary = item,
                    currencySymbol = currency,
                    isUserAdmin = uiState.isUserAdmin,
                    onMarkPaid = {
                        onOpenRecordPayment(item.employee.id, "SALARY")
                    },
                    onRecordAdvance = {
                        onOpenRecordPayment(item.employee.id, "ADVANCE")
                    },
                    onViewCalculation = {
                        auditCalculationEmp = item
                    },
                    onViewSlip = {
                        selectedPayslipEmp = item
                    },
                    onViewHistory = {
                        selectedHistoryEmp = item.employee
                    },
                    onUpdateWageRate = {
                        wageRateEmpToUpdate = item.employee
                    }
                )
            }
        }
    }

    if (showManageWageRatesDialog || wageRateEmpToUpdate != null) {
        UpdateWageRateDialog(
            initialEmployee = wageRateEmpToUpdate,
            uiState = uiState,
            viewModel = viewModel,
            onDismiss = {
                showManageWageRatesDialog = false
                wageRateEmpToUpdate = null
            }
        )
    }

    if (selectedHistoryEmp != null) {
        EmployeeSalaryHistoryDialog(
            employee = selectedHistoryEmp!!,
            uiState = uiState,
            viewModel = viewModel,
            onDismiss = { selectedHistoryEmp = null }
        )
    }

    // Export Dialog
    if (showExportDialog) {
        val sb = StringBuilder()
        sb.append("💰 ALFA GLAZING MONTHLY SALARY SHEET\n")
        sb.append("Month: $monthStr\n")
        sb.append("Company: ${uiState.companyProfile.companyName}\n")
        sb.append("-------------------------------------------\n")
        sb.append("Total Net Payroll: $currency${totalNetPayable.toInt()}\n")
        sb.append("Total Workers: ${salarySummaries.size}\n")
        sb.append("-------------------------------------------\n\n")

        filteredSummaries.forEachIndexed { idx, s ->
            sb.append("${idx + 1}. ${s.employee.name} (${s.employee.employeeCode}) [${s.employee.designation}]\n")
            sb.append("   - Daily Rate: $currency${s.dailyWage.toInt()}\n")
            sb.append("   - Days Worked: ${s.presentDays} days | OT: ${s.overtimeHours} hrs\n")
            sb.append("   - Basic Earned: $currency${s.earnedBasicWage.toInt()} | OT Pay: $currency${s.overtimePay.toInt()}\n")
            sb.append("   - Advance Deducted: $currency${s.totalAdvanceDeducted.toInt()}\n")
            sb.append("   -> NET PAYABLE: $currency${s.netSalaryPayable.toInt()} [Status: ${s.paymentStatus}]\n\n")
        }

        val csvData = com.example.util.CsvExportUtil.generateMonthlySalaryCsv(
            monthStr = monthStr,
            companyName = uiState.companyProfile.companyName,
            currencySymbol = currency,
            salarySummaries = salarySummaries
        )
        val csvFileName = "AlfaGlazing_SalaryReport_${monthStr.replace("-", "_")}.csv"

        ExportReportDialog(
            title = "Monthly Salary Sheet ($monthStr)",
            reportText = sb.toString(),
            csvContent = csvData,
            csvFileName = csvFileName,
            onDismiss = { showExportDialog = false }
        )
    }

    // Individual Payslip Detail Dialog
    if (selectedPayslipEmp != null) {
        val s = selectedPayslipEmp!!
        AlertDialog(
            onDismissRequest = { selectedPayslipEmp = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(GlazingBluePrimary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_alfa_logo_cropped_1786308824043),
                            contentDescription = "ALFA GLAZING Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "ALFA GLAZING",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = GlazingBluePrimary
                        )
                        Text(
                            text = "Payslip - ${s.employee.name}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Code: ${s.employee.employeeCode} | Designation: ${s.employee.designation}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Month: $monthStr",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )

                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Daily Wage Rate:")
                        Text("$currency${s.dailyWage.toInt()}/day", fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Days Worked:")
                        Text("${s.presentDays} Days", fontWeight = FontWeight.Bold, color = PresentGreen)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Base Wage Earned:")
                        Text("$currency${s.earnedBasicWage.toInt()}", fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Overtime Pay (${s.overtimeHours} hrs):")
                        Text("$currency${s.overtimePay.toInt()}", fontWeight = FontWeight.Bold, color = OvertimeAmber)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Advance Cash Deducted:")
                        Text("- $currency${s.totalAdvanceDeducted.toInt()}", fontWeight = FontWeight.Bold, color = AbsentRed)
                    }

                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("NET SALARY PAYABLE:", fontWeight = FontWeight.ExtraBold)
                        Text(
                            "$currency${s.netSalaryPayable.toInt()}",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            singlePayslipToShare = s
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share Payslip")
                    }

                    Button(
                        onClick = {
                            viewModel.markSalaryAsPaid(s)
                            selectedPayslipEmp = null
                        },
                        enabled = uiState.isUserAdmin,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (!uiState.isUserAdmin) "Mark Paid (Admin Only)" else if (s.paymentStatus == "PAID") "Re-confirm Paid" else "Mark Paid Now")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedPayslipEmp = null }) {
                    Text("Close")
                }
            }
        )
    }

    if (singlePayslipToShare != null) {
        val sp = singlePayslipToShare!!
        val text = """
            ==================================
                   ALFA GLAZING SALARY SLIP
            ==================================
            Employee: ${sp.employee.name} (${sp.employee.employeeCode})
            Designation: ${sp.employee.designation}
            Phone: ${sp.employee.phone}
            Month: $monthStr

            ----------------------------------
            - Daily Rate: $currency${sp.dailyWage.toInt()}/day
            - Days Worked: ${sp.presentDays} days
            - Basic Wage Earned: $currency${sp.earnedBasicWage.toInt()}
            - Overtime Pay (${sp.overtimeHours} hrs): $currency${sp.overtimePay.toInt()}
            - Total Advance Deducted: -$currency${sp.totalAdvanceDeducted.toInt()}
            ----------------------------------
            -> NET SALARY PAYABLE: $currency${sp.netSalaryPayable.toInt()}
            -> Payment Status: ${sp.paymentStatus}
            ==================================
        """.trimIndent()

        ExportReportDialog(
            title = "Payslip - ${sp.employee.name}",
            reportText = text,
            onDismiss = { singlePayslipToShare = null }
        )
    }

    // Step-by-Step Salary Calculation Audit Dialog
    if (auditCalculationEmp != null) {
        val auditEmp = auditCalculationEmp!!
        SalaryCalculationAuditDialog(
            summary = auditEmp,
            monthStr = monthStr,
            uiState = uiState,
            viewModel = viewModel,
            onDismiss = { auditCalculationEmp = null },
            onUpdateWageRate = {
                val emp = auditEmp.employee
                auditCalculationEmp = null
                wageRateEmpToUpdate = emp
            },
            onRecordAdvance = {
                val empId = auditEmp.employee.id
                auditCalculationEmp = null
                onOpenRecordAdvance(empId)
            },
            onMarkPaid = {
                val empId = auditEmp.employee.id
                auditCalculationEmp = null
                onOpenRecordPayment(empId, "SALARY")
            }
        )
    }

    // Batch Pay Confirmation Dialog
    if (showBatchPayDialog) {
        var selectedMethod by remember { mutableStateOf("Bank Transfer") }
        AlertDialog(
            onDismissRequest = { showBatchPayDialog = false },
            icon = {
                Icon(Icons.Default.Payments, contentDescription = null, tint = PresentGreen, modifier = Modifier.size(32.dp))
            },
            title = {
                Text("Batch Pay Pending Salaries", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "Mark all pending employee salaries as PAID for $monthStr?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Pending Workers: $pendingCount",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Total Amount to Pay: $currency${salarySummaries.filter { it.paymentStatus != "PAID" }.sumOf { it.netSalaryPayable }.toInt()}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Payment Method:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Bank Transfer", "Cash", "UPI/Cheque").forEach { method ->
                            FilterChip(
                                selected = selectedMethod == method,
                                onClick = { selectedMethod = method },
                                label = { Text(method, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.markAllSalariesAsPaid(monthStr, selectedMethod)
                        showBatchPayDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PresentGreen)
                ) {
                    Text("Confirm Batch Payment")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchPayDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun WorkerSalaryCard(
    summary: EmployeeSalarySummary,
    currencySymbol: String,
    isUserAdmin: Boolean = true,
    onMarkPaid: () -> Unit,
    onRecordAdvance: () -> Unit,
    onViewSlip: () -> Unit,
    onViewCalculation: () -> Unit = {},
    onViewHistory: () -> Unit = {},
    onUpdateWageRate: () -> Unit = {}
) {
    val emp = summary.employee
    val isPaid = summary.paymentStatus == "PAID"

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emp.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = emp.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${emp.employeeCode} • ${emp.designation}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = if (isPaid) PresentGreen.copy(alpha = 0.2f) else OvertimeAmber.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (isPaid) "PAID" else "PENDING",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isPaid) PresentGreen else OvertimeAmber,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Work Attendance & Wage calculation stats grid (Clickable to open calculation breakdown)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onViewCalculation() }
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable(enabled = isUserAdmin, onClick = onUpdateWageRate)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Daily Rate", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (isUserAdmin) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Wage Rate", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Text("$currencySymbol${summary.dailyWage.toInt()}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Days Worked", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${summary.presentDays}d", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = PresentGreen)
                    if (summary.halfDays > 0) {
                        Text("${summary.halfDays} half", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("OT Pay", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$currencySymbol${summary.overtimePay.toInt()}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = OvertimeAmber)
                    if (summary.overtimeHours > 0) {
                        Text("${summary.overtimeHours}h", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Advance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$currencySymbol${summary.totalAdvanceDeducted.toInt()}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = AbsentRed)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Net Payable Salary:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "$currencySymbol${summary.netSalaryPayable.toInt()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = onViewCalculation,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Calculate, contentDescription = "Calculation Breakdown", modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Calc", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    IconButton(
                        onClick = onUpdateWageRate,
                        enabled = isUserAdmin,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.Default.AttachMoney,
                            contentDescription = "Update Wage Rate",
                            tint = if (isUserAdmin) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onViewHistory,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = "View Salary History",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onRecordAdvance,
                        enabled = isUserAdmin,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.Default.PriceChange,
                            contentDescription = "Record Advance",
                            tint = if (isUserAdmin) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    OutlinedButton(
                        onClick = onViewSlip,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Slip", fontSize = 11.sp)
                    }

                    if (!isPaid) {
                        Button(
                            onClick = onMarkPaid,
                            enabled = isUserAdmin,
                            colors = ButtonDefaults.buttonColors(containerColor = PresentGreen),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("mark_paid_button")
                        ) {
                            Text(if (isUserAdmin) "Pay" else "Read", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
