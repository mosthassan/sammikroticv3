package com.example.data.cards

import androidx.compose.ui.graphics.Color
import kotlin.math.floor

/**
 * أنواع المكونات والعناصر القابلة للتخصيص داخل كرت الإنترنت
 */
enum class CardElementType(val displayNameArabic: String) {
    NETWORK_NAME("اسم الشبكة"),
    PRICE_BADGE("شارة السعر"),
    CODE_PIN_BOX("صندوق الكود والرمز"),
    QR_CODE("رمز الـ QR"),
    VALIDITY("مدة الصلاحية والحصة"),
    BATCH_NO("رقم الدفعة / السيريال"),
    EXPIRY_DATE("تاريخ الانتهاء")
}

/**
 * أشكال وقصّات العناصر الهندسية
 */
enum class ElementShape(val displayNameArabic: String) {
    ROUNDED_RECTANGLE("مستطيل منحني"),
    CAPSULE("كبسولة دائرية"),
    PILL("شارة حبّة (Pill)"),
    SHARP_RECTANGLE("مستطيل حاد الزوايا")
}

/**
 * أنماط التوهج والمظهر البصري المتطورة (Visual Styles)
 */
enum class VisualStyleType(val displayNameArabic: String) {
    SOLID("لون مصمت (Solid)"),
    OUTLINE("إطار مفرغ (Outline)"),
    GLASSMORPHISM("زجاجي شفاف (Glassmorphism)"),
    NEON_GLOW("نيون توهّج (Neon Glow)")
}

/**
 * أنماط إطار وخلفية رمز الـ QR Code / Barcode
 */
enum class QrFrameStyle(val displayNameArabic: String) {
    NONE("بدون إطار"),
    ROUNDED_BORDER("إطار منحني مخصص"),
    WHITE_CARD("بطاقة بيضاء خلفية"),
    NEON_BORDER("إطار نيون متوهج")
}

/**
 * إعدادات تخصيص رمز الـ QR Code والباربود
 */
data class QrStyleConfig(
    val frameStyle: QrFrameStyle = QrFrameStyle.WHITE_CARD,
    val darkColorHex: String = "#0F172A",
    val lightColorHex: String = "#FFFFFF",
    val cornerRadiusDp: Int = 8
)

/**
 * تمثيل موضع ونمط كل عنصر داخل الكرت بنسب مئوية نسبية (0-100%)
 */
data class CardElementPosition(
    val elementType: CardElementType,
    val xPercent: Float = 0.0f,          // الإحداثي السيني بنسبة مئوية (0.0% - 100.0%)
    val yPercent: Float = 0.0f,          // الإحداثي الصادي بنسبة مئوية (0.0% - 100.0%)
    val widthPercent: Float = 30.0f,     // العرض بنسبة مئوية من عرض الكرت
    val heightPercent: Float = 20.0f,    // الارتفاع بنسبة مئوية من ارتفاع الكرت
    val fontSizeSp: Float = 12.0f,       // حجم الخط
    val isVisible: Boolean = true,       // حالة إظهار أو إخفاء العنصر
    val textColorHex: String = "#FFFFFF", // لون النص بصيغة الهكس
    val backgroundColorHex: String = "#00000000", // لون الخلفية
    val borderColorHex: String = "#00000000",     // لون الإطار
    val glowColorHex: String = "#00F0FF",         // لون توهج النيون
    val shape: ElementShape = ElementShape.ROUNDED_RECTANGLE,
    val visualStyle: VisualStyleType = VisualStyleType.SOLID,
    val isBold: Boolean = false,         // خط عريض
    val zIndex: Int = 0                  // الترتيب والطبقة
) {
    fun clampedX(minVal: Float = 0.0f, maxVal: Float = 100.0f - widthPercent): Float {
        return xPercent.coerceIn(minVal, maxVal.coerceAtLeast(0.0f))
    }

    fun clampedY(minVal: Float = 0.0f, maxVal: Float = 100.0f - heightPercent): Float {
        return yPercent.coerceIn(minVal, maxVal.coerceAtLeast(0.0f))
    }
}

/**
 * أبعاد مقاسات الكرت بالمليمتر (mm)
 */
