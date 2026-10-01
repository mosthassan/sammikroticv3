package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.PartnerEntity
import com.example.data.local.entity.PartnerTransactionEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.EquityBlue
import com.example.ui.theme.InvestmentGold
import com.example.ui.theme.MikroTikCyan
import com.example.ui.theme.MikroTikNavy
import com.example.ui.theme.MikroTikPrimary
import com.example.ui.theme.PaymentRed
import com.example.ui.theme.ProfitEmerald
import com.example.ui.theme.ReceiptGreen
import com.example.util.CurrencyHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PartnersScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val partners by viewModel.partners.collectAsState()
    val totalInvestedCapital by viewModel.totalInvestedCapital.collectAsState()
    val totalDistributedProfits by viewModel.totalDistributedProfits.collectAsState()
    val partnerTransactions by viewModel.partnerTransactions.collectAsState()
    val vouchers by viewModel.vouchers.collectAsState()
    val totalReceipts by viewModel.totalReceipts.collectAsState()
    val totalPayments by viewModel.totalPayments.collectAsState()
    val networkIdentity by viewModel.networkIdentity.collectAsState()

    val context = LocalContext.current

    var showAddPartnerDialog by remember { mutableStateOf(false) }
    var showPayoutDialog by remember { mutableStateOf(false) }
    var partnerToEdit by remember { mutableStateOf<PartnerEntity?>(null) }
    var partnerToDelete by remember { mutableStateOf<PartnerEntity?>(null) }
    var selectedPartnerForPayout by remember { mutableStateOf<PartnerEntity?>(null) }

    val investedTotal = totalInvestedCapital ?: 0.0
    val distributedTotal = totalDistributedProfits ?: 0.0
    val receiptsVal = totalReceipts ?: 0.0
    val paymentsVal = totalPayments ?: 0.0
    val netOperatingProfit = (receiptsVal - paymentsVal).coerceAtLeast(0.0)

    Box(modifier = modifier.fillMaxSize().testTag("partners_screen")) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Hero Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MikroTikNavy),
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
                                    text = "إدارة الشركاء ورأس المال الاستثماري",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )
                                Text(
                                    text = "توزيع الحصص، رأس المال المدفوع، وقسمة الأرباح الدورية",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(InvestmentGold.copy(alpha = 0.2f))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Group,
                                        contentDescription = null,
                                        tint = InvestmentGold,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${partners.size} شركاء",
                                        color = InvestmentGold,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Financial Overview Mini Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Capital Invested
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "إجمالي رأس المال",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${String.format(Locale.US, "%,.0f", investedTotal)} ر.ي",
                                        color = InvestmentGold,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            // Available Profit For Distribution
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "أرباح التشغيل الصافية",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${String.format(Locale.US, "%,.0f", netOperatingProfit)} ر.ي",
                                        color = ProfitEmerald,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            // Distributed Profits
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "المسحوبات والأرباح الموزعة",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${String.format(Locale.US, "%,.0f", distributedTotal)} ر.ي",
                                        color = MikroTikCyan,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section: Partners List & Dividends Calculator
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "جدول حصص الشركاء ومستحقاتهم",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Button(
                        onClick = {
                            selectedPartnerForPayout = partners.firstOrNull()
                            showPayoutDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ProfitEmerald),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        enabled = partners.isNotEmpty(),
                        modifier = Modifier.testTag("btn_distribute_dividends")
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("توزيع أرباح", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (partners.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Group,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "لم يتم إضافة شركاء حتى الآن",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "اضغط على زر (+) في الأسفل لإضافة بيانات الشركاء ورؤوس أموالهم ونسبهم.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(partners, key = { it.id }) { partner ->
                    // Partner Investment Card
                    val expectedDividendShare = (netOperatingProfit * (partner.sharePercentage / 100.0))
                    PartnerCard(
                        partner = partner,
                        expectedDividend = expectedDividendShare,
                        totalCapital = investedTotal,
                        onEdit = {
                            partnerToEdit = partner
                            showAddPartnerDialog = true
                        },
                        onDelete = {
                            partnerToDelete = partner
                        },
                        onPayout = {
                            selectedPartnerForPayout = partner
                            showPayoutDialog = true
                        },
                        onShareReport = {
                            val shareText = """
                                📊 كشف حصة الشريك في مشروع شبكة الوايرلس:
                                👤 الشريك: ${partner.name}
                                📞 الهاتف: ${partner.phone}
                                💰 رأس المال المستثمر: ${String.format(Locale.US, "%,.0f", partner.capitalInvested.toDouble())} ريال
                                📈 نسبة الشراكة: ${partner.sharePercentage}%
                                💵 إجمالي الأرباح المستلمة حتى الآن: ${String.format(Locale.US, "%,.0f", partner.totalWithdrawnProfit.toDouble())} ريال
                                🌟 حصة الأرباح المستحقة حالياً: ${String.format(Locale.US, "%,.0f", expectedDividendShare)} ريال
                                📝 ملاحظات: ${partner.notes.ifBlank { "مشروع استثماري مستقر" }}
                            """.trimIndent()
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "مشاركة تقرير الشريك"))
                        }
                    )
                }
            }

            // Section: History of Partner Transactions (Dividends & Capital Additions)
            if (partnerTransactions.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "سجل حركات وسندات الشركاء الأخيرة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                items(partnerTransactions.take(8), key = { it.id }) { tx ->
                    PartnerTransactionItem(
                        transaction = tx,
                        onDelete = {
                            viewModel.deletePartnerTransaction(tx)
                            Toast.makeText(context, "تم حذف الحركة وإلغاء القيد بنجاح", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // Bottom Spacing for FAB
            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }

        // Add Partner FAB
        FloatingActionButton(
            onClick = {
                partnerToEdit = null
                showAddPartnerDialog = true
            },
            containerColor = InvestmentGold,
            contentColor = Color.Black,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("fab_add_partner")
        ) {
            Icon(Icons.Default.Add, contentDescription = "إضافة شريك جديد")
        }
    }

    // Add / Edit Partner Dialog
    if (showAddPartnerDialog) {
        AddEditPartnerDialog(
            partnerToEdit = partnerToEdit,
            sarToYerRate = if (networkIdentity.sarToYerRate > 0) networkIdentity.sarToYerRate else 430.0,
            usdToYerRate = if (networkIdentity.usdToYerRate > 0) networkIdentity.usdToYerRate else 1630.0,
            onDismiss = { showAddPartnerDialog = false },
            onSave = { partner ->
                viewModel.savePartner(partner) {
                    Toast.makeText(context, "تم حفظ بيانات الشريك بنجاح", Toast.LENGTH_SHORT).show()
                }
                showAddPartnerDialog = false
            }
        )
    }

    // Dividend Payout Dialog
    if (showPayoutDialog) {
        DividendPayoutDialog(
            partners = partners,
            preSelectedPartner = selectedPartnerForPayout,
            availableOperatingProfit = netOperatingProfit,
            sarToYerRate = if (networkIdentity.sarToYerRate > 0) networkIdentity.sarToYerRate else 430.0,
            usdToYerRate = if (networkIdentity.usdToYerRate > 0) networkIdentity.usdToYerRate else 1630.0,
            onDismiss = { showPayoutDialog = false },
            onConfirmPayout = { tx ->
                viewModel.recordPartnerTransaction(tx) {
                    Toast.makeText(context, "تم قيد صرف الأرباح وإصدار السند للشريك بنجاح", Toast.LENGTH_LONG).show()
                }
                showPayoutDialog = false
            }
        )
    }

    // Delete Partner Confirmation
    if (partnerToDelete != null) {
        AlertDialog(
            onDismissRequest = { partnerToDelete = null },
            title = { Text("حذف الشريك", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من حذف الشريك (${partnerToDelete?.name})؟ سيتم حذف بيانات حصته من النظام.") },
            confirmButton = {
                Button(
                    onClick = {
                        partnerToDelete?.let { viewModel.deletePartner(it) }
                        partnerToDelete = null
                        Toast.makeText(context, "تم حذف الشريك", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PaymentRed)
                ) {
                    Text("تأكيد الحذف", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { partnerToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun PartnerCard(
    partner: PartnerEntity,
    expectedDividend: Double,
    totalCapital: Double,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPayout: () -> Unit,
    onShareReport: () -> Unit
) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(EquityBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = partner.name.take(1),
                            color = EquityBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = partner.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (partner.phone.isNotBlank()) {
                            Text(
                                text = partner.phone,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Share % Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(InvestmentGold.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${partner.sharePercentage}% من الأرباح",
                        color = InvestmentGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Equity Progress
            LinearProgressIndicator(
                progress = { (partner.sharePercentage / 100f).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = EquityBlue,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Financial breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("رأس المال المدفوع", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${String.format(Locale.US, "%,.0f", partner.capitalInvested.toDouble())} ر.ي",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (partner.currency.isNotBlank() && partner.currency != "YER" && partner.originalCapital > java.math.BigDecimal.ZERO) {
                        Text(
                            text = CurrencyHelper.formatAmount(partner.originalCapital.toDouble(), partner.currency),
                            fontSize = 10.sp,
                            color = InvestmentGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("الأرباح المستلمة", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${String.format(Locale.US, "%,.0f", partner.totalWithdrawnProfit.toDouble())} ر.ي",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = ProfitEmerald
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("المستحق بالدورة الحالية", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${String.format(Locale.US, "%,.0f", expectedDividend)} ر.ي",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = InvestmentGold
                    )
                }
            }

            if (partner.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "ملاحظات: ${partner.notes}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onShareReport, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Share, contentDescription = "مشاركة كشف الشريك", tint = MikroTikPrimary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = PaymentRed, modifier = Modifier.size(18.dp))
                }

                Spacer(modifier = Modifier.width(6.dp))

                OutlinedButton(
                    onClick = onPayout,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ProfitEmerald),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.MonetizationOn, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("صرف أرباح", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PartnerTransactionItem(
    transaction: PartnerTransactionEntity,
    onDelete: () -> Unit
) {
    val isDividend = transaction.transactionType == "DIVIDEND_PAYOUT"
    val isCapital = transaction.transactionType == "CAPITAL_ADDITION"

    val typeLabel = when (transaction.transactionType) {
        "DIVIDEND_PAYOUT" -> "صرف أرباح"
        "CAPITAL_ADDITION" -> "إيداع رأس مال"
        else -> "سحب خاص"
    }

    val typeColor = when (transaction.transactionType) {
        "DIVIDEND_PAYOUT" -> ProfitEmerald
        "CAPITAL_ADDITION" -> InvestmentGold
        else -> PaymentRed
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(typeColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isDividend) Icons.Default.MonetizationOn else Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = typeColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "${transaction.partnerName} ($typeLabel)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale.US).format(Date(transaction.dateMillis))} • ${transaction.notes.ifBlank { "عملية قياسية" }}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${String.format(Locale.US, "%,.0f", transaction.amount)} ر.ي",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = typeColor
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف القيد", tint = PaymentRed.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun AddEditPartnerDialog(
    partnerToEdit: PartnerEntity?,
    sarToYerRate: Double,
    usdToYerRate: Double,
    onDismiss: () -> Unit,
    onSave: (PartnerEntity) -> Unit
) {
    var name by remember { mutableStateOf(partnerToEdit?.name ?: "") }
    var phone by remember { mutableStateOf(partnerToEdit?.phone ?: "") }
    var selectedCurrency by remember { mutableStateOf(partnerToEdit?.currency?.ifBlank { "YER" } ?: "YER") }

    val initialCapital = if (partnerToEdit != null && partnerToEdit.currency != "YER" && partnerToEdit.originalCapitalDouble > 0) {
        partnerToEdit.originalCapitalDouble
    } else {
        partnerToEdit?.capitalInvestedDouble ?: 0.0
    }
    var capitalText by remember { mutableStateOf(if (initialCapital > 0) initialCapital.toInt().toString() else "") }
    var percentageText by remember { mutableStateOf(partnerToEdit?.sharePercentage?.toString() ?: "") }
    var notes by remember { mutableStateOf(partnerToEdit?.notes ?: "") }

    val rawEnteredCapital = capitalText.toDoubleOrNull() ?: 0.0
    val equivalentYerCapital = CurrencyHelper.convertToYer(rawEnteredCapital, selectedCurrency, sarToYerRate, usdToYerRate)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (partnerToEdit == null) "إضافة شريك جديد" else "تعديل بيانات الشريك",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الشريك الكامل *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_partner_name")
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف للتواصل والواتساب") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Currency selector chips
                Column {
                    Text("عملة رأس المال المستثمر:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            CurrencyHelper.CURRENCY_YER to "ريال يمني",
                            CurrencyHelper.CURRENCY_SAR to "ريال سعودي",
                            CurrencyHelper.CURRENCY_USD to "دولار أمريكي"
                        ).forEach { (cKey, cLabel) ->
                            val isSelected = selectedCurrency == cKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) InvestmentGold else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .clickable { selectedCurrency = cKey }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cLabel,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = capitalText,
                    onValueChange = { capitalText = it },
                    label = { Text("رأس المال المستثمر (${CurrencyHelper.getCurrencySymbol(selectedCurrency)}) *") },
                    placeholder = { Text("مثال: 1000000") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_partner_capital")
                )

                // Currency conversion notice card
                if (selectedCurrency != CurrencyHelper.CURRENCY_YER && rawEnteredCapital > 0) {
                    val rate = if (selectedCurrency == CurrencyHelper.CURRENCY_USD) usdToYerRate else sarToYerRate
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = InvestmentGold.copy(alpha = 0.12f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "المعادل بالريال اليمني (سعر الصرف: $rate):",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${String.format(Locale.US, "%,.0f", equivalentYerCapital)} ر.ي",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = InvestmentGold
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = percentageText,
                    onValueChange = { percentageText = it },
                    label = { Text("نسبة الشراكة من الأرباح (%) *") },
                    placeholder = { Text("مثال: 50 أو 33.3") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_partner_percent")
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات وشروط الاتفاق") },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val entered = capitalText.toDoubleOrNull() ?: 0.0
                        val capitalInYer = CurrencyHelper.convertToYer(entered, selectedCurrency, sarToYerRate, usdToYerRate)
                        val originalCapital = if (selectedCurrency != CurrencyHelper.CURRENCY_YER) entered else 0.0
                        val percent = percentageText.toDoubleOrNull() ?: 0.0

                        val partner = partnerToEdit?.copy(
                            name = name,
                            phone = phone,
                            capitalInvested = java.math.BigDecimal.valueOf(capitalInYer),
                            currency = selectedCurrency,
                            originalCapital = java.math.BigDecimal.valueOf(originalCapital),
                            sharePercentage = percent,
                            notes = notes
                        ) ?: PartnerEntity(
                            name = name,
                            phone = phone,
                            capitalInvested = java.math.BigDecimal.valueOf(capitalInYer),
                            currency = selectedCurrency,
                            originalCapital = java.math.BigDecimal.valueOf(originalCapital),
                            sharePercentage = percent,
                            notes = notes
                        )
                        onSave(partner)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = InvestmentGold),
                modifier = Modifier.testTag("btn_save_partner")
            ) {
                Text("حفظ الشريك", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DividendPayoutDialog(
    partners: List<PartnerEntity>,
    preSelectedPartner: PartnerEntity?,
    availableOperatingProfit: Double,
    sarToYerRate: Double,
    usdToYerRate: Double,
    onDismiss: () -> Unit,
    onConfirmPayout: (PartnerTransactionEntity) -> Unit
) {
    var selectedPartner by remember { mutableStateOf(preSelectedPartner ?: partners.firstOrNull()) }
    var isPartnerDropdownExpanded by remember { mutableStateOf(false) }
    var selectedCurrency by remember { mutableStateOf(CurrencyHelper.CURRENCY_YER) }
    var payoutAmountText by remember {
        val suggestedAmount = ((availableOperatingProfit * ((selectedPartner?.sharePercentage ?: 0.0) / 100.0))).toInt()
        mutableStateOf(if (suggestedAmount > 0) suggestedAmount.toString() else "")
    }
    var notes by remember { mutableStateOf("صرف أرباح دورية للشبكة") }

    val rawEnteredPayout = payoutAmountText.toDoubleOrNull() ?: 0.0
    val equivalentYerPayout = CurrencyHelper.convertToYer(rawEnteredPayout, selectedCurrency, sarToYerRate, usdToYerRate)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = ProfitEmerald)
                Spacer(modifier = Modifier.width(8.dp))
                Text("سند توزيع وصرف أرباح", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "الأرباح الصافية المتاحة حالياً للتوزيع: ${String.format(Locale.US, "%,.0f", availableOperatingProfit)} ريال يمني",
                    fontSize = 12.sp,
                    color = ProfitEmerald,
                    fontWeight = FontWeight.Bold
                )

                // Partner Selector
                ExposedDropdownMenuBox(
                    expanded = isPartnerDropdownExpanded,
                    onExpandedChange = { isPartnerDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedPartner?.name ?: "اختر الشريك",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("الشريك المستفيد") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPartnerDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isPartnerDropdownExpanded,
                        onDismissRequest = { isPartnerDropdownExpanded = false }
                    ) {
                        partners.forEach { p ->
                            DropdownMenuItem(
                                text = { Text("${p.name} (${p.sharePercentage}%)") },
                                onClick = {
                                    selectedPartner = p
                                    val suggested = ((availableOperatingProfit * (p.sharePercentage / 100.0))).toInt()
                                    if (suggested > 0) payoutAmountText = suggested.toString()
                                    isPartnerDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Currency selector chips
                Column {
                    Text("عملة صرف وتوزيع الأرباح:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            CurrencyHelper.CURRENCY_YER to "ريال يمني",
                            CurrencyHelper.CURRENCY_SAR to "ريال سعودي",
                            CurrencyHelper.CURRENCY_USD to "دولار أمريكي"
                        ).forEach { (cKey, cLabel) ->
                            val isSelected = selectedCurrency == cKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) ProfitEmerald else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .clickable { selectedCurrency = cKey }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cLabel,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = payoutAmountText,
                    onValueChange = { payoutAmountText = it },
                    label = { Text("مبلغ الأرباح المصروف (${CurrencyHelper.getCurrencySymbol(selectedCurrency)}) *") },
                    placeholder = { Text("مثال: 50000") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_dividend_amount")
                )

                // Currency conversion notice card
                if (selectedCurrency != CurrencyHelper.CURRENCY_YER && rawEnteredPayout > 0) {
                    val rate = if (selectedCurrency == CurrencyHelper.CURRENCY_USD) usdToYerRate else sarToYerRate
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = ProfitEmerald.copy(alpha = 0.1f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "المعادل بالريال اليمني (سعر الصرف: $rate):",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${String.format(Locale.US, "%,.0f", equivalentYerPayout)} ر.ي",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = ProfitEmerald
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("البيان وملاحظات السند") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val entered = payoutAmountText.toDoubleOrNull() ?: 0.0
                    val amountInYer = CurrencyHelper.convertToYer(entered, selectedCurrency, sarToYerRate, usdToYerRate)
                    val originalAmount = if (selectedCurrency != CurrencyHelper.CURRENCY_YER) entered else 0.0
                    val partner = selectedPartner
                    if (amountInYer > 0 && partner != null) {
                        onConfirmPayout(
                            PartnerTransactionEntity(
                                partnerId = partner.id,
                                partnerName = partner.name,
                                transactionType = "DIVIDEND_PAYOUT",
                                amount = java.math.BigDecimal.valueOf(amountInYer),
                                currency = selectedCurrency,
                                originalAmount = java.math.BigDecimal.valueOf(originalAmount),
                                notes = notes
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ProfitEmerald),
                modifier = Modifier.testTag("btn_confirm_dividend_payout")
            ) {
                Text("تأكيد وصرف الأرباح", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
