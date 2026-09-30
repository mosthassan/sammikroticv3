package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.repository.PeriodicSettlementData
import com.example.ui.reports.PeriodicReportViewModel
import com.example.ui.theme.*
import com.example.util.WhatsAppHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * شاشة لوحة تقرير الإقفال والتسوية الدورية (Periodic Settlement Dashboard)
 * تتيح لمالك الشبكة تحديد فترة زمنية واستعراض مؤشرات الأداء الحقيقية:
 * - إجمالي المقبوضات
 * - إجمالي المصروفات والمشتريات
 * - الديون الجديدة الآجلة
 * - إجمالي المبيعات
 * - عدد الكروت المولدة والمباعة
 * - صافي الربح / الرصيد الفتراوي
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodicReportScreen(
    viewModel: PeriodicReportViewModel = viewModel(),
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val startDateMillis by viewModel.startDateMillis.collectAsState()
    val endDateMillis by viewModel.endDateMillis.collectAsState()
    val settlementData by viewModel.settlementData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var selectedRangePreset by remember { mutableStateOf("THIS_MONTH") } // "TODAY", "THIS_WEEK", "THIS_MONTH", "THIS_YEAR", "CUSTOM"
    var showCustomDateRangePicker by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }
    val startDateStr = remember(startDateMillis) { dateFormat.format(Date(startDateMillis)) }
    val endDateStr = remember(endDateMillis) { dateFormat.format(Date(endDateMillis)) }

    val shareText = remember(settlementData, startDateStr, endDateStr) {
        val data = settlementData
        if (data == null) "" else buildString {
            appendLine("📋 *تقرير الإقفال والتسوية الدورية - ${WhatsAppHelper.NETWORK_BRAND_NAME}*")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("📅 *الفترة الزمنية:* من $startDateStr إلى $endDateStr")
            appendLine("─────────────────────────")
            appendLine("📥 *إجمالي المقبوضات (سندات القبض):* ${String.format(Locale.US, "%,.0f", data.totalReceiptsAmount)} ريال")
            appendLine("📤 *إجمالي المصروفات والمشتريات:* ${String.format(Locale.US, "%,.0f", data.totalExpensesAmount)} ريال")
            appendLine("💳 *إجمالي المبيعات الآجلة (ديون جديدة):* ${String.format(Locale.US, "%,.0f", data.totalNewDebt)} ريال")
            appendLine("📊 *إجمالي قيمة المبيعات:* ${String.format(Locale.US, "%,.0f", data.totalSalesAmount)} ريال")
            appendLine("🎴 *عدد الكروت المولدة:* ${data.generatedCardsCount} كرت")
            appendLine("🏷️ *عدد الكروت المباعة/الموزعة:* ${data.soldCardsCount} كرت")
            appendLine("─────────────────────────")
            val netSign = if (data.netBalance >= 0) "🟢" else "🔴"
            appendLine("$netSign *صافي الأرباح / الرصيد الفتراوي:* ${String.format(Locale.US, "%,.0f", data.netBalance)} ريال")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("📡 *نظام الإقفال المالي المحاسبي - ${WhatsAppHelper.NETWORK_BRAND_NAME}*")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("periodic_report_screen")
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onNavigateBack != null) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Column {
                    Text(
                        text = "تقرير الإقفال والتسوية الدورية 📋",
                        fontFamily = CairoFontFamily,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "تحليل الأداء المالي، الكروت، والمقبوضات خلال الفترة المحددة",
                        fontFamily = CairoFontFamily,
                        fontSize = 11.sp,
                        color = TextSecondaryDark
                    )
                }
            }

            IconButton(onClick = { viewModel.loadData() }) {
                Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = MikroTikCyan)
            }
        }

        // Date Range Selector Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DatePresetChip(
                text = "اليوم",
                selected = selectedRangePreset == "TODAY",
                onClick = {
                    selectedRangePreset = "TODAY"
                    val start = PeriodicReportViewModel.getStartOfDayMillis()
                    val end = PeriodicReportViewModel.getEndOfDayMillis()
                    viewModel.setDateRange(start, end)
                },
                modifier = Modifier.weight(1f)
            )
            DatePresetChip(
                text = "الأسبوع",
                selected = selectedRangePreset == "THIS_WEEK",
                onClick = {
                    selectedRangePreset = "THIS_WEEK"
                    val start = PeriodicReportViewModel.getStartOfWeekMillis()
                    val end = PeriodicReportViewModel.getEndOfDayMillis()
                    viewModel.setDateRange(start, end)
                },
                modifier = Modifier.weight(1f)
            )
            DatePresetChip(
                text = "الشهر",
                selected = selectedRangePreset == "THIS_MONTH",
                onClick = {
                    selectedRangePreset = "THIS_MONTH"
                    val start = PeriodicReportViewModel.getStartOfMonthMillis()
                    val end = PeriodicReportViewModel.getEndOfDayMillis()
                    viewModel.setDateRange(start, end)
                },
                modifier = Modifier.weight(1f)
            )
            DatePresetChip(
                text = "السنة",
                selected = selectedRangePreset == "THIS_YEAR",
                onClick = {
                    selectedRangePreset = "THIS_YEAR"
                    val start = PeriodicReportViewModel.getStartOfYearMillis()
                    val end = PeriodicReportViewModel.getEndOfDayMillis()
                    viewModel.setDateRange(start, end)
                },
                modifier = Modifier.weight(1f)
            )
            DatePresetChip(
                text = "مخصص 📅",
                selected = selectedRangePreset == "CUSTOM",
                onClick = {
                    selectedRangePreset = "CUSTOM"
                    showCustomDateRangePicker = true
                },
                modifier = Modifier.weight(1.1f)
            )
        }

        // Selected Date Range Banner
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF0F1E33),
            border = BorderStroke(1.dp, MikroTikPrimary.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = MikroTikCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "نطاق الإقفال الفتراوي:",
                        fontFamily = CairoFontFamily,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondaryDark
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MikroTikNavy,
                    border = BorderStroke(1.dp, CyberBorder),
                    onClick = { showCustomDateRangePicker = true }
                ) {
                    Text(
                        text = "$startDateStr ➔ $endDateStr",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MikroTikCyan, strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("جاري استعلام وحساب مؤشرات التسوية الدورية...", fontFamily = CairoFontFamily, fontSize = 12.sp, color = TextSecondaryDark)
                }
            }
        } else {
            val data = settlementData
            if (data == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا توجد بيانات متاحة لهذه الفترة الزمنية.", fontFamily = CairoFontFamily, color = TextSecondaryDark, fontSize = 13.sp)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Item 1: Total Receipts
                    item {
                        PeriodicMetricCard(
                            title = "إجمالي المقبوضات",
                            subtitle = "سندات القبض الفتراوية",
                            value = "${String.format(Locale.US, "%,.0f", data.totalReceiptsAmount)} ر.ي",
                            icon = Icons.Default.MonetizationOn,
                            color = ProfitEmerald
                        )
                    }

                    // Item 2: Total Expenses
                    item {
                        PeriodicMetricCard(
                            title = "إجمالي المصروفات",
                            subtitle = "سندات الصرف والمشتريات",
                            value = "${String.format(Locale.US, "%,.0f", data.totalExpensesAmount)} ر.ي",
                            icon = Icons.Default.Payments,
                            color = PaymentRed
                        )
                    }

                    // Item 3: New Debt / Unpaid Credit Invoices
                    item {
                        PeriodicMetricCard(
                            title = "ديون جديدة آجل",
                            subtitle = "فواتير مبيعات غير مسددة",
                            value = "${String.format(Locale.US, "%,.0f", data.totalNewDebt)} ر.ي",
                            icon = Icons.Default.ReceiptLong,
                            color = StatusWarning
                        )
                    }

                    // Item 4: Total Sales Amount
                    item {
                        PeriodicMetricCard(
                            title = "إجمالي المبيعات",
                            subtitle = "قيمة الكروت المباعة بالكامل",
                            value = "${String.format(Locale.US, "%,.0f", data.totalSalesAmount)} ر.ي",
                            icon = Icons.Default.Store,
                            color = Color(0xFF38BDF8)
                        )
                    }

                    // Item 5: Generated Cards Count
                    item {
                        PeriodicMetricCard(
                            title = "الكروت المولدة",
                            subtitle = "إجمالي الكروت المنشأة",
                            value = "${data.generatedCardsCount} كرت",
                            icon = Icons.Default.CreditCard,
                            color = AssetPurple
                        )
                    }

                    // Item 6: Sold/Distributed Cards Count
                    item {
                        PeriodicMetricCard(
                            title = "الكروت المباعة",
                            subtitle = "الكروت الموزعة والمسلمة",
                            value = "${data.soldCardsCount} كرت",
                            icon = Icons.Default.ConfirmationNumber,
                            color = InvestmentGold
                        )
                    }

                    // Net Profit / Balance Summary Banner
                    item(span = { GridItemSpan(2) }) {
                        val isPositive = data.netBalance >= 0
                        val netColor = if (isPositive) ProfitEmerald else PaymentRed

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
                            border = BorderStroke(1.5.dp, netColor.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
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
                                                .background(netColor.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                                contentDescription = null,
                                                tint = netColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "صافي الربح والرصيد الفتراوي (Net Balance)",
                                                fontFamily = CairoFontFamily,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "المقبوضات المباشرة (${String.format(Locale.US, "%,.0f", data.totalReceiptsAmount)}) - المصروفات (${String.format(Locale.US, "%,.0f", data.totalExpensesAmount)})",
                                                fontFamily = CairoFontFamily,
                                                fontSize = 10.sp,
                                                color = TextSecondaryDark
                                            )
                                        }
                                    }

                                    Text(
                                        text = "${String.format(Locale.US, "%,.0f", data.netBalance)} ريال",
                                        fontFamily = CairoFontFamily,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = netColor
                                    )
                                }

                                Divider(modifier = Modifier.padding(vertical = 10.dp), color = CyberBorder)

                                // Quick Actions: Share via WhatsApp & Copy
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            WhatsAppHelper.shareTextIntent(context, shareText, "مشاركة تقرير الإقفال الدوري عبر الواتساب")
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Chat, contentDescription = null, tint = WhatsAppDarkGreen, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("مشاركة الواتساب", fontFamily = CairoFontFamily, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = WhatsAppDarkGreen)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(shareText))
                                            Toast.makeText(context, "تم نسخ تقرير الإقفال للحافظة بنجاح ✓", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, MikroTikCyan),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MikroTikCyan),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("نسخ التقرير", fontFamily = CairoFontFamily, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    item(span = { GridItemSpan(2) }) {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }

    // Material 3 Date Range Picker Dialog
    if (showCustomDateRangePicker) {
        val datePickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = startDateMillis,
            initialSelectedEndDateMillis = endDateMillis
        )

        DatePickerDialog(
            onDismissRequest = { showCustomDateRangePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val start = datePickerState.selectedStartDateMillis ?: startDateMillis
                        val end = datePickerState.selectedEndDateMillis ?: endDateMillis
                        viewModel.setDateRange(start, end)
                        showCustomDateRangePicker = false
                    }
                ) {
                    Text("تطبيق وتصفية", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, color = ProfitEmerald)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDateRangePicker = false }) {
                    Text("إلغاء", fontFamily = CairoFontFamily, color = TextSecondaryDark)
                }
            }
        ) {
            DateRangePicker(
                state = datePickerState,
                title = {
                    Text(
                        text = "تحديد النطاق الزمني لتقرير الإقفال",
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                },
                modifier = Modifier.height(450.dp)
            )
        }
    }
}

@Composable
private fun DatePresetChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MikroTikPrimary else CyberDarkSurface,
        border = BorderStroke(1.dp, if (selected) MikroTikPrimary else CyberBorder),
        onClick = onClick,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.padding(vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontFamily = CairoFontFamily,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) Color.White else TextSecondaryDark
            )
        }
    }
}

@Composable
private fun PeriodicMetricCard(
    title: String,
    subtitle: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                }

                Text(
                    text = value,
                    fontFamily = CairoFontFamily,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                fontFamily = CairoFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = subtitle,
                fontFamily = CairoFontFamily,
                fontSize = 10.sp,
                color = TextSecondaryDark
            )
        }
    }
}
