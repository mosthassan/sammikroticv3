package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String,
    val fullName: String,
    val role: String,                    // "OWNER", "ENGINEER", "DISTRIBUTOR", "RETAILER"
    val email: String = "",              // Used for Google Sign-In and Cloud Sync ID
    val photoUrl: String = "",
    val phone: String = "",
    val retailerId: Long? = null,        // If role == "RETAILER", link to the grocery
    val assignedArea: String = "",       // e.g. "حي النهضة والبقالات المجاورة"
    val commissionRate: Double = 0.0,    // e.g. 5.0%
    val notes: String = "",
    val isActive: Boolean = true,
    val isGoogleUser: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
