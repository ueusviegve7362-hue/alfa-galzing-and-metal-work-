package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GlazingBluePrimary
import com.example.ui.viewmodel.AlfaGlazingViewModel

@Composable
fun RoleSwitchDialog(
    currentRole: String,
    viewModel: AlfaGlazingViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isAdmin = currentRole.equals("ADMIN", ignoreCase = true)
    var targetRole by remember { mutableStateOf(if (isAdmin) "VIEWER" else "ADMIN") }
    var pinInput by remember { mutableStateOf("") }
    var mobileInput by remember { mutableStateOf(uiState.companyProfile.loggedInMobileNumber.ifBlank { "+91 9811100001" }) }
    var emailInput by remember { mutableStateOf(uiState.companyProfile.loggedInEmail.ifBlank { "admin@alfaglazing.com" }) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = null,
                    tint = GlazingBluePrimary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Sign-In Setup & Identification",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Select access role and enter account mobile number & email:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Admin Option Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (targetRole == "ADMIN") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            targetRole = "ADMIN"
                            errorMessage = null
                        }
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = targetRole == "ADMIN",
                            onClick = {
                                targetRole = "ADMIN"
                                errorMessage = null
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Admin / Editor Role",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Full permissions to add workers, mark attendance, record advances, & view all records",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Viewer Option Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (targetRole == "VIEWER") MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            targetRole = "VIEWER"
                            errorMessage = null
                        }
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = targetRole == "VIEWER",
                            onClick = {
                                targetRole = "VIEWER"
                                errorMessage = null
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Viewer / Employee Personal Role",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Strict Personal Access: View only your personal attendance, salary & advance records",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Sign-In Mobile Number:",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = mobileInput,
                    onValueChange = {
                        mobileInput = it
                        errorMessage = null
                    },
                    label = { Text("Mobile Number (e.g. +91 9811100001)") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("viewer_mobile_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Sign-In Email Address:",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = emailInput,
                    onValueChange = {
                        emailInput = it
                        errorMessage = null
                    },
                    label = { Text("Email Address (e.g. worker@alfaglazing.com)") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("viewer_email_input")
                )

                if (targetRole == "VIEWER") {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Quick Select Registered Employee Credentials:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        listOf(
                            Triple("Ramesh Kumar", "+91 9811100001", "ramesh.kumar@alfaglazing.com"),
                            Triple("Suresh Sharma", "+91 9811100002", "suresh.sharma@alfaglazing.com"),
                            Triple("Abdul Karim", "+91 9811100003", "abdul.karim@alfaglazing.com")
                        ).forEach { (name, phone, email) ->
                            OutlinedCard(
                                onClick = {
                                    mobileInput = phone
                                    emailInput = email
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = GlazingBluePrimary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text("$name ($phone)", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        Text(email, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                if (targetRole == "ADMIN" && !isAdmin) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Admin Authorization PIN Required:",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            pinInput = it
                            errorMessage = null
                        },
                        label = { Text("Security PIN (e.g. 805281 or 1234)") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = errorMessage != null,
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_pin_input")
                    )

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val success = viewModel.switchUserRole(targetRole, pinInput, mobileInput, emailInput)
                    if (success) {
                        onDismiss()
                    } else {
                        errorMessage = "Incorrect PIN! Please try again."
                    }
                },
                modifier = Modifier.testTag("confirm_role_switch_btn")
            ) {
                Text("Confirm Sign-In")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun RoleStatusBanner(
    isUserAdmin: Boolean,
    loggedInMobile: String = "",
    loggedInEmail: String = "",
    matchedEmployeeName: String = "",
    onSwitchRoleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isUserAdmin) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
    val contentColor = if (isUserAdmin) Color(0xFF2E7D32) else Color(0xFFE65100)
    val icon = if (isUserAdmin) Icons.Default.VerifiedUser else Icons.Default.Person

    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (isUserAdmin) "Role: Admin / Editor (Full Access)" else if (matchedEmployeeName.isNotBlank()) "Viewer: $matchedEmployeeName" else "Role: Viewer (Personal View)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = contentColor
                    )
                    Text(
                        text = if (isUserAdmin) "Mobile: ${loggedInMobile.ifBlank { "+91 9811100001" }} • Email: ${loggedInEmail.ifBlank { "admin@alfaglazing.com" }}"
                        else "Mobile: $loggedInMobile • Email: $loggedInEmail",
                        fontSize = 11.sp,
                        color = contentColor.copy(alpha = 0.85f)
                    )
                }
            }

            OutlinedButton(
                onClick = onSwitchRoleClick,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("switch_role_banner_btn")
            ) {
                Text(
                    text = if (isUserAdmin) "Sign-In Setup" else "Change Login",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
