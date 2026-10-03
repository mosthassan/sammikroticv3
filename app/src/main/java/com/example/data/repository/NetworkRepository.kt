package com.example.data.repository

import java.util.Calendar
import com.example.data.local.AppDatabase
import com.example.data.local.entity.CardBatchEntity
import com.example.data.local.entity.CardEntity
import com.example.data.local.entity.CardPackageEntity
import com.example.data.local.entity.FinancialVoucherEntity
import com.example.data.local.entity.InventoryItemEntity
import com.example.data.local.entity.NetworkAssetEntity
import com.example.data.local.entity.NetworkDeviceEntity
import com.example.data.local.entity.NetworkIdentityEntity
import com.example.data.local.entity.PartnerEntity
import com.example.data.local.entity.PartnerTransactionEntity
import com.example.data.local.entity.PurchaseInvoiceEntity
import com.example.data.local.entity.RetailerEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.CardSalesInvoiceEntity
import com.example.data.local.entity.ChartOfAccountsEntity
import com.example.data.local.entity.InventoryMovementEntity
import com.example.data.local.entity.JournalEntryEntity
import com.example.data.local.entity.JournalEntryHeaderEntity
import com.example.data.local.entity.JournalEntryLineEntity
import com.example.data.local.model.JournalEntryWithLines
import com.example.data.local.model.TrialBalanceRow
import com.example.data.local.model.GeneralLedgerReport
import com.example.data.local.model.GeneralLedgerItem
import com.example.data.local.model.IncomeStatementReport
import com.example.data.model.CardSalesInvoiceItem
import com.example.data.model.PeriodClosingSummary
import com.example.data.model.PartnerPeriodShare
import com.example.data.model.AssetDepreciationResult
import com.example.data.model.AssetDepreciationItem
import org.json.JSONArray
import org.json.JSONObject
import androidx.room.withTransaction
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.random.Random

class NetworkRepository(private val db: AppDatabase) {

    // Network Identity & Core Configuration
    val networkIdentity: Flow<NetworkIdentityEntity?> = db.networkIdentityDao().getNetworkIdentityFlow()

    suspend fun getNetworkIdentity(): NetworkIdentityEntity = withContext(Dispatchers.IO) {
        val existing = db.networkIdentityDao().getNetworkIdentity()
        if (existing != null) {
            existing
        } else {
            val defaultIdentity = NetworkIdentityEntity()
            db.networkIdentityDao().insertOrUpdate(defaultIdentity)
            defaultIdentity
        }
    }

    suspend fun saveNetworkIdentity(identity: NetworkIdentityEntity): Long = withContext(Dispatchers.IO) {
        db.withTransaction {
            val res = db.networkIdentityDao().insertOrUpdate(identity.copy(id = 1L, updatedAt = System.currentTimeMillis()))
            db.currencyRateDao().insertOrUpdateRates(
                listOf(
                    com.example.data.local.entity.CurrencyRateEntity(
                        currencyCode = "USD",
                        rateToBase = BigDecimal.valueOf(if (identity.usdToYerRate > 0) identity.usdToYerRate else 530.0)
                    ),
                    com.example.data.local.entity.CurrencyRateEntity(
                        currencyCode = "SAR",
                        rateToBase = BigDecimal.valueOf(if (identity.sarToYerRate > 0) identity.sarToYerRate else 140.0)
                    ),
                    com.example.data.local.entity.CurrencyRateEntity(
                        currencyCode = "YER",
                        rateToBase = BigDecimal.ONE
                    )
                )
            )
            res
        }
    }

    fun extractSubnetPrefix(subnetOrIp: String): String {
        val clean = subnetOrIp.substringBefore("/").trim()
        val parts = clean.split(".")
        return if (parts.size >= 3) {
            "${parts[0]}.${parts[1]}.${parts[2]}."
        } else {
            "192.168.88."
        }
    }

    fun isIpInApprovedSubnet(ip: String, subnet: String): Boolean {
        val cleanIp = ip.trim()
        if (cleanIp.isEmpty()) return true
        val prefix = extractSubnetPrefix(subnet)
        return cleanIp.startsWith(prefix)
    }

    // Central Accounting Posting Service (المحرك المالي المركزي الموحد)
    val postingService = com.example.data.accounting.AccountingPostingService(db)

    suspend fun repairAllUnpostedEntries(): Int = postingService.repairAllUnpostedEntries()

    // Chart of Accounts (شجرة الحسابات المحاسبية)
    val allChartOfAccounts: Flow<List<ChartOfAccountsEntity>> = db.chartOfAccountsDao().getAllAccounts()
    val activeChartOfAccounts: Flow<List<ChartOfAccountsEntity>> = db.chartOfAccountsDao().getActiveAccounts()

    suspend fun getAccountByCode(code: String): ChartOfAccountsEntity? = withContext(Dispatchers.IO) {
        db.chartOfAccountsDao().getAccountByCode(code)
    }

    suspend fun saveAccount(account: ChartOfAccountsEntity) = withContext(Dispatchers.IO) {
        db.chartOfAccountsDao().insertAccount(account)
    }

    // Trial Balance & Double-Entry Journal Entries (ميزان المراجعة وقيود اليومية المركبة المزدوجة)
    val trialBalance: Flow<List<TrialBalanceRow>> = db.journalEntryDao().getTrialBalance()
    val allJournalEntriesWithLines: Flow<List<JournalEntryWithLines>> = db.journalEntryDao().getAllHeadersWithLines()

    fun getPeriodTrialBalance(startDate: Long, endDate: Long): Flow<List<TrialBalanceRow>> =
        db.journalEntryDao().getPeriodTrialBalance(startDate, endDate)

    /**
     * تقرير دفتر الأستاذ العام (General Ledger Report) لحساب معين
     * مع احتساب الرصيد الافتتاحي والرصيد التراكمي الفعلي بعد كل حركة
     */
    fun getGeneralLedgerReport(
        accountCode: String,
        startDate: Long,
        endDate: Long
    ): Flow<GeneralLedgerReport> = flow {
        val account = db.chartOfAccountsDao().getAccountByCode(accountCode)
        val accName = account?.accountNameAr ?: "حساب $accountCode"
        val accType = account?.accountType ?: "ASSET"
        val normalBal = account?.normalBalance ?: "DEBIT"

        val opening = db.journalEntryDao().getOpeningDebitCredit(accountCode, startDate)
        val openDebit = opening?.totalDebit ?: BigDecimal.ZERO
        val openCredit = opening?.totalCredit ?: BigDecimal.ZERO
        val openingBalance = if (normalBal == "DEBIT") {
            openDebit.subtract(openCredit)
        } else {
            openCredit.subtract(openDebit)
        }

        db.journalEntryDao().getLedgerLinesForAccount(accountCode, startDate, endDate).collect { rawLines ->
            var running = openingBalance
            var periodDebit = BigDecimal.ZERO
            var periodCredit = BigDecimal.ZERO

            val computedLines = rawLines.map { line ->
                periodDebit = periodDebit.add(line.debit)
                periodCredit = periodCredit.add(line.credit)

                running = if (normalBal == "DEBIT") {
                    running.add(line.debit).subtract(line.credit)
                } else {
                    running.add(line.credit).subtract(line.debit)
                }

                GeneralLedgerItem(
                    lineId = line.lineId,
                    headerId = line.headerId,
                    entryNumber = line.entryNumber,
                    dateMillis = line.dateMillis,
                    accountCode = line.accountCode,
                    accountName = line.accountName,
                    description = line.lineDescription.ifBlank { line.headerDescription },
                    debit = line.debit,
                    credit = line.credit,
                    runningBalance = running,
                    referenceType = line.referenceType,
                    referenceId = line.referenceId
                )
            }

            emit(
                GeneralLedgerReport(
                    accountCode = accountCode,
                    accountName = accName,
                    accountType = accType,
                    normalBalance = normalBal,
                    startDateMillis = startDate,
                    endDateMillis = endDate,
                    openingBalance = openingBalance,
                    totalPeriodDebit = periodDebit,
                    totalPeriodCredit = periodCredit,
                    closingBalance = running,
                    lines = computedLines
                )
            )
        }
    }

    /**
     * تقرير قائمة الدخل المعيارية (Income Statement / P&L) المستخرج حصرياً من القيود الموزونة
     */
    fun getIncomeStatementReport(
        startDate: Long,
        endDate: Long
    ): Flow<IncomeStatementReport> = db.journalEntryDao().getIncomeStatementRows(startDate, endDate).map { rows ->
        val rowsMap = rows.associateBy { it.accountCode }

        fun getNetRevenue(code: String): BigDecimal {
            val r = rowsMap[code] ?: return BigDecimal.ZERO
            return r.totalCredit.subtract(r.totalDebit)
        }

        fun getNetExpense(code: String): BigDecimal {
            val r = rowsMap[code] ?: return BigDecimal.ZERO
            return r.totalDebit.subtract(r.totalCredit)
        }

        val cardSalesRevenue = getNetRevenue("4101")
        val subscriptionRevenue = getNetRevenue("4201")
        val totalRevenue = cardSalesRevenue.add(subscriptionRevenue)

        val cogs = getNetExpense("5101")
        val grossProfit = totalRevenue.subtract(cogs)

        val operatingExpenses = getNetExpense("5201")
        val maintenanceExpenses = getNetExpense("5202")
        val totalOpex = operatingExpenses.add(maintenanceExpenses)

        val depreciation = getNetExpense("5203")
        val totalExpenses = cogs.add(totalOpex).add(depreciation)

        val netOperatingProfit = totalRevenue.subtract(totalExpenses)

        val grossMargin = if (totalRevenue > BigDecimal.ZERO) {
            try {
                grossProfit.multiply(BigDecimal("100")).divide(totalRevenue, 2, RoundingMode.HALF_UP).toDouble()
            } catch (e: Exception) { 0.0 }
        } else 0.0

        val netMargin = if (totalRevenue > BigDecimal.ZERO) {
            try {
                netOperatingProfit.multiply(BigDecimal("100")).divide(totalRevenue, 2, RoundingMode.HALF_UP).toDouble()
            } catch (e: Exception) { 0.0 }
        } else 0.0

        IncomeStatementReport(
            startDateMillis = startDate,
            endDateMillis = endDate,
            cardSalesRevenue = cardSalesRevenue,
            subscriptionRevenue = subscriptionRevenue,
            totalOperationalRevenue = totalRevenue,
            cogs = cogs,
            grossProfit = grossProfit,
            operatingExpenses = operatingExpenses,
            maintenanceExpenses = maintenanceExpenses,
            totalOpexExpenses = totalOpex,
            depreciationExpense = depreciation,
            totalExpenses = totalExpenses,
            netOperatingProfit = netOperatingProfit,
            grossMarginPercentage = grossMargin,
            netProfitMarginPercentage = netMargin
        )
    }

    suspend fun postDoubleEntry(
        entryNumber: String,
        referenceType: String,
        referenceId: String,
        description: String,
        lines: List<JournalEntryLineEntity>,
        createdBy: String = "المهندس سام",
        dateMillis: Long = System.currentTimeMillis()
    ): Long = withContext(Dispatchers.IO) {
        val header = JournalEntryHeaderEntity(
            entryNumber = entryNumber,
            dateMillis = dateMillis,
            referenceType = referenceType,
            referenceId = referenceId,
            description = description,
            status = "POSTED",
            createdBy = createdBy
        )
        db.journalEntryDao().postBalancedEntry(header, lines)
    }

    suspend fun voidJournalEntry(
        headerId: Long,
        voidReason: String = "إلغاء القيد المحاسبي",
        performedBy: String = "المهندس سام"
    ): Long? = withContext(Dispatchers.IO) {
        db.journalEntryDao().voidEntryWithReversal(headerId, voidReason, performedBy)
    }

    // =========================================================================
    // 2. محرك إهلاك الأصول الثابتة (Asset Depreciation Engine)
    // =========================================================================

    /**
     * حساب قسط الإهلاك الشهري بطريقة القسط الثابت (Straight-Line Method)
     * بناءً على تصنيف الأصل والعمر الإنتاجي المعتمد، مع افتراض قيمة تخريدية (خردة) 5%
     */
    fun calculateAssetMonthlyDepreciation(asset: NetworkAssetEntity): BigDecimal {
        if (asset.purchaseCost <= BigDecimal.ZERO || asset.status == "DEPRECATED") {
            return BigDecimal.ZERO
        }
        val usefulLifeMonths = when (asset.category.uppercase()) {
            "SERVERS" -> 36        // راوترات ميكروتك وسيرفرات: 3 سنوات
            "TOWERS" -> 120        // أبراج وهياكل حديدية: 10 سنوات
            "SOLAR_POWER" -> 48    // طاقة شمسية وبطاريات: 4 سنوات
            "FIBER_CABLES" -> 60   // كابلات فايبر وهوائية: 5 سنوات
            "CLIENT_DEVICES" -> 36 // أجهزة بث ومودمات: 3 سنوات
            else -> 60             // أخرى: 5 سنوات
        }
        val salvageValue = asset.purchaseCost.multiply(BigDecimal("0.05"))
        val depreciableBase = (asset.purchaseCost.subtract(salvageValue)).max(BigDecimal.ZERO)
        if (depreciableBase <= BigDecimal.ZERO) return BigDecimal.ZERO

        val monthlyDepr = depreciableBase.divide(BigDecimal(usefulLifeMonths), 2, RoundingMode.HALF_UP)
        // لا يمكن إهلاك الأصل بأكثر من القيمة المتبقية فوق قيمة الخردة
        val currentBookAboveSalvage = (asset.estimatedCurrentValue.subtract(salvageValue)).max(BigDecimal.ZERO)
        return monthlyDepr.min(currentBookAboveSalvage)
    }

    /**
     * تنفيذ وترحيل قيد إهلاك الأصول الثابتة الدوري (Double-Entry Balanced Depreciation Entry)
     * مدين: 5203 - مصروف إهلاك أصول الشبكة
     * دائن: 1509 - مجمع إهلاك الأصول الثابتة
     */
    suspend fun executeAssetDepreciation(
        periodMonths: Int = 1,
        periodName: String = "",
        performedBy: String = "المهندس سام"
    ): AssetDepreciationResult = withContext(Dispatchers.IO) {
        db.withTransaction {
            val months = periodMonths.coerceAtLeast(1)
            val activeAssets = db.networkAssetDao().getActiveAssetsList()
            var totalDepreciation = BigDecimal.ZERO
            val processedItems = mutableListOf<AssetDepreciationItem>()

            for (asset in activeAssets) {
                val monthlyRate = calculateAssetMonthlyDepreciation(asset)
                val deprAmount = monthlyRate.multiply(BigDecimal(months))
                if (deprAmount > BigDecimal.ZERO) {
                    val prevBook = asset.estimatedCurrentValue
                    val newBook = (prevBook.subtract(deprAmount)).max(BigDecimal.ZERO)
                    val newStatus = if (newBook <= asset.purchaseCost.multiply(BigDecimal("0.05"))) "DEPRECATED" else asset.status

                    db.networkAssetDao().updateAsset(
                        asset.copy(
                            estimatedCurrentValue = newBook,
                            status = newStatus
                        )
                    )
                    totalDepreciation = totalDepreciation.add(deprAmount)
                    processedItems.add(
                        AssetDepreciationItem(
                            assetId = asset.id,
                            assetName = asset.assetName,
                            category = asset.category,
                            purchaseCost = asset.purchaseCost,
                            previousBookValue = prevBook,
                            depreciationAmount = deprAmount,
                            newBookValue = newBook
                        )
                    )
                }
            }

            var entryHeaderId = 0L
            if (totalDepreciation > BigDecimal.ZERO) {
                val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                val deprEntryNum = db.numberSequenceDao().getNextNumber("JE", currentYear, "JE")
                val pName = periodName.ifBlank { "دورة $months شهر ($deprEntryNum)" }
                val header = JournalEntryHeaderEntity(
                    entryNumber = deprEntryNum,
                    dateMillis = System.currentTimeMillis(),
                    referenceType = "ASSET_DEPRECIATION",
                    referenceId = "DEPR-$months-M",
                    description = "قيد إهلاك أصول الشبكة الدفتري لدورة $pName",
                    createdBy = performedBy
                )

                val lines = listOf(
                    JournalEntryLineEntity(
                        headerId = 0L,
                        accountCode = "5203",
                        accountName = "مصروف إهلاك أصول الشبكة",
                        lineType = "DEBIT",
                        debit = totalDepreciation,
                        lineDescription = "مصروف إهلاك دوري لأصول الشبكة - $pName"
                    ),
                    JournalEntryLineEntity(
                        headerId = 0L,
                        accountCode = "1509",
                        accountName = "مجمع إهلاك الأصول الثابتة",
                        lineType = "CREDIT",
                        credit = totalDepreciation,
                        lineDescription = "مجمع إهلاك الأصول الثابتة المتراكم - $pName"
                    )
                )

                entryHeaderId = db.journalEntryDao().postBalancedEntry(header, lines)
            }

            AssetDepreciationResult(
                totalDepreciationAmount = totalDepreciation,
                assetsProcessedCount = processedItems.size,
                periodMonths = months,
                entryHeaderId = entryHeaderId,
                items = processedItems
            )
        }
    }

    // =========================================================================
    // 3. محرك حقوق الشركاء وإقفال الفترة المالية (Partners & Period Closing Engine)
    // =========================================================================

