package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CardPackageEntity
import com.example.data.local.entity.RetailerEntity
import com.example.ui.theme.MikroTikCyan
import com.example.ui.theme.MikroTikNavy
import com.example.ui.theme.MikroTikPrimary
import com.example.ui.theme.ProfitEmerald

/**
 * نافذة احترافية ومرنة لاستيراد وإضافة دفعات كروت تم إنشاؤها في برامج وأنظمة أخرى:
 * - Mikrotik User Manager
 * - جداول الإكسل (Excel / CSV)
 * - أنظمة SAS4 أو Radius
 * - كروت ورقية ومطبوعة مسبقاً
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportExternalCardsDialog(
    packages: List<CardPackageEntity>,
    retailers: List<RetailerEntity>,
    initialPackage: CardPackageEntity? = null,
    onDismiss: () -> Unit,
    onImportConfirmed: (
        batchName: String,
        categoryName: String,
        retailPrice: Double,
        wholesalePrice: Double,
        quotaMb: Long,
        validityHours: Int,
        speedLimit: String,
        cardsList: List<Pair<String, String>>,
        sourceProgram: String,
        prefix: String,
        targetRetailerId: Long?
    ) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    // مصدر البرنامج
    val sources = listOf("ميكروتك يوزر مانجر", "إكسل / CSV", "نظام SAS4 / Radius", "كروت مطبوعة مسبقاً", "نظام مخصص")
    var selectedSource by remember { mutableStateOf(sources[0]) }

    // الباقة المختارة
    var selectedPackage by remember { mutableStateOf<CardPackageEntity?>(initialPackage ?: packages.firstOrNull()) }

    // الحقول الأساسية
    var batchName by remember {
        mutableStateOf(
            if (initialPackage != null) "دفعة خارجية - ${initialPackage.name}" else "دفعة كروت خارجية"
        )
    }
    var categoryName by remember { mutableStateOf(initialPackage?.name ?: "باقة 500 ريال") }
    var retailPriceText by remember { mutableStateOf(initialPackage?.retailPrice?.toInt()?.toString() ?: "500") }
    var wholesalePriceText by remember { mutableStateOf(initialPackage?.wholesalePrice?.toInt()?.toString() ?: "450") }
    var quotaMbText by remember { mutableStateOf(initialPackage?.quotaMb?.toString() ?: "4500") }
    var validityHoursText by remember { mutableStateOf(initialPackage?.validityHours?.toString() ?: "72") }
    var speedLimitText by remember { mutableStateOf(initialPackage?.speedLimit ?: "4M/2M") }

    // طريقة الإدخال: 0: لصق نصوص/CSV, 1: تسلسل ونطاق أرقام, 2: كمية سريعة بالعدد
    var inputTab by remember { mutableIntStateOf(0) }

    // بيانات التبويب 0: لصق الرموز
    var pastedText by remember { mutableStateOf("") }

    // بيانات التبويب 1: نطاق أرقام تسلسلي
    var rangePrefix by remember { mutableStateOf("UM-") }
    var startNumberText by remember { mutableStateOf("1001") }
    var endNumberText by remember { mutableStateOf("1100") }
    var rangePasswordOption by remember { mutableIntStateOf(0) } // 0: نفس الرمز، 1: رمز PIN عشوائي، 2: بدون كلمة مرور

    // بيانات التبويب 2: كمية سريعة بالعدد
    var bulkCountText by remember { mutableStateOf("50") }
    var bulkPrefix by remember { mutableStateOf("EXT-") }

    // الوجهة بعد الاستيراد
    var depositToWarehouse by remember { mutableStateOf(true) }
    var selectedRetailerForAssign by remember { mutableStateOf<RetailerEntity?>(null) }
    var isRetailerDropdownExpanded by remember { mutableStateOf(false) }

    // لاقط الملفات النصية و CSV
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val stream = context.contentResolver.openInputStream(uri)
                val content = stream?.bufferedReader()?.use { it.readText() }
                if (!content.isNullOrBlank()) {
                    pastedText = content
                    Toast.makeText(context, "تم تحميل محتوى الملف بنجاح ✓", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "الملف فارغ", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "تعذر قراءة الملف: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // تحليل الكروت المستخرجة بحسب التبويب الفعال
    val parsedCards by remember {
        derivedStateOf {
            when (inputTab) {
                0 -> parseCardsFromText(pastedText)
                1 -> generateRangeCards(rangePrefix, startNumberText, endNumberText, rangePasswordOption)
                2 -> generateBulkCountCards(bulkPrefix, bulkCountText, categoryName)
                else -> emptyList()
            }
        }
    }

    val profitMargin = remember(retailPriceText, wholesalePriceText) {
        val r = retailPriceText.toDoubleOrNull() ?: 0.0
        val w = wholesalePriceText.toDoubleOrNull() ?: 0.0
        (r - w).coerceAtLeast(0.0)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0284C7).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PostAdd,
                        contentDescription = null,
                        tint = MikroTikCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("استيراد كروت من برامج أخرى", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("إكسل • يوزر مانجر • SAS4 • كروت مطبوعة", fontSize = 11.sp, color = Color.Gray)
                }
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // 1. اختيار مصدر البرنامج المنشئ
                item {
                    Text("البرنامج أو المصدر المنشئ للكروت:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(sources) { src ->
                            FilterChip(
                                selected = selectedSource == src,
                                onClick = {
                                    selectedSource = src
                                    batchName = "دفعة $src - $categoryName"
                                },
                                label = { Text(src, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF0284C7),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                // 2. الربط السريع مع باقات الشبكة
                if (packages.isNotEmpty()) {
                    item {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("الربط مع باقة معتمدة في الشبكة:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("تعبئة تلقائية للأسعار", fontSize = 10.sp, color = MikroTikCyan)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(packages) { pkg ->
                                    val isSelected = selectedPackage?.id == pkg.id
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) Color(0xFF0284C7) else MaterialTheme.colorScheme.surfaceVariant,
                                        border = if (isSelected) BorderStroke(1.dp, MikroTikCyan) else null,
                                        onClick = {
                                            selectedPackage = pkg
                                            categoryName = pkg.name
                                            retailPriceText = pkg.retailPrice.toInt().toString()
                                            wholesalePriceText = pkg.wholesalePrice.toInt().toString()
                                            quotaMbText = pkg.quotaMb.toString()
                                            validityHoursText = pkg.validityHours.toString()
                                            speedLimitText = pkg.speedLimit
                                            batchName = "دفعة $selectedSource - ${pkg.name}"
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LocalOffer,
                                                contentDescription = null,
                                                tint = if (isSelected) Color.White else MikroTikCyan,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${pkg.name} (${pkg.retailPrice.toInt()} ر.ي)",
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. اسم الدفعة والفئة
                item {
                    OutlinedTextField(
                        value = batchName,
                        onValueChange = { batchName = it },
                        label = { Text("اسم الدفعة في سجل المخزن *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = categoryName,
                        onValueChange = { categoryName = it },
                        label = { Text("اسم الفئة / الصنف *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 4. تسعير الجملة والبيع النهائي
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = retailPriceText,
                            onValueChange = { retailPriceText = it },
                            label = { Text("سعر البيع النهائي (ر.ي)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = wholesalePriceText,
                            onValueChange = { wholesalePriceText = it },
                            label = { Text("سعر الجملة للبقالات (ر.ي)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // شارة الربح للبقالة
                item {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ProfitEmerald.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, ProfitEmerald.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("هامش ربح البقالة للكرت:", fontSize = 11.sp, color = ProfitEmerald)
                            Text("+${profitMargin.toInt()} ريال", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ProfitEmerald)
                        }
                    }
                }

                // 5. تبويبات طرق الإدخال
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("طريقة إدخال أو استيراد الكروت:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    TabRow(
                        selectedTabIndex = inputTab,
                        containerColor = MikroTikNavy,
                        contentColor = Color.White
                    ) {
                        Tab(
                            selected = inputTab == 0,
                            onClick = { inputTab = 0 },
                            text = { Text("📋 لصق / ملف", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = inputTab == 1,
                            onClick = { inputTab = 1 },
                            text = { Text("🔢 نطاق أرقام", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = inputTab == 2,
                            onClick = { inputTab = 2 },
                            text = { Text("⚡ كمية سريعة", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                // محتوى التبويب 0: لصق الرموز أو استيراد ملف
                if (inputTab == 0) {
                    item {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedButton(
                                        onClick = {
                                            val clip = clipboard.getText()?.text
                                            if (!clip.isNullOrBlank()) {
                                                pastedText = if (pastedText.isBlank()) clip else "$pastedText\n$clip"
                                                Toast.makeText(context, "تم اللصق من الحافظة", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "الحافظة فارغة", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("لصق", fontSize = 10.sp)
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    OutlinedButton(
                                        onClick = {
                                            filePickerLauncher.launch("text/*")
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("ملف CSV", fontSize = 10.sp)
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(
                                        onClick = {
                                            pastedText = "sam101,8821\nsam102,9943\nsam103,1102\nsam104,7731\nsam105,4490"
                                        },
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("مثال تجريبي", fontSize = 10.sp, color = MikroTikCyan)
                                    }

                                    if (pastedText.isNotEmpty()) {
                                        IconButton(
                                            onClick = { pastedText = "" },
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Icon(Icons.Default.Clear, contentDescription = "مسح", tint = Color.Gray, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = pastedText,
                                onValueChange = { pastedText = it },
                                label = { Text("الصق بيانات الكروت (رمز، باسورد أو رمز فقط)") },
                                placeholder = {
                                    Text(
                                        "يقبل الصيغ:\nuser1,pass1\nuser2:pass2\nuser3 (هوتسبوت PIN فقط)",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                },
                                minLines = 4,
                                maxLines = 7,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // محتوى التبويب 1: نطاق أرقام تسلسلي
                if (inputTab == 1) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = rangePrefix,
                                onValueChange = { rangePrefix = it },
                                label = { Text("بادئة الرمز (اختياري، مثلاً UM- أو NET)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = startNumberText,
                                    onValueChange = { startNumberText = it },
                                    label = { Text("من الرقم") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = endNumberText,
                                    onValueChange = { endNumberText = it },
                                    label = { Text("إلى الرقم") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Text("كلمة المرور في الكرت:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("نفس الرمز (PIN)", "رمز عشوائي 4 أرقام", "بدون كلمة مرور").forEachIndexed { idx, opt ->
                                    FilterChip(
                                        selected = rangePasswordOption == idx,
                                        onClick = { rangePasswordOption = idx },
                                        label = { Text(opt, fontSize = 10.sp) }
                                    )
                                }
                            }
                        }
                    }
                }

                // محتوى التبويب 2: كمية سريعة بالعدد
                if (inputTab == 2) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = bulkCountText,
                                onValueChange = { bulkCountText = it },
                                label = { Text("إجمالي عدد الكروت المنشأة في البرنامج الآخر *") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = bulkPrefix,
                                onValueChange = { bulkPrefix = it },
                                label = { Text("بادئة الرموز للتعريف في المخزن") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF0F1E33),
                                border = BorderStroke(1.dp, Color(0xFF1E3A5F)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "💡 سيتم إدراج $bulkCountText كرت في المخزن بأسماء مستخدمين تسلسلية تبدأ بـ $bulkPrefix جاهزة للتوزيع الفوري وإصدار الفواتير للبقالات.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8),
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }

                // 6. صندوق نتيجة التحليل المباشر والمعاينة
                item {
                    val count = parsedCards.size
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (count > 0) Color(0xFF062B28) else Color(0xFF2D1515),
                        border = BorderStroke(1.dp, if (count > 0) ProfitEmerald else Color(0xFFEF4444)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (count > 0) Icons.Default.CheckCircle else Icons.Default.HelpOutline,
                                        contentDescription = null,
                                        tint = if (count > 0) ProfitEmerald else Color(0xFFEF4444),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (count > 0) "تم التعرف على $count كرت جاهز للاستيراد" else "لم يتم التعرف على أي كروت بعد",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (count > 0) ProfitEmerald else Color(0xFFEF4444)
                                    )
                                }
                                if (count > 0) {
                                    Text(
                                        text = "الإجمالي: ${(wholesalePriceText.toDoubleOrNull() ?: 0.0) * count} ر.ي",
                                        fontSize = 11.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // معاينة أول 3 كروت
                            if (count > 0) {
                                Spacer(modifier = Modifier.height(6.dp))
                                HorizontalDivider(color = Color(0xFF0F4A42))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("معاينة العينات الأولى:", fontSize = 10.sp, color = Color(0xFF6EE7B7))
                                parsedCards.take(3).forEachIndexed { idx, (u, p) ->
                                    Text(
                                        text = "${idx + 1}. المستخدم: $u  |  الرمز: $p",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = Color.White
                                    )
                                }
                                if (count > 3) {
                                    Text("... والمتبقي ${count - 3} كرت إضافي", fontSize = 10.sp, color = Color(0xFF6EE7B7))
                                }
                            }
                        }
                    }
                }

                // 7. تحديد الوجهة بعد الاستيراد
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("الوجهة بعد الاستيراد:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (depositToWarehouse) Color(0xFF0284C7) else MaterialTheme.colorScheme.surfaceVariant,
                            onClick = {
                                depositToWarehouse = true
                                selectedRetailerForAssign = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "🏢 إيداع بالمستودع",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (depositToWarehouse) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (!depositToWarehouse) ProfitEmerald else MaterialTheme.colorScheme.surfaceVariant,
                            onClick = {
                                depositToWarehouse = false
                                if (selectedRetailerForAssign == null) {
                                    selectedRetailerForAssign = retailers.firstOrNull()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "🏪 تسليم مباشر لبقالة",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!depositToWarehouse) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp)
                            )
                        }
                    }
                }

                // اختيار البقالة إذا كان تسليم مباشر
                if (!depositToWarehouse) {
                    item {
                        ExposedDropdownMenuBox(
                            expanded = isRetailerDropdownExpanded,
                            onExpandedChange = { isRetailerDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedRetailerForAssign?.name ?: "اختر البقالة المستلمة",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("البقالة المستلمة للدفعة") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isRetailerDropdownExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = isRetailerDropdownExpanded,
                                onDismissRequest = { isRetailerDropdownExpanded = false }
                            ) {
                                retailers.forEach { r ->
                                    DropdownMenuItem(
                                        text = { Text("${r.name} (رصيد حالي: ${r.balanceOwed.toInt()} ر.ي)") },
                                        onClick = {
                                            selectedRetailerForAssign = r
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
                    val count = parsedCards.size
                    if (count == 0) {
                        Toast.makeText(context, "يرجى إدخال أو لصق كروت صالحة أولاً", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val finalRetail = retailPriceText.toDoubleOrNull() ?: 500.0
                    val finalWholesale = wholesalePriceText.toDoubleOrNull() ?: 450.0
                    val quotaMb = quotaMbText.toLongOrNull() ?: 4500L
                    val validityHours = validityHoursText.toIntOrNull() ?: 72
                    val speed = speedLimitText.ifBlank { "4M/2M" }

                    onImportConfirmed(
                        batchName,
                        categoryName,
                        finalRetail,
                        finalWholesale,
                        quotaMb,
                        validityHours,
                        speed,
                        parsedCards,
                        selectedSource,
                        if (inputTab == 1) rangePrefix else bulkPrefix,
                        if (!depositToWarehouse) selectedRetailerForAssign?.id else null
                    )
                },
                enabled = parsedCards.isNotEmpty() && batchName.isNotBlank() && (!depositToWarehouse.not() || selectedRetailerForAssign != null),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!depositToWarehouse) ProfitEmerald else Color(0xFF0284C7)
                )
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (depositToWarehouse) "استيراد ${parsedCards.size} كرت للمستودع" else "استيراد وتسليم ${parsedCards.size} كرت للبقالة",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
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
 * تحليل قائمة الكروت من النص الملصوق أو ملف CSV
 * يدعم الفواصل (فاصلة، نقطتان، تاب، خط عمودي، مسافة) أو سطر مفرد
 */
