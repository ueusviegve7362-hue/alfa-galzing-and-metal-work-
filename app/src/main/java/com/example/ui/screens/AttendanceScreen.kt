package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    uiState: AlfaGlazingUiState,
    viewModel: AlfaGlazingViewModel
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var showExportDialog by remember { mutableStateOf(false) }
    var showRoleDialog by remember { mutableStateOf(false) }

    val dateStr = uiState.selectedDate
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Filter employees by search
    val filteredEmployees = uiState.employees.filter { emp ->
        emp.name.contains(searchQuery, ignoreCase = true) ||
                emp.employeeCode.contains(searchQuery, ignoreCase = true) ||
                emp.designation.contains(searchQuery, ignoreCase = true)
    }

    val todayAttendance = uiState.allAttendance.filter { it.dateString == dateStr }
    val presentCount = todayAttendance.count { it.status == "PRESENT" }
    val halfDayCount = todayAttendance.count { it.status == "HALF_DAY" }
    val absentCount = todayAttendance.count { it.status == "ABSENT" }
    val totalOtHours = todayAttendance.sumOf { it.overtimeHours }

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
        // Date Selector & Navigation Bar
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
                            calendar.add(Calendar.DAY_OF_MONTH, -1)
                            viewModel.setSelectedDate(sdf.format(calendar.time))
                        }
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day")
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable { datePickerDialog.show() }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = dateStr,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    IconButton(
                        onClick = {
                            calendar.add(Calendar.DAY_OF_MONTH, 1)
                            viewModel.setSelectedDate(sdf.format(calendar.time))
                        }
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Day")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Attendance Counters Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Text(
                        text = "Present: $presentCount",
                        fontWeight = FontWeight.Bold,
                        color = PresentGreen,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Half Day: $halfDayCount",
                        fontWeight = FontWeight.Bold,
                        color = HalfDayOrange,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Absent: $absentCount",
                        fontWeight = FontWeight.Bold,
                        color = AbsentRed,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "OT: ${totalOtHours.toInt()}h",
                        fontWeight = FontWeight.Bold,
                        color = OvertimeAmber,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bulk Mark Buttons & Search
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.markBulkAttendance("PRESENT") },
                        enabled = uiState.isUserAdmin,
                        colors = ButtonDefaults.buttonColors(containerColor = PresentGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bulk_present_btn")
                    ) {
                        Text("All Present", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.markBulkAttendance("ABSENT") },
                        enabled = uiState.isUserAdmin,
                        colors = ButtonDefaults.buttonColors(containerColor = AbsentRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bulk_absent_btn")
                    ) {
                        Text("All Absent", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { showExportDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("export_attendance_btn")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search worker by name or code...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
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

            items(filteredEmployees, key = { it.id }) { emp ->
                val empRecord = todayAttendance.find { it.employeeId == emp.id }
                AttendanceWorkerCard(
                    employee = emp,
                    record = empRecord,
                    isUserAdmin = uiState.isUserAdmin,
                    loggedInMobile = uiState.companyProfile.loggedInMobileNumber,
                    loggedInEmail = uiState.companyProfile.loggedInEmail,
                    onStatusChange = { newStatus ->
                        if (uiState.isUserAdmin) {
                            viewModel.markAttendance(
                                employeeId = emp.id,
                                status = newStatus,
                                overtimeHours = empRecord?.overtimeHours ?: 0.0,
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
                    }
                )
            }
        }
    }

    if (showExportDialog) {
        val reportBuilder = StringBuilder()
        reportBuilder.append("📋 ALFA GLAZING ATTENDANCE SHEET\n")
        reportBuilder.append("Date: $dateStr\n")
        reportBuilder.append("-----------------------------------\n")
        reportBuilder.append("Total Workers: ${filteredEmployees.size}\n")
        reportBuilder.append("Present: $presentCount | Half Day: $halfDayCount | Absent: $absentCount\n")
        reportBuilder.append("Total Overtime: ${totalOtHours} hrs\n")
        reportBuilder.append("-----------------------------------\n\n")

        filteredEmployees.forEachIndexed { index, emp ->
            val rec = todayAttendance.find { it.employeeId == emp.id }
            val statusStr = rec?.status ?: "UNMARKED"
            val otStr = if ((rec?.overtimeHours ?: 0.0) > 0) " [OT: ${rec?.overtimeHours}h]" else ""
            reportBuilder.append("${index + 1}. ${emp.name} (${emp.employeeCode}): $statusStr$otStr\n")
        }

        ExportReportDialog(
            title = "Attendance Sheet Summary",
            reportText = reportBuilder.toString(),
            onDismiss = { showExportDialog = false }
        )
    }
}

@Composable
fun AttendanceWorkerCard(
    employee: EmployeeEntity,
    record: AttendanceEntity?,
    isUserAdmin: Boolean = true,
    loggedInMobile: String = "",
    loggedInEmail: String = "",
    onStatusChange: (String) -> Unit,
    onOvertimeChange: (Double) -> Unit
) {
    val currentStatus = record?.status ?: "UNMARKED"
    val currentOt = record?.overtimeHours ?: 0.0

    val isSelf = !isUserAdmin && (loggedInMobile.isNotBlank() && employee.phone.contains(loggedInMobile.takeLast(10)))
    val canModify = isUserAdmin || isSelf

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
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            when (currentStatus) {
                                "PRESENT" -> PresentGreen.copy(alpha = 0.2f)
                                "HALF_DAY" -> HalfDayOrange.copy(alpha = 0.2f)
                                "ABSENT" -> AbsentRed.copy(alpha = 0.2f)
                                "LEAVE" -> LeavePurple.copy(alpha = 0.2f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = employee.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = when (currentStatus) {
                            "PRESENT" -> PresentGreen
                            "HALF_DAY" -> HalfDayOrange
                            "ABSENT" -> AbsentRed
                            "LEAVE" -> LeavePurple
                            else -> MaterialTheme.colorScheme.onSurface
                        }
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

                Text(
                    text = "₹${employee.dailyWage.toInt()}/day",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Attendance Status Segment Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AttendanceSegmentButton(
                    label = "Present",
                    isSelected = currentStatus == "PRESENT",
                    color = PresentGreen,
                    enabled = canModify,
                    modifier = Modifier.weight(1f),
                    onClick = { if (canModify) onStatusChange("PRESENT") }
                )

                AttendanceSegmentButton(
                    label = "Half Day",
                    isSelected = currentStatus == "HALF_DAY",
                    color = HalfDayOrange,
                    enabled = canModify,
                    modifier = Modifier.weight(1f),
                    onClick = { if (canModify) onStatusChange("HALF_DAY") }
                )

                AttendanceSegmentButton(
                    label = "Absent",
                    isSelected = currentStatus == "ABSENT",
                    color = AbsentRed,
                    enabled = canModify,
                    modifier = Modifier.weight(1f),
                    onClick = { if (canModify) onStatusChange("ABSENT") }
                )

                AttendanceSegmentButton(
                    label = "Leave",
                    isSelected = currentStatus == "LEAVE",
                    color = LeavePurple,
                    enabled = canModify,
                    modifier = Modifier.weight(1f),
                    onClick = { if (canModify) onStatusChange("LEAVE") }
                )
            }

            // Overtime Bar if Present or Half Day
            if (currentStatus == "PRESENT" || currentStatus == "HALF_DAY") {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Overtime (OT):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

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
                                        color = if (isSelected) OvertimeAmber else Color.Gray,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable(enabled = isUserAdmin) { if (isUserAdmin) onOvertimeChange(hours) }
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
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
        }
    }
}

@Composable
fun AttendanceSegmentButton(
    label: String,
    isSelected: Boolean,
    color: Color,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) color else color.copy(alpha = if (enabled) 0.1f else 0.05f))
            .border(
                width = 1.dp,
                color = if (isSelected) color else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else if (enabled) color else color.copy(alpha = 0.5f)
        )
    }
}
