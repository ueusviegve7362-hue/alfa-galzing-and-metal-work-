package com.example.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AdvancePaymentEntity
import com.example.ui.dialogs.ExportReportDialog
import com.example.ui.dialogs.RoleStatusBanner
import com.example.ui.dialogs.RoleSwitchDialog
import com.example.ui.theme.AbsentRed
import com.example.ui.theme.GlazingBluePrimary
import com.example.ui.theme.PresentGreen
import com.example.ui.viewmodel.AlfaGlazingUiState
import com.example.ui.viewmodel.AlfaGlazingViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancesScreen(
    uiState: AlfaGlazingUiState,
    viewModel: AlfaGlazingViewModel,
    onOpenRecordAdvance: () -> Unit,
    onOpenRecordPayment: ((employeeId: Long?, defaultType: String) -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var showRoleDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var filterToSelectedMonthOnly by remember { mutableStateOf(true) }
    var selectedEmployeeFilterId by remember { mutableStateOf<Long?>(null) }
    var advanceToDelete by remember { mutableStateOf<AdvancePaymentEntity?>(null) }

    val monthStr = uiState.selectedMonth
    val currency = uiState.companyProfile.currencySymbol

    val monthFormat = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()) }
    val monthDisplayFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val formattedMonthName = remember(monthStr) {
        try {
            val parsed = monthFormat.parse(monthStr)
            if (parsed != null) monthDisplayFormat.format(parsed) else monthStr
        } catch (e: Exception) {
            monthStr
        }
    }

    // Filter by period (selected month or all-time)
    val baseAdvances = remember(uiState.allAdvances, monthStr, filterToSelectedMonthOnly) {
        if (filterToSelectedMonthOnly) {
            uiState.allAdvances.filter { it.monthYear == monthStr }
        } else {
            uiState.allAdvances
        }
    }

    val totalAdvancesAmount = remember(baseAdvances) { baseAdvances.sumOf { it.amount } }
    val distinctWorkersCount = remember(baseAdvances) { baseAdvances.map { it.employeeId }.distinct().size }

    // Filter by employee filter and search query
    val displayedAdvances = baseAdvances.filter { adv ->
        val matchesEmployee = selectedEmployeeFilterId == null || adv.employeeId == selectedEmployeeFilterId
        if (!matchesEmployee) return@filter false

        val emp = uiState.employees.find { it.id == adv.employeeId }
        val empName = emp?.name ?: ""
        val empCode = emp?.employeeCode ?: ""
        empName.contains(searchQuery, ignoreCase = true) ||
                empCode.contains(searchQuery, ignoreCase = true) ||
                adv.note.contains(searchQuery, ignoreCase = true)
    }.sortedByDescending { it.dateTimestamp }

    val sdf = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    if (showRoleDialog) {
        RoleSwitchDialog(
            currentRole = uiState.companyProfile.currentUserRole,
            viewModel = viewModel,
            onDismiss = { showRoleDialog = false }
        )
    }

    // Delete Confirmation Dialog
    advanceToDelete?.let { adv ->
        val emp = uiState.employees.find { it.id == adv.employeeId }
        val dateText = sdf.format(Date(adv.dateTimestamp))
        AlertDialog(
            onDismissRequest = { advanceToDelete = null },
            icon = {
                Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            },
            title = {
                Text("Delete Advance Record?", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Are you sure you want to delete this advance payment record?")
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Worker: ${emp?.name ?: "Worker #${adv.employeeId}"}", fontWeight = FontWeight.Bold)
                            Text("Amount: $currency${adv.amount.toInt()}", color = AbsentRed, fontWeight = FontWeight.Bold)
                            Text("Date: $dateText", fontSize = 12.sp)
                            if (adv.note.isNotBlank()) {
                                Text("Note: ${adv.note}", fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAdvancePayment(adv)
                        advanceToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Record")
                }
            },
            dismissButton = {
                TextButton(onClick = { advanceToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            if (uiState.isUserAdmin) {
                ExtendedFloatingActionButton(
                    onClick = { onOpenRecordAdvance() },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Record Advance", fontWeight = FontWeight.Bold) },
                    containerColor = AbsentRed,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("record_advance_fab")
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

            // Header & Summary Controls Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    // Month Selector & Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Advance Payments Tracker",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (filterToSelectedMonthOnly) formattedMonthName else "All-Time Records",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showExportDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("share_advances_btn")
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Export", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Month Navigation & Period Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    val cal = Calendar.getInstance()
                                    try {
                                        cal.time = monthFormat.parse(monthStr) ?: Date()
                                        cal.add(Calendar.MONTH, -1)
                                        viewModel.setSelectedMonth(monthFormat.format(cal.time))
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                            }

                            Text(
                                text = formattedMonthName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            IconButton(
                                onClick = {
                                    val cal = Calendar.getInstance()
                                    try {
                                        cal.time = monthFormat.parse(monthStr) ?: Date()
                                        cal.add(Calendar.MONTH, 1)
                                        viewModel.setSelectedMonth(monthFormat.format(cal.time))
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                            }
                        }

                        FilterChip(
                            selected = !filterToSelectedMonthOnly,
                            onClick = { filterToSelectedMonthOnly = !filterToSelectedMonthOnly },
                            label = {
                                Text(
                                    if (filterToSelectedMonthOnly) "Show All Time" else "Filter by Month",
                                    fontSize = 11.sp
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Summary Stats Pill Container
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AbsentRed.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, AbsentRed.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Total Issued",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$currency${totalAdvancesAmount.toInt()}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = AbsentRed
                                )
                            }

                            VerticalDivider(modifier = Modifier.height(30.dp), color = AbsentRed.copy(alpha = 0.2f))

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Transactions",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${baseAdvances.size}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            VerticalDivider(modifier = Modifier.height(30.dp), color = AbsentRed.copy(alpha = 0.2f))

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Workers",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$distinctWorkersCount",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Worker Filter Chips Row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = selectedEmployeeFilterId == null,
                                onClick = { selectedEmployeeFilterId = null },
                                label = { Text("All Workers", fontSize = 11.sp) }
                            )
                        }
                        items(uiState.employees, key = { it.id }) { emp ->
                            val empTotal = baseAdvances.filter { it.employeeId == emp.id }.sumOf { it.amount }
                            val isSelected = selectedEmployeeFilterId == emp.id
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedEmployeeFilterId = if (isSelected) null else emp.id
                                },
                                label = {
                                    Text(
                                        "${emp.name} ($currency${empTotal.toInt()})",
                                        fontSize = 11.sp
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AbsentRed.copy(alpha = 0.15f),
                                    selectedLabelColor = AbsentRed
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Search input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search worker name, code, or payment note...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("advances_search_input"),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // Advances List
            if (displayedAdvances.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (filterToSelectedMonthOnly)
                                "No advance payments recorded for $formattedMonthName"
                            else
                                "No advance payments found matching criteria",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (uiState.isUserAdmin) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onOpenRecordAdvance,
                                colors = ButtonDefaults.buttonColors(containerColor = AbsentRed)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Issue New Advance")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(displayedAdvances, key = { it.id }) { adv ->
                        val emp = uiState.employees.find { it.id == adv.employeeId }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(1.5.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("advance_card_${adv.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Worker Avatar with Red Accent
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(AbsentRed.copy(alpha = 0.12f))
                                        .border(1.5.dp, AbsentRed.copy(alpha = 0.4f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = emp?.name?.take(1)?.uppercase() ?: "#",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = AbsentRed
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = emp?.name ?: "Worker #${adv.employeeId}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (emp != null) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "• ${emp.employeeCode}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    if (emp != null && emp.designation.isNotBlank()) {
                                        Text(
                                            text = emp.designation,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    // Payment Date & Time
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = sdf.format(Date(adv.dateTimestamp)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (adv.note.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Note: ${adv.note}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    // Amount Pill
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = AbsentRed.copy(alpha = 0.1f),
                                        border = BorderStroke(1.dp, AbsentRed.copy(alpha = 0.3f))
                                    ) {
                                        Text(
                                            text = "- $currency${adv.amount.toInt()}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = AbsentRed,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }

                                    // Delete Option for Admin
                                    if (uiState.isUserAdmin) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        IconButton(
                                            onClick = { advanceToDelete = adv },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                contentDescription = "Delete Advance",
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showExportDialog) {
        val sb = StringBuilder()
        sb.append("==================================\n")
        sb.append("   ALFA GLAZING ADVANCE REGISTER\n")
        sb.append("==================================\n")
        sb.append("Company: ${uiState.companyProfile.companyName}\n")
        sb.append("Period: ${if (filterToSelectedMonthOnly) formattedMonthName else "All Time"}\n")
        sb.append("Total Advance Issued: $currency${totalAdvancesAmount.toInt()}\n")
        sb.append("Total Records: ${displayedAdvances.size}\n")
        sb.append("==================================\n\n")

        if (displayedAdvances.isEmpty()) {
            sb.append("No advance payments recorded for this period.\n")
        } else {
            displayedAdvances.forEachIndexed { idx, adv ->
                val emp = uiState.employees.find { it.id == adv.employeeId }
                val empName = emp?.name ?: "Worker #${adv.employeeId}"
                val empCode = emp?.employeeCode ?: ""
                sb.append("${idx + 1}. $empName ($empCode)\n")
                sb.append("   - Amount: $currency${adv.amount.toInt()}\n")
                if (adv.note.isNotBlank()) {
                    sb.append("   - Note: ${adv.note}\n")
                }
                sb.append("   - Date: ${sdf.format(Date(adv.dateTimestamp))}\n")
                sb.append("   - Month Group: ${adv.monthYear}\n\n")
            }
        }

        ExportReportDialog(
            title = "Advance Cash Register (${if (filterToSelectedMonthOnly) formattedMonthName else "All Time"})",
            reportText = sb.toString(),
            onDismiss = { showExportDialog = false }
        )
    }
}
