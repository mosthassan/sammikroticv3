package com.example.data.firebase

import android.util.Log
import com.example.data.local.entity.CardPackageEntity
import com.example.data.local.entity.CardSalesInvoiceEntity
import com.example.data.local.entity.FinancialVoucherEntity
import com.example.data.local.entity.InventoryItemEntity
import com.example.data.local.entity.NetworkDeviceEntity
import com.example.data.local.entity.NetworkIdentityEntity
import com.example.data.local.entity.RetailerEntity
import com.example.data.local.entity.UserEntity
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

data class CloudSyncResult(
    val success: Boolean,
    val message: String,
    val userPartition: String = "",
    val syncedDevicesCount: Int = 0,
    val syncedRetailersCount: Int = 0,
    val syncedVouchersCount: Int = 0,
    val pulledDevicesCount: Int = 0,
    val pulledRetailersCount: Int = 0,
    val pulledVouchersCount: Int = 0,
    val pulledPackagesCount: Int = 0,
    val pulledInventoryCount: Int = 0,
    val pulledInvoicesCount: Int = 0
)

data class CloudPullData(
    val devices: List<NetworkDeviceEntity> = emptyList(),
    val retailers: List<RetailerEntity> = emptyList(),
    val vouchers: List<FinancialVoucherEntity> = emptyList(),
    val cardPackages: List<CardPackageEntity> = emptyList(),
    val inventoryItems: List<InventoryItemEntity> = emptyList(),
    val salesInvoices: List<CardSalesInvoiceEntity> = emptyList(),
    val networkIdentity: NetworkIdentityEntity? = null
)

class FirebaseDbService {

