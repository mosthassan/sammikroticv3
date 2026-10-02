package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CardSalesInvoiceEntity
import com.example.data.local.entity.InventoryItemEntity
import com.example.data.local.entity.RetailerEntity
import com.example.data.model.CardSalesInvoiceItem
import com.example.ui.theme.*
import com.example.util.*
import org.json.JSONArray
import java.util.UUID

/**
 * نافذة إصدار أو استنساخ فاتورة مبيعات كروت احترافية متعددة الأصناف
 * - تدعم استنساخ الفواتير السابقة بنفس التفاصيل وتوليد رقم جديد
 * - تتيح خيار حذف الفاتورة القديمة تلقائياً بعد نجاح الحفظ
 * - تحسب عدد الكروت وسعر الكرت والإجمالي لكل صنف وللفاتورة ككل تلقائياً
 * - تتيح تحديد نوع السداد: نقد، آجل، أو مبلغ مقدم ومتبقي
 * - تخصم الكميات تلقائياً من مخزن الكروت وتسجل حركة بيع
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCardSalesInvoiceDialog(
    inventoryItems: List<InventoryItemEntity>,
    retailers: List<RetailerEntity>,
    initialRetailer: RetailerEntity? = null,
    preSelectedPackageName: String? = null,
    initialInvoiceToClone: CardSalesInvoiceEntity? = null,
    invoiceToEdit: CardSalesInvoiceEntity? = null,
    onDismiss: () -> Unit,
    onConfirmInvoice: (
        customerName: String,
        customerPhone: String,
        retailerId: Long?,
        items: List<CardSalesInvoiceItem>,
        paymentType: String,
        paidAmount: Double,
        notes: String,
        oldInvoiceToDelete: CardSalesInvoiceEntity?
    ) -> Unit,
    onConfirmEdit: ((
        originalInvoice: CardSalesInvoiceEntity,
        customerName: String,
        customerPhone: String,
        retailerId: Long?,
        items: List<CardSalesInvoiceItem>,
        paymentType: String,
        paidAmount: Double,
        notes: String
    ) -> Unit)? = null
) {
    val context = LocalContext.current
    val activeSourceInvoice = invoiceToEdit ?: initialInvoiceToClone
    val isEditMode = invoiceToEdit != null

    // Parse items if editing or cloning an existing invoice
    val sourceItems = remember(activeSourceInvoice) {
        if (activeSourceInvoice != null && activeSourceInvoice.itemsJson.isNotBlank()) {
            try {
                val list = mutableListOf<CardSalesInvoiceItem>()
                val arr = JSONArray(activeSourceInvoice.itemsJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        CardSalesInvoiceItem(
                            id = if (isEditMode) obj.optString("id", UUID.randomUUID().toString()) else UUID.randomUUID().toString(),
                            packageName = obj.optString("packageName", ""),
                            quantity = obj.optInt("quantity", 1),
                            unitPrice = obj.optDouble("unitPrice", 0.0),
                            retailPrice = obj.optDouble("retailPrice", 0.0)
                        )
                    )
                }
                if (list.isNotEmpty()) list else null
            } catch (e: Exception) {
                null
            }
        } else null
    }

    val defaultRetailer = initialRetailer
        ?: if (activeSourceInvoice?.retailerId != null) retailers.find { it.id == activeSourceInvoice.retailerId }
           else retailers.find { it.name.trim().equals(activeSourceInvoice?.customerName?.trim(), ignoreCase = true) }
        ?: retailers.firstOrNull()

    // Customer Info
    var selectedRetailer by remember(initialRetailer, activeSourceInvoice) {
        mutableStateOf<RetailerEntity?>(
            if (activeSourceInvoice != null) {
                if (activeSourceInvoice.retailerId != null) retailers.find { it.id == activeSourceInvoice.retailerId }
                else retailers.find { it.name.trim().equals(activeSourceInvoice.customerName.trim(), ignoreCase = true) }
            } else defaultRetailer
        )
    }

    var isDirectCustomer by remember(initialRetailer, activeSourceInvoice) {
        mutableStateOf(
            if (activeSourceInvoice != null) {
                activeSourceInvoice.retailerId == null && retailers.none { it.name.trim().equals(activeSourceInvoice.customerName.trim(), ignoreCase = true) }
            } else false
        )
    }

    var customerNameText by remember(initialRetailer, activeSourceInvoice) {
        mutableStateOf(
            activeSourceInvoice?.customerName ?: defaultRetailer?.name ?: "عميل مباشر"
        )
    }

    var customerPhoneText by remember(initialRetailer, activeSourceInvoice) {
        mutableStateOf(
            activeSourceInvoice?.customerPhone ?: defaultRetailer?.phone ?: ""
        )
    }

    var customerSearchQuery by remember { mutableStateOf("") }
    val filteredRetailers = remember(retailers, customerSearchQuery) {
        if (customerSearchQuery.isBlank()) retailers
        else retailers.filter {
            it.name.contains(customerSearchQuery, ignoreCase = true) ||
            it.phone.contains(customerSearchQuery)
        }
    }

    // Multi-Item Invoice Rows
    // Initial row
    val initialPackage = inventoryItems.find { it.packageName == preSelectedPackageName } ?: inventoryItems.firstOrNull()
    val initialItem = CardSalesInvoiceItem(
        id = UUID.randomUUID().toString(),
        packageName = initialPackage?.packageName ?: "باقة 200 ريال يومية",
        quantity = 50,
        unitPrice = initialPackage?.wholesalePrice?.toDouble() ?: 180.0,
        retailPrice = initialPackage?.retailPrice?.toDouble() ?: 200.0
    )

    var invoiceItems by remember(activeSourceInvoice) {
        mutableStateOf(sourceItems ?: listOf(initialItem))
    }
    var isSaving by remember { mutableStateOf(false) }

    // Payment Type: CASH (نقد), CREDIT (آجل), PARTIAL (مقدم ومتبقي)
    var paymentType by remember(initialRetailer, activeSourceInvoice) {
        mutableStateOf(
            if (activeSourceInvoice != null) {
                when {
                    activeSourceInvoice.paymentType.contains("CREDIT", ignoreCase = true) || activeSourceInvoice.paymentType.contains("آجل") -> "CREDIT"
                    activeSourceInvoice.paymentType.contains("CASH", ignoreCase = true) || activeSourceInvoice.paymentType.contains("نقد") -> "CASH"
                    activeSourceInvoice.paymentType.contains("PARTIAL", ignoreCase = true) || activeSourceInvoice.paymentType.contains("مقدم") -> "PARTIAL"
                    else -> if (activeSourceInvoice.remainingAmount > java.math.BigDecimal.ZERO) "CREDIT" else "CASH"
                }
            } else {
                if (defaultRetailer != null) "CREDIT" else "CASH"
            }
        )
    }

    var customPaidAmountText by remember(activeSourceInvoice) {
        mutableStateOf(
            if (activeSourceInvoice != null && activeSourceInvoice.paidAmount > java.math.BigDecimal.ZERO) {
                activeSourceInvoice.paidAmount.toInt().toString()
            } else ""
        )
    }

    var notesText by remember(activeSourceInvoice) {
        mutableStateOf(activeSourceInvoice?.notes ?: "")
    }

    // By default, do NOT delete old invoice when cloning, so both appear in the list unless user explicitly chooses to replace!
    var deleteOldInvoiceOnSave by remember(initialInvoiceToClone) {
        mutableStateOf(false)
    }

    var stockWarningInfo by remember { mutableStateOf<Pair<String, Pair<Int, Int>>?>(null) }

    // Computed totals
    val totalInvoiceCards = invoiceItems.sumOf { it.quantity }
    val grandTotalAmount = invoiceItems.sumOf { it.lineTotal }

    val calculatedPaidAmount = when {
        paymentType.equals("CREDIT", ignoreCase = true) || paymentType.contains("آجل") || paymentType.contains("UNPAID", ignoreCase = true) -> 0.0
        paymentType.equals("CASH", ignoreCase = true) || paymentType.contains("نقد") -> grandTotalAmount
        paymentType.equals("PARTIAL", ignoreCase = true) || paymentType.contains("مقدم") -> (customPaidAmountText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, grandTotalAmount)
        else -> 0.0
    }
    val calculatedRemainingAmount = (grandTotalAmount - calculatedPaidAmount).coerceAtLeast(0.0)

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CyberDarkSurface,
            border = BorderStroke(1.dp, if (initialInvoiceToClone != null) ProfitEmerald.copy(alpha = 0.5f) else MikroTikPrimary.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 8.dp)
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
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
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    when {
                                        isEditMode -> Color(0xFF38BDF8).copy(alpha = 0.2f)
                                        initialInvoiceToClone != null -> ProfitEmerald.copy(alpha = 0.2f)
                                        else -> MikroTikPrimary.copy(alpha = 0.2f)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                when {
                                    isEditMode -> Icons.Default.Edit
                                    initialInvoiceToClone != null -> Icons.Default.CopyAll
                                    else -> Icons.Default.ReceiptLong
                                },
                                contentDescription = null,
                                tint = when {
                                    isEditMode -> Color(0xFF38BDF8)
                                    initialInvoiceToClone != null -> ProfitEmerald
                                    else -> Color(0xFF38BDF8)
                                },
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = when {
                                    isEditMode -> "تعديل فاتورة مبيعات #${invoiceToEdit?.invoiceNumber}"
                                    initialInvoiceToClone != null -> "استنساخ فاتورة جديدة"
                                    else -> "فاتورة مبيعات كروت جديدة"
                                },
                                fontFamily = CairoFontFamily,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = when {
                                    isEditMode -> "تعديل الكميات والأسعار والسداد للفاتورة القائمة مباشرة"
                                    initialInvoiceToClone != null -> "توليد رقم وتاريخ جديدين مستنسخة من #${initialInvoiceToClone.invoiceNumber}"
                                    else -> "خصم تلقائي من المخزن وحسابات محاسبية دقيقة"
                                },
                                fontFamily = CairoFontFamily,
                                fontSize = 10.5.sp,
                                color = when {
                                    isEditMode -> Color(0xFF38BDF8)
                                    initialInvoiceToClone != null -> ProfitEmerald
                                    else -> Color(0xFF38BDF8)
                                }
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = TextSecondaryDark)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 0. Edit Mode Banner OR Cloned Invoice Banner
                    if (isEditMode && invoiceToEdit != null) {
                        item {
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0284C7).copy(alpha = 0.12f)),
                                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.45f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(10.dp)
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "وضع تعديل الفاتورة رقم #${invoiceToEdit.invoiceNumber}",
                                            fontFamily = CairoFontFamily,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "تعديلك للأصناف أو المبالغ سيحدث سجل الفاتورة وحسابات المخزن فورياً.",
                                            fontFamily = CairoFontFamily,
                                            fontSize = 10.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }
                            }
                        }
                    } else if (initialInvoiceToClone != null) {
                        item {
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = ProfitEmerald.copy(alpha = 0.08f)),
                                border = BorderStroke(1.dp, ProfitEmerald.copy(alpha = 0.45f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(ProfitEmerald.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.CopyAll, contentDescription = null, tint = ProfitEmerald, modifier = Modifier.size(16.dp))
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "استنساخ من الفاتورة #${initialInvoiceToClone.invoiceNumber}",
                                                fontFamily = CairoFontFamily,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ProfitEmerald
                                            )
                                            Text(
                                                text = "سيتم إنشاء فاتورة جديدة برقم وتاريخ اليوم بنفس التفاصيل",
                                                fontFamily = CairoFontFamily,
                                                fontSize = 10.sp,
                                                color = Color(0xFFCBD5E1)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "استبدال وحذف الفاتورة الأصلية السابقة",
                                                fontFamily = CairoFontFamily,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "فعّل هذا الخيار فقط إذا كنت تريد إلغاء الفاتورة السابقة نهائياً",
                                                fontFamily = CairoFontFamily,
                                                fontSize = 9.5.sp,
                                                color = TextSecondaryDark
                                            )
                                        }
                                        Switch(
                                            checked = deleteOldInvoiceOnSave,
                                            onCheckedChange = { deleteOldInvoiceOnSave = it },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = ProfitEmerald
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                    // 1. Customer Selection with Instant Search (بحث فوري فائق السلاسة عن العملاء)
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
                            border = BorderStroke(1.dp, CyberBorder)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Storefront, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("العميل / نقطة البيع:", fontFamily = CairoFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }

                                    // Direct customer toggle
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            if (isDirectCustomer) "عميل كاش مباشر" else "من البقالات المسجلة",
                                            fontFamily = CairoFontFamily,
                                            fontSize = 10.5.sp,
                                            color = if (isDirectCustomer) StatusWarning else ProfitEmerald
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Switch(
                                            checked = isDirectCustomer,
                                            onCheckedChange = {
                                                isDirectCustomer = it
                                                if (it) {
                                                    customerNameText = "عميل نقدي مباشر"
                                                    customerPhoneText = ""
                                                    selectedRetailer = null
                                                } else {
                                                    selectedRetailer = retailers.firstOrNull()
                                                    customerNameText = selectedRetailer?.name ?: ""
                                                    customerPhoneText = selectedRetailer?.phone ?: ""
                                                }
                                            },
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                if (!isDirectCustomer) {
                                    // Search Bar for Retailers
                                    OutlinedTextField(
                                        value = customerSearchQuery,
                                        onValueChange = { customerSearchQuery = it },
                                        placeholder = {
                                            Text(
                                                "ابحث عن العميل أو البقالة بالاسم أو الهاتف...",
                                                fontFamily = CairoFontFamily,
                                                fontSize = 11.5.sp,
                                                color = TextSecondaryDark
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                                        },
                                        trailingIcon = {
                                            if (customerSearchQuery.isNotEmpty()) {
                                                IconButton(onClick = { customerSearchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                                    Icon(Icons.Default.Clear, contentDescription = "مسح البحث", tint = TextSecondaryDark, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        },
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF38BDF8),
                                            unfocusedBorderColor = CyberBorder,
                                            focusedContainerColor = CyberDarkSurface,
                                            unfocusedContainerColor = CyberDarkSurface,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Display currently selected customer card
                                    selectedRetailer?.let { curRet ->
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFF0F253E),
                                            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.7f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .clip(CircleShape)
                                                            .background(ProfitEmerald.copy(alpha = 0.2f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(Icons.Default.Check, contentDescription = null, tint = ProfitEmerald, modifier = Modifier.size(16.dp))
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column {
                                                        Text(
                                                            text = curRet.name,
                                                            fontFamily = CairoFontFamily,
                                                            fontSize = 12.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White
                                                        )
                                                        if (curRet.phone.isNotBlank()) {
                                                            Text(
                                                                text = curRet.phone,
                                                                fontSize = 10.sp,
                                                                color = TextSecondaryDark
                                                            )
                                                        }
                                                    }
                                                }

                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (curRet.balanceOwed > 0) StatusWarning.copy(alpha = 0.15f) else ProfitEmerald.copy(alpha = 0.15f),
                                                    border = BorderStroke(1.dp, if (curRet.balanceOwed > 0) StatusWarning.copy(alpha = 0.4f) else ProfitEmerald.copy(alpha = 0.4f))
                                                ) {
                                                    Text(
                                                        text = if (curRet.balanceOwed > 0) "آجل: ${curRet.balanceOwed.toInt()} ر.ي" else "خالص الحساب ✓",
                                                        fontFamily = CairoFontFamily,
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (curRet.balanceOwed > 0) StatusWarning else ProfitEmerald,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }

                                    // Filtered Retailers List (instant clickable chips / items)
                                    if (filteredRetailers.isNotEmpty()) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            filteredRetailers.take(5).forEach { ret ->
                                                val isSel = selectedRetailer?.id == ret.id
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isSel) MikroTikPrimary.copy(alpha = 0.25f) else CyberDarkSurface,
                                                    border = BorderStroke(1.dp, if (isSel) Color(0xFF38BDF8) else CyberBorder),
                                                    onClick = {
                                                        selectedRetailer = ret
                                                        customerNameText = ret.name
                                                        customerPhoneText = ret.phone
                                                    },
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(
                                                                if (isSel) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                                                contentDescription = null,
                                                                tint = if (isSel) Color(0xFF38BDF8) else TextSecondaryDark,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text(ret.name, fontFamily = CairoFontFamily, fontSize = 11.5.sp, color = Color.White)
                                                        }
                                                        Text(
                                                            text = if (ret.balanceOwed > 0) "آجل: ${ret.balanceOwed.toInt()} ر.ي" else "خالص الحساب",
                                                            fontFamily = CairoFontFamily,
                                                            fontSize = 10.sp,
                                                            color = if (ret.balanceOwed > 0) StatusWarning else TextSecondaryDark
                                                        )
                                                    }
                                                }
                                            }
                                            if (filteredRetailers.size > 5) {
                                                Text(
                                                    text = "+ ${filteredRetailers.size - 5} عملاء آخرين مطابقة للبحث...",
                                                    fontFamily = CairoFontFamily,
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF38BDF8),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    } else if (customerSearchQuery.isNotBlank()) {
                                        // Quick add query as direct customer
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF1E293B),
                                            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                                            onClick = {
                                                isDirectCustomer = true
                                                customerNameText = customerSearchQuery.trim()
                                                selectedRetailer = null
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "لا توجد بقالة بهذا الاسم، انقر لاعتماد \"${customerSearchQuery.trim()}\" كعميل مباشر",
                                                    fontFamily = CairoFontFamily,
                                                    fontSize = 11.sp,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    // Direct customer text inputs
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = customerNameText,
                                            onValueChange = { customerNameText = it },
                                            label = { Text("اسم العميل أو المحل") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1.2f)
                                        )
                                        OutlinedTextField(
                                            value = customerPhoneText,
                                            onValueChange = { customerPhoneText = it },
                                            label = { Text("رقم الهاتف") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. Multi-Item Table Header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "أصناف الفاتورة (${invoiceItems.size} أصناف):",
                                fontFamily = CairoFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Button(
                                onClick = {
                                    val nextPkg = inventoryItems.firstOrNull { inv -> invoiceItems.none { it.packageName == inv.packageName } }
                                        ?: inventoryItems.firstOrNull()
                                    val newItem = CardSalesInvoiceItem(
                                        id = UUID.randomUUID().toString(),
                                        packageName = nextPkg?.packageName ?: "باقة 500 ريال فايبر",
                                        quantity = 20,
                                        unitPrice = nextPkg?.wholesalePrice?.toDouble() ?: 450.0,
                                        retailPrice = nextPkg?.retailPrice?.toDouble() ?: 500.0
                                    )
                                    invoiceItems = invoiceItems + newItem
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ProfitEmerald),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Black)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("+ إضافة صنف آخر", fontFamily = CairoFontFamily, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                    }

                    // 3. Multi-Item Line Rows
                    itemsIndexed(invoiceItems) { index, item ->
                        InvoiceLineItemCard(
                            index = index + 1,
                            item = item,
                            inventoryItems = inventoryItems,
                            canDelete = invoiceItems.size > 1,
                            onUpdateItem = { updated ->
                                invoiceItems = invoiceItems.map { if (it.id == item.id) updated else it }
                            },
                            onDeleteItem = {
                                invoiceItems = invoiceItems.filterNot { it.id == item.id }
                            }
                        )
                    }

                    // 4. Payment Terms Selector
                    item {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
                            border = BorderStroke(1.dp, CyberBorder)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "نوع الفاتورة وطريقة السداد:",
                                    fontFamily = CairoFontFamily,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // نقد
                                    PaymentTypePill(
                                        title = "نقد (كاش)",
                                        sub = "سداد فوري",
                                        selected = paymentType == "CASH",
                                        color = ProfitEmerald,
                                        modifier = Modifier.weight(1f),
                                        onClick = { paymentType = "CASH" }
                                    )
                                    // آجل
                                    PaymentTypePill(
                                        title = "آجل (دين)",
                                        sub = "قيد على الحساب",
                                        selected = paymentType == "CREDIT",
                                        color = Color(0xFFEF4444),
                                        modifier = Modifier.weight(1f),
                                        onClick = { paymentType = "CREDIT" }
                                    )
                                    // جزئي
                                    PaymentTypePill(
                                        title = "مقدم ومتبقي",
                                        sub = "دفع جزئي",
                                        selected = paymentType == "PARTIAL",
                                        color = StatusWarning,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            paymentType = "PARTIAL"
                                            if (customPaidAmountText.isEmpty()) {
                                                customPaidAmountText = (grandTotalAmount / 2).toInt().toString()
                                            }
                                        }
                                    )
                                }

                                if (paymentType == "PARTIAL") {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = customPaidAmountText,
                                            onValueChange = { customPaidAmountText = it },
                                            label = { Text("المبلغ المدفوع مقدماً (ر.ي)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFEF4444).copy(alpha = 0.15f),
                                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Text("المبلغ المتبقي (آجل):", fontFamily = CairoFontFamily, fontSize = 9.5.sp, color = TextSecondaryDark)
                                                Text("${calculatedRemainingAmount.toInt()} ريال", fontFamily = CairoFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 5. Notes
                    item {
                        OutlinedTextField(
                            value = notesText,
                            onValueChange = { notesText = it },
                            label = { Text("ملاحظات الفاتورة") },
                            placeholder = { Text("مثال: تم التسليم لمندوب البقالة - يُسدد الباقي يوم الخميس") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // 6. Grand Calculation Summary Card
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF0F1E33),
                            border = BorderStroke(1.dp, MikroTikPrimary.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("إجمالي عدد الكروت المباعة:", fontFamily = CairoFontFamily, fontSize = 11.5.sp, color = TextSecondaryDark)
                                    Text("$totalInvoiceCards كرت", fontFamily = CairoFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("إجمالي قيمة الفاتورة:", fontFamily = CairoFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("${grandTotalAmount.toInt()} ريال", fontFamily = CairoFontFamily, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Divider(modifier = Modifier.padding(vertical = 6.dp), color = CyberBorder)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("المدفوع نقداً:", fontFamily = CairoFontFamily, fontSize = 10.sp, color = TextSecondaryDark)
                                        Text("${calculatedPaidAmount.toInt()} ريال", fontFamily = CairoFontFamily, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = ProfitEmerald)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("المتبقي (آجل):", fontFamily = CairoFontFamily, fontSize = 10.sp, color = TextSecondaryDark)
                                        Text("${calculatedRemainingAmount.toInt()} ريال", fontFamily = CairoFontFamily, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = if (calculatedRemainingAmount > 0) Color(0xFFEF4444) else ProfitEmerald)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(0.7f)
                    ) {
                        Text("إلغاء", fontFamily = CairoFontFamily)
                    }

                    Button(
                        onClick = {
                            if (isSaving) return@Button
                            if (customerNameText.isBlank()) {
                                Toast.makeText(context, "يرجى تحديد أو إدخال اسم العميل", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val validItems = invoiceItems.filter { it.quantity > 0 }
                            if (validItems.isEmpty() || totalInvoiceCards <= 0) {
                                Toast.makeText(context, "يجب إضافة صنف واحد على الأقل بكمية أكبر من صفر", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val executeFinalSave = {
                                isSaving = true
                                if (isEditMode && invoiceToEdit != null && onConfirmEdit != null) {
                                    onConfirmEdit(
                                        invoiceToEdit,
                                        customerNameText.trim(),
                                        customerPhoneText.trim(),
                                        selectedRetailer?.id,
                                        validItems,
                                        paymentType,
                                        calculatedPaidAmount,
                                        notesText.trim()
                                    )
                                } else {
                                    onConfirmInvoice(
                                        customerNameText.trim(),
                                        customerPhoneText.trim(),
                                        selectedRetailer?.id,
                                        validItems,
                                        paymentType,
                                        calculatedPaidAmount,
                                        notesText.trim(),
                                        if (deleteOldInvoiceOnSave) initialInvoiceToClone else null
                                    )
                                }
                            }

                            // Check stock availability
                            val stockIssue = validItems.firstOrNull { item ->
                                val baseAvailable = findMatchingInventoryItem(inventoryItems, item.packageName)?.quantityAvailable ?: 0
                                val refundedQty = if (isEditMode || (deleteOldInvoiceOnSave && initialInvoiceToClone != null)) {
                                    sourceItems?.filter { it.packageName == item.packageName }?.sumOf { it.quantity } ?: 0
                                } else 0
                                val effectiveAvailable = baseAvailable + refundedQty
                                item.quantity > effectiveAvailable
                            }
                            if (stockIssue != null) {
                                val baseAvailable = findMatchingInventoryItem(inventoryItems, stockIssue.packageName)?.quantityAvailable ?: 0
                                val refundedQty = if (isEditMode || (deleteOldInvoiceOnSave && initialInvoiceToClone != null)) {
                                    sourceItems?.filter { it.packageName == stockIssue.packageName }?.sumOf { it.quantity } ?: 0
                                } else 0
                                val effectiveAvailable = baseAvailable + refundedQty
                                stockWarningInfo = Pair(stockIssue.packageName, Pair(stockIssue.quantity, effectiveAvailable))
                                return@Button
                            }

                            executeFinalSave()
                        },
                        enabled = !isSaving,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when {
                                isEditMode -> Color(0xFF0284C7)
                                initialInvoiceToClone != null && deleteOldInvoiceOnSave -> ProfitEmerald
                                initialInvoiceToClone != null -> Color(0xFF10B981)
                                else -> MikroTikPrimary
                            }
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جاري الحفظ...", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, color = Color.White)
                        } else {
                            Icon(
                                when {
                                    isEditMode -> Icons.Default.Edit
                                    initialInvoiceToClone != null -> Icons.Default.CopyAll
                                    else -> Icons.Default.CheckCircle
                                },
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when {
                                    isEditMode -> "حفظ التعديلات ✓"
                                    initialInvoiceToClone != null && deleteOldInvoiceOnSave -> "حفظ الفاتورة وحذف السابقة ✓"
                                    initialInvoiceToClone != null -> "حفظ كفاتورة جديدة مستنسخة ✓"
                                    else -> "إصدار الفاتورة وخصم المخزن"
                                },
                                fontFamily = CairoFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // نافذة تأكيد تجاوز رصيد المخزن والمتابعة
    stockWarningInfo?.let { warning ->
        val pkgName = warning.first
        val reqQty = warning.second.first
        val availQty = warning.second.second

        AlertDialog(
            onDismissRequest = { stockWarningInfo = null },
            icon = {
                Icon(Icons.Default.Warning, contentDescription = null, tint = StatusWarning, modifier = Modifier.size(32.dp))
            },
            title = {
                Text(
                    text = "تنبيه توفر الكروت بالمخزن",
                    fontFamily = CairoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "الكمية المطلوبة من الصنف ($pkgName) هي $reqQty كرت، بينما الرصيد الحالي بالمخزن هو $availQty كرت.\n\nهل ترغب في متابعة إصدار الفاتورة وتحديث رصيد المخزن وضبط الحسابات؟",
                    fontFamily = CairoFontFamily,
                    fontSize = 13.sp,
                    color = TextSecondaryDark
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        stockWarningInfo = null
                        isSaving = true
                        val validItems = invoiceItems.filter { it.quantity > 0 }
                        if (isEditMode && invoiceToEdit != null && onConfirmEdit != null) {
                            onConfirmEdit(
                                invoiceToEdit,
                                customerNameText.trim(),
                                customerPhoneText.trim(),
                                selectedRetailer?.id,
                                validItems,
                                paymentType,
                                calculatedPaidAmount,
                                notesText.trim()
                            )
                        } else {
                            onConfirmInvoice(
                                customerNameText.trim(),
                                customerPhoneText.trim(),
                                selectedRetailer?.id,
                                validItems,
                                paymentType,
                                calculatedPaidAmount,
                                notesText.trim(),
                                if (deleteOldInvoiceOnSave) initialInvoiceToClone else null
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ProfitEmerald)
                ) {
                    Text("متابعة وحفظ الفاتورة ✓", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { stockWarningInfo = null }) {
                    Text("العودة لتعديل الكمية", fontFamily = CairoFontFamily, color = TextSecondaryDark)
                }
            },
            containerColor = CyberDarkSurface
        )
    }
}

/**
 * سطر الصنف الواحد داخل جدول الفاتورة
 */
