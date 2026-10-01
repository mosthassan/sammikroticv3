package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CardSalesInvoiceEntity
import com.example.data.model.CardSalesInvoiceItem
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import com.example.util.InvoicePdfManager
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * نافذة عرض الفاتورة المحاسبية الاحترافية وإيصال الاستلام
 * - عرض تفاصيل الأصناف المحسوبة بدقة
 * - دعم مشاركة الفاتورة عبر واتساب أو النسخ
 * - تدقيق وتحليل ذكي مع Gemini AI
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardSalesInvoiceDetailDialog(
    invoice: CardSalesInvoiceEntity,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onEditInvoice: ((CardSalesInvoiceEntity) -> Unit)? = null,
    onCloneInvoice: ((CardSalesInvoiceEntity) -> Unit)? = null,
    onDeleteInvoice: ((CardSalesInvoiceEntity) -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    // Parse items JSON
    val items = remember(invoice.itemsJson) {
        val list = mutableListOf<CardSalesInvoiceItem>()
        try {
            val jsonArr = JSONArray(invoice.itemsJson)
            for (i in 0 until jsonArr.length()) {
                val obj = jsonArr.getJSONObject(i)
                list.add(
                    CardSalesInvoiceItem(
                        id = obj.optString("id", "$i"),
                        packageName = obj.optString("packageName", "صنف كروت"),
                        quantity = obj.optInt("quantity", 0),
                        unitPrice = obj.optDouble("unitPrice", 0.0),
                        retailPrice = obj.optDouble("retailPrice", 0.0)
                    )
                )
            }
        } catch (e: Exception) {
            // fallback
        }
        list
    }

    var aiAnalysisText by remember { mutableStateOf<String?>(null) }
    var isAnalyzingAi by remember { mutableStateOf(false) }

    val dateFormatted = remember(invoice.invoiceDateMillis) {
        val sdf = SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale.getDefault())
        sdf.format(Date(invoice.invoiceDateMillis))
    }

    val paymentTitle = when (invoice.paymentType) {
        "CASH" -> "فاتورة نقدية (مدفوعة)"
        "CREDIT" -> "فاتورة آجلة (قيد على الحساب)"
        "PARTIAL" -> "فاتورة سداد جزئي"
        else -> invoice.paymentType
    }
    val paymentBadgeColor = when (invoice.paymentType) {
        "CASH" -> ProfitEmerald
        "CREDIT" -> Color(0xFFEF4444)
        "PARTIAL" -> StatusWarning
        else -> MikroTikPrimary
    }

    val retailers by viewModel.retailers.collectAsState()
    val linkedRetailer = remember(invoice, retailers) {
        retailers.find { it.id == invoice.retailerId || it.name.trim().equals(invoice.customerName.trim(), ignoreCase = true) }
    }
    val totalBalanceOwed = linkedRetailer?.balanceOwed?.toDouble() ?: invoice.remainingAmount.toDouble()

    // Prepare printable / shareable invoice text
    val invoiceShareText = remember(invoice, items, totalBalanceOwed) {
        com.example.util.WhatsAppHelper.generateCardSalesInvoiceMessage(invoice, totalBalanceOwed)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.96f),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CyberDarkSurface,
            border = BorderStroke(1.dp, MikroTikPrimary.copy(alpha = 0.5f)),
            modifier = Modifier.padding(10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(paymentBadgeColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Receipt, contentDescription = null, tint = paymentBadgeColor, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = invoice.invoiceNumber,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = dateFormatted,
                                fontFamily = CairoFontFamily,
                                fontSize = 10.sp,
                                color = TextSecondaryDark
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = paymentBadgeColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, paymentBadgeColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = paymentTitle,
                            fontFamily = CairoFontFamily,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = paymentBadgeColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = 500.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Customer & Issuer Info
                    item {
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
                            border = BorderStroke(1.dp, CyberBorder)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("اسم العميل:", fontFamily = CairoFontFamily, fontSize = 11.sp, color = TextSecondaryDark)
                                    Text(invoice.customerName, fontFamily = CairoFontFamily, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                if (invoice.customerPhone.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("رقم الهاتف:", fontFamily = CairoFontFamily, fontSize = 11.sp, color = TextSecondaryDark)
                                        Text(invoice.customerPhone, fontFamily = FontFamily.Monospace, fontSize = 11.5.sp, color = Color(0xFF38BDF8))
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("مُصدر الفاتورة:", fontFamily = CairoFontFamily, fontSize = 11.sp, color = TextSecondaryDark)
                                    Text(invoice.issuerName, fontFamily = CairoFontFamily, fontSize = 11.5.sp, color = TextSecondaryDark)
                                }
                            }
                        }
                    }

                    // Items Table
                    item {
                        Text("تفاصيل الأصناف والكميات المحسوبة:", fontFamily = CairoFontFamily, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(4.dp))

                        // Table Header
                        Surface(
                            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                            color = Color(0xFF0F1E33),
                            border = BorderStroke(1.dp, CyberBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("الصنف", fontFamily = CairoFontFamily, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = TextSecondaryDark, modifier = Modifier.weight(1.5f))
                                Text("العدد", fontFamily = CairoFontFamily, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = TextSecondaryDark, modifier = Modifier.weight(0.8f))
                                Text("سعر الكرت", fontFamily = CairoFontFamily, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = TextSecondaryDark, modifier = Modifier.weight(1f))
                                Text("الإجمالي", fontFamily = CairoFontFamily, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = TextSecondaryDark, modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    // Items rows
                    itemsIndexed(items) { idx, itm ->
                        Surface(
                            color = if (idx % 2 == 0) CyberDarkSurface else CyberDarkCardElevated,
                            border = BorderStroke(0.5.dp, CyberBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = itm.packageName,
                                    fontFamily = CairoFontFamily,
                                    fontSize = 11.5.sp,
                                    color = Color.White,
                                    modifier = Modifier.weight(1.5f)
                                )
                                Text(
                                    text = "${itm.quantity} كرت",
                                    fontFamily = CairoFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8),
                                    modifier = Modifier.weight(0.8f)
                                )
                                Text(
                                    text = "${itm.unitPrice.toInt()} ر.ي",
                                    fontFamily = CairoFontFamily,
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${itm.lineTotal.toInt()} ر.ي",
                                    fontFamily = CairoFontFamily,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ProfitEmerald,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Grand Totals Summary
                    item {
                        Surface(
                            shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp),
                            color = Color(0xFF0F1E33),
                            border = BorderStroke(1.dp, MikroTikPrimary.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("إجمالي عدد الكروت:", fontFamily = CairoFontFamily, fontSize = 11.sp, color = TextSecondaryDark)
                                    Text("${invoice.totalCardsCount} كرت", fontFamily = CairoFontFamily, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("إجمالي الفاتورة:", fontFamily = CairoFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("${invoice.totalAmount.toInt()} ريال", fontFamily = CairoFontFamily, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Divider(modifier = Modifier.padding(vertical = 4.dp), color = CyberBorder)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("المبلغ المدفوع:", fontFamily = CairoFontFamily, fontSize = 11.sp, color = TextSecondaryDark)
                                    Text("${invoice.paidAmount.toInt()} ريال", fontFamily = CairoFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ProfitEmerald)
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("المبلغ المتبقي (آجل):", fontFamily = CairoFontFamily, fontSize = 11.sp, color = TextSecondaryDark)
                                    Text("${invoice.remainingAmount.toInt()} ريال", fontFamily = CairoFontFamily, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = if (invoice.remainingAmount > java.math.BigDecimal.ZERO) Color(0xFFEF4444) else ProfitEmerald)
                                }
                            }
                        }
                    }

                    // Notes
                    if (invoice.notes.isNotBlank()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = CyberDarkCardElevated,
                                border = BorderStroke(1.dp, CyberBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Notes, contentDescription = null, tint = TextSecondaryDark, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = invoice.notes,
                                        fontFamily = CairoFontFamily,
                                        fontSize = 11.sp,
                                        color = Color(0xFFCBD5E1)
                                    )
                                }
                            }
                        }
                    }

                    // Gemini AI Analysis Section
                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E1B4B).copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, Color(0xFF818CF8).copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFA5B4FC), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("التحليل الذكي (Gemini AI):", fontFamily = CairoFontFamily, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA5B4FC))
                                    }

                                    if (aiAnalysisText == null && !isAnalyzingAi) {
                                        Button(
                                            onClick = {
                                                isAnalyzingAi = true
                                                viewModel.analyzeInvoiceWithAi(invoice, items) { result ->
                                                    isAnalyzingAi = false
                                                    aiAnalysisText = result
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text("تحليل الفاتورة الآن", fontFamily = CairoFontFamily, fontSize = 10.sp, color = Color.White)
                                        }
                                    }
                                }

                                if (isAnalyzingAi) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color(0xFFA5B4FC))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("جاري استنتاج هوامش الربح وتحليل دوران المخزون...", fontFamily = CairoFontFamily, fontSize = 10.sp, color = Color(0xFFCBD5E1))
                                    }
                                } else if (aiAnalysisText != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = aiAnalysisText ?: "",
                                        fontFamily = CairoFontFamily,
                                        fontSize = 11.sp,
                                        color = Color(0xFFE0E7FF),
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Actions: Clone, Delete, Share PDF, Print PDF, Quick WhatsApp Text, Copy, Close
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Row 0: Edit, Clone & Delete Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (onEditInvoice != null) {
                            Button(
                                onClick = {
                                    onDismiss()
                                    onEditInvoice(invoice)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تعديل الفاتورة", fontFamily = CairoFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        if (onCloneInvoice != null) {
                            Button(
                                onClick = {
                                    onDismiss()
                                    onCloneInvoice(invoice)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F2942)),
                                border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.CopyAll, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF38BDF8))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("استنساخ", fontFamily = CairoFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                            }
                        }

                        if (onDeleteInvoice != null) {
                            OutlinedButton(
                                onClick = {
                                    onDismiss()
                                    onDeleteInvoice(invoice)
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.7f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                                modifier = Modifier.weight(0.6f)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFEF4444))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("حذف", fontFamily = CairoFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                            }
                        }
                    }

                    // Row 1: Primary PDF Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Official PDF Document Share
                        Button(
                            onClick = {
                                InvoicePdfManager.shareInvoicePdf(context, invoice, items)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ProfitEmerald),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("مشاركة كـ PDF", fontFamily = CairoFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }

                        // Print / Save to Device via Android Print Manager
                        OutlinedButton(
                            onClick = {
                                InvoicePdfManager.printInvoice(context, invoice, items)
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF38BDF8))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("طباعة / حفظ", fontFamily = CairoFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                        }
                    }

                    // Row 2: Secondary Options (Open PDF, WhatsApp Text, Copy, Close)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Open PDF viewer
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CyberDarkCardElevated,
                            border = BorderStroke(1.dp, CyberBorder),
                            onClick = {
                                InvoicePdfManager.openInvoicePdf(context, invoice, items)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextSecondaryDark)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("معاينة", fontFamily = CairoFontFamily, fontSize = 11.sp, color = Color.White)
                            }
                        }

                        // Share as plain text
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CyberDarkCardElevated,
                            border = BorderStroke(1.dp, CyberBorder),
                            onClick = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, invoiceShareText)
                                }
                                context.startActivity(Intent.createChooser(intent, "مشاركة تفاصيل الفاتورة كنص"))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(14.dp), tint = ProfitEmerald)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("نص واتساب", fontFamily = CairoFontFamily, fontSize = 11.sp, color = Color.White)
                            }
                        }

                        // Copy to clipboard
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CyberDarkCardElevated,
                            border = BorderStroke(1.dp, CyberBorder),
                            onClick = {
                                clipboard.setText(AnnotatedString(invoiceShareText))
                                Toast.makeText(context, "تم نسخ تفاصيل الفاتورة للحافظة ✓", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(0.8f)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(13.dp), tint = TextSecondaryDark)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("نسخ", fontFamily = CairoFontFamily, fontSize = 11.sp, color = Color.White)
                            }
                        }

                        // Dismiss
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = CyberDarkCardElevated),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(0.8f)
                        ) {
                            Text("إغلاق", fontFamily = CairoFontFamily, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
