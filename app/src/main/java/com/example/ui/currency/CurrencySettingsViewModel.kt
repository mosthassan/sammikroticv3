package com.example.ui.currency

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.FirebaseDbService
import com.example.data.local.AppDatabase
import com.example.data.local.entity.CurrencyRateEntity
import com.example.data.local.entity.NetworkIdentityEntity
import com.example.data.repository.NetworkRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal

class CurrencySettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = NetworkRepository(db)
    private val firebaseService = FirebaseDbService()

    val rates: StateFlow<List<CurrencyRateEntity>> = db.currencyRateDao().getAllRates()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val networkIdentity: StateFlow<NetworkIdentityEntity?> = repository.networkIdentity
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        // Load saved rates from Room on initialization, falling back to networkIdentity rates if missing
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val identity = db.networkIdentityDao().getNetworkIdentity() ?: NetworkIdentityEntity()
                val existingUsd = db.currencyRateDao().getRateForCurrency("USD")
                val existingSar = db.currencyRateDao().getRateForCurrency("SAR")

                if (existingUsd == null) {
                    db.currencyRateDao().insertOrUpdateRate(
                        CurrencyRateEntity(
                            currencyCode = "USD",
                            rateToBase = BigDecimal.valueOf(if (identity.usdToYerRate > 0) identity.usdToYerRate else 530.0)
                        )
                    )
                }
                if (existingSar == null) {
                    db.currencyRateDao().insertOrUpdateRate(
                        CurrencyRateEntity(
                            currencyCode = "SAR",
                            rateToBase = BigDecimal.valueOf(if (identity.sarToYerRate > 0) identity.sarToYerRate else 140.0)
                        )
                    )
                }
                db.currencyRateDao().insertOrUpdateRate(
                    CurrencyRateEntity(
                        currencyCode = "YER",
                        rateToBase = BigDecimal.ONE
                    )
                )
            } catch (e: Exception) {
                Log.e("CurrencySettingsVM", "Error loading rates on init", e)
            }
        }
    }

    fun updateExchangeRate(currencyCode: String, rateToBase: BigDecimal, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val code = currencyCode.uppercase()
                val rateEntity = CurrencyRateEntity(
                    currencyCode = code,
                    rateToBase = rateToBase,
                    updatedAt = System.currentTimeMillis()
                )
                db.currencyRateDao().insertOrUpdateRate(rateEntity)

                // Sync NetworkIdentityEntity rate to keep network identity and UI components aligned
                val identity = db.networkIdentityDao().getNetworkIdentity() ?: NetworkIdentityEntity()
                val updatedIdentity = when (code) {
                    "USD" -> identity.copy(usdToYerRate = rateToBase.toDouble(), updatedAt = System.currentTimeMillis())
                    "SAR" -> identity.copy(sarToYerRate = rateToBase.toDouble(), updatedAt = System.currentTimeMillis())
                    else -> identity
                }
                db.networkIdentityDao().insertOrUpdate(updatedIdentity)

                // Sync to Firebase
                firebaseService.pushNetworkIdentity(updatedIdentity)

                onComplete?.invoke()
            } catch (e: Exception) {
                Log.e("CurrencySettingsVM", "Error updating exchange rate", e)
            }
        }
    }
}
