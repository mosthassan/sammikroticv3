package com.example.util

import com.example.data.local.entity.CardSalesInvoiceEntity
import com.example.data.local.entity.InventoryItemEntity
import com.example.data.model.CardSalesInvoiceItem
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal

/**
 * محرك المطابقة الذكي للمخزون وفواتير مبيعات الكروت
 * يوفر مطابقة فائقة الدقة بين مسميات الفئات في فواتير المبيعات (وملفات JSON المستوردة)
 * وبين الأصناف الفعلية في المخزن، مع استخراج الكميات وتوحيد الأرصدة بدقة محاسبية متكاملة.
 */
object InventoryMatchingHelper {

    private val ARABIC_INDIC_DIGITS = mapOf(
        '٠' to '0', '١' to '1', '٢' to '2', '٣' to '3', '٤' to '4',
        '٥' to '5', '٦' to '6', '٧' to '7', '٨' to '8', '٩' to '9'
    )

    private val FILLER_WORDS = setOf(
        "فئة", "باقة", "كرت", "كروت", "حبة", "حبات", "ريال", "يمني", "سعودي",
        "دولار", "نت", "فايبر", "شحن", "رصيد", "yer", "sar", "usd", "profile",
        "package", "card", "cards", "شبكة", "واي", "فاي"
    )

    /**
     * تحويل الأرقام العربية المشرقية (٠-٩) إلى أرقام لاتينية (0-9)
     */
    fun convertArabicDigitsToAscii(input: String): String {
        val sb = java.lang.StringBuilder(input.length)
        for (ch in input) {
            sb.append(ARABIC_INDIC_DIGITS[ch] ?: ch)
        }
        return sb.toString()
    }

    /**
     * تسوية وتطبيع النصوص العربية لإزالة التشكيل وتوحيد الألف والتاء المربوطة
     */
    fun normalizeArabic(text: String): String {
        val ascii = convertArabicDigitsToAscii(text)
        return ascii.trim()
            .replace("""[\u064B-\u0652]""".toRegex(), "") // إزالة حركات التشكيل
            .replace("""[أإآا]""".toRegex(), "ا")
            .replace("""[ةه]""".toRegex(), "ه")
            .replace("""[ئىي]""".toRegex(), "ي")
            .replace("""[\[\]\(\)\{\}<>:;,.\•\*xX×_#/\-]""".toRegex(), " ")
            .replace("""\s+""".toRegex(), " ")
            .trim()
            .lowercase()
    }

    /**
     * استخراج كافة الأرقام من النص
     */
    fun extractAllNumbers(text: String): List<Int> {
        val ascii = convertArabicDigitsToAscii(text)
        val matches = """\d+""".toRegex().findAll(ascii)
        return matches.mapNotNull { it.value.toIntOrNull() }.toList()
    }

    /**
     * استخراج رقم الفئة الأساسي (Denomination) مثل 200، 500، 1000، 2000، 5000 إلخ
     */
    fun extractDenominationNumber(text: String): Int? {
        val numbers = extractAllNumbers(text)
        if (numbers.isEmpty()) return null
        // الأولوية للأرقام الكبيرة الشائعة في فئات الكروت (>= 50)
        val denomination = numbers.firstOrNull { it >= 50 } ?: numbers.firstOrNull()
        return denomination
    }

    /**
     * إزالة الكلمات الزائدة والفرعية للحصول على جوهر الصنف
     */
    fun stripFillerWords(text: String): String {
        val normalized = normalizeArabic(text)
        val tokens = normalized.split(" ").filter { it.isNotBlank() && it !in FILLER_WORDS }
        return tokens.joinToString(" ")
    }

    /**
     * التحقق مما إذا كان الاسمان يشيران لنفس فئة/صنف الكروت
     */
    fun matchesPackage(nameA: String, nameB: String): Boolean {
        val a = nameA.trim()
        val b = nameB.trim()
        if (a.isBlank() || b.isBlank()) return false
        if (a.equals(b, ignoreCase = true)) return true

        val normA = normalizeArabic(a)
        val normB = normalizeArabic(b)
        if (normA == normB) return true

        val strippedA = stripFillerWords(a)
        val strippedB = stripFillerWords(b)
        if (strippedA.isNotBlank() && strippedA == strippedB) return true

        // مطابقة رقم الفئة (مثلاً 200 في "فئة 200 ريال" يطابق "فئة 200" و "باقة 200" و "200")
        val denomA = extractDenominationNumber(a)
        val denomB = extractDenominationNumber(b)
        if (denomA != null && denomB != null && denomA == denomB) {
            return true
        }

        // احتواء جزئي للنصوص المعالجة
        if (normA.length >= 3 && normB.length >= 3) {
            if (normA.contains(normB) || normB.contains(normA)) return true
        }

        return false
    }