@Composable
private fun InvoiceLineItemCard(
    index: Int,
    item: CardSalesInvoiceItem,
    inventoryItems: List<InventoryItemEntity>,
    canDelete: Boolean,
    onUpdateItem: (CardSalesInvoiceItem) -> Unit,
    onDeleteItem: () -> Unit
) {
    val currentStock = findMatchingInventoryItem(inventoryItems, item.packageName)?.quantityAvailable ?: 0
    val isExceedingStock = item.quantity > currentStock

    var quantityInput by remember(item.id) {
        mutableStateOf(item.quantity.toString())
    }
    var unitPriceInput by remember(item.id) {
        mutableStateOf(item.unitPrice.toInt().toString())
    }

    // Keep inputs synced if item changes externally
    LaunchedEffect(item.quantity) {
        if (quantityInput.toIntOrNull() != item.quantity && item.quantity > 0) {
            quantityInput = item.quantity.toString()
        }
    }
    LaunchedEffect(item.unitPrice) {
        if (unitPriceInput.toIntOrNull() != item.unitPrice.toInt()) {
            unitPriceInput = item.unitPrice.toInt().toString()
        }
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
        border = BorderStroke(1.dp, if (isExceedingStock) Color(0xFFEF4444) else CyberBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Header of item row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF0284C7).copy(alpha = 0.2f),
                        modifier = Modifier.size(20.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("$index", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.packageName,
                        fontFamily = CairoFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "المخزن: $currentStock كرت",
                        fontFamily = CairoFontFamily,
                        fontSize = 10.sp,
                        color = if (currentStock > 0) ProfitEmerald else Color(0xFFEF4444)
                    )
                    if (canDelete) {
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(onClick = onDeleteItem, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "حذف الصنف", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Package Selector Dropdown/Row if multiple exist
            if (inventoryItems.size > 1) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    inventoryItems.take(4).forEach { inv ->
                        val isSel = inv.packageName == item.packageName
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isSel) MikroTikPrimary else CyberDarkCardElevated,
                            onClick = {
                                onUpdateItem(
                                    item.copy(
                                        packageName = inv.packageName,
                                        unitPrice = inv.wholesalePrice.toDouble(),
                                        retailPrice = inv.retailPrice.toDouble()
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = inv.packageName.replace("باقة ", ""),
                                fontFamily = CairoFontFamily,
                                fontSize = 9.5.sp,
                                maxLines = 1,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = if (isSel) Color.White else TextSecondaryDark,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Inputs: Quantity (Manual typing + Stepper), Unit Wholesale Price, Line Subtotal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quantity with Editable Input + Steppers
                Column(modifier = Modifier.weight(1.35f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("العدد (اكتب يدوياً):", fontFamily = CairoFontFamily, fontSize = 9.5.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberDarkCardElevated)
                            .border(1.dp, if (item.quantity <= 0) Color(0xFFEF4444) else CyberBorder, RoundedCornerShape(8.dp))
                    ) {
                        IconButton(
                            onClick = {
                                val current = item.quantity
                                val next = when {
                                    current > 10 -> current - 5
                                    current > 1 -> current - 1
                                    else -> 1
                                }
                                quantityInput = next.toString()
                                onUpdateItem(item.copy(quantity = next))
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "إنقاص", tint = TextSecondaryDark, modifier = Modifier.size(14.dp))
                        }

                        // Editable input for direct typing
                        BasicTextField(
                            value = quantityInput,
                            onValueChange = { raw ->
                                val digitsOnly = raw.filter { it.isDigit() }.take(5)
                                quantityInput = digitsOnly
                                val parsed = digitsOnly.toIntOrNull() ?: 0
                                onUpdateItem(item.copy(quantity = parsed))
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            textStyle = TextStyle(
                                fontFamily = CairoFontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            ),
                            cursorBrush = SolidColor(MikroTikPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 4.dp),
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.Center) {
                                    if (quantityInput.isEmpty()) {
                                        Text("0", fontFamily = CairoFontFamily, fontSize = 14.sp, color = TextSecondaryDark, textAlign = TextAlign.Center)
                                    }
                                    innerTextField()
                                }
                            }
                        )

                        IconButton(
                            onClick = {
                                val current = item.quantity
                                val next = current + 5
                                quantityInput = next.toString()
                                onUpdateItem(item.copy(quantity = next))
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "زيادة", tint = MikroTikPrimary, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                // Unit Price (Editable wholesale)
                Column(modifier = Modifier.weight(1.05f)) {
                    Text("سعر الكرت (ر.ي):", fontFamily = CairoFontFamily, fontSize = 9.5.sp, color = TextSecondaryDark)
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CyberDarkCardElevated,
                        border = BorderStroke(1.dp, CyberBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        BasicTextField(
                            value = unitPriceInput,
                            onValueChange = { raw ->
                                val digits = raw.filter { it.isDigit() }.take(6)
                                unitPriceInput = digits
                                val p = digits.toDoubleOrNull() ?: 0.0
                                onUpdateItem(item.copy(unitPrice = p))
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            textStyle = TextStyle(
                                fontFamily = CairoFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8),
                                textAlign = TextAlign.Center
                            ),
                            cursorBrush = SolidColor(MikroTikPrimary),
                            modifier = Modifier.padding(vertical = 7.dp)
                        )
                    }
                }

                // Subtotal for line (العدد × السعر)
                Column(modifier = Modifier.weight(1.15f)) {
                    Text("الإجمالي:", fontFamily = CairoFontFamily, fontSize = 9.5.sp, color = TextSecondaryDark)
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MikroTikPrimary.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, MikroTikPrimary.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${item.lineTotal.toInt()} ر.ي",
                            fontFamily = CairoFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ProfitEmerald,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
            }

            // Quick Preset Quantities (أزرار سريعة للعدد)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(10, 20, 50, 100).forEach { presetVal ->
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (item.quantity == presetVal) MikroTikPrimary.copy(alpha = 0.3f) else CyberDarkCardElevated,
                        border = BorderStroke(0.8.dp, if (item.quantity == presetVal) MikroTikPrimary else CyberBorder),
                        onClick = {
                            quantityInput = presetVal.toString()
                            onUpdateItem(item.copy(quantity = presetVal))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "$presetVal كرت",
                            fontFamily = CairoFontFamily,
                            fontSize = 9.sp,
                            color = if (item.quantity == presetVal) Color(0xFF38BDF8) else TextSecondaryDark,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 3.dp)
                        )
                    }
                }

                if (currentStock > 0) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (item.quantity == currentStock) ProfitEmerald.copy(alpha = 0.2f) else CyberDarkCardElevated,
                        border = BorderStroke(0.8.dp, if (item.quantity == currentStock) ProfitEmerald else CyberBorder),
                        onClick = {
                            quantityInput = currentStock.toString()
                            onUpdateItem(item.copy(quantity = currentStock))
                        },
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Text(
                            text = "كامل المخزن",
                            fontFamily = CairoFontFamily,
                            fontSize = 9.sp,
                            color = ProfitEmerald,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 3.dp)
                        )
                    }
                }
            }

            if (isExceedingStock) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "تنبيه: الكمية المطلوبة (${item.quantity}) تتجاوز الرصيد المتوفر في المخزن ($currentStock)!",
                    fontFamily = CairoFontFamily,
                    fontSize = 10.sp,
                    color = Color(0xFFEF4444)
                )
            }
        }
    }
}

