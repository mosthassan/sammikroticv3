package com.example.ui.retailers

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.RetailerEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.math.BigDecimal

class RetailersViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    val journalEntryDao = db.journalEntryDao()
    private val retailerDao = db.retailerDao()

    val retailers: StateFlow<List<RetailerEntity>> = retailerDao.getAllRetailers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customerGlBalances: StateFlow<Map<Long, BigDecimal>> = journalEntryDao
        .getAllPartyBalancesFlow("1201")
        .map { list -> list.associate { it.partyId to it.balance } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    suspend fun getPartyBalance(accountCode: String = "1201", partyId: Long): BigDecimal {
        return journalEntryDao.getPartyBalance(accountCode, partyId)
    }

    fun getPartyBalanceFlow(accountCode: String = "1201", partyId: Long) =
        journalEntryDao.getPartyBalanceFlow(accountCode, partyId)
}
