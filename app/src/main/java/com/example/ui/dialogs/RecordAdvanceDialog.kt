package com.example.ui.dialogs

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.example.data.local.entity.AdvancePaymentEntity
import com.example.data.local.entity.EmployeeEntity
import com.example.ui.theme.AbsentRed
import com.example.ui.theme.GlazingBluePrimary
import com.example.ui.theme.PresentGreen
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecordAdvanceDialog(
    employees: List<EmployeeEntity>,
    allAdvances: List<AdvancePaymentEntity> = emptyList(),
    preselectedEmployeeId: Long? = null,
    currencySymbol: String = "₹",
    onDismiss: () -> Unit,
    onSave: (employeeId: Long, amount: Double, note: String, dateTimestamp: Long) -> Unit
) {
    val context = LocalContext.current
    var selectedEmpId by remember {
        mutableStateOf(preselectedEmployeeId ?: employees.firstOrNull()?.id ?: 0L)
    }
    var amountText by remember { mutableStateOf("1000") }
    var noteText by remember { mutableStateOf("") }
    var selectedPaymentMethod by remember { mutableStateOf("Cash") }
    var isEmpDropdownExpanded by remember { mutableStateOf(false) }

    // Date state
    var selectedDateCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    val dateDisplaySdf = remember { SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault()) }
    val formattedDateText = remember(selectedDateCalendar.timeInMillis) {
        dateDisplaySdf.format(selectedDateCalendar.time)
    }

    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance()
                newCal.set(year, month, dayOfMonth)
                // preserve current time of day
                val now = Calendar.getInstance()
                newCal.set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY))
                newCal.set(Calendar.MINUTE, now.get(Calendar.MINUTE))
                selectedDateCalendar = newCal
            },
            selectedDateCalendar.get(Calendar.YEAR),
            selectedDateCalendar.get(Calendar.MONTH),
            selectedDateCalendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    var hasAttemptedSubmit by remember { mutableStateOf(false) }
    var isAmountTouched by remember { mutableStateOf(false) }

    // Numeric input sanitization (only allows digits and at most 1 decimal separator with max 2 decimal places)
    fun sanitizeNumericInput(input: String): String {
        val filtered = input.filter { it.isDigit() || it == '.' }
        val dotIndex = filtered.indexOf('.')
        return if (dotIndex != -1) {
            val integerPart = filtered.substring(0, dotIndex)
            val decimalPart = filtered.substring(dotIndex + 1).replace(".", "")
            val limitedDecimal = if (decimalPart.length > 2) decimalPart.substring(0, 2) else decimalPart
            "$integerPart.$limitedDecimal"
        } else {
            filtered
        }
    }

    // Amount validation rules
    fun getAmountValidationError(text: String): String? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return "Advance amount cannot be empty"
        }
        val parsed = trimmed.toDoubleOrNull()
        if (parsed == null) {
            return "Please enter a valid numeric value"
        }
        if (parsed.isNaN() || parsed.isInfinite()) {
            return "Amount must be a finite number"
        }
        if (parsed <= 0.0) {
            return "Amount must be greater than 0"
        }
        if (parsed > 10_000_000.0) {
            return "Amount exceeds maximum limit ($currencySymbol 10,000,000)"
        }
        return null
    }

    val amountError = remember(amountText) { getAmountValidationError(amountText) }
    val isAmountValid = amountError == null
    val showAmountError = (hasAttemptedSubmit || isAmountTouched) && !isAmountValid
    val isEmpValid = selectedEmpId > 0L && employees.any { it.id == selectedEmpId }

    val selectedEmp = employees.find { it.id == selectedEmpId } ?: employees.firstOrNull()

    // Calculate existing advances for selected employee
    val existingEmpAdvances = remember(selectedEmpId, allAdvances) {
        allAdvances.filter { it.employeeId == selectedEmpId }
    }
    val existingTotalAdvance = remember(existingEmpAdvances) {
        existingEmpAdvances.sumOf { it.amount }
    }

    val quickAmounts = listOf(500, 1000, 2000, 5000)
    val quickReasons = listOf(
        "Weekly Advance",
        "Medical / Emergency",
        "Festival Advance",
        "Family Support",
        "Travel / Site Food",
        "Personal Loan"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(AbsentRed.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PriceChange,
                        contentDescription = null,
                        tint = AbsentRed,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = "Record Advance Payment",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Link advance payment with date & employee",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Employee Selection & Status Info
                ExposedDropdownMenuBox(
                    expanded = isEmpDropdownExpanded,
                    onExpandedChange = { isEmpDropdownExpanded = !isEmpDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = "${selectedEmp?.name ?: "Select Worker"} (${selectedEmp?.employeeCode ?: ""})",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Link to Employee *") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isEmpDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = isEmpDropdownExpanded,
                        onDismissRequest = { isEmpDropdownExpanded = false }
                    ) {
                        employees.forEach { emp ->
                            DropdownMenuItem(
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            emp.name.take(1).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                },
                                text = {
                                    Column {
                                        Text(emp.name, fontWeight = FontWeight.Bold)
                                        Text(
                                            "${emp.employeeCode} • ${emp.designation} (Wage: $currencySymbol${emp.dailyWage.toInt()}/day)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    selectedEmpId = emp.id
                                    isEmpDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Employee Quick Stats Pill (Wage & Current Advances)
                selectedEmp?.let { emp ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Daily Wage: $currencySymbol${emp.dailyWage.toInt()}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Total Advances Taken: $currencySymbol${existingTotalAdvance.toInt()}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (existingTotalAdvance > 0) AbsentRed else PresentGreen
                            )
                        }
                    }
                }

                // 2. Payment Date Picker
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { datePickerDialog.show() }
                        .testTag("advance_date_picker_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Payment Date *",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formattedDateText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Icon(
                            Icons.Default.EditCalendar,
                            contentDescription = "Change Date",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // 3. Advance Amount Field with validation
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        isAmountTouched = true
                        amountText = sanitizeNumericInput(input)
                    },
                    label = { Text("Advance Amount ($currencySymbol) *") },
                    leadingIcon = {
                        Text(
                            text = currencySymbol,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(start = 12.dp, end = 4.dp),
                            color = if (showAmountError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (showAmountError) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Amount Error",
                                tint = MaterialTheme.colorScheme.error
                            )
                        } else if (isAmountValid && amountText.isNotBlank()) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Valid Amount",
                                tint = PresentGreen
                            )
                        }
                    },
                    supportingText = {
                        if (showAmountError) {
                            Text(
                                text = amountError ?: "Invalid amount",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else if (isAmountValid && amountText.isNotBlank()) {
                            val parsed = amountText.toDoubleOrNull()
                            if (parsed != null) {
                                Text(
                                    text = "✓ $currencySymbol${String.format(Locale.getDefault(), "%,.2f", parsed)} will be debited as advance",
                                    color = PresentGreen,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    },
                    isError = showAmountError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("advance_amount_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Quick Increment Amount Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickAmounts.forEach { amt ->
                        SuggestionChip(
                            onClick = {
                                isAmountTouched = true
                                val current = amountText.toDoubleOrNull() ?: 0.0
                                amountText = (current + amt).toInt().toString()
                            },
                            label = { Text("+$currencySymbol$amt", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 4. Payment Method Selection
                Text(
                    text = "Payment Mode:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Cash", "UPI", "Bank Transfer").forEach { method ->
                        val isSelected = selectedPaymentMethod == method
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedPaymentMethod = method },
                            label = { Text(method, fontSize = 12.sp) },
                            leadingIcon = {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 5. Reason / Note Field with Quick Tags
                Text(
                    text = "Reason / Purpose (Optional):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickReasons.forEach { reason ->
                        val isSelected = noteText.contains(reason, ignoreCase = true)
                        SuggestionChip(
                            onClick = {
                                noteText = if (noteText.isBlank()) reason else "$noteText - $reason"
                            },
                            label = { Text(reason, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Note / Description") },
                    placeholder = { Text("e.g. Festival advance, emergency medical aid...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    hasAttemptedSubmit = true
                    isAmountTouched = true
                    val parsedAmt = amountText.toDoubleOrNull()
                    if (isAmountValid && parsedAmt != null && parsedAmt > 0 && isEmpValid) {
                        val cleanAmount = Math.round(parsedAmt * 100.0) / 100.0
                        val combinedNote = buildString {
                            if (noteText.isNotBlank()) append(noteText.trim())
                            if (selectedPaymentMethod.isNotBlank()) {
                                if (isNotEmpty()) append(" • ")
                                append(selectedPaymentMethod)
                            }
                        }
                        onSave(selectedEmpId, cleanAmount, combinedNote, selectedDateCalendar.timeInMillis)
                    }
                },
                enabled = isAmountValid && isEmpValid,
                modifier = Modifier.testTag("save_advance_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AbsentRed)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Record Advance", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
