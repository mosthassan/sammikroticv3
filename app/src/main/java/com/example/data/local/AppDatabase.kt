package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.converter.BigDecimalConverter
import com.example.data.local.dao.CardDao
import com.example.data.local.dao.CardPackageDao
import com.example.data.local.dao.CardSalesInvoiceDao
import com.example.data.local.dao.CurrencyRateDao
import com.example.data.local.dao.CustomerLedgerDao
import com.example.data.local.dao.FinancialVoucherDao
import com.example.data.local.dao.InventoryDao
import com.example.data.local.dao.InventoryMovementDao
import com.example.data.local.dao.JournalEntryDao
import com.example.data.local.dao.NetworkAssetDao
import com.example.data.local.dao.NetworkDeviceDao
import com.example.data.local.dao.NetworkIdentityDao
import com.example.data.local.dao.PartnerDao
import com.example.data.local.dao.PurchaseInvoiceDao
import com.example.data.local.dao.RetailerDao
import com.example.data.local.dao.UserDao
import com.example.data.local.entity.CardBatchEntity
import com.example.data.local.entity.CardEntity
import com.example.data.local.entity.CardPackageEntity
import com.example.data.local.entity.CardSalesInvoiceEntity
import com.example.data.local.entity.CurrencyRateEntity
import com.example.data.local.entity.CustomerLedgerEntity
import com.example.data.local.entity.FinancialVoucherEntity
import com.example.data.local.entity.InventoryItemEntity
import com.example.data.local.entity.InventoryMovementEntity
import com.example.data.local.entity.JournalEntryEntity
import com.example.data.local.entity.NetworkAssetEntity
import com.example.data.local.entity.NetworkDeviceEntity
import com.example.data.local.entity.NetworkIdentityEntity
import com.example.data.local.entity.PartnerEntity
import com.example.data.local.entity.PartnerTransactionEntity
import com.example.data.local.entity.PurchaseInvoiceEntity
import com.example.data.local.entity.RetailerEntity
import com.example.data.local.entity.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Database(
    entities = [
        NetworkDeviceEntity::class,
        CardBatchEntity::class,
        CardEntity::class,
        CardPackageEntity::class,
        RetailerEntity::class,
        FinancialVoucherEntity::class,
        UserEntity::class,
        NetworkIdentityEntity::class,
        PartnerEntity::class,
        NetworkAssetEntity::class,
        PartnerTransactionEntity::class,
        InventoryItemEntity::class,
        PurchaseInvoiceEntity::class,
        CardSalesInvoiceEntity::class,
        InventoryMovementEntity::class,
        JournalEntryEntity::class,
        CustomerLedgerEntity::class,
        CurrencyRateEntity::class
    ],
    version = 19,
    exportSchema = false
)
@TypeConverters(BigDecimalConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun networkDeviceDao(): NetworkDeviceDao
    abstract fun cardDao(): CardDao
    abstract fun cardPackageDao(): CardPackageDao
    abstract fun retailerDao(): RetailerDao
    abstract fun financialVoucherDao(): FinancialVoucherDao
    abstract fun userDao(): UserDao
    abstract fun networkIdentityDao(): NetworkIdentityDao
    abstract fun partnerDao(): PartnerDao
    abstract fun networkAssetDao(): NetworkAssetDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun purchaseInvoiceDao(): PurchaseInvoiceDao
    abstract fun cardSalesInvoiceDao(): CardSalesInvoiceDao
    abstract fun inventoryMovementDao(): InventoryMovementDao
    abstract fun journalEntryDao(): JournalEntryDao
    abstract fun customerLedgerDao(): CustomerLedgerDao
    abstract fun currencyRateDao(): CurrencyRateDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `journal_entries` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `entryNumber` TEXT NOT NULL,
                        `dateMillis` INTEGER NOT NULL,
                        `referenceType` TEXT NOT NULL,
                        `referenceId` TEXT NOT NULL,
                        `debitAccount` TEXT NOT NULL,
                        `creditAccount` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `currency` TEXT NOT NULL,
                        `exchangeRate` REAL NOT NULL,
                        `description` TEXT NOT NULL,
                        `createdBy` TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `financial_vouchers` ADD COLUMN `isVoided` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. journal_entries
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `journal_entries_temp` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `entryNumber` TEXT NOT NULL,
                        `dateMillis` INTEGER NOT NULL,
                        `referenceType` TEXT NOT NULL,
                        `referenceId` TEXT NOT NULL,
                        `debitAccount` TEXT NOT NULL,
                        `creditAccount` TEXT NOT NULL,
                        `amount` TEXT NOT NULL,
                        `currency` TEXT NOT NULL,
                        `exchangeRate` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `createdBy` TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO `journal_entries_temp` (`id`, `entryNumber`, `dateMillis`, `referenceType`, `referenceId`, `debitAccount`, `creditAccount`, `amount`, `currency`, `exchangeRate`, `description`, `createdBy`)
                    SELECT `id`, `entryNumber`, `dateMillis`, `referenceType`, `referenceId`, `debitAccount`, `creditAccount`, CAST(`amount` AS TEXT), `currency`, CAST(`exchangeRate` AS TEXT), `description`, `createdBy`
                    FROM `journal_entries`
                """.trimIndent())
                db.execSQL("DROP TABLE `journal_entries`")
                db.execSQL("ALTER TABLE `journal_entries_temp` RENAME TO `journal_entries`")

                // 2. financial_vouchers
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `financial_vouchers_temp` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `voucherNumber` TEXT NOT NULL,
                        `voucherType` TEXT NOT NULL,
                        `amount` TEXT NOT NULL,
                        `currency` TEXT NOT NULL,
                        `originalAmount` TEXT NOT NULL,
                        `partyName` TEXT NOT NULL,
                        `retailerId` INTEGER,
                        `invoiceId` INTEGER,
                        `invoiceNumber` TEXT NOT NULL,
                        `allocatedAmount` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `paymentMethod` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `issuerName` TEXT NOT NULL,
                        `dateMillis` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL,
                        `isVoided` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO `financial_vouchers_temp` (`id`, `voucherNumber`, `voucherType`, `amount`, `currency`, `originalAmount`, `partyName`, `retailerId`, `invoiceId`, `invoiceNumber`, `allocatedAmount`, `category`, `paymentMethod`, `description`, `issuerName`, `dateMillis`, `notes`, `isVoided`)
                    SELECT `id`, `voucherNumber`, `voucherType`, CAST(`amount` AS TEXT), `currency`, CAST(`originalAmount` AS TEXT), `partyName`, `retailerId`, `invoiceId`, `invoiceNumber`, CAST(`allocatedAmount` AS TEXT), `category`, `paymentMethod`, `description`, `issuerName`, `dateMillis`, `notes`, `isVoided`
                    FROM `financial_vouchers`
                """.trimIndent())
                db.execSQL("DROP TABLE `financial_vouchers`")
                db.execSQL("ALTER TABLE `financial_vouchers_temp` RENAME TO `financial_vouchers`")

                // 3. card_sales_invoices
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `card_sales_invoices_temp` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `invoiceNumber` TEXT NOT NULL,
                        `customerName` TEXT NOT NULL,
                        `customerPhone` TEXT NOT NULL,
                        `retailerId` INTEGER,
                        `invoiceDateMillis` INTEGER NOT NULL,
                        `paymentType` TEXT NOT NULL,
                        `totalAmount` TEXT NOT NULL,
                        `paidAmount` TEXT NOT NULL,
                        `remainingAmount` TEXT NOT NULL,
                        `totalCardsCount` INTEGER NOT NULL,
                        `itemsCount` INTEGER NOT NULL,
                        `itemsSummary` TEXT NOT NULL,
                        `itemsJson` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `issuerName` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO `card_sales_invoices_temp` (`id`, `invoiceNumber`, `customerName`, `customerPhone`, `retailerId`, `invoiceDateMillis`, `paymentType`, `totalAmount`, `paidAmount`, `remainingAmount`, `totalCardsCount`, `itemsCount`, `itemsSummary`, `itemsJson`, `notes`, `issuerName`, `status`, `createdAt`)
                    SELECT `id`, `invoiceNumber`, `customerName`, `customerPhone`, `retailerId`, `invoiceDateMillis`, `paymentType`, CAST(`totalAmount` AS TEXT), CAST(`paidAmount` AS TEXT), CAST(`remainingAmount` AS TEXT), `totalCardsCount`, `itemsCount`, `itemsSummary`, `itemsJson`, `notes`, `issuerName`, `status`, `createdAt`
                    FROM `card_sales_invoices`
                """.trimIndent())
                db.execSQL("DROP TABLE `card_sales_invoices`")
                db.execSQL("ALTER TABLE `card_sales_invoices_temp` RENAME TO `card_sales_invoices`")

                // 4. purchase_invoices
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `purchase_invoices_temp` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `invoiceNumber` TEXT NOT NULL,
                        `supplierName` TEXT NOT NULL,
                        `invoiceDateMillis` INTEGER NOT NULL,
                        `targetType` TEXT NOT NULL,
                        `totalAmount` TEXT NOT NULL,
                        `currency` TEXT NOT NULL,
                        `originalAmount` TEXT NOT NULL,
                        `paidAmount` TEXT NOT NULL,
                        `paymentMethod` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `itemsSummary` TEXT NOT NULL,
                        `itemsJson` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO `purchase_invoices_temp` (`id`, `invoiceNumber`, `supplierName`, `invoiceDateMillis`, `targetType`, `totalAmount`, `currency`, `originalAmount`, `paidAmount`, `paymentMethod`, `status`, `notes`, `itemsSummary`, `itemsJson`, `createdAt`)
                    SELECT `id`, `invoiceNumber`, `supplierName`, `invoiceDateMillis`, `targetType`, CAST(`totalAmount` AS TEXT), `currency`, CAST(`originalAmount` AS TEXT), CAST(`paidAmount` AS TEXT), `paymentMethod`, `status`, `notes`, `itemsSummary`, `itemsJson`, `createdAt`
                    FROM `purchase_invoices`
                """.trimIndent())
                db.execSQL("DROP TABLE `purchase_invoices`")
                db.execSQL("ALTER TABLE `purchase_invoices_temp` RENAME TO `purchase_invoices`")

                // 5. partner_transactions
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `partner_transactions_temp` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `partnerId` INTEGER NOT NULL,
                        `partnerName` TEXT NOT NULL,
                        `transactionType` TEXT NOT NULL,
                        `amount` TEXT NOT NULL,
                        `currency` TEXT NOT NULL,
                        `originalAmount` TEXT NOT NULL,
                        `dateMillis` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL,
                        `referenceVoucherId` INTEGER
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO `partner_transactions_temp` (`id`, `partnerId`, `partnerName`, `transactionType`, `amount`, `currency`, `originalAmount`, `dateMillis`, `notes`, `referenceVoucherId`)
                    SELECT `id`, `partnerId`, `partnerName`, `transactionType`, CAST(`amount` AS TEXT), `currency`, CAST(`originalAmount` AS TEXT), `dateMillis`, `notes`, `referenceVoucherId`
                    FROM `partner_transactions`
                """.trimIndent())
                db.execSQL("DROP TABLE `partner_transactions`")
                db.execSQL("ALTER TABLE `partner_transactions_temp` RENAME TO `partner_transactions`")

                // 6. card_packages
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `card_packages_temp` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `retailPrice` TEXT NOT NULL,
                        `wholesalePrice` TEXT NOT NULL,
                        `quotaMb` INTEGER NOT NULL,
                        `validityHours` INTEGER NOT NULL,
                        `speedLimit` TEXT NOT NULL,
                        `mikrotikProfile` TEXT NOT NULL,
                        `colorTheme` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO `card_packages_temp` (`id`, `name`, `retailPrice`, `wholesalePrice`, `quotaMb`, `validityHours`, `speedLimit`, `mikrotikProfile`, `colorTheme`, `notes`, `createdAt`)
                    SELECT `id`, `name`, CAST(`retailPrice` AS TEXT), CAST(`wholesalePrice` AS TEXT), `quotaMb`, `validityHours`, `speedLimit`, `mikrotikProfile`, `colorTheme`, `notes`, `createdAt`
                    FROM `card_packages`
                """.trimIndent())
                db.execSQL("DROP TABLE `card_packages`")
                db.execSQL("ALTER TABLE `card_packages_temp` RENAME TO `card_packages`")
            }
        }

        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `customer_ledger` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `customerId` INTEGER NOT NULL,
                        `transactionDate` INTEGER NOT NULL,
                        `transactionType` TEXT NOT NULL,
                        `referenceId` TEXT NOT NULL,
                        `debit` TEXT NOT NULL,
                        `credit` TEXT NOT NULL,
                        `description` TEXT
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_customer_ledger_customerId` ON `customer_ledger` (`customerId`)")
            }
        }

        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `currency_rates` (
                        `currencyCode` TEXT NOT NULL,
                        `rateToBase` TEXT NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`currencyCode`)
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sam_mikrotik_db"
                )
                    .addMigrations(MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19)
                    .fallbackToDestructiveMigration()
                    .addCallback(AppDatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class AppDatabaseCallback : RoomDatabase.Callback() {
            private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                scope.launch {
                    try {
                        INSTANCE?.let { database ->
                            InitialDataSeeder.seedDatabase(database)
                            CustomerLedgerBackfillHelper.backfillIfEmpty(database)
                        }
                    } catch (e: Throwable) {
                        android.util.Log.e("AppDatabase", "Error seeding database on create: ${e.message}", e)
                    }
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                scope.launch {
                    try {
                        INSTANCE?.let { database ->
                            CustomerLedgerBackfillHelper.backfillIfEmpty(database)
                        }
                    } catch (e: Throwable) {
                        android.util.Log.e("AppDatabase", "Error backfilling ledger on open: ${e.message}", e)
                    }
                }
            }
        }
    }
}
