package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "company_profile")
data class CompanyProfileEntity(
    @PrimaryKey
    val id: Int = 1,
    val companyName: String = "ALFA GLAZING",
    val contractorName: String = "Alfa Glazing Contractor",
    val phone: String = "+91 9876543210",
    val location: String = "Site Office - Sector 62, Industrial Zone",
    val currencySymbol: String = "₹",
    val defaultOvertimeRate: Double = 100.0,
    val isDarkMode: Boolean = false,
    val securityPin: String = "805281",
    val currentUserRole: String = "ADMIN", // "ADMIN" or "VIEWER"
    val adminSecurityPin: String = "805281",
    val loggedInMobileNumber: String = "+91 9811100001",
    val loggedInEmail: String = "admin@alfaglazing.com",
    val isGeofenceEnabled: Boolean = true,
    val jobSiteLatitude: Double = 28.6139,
    val jobSiteLongitude: Double = 77.2090,
    val geofenceRadiusMeters: Int = 200,
    val jobSiteAddressName: String = "Alfa Glazing Sector 62 Work Site"
)
