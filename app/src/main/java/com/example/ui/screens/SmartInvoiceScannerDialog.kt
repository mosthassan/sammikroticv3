package com.example.ui.screens

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.PurchaseInvoiceEntity
import com.example.data.model.InvoiceItem
import com.example.data.model.ParsedInvoiceData
import com.example.ui.MainViewModel
import com.example.ui.theme.AssetPurple
import com.example.ui.theme.CairoFontFamily
import com.example.ui.theme.InvestmentGold
import com.example.ui.theme.MikroTikCyan
import com.example.ui.theme.MikroTikDarkBg
import com.example.ui.theme.MikroTikDarkSurface
import com.example.ui.theme.MikroTikNavy
import com.example.ui.theme.MikroTikNavyLight
import com.example.ui.theme.MikroTikPrimary
import com.example.ui.theme.PaymentRed
import com.example.ui.theme.ProfitEmerald
import com.example.ui.theme.ReceiptGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartInvoiceScannerDialog(
    viewModel: MainViewModel,
    initialTargetType: String = "ASSETS", // "ASSETS" or "EXPENSES"
    initialParsedData: ParsedInvoiceData? = null,
    invoiceToEdit: PurchaseInvoiceEntity? = null,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val isScanning by viewModel.isScanningInvoice.collectAsState()

    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var hasParsedInvoice by remember { mutableStateOf(invoiceToEdit != null || initialParsedData != null) }
    var isAiExtracted by remember { mutableStateOf(invoiceToEdit != null || initialParsedData != null) }
    var aiErrorMessage by remember { mutableStateOf<String?>(null) }
    var showJsonImportDialog by remember { mutableStateOf(false) }

    // Invoice Header Fields
    var supplierName by remember { mutableStateOf(invoiceToEdit?.supplierName ?: initialParsedData?.supplierName ?: "") }
    var invoiceNumber by remember { mutableStateOf(invoiceToEdit?.invoiceNumber ?: initialParsedData?.invoiceNumber ?: "") }
    var invoiceDate by remember {
        mutableStateOf(
            if (invoiceToEdit != null) SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date(invoiceToEdit.invoiceDateMillis))
            else if (!initialParsedData?.invoiceDate.isNullOrBlank()) initialParsedData!!.invoiceDate
            else SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())
        )
    }
    var targetType by remember {
        mutableStateOf(
            if (invoiceToEdit != null) invoiceToEdit.targetType
            else if (!initialParsedData?.invoiceType.isNullOrBlank()) initialParsedData!!.invoiceType
            else initialTargetType
        )
    }
    var currency by remember {
        mutableStateOf(
            if (invoiceToEdit != null) invoiceToEdit.currency
            else if (!initialParsedData?.currency.isNullOrBlank()) initialParsedData!!.currency
            else "YER"
        )
    }
    var paymentMethod by remember { mutableStateOf(invoiceToEdit?.paymentMethod ?: "نقداً") }
    var notes by remember { mutableStateOf(invoiceToEdit?.notes ?: initialParsedData?.notes ?: "") }
    var saveAsAssets by remember {
        mutableStateOf(
            if (invoiceToEdit != null) invoiceToEdit.targetType == "ASSETS"
            else (if (!initialParsedData?.invoiceType.isNullOrBlank()) initialParsedData!!.invoiceType else initialTargetType) == "ASSETS"
        )
    }
    var saveAsVoucher by remember { mutableStateOf(true) }

    var showApiKeyDialog by remember { mutableStateOf(false) }
    var tempPhotoFile by remember { mutableStateOf<File?>(null) }
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

    // Dynamic Items List
    val itemsList = remember(invoiceToEdit, initialParsedData) {
        mutableStateListOf<InvoiceItem>().apply {
            if (invoiceToEdit != null && invoiceToEdit.itemsJson.isNotBlank()) {
                try {
                    val arr = org.json.JSONArray(invoiceToEdit.itemsJson)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        add(
                            InvoiceItem(
                                id = obj.optString("id", UUID.randomUUID().toString()),
                                name = obj.optString("name", "صنف"),
                                quantity = obj.optDouble("quantity", 1.0),
                                unitPrice = obj.optDouble("unitPrice", 0.0),
                                subtotal = obj.optDouble("subtotal", 0.0),
                                category = obj.optString("category", "GENERAL")
                            )
                        )
                    }
                } catch (e: Exception) {
                    // fallback
                }
            } else if (initialParsedData != null && initialParsedData.items.isNotEmpty()) {
                addAll(initialParsedData.items)
            }
        }
    }

    fun processInvoiceBitmap(bitmap: Bitmap) {
        aiErrorMessage = null
        if (!viewModel.isAiApiKeyConfigured()) {
            showApiKeyDialog = true
            return
        }
        viewModel.scanInvoiceWithAi(
            bitmap = bitmap,
            onResult = { parsed ->
                populateFieldsFromParsed(
                    parsed = parsed,
                    setSupplier = { supplierName = it },
                    setNumber = { invoiceNumber = it },
                    setDate = { if (it.isNotBlank()) invoiceDate = it },
                    setTarget = { targetType = it },
                    setNotes = { notes = it },
                    setCurrency = { currency = it },
                    itemsList = itemsList
                )
                hasParsedInvoice = true
                isAiExtracted = true
                aiErrorMessage = null
                if (parsed.items.isEmpty()) {
                    Toast.makeText(context, "تم تحليل الصورة بنجاح ولكن لم يتم العثور على أصناف واضحة، يمكنك إضافتها يدوياً", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "تم استخراج وقراءة بيانات الفاتورة الفعلية (${parsed.items.size} أصناف) بنجاح! ✓", Toast.LENGTH_SHORT).show()
                }
            },
            onError = { err ->
                isAiExtracted = false
                aiErrorMessage = err
                if (err.contains("مفتاح") || err.contains("API") || err.contains("key") || err.contains("400") || err.contains("401") || err.contains("403")) {
                    showApiKeyDialog = true
                }
                Toast.makeText(context, "تنبيه قراءة الفاتورة: $err", Toast.LENGTH_LONG).show()
            }
        )
    }

    // High-Resolution Camera Launcher via FileProvider
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoFile != null && tempPhotoFile!!.exists()) {
            val bitmap = BitmapFactory.decodeFile(tempPhotoFile!!.absolutePath)
            if (bitmap != null) {
                capturedBitmap = bitmap
                hasParsedInvoice = false
                isAiExtracted = false
                processInvoiceBitmap(bitmap)
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val invoicesDir = File(context.cacheDir, "invoices").apply { mkdirs() }
                val photoFile = File(invoicesDir, "inv_capture_${System.currentTimeMillis()}.jpg")
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
                tempPhotoFile = photoFile
                tempPhotoUri = uri
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(context, "تعذر تشغيل الكاميرا: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "يلزم منح إذن الكاميرا لتصوير الفاتورة بدقة عالية", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchCamera() {
        val hasCam = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (hasCam) {
            try {
                val invoicesDir = File(context.cacheDir, "invoices").apply { mkdirs() }
                val photoFile = File(invoicesDir, "inv_capture_${System.currentTimeMillis()}.jpg")
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
                tempPhotoFile = photoFile
                tempPhotoUri = uri
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(context, "تعذر تشغيل الكاميرا: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val bitmap = uriToBitmap(context, uri)
            if (bitmap != null) {
                capturedBitmap = bitmap
                hasParsedInvoice = false
                isAiExtracted = false
                processInvoiceBitmap(bitmap)
            }
        }
    }

    val totalCalculated = itemsList.sumOf { it.subtotal }
    var isSavingInvoice by remember { mutableStateOf(false) }

    val performSaveInvoice: () -> Unit = {
        if (!isSavingInvoice) {
            if (itemsList.isEmpty()) {
                Toast.makeText(context, "يرجى إضافة صنف واحد على الأقل للفاتورة", Toast.LENGTH_SHORT).show()
            } else {
                isSavingInvoice = true
                val finalDateMillis = try {
                    SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(invoiceDate)?.time ?: System.currentTimeMillis()
                } catch (e: Exception) {
                    invoiceToEdit?.invoiceDateMillis ?: System.currentTimeMillis()
                }

                val finalInvNum = invoiceNumber.ifBlank { "INV-${System.currentTimeMillis() % 100000}" }
                val finalSupp = supplierName.ifBlank { "مورد أجهزة ومعدات" }

                val finalInvoice = if (invoiceToEdit != null) {
                    invoiceToEdit.copy(
                        invoiceNumber = finalInvNum,
                        supplierName = finalSupp,
                        invoiceDateMillis = finalDateMillis,
                        targetType = targetType,
                        totalAmount = java.math.BigDecimal.valueOf(totalCalculated),
                        currency = currency,
                        originalAmount = java.math.BigDecimal.valueOf(totalCalculated),
                        paidAmount = java.math.BigDecimal.valueOf(totalCalculated),
                        paymentMethod = paymentMethod,
                        notes = notes
                    )
                } else {
                    PurchaseInvoiceEntity(
                        invoiceNumber = finalInvNum,
                        supplierName = finalSupp,
                        invoiceDateMillis = finalDateMillis,
                        targetType = targetType,
                        totalAmount = java.math.BigDecimal.valueOf(totalCalculated),
                        currency = currency,
                        originalAmount = java.math.BigDecimal.valueOf(totalCalculated),
                        paidAmount = java.math.BigDecimal.valueOf(totalCalculated),
                        paymentMethod = paymentMethod,
                        notes = notes
                    )
                }

                viewModel.approveAndSaveInvoice(
                    invoice = finalInvoice,
                    items = itemsList.toList(),
                    saveAsAssets = saveAsAssets,
                    saveAsVoucher = saveAsVoucher,
                    onComplete = {
                        Toast.makeText(context, if (invoiceToEdit != null) "تم تحديث بيانات الفاتورة بنجاح! ✓" else "تم اعتماد وحفظ الفاتورة والأصناف بنجاح! ✓", Toast.LENGTH_LONG).show()
                        onDismissRequest()
                    }
                )
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
            color = MikroTikDarkBg,
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                // Top Header Bar
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
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MikroTikCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "تحويل وتعديل الفاتورة الذكية",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = Color.White
                            )
                            Text(
                                text = "استخراج وتعديل الأصناف والأسعار والاعتماد",
                                fontSize = 10.5.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Quick Save button in header if invoice data is loaded/being edited
                        if (hasParsedInvoice || capturedBitmap != null) {
                            Button(
                                onClick = { performSaveInvoice() },
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("top_save_invoice_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = ProfitEmerald),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("حفظ 💾", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = CairoFontFamily)
                            }

                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        OutlinedButton(
                            onClick = { showJsonImportDialog = true },
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("top_import_json_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MikroTikCyan),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MikroTikCyan.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("استيراد JSON", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = CairoFontFamily)
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = onDismissRequest,
                            modifier = Modifier.size(32.dp).testTag("close_invoice_dialog")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // AI Key Status Banner
                val isAiConfigured = viewModel.isAiApiKeyConfigured()
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showApiKeyDialog = true },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isAiConfigured) Color(0xFF064E3B).copy(alpha = 0.5f) else Color(0xFF78350F).copy(alpha = 0.5f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isAiConfigured) ProfitEmerald.copy(alpha = 0.6f) else InvestmentGold.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = if (isAiConfigured) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isAiConfigured) ProfitEmerald else InvestmentGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (isAiConfigured) "وكيل الذكاء الاصطناعي (Gemini Vision) جاهز ومفعل ✓" else "تفعيل مفتاح الذكاء الاصطناعي لقراءة الفواتير الحقيقية",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isAiConfigured) "انقر لمراجعة أو تغيير المفتاح" else "انقر هنا لإدخال مفتاح Gemini API لمنع أي بيانات وهمية",
                                    fontSize = 10.sp,
                                    color = if (isAiConfigured) Color(0xFFA7F3D0) else Color(0xFFFDE68A)
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = { showApiKeyDialog = true },
                            modifier = Modifier.height(30.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isAiConfigured) ProfitEmerald else InvestmentGold),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isAiConfigured) ProfitEmerald else InvestmentGold)
                        ) {
                            Text(if (isAiConfigured) "تعديل ⚙️" else "إدخال المفتاح 🔑", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // If currently scanning with AI: show animated spinner
                if (isScanning) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = MikroTikCyan,
                                strokeWidth = 4.dp,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "جارٍ قراءة وفحص صورة الفاتورة بواسطة الذكاء الاصطناعي (Gemini Vision)...",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "استخراج اسم المورد، التاريخ، قائمة الأصناف، الكميات، والأسعار بدقة",
                                color = MikroTikCyan,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else if (!hasParsedInvoice && capturedBitmap == null) {
                    // Phase 1: Capture or Pick Image
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(MikroTikNavyLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = MikroTikCyan,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "صور فاتورة الشراء أو الأصول",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "التقط صورة واضحة لفاتورة المشتريات الورقية أو اخترها من المعرض ليقوم الذكاء الاصطناعي بتحويلها تلقائياً إلى أصناف وجدول جاهز للمراجعة والاعتماد.",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        // Capture Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { launchCamera() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("open_camera_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MikroTikPrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("تصوير بالكاميرا", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    galleryLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("open_gallery_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MikroTikCyan),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MikroTikCyan)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("اختيار من المعرض")
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // JSON Import Option (No Token / Offline)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clickable { showJsonImportDialog = true }
                                .testTag("open_json_import_phase1_card"),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2B48)),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, MikroTikCyan.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(MikroTikCyan.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Description,
                                            contentDescription = null,
                                            tint = MikroTikCyan,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "استيراد من ملف أو نص JSON 📄",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Color.White,
                                                fontFamily = CairoFontFamily
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(ProfitEmerald.copy(alpha = 0.25f))
                                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = "بدون توكن ⚡",
                                                    color = ProfitEmerald,
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = CairoFontFamily
                                                )
                                            }
                                        }
                                        Text(
                                            text = "استيراد فوري للأصناف والأسعار لتوفير رصيدك اليومي بدقة 100%",
                                            fontSize = 11.sp,
                                            color = Color(0xFF94A3B8),
                                            fontFamily = CairoFontFamily
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.FileUpload,
                                    contentDescription = null,
                                    tint = MikroTikCyan,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Manual entry button without taking a picture
                        OutlinedButton(
                            onClick = {
                                hasParsedInvoice = true
                                isAiExtracted = false
                                if (itemsList.isEmpty()) {
                                    itemsList.add(
                                        InvoiceItem(
                                            name = "",
                                            quantity = 1.0,
                                            unitPrice = 0.0,
                                            subtotal = 0.0,
                                            category = if (targetType == "ASSETS") "SERVERS" else "MAINTENANCE"
                                        )
                                    )
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCBD5E1)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = MikroTikCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إدخال بنود الفاتورة يدوياً بدون تصوير", fontSize = 12.sp)
                        }
                    }
                } else if (!hasParsedInvoice && capturedBitmap != null) {
                    // Phase 1.5: Image captured, but AI analysis failed or requires review
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MikroTikNavyLight)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Image(
                                    bitmap = capturedBitmap!!.asImageBitmap(),
                                    contentDescription = "صورة الفاتورة الملتقطة",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF381515)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "تنبيه قراءة الفاتورة بالذكاء الاصطناعي",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFCA5A5),
                                        fontSize = 13.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = aiErrorMessage ?: "تعذر قراءة نصوص الفاتورة بدقة. يرجى التأكد من وضوح الصورة وتفعيل مفتاح Gemini API.",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { processInvoiceBitmap(capturedBitmap!!) },
                                modifier = Modifier.fillMaxWidth().height(46.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MikroTikCyan),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("إعادة المحاولة بالفحص الذكي 🔄", color = Color.Black, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { showJsonImportDialog = true },
                                modifier = Modifier.fillMaxWidth().height(46.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ProfitEmerald),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("استيراد من ملف JSON بديل فوري (بدون توكن) 📄", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = CairoFontFamily)
                            }

                            OutlinedButton(
                                onClick = { showApiKeyDialog = true },
                                modifier = Modifier.fillMaxWidth().height(46.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = InvestmentGold),
                                border = androidx.compose.foundation.BorderStroke(1.dp, InvestmentGold),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.VpnKey, contentDescription = null, tint = InvestmentGold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("تعديل أو إدخال مفتاح Gemini API 🔑", fontWeight = FontWeight.Bold)
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(
                                    onClick = { launchCamera() },
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("إعادة التصوير", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        hasParsedInvoice = true
                                        isAiExtracted = false
                                        if (itemsList.isEmpty()) {
                                            itemsList.add(
                                                InvoiceItem(
                                                    name = "",
                                                    quantity = 1.0,
                                                    unitPrice = 0.0,
                                                    subtotal = 0.0,
                                                    category = if (targetType == "ASSETS") "SERVERS" else "MAINTENANCE"
                                                )
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MikroTikNavyLight),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = MikroTikCyan, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("إدخال يدوي", fontSize = 12.sp, color = Color.White)
                                }
                            }
                        }
                    }
                } else {
                    // Phase 2: Review, Edit, Add & Approve Structured Invoice
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Image Thumbnail & Retake Bar
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MikroTikNavyLight),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (capturedBitmap != null) {
                                            Image(
                                                bitmap = capturedBitmap!!.asImageBitmap(),
                                                contentDescription = "صورة الفاتورة",
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(RoundedCornerShape(8.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                        }
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = if (isAiExtracted) Icons.Default.CheckCircle else Icons.Default.Edit,
                                                    contentDescription = null,
                                                    tint = if (isAiExtracted) ProfitEmerald else MikroTikCyan,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isAiExtracted) "تم استخراج وقراءة بيانات الفاتورة الفعلية بالذكاء الاصطناعي (${itemsList.size} أصناف)" else "إدخال ومراجعة بيانات الفاتورة",
                                                    color = if (isAiExtracted) ProfitEmerald else MikroTikCyan,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                            }
                                            Text(
                                                text = "يمكنك مراجعة وتعديل أي صنف أو كمية أو سعر قبل الاعتماد",
                                                color = Color(0xFF94A3B8),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Row {
                                        IconButton(onClick = { launchCamera() }) {
                                            Icon(Icons.Default.CameraAlt, contentDescription = "إعادة التصوير", tint = MikroTikCyan)
                                        }
                                        IconButton(onClick = {
                                            galleryLauncher.launch(
                                                androidx.activity.result.PickVisualMediaRequest(
                                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                                )
                                            )
                                        }) {
                                            Icon(Icons.Default.PhotoLibrary, contentDescription = "اختيار صورة أخرى", tint = MikroTikCyan)
                                        }
                                    }
                                }
                            }
                        }

                        // Invoice General Info
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MikroTikDarkSurface),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "بيانات الفاتورة والمورد",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MikroTikCyan
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = supplierName,
                                        onValueChange = { supplierName = it },
                                        label = { Text("اسم المورد / المتجر") },
                                        modifier = Modifier.fillMaxWidth().testTag("supplier_name_input"),
                                        colors = darkTextFieldColors()
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = invoiceNumber,
                                            onValueChange = { invoiceNumber = it },
                                            label = { Text("رقم الفاتورة") },
                                            modifier = Modifier.weight(1f).testTag("invoice_number_input"),
                                            colors = darkTextFieldColors()
                                        )

                                        OutlinedTextField(
                                            value = invoiceDate,
                                            onValueChange = { invoiceDate = it },
                                            label = { Text("تاريخ الفاتورة") },
                                            modifier = Modifier.weight(1f).testTag("invoice_date_input"),
                                            colors = darkTextFieldColors()
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Currency Selector
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "عملة الفاتورة:",
                                            fontSize = 12.sp,
                                            color = Color(0xFFCBD5E1),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        listOf(
                                            "YER" to "ريال يمني",
                                            "SAR" to "ريال سعودي",
                                            "USD" to "دولار أمريكي"
                                        ).forEach { (code, label) ->
                                            val isSelected = currency == code
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) MikroTikCyan.copy(alpha = 0.2f) else MikroTikNavyLight)
                                                    .border(1.dp, if (isSelected) MikroTikCyan else Color(0xFF334155), RoundedCornerShape(8.dp))
                                                    .clickable { currency = code }
                                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = label,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) MikroTikCyan else Color(0xFF94A3B8)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Target Type selector: ASSETS vs EXPENSES
                                    Text(
                                        text = "نوع الفاتورة والوجهة:",
                                        fontSize = 12.sp,
                                        color = Color(0xFFCBD5E1),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        InvoiceTargetOption(
                                            title = "أصول رأسمالية (CAPEX)",
                                            subtitle = "راوترات، أبراج، بطاريات...",
                                            isSelected = targetType == "ASSETS",
                                            selectedColor = AssetPurple,
                                            onClick = {
                                                targetType = "ASSETS"
                                                saveAsAssets = true
                                            },
                                            modifier = Modifier.weight(1f)
                                        )

                                        InvoiceTargetOption(
                                            title = "مصروفات مشتريات (OPEX)",
                                            subtitle = "ديزل، صيانة، كابلات...",
                                            isSelected = targetType == "EXPENSES",
                                            selectedColor = PaymentRed,
                                            onClick = {
                                                targetType = "EXPENSES"
                                                saveAsAssets = false
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Checkboxes for saving
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = saveAsVoucher,
                                            onCheckedChange = { saveAsVoucher = it },
                                            colors = CheckboxDefaults.colors(checkedColor = ProfitEmerald)
                                        )
                                        Text(
                                            text = "تسجيل سند صرف مالي تلقائي في المحاسبة",
                                            color = Color.White,
                                            fontSize = 12.sp
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = saveAsAssets,
                                            onCheckedChange = { saveAsAssets = it },
                                            colors = CheckboxDefaults.colors(checkedColor = AssetPurple)
                                        )
                                        Text(
                                            text = "إضافة الأصناف إلى سجل الأصول الثابتة (CAPEX)",
                                            color = Color.White,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Items Section Header with "+ إضافة صنف"
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "الأصناف المستخرجة (${itemsList.size})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                }

                                Button(
                                    onClick = {
                                        itemsList.add(
                                            InvoiceItem(
                                                name = "صنف جديد",
                                                quantity = 1.0,
                                                unitPrice = 0.0,
                                                subtotal = 0.0,
                                                category = if (targetType == "ASSETS") "SERVERS" else "MAINTENANCE"
                                            )
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MikroTikPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(36.dp).testTag("add_item_to_invoice_button")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("إضافة صنف", fontSize = 12.sp)
                                }
                            }
                        }

                        // List of items
                        itemsIndexed(itemsList) { index, item ->
                            InvoiceItemCard(
                                item = item,
                                index = index,
                                currency = currency,
                                onUpdate = { updatedItem ->
                                    itemsList[index] = updatedItem
                                },
                                onDelete = {
                                    itemsList.removeAt(index)
                                }
                            )
                        }

                        // Grand Total Box
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MikroTikNavyLight),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MikroTikCyan.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "إجمالي الفاتورة المعتمد:",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "${formatMoney(totalCalculated)} ${com.example.util.CurrencyHelper.getCurrencySymbol(currency)}",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 18.sp,
                                            color = ProfitEmerald
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = notes,
                                        onValueChange = { notes = it },
                                        label = { Text("ملاحظات الفاتورة") },
                                        modifier = Modifier.fillMaxWidth().testTag("invoice_notes_input"),
                                        colors = darkTextFieldColors()
                                    )
                                }
                            }
                        }
                        // End of list save card
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = ProfitEmerald.copy(alpha = 0.12f)),
                                border = BorderStroke(1.dp, ProfitEmerald.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "هل انتهيت من تعديل ومراجعة الأصناف (${itemsList.size}) بإجمالي ${formatMoney(totalCalculated)} $currency؟",
                                        color = Color.White,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = CairoFontFamily
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = { performSaveInvoice() },
                                        enabled = !isSavingInvoice,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(46.dp)
                                            .testTag("scrollable_end_save_button"),
                                        colors = ButtonDefaults.buttonColors(containerColor = ProfitEmerald),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        if (isSavingInvoice) {
                                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("جاري الحفظ...", fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = CairoFontFamily, color = Color.White)
                                        } else {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "اعتماد وحفظ الفاتورة الآن 💾 ✓",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                fontFamily = CairoFontFamily,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Fixed Pinned Bottom Actions Bar: Always visible above system navigation buttons
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MikroTikDarkSurface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, ProfitEmerald.copy(alpha = 0.6f))
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
                                    .weight(0.7f)
                                    .height(46.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8))
                            ) {
                                Text("إلغاء", fontSize = 12.sp, fontFamily = CairoFontFamily)
                            }

                            Button(
                                onClick = { performSaveInvoice() },
                                enabled = !isSavingInvoice,
                                modifier = Modifier
                                    .weight(2f)
                                    .height(46.dp)
                                    .testTag("approve_and_save_invoice_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = ProfitEmerald),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                if (isSavingInvoice) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("جاري الحفظ...", fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = CairoFontFamily, color = Color.White)
                                } else {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("اعتماد وحفظ الفاتورة والأصناف 💾", fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = CairoFontFamily)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog for Entering / Managing Gemini API Key
    if (showApiKeyDialog) {
        var apiKeyInput by remember { mutableStateOf(viewModel.getAiApiKey()) }
        var isKeyVisible by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VpnKey, contentDescription = null, tint = InvestmentGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "مفتاح وكيل الذكاء الاصطناعي (Gemini API)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "لقراءة وتفريغ فواتير المشتريات الحقيقية واستخراج أسماء الأجهزة والكميات والأسعار الفعلية بدقة 100% ومنع أي بيانات وهمية، يرجى تفعيل مفتاح Google Gemini API.",
                        fontSize = 13.sp,
                        color = Color(0xFFCBD5E1),
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("مفتاح API Key") },
                        placeholder = { Text("AIzaSy...") },
                        singleLine = true,
                        visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                                    Icon(
                                        imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "إظهار أو إخفاء",
                                        tint = MikroTikCyan
                                    )
                                }
                                IconButton(onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val clip = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()?.trim()
                                    if (!clip.isNullOrBlank()) {
                                        apiKeyInput = clip
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = "لصق من الحافظة",
                                        tint = ProfitEmerald
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = darkTextFieldColors()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MikroTikNavyLight),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "💡 يمكنك الحصول على مفتاح مجاني وسريع من:",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = "aistudio.google.com/apikey",
                                fontSize = 12.sp,
                                color = MikroTikCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = apiKeyInput.trim()
                        if (trimmed.isNotBlank()) {
                            viewModel.setAiApiKey(trimmed)
                            Toast.makeText(context, "تم حفظ وتفعيل مفتاح الذكاء الاصطناعي بنجاح! ✓", Toast.LENGTH_SHORT).show()
                            showApiKeyDialog = false
                            if (capturedBitmap != null) {
                                processInvoiceBitmap(capturedBitmap!!)
                            }
                        } else {
                            Toast.makeText(context, "يرجى كتابة أو لصق المفتاح أولاً", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ProfitEmerald)
                ) {
                    Text("حفظ وتفعيل", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { apiKeyInput = com.example.data.ai.GeminiAiService.DEFAULT_CONFIGURED_KEY }) {
                        Text("استعادة المفتاح المعتمد", color = MikroTikCyan, fontSize = 11.sp)
                    }
                    if (capturedBitmap != null && !hasParsedInvoice) {
                        TextButton(onClick = {
                            showApiKeyDialog = false
                            hasParsedInvoice = true
                            isAiExtracted = false
                            if (itemsList.isEmpty()) {
                                itemsList.add(
                                    InvoiceItem(
                                        name = "",
                                        quantity = 1.0,
                                        unitPrice = 0.0,
                                        subtotal = 0.0,
                                        category = if (targetType == "ASSETS") "SERVERS" else "MAINTENANCE"
                                    )
                                )
                            }
                        }) {
                            Text("إدخال يدوي", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                    }
                    TextButton(onClick = { showApiKeyDialog = false }) {
                        Text("إلغاء", color = Color.White, fontSize = 11.sp)
                    }
                }
            },
            containerColor = MikroTikDarkSurface,
            textContentColor = Color.White
        )
    }

    if (showJsonImportDialog) {
        JsonInvoiceImportDialog(
            viewModel = viewModel,
            onDismissRequest = { showJsonImportDialog = false },
            onInvoiceImported = { importedData ->
                populateFieldsFromParsed(
                    parsed = importedData,
                    setSupplier = { supplierName = it },
                    setNumber = { invoiceNumber = it },
                    setDate = { if (it.isNotBlank()) invoiceDate = it },
                    setTarget = {
                        targetType = it
                        saveAsAssets = (it == "ASSETS")
                    },
                    setNotes = { notes = it },
                    setCurrency = { currency = it },
                    itemsList = itemsList
                )
                hasParsedInvoice = true
                isAiExtracted = true
                aiErrorMessage = null
                showJsonImportDialog = false
                Toast.makeText(context, "تم استيراد ${importedData.items.size} أصناف من ملف JSON بنجاح! ✓", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun InvoiceTargetOption(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) selectedColor.copy(alpha = 0.2f) else MikroTikNavyLight
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isSelected) selectedColor else Color.Transparent
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = if (isSelected) selectedColor else Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun InvoiceItemCard(
    item: InvoiceItem,
    index: Int,
    currency: String = "YER",
    onUpdate: (InvoiceItem) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember(item.id) { mutableStateOf(item.name) }
    var qtyStr by remember(item.id) { mutableStateOf(item.quantity.toString()) }
    var priceStr by remember(item.id) { mutableStateOf(item.unitPrice.toString()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MikroTikDarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "صنف #${index + 1}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MikroTikCyan
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp).testTag("delete_item_${index}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف الصنف",
                        tint = PaymentRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    onUpdate(item.copy(name = it))
                },
                label = { Text("اسم الصنف أو المعدة") },
                modifier = Modifier.fillMaxWidth().testTag("item_name_${index}"),
                colors = darkTextFieldColors()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = qtyStr,
                    onValueChange = {
                        qtyStr = it
                        val q = it.toDoubleOrNull() ?: 1.0
                        val p = priceStr.toDoubleOrNull() ?: item.unitPrice
                        onUpdate(item.copy(quantity = q, subtotal = q * p))
                    },
                    label = { Text("الكمية") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f).testTag("item_qty_${index}"),
                    colors = darkTextFieldColors()
                )

                OutlinedTextField(
                    value = priceStr,
                    onValueChange = {
                        priceStr = it
                        val p = it.toDoubleOrNull() ?: 0.0
                        val q = qtyStr.toDoubleOrNull() ?: item.quantity
                        onUpdate(item.copy(unitPrice = p, subtotal = q * p))
                    },
                    label = { Text("سعر الوحدة") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1.5f).testTag("item_price_${index}"),
                    colors = darkTextFieldColors()
                )

                Column(
                    modifier = Modifier.weight(1.5f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "الإجمالي:",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "${formatMoney(item.subtotal)} ${com.example.util.CurrencyHelper.getCurrencySymbol(currency)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = ProfitEmerald
                    )
                }
            }
        }
    }
}

@Composable
fun darkTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MikroTikCyan,
    unfocusedBorderColor = Color(0xFF334155),
    focusedLabelColor = MikroTikCyan,
    unfocusedLabelColor = Color(0xFF94A3B8),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    cursorColor = MikroTikCyan
)

private fun populateFieldsFromParsed(
    parsed: ParsedInvoiceData,
    setSupplier: (String) -> Unit,
    setNumber: (String) -> Unit,
    setDate: (String) -> Unit,
    setTarget: (String) -> Unit,
    setNotes: (String) -> Unit,
    setCurrency: (String) -> Unit,
    itemsList: MutableList<InvoiceItem>
) {
    if (parsed.supplierName.isNotBlank()) setSupplier(parsed.supplierName)
    if (parsed.invoiceNumber.isNotBlank()) setNumber(parsed.invoiceNumber)
    if (parsed.invoiceDate.isNotBlank()) setDate(parsed.invoiceDate)
    if (parsed.invoiceType.isNotBlank()) setTarget(parsed.invoiceType)
    if (parsed.currency.isNotBlank()) setCurrency(parsed.currency)
    if (parsed.notes.isNotBlank()) setNotes(parsed.notes)
    itemsList.clear()
    itemsList.addAll(parsed.items)
}

fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, _, _ ->
                decoder.isMutableRequired = true
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    } catch (e: Exception) {
        null
    }
}

private fun formatMoney(amount: Double): String {
    return String.format(Locale.US, "%,.0f", amount)
}
