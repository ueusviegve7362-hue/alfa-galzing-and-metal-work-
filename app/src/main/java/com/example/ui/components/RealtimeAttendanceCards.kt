package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.EmployeeEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

import com.example.data.local.entity.CompanyProfileEntity
import kotlin.math.*

@Composable
fun EmployeeOneTapPunchCard(
    employee: EmployeeEntity,
    todayAttendance: AttendanceEntity?,
    todayDateStr: String,
    companyProfile: CompanyProfileEntity? = null,
    onPunchIn: (status: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isPunchedIn = todayAttendance?.status == "PRESENT"
    val isHalfDay = todayAttendance?.status == "HALF_DAY"

    val isGeofenceEnabled = companyProfile?.isGeofenceEnabled ?: true
    val targetLat = companyProfile?.jobSiteLatitude ?: 28.6139
    val targetLng = companyProfile?.jobSiteLongitude ?: 77.2090
    val allowedRadiusMeters = companyProfile?.geofenceRadiusMeters ?: 200
    val siteName = companyProfile?.jobSiteAddressName ?: "Sector 62 Work Site"

    // User's current location state (default to being within worksite)
    var isSimulatedAtSite by remember { mutableStateOf(true) }
    var userLat by remember { mutableStateOf(if (isSimulatedAtSite) targetLat + 0.0003 else targetLat + 0.025) }
    var userLng by remember { mutableStateOf(if (isSimulatedAtSite) targetLng + 0.0002 else targetLng + 0.020) }

    // Calculate distance in meters using Haversine formula
    val distanceMeters = remember(userLat, userLng, targetLat, targetLng) {
        val earthRadius = 6371000.0 // meters
        val dLat = Math.toRadians(targetLat - userLat)
        val dLng = Math.toRadians(targetLng - userLng)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(userLat)) * cos(Math.toRadians(targetLat)) * sin(dLng / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        (earthRadius * c).roundToInt()
    }

    val isWithinGeofence = !isGeofenceEnabled || (distanceMeters <= allowedRadiusMeters)

    val statusColor = when (todayAttendance?.status) {
        "PRESENT" -> PresentGreen
        "HALF_DAY" -> HalfDayOrange
        "ABSENT" -> AbsentRed
        else -> Color.Gray
    }

    val timestampStr = remember(todayAttendance?.timestamp) {
        todayAttendance?.timestamp?.let { ts ->
            if (ts > 0) {
                SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(ts))
            } else null
        } ?: "Today"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(3.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header with Employee Info & Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(GlazingBluePrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = GlazingBluePrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Daily Attendance Punch",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Date: $todayDateStr",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = todayAttendance?.status ?: "PENDING",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Geo-Fencing Location Badge
            Surface(
                color = if (isWithinGeofence) PresentGreen.copy(alpha = 0.12f) else AbsentRed.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isWithinGeofence) Icons.Default.GpsFixed else Icons.Default.GpsOff,
                            contentDescription = null,
                            tint = if (isWithinGeofence) PresentGreen else AbsentRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isWithinGeofence) "Within Job Site Geofence (${distanceMeters}m away)"
                                else "Outside Geofence (${distanceMeters}m from site)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isWithinGeofence) PresentGreen else AbsentRed
                            )
                            Text(
                                text = "Target: $siteName (Max radius: ${allowedRadiusMeters}m)",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // GPS Simulator switch for testing
                    TextButton(
                        onClick = {
                            isSimulatedAtSite = !isSimulatedAtSite
                            userLat = if (isSimulatedAtSite) targetLat + 0.0003 else targetLat + 0.025
                            userLng = if (isSimulatedAtSite) targetLng + 0.0002 else targetLng + 0.020
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("toggle_geofence_simulation_btn")
                    ) {
                        Text(
                            text = if (isSimulatedAtSite) "Simulate Away" else "Simulate At Site",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main One-Tap Punch Button
            if (!isPunchedIn) {
                Button(
                    onClick = { if (isWithinGeofence) onPunchIn("PRESENT") },
                    enabled = isWithinGeofence,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PresentGreen,
                        disabledContainerColor = Color.LightGray
                    ),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("one_tap_punch_in_btn")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isWithinGeofence) Icons.Default.TouchApp else Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isWithinGeofence) "ONE-TAP PUNCH IN (PRESENT)" else "PUNCH LOCKED (OUTSIDE GEOFENCE)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { if (isWithinGeofence) onPunchIn("HALF_DAY") },
                        enabled = isWithinGeofence,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Half Day", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { onPunchIn("ABSENT") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AbsentRed)
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Leave / Absent", fontSize = 12.sp)
                    }
                }
            } else {
                // Success Punch-In Status Confirmation Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = PresentGreen.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = PresentGreen,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Punched In Successfully!",
                                    fontWeight = FontWeight.Bold,
                                    color = PresentGreen,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Punched at $timestampStr today",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        TextButton(
                            onClick = { onPunchIn("PRESENT") }
                        ) {
                            Text("Re-Punch", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RealtimeAttendanceFeedCard(
    allAttendance: List<AttendanceEntity>,
    employees: List<EmployeeEntity>,
    todayDateStr: String,
    modifier: Modifier = Modifier
) {
    val todayPunches = remember(allAttendance, todayDateStr) {
        allAttendance.filter { it.dateString == todayDateStr && (it.status == "PRESENT" || it.status == "HALF_DAY") }
            .sortedByDescending { it.timestamp }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PresentGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Sensors,
                            contentDescription = null,
                            tint = PresentGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Live Staff Punch Feed",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Real-time updates from worker mobile apps",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = PresentGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "LIVE AUTO-SYNC",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PresentGreen,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (todayPunches.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No staff punch-ins recorded for $todayDateStr yet.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    todayPunches.take(5).forEach { att ->
                        val emp = employees.find { it.id == att.employeeId }
                        val empName = emp?.name ?: "Employee #${att.employeeId}"
                        val timeStr = if (att.timestamp > 0) {
                            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(att.timestamp))
                        } else "Today"

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = PresentGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = empName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "(${emp?.designation ?: "Staff"})",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = timeStr,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = if (att.status == "PRESENT") PresentGreen.copy(alpha = 0.2f) else HalfDayOrange.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = att.status,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (att.status == "PRESENT") PresentGreen else HalfDayOrange,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
