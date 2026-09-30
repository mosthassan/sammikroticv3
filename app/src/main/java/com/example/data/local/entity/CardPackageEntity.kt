package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * يمثل باقة أو بروفايل كروت إنترنت محددة (Package / Profile)
 * تشمل سعر الجملة للبقالات والموزعين، وسعر البيع النهائي للمستهلكين،
 * وربطها التلقائي بتوليد الكروت وفواتير وسندات بيع الكروت.
 */
@Entity(tableName = "card_packages")
data class CardPackageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,                  // اسم الباقة (مثلاً: "باقة 500 ريال فايبر - 2.5GB")
    val retailPrice: BigDecimal,       // سعر البيع النهائي للمستهلك (مثلاً: 500.0)
    val wholesalePrice: BigDecimal,    // سعر الجملة للبقالة والموزع (مثلاً: 450.0)
    val quotaMb: Long = 2500,          // حجم البيانات بالميجابايت
    val validityHours: Int = 24,       // مدة الصلاحية بالساعات (24 ساعة، 168 ساعة = أسبوع، 720 ساعة = شهر)
    val speedLimit: String = "4M/2M",  // سرعة التنزيل والرفع
    val mikrotikProfile: String = "profile-500", // اسم البروفايل في راوتر ميكروتك
    val colorTheme: String = "cyan",   // لون وثيم البطاقة: "cyan", "emerald", "gold", "purple", "amber", "rose"
    val notes: String = "",            // تفاصيل إضافية أو وصف الباقة
    val createdAt: Long = System.currentTimeMillis()
) {
    /** هامش ربح البقالة/الموزع بالريال لكل كرت */
    val profitPerCard: BigDecimal
        get() = (retailPrice.subtract(wholesalePrice)).max(BigDecimal.ZERO)

    /** نسبة هامش الربح المئوية للبقالة */
    val profitPercentage: Double
        get() = if (wholesalePrice > BigDecimal.ZERO) {
            retailPrice.subtract(wholesalePrice)
                .divide(wholesalePrice, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal("100"))
                .toDouble()
        } else 0.0

    /** نص السعة المنسق */
    val formattedQuota: String
        get() = if (quotaMb >= 1024) {
            val gb = quotaMb.toDouble() / 1024.0
            if (gb == gb.toLong().toDouble()) "${gb.toLong()} GB" else String.format(java.util.Locale.US, "%.1f GB", gb)
        } else {
            "$quotaMb MB"
        }

    /** نص الصلاحية المنسق */
    val formattedValidity: String
        get() = when {
            validityHours <= 0 -> "غير محدد"
            validityHours < 24 -> "$validityHours ساعة"
            validityHours % 720 == 0 -> "${validityHours / 720} شهر"
            validityHours % 24 == 0 -> "${validityHours / 24} يوم"
            else -> "$validityHours ساعة"
        }
}
