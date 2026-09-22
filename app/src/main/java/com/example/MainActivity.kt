package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.entity.EmployeeEntity
import com.example.ui.dialogs.AddEmployeeDialog
import com.example.ui.dialogs.RecordAdvanceDialog
import com.example.ui.dialogs.RecordPaymentDialog
import com.example.ui.screens.*
import com.example.ui.theme.AlfaGlazingTheme
import com.example.ui.theme.GlazingBluePrimary
import com.example.ui.viewmodel.AlfaGlazingViewModel

enum class AlfaNavTab(val title: String, val iconFilled: androidx.compose.ui.graphics.vector.ImageVector, val iconOutlined: androidx.compose.ui.graphics.vector.ImageVector) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    ATTENDANCE("Attendance", Icons.Filled.FactCheck, Icons.Outlined.FactCheck),
    SALARY_SHEET("Salary Sheet", Icons.Filled.Payments, Icons.Outlined.Payments),
    WORKERS("Workers", Icons.Filled.People, Icons.Outlined.People),
    ADVANCES("Advances", Icons.Filled.PriceChange, Icons.Outlined.PriceChange),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: AlfaGlazingViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val context = LocalContext.current

            // Toast Message Observer
            val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
            LaunchedEffect(toastMessage) {
                toastMessage?.let { msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    viewModel.clearToastMessage()
                }
            }

            // Handle Sign-In Extras from SignInActivity
            LaunchedEffect(intent) {
                val roleExtra = intent.getStringExtra("SIGNED_IN_ROLE")
                val emailExtra = intent.getStringExtra("SIGNED_IN_EMAIL")
                val phoneExtra = intent.getStringExtra("SIGNED_IN_PHONE")
                if (!roleExtra.isNullOrBlank()) {
                    viewModel.switchUserRole(
                        targetRole = roleExtra,
                        pinInput = "805281",
                        mobileNumber = phoneExtra ?: "",
                        emailAddress = emailExtra ?: ""
                    )
                }
            }

            AlfaGlazingTheme(darkTheme = uiState.companyProfile.isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAlfaGlazingApp(
                        viewModel = viewModel,
                        uiState = uiState
                    )
                }
            }
        }
    }
}

