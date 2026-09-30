package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.entity.CardSalesInvoiceEntity
import com.example.data.model.CardSalesInvoiceItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * مدير توليد ومشاركة فواتير المبيعات كملفات PDF رسمية احترافية
 * - يولّد ملف PDF بدقة A4 ومظهر محاسبي فاخر متوافق مع شبكة طلقة نت
 * - يشارك ملف الـ PDF مباشرة مع تطبيقات المراسلة (واتساب، تيليجرام، إيميل)
 * - يدعم الطباعة اللاسلكية المباشرة وحفظ الـ PDF في الهاتف
 */
object InvoicePdfManager {

    private const val PAGE_WIDTH = 595 // Standard A4 in points at 72dpi
    private const val PAGE_HEIGHT = 842

    /**
     * توليد ملف PDF للفاتورة وحفظه في ذاكرة التخزين المؤقت
     */
    fun generateInvoicePdfFile(
        context: Context,
        invoice: CardSalesInvoiceEntity,
        items: List<CardSalesInvoiceItem>
    ): File {
        val invoicesDir = File(context.cacheDir, "invoices").apply {
            if (!exists()) mkdirs()
        }
        val safeInvoiceNo = invoice.invoiceNumber.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val file = File(invoicesDir, "فاتورة_${safeInvoiceNo}.pdf")

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        drawInvoiceOnCanvas(canvas, invoice, items)

        document.finishPage(page)
        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()

        return file
    }

    /**
     * مشاركة الفاتورة كملف PDF رسمي
     */
    fun shareInvoicePdf(
        context: Context,
        invoice: CardSalesInvoiceEntity,
        items: List<CardSalesInvoiceItem>
    ) {
        try {
            val pdfFile = generateInvoicePdfFile(context, invoice, items)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "فاتورة مبيعات ${invoice.invoiceNumber} - ${invoice.customerName}")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "مرفق فاتورة مبيعات كروت شبكة برقم ${invoice.invoiceNumber} الخاصة بالعميل: ${invoice.customerName}"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "مشاركة الفاتورة كملف PDF")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "خطأ في مشاركة ملف PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * فتح أو معاينة ملف الـ PDF مباشرة في قارئ المستندات بالهاتف
     */
    fun openInvoicePdf(
        context: Context,
        invoice: CardSalesInvoiceEntity,
        items: List<CardSalesInvoiceItem>
    ) {
        try {
            val pdfFile = generateInvoicePdfFile(context, invoice, items)
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

            val chooser = Intent.createChooser(viewIntent, "فتح الفاتورة")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح المستند: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * طباعة الفاتورة أو حفظها كـ PDF عبر نظام الطباعة الرسمي في أندرويد
     */
    fun printInvoice(
        context: Context,
        invoice: CardSalesInvoiceEntity,
        items: List<CardSalesInvoiceItem>
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val jobName = "فاتورة_${invoice.invoiceNumber}"

        val printAdapter = object : PrintDocumentAdapter() {
            private var pdfDoc: PdfDocument? = null

            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }

                val info = PrintDocumentInfo.Builder("$jobName.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback
            ) {
                pdfDoc = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
                val page = pdfDoc!!.startPage(pageInfo)

                drawInvoiceOnCanvas(page.canvas, invoice, items)

                pdfDoc!!.finishPage(page)

                try {
                    FileOutputStream(destination.fileDescriptor).use { out ->
                        pdfDoc!!.writeTo(out)
                    }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message)
                } finally {
                    pdfDoc?.close()
                    pdfDoc = null
                }
            }
        }

        printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
    }

    /**
     * رسم محتوى الفاتورة المحاسبية الاحترافية على Canvas الـ PDF
     */
    private fun drawInvoiceOnCanvas(
        canvas: Canvas,
        invoice: CardSalesInvoiceEntity,
        items: List<CardSalesInvoiceItem>
    ) {
        // 1. Background (Pure White for printing)
        val bgPaint = Paint().apply {
            color = AndroidColor.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), bgPaint)

