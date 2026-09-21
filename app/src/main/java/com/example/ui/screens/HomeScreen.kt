package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import com.example.ui.components.AttendancePunchInCard
import com.example.ui.components.EmployeeOneTapPunchCard
import com.example.ui.components.RealtimeAttendanceFeedCard
import com.example.ui.dialogs.EmployeeSalaryHistoryDialog
import com.example.ui.dialogs.ExportReportDialog
import com.example.ui.dialogs.RoleStatusBanner
import com.example.ui.dialogs.RoleSwitchDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.AlfaGlazingUiState
import com.example.ui.viewmodel.AlfaGlazingViewModel
import com.example.util.CsvExportUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: AlfaGlazingUiState,
    viewModel: AlfaGlazingViewModel,
    onNavigateToAttendance: () -> Unit,
    onNavigateToSalarySheet: () -> Unit,
    onNavigateToEmployees: () -> Unit,
    onNavigateToAdvances: () -> Unit,
    onOpenAddEmployee: () -> Unit,
    onOpenRecordAdvance: () -> Unit,
    onOpenRecordPayment: ((employeeId: Long?, defaultType: String) -> Unit)? = null
) {
    var showRoleDialog by remember { mutableStateOf(false) }
    var selectedHistoryEmp by remember { mutableStateOf<EmployeeEntity?>(null) }
    var showExportSalaryDialog by remember { mutableStateOf(false) }
    var showExportAttendanceDialog by remember { mutableStateOf(false) }

    val dateStr = uiState.selectedDate
    val todayAttendance = uiState.allAttendance.filter { it.dateString == dateStr }
    val presentCount = todayAttendance.count { it.status == "PRESENT" }
    val halfDayCount = todayAttendance.count { it.status == "HALF_DAY" }
    val absentCount = todayAttendance.count { it.status == "ABSENT" }
    val unmarkedCount = (uiState.employees.size - todayAttendance.size).coerceAtLeast(0)
    val totalOtHours = todayAttendance.sumOf { it.overtimeHours }

    val monthlySalarySummaries = viewModel.generateSalarySummaryForMonth(uiState.selectedMonth)
    val totalMonthlyPayroll = monthlySalarySummaries.sumOf { it.netSalaryPayable }
    val totalAdvancesThisMonth = uiState.allAdvances.filter { it.monthYear == uiState.selectedMonth }.sumOf { it.amount }

    if (showRoleDialog) {
        RoleSwitchDialog(
            currentRole = uiState.companyProfile.currentUserRole,
            viewModel = viewModel,
            onDismiss = { showRoleDialog = false }
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

    if (showExportAttendanceDialog) {
        val currentMonthStr = uiState.selectedMonth
        val csvData = CsvExportUtil.generateMonthlyAttendanceCsv(
            monthStr = currentMonthStr,
            companyName = uiState.companyProfile.companyName,
            employees = uiState.employees,
            attendanceList = uiState.allAttendance
        )
        val csvFileName = "AlfaGlazing_AttendanceReport_${currentMonthStr.replace("-", "_")}.csv"

        ExportReportDialog(
            title = "Attendance Sheet ($currentMonthStr)",
            reportText = "📋 Monthly Attendance Report for $currentMonthStr\nTotal Employees: ${uiState.employees.size}",
            csvContent = csvData,
            csvFileName = csvFileName,
            onDismiss = { showExportAttendanceDialog = false }
        )
    }

    if (showExportSalaryDialog) {
        val currentMonthStr = uiState.selectedMonth
        val csvData = CsvExportUtil.generateMonthlySalaryCsv(
            monthStr = currentMonthStr,
            companyName = uiState.companyProfile.companyName,
            currencySymbol = uiState.companyProfile.currencySymbol,
            salarySummaries = monthlySalarySummaries
        )
        val csvFileName = "AlfaGlazing_SalaryReport_${currentMonthStr.replace("-", "_")}.csv"

        ExportReportDialog(
            title = "Monthly Salary Sheet ($currentMonthStr)",
            reportText = "💰 Monthly Salary Report for $currentMonthStr\nTotal Employees: ${monthlySalarySummaries.size}\nTotal Net Payable: ${uiState.companyProfile.currencySymbol}${totalMonthlyPayroll.toInt()}",
            csvContent = csvData,
            csvFileName = csvFileName,
            onDismiss = { showExportSalaryDialog = false }
        )
    }

    val pullRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = {
            viewModel.refreshDashboardDataFromFirestore()
        },
        state = pullRefreshState,
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_pull_to_refresh")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Role Status Banner
            item {
                val matchedEmpName = uiState.employees.firstOrNull()?.name ?: ""
                RoleStatusBanner(
                    isUserAdmin = uiState.isUserAdmin,
                    loggedInMobile = uiState.companyProfile.loggedInMobileNumber,
                    loggedInEmail = uiState.companyProfile.loggedInEmail,
                    matchedEmployeeName = matchedEmpName,
                onSwitchRoleClick = { showRoleDialog = true }
            )
        }

        // Current Date & Attendance Punch-In Card (with Simulated Firestore Write Operation)
        item {
            val isSyncing by viewModel.isPunchInSyncing.collectAsState()
            val personalEmp = uiState.employees.firstOrNull()
            val todayAtt = personalEmp?.let { emp ->
                uiState.allAttendance.find { it.employeeId == emp.id && it.dateString == dateStr }
            } ?: uiState.allAttendance.find { it.dateString == dateStr }

            AttendancePunchInCard(
                currentDateStr = dateStr,
                isSyncing = isSyncing,
                todayAttendance = todayAtt,
                currentEmployee = personalEmp,
                onPunchInClick = {
                    viewModel.triggerSimulatedFirestorePunchIn(personalEmp?.id)
                }
            )
        }

        if (!uiState.isUserAdmin) {
            val personalEmp = uiState.employees.firstOrNull()
            val todayAtt = personalEmp?.let { emp ->
                uiState.allAttendance.find { it.employeeId == emp.id && it.dateString == dateStr }
            }

            if (personalEmp != null) {
                item {
                    EmployeeOneTapPunchCard(
                        employee = personalEmp,
                        todayAttendance = todayAtt,
                        todayDateStr = dateStr,
                        companyProfile = uiState.companyProfile,
                        onPunchIn = { status ->
                            viewModel.employeeSelfPunchIn(personalEmp.id, status)
                        }
                    )
                }
            }

            item {
                val personalSummary = monthlySalarySummaries.firstOrNull()

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Personal Identification Card", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            }
                            AssistChip(
                                onClick = { showRoleDialog = true },
                                label = { Text("Change Mobile", fontSize = 11.sp) },
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (personalEmp != null) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = personalEmp.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "📱 ${personalEmp.phone}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text(text = "🛠️ Designation: ${personalEmp.designation}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(12.dp))

                            val presDays = personalSummary?.presentDays ?: 0.0
                            val absDays = personalSummary?.absentDays ?: 0
                            val totDays = personalSummary?.totalDaysInMonth ?: 30
                            val absentPct = if (totDays > 0) ((absDays.toDouble() / totDays) * 100).toInt() else 0

                            Text(text = "Personal Attendance Summary (${uiState.selectedMonth})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = PresentGreen.copy(alpha = 0.15f)),
                                    modifier = Modifier.weight(1f).padding(end = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Present Days", fontSize = 11.sp, color = PresentGreen, fontWeight = FontWeight.Bold)
                                        Text("${presDays.toInt()} Days", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = PresentGreen)
                                    }
                                }

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = AbsentRed.copy(alpha = 0.15f)),
                                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Absent Days", fontSize = 11.sp, color = AbsentRed, fontWeight = FontWeight.Bold)
                                        Text("$absDays Days", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = AbsentRed)
                                    }
                                }

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = HalfDayOrange.copy(alpha = 0.15f)),
                                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Absent Rate", fontSize = 11.sp, color = HalfDayOrange, fontWeight = FontWeight.Bold)
                                        Text("$absentPct %", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = HalfDayOrange)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = { selectedHistoryEmp = personalEmp },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("personal_salary_history_btn")
                            ) {
                                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("View Detailed Salary & Payment History", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Text(
                                text = "No employee record found matching mobile: ${uiState.companyProfile.loggedInMobileNumber}",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                            Button(
                                onClick = { showRoleDialog = true },
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Text("Enter Registered Mobile Number")
                            }
                        }
                    }
                }
            }
        }

        // Hero Branding Header
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(RoundedCornerShape(12.dp))
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

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.companyProfile.companyName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Contractor Labor Management",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${uiState.employees.size} Active Workers Onboarding",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Quick Action Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onNavigateToAttendance,
                    colors = ButtonDefaults.buttonColors(containerColor = GlazingBluePrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("mark_attendance_quick_btn")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Daily Attendance", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onNavigateToSalarySheet,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("salary_sheet_quick_btn")
                ) {
                    Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Salary Sheet", fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
        }

        // Real-Time Staff Punch Feed
        item {
            RealtimeAttendanceFeedCard(
                allAttendance = uiState.allAttendance,
                employees = uiState.employees,
                todayDateStr = dateStr
            )
        }

        // Attendance Status Banner for Today
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Today's Attendance Overview",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Date: $dateStr",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { showExportAttendanceDialog = true },
                                modifier = Modifier.size(36.dp).testTag("home_export_attendance_csv_btn")
                            ) {
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = "Export Attendance CSV",
                                    tint = PresentGreen
                                )
                            }
                            TextButton(onClick = onNavigateToAttendance) {
                                Text("Open Sheet >", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        AttendanceStatBadge(
                            label = "Present",
                            count = presentCount,
                            color = PresentGreen,
                            icon = Icons.Default.CheckCircle
                        )
                        AttendanceStatBadge(
                            label = "Half Day",
                            count = halfDayCount,
                            color = HalfDayOrange,
                            icon = Icons.Default.Schedule
                        )
                        AttendanceStatBadge(
                            label = "Absent",
                            count = absentCount,
                            color = AbsentRed,
                            icon = Icons.Default.Cancel
                        )
                        AttendanceStatBadge(
                            label = "Unmarked",
                            count = unmarkedCount,
                            color = Color.Gray,
                            icon = Icons.Default.HelpOutline
                        )
                    }

                    if (totalOtHours > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(OvertimeAmber.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.MoreTime, contentDescription = null, tint = OvertimeAmber)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Total Overtime Today: $totalOtHours Hours",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Monthly Payroll & Advance Overview Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Monthly Payroll Sheet (${uiState.selectedMonth})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { showExportSalaryDialog = true },
                                modifier = Modifier.size(36.dp).testTag("home_export_salary_csv_btn")
                            ) {
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = "Export Salary CSV",
                                    tint = GlazingBluePrimary
                                )
                            }
                            TextButton(onClick = onNavigateToSalarySheet) {
                                Text("Open Sheet >", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Estimated Payroll Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Estimated Net Payroll",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${uiState.companyProfile.currencySymbol}${totalMonthlyPayroll.toInt()}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        // Total Advances Given Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Advances Issued",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${uiState.companyProfile.currencySymbol}${totalAdvancesThisMonth.toInt()}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }

        // Secondary Action Row (Add Worker & Record Payment)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenAddEmployee,
                    enabled = uiState.isUserAdmin,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("add_worker_home_btn")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (uiState.isUserAdmin) "Add Worker" else "Add Worker (Admin)")
                }

                Button(
                    onClick = { onOpenRecordPayment?.invoke(null, "SALARY") ?: onOpenRecordAdvance() },
                    enabled = uiState.isUserAdmin,
                    colors = ButtonDefaults.buttonColors(containerColor = GlazingBluePrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("record_payment_home_btn")
                ) {
                    Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (uiState.isUserAdmin) "Record Payment" else "Payment (Admin)", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Admin Payment Disbursals Card (Direct sync to Firestore 'payments' collection)
        if (uiState.isUserAdmin) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(GlazingBluePrimary.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = GlazingBluePrimary, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Payments Portal",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Input worker salary or advance payment details",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onOpenRecordPayment?.invoke(null, "SALARY") ?: onOpenRecordAdvance() },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("record_salary_btn")
                            ) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pay Salary", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { onOpenRecordPayment?.invoke(null, "ADVANCE") ?: onOpenRecordAdvance() },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("record_advance_btn")
                            ) {
                                Icon(Icons.Default.PriceChange, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pay Advance", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Active Employees Preview Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Workers Roster (${uiState.employees.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onNavigateToEmployees) {
                    Text("View All Workers >", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Top 5 Workers Preview
        items(uiState.employees.take(5)) { emp ->
            WorkerQuickCard(
                employee = emp,
                currencySymbol = uiState.companyProfile.currencySymbol,
                todayStatus = todayAttendance.find { it.employeeId == emp.id }?.status
            )
        }
    }
}
}

@Composable
fun AttendanceStatBadge(
    label: String,
    count: Int,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun WorkerQuickCard(
    employee: com.example.data.local.entity.EmployeeEntity,
    currencySymbol: String,
    todayStatus: String?
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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
                    text = employee.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = employee.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${employee.employeeCode} • ${employee.designation}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$currencySymbol${employee.dailyWage.toInt()}/day",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                val (badgeText, badgeColor) = when (todayStatus) {
                    "PRESENT" -> "PRESENT" to PresentGreen
                    "HALF_DAY" -> "HALF DAY" to HalfDayOrange
                    "ABSENT" -> "ABSENT" to AbsentRed
                    "LEAVE" -> "LEAVE" to LeavePurple
                    else -> "UNMARKED" to Color.Gray
                }

                Surface(
                    color = badgeColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
