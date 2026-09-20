package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PriceChange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.EmployeeEntity
import com.example.ui.theme.GlazingBluePrimary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordAdvanceDialog(
    employees: List<EmployeeEntity>,
    preselectedEmployeeId: Long? = null,
    onDismiss: () -> Unit,
    onSave: (employeeId: Long, amount: Double, note: String) -> Unit
) {
    var selectedEmpId by remember { mutableStateOf(preselectedEmployeeId ?: employees.firstOrNull()?.id ?: 0L) }
    var amountText by remember { mutableStateOf("1000") }
    var noteText by remember { mutableStateOf("") }
    var isEmpDropdownExpanded by remember { mutableStateOf(false) }

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
            return "Amount must be greater than 0 (positive numeric value)"
        }
        if (parsed > 10_000_000.0) {
            return "Amount exceeds maximum limit (₹10,000,000)"
        }
        return null
    }

    val amountError = remember(amountText) { getAmountValidationError(amountText) }
    val isAmountValid = amountError == null
    val showAmountError = (hasAttemptedSubmit || isAmountTouched) && !isAmountValid
    val isEmpValid = selectedEmpId > 0L && employees.any { it.id == selectedEmpId }

    val selectedEmp = employees.find { it.id == selectedEmpId } ?: employees.firstOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(GlazingBluePrimary.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PriceChange,
                        contentDescription = null,
                        tint = GlazingBluePrimary
                    )
                }
                Column {
                    Text(
                        text = "Record Advance Payment",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Saves to Firestore 'payments' & local records",
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
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Cloud Sync Notice Banner
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Advance Payment • Cloud Firestore Synced",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = isEmpDropdownExpanded,
                    onExpandedChange = { isEmpDropdownExpanded = !isEmpDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = "${selectedEmp?.name ?: "Select"} (${selectedEmp?.employeeCode ?: ""})",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Worker / Labor *") },
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
                                text = { Text("${emp.name} (${emp.employeeCode}) - ${emp.designation}") },
                                onClick = {
                                    selectedEmpId = emp.id
                                    isEmpDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Advance Amount with Validation
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        isAmountTouched = true
                        amountText = sanitizeNumericInput(input)
                    },
                    label = { Text("Advance Amount (₹) *") },
                    leadingIcon = {
                        Text(
                            text = "₹",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
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
                                tint = Color(0xFF2E7D32)
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
                                    text = "✓ Valid numeric value: ₹${String.format(Locale.getDefault(), "%,.2f", parsed)} to Firestore",
                                    color = Color(0xFF2E7D32),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            Text(
                                text = "Only valid positive numbers accepted (e.g. 1000 or 2500.50)",
                                style = MaterialTheme.typography.bodySmall
                            )
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

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Reason / Note (Optional)") },
                    placeholder = { Text("e.g. Medical emergency, Festival cash") },
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
                        onSave(selectedEmpId, cleanAmount, noteText.trim())
                    }
                },
                enabled = isAmountValid && isEmpValid,
                modifier = Modifier.testTag("save_advance_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GlazingBluePrimary)
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Advance", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
