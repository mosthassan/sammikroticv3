package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.entity.RetailerEntity
import com.example.data.local.model.GeneralLedgerLineRow
import com.example.ui.customers.CustomerStatementViewModel
import com.example.ui.theme.CairoFontFamily
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberDarkCanvas
import com.example.ui.theme.CyberDarkCardElevated
import com.example.ui.theme.CyberDarkSurface
import com.example.ui.theme.InvestmentGold
import com.example.ui.theme.MikroTikCyan
import com.example.ui.theme.MikroTikPrimary
import com.example.ui.theme.ReceiptGreen
import com.example.ui.theme.StatusOffline
import com.example.ui.theme.StatusOnline
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.theme.WhatsAppDarkGreen
import com.example.ui.theme.WhatsAppGreen
import com.example.util.WhatsAppHelper
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * نافذة كشف حساب تفصيلي للعميل / البقالة
 * يقرأ حصرياً من قيود الأستاذ العام (حساب 1201 ذمم العملاء والوكلاء)
 * لضمان مطابقة بنسبة 100% مع ميزان المراجعة والأستاذ العام
 */
@Composable
fun CustomerStatementDialog(
    retailer: RetailerEntity,
    onDismiss: () -> Unit,
    viewModel: CustomerStatementViewModel = viewModel()
) {
    val context = LocalContext.current

    LaunchedEffect(retailer.id) {
        viewModel.setCustomerId(retailer.id)
    }

    val summary by viewModel.statementSummary.collectAsStateWithLifecycle()
    val lines by viewModel.statementLines.collectAsStateWithLifecycle()

    // حساب الرصيد التراكمي بعد كل حركة
    val runningBalances = remember(lines) {
        var currentBal = BigDecimal.ZERO
        lines.map { line ->
            currentBal = currentBal.add(line.debit).subtract(line.credit)
            currentBal
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, CyberBorder, RoundedCornerShape(20.dp))
                .testTag("customer_statement_dialog"),
            color = CyberDarkCanvas
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. Header Bar
                StatementHeader(
                    retailer = retailer,
                    finalBalance = summary.finalBalance,
                    onDismiss = onDismiss
                )

                HorizontalDivider(color = CyberBorder, thickness = 1.dp)

                // 2. Summary KPI Cards (إجمالي المبيعات، المسدد، الصافي)
                StatementKpiSection(
                    totalSales = summary.totalSales,
                    totalPaid = summary.totalPaid,
                    finalBalance = summary.finalBalance
                )

                HorizontalDivider(color = CyberBorder, thickness = 1.dp)

                // 3. Transactions List
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    if (lines.isEmpty()) {
                        EmptyStatementView()
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 12.dp)
                        ) {
                            item {
                                StatementTableHeader()
                            }
                            itemsIndexed(lines, key = { _, item -> item.lineId }) { index, line ->
                                val runningBal = runningBalances.getOrElse(index) { BigDecimal.ZERO }
                                StatementRowItem(
                                    line = line,
                                    runningBalance = runningBal
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = CyberBorder, thickness = 1.dp)

                // 4. Bottom Actions Bar (واتساب، مشاركة، إغلاق)
                StatementBottomActions(
                    context = context,
                    retailer = retailer,
                    summary = summary,
                    lines = lines,
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@Composable
private fun StatementHeader(
    retailer: RetailerEntity,
    finalBalance: BigDecimal,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CyberDarkSurface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(InvestmentGold.copy(alpha = 0.15f))
                    .border(1.dp, InvestmentGold.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = InvestmentGold,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "كشف حساب: ${retailer.name}",
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimaryDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "المسؤول: ${retailer.ownerName.ifBlank { "غير محدد" }} • هاتف: ${retailer.phone.ifBlank { "غير مسجل" }}",
                    fontFamily = CairoFontFamily,
                    fontSize = 11.5.sp,
                    color = TextSecondaryDark
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Balance Badge
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "الرصيد المستحق:",
                fontFamily = CairoFontFamily,
                fontSize = 10.sp,
                color = TextSecondaryDark
            )
            Text(
                text = "${finalBalance.toInt()} ريال",
                fontFamily = CairoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (finalBalance > BigDecimal.ZERO) StatusWarning else StatusOnline
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(CyberDarkCardElevated)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "إغلاق",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun StatementKpiSection(
    totalSales: BigDecimal,
    totalPaid: BigDecimal,
    finalBalance: BigDecimal
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CyberDarkSurface.copy(alpha = 0.6f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // KPI 1: إجمالي المبيعات (مدين)
        KpiCard(
            modifier = Modifier.weight(1f),
            title = "إجمالي المبيعات (+)",
            amount = "${totalSales.toInt()} ر.ي",
            icon = Icons.Default.ArrowUpward,
            iconTint = StatusWarning,
            containerColor = StatusWarning.copy(alpha = 0.08f),
            borderColor = StatusWarning.copy(alpha = 0.3f)
        )

        // KPI 2: إجمالي السداد (دائن)
        KpiCard(
            modifier = Modifier.weight(1f),
            title = "إجمالي المسدد (-)",
            amount = "${totalPaid.toInt()} ر.ي",
            icon = Icons.Default.ArrowDownward,
            iconTint = ReceiptGreen,
            containerColor = ReceiptGreen.copy(alpha = 0.08f),
            borderColor = ReceiptGreen.copy(alpha = 0.3f)
        )

        // KPI 3: صافي الرصيد المستحق
        KpiCard(
            modifier = Modifier.weight(1f),
            title = "صافي المتبقي",
            amount = "${finalBalance.toInt()} ر.ي",
            icon = Icons.Default.AccountBalanceWallet,
            iconTint = if (finalBalance > BigDecimal.ZERO) MikroTikCyan else StatusOnline,
            containerColor = (if (finalBalance > BigDecimal.ZERO) MikroTikCyan else StatusOnline).copy(alpha = 0.08f),
            borderColor = (if (finalBalance > BigDecimal.ZERO) MikroTikCyan else StatusOnline).copy(alpha = 0.3f)
        )
    }
}

@Composable
private fun KpiCard(
    modifier: Modifier = Modifier,
    title: String,
    amount: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    containerColor: Color,
    borderColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    fontFamily = CairoFontFamily,
                    fontSize = 10.sp,
                    color = TextSecondaryDark,
                    maxLines = 1
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(13.dp)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = amount,
                fontFamily = CairoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                color = TextPrimaryDark,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun StatementTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CyberDarkCardElevated)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "التاريخ / نوع الحركة والرقم",
            fontFamily = CairoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = TextSecondaryDark,
            modifier = Modifier.weight(1.8f)
        )
        Text(
            text = "البيان / الوصف",
            fontFamily = CairoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = TextSecondaryDark,
            modifier = Modifier.weight(2f)
        )
        Text(
            text = "مدين (+)",
            fontFamily = CairoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = StatusWarning,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1.1f)
        )
        Text(
            text = "دائن (-)",
            fontFamily = CairoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = ReceiptGreen,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1.1f)
        )
        Text(
            text = "الرصيد",
            fontFamily = CairoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = MikroTikCyan,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.2f)
        )
    }
}

