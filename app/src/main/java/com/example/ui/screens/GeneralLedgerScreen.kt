package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.model.GeneralLedgerItem
import com.example.data.local.model.GeneralLedgerReport
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * شاشة دفتر الأستاذ العام المعياري (General Ledger Screen)
 * مع احتساب الرصيد الافتتاحي وعرض أسطر الحركات والرصيد التراكمي الفعلي بعد كل حركة
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralLedgerScreen(
    viewModel: MainViewModel,
    initialAccountCode: String = "1101",
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val df = remember { DecimalFormat("#,##0.00") }
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("ar")) }

    // شجرة الحسابات للاختيار منها
    val chartOfAccounts by viewModel.chartOfAccounts.collectAsState()
    var selectedAccountCode by remember(initialAccountCode) { mutableStateOf(initialAccountCode) }

    // خيارات النطاق الزمني
    var selectedPeriodIndex by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val periodOptions = listOf("كامل المدة", "هذا الشهر", "الربع الحالي", "هذا العام")

    val dateRange = remember(selectedPeriodIndex) {
        val cal = Calendar.getInstance()
        val now = cal.timeInMillis
        when (selectedPeriodIndex) {
            1 -> { // هذا الشهر
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                Pair(cal.timeInMillis, now)
            }
            2 -> { // الربع الحالي
                cal.add(Calendar.MONTH, -3)
                Pair(cal.timeInMillis, now)
            }
            3 -> { // هذا العام
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                Pair(cal.timeInMillis, now)
            }
            else -> Pair(0L, Long.MAX_VALUE)
        }
    }

    // جلب تقرير دفتر الأستاذ للحساب المحدد
    val ledgerReport by viewModel.getGeneralLedgerReport(
        accountCode = selectedAccountCode,
        startDate = dateRange.first,
        endDate = dateRange.second
    ).collectAsState(initial = null)

    var searchQuery by remember { mutableStateOf("") }

    val filteredLines = remember(ledgerReport, searchQuery) {
        val lines = ledgerReport?.lines ?: emptyList()
        if (searchQuery.isBlank()) {
            lines
        } else {
            val q = searchQuery.trim().lowercase()
            lines.filter {
                it.entryNumber.lowercase().contains(q) ||
                it.description.lowercase().contains(q) ||
                it.referenceType.lowercase().contains(q) ||
                it.referenceId.lowercase().contains(q)
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("general_ledger_screen"),
        containerColor = CyberDarkSurface,
        topBar = {
            if (onNavigateBack != null) {
                TopAppBar(
                    title = {
                        Text(
                            text = "دفتر الأستاذ العام (General Ledger)",
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = CyberDarkCardElevated)
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // العنوان ومشاركة التقرير
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "دفتر الأستاذ العام 📖",
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = "سجل تفصيلي لحركات الحساب مع الرصيد الافتتاحي والتراكمي",
                            fontFamily = CairoFontFamily,
                            fontSize = 11.5.sp,
                            color = TextSecondaryDark
                        )
                    }

                    IconButton(
                        onClick = {
                            val r = ledgerReport
                            if (r != null) {
                                val text = buildString {
                                    appendLine("=== دفتر الأستاذ العام ===")
                                    appendLine("الحساب: ${r.accountCode} - ${r.accountName} (${r.normalBalance})")
                                    appendLine("الفترة: ${periodOptions[selectedPeriodIndex]}")
                                    appendLine("الرصيد الافتتاحي: ${df.format(r.openingBalance)} ر.ي")
                                    appendLine("إجمالي حركة المدين: ${df.format(r.totalPeriodDebit)} ر.ي")
                                    appendLine("إجمالي حركة الدائن: ${df.format(r.totalPeriodCredit)} ر.ي")
                                    appendLine("الرصيد الختامي: ${df.format(r.closingBalance)} ر.ي")
                                    appendLine("---------------------------------------")
                                    r.lines.forEach {
                                        appendLine("${dateFormat.format(Date(it.dateMillis))} | ${it.entryNumber} | ${it.description} | مدين: ${df.format(it.debit)} | دائن: ${df.format(it.credit)} | الرصيد: ${df.format(it.runningBalance)}")
                                    }
                                }
                                clipboardManager.setText(AnnotatedString(text))
                                Toast.makeText(context, "تم نسخ تقرير دفتر الأستاذ إلى الحافظة ✓", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "مشاركة", tint = MikroTikCyan)
                    }
                }
            }

            // شريط اختيار الحساب (1101 إلى 5203)
            item {
                Text(
                    text = "اختر الحساب المحاسبي:",
                    fontFamily = CairoFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MikroTikCyan
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val availableAccounts = if (chartOfAccounts.isNotEmpty()) chartOfAccounts else com.example.data.local.dao.ChartOfAccountsDao.DEFAULT_STANDARD_CHART
                    items(availableAccounts) { acc ->
                        val isSelected = acc.accountCode == selectedAccountCode
                        val catColor = when {
                            acc.accountCode.startsWith("1") -> ReceiptGreen
                            acc.accountCode.startsWith("2") -> PaymentRed
                            acc.accountCode.startsWith("3") -> EquityBlue
                            acc.accountCode.startsWith("4") -> InvestmentGold
                            acc.accountCode.startsWith("5") -> AssetPurple
                            else -> MikroTikCyan
                        }

                        Surface(
                            modifier = Modifier.clickable { selectedAccountCode = acc.accountCode },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) catColor.copy(alpha = 0.25f) else CyberDarkCardElevated,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) catColor else CyberBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = acc.accountCode,
                                    fontFamily = CairoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isSelected) catColor else TextSecondaryDark
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = acc.accountNameAr,
                                    fontFamily = CairoFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else TextSecondaryDark
                                )
                            }
                        }
                    }
                }
            }

            // فلاتر النطاق الزمني
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    periodOptions.forEachIndexed { index, option ->
                        val isSelected = selectedPeriodIndex == index
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedPeriodIndex = index },
                            label = {
                                Text(
                                    text = option,
                                    fontFamily = CairoFontFamily,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MikroTikCyan,
                                selectedLabelColor = Color.Black,
                                containerColor = CyberDarkCardElevated,
                                labelColor = TextSecondaryDark
                            )
                        )
                    }
                }
            }

            // بطاقة ملخص الحساب: الرصيد الافتتاحي والختامي وإجمالي المدين والدائن
            item {
                val report = ledgerReport
                if (report != null) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${report.accountCode} - ${report.accountName}",
                                        fontFamily = CairoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "طبيعة الحساب: ${if (report.normalBalance == "DEBIT") "مدين بطبيعته (DEBIT)" else "دائن بطبيعته (CREDIT)"}",
                                        fontFamily = CairoFontFamily,
                                        fontSize = 11.sp,
                                        color = TextSecondaryDark
                                    )
                                }

                                Surface(
                                    color = MikroTikCyan.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = report.accountType,
                                        fontFamily = CairoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = MikroTikCyan,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = CyberBorder, thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("الرصيد الافتتاحي", fontFamily = CairoFontFamily, fontSize = 11.sp, color = TextSecondaryDark)
                                    Text("${df.format(report.openingBalance)} ر.ي", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                }
                                Column {
                                    Text("إجمالي المدين (+)", fontFamily = CairoFontFamily, fontSize = 11.sp, color = TextSecondaryDark)
                                    Text("${df.format(report.totalPeriodDebit)} ر.ي", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ReceiptGreen)
                                }
                                Column {
                                    Text("إجمالي الدائن (-)", fontFamily = CairoFontFamily, fontSize = 11.sp, color = TextSecondaryDark)
                                    Text("${df.format(report.totalPeriodCredit)} ر.ي", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF38BDF8))
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("الرصيد الختامي", fontFamily = CairoFontFamily, fontSize = 11.sp, color = TextSecondaryDark)
                                    Text(
                                        "${df.format(report.closingBalance)} ر.ي",
                                        fontFamily = CairoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (report.closingBalance >= BigDecimal.ZERO) ProfitEmerald else PaymentRed
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // حقل البحث في الحركات
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text("بحث برقم القيد أو البيان أو المرجع...", fontFamily = CairoFontFamily, fontSize = 12.sp, color = TextSecondaryDark)
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MikroTikCyan, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "مسح", tint = TextSecondaryDark, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MikroTikCyan,
                        unfocusedBorderColor = CyberBorder,
                        focusedContainerColor = CyberDarkCardElevated,
                        unfocusedContainerColor = CyberDarkCardElevated,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // أسطر الحركات في دفتر الأستاذ
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الحركات التفصيلية (${filteredLines.size}):",
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }
            }

            if (filteredLines.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = TextSecondaryDark, modifier = Modifier.size(44.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "لا توجد حركات مقيدة لهذا الحساب في الفترة المحددة",
                                fontFamily = CairoFontFamily,
                                color = TextSecondaryDark,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            } else {
                items(filteredLines, key = { it.lineId }) { item ->
                    GeneralLedgerMovementCard(item = item, df = df, dateFormat = dateFormat)
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun GeneralLedgerMovementCard(
    item: GeneralLedgerItem,
    df: DecimalFormat,
    dateFormat: SimpleDateFormat
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ledger_item_${item.lineId}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
        border = androidx.compose.foundation.BorderStroke(0.8.dp, CyberBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // الصف العلوي: رقم القيد والتاريخ
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MikroTikCyan.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = item.entryNumber,
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            color = MikroTikCyan,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (item.referenceType.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = item.referenceType,
                            fontFamily = CairoFontFamily,
                            fontSize = 10.sp,
                            color = TextSecondaryDark
                        )
                    }
                }

                Text(
                    text = dateFormat.format(Date(item.dateMillis)),
                    fontFamily = CairoFontFamily,
                    fontSize = 11.sp,
                    color = TextSecondaryDark
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // البيان والوصف
            Text(
                text = item.description.ifBlank { "قيد محاسبي" },
                fontFamily = CairoFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.5.sp,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            // القيم: مدين / دائن / الرصيد التراكمي بعد الحركة
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberDarkSurface.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.debit > BigDecimal.ZERO) {
                    Column {
                        Text("مدين (Debit)", fontFamily = CairoFontFamily, fontSize = 10.sp, color = TextSecondaryDark)
                        Text("+${df.format(item.debit)}", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ReceiptGreen)
                    }
                } else {
                    Column {
                        Text("مدين (Debit)", fontFamily = CairoFontFamily, fontSize = 10.sp, color = TextSecondaryDark)
                        Text("-", fontFamily = CairoFontFamily, fontSize = 12.sp, color = TextSecondaryDark)
                    }
                }

                if (item.credit > BigDecimal.ZERO) {
                    Column {
                        Text("دائن (Credit)", fontFamily = CairoFontFamily, fontSize = 10.sp, color = TextSecondaryDark)
                        Text("-${df.format(item.credit)}", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF38BDF8))
                    }
                } else {
                    Column {
                        Text("دائن (Credit)", fontFamily = CairoFontFamily, fontSize = 10.sp, color = TextSecondaryDark)
                        Text("-", fontFamily = CairoFontFamily, fontSize = 12.sp, color = TextSecondaryDark)
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("الرصيد التراكمي", fontFamily = CairoFontFamily, fontSize = 10.sp, color = TextSecondaryDark)
                    Text(
                        "${df.format(item.runningBalance)} ر.ي",
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        color = ProfitEmerald
                    )
                }
            }
        }
    }
}