    /**
     * البحث الذكي المرن عن الصنف المخزني المطابق من قائمة المخزن
     * مع تفضيل الصنف الذي يحتوي على رصيد متوفر لضمان الخصم السليم
     */
    fun findBestMatchingInventoryItem(
        targetPackageName: String,
        inventoryItems: List<InventoryItemEntity>
    ): InventoryItemEntity? {
        val cleanTarget = targetPackageName.trim()
        if (cleanTarget.isBlank() || inventoryItems.isEmpty()) return null

        // 1. التطابق التام الدقيق
        val exact = inventoryItems.find { it.packageName.trim().equals(cleanTarget, ignoreCase = true) }
        if (exact != null) return exact

        // 2. مطابقة النص المعالج (Normalized)
        val targetNorm = normalizeArabic(cleanTarget)
        val normalizedMatches = inventoryItems.filter { normalizeArabic(it.packageName) == targetNorm }
        if (normalizedMatches.isNotEmpty()) {
            return normalizedMatches.maxByOrNull { it.quantityAvailable } ?: normalizedMatches.first()
        }

        // 3. مطابقة الكلمات الجوهرية بدون حشو
        val targetStripped = stripFillerWords(cleanTarget)
        if (targetStripped.isNotBlank()) {
            val strippedMatches = inventoryItems.filter { stripFillerWords(it.packageName) == targetStripped }
            if (strippedMatches.isNotEmpty()) {
                return strippedMatches.maxByOrNull { it.quantityAvailable } ?: strippedMatches.first()
            }
        }

        // 4. مطابقة رقم الفئة المالي (200، 500، 1000...)
        val targetDenom = extractDenominationNumber(cleanTarget)
        if (targetDenom != null) {
            val denomMatches = inventoryItems.filter { extractDenominationNumber(it.packageName) == targetDenom }
            if (denomMatches.isNotEmpty()) {
                // نفضل الصنف الذي يحتوي على رصيد متوفر، أو الأقدم تاريخاً
                return denomMatches.maxByOrNull { it.quantityAvailable } ?: denomMatches.first()
            }
        }

        // 5. الاحتواء النصي
        val substringMatches = inventoryItems.filter {
            val itemNorm = normalizeArabic(it.packageName)
            itemNorm.contains(targetNorm) || targetNorm.contains(itemNorm)
        }
        if (substringMatches.isNotEmpty()) {
            return substringMatches.maxByOrNull { it.quantityAvailable } ?: substringMatches.first()
        }

        return null
    }

    /**
     * تنظيف اسم الصنف المستخرج وإزالة الأقواس والبادئات الشائبة مع الحفاظ التام على أرقام الفئات
     */
    fun cleanExtractedPackageName(rawName: String): String {
        var cleaned = rawName.trim()
        // إزالة الأقواس المحيطة
        cleaned = cleaned.removePrefix("[").removeSuffix("]")
            .removePrefix("(").removeSuffix(")")
            .removePrefix("{").removeSuffix("}").trim()

        // إذا كان يحتوي على "50 كرت" في البداية نزيله
        cleaned = cleaned.replace("""^\d+\s*(?:كرت|حبة|كروت)?\s*""".toRegex(), "").trim()
        cleaned = cleaned.removePrefix("-").removePrefix("•").removePrefix(":").trim()

        return if (cleaned.isNotBlank()) cleaned else rawName.trim()
    }

