package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.NetworkAssetEntity
import com.example.data.local.entity.PurchaseInvoiceEntity
import com.example.data.model.ParsedInvoiceData
import com.example.ui.MainViewModel
import com.example.ui.theme.AssetPurple
import com.example.ui.theme.CairoFontFamily
import com.example.ui.theme.InvestmentGold
import com.example.ui.theme.MikroTikCyan
import com.example.ui.theme.MikroTikNavy
import com.example.ui.theme.MikroTikPrimary
import com.example.ui.theme.PaymentRed
import com.example.ui.theme.ProfitEmerald
import com.example.ui.theme.StatusOnline
import com.example.util.CurrencyHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AssetsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val assets by viewModel.assets.collectAsState()
    val invoices by viewModel.invoices.collectAsState()
    val totalPurchaseCost by viewModel.totalAssetPurchaseCost.collectAsState()
    val totalCurrentAssetValue by viewModel.totalCurrentAssetValue.collectAsState()
    val networkIdentity by viewModel.networkIdentity.collectAsState()
    val context = LocalContext.current

    var selectedTabIndex by remember { mutableStateOf(0) } // 0: الأصول الرأسمالية, 1: فواتير المشتريات
    var selectedCategoryFilter by remember { mutableStateOf("الكل") }
    var showAddAssetDialog by remember { mutableStateOf(false) }
    var showInvoiceScannerDialog by remember { mutableStateOf(false) }
    var showJsonImportDialog by remember { mutableStateOf(false) }
    var parsedInvoiceForAssets by remember { mutableStateOf<ParsedInvoiceData?>(null) }
    var assetToEdit by remember { mutableStateOf<NetworkAssetEntity?>(null) }
    var assetToDelete by remember { mutableStateOf<NetworkAssetEntity?>(null) }
    var invoiceToEdit by remember { mutableStateOf<PurchaseInvoiceEntity?>(null) }
    var invoiceToDelete by remember { mutableStateOf<PurchaseInvoiceEntity?>(null) }

    val categories = listOf("الكل", "سيرفرات وراوترات", "أبراج وهوائيات", "طاقة شمسية وبطاريات", "كابلات وألياف", "أخرى")

    val filteredAssets = assets.filter { asset ->
        when (selectedCategoryFilter) {
            "سيرفرات وراوترات" -> asset.category == "SERVERS"
            "أبراج وهوائيات" -> asset.category == "TOWERS"
            "طاقة شمسية وبطاريات" -> asset.category == "SOLAR_POWER"
            "كابلات وألياف" -> asset.category == "FIBER_CABLES"
            "أخرى" -> asset.category == "OTHER"
            else -> true
        }
    }

    val costTotal = totalPurchaseCost ?: 0.0
    val currentValTotal = totalCurrentAssetValue ?: 0.0
    val totalDepreciation = (costTotal - currentValTotal).coerceAtLeast(0.0)

    Column(modifier = modifier.fillMaxSize().testTag("assets_screen")) {
        // TabRow في أعلى الشاشة
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = AssetPurple,
            indicator = { tabPositions ->
                if (selectedTabIndex < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = AssetPurple,
                        height = 3.dp
                    )
                }
            }
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Devices, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "الأصول الرأسمالية",
                            fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp,
                            fontFamily = CairoFontFamily
                        )
                    }
                }
            )

            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "فواتير المشتريات",
                            fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp,
                            fontFamily = CairoFontFamily
                        )
                    }
                }
            )
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (selectedTabIndex == 0) {
                // تبويب الأصول الرأسمالية
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Header Hero Card
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MikroTikNavy),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "سجل الأصول الرأسمالية (CAPEX)",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 17.sp,
                                                fontFamily = CairoFontFamily
                                            )
                                            Text(
                                                text = "حصر أجهزة ومعدات وأبراج ومنظومات طاقة الشبكة",
                                                color = Color(0xFF94A3B8),
                                                fontSize = 12.sp,
                                                fontFamily = CairoFontFamily
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(AssetPurple.copy(alpha = 0.25f))
                                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                        ) {
                                            Text(
                                                text = "${assets.size} أصل ثابت",
                                                color = AssetPurple,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = CairoFontFamily
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Stats Grid
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Total Purchase Cost
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text("تكلفة الشراء الأصلية", color = Color(0xFF94A3B8), fontSize = 11.sp, fontFamily = CairoFontFamily)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "${String.format(Locale.US, "%,.0f", costTotal)} ر.ي",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    fontFamily = CairoFontFamily
                                                )
                                            }
                                        }

                                        // Current Estimated Value
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text("القيمة السوقية الحالية", color = Color(0xFF94A3B8), fontSize = 11.sp, fontFamily = CairoFontFamily)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "${String.format(Locale.US, "%,.0f", currentValTotal)} ر.ي",
                                                    color = ProfitEmerald,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    fontFamily = CairoFontFamily
                                                )
                                            }
                                        }

                                        // Depreciation
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text("مجموع الإهلاك التقديري", color = Color(0xFF94A3B8), fontSize = 11.sp, fontFamily = CairoFontFamily)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "${String.format(Locale.US, "%,.0f", totalDepreciation)} ر.ي",
                                                    color = InvestmentGold,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    fontFamily = CairoFontFamily
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // AI & JSON Invoice Scanning Quick Banner
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = AssetPurple.copy(alpha = 0.15f)),
                                border = BorderStroke(1.dp, AssetPurple.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(AssetPurple.copy(alpha = 0.3f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CameraAlt,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "إدخال وتفريغ فواتير الأصول والمعدات",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = Color.White,
                                                    fontFamily = CairoFontFamily
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = Icons.Default.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = InvestmentGold,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                            Text(
                                                text = "صور الفاتورة بالذكاء الاصطناعي أو استورد ملف JSON لتوفير رصيد التوكن",
                                                fontSize = 11.sp,
                                                color = Color(0xFFCBD5E1),
                                                fontFamily = CairoFontFamily
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                parsedInvoiceForAssets = null
                                                showInvoiceScannerDialog = true
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = AssetPurple),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(38.dp)
                                                .testTag("scan_asset_invoice_button")
                                        ) {
                                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("مسح بالذكاء 📸", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, fontFamily = CairoFontFamily)
                                        }

                                        Button(
                                            onClick = { showJsonImportDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = ProfitEmerald),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .weight(1.2f)
                                                .height(38.dp)
                                                .testTag("import_asset_json_button")
                                        ) {
                                            Icon(Icons.Default.Description, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("استيراد JSON (بدون توكن) ⚡", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = CairoFontFamily)
                                        }
                                    }
                                }
                            }
                        }

                        // Category Filter Chips
                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(categories) { cat ->
                                    val isSelected = selectedCategoryFilter == cat
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(
                                                if (isSelected) AssetPurple else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .clickable { selectedCategoryFilter = cat }
                                            .padding(horizontal = 14.dp, vertical = 7.dp)
                                    ) {
                                        Text(
                                            text = cat,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontFamily = CairoFontFamily
                                        )
                                    }
                                }
                            }
                        }

                        // Assets List
                        if (filteredAssets.isEmpty()) {
                            item {
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.Devices, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("لا توجد أصول مسجلة في هذا التصنيف", fontWeight = FontWeight.Bold, fontFamily = CairoFontFamily)
                                        Text(
                                            text = "استخدم زر (+) لإضافة الأجهزة والمعدات لحساب أصول الشبكة بدقة.",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                            fontFamily = CairoFontFamily
                                        )
                                    }
                                }
                            }
                        } else {
                            items(filteredAssets, key = { it.id }) { asset ->
                                AssetCard(
                                    asset = asset,
                                    onClone = {
                                        assetToEdit = asset.copy(
                                            id = 0L,
                                            assetName = "${asset.assetName} (نسخة)",
                                            serialNumber = "",
                                            purchaseDateMillis = System.currentTimeMillis()
                                        )
                                        showAddAssetDialog = true
                                        Toast.makeText(context, "تم استنساخ الأصل. قم بتعديل التفاصيل ثم احفظ", Toast.LENGTH_SHORT).show()
                                    },
                                    onEdit = {
                                        assetToEdit = asset
                                        showAddAssetDialog = true
                                    },
                                    onDelete = {
                                        assetToDelete = asset
                                    },
                                    onShare = {
                                        val shareText = """
                                            🛠 كشف أصل رأسمالي لشبكة الوايرلس:
                                            🏷 الاسم: ${asset.assetName}
                                            🏢 التصنيف: ${asset.category}
                                            💵 تكلفة الشراء: ${String.format(Locale.US, "%,.0f", asset.purchaseCost)} ريال
                                            📉 القيمة الدفترية الحالية: ${String.format(Locale.US, "%,.0f", asset.estimatedCurrentValue)} ريال
                                            📍 الموقع: ${asset.location.ifBlank { "غير محدد" }}
                                            🔢 السيريال / الموديل: ${asset.serialNumber.ifBlank { "غير متوفر" }}
                                            ⚡ الحالة: ${if (asset.status == "ACTIVE") "يعمل بكفاءة" else "صيانة / مستهلك"}
                                            📝 ملاحظات: ${asset.notes.ifBlank { "أصل مسجل ومعتمد" }}
                                        """.trimIndent()
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "مشاركة بيانات الأصل"))
                                    }
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(72.dp))
                        }
                    }

                    // Floating Buttons for Assets
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FloatingActionButton(
                            onClick = { showInvoiceScannerDialog = true },
                            containerColor = MikroTikPrimary,
                            contentColor = Color.White,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.testTag("fab_scan_asset_invoice")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("مسح فاتورة (AI)", fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = CairoFontFamily)
                            }
                        }

                        FloatingActionButton(
                            onClick = {
                                assetToEdit = null
                                showAddAssetDialog = true
                            },
                            containerColor = AssetPurple,
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier.testTag("fab_add_asset")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "إضافة أصل يدوي")
                        }
                    }
                }
            } else {
                // تبويب فواتير المشتريات
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Header Hero Card for Invoices
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MikroTikNavy),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "فواتير المشتريات والأصول",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 17.sp,
                                                fontFamily = CairoFontFamily
                                            )
                                            Text(
                                                text = "سجل كافة الفواتير المسحوبة والمدخلة في النظام",
                                                color = Color(0xFF94A3B8),
                                                fontSize = 12.sp,
                                                fontFamily = CairoFontFamily
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(AssetPurple.copy(alpha = 0.25f))
                                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                        ) {
                                            Text(
                                                text = "${invoices.size} فاتورة",
                                                color = AssetPurple,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = CairoFontFamily
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // AI Quick Scan Banner for Invoices
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = AssetPurple.copy(alpha = 0.15f)),
                                border = BorderStroke(1.dp, AssetPurple.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = InvestmentGold, modifier = Modifier.size(22.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text("مسح وتسجيل فاتورة جديدة بالذكاء", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White, fontFamily = CairoFontFamily)
                                            Text("صور الفاتورة لتفريغها وتخزينها تلقائياً", fontSize = 11.sp, color = Color(0xFFCBD5E1), fontFamily = CairoFontFamily)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { showInvoiceScannerDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = AssetPurple),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("مسح", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, fontFamily = CairoFontFamily)
                                    }
                                }
                            }
                        }

                        // Invoices List
                        if (invoices.isEmpty()) {
                            item {
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("لا توجد فواتير مشتريات مسجلة", fontWeight = FontWeight.Bold, fontFamily = CairoFontFamily)
                                        Text(
                                            text = "يمكنك مسح وتفريغ فواتير المشتريات بالذكاء الاصطناعي لحساب تكاليف النظام بكفاءة.",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                            fontFamily = CairoFontFamily
                                        )
                                    }
                                }
                            }
                        } else {
                            items(invoices, key = { it.id }) { invoice ->
                                AssetInvoiceCard(
                                    invoice = invoice,
                                    onEdit = { invoiceToEdit = invoice },
                                    onDelete = { invoiceToDelete = invoice },
                                    onShare = {
                                        shareInvoice(context, invoice)
                                    }
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(72.dp))
                        }
                    }

                    // Floating Action Button for Invoices
                    FloatingActionButton(
                        onClick = { showInvoiceScannerDialog = true },
                        containerColor = AssetPurple,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(20.dp)
                            .testTag("fab_scan_invoice_tab")
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "مسح فاتورة")
                    }
                }
            }
        }
    }

    // Smart Invoice AI / JSON Scanner Dialog
    if (showInvoiceScannerDialog) {
        SmartInvoiceScannerDialog(
            viewModel = viewModel,
            initialTargetType = "ASSETS",
            initialParsedData = parsedInvoiceForAssets,
            onDismissRequest = {
                showInvoiceScannerDialog = false
                parsedInvoiceForAssets = null
            }
        )
    }

    // Direct JSON Invoice Import Dialog
    if (showJsonImportDialog) {
        JsonInvoiceImportDialog(
            viewModel = viewModel,
            onDismissRequest = { showJsonImportDialog = false },
            onInvoiceImported = { importedData ->
                parsedInvoiceForAssets = importedData
                showInvoiceScannerDialog = true
                showJsonImportDialog = false
            }
        )
    }

    // Add / Edit Asset Dialog
    if (showAddAssetDialog) {
        AddEditAssetDialog(
            assetToEdit = assetToEdit,
            sarToYerRate = if (networkIdentity.sarToYerRate > 0) networkIdentity.sarToYerRate else 430.0,
            usdToYerRate = if (networkIdentity.usdToYerRate > 0) networkIdentity.usdToYerRate else 1630.0,
            onDismiss = { showAddAssetDialog = false },
            onSave = { asset ->
                viewModel.saveAsset(asset) {
                    Toast.makeText(context, "تم حفظ الأصل الرأسمالي بنجاح", Toast.LENGTH_SHORT).show()
                }
                showAddAssetDialog = false
            }
        )
    }

    // Edit Purchase Invoice Dialog
    if (invoiceToEdit != null) {
        SmartInvoiceScannerDialog(
            viewModel = viewModel,
            initialTargetType = invoiceToEdit!!.targetType,
            invoiceToEdit = invoiceToEdit,
            onDismissRequest = { invoiceToEdit = null }
        )
    }

    // Delete Asset Dialog
    if (assetToDelete != null) {
        AlertDialog(
            onDismissRequest = { assetToDelete = null },
            title = { Text("حذف الأصل من السجل", fontWeight = FontWeight.Bold, fontFamily = CairoFontFamily) },
            text = { Text("هل أنت متأكد من حذف الأصل (${assetToDelete?.assetName})؟", fontFamily = CairoFontFamily) },
            confirmButton = {
                Button(
                    onClick = {
                        assetToDelete?.let { viewModel.deleteAsset(it) }
                        assetToDelete = null
                        Toast.makeText(context, "تم حذف الأصل بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PaymentRed)
                ) {
                    Text("تأكيد الحذف", color = Color.White, fontFamily = CairoFontFamily)
                }
            },
            dismissButton = {
                TextButton(onClick = { assetToDelete = null }) {
                    Text("إلغاء", fontFamily = CairoFontFamily)
                }
            }
        )
    }

    // Delete Purchase Invoice Dialog
    if (invoiceToDelete != null) {
        AlertDialog(
            onDismissRequest = { invoiceToDelete = null },
            title = { Text("حذف الفاتورة", fontWeight = FontWeight.Bold, fontFamily = CairoFontFamily) },
            text = {
                Text(
                    "هل أنت متأكد من حذف الفاتورة رقم (${invoiceToDelete?.invoiceNumber}) للمورد (${invoiceToDelete?.supplierName})؟",
                    fontFamily = CairoFontFamily
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        invoiceToDelete?.let { inv ->
                            viewModel.deleteInvoice(inv) {
                                Toast.makeText(context, "تم حذف الفاتورة بنجاح", Toast.LENGTH_SHORT).show()
                            }
                        }
                        invoiceToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PaymentRed)
                ) {
                    Text("تأكيد الحذف", color = Color.White, fontFamily = CairoFontFamily)
                }
            },
            dismissButton = {
                TextButton(onClick = { invoiceToDelete = null }) {
                    Text("إلغاء", fontFamily = CairoFontFamily)
                }
            }
        )
    }
}

