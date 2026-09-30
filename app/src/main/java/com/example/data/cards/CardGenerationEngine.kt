package com.example.data.cards

import com.example.data.local.entity.CardBatchEntity
import com.example.data.local.entity.CardEntity
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Character set options for code generation matching the web repository.
 */
enum class CodeCharacterSet(val titleArabic: String) {
    DIGITS_ONLY("أرقام فقط (1-9) - الأسهل للمستخدمين"),
    ALPHANUMERIC_UPPER("أرقام وحروف إنجليزية كبيرة (ABC789)"),
    ALPHANUMERIC_LOWER("أرقام وحروف إنجليزية صغيرة (abc789)"),
    NO_CONFUSION("أرقام وحروف بدون التباس (استبعاد 0, O, 1, I, l)")
}

enum class PasswordPolicy(val titleArabic: String) {
    SAME_AS_USERNAME("نفس اسم المستخدم (User = Password)"),
    SEPARATE_PIN("رمز PIN سري منفصل (4-6 أرقام)"),
    NO_PASSWORD("بدون كلمة مرور (Hotspot Mac/Code)")
}

data class CardGenerationSpec(
    val batchName: String,
    val categoryName: String,
    val retailPrice: Double,
    val wholesalePrice: Double,
    val quotaMb: Long,
    val validityHours: Int,
    val speedLimit: String,
    val count: Int,
    val prefix: String = "SAM",
    val codeLength: Int = 6,
    val characterSet: CodeCharacterSet = CodeCharacterSet.DIGITS_ONLY,
    val passwordPolicy: PasswordPolicy = PasswordPolicy.SAME_AS_USERNAME,
    val loginDomain: String = "wifi.net",
    val mikrotikProfile: String = "default"
)

data class GeneratedCardModel(
    val username: String,
    val password: String,
    val qrUrl: String,
    val batchNumber: String,
    val serialNumber: String
)

object CardGenerationEngine {

    private val secureRandom = SecureRandom()

    private const val DIGITS = "23456789" // Avoid 0, 1 for digits when possible or standard
    private const val ALL_DIGITS = "0123456789"
    private const val UPPER_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private const val LOWER_CHARS = "abcdefghjkmnpqrstuvwxyz23456789"
    private const val NO_CONFUSION_CHARS = "23456789ABCDEFGHJKLMNPRSTUVWXYZ"

    fun generateVoucherCode(
        length: Int,
        prefix: String,
        charSet: CodeCharacterSet
    ): String {
        val pool = when (charSet) {
            CodeCharacterSet.DIGITS_ONLY -> ALL_DIGITS
            CodeCharacterSet.ALPHANUMERIC_UPPER -> UPPER_CHARS
            CodeCharacterSet.ALPHANUMERIC_LOWER -> LOWER_CHARS
            CodeCharacterSet.NO_CONFUSION -> NO_CONFUSION_CHARS
        }
        val sb = java.lang.StringBuilder()
        if (prefix.isNotBlank()) {
            sb.append(prefix.trim().uppercase())
        }
        for (i in 0 until length) {
            val idx = secureRandom.nextInt(pool.length)
            sb.append(pool[idx])
        }
        return sb.toString()
    }

    fun generatePin(length: Int = 4): String {
        val sb = java.lang.StringBuilder()
        for (i in 0 until length) {
            sb.append(secureRandom.nextInt(10))
        }
        return sb.toString()
    }

    fun buildAutoLoginUrl(domain: String, username: String, password: String): String {
        val cleanDomain = domain.trim().ifBlank { "wifi.net" }
        return "http://$cleanDomain/login?username=$username&password=$password"
    }

    /**
     * Sanitizes RouterOS strings to strictly prevent injection vulnerabilities
     * as implemented in the NetFlow/sammikroticsy repository.
     */
    fun sanitizeRouterOSValue(raw: String, maxLen: Int = 40): String {
        return raw.replace("\"", "")
            .replace("\\", "")
            .replace(";", "")
            .replace("\n", "")
            .replace("\r", "")
            .replace("\$", "")
            .take(maxLen)
    }

    fun sanitizeRouterOSIdentifier(raw: String, fallback: String = "default"): String {
        val cleaned = raw.replace(Regex("[^a-zA-Z0-9_-]"), "")
        return cleaned.ifBlank { fallback }
    }

