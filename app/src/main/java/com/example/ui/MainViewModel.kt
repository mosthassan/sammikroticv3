package com.example.ui

import android.app.Activity
import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import java.math.BigDecimal
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.GeminiAiService
import com.example.data.auth.GoogleAuthManager
import com.example.data.firebase.FirebaseDbService
import com.example.data.local.AppDatabase
import com.example.data.local.entity.CardBatchEntity
import com.example.data.local.entity.CardEntity
import com.example.data.local.entity.CardPackageEntity
import com.example.data.local.entity.CardSalesInvoiceEntity
import com.example.data.local.entity.ChartOfAccountsEntity
import com.example.data.local.entity.FinancialVoucherEntity
import com.example.data.local.entity.InventoryItemEntity
import com.example.data.local.entity.InventoryMovementEntity
import com.example.data.local.entity.JournalEntryEntity
import com.example.data.local.entity.JournalEntryHeaderEntity
import com.example.data.local.entity.JournalEntryLineEntity
import com.example.data.local.entity.NetworkAssetEntity
import com.example.data.local.entity.NetworkDeviceEntity
import com.example.data.local.entity.NetworkIdentityEntity
import com.example.data.local.entity.PartnerEntity
import com.example.data.local.entity.PartnerTransactionEntity
import com.example.data.local.entity.PurchaseInvoiceEntity
import com.example.data.local.entity.RetailerEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.model.JournalEntryWithLines
import com.example.data.local.model.TrialBalanceRow
import com.example.data.local.model.GeneralLedgerReport
import com.example.data.local.model.IncomeStatementReport
import kotlinx.coroutines.flow.Flow
import com.example.data.model.CardSalesInvoiceItem
import com.example.data.model.InvoiceItem
import com.example.data.model.ParsedInvoiceData
import com.example.data.model.PeriodClosingSummary
import com.example.data.model.AssetDepreciationResult
import com.example.data.repository.NetworkRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class IpValidationResult(
    val hasConflict: Boolean = false,
    val conflictingDeviceName: String? = null,
    val conflictingDeviceLocation: String? = null,
    val isOutsideSubnet: Boolean = false,
    val approvedSubnet: String = "192.168.88.0/24"
)

