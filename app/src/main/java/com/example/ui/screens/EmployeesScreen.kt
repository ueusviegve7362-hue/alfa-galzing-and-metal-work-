package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.EmployeeEntity
import com.example.ui.dialogs.EmployeeSalaryHistoryDialog
import com.example.ui.dialogs.RoleStatusBanner
import com.example.ui.dialogs.RoleSwitchDialog
import com.example.ui.dialogs.UpdateWageRateDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.AlfaGlazingUiState
import com.example.ui.viewmodel.AlfaGlazingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeesScreen(
    uiState: AlfaGlazingUiState,
    viewModel: AlfaGlazingViewModel,
    onOpenAddEmployee: () -> Unit,
    onEditEmployee: (EmployeeEntity) -> Unit,
    onRecordAdvance: (Long) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedRoleFilter by remember { mutableStateOf("ALL") }
    var workerToDelete by remember { mutableStateOf<EmployeeEntity?>(null) }
    var selectedHistoryEmp by remember { mutableStateOf<EmployeeEntity?>(null) }
    var wageRateEmpToUpdate by remember { mutableStateOf<EmployeeEntity?>(null) }
    var showManageWageRatesDialog by remember { mutableStateOf(false) }
    var showRoleDialog by remember { mutableStateOf(false) }

    val rolesList = listOf("ALL", "Master Glazier", "Aluminum Fabricator", "Glass Fitter", "Structural Glazier", "Site Helper", "Site Supervisor")

    val filteredEmployees = uiState.employees.filter { emp ->
        val matchesQuery = emp.name.contains(searchQuery, ignoreCase = true) ||
                emp.employeeCode.contains(searchQuery, ignoreCase = true) ||
                emp.phone.contains(searchQuery) ||
                emp.designation.contains(searchQuery, ignoreCase = true)

        val matchesRole = selectedRoleFilter == "ALL" || emp.designation.equals(selectedRoleFilter, ignoreCase = true)
        matchesQuery && matchesRole
    }

    if (showRoleDialog) {
        RoleSwitchDialog(
            currentRole = uiState.companyProfile.currentUserRole,
            viewModel = viewModel,
            onDismiss = { showRoleDialog = false }
        )
    }

    Scaffold(
        floatingActionButton = {
            if (uiState.isUserAdmin) {
                ExtendedFloatingActionButton(
                    onClick = onOpenAddEmployee,
                    icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                    text = { Text("Add Worker", fontWeight = FontWeight.Bold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_worker_fab")
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            RoleStatusBanner(
                isUserAdmin = uiState.isUserAdmin,
                loggedInMobile = uiState.companyProfile.loggedInMobileNumber,
                loggedInEmail = uiState.companyProfile.loggedInEmail,
                onSwitchRoleClick = { showRoleDialog = true }
            )
            // Search & Filter Header
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
                        Text(
                            text = "ALFA GLAZING Labor Directory (${uiState.employees.size} Workers)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )

                        if (uiState.isUserAdmin) {
                            OutlinedButton(
                                onClick = { showManageWageRatesDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("manage_wage_rates_header_btn")
                            ) {
                                Icon(Icons.Default.AttachMoney, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Wage Form", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by worker name, code AG-001, phone...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("employee_search_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Role filter chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(rolesList) { role ->
                            FilterChip(
                                selected = selectedRoleFilter == role,
                                onClick = { selectedRoleFilter = role },
                                label = { Text(role, fontSize = 12.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }

            // Workers List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredEmployees, key = { it.id }) { emp ->
                    EmployeeDetailCard(
                        employee = emp,
                        currencySymbol = uiState.companyProfile.currencySymbol,
                        isUserAdmin = uiState.isUserAdmin,
                        onHistory = { selectedHistoryEmp = emp },
                        onUpdateWageRate = { wageRateEmpToUpdate = emp },
                        onEdit = { onEditEmployee(emp) },
                        onAdvance = { onRecordAdvance(emp.id) },
                        onDelete = { workerToDelete = emp }
                    )
                }
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

    // Delete Confirmation Dialog
    if (workerToDelete != null) {
        val emp = workerToDelete!!
        AlertDialog(
            onDismissRequest = { workerToDelete = null },
            title = { Text("Remove Worker?") },
            text = { Text("Are you sure you want to remove ${emp.name} (${emp.employeeCode}) from the ALFA GLAZING roster?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteEmployee(emp)
                        workerToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AbsentRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { workerToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun EmployeeDetailCard(
    employee: EmployeeEntity,
    currencySymbol: String,
    isUserAdmin: Boolean = true,
    onHistory: () -> Unit = {},
    onUpdateWageRate: () -> Unit = {},
    onEdit: () -> Unit,
    onAdvance: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
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
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${employee.employeeCode} • ${employee.designation}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Phone: ${employee.phone}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$currencySymbol${employee.dailyWage.toInt()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "per day",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "OT: $currencySymbol${employee.overtimeRatePerHour.toInt()}/hr",
                        style = MaterialTheme.typography.labelSmall,
                        color = OvertimeAmber,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDelete,
                    enabled = isUserAdmin
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = if (isUserAdmin) AbsentRed else Color.Gray
                    )
                }
                OutlinedButton(
                    onClick = onUpdateWageRate,
                    enabled = isUserAdmin,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Icon(Icons.Default.AttachMoney, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Wage Rate", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onHistory,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("History", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onAdvance,
                    enabled = isUserAdmin,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Icon(Icons.Default.PriceChange, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Advance", fontSize = 12.sp)
                }
                Button(
                    onClick = onEdit,
                    enabled = isUserAdmin,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit", fontSize = 12.sp)
                }
            }
        }
    }
}
