package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.data.local.entity.CardPackageEntity
import com.example.data.local.entity.FinancialVoucherEntity
import com.example.data.local.entity.RetailerEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WhatsAppHelper {

    const val NETWORK_BRAND_NAME = "شبكة طلقة نت"
    const val NETWORK_SUPPORT_PHONE = "771234567"

    /**
     * تنظيف وتنسيق رقم الهاتف ليتوافق مع المعيار الدولي للواتساب
     * يدعم أرقام اليمن (77, 73, 71, 70, 78) وكافة الصيغ الدولية
     */
    fun normalizePhoneNumber(rawPhone: String): String {
        var clean = rawPhone.replace("[^0-9+]".toRegex(), "")
        if (clean.startsWith("+")) {
            clean = clean.substring(1)
        } else if (clean.startsWith("00")) {
            clean = clean.substring(2)
        }

        // إذا كان الرقم محلي يمني مكون من 9 أرقام يبدأ بـ 7
        if (clean.length == 9 && clean.startsWith("7")) {
            return "967$clean"
        }
        // إذا كان يبدأ بـ 07 (10 أرقام)
        if (clean.length == 10 && clean.startsWith("07")) {
            return "967${clean.substring(1)}"
        }

        return clean
    }

    /**
     * إرسال رسالة مباشرة إلى جهة اتصال عبر الواتساب مع خيار التراجع التلقائي للمشاركة العامة
     */
    fun sendWhatsAppMessage(context: Context, phone: String, message: String) {
        val normalized = normalizePhoneNumber(phone)
        try {
            val uri = if (normalized.isNotBlank()) {
                Uri.parse("https://api.whatsapp.com/send?phone=$normalized&text=${Uri.encode(message)}")
            } else {
                Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(message)}")
            }

            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // محاولة أخرى عبر مشاركة عامة
            shareTextIntent(context, message, "مشاركة الفاتورة عبر:")
        }
    }

    /**
     * فتح نافذة المشاركة العامة في الأندرويد لأي تطبيق (واتساب، تيليجرام، بلوتوث وغيرها)
     */
    fun shareTextIntent(context: Context, message: String, title: String = "مشاركة عبر:") {
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val chooser = Intent.createChooser(sendIntent, title).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح نافذة المشاركة", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * اسم بديل متوافق لصياغة فاتورة بيع الكروت
     */
    fun generateCardInvoiceMessage(
        invoiceNumber: String,
        pkg: CardPackageEntity,
        retailer: RetailerEntity,
        quantity: Int,
        paymentMethod: String,
        notes: String = "",
        issuerName: String = "المهندس حسن"
    ): String = generateCardSalesInvoiceMessage(invoiceNumber, pkg, retailer, quantity, paymentMethod, notes, issuerName)

    /**
     * صياغة فاتورة بيع كروت محاسبية رسمية للبقالات مع تذييل شبكة طلقة نت
     */
    fun generateCardSalesInvoiceMessage(
        invoiceNumber: String,
        pkg: CardPackageEntity,
        retailer: RetailerEntity,
        quantity: Int,
        paymentMethod: String,
        notes: String = "",
        issuerName: String = "المهندس حسن"
    ): String {
        val totalWholesale = (pkg.wholesalePrice.toDouble() * quantity).toInt()
        val totalRetail = (pkg.retailPrice.toDouble() * quantity).toInt()
        val totalProfit = (totalRetail - totalWholesale).coerceAtLeast(0)
        val dateStr = SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale("ar")).format(Date())
        val paidNow = if (paymentMethod.contains("آجل")) 0 else totalWholesale
        val remainingInvoice = if (paymentMethod.contains("آجل")) totalWholesale else 0
        val totalBalanceOwed = (retailer.balanceOwed.toDouble() + remainingInvoice).toInt()

        val builder = StringBuilder()
        builder.appendLine("🧾 *فاتورة مبيعات كروت - $NETWORK_BRAND_NAME*")
        builder.appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━")
        builder.appendLine("📄 *رقم الفاتورة:* #$invoiceNumber")
        builder.appendLine("📅 *التاريخ:* $dateStr")
        builder.appendLine("🏪 *العميل / البقالة:* ${retailer.name}")
        if (retailer.ownerName.isNotBlank()) {
            builder.appendLine("👤 *المسؤول:* ${retailer.ownerName}")
        }
        if (retailer.location.isNotBlank()) {
            builder.appendLine("📍 *الموقع:* ${retailer.location}")
        }
        builder.appendLine("─────────────────────────")
        builder.appendLine("📦 *فئة الكروت:* ${pkg.name}")
        builder.appendLine("📶 *الحصة والسرعة:* ${pkg.formattedQuota} • ${pkg.speedLimit}")
        builder.appendLine("⏱️ *مدة الصلاحية:* ${pkg.formattedValidity}")
        builder.appendLine("🔢 *الكمية:* $quantity كرت")
        builder.appendLine("💵 *سعر الجملة للكرت:* ${pkg.wholesalePrice.toInt()} ريال")
        builder.appendLine("💰 *إجمالي الفاتورة الحالي:* $totalWholesale ريال يمني")
        builder.appendLine("💵 *المبلغ الواصل / المسدد الآن:* $paidNow ريال يمني")
        builder.appendLine("⏳ *المتبقي من هذه الفاتورة:* $remainingInvoice ريال يمني")
        builder.appendLine("💳 *إجمالي الرصيد المتبقي الكلي في ذمتكم حتى تاريخه:* $totalBalanceOwed ريال")
        builder.appendLine("📈 *إجمالي البيع للمستهلكين:* $totalRetail ريال")
        builder.appendLine("🎁 *صافي ربح البقالة:* +$totalProfit ريال")
        builder.appendLine("💳 *طريقة السداد:* $paymentMethod")
        if (notes.isNotBlank()) {
            builder.appendLine("📝 *ملاحظات:* $notes")
        }
        builder.appendLine("─────────────────────────")
        builder.appendLine("• رصيد الكروت بحوزتكم: ${retailer.activeCardsCount + quantity} كرت")
        builder.appendLine("👤 *المسؤول المعتمد:* $issuerName")
        builder.appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━")
        builder.appendLine("📡 *إدارة $NETWORK_BRAND_NAME للإنترنت عالي السرعة*")
        builder.appendLine("⚡ أقوى تغطية • أعلى سرعة • خدمة مستمرة 24/7")
        builder.appendLine("📞 خدمة العملاء والدعم الفني: $NETWORK_SUPPORT_PHONE")
        builder.append("✨ نسعد دائماً بخدمتكم والتعامل معكم - $NETWORK_BRAND_NAME ✨")

        return builder.toString()
    }

    /**
     * صياغة نص فاتورة مبيعات كروت كائنية
     */
    fun generateCardSalesInvoiceMessage(
        invoice: com.example.data.local.entity.CardSalesInvoiceEntity,
        retailerBalanceOwed: Double? = null
    ): String {
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("ar"))
        val dateStr = sdf.format(Date(invoice.invoiceDateMillis))
        val totalBalance = (retailerBalanceOwed ?: invoice.remainingAmount).toInt()

        return buildString {
            appendLine("🧾 *فاتورة مبيعات كروت - $NETWORK_BRAND_NAME*")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("📄 *رقم الفاتورة:* #${invoice.invoiceNumber}")
            appendLine("📅 *التاريخ:* $dateStr")
            appendLine("🏪 *العميل:* ${invoice.customerName}")
            if (invoice.customerPhone.isNotBlank()) {
                appendLine("📱 *الهاتف:* ${invoice.customerPhone}")
            }
            if (invoice.itemsSummary.isNotBlank()) {
                appendLine("📦 *الأصناف:* ${invoice.itemsSummary}")
            }
            appendLine("🔢 *إجمالي الكروت:* ${invoice.totalCardsCount} كرت")
            appendLine("─────────────────────────")
            appendLine("💰 *إجمالي الفاتورة الحالي:* ${invoice.totalAmount.toInt()} ريال يمني")
            appendLine("💵 *المبلغ الواصل / المسدد الآن:* ${invoice.paidAmount.toInt()} ريال يمني")
            appendLine("⏳ *المتبقي من هذه الفاتورة:* ${invoice.remainingAmount.toInt()} ريال يمني")
            appendLine("💳 *إجمالي الرصيد المتبقي الكلي في ذمتكم حتى تاريخه:* $totalBalance ريال")
            if (invoice.notes.isNotBlank()) {
                appendLine("📝 *ملاحظات:* ${invoice.notes}")
            }
            appendLine("👤 *المسؤول المعتمد:* ${invoice.issuerName.ifBlank { "المهندس حسن" }}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("📡 *إدارة $NETWORK_BRAND_NAME للإنترنت عالي السرعة*")
            appendLine("📞 خدمة العملاء والدعم الفني: $NETWORK_SUPPORT_PHONE")
            append("✨ نسعد دائماً بخدمتكم والتعامل معكم ✨")
        }
    }

    /**
     * صياغة سند قبض أو صرف مالي رسمي مع تذييل شبكة طلقة نت
     */
    fun generateVoucherMessage(
        voucher: FinancialVoucherEntity,
        retailerPhone: String? = null,
        retailerBalanceOwed: Double? = null
    ): String {
        val isReceipt = voucher.voucherType == "RECEIPT"
        val title = if (isReceipt) "سند قبض مالي رسمي" else "سند صرف مالي رسمي"
        val dateStr = SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale("ar")).format(Date(voucher.dateMillis))

        val builder = StringBuilder()
        builder.appendLine("🧾 *$title - $NETWORK_BRAND_NAME*")
        builder.appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━")
        builder.appendLine("📄 *رقم السند:* ${voucher.voucherNumber}")
        builder.appendLine("📅 *التاريخ:* $dateStr")
        if (voucher.isVoided) {
            builder.appendLine("⛔ *حالة السند:* ملغى / VOID (تم إلغاء التأثير المالي للسند)")
        }
        if (voucher.invoiceNumber.isNotBlank()) {
            builder.appendLine("🔗 *مرتبط بالفاتورة رقم:* #${voucher.invoiceNumber}")
        }
        builder.appendLine("─────────────────────────")
        builder.appendLine("💰 *مبلغ السند الحالي:* ${voucher.amount.toInt()} ريال يمني")
        builder.appendLine("💵 *المبلغ المسدد:* ${if (voucher.isVoided) 0 else voucher.amount.toInt()} ريال يمني")
        if (retailerBalanceOwed != null) {
            builder.appendLine("💳 *إجمالي الرصيد التراكمي المستحق الكلي في ذمتكم حتى تاريخه:* ${retailerBalanceOwed.toInt()} ريال يمني")
        }
        builder.appendLine(if (isReceipt) "📥 *استلمنا من الأخ:* ${voucher.partyName}" else "📤 *صرفنا إلى الأخ:* ${voucher.partyName}")
        if (!retailerPhone.isNullOrBlank()) {
            builder.appendLine("📱 *هاتف العميل:* $retailerPhone")
        }
        builder.appendLine("📝 *وذلك عن:* ${voucher.category} - ${voucher.description}")
        builder.appendLine("💳 *طريقة الدفع:* ${voucher.paymentMethod}")
        builder.appendLine("👤 *أمين الصندوق / الإدارة:* ${voucher.issuerName}")
        builder.appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━")
        builder.appendLine("📡 *نظام الحسابات والمالية - $NETWORK_BRAND_NAME*")
        builder.appendLine("📞 خدمة العملاء والدعم: $NETWORK_SUPPORT_PHONE")
        builder.append("⚡ شكراً لتعاملكم معنا - مع تحيات إدارة $NETWORK_BRAND_NAME")

        return builder.toString()
    }

    /**
     * صياغة كشف حساب ومطابقة رصيد للبقالة مع تذييل شبكة طلقة نت
     */
    fun generateRetailerStatementMessage(
        retailer: RetailerEntity
    ): String {
        val dateStr = SimpleDateFormat("yyyy/MM/dd", Locale("ar")).format(Date())

        val builder = StringBuilder()
        builder.appendLine("📋 *كشف حساب ومطابقة رصيد - $NETWORK_BRAND_NAME*")
        builder.appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━")
        builder.appendLine("🏪 *البقالة / المحل:* ${retailer.name}")
        if (retailer.ownerName.isNotBlank()) {
            builder.appendLine("👤 *المسؤول:* ${retailer.ownerName}")
        }
        if (retailer.location.isNotBlank()) {
            builder.appendLine("📍 *الموقع:* ${retailer.location}")
        }
        builder.appendLine("📅 *تاريخ الكشف:* $dateStr")
        builder.appendLine("─────────────────────────")
        builder.appendLine("💳 *الرصيد المستحق للشبكة:* ${retailer.balanceOwed.toInt()} ريال")
        builder.appendLine("💰 *إجمالي المبالغ المسددة:* ${retailer.totalPaid.toInt()} ريال")
        builder.appendLine("📦 *الكروت المتبقية بحوزتكم:* ${retailer.activeCardsCount} كرت")
        builder.appendLine("🏷️ *نسبة العمولة المعتمدة:* ${retailer.commissionPercent.toInt()}%")
        if (retailer.notes.isNotBlank()) {
            builder.appendLine("📝 *ملاحظات الحساب:* ${retailer.notes}")
        }
        builder.appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━")
        builder.appendLine("📡 *إدارة $NETWORK_BRAND_NAME للإنترنت*")
        builder.appendLine("📞 للاستفسارات وخدمة العملاء: $NETWORK_SUPPORT_PHONE")
        builder.append("⚡ نسعد بخدمتكم دائماً - مع تحيات إدارة $NETWORK_BRAND_NAME")

        return builder.toString()
    }

    /**
     * صياغة إشعار تسليم وتوزيع كروت للبقالة
     */
    fun generateCardDeliveryReceiptMessage(
        retailer: RetailerEntity,
        batchName: String,
        quantity: Int,
        totalWholesale: Double
    ): String {
        val dateStr = SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale("ar")).format(Date())
        val builder = StringBuilder()
        builder.appendLine("📦 *إشعار استلام وتسليم كروت - $NETWORK_BRAND_NAME*")
        builder.appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━")
        builder.appendLine("🏪 *البقالة المستلمة:* ${retailer.name}")
        builder.appendLine("👤 *المستلم:* ${retailer.ownerName}")
        builder.appendLine("📅 *تاريخ التسليم:* $dateStr")
        builder.appendLine("─────────────────────────")
        builder.appendLine("🏷️ *الدفعة / الفئة:* $batchName")
        builder.appendLine("🔢 *الكمية المسلمة:* $quantity كرت")
        builder.appendLine("💰 *إجمالي القيمة (جملة):* ${totalWholesale.toInt()} ريال")
        builder.appendLine("📦 *إجمالي الكروت بحوزتكم الآن:* ${retailer.activeCardsCount + quantity} كرت")
        builder.appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━")
        builder.appendLine("📡 *إدارة شبكة وتوزيع $NETWORK_BRAND_NAME*")
        builder.appendLine("📞 للاستفسار والدعم الفني: $NETWORK_SUPPORT_PHONE")
        builder.append("⚡ مع تحيات إدارة $NETWORK_BRAND_NAME")
        return builder.toString()
    }
}
