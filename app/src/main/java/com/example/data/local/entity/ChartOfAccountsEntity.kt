package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * يمثل حساباً في شجرة الحسابات المحاسبية المعيارية (Chart of Accounts)
 * وفق تصنيف الحسابات المحاسبية الدولية:
 * 1: الأصول (Assets)
 * 2: الخصوم والالتزامات (Liabilities)
 * 3: حقوق الملكية ورأس المال (Equity)
 * 4: الإيرادات (Revenue)
 * 5: تكاليف المبيعات والمصروفات (COGS & Expenses)
 */
@Entity(tableName = "chart_of_accounts")
data class ChartOfAccountsEntity(
    @PrimaryKey
    val accountCode: String,            // كود الحساب الفريد، مثل: 1101, 1201, 3101, 4101, 5101
    val accountNameAr: String,          // الاسم العربي للحساب
    val accountNameEn: String = "",     // الاسم الإنجليزي للحساب
    val accountType: String,            // نوع الحساب: "ASSET", "LIABILITY", "EQUITY", "REVENUE", "EXPENSE"
    val normalBalance: String,          // الطبيعة المحاسبية: "DEBIT" (مدين), "CREDIT" (دائن)
    val parentAccountCode: String? = null, // كود الحساب الأب للشجرة
    val isHeader: Boolean = false,      // هل هو حساب رئيسي تجميعي أم حساب فرعي يقبل القيود
    val isActive: Boolean = true,       // حالة تفعيل الحساب
    val description: String = ""        // وصف الحساب واستخداماته
)
