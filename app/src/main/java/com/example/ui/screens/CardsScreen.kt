package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import com.example.data.local.entity.CardSalesInvoiceEntity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CardEntity
import com.example.data.local.entity.CardPackageEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.CairoFontFamily
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberDarkCardElevated
import com.example.ui.theme.CyberDarkSurface
import com.example.ui.theme.MikroTikCyan
import com.example.ui.theme.MikroTikNavy
import com.example.ui.theme.MikroTikPrimary
import com.example.ui.theme.ProfitEmerald
import com.example.ui.theme.StatusDistributed
import com.example.ui.theme.StatusOnline
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

@Composable
fun CardsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val batches by viewModel.cardBatches.collectAsState()
    val cards by viewModel.cards.collectAsState()
    val cardPackages by viewModel.cardPackages.collectAsState()
    val retailers by viewModel.retailers.collectAsState()
    val availableCount by viewModel.availableCardsCount.collectAsState()
    val distributedCount by viewModel.distributedCardsCount.collectAsState()
    val soldCount by viewModel.soldCardsCount.collectAsState()
    val inventoryItems by viewModel.inventoryItems.collectAsState()

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var selectedStatusFilter by remember { mutableStateOf("الكل") }
    // 2: مخزن الأصناف بالعدد, 3: فواتير مبيعات الكروت, 1: باقات الكروت, 0: استوديو A4, 5: سجل أرقام الكروت
    var screenViewMode by remember { mutableIntStateOf(2) } // المخزن بالعدد افتراضياً بناء على طلب المستخدم
    var showGenerateDialog by remember { mutableStateOf(false) }
    var selectedPackageForBatch by remember { mutableStateOf<CardPackageEntity?>(null) }
    var showImportExternalDialog by remember { mutableStateOf(false) }
    var selectedPackageForImport by remember { mutableStateOf<CardPackageEntity?>(null) }
    var showManualCountDialog by remember { mutableStateOf(false) }
    var selectedPackageForManualCount by remember { mutableStateOf<CardPackageEntity?>(null) }
    var showBatchChoiceDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showStudioDialog by remember { mutableStateOf(false) }
    var selectedCardForStudio by remember { mutableStateOf<CardEntity?>(null) }

    var showCreateInvoiceDialog by remember { mutableStateOf(false) }
    var preselectedPackageForInvoice by remember { mutableStateOf<String?>(null) }
    var invoiceToClone by remember { mutableStateOf<CardSalesInvoiceEntity?>(null) }
    var invoiceToEdit by remember { mutableStateOf<CardSalesInvoiceEntity?>(null) }

    val statusFilters = listOf("الكل", "المتاحة بالمستودع", "الموزعة للمحلات", "المباعة")

    val filteredCards = cards.filter { card ->
        when (selectedStatusFilter) {
            "المتاحة بالمستودع" -> card.status == "AVAILABLE"
            "الموزعة للمحلات" -> card.status == "DISTRIBUTED"
            "المباعة" -> card.status == "SOLD"
            else -> true
        }
    }

    Box(modifier = modifier.fillMaxSize().testTag("cards_screen")) {
        when (screenViewMode) {
            0 -> {
                // 0: نمط استوديو وتصميم الكروت وطباعتها A4
                Column(modifier = Modifier.fillMaxSize()) {
                    CardsTopNavBar(
                        activeMode = screenViewMode,
                        onSelectMode = { screenViewMode = it },
                        packagesCount = cardPackages.size,
                        cardsCount = cards.size
                    )

                    // شاشة الاستوديو الكاملة
                    CardStudioScreen(
                        viewModel = viewModel,
                        onNavigateToInventory = { screenViewMode = 2 }
                    )
                }
            }
            1 -> {
                // 1: تبويب باقات وفئات الكروت (أسعار الجملة، البيع النهائي، الأرباح، والربط بالتوليد والفواتير)
                Column(modifier = Modifier.fillMaxSize()) {
                    CardsTopNavBar(
                        activeMode = screenViewMode,
                        onSelectMode = { screenViewMode = it },
                        packagesCount = cardPackages.size,
                        cardsCount = cards.size,
                        onImportExternal = {
                            selectedPackageForImport = cardPackages.firstOrNull()
                            showImportExternalDialog = true
                        },
                        onAddManualQuantity = {
                            selectedPackageForManualCount = cardPackages.firstOrNull()
                            showManualCountDialog = true
                        }
                    )

                    PackagesManagementTab(
                        viewModel = viewModel,
                        onQuickGenerateBatch = { pkg ->
                            selectedPackageForBatch = pkg
                            showGenerateDialog = true
                        },
                        onImportExternalBatch = { pkg ->
                            selectedPackageForImport = pkg
                            showImportExternalDialog = true
                        },
                        onAddManualQuantity = { pkg ->
                            selectedPackageForManualCount = pkg
                            showManualCountDialog = true
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            2 -> {
                // 2: تبويب مخزن الأصناف بالعدد (وينقص تلقائياً في كل عملية بيع ويزداد في حال الإضافة)
                Column(modifier = Modifier.fillMaxSize()) {
                    CardsTopNavBar(
                        activeMode = screenViewMode,
                        onSelectMode = { screenViewMode = it },
                        packagesCount = cardPackages.size,
                        cardsCount = inventoryItems.sumOf { it.quantityAvailable },
                        onAddManualQuantity = {
                            selectedPackageForManualCount = cardPackages.firstOrNull()
                            showManualCountDialog = true
                        }
                    )

                    CardStockCategoryTab(
                        viewModel = viewModel,
                        onIssueInvoiceForPackage = { pkgName ->
                            preselectedPackageForInvoice = pkgName
                            showCreateInvoiceDialog = true
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            3 -> {
                // 3: تبويب فواتير مبيعات الكروت المحاسبية الاحترافية
                Column(modifier = Modifier.fillMaxSize()) {
                    CardsTopNavBar(
                        activeMode = screenViewMode,
                        onSelectMode = { screenViewMode = it },
                        packagesCount = cardPackages.size,
                        cardsCount = inventoryItems.sumOf { it.quantityAvailable },
                        onAddManualQuantity = {
                            selectedPackageForManualCount = cardPackages.firstOrNull()
                            showManualCountDialog = true
                        }
                    )

                    CardSalesInvoicesSubScreen(
                        viewModel = viewModel,
                        onOpenCreateInvoice = {
                            preselectedPackageForInvoice = null
                            invoiceToClone = null
                            invoiceToEdit = null
                            showCreateInvoiceDialog = true
                        },
                        onEditInvoice = { inv ->
                            invoiceToEdit = inv
                            invoiceToClone = null
                            preselectedPackageForInvoice = null
                            showCreateInvoiceDialog = true
                        },
                        onCloneInvoice = { inv ->
                            invoiceToClone = inv
                            invoiceToEdit = null
                            preselectedPackageForInvoice = null
                            showCreateInvoiceDialog = true
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            5 -> {
                // 5: نمط جدول وسجل كروت المخزن بالتسلسلات
                Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                    CardsTopNavBar(
                        activeMode = screenViewMode,
                        onSelectMode = { screenViewMode = it },
                        packagesCount = cardPackages.size,
                        cardsCount = cards.size,
                        onExport = { showExportDialog = true },
                        onImportExternal = {
                            selectedPackageForImport = cardPackages.firstOrNull()
                            showImportExternalDialog = true
                        },
                        onAddManualQuantity = {
                            selectedPackageForManualCount = cardPackages.firstOrNull()
                            showManualCountDialog = true
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // شريط العمليات السريعة لتغذية المخزن
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0F1E33),
                        border = BorderStroke(1.dp, Color(0xFF1E3A5F)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("إدارة ودفعات الكروت:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                // زر إضافة بالعدد مباشرة
                                Button(
                                    onClick = {
                                        selectedPackageForManualCount = cardPackages.firstOrNull()
                                        showManualCountDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ProfitEmerald),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Default.Numbers, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("إضافة بالعدد", color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        selectedPackageForImport = cardPackages.firstOrNull()
                                        showImportExternalDialog = true
                                    },
                                    border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Default.PostAdd, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("استيراد خارجي", color = Color(0xFF38BDF8), fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        selectedPackageForBatch = cardPackages.firstOrNull()
                                        showGenerateDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("توليد كروت", color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                // Inventory Metric Cards Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricPill(title = "جاهزة بالمستودع", count = availableCount, color = StatusOnline, modifier = Modifier.weight(1f))
                    MetricPill(title = "بحوزة المحلات", count = distributedCount, color = StatusDistributed, modifier = Modifier.weight(1f))
                    MetricPill(title = "مباعة ومفعلة", count = soldCount, color = Color(0xFF8B5CF6), modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Filter Tabs
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(statusFilters) { filter ->
                        val isSelected = selectedStatusFilter == filter
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isSelected) MikroTikPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .clickable { selectedStatusFilter = filter }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = filter,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Cards List
                if (filteredCards.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "لا توجد كروت في هذه الحالة حالياً", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredCards, key = { it.id }) { card ->
                            CardItemRow(
                                card = card,
                                onCopy = {
                                    clipboard.setText(AnnotatedString("User: ${card.username} | PIN: ${card.password}"))
                                    Toast.makeText(context, "تم نسخ بيانات الكرت: ${card.username}", Toast.LENGTH_SHORT).show()
                                },
                                onPreview = {
                                    selectedCardForStudio = card
                                    showStudioDialog = true
                                },
                                onMarkSold = {
                                    viewModel.markCardSold(card.id)
                                    Toast.makeText(context, "تم تحديد الكرت كمباع ومفعل ✓", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(72.dp))
                        }
                    }
                }
            }

            // FAB to Add / Generate Batches
            FloatingActionButton(
                onClick = { showBatchChoiceDialog = true },
                containerColor = MikroTikPrimary,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .testTag("generate_batch_fab")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة كروت")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إضافة كروت جديدة", fontWeight = FontWeight.Bold)
                }
            }
        }
        4 -> {
            InventoryScreen(
                viewModel = viewModel,
                onNavigateToStudio = { screenViewMode = 0 },
                onNavigateToBatches = { screenViewMode = 5 }
            )
        }
    }

        // نافذة الاختيار المرنة: توليد عبر التطبيق أو استيراد دفعة من برنامج آخر
        if (showBatchChoiceDialog) {
            AlertDialog(
                onDismissRequest = { showBatchChoiceDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = MikroTikCyan, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إضافة كروت جديدة للمخزن", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "اختر الطريقة المناسبة لإضافة دفعة الكروت وتغذية حسابات الشبكة والموزعين:",
                            fontSize = 11.5.sp,
                            color = Color.Gray
                        )

                        // الخيار 1 (الأساسي): إضافة كروت يدوياً بالعدد دون اشتراط أرقام الكروت
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF062820),
                            border = BorderStroke(1.2.dp, ProfitEmerald),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showBatchChoiceDialog = false
                                    selectedPackageForManualCount = cardPackages.firstOrNull()
                                    showManualCountDialog = true
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ProfitEmerald.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Numbers, contentDescription = null, tint = ProfitEmerald, modifier = Modifier.size(22.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("إضافة كروت يدوياً بالعدد (مباشرة)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                    Text("مثلاً: فئة 200 ريال بعدد 1000 كرت - دون اشتراط أرقام أو رموز", fontSize = 10.5.sp, color = Color(0xFF6EE7B7))
                                }
                            }
                        }

                        // الخيار 2: توليد كروت عبر التطبيق
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF0F1E33),
                            border = BorderStroke(1.dp, Color(0xFF1E3A5F)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showBatchChoiceDialog = false
                                    selectedPackageForBatch = cardPackages.firstOrNull()
                                    showGenerateDialog = true
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF0284C7).copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Bolt, contentDescription = null, tint = MikroTikCyan, modifier = Modifier.size(22.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("توليد كروت جديدة عبر التطبيق", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                    Text("توليد رموز عشوائية مشفرة وبروفايل ميكروتك وقوالب طباعة A4", fontSize = 10.5.sp, color = Color(0xFF94A3B8))
                                }
                            }
                        }

                        // الخيار 3: استيراد دفعة من برنامج آخر
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF111827),
                            border = BorderStroke(1.dp, Color(0xFF374151)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showBatchChoiceDialog = false
                                    selectedPackageForImport = cardPackages.firstOrNull()
                                    showImportExternalDialog = true
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF38BDF8).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.PostAdd, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(22.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("استيراد كروت من برنامج آخر", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                    Text("استيراد كروت من ميكروتك يوزر مانجر أو ملف إكسل CSV", fontSize = 10.5.sp, color = Color(0xFF9CA3AF))
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showBatchChoiceDialog = false }) {
                        Text("إلغاء")
                    }
                }
            )
        }

        // نافذة إضافة كروت يدوياً بالعدد فقط (مثلاً 1000 كرت فئة 200 ريال) دون اشتراط أرقام الكروت
        if (showManualCountDialog) {
            AddManualCardsCountDialog(
                packages = cardPackages,
                retailers = retailers,
                initialPackage = selectedPackageForManualCount,
                onDismiss = {
                    showManualCountDialog = false
                    selectedPackageForManualCount = null
                },
                onConfirm = { catName, qty, retail, wholesale, bName, retId ->
                    viewModel.addManualCardQuantity(
                        categoryName = catName,
                        quantity = qty,
                        retailPrice = retail,
                        wholesalePrice = wholesale,
                        batchName = bName,
                        targetRetailerId = retId
                    ) { batchId, count ->
                        showManualCountDialog = false
                        selectedPackageForManualCount = null
                        val destMsg = if (retId != null) "وصرفها للبقالة بنجاح ✓" else "إلى المستودع العام بنجاح ✓"
                        Toast.makeText(context, "تمت إضافة $count كرت فئة ($catName) $destMsg", Toast.LENGTH_LONG).show()
                    }
                }
            )
        }

        // نافذة استيراد كروت من برامج أخرى (MikroTik User Manager, Excel, SAS4, إلخ)
        if (showImportExternalDialog) {
            ImportExternalCardsDialog(
                packages = cardPackages,
                retailers = retailers,
                initialPackage = selectedPackageForImport,
                onDismiss = {
                    showImportExternalDialog = false
                    selectedPackageForImport = null
                },
                onImportConfirmed = { bName, cat, retail, wholesale, mb, hours, speed, cardsList, source, prefix, retailerId ->
                    viewModel.importExternalCardBatch(
                        batchName = bName,
                        categoryName = cat,
                        retailPrice = retail,
                        wholesalePrice = wholesale,
                        quotaMb = mb,
                        validityHours = hours,
                        speedLimit = speed,
                        cardsList = cardsList,
                        sourceProgram = source,
                        prefix = prefix,
                        targetRetailerId = retailerId
                    ) { batchId, count ->
                        showImportExternalDialog = false
                        selectedPackageForImport = null
                        val destMsg = if (retailerId != null) "وصرفها للبقالة بنجاح ✓" else "إلى المستودع بنجاح ✓"
                        Toast.makeText(context, "تم استيراد $count كرت من ($source) $destMsg", Toast.LENGTH_LONG).show()
                    }
                }
            )
        }

        // Card Studio Dialog
        if (showStudioDialog) {
            CardStudioDialog(
                sampleCards = cards,
                initialCard = selectedCardForStudio,
                onDismiss = {
                    showStudioDialog = false
                    selectedCardForStudio = null
                }
            )
        }

        // Generate Cards Dialog (مرتبطة بالباقات تلقائياً)
        if (showGenerateDialog) {
            GenerateBatchDialog(
                packages = cardPackages,
                initialPackage = selectedPackageForBatch,
                onDismiss = {
                    showGenerateDialog = false
                    selectedPackageForBatch = null
                },
                onGenerate = { name, cat, retail, wholesale, mb, hours, speed, count, prefix, len, charSet, pwdPolicy ->
                    viewModel.createBatchAndGenerateCards(
                        name, cat, retail, wholesale, mb, hours, speed, count, prefix, len, charSet, pwdPolicy
                    ) {
                        showGenerateDialog = false
                        selectedPackageForBatch = null
                        Toast.makeText(context, "تم توليد $count كرت جديد بنجاح ✓", Toast.LENGTH_LONG).show()
                    }
                }
            )
        }

        // Export Cards Dialog
        if (showExportDialog) {
            val exportText = cards.filter { it.status == "AVAILABLE" }.take(50).joinToString("\n") {
                "${it.username},${it.password},${it.categoryName},${it.retailPrice.toInt()} YER"
            }
            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                title = { Text("تصدير الكروت للطباعة أو ميكروتك") },
                text = {
                    Column {
                        Text(
                            text = "صيغة CSV جاهزة للطباعة أو الاستيراد في سيرفر الميكروتك (أول 50 كرت متاح):",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F172A))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = exportText.ifEmpty { "لا توجد كروت متاحة بالمستودع حالياً للتصدير." },
                                color = Color(0xFF38BDF8),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            clipboard.setText(AnnotatedString(exportText))
                            Toast.makeText(context, "تم نسخ قائمة الكروت إلى الحافظة", Toast.LENGTH_SHORT).show()
                            showExportDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MikroTikPrimary)
                    ) {
                        Text("نسخ الكل")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExportDialog = false }) {
                        Text("إغلاق")
                    }
                }
            )
        }

        // نافذة إصدار أو تعديل أو استنساخ فاتورة مبيعات كروت احترافية
        if (showCreateInvoiceDialog) {
            CreateCardSalesInvoiceDialog(
                inventoryItems = inventoryItems,
                retailers = retailers,
                preSelectedPackageName = preselectedPackageForInvoice,
                initialInvoiceToClone = invoiceToClone,
                invoiceToEdit = invoiceToEdit,
                onDismiss = {
                    showCreateInvoiceDialog = false
                    preselectedPackageForInvoice = null
                    invoiceToClone = null
                    invoiceToEdit = null
                },
                onConfirmEdit = { originalInvoice, customerName, customerPhone, retailerId, items, paymentType, paidAmount, notes ->
                    viewModel.updateSalesInvoice(
                        originalInvoice = originalInvoice,
                        customerName = customerName,
                        customerPhone = customerPhone,
                        retailerId = retailerId,
                        items = items,
                        paymentType = paymentType,
                        paidAmount = paidAmount,
                        notes = notes
                    ) {
                        showCreateInvoiceDialog = false
                        preselectedPackageForInvoice = null
                        invoiceToClone = null
                        invoiceToEdit = null
                        Toast.makeText(context, "تم حفظ وتحديث الفاتورة وضبط الحسابات والمخزن بنجاح ✓", Toast.LENGTH_LONG).show()
                    }
                },
                onConfirmInvoice = { customerName, customerPhone, retailerId, items, paymentType, paidAmount, notes, oldInvoiceToDelete ->
                    if (oldInvoiceToDelete != null) {
                        viewModel.replaceSalesInvoice(
                            oldInvoice = oldInvoiceToDelete,
                            customerName = customerName,
                            customerPhone = customerPhone,
                            retailerId = retailerId,
                            items = items,
                            paymentType = paymentType,
                            paidAmount = paidAmount,
                            notes = notes
                        ) { invoiceId ->
                            showCreateInvoiceDialog = false
                            preselectedPackageForInvoice = null
                            invoiceToClone = null
                            invoiceToEdit = null
                            Toast.makeText(context, "تم استنساخ الفاتورة وحذف السابقة بنجاح ✓", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        viewModel.issueMultiItemSalesInvoice(
                            customerName = customerName,
                            customerPhone = customerPhone,
                            retailerId = retailerId,
                            items = items,
                            paymentType = paymentType,
                            paidAmount = paidAmount,
                            notes = notes
                        ) { invoiceId ->
                            showCreateInvoiceDialog = false
                            preselectedPackageForInvoice = null
                            invoiceToClone = null
                            invoiceToEdit = null
                            val successMsg = if (invoiceToClone != null) {
                                "تم استنساخ الفاتورة وإصدارها برقم جديد بنجاح ✓"
                            } else {
                                "تم إصدار فاتورة المبيعات وخصم الكميات من المخزن بنجاح ✓"
                            }
                            Toast.makeText(context, successMsg, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun MetricPill(
    title: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "$count", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = title, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun CardItemRow(
    card: CardEntity,
    onCopy: () -> Unit,
    onPreview: () -> Unit,
    onMarkSold: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().testTag("card_row_${card.id}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Card details
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f).clickable { onPreview() }
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MikroTikPrimary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = MikroTikPrimary, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = card.username,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "PIN: ${card.password}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Text(
                        text = "${card.categoryName} • سعر البيع: ${card.retailPrice.toInt()} ريال",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (card.retailerName != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Store, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "لدى: ${card.retailerName}",
                                fontSize = 10.sp,
                                color = MikroTikPrimary
                            )
                        }
                    }
                }
            }

            // Actions & Status
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPreview, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Visibility, contentDescription = "معاينة وتصميم الكرت", tint = MikroTikPrimary, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onCopy, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
                if (card.status == "DISTRIBUTED") {
                    Button(
                        onClick = onMarkSold,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusOnline),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("تم البيع", fontSize = 10.sp)
                    }
                } else {
                    val statusText = when (card.status) {
                        "AVAILABLE" -> "متاح"
                        "SOLD" -> "مباع"
                        else -> card.status
                    }
                    val statusColor = when (card.status) {
                        "AVAILABLE" -> StatusOnline
                        "SOLD" -> Color.Gray
                        else -> StatusDistributed
                    }
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CardsTopNavBar(
    activeMode: Int,
    onSelectMode: (Int) -> Unit,
    packagesCount: Int,
    cardsCount: Int,
    onExport: (() -> Unit)? = null,
    onImportExternal: (() -> Unit)? = null,
    onAddManualQuantity: (() -> Unit)? = null
) {
    Surface(
        color = Color(0xFF070E1A),
        border = BorderStroke(1.dp, Color(0xFF1E2F52)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Scrollable futuristic pill navigation
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavPill(
                    selected = activeMode == 2,
                    icon = Icons.Default.Inventory2,
                    label = "المخزن بالعدد",
                    badge = "$cardsCount",
                    onClick = { onSelectMode(2) }
                )
                NavPill(
                    selected = activeMode == 3,
                    icon = Icons.Default.ReceiptLong,
                    label = "فواتير المبيعات",
                    badge = null,
                    onClick = { onSelectMode(3) }
                )
                NavPill(
                    selected = activeMode == 1,
                    icon = Icons.Default.LocalOffer,
                    label = "باقات الكروت",
                    badge = "$packagesCount",
                    onClick = { onSelectMode(1) }
                )
                NavPill(
                    selected = activeMode == 0,
                    icon = Icons.Default.Palette,
                    label = "استوديو A4",
                    badge = null,
                    onClick = { onSelectMode(0) }
                )
                NavPill(
                    selected = activeMode == 5,
                    icon = Icons.Default.ConfirmationNumber,
                    label = "سجل الأرقام",
                    badge = null,
                    onClick = { onSelectMode(5) }
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Action Quick Tools
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onAddManualQuantity != null) {
                    IconButton(
                        onClick = onAddManualQuantity,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ProfitEmerald.copy(alpha = 0.15f))
                            .border(1.dp, ProfitEmerald.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            Icons.Default.Numbers,
                            contentDescription = "إضافة كروت بالعدد",
                            tint = ProfitEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                if (onImportExternal != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onImportExternal,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0284C7).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            Icons.Default.PostAdd,
                            contentDescription = "استيراد كروت خارجية",
                            tint = MikroTikCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                if (onExport != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onExport,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E2F52))
                            .border(1.dp, Color(0xFF2A4374), RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "تصدير",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavPill(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    badge: String? = null,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) Color(0xFF0A223E) else Color(0xFF0B1422),
        border = BorderStroke(
            1.2.dp,
            if (selected) Color(0xFF00E5FF).copy(alpha = 0.7f) else Color(0xFF1B2A44)
        ),
        onClick = onClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) Color(0xFF00E5FF) else TextSecondaryDark,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = if (selected) Color.White else TextSecondaryDark,
                fontFamily = CairoFontFamily,
                fontSize = 11.5.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
            if (badge != null) {
                Spacer(modifier = Modifier.width(5.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (selected) Color(0xFF00E5FF).copy(alpha = 0.25f) else Color(0xFF1E2F52).copy(alpha = 0.6f)
                ) {
                    Text(
                        text = badge,
                        color = if (selected) Color(0xFF00E5FF) else TextSecondaryDark,
                        fontSize = 10.sp,
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun GenerateBatchDialog(
    packages: List<CardPackageEntity> = emptyList(),
    initialPackage: CardPackageEntity? = null,
    onDismiss: () -> Unit,
    onGenerate: (
        name: String,
        cat: String,
        retail: Double,
        wholesale: Double,
        mb: Long,
        hours: Int,
        speed: String,
        count: Int,
        prefix: String,
        codeLength: Int,
        charSet: com.example.data.cards.CodeCharacterSet,
        passwordPolicy: com.example.data.cards.PasswordPolicy
    ) -> Unit
) {
    var selectedPkg by remember { mutableStateOf(initialPackage) }
    var batchName by remember {
        mutableStateOf(initialPackage?.let { "دفعة ${it.name}" } ?: "دفعة كروت فئة 500 ريال")
    }
    var categoryName by remember {
        mutableStateOf(initialPackage?.let { "${it.name} - ${it.formattedQuota}" } ?: "500 ريال - 4.5GB")
    }
    var retailPrice by remember {
        mutableStateOf(initialPackage?.retailPrice?.toInt()?.toString() ?: "500")
    }
    var wholesalePrice by remember {
        mutableStateOf(initialPackage?.wholesalePrice?.toInt()?.toString() ?: "450")
    }
    var quotaMb by remember {
        mutableStateOf(initialPackage?.quotaMb ?: 4608L)
    }
    var validityHours by remember {
        mutableStateOf(initialPackage?.validityHours ?: 72)
    }
    var speedLimit by remember {
        mutableStateOf(initialPackage?.speedLimit ?: "6M/3M")
    }
    var countText by remember { mutableStateOf("50") }
    var prefix by remember {
        mutableStateOf(initialPackage?.let { "SAM${it.retailPrice.toInt()}" } ?: "SAM500")
    }
    var codeLength by remember { mutableStateOf("6") }
    var selectedCharSet by remember { mutableStateOf(com.example.data.cards.CodeCharacterSet.DIGITS_ONLY) }
    var selectedPasswordPolicy by remember { mutableStateOf(com.example.data.cards.PasswordPolicy.SAME_AS_USERNAME) }

    val retail = retailPrice.toDoubleOrNull() ?: 0.0
    val wholesale = wholesalePrice.toDoubleOrNull() ?: 0.0
    val profit = (retail - wholesale).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("توليد دفعة كروت هوتسبوت جديدة", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Quick Packages Selection Row
                if (packages.isNotEmpty()) {
                    item {
                        Column {
                            Text("تعبئة تلقائية من باقات الشبكة:", fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(packages) { pkg ->
                                    val isSelected = selectedPkg?.id == pkg.id
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) Color(0xFF0284C7) else Color(0xFF0F1E33),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color.White else Color(0xFF1E3A5F)),
                                        onClick = {
                                            selectedPkg = pkg
                                            batchName = "دفعة ${pkg.name}"
                                            categoryName = "${pkg.name} - ${pkg.formattedQuota}"
                                            retailPrice = pkg.retailPrice.toInt().toString()
                                            wholesalePrice = pkg.wholesalePrice.toInt().toString()
                                            quotaMb = pkg.quotaMb
                                            validityHours = pkg.validityHours
                                            speedLimit = pkg.speedLimit
                                            prefix = "SAM${pkg.retailPrice.toInt()}"
                                        }
                                    ) {
                                        Text(
                                            text = "${pkg.name} (${pkg.retailPrice.toInt()} ر.ي)",
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = batchName,
                        onValueChange = { batchName = it },
                        label = { Text("اسم الدفعة") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = categoryName,
                        onValueChange = { categoryName = it },
                        label = { Text("الفئة / السعة") },
                        placeholder = { Text("مثال: 500 ريال - 4.5GB") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = retailPrice,
                            onValueChange = { retailPrice = it },
                            label = { Text("سعر المستهلك (ر.ي)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = wholesalePrice,
                            onValueChange = { wholesalePrice = it },
                            label = { Text("سعر الجملة (ر.ي)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF0F1E33),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "هامش ربح البقالة للكرت الواحد: ${profit.toInt()} ر.ي",
                            fontSize = 11.sp,
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = countText,
                            onValueChange = { countText = it },
                            label = { Text("الكمية (عدد الكروت)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = prefix,
                            onValueChange = { prefix = it },
                            label = { Text("بادئة الكرت") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = codeLength,
                        onValueChange = { codeLength = it },
                        label = { Text("طول كود المستخدم (أرقام/حروف)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Text("نمط الرموز ومجموعة الأحرف:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        com.example.data.cards.CodeCharacterSet.values().forEach { set ->
                            Card(
                                shape = RoundedCornerShape(6.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedCharSet == set) MikroTikPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedCharSet = set }
                                    .padding(vertical = 2.dp)
                            ) {
                                Text(
                                    text = set.titleArabic,
                                    fontSize = 10.5.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    fontWeight = if (selectedCharSet == set) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val count = countText.toIntOrNull() ?: 50
                    val finalRetail = retailPrice.toDoubleOrNull() ?: 500.0
                    val finalWholesale = wholesalePrice.toDoubleOrNull() ?: 450.0
                    val len = codeLength.toIntOrNull() ?: 6
                    onGenerate(
                        batchName, categoryName, finalRetail, finalWholesale,
                        quotaMb, validityHours, speedLimit, count, prefix, len,
                        selectedCharSet, selectedPasswordPolicy
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = MikroTikPrimary)
            ) {
                Text("توليد الكروت الآن", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

