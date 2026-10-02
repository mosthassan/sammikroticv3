package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.entity.CardSalesInvoiceEntity
import com.example.data.local.entity.FinancialVoucherEntity
import com.example.data.local.entity.RetailerEntity
import com.example.ui.MainViewModel
import com.example.ui.reports.ReportsViewModel
import com.example.ui.reports.TimeRangeOption
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
import com.example.util.*
import com.example.ui.theme.WhatsAppDarkGreen
import com.example.ui.theme.WhatsAppGreen
import com.example.util.WhatsAppHelper
import java.util.Calendar

@Composable
fun ReportsDashboardScreen(
    reportsViewModel: ReportsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var reportSubTab by remember { androidx.compose.runtime.mutableIntStateOf(0) }

    if (reportSubTab == 1) {
        PeriodicReportScreen(
            onNavigateBack = { reportSubTab = 0 },
            modifier = modifier
        )
        return
    }

    val selectedTimeRange by reportsViewModel.selectedTimeRange.collectAsState()
    val summary by reportsViewModel.financialReportSummary.collectAsState()
    val aiSummary by reportsViewModel.aiSummary.collectAsState()
    val isAiLoading by reportsViewModel.isAiLoading.collectAsState()
    val networkIdentity by reportsViewModel.networkIdentity.collectAsState()

    val allRetailers by reportsViewModel.allRetailers.collectAsState()
    var selectedCustomerForLedger by remember { mutableStateOf<RetailerEntity?>(null) }
    var customerSearchQueryInReports by remember { mutableStateOf("") }
    var isCustomerDropdownExpanded by remember { mutableStateOf(false) }

    val filteredRetailersForReports = remember(allRetailers, customerSearchQueryInReports) {
        if (customerSearchQueryInReports.isBlank()) {
            allRetailers
        } else {
            val q = customerSearchQueryInReports.trim()
            allRetailers.filter {
                it.name.contains(q, ignoreCase = true) ||
                it.phone.contains(q) ||
                it.ownerName.contains(q, ignoreCase = true)
            }
        }
    }

    var showCustomDateDialog by remember { mutableStateOf(false) }
    var selectedAgentForStatement by remember { mutableStateOf<RetailerEntity?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("reports_dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "لوحة التقارير والتحليلات المالية الذكية 📊",
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White
                    )
                    Text(
                        text = "مؤشرات المبيعات، المقبوضات، المصروفات، وأعلى الوكلاء والديون",
                        fontFamily = CairoFontFamily,
                        fontSize = 11.5.sp,
                        color = TextSecondaryDark
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MikroTikCyan.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, MikroTikCyan.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(ProfitEmerald)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "تحليل مباشر",
                            color = MikroTikCyan,
                            fontFamily = CairoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Sub-Tab Navigation Bar (التقرير المالي الشامل vs تقرير الإقفال الدوري)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (reportSubTab == 0) MikroTikPrimary else CyberDarkSurface,
                    border = BorderStroke(1.dp, if (reportSubTab == 0) MikroTikPrimary else CyberBorder),
                    onClick = { reportSubTab = 0 },
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(15.dp), tint = if (reportSubTab == 0) Color.White else TextSecondaryDark)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("التقرير الشامل", fontFamily = CairoFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (reportSubTab == 0) Color.White else TextSecondaryDark)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (reportSubTab == 1) MikroTikPrimary else CyberDarkSurface,
                    border = BorderStroke(1.dp, if (reportSubTab == 1) MikroTikPrimary else CyberBorder),
                    onClick = { reportSubTab = 1 },
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(15.dp), tint = if (reportSubTab == 1) Color.White else TextSecondaryDark)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("الإقفال الدوري 📋", fontFamily = CairoFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (reportSubTab == 1) Color.White else TextSecondaryDark)
                    }
                }
            }
        }

        // 1. Time Filter Bar (اليوم، هذا الأسبوع، هذا الشهر، مخصص)
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TimeRangeOption.values().forEach { option ->
                        val isSelected = selectedTimeRange == option
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) MikroTikPrimary else Color.Transparent
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) MikroTikCyan else Color.Transparent,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    if (option == TimeRangeOption.CUSTOM) {
                                        showCustomDateDialog = true
                                    } else {
                                        reportsViewModel.setTimeRangeOption(option)
                                    }
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (option == TimeRangeOption.CUSTOM) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else TextSecondaryDark,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                }
                                Text(
                                    text = option.label,
                                    color = if (isSelected) Color.White else TextSecondaryDark,
                                    fontFamily = CairoFontFamily,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Consolidated Financial KPI Cards Grid (4 Cards)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Row 1: Total Sales & Total Receipts
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ReportKpiCard(
                        title = "إجمالي المبيعات",
                        amount = summary.totalSales,
                        subtitle = "فواتير مبيعات الكروت",
                        color = MikroTikCyan,
                        icon = Icons.Default.Store,
                        modifier = Modifier.weight(1f)
                    )
                    ReportKpiCard(
                        title = "إجمالي المقبوضات",
                        amount = summary.totalReceipts,
                        subtitle = "سندات توريد نقدية",
                        color = ReceiptGreen,
                        icon = Icons.Default.ArrowDownward,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 2: Total Expenses & True Net Profit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ReportKpiCard(
                        title = "إجمالي المصروفات",
                        amount = summary.totalExpenses,
                        subtitle = "مصاريف وسندات صرف",
                        color = PaymentRed,
                        icon = Icons.Default.ArrowUpward,
                        modifier = Modifier.weight(1f)
                    )
                    ReportKpiCard(
                        title = "صافي الربح الحقيقي",
                        amount = summary.netTrueProfit,
                        subtitle = "هامش ${String.format(Locale.US, "%.1f", summary.profitMarginPercent)}%",
                        color = if (summary.netTrueProfit >= 0) ProfitEmerald else PaymentRed,
                        icon = Icons.Default.MonetizationOn,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. Smart AI Summary Section (زر "الملخص الذكي AI")
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, InvestmentGold.copy(alpha = 0.4f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reports_ai_summary_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(InvestmentGold.copy(alpha = 0.18f))
                                    .border(1.dp, InvestmentGold.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = InvestmentGold,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "الملخص الذكي AI للمبيعات والديون",
                                    color = Color.White,
                                    fontFamily = CairoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "تحليل مالي تلقائي بواسطة Gemini AI لقراءة أداء الفترة",
                                    color = TextSecondaryDark,
                                    fontFamily = CairoFontFamily,
                                    fontSize = 10.5.sp
                                )
                            }
                        }

                        Button(
                            onClick = { reportsViewModel.generateSmartAiSummary() },
                            enabled = !isAiLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = InvestmentGold),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isAiLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = Color.Black,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("توليد الملخص", color = Color.Black, fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }

                    if (!aiSummary.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = CyberDarkCardElevated,
                            border = BorderStroke(1.dp, InvestmentGold.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = aiSummary!!,
                                    color = TextPrimaryDark,
                                    fontFamily = CairoFontFamily,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(aiSummary!!))
                                            Toast.makeText(context, "تم نسخ الملخص الذكي إلى الحافظة", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", tint = MikroTikCyan, modifier = Modifier.size(15.dp))
                                    }

                                    IconButton(
                                        onClick = {
                                            val sendIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT, aiSummary!!)
                                                type = "text/plain"
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, "مشاركة الملخص الذكي"))
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "مشاركة", tint = InvestmentGold, modifier = Modifier.size(15.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Top 5 Agents by Purchase Volume Card (أعلى 5 وكلاء شراءً)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MikroTikCyan.copy(alpha = 0.15f))
                                    .border(1.dp, MikroTikCyan.copy(alpha = 0.35f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Store, contentDescription = null, tint = MikroTikCyan, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "أعلى 5 وكلاء / بقالات شراءً (Top Distributors)",
                                    color = Color.White,
                                    fontFamily = CairoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                )
                                Text(
                                    text = "الوكلاء الأكثر سحباً لمبيعات الكروت خلال الفترة",
                                    color = TextSecondaryDark,
                                    fontFamily = CairoFontFamily,
                                    fontSize = 10.5.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (summary.topAgentsBySales.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("لا توجد مبيعات مسجلة للوكلاء في هذه الفترة", color = TextSecondaryDark, fontFamily = CairoFontFamily, fontSize = 11.5.sp)
                        }
                    } else {
                        summary.topAgentsBySales.forEachIndexed { index, (retailer, totalSales) ->
                            val percent = if (summary.totalSales > 0) (totalSales / summary.totalSales) * 100.0 else 0.0

                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(MikroTikPrimary.copy(alpha = 0.3f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("${index + 1}", color = MikroTikCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(retailer.name, color = Color.White, fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${String.format(Locale.US, "%,.0f", totalSales)} ر.ي",
                                            color = MikroTikCyan,
                                            fontFamily = CairoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        OutlinedButton(
                                            onClick = { selectedAgentForStatement = retailer },
                                            modifier = Modifier.height(26.dp),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, MikroTikCyan.copy(alpha = 0.5f))
                                        ) {
                                            Text("كشف حساب", fontSize = 9.5.sp, color = MikroTikCyan, fontFamily = CairoFontFamily)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                LinearProgressIndicator(
                                    progress = { (percent / 100f).toFloat().coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = MikroTikCyan,
                                    trackColor = CyberDarkCardElevated
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Top 5 Aging Debtors Card (أعلى 5 وكلاء مديونية)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, PaymentRed.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PaymentRed.copy(alpha = 0.15f))
                                    .border(1.dp, PaymentRed.copy(alpha = 0.35f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.TrendingDown, contentDescription = null, tint = PaymentRed, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "أعلى 5 وكلاء مديونية (Aging Debtors)",
                                    color = Color.White,
                                    fontFamily = CairoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                )
                                Text(
                                    text = "الديون التراكمية المستحقة والائتمان المتاح",
                                    color = TextSecondaryDark,
                                    fontFamily = CairoFontFamily,
                                    fontSize = 10.5.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (summary.topAgingDebtors.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("لا توجد مديونيات على الوكلاء 👍", color = ProfitEmerald, fontFamily = CairoFontFamily, fontSize = 11.5.sp)
                        }
                    } else {
                        summary.topAgingDebtors.forEachIndexed { index, (retailer, debtAmount) ->
                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(PaymentRed.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("${index + 1}", color = PaymentRed, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(retailer.name, color = Color.White, fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                            if (retailer.phone.isNotBlank()) {
                                                Text(retailer.phone, color = TextSecondaryDark, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                            }
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${String.format(Locale.US, "%,.0f", debtAmount)} ر.ي",
                                            color = PaymentRed,
                                            fontFamily = CairoFontFamily,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp
                                        )

                                        Spacer(modifier = Modifier.width(6.dp))

                                        if (retailer.phone.isNotBlank()) {
                                            IconButton(
                                                onClick = {
                                                    val msg = WhatsAppHelper.generateRetailerStatementMessage(retailer)
                                                    WhatsAppHelper.sendWhatsAppMessage(context, retailer.phone, msg)
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Chat, contentDescription = "واتساب", tint = WhatsAppDarkGreen, modifier = Modifier.size(15.dp))
                                            }
                                        }

                                        Button(
                                            onClick = { selectedAgentForStatement = retailer },
                                            colors = ButtonDefaults.buttonColors(containerColor = PaymentRed.copy(alpha = 0.2f)),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.height(26.dp)
                                        ) {
                                            Text("كشف حساب", fontSize = 9.5.sp, color = PaymentRed, fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                HorizontalDivider(color = CyberBorder.copy(alpha = 0.5f), modifier = Modifier.padding(top = 6.dp))
                            }
                        }
                    }
                }
            }
        }

        // 5. Customer Selection & Financial Ledger Picker (كشف حساب عميل / بقالة مخصص)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, MikroTikCyan.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("customer_ledger_section")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MikroTikCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Store, contentDescription = null, tint = MikroTikCyan, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "كشف حساب عميل / سوبرماركت تفصيلي",
                                color = Color.White,
                                fontFamily = CairoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "ابحث بالاسم لعرض كشف حساب أي عميل فورياً بدقة متناهية",
                                color = TextSecondaryDark,
                                fontFamily = CairoFontFamily,
                                fontSize = 10.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    @OptIn(ExperimentalMaterial3Api::class)
                    ExposedDropdownMenuBox(
                        expanded = isCustomerDropdownExpanded,
                        onExpandedChange = { isCustomerDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = if (selectedCustomerForLedger != null && customerSearchQueryInReports.isEmpty()) selectedCustomerForLedger!!.name else customerSearchQueryInReports,
                            onValueChange = {
                                customerSearchQueryInReports = it
                                isCustomerDropdownExpanded = true
                                if (it.isBlank()) selectedCustomerForLedger = null
                            },
                            label = { Text("بحث واختيار عميل / بقالة *", fontFamily = CairoFontFamily) },
                            placeholder = { Text("اكتب اسم المحل أو رقم الهاتف...", fontSize = 11.5.sp, color = TextSecondaryDark) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MikroTikCyan) },
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (customerSearchQueryInReports.isNotEmpty() || selectedCustomerForLedger != null) {
                                        IconButton(onClick = {
                                            customerSearchQueryInReports = ""
                                            selectedCustomerForLedger = null
                                            isCustomerDropdownExpanded = false
                                        }) {
                                            Icon(Icons.Default.Close, contentDescription = "مسح", tint = TextSecondaryDark, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCustomerDropdownExpanded)
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = CyberDarkCardElevated,
                                unfocusedContainerColor = CyberDarkCardElevated,
                                focusedBorderColor = MikroTikCyan,
                                unfocusedBorderColor = CyberBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("customer_ledger_dropdown")
                        )

                        ExposedDropdownMenu(
                            expanded = isCustomerDropdownExpanded && filteredRetailersForReports.isNotEmpty(),
                            onDismissRequest = { isCustomerDropdownExpanded = false },
                            modifier = Modifier.background(CyberDarkCardElevated)
                        ) {
                            filteredRetailersForReports.forEach { retailer ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(retailer.name, fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.5.sp)
                                                if (retailer.phone.isNotBlank()) {
                                                    Text(retailer.phone, fontSize = 10.sp, color = TextSecondaryDark)
                                                }
                                            }
                                            Text(
                                                text = "دين: ${retailer.balanceOwed.toInt()} ر.ي",
                                                fontFamily = CairoFontFamily,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (retailer.balanceOwed > 0) PaymentRed else ReceiptGreen
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedCustomerForLedger = retailer
                                        selectedAgentForStatement = retailer
                                        customerSearchQueryInReports = retailer.name
                                        isCustomerDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (selectedCustomerForLedger != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = { selectedAgentForStatement = selectedCustomerForLedger },
                                colors = ButtonDefaults.buttonColors(containerColor = MikroTikPrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("عرض كشف الحساب المالي المباشر", fontFamily = CairoFontFamily, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 6. Report Export Action Bar
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "خيارات تصدير ومشاركة التقرير المالي الشامل",
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { reportsViewModel.exportFinancialReportPdf(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = MikroTikPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تصدير PDF رسمية", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        }

                        OutlinedButton(
                            onClick = { reportsViewModel.exportFinancialReportText(context) },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MikroTikCyan),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MikroTikCyan),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تصدير نصي", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }

    // Custom Date Range Dialog
    if (showCustomDateDialog) {
        CustomDateRangePickerDialog(
            onDismiss = { showCustomDateDialog = false },
            onConfirm = { start, end ->
                reportsViewModel.setCustomDateRange(start, end)
                showCustomDateDialog = false
            }
        )
    }

    // Agent Account Statement Dialog
    if (selectedAgentForStatement != null) {
        val retailer = selectedAgentForStatement!!
        AgentAccountStatementModal(
            retailer = retailer,
            reportsViewModel = reportsViewModel,
            onDismiss = { selectedAgentForStatement = null }
        )
    }
}

@Composable
fun ReportKpiCard(
    title: String,
    amount: Double,
    subtitle: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontFamily = CairoFontFamily, fontSize = 11.sp, color = TextSecondaryDark)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(15.dp))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${String.format(Locale.US, "%,.0f", amount)} ر.ي",
                fontFamily = CairoFontFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
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

data class StatementTxItem(
    val id: String,
    val dateMillis: Long,
    val typeLabel: String,
    val isReceipt: Boolean,
    val referenceNumber: String,
    val details: String,
    val debitAmount: Double,  // المبيعات (عليها)
    val creditAmount: Double  // المسدد (لها)
)

@Composable
fun AgentAccountStatementModal(
    retailer: RetailerEntity,
    reportsViewModel: ReportsViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val allInvoices by reportsViewModel.allInvoices.collectAsState()
    val allVouchers by reportsViewModel.allVouchers.collectAsState()

    val agentInvoices = remember(allInvoices, retailer) {
        allInvoices
            .filter {
                (it.retailerId == retailer.id || it.customerName.trim().equals(retailer.name.trim(), ignoreCase = true)) &&
                !it.invoiceNumber.startsWith("INV-DELIV-")
            }
            .distinctBy { if (it.invoiceNumber.isNotBlank()) it.invoiceNumber else it.id.toString() }
    }

    val agentReceiptVouchers = remember(allVouchers, retailer) {
        allVouchers
            .filter {
                (it.retailerId == retailer.id || it.partyName.trim().equals(retailer.name.trim(), ignoreCase = true)) &&
                !it.isVoided &&
                it.voucherType == "RECEIPT"
            }
            .distinctBy { if (it.voucherNumber.isNotBlank()) it.voucherNumber else it.id.toString() }
    }

    val totalPurchases = agentInvoices.sumOf { it.totalAmount.toDouble() }

    val totalPaid = agentReceiptVouchers.sumOf { it.amount.toDouble() }
    val finalBalance = totalPurchases - totalPaid

    val txList = remember(agentInvoices, agentReceiptVouchers) {
        val list = mutableListOf<StatementTxItem>()
        agentInvoices.forEach { inv ->
            list.add(
                StatementTxItem(
                    id = "INV_${inv.id}",
                    dateMillis = inv.invoiceDateMillis,
                    typeLabel = "فاتورة مبيعات",
                    isReceipt = false,
                    referenceNumber = inv.invoiceNumber,
                    details = "${inv.totalCardsCount} كرت ${if (inv.itemsSummary.isNotBlank()) "• ${inv.itemsSummary}" else ""}",
                    debitAmount = inv.totalAmount.toDouble(),
                    creditAmount = 0.0
                )
            )
        }

        agentReceiptVouchers.forEach { v ->
            list.add(
                StatementTxItem(
                    id = "VOUCHER_${v.id}",
                    dateMillis = v.dateMillis,
                    typeLabel = "سند قبض",
                    isReceipt = true,
                    referenceNumber = v.voucherNumber,
                    details = "${v.category} ${if (v.description.isNotBlank()) "• ${v.description}" else ""}",
                    debitAmount = 0.0,
                    creditAmount = v.amount.toDouble()
                )
            )
        }

        list.sortedByDescending { it.dateMillis }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("كشف حساب تفصيلي: ${retailer.name}", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                if (retailer.phone.isNotBlank()) {
                    Text("الهاتف: ${retailer.phone}", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TextSecondaryDark)
                }
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Summary Box
                item {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CyberDarkCardElevated,
                        border = BorderStroke(1.dp, CyberBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("إجمالي المبيعات (مدين):", fontSize = 11.sp, fontFamily = CairoFontFamily, color = TextSecondaryDark)
                                Text("${String.format(Locale.US, "%,.0f", totalPurchases)} ر.ي", fontSize = 11.5.sp, fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, color = MikroTikCyan)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("إجمالي المسدد (دائن):", fontSize = 11.sp, fontFamily = CairoFontFamily, color = TextSecondaryDark)
                                Text("${String.format(Locale.US, "%,.0f", totalPaid)} ر.ي", fontSize = 11.5.sp, fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, color = ReceiptGreen)
                            }

                            HorizontalDivider(color = CyberBorder, modifier = Modifier.padding(vertical = 4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("الرصيد المستحق النهائي:", fontSize = 11.5.sp, fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("${String.format(Locale.US, "%,.0f", finalBalance)} ر.ي", fontSize = 12.5.sp, fontFamily = CairoFontFamily, fontWeight = FontWeight.ExtraBold, color = if (finalBalance > 0) PaymentRed else ReceiptGreen)
                            }
                        }
                    }
                }

                item {
                    Text("سجل الحركات الأخير (${txList.size} حركات):", fontFamily = CairoFontFamily, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TextSecondaryDark)
                }

                if (txList.isEmpty()) {
                    item {
                        Text("لا توجد فواتير أو سندات سابقة لهذا الوكيل", fontFamily = CairoFontFamily, fontSize = 11.sp, color = TextSecondaryDark)
                    }
                } else {
                    items(txList, key = { it.id }) { item ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CyberDarkCardElevated,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${item.referenceNumber} (${item.typeLabel})", fontSize = 11.5.sp, fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, color = if (item.isReceipt) ReceiptGreen else Color.White)
                                    Text("${item.details} • ${SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(item.dateMillis))}", fontSize = 10.sp, color = TextSecondaryDark)
                                }
                                Text(
                                    text = if (item.isReceipt) "-${item.creditAmount.toInt()} ر.ي" else "+${item.debitAmount.toInt()} ر.ي",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (item.isReceipt) ReceiptGreen else MikroTikCyan
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = { reportsViewModel.exportAgentAccountStatementPdf(context, retailer) },
                    colors = ButtonDefaults.buttonColors(containerColor = MikroTikPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PDF", fontFamily = CairoFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { reportsViewModel.exportAgentAccountStatementText(context, retailer) },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MikroTikCyan)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = MikroTikCyan, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("مشاركة", fontFamily = CairoFontFamily, fontSize = 11.sp, color = MikroTikCyan)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق", fontFamily = CairoFontFamily, color = TextSecondaryDark)
            }
        },
        containerColor = CyberDarkSurface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun CustomDateRangePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (Long, Long) -> Unit
) {
    var startText by remember { mutableStateOf("2026/01/01") }
    var endText by remember {
        mutableStateOf(SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date()))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تحديد فترة زمني مخصصة", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = startText,
                    onValueChange = { startText = it },
                    label = { Text("تاريخ البداية (YYYY/MM/DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = endText,
                    onValueChange = { endText = it },
                    label = { Text("تاريخ النهاية (YYYY/MM/DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.US)
                    val startMillis = try { sdf.parse(startText)?.time ?: (System.currentTimeMillis() - 86400000L * 30) } catch (e: Exception) { System.currentTimeMillis() - 86400000L * 30 }
                    val endMillis = try { sdf.parse(endText)?.time ?: System.currentTimeMillis() } catch (e: Exception) { System.currentTimeMillis() }
                    onConfirm(startMillis, endMillis)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MikroTikPrimary)
            ) {
                Text("تطبيق الفلتر", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", fontFamily = CairoFontFamily)
            }
        },
        containerColor = CyberDarkSurface,
        shape = RoundedCornerShape(16.dp)
    )
}
