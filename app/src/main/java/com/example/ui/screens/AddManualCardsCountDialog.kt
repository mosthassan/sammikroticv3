package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CardPackageEntity
import com.example.data.local.entity.RetailerEntity
import com.example.ui.theme.MikroTikCyan
import com.example.ui.theme.MikroTikNavy
import com.example.ui.theme.MikroTikPrimary
import com.example.ui.theme.ProfitEmerald

/**
 * نافذة لإضافة رصيد كروت يدوياً بالعدد فقط (مثلاً 1000 كرت فئة 200 ريال)
 * دون اشتراط وجود أو إدخال أرقام ورموز وكلمات مرور الكروت.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddManualCardsCountDialog(
    packages: List<CardPackageEntity> = emptyList(),
    retailers: List<RetailerEntity> = emptyList(),
    initialPackage: CardPackageEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        categoryName: String,
        quantity: Int,
        retailPrice: Double,
        wholesalePrice: Double,
        batchName: String,
        targetRetailerId: Long?
    ) -> Unit
) {
    // تحديد الباقة المختارة كبداية
    val defaultPkg = initialPackage ?: packages.firstOrNull()
    var isSaving by remember { mutableStateOf(false) }

    var categoryName by remember {
        mutableStateOf(defaultPkg?.let { it.name } ?: "كروت فئة 200 ريال")
    }
    var countText by remember { mutableStateOf("1000") }
    var retailPriceText by remember {
        mutableStateOf(defaultPkg?.retailPrice?.toInt()?.toString() ?: "200")
    }
    var wholesalePriceText by remember {
        mutableStateOf(defaultPkg?.wholesalePrice?.toInt()?.toString() ?: "180")
    }
    var batchName by remember {
        mutableStateOf(defaultPkg?.let { "إضافة يدوية - ${it.name}" } ?: "إضافة كروت بالعدد - فئة 200")
    }

    // الوجهة: مخزن المستودع العام، أو تسليم فوري لبقالة
    var destinationMode by remember { mutableStateOf(0) } // 0: للمخزن، 1: لبقالة
    var selectedRetailer by remember { mutableStateOf(retailers.firstOrNull()) }
    var isRetailerDropdownExpanded by remember { mutableStateOf(false) }

    val quantity = countText.toIntOrNull() ?: 0
    val retailPrice = retailPriceText.toDoubleOrNull() ?: 0.0
    val wholesalePrice = wholesalePriceText.toDoubleOrNull() ?: 0.0
    val unitProfit = (retailPrice - wholesalePrice).coerceAtLeast(0.0)
    val totalWholesaleAmount = quantity * wholesalePrice
    val totalRetailAmount = quantity * retailPrice
    val totalRetailerProfit = quantity * unitProfit

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ProfitEmerald.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Numbers,
                        contentDescription = null,
                        tint = ProfitEmerald,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        "إضافة كروت يدوياً بالعدد",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                    Text(
                        "إضافة رصيد كروت للمخزن بدون اشتراط أرقام الكروت",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // شريط توضيحي مريح للمستخدم
                item {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0F1E33),
                        border = BorderStroke(1.dp, Color(0xFF1E3A5F))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MikroTikCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "تستطيع إدخال أي عدد (مثلاً 1000 كرت فئة 200) وسيتم قيده مباشرة في المخزن والمبيعات دون الحاجة لإدخال رموز سرية.",
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // اختيار أو تحديد الفئة / الباقة
                item {
                    Text(
                        "1. فئة الكرت / الباقة المعتمدة:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    if (packages.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(packages) { pkg ->
                                val isSelected = categoryName == pkg.name || categoryName.startsWith(pkg.name)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) ProfitEmerald.copy(alpha = 0.2f) else Color(0xFF1E293B),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) ProfitEmerald else Color(0xFF334155)
                                    ),
                                    modifier = Modifier.clickable {
                                        categoryName = pkg.name
                                        retailPriceText = pkg.retailPrice.toInt().toString()
                                        wholesalePriceText = pkg.wholesalePrice.toInt().toString()
                                        batchName = "إضافة يدوية - ${pkg.name}"
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = ProfitEmerald,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text(
                                            pkg.name,
                                            fontSize = 11.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) ProfitEmerald else Color(0xFFE2E8F0)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = categoryName,
                        onValueChange = {
                            categoryName = it
                            batchName = "إضافة يدوية - $it"
                        },
                        label = { Text("اسم الفئة (مثلاً: كروت فئة 200 ريال)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // عدد الكروت المراد إضافتها
                item {
                    Text(
                        "2. عدد الكروت (الكمية الإجمالية):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    // أزرار أعداد سريعة شائعة
                    val countPresets = listOf(50, 100, 200, 500, 1000, 2000, 5000)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(countPresets) { preset ->
                            val isChosen = countText == preset.toString()
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isChosen) Color(0xFF0284C7).copy(alpha = 0.25f) else Color(0xFF1E293B),
                                border = BorderStroke(
                                    1.dp,
                                    if (isChosen) Color(0xFF38BDF8) else Color(0xFF334155)
                                ),
                                modifier = Modifier.clickable { countText = preset.toString() }
                            ) {
                                Text(
                                    text = if (preset >= 1000) "${preset / 1000} ألف كرت" else "$preset كرت",
                                    fontSize = 11.sp,
                                    fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isChosen) Color(0xFF38BDF8) else Color(0xFFE2E8F0),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = countText,
                        onValueChange = { countText = it.filter { char -> char.isDigit() } },
                        label = { Text("أدخل العدد يدوياً") },
                        placeholder = { Text("مثال: 1000") },
                        leadingIcon = {
                            Icon(Icons.Default.Numbers, contentDescription = null, tint = ProfitEmerald)
                        },
                        trailingIcon = {
                            Text("كرت", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(end = 8.dp))
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // أسعار الجملة والتجزئة
                item {
                    Text(
                        "3. الأسعار المحاسبية (ريال يمني):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = wholesalePriceText,
                            onValueChange = { wholesalePriceText = it },
                            label = { Text("سعر الجملة للبقالة") },
                            placeholder = { Text("180") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = retailPriceText,
                            onValueChange = { retailPriceText = it },
                            label = { Text("سعر البيع للزبون") },
                            placeholder = { Text("200") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // الحسبة الإجمالية والمالية
                item {
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        border = BorderStroke(1.dp, Color(0xFF1E293B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "ملخص الحسبة لهذه الكمية:",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MikroTikCyan
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("إجمالي عدد الكروت المضافة:", fontSize = 11.5.sp, color = Color(0xFF94A3B8))
                                Text("$quantity كرت", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("إجمالي قيمة الجملة (المستحقة):", fontSize = 11.5.sp, color = Color(0xFF94A3B8))
                                Text("${totalWholesaleAmount.toInt()} ريال", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ProfitEmerald)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("إجمالي قيمة البيع النهائي:", fontSize = 11.5.sp, color = Color(0xFF94A3B8))
                                Text("${totalRetailAmount.toInt()} ريال", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("هامش ربح البقالات المتوقع:", fontSize = 11.5.sp, color = Color(0xFF94A3B8))
                                Text("+${totalRetailerProfit.toInt()} ريال", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFBBF24))
                            }
                        }
                    }
                }

                // الوجهة: إيداع بالمستودع أو تسليم فوري لبقالة
                item {
                    Text(
                        "4. وجهة الإيداع والصرف:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (destinationMode == 0) Color(0xFF0284C7).copy(alpha = 0.2f) else Color(0xFF1E293B),
                            border = BorderStroke(1.dp, if (destinationMode == 0) Color(0xFF38BDF8) else Color(0xFF334155)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { destinationMode = 0 }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = destinationMode == 0,
                                    onClick = { destinationMode = 0 },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF38BDF8))
                                )
                                Column {
                                    Text("المستودع العام", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("رصيد متاح للتوزيع", fontSize = 9.5.sp, color = Color(0xFF94A3B8))
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (destinationMode == 1) ProfitEmerald.copy(alpha = 0.2f) else Color(0xFF1E293B),
                            border = BorderStroke(1.dp, if (destinationMode == 1) ProfitEmerald else Color(0xFF334155)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { destinationMode = 1 }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = destinationMode == 1,
                                    onClick = { destinationMode = 1 },
                                    colors = RadioButtonDefaults.colors(selectedColor = ProfitEmerald)
                                )
                                Column {
                                    Text("صرف لبقالة", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("قيد دين مباشر", fontSize = 9.5.sp, color = Color(0xFF94A3B8))
                                }
                            }
                        }
                    }

                    if (destinationMode == 1 && retailers.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        ExposedDropdownMenuBox(
                            expanded = isRetailerDropdownExpanded,
                            onExpandedChange = { isRetailerDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedRetailer?.name ?: "اختر البقالة المستلمة",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("البقالة المستلمة") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isRetailerDropdownExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = isRetailerDropdownExpanded,
                                onDismissRequest = { isRetailerDropdownExpanded = false }
                            ) {
                                retailers.forEach { ret ->
                                    DropdownMenuItem(
                                        text = { Text("${ret.name} - ${ret.location}") },
                                        onClick = {
                                            selectedRetailer = ret
                                            isRetailerDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!isSaving && categoryName.isNotBlank() && quantity > 0) {
                        isSaving = true
                        val targetRetId = if (destinationMode == 1) selectedRetailer?.id else null
                        onConfirm(
                            categoryName.trim(),
                            quantity,
                            retailPrice,
                            wholesalePrice,
                            batchName.trim(),
                            targetRetId
                        )
                    }
                },
                enabled = !isSaving && categoryName.isNotBlank() && quantity > 0,
                colors = ButtonDefaults.buttonColors(containerColor = ProfitEmerald),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("جاري الحفظ والإضافة...", color = Color.White, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    val targetName = if (destinationMode == 1 && selectedRetailer != null) "لـ (${selectedRetailer?.name})" else "للمخزن"
                    Text("إضافة $quantity كرت $targetName", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
