package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CardSalesInvoiceEntity
import com.example.data.model.CardSalesInvoiceItem
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import com.example.util.InvoicePdfManager
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * شاشة وقائمة فواتير مبيعات الكروت المحاسبية الاحترافية - تصميم 2026 العصري
 * - عرض الفواتير كقائمة مدمجة عالية الانسيابية والأناقة
 * - سرعة وسهولة التمرير والتصفح مع رؤية واضحة لعشرات الفواتير في شاشة واحدة
 * - شريط إحصائي تنفيذي مدمج لا يستهلك مساحة الشاشة
 * - بحث فوري وفلاتر سريعة بنقرة واحدة
 * - قائمة إجراءات سريعة منبثقة لكل فاتورة (PDF، واتساب، استنساخ، تعديل، حذف)
 */
@Composable
fun CardSalesInvoicesSubScreen(
    viewModel: MainViewModel,
    onOpenCreateInvoice: () -> Unit,
    onEditInvoice: ((CardSalesInvoiceEntity) -> Unit)? = null,
    onCloneInvoice: ((CardSalesInvoiceEntity) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val rawInvoices by viewModel.salesInvoices.collectAsState()
    val retailers by viewModel.retailers.collectAsState()

    // استبعاد الفواتير التلقائية للتسليم الداخلي وترتيبها بالأحدث أولاً
    val invoices = remember(rawInvoices) {
        rawInvoices
            .filter { !it.invoiceNumber.startsWith("INV-DELIV-") }
            .sortedByDescending { it.invoiceDateMillis }
    }

    // مؤشرات KPI المالية العامة
    val totalSalesAmount = remember(invoices) { invoices.fold(java.math.BigDecimal.ZERO) { acc, inv -> acc.add(inv.totalAmount) } }
    val totalPaidAmount = remember(invoices) { invoices.fold(java.math.BigDecimal.ZERO) { acc, inv -> acc.add(inv.paidAmount) } }
    val totalCreditRemaining = remember(invoices) { invoices.fold(java.math.BigDecimal.ZERO) { acc, inv -> acc.add(inv.remainingAmount) } }
    val totalSoldCards = remember(invoices) { invoices.sumOf { it.totalCardsCount } }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, CREDIT, CASH, PARTIAL
    var selectedInvoiceForDetail by remember { mutableStateOf<CardSalesInvoiceEntity?>(null) }
    var invoiceToDelete by remember { mutableStateOf<CardSalesInvoiceEntity?>(null) }
    var invoiceForCloneChoice by remember { mutableStateOf<CardSalesInvoiceEntity?>(null) }
    var showSalesJsonDialog by rememberSaveable { mutableStateOf(false) }
    var isCloningInstant by remember { mutableStateOf(false) }

    // تتبع الفاتورة المنشأة أو المستنسخة حديثاً لتمييزها وتمرير الشاشة إليها
    var newlyCreatedInvoiceId by remember { mutableStateOf<Long?>(null) }
    var newlyClonedNumberBanner by remember { mutableStateOf<String?>(null) }
    var expandedInvoiceId by remember { mutableStateOf<Long?>(null) }

    // إحصاءات الفلاتر
    val creditCount = remember(invoices) { invoices.count { it.paymentType == "CREDIT" || it.remainingAmount.compareTo(java.math.BigDecimal.ZERO) > 0 } }
    val cashCount = remember(invoices) { invoices.count { it.paymentType == "CASH" && it.remainingAmount.compareTo(java.math.BigDecimal.ZERO) <= 0 } }
    val partialCount = remember(invoices) { invoices.count { it.paymentType == "PARTIAL" } }

    val filteredInvoices = remember(invoices, searchQuery, selectedFilter) {
        invoices.filter { inv ->
            val matchesSearch = searchQuery.isBlank() ||
                    inv.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                    inv.customerName.contains(searchQuery, ignoreCase = true) ||
                    inv.customerPhone.contains(searchQuery, ignoreCase = true) ||
                    inv.itemsSummary.contains(searchQuery, ignoreCase = true) ||
                    inv.notes.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "CREDIT" -> inv.paymentType == "CREDIT" || inv.remainingAmount.compareTo(java.math.BigDecimal.ZERO) > 0
                "CASH" -> inv.paymentType == "CASH" && inv.remainingAmount.compareTo(java.math.BigDecimal.ZERO) <= 0
                "PARTIAL" -> inv.paymentType == "PARTIAL"
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("card_sales_invoices_sub_screen")
    ) {
        // 1. شريط إحصائي تنفيذي فائق الضغط والأناقة (Compact Financial Strip) - لا يستهلك مساحة
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = CyberDarkSurface,
            border = BorderStroke(0.8.dp, CyberBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // مبيعات
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("المبيعات: ", fontFamily = CairoFontFamily, fontSize = 10.sp, color = TextSecondaryDark)
                    Text("${totalSalesAmount.setScale(0, java.math.RoundingMode.HALF_UP)} ر.ي", fontFamily = CairoFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                }
                // المحصل
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Payments, contentDescription = null, tint = ProfitEmerald, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("المحصل: ", fontFamily = CairoFontFamily, fontSize = 10.sp, color = TextSecondaryDark)
                    Text("${totalPaidAmount.setScale(0, java.math.RoundingMode.HALF_UP)} ر.ي", fontFamily = CairoFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ProfitEmerald)
                }
                // الآجل
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = if (totalCreditRemaining.compareTo(java.math.BigDecimal.ZERO) > 0) Color(0xFFEF4444) else ProfitEmerald, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("الآجل: ", fontFamily = CairoFontFamily, fontSize = 10.sp, color = TextSecondaryDark)
                    Text("${totalCreditRemaining.setScale(0, java.math.RoundingMode.HALF_UP)} ر.ي", fontFamily = CairoFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (totalCreditRemaining.compareTo(java.math.BigDecimal.ZERO) > 0) Color(0xFFEF4444) else ProfitEmerald)
                }
                // الكروت
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = InvestmentGold, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("$totalSoldCards كرت", fontFamily = CairoFontFamily, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = InvestmentGold)
                }
            }
        }

        // إشعار النجاح عند استنساخ الفاتورة فوراً
        newlyClonedNumberBanner?.let { bannerMessage ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF064E3B),
                border = BorderStroke(1.dp, ProfitEmerald),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ProfitEmerald, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = bannerMessage,
                            fontFamily = CairoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    IconButton(
                        onClick = { newlyClonedNumberBanner = null },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White, modifier = Modifier.size(13.dp))
                    }
                }
            }
        }

        // 2. شريط الأكشن والفلترة المتكامل المدمج (Single Unified Action Bar)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // زر إصدار فاتورة جديدة + زر النسخ الاحتياطي
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = onOpenCreateInvoice,
                    colors = ButtonDefaults.buttonColors(containerColor = ProfitEmerald),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("فاتورة جديدة", fontFamily = CairoFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }

                OutlinedButton(
                    onClick = { showSalesJsonDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MikroTikCyan),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(13.dp), tint = MikroTikCyan)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("JSON 📁", fontFamily = CairoFontFamily, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = MikroTikCyan)
                }
            }

            // فلاتر الحالة السريعة
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompactFilterChip(
                    title = "الكل",
                    count = invoices.size,
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" }
                )
                CompactFilterChip(
                    title = "آجلة",
                    count = creditCount,
                    selected = selectedFilter == "CREDIT",
                    color = Color(0xFFEF4444),
                    onClick = { selectedFilter = "CREDIT" }
                )
                CompactFilterChip(
                    title = "نقدية",
                    count = cashCount,
                    selected = selectedFilter == "CASH",
                    color = ProfitEmerald,
                    onClick = { selectedFilter = "CASH" }
                )
                CompactFilterChip(
                    title = "جزئية",
                    count = partialCount,
                    selected = selectedFilter == "PARTIAL",
                    color = StatusWarning,
                    onClick = { selectedFilter = "PARTIAL" }
                )
            }

            // أزرار البحث والتدقيق
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { isSearchExpanded = !isSearchExpanded },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "بحث",
                        tint = if (searchQuery.isNotBlank() || isSearchExpanded) Color(0xFF38BDF8) else TextSecondaryDark,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = {
                        viewModel.reconcileAccountingLedger {
                            Toast.makeText(context, "تم تدقيق ومطابقة فواتير المبيعات مع دفاتر البقالات بنجاح 100% ✓", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "تدقيق", tint = Color(0xFF38BDF8), modifier = Modifier.size(17.dp))
                }
            }
        }

        // حقل البحث القابل للتوسيع (يظهر فقط عند النقر على أيقونة البحث أو وجود نص بحث)
        AnimatedVisibility(visible = isSearchExpanded || searchQuery.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text("بحث باسم العميل، الرقم، الهاتف، الملاحظات...", fontFamily = CairoFontFamily, fontSize = 11.sp, color = TextSecondaryDark)
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                    },
                    trailingIcon = {
                        IconButton(onClick = {
                            searchQuery = ""
                            isSearchExpanded = false
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = TextSecondaryDark, modifier = Modifier.size(15.dp))
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = CyberBorder,
                        focusedContainerColor = CyberDarkSurface,
                        unfocusedContainerColor = CyberDarkSurface,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                )
            }
        }

        // 3. قائمة الفواتير المباشرة والواضحة في واجهة الشاشة
        if (filteredInvoices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(26.dp))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "لا توجد فواتير مطابقة للبحث" else "لا توجد فواتير مبيعات مسجلة حتى الآن",
                        fontFamily = CairoFontFamily,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "جرب كتابة جزء من اسم المحل أو رقم الفاتورة" else "اضغط على زر (فاتورة جديدة) بالأعلى لإصدار فاتورة فوراً",
                        fontFamily = CairoFontFamily,
                        fontSize = 11.sp,
                        color = TextSecondaryDark,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(bottom = 75.dp)
            ) {
                items(filteredInvoices, key = { it.id }) { invoice ->
                    val isRecent = invoice.id == newlyCreatedInvoiceId ||
                            (System.currentTimeMillis() - invoice.invoiceDateMillis < 90_000L)
                    val isExpanded = expandedInvoiceId == invoice.id

                    val matchedRetailer = retailers.firstOrNull { it.id == invoice.retailerId || it.name.trim().equals(invoice.customerName.trim(), ignoreCase = true) }

                    CardSalesInvoiceListItem(
                        invoice = invoice,
                        isHighlighted = isRecent,
                        isExpanded = isExpanded,
                        retailerBalanceOwed = matchedRetailer?.balanceOwed?.toDouble(),
                        onClick = {
                            expandedInvoiceId = if (isExpanded) null else invoice.id
                        },
                        onViewDetail = { selectedInvoiceForDetail = invoice },
                        onEdit = { onEditInvoice?.invoke(invoice) },
                        onClone = { invoiceForCloneChoice = invoice },
                        onDelete = { invoiceToDelete = invoice }
                    )
                }
            }
        }
    }

    // نافذة خيارات الاستنساخ
    invoiceForCloneChoice?.let { sourceInvoice ->
        AlertDialog(
            onDismissRequest = {
                if (!isCloningInstant) invoiceForCloneChoice = null
            },
            icon = {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(ProfitEmerald.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CopyAll, contentDescription = null, tint = ProfitEmerald, modifier = Modifier.size(24.dp))
                }
            },
            title = {
                Text(
                    text = "استنساخ الفاتورة #${sourceInvoice.invoiceNumber}",
                    fontFamily = CairoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "العميل: ${sourceInvoice.customerName} • ${sourceInvoice.totalCardsCount} كرت • ${sourceInvoice.totalAmount.toInt()} ر.ي",
                        fontFamily = CairoFontFamily,
                        fontSize = 11.5.sp,
                        color = TextSecondaryDark,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // استنساخ فوري
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0F2942),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                        onClick = {
                            if (!isCloningInstant) {
                                isCloningInstant = true
                                viewModel.instantCloneSalesInvoice(sourceInvoice) { newInvoiceId ->
                                    isCloningInstant = false
                                    invoiceForCloneChoice = null
                                    if (newInvoiceId > 0) {
                                        newlyCreatedInvoiceId = newInvoiceId
                                        selectedFilter = "ALL"
                                        searchQuery = ""
                                        newlyClonedNumberBanner = "تم استنساخ الفاتورة فوراً بنجاح في أعلى القائمة وتحديث المخزن والحسابات ✓"
                                        coroutineScope.launch {
                                            listState.animateScrollToItem(0)
                                        }
                                        Toast.makeText(context, "تم استنساخ الفاتورة بنجاح في أعلى القائمة ✓", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "تعذر استنساخ الفاتورة!", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isCloningInstant) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF38BDF8), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("استنساخ فوري مباشر برقم جديد", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                Text("إصدار نسخة فورية برقم جديد وتاريخ اللحظة وخصم المخزن تلقائياً", fontFamily = CairoFontFamily, fontSize = 10.5.sp, color = Color(0xFF94A3B8))
                            }
                        }
                    }

                    // استنساخ مع تعديل
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CyberDarkCardElevated,
                        border = BorderStroke(1.dp, CyberBorder),
                        onClick = {
                            val inv = sourceInvoice
                            invoiceForCloneChoice = null
                            onCloneInvoice?.invoke(inv)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.EditNote, contentDescription = null, tint = InvestmentGold, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("تعديل البيانات قبل الاستنساخ", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                Text("فتح نافذة الفاتورة لتغيير الكميات، الأسعار، العميل أو طريقة الدفع", fontFamily = CairoFontFamily, fontSize = 10.5.sp, color = TextSecondaryDark)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { invoiceForCloneChoice = null },
                    enabled = !isCloningInstant
                ) {
                    Text("إلغاء", fontFamily = CairoFontFamily, color = TextSecondaryDark)
                }
            },
            containerColor = CyberDarkSurface
        )
    }

    // نافذة تفاصيل الفاتورة الكاملة
    selectedInvoiceForDetail?.let { inv ->
        CardSalesInvoiceDetailDialog(
            invoice = inv,
            viewModel = viewModel,
            onDismiss = { selectedInvoiceForDetail = null },
            onEditInvoice = { invoiceToEdit ->
                selectedInvoiceForDetail = null
                onEditInvoice?.invoke(invoiceToEdit)
            },
            onCloneInvoice = { invoiceToClone ->
                selectedInvoiceForDetail = null
                invoiceForCloneChoice = invoiceToClone
            },
            onDeleteInvoice = { invoiceToDel ->
                selectedInvoiceForDetail = null
                invoiceToDelete = invoiceToDel
            }
        )
    }

    // نافذة تأكيد حذف الفاتورة واسترجاع المخزن
    invoiceToDelete?.let { inv ->
        AlertDialog(
            onDismissRequest = { invoiceToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تأكيد حذف الفاتورة", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                }
            },
            text = {
                Text(
                    "هل أنت متأكد من رغبتك بحذف الفاتورة #${inv.invoiceNumber} للعميل (${inv.customerName})؟\n\n" +
                            "• سيتم إرجاع كميات الكروت (${inv.totalCardsCount} كرت) إلى المخزن تلقائياً.\n" +
                            "• سيتم إلغاء السند المالي وضبط حساب العميل محاسبياً 100%.",
                    fontFamily = CairoFontFamily,
                    fontSize = 12.5.sp,
                    color = TextSecondaryDark
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toDelete = inv
                        invoiceToDelete = null
                        viewModel.deleteSalesInvoice(toDelete) {
                            Toast.makeText(context, "تم حذف الفاتورة ${toDelete.invoiceNumber} واسترجاع المخزن بنجاح ✓", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("تأكيد الحذف واسترجاع المخزن", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { invoiceToDelete = null }) {
                    Text("إلغاء", fontFamily = CairoFontFamily, color = TextSecondaryDark)
                }
            },
            containerColor = CyberDarkSurface
        )

    if (showSalesJsonDialog) {
        SalesInvoiceJsonBackupDialog(
            viewModel = viewModel,
            onDismissRequest = { showSalesJsonDialog = false }
        )
    }
    }
}

/**
 * عنصر قائمة فواتير مبيعات الكروت المدمج والأنيق - أسلوب 2026 لقوائم المحاسبة
 * - تصميم سطر/بطاقة ذكية بارتفاع مدمج يسمح برؤية 6-8 فواتير على الشاشة
 * - نقرة واحدة لفتح شريط الإجراءات السريعة (PDF, واتساب, استنساخ, تعديل, حذف)
 * - نقرة على زر التفاصيل لفتح المعاينة الشاملة
 */
@Composable
private fun CardSalesInvoiceListItem(
    invoice: CardSalesInvoiceEntity,
    isHighlighted: Boolean = false,
    isExpanded: Boolean = false,
    retailerBalanceOwed: Double? = null,
    onClick: () -> Unit,
    onViewDetail: () -> Unit,
    onEdit: () -> Unit,
    onClone: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    val timeFormatted = remember(invoice.invoiceDateMillis) {
        val sdf = SimpleDateFormat("HH:mm - yyyy/MM/dd", Locale.getDefault())
        sdf.format(Date(invoice.invoiceDateMillis))
    }

    val (badgeText, badgeColor, badgeIcon) = remember(invoice.paymentType, invoice.remainingAmount) {
        when {
            invoice.remainingAmount <= java.math.BigDecimal.ZERO -> Triple("خالص ✓", ProfitEmerald, Icons.Default.CheckCircle)
            invoice.paidAmount <= java.math.BigDecimal.ZERO -> Triple("آجل ⏳", Color(0xFFEF4444), Icons.Default.Schedule)
            else -> Triple("جزئي", StatusWarning, Icons.Default.PriceCheck)
        }
    }

    val parsedItems = remember(invoice.itemsJson) {
        val list = mutableListOf<CardSalesInvoiceItem>()
        if (invoice.itemsJson.isNotBlank()) {
            try {
                val arr = JSONArray(invoice.itemsJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        CardSalesInvoiceItem(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            packageName = obj.optString("packageName", "صنف"),
                            quantity = obj.optInt("quantity", 0),
                            unitPrice = obj.optDouble("unitPrice", 0.0),
                            retailPrice = obj.optDouble("retailPrice", 0.0)
                        )
                    )
                }
            } catch (e: Exception) {
                // ignore
            }
        }
        list
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) Color(0xFF0D2538) else CyberDarkSurface
        ),
        border = BorderStroke(
            if (isHighlighted) 1.5.dp else 0.8.dp,
            if (isHighlighted) ProfitEmerald else CyberBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
            // الصف الأساسي: أيقونة الحالة + اسم العميل والتفاصيل + المبلغ وحالة السداد
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. أيقونة الحالة (أخضر للنقدي، أحمر للآجل، أصفر للجزئي)
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(badgeColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(badgeIcon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(20.dp))
                }

                Spacer(modifier = Modifier.width(10.dp))

                // 2. اسم العميل والتاريخ وتفاصيل الأصناف
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = invoice.customerName,
                            fontFamily = CairoFontFamily,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        // شارة رقم الفاتورة
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF1E293B))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "#${invoice.invoiceNumber}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${invoice.totalCardsCount} كرت",
                            fontFamily = CairoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = InvestmentGold
                        )
                        Text(
                            text = " • $timeFormatted",
                            fontFamily = CairoFontFamily,
                            fontSize = 10.sp,
                            color = TextSecondaryDark
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // 3. المبلغ الإجمالي وحالة السداد
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "${invoice.totalAmount.toInt()} ر.ي",
                        fontFamily = CairoFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeColor.copy(alpha = 0.15f),
                        border = BorderStroke(0.6.dp, badgeColor.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (invoice.remainingAmount > java.math.BigDecimal.ZERO) "آجل ${invoice.remainingAmount.toInt()}" else badgeText,
                                fontFamily = CairoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor
                            )
                        }
                    }
                }

                // سهم التوسيع/الطي
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = TextSecondaryDark,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(start = 4.dp)
                )
            }

            // الصف المنبثق للإجراءات السريعة (يظهر عند النقر على الفاتورة)
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    HorizontalDivider(
                        color = CyberBorder.copy(alpha = 0.5f),
                        thickness = 0.6.dp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // أزرار العمليات
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // تفاصيل
                        CompactActionButton(
                            title = "عرض",
                            icon = Icons.Default.Visibility,
                            color = Color.White,
                            containerColor = Color(0xFF1E293B),
                            onClick = onViewDetail,
                            modifier = Modifier.weight(1f)
                        )

                        // PDF
                        CompactActionButton(
                            title = "PDF",
                            icon = Icons.Default.PictureAsPdf,
                            color = Color(0xFF38BDF8),
                            containerColor = Color(0xFF0F2942),
                            onClick = {
                                InvoicePdfManager.shareInvoicePdf(context, invoice, parsedItems)
                            },
                            modifier = Modifier.weight(1f)
                        )

                        // واتساب
                        CompactActionButton(
                            title = "واتساب",
                            icon = Icons.Default.Share,
                            color = Color(0xFF22C55E),
                            containerColor = Color(0xFF063319),
                            onClick = {
                                shareInvoiceViaWhatsApp(context, invoice, retailerBalanceOwed)
                            },
                            modifier = Modifier.weight(1.1f)
                        )

                        // استنساخ
                        CompactActionButton(
                            title = "استنساخ",
                            icon = Icons.Default.CopyAll,
                            color = InvestmentGold,
                            containerColor = Color(0xFF332608),
                            onClick = onClone,
                            modifier = Modifier.weight(1.2f)
                        )

                        // تعديل
                        CompactActionButton(
                            title = "تعديل",
                            icon = Icons.Default.Edit,
                            color = Color(0xFF60A5FA),
                            containerColor = Color(0xFF132A4A),
                            onClick = onEdit,
                            modifier = Modifier.weight(1f)
                        )

                        // حذف
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF381515))
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = Color(0xFFEF4444), modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * زر عملي سريع ومضغوط
 */
