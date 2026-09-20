package com.example.ui.dialogs

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.EmployeeEntity
import com.example.ui.theme.GlazingBlueDark
import com.example.ui.theme.GlazingBluePrimary
import com.example.ui.theme.PresentGreen
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordPaymentDialog(
    employees: List<EmployeeEntity>,
    preselectedEmployeeId: Long? = null,
    initialPaymentType: String = "SALARY", // "SALARY" or "ADVANCE"
    currencySymbol: String = "₹",
    currentMonth: String = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()),
    onDismiss: () -> Unit,
    onSave: (
        employeeId: Long,
        paymentType: String,
        amount: Double,
        paymentMethod: String,
        referenceNo: String,
        paymentDate: String,
        monthYear: String,
        note: String
    ) -> Unit
) {
    val context = LocalContext.current
    var paymentType by remember { mutableStateOf(initialPaymentType.uppercase()) }
    var selectedEmpId by remember {
        mutableStateOf(preselectedEmployeeId ?: employees.firstOrNull()?.id ?: 0L)
    }
    var amountText by remember { mutableStateOf(if (initialPaymentType == "ADVANCE") "2000" else "15000") }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var isMethodDropdownExpanded by remember { mutableStateOf(false) }
    var referenceNo by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var monthYearText by remember { mutableStateOf(currentMonth) }

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    var paymentDate by remember { mutableStateOf(todayStr) }
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
            return "Payment amount cannot be empty"
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
            return "Amount exceeds maximum limit ($currencySymbol 10,000,000)"
        }
        return null
    }

    val amountError = remember(amountText) { getAmountValidationError(amountText) }
    val isAmountValid = amountError == null
    val showAmountError = (hasAttemptedSubmit || isAmountTouched) && !isAmountValid
    val isEmpValid = selectedEmpId > 0L && employees.any { it.id == selectedEmpId }

    val selectedEmp = employees.find { it.id == selectedEmpId } ?: employees.firstOrNull()

    // DatePicker setup
    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance()
            cal.set(year, month, dayOfMonth)
            paymentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
            monthYearText = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val paymentMethods = listOf(
        "Cash",
        "Bank Transfer (NEFT/IMPS)",
        "UPI / GPay / PhonePe",
        "Cheque"
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
                        .size(40.dp)
                        .background(GlazingBluePrimary.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (paymentType == "SALARY") Icons.Default.Payments else Icons.Default.PriceChange,
                        contentDescription = null,
                        tint = GlazingBluePrimary
                    )
                }
                Column {
                    Text(
                        text = "Record Payment Details",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Saves to Firestore 'payments' collection",
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
                    .verticalScroll(rememberScrollState()),
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
                            text = "Admin Payment Form • Cloud Firestore Synced",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // 1. Payment Type Selector (Salary vs Advance)
                Text(
                    text = "Payment Classification *",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = paymentType == "SALARY",
                        onClick = {
                            paymentType = "SALARY"
                            if (amountText == "2000") amountText = "15000"
                        },
                        label = { Text("Salary Payment", fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("payment_type_salary_chip")
                    )
                    FilterChip(
                        selected = paymentType == "ADVANCE",
                        onClick = {
                            paymentType = "ADVANCE"
                            if (amountText == "15000") amountText = "2000"
                        },
                        label = { Text("Advance Payment", fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.PriceChange,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("payment_type_advance_chip")
                    )
                }

                // 2. Worker Selection Dropdown
                ExposedDropdownMenuBox(
                    expanded = isEmpDropdownExpanded,
                    onExpandedChange = { isEmpDropdownExpanded = !isEmpDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = "${selectedEmp?.name ?: "Select Worker"} (${selectedEmp?.employeeCode ?: ""})",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Employee / Labor *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isEmpDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("payment_employee_dropdown"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = isEmpDropdownExpanded,
                        onDismissRequest = { isEmpDropdownExpanded = false }
                    ) {
                        employees.forEach { emp ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("${emp.name} (${emp.employeeCode})", fontWeight = FontWeight.Bold)
                                        Text(emp.designation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

                // 3. Payment Amount Field with Numeric Validation
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        isAmountTouched = true
                        amountText = sanitizeNumericInput(input)
                    },
                    label = { Text("Amount ($currencySymbol) *") },
                    leadingIcon = {
                        Text(
                            text = currencySymbol,
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
                                    text = "✓ Valid numeric value: $currencySymbol${String.format(Locale.getDefault(), "%,.2f", parsed)} to Firestore",
                                    color = Color(0xFF2E7D32),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            Text(
                                text = "Only valid positive numbers accepted (e.g. 5000 or 15000.50)",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    isError = showAmountError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_amount_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // 4. Payment Method Dropdown
                ExposedDropdownMenuBox(
                    expanded = isMethodDropdownExpanded,
                    onExpandedChange = { isMethodDropdownExpanded = !isMethodDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = paymentMethod,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Payment Method *") },
                        leadingIcon = {
                            Icon(
                                imageVector = when (paymentMethod) {
                                    "Cash" -> Icons.Default.Money
                                    "Cheque" -> Icons.Default.FactCheck
                                    else -> Icons.Default.AccountBalance
                                },
                                contentDescription = null
                            )
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isMethodDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("payment_method_dropdown"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = isMethodDropdownExpanded,
                        onDismissRequest = { isMethodDropdownExpanded = false }
                    ) {
                        paymentMethods.forEach { method ->
                            DropdownMenuItem(
                                text = { Text(method) },
                                onClick = {
                                    paymentMethod = method
                                    isMethodDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // 5. Payment Date & Period Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = paymentDate,
                        onValueChange = { paymentDate = it },
                        label = { Text("Date (yyyy-MM-dd)") },
                        trailingIcon = {
                            IconButton(onClick = { datePickerDialog.show() }) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Pick Date", modifier = Modifier.size(18.dp))
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("payment_date_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = monthYearText,
                        onValueChange = { monthYearText = it },
                        label = { Text("Month (yyyy-MM)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(0.8f)
                            .testTag("payment_month_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // 6. Reference / Transaction No
                OutlinedTextField(
                    value = referenceNo,
                    onValueChange = { referenceNo = it },
                    label = { Text("Reference / UTR / Cheque No (Optional)") },
                    placeholder = { Text("e.g. UTR928419402") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_reference_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // 7. Notes / Remarks
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Remarks / Description (Optional)") },
                    placeholder = { Text(if (paymentType == "SALARY") "e.g. Cleared monthly wage" else "e.g. Emergency personal cash advance") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_notes_input"),
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
                        onSave(
                            selectedEmpId,
                            paymentType,
                            cleanAmount,
                            paymentMethod,
                            referenceNo.trim(),
                            paymentDate.trim(),
                            monthYearText.trim(),
                            noteText.trim()
                        )
                    }
                },
                enabled = isAmountValid && isEmpValid,
                modifier = Modifier.testTag("submit_payment_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GlazingBluePrimary)
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save to Firestore", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
