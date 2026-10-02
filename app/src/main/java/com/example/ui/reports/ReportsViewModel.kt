package com.example.ui.reports

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.GeminiAiService
import com.example.data.local.AppDatabase
import com.example.data.local.entity.CardSalesInvoiceEntity
import com.example.data.local.entity.FinancialVoucherEntity
import com.example.data.local.entity.NetworkIdentityEntity
import com.example.data.local.entity.RetailerEntity
import com.example.data.repository.NetworkRepository
import com.example.util.ReportPdfManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class TimeRangeOption(val label: String) {
    TODAY("اليوم"),
    THIS_WEEK("هذا الأسبوع"),
    THIS_MONTH("هذا الشهر"),
    CUSTOM("مخصص")
}

data class FinancialReportSummary(
    val totalSales: Double = 0.0,
    val totalReceipts: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val totalCapexAssets: Double = 0.0,
    val estimatedCostOfGoods: Double = 0.0,
    val assetDepreciation: Double = 0.0,
    val netTrueProfit: Double = 0.0,
    val profitMarginPercent: Double = 0.0,
    val topAgentsBySales: List<Pair<RetailerEntity, Double>> = emptyList(),
    val topAgingDebtors: List<Pair<RetailerEntity, Double>> = emptyList()
)

class ReportsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = NetworkRepository(db)
    private val aiService = GeminiAiService(application.applicationContext)

    // Time Range Selection State
    private val _selectedTimeRange = MutableStateFlow(TimeRangeOption.THIS_MONTH)
    val selectedTimeRange: StateFlow<TimeRangeOption> = _selectedTimeRange.asStateFlow()

    private val _customStartMillis = MutableStateFlow<Long>(getStartOfMonthMillis())
    val customStartMillis: StateFlow<Long> = _customStartMillis.asStateFlow()

    private val _customEndMillis = MutableStateFlow<Long>(System.currentTimeMillis())
    val customEndMillis: StateFlow<Long> = _customEndMillis.asStateFlow()

    fun setTimeRangeOption(option: TimeRangeOption) {
        _selectedTimeRange.value = option
    }

    fun setCustomDateRange(startMillis: Long, endMillis: Long) {
        _customStartMillis.value = startMillis
        _customEndMillis.value = endMillis
        _selectedTimeRange.value = TimeRangeOption.CUSTOM
    }

    // Network Identity
    val networkIdentity: StateFlow<NetworkIdentityEntity> = repository.networkIdentity
        .map { it ?: NetworkIdentityEntity() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, NetworkIdentityEntity())

    // Raw Flows from Repository
    val allInvoices: StateFlow<List<CardSalesInvoiceEntity>> = repository.allSalesInvoices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allVouchers: StateFlow<List<FinancialVoucherEntity>> = repository.allVouchers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRetailers: StateFlow<List<RetailerEntity>> = repository.allRetailers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalAssetPurchaseCost: StateFlow<Double?> = repository.totalAssetPurchaseCost
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalCurrentAssetValue: StateFlow<Double?> = repository.totalCurrentAssetValue
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Filtered Financial Summary Engine
    val financialReportSummary: StateFlow<FinancialReportSummary> = combine(
        allInvoices,
        allVouchers,
        allRetailers,
        selectedTimeRange,
        customStartMillis,
        customEndMillis,
        totalAssetPurchaseCost,
        totalCurrentAssetValue
    ) { args ->
        val invoices = args[0] as List<CardSalesInvoiceEntity>
        val vouchers = args[1] as List<FinancialVoucherEntity>
        val retailers = args[2] as List<RetailerEntity>
        val timeOption = args[3] as TimeRangeOption
        val customStart = args[4] as Long
        val customEnd = args[5] as Long
        val assetCost = (args[6] as Double?) ?: 0.0
        val assetVal = (args[7] as Double?) ?: 0.0

        val (startMillis, endMillis) = getTimeRangeBoundaries(timeOption, customStart, customEnd)

        // Filter invoices and vouchers for the selected period
        val periodInvoices = invoices.filter { it.invoiceDateMillis in startMillis..endMillis }
        val periodVouchers = vouchers.filter { !it.isVoided && it.dateMillis in startMillis..endMillis }

        // 1. Total Sales
        val totalSales = periodInvoices.sumOf { it.totalAmount.toDouble() }

        // 2. Total Receipts (سندات قبض)
        val totalReceipts = periodVouchers.filter { it.voucherType == "RECEIPT" }.sumOf { it.amount.toDouble() }

        // 3. Total OpEx Expenses (سندات صرف تشغيلية - مستثنى منها أصول CAPEX)
        val totalExpenses = periodVouchers
            .filter {
                it.voucherType == "PAYMENT" &&
                !it.category.equals("CAPEX", ignoreCase = true) &&
                !it.category.contains("أصول", ignoreCase = true) &&
                !it.category.contains("CAPEX", ignoreCase = true)
            }
            .sumOf { it.amount.toDouble() }

        // Dedicated Total CAPEX Assets Value (أصول ثابتة)
        val totalCapexAssets = periodVouchers
            .filter {
                it.voucherType == "PAYMENT" &&
                (it.category.equals("CAPEX", ignoreCase = true) || it.category.contains("أصول", ignoreCase = true) || it.category.contains("CAPEX", ignoreCase = true))
            }
            .sumOf { it.amount.toDouble() } + assetCost

        // 4. Direct Cost of Services (Account 5101 - Starlink / Main Internet)
        // Strictly 0.0 YER if no payment vouchers are posted for Account 5101!
        val estimatedCostOfGoods = periodVouchers
            .filter {
                it.voucherType == "PAYMENT" &&
                (it.category.contains("5101") || it.category.contains("ستارلينك") || it.category.contains("نت رئيسي") || it.category.contains("اشتراك نت"))
            }
            .sumOf { it.amount.toDouble() }

        // 5. Depreciation of Assets
        val assetDepreciation = (assetCost - assetVal).coerceAtLeast(0.0)

        // 6. Net True Profit (إجمالي المبيعات - المصروفات التشغيلية OpEx - الإهلاك)
        val netTrueProfit = totalSales - totalExpenses - assetDepreciation
        val profitMarginPercent = if (totalSales > 0) (netTrueProfit / totalSales) * 100.0 else 0.0

        // 7. Top 5 Agents by Purchase Volume in Period
        val salesByAgentMap = mutableMapOf<Long, Double>()
        periodInvoices.forEach { inv ->
            inv.retailerId?.let { rId ->
                salesByAgentMap[rId] = (salesByAgentMap[rId] ?: 0.0) + inv.totalAmount.toDouble()
            }
        }
        val topAgentsList = salesByAgentMap.entries
            .mapNotNull { entry ->
                val retailer = retailers.firstOrNull { it.id == entry.key }
                if (retailer != null) Pair(retailer, entry.value) else null
            }
            .sortedByDescending { it.second }
            .take(5)

        // 8. Top 5 Aging Debtors (أعلى 5 وكلاء مديونية)
        val topDebtorsList = retailers
            .filter { it.balanceOwedDouble > 0 }
            .map { Pair(it, it.balanceOwedDouble) }
            .sortedByDescending { it.second }
            .take(5)

        FinancialReportSummary(
            totalSales = totalSales,
            totalReceipts = totalReceipts,
            totalExpenses = totalExpenses,
            totalCapexAssets = totalCapexAssets,
            estimatedCostOfGoods = estimatedCostOfGoods,
            assetDepreciation = assetDepreciation,
            netTrueProfit = netTrueProfit,
            profitMarginPercent = profitMarginPercent,
            topAgentsBySales = topAgentsList,
            topAgingDebtors = topDebtorsList
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        FinancialReportSummary()
    )

    // Smart AI Summary State
    private val _aiSummary = MutableStateFlow<String?>(null)
    val aiSummary: StateFlow<String?> = _aiSummary.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    /**
     * توليد ملخص ذكي AI لأداء الشبكة المالي خلال الفترة المحددة
     */
    fun generateSmartAiSummary() {
        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                val summary = financialReportSummary.value
                val rangeOption = _selectedTimeRange.value
                val identity = networkIdentity.value

                val topAgentsNames = summary.topAgentsBySales.joinToString { "${it.first.name} (${it.second.toInt()} ر.ي)" }
                val topDebtorsNames = summary.topAgingDebtors.joinToString { "${it.first.name} (دين: ${it.second.toInt()} ر.ي)" }

                val prompt = """
                    أنت خبير مالي ومستشار مبيعات شبكات لاسلكية وميكروتك.
                    قم بصياغة فقرة تحليلية ملخصة وموجزة وشديدة الاحترافية باللغة العربية تشرح الأداء المالي والتحليلي لشبكة (${identity.networkName}) خلال فترة (${rangeOption.label}):
                    
                    المؤشرات المالية:
                    - إجمالي المبيعات: ${summary.totalSales.toInt()} ريال يمني
                    - إجمالي المقبوضات والنقدية: ${summary.totalReceipts.toInt()} ريال
                    - إجمالي المصروفات التشغيلية: ${summary.totalExpenses.toInt()} ريال
                    - صافي الربح الحقيقي: ${summary.netTrueProfit.toInt()} ريال (هامش ربح ${String.format(Locale.US, "%.1f", summary.profitMarginPercent)}%)
                    - كبار الوكلاء شراءً: ${if (topAgentsNames.isNotBlank()) topAgentsNames else "لا توجد مبيعات"}
                    - كبار الوكلاء مديونية (Aging Debtors): ${if (topDebtorsNames.isNotBlank()) topDebtorsNames else "لا توجد ديون"}
                    
                    اذكر باختصار:
                    1. تقييم سريع وموجز لسلامة التدفق النقدي وصافي الأرباح.
                    2. سرعة دوران المبيعات وأعلى نقاط التوزيع أداءً.
                    3. نصيحة محددة لإدارة ديون الوكلاء والتحصيل.
                """.trimIndent()

                val response = aiService.generateText(prompt)
                _aiSummary.value = response
            } catch (e: Exception) {
                Log.e("ReportsViewModel", "Error generating AI summary", e)
                _aiSummary.value = "تعذر الاتصال بالمساعد الذكي AI: ${e.message}"
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    // Export PDF Methods

    /**
     * تصدير التقرير المالي الشامل كملف PDF رسمي
     */
    fun exportFinancialReportPdf(context: Context, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val summary = financialReportSummary.value
                val identity = networkIdentity.value
                val timeLabel = getTimeRangeLabel(_selectedTimeRange.value, _customStartMillis.value, _customEndMillis.value)

                val pdfFile = ReportPdfManager.generateFinancialReportPdfFile(
                    context = context,
                    networkIdentity = identity,
                    timeRangeLabel = timeLabel,
                    totalSales = summary.totalSales,
                    totalReceipts = summary.totalReceipts,
                    totalExpenses = summary.totalExpenses,
                    netTrueProfit = summary.netTrueProfit,
                    topAgentsBySales = summary.topAgentsBySales,
                    topAgingDebtors = summary.topAgingDebtors,
                    aiSummaryText = _aiSummary.value
                )

                ReportPdfManager.sharePdf(
                    context = context,
                    pdfFile = pdfFile,
                    subject = "التقرير المالي الشامل - ${identity.networkName}",
                    text = "مرفق التقرير المالي الشامل لمؤشرات الأداء والمبيعات والديون لشبكة ${identity.networkName}"
                )
            } catch (e: Exception) {
                Log.e("ReportsViewModel", "Error exporting financial report PDF", e)
            }
            onDone()
        }
    }

    /**
     * تصدير التقرير المالي الشامل كملف نصي منسق
     */
    fun exportFinancialReportText(context: Context) {
        val summary = financialReportSummary.value
        val identity = networkIdentity.value
        val timeLabel = getTimeRangeLabel(_selectedTimeRange.value, _customStartMillis.value, _customEndMillis.value)
        val nowStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).format(Date())

        val topAgentsText = summary.topAgentsBySales.joinToString("\n") {
            "• ${it.first.name}: ${String.format(Locale.US, "%,.0f", it.second)} ريال"
        }.ifBlank { "لا توجد مبيعات مسجلة" }

        val topDebtorsText = summary.topAgingDebtors.joinToString("\n") {
            "• ${it.first.name} (${it.first.phone}): ${String.format(Locale.US, "%,.0f", it.second)} ريال"
        }.ifBlank { "لا توجد ديون مسجلة" }

        val reportText = """
            📊 التقرير المالي والتحليلي المجمع:
            🌐 الشبكة: ${identity.networkName}
            📅 الفترة: $timeLabel
            ⏱️ تاريخ الاستخراج: $nowStr
            
            💰 إجمالي المبيعات: ${String.format(Locale.US, "%,.0f", summary.totalSales)} ريال yemen
            📥 إجمالي المقبوضات النقدية: ${String.format(Locale.US, "%,.0f", summary.totalReceipts)} ريال
            📤 إجمالي المصروفات التشغيلية: ${String.format(Locale.US, "%,.0f", summary.totalExpenses)} ريال
            🌟 صافي الربح الحقيقي: ${String.format(Locale.US, "%,.0f", summary.netTrueProfit)} ريال
            📈 هامش الربح الصافي: ${String.format(Locale.US, "%.1f", summary.profitMarginPercent)}%
            
            🏆 أعلى 5 وكلاء شراءً:
            $topAgentsText
            
            ⚠️ أعلى 5 وكلاء مديونية (Aging Debtors):
            $topDebtorsText
            
            ${if (!_aiSummary.value.isNullOrBlank()) "🤖 الملخص الذكي AI:\n${_aiSummary.value}\n" else ""}
            تم الإصدار آلياً بواسطة تطبيق سام ميكروتك
        """.trimIndent()

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "التقرير المالي - ${identity.networkName}")
            putExtra(Intent.EXTRA_TEXT, reportText)
        }
        val chooser = Intent.createChooser(shareIntent, "مشاركة التقرير المالي النصي")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /**
     * تصدير كشف حساب وكيل كملف PDF احترافي
     */
    fun exportAgentAccountStatementPdf(
        context: Context,
        retailer: RetailerEntity,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val identity = networkIdentity.value
                val (startMillis, endMillis) = getTimeRangeBoundaries(_selectedTimeRange.value, _customStartMillis.value, _customEndMillis.value)
                val timeLabel = getTimeRangeLabel(_selectedTimeRange.value, _customStartMillis.value, _customEndMillis.value)

                val agentInvoices = allInvoices.value
                    .filter {
                        (it.retailerId == retailer.id || it.customerName.trim().equals(retailer.name.trim(), ignoreCase = true)) &&
                        !it.invoiceNumber.startsWith("INV-DELIV-") &&
                        it.invoiceDateMillis in startMillis..endMillis
                    }
                    .distinctBy { if (it.invoiceNumber.isNotBlank()) it.invoiceNumber else it.id.toString() }

                val agentVouchers = allVouchers.value
                    .filter {
                        (it.retailerId == retailer.id || it.partyName.trim().equals(retailer.name.trim(), ignoreCase = true)) &&
                        !it.isVoided &&
                        it.voucherType == "RECEIPT" &&
                        it.dateMillis in startMillis..endMillis
                    }
                    .distinctBy { if (it.voucherNumber.isNotBlank()) it.voucherNumber else it.id.toString() }

                val totalPurchases = agentInvoices.sumOf { it.totalAmount.toDouble() }
                val unlinkedInvoicePaidSum = agentInvoices.sumOf { inv ->
                    val hasVoucher = agentVouchers.any { v ->
                        v.invoiceId == inv.id || (v.invoiceNumber.isNotBlank() && v.invoiceNumber == inv.invoiceNumber)
                    }
                    if (!hasVoucher && inv.paidAmount.compareTo(java.math.BigDecimal.ZERO) > 0) inv.paidAmount.toDouble() else 0.0
                }
                val totalPaid = agentVouchers.sumOf { it.amount.toDouble() } + unlinkedInvoicePaidSum
                val finalBalance = totalPurchases - totalPaid

                val pdfFile = ReportPdfManager.generateAgentAccountStatementPdfFile(
                    context = context,
                    networkIdentity = identity,
                    retailer = retailer,
                    invoices = agentInvoices,
                    vouchers = agentVouchers,
                    timeRangeLabel = timeLabel
                )

                ReportPdfManager.sharePdf(
                    context = context,
                    pdfFile = pdfFile,
                    subject = "كشف حساب الوكيل ${retailer.name} - ${identity.networkName}",
                    text = "مرفق كشف الحساب التفصيلي للوكيل / البقالة (${retailer.name}) وإجمالي الرصيد المتبقي (${String.format(Locale.US, "%,.0f", finalBalance)} ريال)"
                )
            } catch (e: Exception) {
                Log.e("ReportsViewModel", "Error exporting agent statement PDF", e)
            }
            onDone()
        }
    }

    /**
     * تصدير كشف حساب وكيل كملف نصي منسق
     */
    fun exportAgentAccountStatementText(context: Context, retailer: RetailerEntity) {
        val identity = networkIdentity.value
        val (startMillis, endMillis) = getTimeRangeBoundaries(_selectedTimeRange.value, _customStartMillis.value, _customEndMillis.value)
        val timeLabel = getTimeRangeLabel(_selectedTimeRange.value, _customStartMillis.value, _customEndMillis.value)

        val agentInvoices = allInvoices.value
            .filter {
                (it.retailerId == retailer.id || it.customerName.trim().equals(retailer.name.trim(), ignoreCase = true)) &&
                !it.invoiceNumber.startsWith("INV-DELIV-") &&
                it.invoiceDateMillis in startMillis..endMillis
            }
            .distinctBy { if (it.invoiceNumber.isNotBlank()) it.invoiceNumber else it.id.toString() }

        val agentVouchers = allVouchers.value
            .filter {
                (it.retailerId == retailer.id || it.partyName.trim().equals(retailer.name.trim(), ignoreCase = true)) &&
                !it.isVoided &&
                it.voucherType == "RECEIPT" &&
                it.dateMillis in startMillis..endMillis
            }
            .distinctBy { if (it.voucherNumber.isNotBlank()) it.voucherNumber else it.id.toString() }

        val totalPurchases = agentInvoices.sumOf { it.totalAmount.toDouble() }
        val unlinkedInvoicePaidSum = agentInvoices.sumOf { inv ->
            val hasVoucher = agentVouchers.any { v ->
                v.invoiceId == inv.id || (v.invoiceNumber.isNotBlank() && v.invoiceNumber == inv.invoiceNumber)
            }
            if (!hasVoucher && inv.paidAmount.compareTo(java.math.BigDecimal.ZERO) > 0) inv.paidAmount.toDouble() else 0.0
        }
        val totalPaid = agentVouchers.sumOf { it.amount.toDouble() } + unlinkedInvoicePaidSum
        val finalBalance = totalPurchases - totalPaid

        val statementText = """
            🧾 كشف حساب وكيل تفصيلي:
            🌐 الشبكة: ${identity.networkName}
            👤 الوكيل / البقالة: ${retailer.name}
            📱 الهاتف: ${retailer.phone.ifBlank { "غير مسجل" }}
            📅 الفترة: $timeLabel
            
            💰 إجمالي المبيعات (مدين): ${String.format(Locale.US, "%,.0f", totalPurchases)} ريال
            📥 إجمالي المسدد (دائن): ${String.format(Locale.US, "%,.0f", totalPaid)} ريال
            🔴 الرصيد الإجمالي المتبقي (المديونية): ${String.format(Locale.US, "%,.0f", finalBalance)} ريال
            
            نُرجى التكرم بالاطلاع ومطابقة الرصيد.
            خدمة العملاء: ${identity.supportPhone}
        """.trimIndent()

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "كشف حساب - ${retailer.name}")
            putExtra(Intent.EXTRA_TEXT, statementText)
        }
        val chooser = Intent.createChooser(shareIntent, "مشاركة كشف الحساب")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    // Helper Date Boundary Functions
    private fun getTimeRangeBoundaries(option: TimeRangeOption, customStart: Long, customEnd: Long): Pair<Long, Long> {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        return when (option) {
            TimeRangeOption.TODAY -> {
                calendar.apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                Pair(calendar.timeInMillis, now)
            }
            TimeRangeOption.THIS_WEEK -> {
                calendar.apply {
                    add(Calendar.DAY_OF_YEAR, -7)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                Pair(calendar.timeInMillis, now)
            }
            TimeRangeOption.THIS_MONTH -> {
                calendar.apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                Pair(calendar.timeInMillis, now)
            }
            TimeRangeOption.CUSTOM -> {
                Pair(customStart, customEnd)
            }
        }
    }

    private fun getTimeRangeLabel(option: TimeRangeOption, startMillis: Long, endMillis: Long): String {
        return when (option) {
            TimeRangeOption.TODAY -> "اليوم"
            TimeRangeOption.THIS_WEEK -> "هذا الأسبوع (آخر 7 أيام)"
            TimeRangeOption.THIS_MONTH -> "هذا الشهر (${SimpleDateFormat("MMMM yyyy", Locale.US).format(Date())})"
            TimeRangeOption.CUSTOM -> {
                val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.US)
                "${sdf.format(Date(startMillis))} - ${sdf.format(Date(endMillis))}"
            }
        }
    }

    private fun getStartOfMonthMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
