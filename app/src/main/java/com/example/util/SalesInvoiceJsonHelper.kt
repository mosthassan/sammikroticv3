package com.example.util

import com.example.data.local.entity.CardSalesInvoiceEntity
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * خدمة مساعدة لتصدير واستيراد فواتير مبيعات الكروت بصيغة JSON
 * لتسهيل عملية نقل البيانات والنسخ الاحتياطي بين الأجهزة.
 */
object SalesInvoiceJsonHelper {

    fun exportInvoicesToJson(invoices: List<CardSalesInvoiceEntity>, networkName: String = "شبكة المايكروتك"): String {
        val root = JSONObject()
        root.put("format", "SAM_SALES_INVOICE_BACKUP")
        root.put("version", 1)
        root.put("networkName", networkName)
        root.put("exportedAt", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).format(Date()))
        root.put("invoiceCount", invoices.size)

        val array = JSONArray()
        invoices.forEach { inv ->
            val obj = JSONObject()
            obj.put("invoiceNumber", inv.invoiceNumber)
            obj.put("customerName", inv.customerName)
            obj.put("customerPhone", inv.customerPhone)
            if (inv.retailerId != null) obj.put("retailerId", inv.retailerId)
            obj.put("invoiceDateMillis", inv.invoiceDateMillis)
            obj.put("paymentType", inv.paymentType)
            obj.put("totalAmount", inv.totalAmount.toDouble())
            obj.put("paidAmount", inv.paidAmount.toDouble())
            obj.put("remainingAmount", inv.remainingAmount.toDouble())
            obj.put("totalCardsCount", inv.totalCardsCount)
            obj.put("itemsCount", inv.itemsCount)
            obj.put("itemsSummary", inv.itemsSummary)
            obj.put("itemsJson", inv.itemsJson)
            obj.put("notes", inv.notes)
            obj.put("issuerName", inv.issuerName)
            obj.put("status", inv.status)
            obj.put("isVoided", inv.isVoided)
            obj.put("voidReason", inv.voidReason)
            obj.put("createdAt", inv.createdAt)
            array.put(obj)
        }
        root.put("salesInvoices", array)

