package com.example.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.EmployeeEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEmployeeDialog(
    employeeToEdit: EmployeeEntity? = null,
    onDismiss: () -> Unit,
    onSave: (code: String, name: String, phone: String, designation: String, dailyWage: Double, overtimeRate: Double) -> Unit
) {
    var codeText by remember { mutableStateOf(employeeToEdit?.employeeCode ?: "") }
    var nameText by remember { mutableStateOf(employeeToEdit?.name ?: "") }
    var phoneText by remember { mutableStateOf(employeeToEdit?.phone ?: "") }
    var designationText by remember { mutableStateOf(employeeToEdit?.designation ?: "Glass Fitter") }
    var dailyWageText by remember { mutableStateOf(employeeToEdit?.dailyWage?.toInt()?.toString() ?: "800") }
    var overtimeRateText by remember { mutableStateOf(employeeToEdit?.overtimeRatePerHour?.toInt()?.toString() ?: "100") }

    val designations = listOf(
        "Master Glazier",
        "Aluminum Fabricator",
        "Glass Fitter",
        "Structural Glazier",
        "Assistant Glazier",
        "Crane Operator",
        "Site Helper",
        "Site Supervisor",
        "Safety Inspector"
    )
    var isDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (employeeToEdit == null) "Add New Worker" else "Edit Worker Details",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Worker Full Name *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("employee_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = codeText,
                    onValueChange = { codeText = it },
                    label = { Text("Employee Code (e.g. AG-031)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = phoneText,
                    onValueChange = { phoneText = it },
                    label = { Text("Phone Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenuBox(
                    expanded = isDropdownExpanded,
                    onExpandedChange = { isDropdownExpanded = !isDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = designationText,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Designation / Role") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false }
                    ) {
                        designations.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item) },
                                onClick = {
                                    designationText = item
                                    isDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = dailyWageText,
                        onValueChange = { dailyWageText = it },
                        label = { Text("Daily Wage (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("daily_wage_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = overtimeRateText,
                        onValueChange = { overtimeRateText = it },
                        label = { Text("OT Rate/Hr (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameText.isNotBlank()) {
                        val wage = dailyWageText.toDoubleOrNull() ?: 800.0
                        val otRate = overtimeRateText.toDoubleOrNull() ?: 100.0
                        onSave(codeText, nameText, phoneText, designationText, wage, otRate)
                    }
                },
                modifier = Modifier.testTag("save_employee_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save Worker")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
