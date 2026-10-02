package com.example.util

import com.example.data.local.entity.RetailerEntity
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * خدمة مساعدة لتصدير واستيراد بيانات العلاء والوكلاء (البقالات) بصيغة JSON
 * لتسهيل عملية نقل البيانات والنسخ الاحتياطي بين الأجهزة.
 */
object CustomerJsonHelper {

    /**
     * تصدير قائمة العملاء والوكلاء كملف أو نص JSON منسق
     */
    fun exportCustomersToJson(customers: List<RetailerEntity>, networkName: String = "شبكة المايكروتك"): String {
        val root = JSONObject()
        root.put("format", "SAM_CUSTOMER_BACKUP")
        root.put("version", 1)
        root.put("networkName", networkName)
        root.put("exportedAt", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).format(Date()))
        root.put("customerCount", customers.size)

        val customersArray = JSONArray()
        customers.forEach { customer ->
            val obj = JSONObject()
            obj.put("name", customer.name)
            obj.put("ownerName", customer.ownerName)
            obj.put("phone", customer.phone)
            obj.put("location", customer.location)
            obj.put("commissionPercent", customer.commissionPercent)
            obj.put("balanceOwed", customer.balanceOwed.toDouble())
            obj.put("totalPaid", customer.totalPaid.toDouble())
            obj.put("activeCardsCount", customer.activeCardsCount)
            obj.put("notes", customer.notes)
            obj.put("createdAt", customer.createdAt)
            customersArray.put(obj)
        }
        root.put("customers", customersArray)

        return root.toString(2)
    }

    /**
     * استيراد قائمة العملاء/البقالات من كود أو ملف JSON
     */
    fun parseCustomersFromJson(jsonString: String): Result<List<RetailerEntity>> {
        return runCatching {
            val trimmed = jsonString.trim()
            val list = mutableListOf<RetailerEntity>()

            if (trimmed.startsWith("[")) {
                val array = JSONArray(trimmed)
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    parseCustomerObject(obj)?.let { list.add(it) }
                }
            } else {
                val root = JSONObject(trimmed)
                if (root.has("customers")) {
                    val array = root.getJSONArray("customers")
                    for (i in 0 until array.length()) {
                        val obj = array.optJSONObject(i) ?: continue
                        parseCustomerObject(obj)?.let { list.add(it) }
                    }
                } else if (root.has("retailers")) {
                    val array = root.getJSONArray("retailers")
                    for (i in 0 until array.length()) {
                        val obj = array.optJSONObject(i) ?: continue
                        parseCustomerObject(obj)?.let { list.add(it) }
                    }
                } else if (root.has("name") || root.has("ownerName")) {
                    parseCustomerObject(root)?.let { list.add(it) }
                } else {
                    throw IllegalArgumentException("الملف لا يحتوي على مصفوفة عملاء (customers)")
                }
            }

            if (list.isEmpty()) {
                throw IllegalArgumentException("لم يتم العثور على أي عملاء صالحين في ملف JSON")
            }

            list
        }
    }

    private fun parseCustomerObject(obj: JSONObject): RetailerEntity? {
        val name = obj.optString("name", obj.optString("customerName", "")).trim()
        if (name.isBlank()) return null

        return RetailerEntity(
            id = 0,
            name = name,
            ownerName = obj.optString("ownerName", ""),
            phone = obj.optString("phone", obj.optString("mobile", "")),
            location = obj.optString("location", obj.optString("address", "")),
            commissionPercent = obj.optDouble("commissionPercent", 10.0),
            balanceOwed = BigDecimal.valueOf(obj.optDouble("balanceOwed", obj.optDouble("balance", 0.0))),
            totalPaid = BigDecimal.valueOf(obj.optDouble("totalPaid", 0.0)),
            activeCardsCount = obj.optInt("activeCardsCount", 0),
            notes = obj.optString("notes", ""),
            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
        )
    }

    val SAMPLE_CUSTOMERS_JSON: String = """
    {
      "format": "SAM_CUSTOMER_BACKUP",
      "version": 1,
      "networkName": "شبكة المايكروتك النموذجية",
      "customers": [
        {
          "name": "بقالة البركة والخير",
          "ownerName": "أبو فهد الحبيشي",
          "phone": "771234567",
          "location": "حي الجامعة - شارع 14",
          "commissionPercent": 10.0,
          "balanceOwed": 25000.0,
          "totalPaid": 150000.0,
          "notes": "وكيل توزيع رئيسي"
        },
        {
          "name": "سوبرماركت النخبة",
          "ownerName": "المهندس عادل",
          "phone": "777654321",
          "location": "شارع حدة - مقابل البريد",
          "commissionPercent": 12.0,
          "balanceOwed": 0.0,
          "totalPaid": 320000.0,
          "notes": "سداد نقد فور الاستلام"
        }
      ]
    }
    """.trimIndent()
}
