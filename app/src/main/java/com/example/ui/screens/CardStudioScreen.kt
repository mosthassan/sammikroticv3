package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.cards.CardDimensions
import com.example.data.cards.CardElementType
import com.example.data.cards.CardPdfPrintManager
import com.example.data.cards.CardStudioDefaults
import com.example.data.cards.CardTemplateConfig
import com.example.data.cards.CardThemeStyle
import com.example.data.cards.CardThemesLibrary
import com.example.data.cards.ElementShape
import com.example.data.cards.QrFrameStyle
import com.example.data.cards.VisualStyleType
import com.example.data.local.entity.CardEntity
import com.example.data.local.entity.CardPackageEntity
import com.example.ui.MainViewModel
import com.example.ui.cards.CardStudioUiState
import com.example.ui.cards.CardStudioViewModel
import com.example.ui.theme.CairoFontFamily
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberDarkCardElevated
import com.example.ui.theme.CyberDarkSurface
import com.example.ui.theme.MikroTikCyan
import com.example.ui.theme.MikroTikPrimary
import com.example.ui.theme.ProfitEmerald
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import kotlinx.coroutines.launch

enum class StudioToolType(val titleArabic: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    TEMPLATES("القوالب والباقات", Icons.Default.Palette),
    COLORS("الألوان والأنماط", Icons.Default.AutoAwesome),
    DIMENSIONS("الأبعاد والشبكة", Icons.Default.AspectRatio),
    LAYERS("العناصر والطبقات", Icons.Default.Layers),
    INSPECTOR("عنصر محدد", Icons.Default.Tune)
}

