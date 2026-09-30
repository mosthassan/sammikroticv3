package com.example.util

import com.example.data.model.InvoiceItem
import com.example.data.model.ParsedInvoiceData
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * محول ومحلل فواتير المشتريات والأصول من ملفات ونصوص JSON
 * يتيح استيراد الفواتير بدقة فورية كبديل لوكيل الذكاء الاصطناعي لتوفير رصيد التوكن اليومي والعمل بدون اتصال
 */
object JsonInvoiceParser {

    const val SAMPLE_JSON = """{
  "supplierName": "شركة التقنية ومعدات المايكروتك",
  "invoiceNumber": "INV-2026-902",
  "invoiceDate": "2026-09-22",
  "invoiceType": "ASSETS",
  "currency": "YER",
  "notes": "فاتورة توريد سيرفرات وأجهزة أبراج شبكة",
  "items": [
    {
      "name": "راوتر بورد MikroTik CCR2004-16G-2S+",
      "quantity": 1.0,
      "unitPrice": 450000.0,
      "subtotal": 450000.0,
      "category": "SERVERS"
    },
    {
      "name": "أنتينا سيكتور Ubiquiti PrismStation 5AC",
      "quantity": 2.0,
      "unitPrice": 185000.0,
      "subtotal": 370000.0,
      "category": "TOWERS"
    },
    {
      "name": "بطارية جل 200A للطاقة الشمسية",
      "quantity": 4.0,
      "unitPrice": 165000.0,
      "subtotal": 660000.0,
      "category": "SOLAR_POWER"
    },
    {
      "name": "لفة كابل شبكة Cat6 خارجي محمي (305م)",
      "quantity": 2.0,
      "unitPrice": 75000.0,
      "subtotal": 150000.0,
      "category": "CABLES"
    }
  ]
}"""

    fun parse(jsonStr: String): Result<ParsedInvoiceData> {
        return try {
            val trimmed = jsonStr.trim()
            if (trimmed.isEmpty()) {
                return Result.failure(IllegalArgumentException("محتوى ملف JSON فارغ"))
            }

            // Case 1: Root is a JSON Array: [ { name: "...", quantity: 1, unitPrice: 100 }, ... ]
            if (trimmed.startsWith("[")) {
                val array = JSONArray(trimmed)
                val items = mutableListOf<InvoiceItem>()
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    parseItem(obj)?.let { items.add(it) }
                }
                if (items.isEmpty()) {
                    return Result.failure(IllegalArgumentException("لم يتم العثور على أصناف صالحة داخل مصفوفة JSON"))
                }
                val total = items.sumOf { it.subtotal }
                return Result.success(
                    ParsedInvoiceData(
                        supplierName = "مورد مشتريات مستوردة",
                        invoiceNumber = "JSON-${System.currentTimeMillis() % 100000}",
                        invoiceDate = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date()),
                        invoiceType = "ASSETS",
                        totalAmount = total,
                        currency = "YER",
                        notes = "تم الاستيراد من مصفوفة بنود JSON",
                        items = items,
                        rawAiAnalysis = "Imported from JSON array"
                    )
                )
            }

            // Case 2: Root is a JSON Object
            val root = JSONObject(trimmed)
            val supplier = root.optString("supplierName", "").ifBlank {
                root.optString("supplier", "").ifBlank {
                    root.optString("vendor", "").ifBlank {
                        root.optString("vendorName", "").ifBlank {
                            root.optString("المورد", "").ifBlank {
                                "مورد مشتريات"
                            }
                        }
                    }
                }
            }

            val invoiceNum = root.optString("invoiceNumber", "").ifBlank {
                root.optString("invoice_number", "").ifBlank {
                    root.optString("number", "").ifBlank {
                        root.optString("رقم_الفاتورة", "").ifBlank {
                            "JSON-${System.currentTimeMillis() % 100000}"
                        }
                    }
                }
            }

