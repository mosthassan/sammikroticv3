package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.RetailerEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import com.example.util.CustomerJsonHelper

/**
 * نافذة تصدير واستيراد بيانات العملاء والبقالات كملف JSON
 */
@Composable
fun CustomerJsonBackupDialog(
    viewModel: MainViewModel,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val currentRetailers by viewModel.retailers.collectAsState()
    val networkIdentity by viewModel.networkIdentity.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Export, 1: Import
    var jsonInput by remember { mutableStateOf("") }
    var parseError by remember { mutableStateOf<String?>(null) }
    var parsedCustomers by remember { mutableStateOf<List<RetailerEntity>?>(null) }
    var replaceExisting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }

    val exportedJson = remember(currentRetailers, networkIdentity) {
        val netName = networkIdentity?.networkName?.ifBlank { "شبكة المايكروتك" } ?: "شبكة المايكروتك"
        CustomerJsonHelper.exportCustomersToJson(currentRetailers, netName)
    }

    fun updateImportJson(content: String) {
        jsonInput = content
        if (content.isBlank()) {
            parseError = null
            parsedCustomers = null
            return
        }
        val result = CustomerJsonHelper.parseCustomersFromJson(content)
        result.onSuccess { customers ->
            parsedCustomers = customers
            parseError = null
        }.onFailure { err ->
            parsedCustomers = null
            parseError = err.localizedMessage ?: "صيغة JSON غير صحيحة"
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val content = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                if (content.isNotBlank()) {
                    updateImportJson(content)
                    Toast.makeText(context, "تم قراءة ملف JSON للعملاء بنجاح ✓", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "الملف فارغ", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "فشل قراءة الملف: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val fileSaverLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(exportedJson.toByteArray())
                }
                Toast.makeText(context, "تم حفظ ملف العملاء بنجاح 💾", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "فشل حفظ الملف: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.97f)
                .fillMaxHeight(0.94f)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .clip(RoundedCornerShape(18.dp)),
            color = CyberDarkCanvas,
            border = BorderStroke(1.dp, CyberBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MikroTikCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Store,
                                contentDescription = null,
                                tint = MikroTikCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "تصدير واستيراد العملاء والبقالات (JSON)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White,
                                fontFamily = CairoFontFamily
                            )
                            Text(
                                text = "نقل بيانات العملاء والوكلاء والأرصدة بين الهواتف بسهولة",
                                fontSize = 10.5.sp,
                                color = TextSecondaryDark,
                                fontFamily = CairoFontFamily
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(34.dp).testTag("close_customer_backup_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tab Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberDarkSurface)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        color = if (selectedTab == 0) MikroTikCyan else Color.Transparent,
                        onClick = { selectedTab = 0 }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.FileUpload,
                                contentDescription = null,
                                tint = if (selectedTab == 0) Color.Black else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "تصدير العملاء (${currentRetailers.size}) 📤",
                                color = if (selectedTab == 0) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = CairoFontFamily
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        color = if (selectedTab == 1) ProfitEmerald else Color.Transparent,
                        onClick = { selectedTab = 1 }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.FileDownload,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "استيراد عملاء من JSON 📥",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = CairoFontFamily
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (selectedTab == 0) {
                    // EXPORT TAB
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, CyberBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = MikroTikCyan, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "جاهز لتصدير ${currentRetailers.size} عميل وبقالة من قاعدة البيانات",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color.White,
                                        fontFamily = CairoFontFamily
                                    )
                                    Text(
                                        text = "يشمل الأسماء، الهواتف، العناوين، العمولة، والأرصدة المستحقة",
                                        fontSize = 10.5.sp,
                                        color = TextSecondaryDark,
                                        fontFamily = CairoFontFamily
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "معاينة كود JSON المُنشأ:",
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = CairoFontFamily
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        OutlinedTextField(
                            value = exportedJson,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.5.sp,
                                color = Color(0xFFE2E8F0)
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberBorder,
                                unfocusedBorderColor = CyberBorder,
                                focusedContainerColor = CyberDarkSurface,
                                unfocusedContainerColor = CyberDarkSurface
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    try {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("Customers JSON", exportedJson)
                                        clipboard?.setPrimaryClip(clip)
                                        Toast.makeText(context, "تم نسخ كود JSON للعملاء إلى الحافظة 📋", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "فشل النسخ: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, MikroTikCyan.copy(alpha = 0.6f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MikroTikCyan)
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("نسخ الكود 📋", fontSize = 11.5.sp, fontFamily = CairoFontFamily)
                            }

                            OutlinedButton(
                                onClick = {
                                    try {
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, exportedJson)
                                            putExtra(Intent.EXTRA_TITLE, "نسخة_احتياطية_للعملاء.json")
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "مشاركة بيانات العملاء عبر:"))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "فشل المشاركة: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, InvestmentGold.copy(alpha = 0.6f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = InvestmentGold)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("مشاركة 📤", fontSize = 11.5.sp, fontFamily = CairoFontFamily)
                            }

                            Button(
                                onClick = {
                                    fileSaverLauncher.launch("customers_${System.currentTimeMillis() % 100000}.json")
                                },
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MikroTikCyan)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("حفظ كملف 💾", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, fontFamily = CairoFontFamily)
                            }
                        }
                    }
                } else {
                    // IMPORT TAB
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = { filePickerLauncher.launch("*/*") },
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(38.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ProfitEmerald),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.FileUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("اختيار ملف JSON 📁", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = CairoFontFamily)
                            }

                            OutlinedButton(
                                onClick = {
                                    try {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        val clipText = clipboard?.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                                        if (clipText.isNotBlank()) {
                                            updateImportJson(clipText)
                                            Toast.makeText(context, "تم لصق كود JSON للعملاء", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "الحافظة فارغة", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "تعذر اللصق: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .weight(0.9f)
                                    .height(38.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = BorderStroke(1.dp, CyberBorder),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("لصق 📋", fontSize = 11.sp, fontFamily = CairoFontFamily)
                            }

                            OutlinedButton(
                                onClick = {
                                    updateImportJson(CustomerJsonHelper.SAMPLE_CUSTOMERS_JSON)
                                    Toast.makeText(context, "تم إدراج نموذج عملاء تجريبي", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .weight(1.1f)
                                    .height(38.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = InvestmentGold),
                                border = BorderStroke(1.dp, InvestmentGold.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp), tint = InvestmentGold)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("نموذج جاهز 💡", fontSize = 11.sp, fontFamily = CairoFontFamily)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (parsedCustomers == null) {
                            OutlinedTextField(
                                value = jsonInput,
                                onValueChange = { updateImportJson(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                placeholder = {
                                    Text(
                                        text = "الصق كود JSON الخاص بالعملاء هنا، أو اضغط على 'اختيار ملف JSON'...",
                                        color = TextSecondaryDark,
                                        fontSize = 11.sp,
                                        fontFamily = CairoFontFamily
                                    )
                                },
                                textStyle = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color(0xFFE2E8F0)
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ProfitEmerald,
                                    unfocusedBorderColor = CyberBorder,
                                    focusedContainerColor = CyberDarkSurface,
                                    unfocusedContainerColor = CyberDarkSurface
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )

                            if (parseError != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = PaymentRed.copy(alpha = 0.15f)),
                                    border = BorderStroke(1.dp, PaymentRed.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = PaymentRed, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(parseError ?: "", color = Color(0xFFFECACA), fontSize = 11.sp, fontFamily = CairoFontFamily)
                                    }
                                }
                            }
                        } else {
                            val customersList = parsedCustomers!!

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ProfitEmerald, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "تم استخراج (${customersList.size}) عميل بنجاح ✓",
                                            color = ProfitEmerald,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                            fontFamily = CairoFontFamily
                                        )
                                    }
                                    TextButton(
                                        onClick = { updateImportJson("") },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                    ) {
                                        Text("إلغاء والتعديل", color = TextSecondaryDark, fontSize = 11.sp, fontFamily = CairoFontFamily)
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(CyberDarkSurface)
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "طريقة الإضافة:",
                                        fontSize = 11.sp,
                                        color = TextSecondaryDark,
                                        fontFamily = CairoFontFamily
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        FilterChip(
                                            selected = !replaceExisting,
                                            onClick = { replaceExisting = false },
                                            label = { Text("دمج وإضافة جديدة", fontSize = 10.5.sp, fontFamily = CairoFontFamily) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = ProfitEmerald.copy(alpha = 0.3f),
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                        FilterChip(
                                            selected = replaceExisting,
                                            onClick = { replaceExisting = true },
                                            label = { Text("استبدال كل العملاء", fontSize = 10.5.sp, fontFamily = CairoFontFamily) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = PaymentRed.copy(alpha = 0.3f),
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    itemsIndexed(customersList) { idx, customer ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, CyberBorder)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("${idx + 1}. ", color = TextSecondaryDark, fontSize = 11.sp)
                                                    Column {
                                                        Text(
                                                            text = customer.name,
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 11.5.sp,
                                                            fontFamily = CairoFontFamily
                                                        )
                                                        Text(
                                                            text = "${customer.ownerName} • ${customer.phone}",
                                                            color = TextSecondaryDark,
                                                            fontSize = 10.sp,
                                                            fontFamily = CairoFontFamily
                                                        )
                                                    }
                                                }

                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(
                                                        text = "دين: ${customer.balanceOwed.toInt()} ر.ي",
                                                        color = if (customer.balanceOwed.toDouble() > 0) PaymentRed else ReceiptGreen,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.5.sp,
                                                        fontFamily = CairoFontFamily
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = CyberDarkSurface,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, CyberBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = onDismissRequest,
                                    modifier = Modifier
                                        .weight(0.8f)
                                        .height(44.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondaryDark)
                                ) {
                                    Text("إلغاء", fontFamily = CairoFontFamily, fontSize = 11.5.sp)
                                }

                                Button(
                                    onClick = {
                                        val listToImport = parsedCustomers ?: return@Button
                                        isImporting = true
                                        viewModel.importRetailersBatch(
                                            retailersToImport = listToImport,
                                            replaceExisting = replaceExisting,
                                            onComplete = { count ->
                                                isImporting = false
                                                Toast.makeText(
                                                    context,
                                                    "تم استيراد وحفظ $count عميل/بقالة بنجاح في قاعدة البيانات! ✓",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                onDismissRequest()
                                            }
                                        )
                                    },
                                    enabled = parsedCustomers != null && !isImporting,
                                    modifier = Modifier
                                        .weight(2f)
                                        .height(44.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ProfitEmerald,
                                        disabledContainerColor = CyberDarkCardElevated
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    if (isImporting) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.DownloadDone, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (parsedCustomers != null) "بدء استيراد (${parsedCustomers!!.size}) عميل 🚀" else "استيراد العملاء 📥",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            fontFamily = CairoFontFamily,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