/**
 * زر تحديد نوع السداد
 */
@Composable
private fun PaymentTypePill(
    title: String,
    sub: String,
    selected: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) color.copy(alpha = 0.2f) else CyberDarkSurface,
        border = BorderStroke(1.dp, if (selected) color else CyberBorder),
        onClick = onClick,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontFamily = CairoFontFamily,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) color else Color.White
            )
            Text(
                text = sub,
                fontFamily = CairoFontFamily,
                fontSize = 9.sp,
                color = TextSecondaryDark
            )
        }
    }
}

private fun findMatchingInventoryItem(
    inventoryItems: List<InventoryItemEntity>,
    packageName: String
): InventoryItemEntity? {
    val clean = packageName.trim()
    if (clean.isBlank()) return null

    val exact = inventoryItems.find { it.packageName == packageName }
    if (exact != null) return exact

    val trimmed = inventoryItems.find { it.packageName.trim() == clean }
    if (trimmed != null) return trimmed

    val targetNorm = normalizeArabicDialog(clean)
    return inventoryItems.find {
        val cleanItemName = it.packageName.trim()
        cleanItemName.equals(clean, ignoreCase = true) ||
        normalizeArabicDialog(cleanItemName) == targetNorm ||
        cleanItemName.contains(clean, ignoreCase = true) ||
        clean.contains(cleanItemName, ignoreCase = true)
    }
}

private fun normalizeArabicDialog(text: String): String {
    return text.trim()
        .replace("[أإآا]".toRegex(), "ا")
        .replace("[ةه]".toRegex(), "ه")
        .replace("[ئىي]".toRegex(), "ي")
        .lowercase()
}
