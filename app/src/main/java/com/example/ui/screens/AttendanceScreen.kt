package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.EmployeeEntity
import com.example.ui.components.AttendancePunchInCard
import com.example.ui.dialogs.ExportReportDialog
import com.example.ui.dialogs.RoleStatusBanner
import com.example.ui.dialogs.RoleSwitchDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.AlfaGlazingUiState
import com.example.ui.viewmodel.AlfaGlazingViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AttendanceScreen(
    uiState: AlfaGlazingUiState,
    viewModel: AlfaGlazingViewModel
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("ALL") } // "ALL", "PRESENT", "ABSENT", "LEAVE", "HALF_DAY", "UNMARKED"
    var showExportDialog by remember { mutableStateOf(false) }
    var showRoleDialog by remember { mutableStateOf(false) }

    // Dialog state for setting leave reason / notes
    var leaveReasonTarget by remember { mutableStateOf<Pair<EmployeeEntity, AttendanceEntity?>?>(null) }

    val dateStr = uiState.selectedDate
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val isToday = dateStr == todayStr

    val prettyDate = remember(dateStr) {
        try {
            val parsed = sdf.parse(dateStr)
            if (parsed != null) {
                SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(parsed)
            } else dateStr
        } catch (e: Exception) {
            dateStr
        }
    }

    val dateRelationText = when {
        isToday -> "TODAY"
        dateStr < todayStr -> "PAST DATE"
        else -> "FUTURE DATE"
    }

    // Attendance stats for selected date
    val todayAttendance = uiState.allAttendance.filter { it.dateString == dateStr }
    val presentCount = todayAttendance.count { it.status == "PRESENT" }
    val absentCount = todayAttendance.count { it.status == "ABSENT" }
    val leaveCount = todayAttendance.count { it.status == "LEAVE" }
    val halfDayCount = todayAttendance.count { it.status == "HALF_DAY" }
    val markedCount = presentCount + absentCount + leaveCount + halfDayCount
    val totalEmployees = uiState.employees.size
    val unmarkedCount = (totalEmployees - markedCount).coerceAtLeast(0)
    val totalOtHours = todayAttendance.sumOf { it.overtimeHours }

    // Filter employees by search and status filter
    val filteredEmployees = uiState.employees.filter { emp ->
        val matchesSearch = emp.name.contains(searchQuery, ignoreCase = true) ||
                emp.employeeCode.contains(searchQuery, ignoreCase = true) ||
                emp.designation.contains(searchQuery, ignoreCase = true)
        if (!matchesSearch) return@filter false

        val rec = todayAttendance.find { it.employeeId == emp.id }
        val status = rec?.status ?: "UNMARKED"
        when (selectedStatusFilter) {
            "ALL" -> true
            "PRESENT" -> status == "PRESENT"
            "ABSENT" -> status == "ABSENT"
            "LEAVE" -> status == "LEAVE"
            "HALF_DAY" -> status == "HALF_DAY"
            "UNMARKED" -> status == "UNMARKED"
            else -> true
        }
    }

    // Date Picker Dialog Logic
    val calendar = Calendar.getInstance()
    try {
        val parsedDate = sdf.parse(dateStr)
        if (parsedDate != null) calendar.time = parsedDate
    } catch (e: Exception) {
        e.printStackTrace()
    }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val newCal = Calendar.getInstance()
            newCal.set(year, month, dayOfMonth)
            viewModel.setSelectedDate(sdf.format(newCal.time))
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    if (showRoleDialog) {
        RoleSwitchDialog(
            currentRole = uiState.companyProfile.currentUserRole,
            viewModel = viewModel,
            onDismiss = { showRoleDialog = false }
        )
    }

    // Leave Reason & Remark Dialog
    leaveReasonTarget?.let { (employee, record) ->
        AttendanceLeaveReasonDialog(
            employee = employee,
            currentRecord = record,
            dateStr = dateStr,
            onDismiss = { leaveReasonTarget = null },
            onSaveRemark = { remarkText ->
                viewModel.markAttendance(
                    employeeId = employee.id,
                    status = "LEAVE",
                    overtimeHours = 0.0,
                    remark = remarkText
                )
            }
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

        // Top Control Panel: Date Selector, Overview Counters, Bulk Actions, Search
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
            elevation = CardDefaults.cardElevation(3.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {

                // 1. Specific Date Selector & Navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            calendar.add(Calendar.DAY_OF_MONTH, -1)
                            viewModel.setSelectedDate(sdf.format(calendar.time))
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("attendance_prev_day_btn")
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day")
                    }

                    // Center Date Picker Card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .clickable { datePickerDialog.show() }
                            .testTag("attendance_date_picker_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = prettyDate,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isToday) PresentGreen else MaterialTheme.colorScheme.primary
                                    ) {
                                        Text(
                                            text = dateRelationText,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = dateStr,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            calendar.add(Calendar.DAY_OF_MONTH, 1)
                            viewModel.setSelectedDate(sdf.format(calendar.time))
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("attendance_next_day_btn")
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Day")
                    }
                }

                // If not today, show a quick "Jump to Today" shortcut chip
                if (!isToday) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        AssistChip(
                            onClick = { viewModel.setSelectedDate(todayStr) },
                            label = { Text("Jump back to Today ($todayStr)", fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Attendance Progress & Counters Bar for the Date
                val progressFrac = if (totalEmployees > 0) markedCount.toFloat() / totalEmployees else 0f
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daily Attendance Progress",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$markedCount of $totalEmployees Marked (${(progressFrac * 100).toInt()}%)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (markedCount == totalEmployees) PresentGreen else MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progressFrac },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = if (markedCount == totalEmployees) PresentGreen else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Summary Counters row (Present, Absent, Leave, Half Day, Unmarked)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AttendanceCountPill(
                            label = "Present",
                            count = presentCount,
                            color = PresentGreen,
                            icon = Icons.Default.CheckCircle,
                            isSelected = selectedStatusFilter == "PRESENT",
                            onClick = {
                                selectedStatusFilter = if (selectedStatusFilter == "PRESENT") "ALL" else "PRESENT"
                            }
                        )
                        AttendanceCountPill(
                            label = "Absent",
                            count = absentCount,
                            color = AbsentRed,
                            icon = Icons.Default.Cancel,
                            isSelected = selectedStatusFilter == "ABSENT",
                            onClick = {
                                selectedStatusFilter = if (selectedStatusFilter == "ABSENT") "ALL" else "ABSENT"
                            }
                        )
                        AttendanceCountPill(
                            label = "Leave",
                            count = leaveCount,
                            color = LeavePurple,
                            icon = Icons.Default.EventBusy,
                            isSelected = selectedStatusFilter == "LEAVE",
                            onClick = {
                                selectedStatusFilter = if (selectedStatusFilter == "LEAVE") "ALL" else "LEAVE"
                            }
                        )
                        AttendanceCountPill(
                            label = "Half Day",
                            count = halfDayCount,
                            color = HalfDayOrange,
                            icon = Icons.Default.Schedule,
                            isSelected = selectedStatusFilter == "HALF_DAY",
                            onClick = {
                                selectedStatusFilter = if (selectedStatusFilter == "HALF_DAY") "ALL" else "HALF_DAY"
                            }
                        )
                        AttendanceCountPill(
                            label = "Unmarked",
                            count = unmarkedCount,
                            color = Color.Gray,
                            icon = Icons.Default.HelpOutline,
                            isSelected = selectedStatusFilter == "UNMARKED",
                            onClick = {
                                selectedStatusFilter = if (selectedStatusFilter == "UNMARKED") "ALL" else "UNMARKED"
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Contractor 1-Tap Bulk Action Buttons (Present, Absent, On Leave, Export)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { viewModel.markBulkAttendance("PRESENT") },
                        enabled = uiState.isUserAdmin,
                        colors = ButtonDefaults.buttonColors(containerColor = PresentGreen),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bulk_present_btn")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("All Present", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }

                    Button(
                        onClick = { viewModel.markBulkAttendance("ABSENT") },
                        enabled = uiState.isUserAdmin,
                        colors = ButtonDefaults.buttonColors(containerColor = AbsentRed),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bulk_absent_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("All Absent", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }

                    Button(
                        onClick = { viewModel.markBulkAttendance("LEAVE") },
                        enabled = uiState.isUserAdmin,
                        colors = ButtonDefaults.buttonColors(containerColor = LeavePurple),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bulk_leave_btn")
                    ) {
                        Icon(Icons.Default.EventBusy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("All Leave", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = { showExportDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("export_attendance_btn")
                    ) {
                        Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Export", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 4. Quick Filter Chips Bar (All, Present, Absent, On Leave, Half Day, Unmarked)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedStatusFilter == "ALL",
                            onClick = { selectedStatusFilter = "ALL" },
                            label = { Text("All ($totalEmployees)", fontSize = 11.sp) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedStatusFilter == "PRESENT",
                            onClick = { selectedStatusFilter = "PRESENT" },
                            label = { Text("Present ($presentCount)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PresentGreen.copy(alpha = 0.2f),
                                selectedLabelColor = PresentGreen
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedStatusFilter == "ABSENT",
                            onClick = { selectedStatusFilter = "ABSENT" },
                            label = { Text("Absent ($absentCount)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AbsentRed.copy(alpha = 0.2f),
                                selectedLabelColor = AbsentRed
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedStatusFilter == "LEAVE",
                            onClick = { selectedStatusFilter = "LEAVE" },
                            label = { Text("On Leave ($leaveCount)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LeavePurple.copy(alpha = 0.2f),
                                selectedLabelColor = LeavePurple
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedStatusFilter == "HALF_DAY",
                            onClick = { selectedStatusFilter = "HALF_DAY" },
                            label = { Text("Half Day ($halfDayCount)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HalfDayOrange.copy(alpha = 0.2f),
                                selectedLabelColor = HalfDayOrange
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedStatusFilter == "UNMARKED",
                            onClick = { selectedStatusFilter = "UNMARKED" },
                            label = { Text("Unmarked ($unmarkedCount)", fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search worker by name, code or designation...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("attendance_search_input"),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        // Employee Attendance Cards List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Optional punch card if non-admin worker logged in
            if (!uiState.isUserAdmin) {
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
                        },
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }

            if (filteredEmployees.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No workers match the current filter",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(onClick = {
                                searchQuery = ""
                                selectedStatusFilter = "ALL"
                            }) {
                                Text("Reset Filters")
                            }
                        }
                    }
                }
            } else {
                items(filteredEmployees, key = { it.id }) { emp ->
                    val empRecord = todayAttendance.find { it.employeeId == emp.id }
                    AttendanceWorkerCard(
                        employee = emp,
                        record = empRecord,
                        dateStr = dateStr,
                        isUserAdmin = uiState.isUserAdmin,
                        loggedInMobile = uiState.companyProfile.loggedInMobileNumber,
                        loggedInEmail = uiState.companyProfile.loggedInEmail,
                        onStatusChange = { newStatus ->
                            if (uiState.isUserAdmin) {
                                viewModel.markAttendance(
                                    employeeId = emp.id,
                                    status = newStatus,
                                    overtimeHours = if (newStatus == "PRESENT" || newStatus == "HALF_DAY") empRecord?.overtimeHours ?: 0.0 else 0.0,
                                    remark = empRecord?.remarks ?: ""
                                )
                            } else {
                                viewModel.employeeSelfPunchIn(emp.id, newStatus)
                            }
                        },
                        onOvertimeChange = { otHours ->
                            viewModel.markAttendance(
                                employeeId = emp.id,
                                status = empRecord?.status ?: "PRESENT",
                                overtimeHours = otHours,
                                remark = empRecord?.remarks ?: ""
                            )
                        },
                        onOpenRemarkDialog = {
                            leaveReasonTarget = Pair(emp, empRecord)
                        }
                    )
                }
            }
        }
    }

    // Export Dialog with Full Attendance Information
    if (showExportDialog) {
        val currentMonthStr = dateStr.substring(0, 7.coerceAtMost(dateStr.length))
        val reportBuilder = StringBuilder()
        reportBuilder.append("📋 ALFA GLAZING DAILY ATTENDANCE SHEET\n")
        reportBuilder.append("Date: $prettyDate ($dateStr)\n")
        reportBuilder.append("Month: $currentMonthStr\n")
        reportBuilder.append("-----------------------------------\n")
        reportBuilder.append("Total Team: ${filteredEmployees.size}\n")
        reportBuilder.append("Present: $presentCount | Absent: $absentCount | On Leave: $leaveCount | Half Day: $halfDayCount | Unmarked: $unmarkedCount\n")
        reportBuilder.append("Total Overtime Hours: ${totalOtHours}h\n")
        reportBuilder.append("-----------------------------------\n\n")

        filteredEmployees.forEachIndexed { index, emp ->
            val rec = todayAttendance.find { it.employeeId == emp.id }
            val statusStr = when (rec?.status) {
                "PRESENT" -> "PRESENT"
                "ABSENT" -> "ABSENT"
                "LEAVE" -> "ON LEAVE"
                "HALF_DAY" -> "HALF DAY"
                else -> "UNMARKED"
            }
            val otStr = if ((rec?.overtimeHours ?: 0.0) > 0) " [OT: ${rec?.overtimeHours}h]" else ""
            val remarkStr = if (!rec?.remarks.isNullOrBlank()) " (Reason: ${rec?.remarks})" else ""
            reportBuilder.append("${index + 1}. ${emp.name} (${emp.employeeCode}): $statusStr$otStr$remarkStr\n")
        }

        val csvData = com.example.util.CsvExportUtil.generateMonthlyAttendanceCsv(
            monthStr = currentMonthStr,
            companyName = uiState.companyProfile.companyName,
            employees = uiState.employees,
            attendanceList = uiState.allAttendance
        )
        val csvFileName = "AlfaGlazing_AttendanceReport_${currentMonthStr.replace("-", "_")}.csv"

        ExportReportDialog(
            title = "Attendance Sheet ($dateStr)",
            reportText = reportBuilder.toString(),
            csvContent = csvData,
            csvFileName = csvFileName,
            onDismiss = { showExportDialog = false }
        )
    }
}

@Composable
fun AttendanceCountPill(
    label: String,
    count: Int,
    color: Color,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) color else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "$count",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = color
                )
            }
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AttendanceWorkerCard(
    employee: EmployeeEntity,
    record: AttendanceEntity?,
    dateStr: String,
    isUserAdmin: Boolean = true,
    loggedInMobile: String = "",
    loggedInEmail: String = "",
    onStatusChange: (String) -> Unit,
    onOvertimeChange: (Double) -> Unit,
    onOpenRemarkDialog: () -> Unit
) {
    val currentStatus = record?.status ?: "UNMARKED"
    val currentOt = record?.overtimeHours ?: 0.0
    val currentRemark = record?.remarks ?: ""

    val isSelf = !isUserAdmin && (loggedInMobile.isNotBlank() && employee.phone.contains(loggedInMobile.takeLast(10)))
    val canModify = isUserAdmin || isSelf

    val statusColor = when (currentStatus) {
        "PRESENT" -> PresentGreen
        "ABSENT" -> AbsentRed
        "LEAVE" -> LeavePurple
        "HALF_DAY" -> HalfDayOrange
        else -> Color.Gray
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("worker_card_${employee.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Worker identity & current status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Initial Avatar with Status Indicator Ring
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.15f))
                        .border(1.5.dp, statusColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = employee.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = employee.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isSelf) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "YOU",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "${employee.employeeCode} • ${employee.designation}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${employee.dailyWage.toInt()}/day",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    // Status Badge Pill
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = statusColor.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = when (currentStatus) {
                                "PRESENT" -> "Present"
                                "ABSENT" -> "Absent"
                                "LEAVE" -> "On Leave"
                                "HALF_DAY" -> "Half Day"
                                else -> "Unmarked"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Primary 1-Tap Attendance Buttons: Present, Absent, On Leave, Half Day
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AttendanceSegmentButton(
                    label = "Present",
                    icon = Icons.Default.CheckCircle,
                    isSelected = currentStatus == "PRESENT",
                    color = PresentGreen,
                    enabled = canModify,
                    modifier = Modifier.weight(1f),
                    testTag = "present_btn_${employee.id}",
                    onClick = { if (canModify) onStatusChange("PRESENT") }
                )

                AttendanceSegmentButton(
                    label = "Absent",
                    icon = Icons.Default.Cancel,
                    isSelected = currentStatus == "ABSENT",
                    color = AbsentRed,
                    enabled = canModify,
                    modifier = Modifier.weight(1f),
                    testTag = "absent_btn_${employee.id}",
                    onClick = { if (canModify) onStatusChange("ABSENT") }
                )

                AttendanceSegmentButton(
                    label = "Leave",
                    icon = Icons.Default.EventBusy,
                    isSelected = currentStatus == "LEAVE",
                    color = LeavePurple,
                    enabled = canModify,
                    modifier = Modifier.weight(1f),
                    testTag = "leave_btn_${employee.id}",
                    onClick = { if (canModify) onStatusChange("LEAVE") }
                )

                AttendanceSegmentButton(
                    label = "Half Day",
                    icon = Icons.Default.Schedule,
                    isSelected = currentStatus == "HALF_DAY",
                    color = HalfDayOrange,
                    enabled = canModify,
                    modifier = Modifier.weight(1f),
                    testTag = "half_day_btn_${employee.id}",
                    onClick = { if (canModify) onStatusChange("HALF_DAY") }
                )
            }

            // On Leave Reason Highlight Box
            if (currentStatus == "LEAVE") {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = LeavePurple.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, LeavePurple.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.EventBusy,
                                contentDescription = null,
                                tint = LeavePurple,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (currentRemark.isNotBlank()) "Reason: $currentRemark" else "Marked On Leave",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = LeavePurple
                                )
                                if (currentRemark.isBlank()) {
                                    Text(
                                        text = "Tap to specify reason (e.g. Sick, Family)",
                                        fontSize = 10.sp,
                                        color = LeavePurple.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }

                        if (canModify) {
                            FilledTonalButton(
                                onClick = onOpenRemarkDialog,
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (currentRemark.isNotBlank()) "Edit" else "+ Reason",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Overtime Bar for Present or Half Day workers
            if (currentStatus == "PRESENT" || currentStatus == "HALF_DAY") {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.MoreTime,
                            contentDescription = null,
                            tint = OvertimeAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Overtime (OT):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(0.0, 1.0, 2.0, 3.0, 4.0).forEach { hours ->
                            val isSelected = currentOt == hours
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) OvertimeAmber else Color.Transparent)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) OvertimeAmber else Color.Gray.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable(enabled = isUserAdmin) { if (isUserAdmin) onOvertimeChange(hours) }
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (hours == 0.0) "0h" else "${hours.toInt()}h",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // General Remark display if present for any other status
            if (currentStatus != "LEAVE" && currentRemark.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Default.Notes,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Note: $currentRemark",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun AttendanceSegmentButton(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    color: Color,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    testTag: String = "",
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .testTag(testTag)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) color else color.copy(alpha = if (enabled) 0.1f else 0.04f))
            .border(
                width = 1.2.dp,
                color = if (isSelected) color else color.copy(alpha = 0.25f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else if (enabled) color else color.copy(alpha = 0.4f),
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else if (enabled) color else color.copy(alpha = 0.4f),
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AttendanceLeaveReasonDialog(
    employee: EmployeeEntity,
    currentRecord: AttendanceEntity?,
    dateStr: String,
    onDismiss: () -> Unit,
    onSaveRemark: (String) -> Unit
) {
    var remarkText by remember { mutableStateOf(currentRecord?.remarks ?: "") }
    val quickReasons = listOf(
        "Sick / Medical Leave",
        "Family Emergency",
        "Personal Work",
        "Casual Leave",
        "Festival / Out of Station",
        "Pre-approved Leave",
        "Uninformed Absence"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.EventBusy,
                contentDescription = null,
                tint = LeavePurple,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Leave Reason & Remarks",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${employee.name} (${employee.employeeCode}) • $dateStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Select Quick Reason:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickReasons.forEach { reason ->
                        val isSelected = remarkText.equals(reason, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { remarkText = reason },
                            label = { Text(reason, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LeavePurple.copy(alpha = 0.2f),
                                selectedLabelColor = LeavePurple
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = remarkText,
                    onValueChange = { remarkText = it },
                    label = { Text("Custom Reason or Note") },
                    placeholder = { Text("e.g., Doctor appointment, visiting hometown...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveRemark(remarkText.trim())
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = LeavePurple),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Mark On Leave with Reason", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
