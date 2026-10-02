package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import com.example.data.local.entity.FinancialVoucherEntity
import com.example.data.local.entity.RetailerEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.AssetPurple
import com.example.ui.theme.CairoFontFamily
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberDarkCardElevated
import com.example.ui.theme.CyberDarkSurface
import com.example.ui.theme.EquityBlue
import com.example.ui.theme.InvestmentGold
import com.example.ui.theme.MikroTikCyan
import com.example.ui.theme.MikroTikNavy
import com.example.ui.theme.MikroTikPrimary
import com.example.ui.theme.PaymentRed
import com.example.ui.theme.ProfitEmerald
import com.example.ui.theme.ReceiptGreen
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.theme.WhatsAppDarkGreen
import com.example.ui.theme.WhatsAppGreen
import com.example.util.WhatsAppHelper
import com.example.util.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VouchersScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var selectedFinanceTab by remember { mutableStateOf(0) }
    var selectedLedgerAccountCode by remember { mutableStateOf("1101") }
    var showSmartInvoiceScanner by remember { mutableStateOf(false) }
    var invoiceScannerTargetType by remember { mutableStateOf("EXPENSES") }

    Column(modifier = modifier.fillMaxSize()) {
        // High-level Top Finance & Investment Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedFinanceTab,
            containerColor = CyberDarkSurface,
            contentColor = Color.White,
            edgePadding = 12.dp,
            indicator = { tabPositions ->
                if (selectedFinanceTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedFinanceTab]),
                        color = when (selectedFinanceTab) {
                            1 -> MikroTikCyan
                            2 -> ReceiptGreen
                            3 -> InvestmentGold
                            4 -> ProfitEmerald
                            5 -> AssetPurple
                            6 -> EquityBlue
                            7 -> Color(0xFF38BDF8)
                            else -> Color(0xFF38BDF8)
                        },
                        height = 3.dp
                    )
                }
            }
        ) {
            val tabs = listOf(
                Triple("السندات والمصروفات", Icons.Default.Receipt, 0),
                Triple("لوحة التقارير 📊", Icons.Default.Assessment, 1),
                Triple("دفتر الأستاذ العام 📖", Icons.Default.AccountBalance, 2),
                Triple("ميزان المراجعة ⚖️", Icons.Default.Assessment, 3),
                Triple("الأرباح والخسائر (P&L) 📈", Icons.Default.Assessment, 4),
                Triple("فواتير المشتريات (AI)", Icons.Default.AutoAwesome, 5),
                Triple("الأصول الثابتة (CAPEX)", Icons.Default.Devices, 6),
                Triple("الشركاء ورأس المال", Icons.Default.Group, 7)
            )

            tabs.forEach { (title, icon, idx) ->
                val isSelected = selectedFinanceTab == idx
                Tab(
                    selected = isSelected,
                    onClick = { selectedFinanceTab = idx },
                    text = {
                        Text(
                            text = title,
                            fontFamily = CairoFontFamily,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else TextSecondaryDark
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) {
                                when (idx) {
                                    1 -> MikroTikCyan
                                    2 -> ReceiptGreen
                                    3 -> InvestmentGold
                                    4 -> ProfitEmerald
                                    5 -> AssetPurple
                                    6 -> EquityBlue
                                    7 -> Color(0xFF38BDF8)
                                    else -> Color(0xFF38BDF8)
                                }
                            } else TextSecondaryDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }

        // Screen Body Based on Selected Tab
        when (selectedFinanceTab) {
            0 -> VouchersListSubScreen(
                viewModel = viewModel,
                onOpenScanDialog = { target ->
                    invoiceScannerTargetType = target
                    showSmartInvoiceScanner = true
                }
            )
            1 -> ReportsDashboardScreen()
            2 -> GeneralLedgerScreen(
                viewModel = viewModel,
                initialAccountCode = selectedLedgerAccountCode
            )
            3 -> TrialBalanceScreen(
                viewModel = viewModel,
                onNavigateToLedger = { accountCode ->
                    selectedLedgerAccountCode = accountCode
                    selectedFinanceTab = 2
                }
            )
            4 -> ProfitLossScreen(
                viewModel = viewModel,
                onNavigateToAI = { prompt ->
                    viewModel.consultAi(prompt)
                }
            )
            5 -> PurchaseInvoicesSubScreen(
                viewModel = viewModel,
                onOpenScanDialog = { target ->
                    invoiceScannerTargetType = target
                    showSmartInvoiceScanner = true
                }
            )
            6 -> AssetsScreen(viewModel = viewModel)
            7 -> PartnersScreen(viewModel = viewModel)
        }
    }

    if (showSmartInvoiceScanner) {
        SmartInvoiceScannerDialog(
            viewModel = viewModel,
            initialTargetType = invoiceScannerTargetType,
            onDismissRequest = { showSmartInvoiceScanner = false }
        )
    }
}

@Composable
fun VouchersListSubScreen(
    viewModel: MainViewModel,
    onOpenScanDialog: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val vouchers by viewModel.vouchers.collectAsState()
    val totalReceipts by viewModel.totalReceipts.collectAsState()
    val totalPayments by viewModel.totalPayments.collectAsState()
    val retailers by viewModel.retailers.collectAsState()
    val context = LocalContext.current

    var selectedTypeFilter by remember { mutableStateOf("الكل") }
    var showAddDialog by remember { mutableStateOf(false) }
    var voucherToClone by remember { mutableStateOf<FinancialVoucherEntity?>(null) }
    var voucherToEdit by remember { mutableStateOf<FinancialVoucherEntity?>(null) }
    var voucherToPreview by remember { mutableStateOf<FinancialVoucherEntity?>(null) }
    var voucherToVoid by remember { mutableStateOf<FinancialVoucherEntity?>(null) }
    var voucherToDelete by remember { mutableStateOf<FinancialVoucherEntity?>(null) }

    LaunchedEffect(Unit) {
        viewModel.deduplicateVouchers()
    }

    val filterTypes = listOf("الكل", "سندات قبض", "سندات صرف")

    val filteredVouchers = vouchers.filter { voucher ->
        when (selectedTypeFilter) {
            "سندات قبض" -> voucher.voucherType == "RECEIPT"
            "سندات صرف" -> voucher.voucherType == "PAYMENT"
            else -> true
        }
    }

    val (receiptsVal, paymentsVal, netProfit, summaryCurrency) = remember(filteredVouchers) {
        val active = filteredVouchers.filter { !it.isVoided }
        if (active.isEmpty()) {
            listOf(0.0, 0.0, 0.0, "YER")
        } else {
            val distinctCurrencies = active.map { it.currency.uppercase() }.toSet()
            if (distinctCurrencies.size == 1) {
                val curr = distinctCurrencies.first()
                val receipts = active.filter { it.voucherType == "RECEIPT" }.sumOf { it.amount.toDouble() }
                val payments = active.filter { it.voucherType == "PAYMENT" }.sumOf { it.amount.toDouble() }
                listOf(receipts, payments, receipts - payments, curr)
            } else {
                val receiptsYer = active.filter { it.voucherType == "RECEIPT" }.sumOf { v ->
                    viewModel.convertToYer(v.amount.toDouble(), v.currency)
                }
                val paymentsYer = active.filter { it.voucherType == "PAYMENT" }.sumOf { v ->
                    viewModel.convertToYer(v.amount.toDouble(), v.currency)
                }
                listOf(receiptsYer, paymentsYer, receiptsYer - paymentsYer, "YER")
            }
        }
    }

    val finalReceipts = receiptsVal as Double
    val finalPayments = paymentsVal as Double
    val finalNet = netProfit as Double
    val finalCurr = summaryCurrency as String

    Box(modifier = modifier.fillMaxSize().testTag("vouchers_screen")) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "السندات المالية والمصروفات",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "سندات القبض والصرف، أرباح الكروت، ومصاريف التشغيل",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            viewModel.deduplicateVouchers { count ->
                                val msg = if (count > 0) "تم تنظيف وحذف $count سند مكرر بنجاح ✓" else "جميع السندات فريدة ولا توجد تكرارات ✓"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("deduplicate_vouchers_button")
                    ) {
                        Text("إزالة التكرار", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = CairoFontFamily)
                    }

                    Button(
                        onClick = { onOpenScanDialog("EXPENSES") },
                        colors = ButtonDefaults.buttonColors(containerColor = PaymentRed.copy(alpha = 0.9f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("scan_invoice_vouchers_header_button")
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مسح فاتورة (AI)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }


            Spacer(modifier = Modifier.height(14.dp))

            // Accounting KPI Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VoucherSummaryCard(
                    title = "إجمالي المقبوضات",
                    amount = finalReceipts,
                    currency = finalCurr,
                    color = ReceiptGreen,
                    icon = Icons.Default.ArrowDownward,
                    modifier = Modifier.weight(1f)
                )
                VoucherSummaryCard(
                    title = "إجمالي المصروفات",
                    amount = finalPayments,
                    currency = finalCurr,
                    color = PaymentRed,
                    icon = Icons.Default.ArrowUpward,
                    modifier = Modifier.weight(1f)
                )
                VoucherSummaryCard(
                    title = "صافي الحركة",
                    amount = finalNet,
                    currency = finalCurr,
                    color = if (finalNet >= 0) ReceiptGreen else PaymentRed,
                    icon = Icons.Default.Receipt,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Filter Tabs
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filterTypes) { filter ->
                    val isSelected = selectedTypeFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) MikroTikPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                            .clickable { selectedTypeFilter = filter }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = filter,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Vouchers List
            if (filteredVouchers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "لا توجد سندات مالية مسجلة في هذا القسم", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredVouchers, key = { it.id }) { voucher ->
                        val linkedRetailer = retailers.firstOrNull { it.id == voucher.retailerId }
                        VoucherItemCard(
                            voucher = voucher,
                            onEdit = {
                                voucherToEdit = voucher
                                voucherToClone = null
                                showAddDialog = true
                            },
                            onClone = {
                                voucherToClone = voucher
                                voucherToEdit = null
                                showAddDialog = true
                                Toast.makeText(context, "تم نسخ تفاصيل السند لتسجيل سند جديد بسهولة", Toast.LENGTH_SHORT).show()
                            },
                            onPreview = { voucherToPreview = voucher },
                            onWhatsAppShare = {
                                val msg = WhatsAppHelper.generateVoucherMessage(voucher, linkedRetailer?.phone, linkedRetailer?.balanceOwed?.toDouble())
                                if (linkedRetailer != null && linkedRetailer.phone.isNotBlank()) {
                                    WhatsAppHelper.sendWhatsAppMessage(context, linkedRetailer.phone, msg)
                                } else {
                                    WhatsAppHelper.shareTextIntent(context, msg, "مشاركة السند عبر الواتساب")
                                }
                            },
                            onVoid = { voucherToVoid = voucher },
                            onDelete = { voucherToDelete = voucher }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }

        // FAB: Issue Financial Voucher
        FloatingActionButton(
            onClick = {
                voucherToClone = null
                voucherToEdit = null
                showAddDialog = true
            },
            containerColor = MikroTikPrimary,
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_voucher_fab")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = "إصدار سند")
                Spacer(modifier = Modifier.width(6.dp))
                Text("إصدار سند مالي", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        // Add Voucher Dialog
        if (showAddDialog) {
            AddVoucherDialog(
                retailers = retailers,
                voucherToClone = voucherToClone,
                voucherToEdit = voucherToEdit,
                onDismiss = {
                    showAddDialog = false
                    voucherToClone = null
                    voucherToEdit = null
                },
                onSave = { type, amount, curr, party, retailerId, cat, method, desc, shareWhatsApp ->
                    if (voucherToEdit != null) {
                        viewModel.updateVoucher(
                            voucherToEdit!!.copy(
                                voucherType = type,
                                amount = java.math.BigDecimal.valueOf(amount),
                                currency = curr,
                                partyName = party,
                                retailerId = retailerId,
                                category = cat,
                                paymentMethod = method,
                                description = desc
                            )
                        ) {
                            showAddDialog = false
                            voucherToEdit = null
                            voucherToClone = null
                            Toast.makeText(context, "تم تعديل السند المالي وإعادة ضبط الرصيد بنجاح ✓", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        viewModel.createVoucher(
                            voucherType = type,
                            amount = amount,
                            currency = curr,
                            partyName = party,
                            retailerId = retailerId,
                            category = cat,
                            paymentMethod = method,
                            description = desc
                        ) {
                            showAddDialog = false
                            voucherToClone = null
                            voucherToEdit = null
                            Toast.makeText(context, "تم إصدار السند المالي بنجاح ✓", Toast.LENGTH_SHORT).show()

                            if (shareWhatsApp) {
                                val r = retailers.firstOrNull { it.id == retailerId }
                                val tempVoucher = FinancialVoucherEntity(
                                    voucherNumber = "NEW",
                                    voucherType = type,
                                    amount = java.math.BigDecimal.valueOf(amount),
                                    currency = curr,
                                    partyName = party,
                                    retailerId = retailerId,
                                    category = cat,
                                    paymentMethod = method,
                                    description = desc
                                )
                                val msg = WhatsAppHelper.generateVoucherMessage(tempVoucher, r?.phone, r?.balanceOwed?.toDouble())
                                if (r != null && r.phone.isNotBlank()) {
                                    WhatsAppHelper.sendWhatsAppMessage(context, r.phone, msg)
                                } else {
                                    WhatsAppHelper.shareTextIntent(context, msg, "مشاركة السند المالي عبر الواتساب")
                                }
                            }
                        }
                    }
                }
            )
        }

        // Voucher Print & Share Slip Modal
        if (voucherToPreview != null) {
            val previewRetailer = retailers.firstOrNull { it.id == voucherToPreview?.retailerId }
            VoucherSlipModal(
                voucher = voucherToPreview!!,
                retailer = previewRetailer,
                onDismiss = { voucherToPreview = null },
                onShare = { shareText ->
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "مشاركة السند المالي"))
                }
            )
        }

        // Void Voucher Dialog
        if (voucherToVoid != null) {
            AlertDialog(
                onDismissRequest = { voucherToVoid = null },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = PaymentRed, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تأكيد إلغاء السند المالي", fontWeight = FontWeight.Bold, color = PaymentRed, fontFamily = CairoFontFamily)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "هل أنت متأكد من إلغاء السند رقم (${voucherToVoid?.voucherNumber}) بقيمة (${voucherToVoid?.amount?.toInt()} ريال)؟",
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "سيتم إنشاء قيد يومية عكسي (Reversal Journal Entry) تلقائياً لإلغاء التأثير المالي للسند وإعادة ضبط رصيد الحساب المربوط فوراً.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = CairoFontFamily
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            voucherToVoid?.let { v ->
                                viewModel.voidVoucher(v) {
                                    Toast.makeText(context, "تم إلغاء السند وإعادة ضبط الحساب بنجاح ✓", Toast.LENGTH_SHORT).show()
                                }
                            }
                            voucherToVoid = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PaymentRed)
                    ) {
                        Text("تأكيد الإلغاء (Void)", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = CairoFontFamily)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { voucherToVoid = null }) {
                        Text("تراجع", fontFamily = CairoFontFamily)
                    }
                }
            )
        }

        // Delete Dialog
        if (voucherToDelete != null) {
            AlertDialog(
                onDismissRequest = { voucherToDelete = null },
                title = { Text("تأكيد حذف السند نهائياً", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("هل أنت متأكد من حذف السند (${voucherToDelete?.voucherNumber}) نهائياً؟", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold)
                        Text("سيتم حذف السند وإزالة قيوده ومزامنة حذفه سحابياً لمنع عودته أبداً.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = CairoFontFamily)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            voucherToDelete?.let { v ->
                                viewModel.deleteVoucher(v) {
                                    Toast.makeText(context, "تم حذف السند نهائياً وتنظيف السجلات ✓", Toast.LENGTH_SHORT).show()
                                }
                            }
                            voucherToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PaymentRed)
                    ) {
                        Text("تأكيد الحذف", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { voucherToDelete = null }) {
                        Text("إلغاء", fontFamily = CairoFontFamily)
                    }
                }
            )
        }
    }
}

@Composable
fun VoucherSummaryCard(
    title: String,
    amount: Double,
    currency: String = "YER",
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
        border = BorderStroke(1.dp, CyberBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = title,
                    fontFamily = CairoFontFamily,
                    fontSize = 11.sp,
                    color = TextSecondaryDark,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = amount.toCleanCurrency(currency),
                fontFamily = CairoFontFamily,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                softWrap = false,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun VoucherItemCard(
    voucher: FinancialVoucherEntity,
    onEdit: (() -> Unit)? = null,
    onClone: () -> Unit,
    onPreview: () -> Unit,
    onWhatsAppShare: () -> Unit,
    onVoid: () -> Unit,
    onDelete: () -> Unit
) {
    val isReceipt = voucher.voucherType == "RECEIPT"
    val isVoided = voucher.isVoided
    val badgeColor = if (isVoided) PaymentRed else (if (isReceipt) ReceiptGreen else PaymentRed)
    val badgeText = if (isVoided) "⛔ ملغى / VOID" else (if (isReceipt) "سند قبض" else "سند صرف")
    val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(voucher.dateMillis))

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (isVoided) CyberDarkSurface.copy(alpha = 0.7f) else CyberDarkSurface),
        border = BorderStroke(1.dp, if (isVoided) PaymentRed.copy(alpha = 0.5f) else CyberBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth().testTag("voucher_card_${voucher.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Voucher Number, Type Badge, Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeColor.copy(alpha = 0.15f))
                            .border(1.dp, badgeColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = badgeColor,
                            fontFamily = CairoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = voucher.voucherNumber,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray
                    )
                }

                Text(
                    text = voucher.amount.toCleanCurrency(voucher.currency),
                    fontFamily = CairoFontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isVoided) Color.Gray else badgeColor,
                    maxLines = 1,
                    softWrap = false,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    style = if (isVoided) androidx.compose.ui.text.TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough) else androidx.compose.ui.text.TextStyle.Default
                )
            }

            if (isVoided) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(PaymentRed.copy(alpha = 0.15f))
                        .border(1.dp, PaymentRed.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⛔ تم إلغاء هذا السند وإجراء قيد عكسي لتصفية الأثر المالي",
                        color = PaymentRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = CairoFontFamily
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Party Name
            Text(
                text = if (isReceipt) "استلمنا من: ${voucher.partyName}" else "صرفنا إلى: ${voucher.partyName}",
                fontFamily = CairoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.5.sp,
                color = if (isVoided) Color.LightGray else Color.White
            )

            // Category & Description
            if (voucher.description.isNotEmpty()) {
                Text(
                    text = "${voucher.category} • ${voucher.description}",
                    fontFamily = CairoFontFamily,
                    fontSize = 12.sp,
                    color = TextSecondaryDark
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Footer info and action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$dateStr | ${voucher.issuerName}",
                    fontSize = 10.sp,
                    color = Color.Gray
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (onEdit != null && !isVoided) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "تعديل السند", tint = MikroTikCyan, modifier = Modifier.size(15.dp))
                        }
                    }

                    IconButton(onClick = onClone, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "استنساخ السند", tint = InvestmentGold, modifier = Modifier.size(15.dp))
                    }

                    Button(
                        onClick = onWhatsAppShare,
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = "واتساب", tint = WhatsAppDarkGreen, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("واتساب", color = WhatsAppDarkGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onPreview,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, tint = MikroTikPrimary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("معاينة", color = MikroTikPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    if (!isVoided) {
                        Button(
                            onClick = onVoid,
                            colors = ButtonDefaults.buttonColors(containerColor = PaymentRed.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "إلغاء السند", tint = PaymentRed, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إلغاء", color = PaymentRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVoucherDialog(
    retailers: List<com.example.data.local.entity.RetailerEntity>,
    voucherToClone: FinancialVoucherEntity? = null,
    voucherToEdit: FinancialVoucherEntity? = null,
    onDismiss: () -> Unit,
    onSave: (type: String, amount: Double, currency: String, partyName: String, retailerId: Long?, category: String, paymentMethod: String, description: String, shareWhatsApp: Boolean) -> Unit
) {
    val targetVoucher = voucherToEdit ?: voucherToClone
    var isSaving by remember { mutableStateOf(false) }
    var voucherType by remember(targetVoucher) { mutableStateOf(targetVoucher?.voucherType ?: "RECEIPT") } // RECEIPT or PAYMENT
    var amountText by remember(targetVoucher) { mutableStateOf(targetVoucher?.let { it.amount.toInt().toString() } ?: "") }
    var selectedCurrency by remember(targetVoucher) { mutableStateOf(targetVoucher?.currency ?: "YER") }
    var partyName by remember(targetVoucher) { mutableStateOf(targetVoucher?.partyName ?: "") }
    var selectedRetailerId by remember(targetVoucher) { mutableStateOf<Long?>(targetVoucher?.retailerId) }
    var category by remember(targetVoucher) { mutableStateOf(targetVoucher?.category ?: "توريد مبيعات كروت") }
    var paymentMethod by remember(targetVoucher) { mutableStateOf(targetVoucher?.paymentMethod ?: "نقداً") }
    var description by remember(targetVoucher) { mutableStateOf(targetVoucher?.description ?: "") }
    var shareViaWhatsApp by remember { mutableStateOf(false) }

    var isRetailerDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (voucherToEdit != null) "تعديل السند المالي" else if (voucherToClone != null) "استنساخ سند مالي وتعديله" else if (voucherType == "RECEIPT") "إصدار سند قبض مالي (توريد)" else "إصدار سند صرف مالي (مصروفات)",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Type Selector (Receipt vs Payment)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                voucherType = "RECEIPT"
                                category = "توريد مبيعات كروت"
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (voucherType == "RECEIPT") ReceiptGreen else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("سند قبض", color = if (voucherType == "RECEIPT") Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Button(
                            onClick = {
                                voucherType = "PAYMENT"
                                category = "اشتراك نت رئيسي"
                                selectedRetailerId = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (voucherType == "PAYMENT") PaymentRed else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("سند صرف", color = if (voucherType == "PAYMENT") Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Currency Selector
                item {
                    Column {
                        Text(
                            text = "عملة السند المالي:",
                            fontFamily = CairoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("YER" to "ريال يمني (ر.ي)", "USD" to "دولار ($)", "SAR" to "سعودي (ر.س)").forEach { (currCode, currLabel) ->
                                val isSel = selectedCurrency == currCode
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) MikroTikPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, if (isSel) MikroTikCyan else CyberBorder),
                                    onClick = { selectedCurrency = currCode },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = currLabel,
                                        fontFamily = CairoFontFamily,
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Amount
                item {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("المبلغ (${CurrencyHelper.getCurrencySymbol(selectedCurrency)}) *") },
                        placeholder = { Text("مثال: 50000") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("voucher_amount_input")
                    )
                }

                // Select from Retailers (Optional for receipts)
                if (voucherType == "RECEIPT" && retailers.isNotEmpty()) {
                    item {
                        ExposedDropdownMenuBox(
                            expanded = isRetailerDropdownExpanded,
                            onExpandedChange = { isRetailerDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = retailers.firstOrNull { it.id == selectedRetailerId }?.name ?: "اختيار بقالة مسجلة (اختياري)",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("تحديد البقالة لتخفيض رصيدها تلقائياً") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isRetailerDropdownExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = isRetailerDropdownExpanded,
                                onDismissRequest = { isRetailerDropdownExpanded = false }
                            ) {
                                retailers.forEach { r ->
                                    DropdownMenuItem(
                                        text = { Text("${r.name} (دين: ${r.balanceOwed.toInt()} ريال)") },
                                        onClick = {
                                            selectedRetailerId = r.id
                                            partyName = r.name
                                            if (r.phone.isNotBlank()) {
                                                shareViaWhatsApp = true
                                            }
                                            isRetailerDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Party Name (Custom or autofilled)
                item {
                    OutlinedTextField(
                        value = partyName,
                        onValueChange = { partyName = it },
                        label = { Text(if (voucherType == "RECEIPT") "استلمنا من الأخ / الجهة *" else "صرفنا إلى الأخ / الجهة *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("voucher_party_input")
                    )
                }

                // Category
                item {
                    Column {
                        Text(
                            text = if (voucherType == "RECEIPT") "تصنيف القبض والتوريد:" else "بند المصروف التشغيلي (Expense Category):",
                            fontFamily = CairoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        val expenseCategories = if (voucherType == "RECEIPT") {
                            listOf("توريد مبيعات كروت", "سداد آجل بقالة", "تأمين أجهزة", "إيرادات أخرى")
                        } else {
                            listOf("إنترنت شامل", "صيانة أبراج", "كهرباء/ديزل", "أجور عمالة", "أخرى")
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(expenseCategories) { cat ->
                                val isSel = category == cat
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) (if (voucherType == "RECEIPT") ReceiptGreen else PaymentRed) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, if (isSel) Color.White else CyberBorder),
                                    onClick = { category = cat },
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Text(
                                        text = cat,
                                        fontFamily = CairoFontFamily,
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("تخصيص تصنيف المصروف / البند") },
                            placeholder = { Text(if (voucherType == "RECEIPT") "توريد كروت، تأمين..." else "إنترنت شامل، ديزل، صيانة...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Payment Method
                item {
                    OutlinedTextField(
                        value = paymentMethod,
                        onValueChange = { paymentMethod = it },
                        label = { Text("طريقة الدفع (نقداً / حوالة / بنك)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Description
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("البيان والتفاصيل") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // خيار إرسال السند للواتساب بتذييل طلقة نت
                item {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = WhatsAppGreen.copy(alpha = 0.1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, WhatsAppGreen.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { shareViaWhatsApp = !shareViaWhatsApp }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = shareViaWhatsApp,
                                onCheckedChange = { shareViaWhatsApp = it },
                                colors = CheckboxDefaults.colors(checkedColor = WhatsAppDarkGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "مشاركة السند فوراً عبر الواتساب",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = WhatsAppDarkGreen
                                )
                                Text(
                                    text = "تذييل تلقائي معتمد باسم (${WhatsAppHelper.NETWORK_BRAND_NAME})",
                                    fontSize = 10.sp,
                                    color = Color(0xFF1F2937)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (!isSaving && amt > 0 && partyName.isNotBlank()) {
                        isSaving = true
                        onSave(voucherType, amt, selectedCurrency, partyName.trim(), selectedRetailerId, category.trim(), paymentMethod.trim(), description.trim(), shareViaWhatsApp)
                    }
                },
                enabled = !isSaving && amountText.toDoubleOrNull() != null && partyName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (shareViaWhatsApp) WhatsAppDarkGreen else (if (voucherType == "RECEIPT") ReceiptGreen else PaymentRed)
                )
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("جاري الحفظ...", color = Color.White, fontWeight = FontWeight.Bold)
                } else if (shareViaWhatsApp) {
                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إصدار ومشاركة واتساب", color = Color.White, fontWeight = FontWeight.Bold)
                } else {
                    Text(if (voucherToEdit != null) "حفظ التعديلات" else if (voucherToClone != null) "حفظ السند المستنسخ" else "إصدار السند", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun VoucherSlipModal(
    voucher: FinancialVoucherEntity,
    retailer: RetailerEntity? = null,
    onDismiss: () -> Unit,
    onShare: (String) -> Unit
) {
    val context = LocalContext.current
    val isReceipt = voucher.voucherType == "RECEIPT"
    val isVoided = voucher.isVoided
    val title = if (isVoided) "سند مالي ملغى (VOID)" else if (isReceipt) "سند قبض مالي رسمي" else "سند صرف مالي رسمي"
    val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(voucher.dateMillis))

    val shareText = WhatsAppHelper.generateVoucherMessage(voucher, retailer?.phone, retailer?.balanceOwed?.toDouble())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Surface(
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isVoided) PaymentRed else Color(0xFFCBD5E1)),
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = WhatsAppHelper.NETWORK_BRAND_NAME, fontWeight = FontWeight.Bold, color = MikroTikNavy, fontSize = 14.sp)
                        Text(text = voucher.voucherNumber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = MikroTikPrimary, fontSize = 12.sp)
                    }
                    Text(text = "التاريخ: $dateStr", fontSize = 11.sp, color = Color.Gray)

                    if (isVoided) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(PaymentRed.copy(alpha = 0.15f))
                                .border(1.dp, PaymentRed, RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "⛔ ملغى / VOID - لا يُعتد بهذا السند مالياً",
                                fontWeight = FontWeight.ExtraBold,
                                color = PaymentRed,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE2E8F0)))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Amount box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isVoided) Color.Gray.copy(alpha = 0.15f) else (if (isReceipt) ReceiptGreen.copy(alpha = 0.1f) else PaymentRed.copy(alpha = 0.1f)))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "المبلغ: ${voucher.amount.toCleanCurrency(voucher.currency)} فقط لا غير",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isVoided) Color.Gray else (if (isReceipt) ReceiptGreen else PaymentRed)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isReceipt) "وصلنا من الأخ/السيد:" else "صرفنا للأخ/السيد:",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Text(text = voucher.partyName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = "وذلك عن:", fontSize = 11.sp, color = Color.Gray)
                    Text(text = "${voucher.category} - ${voucher.description}", fontSize = 13.sp, color = Color.DarkGray)

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "طريقة الدفع: ${voucher.paymentMethod}", fontSize = 12.sp, color = Color.DarkGray)

                    if (retailer != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("إجمالي الرصيد التراكمي المتبقي:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MikroTikNavy)
                            Text(retailer.balanceOwed.toCleanCurrency("YER"), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = PaymentRed)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Signatures
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "المستلم", fontSize = 11.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(18.dp))
                            Text(text = ".......................", fontSize = 10.sp, color = Color.Gray)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "أمين الصندوق / الإدارة", fontSize = 11.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = voucher.issuerName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MikroTikNavy)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        if (retailer != null && retailer.phone.isNotBlank()) {
                            WhatsAppHelper.sendWhatsAppMessage(context, retailer.phone, shareText)
                        } else {
                            WhatsAppHelper.shareTextIntent(context, shareText, "مشاركة السند عبر الواتساب")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppDarkGreen)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("واتساب (طلقة نت)")
                }

                Button(
                    onClick = { onShare(shareText) },
                    colors = ButtonDefaults.buttonColors(containerColor = MikroTikPrimary)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("مشاركة")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}