    /**
     * احتساب ملخص الدورة المالية والأرباح التشغيلية الحقيقية للفترة
     * صافي الربح = الإيرادات (4101 + 4201) - (COGS 5101 + المصروفات 5201 + 5202 + 5203)
     */
    suspend fun calculateFinancialPeriodSummary(
        startDateMillis: Long,
        endDateMillis: Long
    ): PeriodClosingSummary = withContext(Dispatchers.IO) {
        val invoices = db.cardSalesInvoiceDao().getSalesInvoicesList()
            .filter { !it.isVoided && it.invoiceDateMillis in startDateMillis..endDateMillis }
        val cardSalesRevenue = invoices.fold(BigDecimal.ZERO) { acc, inv -> acc.add(inv.totalAmount) }

        // إيرادات الاشتراكات المباشرة والخدمات (4201) من السندات
        val directSubscriptions = db.financialVoucherDao().getVouchersList()
            .filter { !it.isVoided && it.voucherType == "RECEIPT" && (it.category.contains("اشتراك") || it.category.contains("مباشر")) && it.dateMillis in startDateMillis..endDateMillis }
            .fold(BigDecimal.ZERO) { acc, v -> acc.add(v.amount) }

        val totalGrossRevenue = cardSalesRevenue.add(directSubscriptions)

        // Cost of Services (COGS - Account 5101 Main Internet Subscription / Starlink)
        // Strictly fetched from posted journal entries / vouchers for Account 5101.
        // If NO entries exist for Account 5101, COGS MUST strictly evaluate to 0 YER!
        val cogsRows = db.journalEntryDao().getIncomeStatementRows(startDateMillis, endDateMillis).first()
        val cogsRow = cogsRows.find { it.accountCode == "5101" }
        val totalCogs = cogsRow?.let { (it.totalDebit.subtract(it.totalCredit)).max(BigDecimal.ZERO) } ?: BigDecimal.ZERO

        // المصروفات من السندات
        val vouchers = db.financialVoucherDao().getVouchersList()
            .filter { !it.isVoided && it.dateMillis in startDateMillis..endDateMillis }
        val operatingExpenses = vouchers
            .filter { it.voucherType == "PAYMENT" && !it.category.contains("صيانة") }
            .fold(BigDecimal.ZERO) { acc, v -> acc.add(v.amount) }
        val maintenanceExpenses = vouchers
            .filter { it.voucherType == "PAYMENT" && it.category.contains("صيانة") }
            .fold(BigDecimal.ZERO) { acc, v -> acc.add(v.amount) }

        // إهلاك الأصول التقديري للفترة
        val activeAssets = db.networkAssetDao().getActiveAssetsList()
        val totalDepreciation = activeAssets.fold(BigDecimal.ZERO) { acc, a ->
            acc.add(calculateAssetMonthlyDepreciation(a))
        }

        val totalExpenses = totalCogs.add(operatingExpenses).add(maintenanceExpenses).add(totalDepreciation)
        val netOperatingProfit = (totalGrossRevenue.subtract(totalExpenses)).max(BigDecimal.ZERO)

        val activePartners = db.partnerDao().getActivePartnersList()
        val totalSharePercentage = activePartners.sumOf { it.sharePercentage }
        val effectivePercentageBase = if (totalSharePercentage > 0.0) totalSharePercentage else 100.0

        val partnerShares = activePartners.map { partner ->
            val shareRatio = BigDecimal.valueOf(partner.sharePercentage)
                .divide(BigDecimal.valueOf(effectivePercentageBase), 6, RoundingMode.HALF_UP)
            val allocatedProfit = netOperatingProfit.multiply(shareRatio).setScale(2, RoundingMode.HALF_UP)
            PartnerPeriodShare(
                partnerId = partner.id,
                partnerName = partner.name,
                sharePercentage = partner.sharePercentage,
                allocatedProfit = allocatedProfit
            )
        }

        PeriodClosingSummary(
            startDateMillis = startDateMillis,
            endDateMillis = endDateMillis,
            totalGrossRevenue = totalGrossRevenue,
            cardSalesRevenue = cardSalesRevenue,
            directSubscriptionsRevenue = directSubscriptions,
            totalCogs = totalCogs,
            totalOperatingExpenses = operatingExpenses,
            totalMaintenanceExpenses = maintenanceExpenses,
            totalDepreciation = totalDepreciation,
            totalExpenses = totalExpenses,
            netOperatingProfit = netOperatingProfit,
            partnerShares = partnerShares
        )
    }

    /**
     * تنفيذ قيد إقفال الفترة وتوزيع الأرباح التشغيلية على حسابات جاري الشركاء (3201)
     * مع بقاء رأس المال الأساسي (3101) مستقلاً تماماً ومحفوظاً دون مساس
     */
    suspend fun executePeriodClosingEntry(
        startDateMillis: Long,
        endDateMillis: Long,
        closingPeriodName: String,
        performedBy: String = "المهندس سام"
    ): Long = withContext(Dispatchers.IO) {
        db.withTransaction {
            val summary = calculateFinancialPeriodSummary(startDateMillis, endDateMillis)
            if (summary.totalGrossRevenue <= BigDecimal.ZERO && summary.totalExpenses <= BigDecimal.ZERO) {
                return@withTransaction 0L
            }

            val pName = closingPeriodName.ifBlank { "دورة مالية (${System.currentTimeMillis() % 100000})" }
            val lines = mutableListOf<JournalEntryLineEntity>()

            // 1. إقفال الإيرادات (مدين 4101 و 4201)
            if (summary.cardSalesRevenue > BigDecimal.ZERO) {
                lines.add(
                    JournalEntryLineEntity(
                        headerId = 0L,
                        accountCode = "4101",
                        accountName = "إيرادات مبيعات كروت الشبكة",
                        lineType = "DEBIT",
                        debit = summary.cardSalesRevenue,
                        lineDescription = "إقفال إيرادات مبيعات الكروت - $pName"
                    )
                )
            }
            if (summary.directSubscriptionsRevenue > BigDecimal.ZERO) {
                lines.add(
                    JournalEntryLineEntity(
                        headerId = 0L,
                        accountCode = "4201",
                        accountName = "إيرادات الاشتراكات المباشرة",
                        lineType = "DEBIT",
                        debit = summary.directSubscriptionsRevenue,
                        lineDescription = "إقفال إيرادات الاشتراكات المباشرة - $pName"
                    )
                )
            }

            // 2. إقفال التكلفة والمصروفات (دائن 5101, 5201, 5202, 5203)
            if (summary.totalCogs > BigDecimal.ZERO) {
                lines.add(
                    JournalEntryLineEntity(
                        headerId = 0L,
                        accountCode = "5101",
                        accountName = "تكلفة الكروت المباعة (COGS)",
                        lineType = "CREDIT",
                        credit = summary.totalCogs,
                        lineDescription = "إقفال تكلفة الكروت المباعة - $pName"
                    )
                )
            }
            if (summary.totalOperatingExpenses > BigDecimal.ZERO) {
                lines.add(
                    JournalEntryLineEntity(
                        headerId = 0L,
                        accountCode = "5201",
                        accountName = "مصروفات تشغيلية وعمومية",
                        lineType = "CREDIT",
                        credit = summary.totalOperatingExpenses,
                        lineDescription = "إقفال المصروفات التشغيلية - $pName"
                    )
                )
            }
            if (summary.totalMaintenanceExpenses > BigDecimal.ZERO) {
                lines.add(
                    JournalEntryLineEntity(
                        headerId = 0L,
                        accountCode = "5202",
                        accountName = "مصروفات صيانة وقطع غيار",
                        lineType = "CREDIT",
                        credit = summary.totalMaintenanceExpenses,
                        lineDescription = "إقفال مصروفات الصيانة - $pName"
                    )
                )
            }
            if (summary.totalDepreciation > BigDecimal.ZERO) {
                lines.add(
                    JournalEntryLineEntity(
                        headerId = 0L,
                        accountCode = "5203",
                        accountName = "مصروف إهلاك أصول الشبكة",
                        lineType = "CREDIT",
                        credit = summary.totalDepreciation,
                        lineDescription = "إقفال قسط إهلاك الأصول - $pName"
                    )
                )
            }

            // 3. توزيع الأرباح الصافية على الحسابات الجارية للشركاء (دائن 3201)
            val totalDebits = lines.filter { it.lineType == "DEBIT" }.fold(BigDecimal.ZERO) { acc, l -> acc.add(l.debit) }
            val totalCreditsBeforePartners = lines.filter { it.lineType == "CREDIT" }
                .fold(BigDecimal.ZERO) { acc, l -> acc.add(l.credit) }
            val balanceToDistribute = totalDebits.subtract(totalCreditsBeforePartners)

            if (balanceToDistribute > BigDecimal.ZERO && summary.partnerShares.isNotEmpty()) {
                var runningAllocated = BigDecimal.ZERO
                summary.partnerShares.forEachIndexed { index, partnerShare ->
                    val isLast = index == summary.partnerShares.size - 1
                    val shareAmount = if (isLast) {
                        (balanceToDistribute.subtract(runningAllocated)).max(BigDecimal.ZERO)
                    } else {
                        partnerShare.allocatedProfit.min(balanceToDistribute.subtract(runningAllocated))
                    }
                    runningAllocated = runningAllocated.add(shareAmount)

                    if (shareAmount > BigDecimal.ZERO) {
                        lines.add(
                            JournalEntryLineEntity(
                                headerId = 0L,
                                accountCode = "3201",
                                accountName = "جاري الشركاء والمسحوبات / ${partnerShare.partnerName}",
                                lineType = "CREDIT",
                                credit = shareAmount,
                                partyId = partnerShare.partnerId,
                                partyType = "PARTNER",
                                lineDescription = "حصة الشريك من صافي أرباح دورة $pName (${partnerShare.sharePercentage}%)"
                            )
                        )

                        // تحديث رصيد الحساب الجاري الدائن للشريك دون المساس برأس المال الأساسي
                        val partner = db.partnerDao().getPartnerById(partnerShare.partnerId)
                        if (partner != null) {
                            db.partnerDao().updatePartner(
                                partner.copy(currentAccountBalance = partner.currentAccountBalance.add(shareAmount))
                            )
                        }
                    }
                }
            } else if (balanceToDistribute < BigDecimal.ZERO && summary.partnerShares.isNotEmpty()) {
                // حالة صافي عجز/خسارة تشغيلية: تُحمل على الحسابات الجارية للشركاء كمدينين
                val lossToDistribute = balanceToDistribute.abs()
                var runningLoss = BigDecimal.ZERO
                summary.partnerShares.forEachIndexed { index, partnerShare ->
                    val isLast = index == summary.partnerShares.size - 1
                    val lossAmount = if (isLast) {
                        (lossToDistribute.subtract(runningLoss)).max(BigDecimal.ZERO)
                    } else {
                        lossToDistribute.multiply(BigDecimal.valueOf(partnerShare.sharePercentage))
                            .divide(BigDecimal("100"), 2, RoundingMode.HALF_UP)
                    }
                    runningLoss = runningLoss.add(lossAmount)

                    if (lossAmount > BigDecimal.ZERO) {
                        lines.add(
                            JournalEntryLineEntity(
                                headerId = 0L,
                                accountCode = "3201",
                                accountName = "جاري الشركاء والمسحوبات / ${partnerShare.partnerName}",
                                lineType = "DEBIT",
                                debit = lossAmount,
                                partyId = partnerShare.partnerId,
                                partyType = "PARTNER",
                                lineDescription = "تحميل عجز تشغيلي لدورة $pName (${partnerShare.sharePercentage}%)"
                            )
                        )

                        val partner = db.partnerDao().getPartnerById(partnerShare.partnerId)
                        if (partner != null) {
                            db.partnerDao().updatePartner(
                                partner.copy(currentAccountBalance = partner.currentAccountBalance.subtract(lossAmount))
                            )
                        }
                    }
                }
            }

            val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            val closeEntryNum = db.numberSequenceDao().getNextNumber("JE", currentYear, "JE")
            val header = JournalEntryHeaderEntity(
                entryNumber = closeEntryNum,
                dateMillis = System.currentTimeMillis(),
                referenceType = "PERIOD_CLOSING",
                referenceId = pName,
                description = "إقفال الدورة المالية وتوزيع الأرباح على الحسابات الجارية للشركاء: $pName",
                createdBy = performedBy
            )

            db.journalEntryDao().postBalancedEntry(header, lines)
        }
    }

    // Devices
    val allDevices: Flow<List<NetworkDeviceEntity>> = db.networkDeviceDao().getAllDevices()
    val deviceCount: Flow<Int> = db.networkDeviceDao().getDeviceCount()

    suspend fun checkIpConflict(ip: String, excludeDeviceId: Long? = null): NetworkDeviceEntity? = withContext(Dispatchers.IO) {
        val cleanIp = ip.trim()
        if (cleanIp.isEmpty()) return@withContext null
        val devices = db.networkDeviceDao().findDevicesWithIp(cleanIp)
        devices.firstOrNull { it.id != (excludeDeviceId ?: -1L) }
    }

    suspend fun suggestNextAvailableIp(basePrefix: String? = null): String = withContext(Dispatchers.IO) {
        val identity = getNetworkIdentity()
        val prefix = basePrefix ?: extractSubnetPrefix(identity.approvedDeviceSubnet)
        val devices = db.networkDeviceDao().getAllDevices().first()
        val usedHostNumbers = mutableSetOf<Int>()
        for (device in devices) {
            val ip = device.ipAddress.trim()
            if (ip.startsWith(prefix)) {
                val lastPart = ip.removePrefix(prefix).toIntOrNull()
                if (lastPart != null) {
                    usedHostNumbers.add(lastPart)
                }
            }
        }
        // Exclude gateway IP host if in same subnet
        if (identity.gatewayIp.startsWith(prefix)) {
            val gwPart = identity.gatewayIp.removePrefix(prefix).toIntOrNull()
            if (gwPart != null) usedHostNumbers.add(gwPart)
        }

        val startHost = if (identity.ipRangeStart.startsWith(prefix)) {
            identity.ipRangeStart.removePrefix(prefix).toIntOrNull() ?: 2
        } else 2

        val endHost = if (identity.ipRangeEnd.startsWith(prefix)) {
            identity.ipRangeEnd.removePrefix(prefix).toIntOrNull() ?: 254
        } else 254

        // Find first unused between startHost and endHost
        for (i in startHost..endHost) {
            if (!usedHostNumbers.contains(i)) {
                return@withContext "$prefix$i"
            }
        }
        return@withContext "${prefix}100"
    }

    suspend fun saveDevice(device: NetworkDeviceEntity): Long = withContext(Dispatchers.IO) {
        if (device.id == 0L) {
            db.networkDeviceDao().insertDevice(device)
        } else {
            db.networkDeviceDao().updateDevice(device)
            device.id
        }
    }

    suspend fun deleteDevice(device: NetworkDeviceEntity) = withContext(Dispatchers.IO) {
        db.networkDeviceDao().deleteDevice(device)
    }

    // Cards & Batches
    val allBatches: Flow<List<CardBatchEntity>> = db.cardDao().getAllBatches()
    val allCards: Flow<List<CardEntity>> = db.cardDao().getAllCards()
    val availableCardsCount: Flow<Int> = db.cardDao().countAvailableCards()
    val distributedCardsCount: Flow<Int> = db.cardDao().countDistributedCards()
    val soldCardsCount: Flow<Int> = db.cardDao().countSoldCards()

    fun getCardsForBatch(batchId: Long): Flow<List<CardEntity>> = db.cardDao().getCardsByBatch(batchId)
    fun getCardsForRetailer(retailerId: Long): Flow<List<CardEntity>> = db.cardDao().getCardsByRetailer(retailerId)

    suspend fun createBatchAndGenerateCards(
        batchName: String,
        categoryName: String,
        retailPrice: Double,
        wholesalePrice: Double,
        quotaMb: Long,
        validityHours: Int,
        speedLimit: String,
        count: Int,
        prefix: String,
        codeLength: Int = 6,
        charSet: com.example.data.cards.CodeCharacterSet = com.example.data.cards.CodeCharacterSet.DIGITS_ONLY,
        passwordPolicy: com.example.data.cards.PasswordPolicy = com.example.data.cards.PasswordPolicy.SAME_AS_USERNAME
    ): Long = withContext(Dispatchers.IO) {
        val batch = CardBatchEntity(
            batchName = batchName,
            categoryName = categoryName,
            retailPrice = retailPrice,
            wholesalePrice = wholesalePrice,
            quotaMb = quotaMb,
            validityHours = validityHours,
            speedLimit = speedLimit,
            totalCount = count,
            mikrotikProfile = "prof-${retailPrice.toInt()}",
            prefix = prefix
        )
        val batchId = db.cardDao().insertBatch(batch)

        val cards = mutableListOf<CardEntity>()
        for (i in 1..count) {
            val code = com.example.data.cards.CardGenerationEngine.generateVoucherCode(codeLength, prefix, charSet)
            val pass = when (passwordPolicy) {
                com.example.data.cards.PasswordPolicy.SAME_AS_USERNAME -> code
                com.example.data.cards.PasswordPolicy.SEPARATE_PIN -> com.example.data.cards.CardGenerationEngine.generatePin(4)
                com.example.data.cards.PasswordPolicy.NO_PASSWORD -> ""
            }
            cards.add(
                CardEntity(
                    batchId = batchId,
                    username = code,
                    password = pass,
                    categoryName = categoryName,
                    retailPrice = retailPrice,
                    wholesalePrice = wholesalePrice,
                    status = "AVAILABLE"
                )
            )
        }
        db.cardDao().insertCards(cards)
        batchId
    }

    /**
     * استيراد دفعة كروت خارجية منشأة في برامج أخرى (مثل ميكروتك يوزر مانجر، إكسل، SAS4، وغيرها)
     * مع إمكانية إيداعها بالمستودع أو صرفها مباشرة لبقالة وتحديث القيود المحاسبية.
     */
    suspend fun importExternalCardBatch(
        batchName: String,
        categoryName: String,
        retailPrice: Double,
        wholesalePrice: Double,
        quotaMb: Long,
        validityHours: Int,
        speedLimit: String,
        cardsList: List<Pair<String, String>>,
        sourceProgram: String = "برنامج خارجي",
        prefix: String = "EXT",
        targetRetailerId: Long? = null
    ): Long = withContext(Dispatchers.IO) {
        val totalCount = cardsList.size
        val batch = CardBatchEntity(
            batchName = batchName,
            categoryName = categoryName,
            retailPrice = retailPrice,
            wholesalePrice = wholesalePrice,
            quotaMb = quotaMb,
            validityHours = validityHours,
            speedLimit = speedLimit,
            totalCount = totalCount,
            mikrotikProfile = "ext-${retailPrice.toInt()}",
            prefix = prefix
        )
        val batchId = db.cardDao().insertBatch(batch)

        val retailer = if (targetRetailerId != null && targetRetailerId > 0) {
            db.retailerDao().getRetailerById(targetRetailerId)
        } else null

        val now = System.currentTimeMillis()
        val cards = cardsList.map { (user, pass) ->
            CardEntity(
                batchId = batchId,
                username = user.trim(),
                password = pass.trim().ifEmpty { user.trim() },
                categoryName = categoryName,
                retailPrice = retailPrice,
                wholesalePrice = wholesalePrice,
                status = if (retailer != null) "DISTRIBUTED" else "AVAILABLE",
                retailerId = retailer?.id,
                retailerName = retailer?.name,
                distributedAt = if (retailer != null) now else null
            )
        }
        db.cardDao().insertCards(cards)

        // إذا تم الصرف الفوري لبقالة، نقيد الرصيد والكمية بحساب البقالة
        if (retailer != null) {
            val totalWholesaleDebt = wholesalePrice * totalCount
            db.retailerDao().updateBalanceAndCards(retailer.id, totalWholesaleDebt, totalCount)
        }

        batchId
    }