        return root.toString(2)
    }

    fun parseInvoicesFromJson(jsonString: String): Result<List<CardSalesInvoiceEntity>> {
        return runCatching {
            val trimmed = jsonString.trim()
            val list = mutableListOf<CardSalesInvoiceEntity>()

            if (trimmed.startsWith("[")) {
                val array = JSONArray(trimmed)
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    parseInvoiceObject(obj)?.let { list.add(it) }
                }
            } else {
                val root = JSONObject(trimmed)
                if (root.has("salesInvoices")) {
                    val array = root.getJSONArray("salesInvoices")
                    for (i in 0 until array.length()) {
                        val obj = array.optJSONObject(i) ?: continue
                        parseInvoiceObject(obj)?.let { list.add(it) }
                    }
                } else if (root.has("cardSalesInvoices")) {
                    val array = root.getJSONArray("cardSalesInvoices")
                    for (i in 0 until array.length()) {
                        val obj = array.optJSONObject(i) ?: continue
                        parseInvoiceObject(obj)?.let { list.add(it) }
                    }
                } else if (root.has("customerName") || root.has("invoiceNumber")) {
                    parseInvoiceObject(root)?.let { list.add(it) }
                } else {
                    throw IllegalArgumentException("الملف لا يحتوي على مصفوفة فواتير مبيعات (salesInvoices)")
                }
            }

            if (list.isEmpty()) {
                throw IllegalArgumentException("لم يتم العثور على أي فواتير مبيعات صالحة في ملف JSON")
            }

            list
        }
    }

    private fun parseInvoiceObject(obj: JSONObject): CardSalesInvoiceEntity? {
        val cust = obj.optString("customerName", obj.optString("customer", "")).trim()
        val num = obj.optString("invoiceNumber", obj.optString("number", "")).trim()

        if (cust.isBlank() && num.isBlank()) return null

        val total = obj.optDouble("totalAmount", obj.optDouble("amount", 0.0))
        val paid = obj.optDouble("paidAmount", total)
        val rem = obj.optDouble("remainingAmount", (total - paid).coerceAtLeast(0.0))

        var itemsJsonStr = obj.optString("itemsJson", "")
        if (itemsJsonStr.isBlank() || itemsJsonStr == "[]") {
            val possibleItemKeys = listOf("items", "cardItems", "packages", "lines", "details", "invoiceItems")
            for (k in possibleItemKeys) {
                if (obj.has(k) && !obj.isNull(k)) {
                    val arr = obj.optJSONArray(k)
                    if (arr != null && arr.length() > 0) {
                        itemsJsonStr = arr.toString()
                        break
                    }
                }
            }
        }

        var itemsSummaryStr = obj.optString("itemsSummary", "").trim()
        var totalCardsCountVal = obj.optInt("totalCardsCount", obj.optInt("cardsCount", obj.optInt("totalCards", 0)))

        // إذا كان itemsSummary فارغاً ولكن itemsJson متوفر، ننشئ itemsSummary تلقائياً ونحسب إجمالي الكروت
        var calculatedCardsCount = 0
        var itemsCountVal = obj.optInt("itemsCount", 0)
        if (itemsJsonStr.isNotBlank() && itemsJsonStr != "[]") {
            try {
                val arr = JSONArray(itemsJsonStr)
                itemsCountVal = arr.length()
                val summaryParts = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    val itObj = arr.getJSONObject(i)
                    val rawName = itObj.optString("packageName",
                        itObj.optString("package_name",
                            itObj.optString("category",
                                itObj.optString("name",
                                    itObj.optString("profile",
                                        itObj.optString("package", ""))))))
                    val pName = InventoryMatchingHelper.cleanExtractedPackageName(rawName)
                    val q = itObj.optInt("quantity",
                        itObj.optInt("qty",
                            itObj.optInt("count",
                                itObj.optInt("cardsCount",
                                    itObj.optInt("totalCards", 1)))))
                    calculatedCardsCount += q
                    if (pName.isNotBlank()) {
                        summaryParts.add("$q كرت [$pName]")
                    }
                }
                if (itemsSummaryStr.isBlank() && summaryParts.isNotEmpty()) {
                    itemsSummaryStr = summaryParts.joinToString(" • ")
                }
            } catch (_: Exception) {}
        }

        if (totalCardsCountVal <= 0 && calculatedCardsCount > 0) {
            totalCardsCountVal = calculatedCardsCount
        }

        return CardSalesInvoiceEntity(
            id = 0,
            invoiceNumber = if (num.isNotBlank()) num else "INV-2026-${System.currentTimeMillis() % 100000}",
            customerName = if (cust.isNotBlank()) cust else "عميل عام",
            customerPhone = obj.optString("customerPhone", ""),
            retailerId = if (obj.has("retailerId") && !obj.isNull("retailerId")) obj.optLong("retailerId") else null,
            invoiceDateMillis = obj.optLong("invoiceDateMillis", System.currentTimeMillis()),
            paymentType = obj.optString("paymentType", "CASH"),
            totalAmount = BigDecimal.valueOf(total),
            paidAmount = BigDecimal.valueOf(paid),
            remainingAmount = BigDecimal.valueOf(rem),
            totalCardsCount = if (totalCardsCountVal > 0) totalCardsCountVal else 0,
            itemsCount = if (itemsCountVal > 0) itemsCountVal else 1,
            itemsSummary = itemsSummaryStr,
            itemsJson = if (itemsJsonStr.isNotBlank()) itemsJsonStr else "[]",
            notes = obj.optString("notes", ""),
            issuerName = obj.optString("issuerName", "المهندس حسن"),
            status = obj.optString("status", "PAID"),
            isVoided = obj.optBoolean("isVoided", false),
            voidReason = obj.optString("voidReason", ""),
            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
        )
    }

    val SAMPLE_SALES_INVOICES_JSON: String = """
    {
      "format": "SAM_SALES_INVOICE_BACKUP",
      "version": 1,
      "networkName": "شبكة المايكروتك النموذجية",
      "salesInvoices": [
        {
          "invoiceNumber": "INV-2026-0001",
          "customerName": "بقالة البركة والخير",
          "customerPhone": "771234567",
          "paymentType": "CASH",
          "totalAmount": 10000.0,
          "paidAmount": 10000.0,
          "remainingAmount": 0.0,
          "totalCardsCount": 50,
          "itemsSummary": "50 كرت [فئة 200 ريال]",
          "status": "PAID"
        }
      ]
    }
    """.trimIndent()
}
