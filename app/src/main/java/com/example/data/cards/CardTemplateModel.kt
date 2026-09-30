package com.example.data.cards

import androidx.compose.ui.graphics.Color

/**
 * Visual styling theme for cards inspired by the web project presets.
 */
data class CardThemeStyle(
    val id: String,
    val nameArabic: String,
    val category: String, // "داكن", "فاتح", "حيوي", "فاخر"
    val backgroundStart: Color,
    val backgroundEnd: Color,
    val textColor: Color,
    val accentColor: Color,
    val badgeBgColor: Color,
    val badgeTextColor: Color,
    val codeBoxBgColor: Color,
    val codeBoxBorderColor: Color,
    val codeBoxTextColor: Color,
    val borderColor: Color,
    val isLightInkSaver: Boolean = false
)

data class CardTemplateConfig(
    val templateId: String = "tpl_cyber_neon",
    val theme: CardThemeStyle = CardThemesLibrary.CYBER_NEON,
    val networkTitle: String = "سام ميكروتك للإنترنت",
    val footerNote: String = "امسح رمز الاستجابة السريعة أو أدخل الكود للدخول الفوري",
    val supportPhone: String = "777000000",
    val loginDomain: String = "wifi.net",
    val showQrCode: Boolean = true,
    val showPasswordOrPin: Boolean = true,
    val showPriceBadge: Boolean = true,
    val showSpeedAndQuota: Boolean = true,
    val showScratchGuide: Boolean = false,
    val showCutLines: Boolean = true,
    val cornerRadiusDp: Int = 12
)

object CardThemesLibrary {

    val CYBER_NEON = CardThemeStyle(
        id = "cyber_neon",
        nameArabic = "نيون سيبراني متوهج",
        category = "حيوي",
        backgroundStart = Color(0xFF080318),
        backgroundEnd = Color(0xFF16082F),
        textColor = Color(0xFFFDF4FF),
        accentColor = Color(0xFF00F0FF),
        badgeBgColor = Color(0xFFFF007F),
        badgeTextColor = Color.White,
        codeBoxBgColor = Color(0xFF12062B),
        codeBoxBorderColor = Color(0xFF00F0FF),
        codeBoxTextColor = Color(0xFF00F0FF),
        borderColor = Color(0xFF00F0FF)
    )

    val FIBER_BLUE = CardThemeStyle(
        id = "fiber_blue",
        nameArabic = "أزرق فايبر الملكي",
        category = "داكن",
        backgroundStart = Color(0xFF071322),
        backgroundEnd = Color(0xFF0F2744),
        textColor = Color.White,
        accentColor = Color(0xFF38BDF8),
        badgeBgColor = Color(0xFFF59E0B),
        badgeTextColor = Color.Black,
        codeBoxBgColor = Color(0xFF0F172A),
        codeBoxBorderColor = Color(0xFF38BDF8),
        codeBoxTextColor = Color.White,
        borderColor = Color(0xFF38BDF8)
    )

    val CLEAN_INK_SAVER = CardThemeStyle(
        id = "clean_white",
        nameArabic = "أبيض اقتصادي (موفر حبر)",
        category = "فاتح",
        backgroundStart = Color(0xFFFFFFFF),
        backgroundEnd = Color(0xFFF8FAFC),
        textColor = Color(0xFF0F172A),
        accentColor = Color(0xFF0284C7),
        badgeBgColor = Color(0xFF0F172A),
        badgeTextColor = Color.White,
        codeBoxBgColor = Color(0xFFF1F5F9),
        codeBoxBorderColor = Color(0xFF0F172A),
        codeBoxTextColor = Color(0xFF0F172A),
        borderColor = Color(0xFFCBD5E1),
        isLightInkSaver = true
    )

    val ROYAL_GOLD = CardThemeStyle(
        id = "royal_gold",
        nameArabic = "الذهب الملكي الفاخر VIP",
        category = "فاخر",
        backgroundStart = Color(0xFF0C0A09),
        backgroundEnd = Color(0xFF1C1917),
        textColor = Color(0xFFFEF08A),
        accentColor = Color(0xFFEAB308),
        badgeBgColor = Color(0xFFCA8A04),
        badgeTextColor = Color.Black,
        codeBoxBgColor = Color(0xFF1C1917),
        codeBoxBorderColor = Color(0xFFEAB308),
        codeBoxTextColor = Color(0xFFFEF08A),
        borderColor = Color(0xFFEAB308)
    )

    val TURBO_BLAZE = CardThemeStyle(
        id = "turbo_blaze",
        nameArabic = "تيربو ناري رياضي",
        category = "حيوي",
        backgroundStart = Color(0xFF141418),
        backgroundEnd = Color(0xFF291515),
        textColor = Color.White,
        accentColor = Color(0xFFEA580C),
        badgeBgColor = Color(0xFFF59E0B),
        badgeTextColor = Color.Black,
        codeBoxBgColor = Color(0xFF1C1414),
        codeBoxBorderColor = Color(0xFFEA580C),
        codeBoxTextColor = Color.White,
        borderColor = Color(0xFFEA580C)
    )

    val EMERALD_PRO = CardThemeStyle(
        id = "emerald_green",
        nameArabic = "الزمرد التجاري الرسمي",
        category = "داكن",
        backgroundStart = Color(0xFF022C22),
        backgroundEnd = Color(0xFF064E3B),
        textColor = Color.White,
        accentColor = Color(0xFF34D399),
        badgeBgColor = Color(0xFFFBBF24),
        badgeTextColor = Color(0xFF78350F),
        codeBoxBgColor = Color(0xFF064E3B),
        codeBoxBorderColor = Color(0xFF34D399),
        codeBoxTextColor = Color.White,
        borderColor = Color(0xFF34D399)
    )

    val ALL_THEMES = listOf(
        CYBER_NEON,
        FIBER_BLUE,
        CLEAN_INK_SAVER,
        ROYAL_GOLD,
        TURBO_BLAZE,
        EMERALD_PRO
    )
}
