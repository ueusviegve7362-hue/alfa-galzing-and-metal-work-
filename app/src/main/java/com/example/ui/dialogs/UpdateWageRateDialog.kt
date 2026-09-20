package com.example.ui.dialogs

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entity.EmployeeEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.AlfaGlazingUiState
import com.example.ui.viewmodel.AlfaGlazingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateWageRateDialog(
    initialEmployee: EmployeeEntity? = null,
    uiState: AlfaGlazingUiState,
    viewModel: AlfaGlazingViewModel,
    onDismiss: () -> Unit
) {
    var selectedEmp by remember { mutableStateOf(initialEmployee ?: uiState.employees.firstOrNull()) }
    var dailyWageText by remember { mutableStateOf(selectedEmp?.dailyWage?.toInt()?.toString() ?: "800") }
    var overtimeRateText by remember { mutableStateOf(selectedEmp?.overtimeRatePerHour?.toInt()?.toString() ?: "100") }
    var applyToSameDesignation by remember { mutableStateOf(false) }

    // When selectedEmp changes, update fields
    LaunchedEffect(selectedEmp) {
        selectedEmp?.let { emp ->
            dailyWageText = emp.dailyWage.toInt().toString()
            overtimeRateText = emp.overtimeRatePerHour.toInt().toString()
        }
    }

    var isEmpDropdownExpanded by remember { mutableStateOf(false) }

    val currency = uiState.companyProfile.currencySymbol
    val dailyWageVal = dailyWageText.toDoubleOrNull() ?: 0.0
    val otRateVal = overtimeRateText.toDoubleOrNull() ?: 0.0

    // Preview calculation for 26 days standard month
    val est26DayWage = dailyWageVal * 26
    val est40HrOt = otRateVal * 40

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
                        text = "Wage Rate Management",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GlazingBluePrimary
                    )
                    Text(
                        text = "Update Daily Wage & Overtime Rates",
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
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Employee Selection Dropdown
                ExposedDropdownMenuBox(
                    expanded = isEmpDropdownExpanded,
                    onExpandedChange = { isEmpDropdownExpanded = !isEmpDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedEmp?.let { "${it.name} (${it.employeeCode}) - ${it.designation}" } ?: "Select Employee",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Worker to Update *") },
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isEmpDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("wage_rate_emp_select"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = isEmpDropdownExpanded,
                        onDismissRequest = { isEmpDropdownExpanded = false }
                    ) {
                        uiState.employees.forEach { emp ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(emp.name, fontWeight = FontWeight.Bold)
                                        Text("${emp.employeeCode} • ${emp.designation} • $currency${emp.dailyWage.toInt()}/day", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                onClick = {
                                    selectedEmp = emp
                                    isEmpDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                if (selectedEmp != null) {
                    val emp = selectedEmp!!

                    // Current Rate Card
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Current Active Wage", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$currency${emp.dailyWage.toInt()} / day", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Column {
                                Text("Current OT Rate", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$currency${emp.overtimeRatePerHour.toInt()} / hr", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = OvertimeAmber)
                            }
                        }
                    }

                    // Input Fields for New Rates
                    Text(
                        text = "Enter Updated Daily & Hourly Rates:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = dailyWageText,
                            onValueChange = { dailyWageText = it },
                            label = { Text("Daily Wage Rate ($currency)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("update_daily_wage_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = overtimeRateText,
                            onValueChange = { overtimeRateText = it },
                            label = { Text("OT Rate/Hour ($currency)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null, tint = OvertimeAmber) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("update_ot_rate_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Calculation Engine Preview Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Text("Salary Engine Calculation Impact", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Updating rates automatically recalculates all unfinalized month salary sheets, payslips, and gross wages for ${emp.name}.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Est. 26-Day Wage", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("$currency${est26DayWage.toInt()}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Column {
                                    Text("Est. 40h OT Pay", fontSize = 11.sp, color = OvertimeAmber)
                                    Text("$currency${est40HrOt.toInt()}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = OvertimeAmber)
                                }
                                Column {
                                    Text("Est. Total Monthly", fontSize = 11.sp, color = PresentGreen)
                                    Text("$currency${(est26DayWage + est40HrOt).toInt()}", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = PresentGreen)
                                }
                            }
                        }
                    }

                    // Bulk Option to apply to same designation
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = applyToSameDesignation,
                            onCheckedChange = { applyToSameDesignation = it }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Apply this rate to all ${emp.designation}s (${uiState.employees.count { it.designation == emp.designation }} workers)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Text("No workers available in the directory.", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val emp = selectedEmp
                    if (emp != null && dailyWageVal > 0) {
                        if (applyToSameDesignation) {
                            viewModel.updateWageRatesForDesignation(
                                designation = emp.designation,
                                newDailyWage = dailyWageVal,
                                newOvertimeRate = otRateVal
                            )
                        } else {
                            viewModel.updateEmployeeWageRate(
                                employeeId = emp.id,
                                newDailyWage = dailyWageVal,
                                newOvertimeRate = otRateVal
                            )
                        }
                        onDismiss()
                    }
                },
                enabled = selectedEmp != null && dailyWageVal > 0,
                modifier = Modifier.testTag("save_wage_rate_btn"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Apply Updated Rates")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
