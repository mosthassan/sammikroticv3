package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.entity.CardSalesInvoiceEntity
import com.example.data.local.entity.FinancialVoucherEntity
import com.example.data.local.entity.NetworkIdentityEntity
import com.example.data.local.entity.RetailerEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * مدير توليد ومشاركة التقارير المالية وكشوفات الحسابات كملفات PDF رسمية احترافية
 * - يولّد ملفات PDF بدقة A4 ومظهر محاسبي فاخر متوافق مع الهوية البصرية للشبكة
 * - يدعم التقارير المالية الشاملة وكشوفات حسابات الوكلاء والتصديق الرسمي
 */
object ReportPdfManager {

    private const val PAGE_WIDTH = 595 // Standard A4 width in points at 72dpi
    private const val PAGE_HEIGHT = 842 // Standard A4 height in points at 72dpi

    /**
     * توليد ملف PDF للتقرير المالي الشامل
     */
    fun generateFinancialReportPdfFile(
        context: Context,
        networkIdentity: NetworkIdentityEntity,
        timeRangeLabel: String,
        totalSales: Double,
        totalReceipts: Double,
        totalExpenses: Double,
        netTrueProfit: Double,
        topAgentsBySales: List<Pair<RetailerEntity, Double>>,
        topAgingDebtors: List<Pair<RetailerEntity, Double>>,
        aiSummaryText: String? = null
    ): File {
        val reportsDir = File(context.cacheDir, "shared_docs").apply {
            if (!exists()) mkdirs()
        }
        val safeFileName = "تقرير_مالي_شامل_${System.currentTimeMillis() % 10000}.pdf"
        val file = File(reportsDir, safeFileName)

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        drawFinancialReportOnCanvas(
            canvas = canvas,
            identity = networkIdentity,
            timeRangeLabel = timeRangeLabel,
            totalSales = totalSales,
            totalReceipts = totalReceipts,
            totalExpenses = totalExpenses,
            netTrueProfit = netTrueProfit,
            topAgentsBySales = topAgentsBySales,
            topAgingDebtors = topAgingDebtors,
            aiSummaryText = aiSummaryText
        )

        document.finishPage(page)
        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()

        return file
    }

    /**
     * توليد كشف حساب وكيل/بقالة كملف PDF
     */
    fun generateAgentAccountStatementPdfFile(
        context: Context,
        networkIdentity: NetworkIdentityEntity,
        retailer: RetailerEntity,
        invoices: List<CardSalesInvoiceEntity>,
        vouchers: List<FinancialVoucherEntity>,
        timeRangeLabel: String
    ): File {
        val reportsDir = File(context.cacheDir, "shared_docs").apply {
            if (!exists()) mkdirs()
        }
        val safeAgentName = retailer.name.replace("[^a-zA-Z0-9_ -]".toRegex(), "_")
        val safeFileName = "كشف_حساب_${safeAgentName}.pdf"
        val file = File(reportsDir, safeFileName)

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        drawAgentStatementOnCanvas(
            canvas = canvas,
            identity = networkIdentity,
            retailer = retailer,
            invoices = invoices,
            vouchers = vouchers,
            timeRangeLabel = timeRangeLabel
        )

        document.finishPage(page)
        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()

        return file
    }