    /**
     * استخراج عناصر فاتورة المبيعات باحترافية وتحديد الكميات والأصناف بدقة
     */
    fun extractInvoiceItems(invoice: CardSalesInvoiceEntity): List<CardSalesInvoiceItem> {
        val items = mutableListOf<CardSalesInvoiceItem>()

        // 1. المحاولة أولاً من itemsJson
        if (invoice.itemsJson.isNotBlank() && invoice.itemsJson.trim() != "[]") {
            try {
                var jsonStr = invoice.itemsJson.trim()
                if (jsonStr.startsWith("\"") && jsonStr.endsWith("\"")) {
                    jsonStr = jsonStr.substring(1, jsonStr.length - 1).replace("\\\"", "\"")
                }
                val arr = JSONArray(jsonStr)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val rawName = obj.optString("packageName",
                        obj.optString("package_name",
                            obj.optString("category",
                                obj.optString("name",
                                    obj.optString("profile",
                                        obj.optString("package",
                                            obj.optString("item",
                                                obj.optString("itemName", ""))))))))
                    val pkgName = cleanExtractedPackageName(rawName)
                    val qty = obj.optInt("quantity",
                        obj.optInt("qty",
                            obj.optInt("count",
                                obj.optInt("cardsCount",
                                    obj.optInt("totalCards",
                                        obj.optInt("amount", 0))))))
                    val unitPrice = obj.optDouble("unitPrice", obj.optDouble("price", obj.optDouble("wholesalePrice", 0.0)))
                    val retailPrice = obj.optDouble("retailPrice", 0.0)

                    if (pkgName.isNotBlank() && qty > 0) {
                        items.add(
                            CardSalesInvoiceItem(
                                id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                                packageName = pkgName,
                                quantity = qty,
                                unitPrice = unitPrice,
                                retailPrice = retailPrice
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("InventoryMatching", "Error parsing itemsJson for #${invoice.invoiceNumber}: ${e.message}")
            }
        }

        // 2. استخراج من itemsSummary إذا كان itemsJson فارغاً أو فشل استخراجه
        if (items.isEmpty() && invoice.itemsSummary.isNotBlank()) {
            val parts = invoice.itemsSummary.split("•", ",", "+", "\n", "\r", "،", ";")

            // أنماط regex لاكتشاف مختلف صيغ كتابة الكروت
            // 1) 50 كرت [فئة 200 ريال] أو [فئة 200 ريال] 50
            val patternLeadingQtyBracket = """(\d+)\s*(?:كرت|حبة|كروت)?\s*\[([^\]]+)\]""".toRegex()
            val patternTrailingQtyBracket = """\[([^\]]+)\]\s*(?:[:\-xX×*]?\s*)?(\d+)\s*(?:كرت|حبة|كروت)?""".toRegex()

            // 2) فئة 200 ريال (50 كرت) أو (50) فئة 200
            val patternParenthesis = """([^\(\)]+)\((\d+)\s*(?:كرت|حبة|كروت)?\)""".toRegex()

            // 3) فئة 200 ريال x 50 أو 50 x فئة 200 ريال
            val patternMultiplierA = """(\d+)\s*(?:كرت|حبة|كروت)?\s*[*xX×:]\s*(.+)""".toRegex()
            val patternMultiplierB = """(.+)\s*[*xX×:]\s*(\d+)\s*(?:كرت|حبة|كروت)?""".toRegex()

            // 4) 50 كرت فئة 200 ريال
            val patternText = """(\d+)\s*(?:كرت|حبة|كروت)?\s+(.+)""".toRegex()

            for (part in parts) {
                val trimmed = part.trim()
                if (trimmed.isBlank()) continue

                var parsedQty = 0
                var parsedName = ""

                val m1 = patternLeadingQtyBracket.find(trimmed)
                val m2 = if (m1 == null) patternTrailingQtyBracket.find(trimmed) else null
                val m3 = if (m1 == null && m2 == null) patternParenthesis.find(trimmed) else null
                val m4 = if (m1 == null && m2 == null && m3 == null) patternMultiplierA.find(trimmed) else null
                val m5 = if (m1 == null && m2 == null && m3 == null && m4 == null) patternMultiplierB.find(trimmed) else null
                val m6 = if (m1 == null && m2 == null && m3 == null && m4 == null && m5 == null) patternText.find(trimmed) else null

                when {
                    m1 != null -> {
                        parsedQty = m1.groupValues[1].toIntOrNull() ?: 1
                        parsedName = m1.groupValues[2]
                    }
                    m2 != null -> {
                        parsedName = m2.groupValues[1]
                        parsedQty = m2.groupValues[2].toIntOrNull() ?: 1
                    }
                    m3 != null -> {
                        parsedName = m3.groupValues[1]
                        parsedQty = m3.groupValues[2].toIntOrNull() ?: 1
                    }
                    m4 != null -> {
                        parsedQty = m4.groupValues[1].toIntOrNull() ?: 1
                        parsedName = m4.groupValues[2]
                    }
                    m5 != null -> {
                        parsedName = m5.groupValues[1]
                        parsedQty = m5.groupValues[2].toIntOrNull() ?: 1
                    }
                    m6 != null -> {
                        parsedQty = m6.groupValues[1].toIntOrNull() ?: 1
                        parsedName = m6.groupValues[2]
                    }
                    else -> {
                        // محاولة إذا كان جزء مفرد والعدد مأخوذ من totalCardsCount
                        if (parts.size == 1 && invoice.totalCardsCount > 0) {
                            parsedQty = invoice.totalCardsCount
                            parsedName = trimmed
                        }
                    }
                }

                val clean = cleanExtractedPackageName(parsedName)
                if (clean.isNotBlank() && parsedQty > 0) {
                    val unit = if (invoice.totalCardsCount > 0 && invoice.totalAmount > BigDecimal.ZERO) {
                        invoice.totalAmount.toDouble() / invoice.totalCardsCount
                    } else 0.0
                    items.add(
                        CardSalesInvoiceItem(
                            packageName = clean,
                            quantity = parsedQty,
                            unitPrice = unit,
                            retailPrice = unit
                        )
                    )
                }
            }
        }

        // 3. احتياطي إذا كان totalCardsCount > 0 وما زالت القائمة فارغة
        if (items.isEmpty() && invoice.totalCardsCount > 0) {
            val fallbackName = when {
                invoice.itemsSummary.isNotBlank() -> cleanExtractedPackageName(invoice.itemsSummary)
                invoice.notes.isNotBlank() && (invoice.notes.contains("باقة") || invoice.notes.contains("فئة")) -> cleanExtractedPackageName(invoice.notes)
                else -> "كروت شبكة"
            }.ifBlank { "كروت شبكة" }

            val unit = if (invoice.totalAmount > BigDecimal.ZERO) {
                invoice.totalAmount.toDouble() / invoice.totalCardsCount
            } else 0.0

            items.add(
                CardSalesInvoiceItem(
                    packageName = fallbackName,
                    quantity = invoice.totalCardsCount,
                    unitPrice = unit,
                    retailPrice = unit
                )
            )
        }

        return items
    }
}
