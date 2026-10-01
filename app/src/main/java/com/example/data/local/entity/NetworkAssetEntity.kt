package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.math.BigDecimal

/**
 * يمثل أصلاً رأسمالياً ثابتاً من أصول الشبكة (CAPEX)
 * تم تحويل كافة الحقول المالية إلى BigDecimal لضمان دقة الإهلاك والقيمة الدفترية
 */
@Entity(tableName = "network_assets")
data class NetworkAssetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val assetName: String,                    // اسم الأصل (مثال: راوتر CCR2004، بطارية ليثيوم 200A)
    val category: String = "SERVERS",         // التصنيف: SERVERS, TOWERS, SOLAR_POWER, FIBER_CABLES, CLIENT_DEVICES, OTHER
    val purchaseCost: BigDecimal = BigDecimal.ZERO,          // تكلفة الشراء الأولية المحولة للريال اليمني
    val estimatedCurrentValue: BigDecimal = BigDecimal.ZERO, // القيمة السوقية الحالية التقديرية بعد الإهلاك
    val currency: String = "USD",             // العملة الأصلية: USD, SAR, YER
    val originalCost: BigDecimal = BigDecimal.ZERO,          // القيمة بالعملة الأصلية (مثلاً 450 دولار)
    val purchaseDateMillis: Long = System.currentTimeMillis(),
    val location: String = "",                // الموقع
    val serialNumber: String = "",            // الرقم التسلسلي أو موديل الجهاز
    val status: String = "ACTIVE",            // الحالة: ACTIVE (يعمل), MAINTENANCE (صيانة), DEPRECATED (مستهلك/تالف)
    val notes: String = ""                    // أي ملاحظات أو ضمان
) {
    @Ignore
    constructor(
        id: Long = 0,
        assetName: String,
        category: String = "SERVERS",
        purchaseCost: Double,
        estimatedCurrentValue: Double,
        currency: String = "USD",
        originalCost: Double = 0.0,
        purchaseDateMillis: Long = System.currentTimeMillis(),
        location: String = "",
        serialNumber: String = "",
        status: String = "ACTIVE",
        notes: String = ""
    ) : this(
        id = id,
        assetName = assetName,
        category = category,
        purchaseCost = BigDecimal.valueOf(purchaseCost),
        estimatedCurrentValue = BigDecimal.valueOf(estimatedCurrentValue),
        currency = currency,
        originalCost = BigDecimal.valueOf(originalCost),
        purchaseDateMillis = purchaseDateMillis,
        location = location,
        serialNumber = serialNumber,
        status = status,
        notes = notes
    )

    val purchaseCostDouble: Double get() = purchaseCost.toDouble()
    val estimatedCurrentValueDouble: Double get() = estimatedCurrentValue.toDouble()
    val originalCostDouble: Double get() = originalCost.toDouble()
}
