package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CardPackageEntity
import com.example.data.local.entity.RetailerEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.CairoFontFamily
import com.example.ui.theme.InvestmentGold
import com.example.ui.theme.MikroTikCyan
import com.example.ui.theme.MikroTikNavy
import com.example.ui.theme.MikroTikPrimary
import com.example.ui.theme.ProfitEmerald
import com.example.ui.theme.StatusOnline
import com.example.ui.theme.WhatsAppDarkGreen
import com.example.ui.theme.WhatsAppGreen
import com.example.util.WhatsAppHelper
import java.util.Locale

/**
 * تبويب إدارة وباقات فئات كروت الشبكة
 * يشمل إضافة وتعديل الباقات بأسعار الجملة وسعر البيع النهائي،
 * وتصميم بطاقة فخمة جذابة مع ربط الباقة بالتوليد المباشر وفواتير بيع الكروت.
 */
@Composable
fun PackagesManagementTab(
    viewModel: MainViewModel,
    onQuickGenerateBatch: (CardPackageEntity) -> Unit,
    onImportExternalBatch: (CardPackageEntity) -> Unit = {},
    onAddManualQuantity: (CardPackageEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val packages by viewModel.cardPackages.collectAsState()
    val retailers by viewModel.retailers.collectAsState()

    var showAddEditDialog by remember { mutableStateOf(false) }
    var packageToEdit by remember { mutableStateOf<CardPackageEntity?>(null) }
    var packageForSalesInvoice by remember { mutableStateOf<CardPackageEntity?>(null) }
    var packageToDelete by remember { mutableStateOf<CardPackageEntity?>(null) }

    val totalPackagesCount = packages.size
    val avgProfitMargin = if (packages.isNotEmpty()) {
        packages.map { it.profitPerCard.toDouble() }.average()
    } else 0.0

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp)
            .testTag("packages_management_tab"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header Banner with Stats & Add Button
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E33)),
                border = BorderStroke(1.dp, Color(0xFF1E3A5F)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocalOffer,
                                    contentDescription = null,
                                    tint = MikroTikCyan,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "باقات وفئات كروت الشبكة",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "تسعير الجملة والبيع النهائي مع ربط مباشر بتوليد الكروت وفواتير الموزعين",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            // إضافة كروت يدوياً بالعدد دون اشتراط أرقام الكروت
                            Button(
                                onClick = {
                                    val target = packages.firstOrNull() ?: CardPackageEntity(
                                        name = "كروت فئة 200 ريال",
                                        wholesalePrice = java.math.BigDecimal("180.0"),
                                        retailPrice = java.math.BigDecimal("200.0"),
                                        quotaMb = 1500,
                                        validityHours = 24
                                    )
                                    onAddManualQuantity(target)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ProfitEmerald),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Numbers, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إضافة كروت بالعدد", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    val target = packages.firstOrNull() ?: CardPackageEntity(
                                        name = "باقة جديدة",
                                        wholesalePrice = java.math.BigDecimal("450.0"),
                                        retailPrice = java.math.BigDecimal("500.0"),
                                        quotaMb = 4500,
                                        validityHours = 72
                                    )
                                    onImportExternalBatch(target)
                                },
                                border = BorderStroke(1.dp, MikroTikCyan),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.PostAdd, contentDescription = null, tint = MikroTikCyan, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("استيراد كروت", color = MikroTikCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    packageToEdit = null
                                    showAddEditDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("add_package_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إضافة باقة", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Stats row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0A192F),
                            border = BorderStroke(1.dp, Color(0xFF172A45))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF0284C7).copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Wifi, contentDescription = null, tint = MikroTikCyan, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("عدد الباقات", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                    Text("$totalPackagesCount باقة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0A192F),
                            border = BorderStroke(1.dp, Color(0xFF172A45))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(ProfitEmerald.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = ProfitEmerald, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("متوسط ربح المحل", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                    Text("${avgProfitMargin.toInt()} ر.ي/كرت", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ProfitEmerald)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Packages Grid / Cards List
        if (packages.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A192F)),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.LocalOffer, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("لا توجد باقات معرفة حالياً", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("قم بإضافة باقاتك لربط سعر الجملة وسعر البيع وتوليد الكروت وفواتير الموزعين بنقرة واحدة.", color = Color(0xFF94A3B8), fontSize = 12.sp, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                packageToEdit = null
                                showAddEditDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MikroTikPrimary)
                        ) {
                            Text("إضافة أول باقة للشبكة")
                        }
                    }
                }
            }
        } else {
            items(packages, key = { it.id }) { pkg ->
                PackageCardItem(
                    pkg = pkg,
                    onEdit = {
                        packageToEdit = pkg
                        showAddEditDialog = true
                    },
                    onDelete = {
                        packageToDelete = pkg
                    },
                    onQuickGenerate = {
                        onQuickGenerateBatch(pkg)
                    },
                    onSalesInvoice = {
                        packageForSalesInvoice = pkg
                    },
                    onImportExternal = {
                        onImportExternalBatch(pkg)
                    },
                    onAddManualQuantity = {
                        onAddManualQuantity(pkg)
                    }
                )
            }
        }
    }

    // Dialogs
    if (showAddEditDialog) {
        AddEditPackageDialog(
            initialPackage = packageToEdit,
            onDismiss = { showAddEditDialog = false },
            onSave = { savedPkg ->
                viewModel.savePackage(savedPkg) {
                    Toast.makeText(context, "تم حفظ الباقة [${savedPkg.name}] بنجاح", Toast.LENGTH_SHORT).show()
                }
                showAddEditDialog = false
            }
        )
    }

    packageForSalesInvoice?.let { pkg ->
        PackageSalesInvoiceDialog(
            pkg = pkg,
            retailers = retailers,
            onDismiss = { packageForSalesInvoice = null },
            onConfirm = { retailer, quantity, method, notes, shareWhatsApp ->
                viewModel.issueCardSalesInvoice(
                    pkg = pkg,
                    retailerId = retailer.id,
                    quantity = quantity,
                    paymentMethod = method,
                    notes = notes
                ) { voucherId ->
                    Toast.makeText(context, "تم إصدار فاتورة بيع $quantity كرت برقم #$voucherId وتحديث الحساب", Toast.LENGTH_LONG).show()

                    if (shareWhatsApp && retailer.phone.isNotBlank()) {
                        val invoiceMsg = WhatsAppHelper.generateCardInvoiceMessage(
                            invoiceNumber = "INV-$voucherId",
                            retailer = retailer,
                            pkg = pkg,
                            quantity = quantity,
                            paymentMethod = method,
                            notes = notes
                        )
                        WhatsAppHelper.sendWhatsAppMessage(context, retailer.phone, invoiceMsg)
                    }
                }
                packageForSalesInvoice = null
            }
        )
    }

    packageToDelete?.let { pkg ->
        AlertDialog(
            onDismissRequest = { packageToDelete = null },
            title = { Text("تأكيد حذف الباقة", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من رغبتك في حذف الباقة [${pkg.name}]؟ لن يتم حذف الكروت التي تم توليدها مسبقاً.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePackage(pkg) {
                            Toast.makeText(context, "تم حذف الباقة بنجاح", Toast.LENGTH_SHORT).show()
                        }
                        packageToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("حذف نهائي", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { packageToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

/**
 * تصميم بطاقة الباقة الاحترافي والجذاب
 * مع إبراز سعر البيع النهائي وسعر الجملة وهامش الربح وربط العمليات
 */
@Composable
fun PackageCardItem(
    pkg: CardPackageEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onQuickGenerate: () -> Unit,
    onSalesInvoice: () -> Unit,
    onImportExternal: () -> Unit = {},
    onAddManualQuantity: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Determine theme accent colors (2026 Cyber-Fintech AI Palette)
    val (accentGradient, badgeColor, borderGlow) = when (pkg.colorTheme.lowercase()) {
        "emerald", "green" -> Triple(
            Brush.horizontalGradient(listOf(Color(0xFF04211A), Color(0xFF07382B), Color(0xFF0A4F3E))),
            ProfitEmerald,
            Color(0xFF10B981)
        )
        "gold", "amber" -> Triple(
            Brush.horizontalGradient(listOf(Color(0xFF241506), Color(0xFF3D2309), Color(0xFF57320D))),
            InvestmentGold,
            Color(0xFFF59E0B)
        )
        "purple", "violet" -> Triple(
            Brush.horizontalGradient(listOf(Color(0xFF1B0B33), Color(0xFF2C1252), Color(0xFF431B7C))),
            Color(0xFFA855F7),
            Color(0xFF8B5CF6)
        )
        "rose", "red" -> Triple(
            Brush.horizontalGradient(listOf(Color(0xFF2E0914), Color(0xFF470E1F), Color(0xFF6B1530))),
            Color(0xFFFB7185),
            Color(0xFFF43F5E)
        )
        else -> Triple( // Default Electric Cyan
            Brush.horizontalGradient(listOf(Color(0xFF06182C), Color(0xFF0B2948), Color(0xFF103E6D))),
            MikroTikCyan,
            Color(0xFF00E5FF)
        )
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF091222)),
        border = BorderStroke(1.2.dp, borderGlow.copy(alpha = 0.45f)),
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .testTag("package_card_${pkg.id}")
    ) {
        Column {
            // Header Bar with Futuristic Cyber Gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(accentGradient)
                    .padding(horizontal = 14.dp, vertical = 11.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = pkg.mikrotikProfile,
                                color = badgeColor,
                                fontSize = 10.5.sp,
                                fontFamily = CairoFontFamily,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                maxLines = 1
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = pkg.name,
                            color = Color.White,
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Action icons (Edit & Delete)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.25f))
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "تعديل",
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.25f))
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "حذف",
                                tint = Color(0xFFF87171),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Body
            Column(modifier = Modifier.padding(14.dp)) {
                // 1. Dual-Price Highlight Box (سعر البيع النهائي وسعر الجملة وهامش الربح)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF050B15),
                    border = BorderStroke(1.dp, Color(0xFF1B2C4B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // سعر البيع النهائي للمستهلك
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = badgeColor.copy(alpha = 0.3f),
                                    modifier = Modifier.size(8.dp)
                                ) {}
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "سعر البيع النهائي",
                                    fontSize = 10.sp,
                                    fontFamily = CairoFontFamily,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "${pkg.retailPrice.toInt()}",
                                    fontSize = 21.sp,
                                    fontFamily = CairoFontFamily,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "ر.ي",
                                    fontSize = 11.5.sp,
                                    fontFamily = CairoFontFamily,
                                    color = badgeColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // فاصل عمودي
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(40.dp)
                                .background(Color(0xFF1B2C4B))
                        )

                        // سعر الجملة للبقالات
                        Column {
                            Text(
                                text = "سعر الجملة للبقالة",
                                fontSize = 10.sp,
                                fontFamily = CairoFontFamily,
                                color = Color(0xFF94A3B8)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "${pkg.wholesalePrice.toInt()}",
                                    fontSize = 18.sp,
                                    fontFamily = CairoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "ر.ي",
                                    fontSize = 10.5.sp,
                                    fontFamily = CairoFontFamily,
                                    color = Color(0xFF38BDF8)
                                )
                            }
                        }

                        // فاصل عمودي
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(40.dp)
                                .background(Color(0xFF1B2C4B))
                        )

                        // ربح المحل للكرت
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "ربح المحل / كرت",
                                fontSize = 10.sp,
                                fontFamily = CairoFontFamily,
                                color = ProfitEmerald
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Surface(
                                shape = RoundedCornerShape(7.dp),
                                color = ProfitEmerald.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, ProfitEmerald.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "+${pkg.profitPerCard.toInt()} ر.ي",
                                        fontSize = 12.sp,
                                        fontFamily = CairoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = ProfitEmerald
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "(${String.format(Locale.US, "%.0f", pkg.profitPercentage)}%)",
                                        fontSize = 10.sp,
                                        fontFamily = CairoFontFamily,
                                        color = ProfitEmerald
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(11.dp))

                // 2. Specifications Pills (السعة، الصلاحية، السرعة)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SpecPill(
                        icon = Icons.Default.Wifi,
                        label = "السعة",
                        value = pkg.formattedQuota,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.weight(1f)
                    )
                    SpecPill(
                        icon = Icons.Default.Timer,
                        label = "الصلاحية",
                        value = pkg.formattedValidity,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.weight(1f)
                    )
                    SpecPill(
                        icon = Icons.Default.Speed,
                        label = "السرعة",
                        value = pkg.speedLimit,
                        tint = Color(0xFFA78BFA),
                        modifier = Modifier.weight(1f)
                    )
                }

                if (pkg.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = pkg.notes,
                        fontSize = 11.sp,
                        fontFamily = CairoFontFamily,
                        color = Color(0xFF94A3B8),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFF1B2C4B), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // 3. Operational Integration Buttons (2x2 Cyber Action Grid - Spacious & Modern)
                // Row 1: Operations for Stock Count and Sales Invoice
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // إضافة كروت يدوياً بالعدد دون رموز
                    Surface(
                        onClick = onAddManualQuantity,
                        shape = RoundedCornerShape(10.dp),
                        color = ProfitEmerald.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, ProfitEmerald.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Numbers,
                                contentDescription = null,
                                tint = ProfitEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "إضافة بالعدد",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontFamily = CairoFontFamily,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }

                    // فاتورة بيع كروت للبقالة
                    Surface(
                        onClick = onSalesInvoice,
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFBBF24).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFFFBBF24).copy(alpha = 0.55f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "فاتورة مبيعات",
                                color = Color(0xFFFDE68A),
                                fontSize = 12.sp,
                                fontFamily = CairoFontFamily,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Row 2: Generator & Import External
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // توليد دفعة كروت فورية من هذه الباقة
                    Surface(
                        onClick = onQuickGenerate,
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0284C7).copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Bolt,
                                contentDescription = null,
                                tint = MikroTikCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "توليد كروت",
                                color = MikroTikCyan,
                                fontSize = 12.sp,
                                fontFamily = CairoFontFamily,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }

                    // استيراد كروت من برنامج آخر لهذه الباقة
                    Surface(
                        onClick = onImportExternal,
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFA855F7).copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.5f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.PostAdd,
                                contentDescription = null,
                                tint = Color(0xFFA855F7),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "استيراد خارجي",
                                color = Color(0xFFE9D5FF),
                                fontSize = 12.sp,
                                fontFamily = CairoFontFamily,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpecPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF0F1E33),
        border = BorderStroke(1.dp, Color(0xFF1E3A5F).copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(label, fontSize = 9.sp, color = Color(0xFF94A3B8))
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
        }
    }
}