data class CardDimensions(
    val widthMm: Float = 63.0f,
    val heightMm: Float = 19.0f,
    val label: String = "63 × 19 مم (شريط رفيع قياسي)"
) {
    val aspectRatio: Float get() = if (heightMm > 0f) widthMm / heightMm else 3.315f

    fun toPointsWidth(): Float = widthMm * MM_TO_POINTS
    fun toPointsHeight(): Float = heightMm * MM_TO_POINTS
    fun toPixelsWidth(dpi: Int = 300): Float = (widthMm / MM_PER_INCH) * dpi
    fun toPixelsHeight(dpi: Int = 300): Float = (heightMm / MM_PER_INCH) * dpi

    companion object {
        const val MM_TO_POINTS = 2.8346457f
        const val MM_PER_INCH = 25.4f

        val STANDARD_63_19 = CardDimensions(63.0f, 19.0f, "63 × 19 مم (شريط رفيع - 24/42 كرت)")
        val LARGE_63_33 = CardDimensions(63.0f, 33.0f, "63 × 33 مم (كبير متكامل)")
        val COMPACT_54_25 = CardDimensions(54.0f, 25.0f, "54 × 25 مم (مدمج)")
        val CREDIT_CARD_85_54 = CardDimensions(85.4f, 54.0f, "85 × 54 مم (حجم بطاقة ائتمانية)")

        val PRESET_DIMENSIONS = listOf(
            STANDARD_63_19,
            LARGE_63_33,
            COMPACT_54_25,
            CREDIT_CARD_85_54
        )

        fun createCustom(widthMm: Float, heightMm: Float): CardDimensions {
            val safeWidth = widthMm.coerceAtLeast(10.0f)
            val safeHeight = heightMm.coerceAtLeast(10.0f)
            return CardDimensions(
                widthMm = safeWidth,
                heightMm = safeHeight,
                label = "${safeWidth.toInt()} × ${safeHeight.toInt()} مم (مخصص)"
            )
        }
    }
}

enum class A4Orientation(val displayNameArabic: String) {
    PORTRAIT("عمودي (Portrait)"),
    LANDSCAPE("أفقي (Landscape)")
}

/**
 * Dynamic A4 Grid Math Engine
 * تحسب تلقائياً عدد الكروت مع دعم التجاوز اليدوي المباشر (Manual Grid Override)
 */
data class A4GridConfig(
    val cardDimensions: CardDimensions = CardDimensions.STANDARD_63_19,
    val marginTopMm: Float = 10.0f,
    val marginBottomMm: Float = 10.0f,
    val marginLeftMm: Float = 10.0f,
    val marginRightMm: Float = 10.0f,
    val horizontalGapMm: Float = 2.0f,
    val verticalGapMm: Float = 2.0f,
    val showCutLines: Boolean = true,
    val orientation: A4Orientation = A4Orientation.PORTRAIT,
    val isManualOverride: Boolean = false,
    val overrideColumns: Int? = null,
    val overrideRows: Int? = null
) {
    val pageWidthMm: Float get() = if (orientation == A4Orientation.PORTRAIT) 210.0f else 297.0f
    val pageHeightMm: Float get() = if (orientation == A4Orientation.PORTRAIT) 297.0f else 210.0f

    val printableWidthMm: Float get() = (pageWidthMm - marginLeftMm - marginRightMm).coerceAtLeast(0.0f)
    val printableHeightMm: Float get() = (pageHeightMm - marginTopMm - marginBottomMm).coerceAtLeast(0.0f)

    /**
     * الحساب الديناميكي لعدد الأعمدة مع دعم التجاوز اليدوي
     */
    val columns: Int
        get() {
            if (isManualOverride && overrideColumns != null && overrideColumns > 0) return overrideColumns
            val step = cardDimensions.widthMm + horizontalGapMm
            if (step <= 0.0f) return 1
            val cols = floor((printableWidthMm + horizontalGapMm) / step).toInt()
            return cols.coerceAtLeast(1)
        }

    /**
     * الحساب الديناميكي لعدد الصفوف مع دعم التجاوز اليدوي
     */
    val rows: Int
        get() {
            if (isManualOverride && overrideRows != null && overrideRows > 0) return overrideRows
            val step = cardDimensions.heightMm + verticalGapMm
            if (step <= 0.0f) return 1
            val r = floor((printableHeightMm + verticalGapMm) / step).toInt()
            return r.coerceAtLeast(1)
        }

    val cardsPerPage: Int get() = columns * rows

    val totalWidthUsedMm: Float get() = (columns * cardDimensions.widthMm) + ((columns - 1) * horizontalGapMm)
    val totalHeightUsedMm: Float get() = (rows * cardDimensions.heightMm) + ((rows - 1) * verticalGapMm)
}

/**
 * نموذج القالب المستقل الجاهز للإنشاء المباشر (Pre-built Template Preset)
 */
data class CardTemplatePreset(
    val id: String,
    val nameArabic: String,
    val category: String, // "قياسي", "سيبراني", "ملكي", "اقتصادي"
    val dimensions: CardDimensions,
    val theme: CardThemeStyle,
    val positions: Map<CardElementType, CardElementPosition>,
    val qrConfig: QrStyleConfig = QrStyleConfig()
)

/**
 * القيم الافتراضية للنماذج والقوالب المجهزة مسبقاً
 */
object CardStudioDefaults {