@Composable
fun AssetInvoiceCard(
    invoice: PurchaseInvoiceEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    val dateStr = SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH).format(Date(invoice.invoiceDateMillis))
    val origAmount = if (invoice.originalAmount > java.math.BigDecimal.ZERO) invoice.originalAmount else invoice.totalAmount
    val currencySymbol = CurrencyHelper.getCurrencySymbol(invoice.currency)
    val formattedOriginalTotal = "${String.format(Locale.US, "%,.2f", origAmount).replace(".00", "")} $currencySymbol"

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("asset_invoice_card_${invoice.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Supplier Name, Invoice Number & Total in original currency
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(AssetPurple.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = AssetPurple,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = invoice.supplierName.ifBlank { "مورد غير محدد" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = CairoFontFamily
                        )
                        Text(
                            text = "رقم الفاتورة: ${invoice.invoiceNumber.ifBlank { "بدون رقم" }}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = CairoFontFamily
                        )
                    }
                }

                // Total in Original Currency Badge
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "الإجمالي بالعملة الأصلية",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = CairoFontFamily
                    )
                    Text(
                        text = formattedOriginalTotal,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = ProfitEmerald,
                        fontFamily = CairoFontFamily
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Details Row: Date & Items summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "التاريخ: $dateStr",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = CairoFontFamily
                    )
                }

                if (invoice.currency != "YER" && invoice.totalAmount > java.math.BigDecimal.ZERO) {
                    Text(
                        text = "(${String.format(Locale.US, "%,.0f", invoice.totalAmount)} ر.ي)",
                        fontSize = 11.sp,
                        color = InvestmentGold,
                        fontWeight = FontWeight.Bold,
                        fontFamily = CairoFontFamily
                    )
                }
            }

            if (invoice.itemsSummary.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "الأصناف: ${invoice.itemsSummary}",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    fontFamily = CairoFontFamily
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons Row: Edit (تعديل), Delete (حذف), Share (مشاركة)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onShare,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "مشاركة", tint = MikroTikCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("مشاركة", fontSize = 11.5.sp, color = MikroTikCyan, fontFamily = CairoFontFamily)
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تعديل", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurface, fontFamily = CairoFontFamily)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = PaymentRed.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = PaymentRed, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("حذف", fontSize = 11.5.sp, color = PaymentRed, fontWeight = FontWeight.Bold, fontFamily = CairoFontFamily)
                }
            }
        }
    }
}

