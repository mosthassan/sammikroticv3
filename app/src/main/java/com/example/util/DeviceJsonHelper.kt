package com.example.util

import com.example.data.local.entity.NetworkAssetEntity
import com.example.data.local.entity.NetworkDeviceEntity
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * خدمة مساعدة لتصدير واستيراد بيانات وأسماء وتفاصيل أجهزة وأصول الشبكة بصيغة JSON
 * لتسهيل عملية نقل البيانات والنسخ الاحتياطي بين الأجهزة.
 */
object DeviceJsonHelper {

    /**
     * تصدير قائمة الأجهزة كملف أو نص JSON منسق
     */
    fun exportDevicesToJson(devices: List<NetworkDeviceEntity>, networkName: String = "شبكة المايكروتك"): String {
        val root = JSONObject()
        root.put("format", "MIKROTIK_DEVICE_BACKUP")
        root.put("version", 1)
        root.put("networkName", networkName)
        root.put("exportedAt", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).format(Date()))
        root.put("deviceCount", devices.size)

        val devicesArray = JSONArray()
        devices.forEach { dev ->
            val obj = JSONObject()
            obj.put("name", dev.name)
            obj.put("deviceType", dev.deviceType)
            obj.put("ipAddress", dev.ipAddress)
            obj.put("macAddress", dev.macAddress)
            obj.put("locationArea", dev.locationArea)
            obj.put("portOrInterface", dev.portOrInterface)
            obj.put("frequencyOrSsid", dev.frequencyOrSsid)
            obj.put("model", dev.model)
            obj.put("username", dev.username)
            obj.put("status", dev.status)
            obj.put("signalDbm", dev.signalDbm)
            obj.put("uptimeHours", dev.uptimeHours)
            obj.put("notes", dev.notes)
            obj.put("latitude", dev.latitude)
            obj.put("longitude", dev.longitude)
            obj.put("coverageRadiusMeters", dev.coverageRadiusMeters)
            if (dev.parentDeviceId != null) {
                obj.put("parentDeviceId", dev.parentDeviceId)
            }
            devicesArray.put(obj)
        }
        root.put("devices", devicesArray)

