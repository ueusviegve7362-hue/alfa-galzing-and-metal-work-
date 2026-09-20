package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PriceChange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AdvancePaymentEntity
import com.example.ui.dialogs.ExportReportDialog
import com.example.ui.dialogs.RoleStatusBanner
import com.example.ui.dialogs.RoleSwitchDialog
import com.example.ui.theme.AbsentRed
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

    val monthStr = uiState.selectedMonth
    val currency = uiState.companyProfile.currencySymbol

    val monthAdvances = uiState.allAdvances.filter { it.monthYear == monthStr }
    val totalMonthAdvance = monthAdvances.sumOf { it.amount }

    val filteredAdvances = monthAdvances.filter { adv ->
        val emp = uiState.employees.find { it.id == adv.employeeId }
        val empName = emp?.name ?: ""
        val empCode = emp?.employeeCode ?: ""
        empName.contains(searchQuery, ignoreCase = true) ||
                empCode.contains(searchQuery, ignoreCase = true) ||
                adv.note.contains(searchQuery, ignoreCase = true)
    }

    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

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
                    onClick = { onOpenRecordPayment?.invoke(null, "ADVANCE") ?: onOpenRecordAdvance() },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Issue Advance / Payment", fontWeight = FontWeight.Bold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
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
                onSwitchRoleClick = { showRoleDialog = true }
            )
            // Summary Card
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
                        Column {
                            Text(
                                text = "Advance Cash Register",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Month: $monthStr",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showExportDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("share_advances_btn")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share", fontSize = 12.sp)
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                    Text(
                                        text = "Total Issued",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = "$currency${totalMonthAdvance.toInt()}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search worker or advance note...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("advances_search_input"),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // Advances List
            if (filteredAdvances.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No advance payments recorded for $monthStr",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredAdvances, key = { it.id }) { adv ->
                        val emp = uiState.employees.find { it.id == adv.employeeId }
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
                                        .background(MaterialTheme.colorScheme.errorContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.PriceChange,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = emp?.name ?: "Worker #${adv.employeeId}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${emp?.employeeCode ?: ""} • ${emp?.designation ?: ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (adv.note.isNotBlank()) {
                                        Text(
                                            text = "Note: ${adv.note}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = sdf.format(Date(adv.dateTimestamp)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Text(
                                    text = "- $currency${adv.amount.toInt()}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = AbsentRed
                                )
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
        sb.append("Month: $monthStr\n")
        sb.append("Total Issued: $currency${totalMonthAdvance.toInt()}\n")
        sb.append("==================================\n\n")

        if (monthAdvances.isEmpty()) {
            sb.append("No advance payments issued for $monthStr.\n")
        } else {
            monthAdvances.forEachIndexed { idx, adv ->
                val emp = uiState.employees.find { it.id == adv.employeeId }
                val empName = emp?.name ?: "Worker #${adv.employeeId}"
                val empCode = emp?.employeeCode ?: ""
                sb.append("${idx + 1}. $empName ($empCode)\n")
                sb.append("   - Amount: $currency${adv.amount.toInt()}\n")
                if (adv.note.isNotBlank()) {
                    sb.append("   - Note: ${adv.note}\n")
                }
                sb.append("   - Date: ${sdf.format(Date(adv.dateTimestamp))}\n\n")
            }
        }

        ExportReportDialog(
            title = "Advance Cash Register ($monthStr)",
            reportText = sb.toString(),
            onDismiss = { showExportDialog = false }
        )
    }
}