    /**
     * Generates a fully hardened RouterOS Hotspot User creation script
     * matching the web project's terminal script architecture.
     */
    fun generateRouterOsHotspotScript(
        cards: List<CardEntity>,
        profileName: String = "default",
        serverName: String = "all",
        batchComment: String = "SAM_BATCH"
    ): String {
        val safeProfile = sanitizeRouterOSIdentifier(profileName, "default")
        val safeServer = sanitizeRouterOSIdentifier(serverName, "all")
        val safeComment = sanitizeRouterOSValue(batchComment, 30)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
        val sb = StringBuilder()
        sb.append("# ==========================================================\n")
        sb.append("# SAM MIKROTIK - سكربت إضافة كروت الهوتسبوت إلى راوتر ميكروتك\n")
        sb.append("# تاريخ الإنشاء: ${dateFormat.format(Date())}\n")
        sb.append("# إجمالي الكروت: ${cards.size}\n")
        sb.append("# البروفايل المستهدف: $safeProfile | السيرفر: $safeServer\n")
        sb.append("# محمي ضد الرموز الخاصة وحقن السكربتات (Hardened & Protected)\n")
        sb.append("# ==========================================================\n\n")
        sb.append("/ip hotspot user\n")

        for (card in cards) {
            val user = sanitizeRouterOSValue(card.username)
            val pass = sanitizeRouterOSValue(card.password)
            sb.append("add name=\"$user\" password=\"$pass\" profile=\"$safeProfile\" server=\"$safeServer\" comment=\"${safeComment}_${card.categoryName}\"\n")
        }

        return sb.toString()
    }

    /**
     * Generates User Manager v6 & v7 scripts for User Manager environments.
     */
    fun generateUserManagerV7Script(
        cards: List<CardEntity>,
        userGroup: String = "default",
        batchComment: String = "SAM_BATCH"
    ): String {
        val safeGroup = sanitizeRouterOSIdentifier(userGroup, "default")
        val safeComment = sanitizeRouterOSValue(batchComment, 30)
        val sb = StringBuilder()
        sb.append("# SAM MIKROTIK - سكربت User Manager v7 الحديث (RouterOS v7)\n")
        sb.append("/user-manager user\n")
        for (card in cards) {
            val user = sanitizeRouterOSValue(card.username)
            val pass = sanitizeRouterOSValue(card.password)
            sb.append("add name=\"$user\" password=\"$pass\" group=\"$safeGroup\" comment=\"${safeComment}\"\n")
            sb.append("/user-manager user-profile add user=\"$user\" profile=\"$safeGroup\"\n")
        }
        return sb.toString()
    }

    /**
     * Generates the automated Cleanup script to purge expired hotspot users
     * with an exclusion list for admin/VIP accounts.
     */
    fun generateExpiredUsersCleanupScript(
        excludeComments: String = "admin,keep_admin,vip,bypass"
    ): String {
        val tags = excludeComments.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val sb = StringBuilder()
        sb.append("# SAM MIKROTIK - سكربت الحذف التلقائي للمستخدمين المنتهية صلاحيتهم\n")
        sb.append("# يستثني الحسابات التالية: $excludeComments\n")
        sb.append("/ip hotspot active\n")
        sb.append(":foreach u in=[/ip hotspot user find] do={\n")
        sb.append("  :local uComment [/ip hotspot user get \$u comment];\n")
        sb.append("  :local uUptime [/ip hotspot user get \$u uptime];\n")
        sb.append("  :local uLimitUptime [/ip hotspot user get \$u limit-uptime];\n")
        sb.append("  :local isExcluded false;\n")
        for (tag in tags) {
            sb.append("  :if ([:find \$uComment \"$tag\"] >= 0) do={ :set isExcluded true; }\n")
        }
        sb.append("  :if (!\$isExcluded) do={\n")
        sb.append("    :if ((\$uLimitUptime != [:nothing] && \$uUptime >= \$uLimitUptime)) do={\n")
        sb.append("      /ip hotspot user remove \$u;\n")
        sb.append("    }\n")
        sb.append("  }\n")
        sb.append("}\n")
        return sb.toString()
    }
}