        // 2. Outer decorative border
        val framePaint = Paint().apply {
            color = AndroidColor.parseColor("#CBD5E1")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(RectF(18f, 18f, PAGE_WIDTH - 18f, PAGE_HEIGHT - 18f), 8f, 8f, framePaint)

        // 3. Top Header Banner (Corporate Deep Blue)
        val headerPaint = Paint().apply {
            color = AndroidColor.parseColor("#0A192F")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(18f, 18f, PAGE_WIDTH - 18f, 95f), 8f, 8f, headerPaint)
        // Fix bottom corners of banner
        canvas.drawRect(18f, 80f, PAGE_WIDTH - 18f, 95f, headerPaint)

        // Accent Cyan Line
        val accentLinePaint = Paint().apply {
            color = AndroidColor.parseColor("#0284C7")
            style = Paint.Style.FILL
        }
        canvas.drawRect(18f, 95f, PAGE_WIDTH - 18f, 98f, accentLinePaint)

        // Network Brand Title (Right-to-Left Arabic)
        val brandPaint = Paint().apply {
            color = AndroidColor.WHITE
            textSize = 17f
            isFakeBoldText = true
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("شبكة طلقة نت للإنترنت اللاسلكي", PAGE_WIDTH - 36f, 48f, brandPaint)

        val brandSubPaint = Paint().apply {
            color = AndroidColor.parseColor("#94A3B8")
            textSize = 9.5f
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("نظام إدارة مبيعات وتوزيع الكروت الميكروتك الذكي", PAGE_WIDTH - 36f, 65f, brandSubPaint)

        // Left Header: Invoice Title Badge
        val docTitlePaint = Paint().apply {
            color = AndroidColor.parseColor("#38BDF8")
            textSize = 14f
            isFakeBoldText = true
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText("فاتورة مبيعات كروت", 36f, 48f, docTitlePaint)

        val docSubtitlePaint = Paint().apply {
            color = AndroidColor.parseColor("#F59E0B")
            textSize = 9.5f
            isFakeBoldText = true
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText("TAX / SALES INVOICE", 36f, 65f, docSubtitlePaint)

        // 4. Meta Information Box
        val metaBoxPaint = Paint().apply {
            color = AndroidColor.parseColor("#F8FAFC")
            style = Paint.Style.FILL
        }
        val metaBorderPaint = Paint().apply {
            color = AndroidColor.parseColor("#E2E8F0")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val metaRect = RectF(30f, 110f, PAGE_WIDTH - 30f, 185f)
        canvas.drawRoundRect(metaRect, 6f, 6f, metaBoxPaint)
        canvas.drawRoundRect(metaRect, 6f, 6f, metaBorderPaint)

        val metaLabelPaint = Paint().apply {
            color = AndroidColor.parseColor("#64748B")
            textSize = 9f
            textAlign = Paint.Align.RIGHT
        }
        val metaValPaint = Paint().apply {
            color = AndroidColor.parseColor("#0F172A")
            textSize = 10f
            isFakeBoldText = true
            textAlign = Paint.Align.RIGHT
        }

        val dateFormatted = try {
            val sdf = SimpleDateFormat("yyyy/MM/dd  HH:mm", Locale.getDefault())
            sdf.format(Date(invoice.invoiceDateMillis))
        } catch (e: Exception) {
            "2026-09-20"
        }

        val paymentTitle = when (invoice.paymentType) {
            "CASH" -> "نقداً (مدفوعة بالكامل)"
            "CREDIT" -> "آجل (قيد على الحساب)"
            "PARTIAL" -> "سداد جزئي (مقدم ومتبقي)"
            else -> invoice.paymentType
        }

        // Right column: Invoice No, Customer, Phone
        canvas.drawText("رقم الفاتورة:  ${invoice.invoiceNumber}", PAGE_WIDTH - 42f, 130f, metaValPaint)
        canvas.drawText("العميل / البقالة:  ${invoice.customerName}", PAGE_WIDTH - 42f, 150f, metaValPaint)
        if (invoice.customerPhone.isNotBlank()) {
            canvas.drawText("هاتف العميل:  ${invoice.customerPhone}", PAGE_WIDTH - 42f, 170f, metaValPaint)
        } else {
            canvas.drawText("نوع العميل:  موزع معتمد", PAGE_WIDTH - 42f, 170f, metaValPaint)
        }

        // Left column: Date, Payment Type, Issuer
        val leftMetaValPaint = Paint(metaValPaint).apply { textAlign = Paint.Align.LEFT }
        canvas.drawText("التاريخ:  $dateFormatted", 42f, 130f, leftMetaValPaint)
        canvas.drawText("طريقة السداد:  $paymentTitle", 42f, 150f, leftMetaValPaint)
        canvas.drawText("المسؤول:  ${invoice.issuerName}", 42f, 170f, leftMetaValPaint)

        // 5. Items Table Header
        var currentY = 205f
        val tableHeaderPaint = Paint().apply {
            color = AndroidColor.parseColor("#0F2744")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(30f, currentY, PAGE_WIDTH - 30f, currentY + 24f), 4f, 4f, tableHeaderPaint)

        val thTextPaint = Paint().apply {
            color = AndroidColor.WHITE
            textSize = 9.5f
            isFakeBoldText = true
        }

        // Columns positions:
        // [م] (right: 48) | [اسم الصنف / الباقة] (right: 110-300) | [الكمية] (380) | [سعر الكرت] (460) | [الإجمالي] (540)
        thTextPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("م", PAGE_WIDTH - 46f, currentY + 16f, thTextPaint)

        thTextPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("اسم الصنف / الباقة", PAGE_WIDTH - 80f, currentY + 16f, thTextPaint)

        thTextPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("الكمية", PAGE_WIDTH - 250f, currentY + 16f, thTextPaint)
        canvas.drawText("سعر الجملة", PAGE_WIDTH - 350f, currentY + 16f, thTextPaint)

        thTextPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("الإجمالي (ر.ي)", 45f, currentY + 16f, thTextPaint)

        currentY += 24f

        // Table Rows
        val rowBgEven = Paint().apply { color = AndroidColor.WHITE; style = Paint.Style.FILL }
        val rowBgOdd = Paint().apply { color = AndroidColor.parseColor("#F8FAFC"); style = Paint.Style.FILL }
        val rowBorder = Paint().apply { color = AndroidColor.parseColor("#E2E8F0"); style = Paint.Style.STROKE; strokeWidth = 0.6f }

        val tdText = Paint().apply {
            color = AndroidColor.parseColor("#1E293B")
            textSize = 9.5f
        }
        val tdBold = Paint().apply {
            color = AndroidColor.parseColor("#0F172A")
            textSize = 9.5f
            isFakeBoldText = true
        }

        val rowHeight = 22f

        for ((idx, itm) in items.withIndex()) {
            val rowRect = RectF(30f, currentY, PAGE_WIDTH - 30f, currentY + rowHeight)
            canvas.drawRect(rowRect, if (idx % 2 == 0) rowBgEven else rowBgOdd)
            canvas.drawRect(rowRect, rowBorder)

            // Index
            tdText.textAlign = Paint.Align.CENTER
            canvas.drawText("${idx + 1}", PAGE_WIDTH - 46f, currentY + 15f, tdText)

            // Package Name
            tdBold.textAlign = Paint.Align.RIGHT
            canvas.drawText(itm.packageName.take(30), PAGE_WIDTH - 80f, currentY + 15f, tdBold)

            // Quantity
            tdBold.textAlign = Paint.Align.CENTER
            canvas.drawText("${itm.quantity} كرت", PAGE_WIDTH - 250f, currentY + 15f, tdBold)

            // Unit Price
            tdText.textAlign = Paint.Align.CENTER
            canvas.drawText("${itm.unitPrice.toInt()} ر.ي", PAGE_WIDTH - 350f, currentY + 15f, tdText)

            // Line Total
            tdBold.textAlign = Paint.Align.LEFT
            canvas.drawText("${itm.lineTotal.toInt()} ر.ي", 45f, currentY + 15f, tdBold)

            currentY += rowHeight
        }

        currentY += 14f

        // 6. Financial Summary Card (Right side or Full Width)
        val summaryRect = RectF(PAGE_WIDTH - 280f, currentY, PAGE_WIDTH - 30f, currentY + 115f)
        val summaryBg = Paint().apply {
            color = AndroidColor.parseColor("#F1F5F9")
            style = Paint.Style.FILL
        }
        val summaryBorder = Paint().apply {
            color = AndroidColor.parseColor("#CBD5E1")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(summaryRect, 6f, 6f, summaryBg)
        canvas.drawRoundRect(summaryRect, 6f, 6f, summaryBorder)

        val sLabelPaint = Paint().apply {
            color = AndroidColor.parseColor("#475569")
            textSize = 9.5f
            textAlign = Paint.Align.RIGHT
        }
        val sValPaint = Paint().apply {
            color = AndroidColor.parseColor("#0F172A")
            textSize = 10.5f
            isFakeBoldText = true
            textAlign = Paint.Align.LEFT
        }

        // Summary Lines:
        // Total Cards
        canvas.drawText("إجمالي عدد الكروت:", PAGE_WIDTH - 42f, currentY + 22f, sLabelPaint)
        canvas.drawText("${invoice.totalCardsCount} كرت", PAGE_WIDTH - 268f, currentY + 22f, sValPaint)

        // Divider
        val sDiv = Paint().apply { color = AndroidColor.parseColor("#CBD5E1"); strokeWidth = 0.8f }
        canvas.drawLine(PAGE_WIDTH - 275f, currentY + 30f, PAGE_WIDTH - 35f, currentY + 30f, sDiv)

        // Grand Total
        val grandLabel = Paint(sLabelPaint).apply { color = AndroidColor.parseColor("#0284C7"); isFakeBoldText = true; textSize = 10.5f }
        val grandVal = Paint(sValPaint).apply { color = AndroidColor.parseColor("#0284C7"); isFakeBoldText = true; textSize = 11.5f }
        canvas.drawText("إجمالي الفاتورة:", PAGE_WIDTH - 42f, currentY + 48f, grandLabel)
        canvas.drawText("${invoice.totalAmount.toInt()} ريال يمني", PAGE_WIDTH - 268f, currentY + 48f, grandVal)

        canvas.drawLine(PAGE_WIDTH - 275f, currentY + 56f, PAGE_WIDTH - 35f, currentY + 56f, sDiv)

        // Paid
        val paidVal = Paint(sValPaint).apply { color = AndroidColor.parseColor("#16A34A") }
        canvas.drawText("المبلغ المدفوع (نقداً):", PAGE_WIDTH - 42f, currentY + 74f, sLabelPaint)
        canvas.drawText("${invoice.paidAmount.toInt()} ريال", PAGE_WIDTH - 268f, currentY + 74f, paidVal)

        // Remaining
        val remColor = if (invoice.remainingAmount > java.math.BigDecimal.ZERO) AndroidColor.parseColor("#DC2626") else AndroidColor.parseColor("#16A34A")
        val remVal = Paint(sValPaint).apply { color = remColor }
        canvas.drawText("المتبقي (آجل):", PAGE_WIDTH - 42f, currentY + 98f, sLabelPaint)
        canvas.drawText("${invoice.remainingAmount.toInt()} ريال", PAGE_WIDTH - 268f, currentY + 98f, remVal)

        // Left side: Notes Box
        val notesRect = RectF(30f, currentY, PAGE_WIDTH - 295f, currentY + 115f)
        val notesBg = Paint().apply {
            color = AndroidColor.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(notesRect, 6f, 6f, notesBg)
        canvas.drawRoundRect(notesRect, 6f, 6f, summaryBorder)

        val notesTitlePaint = Paint().apply {
            color = AndroidColor.parseColor("#334155")
            textSize = 9.5f
            isFakeBoldText = true
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("ملاحظات وشروط الفاتورة:", PAGE_WIDTH - 305f, currentY + 22f, notesTitlePaint)

        val notesBodyPaint = Paint().apply {
            color = AndroidColor.parseColor("#64748B")
            textSize = 8.5f
            textAlign = Paint.Align.RIGHT
        }
        val noteContent = if (invoice.notes.isNotBlank()) invoice.notes else "تم خصم الكميات من المخزن المركزي تلقائياً وتحديث حساب العميل."
        canvas.drawText(noteContent.take(45), PAGE_WIDTH - 305f, currentY + 42f, notesBodyPaint)
        canvas.drawText("الكروت المباعة مسؤولية الموزع، يُرجى فحص الكميات.", PAGE_WIDTH - 305f, currentY + 60f, notesBodyPaint)

        // Status Badge in Notes
        val statusBgPaint = Paint().apply {
            color = if (invoice.remainingAmount <= java.math.BigDecimal.ZERO) AndroidColor.parseColor("#DCFCE7") else AndroidColor.parseColor("#FEE2E2")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(40f, currentY + 80f, PAGE_WIDTH - 305f, currentY + 104f), 4f, 4f, statusBgPaint)

        val statusTextPaint = Paint().apply {
            color = if (invoice.remainingAmount <= java.math.BigDecimal.ZERO) AndroidColor.parseColor("#15803D") else AndroidColor.parseColor("#B91C1C")
            textSize = 9f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val statusBadgeText = if (invoice.remainingAmount <= java.math.BigDecimal.ZERO) "خالصة الحساب بالكامل ✓" else "متبقي مديونية: ${invoice.remainingAmount.toInt()} ريال"
        val badgeCenterX = (40f + (PAGE_WIDTH - 305f)) / 2f
        canvas.drawText(statusBadgeText, badgeCenterX, currentY + 96f, statusTextPaint)

        // 7. Signatures section
        val signY = PAGE_HEIGHT - 120f
        val signLabelPaint = Paint().apply {
            color = AndroidColor.parseColor("#475569")
            textSize = 9f
            textAlign = Paint.Align.CENTER
        }
        val signLinePaint = Paint().apply {
            color = AndroidColor.parseColor("#94A3B8")
            strokeWidth = 0.8f
        }

        // Issuer signature (Right)
        canvas.drawText("توقيع وختم مصدر الفاتورة", PAGE_WIDTH - 120f, signY, signLabelPaint)
        canvas.drawLine(PAGE_WIDTH - 200f, signY + 30f, PAGE_WIDTH - 40f, signY + 30f, signLinePaint)

        // Customer signature (Left)
        canvas.drawText("توقيع وموافقة العميل / المستلم", 120f, signY, signLabelPaint)
        canvas.drawLine(40f, signY + 30f, 200f, signY + 30f, signLinePaint)

        // 8. Footer Contacts & Brand
        val footerPaint = Paint().apply {
            color = AndroidColor.parseColor("#64748B")
            textSize = 8.5f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "شكراً لتعاملكم معنا  •  شبكة طلقة نت  •  خدمة العملاء: 771234567  •  تم الإصدار آلياً بواسطة تطبيق سام ميكروتك",
            PAGE_WIDTH / 2f,
            PAGE_HEIGHT - 35f,
            footerPaint
        )
    }
}