/**
 * نافذة حوار إضافة وتعديل الباقة مع حسابات الأرباح الحية
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditPackageDialog(
    initialPackage: CardPackageEntity?,
    onDismiss: () -> Unit,
    onSave: (CardPackageEntity) -> Unit
) {
    var name by remember { mutableStateOf(initialPackage?.name ?: "باقة ") }
    var retailPriceText by remember { mutableStateOf(initialPackage?.retailPrice?.toInt()?.toString() ?: "500") }
    var wholesalePriceText by remember { mutableStateOf(initialPackage?.wholesalePrice?.toInt()?.toString() ?: "450") }
    var quotaMbText by remember { mutableStateOf(initialPackage?.quotaMb?.toString() ?: "2500") }
    var validityHoursText by remember { mutableStateOf(initialPackage?.validityHours?.toString() ?: "24") }
    var speedLimit by remember { mutableStateOf(initialPackage?.speedLimit ?: "4M/2M") }
    var mikrotikProfile by remember { mutableStateOf(initialPackage?.mikrotikProfile ?: "profile-500") }
    var selectedColor by remember { mutableStateOf(initialPackage?.colorTheme ?: "cyan") }
    var notes by remember { mutableStateOf(initialPackage?.notes ?: "") }

    val retail = retailPriceText.toDoubleOrNull() ?: 0.0
    val wholesale = wholesalePriceText.toDoubleOrNull() ?: 0.0
    val profit = (retail - wholesale).coerceAtLeast(0.0)
    val profitPercent = if (wholesale > 0) (profit / wholesale) * 100 else 0.0

    val availableColors = listOf(
        "cyan" to "سماوي كلاسيكي",
        "emerald" to "أخضر زمردي",
        "gold" to "ذهبي فخم",
        "purple" to "بنفسجي VIP",
        "rose" to "وردي أحمر",
        "amber" to "عنبر برتقالي"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialPackage == null) "إضافة باقة كروت جديدة" else "تعديل باقة [${initialPackage.name}]",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // اسم الباقة
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم الباقة *") },
                        placeholder = { Text("مثال: باقة 500 ريال فايبر") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("package_name_input")
                    )
                }

                // الأسعار: الجملة والتجزئة
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = retailPriceText,
                            onValueChange = { retailPriceText = it },
                            label = { Text("سعر البيع النهائي (ر.ي) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("package_retail_price_input")
                        )
                        OutlinedTextField(
                            value = wholesalePriceText,
                            onValueChange = { wholesalePriceText = it },
                            label = { Text("سعر الجملة للبقالة (ر.ي) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("package_wholesale_price_input")
                        )
                    }
                }

                // صندوق هامش الربح المحسوب تلقائياً
                item {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ProfitEmerald.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, ProfitEmerald.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("هامش ربح البقالة للكرت الواحد:", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                Text(
                                    text = "${profit.toInt()} ريال يمني",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ProfitEmerald
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = ProfitEmerald
                            ) {
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", profitPercent)}% نسبة الربح",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // السعة بالميجابايت مع أزرار سريعة
                item {
                    Column {
                        OutlinedTextField(
                            value = quotaMbText,
                            onValueChange = { quotaMbText = it },
                            label = { Text("السعة (بالميجابايت) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(
                                700L to "700MB",
                                1536L to "1.5GB",
                                2560L to "2.5GB",
                                4608L to "4.5GB",
                                10240L to "10GB",
                                22528L to "22GB",
                                51200L to "50GB"
                            ).forEach { (mb, label) ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (quotaMbText == mb.toString()) Color(0xFF0284C7) else MaterialTheme.colorScheme.surfaceVariant,
                                    onClick = { quotaMbText = mb.toString() }
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        color = if (quotaMbText == mb.toString()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // مدة الصلاحية بالساعات مع أزرار سريعة
                item {
                    Column {
                        OutlinedTextField(
                            value = validityHoursText,
                            onValueChange = { validityHoursText = it },
                            label = { Text("الصلاحية (بالساعات) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(
                                12 to "12 ساعة",
                                24 to "يوم كامل",
                                72 to "3 أيام",
                                168 to "أسبوع",
                                360 to "15 يوم",
                                720 to "شهر كامل"
                            ).forEach { (hrs, label) ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (validityHoursText == hrs.toString()) Color(0xFF0284C7) else MaterialTheme.colorScheme.surfaceVariant,
                                    onClick = { validityHoursText = hrs.toString() }
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        color = if (validityHoursText == hrs.toString()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // بروفايل ميكروتك والسرعة
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = speedLimit,
                            onValueChange = { speedLimit = it },
                            label = { Text("السرعة") },
                            placeholder = { Text("4M/2M") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = mikrotikProfile,
                            onValueChange = { mikrotikProfile = it },
                            label = { Text("بروفايل ميكروتك") },
                            placeholder = { Text("profile-500") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // ثيم ولون الكرت
                item {
                    Text("لون وهوية الكرت:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableColors.forEach { (key, label) ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedColor == key) Color(0xFF0284C7) else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (selectedColor == key) BorderStroke(1.dp, Color.White) else null,
                                onClick = { selectedColor = key }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    color = if (selectedColor == key) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // ملاحظات
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("ملاحظات إضافية (اختياري)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    val finalRetail = retailPriceText.toDoubleOrNull() ?: 500.0
                    val finalWholesale = wholesalePriceText.toDoubleOrNull() ?: 450.0
                    val quotaMb = quotaMbText.toLongOrNull() ?: 2500L
                    val validity = validityHoursText.toIntOrNull() ?: 24

                    val pkg = CardPackageEntity(
                        id = initialPackage?.id ?: 0L,
                        name = name.trim(),
                        retailPrice = java.math.BigDecimal.valueOf(finalRetail),
                        wholesalePrice = java.math.BigDecimal.valueOf(finalWholesale),
                        quotaMb = quotaMb,
                        validityHours = validity,
                        speedLimit = speedLimit.trim().ifEmpty { "4M/2M" },
                        mikrotikProfile = mikrotikProfile.trim().ifEmpty { "profile-${finalRetail.toInt()}" },
                        colorTheme = selectedColor,
                        notes = notes.trim()
                    )
                    onSave(pkg)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
            ) {
                Text("حفظ الباقة", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

/**
 * نافذة إصدار فاتورة وسند بيع كروت مباشرة للبقالة من الباقة المحددة
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackageSalesInvoiceDialog(
    pkg: CardPackageEntity,
    retailers: List<RetailerEntity>,
    onDismiss: () -> Unit,
    onConfirm: (retailer: RetailerEntity, quantity: Int, paymentMethod: String, notes: String, shareWhatsApp: Boolean) -> Unit
) {
    var selectedRetailer by remember { mutableStateOf(retailers.firstOrNull()) }
    var retailerDropdownExpanded by remember { mutableStateOf(false) }
    var quantityText by remember { mutableStateOf("50") }
    var selectedPaymentMethod by remember { mutableStateOf("نقداً") }
    var notesText by remember { mutableStateOf("") }
    var shareWhatsApp by remember { mutableStateOf(selectedRetailer?.phone?.isNotBlank() == true) }

    val quantity = quantityText.toIntOrNull() ?: 0
    val totalWholesale = pkg.wholesalePrice.multiply(java.math.BigDecimal.valueOf(quantity.toLong()))
    val totalRetail = pkg.retailPrice.multiply(java.math.BigDecimal.valueOf(quantity.toLong()))
    val totalRetailerProfit = (pkg.retailPrice.subtract(pkg.wholesalePrice)).multiply(java.math.BigDecimal.valueOf(quantity.toLong()))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = ProfitEmerald, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("فاتورة بيع كروت للبقالة", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // ملخص الباقة
                item {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0B1728),
                        border = BorderStroke(1.dp, Color(0xFF1E3A5F)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(pkg.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                Text("السعة: ${pkg.formattedQuota} | ${pkg.formattedValidity}", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${pkg.wholesalePrice.toInt()} ر.ي", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                                Text("سعر الجملة للكرت", fontSize = 10.sp, color = Color(0xFF94A3B8))
                            }
                        }
                    }
                }

                // اختيار البقالة المستلمة
                item {
                    if (retailers.isEmpty()) {
                        Text("لا يوجد بقالات مسجلة، يرجى إضافة بقالة أولاً من قسم الموزعين", color = Color(0xFFEF4444), fontSize = 12.sp)
                    } else {
                        ExposedDropdownMenuBox(
                            expanded = retailerDropdownExpanded,
                            onExpandedChange = { retailerDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedRetailer?.name ?: "اختر البقالة / نقطة البيع",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("البقالة / الموزع المستلم *") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = retailerDropdownExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = retailerDropdownExpanded,
                                onDismissRequest = { retailerDropdownExpanded = false }
                            ) {
                                retailers.forEach { retailer ->
                                    DropdownMenuItem(
                                        text = { Text("${retailer.name} (${retailer.location.ifBlank { "بدون عنوان" }})") },
                                        onClick = {
                                            selectedRetailer = retailer
                                            if (retailer.phone.isNotBlank()) {
                                                shareWhatsApp = true
                                            }
                                            retailerDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // خيار مشاركة الفاتورة عبر الواتساب مع تذييل شبكة طلقة نت
                item {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = WhatsAppGreen.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, WhatsAppGreen.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { shareWhatsApp = !shareWhatsApp }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = shareWhatsApp,
                                onCheckedChange = { shareWhatsApp = it },
                                colors = CheckboxDefaults.colors(checkedColor = WhatsAppDarkGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Chat, contentDescription = null, tint = WhatsAppDarkGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "مشاركة الفاتورة عبر الواتساب فوراً",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = WhatsAppDarkGreen
                                    )
                                }
                                Text(
                                    text = if (selectedRetailer?.phone?.isNotBlank() == true)
                                        "إرسال الفاتورة برقم ${selectedRetailer?.phone} بتذييل (${WhatsAppHelper.NETWORK_BRAND_NAME})"
                                    else
                                        "تنبيه: البقالة ليس لها رقم هاتف مسجل - سيتم فتح قائمة المشاركة العامة",
                                    fontSize = 10.sp,
                                    color = if (selectedRetailer?.phone?.isNotBlank() == true) Color(0xFF1F2937) else Color(0xFFDC2626)
                                )
                            }
                        }
                    }
                }

                // كمية الكروت المباعة
                item {
                    Column {
                        OutlinedTextField(
                            value = quantityText,
                            onValueChange = { quantityText = it },
                            label = { Text("عدد الكروت المباعة *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(20, 50, 100, 200).forEach { count ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (quantity == count) Color(0xFF0284C7) else MaterialTheme.colorScheme.surfaceVariant,
                                    onClick = { quantityText = count.toString() }
                                ) {
                                    Text(
                                        text = "$count كرت",
                                        fontSize = 11.sp,
                                        color = if (quantity == count) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // الصندوق المالي المحسوب للفاتورة
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0F1E33),
                        border = BorderStroke(1.dp, Color(0xFF1E3A5F)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("المبلغ المطلوب من البقالة (سعر الجملة):", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                Text("${totalWholesale.toInt()} ر.ي", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("إجمالي البيع للمستهلكين:", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                Text("${totalRetail.toInt()} ر.ي", fontSize = 12.sp, color = Color.White)
                            }
                            HorizontalDivider(color = Color(0xFF1E293B))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("صافي ربح البقالة المقدر:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ProfitEmerald)
                                Text("+${totalRetailerProfit.toInt()} ر.ي", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ProfitEmerald)
                            }
                        }
                    }
                }

                // طريقة الدفع
                item {
                    Text("طريقة السداد:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("نقداً", "آجل / على الحساب", "تحويل كاش / بنكي").forEach { method ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedPaymentMethod == method) ProfitEmerald else MaterialTheme.colorScheme.surfaceVariant,
                                onClick = { selectedPaymentMethod = method }
                            ) {
                                Text(
                                    text = method,
                                    fontSize = 11.sp,
                                    color = if (selectedPaymentMethod == method) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // ملاحظات
                item {
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text("ملاحظات الفاتورة (اختياري)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val r = selectedRetailer
                    if (r == null || quantity <= 0) return@Button
                    onConfirm(r, quantity, selectedPaymentMethod, notesText, shareWhatsApp)
                },
                enabled = selectedRetailer != null && quantity > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (shareWhatsApp) WhatsAppDarkGreen else ProfitEmerald
                )
            ) {
                if (shareWhatsApp) {
                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إصدار ومشاركة عبر الواتساب", fontWeight = FontWeight.Bold, color = Color.White)
                } else {
                    Text("إصدار الفاتورة والسند", fontWeight = FontWeight.Bold, color = Color.White)
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
