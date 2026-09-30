package com.example.ui.reports

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.repository.NetworkRepository
import com.example.data.repository.PeriodicSettlementData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * ViewModel الخاص بتقرير الإقفال والتسوية الدورية (Periodic Settlement Dashboard)
 * يقوم بإدارة الفلترة الزمنية وتشغيل الاستعلامات التجميعية الفعالة مباشرة عبر Room DAO على Dispatchers.IO
 */
class PeriodicReportViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NetworkRepository

    private val _startDateMillis = MutableStateFlow<Long>(getStartOfMonthMillis())
    val startDateMillis: StateFlow<Long> = _startDateMillis.asStateFlow()

    private val _endDateMillis = MutableStateFlow<Long>(getEndOfDayMillis())
    val endDateMillis: StateFlow<Long> = _endDateMillis.asStateFlow()

    private val _settlementData = MutableStateFlow<PeriodicSettlementData?>(null)
    val settlementData: StateFlow<PeriodicSettlementData?> = _settlementData.asStateFlow()

    private val _isLoading = MutableStateFlow<Boolean>(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = NetworkRepository(db)
        loadData()
    }

    fun setDateRange(start: Long, end: Long) {
        _startDateMillis.value = start
        _endDateMillis.value = end
        loadData()
    }

    fun loadData() {
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val data = repository.getPeriodicSettlementData(
                    startDateMillis = _startDateMillis.value,
                    endDateMillis = _endDateMillis.value
                )
                _settlementData.value = data
            } catch (e: Exception) {
                Log.e("PeriodicReportVM", "Error loading settlement data: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    companion object {
        fun getStartOfMonthMillis(): Long {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        fun getStartOfWeekMillis(): Long {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        fun getStartOfDayMillis(): Long {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        fun getStartOfYearMillis(): Long {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_YEAR, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        fun getEndOfDayMillis(): Long {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            return cal.timeInMillis
        }
    }
}
