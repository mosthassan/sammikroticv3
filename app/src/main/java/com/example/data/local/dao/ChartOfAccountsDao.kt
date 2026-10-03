package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ChartOfAccountsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChartOfAccountsDao {

    @Query("SELECT * FROM chart_of_accounts ORDER BY accountCode ASC")
    fun getAllAccounts(): Flow<List<ChartOfAccountsEntity>>

    @Query("SELECT * FROM chart_of_accounts WHERE isActive = 1 ORDER BY accountCode ASC")
    fun getActiveAccounts(): Flow<List<ChartOfAccountsEntity>>

    @Query("SELECT * FROM chart_of_accounts WHERE accountCode = :code LIMIT 1")
    suspend fun getAccountByCode(code: String): ChartOfAccountsEntity?

    @Query("SELECT * FROM chart_of_accounts WHERE accountType = :type AND isActive = 1 ORDER BY accountCode ASC")
    fun getAccountsByType(type: String): Flow<List<ChartOfAccountsEntity>>

    @Query("SELECT COUNT(*) FROM chart_of_accounts")
    suspend fun getAccountsCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAccount(account: ChartOfAccountsEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAccounts(accounts: List<ChartOfAccountsEntity>)

    @Update
    suspend fun updateAccount(account: ChartOfAccountsEntity)

    @Delete
    suspend fun deleteAccount(account: ChartOfAccountsEntity)

    companion object {
        val DEFAULT_STANDARD_CHART = listOf(
            // 1000 - الأصول (Assets)
            ChartOfAccountsEntity(
                accountCode = "1101",
                accountNameAr = "الصندوق الرئيسي (النقدية)",
                accountNameEn = "Main Cash Box",
                accountType = "ASSET",
                normalBalance = "DEBIT",
                description = "النقدية السائلة المتوفرة في الصندوق والعهدة"
            ),
            ChartOfAccountsEntity(
                accountCode = "1102",
                accountNameAr = "البنوك ومحافظ الصرافة",
                accountNameEn = "Banks & Digital Wallets",
                accountType = "ASSET",
                normalBalance = "DEBIT",
                description = "حسابات الصرافة والكريمي والمحافظ الإلكترونية"
            ),
            ChartOfAccountsEntity(
                accountCode = "1201",
                accountNameAr = "ذمم العملاء والوكلاء (مدينون)",
                accountNameEn = "Accounts Receivable - Agents",
                accountType = "ASSET",
                normalBalance = "DEBIT",
                description = "مديونيات وأرصدة البقالات والوكلاء وموزعي الكروت"
            ),
            ChartOfAccountsEntity(
                accountCode = "1301",
                accountNameAr = "مخزون كروت الشبكة المطبوعة",
                accountNameEn = "Inventory - Printed Cards",
                accountType = "ASSET",
                normalBalance = "DEBIT",
                description = "قيمة الكروت المطبوعة الجاهزة للبيع بتكلفة الإنتاج"
            ),
            ChartOfAccountsEntity(
                accountCode = "1501",
                accountNameAr = "الأصول الرأسمالية الثابتة",
                accountNameEn = "Property, Plant & Equipment",
                accountType = "ASSET",
                normalBalance = "DEBIT",
                description = "أجهزة وسيرفرات الشبكة، راوترات ميكروتك، الأبراج والطاقة"
            ),
            ChartOfAccountsEntity(
                accountCode = "1509",
                accountNameAr = "مجمع إهلاك الأصول الثابتة",
                accountNameEn = "Accumulated Depreciation",
                accountType = "ASSET",
                normalBalance = "CREDIT",
                description = "حساب مقابل للأصول لتجميع أقساط الإهلاك المحملة دورياً"
            ),

            // 2000 - الخصوم (Liabilities)
            ChartOfAccountsEntity(
                accountCode = "2101",
                accountNameAr = "ذمم الموردين والشركات المزودة (دائنون)",
                accountNameEn = "Accounts Payable - Suppliers",
                accountType = "LIABILITY",
                normalBalance = "CREDIT",
                description = "مستحقات مزودي باقات الإنترنت، تجار الأجهزة، والموردين"
            ),

            // 3000 - حقوق الملكية (Equity)
            ChartOfAccountsEntity(
                accountCode = "3101",
                accountNameAr = "رأس مال الشركاء الأساسي",
                accountNameEn = "Partners Invested Capital",
                accountType = "EQUITY",
                normalBalance = "CREDIT",
                description = "حصص رأس المال التأسيسي والإضافي المدفوع من الشركاء"
            ),
            ChartOfAccountsEntity(
                accountCode = "3201",
                accountNameAr = "جاري الشركاء والمسحوبات",
                accountNameEn = "Partners Current & Drawings",
                accountType = "EQUITY",
                normalBalance = "CREDIT",
                description = "الحساب الجاري لحركات الأرباح المعتمدة والمسحوبات الدورية"
            ),
            ChartOfAccountsEntity(
                accountCode = "3301",
                accountNameAr = "الأرباح المدورة والمحتجزة",
                accountNameEn = "Retained Earnings",
                accountType = "EQUITY",
                normalBalance = "CREDIT",
                description = "الأرباح غير الموزعة المتبقية لتمويل توسعات الشبكة"
            ),

            // 4000 - الإيرادات (Revenue)
            ChartOfAccountsEntity(
                accountCode = "4101",
                accountNameAr = "إيرادات مبيعات كروت الشبكة",
                accountNameEn = "Card Sales Revenue",
                accountType = "REVENUE",
                normalBalance = "CREDIT",
                description = "الإيرادات المحققة من بيع وتوزيع كروت الإنترنت"
            ),
            ChartOfAccountsEntity(
                accountCode = "4102",
                accountNameAr = "مردودات ومسموحات المبيعات",
                accountNameEn = "Sales Returns & Allowances",
                accountType = "REVENUE",
                normalBalance = "DEBIT",
                description = "مردودات ومسوغات كروت ومبيعات الشبكة المسترجعة"
            ),
            ChartOfAccountsEntity(
                accountCode = "4201",
                accountNameAr = "إيرادات الاشتراكات المباشرة",
                accountNameEn = "Direct Subscriptions Revenue",
                accountType = "REVENUE",
                normalBalance = "CREDIT",
                description = "إيرادات الخطوط المنزلية والربط اللاسلكي المباشر"
            ),

            // 5000 - تكلفة المبيعات والمصروفات (COGS & Expenses)
            ChartOfAccountsEntity(
                accountCode = "5101",
                accountNameAr = "اشتراك النت الرئيسي (ستارلينك)",
                accountNameEn = "Main Internet Subscription / Starlink",
                accountType = "EXPENSE",
                normalBalance = "DEBIT",
                description = "التكلفة المباشرة لاشتراك خط الإنترنت الرئيسي والربط مع المزود (ستارلينك)"
            ),
            ChartOfAccountsEntity(
                accountCode = "5201",
                accountNameAr = "مصروفات تشغيلية وعمومية",
                accountNameEn = "Operating Expenses",
                accountType = "EXPENSE",
                normalBalance = "DEBIT",
                description = "اشتراك خطوط النت الرئيسية، وقود الديزل، الطاقة الشمسية، والإيجارات"
            ),
            ChartOfAccountsEntity(
                accountCode = "5202",
                accountNameAr = "مصروفات صيانة وقطع غيار",
                accountNameEn = "Maintenance & Repairs",
                accountType = "EXPENSE",
                normalBalance = "DEBIT",
                description = "صيانة الأبراج، كيابل الفايبر، تغيير قطع الشبكة التالفة"
            ),
            ChartOfAccountsEntity(
                accountCode = "5203",
                accountNameAr = "مصروف إهلاك أصول الشبكة",
                accountNameEn = "Depreciation Expense",
                accountType = "EXPENSE",
                normalBalance = "DEBIT",
                description = "القسط الدوري لإهلاك المعدات والأجهزة الرأسمالية"
            )
        )
    }
}