        return root.toString(2)
    }

    /**
     * استيراد قائمة الأجهزة من كود أو ملف JSON
     * يدعم كلاً من:
     * 1. كائن يحتوي على مصفوفة devices: { "devices": [...] }
     * 2. مصفوفة مباشرة من الأجهزة: [ {...}, {...} ]
     * 3. كائن جهاز واحد
     */
    fun parseDevicesFromJson(jsonString: String): Result<List<NetworkDeviceEntity>> {
        return runCatching {
            val trimmed = jsonString.trim()
            val list = mutableListOf<NetworkDeviceEntity>()

            if (trimmed.startsWith("[")) {
                // مصفوفة مباشرة
                val array = JSONArray(trimmed)
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    parseDeviceObject(obj)?.let { list.add(it) }
                }
            } else {
                // كائن يحتوي على devices أو كائن جهاز واحد
                val root = JSONObject(trimmed)
                if (root.has("devices")) {
                    val array = root.getJSONArray("devices")
                    for (i in 0 until array.length()) {
                        val obj = array.optJSONObject(i) ?: continue
                        parseDeviceObject(obj)?.let { list.add(it) }
                    }
                } else if (root.has("name") || root.has("ipAddress")) {
                    parseDeviceObject(root)?.let { list.add(it) }
                } else {
                    throw IllegalArgumentException("الملف لا يحتوي على مصفوفة أجهزة (devices)")
                }
            }

            if (list.isEmpty()) {
                throw IllegalArgumentException("لم يتم العثور على أي أجهزة صالحة في ملف JSON")
            }

            list
        }
    }

    private fun parseDeviceObject(obj: JSONObject): NetworkDeviceEntity? {
        val name = obj.optString("name", obj.optString("deviceName", "")).trim()
        val ip = obj.optString("ipAddress", obj.optString("ip", "")).trim()

        if (name.isBlank() && ip.isBlank()) return null

        return NetworkDeviceEntity(
            id = 0, // Generated automatically by Room
            name = if (name.isNotBlank()) name else "جهاز بدون اسم ($ip)",
            deviceType = obj.optString("deviceType", obj.optString("type", "Access Point")),
            ipAddress = if (ip.isNotBlank()) ip else "192.168.88.1",
            macAddress = obj.optString("macAddress", obj.optString("mac", "")),
            locationArea = obj.optString("locationArea", obj.optString("location", "الموقع الرئيسي")),
            portOrInterface = obj.optString("portOrInterface", obj.optString("interface", "")),
            frequencyOrSsid = obj.optString("frequencyOrSsid", obj.optString("ssid", obj.optString("frequency", ""))),
            model = obj.optString("model", ""),
            username = obj.optString("username", "admin"),
            status = obj.optString("status", "ONLINE"),
            signalDbm = obj.optInt("signalDbm", obj.optInt("signal", -62)),
            uptimeHours = obj.optInt("uptimeHours", 124),
            notes = obj.optString("notes", ""),
            latitude = obj.optDouble("latitude", obj.optDouble("lat", 0.0)),
            longitude = obj.optDouble("longitude", obj.optDouble("lng", obj.optDouble("lon", 0.0))),
            coverageRadiusMeters = obj.optInt("coverageRadiusMeters", obj.optInt("radius", 300)),
            parentDeviceId = if (obj.has("parentDeviceId") && !obj.isNull("parentDeviceId")) obj.optLong("parentDeviceId") else null,
            createdAt = System.currentTimeMillis()
        )
    }

    /**
     * نموذج تجريبي جاهز للأجهزة
     */
    val SAMPLE_DEVICES_JSON: String = """
    {
      "format": "MIKROTIK_DEVICE_BACKUP",
      "version": 1,
      "networkName": "شبكة المايكروتك النموذجية",
      "devices": [
        {
          "name": "أكسس برج الرويشان الرئيسي",
          "deviceType": "Access Point",
          "ipAddress": "192.168.88.10",
          "macAddress": "D4:CA:6D:4A:2B:1C",
          "locationArea": "برج الرويشان - الطابق الخامس",
          "portOrInterface": "ether2-Hotspot",
          "frequencyOrSsid": "5180 MHz / SAM-WIFI-5G",
          "model": "Ubiquiti Rocket M5",
          "status": "ONLINE",
          "signalDbm": -62,
          "notes": "مرسل رئيسي للمنطقة الشرقية",
          "latitude": 15.3694,
          "longitude": 44.1910,
          "coverageRadiusMeters": 400
        },
        {
          "name": "راوتر سويتش التحرير CCR2004",
          "deviceType": "MikroTik RouterBOARD",
          "ipAddress": "192.168.88.1",
          "macAddress": "6C:3B:6B:11:22:33",
          "locationArea": "غرفة السيرفرات - التحرير",
          "portOrInterface": "sfp-plus1",
          "model": "MikroTik CCR2004-16G-2S+",
          "status": "ONLINE",
          "signalDbm": -55,
          "notes": "الراوتر الرئيسي للتحكم بالسرعات والباقات",
          "latitude": 15.3550,
          "longitude": 44.2050,
          "coverageRadiusMeters": 0
        },
        {
          "name": "سكتر بث شمالي BaseStation",
          "deviceType": "Sector Antenna",
          "ipAddress": "192.168.88.25",
          "macAddress": "DC:9F:DB:88:99:AA",
          "locationArea": "برج السبعين",
          "portOrInterface": "ether1",
          "frequencyOrSsid": "5745 MHz / SECTOR-NORTH",
          "model": "Ubiquiti AirMax Sector 120",
          "status": "ONLINE",
          "signalDbm": -68,
          "notes": "تغطية حي السبعين الشمالي",
          "latitude": 15.3400,
          "longitude": 44.2100,
          "coverageRadiusMeters": 600
        }
      ]
    }
    """.trimIndent()
}