private fun shareInvoice(context: android.content.Context, invoice: PurchaseInvoiceEntity) {
    val dateStr = SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH).format(Date(invoice.invoiceDateMillis))
    val origAmount = if (invoice.originalAmount > java.math.BigDecimal.ZERO) invoice.originalAmount else invoice.totalAmount
    val currencySymbol = CurrencyHelper.getCurrencySymbol(invoice.currency)
    val formattedOriginalTotal = "${String.format(Locale.US, "%,.2f", origAmount).replace(".00", "")} $currencySymbol"

    val shareText = """
        🧾 *فاتورة مشتريات - سام ميكروتك*
        ------------------------------------
        🔢 *رقم الفاتورة:* ${invoice.invoiceNumber.ifBlank { "غير محدد" }}
        🏢 *اسم المورد:* ${invoice.supplierName.ifBlank { "غير محدد" }}
        📅 *التاريخ:* $dateStr
        💵 *الإجمالي بالعملة الأصلية:* $formattedOriginalTotal
        ${if (invoice.itemsSummary.isNotBlank()) "📦 *الأصناف:* ${invoice.itemsSummary}" else ""}
        ${if (invoice.notes.isNotBlank()) "📝 *ملاحظات:* ${invoice.notes}" else ""}
    """.trimIndent()

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, shareText)
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, "مشاركة بيانات الفاتورة"))
}