    /**
     * إضافة رصيد كروت يدوياً بالعدد فقط (مثلاً 1000 كرت فئة 200 ريال)
     * بدون اشتراط وجود أو إدخال أرقام ورموز الكروت.
     * يتم تحديث عدد الكروت المتاحة في المخزن، وإنشاء سجل الدفعة،
     * وتغذية حساب البقالة مباشرة إذا حُددت كوجهة استلام.
     */
    suspend fun addManualCardQuantity(
        categoryName: String,
        quantity: Int,
        retailPrice: Double,
        wholesalePrice: Double,
        batchName: String = "إضافة يدوية - $categoryName",
        quotaMb: Long = 0,
        validityHours: Int = 24,
        speedLimit: String = "4M/2M",
        targetRetailerId: Long? = null
    ): Long = withContext(Dispatchers.IO) {
        val count = quantity.coerceAtLeast(1)
        val now = System.currentTimeMillis()

        // 1. إنشاء سجل الدفعة لتوثيق تاريخ وكمية الإضافة
        val batch = CardBatchEntity(
            batchName = batchName,
            categoryName = categoryName,
            retailPrice = retailPrice,
            wholesalePrice = wholesalePrice,
            quotaMb = quotaMb,
            validityHours = validityHours,
            speedLimit = speedLimit,
            totalCount = count,
            mikrotikProfile = "qty-${retailPrice.toInt()}",
            prefix = "QTY"
        )
        val batchId = db.cardDao().insertBatch(batch)

        val retailer = if (targetRetailerId != null && targetRetailerId > 0) {
            db.retailerDao().getRetailerById(targetRetailerId)
        } else null

        // 2. إدراج الكروت الرقمية لتمكين المتابعة والتوزيع وتوليد الفواتير بدون إجبار المستخدم على إدخال أرقام
        val prefixCode = categoryName.filter { it.isDigit() }.ifEmpty { "C" }
        val timeCode = (now % 100000).toString()
        val cards = (1..count).map { i ->
            CardEntity(
                batchId = batchId,
                username = "$prefixCode-$timeCode-${String.format(java.util.Locale.US, "%04d", i)}",
                password = "", // غير مشروط وجود رمز أو كلمة مرور
                categoryName = categoryName,
                retailPrice = retailPrice,
                wholesalePrice = wholesalePrice,
                status = if (retailer != null) "DISTRIBUTED" else "AVAILABLE",
                retailerId = retailer?.id,
                retailerName = retailer?.name,
                distributedAt = if (retailer != null) now else null
            )
        }
        db.cardDao().insertCards(cards)

        // 3. تحديث أو إدراج مخزون الكروت في جدول inventory_items
        val existingItem = db.inventoryDao().getItemByPackageName(categoryName)
        if (existingItem != null) {
            val updatedQty = if (retailer != null) {
                existingItem.quantityAvailable // تم صرفها مباشرة للبقالة
            } else {
                existingItem.quantityAvailable + count
            }
            db.inventoryDao().updateItem(
                existingItem.copy(
                    quantityAvailable = updatedQty,
                    wholesalePrice = BigDecimal.valueOf(wholesalePrice),
                    retailPrice = BigDecimal.valueOf(retailPrice)
                )
            )
        } else {
            val initialQty = if (retailer != null) 0 else count
            db.inventoryDao().insertItem(
                InventoryItemEntity(
                    packageName = categoryName,
                    quantityAvailable = initialQty,
                    wholesalePrice = BigDecimal.valueOf(wholesalePrice),
                    retailPrice = BigDecimal.valueOf(retailPrice)
                )
            )
        }

        // 4. إذا تم الصرف الفوري لبقالة، نقيد الرصيد والكمية بحساب البقالة
        if (retailer != null) {
            val totalWholesaleDebt = wholesalePrice * count
            db.retailerDao().updateBalanceAndCards(retailer.id, totalWholesaleDebt, count)
        }

        batchId
    }

    // Inventory Management (مخزن الكروت بالعدد)
    val allInventoryItems: Flow<List<InventoryItemEntity>> = db.inventoryDao().getAllInventoryItems()
    val allInventoryMovements: Flow<List<InventoryMovementEntity>> = db.inventoryMovementDao().getAllMovements()

    suspend fun saveInventoryItem(item: InventoryItemEntity): Long = withContext(Dispatchers.IO) {
        if (item.id == 0L) {
            db.inventoryDao().insertItem(item)
        } else {
            db.inventoryDao().updateItem(item)
            item.id
        }
    }

    suspend fun deleteInventoryItem(id: Long) = withContext(Dispatchers.IO) {
        db.inventoryDao().deleteItem(id)
    }

    suspend fun findInventoryItemFlexible(packageName: String): InventoryItemEntity? {
        val cleanName = packageName.trim()
        if (cleanName.isBlank()) return null

        val allItems = db.inventoryDao().getAllItemsList()
        return com.example.util.InventoryMatchingHelper.findBestMatchingInventoryItem(cleanName, allItems)
    }

    private fun normalizeArabicText(text: String): String {
        return com.example.util.InventoryMatchingHelper.normalizeArabic(text)
    }

    /**
     * توريد أو إضافة كمية للمخزن بالعدد فقط بدون إنشاء أو توليد أرقام كروت
     * يزداد الرصيد المتاح لهذا الصنف تلقائياً ويوثق في سجل حركات المخزن
     */
    suspend fun addStockToInventory(
        packageName: String,
        quantity: Int,
        wholesalePrice: Double = 0.0,
        retailPrice: Double = 0.0,
        notes: String = ""
    ): Long = withContext(Dispatchers.IO) {
        val count = quantity.coerceAtLeast(1)
        val existingItem = db.inventoryDao().getItemByPackageName(packageName)
        val finalWholesale: BigDecimal = if (wholesalePrice > 0) BigDecimal.valueOf(wholesalePrice) else (existingItem?.wholesalePrice ?: BigDecimal.ZERO)
        val finalRetail: BigDecimal = if (retailPrice > 0) BigDecimal.valueOf(retailPrice) else (existingItem?.retailPrice ?: BigDecimal.ZERO)

        val newBalance = if (existingItem != null) {
            val updatedQty = existingItem.quantityAvailable + count
            db.inventoryDao().updateItem(
                existingItem.copy(
                    quantityAvailable = updatedQty,
                    wholesalePrice = finalWholesale,
                    retailPrice = finalRetail
                )
            )
            updatedQty
        } else {
            db.inventoryDao().insertItem(
                InventoryItemEntity(
                    packageName = packageName,
                    quantityAvailable = count,
                    wholesalePrice = finalWholesale,
                    retailPrice = finalRetail
                )
            )
            count
        }

        // تسجيل حركة مخزنية رسمية
        val movement = InventoryMovementEntity(
            packageName = packageName,
            movementType = "SUPPLY",
            quantityChange = count,
            resultingBalance = newBalance,
            referenceNumber = "SUP-${System.currentTimeMillis() % 100000}",
            customerOrSupplier = "المستودع الرئيسي",
            unitPrice = finalWholesale,
            notes = notes.ifBlank { "توريد كمية كروت للمخزن (+$count كرت)" }
        )
        db.inventoryMovementDao().insertMovement(movement)

        newBalance.toLong()
    }

    fun matchesPackage(nameA: String, nameB: String): Boolean {
        return com.example.util.InventoryMatchingHelper.matchesPackage(nameA, nameB)
    }

    /**
     * استخراج عناصر فاتورة المبيعات بدقة متناهية من itemsJson أو itemsSummary أو totalCardsCount
     */
    fun extractInvoiceItems(invoice: CardSalesInvoiceEntity): List<CardSalesInvoiceItem> {
        return com.example.util.InventoryMatchingHelper.extractInvoiceItems(invoice)
    }

    /**
     * خصم كروت الفاتورة من المخزن الفعلي وتوثيق الحركة في سجل حركات المخزن
     */
    suspend fun deductInvoiceFromInventory(
        invoice: CardSalesInvoiceEntity,
        reason: String = "خصم مبيعات"
    ): Boolean = withContext(Dispatchers.IO) {
        if (invoice.isVoided || invoice.status == "VOIDED") return@withContext false
        val items = extractInvoiceItems(invoice)
        if (items.isEmpty()) return@withContext false

        for (item in items) {
            val existing = findInventoryItemFlexible(item.packageName)
            val canonicalPkgName = existing?.packageName ?: item.packageName.trim()
            val newQty = if (existing != null) {
                val updated = (existing.quantityAvailable - item.quantity).coerceAtLeast(0)
                db.inventoryDao().updateItem(existing.copy(quantityAvailable = updated))
                updated
            } else {
                val newItem = InventoryItemEntity(
                    packageName = canonicalPkgName,
                    quantityAvailable = 0,
                    wholesalePrice = BigDecimal.valueOf(item.unitPrice),
                    retailPrice = BigDecimal.valueOf(item.retailPrice)
                )
                db.inventoryDao().insertItem(newItem)
                0
            }

            db.inventoryMovementDao().insertMovement(
                InventoryMovementEntity(
                    packageName = canonicalPkgName,
                    movementType = "SALE",
                    quantityChange = -item.quantity,
                    resultingBalance = newQty,
                    referenceNumber = invoice.invoiceNumber,
                    customerOrSupplier = invoice.customerName,
                    unitPrice = item.unitPrice,
                    notes = "$reason - فاتورة #${invoice.invoiceNumber} لـ ${invoice.customerName}"
                )
            )
        }
        true
    }

    /**
     * استرجاع كروت الفاتورة إلى المخزن الفعلي عند الحذف أو الإلغاء وتوثيق حركة الاسترجاع
     */
    suspend fun returnInvoiceToInventory(
        invoice: CardSalesInvoiceEntity,
        reason: String = "استرجاع مبيعات"
    ): Boolean = withContext(Dispatchers.IO) {
        val items = extractInvoiceItems(invoice)
        if (items.isEmpty()) return@withContext false

        for (item in items) {
            val existing = findInventoryItemFlexible(item.packageName)
            val canonicalPkgName = existing?.packageName ?: item.packageName.trim()
            val newQty = if (existing != null) {
                val updated = existing.quantityAvailable + item.quantity
                db.inventoryDao().updateItem(existing.copy(quantityAvailable = updated))
                updated
            } else {
                val newItem = InventoryItemEntity(
                    packageName = canonicalPkgName,
                    quantityAvailable = item.quantity,
                    wholesalePrice = BigDecimal.valueOf(item.unitPrice),
                    retailPrice = BigDecimal.valueOf(item.retailPrice)
                )
                db.inventoryDao().insertItem(newItem)
                item.quantity
            }

            db.inventoryMovementDao().insertMovement(
                InventoryMovementEntity(
                    packageName = canonicalPkgName,
                    movementType = "RETURN",
                    quantityChange = item.quantity,
                    resultingBalance = newQty,
                    referenceNumber = "REFUND-${invoice.invoiceNumber}",
                    customerOrSupplier = invoice.customerName,
                    unitPrice = item.unitPrice,
                    notes = "$reason - استرجاع كميات الفاتورة #${invoice.invoiceNumber}"
                )
            )
        }
        true
    }

    /**
     * جرد احترافي شامل ومطابقة تامة بين كافة فواتير المبيعات ورصيد المخزون الفعلي
     * يحسب عدد الكروت المباعة في كافة الفواتير من كل فئة وينقصها من المخزن فوراً،
     * مع تنظيف الأصناف المكررة وتوثيق السجل المخزني بدقة 100%.
     */
    suspend fun reconcileInventoryWithSalesInvoices(): InventoryReconciliationSummary = withContext(Dispatchers.IO) {
        db.withTransaction {
            val allInvoices = db.cardSalesInvoiceDao().getAllInvoicesList()
            var allInventory = db.inventoryDao().getAllItemsList().toMutableList()

            // 1. تنظيف ودمج أي أصناف وهمية برصيد 0 تم إنشاؤها مسبقاً وتطابق أصنافاً حقيقية ذات رصيد
            val itemsToDelete = mutableListOf<Long>()
            for (item in allInventory) {
                if (item.quantityAvailable == 0) {
                    val realMatch = allInventory.find { other ->
                        other.id != item.id && other.quantityAvailable > 0 && matchesPackage(other.packageName, item.packageName)
                    }
                    if (realMatch != null) {
                        itemsToDelete.add(item.id)
                        db.inventoryDao().deleteItem(item.id)
                    }
                }
            }
            if (itemsToDelete.isNotEmpty()) {
                allInventory = db.inventoryDao().getAllItemsList().toMutableList()
            }

            var newlyDeductedInvoices = 0
            var totalCardsNewlyDeducted = 0
            var totalCardsReturned = 0
            val packageSoldMap = mutableMapOf<String, Int>()
            val newlyDeductedPerPkg = mutableMapOf<String, Int>()

            for (inv in allInvoices) {
                val isVoid = inv.isVoided || inv.status == "VOIDED"
                val items = extractInvoiceItems(inv)

                if (!isVoid) {
                    for (item in items) {
                        val key = item.packageName.trim()
                        packageSoldMap[key] = (packageSoldMap[key] ?: 0) + item.quantity
                    }

                    val hasSale = db.inventoryMovementDao().hasSaleMovementForInvoice(inv.invoiceNumber) > 0
                    if (!hasSale && items.isNotEmpty()) {
                        deductInvoiceFromInventory(inv, "جرد ومطابقة مبيعات الفواتير")
                        newlyDeductedInvoices++
                        for (item in items) {
                            totalCardsNewlyDeducted += item.quantity
                            val key = item.packageName.trim()
                            newlyDeductedPerPkg[key] = (newlyDeductedPerPkg[key] ?: 0) + item.quantity
                        }
                    }
                } else {
                    val hasSale = db.inventoryMovementDao().hasSaleMovementForInvoice(inv.invoiceNumber) > 0
                    val hasReturn = db.inventoryMovementDao().hasReturnMovementForInvoice(inv.invoiceNumber) > 0
                    if (hasSale && !hasReturn && items.isNotEmpty()) {
                        returnInvoiceToInventory(inv, "استرجاع كميات فاتورة ملغاة غير مسترجعة")
                        for (item in items) {
                            totalCardsReturned += item.quantity
                        }
                    }
                }
            }

            val finalInventory = db.inventoryDao().getAllItemsList()
            val reports = finalInventory.map { invItem ->
                val clean = invItem.packageName.trim()
                val sold = packageSoldMap.entries
                    .filter { matchesPackage(it.key, clean) }
                    .sumOf { it.value }
                val newlyDeducted = newlyDeductedPerPkg.entries
                    .filter { matchesPackage(it.key, clean) }
                    .sumOf { it.value }

                PackageAuditReport(
                    packageName = invItem.packageName,
                    totalSoldInInvoices = sold,
                    currentAvailableStock = invItem.quantityAvailable,
                    newlyDeductedCount = newlyDeducted,
                    wholesalePrice = invItem.wholesalePriceDouble,
                    retailPrice = invItem.retailPriceDouble,
                    totalSoldValuation = sold * invItem.wholesalePriceDouble,
                    availableStockValuation = invItem.quantityAvailable * invItem.wholesalePriceDouble
                )
            }

            InventoryReconciliationSummary(
                totalInvoicesAudited = allInvoices.size,
                newlyDeductedInvoicesCount = newlyDeductedInvoices,
                totalCardsNewlyDeducted = totalCardsNewlyDeducted,
                totalReturnedCardsFromVoided = totalCardsReturned,
                categoryReports = reports
            )
        }
    }

    // Card Sales Invoices (فواتير مبيعات الكروت متعددة الأصناف)
    val allSalesInvoices: Flow<List<CardSalesInvoiceEntity>> = db.cardSalesInvoiceDao().getAllSalesInvoices()
    val totalSalesAmount: Flow<Double?> = db.cardSalesInvoiceDao().getTotalSalesAmount()
    val totalSoldCardsCount: Flow<Int?> = db.cardSalesInvoiceDao().getTotalSoldCardsCount()
    val totalCreditRemaining: Flow<Double?> = db.cardSalesInvoiceDao().getTotalCreditRemaining()

    private data class InvoicePaymentResult(
        val canonicalType: String,
        val paidAmount: BigDecimal,
        val remainingAmount: BigDecimal,
        val status: String
    )

    private fun calculateInvoicePayment(
        paymentType: String,
        totalAmount: BigDecimal,
        inputPaidAmount: BigDecimal
    ): InvoicePaymentResult {
        val upperType = paymentType.trim().uppercase(Locale.ROOT)
        val isCredit = upperType == "CREDIT" || paymentType.contains("آجل") || paymentType.contains("UNPAID", ignoreCase = true) || paymentType.contains("Credit", ignoreCase = true)
        val isCash = upperType == "CASH" || paymentType.contains("نقد") || paymentType.contains("Cash", ignoreCase = true)
        val isPartial = upperType == "PARTIAL" || paymentType.contains("مقدم") || paymentType.contains("Partial", ignoreCase = true)

        val canonicalType = when {
            isCredit -> "CREDIT"
            isCash -> "CASH"
            isPartial -> "PARTIAL"
            else -> if (inputPaidAmount.compareTo(BigDecimal.ZERO) <= 0) "CREDIT" else if (inputPaidAmount.compareTo(totalAmount) >= 0) "CASH" else "PARTIAL"
        }

        val actualPaid = when (canonicalType) {
            "CASH" -> totalAmount
            "CREDIT" -> BigDecimal.ZERO
            "PARTIAL" -> inputPaidAmount.max(BigDecimal.ZERO).min(totalAmount)
            else -> BigDecimal.ZERO
        }

        val remaining = totalAmount.subtract(actualPaid).max(BigDecimal.ZERO)

        val status = when {
            remaining.compareTo(BigDecimal("0.01")) <= 0 -> "PAID"
            actualPaid.compareTo(BigDecimal("0.01")) <= 0 -> "CREDIT"
            else -> "PARTIAL"
        }

        return InvoicePaymentResult(
            canonicalType = canonicalType,
            paidAmount = actualPaid,
            remainingAmount = remaining,
            status = status
        )
    }

    private fun calculateInvoicePayment(
        paymentType: String,
        totalAmount: Double,
        inputPaidAmount: Double
    ): InvoicePaymentResult {
        return calculateInvoicePayment(
            paymentType,
            BigDecimal.valueOf(totalAmount),
            BigDecimal.valueOf(inputPaidAmount)
        )
    }

