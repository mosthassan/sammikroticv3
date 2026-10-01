package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CardPackageEntity
import com.example.data.local.entity.InventoryItemEntity
import com.example.data.local.entity.InventoryMovementEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * تبويب إدارة مخزن الكروت بالعدد والأصناف
 * - يعرض عدد الكروت المتوفرة في كل صنف بدقة
 * - ينقص العدد تلقائياً عند إصدار فواتير المبيعات
 * - يزداد عند توريد أو إضافة كميات جديدة للمخزن
 * - لا يتطلب توليد أرقام كروت أو تسلسلات، وفق متطلبات إدارة المستودعات والأنظمة المحاسبية
 */
@Composable
fun CardStockCategoryTab(
    viewModel: MainViewModel,
    onIssueInvoiceForPackage: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val inventoryItems by viewModel.inventoryItems.collectAsState()
    val packages by viewModel.cardPackages.collectAsState()
    val movements by viewModel.inventoryMovements.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showAddStockDialog by remember { mutableStateOf(false) }
    var selectedPackageForRestock by remember { mutableStateOf<String?>(null) }
    var selectedSubView by remember { mutableIntStateOf(0) } // 0: أرصدة الأصناف, 1: سجل حركات المخزن

    // حساب الإحصائيات العامة للمخزون
    val totalStockCards = inventoryItems.sumOf { it.quantityAvailable }
    val totalInventoryValuation = inventoryItems.sumOf { it.quantityAvailable * it.wholesalePriceDouble }
    val lowStockCount = inventoryItems.count { it.quantityAvailable in 1..50 }
    val outOfStockCount = inventoryItems.count { it.quantityAvailable == 0 }

    val filteredItems = inventoryItems.filter {
        searchQuery.isBlank() || it.packageName.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("card_stock_category_tab")
    ) {
        // KPI Summary Cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // إجمالي كروت المخزن
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, MikroTikPrimary.copy(alpha = 0.4f)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Inventory2, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إجمالي الكروت", fontFamily = CairoFontFamily, fontSize = 11.sp, color = TextSecondaryDark)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$totalStockCards كرت",
                        fontFamily = CairoFontFamily,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                }
            }

            // القيمة بسعر الجملة
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, ProfitEmerald.copy(alpha = 0.4f)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = ProfitEmerald, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("قيمة المخزون", fontFamily = CairoFontFamily, fontSize = 11.sp, color = TextSecondaryDark)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${totalInventoryValuation.toInt()} ر.ي",
                        fontFamily = CairoFontFamily,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = ProfitEmerald
                    )
                }
            }

            // عدد الأصناف
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.weight(0.9f)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Category, contentDescription = null, tint = Color(0xFFA855F7), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("الأصناف", fontFamily = CairoFontFamily, fontSize = 11.sp, color = TextSecondaryDark)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${inventoryItems.size} صنف",
                        fontFamily = CairoFontFamily,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA855F7)
                    )
                }
            }
        }

        // Sub Navigation & Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // View Mode Toggles
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberDarkSurface)
                    .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (selectedSubView == 0) MikroTikPrimary else Color.Transparent,
                    onClick = { selectedSubView = 0 }
                ) {
                    Text(
                        text = "رصيد الأصناف (${inventoryItems.size})",
                        fontFamily = CairoFontFamily,
                        fontSize = 11.5.sp,
                        fontWeight = if (selectedSubView == 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedSubView == 0) Color.White else TextSecondaryDark,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (selectedSubView == 1) MikroTikPrimary else Color.Transparent,
                    onClick = { selectedSubView = 1 }
                ) {
                    Text(
                        text = "سجل حركات المخزن",
                        fontFamily = CairoFontFamily,
                        fontSize = 11.5.sp,
                        fontWeight = if (selectedSubView == 1) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedSubView == 1) Color.White else TextSecondaryDark,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // Quick Add Stock Button
            Button(
                onClick = {
                    selectedPackageForRestock = null
                    showAddStockDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = ProfitEmerald),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.Black)
                Spacer(modifier = Modifier.width(4.dp))
                Text("توريد رصيد بالعدد", fontFamily = CairoFontFamily, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }

        if (selectedSubView == 0) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث عن صنف أو باقة...", fontFamily = CairoFontFamily, fontSize = 12.sp, color = TextSecondaryDark) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondaryDark, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = null, tint = TextSecondaryDark, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MikroTikPrimary,
                    unfocusedBorderColor = CyberBorder,
                    focusedContainerColor = CyberDarkSurface,
                    unfocusedContainerColor = CyberDarkSurface,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            )

            // Items List
            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Inventory2, contentDescription = null, tint = TextSecondaryDark, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "لا توجد أصناف مطابقة للبحث" else "المخزن فارغ حالياً",
                            fontFamily = CairoFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "اضغط على زر (توريد رصيد بالعدد) لإضافة رصيد كروت لأي صنف مباشرة دون الحاجة لتوليد أرقام",
                            fontFamily = CairoFontFamily,
                            fontSize = 12.sp,
                            color = TextSecondaryDark,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        CategoryStockCard(
                            item = item,
                            onAddStock = {
                                selectedPackageForRestock = item.packageName
                                showAddStockDialog = true
                            },
                            onIssueInvoice = {
                                onIssueInvoiceForPackage(item.packageName)
                            }
                        )
                    }
                }
            }
        } else {
            // سجل حركات المخزن
            if (movements.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا توجد حركات مخزنية مسجلة بعد.", fontFamily = CairoFontFamily, color = TextSecondaryDark)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(movements, key = { it.id }) { mov ->
                        MovementItemRow(movement = mov)
                    }
                }
            }
        }
    }

    // نافذة توريد وإضافة كروت بالعدد فقط للمخزن
    if (showAddStockDialog) {
        QuickStockSupplyDialog(
            packages = packages,
            initialPackageName = selectedPackageForRestock,
            existingItems = inventoryItems,
            onDismiss = { showAddStockDialog = false },
            onConfirm = { pkgName, qty, wholesale, retail, notes ->
                viewModel.addStockToInventory(
                    packageName = pkgName,
                    quantity = qty,
                    wholesalePrice = wholesale,
                    retailPrice = retail,
                    notes = notes
                ) { newBalance ->
                    showAddStockDialog = false
                    Toast.makeText(context, "تم توريد $qty كرت لصنف ($pkgName) بنجاح! الرصيد الجديد: $newBalance كرت ✓", Toast.LENGTH_LONG).show()
                }
            }
        )
    }
}

