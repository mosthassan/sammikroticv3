package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.PurchaseInvoiceEntity
import com.example.data.model.ParsedInvoiceData
import com.example.ui.MainViewModel
import com.example.ui.theme.AssetPurple
import com.example.ui.theme.CairoFontFamily
import com.example.ui.theme.InvestmentGold
import com.example.ui.theme.MikroTikCyan
import com.example.ui.theme.MikroTikDarkBg
import com.example.ui.theme.MikroTikDarkSurface
import com.example.ui.theme.MikroTikNavy
import com.example.ui.theme.MikroTikNavyLight
import com.example.ui.theme.MikroTikPrimary
import com.example.ui.theme.PaymentRed
import com.example.ui.theme.ProfitEmerald
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PurchaseInvoicesSubScreen(
    viewModel: MainViewModel,
    onOpenScanDialog: (targetType: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val invoices by viewModel.invoices.collectAsState()
    val totalInvoicesAmount by viewModel.totalInvoicesAmount.collectAsState()
    val context = LocalContext.current

    var selectedFilter by remember { mutableStateOf("الكل") }
    var invoiceToDelete by remember { mutableStateOf<PurchaseInvoiceEntity?>(null) }
    var invoiceToEdit by remember { mutableStateOf<PurchaseInvoiceEntity?>(null) }
    var showJsonImportDialog by remember { mutableStateOf(false) }
    var directParsedInvoice by remember { mutableStateOf<ParsedInvoiceData?>(null) }

    val filterOptions = listOf("الكل", "أصول ثابتة (CAPEX)", "مصروفات مشتريات (OPEX)")

    val filteredInvoices = invoices.filter { inv ->
        when (selectedFilter) {
            "أصول ثابتة (CAPEX)" -> inv.targetType == "ASSETS"
            "مصروفات مشتريات (OPEX)" -> inv.targetType == "EXPENSES"
            else -> true
        }
    }

    Box(modifier = modifier.fillMaxSize().testTag("purchase_invoices_subscreen")) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ... [content remains identical] ...
            item {
                // Hero Card
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().testTag("purchase_invoices_subscreen")) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero Banner Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MikroTikNavy),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = MikroTikCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "فواتير المشتريات والأصول الذكية (AI)",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                                Text(
                                    text = "تحويل وتوثيق فواتير المشتريات والأصول بالرؤية الحاسوبية",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MikroTikCyan.copy(alpha = 0.2f))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "${invoices.size} فاتورة معتمدة",
                                    color = MikroTikCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("إجمالي قيمة الفواتير", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${formatMoney(totalInvoicesAmount)} ر.ي",
                                        color = ProfitEmerald,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp
                                    )
                                }
                            }

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("الأصول الناتجة عن الفواتير", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${invoices.count { it.targetType == "ASSETS" }} فاتورة أصول",
                                        color = AssetPurple,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Buttons: Scan Invoice & Import from JSON
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onOpenScanDialog("ASSETS") },
                                colors = ButtonDefaults.buttonColors(containerColor = MikroTikPrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("scan_invoice_hero_button")
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تصوير ومسح فاتورة جديدة بالذكاء الاصطناعي 📸",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    fontFamily = CairoFontFamily
                                )
                            }

                            Button(
                                onClick = { showJsonImportDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = ProfitEmerald),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("import_json_invoice_button")
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "استيراد فاتورة من ملف JSON (بدون توكن) 📄 ⚡",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    fontFamily = CairoFontFamily
                                )
                            }
                        }
                    }
                }
            }

            // Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filterOptions) { opt ->
                        val isSelected = selectedFilter == opt
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isSelected) MikroTikPrimary else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { selectedFilter = opt }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = opt,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Invoices List
            if (filteredInvoices.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = MikroTikDarkSurface),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(MikroTikNavyLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = MikroTikCyan,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "لا توجد فواتير مشتريات معتمدة في هذا التصنيف",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "يمكنك تصوير أي فاتورة ورقية لمشتريات الشبكة أو الأجهزة ليقوم الذكاء الاصطناعي بتحويلها لجدول أصناف معتمد فوراً.",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onOpenScanDialog("ASSETS") },
                                    colors = ButtonDefaults.buttonColors(containerColor = MikroTikPrimary),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("تصوير أول فاتورة", fontSize = 11.5.sp, fontFamily = CairoFontFamily)
                                }

                                Button(
                                    onClick = { showJsonImportDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = ProfitEmerald),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Description, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("استيراد JSON ⚡", fontSize = 11.5.sp, color = Color.White, fontFamily = CairoFontFamily)
                                }
                            }
                        }
                    }
                }
            } else {
                items(filteredInvoices, key = { it.id }) { invoice ->
                    PurchaseInvoiceCard(
                        invoice = invoice,
                        onEdit = { invoiceToEdit = invoice },
                        onDelete = { invoiceToDelete = invoice },
                        onShare = {
                            shareInvoiceSummary(context, invoice)
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    // Delete Confirmation Dialog
    if (invoiceToDelete != null) {
        AlertDialog(
            onDismissRequest = { invoiceToDelete = null },
            title = { Text("حذف الفاتورة", fontWeight = FontWeight.Bold) },
            text = {
                Text("هل أنت متأكد من حذف فاتورة رقم (${invoiceToDelete?.invoiceNumber}) الصادرة من (${invoiceToDelete?.supplierName})؟ لن يتم حذف الأصول أو السندات التي تم اعتمادها سابقاً.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        invoiceToDelete?.let { inv ->
                            viewModel.deleteInvoice(inv) {
                                Toast.makeText(context, "تم حذف الفاتورة", Toast.LENGTH_SHORT).show()
                            }
                        }
                        invoiceToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PaymentRed)
                ) {
                    Text("نعم، حذف", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { invoiceToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Edit Invoice Dialog
    if (invoiceToEdit != null) {
        SmartInvoiceScannerDialog(
            viewModel = viewModel,
            initialTargetType = invoiceToEdit!!.targetType,
            invoiceToEdit = invoiceToEdit,
            onDismissRequest = { invoiceToEdit = null }
        )
    }

    if (directParsedInvoice != null) {
        SmartInvoiceScannerDialog(
            viewModel = viewModel,
            initialTargetType = directParsedInvoice!!.invoiceType,
            initialParsedData = directParsedInvoice,
            onDismissRequest = { directParsedInvoice = null }
        )
    }

    if (showJsonImportDialog) {
        JsonInvoiceImportDialog(
            viewModel = viewModel,
            onDismissRequest = { showJsonImportDialog = false },
            onInvoiceImported = { data ->
                directParsedInvoice = data
                showJsonImportDialog = false
            }
        )
    }
}

@Composable
fun PurchaseInvoiceCard(
    invoice: PurchaseInvoiceEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val isAsset = invoice.targetType == "ASSETS"
    val dateStr = SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH).format(Date(invoice.invoiceDateMillis))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("invoice_card_${invoice.id}"),
        colors = CardDefaults.cardColors(containerColor = MikroTikDarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isAsset) AssetPurple.copy(alpha = 0.2f) else PaymentRed.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isAsset) Icons.Default.Devices else Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = if (isAsset) AssetPurple else PaymentRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = invoice.supplierName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Text(
                            text = "فاتورة #${invoice.invoiceNumber} | $dateStr",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                // Amount Badge
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${formatMoney(invoice.totalAmount)} ${com.example.util.CurrencyHelper.getCurrencySymbol(invoice.currency)}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = ProfitEmerald
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isAsset) AssetPurple.copy(alpha = 0.2f) else PaymentRed.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isAsset) "أصول رأسمالية (CAPEX)" else "مصروفات مشتريات",
                            fontSize = 9.sp,
                            color = if (isAsset) AssetPurple else PaymentRed,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Items Summary
            if (invoice.itemsSummary.isNotBlank()) {
                Text(
                    text = "الأصناف: ${invoice.itemsSummary}",
                    fontSize = 11.sp,
                    color = Color(0xFFCBD5E1),
                    maxLines = if (isExpanded) 10 else 2
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Action row: Expand, Edit, Share, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { isExpanded = !isExpanded }
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MikroTikCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isExpanded) "إخفاء التفاصيل" else "عرض تفاصيل الأصناف",
                        fontSize = 11.sp,
                        color = MikroTikCyan
                    )
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MikroTikCyan, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onShare) {
                        Icon(Icons.Default.Share, contentDescription = "مشاركة", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = PaymentRed.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Expanded Item Breakdown Table
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MikroTikNavyLight)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "جدول الأصناف والكميات والأسعار:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val items = parseItemsFromJson(invoice.itemsJson)
                    items.forEachIndexed { idx, itm ->
                        val itemSymbol = com.example.util.CurrencyHelper.getCurrencySymbol(itm.currency.ifBlank { invoice.currency })
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${idx + 1}. ${itm.name} (${itm.quantity.toInt()} × ${formatMoney(itm.unitPrice)} $itemSymbol)",
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1),
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${formatMoney(itm.subtotal)} $itemSymbol",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ProfitEmerald
                            )
                        }
                        if (idx < items.size - 1) {
                            HorizontalDivider(color = Color(0xFF334155).copy(alpha = 0.5f), thickness = 0.5.dp)
                        }
                    }

                    if (invoice.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "ملاحظات: ${invoice.notes}",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }
    }
}