data class GoogleSignInUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isMissingClientId: Boolean = false,
    val activeFirebaseEmail: String? = null,
    val activeFirebaseUid: String? = null,
    val activeAuthProvider: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = NetworkRepository(db)
    private val aiService = GeminiAiService(application.applicationContext)
    private val firebaseService = FirebaseDbService()
    val authManager = GoogleAuthManager(application)

    fun isAiApiKeyConfigured(): Boolean = aiService.isApiKeyConfigured()
    fun getAiApiKey(): String = aiService.getActiveApiKey()
    fun setAiApiKey(key: String) = aiService.setCustomApiKey(key)
    fun clearAiApiKey() = aiService.clearCustomApiKey()

    // Google Sign-In state
    private val _googleSignInState = MutableStateFlow(
        GoogleSignInUiState(
            activeFirebaseEmail = authManager.getActiveEmail(),
            activeFirebaseUid = authManager.getActiveUid(),
            activeAuthProvider = if (authManager.isUserLoggedIn()) "Firebase" else null
        )
    )
    val googleSignInState: StateFlow<GoogleSignInUiState> = _googleSignInState.asStateFlow()

    // Active User Role
    val users: StateFlow<List<UserEntity>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val distributors: StateFlow<List<UserEntity>> = repository.allDistributors
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    fun selectUser(user: UserEntity) {
        _currentUser.value = user
    }

    fun saveDistributor(
        distributor: UserEntity,
        onComplete: () -> Unit = {}
    ) {
        onComplete()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val normalizedUser = distributor.copy(
                    role = "DISTRIBUTOR",
                    email = distributor.email.trim().lowercase(),
                    username = if (distributor.username.isBlank()) {
                        distributor.email.substringBefore("@").ifBlank { "dist_${System.currentTimeMillis()}" }
                    } else distributor.username
                )
                val id = repository.saveUser(normalizedUser)
                val updated = if (distributor.id == 0L) normalizedUser.copy(id = id) else normalizedUser

                // Push to cloud authorized_users directory
                val adminEmail = _currentUser.value?.email?.takeIf { it.isNotBlank() }
                firebaseService.pushAuthorizedUser(updated, adminEmail)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error saving distributor in background", e)
            }
        }
    }

    fun deleteDistributor(distributor: UserEntity, onComplete: () -> Unit = {}) {
        onComplete()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deleteUser(distributor)
                if (distributor.email.isNotBlank()) {
                    firebaseService.deleteAuthorizedUser(distributor.email)
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error deleting distributor in background", e)
            }
        }
    }

    fun toggleDistributorActive(distributor: UserEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val updated = distributor.copy(isActive = !distributor.isActive)
                repository.saveUser(updated)
                val adminEmail = _currentUser.value?.email?.takeIf { it.isNotBlank() }
                firebaseService.pushAuthorizedUser(updated, adminEmail)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error toggling distributor active in background", e)
            }
        }
    }

    // Google One-Tap & Authentication Dialog state
    /**
     * Genuine Google Sign-In through Android Credential Manager and Firebase Authentication.
     */
    fun signInWithGoogle(activity: Activity? = null, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _googleSignInState.value = _googleSignInState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )
            val result = authManager.signInWithGoogle(activity = activity)
            if (result.success && !result.email.isNullOrBlank()) {
                handleUserSignInSuccess(
                    email = result.email,
                    displayName = result.displayName,
                    photoUrl = result.photoUrl,
                    uid = result.uid,
                    authProvider = result.authProvider
                )
                onComplete(true)
            } else if (result.isCancelled) {
                _googleSignInState.value = _googleSignInState.value.copy(
                    isLoading = false,
                    errorMessage = null
                )
                onComplete(false)
            } else if (result.isMissingClientId) {
                _googleSignInState.value = _googleSignInState.value.copy(
                    isLoading = false,
                    isMissingClientId = true,
                    errorMessage = result.errorMessage
                )
                onComplete(false)
            } else {
                _googleSignInState.value = _googleSignInState.value.copy(
                    isLoading = false,
                    errorMessage = result.errorMessage ?: "تعذر إكمال تسجيل الدخول عبر Google"
                )
                onComplete(false)
            }
        }
    }

    fun signOutGoogle() {
        viewModelScope.launch {
            authManager.signOut()
            // تفريغ البيانات المحلية التشغيلية لمنع تسريب بيانات الحساب السابق إلى أي جلسة لاحقة
            repository.purgeAllOperationalData()
            prefs.edit().remove("last_active_email").putBoolean("is_production_mode", false).apply()
            _currentUser.value = null
            _isProductionMode.value = false
            _googleSignInState.value = GoogleSignInUiState(
                successMessage = "تم تسجيل الخروج وتجهيز التطبيق لحساب مستخدم جديد ✓"
            )
        }
    }

    fun signInWithEmail(email: String, pass: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _googleSignInState.value = _googleSignInState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )
            val result = authManager.signInWithEmailAndPassword(email, pass)
            if (result.success && !result.email.isNullOrBlank()) {
                handleUserSignInSuccess(
                    email = result.email,
                    displayName = result.displayName,
                    photoUrl = "",
                    uid = result.uid,
                    authProvider = "password"
                )
                onComplete(true)
            } else {
                _googleSignInState.value = _googleSignInState.value.copy(
                    isLoading = false,
                    errorMessage = result.errorMessage ?: "تعذر تسجيل الدخول بالبريد"
                )
                onComplete(false)
            }
        }
    }

    fun signUpWithEmail(email: String, pass: String, name: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _googleSignInState.value = _googleSignInState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )
            val result = authManager.signUpWithEmailAndPassword(email, pass, name)
            if (result.success && !result.email.isNullOrBlank()) {
                handleUserSignInSuccess(
                    email = result.email,
                    displayName = name.ifBlank { result.displayName },
                    photoUrl = "",
                    uid = result.uid,
                    authProvider = "password"
                )
                onComplete(true)
            } else {
                _googleSignInState.value = _googleSignInState.value.copy(
                    isLoading = false,
                    errorMessage = result.errorMessage ?: "تعذر إنشاء الحساب"
                )
                onComplete(false)
            }
        }
    }

    /**
     * معالجة دخول مستخدم جديد أو التبديل بين الحسابات لضمان العزل التام للبيانات كمنتج رقمي
     */
    private suspend fun handleUserSignInSuccess(
        email: String,
        displayName: String?,
        photoUrl: String?,
        uid: String?,
        authProvider: String
    ) {
        val cleanEmail = email.trim().lowercase()
        val lastActiveEmail = prefs.getString("last_active_email", null)?.trim()?.lowercase()
        val isDifferentAccount = lastActiveEmail != null && lastActiveEmail != cleanEmail

        val registeredUser = if (isDifferentAccount || lastActiveEmail == null) {
            // حساب جديد أو تبديل مستخدم: تهيئة مساحة عمل معزولة خاصة به كمالك للشبكة
            repository.switchUserWorkspace(
                email = cleanEmail,
                displayName = displayName ?: cleanEmail.substringBefore("@"),
                photoUrl = photoUrl ?: ""
            )
        } else {
            repository.registerOrUpdateGoogleUser(
                email = cleanEmail,
                displayName = displayName ?: cleanEmail.substringBefore("@"),
                photoUrl = photoUrl ?: ""
            )
        }

        prefs.edit().putString("last_active_email", cleanEmail).apply()
        _currentUser.value = registeredUser

        // سحب بيانات هذا الحساب فقط من السحابة إذا كان لديه سحابة سابقة
        val cloudData = firebaseService.pullFromCloud(cleanEmail)
        val hasCloudData = cloudData.devices.isNotEmpty() || cloudData.retailers.isNotEmpty() ||
                cloudData.vouchers.isNotEmpty() || cloudData.cardPackages.isNotEmpty() ||
                cloudData.inventoryItems.isNotEmpty() || cloudData.salesInvoices.isNotEmpty()

        if (hasCloudData) {
            repository.restoreFromCloudData(cloudData)
            _isProductionMode.value = true
        } else {
            // حساب جديد تماماً: لا بيانات سابقة، ويظهر زر التهيئة ليخصص شبكته
            _isProductionMode.value = false
        }

        _googleSignInState.value = GoogleSignInUiState(
            isLoading = false,
            successMessage = "تم تسجيل الدخول بنجاح بحساب $cleanEmail كمالك للشبكة ✓",
            activeFirebaseEmail = cleanEmail,
            activeFirebaseUid = uid,
            activeAuthProvider = authProvider
        )
    }

    fun dismissGoogleMessage() {
        _googleSignInState.value = _googleSignInState.value.copy(errorMessage = null, successMessage = null)
    }

    // Reset To Production Environment (حذف كافة البيانات وتصفير التطبيق للبدء من الصفر كمالك جديد)
    private val prefs = application.getSharedPreferences("sam_mikrotik_prefs", android.content.Context.MODE_PRIVATE)

    private val _isProductionMode = MutableStateFlow(
        prefs.getBoolean("is_production_mode", false)
    )
    val isProductionMode: StateFlow<Boolean> = _isProductionMode.asStateFlow()

    private val _resetProductionState = MutableStateFlow<String?>(null)
    val resetProductionState: StateFlow<String?> = _resetProductionState.asStateFlow()

    fun resetToProductionEnvironment(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val activeUser = currentUser.value
            val activeEmail = activeUser?.email?.takeIf { it.isNotBlank() } ?: authManager.getActiveEmail()

            // تنظيف مساحة السحابة لهذا الحساب لضمان تصفير تام
            if (!activeEmail.isNullOrBlank()) {
                firebaseService.clearUserCloudPartition(activeEmail)
            }

            val productionAdmin = repository.resetToProductionEnvironment(
                activeGoogleUser = if (activeUser?.isGoogleUser == true) activeUser else null
            )
            _currentUser.value = productionAdmin
            prefs.edit().putBoolean("is_production_mode", true).apply()
            _isProductionMode.value = true
            _resetProductionState.value = "تمت تهيئة النظام بنجاح وتصفير كافة الكروت والسندات والأجهزة للبدء من الصفر كمالك جديد."
            onComplete()
        }
    }

    fun dismissResetMessage() {
        _resetProductionState.value = null
    }

    // Devices
    val devices: StateFlow<List<NetworkDeviceEntity>> = repository.allDevices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deviceCount: StateFlow<Int> = repository.deviceCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // IP Conflict and Subnet check
    private val _ipValidation = MutableStateFlow(IpValidationResult())
    val ipValidation: StateFlow<IpValidationResult> = _ipValidation.asStateFlow()

    // Network Identity & Configuration State
    val networkIdentity: StateFlow<NetworkIdentityEntity> = repository.networkIdentity
        .map { it ?: NetworkIdentityEntity() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, NetworkIdentityEntity())

    val sarToYerRate: StateFlow<Double> = networkIdentity
        .map { if (it.sarToYerRate > 0) it.sarToYerRate else 140.0 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 140.0)

    val usdToYerRate: StateFlow<Double> = networkIdentity
        .map { if (it.usdToYerRate > 0) it.usdToYerRate else 530.0 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 530.0)

    fun convertToYer(amount: Double, fromCurrency: String): Double {
        val identity = networkIdentity.value
        return com.example.util.CurrencyHelper.convertToYer(
            amount,
            fromCurrency,
            if (identity.sarToYerRate > 0) identity.sarToYerRate else 140.0,
            if (identity.usdToYerRate > 0) identity.usdToYerRate else 530.0
        )
    }

    fun reconcileAccountingLedger(onComplete: () -> Unit = {}) {
        onComplete()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.reconcileAccountingLedger()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error reconciling accounting ledger in background", e)
            }
        }
    }

    fun saveNetworkIdentity(identity: NetworkIdentityEntity, onComplete: () -> Unit = {}) {
        onComplete()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.saveNetworkIdentity(identity)
                val userEmail = _currentUser.value?.email?.takeIf { it.isNotBlank() }
                firebaseService.pushNetworkIdentity(identity, userEmail)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error saving network identity in background", e)
            }
        }
    }

    fun resetNetworkIdentityToDefault(onComplete: () -> Unit = {}) {
        onComplete()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val defaultIdentity = NetworkIdentityEntity(
                    id = 1L,
                    networkName = "شبكة سام ميكروتك الذكية",
                    ownerName = _currentUser.value?.fullName ?: "المهندس سام",
                    supportPhone = "770000001",
                    supportWhatsapp = "967770000001",
                    supportEmail = _currentUser.value?.email?.takeIf { it.isNotBlank() } ?: "support@sam-mikrotic.ye",
                    networkLocation = "اليمن - صنعاء - السبعين",
                    routerModel = "MikroTik CCR2004-16G-2S+",
                    routerOsVersion = "RouterOS v7.15",
                    approvedDeviceSubnet = "192.168.88.0/24",
                    gatewayIp = "192.168.88.1",
                    ipRangeStart = "192.168.88.2",
                    ipRangeEnd = "192.168.88.254",
                    hotspotSubnet = "10.5.50.0/24",
                    hotspotGatewayIp = "10.5.50.1",
                    dnsServers = "8.8.8.8, 1.1.1.1",
                    welcomeNotice = "أهلاً بكم في شبكة سام اللاسلكية - إنترنت فائق السرعة واستقرار دائم"
                )
                repository.saveNetworkIdentity(defaultIdentity)
                val userEmail = _currentUser.value?.email?.takeIf { it.isNotBlank() }
                firebaseService.pushNetworkIdentity(defaultIdentity, userEmail)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error resetting network identity in background", e)
            }
        }
    }

    fun validateIp(ip: String, excludeDeviceId: Long? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val cleanIp = ip.trim()
            val currentIdentity = networkIdentity.value
            if (cleanIp.isEmpty()) {
                _ipValidation.value = IpValidationResult(false, approvedSubnet = currentIdentity.approvedDeviceSubnet)
                return@launch
            }
            val isOutside = !repository.isIpInApprovedSubnet(cleanIp, currentIdentity.approvedDeviceSubnet)
            val conflict = repository.checkIpConflict(cleanIp, excludeDeviceId)
            if (conflict != null) {
                _ipValidation.value = IpValidationResult(
                    hasConflict = true,
                    conflictingDeviceName = conflict.name,
                    conflictingDeviceLocation = conflict.locationArea,
                    isOutsideSubnet = isOutside,
                    approvedSubnet = currentIdentity.approvedDeviceSubnet
                )
            } else {
                _ipValidation.value = IpValidationResult(
                    hasConflict = false,
                    isOutsideSubnet = isOutside,
                    approvedSubnet = currentIdentity.approvedDeviceSubnet
                )
            }
        }
    }

    fun clearIpValidation() {
        _ipValidation.value = IpValidationResult(false, approvedSubnet = networkIdentity.value.approvedDeviceSubnet)
    }

    suspend fun getSuggestedIp(): String {
        return repository.suggestNextAvailableIp()
    }

    fun saveDevice(device: NetworkDeviceEntity, onComplete: () -> Unit = {}) {
        onComplete()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val id = repository.saveDevice(device)
                val updated = if (device.id == 0L) device.copy(id = id) else device
                val userEmail = _currentUser.value?.email?.takeIf { it.isNotBlank() }
                firebaseService.pushDevice(updated, userEmail)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error saving device in background", e)
            }
        }
    }

    fun importDevicesBatch(
        devicesToImport: List<NetworkDeviceEntity>,
        replaceExisting: Boolean = false,
        onComplete: (importedCount: Int) -> Unit
    ) {
        onComplete(devicesToImport.size)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (replaceExisting) {
                    devices.value.forEach { repository.deleteDevice(it) }
                }
                val userEmail = _currentUser.value?.email?.takeIf { it.isNotBlank() }
                devicesToImport.forEach { dev ->
                    val cleanDev = dev.copy(id = 0)
                    val newId = repository.saveDevice(cleanDev)
                    firebaseService.pushDevice(cleanDev.copy(id = newId), userEmail)
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error importing devices in background", e)
            }
        }
    }

    fun importRetailersBatch(
        retailersToImport: List<RetailerEntity>,
        replaceExisting: Boolean = false,
        onComplete: (importedCount: Int) -> Unit
    ) {
        onComplete(retailersToImport.size)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val count = repository.importRetailersBatch(retailersToImport, replaceExisting)
                val userEmail = _currentUser.value?.email?.takeIf { it.isNotBlank() }
                retailersToImport.forEach { r ->
                    firebaseService.pushRetailer(r, userEmail)
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error importing retailers in background", e)
            }
        }
    }

    fun importPurchaseInvoicesBatch(
        invoicesToImport: List<PurchaseInvoiceEntity>,
        replaceExisting: Boolean = false,
        onComplete: (importedCount: Int) -> Unit
    ) {
        onComplete(invoicesToImport.size)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.importPurchaseInvoicesBatch(invoicesToImport, replaceExisting)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error importing purchase invoices in background", e)
            }
        }
    }

    fun importSalesInvoicesBatch(
        invoicesToImport: List<CardSalesInvoiceEntity>,
        replaceExisting: Boolean = false,
        onComplete: (importedCount: Int) -> Unit
    ) {
        onComplete(invoicesToImport.size)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.importSalesInvoicesBatch(invoicesToImport, replaceExisting)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error importing sales invoices in background", e)
            }
        }
    }

    fun updateDeviceCoordinates(
        deviceId: Long,
        latitude: Double,
        longitude: Double,
        coverageRadiusMeters: Int? = null,
        parentDeviceId: Long? = null,
        onComplete: () -> Unit = {}
    ) {
        onComplete()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentDevice = devices.value.firstOrNull { it.id == deviceId } ?: return@launch
                val updated = currentDevice.copy(
                    latitude = latitude,
                    longitude = longitude,
                    coverageRadiusMeters = coverageRadiusMeters ?: currentDevice.coverageRadiusMeters,
                    parentDeviceId = parentDeviceId ?: currentDevice.parentDeviceId
                )
                repository.saveDevice(updated)
                val userEmail = _currentUser.value?.email?.takeIf { it.isNotBlank() }
                firebaseService.pushDevice(updated, userEmail)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error updating device coordinates in background", e)
            }
        }
    }

    fun deleteDevice(device: NetworkDeviceEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deleteDevice(device)
                val userEmail = _currentUser.value?.email?.takeIf { it.isNotBlank() }
                firebaseService.deleteDevice(device.id, userEmail)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error deleting device in background", e)
            }
        }
    }
    // Inventory Management (مخزن الكروت بالعدد والأصناف)
    val inventoryItems: StateFlow<List<InventoryItemEntity>> = repository.allInventoryItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val inventoryMovements: StateFlow<List<InventoryMovementEntity>> = repository.allInventoryMovements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * إضافة أو توريد كروت للمخزن بالعدد فقط بدون إنشاء أو توليد أرقام كروت
     */
    fun addStockToInventory(
        packageName: String,
        quantity: Int,
        wholesalePrice: Double = 0.0,
        retailPrice: Double = 0.0,
        notes: String = "",
        onComplete: (Long) -> Unit = {}
    ) {
        onComplete(quantity.toLong())
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.addStockToInventory(
                    packageName = packageName,
                    quantity = quantity,
                    wholesalePrice = wholesalePrice,
                    retailPrice = retailPrice,
                    notes = notes
                )
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error adding stock to inventory in background", e)
            }
        }
    }

    fun saveInventoryItem(
        packageName: String,
        quantity: Int,
        wholesalePrice: Double,
        retailPrice: Double
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.saveInventoryItem(
                    InventoryItemEntity(
                        packageName = packageName,
                        quantityAvailable = quantity,
                        wholesalePrice = wholesalePrice,
                        retailPrice = retailPrice
                    )
                )
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error saving inventory item in background", e)
            }
        }
    }

    fun deleteInventoryItem(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deleteInventoryItem(id)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error deleting inventory item in background", e)
            }
        }
    }

    fun distributeFromInventory(
        inventoryId: Long,
        retailerId: Long,
        quantity: Int,
        onResult: (Boolean) -> Unit
    ) {
        onResult(true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.distributeFromInventory(inventoryId, retailerId, quantity)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error distributing from inventory in background", e)
            }
        }
    }

    /**
     * جرد ومطابقة المخزون الفعلي مع مبيعات الفواتير وحساب المبيعات لكل فئة
     */
    fun reconcileInventoryWithSalesInvoices(onComplete: (com.example.data.repository.InventoryReconciliationSummary) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val summary = repository.reconcileInventoryWithSalesInvoices()
                withContext(Dispatchers.Main) {
                    onComplete(summary)
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error reconciling inventory: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    onComplete(
                        com.example.data.repository.InventoryReconciliationSummary(
                            totalInvoicesAudited = 0,
                            newlyDeductedInvoicesCount = 0,
                            totalCardsNewlyDeducted = 0,
                            totalReturnedCardsFromVoided = 0,
                            categoryReports = emptyList()
                        )
                    )
                }
            }
        }
    }

    // Card Sales Invoices (فواتير مبيعات الكروت متعددة الأصناف)
    val salesInvoices: StateFlow<List<CardSalesInvoiceEntity>> = repository.allSalesInvoices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalSalesAmount: StateFlow<Double?> = repository.totalSalesAmount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalSoldCardsCount: StateFlow<Int?> = repository.totalSoldCardsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalCreditRemaining: StateFlow<Double?> = repository.totalCreditRemaining
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    /**
     * إصدار فاتورة مبيعات كروت متعددة الأصناف مع الخصم التلقائي من المخزن
     */
    fun issueMultiItemSalesInvoice(
        customerName: String,
        customerPhone: String = "",
        retailerId: Long? = null,
        items: List<CardSalesInvoiceItem>,
        paymentType: String = "CASH",
        paidAmount: Double = 0.0,
        notes: String = "",
        onComplete: (Long) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val issuer = _currentUser.value?.fullName ?: "المهندس حسن"
                val newInvoiceId = repository.issueMultiItemSalesInvoice(
                    customerName = customerName,
                    customerPhone = customerPhone,
                    retailerId = retailerId,
                    items = items,
                    paymentType = paymentType,
                    paidAmount = paidAmount,
                    notes = notes,
                    issuerName = issuer
                )
                withContext(Dispatchers.Main) {
                    onComplete(newInvoiceId)
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error issuing sales invoice", e)
                withContext(Dispatchers.Main) {
                    onComplete(-1L)
                }
            }
        }
    }

    fun deleteSalesInvoice(invoice: CardSalesInvoiceEntity, onComplete: (() -> Unit)? = null) {
        onComplete?.invoke()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deleteSalesInvoice(invoice)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error deleting sales invoice in background", e)
            }
        }
    }

    /**
     * تحديث وتعديل فاتورة مبيعات قائمة مع حفظ التعديلات وإعادة ضبط الأرصدة والمخزن
     */
    fun updateSalesInvoice(
        originalInvoice: CardSalesInvoiceEntity,
        customerName: String,
        customerPhone: String = "",
        retailerId: Long? = null,
        items: List<CardSalesInvoiceItem>,
        paymentType: String = "CASH",
        paidAmount: Double = 0.0,
        notes: String = "",
        onComplete: () -> Unit = {}
    ) {
        onComplete()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val issuer = _currentUser.value?.fullName ?: "المهندس حسن"
                repository.updateMultiItemSalesInvoice(
                    originalInvoice = originalInvoice,
                    customerName = customerName,
                    customerPhone = customerPhone,
                    retailerId = retailerId,
                    items = items,
                    paymentType = paymentType,
                    paidAmount = paidAmount,
                    notes = notes,
                    issuerName = issuer
                )
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error updating sales invoice in background", e)
            }
        }
    }

    /**
     * استنساخ مباشر وسريع لفاتورة مبيعات كروت بنقرة واحدة
     */
    fun instantCloneSalesInvoice(
        invoice: CardSalesInvoiceEntity,
        onComplete: (Long) -> Unit = {}
    ) {
        val tempId = System.currentTimeMillis()
        onComplete(tempId)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val issuer = _currentUser.value?.fullName ?: "المهندس حسن"
                repository.cloneSalesInvoice(invoice, issuer)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error cloning sales invoice in background", e)
            }
        }
    }

    /**
     * استبدال واستنساخ الفاتورة:
     * يقوم بحذف الفاتورة السابقة وإصدار الفاتورة الجديدة بنظام متسلسل يضمن استقرار المخزن وضبط الأرصدة
     */
    fun replaceSalesInvoice(
        oldInvoice: CardSalesInvoiceEntity,
        customerName: String,
        customerPhone: String,
        retailerId: Long?,
        items: List<CardSalesInvoiceItem>,
        paymentType: String,
        paidAmount: Double,
        notes: String,
        issuer: String = "المهندس حسن",
        onComplete: (Long) -> Unit
    ) {
        val tempId = System.currentTimeMillis()
        onComplete(tempId)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deleteSalesInvoice(oldInvoice)
                repository.issueMultiItemSalesInvoice(
                    customerName = customerName,
                    customerPhone = customerPhone,
                    retailerId = retailerId,
                    items = items,
                    paymentType = paymentType,
                    paidAmount = paidAmount,
                    notes = notes,
                    issuerName = issuer
                )
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error replacing sales invoice in background", e)
            }
        }
    }

    /**
     * تدقيق وتحليل ذكي للفاتورة بواسطة Gemini AI
     */
    fun analyzeInvoiceWithAi(
        invoice: CardSalesInvoiceEntity,
        items: List<CardSalesInvoiceItem>,
        onResult: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val prompt = """
                    أنت مستشار مالي وخبير أنظمة شبكات ميكروتك ومحاسبة نقاط البيع.
                    قم بتحليل فاتورة مبيعات الكروت التالية وقدم ملخصاً احترافياً موجزاً (3-4 أسطر فقط باللغة العربية):
                    - رقم الفاتورة: ${invoice.invoiceNumber}
                    - العميل: ${invoice.customerName}
                    - نوع الفاتورة: ${invoice.paymentType}
                    - إجمالي المبلغ: ${invoice.totalAmount} ريال
                    - المبلغ المدفوع: ${invoice.paidAmount} ريال
                    - المتبقي (الآجل): ${invoice.remainingAmount} ريال
                    - إجمالي عدد الكروت المباعة: ${invoice.totalCardsCount} كرت
                    - تفاصيل الأصناف: ${items.joinToString { "${it.packageName}: ${it.quantity} كرت بسعر جملة ${it.unitPrice} ر.ي (تجزئة ${it.retailPrice} ر.ي)" }}
                    
                    اذكر باختصار:
                    1. هامش ربح البقالة/العميل الإجمالي المتوقع
                    2. سرعة دوران الأصناف الأكثر سحباً
                    3. نصيحة لإدارة الائتمان وسداد المبلغ المتبقي إن وجد
                """.trimIndent()
                val response = aiService.generateText(prompt)
                onResult(response)
            } catch (e: Exception) {
                onResult("تعذر الاتصال بالمساعد الذكي: ${e.message}")
            }
        }
    }

    // Cards & Batches
    val cardBatches: StateFlow<List<CardBatchEntity>> = repository.allBatches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cards: StateFlow<List<CardEntity>> = repository.allCards
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableCardsCount: StateFlow<Int> = repository.availableCardsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val distributedCardsCount: StateFlow<Int> = repository.distributedCardsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val soldCardsCount: StateFlow<Int> = repository.soldCardsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun createBatchAndGenerateCards(
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
        passwordPolicy: com.example.data.cards.PasswordPolicy = com.example.data.cards.PasswordPolicy.SAME_AS_USERNAME,
        onComplete: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val id = repository.createBatchAndGenerateCards(
                batchName, categoryName, retailPrice, wholesalePrice,
                quotaMb, validityHours, speedLimit, count, prefix,
                codeLength, charSet, passwordPolicy
            )
            onComplete(id)
        }
    }

    /**
     * استيراد دفعة كروت تم إنشاؤها في برامج خارجية (User Manager, Excel, SAS4, إلخ)
     */
    fun importExternalCardBatch(
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
        targetRetailerId: Long? = null,
        onComplete: (batchId: Long, importedCount: Int) -> Unit
    ) {
        viewModelScope.launch {
            val batchId = repository.importExternalCardBatch(
                batchName = batchName,
                categoryName = categoryName,
                retailPrice = retailPrice,
                wholesalePrice = wholesalePrice,
                quotaMb = quotaMb,
                validityHours = validityHours,
                speedLimit = speedLimit,
                cardsList = cardsList,
                sourceProgram = sourceProgram,
                prefix = prefix,
                targetRetailerId = targetRetailerId
            )
            onComplete(batchId, cardsList.size)
        }
    }

    /**
     * إضافة كروت يدوياً بالعدد فقط (مثلاً كروت فئة 200 ريال بعدد 1000 كرت)
     * دون اشتراط وجود رموز وأرقام الكروت مسبقاً.
     */
    fun addManualCardQuantity(
        categoryName: String,
        quantity: Int,
        retailPrice: Double,
        wholesalePrice: Double,
        batchName: String = "إضافة يدوية - $categoryName",
        quotaMb: Long = 0,
        validityHours: Int = 24,
        speedLimit: String = "4M/2M",
        targetRetailerId: Long? = null,
        onComplete: (batchId: Long, count: Int) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            val batchId = repository.addManualCardQuantity(
                categoryName = categoryName,
                quantity = quantity,
                retailPrice = retailPrice,
                wholesalePrice = wholesalePrice,
                batchName = batchName,
                quotaMb = quotaMb,
                validityHours = validityHours,
                speedLimit = speedLimit,
                targetRetailerId = targetRetailerId
            )
            onComplete(batchId, quantity)
        }
    }

    // Customer / Supermarket Instant Search State
    private val _customerSearchQuery = MutableStateFlow("")
    val customerSearchQuery: StateFlow<String> = _customerSearchQuery.asStateFlow()

    fun updateCustomerSearchQuery(query: String) {
        _customerSearchQuery.value = query
    }

    // Retailers / Groceries
    val retailers: StateFlow<List<RetailerEntity>> = repository.allRetailers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Unified General Ledger Account 1201 Customer Balances
    val customerGlBalances: StateFlow<Map<Long, java.math.BigDecimal>> = repository.customerGlBalances
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Dynamically sum GL Account 1201 positive net balances for all retailers
    val totalShopsDebt: StateFlow<java.math.BigDecimal> = kotlinx.coroutines.flow.combine(retailers, customerGlBalances) { list, balancesMap ->
        val mappedSum = list.fold(java.math.BigDecimal.ZERO) { acc, r ->
            val bal = balancesMap[r.id] ?: java.math.BigDecimal.ZERO
            acc.add(bal.coerceAtLeast(java.math.BigDecimal.ZERO))
        }
        val unmappedPositiveSum = balancesMap.filter { entry ->
            list.none { it.id == entry.key } && entry.value > java.math.BigDecimal.ZERO
        }.values.fold(java.math.BigDecimal.ZERO) { acc, b -> acc.add(b) }
        mappedSum.add(unmappedPositiveSum)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), java.math.BigDecimal.ZERO)

    suspend fun getPartyBalance(accountCode: String = "1201", partyId: Long): java.math.BigDecimal {
        return repository.getPartyBalance(accountCode, partyId)
    }

    fun getPartyBalanceFlow(accountCode: String = "1201", partyId: Long) =
        repository.getPartyBalanceFlow(accountCode, partyId)

    fun getCustomerAccountSummary(customerId: Long): kotlinx.coroutines.flow.Flow<com.example.data.local.dao.CustomerAccountSummary> {
        return repository.getCustomerAccountSummary(customerId)
    }

    fun getCustomerLedger(customerId: Long): kotlinx.coroutines.flow.Flow<List<com.example.data.local.entity.CustomerLedgerEntity>> {
        return repository.getCustomerLedger(customerId)
    }

    fun saveRetailer(retailer: RetailerEntity, onComplete: () -> Unit = {}) {
        onComplete()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val id = repository.saveRetailer(retailer)
                val updated = if (retailer.id == 0L) retailer.copy(id = id) else retailer
                val userEmail = _currentUser.value?.email?.takeIf { it.isNotBlank() }
                firebaseService.pushRetailer(updated, userEmail)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error saving retailer in background", e)
            }
        }
    }

    fun deleteRetailer(retailer: RetailerEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deleteRetailer(retailer)
                val userEmail = _currentUser.value?.email?.takeIf { it.isNotBlank() }
                firebaseService.deleteRetailer(retailer.id, userEmail)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error deleting retailer in background", e)
            }
        }
    }

    fun distributeCardsToRetailer(
        batchId: Long,
        retailerId: Long,
        quantity: Int,
        onResult: (Boolean) -> Unit
    ) {
        onResult(true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.distributeCardsToRetailer(batchId, retailerId, quantity)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error distributing cards in background", e)
            }
        }
    }

    fun markCardSold(cardId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.markCardSold(cardId)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error marking card sold in background", e)
            }
        }
    }

    // Packages & Profiles (باقات وفئات الكروت والبروفايلات)
    val cardPackages: StateFlow<List<CardPackageEntity>> = repository.allPackages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun savePackage(pkg: CardPackageEntity, onDone: (() -> Unit)? = null) {
        onDone?.invoke()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.savePackage(pkg)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error saving package in background", e)
            }
        }
    }

    fun deletePackage(pkg: CardPackageEntity, onDone: (() -> Unit)? = null) {
        onDone?.invoke()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deletePackage(pkg)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error deleting package in background", e)
            }
        }
    }

    fun issueCardSalesInvoice(
        pkg: CardPackageEntity,
        retailerId: Long,
        quantity: Int,
        paymentMethod: String = "نقداً",
        notes: String = "",
        onDone: ((Long) -> Unit)? = null
    ) {
        val tempId = System.currentTimeMillis()
        onDone?.invoke(tempId)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val issuer = _currentUser.value?.fullName ?: "المهندس حسن"
                repository.issueCardSalesInvoice(
                    packageEntity = pkg,
                    retailerId = retailerId,
                    quantity = quantity,
                    paymentMethod = paymentMethod,
                    issuerName = issuer,
                    notes = notes
                )
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error issuing card sales invoice in background", e)
            }
        }
    }

    // Financial Vouchers
    val vouchers: StateFlow<List<FinancialVoucherEntity>> = repository.allVouchers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalReceipts: StateFlow<Double?> = repository.totalReceipts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalPayments: StateFlow<Double?> = repository.totalPayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Chart of Accounts & Standard Double-Entry Ledger (شجرة الحسابات المعيارية وميزان المراجعة)
    val chartOfAccounts: StateFlow<List<ChartOfAccountsEntity>> = repository.allChartOfAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trialBalance: StateFlow<List<TrialBalanceRow>> = repository.trialBalance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val journalEntriesWithLines: StateFlow<List<JournalEntryWithLines>> = repository.allJournalEntriesWithLines
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getPeriodTrialBalance(startDate: Long, endDate: Long): Flow<List<TrialBalanceRow>> {
        return repository.getPeriodTrialBalance(startDate, endDate)
    }

    fun getGeneralLedgerReport(accountCode: String, startDate: Long, endDate: Long): Flow<GeneralLedgerReport> {
        return repository.getGeneralLedgerReport(accountCode, startDate, endDate)
    }

    fun getIncomeStatementReport(startDate: Long, endDate: Long): Flow<IncomeStatementReport> {
        return repository.getIncomeStatementReport(startDate, endDate)
    }

    fun postBalancedJournalEntry(
        entryNumber: String,
        referenceType: String,
        referenceId: String,
        description: String,
        lines: List<JournalEntryLineEntity>,
        onSuccess: (Long) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val createdBy = _currentUser.value?.fullName ?: "المهندس سام"
                val id = repository.postDoubleEntry(
                    entryNumber = entryNumber,
                    referenceType = referenceType,
                    referenceId = referenceId,
                    description = description,
                    lines = lines,
                    createdBy = createdBy
                )
                withContext(Dispatchers.Main) { onSuccess(id) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError(e.message ?: "حدث خطأ في حفظ القيد") }
            }
        }
    }

    fun voidJournalEntry(
        headerId: Long,
        voidReason: String = "إلغاء القيد المحاسبي",
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val performedBy = _currentUser.value?.fullName ?: "المهندس سام"
            val reversalId = repository.voidJournalEntry(headerId, voidReason, performedBy)
            withContext(Dispatchers.Main) { onComplete(reversalId != null) }
        }
    }

    fun saveChartOfAccount(
        account: ChartOfAccountsEntity,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveAccount(account)
            withContext(Dispatchers.Main) { onComplete() }
        }
    }

    fun createVoucher(
        voucherType: String,
        amount: Double,
        currency: String = "YER",
        partyName: String,
        retailerId: Long?,
        category: String,
        paymentMethod: String,
        description: String,
        invoiceId: Long? = null,
        invoiceNumber: String = "",
        onComplete: (String) -> Unit = {}
    ) {
        val tempVoucherNumber = "${if (voucherType == "RECEIPT") "REC" else "PAY"}-2026-${System.currentTimeMillis() % 100000}"
        onComplete(tempVoucherNumber)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val issuer = _currentUser.value?.fullName ?: "المهندس سام"
                val id = repository.createVoucher(
                    voucherType = voucherType,
                    amount = amount,
                    currency = currency,
                    partyName = partyName,
                    retailerId = retailerId,
                    category = category,
                    paymentMethod = paymentMethod,
                    description = description,
                    issuerName = issuer,
                    invoiceId = invoiceId,
                    invoiceNumber = invoiceNumber
                )
                val voucherNumberGenerated = "${if (voucherType == "RECEIPT") "REC" else "PAY"}-2026-${id}"
                val userEmail = _currentUser.value?.email?.takeIf { it.isNotBlank() }
                firebaseService.pushVoucher(
                    FinancialVoucherEntity(
                        id = id,
                        voucherNumber = voucherNumberGenerated,
                        voucherType = voucherType,
                        amount = BigDecimal.valueOf(amount),
                        currency = currency,
                        partyName = partyName,
                        retailerId = retailerId,
                        invoiceId = invoiceId,
                        invoiceNumber = invoiceNumber,
                        allocatedAmount = if (invoiceId != null || invoiceNumber.isNotBlank()) BigDecimal.valueOf(amount) else BigDecimal.ZERO,
                        category = category,
                        paymentMethod = paymentMethod,
                        description = description,
                        issuerName = issuer
                    ),
                    userEmail
                )
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error creating voucher in background", e)
            }
        }
    }

    fun deleteVoucher(voucher: FinancialVoucherEntity, onComplete: () -> Unit = {}) {
        onComplete()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deleteVoucher(voucher)
                val userEmail = _currentUser.value?.email?.takeIf { it.isNotBlank() } ?: authManager.getActiveEmail()
                firebaseService.deleteVoucher(voucher.voucherNumber, userEmail)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error deleting voucher in background", e)
            }
        }
    }

    fun deduplicateVouchers(onComplete: (Int) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val count = repository.deduplicateVouchers()
                withContext(Dispatchers.Main) {
                    onComplete(count)
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error deduplicating vouchers", e)
                withContext(Dispatchers.Main) {
                    onComplete(0)
                }
            }
        }
    }

    fun updateVoucher(voucher: FinancialVoucherEntity, onComplete: () -> Unit = {}) {
        onComplete()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.updateVoucher(voucher)
                val userEmail = _currentUser.value?.email?.takeIf { it.isNotBlank() }
                firebaseService.pushVoucher(voucher, userEmail)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error updating voucher in background", e)
            }
        }
    }

    fun voidVoucher(voucher: FinancialVoucherEntity, reason: String = "", onComplete: () -> Unit = {}) {
        onComplete()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.voidVoucher(voucher, reason)
                val userEmail = _currentUser.value?.email?.takeIf { it.isNotBlank() }
                firebaseService.pushVoucher(
                    voucher.copy(isVoided = true),
                    userEmail
                )
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error voiding voucher in background", e)
            }
        }
    }

    // =========================================================
    // Investment, Partners & Fixed Assets (CAPEX & Equity System)
    // =========================================================
    val partners: StateFlow<List<PartnerEntity>> = repository.allPartners
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalInvestedCapital: StateFlow<Double?> = repository.totalInvestedCapital
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalDistributedProfits: StateFlow<Double?> = repository.totalDistributedProfits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val partnerTransactions: StateFlow<List<PartnerTransactionEntity>> = repository.allPartnerTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun savePartner(partner: PartnerEntity, onComplete: () -> Unit = {}) {
        onComplete()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.savePartner(partner)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error saving partner in background", e)
            }
        }
    }

    fun deletePartner(partner: PartnerEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deletePartner(partner)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error deleting partner in background", e)
            }
        }
    }

    fun recordPartnerTransaction(tx: PartnerTransactionEntity, onComplete: () -> Unit = {}) {
        onComplete()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.recordPartnerTransaction(tx)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error recording partner transaction in background", e)
            }
        }
    }

    fun deletePartnerTransaction(tx: PartnerTransactionEntity) {
        viewModelScope.launch {
            repository.deletePartnerTransaction(tx)
        }
    }

    // Fixed Assets (CAPEX)
    val assets: StateFlow<List<NetworkAssetEntity>> = repository.allAssets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalAssetPurchaseCost: StateFlow<Double?> = repository.totalAssetPurchaseCost
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalCurrentAssetValue: StateFlow<Double?> = repository.totalCurrentAssetValue
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val assetCount: StateFlow<Int> = repository.assetCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun saveAsset(asset: NetworkAssetEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.saveAsset(asset)
            onComplete()
        }
    }

    fun deleteAsset(asset: NetworkAssetEntity) {
        viewModelScope.launch {
            repository.deleteAsset(asset)
        }
    }

    // =========================================================
    // Purchase Invoices & AI Vision Capture (فواتير المشتريات والأصول)
    // =========================================================
    val invoices: StateFlow<List<PurchaseInvoiceEntity>> = repository.allInvoices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalInvoicesAmount: StateFlow<Double> = repository.totalInvoicesAmount
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val _isScanningInvoice = MutableStateFlow(false)
    val isScanningInvoice: StateFlow<Boolean> = _isScanningInvoice.asStateFlow()

    private val _lastScannedInvoice = MutableStateFlow<ParsedInvoiceData?>(null)
    val lastScannedInvoice: StateFlow<ParsedInvoiceData?> = _lastScannedInvoice.asStateFlow()

    fun scanInvoiceWithAi(
        bitmap: Bitmap,
        onResult: (ParsedInvoiceData) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isScanningInvoice.value = true
            try {
                val parsed = aiService.parseInvoiceImage(bitmap)
                _lastScannedInvoice.value = parsed
                _isScanningInvoice.value = false
                onResult(parsed)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error analyzing invoice with AI", e)
                _isScanningInvoice.value = false
                onError(e.localizedMessage ?: "فشل تحليل الفاتورة")
            }
        }
    }

    fun approveAndSaveInvoice(
        invoice: PurchaseInvoiceEntity,
        items: List<InvoiceItem>,
        saveAsAssets: Boolean,
        saveAsVoucher: Boolean,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            // Build items JSON and summary
            val itemsArray = JSONArray()
            items.forEach { item ->
                val itemObj = JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("quantity", item.quantity)
                    put("unitPrice", item.unitPrice)
                    put("subtotal", item.subtotal)
                    put("category", item.category)
                    put("currency", invoice.currency)
                }
                itemsArray.put(itemObj)
            }
            val currSym = com.example.util.CurrencyHelper.getCurrencySymbol(invoice.currency)
            val summaryText = items.joinToString("، ") { "${it.name} (${it.quantity.toInt()} × ${it.unitPrice.toInt()} $currSym)" }
            val totalCalc = items.sumOf { it.subtotal }

            val isEdit = invoice.id > 0L
            val invoiceToSave = invoice.copy(
                totalAmount = if (totalCalc > 0) BigDecimal.valueOf(totalCalc) else invoice.totalAmount,
                itemsJson = itemsArray.toString(),
                itemsSummary = summaryText,
                status = "APPROVED"
            )
            val savedInvoiceId = repository.saveInvoice(invoiceToSave)
            val finalInvoiceNumber = invoiceToSave.invoiceNumber

            // 1. If user designated as Assets or requested saving items to Fixed Assets (only for new invoices to avoid duplication)
            if ((saveAsAssets || invoiceToSave.targetType == "ASSETS") && !isEdit) {
                items.forEach { item ->
                    val assetCat = when (item.category.uppercase()) {
                        "SERVERS", "ROUTERS" -> "SERVERS"
                        "TOWERS", "ANTENNAS" -> "TOWERS"
                        "SOLAR_POWER", "BATTERIES" -> "SOLAR_POWER"
                        "CABLES", "FIBER" -> "FIBER_CABLES"
                        else -> "OTHER"
                    }
                    val costInYer = convertToYer(item.subtotal, invoiceToSave.currency)
                    val asset = NetworkAssetEntity(
                        assetName = item.name,
                        category = assetCat,
                        purchaseCost = costInYer,
                        estimatedCurrentValue = costInYer,
                        currency = invoiceToSave.currency,
                        originalCost = item.subtotal,
                        purchaseDateMillis = invoiceToSave.invoiceDateMillis,
                        location = "المركز الرئيسي والشبكة",
                        serialNumber = "",
                        status = "ACTIVE",
                        notes = "مستورد من فاتورة رقم: $finalInvoiceNumber (المورد: ${invoiceToSave.supplierName})"
                    )
                    repository.saveAsset(asset)
                }
            }

            // 2. If user requested recording in financial accounting vouchers (سند صرف مشتريات)
            if (saveAsVoucher) {
                val existingVouchers = db.financialVoucherDao().getVouchersByInvoiceNumber(finalInvoiceNumber)
                val existingVoucher = existingVouchers.firstOrNull()
                val voucherNum = existingVoucher?.voucherNumber ?: "PAY-${System.currentTimeMillis() % 100000}"
                val voucherCat = if (invoiceToSave.targetType == "ASSETS") "أصول ومعدات شبكة" else "صيانة ومعدات"
                val desc = "سداد فاتورة مشتريات #$finalInvoiceNumber (${invoiceToSave.supplierName}): $summaryText"

                val voucher = FinancialVoucherEntity(
                    id = existingVoucher?.id ?: 0L,
                    voucherNumber = voucherNum,
                    voucherType = "PAYMENT",
                    amount = invoiceToSave.totalAmount,
                    currency = invoiceToSave.currency,
                    partyName = invoiceToSave.supplierName.ifBlank { "مورد معدات" },
                    category = voucherCat,
                    paymentMethod = invoiceToSave.paymentMethod,
                    description = desc,
                    issuerName = _currentUser.value?.fullName ?: "المهندس سام",
                    dateMillis = invoiceToSave.invoiceDateMillis,
                    notes = invoiceToSave.notes,
                    invoiceId = savedInvoiceId,
                    invoiceNumber = finalInvoiceNumber,
                    allocatedAmount = invoiceToSave.totalAmount
                )
                repository.insertVoucher(voucher)
                val userEmail = _currentUser.value?.email?.takeIf { it.isNotBlank() }
                firebaseService.pushVoucher(voucher, userEmail)

                // حفظ قيد يومية محاسبي مزدوج في جدول journal_entries عبر repository
                val convertedAmountYer = convertToYer(invoiceToSave.totalAmount.toDouble(), invoiceToSave.currency)
                val exchangeRate = if (invoiceToSave.totalAmount > java.math.BigDecimal.ZERO) {
                    java.math.BigDecimal.valueOf(convertedAmountYer).divide(invoiceToSave.totalAmount, 4, java.math.RoundingMode.HALF_UP)
                } else {
                    java.math.BigDecimal.ONE
                }

                val (debitAcc, creditAcc) = if (invoiceToSave.targetType == "ASSETS") {
                    "1501 - أصول البنية التحتية والشبكة" to "1101 - الصندوق الرئيسي"
                } else {
                    "5201 - مصروفات تشغيل وصيانة" to "1101 - الصندوق الرئيسي"
                }

                val journalEntry = JournalEntryEntity(
                    entryNumber = "JE-PURCHASE-$finalInvoiceNumber-${System.currentTimeMillis() % 100000}",
                    dateMillis = invoiceToSave.invoiceDateMillis,
                    referenceType = if (isEdit) "PURCHASE_INVOICE_EDIT" else "PURCHASE_INVOICE",
                    referenceId = finalInvoiceNumber,
                    debitAccount = debitAcc,
                    creditAccount = creditAcc,
                    amount = java.math.BigDecimal.valueOf(convertedAmountYer),
                    currency = invoiceToSave.currency,
                    exchangeRate = exchangeRate,
                    description = desc,
                    createdBy = _currentUser.value?.fullName ?: "المهندس سام"
                )
                repository.saveJournalEntry(journalEntry)
            }

            onComplete()
        }
    }

    fun deleteInvoice(invoice: PurchaseInvoiceEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteInvoice(invoice)
            onComplete()
        }
    }

    // =========================================================
    // محركات العمليات المالية التشغيلية (COGS، الإهلاك، وإقفال الدورة)
    // =========================================================

    suspend fun calculateFinancialPeriodSummary(startDateMillis: Long, endDateMillis: Long): PeriodClosingSummary {
        return repository.calculateFinancialPeriodSummary(startDateMillis, endDateMillis)
    }

    fun executePeriodClosingEntry(
        startDateMillis: Long,
        endDateMillis: Long,
        closingPeriodName: String,
        performedBy: String = "المهندس سام",
        onComplete: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val headerId = repository.executePeriodClosingEntry(
                    startDateMillis = startDateMillis,
                    endDateMillis = endDateMillis,
                    closingPeriodName = closingPeriodName,
                    performedBy = performedBy
                )
                onComplete(headerId)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error executing period closing entry", e)
                onComplete(-1L)
            }
        }
    }

    fun executeAssetDepreciation(
        periodMonths: Int = 1,
        periodName: String = "",
        performedBy: String = "المهندس سام",
        onComplete: (AssetDepreciationResult) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val result = repository.executeAssetDepreciation(
                    periodMonths = periodMonths,
                    periodName = periodName,
                    performedBy = performedBy
                )
                onComplete(result)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error executing asset depreciation", e)
            }
        }
    }

    fun calculateAssetMonthlyDepreciation(asset: NetworkAssetEntity): java.math.BigDecimal {
        return repository.calculateAssetMonthlyDepreciation(asset)
    }

    // AI Consultant State
    private val _aiResponse = MutableStateFlow<String?>(null)
    val aiResponse: StateFlow<String?> = _aiResponse.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    fun consultAi(prompt: String) {
        viewModelScope.launch {
            _isAiLoading.value = true
            val identity = networkIdentity.value
            val currentDeviceList = devices.value.joinToString { "${it.name} (${it.ipAddress} - ${it.locationArea})" }
            val statsContext = "إجمالي الأجهزة: ${devices.value.size}, البقالات: ${retailers.value.size}, الكروت المتاحة: ${availableCardsCount.value}, الأجهزة الحالية: $currentDeviceList"
            val response = aiService.consultMikrotikAi(prompt, statsContext, identity)
            _aiResponse.value = response
            _isAiLoading.value = false
        }
    }

    // Cloud Sync Status
    private val _syncStatus = MutableStateFlow("متزامن سحابياً ومحلياً مع Firebase (sam-mikrotic) ☁️⚡")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    /**
     * سحب واستعادة البيانات من Firebase إلى قاعدة البيانات المحلية
     */
    fun pullDataFromCloud(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val userEmail = _currentUser.value?.email?.takeIf { it.isNotBlank() } ?: authManager.getActiveEmail()
            val targetLabel = if (!userEmail.isNullOrBlank()) "حساب $userEmail" else "المشروع السحابي"
            _syncStatus.value = "جارٍ استيراد وتحديث البيانات من السحابة ($targetLabel)..."

            try {
                val cloudData = firebaseService.pullFromCloud(userEmail)
                repository.restoreFromCloudData(cloudData)
                val totalItems = cloudData.devices.size + cloudData.retailers.size + cloudData.vouchers.size +
                        cloudData.cardPackages.size + cloudData.inventoryItems.size + cloudData.salesInvoices.size
                if (totalItems > 0) {
                    _syncStatus.value = "تم استيراد $totalItems سجل بنجاح من السحابة (${cloudData.devices.size} جهاز، ${cloudData.retailers.size} بقالة، ${cloudData.vouchers.size} سند، ${cloudData.cardPackages.size} باقة، ${cloudData.inventoryItems.size} صنف مخزن، ${cloudData.salesInvoices.size} فاتورة) ✓"
                } else {
                    _syncStatus.value = "لا توجد بيانات سابقة في السحابة لهذا الحساب ($targetLabel)"
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error pulling data from cloud", e)
                _syncStatus.value = "تعذر سحب البيانات السحابية: ${e.message}"
            }
            onDone()
        }
    }

    /**
     * مزامنة ثنائية كاملة: سحب أحدث بيانات من السحابة ودمجها محلياً، ثم رفع أي بيانات محلية جديدة للسحابة
     */
    fun triggerCloudSync(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val userEmail = _currentUser.value?.email?.takeIf { it.isNotBlank() } ?: authManager.getActiveEmail()
            val targetLabel = if (!userEmail.isNullOrBlank()) "حساب $userEmail" else "المشروع sam-mikrotic"
            _syncStatus.value = "جارٍ المزامنة التفاعلية مع السحابة ($targetLabel)..."

            try {
                // مزامنة حذف السندات المحذوفة محلياً أولاً لضمان عدم استعادتها
                val deletedVouchers = com.example.data.local.DeletedRecordsTracker.getAllDeletedVouchers()
                for (vNum in deletedVouchers) {
                    firebaseService.deleteVoucher(vNum, userEmail)
                }

                // تنظيف التكرارات المحلية قبل المزامنة
                repository.deduplicateVouchers()

                // الخطوة 1: سحب أي بيانات مخزنة بالسحابة (مهم جداً للهواتف الجديدة أو الأجهزة المتعددة)
                val cloudData = firebaseService.pullFromCloud(userEmail)
                repository.restoreFromCloudData(cloudData)

                // الخطوة 2: رفع كافة البيانات المحلية إلى السحابة
                val devList = devices.value
                val retList = retailers.value
                val vouchList = vouchers.value
                val pkgList = cardPackages.value
                val invList = inventoryItems.value
                val invSalesList = salesInvoices.value
                val identity = networkIdentity.value

                val result = firebaseService.syncAllToCloud(
                    devices = devList,
                    retailers = retList,
                    vouchers = vouchList,
                    userEmail = userEmail,
                    networkIdentity = identity,
                    cardPackages = pkgList,
                    inventoryItems = invList,
                    salesInvoices = invSalesList
                )

                if (result.success) {
                    val pulledCount = cloudData.devices.size + cloudData.retailers.size + cloudData.vouchers.size +
                            cloudData.cardPackages.size + cloudData.inventoryItems.size + cloudData.salesInvoices.size
                    val msg = if (pulledCount > 0) {
                        "تمت المزامنة الثنائية بنجاح 🔄 (سحب $pulledCount سجل من السحابة ومزامنة كافة الأجهزة والبقالات والفواتير)"
                    } else {
                        "تمت المزامنة بنجاح ($targetLabel): ${result.syncedDevicesCount} جهاز، ${result.syncedRetailersCount} بقالة، ${result.syncedVouchersCount} سند، وهوية الشبكة ✓"
                    }
                    _syncStatus.value = msg
                } else {
                    _syncStatus.value = result.message
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error during cloud sync", e)
                _syncStatus.value = "خطأ في المزامنة: ${e.message}"
            }
            onDone()
        }
    }

    init {
        viewModelScope.launch {
            val activeEmail = authManager.getActiveEmail()?.trim()?.lowercase()
            val lastActiveEmail = prefs.getString("last_active_email", null)?.trim()?.lowercase()

            if (!activeEmail.isNullOrBlank()) {
                if (lastActiveEmail != null && lastActiveEmail != activeEmail) {
                    // تم تسجيل الدخول بحساب مختلف تماماً أثناء توقف التطبيق -> تهيئة مساحة عمل معزولة خاصة به
                    val activeFirebaseUser = authManager.getCurrentUser()
                    val registeredUser = repository.switchUserWorkspace(
                        email = activeEmail,
                        displayName = activeFirebaseUser?.displayName ?: activeEmail.substringBefore("@"),
                        photoUrl = activeFirebaseUser?.photoUrl?.toString() ?: ""
                    )
                    _currentUser.value = registeredUser
                    prefs.edit().putString("last_active_email", activeEmail).apply()
                }

                // سحب البيانات الخاصة بحساب هذا المستخدم فقط من السحابة
                try {
                    val cloudData = firebaseService.pullFromCloud(activeEmail)
                    if (cloudData.devices.isNotEmpty() || cloudData.retailers.isNotEmpty() || cloudData.vouchers.isNotEmpty()) {
                        repository.restoreFromCloudData(cloudData)
                        _isProductionMode.value = true
                    }
                } catch (e: Throwable) {
                    Log.w("MainViewModel", "Auto cloud pull note: ${e.message}")
                }
            }

            try {
                // تسوية ومطابقة الحسابات والديون المحاسبية فورياً لضمان التزامن التام
                repository.reconcileAccountingLedger()
            } catch (e: Throwable) {
                Log.e("MainViewModel", "Error reconciling ledger: ${e.message}")
            }

            repository.allUsers.collect { list ->
                if (list.isNotEmpty()) {
                    val currentActiveEmail = authManager.getActiveEmail()?.trim()?.lowercase()
                    val current = _currentUser.value
                    if (current == null) {
                        // Only log in if there is a genuinely active authenticated Google session
                        if (!currentActiveEmail.isNullOrBlank()) {
                            val matchedLoggedIn = list.find { it.email.trim().lowercase() == currentActiveEmail }
                            if (matchedLoggedIn != null) {
                                _currentUser.value = matchedLoggedIn
                            }
                        }
                    } else {
                        // Refresh current user if data was updated in database
                        val refreshed = list.find { it.id == current.id || (current.email.isNotBlank() && it.email.equals(current.email, ignoreCase = true)) }
                        if (refreshed != null && refreshed != current) {
                            _currentUser.value = refreshed
                        }
                    }
                }
            }
        }
    }
}