@Composable
private fun StatementRowItem(
    line: GeneralLedgerLineRow,
    runningBalance: BigDecimal
) {
    val isInvoice = line.debit > BigDecimal.ZERO
    val isVoucher = line.credit > BigDecimal.ZERO
    val dateStr = remember(line.dateMillis) {
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        sdf.format(Date(line.dateMillis))
    }

    val typeBadgeText = if (isInvoice) "فاتورة مبيعات" else if (isVoucher) "سند قبض" else "قيد تسوية"
    val typeBadgeColor = if (isInvoice) MikroTikPrimary else ReceiptGreen
    val refDisplay = line.referenceId.ifBlank { line.entryNumber }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
        border = BorderStroke(0.8.dp, CyberBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Col 1: Date & Type Badge
            Column(modifier = Modifier.weight(1.8f)) {
                Text(
                    text = dateStr,
                    fontSize = 10.sp,
                    color = TextSecondaryDark,
                    fontFamily = CairoFontFamily
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(typeBadgeColor.copy(alpha = 0.2f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = typeBadgeText,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = typeBadgeColor,
                            fontFamily = CairoFontFamily
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "#$refDisplay",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Col 2: Description
            Text(
                text = line.lineDescription.ifBlank { line.headerDescription },
                fontFamily = CairoFontFamily,
                fontSize = 10.5.sp,
                color = TextPrimaryDark,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(2f).padding(horizontal = 4.dp)
            )

            // Col 3: Debit
            Text(
                text = if (line.debit > BigDecimal.ZERO) "${line.debit.toInt()}" else "-",
                fontFamily = CairoFontFamily,
                fontWeight = if (line.debit > BigDecimal.ZERO) FontWeight.Bold else FontWeight.Normal,
                fontSize = 11.5.sp,
                color = if (line.debit > BigDecimal.ZERO) StatusWarning else TextSecondaryDark,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1.1f)
            )

            // Col 4: Credit
            Text(
                text = if (line.credit > BigDecimal.ZERO) "${line.credit.toInt()}" else "-",
                fontFamily = CairoFontFamily,
                fontWeight = if (line.credit > BigDecimal.ZERO) FontWeight.Bold else FontWeight.Normal,
                fontSize = 11.5.sp,
                color = if (line.credit > BigDecimal.ZERO) ReceiptGreen else TextSecondaryDark,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1.1f)
            )

            // Col 5: Running Balance
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.weight(1.2f)
            ) {
                Text(
                    text = "${runningBalance.toInt()}",
                    fontFamily = CairoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    color = if (runningBalance > BigDecimal.ZERO) StatusWarning else StatusOnline
                )
                Text(
                    text = if (runningBalance > BigDecimal.ZERO) "متبقي" else "مسدد",
                    fontSize = 8.5.sp,
                    fontFamily = CairoFontFamily,
                    color = TextSecondaryDark
                )
            }
        }
    }
}