    private val tag = "FirebaseDbService"

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Throwable) {
            Log.e(tag, "FirebaseFirestore unavailable: ${e.message}", e)
            null
        }
    }

    /**
     * Sanitizes email to be used as a document or collection path safely.
     * E.g. "mosthassan.ye2@gmail.com" -> "mosthassan_ye2_at_gmail_com"
     */
    private fun sanitizeEmail(email: String): String {
        return email.trim().lowercase()
            .replace("@", "_at_")
            .replace(".", "_")
            .replace("+", "_plus_")
    }

    private fun getDevicesRef(userEmail: String?): CollectionReference? {
        val fs = firestore ?: return null
        return if (!userEmail.isNullOrBlank()) {
            val userKey = sanitizeEmail(userEmail)
            fs.collection("users").document(userKey).collection("devices")
        } else {
            fs.collection("devices")
        }
    }

    private fun getRetailersRef(userEmail: String?): CollectionReference? {
        val fs = firestore ?: return null
        return if (!userEmail.isNullOrBlank()) {
            val userKey = sanitizeEmail(userEmail)
            fs.collection("users").document(userKey).collection("retailers")
        } else {
            fs.collection("retailers")
        }
    }

    private fun getVouchersRef(userEmail: String?): CollectionReference? {
        val fs = firestore ?: return null
        return if (!userEmail.isNullOrBlank()) {
            val userKey = sanitizeEmail(userEmail)
            fs.collection("users").document(userKey).collection("vouchers")
        } else {
            fs.collection("vouchers")
        }
    }

    private fun getCardPackagesRef(userEmail: String?): CollectionReference? {
        val fs = firestore ?: return null
        return if (!userEmail.isNullOrBlank()) {
            val userKey = sanitizeEmail(userEmail)
            fs.collection("users").document(userKey).collection("card_packages")
        } else {
            fs.collection("card_packages")
        }
    }

    private fun getInventoryItemsRef(userEmail: String?): CollectionReference? {
        val fs = firestore ?: return null
        return if (!userEmail.isNullOrBlank()) {
            val userKey = sanitizeEmail(userEmail)
            fs.collection("users").document(userKey).collection("inventory_items")
        } else {
            fs.collection("inventory_items")
        }
    }

    private fun getSalesInvoicesRef(userEmail: String?): CollectionReference? {
        val fs = firestore ?: return null
        return if (!userEmail.isNullOrBlank()) {
            val userKey = sanitizeEmail(userEmail)
            fs.collection("users").document(userKey).collection("card_sales_invoices")
        } else {
            fs.collection("card_sales_invoices")
        }
    }

    private fun getNetworkIdentityDocRef(userEmail: String?): DocumentReference? {
        val fs = firestore ?: return null
        return if (!userEmail.isNullOrBlank()) {
            val userKey = sanitizeEmail(userEmail)
            fs.collection("users").document(userKey).collection("settings").document("network_identity")
        } else {
            fs.collection("settings").document("network_identity")
        }
    }

    suspend fun pushNetworkIdentity(identity: NetworkIdentityEntity, userEmail: String? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            val docRef = getNetworkIdentityDocRef(userEmail) ?: return@withContext false
            val data = mapOf(
                "networkName" to identity.networkName,
                "ownerName" to identity.ownerName,
                "supportPhone" to identity.supportPhone,
                "supportWhatsapp" to identity.supportWhatsapp,
                "supportEmail" to identity.supportEmail,
                "networkLocation" to identity.networkLocation,
                "routerModel" to identity.routerModel,
                "routerOsVersion" to identity.routerOsVersion,
                "approvedDeviceSubnet" to identity.approvedDeviceSubnet,
                "gatewayIp" to identity.gatewayIp,
                "ipRangeStart" to identity.ipRangeStart,
                "ipRangeEnd" to identity.ipRangeEnd,
                "hotspotSubnet" to identity.hotspotSubnet,
                "hotspotGatewayIp" to identity.hotspotGatewayIp,
                "dnsServers" to identity.dnsServers,
                "welcomeNotice" to identity.welcomeNotice,
                "defaultCurrency" to identity.defaultCurrency,
                "sarToYerRate" to identity.sarToYerRate,
                "usdToYerRate" to identity.usdToYerRate,
                "usdToSarRate" to identity.usdToSarRate,
                "updatedAt" to identity.updatedAt,
                "ownerEmail" to (userEmail ?: "public")
            )
            setDocAsync(docRef, data)
            true
        } catch (e: Exception) {
            Log.e(tag, "Error pushing network identity to Firestore", e)
            false
        }
    }

    suspend fun syncAllToCloud(
        devices: List<NetworkDeviceEntity>,
        retailers: List<RetailerEntity>,
        vouchers: List<FinancialVoucherEntity>,
        userEmail: String? = null,
        networkIdentity: NetworkIdentityEntity? = null,
        cardPackages: List<CardPackageEntity> = emptyList(),
        inventoryItems: List<InventoryItemEntity> = emptyList(),
        salesInvoices: List<CardSalesInvoiceEntity> = emptyList()
    ): CloudSyncResult = withContext(Dispatchers.IO) {
        val fs = firestore ?: return@withContext CloudSyncResult(
            success = false,
            message = "خدمة Firebase غير مهيأة بعد"
        )
        try {
            var syncedDevices = 0
            var syncedRetailers = 0
            var syncedVouchers = 0
            var syncedPackages = 0
            var syncedInventory = 0
            var syncedInvoices = 0

            // Upload network identity if provided
            if (networkIdentity != null) {
                pushNetworkIdentity(networkIdentity, userEmail)
            }

            val devRef = getDevicesRef(userEmail)
            val retRef = getRetailersRef(userEmail)
            val vouchRef = getVouchersRef(userEmail)
            val pkgRef = getCardPackagesRef(userEmail)
            val invRef = getInventoryItemsRef(userEmail)
            val invSalesRef = getSalesInvoicesRef(userEmail)

            // Upload user profile meta if email exists
            if (!userEmail.isNullOrBlank()) {
                val userKey = sanitizeEmail(userEmail)
                val userDocRef = fs.collection("users").document(userKey)
                setDocAsync(
                    userDocRef,
                    mapOf(
                        "email" to userEmail,
                        "updatedAt" to System.currentTimeMillis(),
                        "devicesCount" to devices.size,
                        "retailersCount" to retailers.size,
                        "vouchersCount" to vouchers.size,
                        "packagesCount" to cardPackages.size,
                        "inventoryCount" to inventoryItems.size,
                        "salesInvoicesCount" to salesInvoices.size
                    )
                )
            }

            // Upload devices
            if (devRef != null) {
                for (device in devices) {
                    val docId = if (device.id > 0) device.id.toString() else (device.ipAddress.ifBlank { device.name })
                    val docRef = devRef.document(docId)
                    val data = mapOf(
                        "id" to device.id,
                        "name" to device.name,
                        "ipAddress" to device.ipAddress,
                        "macAddress" to device.macAddress,
                        "deviceType" to device.deviceType,
                        "locationArea" to device.locationArea,
                        "portOrInterface" to device.portOrInterface,
                        "frequencyOrSsid" to device.frequencyOrSsid,
                        "model" to device.model,
                        "username" to device.username,
                        "status" to device.status,
                        "signalDbm" to device.signalDbm,
                        "uptimeHours" to device.uptimeHours,
                        "notes" to device.notes,
                        "latitude" to device.latitude,
                        "longitude" to device.longitude,
                        "coverageRadiusMeters" to device.coverageRadiusMeters,
                        "parentDeviceId" to (device.parentDeviceId ?: 0L),
                        "createdAt" to device.createdAt,
                        "ownerEmail" to (userEmail ?: "public")
                    )
                    setDocAsync(docRef, data)
                    syncedDevices++
                }
            }

            // Upload retailers
            if (retRef != null) {
                for (retailer in retailers) {
                    val docId = if (retailer.id > 0) retailer.id.toString() else retailer.name
                    val docRef = retRef.document(docId)
                    val data = mapOf(
                        "id" to retailer.id,
                        "name" to retailer.name,
                        "ownerName" to retailer.ownerName,
                        "phone" to retailer.phone,
                        "location" to retailer.location,
                        "balanceOwed" to retailer.balanceOwed.toDouble(),
                        "totalPaid" to retailer.totalPaid.toDouble(),
                        "activeCardsCount" to retailer.activeCardsCount,
                        "commissionPercent" to retailer.commissionPercent,
                        "notes" to retailer.notes,
                        "createdAt" to retailer.createdAt,
                        "ownerEmail" to (userEmail ?: "public")
                    )
                    setDocAsync(docRef, data)
                    syncedRetailers++
                }
            }

            // Upload vouchers
            if (vouchRef != null) {
                val activeLocalNumbers = mutableSetOf<String>()
                for (voucher in vouchers) {
                    val vNum = voucher.voucherNumber.trim()
                    if (vNum.startsWith("REC-INV-") || com.example.data.local.DeletedRecordsTracker.isVoucherDeleted(vNum)) {
                        continue // NEVER upload legacy REC-INV- vouchers or deleted vouchers
                    }
                    activeLocalNumbers.add(vNum)
                    val docRef = vouchRef.document(vNum)
                    val data = mapOf(
                        "id" to voucher.id,
                        "voucherNumber" to voucher.voucherNumber,
                        "voucherType" to voucher.voucherType,
                        "amount" to voucher.amount.toDouble(),
                        "currency" to voucher.currency,
                        "originalAmount" to voucher.originalAmount.toDouble(),
                        "partyName" to voucher.partyName,
                        "retailerId" to (voucher.retailerId ?: 0L),
                        "invoiceId" to (voucher.invoiceId ?: 0L),
                        "invoiceNumber" to voucher.invoiceNumber,
                        "allocatedAmount" to voucher.allocatedAmount.toDouble(),
                        "category" to voucher.category,
                        "paymentMethod" to voucher.paymentMethod,
                        "description" to voucher.description,
                        "dateMillis" to voucher.dateMillis,
                        "issuerName" to voucher.issuerName,
                        "notes" to voucher.notes,
                        "isVoided" to voucher.isVoided,
                        "ownerEmail" to (userEmail ?: "public")
                    )
                    setDocAsync(docRef, data)
                    syncedVouchers++
                }

                // حذف السندات المحذوفة محلياً من السحابة لضمان عدم عودتها أبداً
                try {
                    val remoteDocs = getCollectionAsync(vouchRef)
                    remoteDocs?.documents?.forEach { rDoc ->
                        val remoteNum = (rDoc.getString("voucherNumber") ?: rDoc.id).trim()
                        if (remoteNum.startsWith("REC-INV-") ||
                            com.example.data.local.DeletedRecordsTracker.isVoucherDeleted(remoteNum) ||
                            (!activeLocalNumbers.contains(remoteNum) && vouchers.isNotEmpty())
                        ) {
                            deleteDocAsync(rDoc.reference)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(tag, "Pruning deleted vouchers note: ${e.message}")
                }
            }

            // Upload card packages
            if (pkgRef != null) {
                for (pkg in cardPackages) {
                    val docId = if (pkg.id > 0) pkg.id.toString() else pkg.name
                    val docRef = pkgRef.document(docId)
                    val data = mapOf(
                        "id" to pkg.id,
                        "name" to pkg.name,
                        "retailPrice" to pkg.retailPrice.toDouble(),
                        "wholesalePrice" to pkg.wholesalePrice.toDouble(),
                        "quotaMb" to pkg.quotaMb,
                        "validityHours" to pkg.validityHours,
                        "speedLimit" to pkg.speedLimit,
                        "mikrotikProfile" to pkg.mikrotikProfile,
                        "colorTheme" to pkg.colorTheme,
                        "notes" to pkg.notes,
                        "createdAt" to pkg.createdAt,
                        "ownerEmail" to (userEmail ?: "public")
                    )
                    setDocAsync(docRef, data)
                    syncedPackages++
                }
            }

            // Upload inventory items
            if (invRef != null) {
                for (item in inventoryItems) {
                    val docId = if (item.id > 0) item.id.toString() else item.packageName
                    val docRef = invRef.document(docId)
                    val data = mapOf(
                        "id" to item.id,
                        "packageName" to item.packageName,
                        "quantityAvailable" to item.quantityAvailable,
                        "wholesalePrice" to item.wholesalePrice.toDouble(),
                        "retailPrice" to item.retailPrice.toDouble(),
                        "costPrice" to item.costPrice.toDouble(),
                        "createdAt" to item.createdAt,
                        "ownerEmail" to (userEmail ?: "public")
                    )
                    setDocAsync(docRef, data)
                    syncedInventory++
                }
            }

            // Upload sales invoices
            if (invSalesRef != null) {
                for (invoice in salesInvoices) {
                    if (invoice.invoiceNumber.startsWith("INV-DELIV-")) {
                        continue // لا ترفع أي فواتير وهمية أو آلية تم إلغاؤها
                    }
                    val docRef = invSalesRef.document(invoice.invoiceNumber)
                    val data = mapOf(
                        "id" to invoice.id,
                        "invoiceNumber" to invoice.invoiceNumber,
                        "customerName" to invoice.customerName,
                        "customerPhone" to invoice.customerPhone,
                        "retailerId" to (invoice.retailerId ?: 0L),
                        "invoiceDateMillis" to invoice.invoiceDateMillis,
                        "paymentType" to invoice.paymentType,
                        "totalAmount" to invoice.totalAmount.toDouble(),
                        "paidAmount" to invoice.paidAmount.toDouble(),
                        "remainingAmount" to invoice.remainingAmount.toDouble(),
                        "totalCardsCount" to invoice.totalCardsCount,
                        "itemsCount" to invoice.itemsCount,
                        "itemsSummary" to invoice.itemsSummary,
                        "itemsJson" to invoice.itemsJson,
                        "notes" to invoice.notes,
                        "issuerName" to invoice.issuerName,
                        "status" to invoice.status,
                        "createdAt" to invoice.createdAt,
                        "ownerEmail" to (userEmail ?: "public")
                    )
                    setDocAsync(docRef, data)
                    syncedInvoices++
                }
            }

            val partitionLabel = if (!userEmail.isNullOrBlank()) "حساب $userEmail" else "المستودع العام"

            CloudSyncResult(
                success = true,
                message = "تمت المزامنة بنجاح مع Firebase ($partitionLabel)",
                userPartition = userEmail ?: "",
                syncedDevicesCount = syncedDevices,
                syncedRetailersCount = syncedRetailers,
                syncedVouchersCount = syncedVouchers,
                pulledPackagesCount = syncedPackages,
                pulledInventoryCount = syncedInventory,
                pulledInvoicesCount = syncedInvoices
            )
        } catch (e: Exception) {
            Log.e(tag, "Failed to sync with Firestore", e)
            CloudSyncResult(
                success = false,
                message = "خطأ بالمزامنة السحابية: ${e.localizedMessage ?: "تأكد من الاتصال بالإنترنت"}"
            )
        }
    }

    suspend fun pushDevice(device: NetworkDeviceEntity, userEmail: String? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            val docRef = getDevicesRef(userEmail)?.document(device.id.toString()) ?: return@withContext false
            val data = mapOf(
                "id" to device.id,
                "name" to device.name,
                "ipAddress" to device.ipAddress,
                "macAddress" to device.macAddress,
                "deviceType" to device.deviceType,
                "locationArea" to device.locationArea,
                "portOrInterface" to device.portOrInterface,
                "frequencyOrSsid" to device.frequencyOrSsid,
                "model" to device.model,
                "username" to device.username,
                "status" to device.status,
                "signalDbm" to device.signalDbm,
                "uptimeHours" to device.uptimeHours,
                "notes" to device.notes,
                "latitude" to device.latitude,
                "longitude" to device.longitude,
                "coverageRadiusMeters" to device.coverageRadiusMeters,
                "parentDeviceId" to (device.parentDeviceId ?: 0L),
                "createdAt" to device.createdAt,
                "ownerEmail" to (userEmail ?: "public")
            )
            setDocAsync(docRef, data)
            true
        } catch (e: Exception) {
            Log.e(tag, "Error pushing device to Firestore", e)
            false
        }
    }

    suspend fun deleteDevice(deviceId: Long, userEmail: String? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            val docRef = getDevicesRef(userEmail)?.document(deviceId.toString()) ?: return@withContext false
            deleteDocAsync(docRef)
            true
        } catch (e: Exception) {
            Log.e(tag, "Error deleting device from Firestore", e)
            false
        }
    }

    suspend fun pushRetailer(retailer: RetailerEntity, userEmail: String? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            val docRef = getRetailersRef(userEmail)?.document(retailer.id.toString()) ?: return@withContext false
            val data = mapOf(
                "id" to retailer.id,
                "name" to retailer.name,
                "ownerName" to retailer.ownerName,
                "phone" to retailer.phone,
                "location" to retailer.location,
                "balanceOwed" to retailer.balanceOwed.toDouble(),
                "totalPaid" to retailer.totalPaid.toDouble(),
                "activeCardsCount" to retailer.activeCardsCount,
                "commissionPercent" to retailer.commissionPercent,
                "notes" to retailer.notes,
                "createdAt" to retailer.createdAt,
                "ownerEmail" to (userEmail ?: "public")
            )
            setDocAsync(docRef, data)
            true
        } catch (e: Exception) {
            Log.e(tag, "Error pushing retailer to Firestore", e)
            false
        }
    }

    suspend fun deleteRetailer(retailerId: Long, userEmail: String? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            val docRef = getRetailersRef(userEmail)?.document(retailerId.toString()) ?: return@withContext false
            deleteDocAsync(docRef)
            true
        } catch (e: Exception) {
            Log.e(tag, "Error deleting retailer from Firestore", e)
            false
        }
    }

    suspend fun pushVoucher(voucher: FinancialVoucherEntity, userEmail: String? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            val docRef = getVouchersRef(userEmail)?.document(voucher.voucherNumber) ?: return@withContext false
            val data = mapOf(
                "id" to voucher.id,
                "voucherNumber" to voucher.voucherNumber,
                "voucherType" to voucher.voucherType,
                "amount" to voucher.amount.toDouble(),
                "currency" to voucher.currency,
                "originalAmount" to voucher.originalAmount.toDouble(),
                "partyName" to voucher.partyName,
                "retailerId" to (voucher.retailerId ?: 0L),
                "invoiceId" to (voucher.invoiceId ?: 0L),
                "invoiceNumber" to voucher.invoiceNumber,
                "allocatedAmount" to voucher.allocatedAmount.toDouble(),
                "category" to voucher.category,
                "paymentMethod" to voucher.paymentMethod,
                "description" to voucher.description,
                "dateMillis" to voucher.dateMillis,
                "issuerName" to voucher.issuerName,
                "notes" to voucher.notes,
                "isVoided" to voucher.isVoided,
                "ownerEmail" to (userEmail ?: "public")
            )
            setDocAsync(docRef, data)
            true
        } catch (e: Exception) {
            Log.e(tag, "Error pushing voucher to Firestore", e)
            false
        }
    }

    suspend fun deleteVoucher(voucherNumber: String, userEmail: String? = null): Boolean = withContext(Dispatchers.IO) {
        val cleanNum = voucherNumber.trim()
        if (cleanNum.isBlank()) return@withContext false
        com.example.data.local.DeletedRecordsTracker.markVoucherDeleted(cleanNum)
        try {
            val userDocRef = getVouchersRef(userEmail)?.document(cleanNum)
            if (userDocRef != null) deleteDocAsync(userDocRef)

            val globalDocRef = firestore?.collection("vouchers")?.document(cleanNum)
            if (globalDocRef != null) deleteDocAsync(globalDocRef)

            // تنظيف أي وثائق فرعية في حال كان المعرف مختلفاً عن رقم السند
            val userCol = getVouchersRef(userEmail)
            if (userCol != null) {
                val snap = getCollectionAsync(userCol)
                snap?.documents?.forEach { doc ->
                    val num = (doc.getString("voucherNumber") ?: doc.id).trim()
                    if (num.equals(cleanNum, ignoreCase = true)) {
                        deleteDocAsync(doc.reference)
                    }
                }
            }
            val fs = firestore
            if (fs != null) {
                val gSnap = getCollectionAsync(fs.collection("vouchers"))
                gSnap?.documents?.forEach { doc ->
                    val num = (doc.getString("voucherNumber") ?: doc.id).trim()
                    if (num.equals(cleanNum, ignoreCase = true)) {
                        deleteDocAsync(doc.reference)
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.e(tag, "Error deleting voucher from Firestore", e)
            false
        }
    }

    // ==========================================
    // Authorized Users & Distributors Cloud Sync
    // ==========================================

    private fun getAuthorizedUsersRef(): CollectionReference? {
        return firestore?.collection("authorized_users")
    }

    suspend fun pushAuthorizedUser(user: UserEntity, adminEmail: String? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            val ref = getAuthorizedUsersRef() ?: return@withContext false
            val emailKey = if (user.email.isNotBlank()) sanitizeEmail(user.email) else "user_${user.id}"
            val docRef = ref.document(emailKey)
            val data = mapOf(
                "id" to user.id,
                "username" to user.username,
                "fullName" to user.fullName,
                "role" to user.role,
                "email" to user.email.trim().lowercase(),
                "phone" to user.phone,
                "assignedArea" to user.assignedArea,
                "commissionRate" to user.commissionRate,
                "notes" to user.notes,
                "isActive" to user.isActive,
                "registeredByAdmin" to (adminEmail ?: "admin"),
                "createdAt" to user.createdAt
            )
            setDocAsync(docRef, data)
            true
        } catch (e: Exception) {
            Log.e(tag, "Error pushing authorized distributor to Firestore", e)
            false
        }
    }

    suspend fun deleteAuthorizedUser(userEmail: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (userEmail.isBlank()) return@withContext false
            val ref = getAuthorizedUsersRef() ?: return@withContext false
            val emailKey = sanitizeEmail(userEmail)
            val docRef = ref.document(emailKey)
            deleteDocAsync(docRef)
            true
        } catch (e: Exception) {
            Log.e(tag, "Error deleting authorized user from Firestore", e)
            false
        }
    }

    suspend fun fetchAuthorizedUserByEmail(email: String): UserEntity? = withContext(Dispatchers.IO) {
        if (email.isBlank()) return@withContext null
        val ref = getAuthorizedUsersRef() ?: return@withContext null
        try {
            val emailKey = sanitizeEmail(email)
            val snap = suspendCancellableCoroutine<com.google.firebase.firestore.DocumentSnapshot?> { cont ->
                ref.document(emailKey).get()
                    .addOnSuccessListener { snapshot ->
                        if (cont.isActive) cont.resume(snapshot)
                    }
                    .addOnFailureListener {
                        if (cont.isActive) cont.resume(null)
                    }
            }

            if (snap != null && snap.exists()) {
                val fullName = snap.getString("fullName") ?: ""
                val role = snap.getString("role") ?: "DISTRIBUTOR"
                val phone = snap.getString("phone") ?: ""
                val assignedArea = snap.getString("assignedArea") ?: ""
                val commissionRate = snap.getDouble("commissionRate") ?: 0.0
                val notes = snap.getString("notes") ?: ""
                val isActive = snap.getBoolean("isActive") ?: true

                return@withContext UserEntity(
                    username = email.substringBefore("@"),
                    fullName = fullName,
                    role = role,
                    email = email.trim().lowercase(),
                    phone = phone,
                    assignedArea = assignedArea,
                    commissionRate = commissionRate,
                    notes = notes,
                    isActive = isActive,
                    isGoogleUser = true
                )
            }
            null
        } catch (e: Exception) {
            Log.e(tag, "Error fetching authorized user from Firestore", e)
            null
        }
    }

    private suspend fun getCollectionAsync(
        colRef: CollectionReference
    ): QuerySnapshot? = suspendCancellableCoroutine { cont ->
        colRef.get()
            .addOnSuccessListener { snap ->
                if (cont.isActive) cont.resume(snap)
            }
            .addOnFailureListener { e ->
                Log.w(tag, "Firestore getCollection note: ${e.message}")
                if (cont.isActive) cont.resume(null)
            }
    }

    private suspend fun getDocumentAsync(
        docRef: DocumentReference
    ): com.google.firebase.firestore.DocumentSnapshot? = suspendCancellableCoroutine { cont ->
        docRef.get()
            .addOnSuccessListener { snap ->
                if (cont.isActive) cont.resume(snap)
            }
            .addOnFailureListener { e ->
                Log.w(tag, "Firestore getDocument note: ${e.message}")
                if (cont.isActive) cont.resume(null)
            }
    }

    /**
     * Pulls all synchronized data from Firestore into local models.
     * Supports fetching from the user-specific partition (users/{userKey}/...)
     * with transparent fallback/aggregation from the global root partition.
     */
    suspend fun pullFromCloud(userEmail: String? = null): CloudPullData = withContext(Dispatchers.IO) {
        val fs = firestore ?: return@withContext CloudPullData()

        val pulledDevices = mutableMapOf<String, NetworkDeviceEntity>()
        val pulledRetailers = mutableMapOf<String, RetailerEntity>()
        val pulledVouchers = mutableMapOf<String, FinancialVoucherEntity>()
        val pulledPackages = mutableMapOf<String, CardPackageEntity>()
        val pulledInventory = mutableMapOf<String, InventoryItemEntity>()
        val pulledInvoices = mutableMapOf<String, CardSalesInvoiceEntity>()
        var pulledIdentity: NetworkIdentityEntity? = null

        // 1. Fetch Network Identity
        try {
            val idDocRef = getNetworkIdentityDocRef(userEmail)
            var idSnap = idDocRef?.let { getDocumentAsync(it) }
            if ((idSnap == null || !idSnap.exists()) && userEmail.isNullOrBlank()) {
                // Fallback to global identity ONLY if user is not logged in (offline guest)
                val globalIdDoc = fs.collection("settings").document("network_identity")
                idSnap = getDocumentAsync(globalIdDoc)
            }
            if (idSnap != null && idSnap.exists()) {
                pulledIdentity = NetworkIdentityEntity(
                    id = 1L,
                    networkName = idSnap.getString("networkName") ?: "شبكة المايكروتك الذكية",
                    ownerName = idSnap.getString("ownerName") ?: (userEmail?.substringBefore("@") ?: "مالك الشبكة"),
                    supportPhone = idSnap.getString("supportPhone") ?: "770000000",
                    supportWhatsapp = idSnap.getString("supportWhatsapp") ?: "770000000",
                    supportEmail = idSnap.getString("supportEmail") ?: (userEmail ?: ""),
                    networkLocation = idSnap.getString("networkLocation") ?: "اليمن",
                    routerModel = idSnap.getString("routerModel") ?: "CCR2004-16G-2S+",
                    routerOsVersion = idSnap.getString("routerOsVersion") ?: "RouterOS v7.18",
                    approvedDeviceSubnet = idSnap.getString("approvedDeviceSubnet") ?: "192.168.88.0/24",
                    gatewayIp = idSnap.getString("gatewayIp") ?: "192.168.88.1",
                    ipRangeStart = idSnap.getString("ipRangeStart") ?: "192.168.88.2",
                    ipRangeEnd = idSnap.getString("ipRangeEnd") ?: "192.168.88.254",
                    hotspotSubnet = idSnap.getString("hotspotSubnet") ?: "10.10.10.0/24",
                    hotspotGatewayIp = idSnap.getString("hotspotGatewayIp") ?: "10.10.10.1",
                    dnsServers = idSnap.getString("dnsServers") ?: "8.8.8.8, 1.1.1.1",
                    welcomeNotice = idSnap.getString("welcomeNotice") ?: "مرحباً بكم في شبكة المايكروتك",
                    defaultCurrency = idSnap.getString("defaultCurrency") ?: "YER",
                    sarToYerRate = idSnap.getDouble("sarToYerRate") ?: 0.0,
                    usdToYerRate = idSnap.getDouble("usdToYerRate") ?: 0.0,
                    usdToSarRate = idSnap.getDouble("usdToSarRate") ?: 0.0,
                    updatedAt = idSnap.getLong("updatedAt") ?: System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "Error pulling network identity", e)
        }

        // Helper to query documents with strict user partition isolation
        suspend fun readCollections(
            userRef: CollectionReference?,
            globalRef: CollectionReference?,
            onDoc: (com.google.firebase.firestore.DocumentSnapshot) -> Unit
        ) {
            if (userRef != null) {
                // If user is authenticated, query ONLY their isolated partition.
                // Do NOT fall back to global collections so other tenants' data is NEVER leaked!
                val userSnap = getCollectionAsync(userRef)
                userSnap?.documents?.forEach { onDoc(it) }
            } else if (globalRef != null) {
                // Unauthenticated fallback only
                val globalSnap = getCollectionAsync(globalRef)
                globalSnap?.documents?.forEach { onDoc(it) }
            }
        }

        // 2. Fetch Devices
        try {
            readCollections(getDevicesRef(userEmail), fs.collection("devices")) { doc ->
                val ip = doc.getString("ipAddress") ?: ""
                val name = doc.getString("name") ?: "جهاز شبكة"
                val key = if (ip.isNotBlank()) ip else name
                pulledDevices[key] = NetworkDeviceEntity(
                    id = doc.getLong("id") ?: 0L,
                    name = name,
                    ipAddress = ip,
                    macAddress = doc.getString("macAddress") ?: "",
                    deviceType = doc.getString("deviceType") ?: "ACCESS_POINT",
                    locationArea = doc.getString("locationArea") ?: "",
                    portOrInterface = doc.getString("portOrInterface") ?: "",
                    frequencyOrSsid = doc.getString("frequencyOrSsid") ?: "",
                    model = doc.getString("model") ?: "",
                    username = doc.getString("username") ?: "admin",
                    status = doc.getString("status") ?: "ONLINE",
                    signalDbm = doc.getLong("signalDbm")?.toInt() ?: -60,
                    uptimeHours = doc.getLong("uptimeHours")?.toInt() ?: 24,
                    notes = doc.getString("notes") ?: "",
                    latitude = doc.getDouble("latitude") ?: 0.0,
                    longitude = doc.getDouble("longitude") ?: 0.0,
                    coverageRadiusMeters = doc.getLong("coverageRadiusMeters")?.toInt() ?: doc.getDouble("coverageRadiusMeters")?.toInt() ?: 120,
                    parentDeviceId = doc.getLong("parentDeviceId"),
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "Error pulling devices", e)
        }

        // 3. Fetch Retailers
        try {
            readCollections(getRetailersRef(userEmail), fs.collection("retailers")) { doc ->
                val rName = doc.getString("name") ?: "بقالة"
                pulledRetailers[rName] = RetailerEntity(
                    id = doc.getLong("id") ?: 0L,
                    name = rName,
                    ownerName = doc.getString("ownerName") ?: "",
                    phone = doc.getString("phone") ?: "",
                    location = doc.getString("location") ?: "",
                    balanceOwed = java.math.BigDecimal.valueOf(doc.getDouble("balanceOwed") ?: 0.0),
                    totalPaid = java.math.BigDecimal.valueOf(doc.getDouble("totalPaid") ?: 0.0),
                    activeCardsCount = doc.getLong("activeCardsCount")?.toInt() ?: 0,
                    commissionPercent = doc.getDouble("commissionPercent") ?: 10.0,
                    notes = doc.getString("notes") ?: "",
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "Error pulling retailers", e)
        }

        // 4. Fetch Vouchers
        try {
            readCollections(getVouchersRef(userEmail), fs.collection("vouchers")) { doc ->
                val vNumber = (doc.getString("voucherNumber") ?: doc.id).trim()
                if (vNumber.startsWith("REC-INV-") || doc.id.startsWith("REC-INV-")) {
                    try { doc.reference.delete() } catch (_: Exception) {}
                    return@readCollections
                }
                // التحقق مما إذا كان السند قد تم حذفه مسبقاً لمنع استعادته وحذفه من السحابة فوراً
                if (com.example.data.local.DeletedRecordsTracker.isVoucherDeleted(vNumber)) {
                    try { doc.reference.delete() } catch (_: Exception) {}
                    return@readCollections
                }
                val isVoided = doc.getBoolean("isVoided") ?: false
                val vCurrency = doc.getString("currency") ?: "YER"
                val invoiceIdVal = doc.getLong("invoiceId")?.takeIf { it != 0L }
                val invoiceNumberVal = doc.getString("invoiceNumber") ?: ""
                val allocatedAmountVal = java.math.BigDecimal.valueOf(doc.getDouble("allocatedAmount") ?: 0.0)
                val originalAmountVal = java.math.BigDecimal.valueOf(doc.getDouble("originalAmount") ?: 0.0)
                pulledVouchers[vNumber] = FinancialVoucherEntity(
                    id = doc.getLong("id") ?: 0L,
                    voucherNumber = vNumber,
                    voucherType = doc.getString("voucherType") ?: "RECEIPT",
                    amount = java.math.BigDecimal.valueOf(doc.getDouble("amount") ?: 0.0),
                    currency = vCurrency,
                    originalAmount = originalAmountVal,
                    partyName = doc.getString("partyName") ?: "",
                    retailerId = doc.getLong("retailerId")?.takeIf { it != 0L },
                    invoiceId = invoiceIdVal,
                    invoiceNumber = invoiceNumberVal,
                    allocatedAmount = allocatedAmountVal,
                    category = doc.getString("category") ?: "عام",
                    paymentMethod = doc.getString("paymentMethod") ?: "CASH",
                    description = doc.getString("description") ?: "",
                    dateMillis = doc.getLong("dateMillis") ?: System.currentTimeMillis(),
                    issuerName = doc.getString("issuerName") ?: "المهندس حسن",
                    notes = doc.getString("notes") ?: "",
                    isVoided = isVoided
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "Error pulling vouchers", e)
        }

        // 5. Fetch Card Packages
        try {
            readCollections(getCardPackagesRef(userEmail), fs.collection("card_packages")) { doc ->
                val pName = doc.getString("name") ?: doc.id
                pulledPackages[pName] = CardPackageEntity(
                    id = doc.getLong("id") ?: 0L,
                    name = pName,
                    retailPrice = java.math.BigDecimal.valueOf(doc.getDouble("retailPrice") ?: 0.0),
                    wholesalePrice = java.math.BigDecimal.valueOf(doc.getDouble("wholesalePrice") ?: 0.0),
                    quotaMb = doc.getLong("quotaMb") ?: 2500L,
                    validityHours = doc.getLong("validityHours")?.toInt() ?: 24,
                    speedLimit = doc.getString("speedLimit") ?: "4M/2M",
                    mikrotikProfile = doc.getString("mikrotikProfile") ?: "default",
                    colorTheme = doc.getString("colorTheme") ?: "cyan",
                    notes = doc.getString("notes") ?: "",
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "Error pulling packages", e)
        }

        // 6. Fetch Inventory Items
        try {
            readCollections(getInventoryItemsRef(userEmail), fs.collection("inventory_items")) { doc ->
                val pkgName = doc.getString("packageName") ?: doc.id
                pulledInventory[pkgName] = InventoryItemEntity(
                    id = doc.getLong("id") ?: 0L,
                    packageName = pkgName,
                    quantityAvailable = doc.getLong("quantityAvailable")?.toInt() ?: 0,
                    wholesalePrice = doc.getDouble("wholesalePrice") ?: 0.0,
                    retailPrice = doc.getDouble("retailPrice") ?: 0.0,
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "Error pulling inventory items", e)
        }

        // 7. Fetch Sales Invoices
        try {
            readCollections(getSalesInvoicesRef(userEmail), fs.collection("card_sales_invoices")) { doc ->
                val invNum = doc.getString("invoiceNumber") ?: doc.id
                if (invNum.startsWith("INV-DELIV-")) {
                    // حذف وتجاهل أي فاتورة تسليم وهمية سابقة من السحابة
                    try { doc.reference.delete() } catch (_: Exception) {}
                    return@readCollections
                }
                pulledInvoices[invNum] = CardSalesInvoiceEntity(
                    id = doc.getLong("id") ?: 0L,
                    invoiceNumber = invNum,
                    customerName = doc.getString("customerName") ?: "",
                    customerPhone = doc.getString("customerPhone") ?: "",
                    retailerId = doc.getLong("retailerId")?.takeIf { it != 0L },
                    invoiceDateMillis = doc.getLong("invoiceDateMillis") ?: System.currentTimeMillis(),
                    paymentType = doc.getString("paymentType") ?: "CASH",
                    totalAmount = java.math.BigDecimal.valueOf(doc.getDouble("totalAmount") ?: 0.0),
                    paidAmount = java.math.BigDecimal.valueOf(doc.getDouble("paidAmount") ?: 0.0),
                    remainingAmount = java.math.BigDecimal.valueOf(doc.getDouble("remainingAmount") ?: 0.0),
                    totalCardsCount = doc.getLong("totalCardsCount")?.toInt() ?: 0,
                    itemsCount = doc.getLong("itemsCount")?.toInt() ?: 0,
                    itemsSummary = doc.getString("itemsSummary") ?: "",
                    itemsJson = doc.getString("itemsJson") ?: "[]",
                    notes = doc.getString("notes") ?: "",
                    issuerName = doc.getString("issuerName") ?: "المهندس حسن",
                    status = doc.getString("status") ?: "PAID",
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "Error pulling sales invoices", e)
        }

        CloudPullData(
            devices = pulledDevices.values.toList(),
            retailers = pulledRetailers.values.toList(),
            vouchers = pulledVouchers.values.toList(),
            cardPackages = pulledPackages.values.toList(),
            inventoryItems = pulledInventory.values.toList(),
            salesInvoices = pulledInvoices.values.toList(),
            networkIdentity = pulledIdentity
        )
    }

    private suspend fun setDocAsync(
        docRef: com.google.firebase.firestore.DocumentReference,
        data: Map<String, Any>
    ): Unit = suspendCancellableCoroutine { continuation ->
        docRef.set(data, SetOptions.merge())
            .addOnSuccessListener {
                if (continuation.isActive) continuation.resume(Unit)
            }
            .addOnFailureListener { exception ->
                Log.w(tag, "Firestore write note: ${exception.message}")
                if (continuation.isActive) continuation.resume(Unit)
            }
    }

    suspend fun clearUserCloudPartition(userEmail: String): Boolean = withContext(Dispatchers.IO) {
        if (userEmail.isBlank()) return@withContext false
        val fs = firestore ?: return@withContext false
        val userKey = sanitizeEmail(userEmail)
        val userDoc = fs.collection("users").document(userKey)
        val collections = listOf("devices", "retailers", "vouchers", "card_packages", "inventory_items", "card_sales_invoices")
        try {
            for (col in collections) {
                val snap = getCollectionAsync(userDoc.collection(col))
                snap?.documents?.forEach { doc ->
                    try { doc.reference.delete() } catch (_: Exception) {}
                }
            }
            try {
                userDoc.collection("settings").document("network_identity").delete()
            } catch (_: Exception) {}
            try { userDoc.delete() } catch (_: Exception) {}
            Log.i(tag, "Successfully cleared cloud partition for $userEmail")
            true
        } catch (e: Exception) {
            Log.e(tag, "Error clearing cloud partition for $userEmail", e)
            false
        }
    }

    private suspend fun deleteDocAsync(
        docRef: com.google.firebase.firestore.DocumentReference
    ): Unit = suspendCancellableCoroutine { continuation ->
        docRef.delete()
            .addOnSuccessListener {
                if (continuation.isActive) continuation.resume(Unit)
            }
            .addOnFailureListener { exception ->
                Log.w(tag, "Firestore delete note: ${exception.message}")
                if (continuation.isActive) continuation.resume(Unit)
            }
    }
}