@Composable
fun AssetCard(
    asset: NetworkAssetEntity,
    onClone: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    val categoryIcon = when (asset.category) {
        "SERVERS" -> Icons.Default.Router
        "TOWERS" -> Icons.Default.Devices
        "SOLAR_POWER" -> Icons.Default.SolarPower
        "FIBER_CABLES" -> Icons.Default.Cable
        else -> Icons.Default.Build
    }

    val isOnline = asset.status == "ACTIVE"

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(AssetPurple.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(categoryIcon, contentDescription = null, tint = AssetPurple, modifier = Modifier.size(20.dp))
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = asset.assetName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = CairoFontFamily
                        )
                        if (asset.location.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = asset.location,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontFamily = CairoFontFamily
                                )
                            }
                        }
                    }
                }

                // Status chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isOnline) StatusOnline.copy(alpha = 0.15f) else PaymentRed.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isOnline) "يعمل بكفاءة" else "تحت الصيانة",
                        color = if (isOnline) StatusOnline else PaymentRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = CairoFontFamily
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Values
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("سعر الشراء", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = CairoFontFamily)
                    Text(
                        text = "${String.format(Locale.US, "%,.0f", asset.purchaseCost)} ر.ي",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = CairoFontFamily
                    )
                    if (asset.currency.isNotBlank() && asset.currency != "YER" && asset.originalCost > 0) {
                        Text(
                            text = CurrencyHelper.formatAmount(asset.originalCost, asset.currency),
                            fontSize = 10.sp,
                            color = InvestmentGold,
                            fontWeight = FontWeight.Bold,
                            fontFamily = CairoFontFamily
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("القيمة الحالية (بعد الإهلاك)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = CairoFontFamily)
                    Text(
                        text = "${String.format(Locale.US, "%,.0f", asset.estimatedCurrentValue)} ر.ي",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = ProfitEmerald,
                        fontFamily = CairoFontFamily
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("تاريخ الشراء", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = CairoFontFamily)
                    Text(
                        text = SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(asset.purchaseDateMillis)),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = CairoFontFamily
                    )
                }
            }

            if (asset.notes.isNotBlank() || asset.serialNumber.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${if (asset.serialNumber.isNotBlank()) "S/N: ${asset.serialNumber} • " else ""}${asset.notes}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = CairoFontFamily
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onShare, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Share, contentDescription = "مشاركة", tint = MikroTikCyan, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onClone, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "استنساخ الأصل", tint = InvestmentGold, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = PaymentRed, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAssetDialog(
    assetToEdit: NetworkAssetEntity?,
    sarToYerRate: Double,
    usdToYerRate: Double,
    onDismiss: () -> Unit,
    onSave: (NetworkAssetEntity) -> Unit
) {
    var isSaving by remember { mutableStateOf(false) }
    var name by remember(assetToEdit) { mutableStateOf(assetToEdit?.assetName ?: "") }
    var category by remember(assetToEdit) { mutableStateOf(assetToEdit?.category ?: "SERVERS") }
    var selectedCurrency by remember(assetToEdit) { mutableStateOf(assetToEdit?.currency?.ifBlank { "YER" } ?: "YER") }

    val initialCost = if (assetToEdit != null && assetToEdit.currency != "YER" && assetToEdit.originalCost > 0) {
        assetToEdit.originalCost
    } else {
        assetToEdit?.purchaseCost ?: 0.0
    }
    var purchaseCostText by remember(assetToEdit) { mutableStateOf(if (initialCost > 0) initialCost.toInt().toString() else "") }
    var currentValueText by remember(assetToEdit) { mutableStateOf(assetToEdit?.estimatedCurrentValue?.toInt()?.toString() ?: "") }
    var location by remember(assetToEdit) { mutableStateOf(assetToEdit?.location ?: "") }
    var serialNumber by remember(assetToEdit) { mutableStateOf(assetToEdit?.serialNumber ?: "") }
    var notes by remember(assetToEdit) { mutableStateOf(assetToEdit?.notes ?: "") }

    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }

    val categoryMap = listOf(
        "SERVERS" to "سيرفرات وراوترات ومفاتيح ميكروتك",
        "TOWERS" to "أبراج وهوائيات وسيكتورات",
        "SOLAR_POWER" to "منظومة طاقة شمسية وبطاريات ومولدات",
        "FIBER_CABLES" to "كابلات ألياف بصرية وفايبر",
        "OTHER" to "أدوات ومعدات أخرى"
    )

    val rawEnteredCost = purchaseCostText.toDoubleOrNull() ?: 0.0
    val equivalentYerCost = CurrencyHelper.convertToYer(rawEnteredCost, selectedCurrency, sarToYerRate, usdToYerRate)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, AssetPurple.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 8.dp)
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
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
                                .background(AssetPurple.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Storage, contentDescription = null, tint = AssetPurple, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (assetToEdit == null || assetToEdit.id == 0L) {
                                    if (assetToEdit?.id == 0L) "استنساخ وتسجيل أصل جديد" else "إضافة أصل رأسمالي جديد"
                                } else "تعديل بيانات الأصل",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontFamily = CairoFontFamily
                            )
                            Text(
                                text = "تسجيل الأصول الثابتة وحساب الإهلاك والقيمة الرأسمالية",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = CairoFontFamily
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Form Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم الأصل والموديل *", fontFamily = CairoFontFamily) },
                        placeholder = { Text("مثال: راوتر CCR2004، بطارية 200A...", fontFamily = CairoFontFamily) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_asset_name")
                    )

                    // Category Dropdown
                    ExposedDropdownMenuBox(
                        expanded = isCategoryDropdownExpanded,
                        onExpandedChange = { isCategoryDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = categoryMap.firstOrNull { it.first == category }?.second ?: category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("تصنيف الأصل", fontFamily = CairoFontFamily) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryDropdownExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = isCategoryDropdownExpanded,
                            onDismissRequest = { isCategoryDropdownExpanded = false }
                        ) {
                            categoryMap.forEach { (catKey, catLabel) ->
                                DropdownMenuItem(
                                    text = { Text(catLabel, fontFamily = CairoFontFamily) },
                                    onClick = {
                                        category = catKey
                                        isCategoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Currency selector chips
                    Column {
                        Text("عملة الشراء والفاتورة:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = CairoFontFamily)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                CurrencyHelper.CURRENCY_YER to "ريال يمني",
                                CurrencyHelper.CURRENCY_SAR to "ريال سعودي",
                                CurrencyHelper.CURRENCY_USD to "دولار أمريكي"
                            ).forEach { (cKey, cLabel) ->
                                val isSelected = selectedCurrency == cKey
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) AssetPurple else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .clickable {
                                            selectedCurrency = cKey
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = cLabel,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontFamily = CairoFontFamily
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = purchaseCostText,
                            onValueChange = {
                                purchaseCostText = it
                                val entered = it.toDoubleOrNull() ?: 0.0
                                val inYer = CurrencyHelper.convertToYer(entered, selectedCurrency, sarToYerRate, usdToYerRate)
                                if (currentValueText.isEmpty()) currentValueText = inYer.toInt().toString()
                            },
                            label = { Text("سعر الشراء (${CurrencyHelper.getCurrencySymbol(selectedCurrency)}) *", fontFamily = CairoFontFamily) },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("input_asset_cost")
                        )

                        OutlinedTextField(
                            value = currentValueText,
                            onValueChange = { currentValueText = it },
                            label = { Text("القيمة الحالية (ريال يمني)", fontFamily = CairoFontFamily) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Currency conversion notice card
                    if (selectedCurrency != CurrencyHelper.CURRENCY_YER && rawEnteredCost > 0) {
                        val rate = if (selectedCurrency == CurrencyHelper.CURRENCY_USD) usdToYerRate else sarToYerRate
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = ProfitEmerald.copy(alpha = 0.1f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "المعادل بالريال اليمني (سعر الصرف: $rate):",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontFamily = CairoFontFamily
                                )
                                Text(
                                    text = "${String.format(Locale.US, "%,.0f", equivalentYerCost)} ر.ي",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = ProfitEmerald,
                                    fontFamily = CairoFontFamily
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("موقع التركيب (البرج / الغرفة)", fontFamily = CairoFontFamily) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = serialNumber,
                        onValueChange = { serialNumber = it },
                        label = { Text("الرقم التسلسلي S/N (اختياري)", fontFamily = CairoFontFamily) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("ملاحظات والضمان", fontFamily = CairoFontFamily) },
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(0.8f)
                    ) {
                        Text("إلغاء", fontFamily = CairoFontFamily)
                    }

                    Button(
                        onClick = {
                            if (!isSaving && name.isNotBlank()) {
                                isSaving = true
                                val entered = purchaseCostText.toDoubleOrNull() ?: 0.0
                                val costInYer = CurrencyHelper.convertToYer(entered, selectedCurrency, sarToYerRate, usdToYerRate)
                                val currentVal = currentValueText.toDoubleOrNull() ?: costInYer
                                val originalCost = if (selectedCurrency != CurrencyHelper.CURRENCY_YER) entered else 0.0

                                val asset = assetToEdit?.copy(
                                    assetName = name,
                                    category = category,
                                    purchaseCost = costInYer,
                                    estimatedCurrentValue = currentVal,
                                    currency = selectedCurrency,
                                    originalCost = originalCost,
                                    location = location,
                                    serialNumber = serialNumber,
                                    notes = notes
                                ) ?: NetworkAssetEntity(
                                    assetName = name,
                                    category = category,
                                    purchaseCost = costInYer,
                                    estimatedCurrentValue = currentVal,
                                    currency = selectedCurrency,
                                    originalCost = originalCost,
                                    location = location,
                                    serialNumber = serialNumber,
                                    notes = notes
                                )
                                onSave(asset)
                            }
                        },
                        enabled = !isSaving && name.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = AssetPurple),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("btn_save_asset")
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جاري الحفظ...", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = CairoFontFamily)
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (assetToEdit != null && assetToEdit.id != 0L) "تحديث بيانات الأصل" else "حفظ الأصل الرأسمالي ✓",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontFamily = CairoFontFamily
                            )
                        }
                    }
                }
            }
        }
    }
}
