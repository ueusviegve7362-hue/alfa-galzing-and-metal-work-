package com.example

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AlfaGlazingTheme
import com.example.ui.theme.GlazingBluePrimary

class SignInActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AlfaGlazingTheme {
                SignInContent(
                    onSuccessSignIn = { role, email, phone ->
                        val intent = Intent(this, MainActivity::class.java).apply {
                            putExtra("SIGNED_IN_ROLE", role)
                            putExtra("SIGNED_IN_EMAIL", email)
                            putExtra("SIGNED_IN_PHONE", phone)
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInContent(
    onSuccessSignIn: (role: String, email: String, phone: String) -> Unit
) {
    val context = LocalContext.current

    var selectedRole by remember { mutableStateOf("ADMIN") } // "ADMIN" or "VIEWER"
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var adminPinInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("+91 9811100001") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(GlazingBluePrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.LockPerson,
                            contentDescription = null,
                            tint = GlazingBluePrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Alfa Glazing Auth",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = GlazingBluePrimary
                    )

                    Text(
                        text = "Role-Based Access for Admins & Staff",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Role Switcher Segment
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            onClick = { selectedRole = "ADMIN"; errorMessage = null },
                            color = if (selectedRole == "ADMIN") GlazingBluePrimary else Color.Transparent,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Admin Access",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (selectedRole == "ADMIN") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            onClick = { selectedRole = "VIEWER"; errorMessage = null },
                            color = if (selectedRole == "VIEWER") GlazingBluePrimary else Color.Transparent,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Employee / Viewer",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (selectedRole == "VIEWER") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Form Fields
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it; errorMessage = null },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("signin_email_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it; errorMessage = null },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("signin_password_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (selectedRole == "ADMIN") {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = adminPinInput,
                            onValueChange = { adminPinInput = it; errorMessage = null },
                            label = { Text("Admin Security PIN") },
                            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("signin_admin_pin_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it; errorMessage = null },
                            label = { Text("Registered Mobile Number") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("signin_phone_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    errorMessage?.let { err ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Sign In Button
                    Button(
                        onClick = {
                            if (selectedRole == "ADMIN" && adminPinInput.isNotBlank() && adminPinInput != "805281") {
                                errorMessage = "Invalid Admin PIN! Standard PIN is 805281."
                                return@Button
                            }

                            isLoading = true
                            val resolvedEmail = if (emailInput.isNotBlank()) {
                                emailInput.trim()
                            } else if (selectedRole == "ADMIN") {
                                "admin@alfaglazing.com"
                            } else {
                                "worker@alfaglazing.com"
                            }

                            Toast.makeText(context, "Signed in successfully as $selectedRole!", Toast.LENGTH_SHORT).show()
                            isLoading = false
                            onSuccessSignIn(selectedRole, resolvedEmail, phoneInput.trim())
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("signin_submit_btn")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Login, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("SIGN IN TO APP", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick Guest / Demo Access Button
                    OutlinedButton(
                        onClick = {
                            val defaultEmail = if (selectedRole == "ADMIN") "admin@alfaglazing.com" else "worker@alfaglazing.com"
                            Toast.makeText(context, "Instant access as $selectedRole", Toast.LENGTH_SHORT).show()
                            onSuccessSignIn(selectedRole, defaultEmail, phoneInput.trim())
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("quick_access_btn")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = GlazingBluePrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Continue as $selectedRole", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