/**
 * شاشة استوديو تصميم الكروت والطباعة A4 (Figma / Canva Mobile Architecture 2026)
 * متطابقة مع كافة محركات الربط المتقدمة:
 * 1. محرك حسابات الشبكة A4 وتجاوز السعة يدوياً
 * 2. حقن بيانات الباقات والفواتير تلقائياً
 * 3. الأشكال المتقدمة (Pill, Capsule, Glassmorphism, Neon Glow)
 * 4. مستودع القوالب الجاهزة
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardStudioScreen(
    viewModel: MainViewModel,
    cardStudioViewModel: CardStudioViewModel = viewModel(),
    onNavigateToInventory: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val uiState by cardStudioViewModel.uiState.collectAsState()
    val cardsPerPage by cardStudioViewModel.cardsPerPage.collectAsState()
    val allCards by viewModel.cards.collectAsState()
    val cardPackages by viewModel.cardPackages.collectAsState()

    var showA4SheetPreview by remember { mutableStateOf(false) }
    var activeTool by remember { mutableStateOf<StudioToolType?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun openToolSheet(tool: StudioToolType) {
        activeTool = tool
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF070E1A), Color(0xFF0A1526), Color(0xFF040812))
                )
            )
            .testTag("card_studio_screen")
    ) {
        // ================= 1. COMPACT TOP ACTION BAR =================
        Surface(
            color = Color(0xFF0A1526).copy(alpha = 0.95f),
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "رجوع",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = uiState.networkTitle,
                            color = Color.White,
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "${uiState.templateName} • ${if (showA4SheetPreview) "معاينة A4 ($cardsPerPage كرت)" else "معاينة تفاعلية"}",
                            color = MikroTikCyan,
                            fontFamily = CairoFontFamily,
                            fontSize = 10.5.sp
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            viewModel.consultAi("اقترح لي ثيم وتصميم نيون جذاب لكروت الإنترنت فئة 1000 ريال")
                            Toast.makeText(context, "جاري استشارة المساعد الذكي AI لتوليد الألوان...", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF7C3AED).copy(alpha = 0.2f))
                            .border(1.dp, Color(0xFF7C3AED), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = "توليد ذكي AI",
                            tint = Color(0xFFA78BFA),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Button(
                        onClick = {
                            val config = CardTemplateConfig(
                                templateId = uiState.selectedTheme.id,
                                theme = uiState.selectedTheme,
                                networkTitle = uiState.networkTitle,
                                supportPhone = uiState.supportPhone,
                                loginDomain = uiState.loginDomain,
                                showPriceBadge = true,
                                cornerRadiusDp = 8
                            )
                            val sampleCards = if (allCards.isNotEmpty()) allCards.take(cardsPerPage) else {
                                List(cardsPerPage) { idx ->
                                    CardEntity(
                                        id = (idx + 1).toLong(),
                                        batchId = 1,
                                        username = "88402${idx % 10}",
                                        password = "88402${idx % 10}",
                                        categoryName = uiState.selectedPackage?.name ?: "باقة 1000 ريال",
                                        retailPrice = uiState.samplePrice,
                                        wholesalePrice = uiState.samplePrice * 0.9
                                    )
                                }
                            }
                            CardPdfPrintManager.printCardsSheet(context, sampleCards, config)
                            Toast.makeText(context, "جاري تصدير ملف PDF لطباعة $cardsPerPage كرت...", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MikroTikPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("btn_export_pdf_studio")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "تصدير PDF ($cardsPerPage)",
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // ================= 2. FOCUS CANVAS AREA (TOP 65-70%) =================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 60.dp),
            contentAlignment = Alignment.Center
        ) {
            if (showA4SheetPreview) {
                A4SheetPreviewComposable(
                    uiState = uiState,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                InteractiveCardCanvas(
                    uiState = uiState,
                    onElementSelected = { elementType ->
                        cardStudioViewModel.selectElement(elementType)
                        openToolSheet(StudioToolType.INSPECTOR)
                    },
                    onElementDragged = { elementType, deltaX, deltaY ->
                        cardStudioViewModel.onElementDragged(elementType, deltaX, deltaY)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.85f),
                border = BorderStroke(1.dp, CyberBorder),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .clickable { showA4SheetPreview = !showA4SheetPreview }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (showA4SheetPreview) Icons.Default.CreditCard else Icons.Default.GridOn,
                        contentDescription = null,
                        tint = MikroTikCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (showA4SheetPreview) "تبديل لكارد مفرد" else "صفحة A4 ($cardsPerPage كرت)",
                        color = Color.White,
                        fontFamily = CairoFontFamily,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ================= 3. FLOATING CONTROL BAR =================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.92f),
                border = BorderStroke(1.5.dp, CyberBorder),
                shadowElevation = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StudioToolPill(
                        tool = StudioToolType.TEMPLATES,
                        isSelected = activeTool == StudioToolType.TEMPLATES,
                        onClick = { openToolSheet(StudioToolType.TEMPLATES) }
                    )

                    StudioToolPill(
                        tool = StudioToolType.COLORS,
                        isSelected = activeTool == StudioToolType.COLORS,
                        onClick = { openToolSheet(StudioToolType.COLORS) }
                    )

                    StudioToolPill(
                        tool = StudioToolType.DIMENSIONS,
                        isSelected = activeTool == StudioToolType.DIMENSIONS,
                        onClick = { openToolSheet(StudioToolType.DIMENSIONS) }
                    )

                    StudioToolPill(
                        tool = StudioToolType.LAYERS,
                        isSelected = activeTool == StudioToolType.LAYERS,
                        onClick = { openToolSheet(StudioToolType.LAYERS) }
                    )

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (showA4SheetPreview) MikroTikCyan.copy(alpha = 0.25f) else Color.Transparent)
                            .border(1.dp, if (showA4SheetPreview) MikroTikCyan else Color.Transparent, CircleShape)
                            .clickable { showA4SheetPreview = !showA4SheetPreview }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (showA4SheetPreview) Icons.Default.CreditCard else Icons.Default.GridOn,
                            contentDescription = "A4 Preview",
                            tint = if (showA4SheetPreview) MikroTikCyan else TextSecondaryDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    // ================= 4. DYNAMIC CONTEXTUAL BOTTOM SHEET =================
    if (activeTool != null) {
        ModalBottomSheet(
            onDismissRequest = { activeTool = null },
            sheetState = sheetState,
            containerColor = CyberDarkSurface,
            scrimColor = Color.Black.copy(alpha = 0.5f),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = activeTool!!.icon,
                            contentDescription = null,
                            tint = MikroTikCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = activeTool!!.titleArabic,
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }

                    IconButton(
                        onClick = {
                            coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                                activeTool = null
                            }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = TextSecondaryDark)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                when (activeTool) {
                    StudioToolType.TEMPLATES -> {
                        TemplatesSheetContent(
                            uiState = uiState,
                            cardPackages = cardPackages,
                            cardStudioViewModel = cardStudioViewModel
                        )
                    }
                    StudioToolType.COLORS -> {
                        ColorsAndStyleSheetContent(
                            uiState = uiState,
                            cardStudioViewModel = cardStudioViewModel
                        )
                    }
                    StudioToolType.DIMENSIONS -> {
                        DimensionsAndGridSheetContent(
                            uiState = uiState,
                            cardStudioViewModel = cardStudioViewModel
                        )
                    }
                    StudioToolType.LAYERS -> {
                        LayersSheetContent(
                            uiState = uiState,
                            cardStudioViewModel = cardStudioViewModel,
                            onSelectElementToInspect = {
                                cardStudioViewModel.selectElement(it)
                                activeTool = StudioToolType.INSPECTOR
                            }
                        )
                    }
                    StudioToolType.INSPECTOR -> {
                        ElementInspectorSheetContent(
                            uiState = uiState,
                            cardStudioViewModel = cardStudioViewModel
                        )
                    }
                    null -> {}
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun StudioToolPill(
    tool: StudioToolType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) MikroTikPrimary else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = tool.icon,
                contentDescription = tool.titleArabic,
                tint = if (isSelected) Color.White else TextSecondaryDark,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = tool.titleArabic,
                fontFamily = CairoFontFamily,
                fontSize = 9.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color.White else TextSecondaryDark
            )
        }
    }
}

@Composable
private fun TemplatesSheetContent(
    uiState: CardStudioUiState,
    cardPackages: List<CardPackageEntity>,
    cardStudioViewModel: CardStudioViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Pre-built Template Presets Repository
        Text(
            text = "مستودع القوالب المجهزة مسبقاً (Pre-built Templates):",
            fontFamily = CairoFontFamily,
            fontSize = 12.sp,
            color = TextSecondaryDark
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(uiState.availableTemplates) { preset ->
                val isSel = preset.id == uiState.activeTemplateId
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = preset.theme.backgroundStart,
                    border = BorderStroke(if (isSel) 2.dp else 1.dp, if (isSel) MikroTikCyan else CyberBorder),
                    onClick = { cardStudioViewModel.applyTemplatePreset(preset) },
                    modifier = Modifier
                        .width(150.dp)
                        .height(80.dp)
                ) {
                    Box(modifier = Modifier.padding(8.dp)) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = preset.nameArabic,
                                color = preset.theme.textColor,
                                fontFamily = CairoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            if (isSel) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MikroTikCyan, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // Package Data Integration Picker
        if (cardPackages.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "ربط وحقن بيانات باقة مسجلة فليكس:",
                fontFamily = CairoFontFamily,
                fontSize = 12.sp,
                color = TextSecondaryDark
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(cardPackages) { pkg ->
                    val isSel = uiState.selectedPackage?.id == pkg.id
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSel) MikroTikPrimary else CyberDarkCardElevated,
                        border = BorderStroke(1.dp, if (isSel) MikroTikCyan else CyberBorder),
                        onClick = { cardStudioViewModel.selectCardPackage(pkg) }
                    ) {
                        Text(
                            text = "${pkg.name} (${pkg.retailPrice.toInt()} ر.ي)",
                            fontFamily = CairoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSel) Color.White else TextSecondaryDark,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedButton(
            onClick = { cardStudioViewModel.resetToDefaultLayout() },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("استعادة التنسيق الافتراضي الأصلي", fontFamily = CairoFontFamily)
        }
    }
}

@Composable
private fun ColorsAndStyleSheetContent(
    uiState: CardStudioUiState,
    cardStudioViewModel: CardStudioViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("ثيمات وألوان الكرت الجاهزة:", fontFamily = CairoFontFamily, fontSize = 12.sp, color = TextSecondaryDark)

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(uiState.availableThemes) { theme ->
                val isSel = theme.id == uiState.selectedTheme.id
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = theme.backgroundStart,
                    border = BorderStroke(if (isSel) 2.dp else 1.dp, if (isSel) MikroTikCyan else CyberBorder),
                    onClick = { cardStudioViewModel.selectTheme(theme) },
                    modifier = Modifier.width(110.dp).height(50.dp)
                ) {
                    Box(modifier = Modifier.padding(6.dp), contentAlignment = Alignment.Center) {
                        Text(theme.nameArabic, color = theme.textColor, fontFamily = CairoFontFamily, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // QR Code Frame Style Control
        Spacer(modifier = Modifier.height(4.dp))
        Text("إطار ونمط الـ QR Code:", fontFamily = CairoFontFamily, fontSize = 12.sp, color = TextSecondaryDark)

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(QrFrameStyle.values()) { style ->
                val isSel = uiState.qrStyleConfig.frameStyle == style
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSel) MikroTikPrimary else CyberDarkCardElevated,
                    border = BorderStroke(1.dp, if (isSel) MikroTikCyan else CyberBorder),
                    onClick = { cardStudioViewModel.updateQrStyleConfig(frameStyle = style) }
                ) {
                    Text(
                        text = style.displayNameArabic,
                        fontFamily = CairoFontFamily,
                        fontSize = 10.5.sp,
                        color = if (isSel) Color.White else TextSecondaryDark,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DimensionsAndGridSheetContent(
    uiState: CardStudioUiState,
    cardStudioViewModel: CardStudioViewModel
) {
    var widthInput by remember(uiState.dimensions) { mutableStateOf(uiState.dimensions.widthMm.toInt().toString()) }
    var heightInput by remember(uiState.dimensions) { mutableStateOf(uiState.dimensions.heightMm.toInt().toString()) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("مقاسات الكرت القياسية والمخصصة (mm):", fontFamily = CairoFontFamily, fontSize = 12.sp, color = TextSecondaryDark)

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(CardDimensions.PRESET_DIMENSIONS) { preset ->
                val isSel = uiState.dimensions.widthMm == preset.widthMm && uiState.dimensions.heightMm == preset.heightMm
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSel) MikroTikPrimary else CyberDarkCardElevated,
                    border = BorderStroke(1.dp, if (isSel) MikroTikCyan else CyberBorder),
                    onClick = { cardStudioViewModel.setCardDimensions(preset) }
                ) {
                    Text(
                        text = preset.label,
                        fontFamily = CairoFontFamily,
                        fontSize = 10.5.sp,
                        color = if (isSel) Color.White else TextSecondaryDark,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // A4 Grid Math & Manual Override Controls
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = CyberDarkCardElevated,
            border = BorderStroke(1.dp, CyberBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("تجاوز سعة الورقة يدوياً (Grid Override):", fontFamily = CairoFontFamily, fontSize = 11.5.sp, color = Color.White)
                    Switch(
                        checked = uiState.gridConfig.isManualOverride,
                        onCheckedChange = { cardStudioViewModel.setManualGridOverride(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = MikroTikCyan)
                    )
                }

                if (uiState.gridConfig.isManualOverride) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { cardStudioViewModel.setManualGridOverride(true, columns = 3, rows = 8) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("فرض 24 كرت (3×8)", fontFamily = CairoFontFamily, fontSize = 10.5.sp)
                        }

                        OutlinedButton(
                            onClick = { cardStudioViewModel.setManualGridOverride(true, columns = 3, rows = 14) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("فرض 42 كرت (3×14)", fontFamily = CairoFontFamily, fontSize = 10.5.sp)
                        }
                    }
                }

                Text(
                    text = "سعة الورقة: ${uiState.gridConfig.columns} أعمدة × ${uiState.gridConfig.rows} صفوف = ${uiState.gridConfig.cardsPerPage} كرت/صفحة A4",
                    fontFamily = CairoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    color = MikroTikCyan
                )
            }
        }
    }
}

@Composable
private fun LayersSheetContent(
    uiState: CardStudioUiState,
    cardStudioViewModel: CardStudioViewModel,
    onSelectElementToInspect: (CardElementType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("طبقات وعناصر الكرت (انقر لتخصيص الشكل والخط):", fontFamily = CairoFontFamily, fontSize = 12.sp, color = TextSecondaryDark)

        CardElementType.values().forEach { elementType ->
            val pos = uiState.elementPositions[elementType]
            val isVisible = pos?.isVisible == true

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = CyberDarkCardElevated,
                border = BorderStroke(1.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectElementToInspect(elementType) }
                    ) {
                        Icon(
                            imageVector = if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = if (isVisible) MikroTikCyan else TextSecondaryDark,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = elementType.displayNameArabic,
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = Color.White
                        )
                    }

                    Switch(
                        checked = isVisible,
                        onCheckedChange = { cardStudioViewModel.setElementVisibility(elementType, it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = MikroTikCyan)
                    )
                }
            }
        }
    }
}

@Composable
private fun ElementInspectorSheetContent(
    uiState: CardStudioUiState,
    cardStudioViewModel: CardStudioViewModel
) {
    val selected = uiState.selectedElement ?: CardElementType.CODE_PIN_BOX
    val pos = uiState.elementPositions[selected]

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MikroTikPrimary.copy(alpha = 0.2f),
            border = BorderStroke(1.dp, MikroTikCyan),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = MikroTikCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("تخصيص عنصر: ${selected.displayNameArabic}", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Color.White)
            }
        }

        if (pos != null) {
            // Element Shape Selector
            Column {
                Text("شكل وقَصّة العنصر (Element Shape):", fontFamily = CairoFontFamily, fontSize = 11.sp, color = TextSecondaryDark)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ElementShape.values()) { shape ->
                        val isSel = pos.shape == shape
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSel) MikroTikPrimary else CyberDarkCardElevated,
                            border = BorderStroke(1.dp, if (isSel) MikroTikCyan else CyberBorder),
                            onClick = { cardStudioViewModel.setElementShape(selected, shape) }
                        ) {
                            Text(
                                text = shape.displayNameArabic,
                                fontFamily = CairoFontFamily,
                                fontSize = 10.sp,
                                color = if (isSel) Color.White else TextSecondaryDark,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // Visual Style Selector (Solid, Outline, Glassmorphism, Neon Glow)
            Column {
                Text("النمط البصري (Visual Style):", fontFamily = CairoFontFamily, fontSize = 11.sp, color = TextSecondaryDark)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(VisualStyleType.values()) { style ->
                        val isSel = pos.visualStyle == style
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSel) MikroTikPrimary else CyberDarkCardElevated,
                            border = BorderStroke(1.dp, if (isSel) MikroTikCyan else CyberBorder),
                            onClick = { cardStudioViewModel.setElementVisualStyle(selected, style) }
                        ) {
                            Text(
                                text = style.displayNameArabic,
                                fontFamily = CairoFontFamily,
                                fontSize = 10.sp,
                                color = if (isSel) Color.White else TextSecondaryDark,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // Font Size Slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("حجم الخط (FontSize):", fontFamily = CairoFontFamily, fontSize = 11.sp, color = TextSecondaryDark)
                    Text("${pos.fontSizeSp.toInt()} sp", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = MikroTikCyan)
                }
                Slider(
                    value = pos.fontSizeSp,
                    onValueChange = { cardStudioViewModel.setElementFontSize(selected, it) },
                    valueRange = 6f..28f,
                    colors = SliderDefaults.colors(thumbColor = MikroTikCyan, activeTrackColor = MikroTikCyan)
                )
            }
        }
    }
}
