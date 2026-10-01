package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.data.local.model.TrialBalanceRow
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import java.math.BigDecimal
import java.text.DecimalFormat
import java.util.Calendar

/**
 * شاشة ميزان المراجعة الموحد المعياري (Standard Trial Balance Screen)
 * قائمة شجرية بكافة الحسابات النشطة مع شريط تدقيق سفلي يثبت التوازن الصارم
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrialBalanceScreen(
    viewModel: MainViewModel,
    onNavigateToLedger: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val df = remember { DecimalFormat("#,##0.00") }

    // خيارات النطاق الزمني
    var selectedPeriodIndex by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val periodOptions = listOf("تراكمي شامل", "هذا الشهر", "الربع الحالي", "هذا العام")

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
            2 -> { // الربع الحالي (آخر 3 أشهر)
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
            else -> Pair(0L, Long.MAX_VALUE) // تراكمي شامل
        }
    }

    // جلب بيانات ميزان المراجعة
    val trialBalanceList by if (selectedPeriodIndex == 0) {
        viewModel.trialBalance.collectAsState()
    } else {
        viewModel.getPeriodTrialBalance(dateRange.first, dateRange.second)
            .collectAsState(initial = emptyList())
    }

    // تصفية حسب نوع الحساب
    var selectedTypeFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }

    val filteredRows = remember(trialBalanceList, selectedTypeFilter, searchQuery) {
        trialBalanceList.filter { row ->
            val matchesType = when (selectedTypeFilter) {
                "ASSET" -> row.accountCode.startsWith("1") || row.accountType == "ASSET"
                "LIABILITY" -> row.accountCode.startsWith("2") || row.accountType == "LIABILITY"
                "EQUITY" -> row.accountCode.startsWith("3") || row.accountType == "EQUITY"
                "REVENUE" -> row.accountCode.startsWith("4") || row.accountType == "REVENUE"
                "EXPENSE" -> row.accountCode.startsWith("5") || row.accountType == "EXPENSE"
                else -> true
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                row.accountCode.contains(searchQuery.trim()) ||
                row.accountName.contains(searchQuery.trim(), ignoreCase = true)
            }
            matchesType && matchesSearch
        }
    }

    // احتساب إجمالي ميزان المراجعة
    val totalDebit = remember(trialBalanceList) {
        trialBalanceList.fold(BigDecimal.ZERO) { acc, r -> acc.add(r.totalDebit) }
    }
    val totalCredit = remember(trialBalanceList) {
        trialBalanceList.fold(BigDecimal.ZERO) { acc, r -> acc.add(r.totalCredit) }
    }
    val difference = remember(totalDebit, totalCredit) {
        totalDebit.subtract(totalCredit).abs()
    }
    val isBalanced = remember(difference) {
        difference.compareTo(BigDecimal("0.01")) < 0
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("trial_balance_screen"),
        containerColor = CyberDarkSurface,
        bottomBar = {
            // شريط التدقيق المالي السفلي الإلزامي (Strict Audit Balance Bar)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (isBalanced) ProfitEmerald.copy(alpha = 0.5f) else PaymentRed, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                color = CyberDarkCardElevated,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (isBalanced) ProfitEmerald else PaymentRed)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBalanced) "حالة التدقيق: ميزان متوازن ومطابق 100% ✓" else "تنبيه: ميزان المراجعة غير متوازن!",
                                fontFamily = CairoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isBalanced) ProfitEmerald else PaymentRed
                            )
                        }

                        IconButton(
                            onClick = {
                                val text = buildString {
                                    appendLine("=== ميزان المراجعة المحاسبي المعياري ===")
                                    appendLine("الفترة: ${periodOptions[selectedPeriodIndex]}")
                                    appendLine("إجمالي المدين: ${df.format(totalDebit)} ر.ي")
                                    appendLine("إجمالي الدائن: ${df.format(totalCredit)} ر.ي")
                                    appendLine("حالة التوازن: ${if (isBalanced) "متطابق تماماً ✓" else "فارق: ${df.format(difference)} ر.ي"}")
                                    appendLine("---------------------------------------")
                                    trialBalanceList.forEach {
                                        appendLine("${it.accountCode} - ${it.accountName}: مدين ${df.format(it.totalDebit)} | دائن ${df.format(it.totalCredit)}")
                                    }
                                }
                                clipboardManager.setText(AnnotatedString(text))
                                Toast.makeText(context, "تم نسخ تقرير ميزان المراجعة إلى الحافظة ✓", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "مشاركة الميزان",
                                tint = MikroTikCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("إجمالي حركة المدين", fontFamily = CairoFontFamily, fontSize = 11.sp, color = TextSecondaryDark)
                            Text("${df.format(totalDebit)} ر.ي", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ReceiptGreen)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("الفارق الدفتري", fontFamily = CairoFontFamily, fontSize = 11.sp, color = TextSecondaryDark)
                            Text("${df.format(difference)} ر.ي", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (isBalanced) ProfitEmerald else PaymentRed)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("إجمالي حركة الدائن", fontFamily = CairoFontFamily, fontSize = 11.sp, color = TextSecondaryDark)
                            Text("${df.format(totalCredit)} ر.ي", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF38BDF8))
                        }
                    }
                }
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
            // عنوان الشاشة والتحكم
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ميزان المراجعة الموحد ⚖️",
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = "قائمة الأرصدة والحركات لجميع حسابات الأستاذ العام",
                            fontFamily = CairoFontFamily,
                            fontSize = 11.5.sp,
                            color = TextSecondaryDark
                        )
                    }

                    Surface(
                        color = if (isBalanced) ProfitEmerald.copy(alpha = 0.15f) else PaymentRed.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isBalanced) ProfitEmerald else PaymentRed)
                    ) {
                        Text(
                            text = if (isBalanced) "موزون ✓" else "فارق!",
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (isBalanced) ProfitEmerald else PaymentRed,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
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

            // حقل البحث السريع وتصنيف شجرة الحسابات
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text("بحث بكود الحساب (1101, 5101) أو الاسم...", fontFamily = CairoFontFamily, fontSize = 12.sp, color = TextSecondaryDark)
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

            // أزرار تصفية التصنيف المحاسبي
            item {
                val types = listOf(
                    Triple("ALL", "الكل", Color(0xFF94A3B8)),
                    Triple("ASSET", "الأصول (1)", ReceiptGreen),
                    Triple("LIABILITY", "الخصوم (2)", PaymentRed),
                    Triple("EQUITY", "حقوق الملكية (3)", EquityBlue),
                    Triple("REVENUE", "الإيرادات (4)", InvestmentGold),
                    Triple("EXPENSE", "المصروفات (5)", AssetPurple)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    types.forEach { (typeKey, label, color) ->
                        val isSelected = selectedTypeFilter == typeKey
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTypeFilter = typeKey },
                            color = if (isSelected) color.copy(alpha = 0.25f) else CyberDarkCardElevated,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) color else CyberBorder)
                        ) {
                            Text(
                                text = label,
                                fontFamily = CairoFontFamily,
                                fontSize = 10.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) color else TextSecondaryDark,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // قائمة أسطر الحسابات في ميزان المراجعة
            if (filteredRows.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Assessment, contentDescription = null, tint = TextSecondaryDark, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("لا توجد حسابات مطابقة لمعايير البحث", fontFamily = CairoFontFamily, color = TextSecondaryDark, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                items(filteredRows, key = { it.accountCode }) { row ->
                    TrialBalanceAccountCard(
                        row = row,
                        df = df,
                        onClickLedger = {
                            onNavigateToLedger?.invoke(row.accountCode)
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun TrialBalanceAccountCard(
    row: TrialBalanceRow,
    df: DecimalFormat,
    onClickLedger: () -> Unit
) {
    val categoryColor = when {
        row.accountCode.startsWith("1") -> ReceiptGreen
        row.accountCode.startsWith("2") -> PaymentRed
        row.accountCode.startsWith("3") -> EquityBlue
        row.accountCode.startsWith("4") -> InvestmentGold
        row.accountCode.startsWith("5") -> AssetPurple
        else -> MikroTikCyan
    }

    val netDebit = if (row.totalDebit > row.totalCredit) row.totalDebit.subtract(row.totalCredit) else BigDecimal.ZERO
    val netCredit = if (row.totalCredit > row.totalDebit) row.totalCredit.subtract(row.totalDebit) else BigDecimal.ZERO

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("trial_balance_row_${row.accountCode}")
            .clickable { onClickLedger() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = categoryColor.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, categoryColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = row.accountCode,
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = categoryColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = row.accountName,
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // مؤشر الرصيد النهائي
                Surface(
                    color = if (netDebit > BigDecimal.ZERO) ReceiptGreen.copy(alpha = 0.15f) else Color(0xFF38BDF8).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (netDebit > BigDecimal.ZERO) "مدين ${df.format(netDebit)}" else "دائن ${df.format(netCredit)}",
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        color = if (netDebit > BigDecimal.ZERO) ReceiptGreen else Color(0xFF38BDF8),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // تفاصيل الحركات: إجمالي المدين وإجمالي الدائن
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberDarkSurface.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("حركة المدين: ", fontFamily = CairoFontFamily, fontSize = 11.sp, color = TextSecondaryDark)
                    Text("${df.format(row.totalDebit)} ر.ي", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ReceiptGreen)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("حركة الدائن: ", fontFamily = CairoFontFamily, fontSize = 11.sp, color = TextSecondaryDark)
                    Text("${df.format(row.totalCredit)} ر.ي", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF38BDF8))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ArrowForwardIos, contentDescription = "عرض دفتر الأستاذ", tint = MikroTikCyan, modifier = Modifier.size(12.dp))
                }
            }
        }
    }
}