/**
 * كرت عرض الصنف ورصيده في المخزن
 */
@Composable
private fun CategoryStockCard(
    item: InventoryItemEntity,
    onAddStock: () -> Unit,
    onIssueInvoice: () -> Unit
) {
    val isOutOfStock = item.quantityAvailable == 0
    val isLowStock = item.quantityAvailable in 1..50
    val stockColor = when {
        isOutOfStock -> Color(0xFFEF4444)
        isLowStock -> StatusWarning
        else -> ProfitEmerald
    }
    val stockStatusText = when {
        isOutOfStock -> "نفد من المخزن"
        isLowStock -> "مخزون منخفض"
        else -> "متوفر بالمخزن"
    }

    val totalWholesaleValuation = item.quantityAvailable * item.wholesalePriceDouble
    val totalRetailValuation = item.quantityAvailable * item.retailPriceDouble
    val unitProfit = (item.retailPriceDouble - item.wholesalePriceDouble).coerceAtLeast(0.0)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF091222)),
        border = BorderStroke(1.2.dp, if (isOutOfStock) Color(0xFFEF4444).copy(alpha = 0.5f) else Color(0xFF1B2C4B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row: Item Title & Stock Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(stockColor.copy(alpha = 0.16f))
                            .border(1.dp, stockColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = stockColor, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = item.packageName,
                            fontFamily = CairoFontFamily,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = stockStatusText,
                            fontFamily = CairoFontFamily,
                            fontSize = 11.sp,
                            color = stockColor
                        )
                    }
                }

                // عدد الكروت الحالي بالمخزن
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = stockColor.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, stockColor.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${item.quantityAvailable}",
                            fontFamily = CairoFontFamily,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black,
                            color = stockColor
                        )
                        Text(
                            text = "كرت متوفر",
                            fontFamily = CairoFontFamily,
                            fontSize = 10.sp,
                            color = stockColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pricing & Valuation Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF050B15))
                    .border(1.dp, Color(0xFF1B2C4B), RoundedCornerShape(12.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("سعر الجملة", fontFamily = CairoFontFamily, fontSize = 10.sp, color = TextSecondaryDark)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("${item.wholesalePrice.toInt()} ر.ي", fontFamily = CairoFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                }
                Column {
                    Text("سعر التجزئة", fontFamily = CairoFontFamily, fontSize = 10.sp, color = TextSecondaryDark)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("${item.retailPrice.toInt()} ر.ي", fontFamily = CairoFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Column {
                    Text("ربح الكرت", fontFamily = CairoFontFamily, fontSize = 10.sp, color = TextSecondaryDark)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("+${unitProfit.toInt()} ر.ي", fontFamily = CairoFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ProfitEmerald)
                }
                Column {
                    Text("قيمة الرصيد", fontFamily = CairoFontFamily, fontSize = 10.sp, color = TextSecondaryDark)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("${totalWholesaleValuation.toInt()} ر.ي", fontFamily = CairoFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA855F7))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // توريد رصيد
                Surface(
                    onClick = onAddStock,
                    shape = RoundedCornerShape(10.dp),
                    color = ProfitEmerald.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, ProfitEmerald.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = ProfitEmerald, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("توريد رصيد (+)", color = Color.White, fontFamily = CairoFontFamily, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // إصدار فاتورة بيع
                Surface(
                    onClick = onIssueInvoice,
                    enabled = item.quantityAvailable > 0,
                    shape = RoundedCornerShape(10.dp),
                    color = if (item.quantityAvailable > 0) Color(0xFF0284C7).copy(alpha = 0.2f) else Color(0xFF1E293B).copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, if (item.quantityAvailable > 0) Color(0xFF00E5FF).copy(alpha = 0.5f) else Color(0xFF334155)),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = if (item.quantityAvailable > 0) MikroTikCyan else TextSecondaryDark,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "فاتورة بيع (-)",
                            color = if (item.quantityAvailable > 0) MikroTikCyan else TextSecondaryDark,
                            fontFamily = CairoFontFamily,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * سطر حركة مخزنية في السجل
 */
@Composable
private fun MovementItemRow(movement: InventoryMovementEntity) {
    val isSupply = movement.movementType == "SUPPLY"
    val badgeColor = if (isSupply) ProfitEmerald else Color(0xFF38BDF8)
    val icon = if (isSupply) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward
    val typeTitle = if (isSupply) "توريد للمخزن" else "صرف بفاتورة مبيعات"

    val dateFormatted = remember(movement.timestamp) {
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        sdf.format(Date(movement.timestamp))
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = CyberDarkSurface,
        border = BorderStroke(1.dp, CyberBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = movement.packageName,
                        fontFamily = CairoFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "$typeTitle | ${movement.referenceNumber} | $dateFormatted",
                        fontFamily = CairoFontFamily,
                        fontSize = 10.sp,
                        color = TextSecondaryDark
                    )
                    if (movement.notes.isNotBlank()) {
                        Text(
                            text = movement.notes,
                            fontFamily = CairoFontFamily,
                            fontSize = 9.5.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (movement.quantityChange > 0) "+${movement.quantityChange}" else "${movement.quantityChange}",
                    fontFamily = CairoFontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor
                )
                Text(
                    text = "الرصيد: ${movement.resultingBalance}",
                    fontFamily = CairoFontFamily,
                    fontSize = 10.sp,
                    color = TextSecondaryDark
                )
            }
        }
    }
}

/**
 * نافذة توريد رصيد كروت بالعدد فقط دون اشتراط أرقام كروت
 */
@Composable
fun QuickStockSupplyDialog(
    packages: List<CardPackageEntity>,
    initialPackageName: String? = null,
    existingItems: List<InventoryItemEntity> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (packageName: String, qty: Int, wholesale: Double, retail: Double, notes: String) -> Unit
) {
    var selectedPackageName by remember {
        mutableStateOf(initialPackageName ?: packages.firstOrNull()?.name ?: existingItems.firstOrNull()?.packageName ?: "")
    }
    var customPackageName by remember { mutableStateOf("") }
    var isCustomPackage by remember { mutableStateOf(false) }

    val matchedPackage = packages.find { it.name == selectedPackageName }
    val matchedExisting = existingItems.find { it.packageName == selectedPackageName }

    var quantityText by remember { mutableStateOf("100") }
    var wholesalePriceText by remember {
        mutableStateOf(matchedPackage?.wholesalePrice?.toInt()?.toString() ?: matchedExisting?.wholesalePrice?.toInt()?.toString() ?: "180")
    }
    var retailPriceText by remember {
        mutableStateOf(matchedPackage?.retailPrice?.toInt()?.toString() ?: matchedExisting?.retailPrice?.toInt()?.toString() ?: "200")
    }
    var notesText by remember { mutableStateOf("") }

    val qty = quantityText.toIntOrNull() ?: 0
    val wholesale = wholesalePriceText.toDoubleOrNull() ?: 0.0
    val totalCost = qty * wholesale

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AddShoppingCart, contentDescription = null, tint = ProfitEmerald, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("توريد رصيد كروت بالعدد للمخزن", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Text(
                        text = "يتم تسجيل الكروت بالعدد مباشرة لربطها بفواتير المبيعات، دون الحاجة لإنشاء أرقام كروت.",
                        fontFamily = CairoFontFamily,
                        fontSize = 11.sp,
                        color = Color(0xFF38BDF8)
                    )
                }

                // اختيار الصنف
                item {
                    Text("الصنف / الباقة:", fontFamily = CairoFontFamily, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    if (!isCustomPackage && (packages.isNotEmpty() || existingItems.isNotEmpty())) {
                        val allNames = (packages.map { it.name } + existingItems.map { it.packageName }).distinct()
                        LazyColumn(modifier = Modifier.heightIn(max = 130.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(allNames) { name ->
                                val isSelected = selectedPackageName == name
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MikroTikPrimary.copy(alpha = 0.2f) else CyberDarkSurface,
                                    border = BorderStroke(1.dp, if (isSelected) MikroTikPrimary else CyberBorder),
                                    onClick = {
                                        selectedPackageName = name
                                        val p = packages.find { it.name == name }
                                        val e = existingItems.find { it.packageName == name }
                                        wholesalePriceText = (p?.wholesalePrice?.toDouble() ?: e?.wholesalePriceDouble ?: 180.0).toInt().toString()
                                        retailPriceText = (p?.retailPrice?.toDouble() ?: e?.retailPriceDouble ?: 200.0).toInt().toString()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(name, fontFamily = CairoFontFamily, fontSize = 12.sp, color = if (isSelected) Color.White else TextSecondaryDark)
                                        if (isSelected) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = MikroTikPrimary, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    TextButton(onClick = { isCustomPackage = !isCustomPackage }) {
                        Text(
                            text = if (isCustomPackage) "← اختيار من الأصناف المسجلة" else "+ إدخال اسم صنف مخصص جديد",
                            fontFamily = CairoFontFamily,
                            fontSize = 11.sp,
                            color = Color(0xFF38BDF8)
                        )
                    }

                    if (isCustomPackage) {
                        OutlinedTextField(
                            value = customPackageName,
                            onValueChange = { customPackageName = it },
                            label = { Text("اسم الصنف الجديد (مثال: فئة 250 ريال)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // الكمية المضافة
                item {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text("عدد الكروت الموردة للمخزن") },
                        placeholder = { Text("مثال: 500") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // الأسعار
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = wholesalePriceText,
                            onValueChange = { wholesalePriceText = it },
                            label = { Text("سعر الجملة (ر.ي)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = retailPriceText,
                            onValueChange = { retailPriceText = it },
                            label = { Text("سعر التجزئة (ر.ي)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // ملاحظات
                item {
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text("ملاحظات التوريد (اختياري)") },
                        placeholder = { Text("مثال: دفعة جديدة من المطبعة أو المورد") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // إجمالي القيمة التقديرية للتوريد
                item {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0F1E33),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("إجمالي تكلفة الدفعة بالجملة:", fontFamily = CairoFontFamily, fontSize = 11.5.sp, color = TextSecondaryDark)
                            Text("${totalCost.toInt()} ريال", fontFamily = CairoFontFamily, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = ProfitEmerald)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalName = if (isCustomPackage) customPackageName.trim() else selectedPackageName
                    val finalQty = quantityText.toIntOrNull() ?: 0
                    val finalWholesale = wholesalePriceText.toDoubleOrNull() ?: 0.0
                    val finalRetail = retailPriceText.toDoubleOrNull() ?: 0.0

                    if (finalName.isNotBlank() && finalQty > 0) {
                        onConfirm(finalName, finalQty, finalWholesale, finalRetail, notesText)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ProfitEmerald),
                enabled = (if (isCustomPackage) customPackageName.isNotBlank() else selectedPackageName.isNotBlank()) && qty > 0
            ) {
                Text("تأكيد التوريد وإضافة الرصيد", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", fontFamily = CairoFontFamily)
            }
        }
    )
}
