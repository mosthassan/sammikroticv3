package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "network_devices",
    indices = [Index(value = ["ipAddress"], unique = false)]
)
data class NetworkDeviceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,                    // e.g. "أكسس برج الرويشان الرئيسي"
    val deviceType: String,              // "Access Point", "MikroTik RouterBOARD", "Sector Antenna", "Switch", "CPE"
    val ipAddress: String,               // e.g. "192.168.88.10"
    val macAddress: String = "",         // e.g. "D4:CA:6D:4A:2B:1C"
    val locationArea: String,            // e.g. "برج التحرير - الطابق الخامس"
    val portOrInterface: String = "",    // e.g. "ether2-Hotspot"
    val frequencyOrSsid: String = "",    // e.g. "5180 MHz / SAM-WIFI-5G"
    val model: String = "",              // e.g. "MikroTik RB4011 / Ubiquiti Rocket M5"
    val username: String = "admin",
    val status: String = "ONLINE",       // "ONLINE", "OFFLINE", "WARNING"
    val signalDbm: Int = -62,            // e.g. -62 dBm
    val uptimeHours: Int = 124,
    val notes: String = "",
    val latitude: Double = 0.0,          // GPS Latitude e.g. 15.3694
    val longitude: Double = 0.0,         // GPS Longitude e.g. 44.1910
    val coverageRadiusMeters: Int = 300, // Coverage radius in meters
    val parentDeviceId: Long? = null,    // Link to upstream router/switch/tower
    val createdAt: Long = System.currentTimeMillis()
)