@Composable
fun MainAlfaGlazingApp(
    viewModel: AlfaGlazingViewModel,
    uiState: com.example.ui.viewmodel.AlfaGlazingUiState
) {
    var selectedTab by remember { mutableStateOf(AlfaNavTab.HOME) }

    // Dialog state
    var showAddEmployeeDialog by remember { mutableStateOf(false) }
    var employeeToEdit by remember { mutableStateOf<EmployeeEntity?>(null) }

    var showRecordAdvanceDialog by remember { mutableStateOf(false) }
    var advancePreselectedEmpId by remember { mutableStateOf<Long?>(null) }

    var showRecordPaymentDialog by remember { mutableStateOf(false) }
    var paymentDialogInitialType by remember { mutableStateOf("SALARY") }
    var paymentPreselectedEmpId by remember { mutableStateOf<Long?>(null) }

    val openPaymentForm: (Long?, String) -> Unit = { empId, defaultType ->
        paymentPreselectedEmpId = empId
        paymentDialogInitialType = defaultType
        showRecordPaymentDialog = true
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_navigation_bar")
            ) {
                AlfaNavTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.iconFilled else tab.iconOutlined,
                                contentDescription = tab.title
                            )
                        },
                        label = { Text(tab.title, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GlazingBluePrimary,
                            selectedTextColor = GlazingBluePrimary,
                            indicatorColor = GlazingBluePrimary.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                AlfaNavTab.HOME -> {
                    HomeScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        onNavigateToAttendance = { selectedTab = AlfaNavTab.ATTENDANCE },
                        onNavigateToSalarySheet = { selectedTab = AlfaNavTab.SALARY_SHEET },
                        onNavigateToEmployees = { selectedTab = AlfaNavTab.WORKERS },
                        onNavigateToAdvances = { selectedTab = AlfaNavTab.ADVANCES },
                        onOpenAddEmployee = {
                            employeeToEdit = null
                            showAddEmployeeDialog = true
                        },
                        onOpenRecordAdvance = {
                            openPaymentForm(null, "ADVANCE")
                        },
                        onOpenRecordPayment = { empId, pType ->
                            openPaymentForm(empId, pType)
                        }
                    )
                }

                AlfaNavTab.ATTENDANCE -> {
                    AttendanceScreen(
                        uiState = uiState,
                        viewModel = viewModel
                    )
                }

                AlfaNavTab.SALARY_SHEET -> {
                    SalarySheetScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        onOpenRecordAdvance = { empId ->
                            openPaymentForm(empId, "ADVANCE")
                        },
                        onOpenRecordPayment = { empId, pType ->
                            openPaymentForm(empId, pType)
                        }
                    )
                }

                AlfaNavTab.WORKERS -> {
                    EmployeesScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        onOpenAddEmployee = {
                            employeeToEdit = null
                            showAddEmployeeDialog = true
                        },
                        onEditEmployee = { emp ->
                            employeeToEdit = emp
                            showAddEmployeeDialog = true
                        },
                        onRecordAdvance = { empId ->
                            openPaymentForm(empId, "ADVANCE")
                        }
                    )
                }

                AlfaNavTab.ADVANCES -> {
                    AdvancesScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        onOpenRecordAdvance = {
                            openPaymentForm(null, "ADVANCE")
                        },
                        onOpenRecordPayment = { empId, pType ->
                            openPaymentForm(empId, pType)
                        }
                    )
                }

                AlfaNavTab.SETTINGS -> {
                    SettingsScreen(
                        uiState = uiState,
                        viewModel = viewModel
                    )
                }
            }

            // Dialogs
            if (showAddEmployeeDialog) {
                AddEmployeeDialog(
                    employeeToEdit = employeeToEdit,
                    onDismiss = {
                        showAddEmployeeDialog = false
                        employeeToEdit = null
                    },
                    onSave = { code, name, phone, designation, wage, otRate ->
                        viewModel.addOrUpdateEmployee(
                            id = employeeToEdit?.id ?: 0,
                            code = code,
                            name = name,
                            phone = phone,
                            designation = designation,
                            dailyWage = wage,
                            overtimeRate = otRate
                        )
                        showAddEmployeeDialog = false
                        employeeToEdit = null
                    }
                )
            }

            if (showRecordAdvanceDialog && uiState.employees.isNotEmpty()) {
                RecordAdvanceDialog(
                    employees = uiState.employees,
                    allAdvances = uiState.allAdvances,
                    preselectedEmployeeId = advancePreselectedEmpId,
                    currencySymbol = uiState.companyProfile.currencySymbol,
                    onDismiss = {
                        showRecordAdvanceDialog = false
                        advancePreselectedEmpId = null
                    },
                    onSave = { empId, amount, note, dateTimestamp ->
                        viewModel.recordAdvancePayment(empId, amount, note, dateTimestamp)
                        showRecordAdvanceDialog = false
                        advancePreselectedEmpId = null
                    }
                )
            }

            if (showRecordPaymentDialog && uiState.employees.isNotEmpty()) {
                RecordPaymentDialog(
                    employees = uiState.employees,
                    preselectedEmployeeId = paymentPreselectedEmpId,
                    initialPaymentType = paymentDialogInitialType,
                    currencySymbol = uiState.companyProfile.currencySymbol,
                    currentMonth = uiState.selectedMonth,
                    onDismiss = {
                        showRecordPaymentDialog = false
                        paymentPreselectedEmpId = null
                    },
                    onSave = { empId, pType, amt, method, ref, pDate, mYear, note ->
                        viewModel.recordPaymentDetails(
                            employeeId = empId,
                            paymentType = pType,
                            amount = amt,
                            paymentMethod = method,
                            referenceNo = ref,
                            paymentDate = pDate,
                            monthYear = mYear,
                            note = note
                        )
                        showRecordPaymentDialog = false
                        paymentPreselectedEmpId = null
                    }
                )
            }
        }
    }
}