@Composable
private fun CompactActionButton(
    title: String,
    icon: ImageVector,
    color: Color,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(7.dp),
        color = containerColor,
        border = BorderStroke(0.6.dp, color.copy(alpha = 0.4f)),
        onClick = onClick,
        modifier = modifier.height(32.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = title,
                fontFamily = CairoFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

/**
 * كبسولة إحصائية مصغرة في الشريط العلوي
 */
@Composable
private fun CompactStatPill(
    title: String,
    value: String,
    color: Color,
    icon: ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
        }
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(
                text = title,
                fontFamily = CairoFontFamily,
                fontSize = 9.sp,
                color = TextSecondaryDark
            )
            Text(
                text = value,
                fontFamily = CairoFontFamily,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

/**
 * كبسولة فلترة سريعة
 */
@Composable
private fun CompactFilterChip(
    title: String,
    count: Int,
    selected: Boolean,
    color: Color = Color(0xFF38BDF8),
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) color else CyberDarkSurface,
        border = BorderStroke(0.8.dp, if (selected) color else CyberBorder),
        onClick = onClick,
        modifier = Modifier.height(28.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$title ($count)",
                fontFamily = CairoFontFamily,
                fontSize = 10.5.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.Black else TextSecondaryDark
            )
        }
    }
}

/**
 * إرسال ملخص الفاتورة مباشرة عبر تطبيق واتساب
 */
private fun shareInvoiceViaWhatsApp(context: Context, invoice: CardSalesInvoiceEntity, retailerBalanceOwed: Double? = null) {
    try {
        val message = com.example.util.WhatsAppHelper.generateCardSalesInvoiceMessage(invoice, retailerBalanceOwed)
        val cleanPhone = invoice.customerPhone.replace(Regex("[^0-9]"), "")
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = if (cleanPhone.isNotBlank()) {
                val formattedPhone = if (cleanPhone.startsWith("967")) cleanPhone else if (cleanPhone.length == 9) "967$cleanPhone" else cleanPhone
                Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(message)}")
            } else {
                Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(message)}")
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        val message = com.example.util.WhatsAppHelper.generateCardSalesInvoiceMessage(invoice, retailerBalanceOwed)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        context.startActivity(Intent.createChooser(shareIntent, "مشاركة الفاتورة"))
    }
}

private fun isToday(millis: Long): Boolean {
    val cal1 = Calendar.getInstance()
    val cal2 = Calendar.getInstance().apply { timeInMillis = millis }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun isYesterday(millis: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val cal2 = Calendar.getInstance().apply { timeInMillis = millis }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
