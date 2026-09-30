package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * يمثل أصلاً رأسمالياً ثابتاً من أصول الشبكة (CAPEX)
 * مثل: راوترات وسيرفرات، أبراج حديدية، ألواح وبطاريات طاقة، كابلات ألياف...
 */
@Entity(tableName = "network_assets")
data class NetworkAssetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val assetName: String,                    // اسم الأصل (مثال: راوتر CCR2004، بطارية ليثيوم 200A)
    val category: String = "SERVERS",         // التصنيف: SERVERS, TOWERS, SOLAR_POWER, FIBER_CABLES, CLIENT_DEVICES, OTHER
    val purchaseCost: Double,                 // تكلفة الشراء الأولية المحولة للريال اليمني
    val estimatedCurrentValue: Double,        // القيمة السوقية الحالية التقديرية بعد الإهلاك
    val currency: String = "USD",             // العملة الأصلية: USD, SAR, YER
    val originalCost: Double = 0.0,           // القيمة بالعملة الأصلية (مثلاً 450 دولار)
    val purchaseDateMillis: Long = System.currentTimeMillis(),
    val location: String = "",                // الموقع (مثلاً: البرج الرئيسي، غرفة السيرفر، السبعين)
    val serialNumber: String = "",            // الرقم التسلسلي أو موديل الجهاز
    val status: String = "ACTIVE",            // الحالة: ACTIVE (يعمل), MAINTENANCE (صيانة), DEPRECATED (مستهلك/تالف)
    val notes: String = ""                    // أي ملاحظات أو ضمان
)
