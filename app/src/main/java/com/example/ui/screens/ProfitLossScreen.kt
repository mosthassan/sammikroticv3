package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.model.IncomeStatementReport
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import java.math.BigDecimal
import java.text.DecimalFormat
import java.util.Calendar

/**
 * شاشة قائمة الدخل والأرباح المعيارية (Income Statement / P&L)
 * تقرأ حصرياً ومباشرة من جدول القيود الموزونة (Single Source of Truth):
 * - الإيرادات (4101 + 4201)
 * - (-) تكلفة البضاعة المباعة COGS (5101)
 * - = مجمل الربح (Gross Profit)
 * - (-) المصروفات التشغيلية والعمومية (5201 + 5202)
 * - (-) مصروف إهلاك أصول الشبكة (5203)
 * - = صافي الربح التشغيلي الحقيقي (Net Operating Profit)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfitLossScreen(
    viewModel: MainViewModel,
    onNavigateToAI: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val df = remember { DecimalFormat("#,##0.00") }

    // خيارات النطاق الزمني
    var selectedPeriodIndex by remember { androidx.compose.runtime.mutableIntStateOf(1) } // افتراضياً هذا الشهر
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

    // جلب تقرير قائمة الدخل من القيود الموزونة
    val report by viewModel.getIncomeStatementReport(dateRange.first, dateRange.second)
        .collectAsState(initial = null)

    var showClosingDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("profit_loss_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // بطاقة الرأس والتحكم
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "قائمة الدخل والأرباح المعيارية 📈",
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                    Text(
                        text = "مبنية حصرياً على القيود المحاسبية المزدوجة الموزونة",
                        fontFamily = CairoFontFamily,
                        fontSize = 11.5.sp,
                        color = TextSecondaryDark
                    )
                }

                Row {
                    IconButton(
                        onClick = {
                            val r = report ?: return@IconButton
                            val text = buildString {
                                appendLine("=== قائمة الدخل المعيارية (P&L) ===")
                                appendLine("الفترة: ${periodOptions[selectedPeriodIndex]}")
                                appendLine("1. إيرادات مبيعات الكروت (4101): ${df.format(r.cardSalesRevenue)} ر.ي")
                                appendLine("2. إيرادات الاشتراكات (4201): ${df.format(r.subscriptionRevenue)} ر.ي")
                                appendLine("-> إجمالي الإيرادات التشغيلية: ${df.format(r.totalOperationalRevenue)} ر.ي")
                                appendLine("3. تكلفة البضاعة المباعة COGS (5101): ${df.format(r.cogs)} ر.ي")
                                appendLine("-> مجمل الربح التشغيلي: ${df.format(r.grossProfit)} ر.ي (${String.format("%.1f", r.grossMarginPercentage)}%)")
                                appendLine("4. المصروفات التشغيلية (5201 + 5202): ${df.format(r.totalOpexExpenses)} ر.ي")
                                appendLine("5. قسط إهلاك الأصول (5203): ${df.format(r.depreciationExpense)} ر.ي")
                                appendLine("---------------------------------------")
                                appendLine("-> صافي الربح التشغيلي الحقيقي: ${df.format(r.netOperatingProfit)} ر.ي (${String.format("%.1f", r.netProfitMarginPercentage)}%)")
                            }
                            clipboardManager.setText(AnnotatedString(text))
                            Toast.makeText(context, "تم نسخ قائمة الدخل إلى الحافظة ✓", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "مشاركة", tint = MikroTikCyan)
                    }
                }
            }
        }

        // أزرار اختيار النطاق الزمني
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

        // بطاقة ملخص صافي الربح والنسب المالية الكبرى (Executive KPI Hero)
        item {
            val r = report
            val netProfit = r?.netOperatingProfit ?: BigDecimal.ZERO
            val isProfit = netProfit >= BigDecimal.ZERO

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isProfit) MikroTikNavy else Color(0xFF330A14)
                ),
                border = BorderStroke(1.dp, if (isProfit) ProfitEmerald.copy(alpha = 0.5f) else PaymentRed),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "صافي الربح التشغيلي الحقيقي للفترة",
                            fontFamily = CairoFontFamily,
                            fontSize = 12.5.sp,
                            color = Color(0xFF94A3B8)
                        )

                        Surface(
                            color = if (isProfit) ProfitEmerald.copy(alpha = 0.2f) else PaymentRed.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (isProfit) "ربح صافٍ تشغيلي ✓" else "عجز / خسارة!",
                                fontFamily = CairoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                color = if (isProfit) ProfitEmerald else PaymentRed,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${df.format(netProfit)} ر.ي",
                        fontFamily = CairoFontFamily,
                        color = if (isProfit) ProfitEmerald else PaymentRed,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // النسب المالية المعيارية
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
                                Text("هامش مجمل الربح", fontFamily = CairoFontFamily, fontSize = 10.sp, color = Color(0xFF94A3B8))
                                Text(
                                    text = "${String.format("%.1f", r?.grossMarginPercentage ?: 0.0)}%",
                                    fontFamily = CairoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = InvestmentGold
                                )
                            }
                        }

                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("هامش صافي الربح", fontFamily = CairoFontFamily, fontSize = 10.sp, color = Color(0xFF94A3B8))
                                Text(
                                    text = "${String.format("%.1f", r?.netProfitMarginPercentage ?: 0.0)}%",
                                    fontFamily = CairoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isProfit) ProfitEmerald else PaymentRed
                                )
                            }
                        }

                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("إهلاك الأصول (5203)", fontFamily = CairoFontFamily, fontSize = 10.sp, color = Color(0xFF94A3B8))
                                Text(
                                    text = "${df.format(r?.depreciationExpense ?: BigDecimal.ZERO)}",
                                    fontFamily = CairoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = AssetPurple
                                )
                            }
                        }
                    }
                }
            }
        }

        // تفاصيل شجرة قائمة الدخل التفصيلية (Income Statement Waterfall Breakdown)
        item {
            val r = report
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
                border = BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "تفاصيل بنود قائمة الدخل (Income Statement Breakdown):",
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 1. الإيرادات التشغيلية
                    IncomeStatementSectionHeader(
                        title = "1. الإيرادات التشغيلية (Revenues)",
                        totalAmount = r?.totalOperationalRevenue ?: BigDecimal.ZERO,
                        df = df,
                        color = ReceiptGreen
                    )
                    IncomeStatementLineItem(
                        code = "4101",
                        name = "إيرادات مبيعات كروت الشبكة",
                        amount = r?.cardSalesRevenue ?: BigDecimal.ZERO,
                        df = df,
                        color = ReceiptGreen
                    )
                    IncomeStatementLineItem(
                        code = "4201",
                        name = "إيرادات الاشتراكات المباشرة والخدمات",
                        amount = r?.subscriptionRevenue ?: BigDecimal.ZERO,
                        df = df,
                        color = ReceiptGreen
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = CyberBorder, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. تكلفة الخدمات المباشرة (COGS - ستارلينك)
                    IncomeStatementSectionHeader(
                        title = "2. (-) تكلفة الخدمات المباشرة / COGS (5101)",
                        totalAmount = r?.cogs ?: BigDecimal.ZERO,
                        df = df,
                        color = PaymentRed
                    )
                    IncomeStatementLineItem(
                        code = "5101",
                        name = "اشتراك النت الرئيسي (ستارلينك)",
                        amount = r?.cogs ?: BigDecimal.ZERO,
                        df = df,
                        color = PaymentRed
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = CyberBorder, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    // 3. مجمل الربح التشغيلي
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(InvestmentGold.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "(=) مجمل الربح التشغيلي (Gross Profit)",
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = InvestmentGold
                        )
                        Text(
                            text = "${df.format(r?.grossProfit ?: BigDecimal.ZERO)} ر.ي",
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = InvestmentGold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 4. المصروفات التشغيلية والعمومية
                    IncomeStatementSectionHeader(
                        title = "3. (-) المصروفات التشغيلية والصيانة (Opex)",
                        totalAmount = r?.totalOpexExpenses ?: BigDecimal.ZERO,
                        df = df,
                        color = PaymentRed
                    )
                    IncomeStatementLineItem(
                        code = "5201",
                        name = "مصروفات تشغيلية وعمومية (وقود، ديزل، خطوط نت، إيجارات)",
                        amount = r?.operatingExpenses ?: BigDecimal.ZERO,
                        df = df,
                        color = PaymentRed
                    )
                    IncomeStatementLineItem(
                        code = "5202",
                        name = "مصروفات صيانة وقطع غيار ومعدات",
                        amount = r?.maintenanceExpenses ?: BigDecimal.ZERO,
                        df = df,
                        color = PaymentRed
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = CyberBorder, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    // 5. قسط إهلاك الأصول الثابتة
                    IncomeStatementSectionHeader(
                        title = "4. (-) قسط إهلاك الأصول الثابتة (Depreciation)",
                        totalAmount = r?.depreciationExpense ?: BigDecimal.ZERO,
                        df = df,
                        color = AssetPurple
                    )
                    IncomeStatementLineItem(
                        code = "5203",
                        name = "مصروف إهلاك أصول الشبكة (سيرفرات، أبراج، طاقة شمسية)",
                        amount = r?.depreciationExpense ?: BigDecimal.ZERO,
                        df = df,
                        color = AssetPurple
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 6. صافي الربح التشغيلي النهائي
                    val finalNet = r?.netOperatingProfit ?: BigDecimal.ZERO
                    val finalIsProfit = finalNet >= BigDecimal.ZERO
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (finalIsProfit) ProfitEmerald.copy(alpha = 0.18f) else PaymentRed.copy(alpha = 0.18f),
                                RoundedCornerShape(10.dp)
                            )
                            .border(
                                1.dp,
                                if (finalIsProfit) ProfitEmerald else PaymentRed,
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "(=) صافي الربح التشغيلي الحقيقي (Net Profit)",
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (finalIsProfit) ProfitEmerald else PaymentRed
                        )
                        Text(
                            text = "${df.format(finalNet)} ر.ي",
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (finalIsProfit) ProfitEmerald else PaymentRed
                        )
                    }
                }
            }
        }

        // أزرار العمليات: استشارة الذكاء الاصطناعي وإقفال الفترة المالية
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val r = report
                        val prompt = if (r != null) {
                            "أنا مهندس شبكة سلكية ولاسلكية، هذه هي نتائج قائمة الدخل الرسمية للدورة الحالية (${periodOptions[selectedPeriodIndex]}):\n" +
                            "- إيرادات مبيعات الكروت: ${df.format(r.cardSalesRevenue)} ر.ي\n" +
                            "- إيرادات الاشتراكات: ${df.format(r.subscriptionRevenue)} ر.ي\n" +
                            "- إجمالي الإيرادات: ${df.format(r.totalOperationalRevenue)} ر.ي\n" +
                            "- تكلفة البضاعة المباعة COGS: ${df.format(r.cogs)} ر.ي (الهامش الإجمالي: ${String.format("%.1f", r.grossMarginPercentage)}%)\n" +
                            "- المصروفات التشغيلية والعمومية: ${df.format(r.operatingExpenses)} ر.ي\n" +
                            "- مصروفات الصيانة: ${df.format(r.maintenanceExpenses)} ر.ي\n" +
                            "- إهلاك أصول الشبكة: ${df.format(r.depreciationExpense)} ر.ي\n" +
                            "- صافي الربح التشغيلي: ${df.format(r.netOperatingProfit)} ر.ي (هامش الصافي: ${String.format("%.1f", r.netProfitMarginPercentage)}%)\n" +
                            "قدم لي تحليلاً محاسبياً تشغيلياً دقيقاً مع 3 توصيات ذكية لخفض تكلفة الكروت وزيادة أرباح الشركاء."
                        } else {
                            "قدم لي تحليلاً محاسبياً لقائمة الدخل وتوصيات لتحسين الأداء المالي للشبكة."
                        }
                        onNavigateToAI(prompt)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MikroTikCyan)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("استشارة الذكاء الاصطناعي لتحليل قائمة الدخل 🤖", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = { showClosingDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, InvestmentGold),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = InvestmentGold)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = InvestmentGold, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إقفال الدورة المالية وتوزيع الأرباح على الشركاء (3201)", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // نافذة تأكيد إقفال الدورة المالية وتوزيع الأرباح
    if (showClosingDialog) {
        AlertDialog(
            onDismissRequest = { showClosingDialog = false },
            title = {
                Text("إقفال الدورة المالية وتوزيع الأرباح ⚖️", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            },
            text = {
                val r = report
                Column {
                    Text(
                        text = "سيتم توليد قيد إقفال معتمد في دفتر اليومية:\n" +
                               "• إقفال الإيرادات (مدين 4101 و 4201)\n" +
                               "• إقفال التكاليف والمصروفات (دائن 5101 و 5201 و 5202 و 5203)\n" +
                               "• ترحيل صافي الربح (${df.format(r?.netOperatingProfit ?: BigDecimal.ZERO)} ر.ي) إلى الحسابات الجارية للشركاء (3201) وفق نسب الحصص مع حفظ رأس المال الأساسي (3101) دون مساس.",
                        fontFamily = CairoFontFamily,
                        fontSize = 12.5.sp,
                        color = TextSecondaryDark
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.executePeriodClosingEntry(
                            startDateMillis = dateRange.first,
                            endDateMillis = dateRange.second,
                            closingPeriodName = periodOptions[selectedPeriodIndex],
                            performedBy = "المهندس سام"
                        ) { headerId ->
                            showClosingDialog = false
                            if (headerId > 0) {
                                Toast.makeText(context, "تم إقفال الدورة المالية وترحيل الأرباح بنجاح ✓ (قيد #$headerId)", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "تم تنفيذ الإقفال أو لا توجد حركات للتوزيع", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ProfitEmerald)
                ) {
                    Text("تأكيد وترحيل الإقفال", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClosingDialog = false }) {
                    Text("إلغاء", fontFamily = CairoFontFamily, color = TextSecondaryDark)
                }
            },
            containerColor = CyberDarkCardElevated
        )
    }
}

@Composable
fun IncomeStatementSectionHeader(
    title: String,
    totalAmount: BigDecimal,
    df: DecimalFormat,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontFamily = CairoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = Color.White
        )
        Text(
            text = "${df.format(totalAmount)} ر.ي",
            fontFamily = CairoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = color
        )
    }
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
fun IncomeStatementLineItem(
    code: String,
    name: String,
    amount: BigDecimal,
    df: DecimalFormat,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                color = color.copy(alpha = 0.15f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = code,
                    fontFamily = CairoFontFamily,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = name,
                fontFamily = CairoFontFamily,
                fontSize = 11.5.sp,
                color = TextSecondaryDark,
                maxLines = 1
            )
        }

        Text(
            text = "${df.format(amount)} ر.ي",
            fontFamily = CairoFontFamily,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}
