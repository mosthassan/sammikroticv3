package com.example.data.local

object InitialDataSeeder {
    /**
     * بيئة إنتاجية حقيقية بالكامل:
     * تم إيقاف وحذف كافة البيانات الافتراضية والوهمية والتجريبية.
     * لا يتم إدراج أي كروت أو أجهزة أو بقالات أو سندات أو فواتير أو أصناف افتراضية.
     * يتم حفظ وقراءة البيانات الحقيقية فقط التي يدخلها المستخدم أو تتم مزامنتها مع MikroTik أو السحابة.
     */
    suspend fun seedDatabase(db: AppDatabase) {
        // No default or dummy data. Real production data only.
    }
}