            val invoiceDate = root.optString("invoiceDate", "").ifBlank {
                root.optString("invoice_date", "").ifBlank {
                    root.optString("date", "").ifBlank {
                        root.optString("التاريخ", "").ifBlank {
                            SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())
                        }
                    }
                }
            }

            val rawType = root.optString("invoiceType", "").ifBlank {
                root.optString("type", "").ifBlank {
                    root.optString("النوع", "ASSETS")
                }
            }
            val invoiceType = if (rawType.contains("مصروف") || rawType.contains("EXPENSE", ignoreCase = true)) "EXPENSES" else "ASSETS"

            val currency = root.optString("currency", "").ifBlank {
                root.optString("العملة", "YER")
            }

            val notes = root.optString("notes", "").ifBlank {
                root.optString("ملاحظات", "تم الاستيراد بنجاح من ملف JSON كبديل للذكاء الاصطناعي")
            }

            val items = mutableListOf<InvoiceItem>()
            val itemsArray = root.optJSONArray("items")
                ?: root.optJSONArray("products")
                ?: root.optJSONArray("lines")
                ?: root.optJSONArray("الأصناف")
                ?: root.optJSONArray("البنود")

            if (itemsArray != null) {
                for (i in 0 until itemsArray.length()) {
                    val obj = itemsArray.optJSONObject(i) ?: continue
                    parseItem(obj)?.let { items.add(it) }
                }
            }

            if (items.isEmpty()) {
                return Result.failure(IllegalArgumentException("لم يتم العثور على حقل أصناف (items) أو قائمة أصناف صالحة في JSON"))
            }

            val calculatedTotal = items.sumOf { it.subtotal }
            val explicitTotal = root.optDouble("totalAmount", root.optDouble("total", calculatedTotal))
            val finalTotal = if (calculatedTotal > 0) calculatedTotal else explicitTotal

            Result.success(
                ParsedInvoiceData(
                    supplierName = supplier,
                    invoiceNumber = invoiceNum,
                    invoiceDate = invoiceDate,
                    invoiceType = invoiceType,
                    totalAmount = finalTotal,
                    currency = currency,
                    notes = notes,
                    items = items,
                    rawAiAnalysis = "Imported from JSON format"
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseItem(obj: JSONObject): InvoiceItem? {
        val name = obj.optString("name", "").ifBlank {
            obj.optString("item", "").ifBlank {
                obj.optString("description", "").ifBlank {
                    obj.optString("product", "").ifBlank {
                        obj.optString("الصنف", "").ifBlank {
                            obj.optString("الاسم", "")
                        }
                    }
                }
            }
        }
        if (name.isBlank()) return null

        val qty = obj.optDouble("quantity", obj.optDouble("qty", obj.optDouble("الكمية", 1.0)))
        val price = obj.optDouble("unitPrice", obj.optDouble("unit_price", obj.optDouble("price", obj.optDouble("السعر", 0.0))))
        val subtotal = obj.optDouble("subtotal", obj.optDouble("total", qty * price))
        val cat = obj.optString("category", obj.optString("التصنيف", "GENERAL")).uppercase()

        val normalizedCat = when {
            cat.contains("SERVER") || cat.contains("سيرفر") || cat.contains("ROUTER") || cat.contains("راوتر") -> "SERVERS"
            cat.contains("TOWER") || cat.contains("برج") || cat.contains("ANTENNA") || cat.contains("أنتينا") -> "TOWERS"
            cat.contains("SOLAR") || cat.contains("شمس") || cat.contains("BATTERY") || cat.contains("بطار") -> "SOLAR_POWER"
            cat.contains("CABLE") || cat.contains("كابل") || cat.contains("سلك") -> "CABLES"
            cat.contains("FUEL") || cat.contains("وقود") || cat.contains("ديزل") -> "FUEL"
            cat.contains("MAINT") || cat.contains("صيان") -> "MAINTENANCE"
            else -> "GENERAL"
        }

        return InvoiceItem(
            id = UUID.randomUUID().toString(),
            name = name,
            quantity = if (qty <= 0) 1.0 else qty,
            unitPrice = price,
            subtotal = if (subtotal > 0) subtotal else (qty * price),
            category = normalizedCat
        )
    }
}
