package com.example.util

import com.example.data.local.entity.PurchaseInvoiceEntity
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * خدمة مساعدة لتصدير واستيراد فواتير المشتريات والأصول بصيغة JSON
 * لتسهيل عملية نقل البيانات والنسخ الاحتياطي بين الأجهزة.
 */
object PurchaseInvoiceJsonHelper {

    fun exportInvoicesToJson(invoices: List<PurchaseInvoiceEntity>, networkName: String = "شبكة المايكروتك"): String {
        val root = JSONObject()
        root.put("format", "SAM_PURCHASE_INVOICE_BACKUP")
        root.put("version", 1)
        root.put("networkName", networkName)
        root.put("exportedAt", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).format(Date()))
        root.put("invoiceCount", invoices.size)

        val array = JSONArray()
        invoices.forEach { inv ->
            val obj = JSONObject()
            obj.put("invoiceNumber", inv.invoiceNumber)
            obj.put("supplierName", inv.supplierName)
            obj.put("invoiceDateMillis", inv.invoiceDateMillis)
            obj.put("targetType", inv.targetType)
            obj.put("totalAmount", inv.totalAmount.toDouble())
            obj.put("currency", inv.currency)
            obj.put("originalAmount", inv.originalAmount.toDouble())
            obj.put("paidAmount", inv.paidAmount.toDouble())
            obj.put("paymentMethod", inv.paymentMethod)
            obj.put("status", inv.status)
            obj.put("isVoided", inv.isVoided)
            obj.put("voidReason", inv.voidReason)
            obj.put("notes", inv.notes)
            obj.put("itemsSummary", inv.itemsSummary)
            obj.put("itemsJson", inv.itemsJson)
            obj.put("createdAt", inv.createdAt)
            array.put(obj)
        }
        root.put("purchaseInvoices", array)

        return root.toString(2)
    }

    fun parseInvoicesFromJson(jsonString: String): Result<List<PurchaseInvoiceEntity>> {
        return runCatching {
            val trimmed = jsonString.trim()
            val list = mutableListOf<PurchaseInvoiceEntity>()

            if (trimmed.startsWith("[")) {
                val array = JSONArray(trimmed)
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    parseInvoiceObject(obj)?.let { list.add(it) }
                }
            } else {
                val root = JSONObject(trimmed)
                if (root.has("purchaseInvoices")) {
                    val array = root.getJSONArray("purchaseInvoices")
                    for (i in 0 until array.length()) {
                        val obj = array.optJSONObject(i) ?: continue
                        parseInvoiceObject(obj)?.let { list.add(it) }
                    }
                } else if (root.has("invoices")) {
                    val array = root.getJSONArray("invoices")
                    for (i in 0 until array.length()) {
                        val obj = array.optJSONObject(i) ?: continue
                        parseInvoiceObject(obj)?.let { list.add(it) }
                    }
                } else if (root.has("supplierName") || root.has("invoiceNumber")) {
                    parseInvoiceObject(root)?.let { list.add(it) }
                } else {
                    throw IllegalArgumentException("الملف لا يحتوي على مصفوفة فواتير مشتريات (purchaseInvoices)")
                }
            }

            if (list.isEmpty()) {
                throw IllegalArgumentException("لم يتم العثور على أي فواتير مشتريات صالحة في ملف JSON")
            }

            list
        }
    }

    private fun parseInvoiceObject(obj: JSONObject): PurchaseInvoiceEntity? {
        val supplier = obj.optString("supplierName", obj.optString("supplier", "")).trim()
        val num = obj.optString("invoiceNumber", obj.optString("number", "")).trim()

        if (supplier.isBlank() && num.isBlank()) return null

        val total = obj.optDouble("totalAmount", obj.optDouble("amount", 0.0))
        val orig = obj.optDouble("originalAmount", total)
        val paid = obj.optDouble("paidAmount", total)

        return PurchaseInvoiceEntity(
            id = 0,
            invoiceNumber = if (num.isNotBlank()) num else "PUR-${System.currentTimeMillis() % 100000}",
            supplierName = if (supplier.isNotBlank()) supplier else "مورد عام",
            invoiceDateMillis = obj.optLong("invoiceDateMillis", System.currentTimeMillis()),
            targetType = obj.optString("targetType", "ASSETS"),
            totalAmount = BigDecimal.valueOf(total),
            currency = obj.optString("currency", "USD"),
            originalAmount = BigDecimal.valueOf(orig),
            paidAmount = BigDecimal.valueOf(paid),
            paymentMethod = obj.optString("paymentMethod", "نقداً"),
            status = obj.optString("status", "APPROVED"),
            isVoided = obj.optBoolean("isVoided", false),
            voidReason = obj.optString("voidReason", ""),
            notes = obj.optString("notes", ""),
            itemsSummary = obj.optString("itemsSummary", ""),
            itemsJson = obj.optString("itemsJson", "[]"),
            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
        )
    }

    val SAMPLE_PURCHASE_INVOICES_JSON: String = """
    {
      "format": "SAM_PURCHASE_INVOICE_BACKUP",
      "version": 1,
      "networkName": "شبكة المايكروتك النموذجية",
      "purchaseInvoices": [
        {
          "invoiceNumber": "PUR-2026-0101",
          "supplierName": "شركة التقنية للشبكات",
          "targetType": "ASSETS",
          "totalAmount": 1200.0,
          "currency": "USD",
          "originalAmount": 1200.0,
          "paidAmount": 1200.0,
          "paymentMethod": "تحويل بنكي",
          "itemsSummary": "راوتر ميكروتك CCR2004 (عدد 1)، سكتر Ubiquiti (عدد 2)",
          "notes": "توريد أصول B2B مع الضمان"
        }
      ]
    }
    """.trimIndent()
}