    fun defaultPositions(): Map<CardElementType, CardElementPosition> {
        return mapOf(
            CardElementType.NETWORK_NAME to CardElementPosition(
                elementType = CardElementType.NETWORK_NAME,
                xPercent = 50.0f,
                yPercent = 8.0f,
                widthPercent = 45.0f,
                heightPercent = 22.0f,
                fontSizeSp = 10.0f,
                isVisible = true,
                textColorHex = "#FFFFFF",
                isBold = true,
                zIndex = 1
            ),
            CardElementType.PRICE_BADGE to CardElementPosition(
                elementType = CardElementType.PRICE_BADGE,
                xPercent = 4.0f,
                yPercent = 8.0f,
                widthPercent = 30.0f,
                heightPercent = 22.0f,
                fontSizeSp = 9.0f,
                isVisible = true,
                textColorHex = "#000000",
                backgroundColorHex = "#F59E0B",
                shape = ElementShape.PILL,
                visualStyle = VisualStyleType.SOLID,
                isBold = true,
                zIndex = 2
            ),
            CardElementType.CODE_PIN_BOX to CardElementPosition(
                elementType = CardElementType.CODE_PIN_BOX,
                xPercent = 4.0f,
                yPercent = 36.0f,
                widthPercent = 70.0f,
                heightPercent = 38.0f,
                fontSizeSp = 14.0f,
                isVisible = true,
                textColorHex = "#00F0FF",
                backgroundColorHex = "#0F172A",
                borderColorHex = "#38BDF8",
                glowColorHex = "#00F0FF",
                shape = ElementShape.CAPSULE,
                visualStyle = VisualStyleType.NEON_GLOW,
                isBold = true,
                zIndex = 3
            ),
            CardElementType.QR_CODE to CardElementPosition(
                elementType = CardElementType.QR_CODE,
                xPercent = 78.0f,
                yPercent = 36.0f,
                widthPercent = 18.0f,
                heightPercent = 38.0f,
                isVisible = false,
                backgroundColorHex = "#FFFFFF",
                zIndex = 4
            ),
            CardElementType.VALIDITY to CardElementPosition(
                elementType = CardElementType.VALIDITY,
                xPercent = 4.0f,
                yPercent = 78.0f,
                widthPercent = 40.0f,
                heightPercent = 18.0f,
                fontSizeSp = 8.0f,
                isVisible = true,
                textColorHex = "#E2E8F0",
                zIndex = 5
            ),
            CardElementType.BATCH_NO to CardElementPosition(
                elementType = CardElementType.BATCH_NO,
                xPercent = 48.0f,
                yPercent = 78.0f,
                widthPercent = 25.0f,
                heightPercent = 18.0f,
                fontSizeSp = 7.5f,
                isVisible = true,
                textColorHex = "#94A3B8",
                zIndex = 6
            ),
            CardElementType.EXPIRY_DATE to CardElementPosition(
                elementType = CardElementType.EXPIRY_DATE,
                xPercent = 75.0f,
                yPercent = 78.0f,
                widthPercent = 22.0f,
                heightPercent = 18.0f,
                fontSizeSp = 7.5f,
                isVisible = true,
                textColorHex = "#94A3B8",
                zIndex = 7
            )
        )
    }

    val PREBUILT_TEMPLATES = listOf(
        CardTemplatePreset(
            id = "classic_1000",
            nameArabic = "كلاسيكي 1000 - قياسي 63x19",
            category = "قياسي",
            dimensions = CardDimensions.STANDARD_63_19,
            theme = CardThemesLibrary.EMERALD_PRO,
            positions = defaultPositions()
        ),
        CardTemplatePreset(
            id = "neon_5000",
            nameArabic = "سيبراني نيون 5000 - كبير 63x33",
            category = "سيبراني",
            dimensions = CardDimensions.LARGE_63_33,
            theme = CardThemesLibrary.CYBER_NEON,
            positions = defaultPositions().mapValues { entry ->
                if (entry.key == CardElementType.QR_CODE) entry.value.copy(isVisible = true) else entry.value
            },
            qrConfig = QrStyleConfig(frameStyle = QrFrameStyle.NEON_BORDER, darkColorHex = "#00F0FF")
        ),
        CardTemplatePreset(
            id = "minimalist_strip",
            nameArabic = "مينيماليست المدمج - 54x25",
            category = "اقتصادي",
            dimensions = CardDimensions.COMPACT_54_25,
            theme = CardThemesLibrary.CLEAN_INK_SAVER,
            positions = defaultPositions()
        ),
        CardTemplatePreset(
            id = "royal_gold_vip",
            nameArabic = "الذهب الملكي VIP - بطاقة 85x54",
            category = "ملكي",
            dimensions = CardDimensions.CREDIT_CARD_85_54,
            theme = CardThemesLibrary.ROYAL_GOLD,
            positions = defaultPositions().mapValues { entry ->
                entry.value.copy(fontSizeSp = entry.value.fontSizeSp * 1.3f)
            },
            qrConfig = QrStyleConfig(frameStyle = QrFrameStyle.ROUNDED_BORDER, darkColorHex = "#CA8A04")
        )
    )
}
