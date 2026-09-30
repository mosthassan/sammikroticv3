package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.data.local.entity.PurchaseInvoiceEntity
import com.example.data.model.ParsedInvoiceData
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import com.example.util.CurrencyHelper
import com.example.util.JsonInvoiceParser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * نافذة استيراد وترحيل الفواتير من كود أو ملف JSON بدقة 100% وبدون استهلاك توكن
 * - تتيح خيارين واضحين للمستخدم:
 *   1. حفظ واعتماد الفاتورة فوراً في قاعدة البيانات والأصول بنقرة واحدة
 *   2. فتح شاشة التعديل والمراجعة الكاملة للأصناف والأسعار قبل الحفظ
 */
@Composable
fun JsonInvoiceImportDialog(
    viewModel: MainViewModel? = null,
    onDismissRequest: () -> Unit,
    onInvoiceImported: (ParsedInvoiceData) -> Unit
) {
    val context = LocalContext.current
    var jsonInput by remember { mutableStateOf("") }
    var parseError by remember { mutableStateOf<String?>(null) }
    var previewData by remember { mutableStateOf<ParsedInvoiceData?>(null) }
    var showRawJsonEditor by remember { mutableStateOf(true) }
    var isSavingDirectly by remember { mutableStateOf(false) }

    // Target type override: CAPEX (أصول) or OPEX (مصروفات)
    var selectedTargetType by remember(previewData) {
        mutableStateOf(previewData?.invoiceType ?: "ASSETS")
    }

    fun updateJson(content: String) {
        jsonInput = content
        if (content.isBlank()) {
            parseError = null
            previewData = null
            showRawJsonEditor = true
            return
        }
        val result = JsonInvoiceParser.parse(content)
        result.onSuccess { data ->
            previewData = data
            selectedTargetType = data.invoiceType
            parseError = null
            // Auto-collapse JSON code box when parsed successfully to maximize preview space
            showRawJsonEditor = false
        }.onFailure { err ->
            previewData = null
            parseError = err.localizedMessage ?: "صيغة JSON غير صحيحة"
            showRawJsonEditor = true
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
                    updateJson(content)
                    Toast.makeText(context, "تم تحميل ملف JSON بنجاح ✓", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "الملف فارغ", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "تعذر قراءة الملف: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .navigationBarsPadding()
                .imePadding()
                .clip(RoundedCornerShape(18.dp)),
            color = MikroTikDarkBg,
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                // Fixed Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MikroTikCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = MikroTikCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "استيراد فاتورة من JSON",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White,
                                    fontFamily = CairoFontFamily
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(ProfitEmerald.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "بدون استهلاك توكن ⚡",
                                        color = ProfitEmerald,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = CairoFontFamily
                                    )
                                }
                            }
                            Text(
                                text = "تفريغ وتوثيق الفاتورة فورياً وبدقة 100%",
                                fontSize = 10.5.sp,
                                color = Color(0xFF94A3B8),
                                fontFamily = CairoFontFamily
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("close_json_import_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable Body
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Action Buttons Row: Pick File, Paste, Insert Sample
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { filePickerLauncher.launch("*/*") },
                            modifier = Modifier
                                .weight(1.3f)
                                .height(38.dp)
                                .testTag("pick_json_file_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MikroTikCyan),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, tint = Color.Black, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("اختيار ملف JSON 📁", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = CairoFontFamily)
                        }

                        OutlinedButton(
                            onClick = {
                                try {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val clipText = clipboard?.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                                    if (clipText.isNotBlank()) {
                                        updateJson(clipText)
                                        Toast.makeText(context, "تم لصق النص من الحافظة", Toast.LENGTH_SHORT).show()
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
                            border = BorderStroke(1.dp, Color(0xFF475569)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("لصق 📋", fontSize = 11.sp, fontFamily = CairoFontFamily)
                        }

                        OutlinedButton(
                            onClick = {
                                updateJson(JsonInvoiceParser.SAMPLE_JSON)
                                Toast.makeText(context, "تم إدراج نموذج تجريبي جاهز", Toast.LENGTH_SHORT).show()
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

                    // Collapsible JSON Input Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MikroTikDarkSurface),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (parseError != null) PaymentRed.copy(alpha = 0.5f) else Color(0xFF334155))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { showRawJsonEditor = !showRawJsonEditor }
                                ) {
                                    Icon(
                                        imageVector = if (showRawJsonEditor) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = MikroTikCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (previewData != null) "كود JSON المستورد (${previewData?.items?.size ?: 0} أصناف)" else "محتوى أو كود JSON:",
                                        color = if (previewData != null) Color.White else Color(0xFF94A3B8),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = CairoFontFamily
                                    )
                                }

                                Row {
                                    if (jsonInput.isNotBlank()) {
                                        TextButton(
                                            onClick = { showRawJsonEditor = !showRawJsonEditor },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                        ) {
                                            Text(
                                                text = if (showRawJsonEditor) "طي الكود ▲" else "عرض/تعديل الكود ▼",
                                                color = MikroTikCyan,
                                                fontSize = 10.5.sp,
                                                fontFamily = CairoFontFamily
                                            )
                                        }
                                        TextButton(
                                            onClick = { updateJson("") },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = PaymentRed, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text("تفريغ", color = PaymentRed, fontSize = 10.5.sp, fontFamily = CairoFontFamily)
                                        }
                                    }
                                }
                            }

                            AnimatedVisibility(visible = showRawJsonEditor || previewData == null) {
                                Column {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = jsonInput,
                                        onValueChange = { updateJson(it) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(if (previewData != null) 100.dp else 140.dp)
                                            .testTag("json_invoice_input_field"),
                                        placeholder = {
                                            Text(
                                                text = "قم بلصق محتوى ملف JSON هنا، أو انقر على 'اختيار ملف JSON' من الأعلى...",
                                                color = Color(0xFF64748B),
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
                                            focusedBorderColor = MikroTikCyan,
                                            unfocusedBorderColor = Color(0xFF334155),
                                            focusedContainerColor = MikroTikNavyLight,
                                            unfocusedContainerColor = MikroTikNavyLight
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Parse Error State
                    if (parseError != null && jsonInput.isNotBlank()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = PaymentRed.copy(alpha = 0.12f)),
                            border = BorderStroke(1.dp, PaymentRed.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = PaymentRed, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text("خطأ في قراءة ملف JSON", color = PaymentRed, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, fontFamily = CairoFontFamily)
                                    Text(parseError ?: "", color = Color(0xFFFECACA), fontSize = 10.5.sp, fontFamily = CairoFontFamily)
                                }
                            }
                        }
                    }

                    // Parsed Preview Card (The Main View)
                    if (previewData != null) {
                        val data = previewData!!
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F243A)),
                            border = BorderStroke(1.5.dp, ProfitEmerald.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                // Status & Target Type Selector
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ProfitEmerald, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "تم تفريغ الفاتورة بنجاح ✓",
                                            color = ProfitEmerald,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                            fontFamily = CairoFontFamily
                                        )
                                    }

                                    // Switch between CAPEX and OPEX
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (selectedTargetType == "ASSETS") AssetPurple else Color.Transparent,
                                            border = BorderStroke(1.dp, AssetPurple.copy(alpha = 0.6f)),
                                            onClick = { selectedTargetType = "ASSETS" }
                                        ) {
                                            Text(
                                                text = "أصول (CAPEX)",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (selectedTargetType == "ASSETS") Color.White else AssetPurple,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                fontFamily = CairoFontFamily
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (selectedTargetType != "ASSETS") InvestmentGold else Color.Transparent,
                                            border = BorderStroke(1.dp, InvestmentGold.copy(alpha = 0.6f)),
                                            onClick = { selectedTargetType = "EXPENSES" }
                                        ) {
                                            Text(
                                                text = "مشتريات (OPEX)",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (selectedTargetType != "ASSETS") Color.Black else InvestmentGold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                fontFamily = CairoFontFamily
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Supplier & Invoice Info Strip
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.04f))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("المورد:", color = Color(0xFF94A3B8), fontSize = 9.5.sp, fontFamily = CairoFontFamily)
                                        Text(data.supplierName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, fontFamily = CairoFontFamily)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("رقم الفاتورة:", color = Color(0xFF94A3B8), fontSize = 9.5.sp, fontFamily = CairoFontFamily)
                                        Text(data.invoiceNumber, color = MikroTikCyan, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, fontFamily = CairoFontFamily)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("التاريخ:", color = Color(0xFF94A3B8), fontSize = 9.5.sp, fontFamily = CairoFontFamily)
                                        Text(data.invoiceDate, color = Color(0xFFCBD5E1), fontSize = 11.sp, fontFamily = CairoFontFamily)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Items List Preview Header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "الأصناف المستخرجة (${data.items.size} صنف):",
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = CairoFontFamily
                                    )
                                    Text(
                                        text = "يمكنك التعديل الكامل عند فتح التعديل",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 9.5.sp,
                                        fontFamily = CairoFontFamily
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Display all items up to 6, then remainder note
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    data.items.take(6).forEachIndexed { idx, item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color.White.copy(alpha = 0.05f))
                                                .padding(horizontal = 8.dp, vertical = 5.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                                Text("${idx + 1}. ", color = Color(0xFF64748B), fontSize = 10.5.sp)
                                                Text(item.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium, fontFamily = CairoFontFamily)
                                            }
                                            Text(
                                                text = "${item.quantity.toInt()} × ${String.format(Locale.US, "%,.0f", item.unitPrice)} = ${String.format(Locale.US, "%,.0f", item.subtotal)} ${CurrencyHelper.getCurrencySymbol(item.currency.ifBlank { data.currency })}",
                                                color = ProfitEmerald,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    if (data.items.size > 6) {
                                        Text(
                                            text = "... وباقي ${data.items.size - 6} أصناف أخرى مدرجة في الفاتورة",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Total Amount Box
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ProfitEmerald.copy(alpha = 0.15f))
                                        .padding(horizontal = 12.dp, vertical = 7.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("إجمالي قيمة الفاتورة المستوردة:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, fontFamily = CairoFontFamily)
                                    Text(
                                        text = "${String.format(Locale.US, "%,.0f", data.totalAmount)} ${data.currency}",
                                        color = ProfitEmerald,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.5.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // PINNED BOTTOM ACTION BAR (Guaranteed Visible on All Screen Sizes)
                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MikroTikDarkSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        if (previewData != null) {
                            Text(
                                text = "اختر الإجراء المطلوب:",
                                fontSize = 10.5.sp,
                                color = Color(0xFF94A3B8),
                                fontFamily = CairoFontFamily,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Cancel button
                            OutlinedButton(
                                onClick = onDismissRequest,
                                modifier = Modifier
                                    .weight(0.7f)
                                    .height(44.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                                border = BorderStroke(1.dp, Color(0xFF475569)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text("إلغاء", fontFamily = CairoFontFamily, fontSize = 11.sp)
                            }

                            // Option 1: Edit & Review in detail before saving
                            Button(
                                onClick = {
                                    val data = previewData ?: return@Button
                                    val updatedData = data.copy(invoiceType = selectedTargetType)
                                    onInvoiceImported(updatedData)
                                    onDismissRequest()
                                },
                                enabled = previewData != null && !isSavingDirectly,
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(44.dp)
                                    .testTag("edit_before_save_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF0284C7),
                                    disabledContainerColor = Color(0xFF334155)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "تعديل قبل الحفظ ✏️",
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = CairoFontFamily,
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                            }

                            // Option 2: Save directly to database now
                            Button(
                                onClick = {
                                    val data = previewData ?: return@Button
                                    isSavingDirectly = true
                                    val finalTarget = selectedTargetType
                                    val finalDateMillis = try {
                                        SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(data.invoiceDate)?.time ?: System.currentTimeMillis()
                                    } catch (e: Exception) {
                                        System.currentTimeMillis()
                                    }
                                    val finalInvoice = PurchaseInvoiceEntity(
                                        invoiceNumber = data.invoiceNumber.ifBlank { "JSON-${System.currentTimeMillis() % 100000}" },
                                        supplierName = data.supplierName.ifBlank { "مورد مشتريات" },
                                        invoiceDateMillis = finalDateMillis,
                                        targetType = finalTarget,
                                        totalAmount = if (data.totalAmount > 0) java.math.BigDecimal.valueOf(data.totalAmount) else java.math.BigDecimal.valueOf(data.items.sumOf { it.subtotal }),
                                        currency = data.currency,
                                        paymentMethod = "نقداً",
                                        notes = data.notes,
                                        itemsJson = "",
                                        itemsSummary = "",
                                        status = "APPROVED"
                                    )

                                    if (viewModel != null) {
                                        viewModel.approveAndSaveInvoice(
                                            invoice = finalInvoice,
                                            items = data.items,
                                            saveAsAssets = finalTarget == "ASSETS",
                                            saveAsVoucher = true,
                                            onComplete = {
                                                isSavingDirectly = false
                                                Toast.makeText(
                                                    context,
                                                    "تم حفظ وترحيل الفاتورة #${finalInvoice.invoiceNumber} و ${data.items.size} أصناف بنجاح! ✓",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                onDismissRequest()
                                            }
                                        )
                                    } else {
                                        // Fallback to onInvoiceImported
                                        isSavingDirectly = false
                                        onInvoiceImported(data.copy(invoiceType = finalTarget))
                                        onDismissRequest()
                                    }
                                },
                                enabled = previewData != null && !isSavingDirectly,
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(44.dp)
                                    .testTag("apply_json_import_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ProfitEmerald,
                                    disabledContainerColor = Color(0xFF334155)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                if (isSavingDirectly) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "حفظ الفاتورة فوراً 💾",
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = CairoFontFamily,
                                        fontSize = 11.5.sp,
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