private data class SimpleItem(
    val name: String,
    val quantity: Double,
    val unitPrice: Double,
    val subtotal: Double,
    val currency: String = ""
)

private fun parseItemsFromJson(jsonString: String): List<SimpleItem> {
    val list = mutableListOf<SimpleItem>()
    try {
        val array = JSONArray(jsonString)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                SimpleItem(
                    name = obj.optString("name", "صنف"),
                    quantity = obj.optDouble("quantity", 1.0),
                    unitPrice = obj.optDouble("unitPrice", 0.0),
                    subtotal = obj.optDouble("subtotal", 0.0),
                    currency = obj.optString("currency", "")
                )
            )
        }
    } catch (e: Exception) {
        // Fallback
    }
    return list
}

private fun shareInvoiceSummary(context: android.content.Context, invoice: PurchaseInvoiceEntity) {
    val dateStr = SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH).format(Date(invoice.invoiceDateMillis))
    val currSymbol = com.example.util.CurrencyHelper.getCurrencySymbol(invoice.currency)
    val text = """
        🧾 *فاتورة مشتريات معتمدة - سام ميكروتك*
        ------------------------------------
        - *المورد / الشركة:* ${invoice.supplierName}
        - *رقم الفاتورة:* ${invoice.invoiceNumber}
        - *التاريخ:* $dateStr
        - *النوع:* ${if (invoice.targetType == "ASSETS") "أصول رأسمالية (CAPEX)" else "مصروفات تشغيلية"}
        - *طريقة الدفع:* ${invoice.paymentMethod}
        ------------------------------------
        📦 *الأصناف:*
        ${invoice.itemsSummary}
        ------------------------------------
        💰 *إجمالي الفاتورة:* ${formatMoney(invoice.totalAmount)} $currSymbol
        ${if (invoice.notes.isNotBlank()) "\n📝 *ملاحظات:* ${invoice.notes}" else ""}
    """.trimIndent()

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "مشاركة الفاتورة"))
}

private fun formatMoney(amount: Double): String {
    return String.format(Locale.US, "%,.0f", amount)
}

private fun formatMoney(amount: java.math.BigDecimal): String {
    return formatMoney(amount.toDouble())
}
