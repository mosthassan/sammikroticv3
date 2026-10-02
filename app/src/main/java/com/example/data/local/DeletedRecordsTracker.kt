package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import java.util.Collections

object DeletedRecordsTracker {
    private const val PREFS_NAME = "sam_mikrotic_deleted_records"
    private const val KEY_DELETED_VOUCHERS = "deleted_vouchers"

    private val deletedVoucherNumbers: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val saved = prefs?.getStringSet(KEY_DELETED_VOUCHERS, emptySet()) ?: emptySet()
            for (s in saved) {
                if (s.isNotBlank()) {
                    deletedVoucherNumbers.add(s.trim().uppercase())
                }
            }
        }
    }

    fun markVoucherDeleted(voucherNumber: String) {
        val trimmed = voucherNumber.trim().uppercase()
        if (trimmed.isBlank()) return
        deletedVoucherNumbers.add(trimmed)
        prefs?.let { p ->
            val set = p.getStringSet(KEY_DELETED_VOUCHERS, emptySet())?.toMutableSet() ?: mutableSetOf()
            set.add(trimmed)
            p.edit().putStringSet(KEY_DELETED_VOUCHERS, set).apply()
        }
    }

    fun isVoucherDeleted(voucherNumber: String): Boolean {
        val trimmed = voucherNumber.trim().uppercase()
        if (trimmed.isBlank()) return false
        return deletedVoucherNumbers.contains(trimmed)
    }

    fun getAllDeletedVouchers(): Set<String> {
        return deletedVoucherNumbers.toSet()
    }
}