private fun parseCardsFromText(text: String): List<Pair<String, String>> {
    if (text.isBlank()) return emptyList()
    val lines = text.lines()
    val list = mutableListOf<Pair<String, String>>()

    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("//")) continue

        // فصل بالفاصلة أو النقطتان أو التاب أو المسافة
        val parts = when {
            trimmed.contains(",") -> trimmed.split(",")
            trimmed.contains(":") -> trimmed.split(":")
            trimmed.contains(";") -> trimmed.split(";")
            trimmed.contains("\t") -> trimmed.split("\t")
            trimmed.contains("|") -> trimmed.split("|")
            trimmed.contains(" ") -> trimmed.split(Regex("\\s+"))
            else -> listOf(trimmed)
        }

        val username = parts.getOrNull(0)?.trim() ?: ""
        val password = parts.getOrNull(1)?.trim() ?: username // إذا لم توجد كلمة مرور نجعلها نفس اسم المستخدم (هوتسبوت)

        if (username.isNotEmpty()) {
            list.add(Pair(username, password))
        }
    }
    return list
}

/**
 * توليد كروت من نطاق تسلسلي (مثلاً من 1001 إلى 1200)
 */
private fun generateRangeCards(
    prefix: String,
    startStr: String,
    endStr: String,
    passwordOption: Int
): List<Pair<String, String>> {
    val start = startStr.toIntOrNull() ?: return emptyList()
    val end = endStr.toIntOrNull() ?: return emptyList()
    if (end < start) return emptyList()
    val count = (end - start + 1).coerceAtMost(2000)

    val list = mutableListOf<Pair<String, String>>()
    for (i in 0 until count) {
        val num = start + i
        val username = "$prefix$num"
        val password = when (passwordOption) {
            0 -> username
            1 -> (1000 + (num * 37) % 9000).toString()
            else -> ""
        }
        list.add(Pair(username, password))
    }
    return list
}

/**
 * توليد كروت سريعة بالكمية فقط
 */
private fun generateBulkCountCards(
    prefix: String,
    countStr: String,
    categoryName: String
): List<Pair<String, String>> {
    val count = (countStr.toIntOrNull() ?: 0).coerceIn(1, 2000)
    val list = mutableListOf<Pair<String, String>>()
    val catCode = categoryName.filter { it.isDigit() }.ifEmpty { "EXT" }

    for (i in 1..count) {
        val username = "$prefix$catCode-${String.format(java.util.Locale.US, "%04d", i)}"
        val password = (1000 + (i * 17) % 9000).toString()
        list.add(Pair(username, password))
    }
    return list
}