    /**
     * مشاركة ملف PDF عبر التطبيقات
     */
    fun sharePdf(
        context: Context,
        pdfFile: File,
        subject: String,
        text: String
    ) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "مشاركة التقرير عبر PDF")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "خطأ في مشاركة المستند: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * فتح ملف PDF لمعاينته مباشرة
     */
    fun openPdf(
        context: Context,
        pdfFile: File
    ) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(viewIntent, "فتح التقرير")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح التقرير: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    // =========================================================
    // Canvas Drawing Helpers
    // =========================================================

    private fun drawFinancialReportOnCanvas(
        canvas: Canvas,
        identity: NetworkIdentityEntity,
        timeRangeLabel: String,
        totalSales: Double,
        totalReceipts: Double,
        totalExpenses: Double,
        netTrueProfit: Double,
        topAgentsBySales: List<Pair<RetailerEntity, Double>>,
        topAgingDebtors: List<Pair<RetailerEntity, Double>>,
        aiSummaryText: String?
    ) {
        // 1. White Background
        val bgPaint = Paint().apply { color = AndroidColor.WHITE; style = Paint.Style.FILL }
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), bgPaint)

        // Outer Border
        val framePaint = Paint().apply { color = AndroidColor.parseColor("#CBD5E1"); style = Paint.Style.STROKE; strokeWidth = 1f }
        canvas.drawRoundRect(RectF(18f, 18f, PAGE_WIDTH - 18f, PAGE_HEIGHT - 18f), 8f, 8f, framePaint)

        // 2. Top Banner Header
        val headerPaint = Paint().apply { color = AndroidColor.parseColor("#0A192F"); style = Paint.Style.FILL }
        canvas.drawRoundRect(RectF(18f, 18f, PAGE_WIDTH - 18f, 95f), 8f, 8f, headerPaint)
        canvas.drawRect(18f, 80f, PAGE_WIDTH - 18f, 95f, headerPaint)

        val accentLinePaint = Paint().apply { color = AndroidColor.parseColor("#0284C7"); style = Paint.Style.FILL }
        canvas.drawRect(18f, 95f, PAGE_WIDTH - 18f, 98f, accentLinePaint)

        val brandPaint = Paint().apply { color = AndroidColor.WHITE; textSize = 16f; isFakeBoldText = true; textAlign = Paint.Align.RIGHT }
        val networkNameStr = identity.networkName.ifBlank { "شبكة سام ميكروتك الذكية" }
        canvas.drawText(networkNameStr, PAGE_WIDTH - 36f, 48f, brandPaint)

        val brandSubPaint = Paint().apply { color = AndroidColor.parseColor("#94A3B8"); textSize = 9.5f; textAlign = Paint.Align.RIGHT }
        canvas.drawText("تقرير مالي شامل ومؤشرات أداء المبيعات والديون", PAGE_WIDTH - 36f, 65f, brandSubPaint)

        val docTitlePaint = Paint().apply { color = AndroidColor.parseColor("#38BDF8"); textSize = 14f; isFakeBoldText = true; textAlign = Paint.Align.LEFT }
        canvas.drawText("التقرير المالي المجمع", 36f, 48f, docTitlePaint)

        val docSubTitlePaint = Paint().apply { color = AndroidColor.parseColor("#F59E0B"); textSize = 9.5f; isFakeBoldText = true; textAlign = Paint.Align.LEFT }
        canvas.drawText("FINANCIAL DASHBOARD REPORT", 36f, 65f, docSubTitlePaint)

        // 3. Metadata Info Bar
        var currentY = 110f
        val metaRect = RectF(30f, currentY, PAGE_WIDTH - 30f, currentY + 36f)
        val metaBg = Paint().apply { color = AndroidColor.parseColor("#F8FAFC"); style = Paint.Style.FILL }
        val metaBorder = Paint().apply { color = AndroidColor.parseColor("#E2E8F0"); style = Paint.Style.STROKE; strokeWidth = 1f }
        canvas.drawRoundRect(metaRect, 6f, 6f, metaBg)
        canvas.drawRoundRect(metaRect, 6f, 6f, metaBorder)

        val metaValPaint = Paint().apply { color = AndroidColor.parseColor("#0F172A"); textSize = 9.5f; isFakeBoldText = true; textAlign = Paint.Align.RIGHT }
        val leftMetaValPaint = Paint(metaValPaint).apply { textAlign = Paint.Align.LEFT }

        val nowStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).format(Date())
        canvas.drawText("الفترة المحددة:  $timeRangeLabel", PAGE_WIDTH - 42f, currentY + 22f, metaValPaint)
        canvas.drawText("تاريخ الإصدار:  $nowStr", 42f, currentY + 22f, leftMetaValPaint)

        currentY += 48f

        // 4. KPI Cards Grid (2x2 Box)
        val boxWidth = (PAGE_WIDTH - 72f) / 2f
        val boxHeight = 52f

        // Card 1: Total Sales
        drawKpiBoxOnCanvas(
            canvas = canvas,
            left = 30f,
            top = currentY,
            width = boxWidth,
            height = boxHeight,
            title = "إجمالي المبيعات",
            amountStr = "${String.format(Locale.US, "%,.0f", totalSales)} ر.ي",
            headerBgColor = "#0284C7"
        )

        // Card 2: Total Receipts
        drawKpiBoxOnCanvas(
            canvas = canvas,
            left = 30f + boxWidth + 12f,
            top = currentY,
            width = boxWidth,
            height = boxHeight,
            title = "إجمالي المقبوضات",
            amountStr = "${String.format(Locale.US, "%,.0f", totalReceipts)} ر.ي",
            headerBgColor = "#16A34A"
        )

        currentY += boxHeight + 10f

        // Card 3: Total Expenses
        drawKpiBoxOnCanvas(
            canvas = canvas,
            left = 30f,
            top = currentY,
            width = boxWidth,
            height = boxHeight,
            title = "إجمالي المصروفات",
            amountStr = "${String.format(Locale.US, "%,.0f", totalExpenses)} ر.ي",
            headerBgColor = "#DC2626"
        )

        // Card 4: Net True Profit
        val profitColorStr = if (netTrueProfit >= 0) "#059669" else "#DC2626"
        drawKpiBoxOnCanvas(
            canvas = canvas,
            left = 30f + boxWidth + 12f,
            top = currentY,
            width = boxWidth,
            height = boxHeight,
            title = "صافي الربح الحقيقي",
            amountStr = "${String.format(Locale.US, "%,.0f", netTrueProfit)} ر.ي",
            headerBgColor = profitColorStr
        )

        currentY += boxHeight + 16f

        // 5. AI Analytical Summary Section (if available)
        if (!aiSummaryText.isNullOrBlank()) {
            val aiBoxRect = RectF(30f, currentY, PAGE_WIDTH - 30f, currentY + 70f)
            val aiBg = Paint().apply { color = AndroidColor.parseColor("#F0F9FF"); style = Paint.Style.FILL }
            val aiBorder = Paint().apply { color = AndroidColor.parseColor("#BAE6FD"); style = Paint.Style.STROKE; strokeWidth = 1f }
            canvas.drawRoundRect(aiBoxRect, 6f, 6f, aiBg)
            canvas.drawRoundRect(aiBoxRect, 6f, 6f, aiBorder)

            val aiTitlePaint = Paint().apply { color = AndroidColor.parseColor("#0369A1"); textSize = 10f; isFakeBoldText = true; textAlign = Paint.Align.RIGHT }
            canvas.drawText("🤖 التحليل الذكي AI لأداء الفترة:", PAGE_WIDTH - 42f, currentY + 18f, aiTitlePaint)

            val aiBodyPaint = Paint().apply { color = AndroidColor.parseColor("#0C4A6E"); textSize = 8.5f; textAlign = Paint.Align.RIGHT }
            val lines = chunkText(aiSummaryText, 85)
            var lineY = currentY + 34f
            for (line in lines.take(3)) {
                canvas.drawText(line, PAGE_WIDTH - 42f, lineY, aiBodyPaint)
                lineY += 13f
            }

            currentY += 80f
        }

        // 6. Top 5 Agents by Purchases Table
        val thBg = Paint().apply { color = AndroidColor.parseColor("#0F2744"); style = Paint.Style.FILL }
        canvas.drawRoundRect(RectF(30f, currentY, PAGE_WIDTH - 30f, currentY + 20f), 4f, 4f, thBg)

        val thText = Paint().apply { color = AndroidColor.WHITE; textSize = 9f; isFakeBoldText = true }
        thText.textAlign = Paint.Align.RIGHT
        canvas.drawText("أعلى 5 وكلاء شراءً في هذه الفترة", PAGE_WIDTH - 42f, currentY + 14f, thText)

        currentY += 20f

        val rowBgEven = Paint().apply { color = AndroidColor.WHITE; style = Paint.Style.FILL }
        val rowBgOdd = Paint().apply { color = AndroidColor.parseColor("#F8FAFC"); style = Paint.Style.FILL }
        val rowBorder = Paint().apply { color = AndroidColor.parseColor("#E2E8F0"); style = Paint.Style.STROKE; strokeWidth = 0.5f }
        val tdText = Paint().apply { color = AndroidColor.parseColor("#1E293B"); textSize = 8.5f }
        val tdBold = Paint().apply { color = AndroidColor.parseColor("#0F172A"); textSize = 8.5f; isFakeBoldText = true }

        if (topAgentsBySales.isEmpty()) {
            val emptyRect = RectF(30f, currentY, PAGE_WIDTH - 30f, currentY + 20f)
            canvas.drawRect(emptyRect, rowBgEven)
            canvas.drawRect(emptyRect, rowBorder)
            tdText.textAlign = Paint.Align.CENTER
            canvas.drawText("لا توجد مبيعات وكلاء مسجلة في هذه الفترة", PAGE_WIDTH / 2f, currentY + 14f, tdText)
            currentY += 20f
        } else {
            for ((idx, pair) in topAgentsBySales.take(5).withIndex()) {
                val rowRect = RectF(30f, currentY, PAGE_WIDTH - 30f, currentY + 18f)
                canvas.drawRect(rowRect, if (idx % 2 == 0) rowBgEven else rowBgOdd)
                canvas.drawRect(rowRect, rowBorder)

                tdBold.textAlign = Paint.Align.RIGHT
                canvas.drawText("${idx + 1}. ${pair.first.name}", PAGE_WIDTH - 42f, currentY + 13f, tdBold)

                tdText.textAlign = Paint.Align.LEFT
                canvas.drawText("مبيعات: ${String.format(Locale.US, "%,.0f", pair.second)} ر.ي", 42f, currentY + 13f, tdText)

                currentY += 18f
            }
        }

        currentY += 12f

        // 7. Top 5 Aging Debtors Table
        val thBg2 = Paint().apply { color = AndroidColor.parseColor("#881337"); style = Paint.Style.FILL }
        canvas.drawRoundRect(RectF(30f, currentY, PAGE_WIDTH - 30f, currentY + 20f), 4f, 4f, thBg2)

        val thText2 = Paint(thText)
        canvas.drawText("أعلى 5 وكلاء مديونية (الديون التراكمية)", PAGE_WIDTH - 42f, currentY + 14f, thText2)

        currentY += 20f

        if (topAgingDebtors.isEmpty()) {
            val emptyRect = RectF(30f, currentY, PAGE_WIDTH - 30f, currentY + 20f)
            canvas.drawRect(emptyRect, rowBgEven)
            canvas.drawRect(emptyRect, rowBorder)
            tdText.textAlign = Paint.Align.CENTER
            canvas.drawText("لا توجد مديونيات أو ديون على الوكلاء", PAGE_WIDTH / 2f, currentY + 14f, tdText)
            currentY += 20f
        } else {
            for ((idx, pair) in topAgingDebtors.take(5).withIndex()) {
                val rowRect = RectF(30f, currentY, PAGE_WIDTH - 30f, currentY + 18f)
                canvas.drawRect(rowRect, if (idx % 2 == 0) rowBgEven else rowBgOdd)
                canvas.drawRect(rowRect, rowBorder)

                tdBold.textAlign = Paint.Align.RIGHT
                canvas.drawText("${idx + 1}. ${pair.first.name} (${pair.first.phone})", PAGE_WIDTH - 42f, currentY + 13f, tdBold)

                val debtPaint = Paint(tdBold).apply { color = AndroidColor.parseColor("#DC2626"); textAlign = Paint.Align.LEFT }
                canvas.drawText("مديونية: ${String.format(Locale.US, "%,.0f", pair.second)} ر.ي", 42f, currentY + 13f, debtPaint)

                currentY += 18f
            }
        }

        // 8. Signature & Footer Line
        val signY = PAGE_HEIGHT - 95f
        val signLabelPaint = Paint().apply { color = AndroidColor.parseColor("#475569"); textSize = 8.5f; textAlign = Paint.Align.CENTER }
        val signLinePaint = Paint().apply { color = AndroidColor.parseColor("#94A3B8"); strokeWidth = 0.8f }

        canvas.drawText("اعتماد الإدارة المالية والمالك", PAGE_WIDTH - 120f, signY, signLabelPaint)
        canvas.drawLine(PAGE_WIDTH - 200f, signY + 24f, PAGE_WIDTH - 40f, signY + 24f, signLinePaint)

        canvas.drawText("التوقيع والختم الرسمي", 120f, signY, signLabelPaint)
        canvas.drawLine(40f, signY + 24f, 200f, signY + 24f, signLinePaint)

        val footerPaint = Paint().apply { color = AndroidColor.parseColor("#64748B"); textSize = 8f; textAlign = Paint.Align.CENTER }
        canvas.drawText(
            "${identity.networkName} • تم الإصدار آلياً بواسطة تطبيق سام ميكروتك للتقارير والتحليلات الذكية",
            PAGE_WIDTH / 2f,
            PAGE_HEIGHT - 30f,
            footerPaint
        )
    }

    private fun drawAgentStatementOnCanvas(
        canvas: Canvas,
        identity: NetworkIdentityEntity,
        retailer: RetailerEntity,
        invoices: List<CardSalesInvoiceEntity>,
        vouchers: List<FinancialVoucherEntity>,
        timeRangeLabel: String
    ) {
        // 1. White Background & Outer Frame
        val bgPaint = Paint().apply { color = AndroidColor.WHITE; style = Paint.Style.FILL }
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), bgPaint)

        val framePaint = Paint().apply { color = AndroidColor.parseColor("#CBD5E1"); style = Paint.Style.STROKE; strokeWidth = 1f }
        canvas.drawRoundRect(RectF(18f, 18f, PAGE_WIDTH - 18f, PAGE_HEIGHT - 18f), 8f, 8f, framePaint)

        // 2. Header
        val headerPaint = Paint().apply { color = AndroidColor.parseColor("#0F172A"); style = Paint.Style.FILL }
        canvas.drawRoundRect(RectF(18f, 18f, PAGE_WIDTH - 18f, 95f), 8f, 8f, headerPaint)
        canvas.drawRect(18f, 80f, PAGE_WIDTH - 18f, 95f, headerPaint)

        val accentLinePaint = Paint().apply { color = AndroidColor.parseColor("#0284C7"); style = Paint.Style.FILL }
        canvas.drawRect(18f, 95f, PAGE_WIDTH - 18f, 98f, accentLinePaint)

        val brandPaint = Paint().apply { color = AndroidColor.WHITE; textSize = 16f; isFakeBoldText = true; textAlign = Paint.Align.RIGHT }
        val netName = identity.networkName.ifBlank { "شبكة سام ميكروتك" }
        canvas.drawText(netName, PAGE_WIDTH - 36f, 48f, brandPaint)

        val brandSubPaint = Paint().apply { color = AndroidColor.parseColor("#94A3B8"); textSize = 9.5f; textAlign = Paint.Align.RIGHT }
        canvas.drawText("كشف حساب تفصيلي ومطابقة أرصدة الوكيل / البقالة", PAGE_WIDTH - 36f, 65f, brandSubPaint)

        val docTitlePaint = Paint().apply { color = AndroidColor.parseColor("#38BDF8"); textSize = 14f; isFakeBoldText = true; textAlign = Paint.Align.LEFT }
        canvas.drawText("كشف حساب وكيل", 36f, 48f, docTitlePaint)

        val docSubTitlePaint = Paint().apply { color = AndroidColor.parseColor("#F59E0B"); textSize = 9.5f; isFakeBoldText = true; textAlign = Paint.Align.LEFT }
        canvas.drawText("AGENT ACCOUNT STATEMENT", 36f, 65f, docSubTitlePaint)

        // 3. Agent & Meta Card
        var currentY = 110f
        val agentCardRect = RectF(30f, currentY, PAGE_WIDTH - 30f, currentY + 60f)
        val agentBg = Paint().apply { color = AndroidColor.parseColor("#F8FAFC"); style = Paint.Style.FILL }
        val agentBorder = Paint().apply { color = AndroidColor.parseColor("#E2E8F0"); style = Paint.Style.STROKE; strokeWidth = 1f }
        canvas.drawRoundRect(agentCardRect, 6f, 6f, agentBg)
        canvas.drawRoundRect(agentCardRect, 6f, 6f, agentBorder)

        val agentNamePaint = Paint().apply { color = AndroidColor.parseColor("#0F172A"); textSize = 12f; isFakeBoldText = true; textAlign = Paint.Align.RIGHT }
        canvas.drawText("الوكيل / البقالة:  ${retailer.name}", PAGE_WIDTH - 42f, currentY + 24f, agentNamePaint)

        val agentSubPaint = Paint().apply { color = AndroidColor.parseColor("#475569"); textSize = 9.5f; textAlign = Paint.Align.RIGHT }
        val phoneStr = if (retailer.phone.isNotBlank()) "هاتف: ${retailer.phone}" else ""
        val locStr = if (retailer.location.isNotBlank()) "الموقع: ${retailer.location}" else ""
        canvas.drawText("$phoneStr  •  $locStr", PAGE_WIDTH - 42f, currentY + 44f, agentSubPaint)

        // Balance Badges on Right / Left
        val leftTextPaint = Paint().apply { color = AndroidColor.parseColor("#DC2626"); textSize = 12f; isFakeBoldText = true; textAlign = Paint.Align.LEFT }
        canvas.drawText("الرصيد المتبقي:  ${String.format(Locale.US, "%,.0f", retailer.balanceOwed)} ر.ي", 42f, currentY + 24f, leftTextPaint)

        val leftSubPaint = Paint().apply { color = AndroidColor.parseColor("#64748B"); textSize = 9f; textAlign = Paint.Align.LEFT }
        canvas.drawText("الفترة: $timeRangeLabel", 42f, currentY + 44f, leftSubPaint)

        currentY += 72f

        // 4. Statement Transactions Table Header
        val thBg = Paint().apply { color = AndroidColor.parseColor("#0F2744"); style = Paint.Style.FILL }
        canvas.drawRoundRect(RectF(30f, currentY, PAGE_WIDTH - 30f, currentY + 22f), 4f, 4f, thBg)

        val thText = Paint().apply { color = AndroidColor.WHITE; textSize = 9f; isFakeBoldText = true }

        // Columns: [م] (42) | [التاريخ] (80) | [البيان / رقم المستند] (220) | [مدين (مبيعات)] (370) | [دائن (سداد)] (460) | [الرصيد] (540)
        thText.textAlign = Paint.Align.CENTER
        canvas.drawText("التاريخ", PAGE_WIDTH - 65f, currentY + 15f, thText)

        thText.textAlign = Paint.Align.RIGHT
        canvas.drawText("البيان ونوع الحركة", PAGE_WIDTH - 120f, currentY + 15f, thText)

        thText.textAlign = Paint.Align.CENTER
        canvas.drawText("مدين (عليها)", PAGE_WIDTH - 300f, currentY + 15f, thText)
        canvas.drawText("دائن (مسدد)", PAGE_WIDTH - 390f, currentY + 15f, thText)

        thText.textAlign = Paint.Align.LEFT
        canvas.drawText("الرصيد المتبقي", 42f, currentY + 15f, thText)

        currentY += 22f

        // Combine Invoices (Debit) & Vouchers (Credit) sorted by date
        data class StatementTx(
            val dateMillis: Long,
            val reference: String,
            val description: String,
            val debit: Double,  // Invoice total amount
            val credit: Double  // Receipt Voucher amount
        )

        val cleanInvoices = invoices
            .filter { !it.invoiceNumber.startsWith("INV-DELIV-") }
            .distinctBy { if (it.invoiceNumber.isNotBlank()) it.invoiceNumber else it.id.toString() }

        val cleanVouchers = vouchers
            .filter { !it.isVoided && it.voucherType == "RECEIPT" }
            .distinctBy { if (it.voucherNumber.isNotBlank()) it.voucherNumber else it.id.toString() }

        val allTxs = mutableListOf<StatementTx>()
        cleanInvoices.forEach { inv ->
            val hasTiedVoucher = cleanVouchers.any { v ->
                v.invoiceId == inv.id || (v.invoiceNumber.isNotBlank() && v.invoiceNumber == inv.invoiceNumber)
            }
            allTxs.add(
                StatementTx(
                    dateMillis = inv.invoiceDateMillis,
                    reference = inv.invoiceNumber,
                    description = "فاتورة مبيعات كروت (${inv.totalCardsCount} كرت)",
                    debit = inv.totalAmount.toDouble(),
                    credit = if (!hasTiedVoucher && inv.paidAmount > java.math.BigDecimal.ZERO) inv.paidAmount.toDouble() else 0.0
                )
            )
        }
        cleanVouchers.forEach { v ->
            val isRec = v.voucherType == "RECEIPT"
            allTxs.add(
                StatementTx(
                    dateMillis = v.dateMillis,
                    reference = v.voucherNumber,
                    description = if (isRec) "سند قبض توريد (${v.paymentMethod.ifBlank { "نقداً" }})" else "سند صرف (${v.category})",
                    debit = if (!isRec) v.amount.toDouble() else 0.0,
                    credit = if (isRec) v.amount.toDouble() else 0.0
                )
            )
        }

        allTxs.sortBy { it.dateMillis }

        val totalDebitSum = allTxs.sumOf { it.debit }
        val totalCreditSum = allTxs.sumOf { it.credit }
        val calcBalance = totalDebitSum - totalCreditSum

        val rowBgEven = Paint().apply { color = AndroidColor.WHITE; style = Paint.Style.FILL }
        val rowBgOdd = Paint().apply { color = AndroidColor.parseColor("#F8FAFC"); style = Paint.Style.FILL }
        val rowBorder = Paint().apply { color = AndroidColor.parseColor("#CBD5E1"); style = Paint.Style.STROKE; strokeWidth = 0.5f }

        val tdText = Paint().apply { color = AndroidColor.parseColor("#1E293B"); textSize = 8.5f }
        val tdBold = Paint().apply { color = AndroidColor.parseColor("#0F172A"); textSize = 8.5f; isFakeBoldText = true }

        var runningBalance = 0.0
        val sdf = SimpleDateFormat("MM/dd", Locale.US)

        if (allTxs.isEmpty()) {
            val emptyRect = RectF(30f, currentY, PAGE_WIDTH - 30f, currentY + 22f)
            canvas.drawRect(emptyRect, rowBgEven)
            canvas.drawRect(emptyRect, rowBorder)
            tdText.textAlign = Paint.Align.CENTER
            canvas.drawText("لا توجد حركات مالية مسجلة لهذا الوكيل في هذه الفترة", PAGE_WIDTH / 2f, currentY + 15f, tdText)
            currentY += 22f
        } else {
            for ((idx, tx) in allTxs.take(18).withIndex()) {
                runningBalance += (tx.debit - tx.credit)

                val rowRect = RectF(30f, currentY, PAGE_WIDTH - 30f, currentY + 18f)
                canvas.drawRect(rowRect, if (idx % 2 == 0) rowBgEven else rowBgOdd)
                canvas.drawRect(rowRect, rowBorder)

                // Date
                tdText.textAlign = Paint.Align.CENTER
                canvas.drawText(sdf.format(Date(tx.dateMillis)), PAGE_WIDTH - 65f, currentY + 13f, tdText)

                // Ref & Description
                tdBold.textAlign = Paint.Align.RIGHT
                canvas.drawText("${tx.reference} - ${tx.description.take(22)}", PAGE_WIDTH - 120f, currentY + 13f, tdBold)

                // Debit (Sales)
                tdText.textAlign = Paint.Align.CENTER
                val debitTextStr = if (tx.debit > 0) "${tx.debit.toInt()}" else "-"
                canvas.drawText(debitTextStr, PAGE_WIDTH - 300f, currentY + 13f, tdText)

                // Credit (Paid)
                val creditPaint = Paint(tdText).apply { color = AndroidColor.parseColor("#16A34A") }
                val creditTextStr = if (tx.credit > 0) "${tx.credit.toInt()}" else "-"
                canvas.drawText(creditTextStr, PAGE_WIDTH - 390f, currentY + 13f, creditPaint)

                // Running Balance
                tdBold.textAlign = Paint.Align.LEFT
                canvas.drawText("${runningBalance.toInt()} ر.ي", 42f, currentY + 13f, tdBold)

                currentY += 18f
            }
        }

        currentY += 14f

        // 5. Total Summary Footer Card
        val totalSummaryRect = RectF(PAGE_WIDTH - 280f, currentY, PAGE_WIDTH - 30f, currentY + 65f)
        val sumBg = Paint().apply { color = AndroidColor.parseColor("#F1F5F9"); style = Paint.Style.FILL }
        val sumBorder = Paint().apply { color = AndroidColor.parseColor("#CBD5E1"); style = Paint.Style.STROKE; strokeWidth = 1f }
        canvas.drawRoundRect(totalSummaryRect, 6f, 6f, sumBg)
        canvas.drawRoundRect(totalSummaryRect, 6f, 6f, sumBorder)

        val sLabel = Paint().apply { color = AndroidColor.parseColor("#475569"); textSize = 9f; textAlign = Paint.Align.RIGHT }
        val sVal = Paint().apply { color = AndroidColor.parseColor("#0F172A"); textSize = 9.5f; isFakeBoldText = true; textAlign = Paint.Align.LEFT }

        canvas.drawText("إجمالي المبيعات (مدين):", PAGE_WIDTH - 42f, currentY + 18f, sLabel)
        canvas.drawText("${totalDebitSum.toInt()} ريال", PAGE_WIDTH - 268f, currentY + 18f, sVal)

        canvas.drawText("إجمالي المسدد (دائن):", PAGE_WIDTH - 42f, currentY + 36f, sLabel)
        val creditValPaint = Paint(sVal).apply { color = AndroidColor.parseColor("#16A34A") }
        canvas.drawText("${totalCreditSum.toInt()} ريال", PAGE_WIDTH - 268f, currentY + 36f, creditValPaint)

        canvas.drawText("الرصيد المتبقي النهائي:", PAGE_WIDTH - 42f, currentY + 54f, sLabel)
        val remColor = if (calcBalance > 0) AndroidColor.parseColor("#DC2626") else AndroidColor.parseColor("#16A34A")
        val remValPaint = Paint(sVal).apply { color = remColor; textSize = 10.5f }
        canvas.drawText("${calcBalance.toInt()} ريال", PAGE_WIDTH - 268f, currentY + 54f, remValPaint)

        // 6. Signature section
        val signY = PAGE_HEIGHT - 95f
        val signLabelPaint = Paint().apply { color = AndroidColor.parseColor("#475569"); textSize = 8.5f; textAlign = Paint.Align.CENTER }
        val signLinePaint = Paint().apply { color = AndroidColor.parseColor("#94A3B8"); strokeWidth = 0.8f }

        canvas.drawText("موافق وتوقيع الوكيل / البقالة", PAGE_WIDTH - 120f, signY, signLabelPaint)
        canvas.drawLine(PAGE_WIDTH - 200f, signY + 24f, PAGE_WIDTH - 40f, signY + 24f, signLinePaint)

        canvas.drawText("ختم واعتماد إدارة الشبكة", 120f, signY, signLabelPaint)
        canvas.drawLine(40f, signY + 24f, 200f, signY + 24f, signLinePaint)

        val footerPaint = Paint().apply { color = AndroidColor.parseColor("#64748B"); textSize = 8f; textAlign = Paint.Align.CENTER }
        canvas.drawText(
            "${identity.networkName} • تم الإصدار آلياً - هاتف الدعم: ${identity.supportPhone}",
            PAGE_WIDTH / 2f,
            PAGE_HEIGHT - 30f,
            footerPaint
        )
    }

    private fun drawKpiBoxOnCanvas(
        canvas: Canvas,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        title: String,
        amountStr: String,
        headerBgColor: String
    ) {
        val rect = RectF(left, top, left + width, top + height)
        val bgPaint = Paint().apply { color = AndroidColor.parseColor("#F8FAFC"); style = Paint.Style.FILL }
        val borderPaint = Paint().apply { color = AndroidColor.parseColor("#CBD5E1"); style = Paint.Style.STROKE; strokeWidth = 1f }
        canvas.drawRoundRect(rect, 6f, 6f, bgPaint)
        canvas.drawRoundRect(rect, 6f, 6f, borderPaint)

        // Accent top bar
        val barRect = RectF(left, top, left + width, top + 5f)
        val barPaint = Paint().apply { color = AndroidColor.parseColor(headerBgColor); style = Paint.Style.FILL }
        canvas.drawRoundRect(barRect, 6f, 6f, barPaint)

        // Title
        val titlePaint = Paint().apply { color = AndroidColor.parseColor("#475569"); textSize = 9.5f; textAlign = Paint.Align.RIGHT }
        canvas.drawText(title, left + width - 10f, top + 22f, titlePaint)

        // Amount
        val amtPaint = Paint().apply { color = AndroidColor.parseColor(headerBgColor); textSize = 13.5f; isFakeBoldText = true; textAlign = Paint.Align.RIGHT }
        canvas.drawText(amountStr, left + width - 10f, top + 42f, amtPaint)
    }

    private fun chunkText(text: String, lineLength: Int): List<String> {
        val result = mutableListOf<String>()
        val words = text.split("\\s+".toRegex())
        var currentLine = StringBuilder()

        for (w in words) {
            if (currentLine.length + w.length + 1 > lineLength) {
                result.add(currentLine.toString())
                currentLine = StringBuilder(w)
            } else {
                if (currentLine.isNotEmpty()) currentLine.append(" ")
                currentLine.append(w)
            }
        }
        if (currentLine.isNotEmpty()) {
            result.add(currentLine.toString())
        }
        return result
    }
}
