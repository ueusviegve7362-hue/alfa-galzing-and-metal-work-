package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import com.example.R
import com.example.data.local.entity.CompanyProfileEntity
import com.example.ui.dialogs.RoleStatusBanner
import com.example.ui.dialogs.RoleSwitchDialog
import com.example.ui.theme.GlazingBluePrimary
import com.example.ui.viewmodel.AlfaGlazingUiState
import com.example.ui.viewmodel.AlfaGlazingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: AlfaGlazingUiState,
    viewModel: AlfaGlazingViewModel
) {
    val profile = uiState.companyProfile

    var companyNameText by remember { mutableStateOf(profile.companyName) }
    var contractorNameText by remember { mutableStateOf(profile.contractorName) }
    var phoneText by remember { mutableStateOf(profile.phone) }
    var locationText by remember { mutableStateOf(profile.location) }
    var currencyText by remember { mutableStateOf(profile.currencySymbol) }
    var defaultOtRateText by remember { mutableStateOf(profile.defaultOvertimeRate.toInt().toString()) }

    var adminPinText by remember { mutableStateOf(profile.adminSecurityPin.ifBlank { "805281" }) }
    var loginMobileText by remember { mutableStateOf(profile.loggedInMobileNumber.ifBlank { "+91 9811100001" }) }
    var loginEmailText by remember { mutableStateOf(profile.loggedInEmail.ifBlank { "admin@alfaglazing.com" }) }

    var isGeofenceEnabledState by remember { mutableStateOf(profile.isGeofenceEnabled) }
    var jobSiteAddressText by remember { mutableStateOf(profile.jobSiteAddressName) }
    var jobSiteLatText by remember { mutableStateOf(profile.jobSiteLatitude.toString()) }
    var jobSiteLngText by remember { mutableStateOf(profile.jobSiteLongitude.toString()) }
    var geofenceRadiusText by remember { mutableStateOf(profile.geofenceRadiusMeters.toString()) }

    var showRoleDialog by remember { mutableStateOf(false) }

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
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        RoleStatusBanner(
            isUserAdmin = uiState.isUserAdmin,
            loggedInMobile = profile.loggedInMobileNumber,
            loggedInEmail = profile.loggedInEmail,
            onSwitchRoleClick = { showRoleDialog = true }
        )

        // Sign-In Setup & Account Credentials Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccountBox,
                        contentDescription = null,
                        tint = GlazingBluePrimary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Sign-In Setup Credentials",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Active account mobile number and email identification",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = loginMobileText,
                    onValueChange = { loginMobileText = it },
                    label = { Text("Account Mobile Number") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth().testTag("settings_login_mobile_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = loginEmailText,
                    onValueChange = { loginEmailText = it },
                    label = { Text("Account Email Address") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth().testTag("settings_login_email_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { showRoleDialog = true },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Switch Access Role", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val updatedProfile = profile.copy(
                                loggedInMobileNumber = loginMobileText,
                                loggedInEmail = loginEmailText
                            )
                            viewModel.updateCompanyProfile(updatedProfile)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("save_sign_in_credentials_btn")
                    ) {
                        Text("Save Account Info", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Job Site Geofence Settings Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = null,
                            tint = GlazingBluePrimary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Job Site Geofencing",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Restrict employee Punch-In to worksite coordinates",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = isGeofenceEnabledState,
                        onCheckedChange = { isGeofenceEnabledState = it },
                        modifier = Modifier.testTag("geofence_enable_switch")
                    )
                }

                if (isGeofenceEnabledState) {
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = jobSiteAddressText,
                        onValueChange = { jobSiteAddressText = it },
                        label = { Text("Job Site Address / Name") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("geofence_address_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = jobSiteLatText,
                            onValueChange = { jobSiteLatText = it },
                            label = { Text("Latitude (°N)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("geofence_lat_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = jobSiteLngText,
                            onValueChange = { jobSiteLngText = it },
                            label = { Text("Longitude (°E)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("geofence_lng_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = geofenceRadiusText,
                        onValueChange = { geofenceRadiusText = it },
                        label = { Text("Allowed Radius (meters)") },
                        leadingIcon = { Icon(Icons.Default.Radar, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("geofence_radius_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val latVal = jobSiteLatText.toDoubleOrNull() ?: profile.jobSiteLatitude
                        val lngVal = jobSiteLngText.toDoubleOrNull() ?: profile.jobSiteLongitude
                        val radiusVal = geofenceRadiusText.toIntOrNull() ?: profile.geofenceRadiusMeters

                        val updated = profile.copy(
                            isGeofenceEnabled = isGeofenceEnabledState,
                            jobSiteAddressName = jobSiteAddressText,
                            jobSiteLatitude = latVal,
                            jobSiteLongitude = lngVal,
                            geofenceRadiusMeters = radiusVal
                        )
                        viewModel.updateCompanyProfile(updated)
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.align(Alignment.End).testTag("save_geofence_settings_btn")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save Geofence Settings", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Security & Role Permissions Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = GlazingBluePrimary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Admin Security & Access Control",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Manage Admin Security PIN for authorization",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = adminPinText,
                    onValueChange = { adminPinText = it },
                    label = { Text("Admin Security PIN (Current default: 805281)") },
                    enabled = uiState.isUserAdmin,
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        viewModel.updateAdminSecurityPin(adminPinText)
                    },
                    enabled = uiState.isUserAdmin,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Update PIN", fontWeight = FontWeight.Bold)
                }
            }
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(GlazingBluePrimary.copy(alpha = 0.1f)),
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
                    Column {
                        Text(
                            text = "Company & Site Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Official Logo & Profile",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = companyNameText,
                    onValueChange = { companyNameText = it },
                    label = { Text("Company Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("company_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = contractorNameText,
                    onValueChange = { contractorNameText = it },
                    label = { Text("Contractor / Manager Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = phoneText,
                    onValueChange = { phoneText = it },
                    label = { Text("Contact Phone") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = locationText,
                    onValueChange = { locationText = it },
                    label = { Text("Site Location / Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = null,
                        tint = GlazingBluePrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Preferences & Currency",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = currencyText,
                        onValueChange = { currencyText = it },
                        label = { Text("Currency Symbol") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("currency_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = defaultOtRateText,
                        onValueChange = { defaultOtRateText = it },
                        label = { Text("Default OT Rate (₹/hr)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        Button(
            onClick = {
                val otRate = defaultOtRateText.toDoubleOrNull() ?: 100.0
                val newProfile = profile.copy(
                    companyName = companyNameText,
                    contractorName = contractorNameText,
                    phone = phoneText,
                    location = locationText,
                    currencySymbol = currencyText,
                    defaultOvertimeRate = otRate
                )
                viewModel.updateCompanyProfile(newProfile)
            },
            enabled = uiState.isUserAdmin,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("save_settings_button")
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (uiState.isUserAdmin) "Save Company Settings" else "Settings Locked (Viewer Mode)", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // App Info Footer
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ALFA GLAZING v1.0",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Contractor Employee Attendance & Salary Sheet App",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