    /**
     * إصدار فاتورة مبيعات كروت متعددة الأصناف احترافية وفق المعايير المحاسبية
     * وينقص العدد تلقائياً من كل صنف بالمخزن، مع حساب الإجمالي والمدفوع والمتبقي
     */
    suspend fun issueMultiItemSalesInvoice(
        customerName: String,
        customerPhone: String = "",
        retailerId: Long? = null,
        items: List<CardSalesInvoiceItem>,
        paymentType: String = "CASH", // "CASH", "CREDIT", "PARTIAL"
        paidAmount: Double = 0.0,
        notes: String = "",
        issuerName: String = "المهندس حسن"
    ): Long = withContext(Dispatchers.IO) {
        db.withTransaction {
            val totalAmount = BigDecimal.valueOf(items.sumOf { it.lineTotal })
            val totalCards = items.sumOf { it.quantity }

            // 1. Check stock for ALL items in transaction first
            for (item in items) {
                val inventoryItem = findInventoryItemFlexible(item.packageName)
                val availableInventoryQty = inventoryItem?.quantityAvailable ?: 0
                val availableCardsCount = db.cardDao().getAvailableCardsByCategory(item.packageName, item.quantity).size
                val effectiveAvailableStock = maxOf(availableInventoryQty, availableCardsCount)

                if (effectiveAvailableStock < item.quantity) {
                    throw IllegalStateException("عفواً، الكمية المطلوبة غير متوفرة في المخزن (${item.packageName})")
                }
            }

            val paymentMath = calculateInvoicePayment(paymentType, totalAmount, BigDecimal.valueOf(paidAmount))

            // Explicit Credit Persistence Rule
            val isCreditType = paymentMath.canonicalType == "CREDIT" || paymentType.contains("آجل") || paymentType.contains("UNPAID", ignoreCase = true)
            val finalCanonicalType = if (isCreditType) "CREDIT" else paymentMath.canonicalType
            val finalPaidAmount = if (isCreditType) BigDecimal.ZERO else paymentMath.paidAmount
            val finalRemainingAmount = if (isCreditType) totalAmount else paymentMath.remainingAmount
            val finalStatus = if (isCreditType) "CREDIT" else paymentMath.status

            val totalInvoicesCount = db.cardSalesInvoiceDao().getSalesInvoicesList().size + 1
            var candidateNumber = "INV-2026-${String.format(Locale.US, "%04d", totalInvoicesCount)}"
            var collisionOffset = 1
            while (db.cardSalesInvoiceDao().getInvoiceByNumber(candidateNumber) != null) {
                candidateNumber = "INV-2026-${String.format(Locale.US, "%04d", totalInvoicesCount + collisionOffset)}"
                collisionOffset++
            }
            val invoiceNumber = candidateNumber
            val itemsSummary = items.joinToString(" + ") { "${it.quantity} كرت [${it.packageName}]" }

            // JSON serialization of items
            val jsonArray = JSONArray()
            items.forEach { item ->
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("packageName", item.packageName)
                    put("quantity", item.quantity)
                    put("unitPrice", item.unitPrice)
                    put("retailPrice", item.retailPrice)
                    put("lineTotal", item.lineTotal)
                }
                jsonArray.put(obj)
            }

            // البحث عن البقالة إذا لم يكن الـ ID محدداً صراحة
            val finalRetailerId = if (retailerId != null && retailerId > 0) {
                retailerId
            } else {
                val allRet = db.retailerDao().getRetailersList()
                allRet.find { it.name.trim().equals(customerName.trim(), ignoreCase = true) }?.id
            }

            val invoiceEntity = CardSalesInvoiceEntity(
                invoiceNumber = invoiceNumber,
                customerName = customerName.ifBlank { "عميل نقدي" },
                customerPhone = customerPhone,
                retailerId = finalRetailerId,
                paymentType = finalCanonicalType,
                totalAmount = totalAmount,
                paidAmount = finalPaidAmount,
                remainingAmount = finalRemainingAmount,
                totalCardsCount = totalCards,
                itemsCount = items.size,
                itemsSummary = itemsSummary,
                itemsJson = jsonArray.toString(),
                notes = notes,
                issuerName = issuerName,
                status = finalStatus
            )
            val invoiceId = db.cardSalesInvoiceDao().insertInvoice(invoiceEntity)

            // Synchronize invoice with customer_ledger
            if (finalRetailerId != null && finalRetailerId > 0) {
                db.customerLedgerDao().insertLedgerEntry(
                    com.example.data.local.entity.CustomerLedgerEntity(
                        customerId = finalRetailerId,
                        transactionDate = System.currentTimeMillis(),
                        transactionType = "INVOICE",
                        referenceId = invoiceId.toString(),
                        debit = totalAmount,
                        credit = BigDecimal.ZERO,
                        description = "فاتورة مبيعات #$invoiceNumber ($itemsSummary)"
                    )
                )
            }

            // 2. Stock Deduction and Card Status Update
            val timestamp = System.currentTimeMillis()

            items.forEach { item ->
                val availableCards = db.cardDao().getAvailableCardsByCategory(item.packageName, item.quantity)
                if (availableCards.isNotEmpty()) {
                    val cardIds = availableCards.map { it.id }
                    db.cardDao().markCardsSoldForInvoice(cardIds, invoiceId, timestamp)
                }

                val existing = findInventoryItemFlexible(item.packageName)
                val newQty = if (existing != null) {
                    val updated = (existing.quantityAvailable - item.quantity).coerceAtLeast(0)
                    db.inventoryDao().updateItem(existing.copy(quantityAvailable = updated))
                    updated
                } else {
                    val newItem = InventoryItemEntity(
                        packageName = item.packageName.trim(),
                        quantityAvailable = 0,
                        wholesalePrice = BigDecimal.valueOf(item.unitPrice),
                        retailPrice = BigDecimal.valueOf(item.retailPrice)
                    )
                    db.inventoryDao().insertItem(newItem)
                    0
                }

                // قيد حركة مخزنية خارجة (خصم مبيعات)
                db.inventoryMovementDao().insertMovement(
                    InventoryMovementEntity(
                        packageName = item.packageName,
                        movementType = "SALE",
                        quantityChange = -item.quantity,
                        resultingBalance = newQty,
                        referenceNumber = invoiceNumber,
                        customerOrSupplier = customerName.ifBlank { "عميل نقدي" },
                        unitPrice = item.unitPrice,
                        notes = "خصم مبيعات فاتورة رقم $invoiceNumber ($finalCanonicalType)"
                    )
                )
            }

            // إنشاء سند قبض مالي رسمي مربوط بمعرف الفاتورة مباشرة عند وجود سداد لبقالة
            if (finalPaidAmount > BigDecimal.ZERO && finalRetailerId != null) {
                val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
                val docYear = cal.get(Calendar.YEAR)
                val voucherNum = db.numberSequenceDao().getNextNumber("REC", docYear, "REC")
                val voucher = FinancialVoucherEntity(
                    voucherNumber = voucherNum,
                    voucherType = "RECEIPT",
                    amount = finalPaidAmount,
                    partyName = customerName.ifBlank { "مبيعات كروت نقدية" },
                    retailerId = finalRetailerId,
                    invoiceId = invoiceId,
                    invoiceNumber = invoiceNumber,
                    invoiceKind = "SALES",
                    allocatedAmount = finalPaidAmount,
                    category = "مبيعات كروت",
                    paymentMethod = if (finalCanonicalType == "CASH") "نقداً" else "دفعة مقدمة",
                    description = "متحصلات من فاتورة مبيعات كروت $invoiceNumber - $itemsSummary",
                    issuerName = issuerName,
                    notes = notes,
                    dateMillis = timestamp
                )
                db.financialVoucherDao().insertVoucher(voucher)
                try {
                    postingService.postFinancialVoucher(voucher, issuerName)
                } catch (e: Exception) {
                    android.util.Log.e("NetworkRepository", "Error posting receipt voucher for invoice: ${e.message}", e)
                }
            }

            // ترحيل فاتورة المبيعات عبر المحرك المركزي الموحد (Single Source of Truth)
            try {
                postingService.postSalesInvoice(
                    invoice = invoiceEntity.copy(id = invoiceId),
                    items = items,
                    performer = issuerName
                )
            } catch (e: Exception) {
                android.util.Log.e("NetworkRepository", "Error posting sales invoice: ${e.message}", e)
            }

            // تسوية دفتر الأستاذ دون تعديل مزدوج
            reconcileAccountingLedgerInternal()

            invoiceId
        }
    }

    /**
     * تحديث وتعديل فاتورة مبيعات كروت سابقة بشكل ذري (Atomic Transaction):
     * - يقوم باسترجاع الأصناف والكميات السابقة للمخزن
     * - إلغاء السند المالي السابق المرتبط بها برقم الفاتورة أو المعرف
     * - خصم الأصناف والكميات المعدلة الجديدة من المخزن
     * - تحديث بيانات الفاتورة والسند المالي بربط قيد مباشر
     * - إعادة تسوية أرصدة المحلات محاسبياً 100% بدون تعديل يدوي مزدوج
     */
    suspend fun updateMultiItemSalesInvoice(
        originalInvoice: CardSalesInvoiceEntity,
        customerName: String,
        customerPhone: String = "",
        retailerId: Long? = null,
        items: List<CardSalesInvoiceItem>,
        paymentType: String = "CASH",
        paidAmount: Double = 0.0,
        notes: String = "",
        issuerName: String = "المهندس حسن"
    ) = withContext(Dispatchers.IO) {
        db.withTransaction {
            // 1. استرجاع الكميات السابقة للمخزن
            returnInvoiceToInventory(originalInvoice, "استرجاع كميات المخزن لتعديل الفاتورة")

            // 2. إلغاء حذف السندات القديمة للحفاظ على السجل المالي وسلامة الصندوق

            // 3. خصم الأصناف الجديدة من المخزن
            items.forEach { item ->
                val inventoryItem = findInventoryItemFlexible(item.packageName)
                val newQty = if (inventoryItem != null) {
                    val q = (inventoryItem.quantityAvailable - item.quantity).coerceAtLeast(0)
                    db.inventoryDao().updateItem(inventoryItem.copy(quantityAvailable = q))
                    q
                } else {
                    val newItem = InventoryItemEntity(
                        packageName = item.packageName.trim(),
                        quantityAvailable = 0,
                        wholesalePrice = BigDecimal.valueOf(item.unitPrice),
                        retailPrice = BigDecimal.valueOf(item.retailPrice)
                    )
                    db.inventoryDao().insertItem(newItem)
                    0
                }
                db.inventoryMovementDao().insertMovement(
                    InventoryMovementEntity(
                        packageName = item.packageName,
                        movementType = "SALE",
                        quantityChange = -item.quantity,
                        resultingBalance = newQty,
                        referenceNumber = "EDIT-${originalInvoice.invoiceNumber}",
                        customerOrSupplier = customerName,
                        unitPrice = item.unitPrice,
                        notes = "تعديل فاتورة مبيعات #${originalInvoice.invoiceNumber}"
                    )
                )
            }

            // 4. احتساب المبالغ والأصناف
            val totalAmount = BigDecimal.valueOf(items.sumOf { it.lineTotal })
            val totalCardsCount = items.sumOf { it.quantity }
            val paymentMath = calculateInvoicePayment(paymentType, totalAmount, BigDecimal.valueOf(paidAmount))

            val itemsJsonArray = JSONArray()
            items.forEach { item ->
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("packageName", item.packageName)
                    put("quantity", item.quantity)
                    put("unitPrice", item.unitPrice)
                    put("retailPrice", item.retailPrice)
                    put("lineTotal", item.lineTotal)
                }
                itemsJsonArray.put(obj)
            }
            val itemsSummary = items.joinToString("، ") { "${it.packageName} (${it.quantity})" }

            // 5. حفظ التعديل في قاعدة البيانات
            val updatedInvoice = originalInvoice.copy(
                customerName = customerName,
                customerPhone = customerPhone,
                retailerId = retailerId,
                totalAmount = totalAmount,
                paidAmount = paymentMath.paidAmount,
                remainingAmount = paymentMath.remainingAmount,
                totalCardsCount = totalCardsCount,
                paymentType = paymentMath.canonicalType,
                itemsJson = itemsJsonArray.toString(),
                itemsSummary = itemsSummary,
                notes = notes,
                issuerName = issuerName,
                status = paymentMath.status
            )
            db.cardSalesInvoiceDao().updateInvoice(updatedInvoice)

            // 6. التعامل مع السند المالي بالفارق (paymentDelta) دون حذف السندات القديمة
            val paymentDelta = paymentMath.paidAmount.subtract(originalInvoice.paidAmount)
            val cal = Calendar.getInstance().apply { timeInMillis = originalInvoice.invoiceDateMillis }
            val docYear = cal.get(Calendar.YEAR)
            if (paymentDelta > java.math.BigDecimal.ZERO) {
                val voucherNum = db.numberSequenceDao().getNextNumber("REC", docYear, "REC")
                val voucher = FinancialVoucherEntity(
                    voucherNumber = voucherNum,
                    voucherType = "RECEIPT",
                    amount = paymentDelta,
                    partyName = customerName,
                    category = "مبيعات كروت شبكة",
                    paymentMethod = if (paymentType == "CASH") "نقداً" else "دفعة مقدمة",
                    description = "سند قبض تكميلي لتعديل فاتورة مبيعات #${originalInvoice.invoiceNumber} ($itemsSummary)",
                    issuerName = issuerName,
                    notes = notes,
                    retailerId = retailerId,
                    invoiceId = originalInvoice.id,
                    invoiceNumber = originalInvoice.invoiceNumber,
                    invoiceKind = "SALES",
                    allocatedAmount = paymentDelta,
                    dateMillis = System.currentTimeMillis()
                )
                db.financialVoucherDao().insertVoucher(voucher)
                try {
                    postingService.postFinancialVoucher(voucher, issuerName)
                } catch (e: Exception) {
                    android.util.Log.e("NetworkRepository", "Error posting receipt voucher on edit: ${e.message}", e)
                }
            } else if (paymentDelta < java.math.BigDecimal.ZERO && retailerId == null) {
                // للعملاء النقديين المباشرين فقط عند رد مبالغ نقدية فعلية، أما البقالات فتُسوّى في ذمم العملاء
                val refundAmount = paymentDelta.abs()
                val voucherNum = db.numberSequenceDao().getNextNumber("PAY", docYear, "PAY")
                val voucher = FinancialVoucherEntity(
                    voucherNumber = voucherNum,
                    voucherType = "PAYMENT",
                    amount = refundAmount,
                    partyName = customerName,
                    category = "استرداد/تسوية مبيعات كروت",
                    paymentMethod = "نقداً",
                    description = "سند صرف استرداد لتعديل فاتورة مبيعات #${originalInvoice.invoiceNumber} ($itemsSummary)",
                    issuerName = issuerName,
                    notes = notes,
                    retailerId = null,
                    invoiceId = originalInvoice.id,
                    invoiceNumber = originalInvoice.invoiceNumber,
                    invoiceKind = "SALES",
                    allocatedAmount = refundAmount,
                    dateMillis = System.currentTimeMillis()
                )
                db.financialVoucherDao().insertVoucher(voucher)
                try {
                    postingService.postFinancialVoucher(voucher, issuerName)
                } catch (e: Exception) {
                    android.util.Log.e("NetworkRepository", "Error posting refund voucher on edit: ${e.message}", e)
                }
            }

            // 7. تحديث قيود اليومية المركزية المزدوجة للفاتورة المعدلة
            try {
                postingService.updateSalesInvoicePosting(
                    invoice = updatedInvoice,
                    items = items,
                    performer = issuerName
                )
            } catch (e: Exception) {
                android.util.Log.e("NetworkRepository", "Error updating sales invoice posting: ${e.message}", e)
            }

            // Update customer_ledger
            db.customerLedgerDao().deleteByReferenceId(originalInvoice.id.toString())
            db.customerLedgerDao().deleteByReferenceId("INV_PAY_${originalInvoice.id}")
            if (retailerId != null && retailerId > 0) {
                db.customerLedgerDao().insertLedgerEntry(
                    com.example.data.local.entity.CustomerLedgerEntity(
                        customerId = retailerId,
                        transactionDate = System.currentTimeMillis(),
                        transactionType = "INVOICE",
                        referenceId = originalInvoice.id.toString(),
                        debit = totalAmount,
                        credit = BigDecimal.ZERO,
                        description = "تعديل فاتورة مبيعات #${originalInvoice.invoiceNumber} ($itemsSummary)"
                    )
                )
            }

            // 7. تسوية الدفاتر المحاسبية
            reconcileAccountingLedgerInternal()
        }
    }

    /**
     * استنساخ مباشر وفوري لفاتورة مبيعات كروت:
     * - ينشئ فاتورة جديدة برقم جديد كلياً
     * - ينسخ كافة الأصناف والأسعار ونوع الدفع وبيانات العميل
     * - يخصم الكروت من المخزن ويسجل السند المحاسبي
     */
    suspend fun cloneSalesInvoice(
        originalInvoice: CardSalesInvoiceEntity,
        issuerName: String = "المهندس حسن"
    ): Long = withContext(Dispatchers.IO) {
        val items = mutableListOf<CardSalesInvoiceItem>()
        if (originalInvoice.itemsJson.isNotBlank()) {
            try {
                val arr = JSONArray(originalInvoice.itemsJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    items.add(
                        CardSalesInvoiceItem(
                            id = java.util.UUID.randomUUID().toString(),
                            packageName = obj.optString("packageName", ""),
                            quantity = obj.optInt("quantity", 1),
                            unitPrice = obj.optDouble("unitPrice", 0.0),
                            retailPrice = obj.optDouble("retailPrice", 0.0)
                        )
                    )
                }
            } catch (e: Exception) {
                // ignore
            }
        }

        val finalItems = if (items.isNotEmpty()) items else listOf(
            CardSalesInvoiceItem(
                id = java.util.UUID.randomUUID().toString(),
                packageName = "صنف كروت",
                quantity = originalInvoice.totalCardsCount.coerceAtLeast(1),
                unitPrice = if (originalInvoice.totalCardsCount > 0) originalInvoice.totalAmount.toDouble() / originalInvoice.totalCardsCount else originalInvoice.totalAmount.toDouble(),
                retailPrice = if (originalInvoice.totalCardsCount > 0) originalInvoice.totalAmount.toDouble() / originalInvoice.totalCardsCount else originalInvoice.totalAmount.toDouble()
            )
        )

        issueMultiItemSalesInvoice(
            customerName = originalInvoice.customerName,
            customerPhone = originalInvoice.customerPhone,
            retailerId = originalInvoice.retailerId,
            items = finalItems,
            paymentType = originalInvoice.paymentType,
            paidAmount = originalInvoice.paidAmount.toDouble(),
            notes = if (originalInvoice.notes.isNotBlank()) "${originalInvoice.notes} (مستنسخة من #${originalInvoice.invoiceNumber})" else "مستنسخة من #${originalInvoice.invoiceNumber}",
            issuerName = issuerName
        )
    }

    /**
     * الدالة الداخلية لتسوية دفتر الأستاذ (تستدعى داخل معامَلة ذرية withTransaction).
     * تعتمد كلياً على الربط الصريح بالمعرفات (invoiceId) والمطابقة الحسابية دون التحقق النصي الهش.
     */
    private suspend fun reconcileAccountingLedgerInternal() {
        try {
            // 0. تنظيف وتطهير أي قيود أو سندات قديمة من بادئة REC-INV- أو INV_PAY_
            db.journalEntryDao().sanitizeLegacyRecInvLines()
            db.journalEntryDao().sanitizeLegacyRecInvHeaders()
            db.customerLedgerDao().sanitizeLegacyRecInvLedger()
            db.financialVoucherDao().sanitizeLegacyRecInvVouchers()

            // تنظيف أي فواتير وهمية قديمة
            db.cardSalesInvoiceDao().deleteSyntheticInvoices()

            // 1. فحص وترحيل كافة الفواتير والسندات التي لم تسجل في اليومية المركزية أولاً لضمان اكتمال قيود الأستاذ العام
            postingService.repairAllUnpostedEntries()

            val retailers = db.retailerDao().getRetailersList()
            val initialInvoices = db.cardSalesInvoiceDao().getSalesInvoicesList()
            val initialVouchers = db.financialVoucherDao().getVouchersList()

            val retailersByName = retailers.associateBy { it.name.trim().lowercase() }
            val retailersById = retailers.associateBy { it.id }
            val invoicesByNumber = initialInvoices.associateBy { it.invoiceNumber }

            // 1. تسوية وتوحيد الربط للبقالات والفواتير القديمة غير المربوطة بمعرف
            for (inv in initialInvoices) {
                if (inv.retailerId == null || inv.retailerId == 0L || !retailersById.containsKey(inv.retailerId)) {
                    val matchedRetailer = retailersByName[inv.customerName.trim().lowercase()]
                    if (matchedRetailer != null) {
                        db.cardSalesInvoiceDao().updateInvoice(inv.copy(retailerId = matchedRetailer.id))
                    }
                }
            }

            for (v in initialVouchers) {
                var updatedVoucher = v
                var changed = false
                if (v.retailerId == null || v.retailerId == 0L || !retailersById.containsKey(v.retailerId)) {
                    val matchedRetailer = retailersByName[v.partyName.trim().lowercase()]
                    if (matchedRetailer != null) {
                        updatedVoucher = updatedVoucher.copy(retailerId = matchedRetailer.id)
                        changed = true
                    }
                }
                // إذا كان السند يحتوي على رقم فاتورة بدون invoiceId نربطه صراحة
                if (updatedVoucher.invoiceId == null && updatedVoucher.invoiceNumber.isNotBlank()) {
                    val matchedInv = invoicesByNumber[updatedVoucher.invoiceNumber]
                    if (matchedInv != null) {
                        updatedVoucher = updatedVoucher.copy(invoiceId = matchedInv.id)
                        changed = true
                    }
                }
                if (changed) {
                    db.financialVoucherDao().updateVoucher(updatedVoucher)
                }
            }

            // إعادة تحميل البيانات بعد اكتمال الربط بالمعرفات
            val allInvoices = db.cardSalesInvoiceDao().getSalesInvoicesList()
            val allVouchers = db.financialVoucherDao().getVouchersList()

            // 2. التسوية الدفترية الدقيقة لكل بقالة بناءً على فواتير المبيعات الحقيقية وسندات القبض
            for (retailer in retailers) {
                val matchingInvoices = allInvoices.filter {
                    it.retailerId == retailer.id ||
                    it.customerName.trim().equals(retailer.name.trim(), ignoreCase = true)
                }.sortedBy { it.invoiceDateMillis }

                val retailerReceipts = allVouchers.filter {
                    (it.retailerId == retailer.id || it.partyName.trim().equals(retailer.name.trim(), ignoreCase = true)) &&
                    it.voucherType == "RECEIPT" && !it.isVoided
                }

                // تقسيم السندات إلى:
                // أ) سندات مرتبطة مباشرة بفاتورة معينة (عبر invoiceId أو invoiceNumber)
                // ب) سندات قبض غير مخصصة (سداد عام للمديونية بنظام FIFO)
                val invoiceBoundReceipts = retailerReceipts.filter {
                    it.invoiceId != null || (it.invoiceNumber.isNotBlank() && matchingInvoices.any { inv -> inv.invoiceNumber == it.invoiceNumber })
                }
                val paymentsByInvoiceId = invoiceBoundReceipts.filter { it.invoiceId != null }.groupBy { it.invoiceId!! }
                val paymentsByInvoiceNumber = invoiceBoundReceipts.filter { it.invoiceNumber.isNotBlank() }.groupBy { it.invoiceNumber }

                val generalReceipts = retailerReceipts.filter { it !in invoiceBoundReceipts }
                var unallocatedPool = generalReceipts.fold(BigDecimal.ZERO) { acc, v -> acc.add(v.amount) }

                var totalDebt = BigDecimal.ZERO
                var totalUnpaidCards = 0
                var totalPaid = BigDecimal.ZERO

                if (matchingInvoices.isNotEmpty()) {
                    for (inv in matchingInvoices) {
                        val directPaidId = paymentsByInvoiceId[inv.id]?.fold(BigDecimal.ZERO) { acc, v -> acc.add(v.amount) } ?: BigDecimal.ZERO
                        val directPaidNum = paymentsByInvoiceNumber[inv.invoiceNumber]?.filter { it.invoiceId == null }?.fold(BigDecimal.ZERO) { acc, v -> acc.add(v.amount) } ?: BigDecimal.ZERO
                        val directPaid = directPaidId.add(directPaidNum)

                        var normType = when {
                            inv.paymentType.equals("CREDIT", ignoreCase = true) || inv.paymentType.contains("آجل") || inv.paymentType.contains("UNPAID", ignoreCase = true) -> "CREDIT"
                            inv.paymentType.equals("CASH", ignoreCase = true) || inv.paymentType.contains("نقد") -> "CASH"
                            inv.paymentType.equals("PARTIAL", ignoreCase = true) || inv.paymentType.contains("مقدم") -> "PARTIAL"
                            else -> if (inv.remainingAmount > BigDecimal.ZERO && inv.paidAmount <= BigDecimal.ZERO) "CREDIT" else inv.paymentType
                        }

                        // في حال كانت الفاتورة مسددة نقداً أو جزئياً عند إنشائها ولم يكن هناك سند
                        val paidAtCreation = if (directPaid > BigDecimal.ZERO) {
                            directPaid
                        } else {
                            when (normType) {
                                "CASH" -> inv.totalAmount
                                "CREDIT" -> BigDecimal.ZERO
                                "PARTIAL" -> (if (inv.remainingAmount > BigDecimal.ZERO && inv.paidAmount <= BigDecimal.ZERO) BigDecimal.ZERO else inv.paidAmount).max(BigDecimal.ZERO).min(inv.totalAmount)
                                else -> if (inv.remainingAmount > BigDecimal.ZERO && inv.paidAmount <= BigDecimal.ZERO) BigDecimal.ZERO else inv.paidAmount.max(BigDecimal.ZERO).min(inv.totalAmount)
                            }
                        }

                        val basePaid = directPaid.max(paidAtCreation)
                        val unpaidAmount = inv.totalAmount.subtract(basePaid).max(BigDecimal.ZERO)

                        // تخصيص المبالغ من صندوق سندات القبض العامة بنظام FIFO
                        val extraPayment = unallocatedPool.min(unpaidAmount)
                        unallocatedPool = unallocatedPool.subtract(extraPayment).max(BigDecimal.ZERO)

                        val finalPaid = basePaid.add(extraPayment).min(inv.totalAmount)
                        val finalRemaining = inv.totalAmount.subtract(finalPaid).max(BigDecimal.ZERO)
                        val finalStatus = when {
                            finalRemaining <= BigDecimal("0.01") -> "PAID"
                            finalPaid > BigDecimal("0.01") -> "PARTIAL"
                            else -> "CREDIT"
                        }

                        if (inv.paidAmount.subtract(finalPaid).abs() > BigDecimal("0.01") ||
                            inv.remainingAmount.subtract(finalRemaining).abs() > BigDecimal("0.01") ||
                            inv.status != finalStatus ||
                            inv.paymentType != normType) {
                            db.cardSalesInvoiceDao().updateInvoice(
                                inv.copy(
                                    paymentType = normType,
                                    paidAmount = finalPaid,
                                    remainingAmount = finalRemaining,
                                    status = finalStatus
                                )
                            )
                        }

                        totalDebt = totalDebt.add(finalRemaining)
                        if (finalRemaining > BigDecimal("0.01")) {
                            totalUnpaidCards += inv.totalCardsCount
                        }
                        totalPaid = totalPaid.add(finalPaid)
                    }

                    // في حال تبقت مبالغ من سندات القبض العامة تفوق مديونية كافة الفواتير
                    totalDebt = totalDebt.subtract(unallocatedPool).max(BigDecimal.ZERO)
                    totalPaid = totalPaid.add(unallocatedPool)
                }

                // Sync customer_ledger records for retailer
                db.customerLedgerDao().deleteByCustomerId(retailer.id)

                for (inv in matchingInvoices) {
                    db.customerLedgerDao().insertLedgerEntry(
                        com.example.data.local.entity.CustomerLedgerEntity(
                            customerId = retailer.id,
                            transactionDate = inv.invoiceDateMillis,
                            transactionType = "INVOICE",
                            referenceId = inv.id.toString(),
                            debit = inv.totalAmount,
                            credit = BigDecimal.ZERO,
                            description = "فاتورة مبيعات #${inv.invoiceNumber}"
                        )
                    )
                }

                // Insert ALL non-voided receipt/payment vouchers for this retailer
                for (v in retailerReceipts) {
                    db.customerLedgerDao().insertLedgerEntry(
                        com.example.data.local.entity.CustomerLedgerEntity(
                            customerId = retailer.id,
                            transactionDate = v.dateMillis,
                            transactionType = if (v.voucherType == "RECEIPT") "PAYMENT" else "INVOICE",
                            referenceId = v.id.toString(),
                            debit = if (v.voucherType == "PAYMENT") v.amount else BigDecimal.ZERO,
                            credit = if (v.voucherType == "RECEIPT") v.amount else BigDecimal.ZERO,
                            description = "سند ${if (v.voucherType == "RECEIPT") "قبض" else "صرف"} #${v.voucherNumber}"
                        )
                    )
                }

                // Query EXCLUSIVELY via CustomerLedgerDao.getCustomerAccountSummary
                val accountSummary = db.customerLedgerDao().getCustomerAccountSummary(retailer.id).first()

                val physicalCards = db.cardDao().getDistributedCardsCountForRetailer(retailer.id)
                val finalActiveCards = maxOf(totalUnpaidCards, physicalCards)

                db.retailerDao().updateRetailer(
                    retailer.copy(
                        balanceOwed = accountSummary.finalBalance,
                        activeCardsCount = finalActiveCards,
                        totalPaid = accountSummary.totalPaid
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("NetworkRepository", "reconcileAccountingLedger error: ${e.message}", e)
        }
    }

    /**
     * محرك التسوية المحاسبية والربط الفوري والتزامن الشامل بين كافة التبويبات.
     * يعمل كمعاملة ذرية مع ضمان تكامل البيانات 100%.
     */
    suspend fun reconcileAccountingLedger() = withContext(Dispatchers.IO) {
        db.withTransaction {
            reconcileAccountingLedgerInternal()
        }
    }

    suspend fun deleteSalesInvoice(invoice: CardSalesInvoiceEntity) = withContext(Dispatchers.IO) {
        db.withTransaction {
            try {
                // 1. استرجاع الكميات للمخزن مع تسجيل حركة الإلغاء
                returnInvoiceToInventory(invoice, "استرجاع كميات بسبب حذف/إلغاء الفاتورة")

                // 2. إلغاء سندات القبض المرتبطة بالفاتورة بقيد عكسي تسوية يحفظ مسار التدقيق دون إنشاء سند صرف إضافي
                val linkedVouchers = if (invoice.id > 0L) {
                    db.financialVoucherDao().getVouchersByInvoiceId(invoice.id)
                } else {
                    db.financialVoucherDao().getVouchersByInvoiceNumber(invoice.invoiceNumber)
                }
                for (voucher in linkedVouchers) {
                    if (!voucher.isVoided) {
                        db.financialVoucherDao().updateVoucher(
                            voucher.copy(
                                isVoided = true,
                                notes = (voucher.notes + " (ملغى بسبب حذف الفاتورة #${invoice.invoiceNumber})").trim()
                            )
                        )
                        val vHeader = db.journalEntryDao().getActiveHeaderByReferenceWithAlt(
                            "FINANCIAL_VOUCHER",
                            voucher.voucherNumber,
                            voucher.id.toString()
                        )
                        if (vHeader != null) {
                            db.journalEntryDao().voidEntryWithReversal(
                                vHeader.id,
                                "إلغاء سند قبض بسبب حذف الفاتورة #${invoice.invoiceNumber}",
                                invoice.issuerName.ifBlank { "النظام المحاسبي" }
                            )
                        }
                    }
                }

                // 3. إلغاء قيود اليومية المعيارية (المبيعات والتكلفة) عبر التسوية العكسية
                try {
                    val header = db.journalEntryDao().getHeaderByReference("SALES_INVOICE", invoice.invoiceNumber)
                        ?: db.journalEntryDao().getHeaderByReference("SALES_INVOICE", invoice.id.toString())
                    if (header != null) {
                        db.journalEntryDao().voidEntryWithReversal(header.id, "إلغاء الفاتورة وتسوية مسار التدقيق", invoice.issuerName.ifBlank { "المهندس حسن" })
                    }
                    val cogsHeader = db.journalEntryDao().getHeaderByReference("COGS", invoice.invoiceNumber)
                    if (cogsHeader != null) {
                        db.journalEntryDao().voidEntryWithReversal(cogsHeader.id, "إلغاء تكلفة بضاعة الفاتورة", invoice.issuerName.ifBlank { "المهندس حسن" })
                    }
                } catch (e: Exception) {
                    android.util.Log.e("NetworkRepository", "Error voiding header for sales invoice: ${e.message}", e)
                }

                val voidedInvoice = invoice.copy(
                    isVoided = true,
                    status = "VOIDED",
                    voidReason = "إلغاء الفاتورة وتسوية مسار التدقيق"
                )
                db.cardSalesInvoiceDao().updateInvoice(voidedInvoice)
                db.customerLedgerDao().deleteByReferenceId(invoice.id.toString())
                db.customerLedgerDao().deleteByReferenceId("INV_PAY_${invoice.id}")
                if (invoice.invoiceNumber.isNotBlank()) {
                    db.customerLedgerDao().deleteByReferenceId(invoice.invoiceNumber)
                }

                // 5. تسوية الحسابات المحاسبية فورا
                reconcileAccountingLedgerInternal()
            } catch (e: Exception) {
                android.util.Log.e("NetworkRepository", "deleteSalesInvoice error: ${e.message}", e)
                val voidedInvoice = invoice.copy(isVoided = true, status = "VOIDED", voidReason = "إلغاء الفاتورة")
                db.cardSalesInvoiceDao().updateInvoice(voidedInvoice)
                db.customerLedgerDao().deleteByReferenceId(invoice.id.toString())
                db.customerLedgerDao().deleteByReferenceId("INV_PAY_${invoice.id}")
                reconcileAccountingLedgerInternal()
            }
        }
    }

    suspend fun distributeFromInventory(
        inventoryId: Long,
        retailerId: Long,
        quantity: Int
    ): Boolean = withContext(Dispatchers.IO) {
        db.withTransaction {
            val retailer = db.retailerDao().getRetailerById(retailerId) ?: return@withTransaction false
            val item = db.inventoryDao().getItemById(inventoryId) ?: return@withTransaction false
            if (item.quantityAvailable < quantity) return@withTransaction false

            // Deduct from inventory
            val updatedItem = item.copy(quantityAvailable = item.quantityAvailable - quantity)
            db.inventoryDao().updateItem(updatedItem)

            val totalDebt = item.wholesalePrice.multiply(BigDecimal.valueOf(quantity.toLong()))
            val cal = Calendar.getInstance()
            val docYear = cal.get(Calendar.YEAR)
            val invoiceNumber = db.numberSequenceDao().getNextNumber("INV", docYear, "INV")

            // قيد حركة مخزنية خارجة (خصم تسليم كروت)
            db.inventoryMovementDao().insertMovement(
                InventoryMovementEntity(
                    packageName = item.packageName,
                    movementType = "SALE",
                    quantityChange = -quantity,
                    resultingBalance = updatedItem.quantityAvailable,
                    referenceNumber = invoiceNumber,
                    customerOrSupplier = retailer.name,
                    unitPrice = item.wholesalePrice,
                    notes = "تسليم كروت للبقالة $invoiceNumber"
                )
            )

            // إنشاء فاتورة مبيعات كروت رسمية بالآجل
            val jsonArray = org.json.JSONArray().apply {
                put(org.json.JSONObject().apply {
                    put("id", java.util.UUID.randomUUID().toString())
                    put("packageName", item.packageName)
                    put("quantity", quantity)
                    put("unitPrice", item.wholesalePrice.toDouble())
                    put("retailPrice", item.retailPrice.toDouble())
                    put("lineTotal", totalDebt.toDouble())
                })
            }

            val invoiceEntity = CardSalesInvoiceEntity(
                invoiceNumber = invoiceNumber,
                customerName = retailer.name,
                customerPhone = retailer.phone,
                retailerId = retailerId,
                paymentType = "CREDIT",
                totalAmount = totalDebt,
                paidAmount = BigDecimal.ZERO,
                remainingAmount = totalDebt,
                totalCardsCount = quantity,
                itemsCount = 1,
                itemsSummary = "$quantity كرت [${item.packageName}]",
                itemsJson = jsonArray.toString(),
                notes = "تسليم وتوزيع كروت بالآجل من المخزن",
                issuerName = "مسؤول التوزيع",
                status = "CREDIT"
            )
            val invoiceId = db.cardSalesInvoiceDao().insertInvoice(invoiceEntity)

            // ترحيل قيد المبيعات عبر المحرك المركزي الموحد
            try {
                postingService.postSalesInvoice(
                    invoice = invoiceEntity.copy(id = invoiceId),
                    performer = "مسؤول التوزيع"
                )
            } catch (e: Exception) {
                android.util.Log.e("NetworkRepository", "Error posting sales invoice for inventory distribution: ${e.message}", e)
            }

            // إعادة التسوية الشاملة ذرياً
            reconcileAccountingLedgerInternal()

            true
        }
    }

    // Distribute Cards to Retailer (تسليم دفعة كروت للبقالة)
    suspend fun distributeCardsToRetailer(
        batchId: Long,
        retailerId: Long,
        quantity: Int
    ): Boolean = withContext(Dispatchers.IO) {
        db.withTransaction {
            val retailer = db.retailerDao().getRetailerById(retailerId) ?: return@withTransaction false
            val availableCards = db.cardDao().getAvailableCardsForBatch(batchId, quantity)
            if (availableCards.isEmpty()) return@withTransaction false

            val countToDistribute = availableCards.size
            val cardIds = availableCards.map { it.id }
            val now = System.currentTimeMillis()

            db.cardDao().assignCardsToRetailer(cardIds, retailerId, retailer.name, now)

            val totalWholesaleDebt = availableCards.sumOf { it.wholesalePrice }
            val totalWholesaleDebtBd = BigDecimal.valueOf(totalWholesaleDebt)
            val categoryName = availableCards.firstOrNull()?.categoryName ?: "كروت شبكة"
            val cal = Calendar.getInstance().apply { timeInMillis = now }
            val docYear = cal.get(Calendar.YEAR)
            val invoiceNumber = db.numberSequenceDao().getNextNumber("INV", docYear, "INV")

            val jsonArray = org.json.JSONArray().apply {
                put(org.json.JSONObject().apply {
                    put("id", java.util.UUID.randomUUID().toString())
                    put("packageName", categoryName)
                    put("quantity", countToDistribute)
                    put("unitPrice", if (countToDistribute > 0) totalWholesaleDebt / countToDistribute else 0.0)
                    put("retailPrice", availableCards.firstOrNull()?.retailPrice ?: 0.0)
                    put("lineTotal", totalWholesaleDebt)
                })
            }

            val invoiceEntity = CardSalesInvoiceEntity(
                invoiceNumber = invoiceNumber,
                customerName = retailer.name,
                customerPhone = retailer.phone,
                retailerId = retailerId,
                paymentType = "CREDIT",
                totalAmount = totalWholesaleDebtBd,
                paidAmount = BigDecimal.ZERO,
                remainingAmount = totalWholesaleDebtBd,
                totalCardsCount = countToDistribute,
                itemsCount = 1,
                itemsSummary = "$countToDistribute كرت [$categoryName]",
                itemsJson = jsonArray.toString(),
                notes = "تسليم دفعة كروت بالآجل للبقالة",
                issuerName = "مسؤول التوزيع",
                status = "CREDIT",
                invoiceDateMillis = now
            )
            val invoiceId = db.cardSalesInvoiceDao().insertInvoice(invoiceEntity)
            deductInvoiceFromInventory(invoiceEntity.copy(id = invoiceId), "تسليم دفعة كروت بالآجل للبقالة")

            // ترحيل قيد إثبات المبيعات الآجلة عبر المحرك المركزي الموحد
            try {
                postingService.postSalesInvoice(
                    invoice = invoiceEntity.copy(id = invoiceId),
                    performer = "مسؤول التوزيع"
                )
            } catch (e: Exception) {
                android.util.Log.e("NetworkRepository", "Error posting sales invoice for batch distribution: ${e.message}", e)
            }

            reconcileAccountingLedgerInternal()

            true
        }
    }

    suspend fun markCardSold(cardId: Long) = withContext(Dispatchers.IO) {
        db.cardDao().markCardSold(cardId, System.currentTimeMillis())
    }

    // Packages (باقات وفئات الكروت والبروفايلات)
    val allPackages: Flow<List<CardPackageEntity>> = db.cardPackageDao().getAllPackages()

    suspend fun savePackage(pkg: CardPackageEntity): Long = withContext(Dispatchers.IO) {
        if (pkg.id == 0L) {
            db.cardPackageDao().insertPackage(pkg)
        } else {
            db.cardPackageDao().updatePackage(pkg)
            pkg.id
        }
    }

    suspend fun deletePackage(pkg: CardPackageEntity) = withContext(Dispatchers.IO) {
        db.cardPackageDao().deletePackage(pkg)
    }

    /**
     * إصدار فاتورة وسند بيع كروت مباشرة للبقالة أو نقطة البيع
     * وحساب سعر الجملة وإجمالي القيمة وربح البقالة وتحديث الرصيد وسندات القبض
     */
    suspend fun issueCardSalesInvoice(
        packageEntity: CardPackageEntity,
        retailerId: Long,
        quantity: Int,
        paymentMethod: String = "نقداً",
        issuerName: String = "المهندس حسن",
        notes: String = ""
    ): Long = withContext(Dispatchers.IO) {
        db.withTransaction {
            val retailer = db.retailerDao().getRetailerById(retailerId)
            val retailerName = retailer?.name ?: "نقطة توزيع"
            val totalWholesale = packageEntity.wholesalePrice.multiply(BigDecimal.valueOf(quantity.toLong()))
            val totalRetail = packageEntity.retailPrice.multiply(BigDecimal.valueOf(quantity.toLong()))
            val retailerProfit = (packageEntity.retailPrice.subtract(packageEntity.wholesalePrice)).multiply(BigDecimal.valueOf(quantity.toLong()))

            val cal = Calendar.getInstance()
            val docYear = cal.get(Calendar.YEAR)
            val invoiceNumber = db.numberSequenceDao().getNextNumber("INV", docYear, "INV")
            val isCash = paymentMethod.contains("نقد")

            val singleItemJson = org.json.JSONArray().apply {
                put(org.json.JSONObject().apply {
                    put("id", java.util.UUID.randomUUID().toString())
                    put("packageName", packageEntity.name)
                    put("quantity", quantity)
                    put("unitPrice", packageEntity.wholesalePrice.toDouble())
                    put("retailPrice", packageEntity.retailPrice.toDouble())
                    put("lineTotal", totalWholesale.toDouble())
                })
            }.toString()

            val invoiceEntity = CardSalesInvoiceEntity(
                invoiceNumber = invoiceNumber,
                customerName = retailerName,
                customerPhone = retailer?.phone ?: "",
                retailerId = retailerId,
                paymentType = if (isCash) "CASH" else "CREDIT",
                totalAmount = totalWholesale,
                paidAmount = if (isCash) totalWholesale else BigDecimal.ZERO,
                remainingAmount = if (isCash) BigDecimal.ZERO else totalWholesale,
                totalCardsCount = quantity,
                itemsCount = 1,
                itemsSummary = "$quantity كرت [${packageEntity.name}]",
                itemsJson = singleItemJson,
                notes = notes.ifBlank { "فاتورة مبيعات باقة ${packageEntity.name}" },
                issuerName = issuerName,
                status = if (isCash) "PAID" else "CREDIT"
            )
            val invoiceId = db.cardSalesInvoiceDao().insertInvoice(invoiceEntity)
            deductInvoiceFromInventory(invoiceEntity.copy(id = invoiceId), "صرف مبيعات مباشرة للبقالة")

            // ترحيل قيد فاتورة المبيعات عبر المحرك المركزي الموحد
            try {
                postingService.postSalesInvoice(
                    invoice = invoiceEntity.copy(id = invoiceId),
                    performer = issuerName
                )
            } catch (e: Exception) {
                android.util.Log.e("NetworkRepository", "Error posting sales invoice for card sale: ${e.message}", e)
            }

            val descriptionText = buildString {
                append("فاتورة بيع $quantity كرت [${packageEntity.name}]")
                append(" بسعر جملة ${packageEntity.wholesalePrice.toInt()} ر.ي (إجمالي الجملة: ${totalWholesale.toInt()} ر.ي)")
                append(" - سعر البيع للمستهلك: ${packageEntity.retailPrice.toInt()} ر.ي (إجمالي: ${totalRetail.toInt()} ر.ي)")
                append(" - ربح البقالة المقدر: ${retailerProfit.toInt()} ر.ي")
                if (notes.isNotBlank()) append(" | ملاحظة: $notes")
            }

            var voucherId = 0L
            if (isCash) {
                val voucherNum = db.numberSequenceDao().getNextNumber("REC", docYear, "REC")
                val voucher = FinancialVoucherEntity(
                    voucherNumber = voucherNum,
                    voucherType = "RECEIPT",
                    amount = totalWholesale,
                    partyName = retailerName,
                    retailerId = retailerId,
                    invoiceId = invoiceId,
                    invoiceNumber = invoiceNumber,
                    invoiceKind = "SALES",
                    allocatedAmount = totalWholesale,
                    category = "مبيعات كروت",
                    paymentMethod = paymentMethod,
                    description = descriptionText,
                    issuerName = issuerName,
                    notes = notes
                )
                voucherId = db.financialVoucherDao().insertVoucher(voucher)
                try {
                    postingService.postFinancialVoucher(voucher, issuerName)
                } catch (e: Exception) {
                    android.util.Log.e("NetworkRepository", "Error posting receipt voucher in card sale: ${e.message}", e)
                }
            }

            reconcileAccountingLedgerInternal()

            if (voucherId > 0) voucherId else invoiceId
        }
    }

    // Retailers
    val allRetailers: Flow<List<RetailerEntity>> = db.retailerDao().getAllRetailers()

    // General Ledger Account 1201 Customer Balances - Unified GL Source of Truth
    val customerGlBalances: Flow<Map<Long, BigDecimal>> = db.journalEntryDao()
        .getAllPartyBalancesFlow("1201")
        .map { list -> list.associate { it.partyId to it.balance } }

    suspend fun getPartyBalance(accountCode: String = "1201", partyId: Long): BigDecimal = withContext(Dispatchers.IO) {
        db.journalEntryDao().getPartyBalance(accountCode, partyId)
    }

    fun getPartyBalanceFlow(accountCode: String = "1201", partyId: Long): Flow<BigDecimal?> =
        db.journalEntryDao().getPartyBalanceFlow(accountCode, partyId)

    // Customer Ledger - Single Source of Truth
    fun getCustomerAccountSummary(customerId: Long): Flow<com.example.data.local.dao.CustomerAccountSummary> =
        db.customerLedgerDao().getCustomerAccountSummary(customerId)

    fun getCustomerLedger(customerId: Long): Flow<List<com.example.data.local.entity.CustomerLedgerEntity>> =
        db.customerLedgerDao().getLedgerForCustomer(customerId)

    suspend fun saveRetailer(retailer: RetailerEntity): Long = withContext(Dispatchers.IO) {
        if (retailer.id == 0L) {
            db.retailerDao().insertRetailer(retailer)
        } else {
            db.retailerDao().updateRetailer(retailer)
            retailer.id
        }
    }

    suspend fun deleteRetailer(retailer: RetailerEntity) = withContext(Dispatchers.IO) {
        db.retailerDao().deleteRetailer(retailer)
    }

    // Financial Vouchers (سندات القبض والصرف)
    val allVouchers: Flow<List<FinancialVoucherEntity>> = db.financialVoucherDao().getAllVouchers()
    val totalReceipts: Flow<Double?> = db.financialVoucherDao().getTotalReceipts()
    val totalPayments: Flow<Double?> = db.financialVoucherDao().getTotalPayments()

    fun getVouchersForRetailer(retailerId: Long): Flow<List<FinancialVoucherEntity>> =
        db.financialVoucherDao().getVouchersForRetailer(retailerId)

    suspend fun createVoucher(
        voucherType: String, // "RECEIPT" or "PAYMENT"
        amount: BigDecimal,
        currency: String = "YER",
        partyName: String,
        retailerId: Long?,
        category: String,
        paymentMethod: String,
        description: String,
        issuerName: String,
        invoiceId: Long? = null,
        invoiceNumber: String = ""
    ): Long = withContext(Dispatchers.IO) {
        db.withTransaction {
            val prefix = if (voucherType == "RECEIPT") "REC" else "PAY"
            val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            val voucherNumberGenerated = db.numberSequenceDao().getNextNumber(prefix, currentYear, prefix)

            val voucher = FinancialVoucherEntity(
                voucherNumber = voucherNumberGenerated,
                voucherType = voucherType,
                amount = amount,
                currency = currency,
                partyName = partyName,
                retailerId = retailerId,
                invoiceId = invoiceId,
                invoiceNumber = invoiceNumber,
                allocatedAmount = if (invoiceId != null || invoiceNumber.isNotBlank()) amount else BigDecimal.ZERO,
                category = category,
                paymentMethod = paymentMethod,
                description = description,
                issuerName = issuerName
            )

            val id = db.financialVoucherDao().insertVoucher(voucher)

            // ترحيل السند المالي بشكل موحد ودقيق إلى قيود اليومية الموزونة
            try {
                postingService.postFinancialVoucher(voucher, issuerName)
            } catch (e: Exception) {
                android.util.Log.e("NetworkRepository", "Error posting balanced entry for voucher: ${e.message}", e)
            }

            reconcileAccountingLedgerInternal()

            id
        }
    }

    suspend fun createVoucher(
        voucherType: String,
        amount: Double,
        currency: String = "YER",
        partyName: String,
        retailerId: Long?,
        category: String,
        paymentMethod: String,
        description: String,
        issuerName: String,
        invoiceId: Long? = null,
        invoiceNumber: String = ""
    ): Long = createVoucher(
        voucherType = voucherType,
        amount = BigDecimal.valueOf(amount),
        currency = currency,
        partyName = partyName,
        retailerId = retailerId,
        category = category,
        paymentMethod = paymentMethod,
        description = description,
        issuerName = issuerName,
        invoiceId = invoiceId,
        invoiceNumber = invoiceNumber
    )

    suspend fun deleteVoucher(voucher: FinancialVoucherEntity) = withContext(Dispatchers.IO) {
        // الإلغاء الآمن نظامياً بقيد عكسي تسوية يحفظ مسار التدقيق الرقابي المحاسبي
        voidVoucher(voucher, "إلغاء وحذف السند المالي")
    }

    suspend fun deduplicateVouchers(): Int = withContext(Dispatchers.IO) {
        db.withTransaction {
            val all = db.financialVoucherDao().getVouchersList()
            val seenNumbers = mutableMapOf<String, FinancialVoucherEntity>()
            val toDelete = mutableListOf<FinancialVoucherEntity>()

            for (v in all) {
                val numKey = v.voucherNumber.trim().uppercase()
                if (numKey.isNotBlank()) {
                    if (seenNumbers.containsKey(numKey)) {
                        val existing = seenNumbers[numKey]!!
                        if (existing.isVoided && !v.isVoided) {
                            toDelete.add(existing)
                            seenNumbers[numKey] = v
                        } else {
                            toDelete.add(v)
                        }
                    } else {
                        seenNumbers[numKey] = v
                    }
                }
            }

            for (dup in toDelete) {
                db.financialVoucherDao().deleteVoucher(dup)
                db.financialVoucherDao().deleteVoucherById(dup.id)
                db.customerLedgerDao().deleteByReferenceId(dup.id.toString())
                if (dup.voucherNumber.isNotBlank()) {
                    db.customerLedgerDao().deleteByReferenceId(dup.voucherNumber)
                    val activeRemaining = all.filter { it.id != dup.id && it.voucherNumber.trim().equals(dup.voucherNumber.trim(), ignoreCase = true) && !it.isVoided }
                    if (activeRemaining.isEmpty()) {
                        db.journalEntryDao().purgeJournalEntriesByReference("FINANCIAL_VOUCHER", dup.voucherNumber)
                        com.example.data.local.DeletedRecordsTracker.markVoucherDeleted(dup.voucherNumber)
                    } else {
                        // هناك سند أصلي نشط متبقي: حذف أي قيد عكسي خاطئ ناتج عن إلغاء النسخة المكررة سابقاً
                        db.journalEntryDao().deleteLinesByReference("VOID_VOUCHER", dup.voucherNumber)
                        db.journalEntryDao().deleteHeadersByReference("VOID_VOUCHER", dup.voucherNumber)
                        db.journalEntryDao().deleteLegacyEntriesByReference("VOID_VOUCHER", dup.voucherNumber)
                        db.journalEntryDao().deleteLinesByReference("VOID_VOUCHER", "REV-${dup.voucherNumber}")
                        db.journalEntryDao().deleteHeadersByReference("VOID_VOUCHER", "REV-${dup.voucherNumber}")
                        db.journalEntryDao().deleteLegacyEntriesByReference("VOID_VOUCHER", "REV-${dup.voucherNumber}")
                        db.journalEntryDao().deleteLinesByReference("VOID_REVERSAL", dup.voucherNumber)
                        db.journalEntryDao().deleteHeadersByReference("VOID_REVERSAL", dup.voucherNumber)
                        db.journalEntryDao().deleteReversalLinesByRef(dup.voucherNumber)
                        db.journalEntryDao().deleteReversalHeadersByRef(dup.voucherNumber)
                    }
                }
            }

            if (toDelete.isNotEmpty()) {
                android.util.Log.i("NetworkRepository", "Deduplicated ${toDelete.size} duplicate vouchers")
                reconcileAccountingLedgerInternal()
            }
            toDelete.size
        }
    }

    suspend fun updateVoucher(voucher: FinancialVoucherEntity) = withContext(Dispatchers.IO) {
        db.withTransaction {
            db.financialVoucherDao().updateVoucher(voucher)
            try {
                postingService.updateFinancialVoucherPosting(voucher)
            } catch (e: Exception) {
                android.util.Log.e("NetworkRepository", "Error updating voucher posting: ${e.message}", e)
            }
            reconcileAccountingLedgerInternal()
        }
    }

    suspend fun voidVoucher(voucher: FinancialVoucherEntity, reason: String = "إلغاء السند المالي") = withContext(Dispatchers.IO) {
        db.withTransaction {
            // 1. تحديث حالة السند لتصبح ملغاة
            val voidedVoucher = voucher.copy(isVoided = true)
            db.financialVoucherDao().updateVoucher(voidedVoucher)
            db.customerLedgerDao().deleteByReferenceId(voucher.id.toString())
            if (voucher.voucherNumber.isNotBlank()) {
                db.customerLedgerDao().deleteByReferenceId(voucher.voucherNumber)
            }

            // 2. إنشاء قيد يومية عكسي تلقائي (Reversal Journal Entry) لإلغاء الأثر المالي للسند
            try {
                val header = db.journalEntryDao().getHeaderByReference("FINANCIAL_VOUCHER", voucher.voucherNumber)
                if (header != null) {
                    db.journalEntryDao().voidEntryWithReversal(header.id, reason, voucher.issuerName)
                }
            } catch (e: Exception) {
                android.util.Log.e("NetworkRepository", "Error voiding header for voucher: ${e.message}", e)
            }

            val (origDebit, origCredit) = if (voucher.voucherType == "RECEIPT") {
                val credit = if (voucher.retailerId != null) "1201 - ذمم الوكلاء / ${voucher.partyName}" else "4101 - إيرادات عامة"
                "1101 - الصندوق الرئيسي" to credit
            } else {
                "5201 - مصروفات / ${voucher.category}" to "1101 - الصندوق الرئيسي"
            }

            db.journalEntryDao().insertEntry(
                JournalEntryEntity(
                    entryNumber = "REV-${voucher.voucherNumber}",
                    referenceType = "VOID_VOUCHER",
                    referenceId = voucher.voucherNumber,
                    debitAccount = origCredit,   // Reversal: Swap Debit and Credit
                    creditAccount = origDebit,
                    amount = voucher.amount,
                    currency = voucher.currency,
                    exchangeRate = BigDecimal.ONE,
                    description = "قيد عكسي لإلغاء السند رقم ${voucher.voucherNumber}${if (reason.isNotBlank()) " - $reason" else ""}",
                    createdBy = voucher.issuerName
                )
            )

            // 3. إعادة ضبط رصيد الحساب المربوط فوراً
            reconcileAccountingLedgerInternal()
        }
    }

    suspend fun createVoucher(
        voucherType: String,
        amount: Double,
        partyName: String,
        retailerId: Long?,
        category: String,
        paymentMethod: String,
        description: String,
        issuerName: String,
        invoiceId: Long? = null,
        invoiceNumber: String = ""
    ): Long = createVoucher(
        voucherType = voucherType,
        amount = BigDecimal.valueOf(amount),
        partyName = partyName,
        retailerId = retailerId,
        category = category,
        paymentMethod = paymentMethod,
        description = description,
        issuerName = issuerName,
        invoiceId = invoiceId,
        invoiceNumber = invoiceNumber
    )

    suspend fun insertVoucher(voucher: FinancialVoucherEntity): Long = withContext(Dispatchers.IO) {
        db.withTransaction {
            val id = db.financialVoucherDao().insertVoucher(voucher)

            if (voucher.retailerId != null && voucher.retailerId > 0 && !voucher.isVoided) {
                db.customerLedgerDao().insertLedgerEntry(
                    com.example.data.local.entity.CustomerLedgerEntity(
                        customerId = voucher.retailerId,
                        transactionDate = voucher.dateMillis,
                        transactionType = if (voucher.voucherType == "RECEIPT") "PAYMENT" else "INVOICE",
                        referenceId = id.toString(),
                        debit = if (voucher.voucherType == "PAYMENT") voucher.amount else BigDecimal.ZERO,
                        credit = if (voucher.voucherType == "RECEIPT") voucher.amount else BigDecimal.ZERO,
                        description = "سند ${if (voucher.voucherType == "RECEIPT") "قبض" else "صرف"} #${voucher.voucherNumber} - ${voucher.partyName}"
                    )
                )
            }

            // إنشاء وحفظ قيد محاسبي مزدوج تلقائي إذا لم يكن السند مرتبطاً بفاتورة مسبقاً
            if (voucher.invoiceId == null && voucher.invoiceNumber.isBlank()) {
                val (debitAcc, creditAcc) = if (voucher.voucherType == "RECEIPT") {
                    val credit = if (voucher.retailerId != null) "1201 - ذمم الوكلاء / ${voucher.partyName}" else "4101 - إيرادات عامة"
                    "1101 - الصندوق الرئيسي" to credit
                } else {
                    "5201 - مصروفات / ${voucher.category}" to "1101 - الصندوق الرئيسي"
                }

                db.journalEntryDao().insertEntry(
                    JournalEntryEntity(
                        entryNumber = "JE-${voucher.voucherNumber}",
                        referenceType = "FINANCIAL_VOUCHER",
                        referenceId = voucher.voucherNumber,
                        debitAccount = debitAcc,
                        creditAccount = creditAcc,
                        amount = voucher.amount,
                        currency = voucher.currency,
                        exchangeRate = BigDecimal.ONE,
                        description = voucher.description,
                        createdBy = voucher.issuerName
                    )
                )
            }

            id
        }
    }

    suspend fun saveJournalEntry(entry: JournalEntryEntity): Long = withContext(Dispatchers.IO) {
        db.journalEntryDao().insertEntry(entry)
    }

    // Users & Roles
    val allUsers: Flow<List<UserEntity>> = db.userDao().getAllUsers()
    val allDistributors: Flow<List<UserEntity>> = db.userDao().getUsersByRole("DISTRIBUTOR")

    suspend fun saveUser(user: UserEntity): Long = withContext(Dispatchers.IO) {
        if (user.id == 0L) {
            db.userDao().insertUser(user)
        } else {
            db.userDao().updateUser(user)
            user.id
        }
    }

    suspend fun deleteUser(user: UserEntity) = withContext(Dispatchers.IO) {
        db.userDao().deleteUser(user)
    }

    companion object {
        fun isSuperAdminEmail(email: String?): Boolean {
            return !email.isNullOrBlank()
        }
    }

    suspend fun purgeAllOperationalData() = withContext(Dispatchers.IO) {
        db.cardDao().deleteAllCards()
        db.cardDao().deleteAllBatches()
        db.financialVoucherDao().deleteAllVouchers()
        db.retailerDao().deleteAllRetailers()
        db.networkDeviceDao().deleteAllDevices()
        db.partnerDao().deleteAllPartners()
        db.partnerDao().deleteAllTransactions()
        db.networkAssetDao().deleteAllAssets()
        db.purchaseInvoiceDao().deleteAllInvoices()
        db.inventoryDao().deleteAllItems()
        db.inventoryMovementDao().deleteAllMovements()
        db.cardSalesInvoiceDao().deleteAllInvoices()
        db.cardPackageDao().deleteAllPackages()
        db.userDao().deleteAllUsers()
    }

    suspend fun switchUserWorkspace(
        email: String,
        displayName: String,
        photoUrl: String = ""
    ): UserEntity = withContext(Dispatchers.IO) {
        purgeAllOperationalData()

        val cleanEmail = email.trim().lowercase()
        val cleanName = displayName.trim().ifBlank { cleanEmail.substringBefore("@") }

        val newOwner = UserEntity(
            username = cleanEmail.substringBefore("@"),
            fullName = cleanName,
            role = "OWNER",
            email = cleanEmail,
            photoUrl = photoUrl,
            isGoogleUser = true
        )
        val id = db.userDao().insertUser(newOwner)

        val freshIdentity = NetworkIdentityEntity(
            id = 1L,
            networkName = "شبكة $cleanName",
            ownerName = cleanName,
            supportEmail = cleanEmail
        )
        db.networkIdentityDao().insertOrUpdate(freshIdentity)

        newOwner.copy(id = id)
    }

    suspend fun registerOrUpdateGoogleUser(
        email: String,
        displayName: String,
        photoUrl: String
    ): UserEntity = withContext(Dispatchers.IO) {
        val normalizedEmail = email.trim().lowercase()
        val existing = db.userDao().getUserByEmail(normalizedEmail)
            ?: if (email != normalizedEmail) db.userDao().getUserByEmail(email) else null

        if (existing != null) {
            val updated = existing.copy(
                fullName = if (existing.fullName.isNotBlank()) existing.fullName else displayName,
                photoUrl = photoUrl,
                email = normalizedEmail,
                role = "OWNER",
                isGoogleUser = true
            )
            db.userDao().updateUser(updated)
            updated
        } else {
            val cleanName = displayName.ifBlank { normalizedEmail.substringBefore("@") }
            val newUser = UserEntity(
                username = normalizedEmail.substringBefore("@"),
                fullName = cleanName,
                role = "OWNER",
                email = normalizedEmail,
                photoUrl = photoUrl,
                isGoogleUser = true
            )
            val newId = db.userDao().insertUser(newUser)
            newUser.copy(id = newId)
        }
    }

    suspend fun resetToProductionEnvironment(activeGoogleUser: UserEntity? = null): UserEntity = withContext(Dispatchers.IO) {
        // 1. Purge all operational and demo data
        purgeAllOperationalData()

        // 2. Create or preserve the authentic production administrator
        val productionAdmin = if (activeGoogleUser != null && activeGoogleUser.email.isNotBlank()) {
            activeGoogleUser.copy(
                id = 0,
                role = "OWNER",
                fullName = activeGoogleUser.fullName.ifBlank { activeGoogleUser.email.substringBefore("@") }
            )
        } else {
            UserEntity(
                username = "owner",
                fullName = "مالك الشبكة الجديد",
                role = "OWNER",
                email = "",
                isGoogleUser = false
            )
        }
        val adminId = db.userDao().insertUser(productionAdmin)

        // 3. Reset network identity to clean slate
        val freshIdentity = NetworkIdentityEntity(
            id = 1L,
            networkName = "شبكتي الخاصة",
            ownerName = productionAdmin.fullName,
            supportEmail = productionAdmin.email
        )
        db.networkIdentityDao().insertOrUpdate(freshIdentity)

        productionAdmin.copy(id = adminId)
    }

    /**
     * حذف كافة البيانات الافتراضية والتجريبية التي تم إدراجها سابقاً
     * مع الإبقاء الكامل على أي بيانات فعلية قام المستخدم بإضافتها أو مزامنتها.
     */
    suspend fun purgeDefaultDataOnly() = withContext(Dispatchers.IO) {
        try {
            val sqlDb = db.openHelper.writableDatabase

            // 1. حذف الكروت والدفعات الافتراضية
            sqlDb.execSQL("""
                DELETE FROM cards WHERE username LIKE 'sam2001%' OR username LIKE 'sam5002%' OR batchId IN (SELECT id FROM card_batches WHERE prefix IN ('SAM2', 'SAM5'))
            """.trimIndent())
            sqlDb.execSQL("""
                DELETE FROM card_batches WHERE prefix IN ('SAM2', 'SAM5') OR batchName LIKE '%دفعة الربيع%' OR batchName LIKE '%دفعة VIP%'
            """.trimIndent())

            // 2. حذف سندات القبض والصرف الافتراضية
            sqlDb.execSQL("""
                DELETE FROM financial_vouchers WHERE voucherNumber IN ('REC-2026-001', 'REC-2026-002', 'PAY-2026-001', 'PAY-2026-002', 'PAY-2026-003')
            """.trimIndent())

            // 3. حذف البقالات ونقاط التوزيع الافتراضية
            sqlDb.execSQL("""
                DELETE FROM retailers WHERE name IN ('بقالة البركة والخير', 'سوبرماركت النخبة', 'كشك الأمل للاتصالات') OR phone IN ('771234567', '777654321', '733889900')
            """.trimIndent())

            // 4. حذف أجهزة ومعدات الشبكة الافتراضية
            sqlDb.execSQL("""
                DELETE FROM network_devices WHERE macAddress IN ('D4:CA:6D:11:22:33', 'D4:CA:6D:44:55:66', 'F0:9F:C2:77:88:99', '68:D7:9A:12:34:56', 'CC:2D:E0:98:76:54', 'B4:FB:E4:AA:BB:CC')
            """.trimIndent())

            // 5. حذف الشركاء وحركات الأرباح الافتراضية
            sqlDb.execSQL("""
                DELETE FROM partners WHERE name IN ('المهندس سام الشبواني', 'الحاج عبد الرحمن القدسي', 'أ. نبيل صالح الحميري') OR phone IN ('770000001', '771122334', '772233445')
            """.trimIndent())
            sqlDb.execSQL("""
                DELETE FROM partner_transactions WHERE notes LIKE '%توزيع أرباح الربع المالي الماضي%'
            """.trimIndent())

            // 6. حذف الأصول الرأسمالية الافتراضية
            sqlDb.execSQL("""
                DELETE FROM network_assets WHERE serialNumber IN ('CCR2004-16G-SN8821', 'TWR-24M-TH', 'GW-SPF3500ES', 'LITH-48100-PRO', 'MIMO-A5C-4SEC', 'FIBER-4C-ARM-1KM', 'GEN-KM-5KVA')
            """.trimIndent())

            // 7. حذف حركات وأصناف المخزن الافتراضية
            sqlDb.execSQL("""
                DELETE FROM inventory_movements WHERE referenceNumber IN ('SUP-2026-001', 'SUP-2026-002')
            """.trimIndent())
            sqlDb.execSQL("""
                DELETE FROM inventory_items WHERE packageName IN ('باقة 100 ريال سريعة', 'باقة 200 ريال يومية', 'باقة 500 ريال فايبر', 'باقة 1000 ريال أسبوعية', 'باقة 2500 ريال نصف شهرية')
            """.trimIndent())

            // 8. حذف فواتير المبيعات الافتراضية والفواتير الوهمية
            sqlDb.execSQL("""
                DELETE FROM card_sales_invoices WHERE invoiceNumber IN ('INV-2026-1042', 'INV-2026-1043') OR invoiceNumber LIKE 'INV-DELIV-%'
            """.trimIndent())

            // 9. حذف المستخدمين التجريبيين الافتراضيين
            sqlDb.execSQL("""
                DELETE FROM app_users WHERE username IN ('eng_ayman', 'dist_fahad', 'pos_baraka')
            """.trimIndent())

            // 10. حذف باقات الكروت الافتراضية
            sqlDb.execSQL("""
                DELETE FROM card_packages WHERE notes LIKE '%باقة اقتصادية خفيفة%' OR notes LIKE '%الباقة الأكثر مبيعاً%' OR notes LIKE '%باقة عائلية سريعة%' OR notes LIKE '%باقة 10 جيجا%' OR notes LIKE '%باقة 22 جيجا%' OR notes LIKE '%باقة شهرية 50 جيجابايت%'
            """.trimIndent())

            android.util.Log.i("NetworkRepository", "All default/dummy records purged from database successfully.")
        } catch (e: Throwable) {
            android.util.Log.e("NetworkRepository", "Error during purgeDefaultDataOnly: ${e.message}", e)
        }
    }

    // ==========================================
    // Investment, Partners & Fixed Assets (CAPEX)
    // ==========================================
    val allPartners: Flow<List<PartnerEntity>> = db.partnerDao().getAllPartners()
    val totalInvestedCapital: Flow<Double?> = db.partnerDao().getTotalInvestedCapital()
    val totalDistributedProfits: Flow<Double?> = db.partnerDao().getTotalDistributedProfits()
    val allPartnerTransactions: Flow<List<PartnerTransactionEntity>> = db.partnerDao().getAllPartnerTransactions()

    suspend fun savePartner(partner: PartnerEntity): Long = withContext(Dispatchers.IO) {
        db.withTransaction {
            val isNew = partner.id == 0L
            val pId = if (isNew) {
                db.partnerDao().insertPartner(partner)
            } else {
                db.partnerDao().updatePartner(partner)
                partner.id
            }

            if (isNew && partner.capitalInvested > BigDecimal.ZERO) {
                val amount = partner.capitalInvested
                val entryNumber = "JE-PARTNER-${pId}-${System.currentTimeMillis() % 100000}"
                db.journalEntryDao().postBalancedEntry(
                    JournalEntryHeaderEntity(
                        entryNumber = entryNumber,
                        referenceType = "PARTNER_CAPITAL",
                        referenceId = pId.toString(),
                        description = "إيداع رأس مال مبدئي للشريك ${partner.name}",
                        createdBy = "المهندس سام"
                    ),
                    listOf(
                        JournalEntryLineEntity(
                            headerId = 0L,
                            accountCode = "1101",
                            accountName = "الصندوق الرئيسي (النقدية)",
                            lineType = "DEBIT",
                            debit = amount,
                            currency = partner.currency.ifBlank { "YER" },
                            partyId = pId,
                            partyType = "PARTNER"
                        ),
                        JournalEntryLineEntity(
                            headerId = 0L,
                            accountCode = "3101",
                            accountName = "رأس مال الشركاء الأساسي / ${partner.name}",
                            lineType = "CREDIT",
                            credit = amount,
                            currency = partner.currency.ifBlank { "YER" },
                            partyId = pId,
                            partyType = "PARTNER"
                        )
                    )
                )

                db.journalEntryDao().insertEntry(
                    JournalEntryEntity(
                        entryNumber = entryNumber,
                        referenceType = "PARTNER_CAPITAL",
                        referenceId = pId.toString(),
                        debitAccount = "1101 - الصندوق الرئيسي",
                        creditAccount = "3101 - رأس مال الشركاء / ${partner.name}",
                        amount = amount,
                        currency = partner.currency.ifBlank { "YER" },
                        exchangeRate = BigDecimal.ONE,
                        description = "إيداع رأس مال مبدئي للشريك ${partner.name}",
                        createdBy = "المهندس سام"
                    )
                )
            }
            pId
        }
    }

    suspend fun deletePartner(partner: PartnerEntity) = withContext(Dispatchers.IO) {
        db.partnerDao().deletePartner(partner)
    }

    suspend fun recordPartnerTransaction(tx: PartnerTransactionEntity) = withContext(Dispatchers.IO) {
        db.withTransaction {
            val txId = db.partnerDao().insertPartnerTransaction(tx)
            val partner = db.partnerDao().getPartnerById(tx.partnerId)

            if (tx.transactionType == "DIVIDEND_PAYOUT" || tx.transactionType == "DRAWING") {
                if (partner != null) {
                    db.partnerDao().updatePartner(
                        partner.copy(
                            totalWithdrawnProfit = partner.totalWithdrawnProfit.add(tx.amount),
                            currentAccountBalance = partner.currentAccountBalance.subtract(tx.amount)
                        )
                    )
                }
                val entryNumber = "JE-DIV-${txId}-${System.currentTimeMillis() % 100000}"
                db.journalEntryDao().postBalancedEntry(
                    JournalEntryHeaderEntity(
                        entryNumber = entryNumber,
                        referenceType = "PARTNER_DIVIDEND",
                        referenceId = txId.toString(),
                        description = "توزيع أرباح / مسحوبات للشريك ${tx.partnerName}",
                        createdBy = "المهندس سام"
                    ),
                    listOf(
                        JournalEntryLineEntity(
                            headerId = 0L,
                            accountCode = "3201",
                            accountName = "جاري الشركاء والمسحوبات / ${tx.partnerName}",
                            lineType = "DEBIT",
                            debit = tx.amount,
                            currency = tx.currency,
                            partyId = tx.partnerId,
                            partyType = "PARTNER"
                        ),
                        JournalEntryLineEntity(
                            headerId = 0L,
                            accountCode = "1101",
                            accountName = "الصندوق الرئيسي (النقدية)",
                            lineType = "CREDIT",
                            credit = tx.amount,
                            currency = tx.currency,
                            partyId = tx.partnerId,
                            partyType = "PARTNER"
                        )
                    )
                )

                db.journalEntryDao().insertEntry(
                    JournalEntryEntity(
                        entryNumber = entryNumber,
                        referenceType = "PARTNER_DIVIDEND",
                        referenceId = txId.toString(),
                        debitAccount = "3201 - أرباح ومسحوبات الشركاء / ${tx.partnerName}",
                        creditAccount = "1101 - الصندوق الرئيسي",
                        amount = tx.amount,
                        currency = tx.currency,
                        exchangeRate = BigDecimal.ONE,
                        description = "توزيع أرباح / مسحوبات للشريك ${tx.partnerName}",
                        createdBy = "المهندس سام"
                    )
                )
            } else if (tx.transactionType == "CAPITAL_ADDITION") {
                if (partner != null) {
                    db.partnerDao().updatePartner(
                        partner.copy(capitalInvested = partner.capitalInvested.add(tx.amount))
                    )
                }
                val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                val entryNumber = db.numberSequenceDao().getNextNumber("JE", currentYear, "JE")
                db.journalEntryDao().postBalancedEntry(
                    JournalEntryHeaderEntity(
                        entryNumber = entryNumber,
                        referenceType = "PARTNER_CAPITAL_ADDITION",
                        referenceId = txId.toString(),
                        description = "زيادة رأس مال للشريك ${tx.partnerName}",
                        createdBy = "المهندس سام"
                    ),
                    listOf(
                        JournalEntryLineEntity(
                            headerId = 0L,
                            accountCode = "1101",
                            accountName = "الصندوق الرئيسي (النقدية)",
                            lineType = "DEBIT",
                            debit = tx.amount,
                            currency = tx.currency,
                            partyId = tx.partnerId,
                            partyType = "PARTNER"
                        ),
                        JournalEntryLineEntity(
                            headerId = 0L,
                            accountCode = "3101",
                            accountName = "رأس مال الشركاء الأساسي / ${tx.partnerName}",
                            lineType = "CREDIT",
                            credit = tx.amount,
                            currency = tx.currency,
                            partyId = tx.partnerId,
                            partyType = "PARTNER"
                        )
                    )
                )

                db.journalEntryDao().insertEntry(
                    JournalEntryEntity(
                        entryNumber = entryNumber,
                        referenceType = "PARTNER_CAPITAL_ADDITION",
                        referenceId = txId.toString(),
                        debitAccount = "1101 - الصندوق الرئيسي",
                        creditAccount = "3101 - رأس مال الشركاء / ${tx.partnerName}",
                        amount = tx.amount,
                        currency = tx.currency,
                        exchangeRate = BigDecimal.ONE,
                        description = "زيادة رأس مال للشريك ${tx.partnerName}",
                        createdBy = "المهندس سام"
                    )
                )
            }
        }
    }

    suspend fun deletePartnerTransaction(tx: PartnerTransactionEntity) = withContext(Dispatchers.IO) {
        db.partnerDao().deletePartnerTransaction(tx)
    }

    // Fixed Assets (CAPEX)
    val allAssets: Flow<List<NetworkAssetEntity>> = db.networkAssetDao().getAllAssets()
    val totalAssetPurchaseCost: Flow<Double?> = db.networkAssetDao().getTotalPurchaseCost()
    val totalCurrentAssetValue: Flow<Double?> = db.networkAssetDao().getTotalCurrentAssetValue()
    val assetCount: Flow<Int> = db.networkAssetDao().getAssetsCount()

    suspend fun saveAsset(asset: NetworkAssetEntity): Long = withContext(Dispatchers.IO) {
        db.withTransaction {
            val identity = db.networkIdentityDao().getNetworkIdentity() ?: NetworkIdentityEntity()
            val sarRate = BigDecimal.valueOf(if (identity.sarToYerRate > 0) identity.sarToYerRate else 140.0)
            val usdRate = BigDecimal.valueOf(if (identity.usdToYerRate > 0) identity.usdToYerRate else 530.0)

            val rawOriginal = if (asset.originalCost > BigDecimal.ZERO) asset.originalCost else asset.purchaseCost
            val baseCost = com.example.util.CurrencyHelper.convertToYer(
                rawOriginal,
                asset.currency,
                sarRate,
                usdRate
            )

            val updatedAsset = asset.copy(
                purchaseCost = baseCost,
                originalCost = rawOriginal
            )

            val isNew = updatedAsset.id == 0L
            val aId = if (isNew) {
                db.networkAssetDao().insertAsset(updatedAsset)
            } else {
                db.networkAssetDao().updateAsset(updatedAsset)
                updatedAsset.id
            }

            // لا ننشئ قيد يومية مباشر هنا لتفادي مضاعفة الأصول ونقصان الصندوق مرتين،
            // فالأصول تسجّل قيودها المحاسبية حصرياً عبر فاتورة المشتريات أو سند الصرف
            aId
        }
    }

    suspend fun deleteAsset(asset: NetworkAssetEntity) = withContext(Dispatchers.IO) {
        db.networkAssetDao().deleteAsset(asset)
    }

    // Purchase Invoices (فواتير المشتريات والأصول)
    val allInvoices: Flow<List<PurchaseInvoiceEntity>> = db.purchaseInvoiceDao().getAllInvoices()
    val totalInvoicesAmount: Flow<Double?> = db.purchaseInvoiceDao().getTotalInvoicesAmount()

    suspend fun saveInvoice(
        invoice: PurchaseInvoiceEntity,
        paymentVoucher: FinancialVoucherEntity? = null
    ): Long = withContext(Dispatchers.IO) {
        db.withTransaction {
            val identity = db.networkIdentityDao().getNetworkIdentity() ?: NetworkIdentityEntity()
            val sarRate = BigDecimal.valueOf(if (identity.sarToYerRate > 0) identity.sarToYerRate else 140.0)
            val usdRate = BigDecimal.valueOf(if (identity.usdToYerRate > 0) identity.usdToYerRate else 530.0)

            val rawOriginal = if (invoice.originalAmount > BigDecimal.ZERO) invoice.originalAmount else invoice.totalAmount
            val baseAmount = com.example.util.CurrencyHelper.convertToYer(rawOriginal, invoice.currency, sarRate, usdRate)

            val updatedInvoice = invoice.copy(
                totalAmount = baseAmount,
                originalAmount = rawOriginal
            )

            val invId = if (updatedInvoice.id == 0L) {
                db.purchaseInvoiceDao().insertInvoice(updatedInvoice)
            } else {
                db.purchaseInvoiceDao().updateInvoice(updatedInvoice)
                updatedInvoice.id
            }

            // إذا وُجد سند صرف مسدد للفاتورة، يتم إدخاله أولاً في الداتابيس لتكتشفه postPurchaseInvoice
            if (paymentVoucher != null) {
                val voucherToInsert = paymentVoucher.copy(
                    invoiceId = invId,
                    invoiceNumber = updatedInvoice.invoiceNumber,
                    invoiceKind = "PURCHASE"
                )
                db.financialVoucherDao().insertVoucher(voucherToInsert)
                try {
                    postingService.postFinancialVoucher(voucherToInsert, voucherToInsert.issuerName)
                } catch (e: Exception) {
                    android.util.Log.e("NetworkRepository", "Error posting payment voucher for purchase: ${e.message}", e)
                }
            }

            // ترحيل فاتورة المشتريات مركزياً عبر المحرك المحاسبي الموحد
            try {
                if (invoice.id != 0L) {
                    postingService.updatePurchaseInvoicePosting(
                        invoice = updatedInvoice.copy(id = invId),
                        performer = "المهندس سام"
                    )
                } else {
                    postingService.postPurchaseInvoice(
                        invoice = updatedInvoice.copy(id = invId),
                        performer = "المهندس سام"
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("NetworkRepository", "Error posting purchase invoice via postingService: ${e.message}", e)
            }

            invId
        }
    }

    suspend fun deleteInvoice(invoice: PurchaseInvoiceEntity) = withContext(Dispatchers.IO) {
        db.withTransaction {
            val voided = invoice.copy(isVoided = true, status = "VOIDED", voidReason = "إلغاء فاتورة المشتريات وتسوية الدفاتر")
            db.purchaseInvoiceDao().updateInvoice(voided)

            // إلغاء سندات الصرف المرتبطة بالفاتورة بقيد عكسي تسوية
            val linkedVouchers = if (invoice.id > 0L) {
                db.financialVoucherDao().getVouchersByInvoiceId(invoice.id)
            } else {
                db.financialVoucherDao().getVouchersByInvoiceNumber(invoice.invoiceNumber)
            }
            for (v in linkedVouchers) {
                if (!v.isVoided) {
                    db.financialVoucherDao().updateVoucher(
                        v.copy(
                            isVoided = true,
                            notes = (v.notes + " (ملغى لإلغاء فاتورة المشتريات #${invoice.invoiceNumber})").trim()
                        )
                    )
                    val vHeader = db.journalEntryDao().getActiveHeaderByReferenceWithAlt(
                        "FINANCIAL_VOUCHER",
                        v.voucherNumber,
                        v.id.toString()
                    )
                    if (vHeader != null) {
                        db.journalEntryDao().voidEntryWithReversal(
                            vHeader.id,
                            "إلغاء سند صرف بسبب إلغاء فاتورة المشتريات #${invoice.invoiceNumber}",
                            "المهندس سام"
                        )
                    }
                }
            }

            try {
                val header = db.journalEntryDao().getActiveHeaderByReferenceWithAlt(
                    "PURCHASE_INVOICE",
                    invoice.invoiceNumber,
                    invoice.id.toString()
                )
                if (header != null) {
                    db.journalEntryDao().voidEntryWithReversal(header.id, "إلغاء فاتورة المشتريات #${invoice.invoiceNumber}", "المهندس سام")
                }
            } catch (e: Exception) {
                android.util.Log.e("NetworkRepository", "Error voiding header for purchase invoice: ${e.message}", e)
            }
        }
    }

    /**
     * Integrates all cloud-restored data into the local Room database.
     * Prevents duplication by matching IP/Names/Numbers and updating existing entities or inserting new ones.
     */
    suspend fun restoreFromCloudData(cloudData: com.example.data.firebase.CloudPullData) = withContext(Dispatchers.IO) {
        // 1. Restore Network Identity safely preserving non-zero exchange rates
        cloudData.networkIdentity?.let { cloudIdentity ->
            val existing = db.networkIdentityDao().getNetworkIdentity() ?: NetworkIdentityEntity()

            val effectiveSar = if (cloudIdentity.sarToYerRate > 0) cloudIdentity.sarToYerRate else if (existing.sarToYerRate > 0) existing.sarToYerRate else 140.0
            val effectiveUsd = if (cloudIdentity.usdToYerRate > 0) cloudIdentity.usdToYerRate else if (existing.usdToYerRate > 0) existing.usdToYerRate else 530.0
            val effectiveUsdSar = if (cloudIdentity.usdToSarRate > 0) cloudIdentity.usdToSarRate else if (existing.usdToSarRate > 0) existing.usdToSarRate else 3.79

            val mergedIdentity = cloudIdentity.copy(
                id = 1L,
                sarToYerRate = effectiveSar,
                usdToYerRate = effectiveUsd,
                usdToSarRate = effectiveUsdSar
            )
            db.networkIdentityDao().insertOrUpdate(mergedIdentity)

            db.currencyRateDao().insertOrUpdateRates(
                listOf(
                    com.example.data.local.entity.CurrencyRateEntity("USD", BigDecimal.valueOf(effectiveUsd)),
                    com.example.data.local.entity.CurrencyRateEntity("SAR", BigDecimal.valueOf(effectiveSar)),
                    com.example.data.local.entity.CurrencyRateEntity("YER", BigDecimal.ONE)
                )
            )
        }

        // 2. Restore Devices
        val existingDevices = db.networkDeviceDao().getAllDevices().first()
        val existingIps = existingDevices.associateBy { it.ipAddress.trim() }
        val existingDevNames = existingDevices.associateBy { it.name.trim() }

        for (device in cloudData.devices) {
            val match = existingIps[device.ipAddress.trim()] ?: existingDevNames[device.name.trim()]
            if (match != null) {
                db.networkDeviceDao().updateDevice(
                    device.copy(id = match.id)
                )
            } else {
                db.networkDeviceDao().insertDevice(
                    device.copy(id = 0L)
                )
            }
        }

        // 3. Restore Retailers
        val existingRetailers = db.retailerDao().getAllRetailers().first()
        val existingRetNames = existingRetailers.associateBy { it.name.trim() }

        for (retailer in cloudData.retailers) {
            val match = existingRetNames[retailer.name.trim()]
            if (match != null) {
                db.retailerDao().updateRetailer(
                    retailer.copy(id = match.id)
                )
            } else {
                db.retailerDao().insertRetailer(
                    retailer.copy(id = 0L)
                )
            }
        }

        // 4. Restore Financial Vouchers
        val existingVouchers = db.financialVoucherDao().getAllVouchers().first()
        val runningVouchers = existingVouchers.associateBy { it.voucherNumber.trim().uppercase() }.toMutableMap()

        for (voucher in cloudData.vouchers) {
            val vNum = voucher.voucherNumber.trim()
            val numKey = vNum.uppercase()
            if (vNum.isBlank() || com.example.data.local.DeletedRecordsTracker.isVoucherDeleted(numKey)) {
                // تجاهل أي سند تم حذفه مسبقاً محلياً لمنع إعادته
                continue
            }
            val match = runningVouchers[numKey]
            if (match != null) {
                val preservedVoid = match.isVoided || voucher.isVoided
                val updated = voucher.copy(id = match.id, isVoided = preservedVoid)
                db.financialVoucherDao().updateVoucher(updated)
                runningVouchers[numKey] = updated
            } else {
                val newId = db.financialVoucherDao().insertVoucher(
                    voucher.copy(id = 0L)
                )
                runningVouchers[numKey] = voucher.copy(id = newId)
            }
        }
        deduplicateVouchers()

        // 5. Restore Card Packages
        val existingPackages = db.cardPackageDao().getAllPackages().first()
        val existingPkgNames = existingPackages.associateBy { it.name.trim() }

        for (pkg in cloudData.cardPackages) {
            val match = existingPkgNames[pkg.name.trim()]
            if (match != null) {
                db.cardPackageDao().updatePackage(
                    pkg.copy(id = match.id)
                )
            } else {
                db.cardPackageDao().insertPackage(
                    pkg.copy(id = 0L)
                )
            }
        }

        // 6. Restore Inventory Items
        val existingInventory = db.inventoryDao().getAllInventoryItems().first()
        val existingInvNames = existingInventory.associateBy { it.packageName.trim() }

        for (item in cloudData.inventoryItems) {
            val match = existingInvNames[item.packageName.trim()]
            if (match != null) {
                db.inventoryDao().updateItem(
                    item.copy(id = match.id)
                )
            } else {
                db.inventoryDao().insertItem(
                    item.copy(id = 0L)
                )
            }
        }

        // 7. Restore Card Sales Invoices
        val existingInvoices = db.cardSalesInvoiceDao().getAllSalesInvoices().first()
        val existingInvNums = existingInvoices.associateBy { it.invoiceNumber.trim() }

        for (inv in cloudData.salesInvoices) {
            val match = existingInvNums[inv.invoiceNumber.trim()]
            val finalId = if (match != null) {
                db.cardSalesInvoiceDao().updateInvoice(
                    inv.copy(id = match.id)
                )
                match.id
            } else {
                db.cardSalesInvoiceDao().insertInvoice(
                    inv.copy(id = 0L)
                )
            }

            // خصم مبيعات السحابة إذا لم تكن مخصومة مسبقاً
            if (!inv.isVoided && inv.status != "VOIDED") {
                val alreadyDeducted = db.inventoryMovementDao().hasSaleMovementForInvoice(inv.invoiceNumber) > 0
                if (!alreadyDeducted) {
                    deductInvoiceFromInventory(inv.copy(id = finalId), "استعادة سحابية")
                }
            }
        }

        // 8. تسوية ومطابقة الحسابات والديون المحاسبية فورياً بعد الاستعادة
        reconcileAccountingLedger()
    }

    /**
     * استعلامات التجميع المحاسبي الفتراوي لتقرير الإقفال والتسوية الدورية (Periodic Settlement Dashboard)
     */
    suspend fun getPeriodicSettlementData(startDateMillis: Long, endDateMillis: Long): PeriodicSettlementData = withContext(Dispatchers.IO) {
        val genCards = db.cardDao().getGeneratedCardsCount(startDateMillis, endDateMillis)
        val soldCardsByCards = db.cardDao().getSoldCardsCount(startDateMillis, endDateMillis)
        val soldCardsByInvoices = db.cardSalesInvoiceDao().getSoldCardsCountFromInvoices(startDateMillis, endDateMillis) ?: 0
        val finalSoldCards = maxOf(soldCardsByCards, soldCardsByInvoices)

        val receipts = db.financialVoucherDao().getTotalReceiptsAmount(startDateMillis, endDateMillis) ?: 0.0
        val expensesVouchers = db.financialVoucherDao().getTotalExpensesAmount(startDateMillis, endDateMillis) ?: 0.0
        val purchases = db.purchaseInvoiceDao().getTotalPurchaseInvoicesAmount(startDateMillis, endDateMillis) ?: 0.0
        val totalExpenses = expensesVouchers + purchases

        val newDebt = db.cardSalesInvoiceDao().getTotalNewDebt(startDateMillis, endDateMillis) ?: 0.0
        val salesAmount = db.cardSalesInvoiceDao().getTotalSalesAmountForPeriod(startDateMillis, endDateMillis) ?: 0.0

        val netBalance = receipts - totalExpenses

        PeriodicSettlementData(
            startDateMillis = startDateMillis,
            endDateMillis = endDateMillis,
            generatedCardsCount = genCards,
            soldCardsCount = finalSoldCards,
            totalReceiptsAmount = receipts,
            totalExpensesAmount = totalExpenses,
            totalNewDebt = newDebt,
            totalSalesAmount = salesAmount,
            netBalance = netBalance
        )
    }

    suspend fun importRetailersBatch(
        retailersToImport: List<RetailerEntity>,
        replaceExisting: Boolean = false
    ): Int = withContext(Dispatchers.IO) {
        db.withTransaction {
            if (replaceExisting) {
                db.retailerDao().deleteAllRetailers()
            }
            val existing = db.retailerDao().getRetailersList().associateBy { it.name.trim().lowercase() }
            var count = 0
            for (ret in retailersToImport) {
                val match = existing[ret.name.trim().lowercase()]
                if (match != null && !replaceExisting) {
                    db.retailerDao().updateRetailer(ret.copy(id = match.id))
                } else {
                    db.retailerDao().insertRetailer(ret.copy(id = 0L))
                }
                count++
            }
            reconcileAccountingLedgerInternal()
            count
        }
    }

    suspend fun importPurchaseInvoicesBatch(
        invoicesToImport: List<PurchaseInvoiceEntity>,
        replaceExisting: Boolean = false
    ): Int = withContext(Dispatchers.IO) {
        db.withTransaction {
            if (replaceExisting) {
                db.purchaseInvoiceDao().deleteAllInvoices()
            }
            val existing = db.purchaseInvoiceDao().getAllInvoices().first().associateBy { it.invoiceNumber.trim().lowercase() }
            val identity = db.networkIdentityDao().getNetworkIdentity() ?: NetworkIdentityEntity()
            val sarRate = BigDecimal.valueOf(if (identity.sarToYerRate > 0) identity.sarToYerRate else 140.0)
            val usdRate = BigDecimal.valueOf(if (identity.usdToYerRate > 0) identity.usdToYerRate else 530.0)

            var count = 0
            for (inv in invoicesToImport) {
                val rawOriginal = if (inv.originalAmount > BigDecimal.ZERO) inv.originalAmount else inv.totalAmount
                val baseAmount = com.example.util.CurrencyHelper.convertToYer(rawOriginal, inv.currency, sarRate, usdRate)
                val invWithBase = inv.copy(
                    totalAmount = baseAmount,
                    originalAmount = rawOriginal
                )
                val match = existing[inv.invoiceNumber.trim().lowercase()]
                val savedInv = if (match != null && !replaceExisting) {
                    val updated = invWithBase.copy(id = match.id)
                    db.purchaseInvoiceDao().updateInvoice(updated)
                    updated
                } else {
                    val newId = db.purchaseInvoiceDao().insertInvoice(invWithBase.copy(id = 0L))
                    invWithBase.copy(id = newId)
                }
                try {
                    postingService.postPurchaseInvoice(savedInv)
                } catch (e: Exception) {
                    android.util.Log.e("NetworkRepository", "Error posting imported purchase invoice: ${e.message}", e)
                }
                count++
            }
            count
        }
    }

    suspend fun importSalesInvoicesBatch(
        invoicesToImport: List<CardSalesInvoiceEntity>,
        replaceExisting: Boolean = false
    ): Int = withContext(Dispatchers.IO) {
        db.withTransaction {
            if (replaceExisting) {
                db.cardSalesInvoiceDao().deleteAllInvoices()
            }
            val existing = db.cardSalesInvoiceDao().getSalesInvoicesList().associateBy { it.invoiceNumber.trim().lowercase() }
            var count = 0
            for (inv in invoicesToImport) {
                val match = existing[inv.invoiceNumber.trim().lowercase()]
                val finalId = if (match != null && !replaceExisting) {
                    db.cardSalesInvoiceDao().updateInvoice(inv.copy(id = match.id))
                    match.id
                } else {
                    db.cardSalesInvoiceDao().insertInvoice(inv.copy(id = 0L))
                }

                // خصم الكروت المباعة في الفاتورة المستوردة من المخزن فوراً وتوثيق الحركة
                if (!inv.isVoided && inv.status != "VOIDED") {
                    val alreadyDeducted = db.inventoryMovementDao().hasSaleMovementForInvoice(inv.invoiceNumber) > 0
                    if (!alreadyDeducted) {
                        deductInvoiceFromInventory(inv.copy(id = finalId), "استيراد مبيعات JSON")
                    }
                }
                count++
            }
            reconcileAccountingLedgerInternal()
            reconcileInventoryWithSalesInvoices()
            count
        }
    }
}

data class PackageAuditReport(
    val packageName: String,
    val totalSoldInInvoices: Int,
    val currentAvailableStock: Int,
    val newlyDeductedCount: Int,
    val wholesalePrice: Double,
    val retailPrice: Double,
    val totalSoldValuation: Double,
    val availableStockValuation: Double
)

data class InventoryReconciliationSummary(
    val totalInvoicesAudited: Int,
    val newlyDeductedInvoicesCount: Int,
    val totalCardsNewlyDeducted: Int,
    val totalReturnedCardsFromVoided: Int,
    val categoryReports: List<PackageAuditReport>
)

data class PeriodicSettlementData(
    val startDateMillis: Long,
    val endDateMillis: Long,
    val generatedCardsCount: Int,
    val soldCardsCount: Int,
    val totalReceiptsAmount: Double,
    val totalExpensesAmount: Double,
    val totalNewDebt: Double,
    val totalSalesAmount: Double,
    val netBalance: Double
)

