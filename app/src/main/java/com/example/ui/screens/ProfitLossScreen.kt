package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.ui.MainViewModel
import com.example.ui.theme.AssetPurple
import com.example.ui.theme.EquityBlue
import com.example.ui.theme.InvestmentGold
import com.example.ui.theme.MikroTikCyan
import com.example.ui.theme.MikroTikNavy
import com.example.ui.theme.MikroTikPrimary
import com.example.ui.theme.PaymentRed
import com.example.ui.theme.ProfitEmerald
import com.example.ui.theme.ReceiptGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfitLossScreen(
    viewModel: MainViewModel,
    onNavigateToAI: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val totalReceipts by viewModel.totalReceipts.collectAsState()
    val totalPayments by viewModel.totalPayments.collectAsState()
    val vouchers by viewModel.vouchers.collectAsState()
    val partners by viewModel.partners.collectAsState()
    val assets by viewModel.assets.collectAsState()
    val totalAssetPurchaseCost by viewModel.totalAssetPurchaseCost.collectAsState()
    val totalCurrentAssetValue by viewModel.totalCurrentAssetValue.collectAsState()
    val totalInvestedCapital by viewModel.totalInvestedCapital.collectAsState()

    val context = LocalContext.current

    val grossRevenue = totalReceipts ?: 0.0
    val totalOpex = remember(vouchers) {
        vouchers
            .filter {
                it.voucherType == "PAYMENT" &&
                !it.isVoided &&
                !it.category.contains("CAPEX", ignoreCase = true) &&
                !it.category.contains("أصول", ignoreCase = true)
            }
            .sumOf { it.amount.toDouble() }
    }
    val totalCapexAssets = remember(vouchers, totalAssetPurchaseCost) {
        val capexVouchersSum = vouchers
            .filter {
                it.voucherType == "PAYMENT" &&
                !it.isVoided &&
                (it.category.contains("CAPEX", ignoreCase = true) || it.category.contains("أصول", ignoreCase = true))
            }
            .sumOf { it.amount.toDouble() }
        capexVouchersSum + (totalAssetPurchaseCost ?: 0.0)
    }

    val netOperatingProfit = grossRevenue - totalOpex
    val profitMarginPercent = if (grossRevenue > 0) (netOperatingProfit / grossRevenue) * 100.0 else 0.0

    // Assets & Depreciation
    val initialAssetCost = totalAssetPurchaseCost ?: 0.0
    val currentAssetVal = totalCurrentAssetValue ?: 0.0
    val assetDepreciation = (initialAssetCost - currentAssetVal).coerceAtLeast(0.0)

    // Capital & ROI
    val investedCapital = totalInvestedCapital ?: 0.0
    val returnOnInvestmentPercent = if (investedCapital > 0) (netOperatingProfit / investedCapital) * 100.0 else 0.0

    // Expense breakdown by categories
    val expensesByCategory = remember(vouchers) {
        vouchers
            .filter { it.voucherType == "PAYMENT" }
            .groupBy { if (it.category.isNotBlank()) it.category else "مصاريف تشغيل عامة" }
            .mapValues { entry -> entry.value.sumOf { it.amount.toDouble() } }
            .toList()
            .sortedByDescending { it.second }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("profit_loss_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Card: Executive P&L Summary
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (netOperatingProfit >= 0) MikroTikNavy else Color(0xFF330A14)
                ),
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
                            Text(
                                text = "قائمة الأرباح والخسائر الشاملة (P&L)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Text(
                                text = "ميزان الإيرادات التشغيلية، تكاليف الشبكة، وصافي العائد",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (netOperatingProfit >= 0) ProfitEmerald.copy(alpha = 0.2f) else PaymentRed.copy(alpha = 0.2f)
                                )
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (netOperatingProfit >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                    contentDescription = null,
                                    tint = if (netOperatingProfit >= 0) ProfitEmerald else PaymentRed,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (netOperatingProfit >= 0) "مشروع رابح" else "عجز مالي",
                                    color = if (netOperatingProfit >= 0) ProfitEmerald else PaymentRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Big Number
                    Text(
                        text = "صافي الربح التشغيلي القابل للتوزيع",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${String.format(Locale.US, "%,.0f", netOperatingProfit)} ر.ي",
                        color = if (netOperatingProfit >= 0) ProfitEmerald else PaymentRed,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Ratios Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("هامش الربح الصافي", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", profitMarginPercent)}%",
                                    color = if (profitMarginPercent >= 0) ProfitEmerald else PaymentRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("العائد على الاستثمار ROI", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", returnOnInvestmentPercent)}%",
                                    color = InvestmentGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("إهلاك الأصول التقديري", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                Text(
                                    text = "${String.format(Locale.US, "%,.0f", assetDepreciation)} ر.ي",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Inflow vs Outflow Comparison Bar
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "التدفق النقدي: المقبوضات مقابل المصروفات",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(ReceiptGreen))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إجمالي الإيرادات: ${String.format(Locale.US, "%,.0f", grossRevenue)} ر.ي", fontSize = 12.sp)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(PaymentRed))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("المصروفات: ${String.format(Locale.US, "%,.0f", totalOpex)} ر.ي", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val totalFlow = (grossRevenue + totalOpex).coerceAtLeast(1.0)
                    LinearProgressIndicator(
                        progress = { (grossRevenue / totalFlow).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = ReceiptGreen,
                        trackColor = PaymentRed
                    )
                }
            }
        }

        // Section: Fixed Assets & Company Equity Card (CAPEX)
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, AssetPurple.copy(alpha = 0.4f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(AssetPurple.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = AssetPurple, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "إجمالي الأصول الثابتة والرأسمالية (CAPEX)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "قيمة المعدات والأبراج والأجهزة المملوكة للشبكة (حقوق الملكية)",
                                fontSize = 10.5.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Text(
                        text = "${String.format(Locale.US, "%,.0f", totalCapexAssets)} ر.ي",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = AssetPurple
                    )
                }
            }
        }

        // Section: Operating Expenses Breakdown (OPEX)
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تحليل بنود المصروفات التشغيلية (OPEX)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Icon(Icons.Default.PieChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (expensesByCategory.isEmpty()) {
                        Text(
                            text = "لا توجد سندات صرف أو مصروفات مسجلة بعد.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        expensesByCategory.forEach { (catName, amount) ->
                            val percent = if (totalOpex > 0) (amount / totalOpex) * 100.0 else 0.0
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(catName, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Text(
                                        text = "${String.format(Locale.US, "%,.0f", amount)} ر.ي (${String.format(Locale.US, "%.1f", percent)}%)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PaymentRed
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                LinearProgressIndicator(
                                    progress = { (percent / 100f).toFloat().coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = PaymentRed,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Projected Dividends Distribution to Partners
        if (partners.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "توزيع صافي الربح التقديري على الشركاء",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Icon(Icons.Default.Group, contentDescription = null, tint = InvestmentGold, modifier = Modifier.size(18.dp))
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        partners.forEach { p ->
                            val partnerShare = (netOperatingProfit.coerceAtLeast(0.0) * (p.sharePercentage / 100.0))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(EquityBlue.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(p.name.take(1), color = EquityBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(p.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text("${p.sharePercentage}% حصة ملكية", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Text(
                                    text = "${String.format(Locale.US, "%,.0f", partnerShare)} ر.ي",
                                    color = ProfitEmerald,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }

        // Action Buttons: Share P&L Statement & AI Financial Advice
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val reportText = """
                            📊 تقرير الأرباح والخسائر الاستثماري لشبكة الوايرلس:
                            💰 إجمالي الإيرادات (المقبوضات): ${String.format(Locale.US, "%,.0f", grossRevenue)} ريال
                            📉 إجمالي المصروفات التشغيلية: ${String.format(Locale.US, "%,.0f", totalOpex)} ريال
                            🌟 صافي الربح التشغيلي: ${String.format(Locale.US, "%,.0f", netOperatingProfit)} ريال
                            📈 هامش الربح الصافي: ${String.format(Locale.US, "%.1f", profitMarginPercent)}%
                            🏗️ إجمالي قيمة الأصول الرأسمالية: ${String.format(Locale.US, "%,.0f", currentAssetVal)} ريال
                            👥 عدد الشركاء المستثمرين: ${partners.size} شركاء
                            📅 تم الاستخراج في: ${SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale.US).format(Date())}
                        """.trimIndent()
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, reportText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "مشاركة قائمة الأرباح والخسائر"))
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("مشاركة التقرير", fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        onNavigateToAI(
                            "قدم لي تحليلاً مالياً ومحاسبياً استثمارياً لشبكتي: إجمالي الإيرادات ${grossRevenue.toInt()} ريال، والمصروفات ${totalOpex.toInt()} ريال، وقيمة الأصول ${currentAssetVal.toInt()} ريال. ما هي أفضل النصائح لتعظيم الأرباح وخفض تكاليف الديزل واشتراك النت للشركاء؟"
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MikroTikPrimary),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("استشارة مالية AI", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}
