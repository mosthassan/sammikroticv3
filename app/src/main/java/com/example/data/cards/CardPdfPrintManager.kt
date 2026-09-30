package com.example.data.cards

import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import com.example.data.local.entity.CardEntity
import java.io.FileOutputStream

object CardPdfPrintManager {

    /**
     * Prints or exports cards to a standard A4 sheet with 24 cards (3 columns x 8 rows)
     * using the Android Print framework (supports WiFi printers, Save as PDF, Mopria).
     */
    fun printCardsSheet(
        context: Context,
        cards: List<CardEntity>,
        templateConfig: CardTemplateConfig,
        jobName: String = "كروت_سام_ميكروتك"
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val printAdapter = object : PrintDocumentAdapter() {
            private var pdfDocument: PdfDocument? = null

            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: android.os.Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }

                val totalCards = cards.size
                val cardsPerPage = 24
                val totalPages = kotlin.math.max(1, (totalCards + cardsPerPage - 1) / cardsPerPage)

                val info = PrintDocumentInfo.Builder("$jobName.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(totalPages)
                    .build()

                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback
            ) {
                pdfDocument = PdfDocument()
                val pageWidth = 595 // Standard A4 points at 72dpi
                val pageHeight = 842
                val cols = 3
                val rows = 8
                val cardsPerPage = cols * rows
                val totalPages = kotlin.math.max(1, (cards.size + cardsPerPage - 1) / cardsPerPage)

                val marginX = 20f
                val marginY = 24f
                val cardW = (pageWidth - (marginX * 2)) / cols
                val cardH = (pageHeight - (marginY * 2)) / rows

                for (pageIndex in 0 until totalPages) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback.onWriteCancelled()
                        pdfDocument?.close()
                        pdfDocument = null
                        return
                    }

                    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
                    val page = pdfDocument!!.startPage(pageInfo)
                    val canvas = page.canvas

                    val startIndex = pageIndex * cardsPerPage
                    val pageCards = cards.drop(startIndex).take(cardsPerPage)

                    for ((idx, card) in pageCards.withIndex()) {
                        val col = idx % cols
                        val row = idx / cols
                        val left = marginX + (col * cardW)
                        val top = marginY + (row * cardH)
                        val right = left + cardW - 4f
                        val bottom = top + cardH - 4f

                        // Draw Card Background
                        val bgPaint = Paint().apply {
                            color = if (templateConfig.theme.isLightInkSaver) AndroidColor.WHITE else AndroidColor.parseColor("#0F172A")
                            style = Paint.Style.FILL
                        }
                        canvas.drawRoundRect(RectF(left, top, right, bottom), 6f, 6f, bgPaint)

                        // Draw Border
                        val borderPaint = Paint().apply {
                            color = if (templateConfig.theme.isLightInkSaver) AndroidColor.LTGRAY else AndroidColor.parseColor("#38BDF8")
                            style = Paint.Style.STROKE
                            strokeWidth = 0.8f
                        }
                        canvas.drawRoundRect(RectF(left, top, right, bottom), 6f, 6f, borderPaint)

                        // Header Network Name
                        val titlePaint = Paint().apply {
                            color = if (templateConfig.theme.isLightInkSaver) AndroidColor.BLACK else AndroidColor.WHITE
                            textSize = 8.5f
                            isFakeBoldText = true
                        }
                        canvas.drawText(templateConfig.networkTitle.take(18), left + 8f, top + 14f, titlePaint)

                        // Price Badge
                        val pricePaint = Paint().apply {
                            color = if (templateConfig.theme.isLightInkSaver) AndroidColor.DKGRAY else AndroidColor.parseColor("#F59E0B")
                            textSize = 8.5f
                            isFakeBoldText = true
                        }
                        canvas.drawText("${card.retailPrice.toInt()} YER", right - 40f, top + 14f, pricePaint)

                        // Category & Details
                        val subPaint = Paint().apply {
                            color = if (templateConfig.theme.isLightInkSaver) AndroidColor.GRAY else AndroidColor.LTGRAY
                            textSize = 7f
                        }
                        canvas.drawText(card.categoryName.take(24), left + 8f, top + 26f, subPaint)

                        // Code Box
                        val codeBoxPaint = Paint().apply {
                            color = if (templateConfig.theme.isLightInkSaver) AndroidColor.parseColor("#F1F5F9") else AndroidColor.parseColor("#1E293B")
                            style = Paint.Style.FILL
                        }
                        canvas.drawRoundRect(RectF(left + 8f, top + 34f, right - 44f, top + 56f), 4f, 4f, codeBoxPaint)

                        val codeTextPaint = Paint().apply {
                            color = if (templateConfig.theme.isLightInkSaver) AndroidColor.BLACK else AndroidColor.parseColor("#38BDF8")
                            textSize = 10f
                            isFakeBoldText = true
                        }
                        canvas.drawText("رمز: ${card.username}", left + 14f, top + 49f, codeTextPaint)

                        // PIN if different
                        if (card.password.isNotBlank() && card.password != card.username) {
                            val pinPaint = Paint().apply {
                                color = if (templateConfig.theme.isLightInkSaver) AndroidColor.DKGRAY else AndroidColor.YELLOW
                                textSize = 7.5f
                            }
                            canvas.drawText("PIN: ${card.password}", left + 14f, bottom - 10f, pinPaint)
                        } else {
                            val hintPaint = Paint().apply {
                                color = AndroidColor.GRAY
                                textSize = 6.5f
                            }
                            canvas.drawText(templateConfig.loginDomain, left + 14f, bottom - 10f, hintPaint)
                        }

                        // Generate and draw QR Code
                        val autoUrl = CardGenerationEngine.buildAutoLoginUrl(templateConfig.loginDomain, card.username, card.password)
                        val qrBitmap = QrCodeGenerator.generateQrBitmap(
                            content = autoUrl,
                            sizePx = 80,
                            darkColor = if (templateConfig.theme.isLightInkSaver) AndroidColor.BLACK else AndroidColor.parseColor("#0F172A"),
                            lightColor = AndroidColor.WHITE
                        )
                        if (qrBitmap != null) {
                            val qrRect = RectF(right - 40f, top + 32f, right - 6f, top + 66f)
                            canvas.drawBitmap(qrBitmap, null, qrRect, null)
                        }
                    }

                    pdfDocument!!.finishPage(page)
                }

                try {
                    val outputStream = FileOutputStream(destination.fileDescriptor)
                    pdfDocument?.writeTo(outputStream)
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message)
                } finally {
                    pdfDocument?.close()
                    pdfDocument = null
                }
            }
        }

        printManager.print(jobName, printAdapter, PrintAttributes.Builder().apply {
            setMediaSize(PrintAttributes.MediaSize.ISO_A4)
        }.build())
    }
}