@Composable
private fun EmptyStatementView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ReceiptLong,
                contentDescription = null,
                tint = TextSecondaryDark,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "لا توجد حركات مالية مسجلة بعد لهذا العميل",
                fontFamily = CairoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = TextSecondaryDark
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "جميع فواتير المبيعات وسندات القبض المسجلة تظهر تلقائياً في هذا الكشف من واقع حساب 1201 في الأستاذ العام.",
                fontFamily = CairoFontFamily,
                fontSize = 11.sp,
                color = TextSecondaryDark.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun StatementBottomActions(
    context: Context,
    retailer: RetailerEntity,
    summary: com.example.data.local.dao.CustomerStatementSummary,
    lines: List<GeneralLedgerLineRow>,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CyberDarkSurface)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // WhatsApp Share Button
        Button(
            onClick = {
                val formattedText = generateWhatsAppStatementText(
                    retailer = retailer,
                    summary = summary,
                    lines = lines
                )
                if (retailer.phone.isNotBlank()) {
                    WhatsAppHelper.sendWhatsAppMessage(context, retailer.phone, formattedText)
                } else {
                    WhatsAppHelper.shareTextIntent(context, formattedText, "مشاركة كشف الحساب")
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.height(38.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Chat,
                contentDescription = "واتساب",
                tint = WhatsAppDarkGreen,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "إرسال كشف الحساب عبر واتساب",
                fontFamily = CairoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp,
                color = WhatsAppDarkGreen
            )
        }

        // Close Button
        OutlinedButton(
            onClick = onDismiss,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, CyberBorder),
            modifier = Modifier.height(38.dp)
        ) {
            Text(
                text = "إغلاق",
                fontFamily = CairoFontFamily,
                fontSize = 12.sp,
                color = Color.White
            )
        }
    }
}

/**
 * صياغة نص كشف الحساب الموجه للعميل عبر الواتساب
 */
private fun generateWhatsAppStatementText(
    retailer: RetailerEntity,
    summary: com.example.data.local.dao.CustomerStatementSummary,
    lines: List<GeneralLedgerLineRow>
): String {
    val dateNow = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("ar")).format(Date())
    val builder = StringBuilder()

    builder.appendLine("📋 *كشف حساب تفصيلي - ${WhatsAppHelper.NETWORK_BRAND_NAME}*")
    builder.appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━")
    builder.appendLine("🏪 *العميل:* ${retailer.name}")
    if (retailer.ownerName.isNotBlank()) {
        builder.appendLine("👤 *المسؤول:* ${retailer.ownerName}")
    }
    builder.appendLine("📅 *تاريخ التقرير:* $dateNow")
    builder.appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━")
    builder.appendLine("📊 *الحركات الأخيرة:*")

    if (lines.isEmpty()) {
        builder.appendLine("لا توجد حركات مسجلة.")
    } else {
        var running = BigDecimal.ZERO
        val recentLines = lines.takeLast(15) // آخر 15 حركة لتفادي طول الرسالة
        for (line in recentLines) {
            running = running.add(line.debit).subtract(line.credit)
            val dStr = SimpleDateFormat("MM/dd", Locale.getDefault()).format(Date(line.dateMillis))
            val typeStr = if (line.debit > BigDecimal.ZERO) "فاتورة (+${line.debit.toInt()})" else "سداد (-${line.credit.toInt()})"
            val ref = line.referenceId.ifBlank { line.entryNumber }
            builder.appendLine("• $dStr | $typeStr | #$ref | الرصيد: ${running.toInt()} ر.ي")
        }
    }

    builder.appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━")
    builder.appendLine("📈 *إجمالي المبيعات:* ${summary.totalSales.toInt()} ريال")
    builder.appendLine("💵 *إجمالي المسدد:* ${summary.totalPaid.toInt()} ريال")
    builder.appendLine("⚖️ *الرصيد المستحق الحالي:* ${summary.finalBalance.toInt()} ريال")
    builder.appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━")
    builder.appendLine("📞 *لأي استفسار يرجى التواصل على:* ${WhatsAppHelper.NETWORK_SUPPORT_PHONE}")
    builder.appendLine("شكراً لتعاملكم معنا 🙏")

    return builder.toString()
}
