package com.example.data.repository

import com.example.data.firebase.CloudPullData
import com.example.data.firebase.CloudSyncResult
import com.example.data.firebase.FirebaseDbService
import com.example.data.local.entity.CardPackageEntity
import com.example.data.local.entity.CardSalesInvoiceEntity
import com.example.data.local.entity.FinancialVoucherEntity
import com.example.data.local.entity.InventoryItemEntity
import com.example.data.local.entity.NetworkDeviceEntity
import com.example.data.local.entity.NetworkIdentityEntity
import com.example.data.local.entity.RetailerEntity

class FirebaseRepository(private val firebaseDbService: FirebaseDbService = FirebaseDbService()) {

    /**
     * Synchronizes local entities to Firebase Cloud.
     * Automatically filters out any records where referenceId or voucherNumber starts with `REC-INV-`.
     */
    suspend fun syncAllToCloud(
        devices: List<NetworkDeviceEntity>,
        retailers: List<RetailerEntity>,
        vouchers: List<FinancialVoucherEntity>,
        userEmail: String? = null,
        networkIdentity: NetworkIdentityEntity? = null,
        cardPackages: List<CardPackageEntity> = emptyList(),
        inventoryItems: List<InventoryItemEntity> = emptyList(),
        salesInvoices: List<CardSalesInvoiceEntity> = emptyList()
    ): CloudSyncResult {
        val cleanVouchers = vouchers.filter { !it.voucherNumber.startsWith("REC-INV-") }
        val cleanInvoices = salesInvoices.filter { !it.invoiceNumber.startsWith("REC-INV-") }

        return firebaseDbService.syncAllToCloud(
            devices = devices,
            retailers = retailers,
            vouchers = cleanVouchers,
            userEmail = userEmail,
            networkIdentity = networkIdentity,
            cardPackages = cardPackages,
            inventoryItems = inventoryItems,
            salesInvoices = cleanInvoices
        )
    }

    /**
     * Pulls synchronized entities from Firebase Cloud.
     * Automatically filters out and ignores any records where referenceId or voucherNumber starts with `REC-INV-`.
     */
    suspend fun pullFromCloud(userEmail: String? = null): CloudPullData {
        val rawData = firebaseDbService.pullFromCloud(userEmail)
        val cleanVouchers = rawData.vouchers.filter { !it.voucherNumber.startsWith("REC-INV-") }
        val cleanInvoices = rawData.salesInvoices.filter { !it.invoiceNumber.startsWith("REC-INV-") }

        return rawData.copy(
            vouchers = cleanVouchers,
            salesInvoices = cleanInvoices
        )
    }

    suspend fun pushVoucher(voucher: FinancialVoucherEntity, userEmail: String? = null): Boolean {
        if (voucher.voucherNumber.startsWith("REC-INV-")) {
            return false // Filter out legacy REC-INV-
        }
        return firebaseDbService.pushVoucher(voucher, userEmail)
    }
}
