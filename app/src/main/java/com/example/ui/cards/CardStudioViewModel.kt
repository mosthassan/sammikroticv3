package com.example.ui.cards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.cards.A4GridConfig
import com.example.data.cards.A4Orientation
import com.example.data.cards.CardDimensions
import com.example.data.cards.CardElementPosition
import com.example.data.cards.CardElementType
import com.example.data.cards.CardStudioDefaults
import com.example.data.cards.CardTemplatePreset
import com.example.data.cards.CardThemeStyle
import com.example.data.cards.CardThemesLibrary
import com.example.data.cards.ElementShape
import com.example.data.cards.QrFrameStyle
import com.example.data.cards.QrStyleConfig
import com.example.data.cards.VisualStyleType
import com.example.data.local.entity.CardPackageEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * حالة واجهة استوديو تصميم الكروت والطباعة A4
 */
data class CardStudioUiState(
    val activeTemplateId: String = "tpl_cyber_neon",
    val templateName: String = "قالب الإنترنت القياسي",
    val dimensions: CardDimensions = CardDimensions.STANDARD_63_19,
    val gridConfig: A4GridConfig = A4GridConfig(cardDimensions = CardDimensions.STANDARD_63_19),
    val selectedTheme: CardThemeStyle = CardThemesLibrary.EMERALD_PRO,
    val availableThemes: List<CardThemeStyle> = CardThemesLibrary.ALL_THEMES,
    val availableTemplates: List<CardTemplatePreset> = CardStudioDefaults.PREBUILT_TEMPLATES,
    val elementPositions: Map<CardElementType, CardElementPosition> = CardStudioDefaults.defaultPositions(),
    val selectedElement: CardElementType? = CardElementType.CODE_PIN_BOX,
    val qrStyleConfig: QrStyleConfig = QrStyleConfig(),
    
    // بيانات الباقة المحددة مسبقاً للربط الفوري (Package & Voucher Data Integration)
    val selectedPackage: CardPackageEntity? = null,
    val networkTitle: String = "شبكة طلقة نت للإنترنت",
    val supportPhone: String = "771234567",
    val loginDomain: String = "wifi.net",
    val samplePrice: Double = 1000.0,
    val sampleQuota: String = "4500 ميجا",
    val sampleValidity: String = "غير محدد",
    val sampleSpeed: String = "4M/2M",
    val sampleCode: String = "884028",
    val sampleBatchNo: String = "BATCH-2026-09",
    val sampleExpiryDate: String = "2026/12/31",
    val isDirty: Boolean = false
)

/**
 * ViewModel لإدارة كافة محركات وأعمال استوديو الكروت والطباعة A4:
 * 1. محرك شبكة A4 الديناميكي مع دعم التجاوز اليدوي (Dynamic A4 Grid Math Engine & Override)
 * 2. حقن بيانات الباقات والفواتير تلقائياً (Package & Voucher Data Integration)
 * 3. محرك الأشكال والأنماط البصرية المتقدمة (Advanced Styling & Shapes Engine)
 * 4. مستودع القوالب الجاهزة المسبقة (Pre-built Template Repository)
 */
class CardStudioViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CardStudioUiState())
    val uiState: StateFlow<CardStudioUiState> = _uiState.asStateFlow()

    val cardsPerPage: StateFlow<Int> = _uiState
        .map { it.gridConfig.cardsPerPage }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 24)

    // =========================================================
    // 1. Dynamic A4 Grid Math Engine & Override
    // =========================================================

    fun setCardDimensions(dimensions: CardDimensions) {
        _uiState.update { current ->
            val updatedGrid = current.gridConfig.copy(cardDimensions = dimensions)
            current.copy(
                dimensions = dimensions,
                gridConfig = updatedGrid,
                isDirty = true
            )
        }
    }

    fun setCustomDimensions(widthMm: Float, heightMm: Float) {
        val custom = CardDimensions.createCustom(widthMm, heightMm)
        setCardDimensions(custom)
    }

    fun setManualGridOverride(isOverride: Boolean, columns: Int? = null, rows: Int? = null) {
        _uiState.update { current ->
            val newGrid = current.gridConfig.copy(
                isManualOverride = isOverride,
                overrideColumns = columns ?: current.gridConfig.columns,
                overrideRows = rows ?: current.gridConfig.rows
            )
            current.copy(gridConfig = newGrid, isDirty = true)
        }
    }

    fun updateGridConfig(
        marginTopMm: Float = _uiState.value.gridConfig.marginTopMm,
        marginBottomMm: Float = _uiState.value.gridConfig.marginBottomMm,
        marginLeftMm: Float = _uiState.value.gridConfig.marginLeftMm,
        marginRightMm: Float = _uiState.value.gridConfig.marginRightMm,
        horizontalGapMm: Float = _uiState.value.gridConfig.horizontalGapMm,
        verticalGapMm: Float = _uiState.value.gridConfig.verticalGapMm,
        showCutLines: Boolean = _uiState.value.gridConfig.showCutLines,
        orientation: A4Orientation = _uiState.value.gridConfig.orientation
    ) {
        _uiState.update { current ->
            val newGrid = current.gridConfig.copy(
                marginTopMm = marginTopMm.coerceAtLeast(0.0f),
                marginBottomMm = marginBottomMm.coerceAtLeast(0.0f),
                marginLeftMm = marginLeftMm.coerceAtLeast(0.0f),
                marginRightMm = marginRightMm.coerceAtLeast(0.0f),
                horizontalGapMm = horizontalGapMm.coerceAtLeast(0.0f),
                verticalGapMm = verticalGapMm.coerceAtLeast(0.0f),
                showCutLines = showCutLines,
                orientation = orientation
            )
            current.copy(gridConfig = newGrid, isDirty = true)
        }
    }

    fun setA4Orientation(orientation: A4Orientation) {
        updateGridConfig(orientation = orientation)
    }

    // =========================================================
    // 2. Package & Voucher Data Integration
    // =========================================================

    /**
     * ربط وحقن بيانات باقة معينة تلقائياً في عناصر الكرت والكانفاس
     */
    fun selectCardPackage(pkg: CardPackageEntity) {
        _uiState.update { current ->
            val validityStr = if (pkg.validityHours >= 24) "${pkg.validityHours / 24} يوم" else "${pkg.validityHours} ساعة"
            val priceVal = pkg.retailPrice.toDouble()
            val priceTheme = when {
                priceVal >= 4000 -> CardThemesLibrary.ROYAL_GOLD
                priceVal >= 1000 -> CardThemesLibrary.EMERALD_PRO
                priceVal >= 500 -> CardThemesLibrary.CYBER_NEON
                else -> CardThemesLibrary.TURBO_BLAZE
            }

            current.copy(
                selectedPackage = pkg,
                samplePrice = priceVal,
                sampleQuota = pkg.formattedQuota,
                sampleValidity = validityStr,
                sampleSpeed = pkg.speedLimit,
                selectedTheme = priceTheme,
                isDirty = true
            )
        }
    }

    fun updateSampleData(
        networkTitle: String = _uiState.value.networkTitle,
        supportPhone: String = _uiState.value.supportPhone,
        loginDomain: String = _uiState.value.loginDomain,
        samplePrice: Double = _uiState.value.samplePrice,
        sampleQuota: String = _uiState.value.sampleQuota,
        sampleValidity: String = _uiState.value.sampleValidity
    ) {
        _uiState.update {
            it.copy(
                networkTitle = networkTitle,
                supportPhone = supportPhone,
                loginDomain = loginDomain,
                samplePrice = samplePrice,
                sampleQuota = sampleQuota,
                sampleValidity = sampleValidity,
                isDirty = true
            )
        }
    }

    // =========================================================
    // 3. Advanced Styling, Shapes & QR Code Engine
    // =========================================================

    fun setElementShape(elementType: CardElementType, shape: ElementShape) {
        _uiState.update { current ->
            val positions = current.elementPositions.toMutableMap()
            val pos = positions[elementType] ?: CardElementPosition(elementType = elementType)
            positions[elementType] = pos.copy(shape = shape)
            current.copy(elementPositions = positions, isDirty = true)
        }
    }

    fun setElementVisualStyle(elementType: CardElementType, visualStyle: VisualStyleType) {
        _uiState.update { current ->
            val positions = current.elementPositions.toMutableMap()
            val pos = positions[elementType] ?: CardElementPosition(elementType = elementType)
            positions[elementType] = pos.copy(visualStyle = visualStyle)
            current.copy(elementPositions = positions, isDirty = true)
        }
    }

    fun setElementGlowColor(elementType: CardElementType, glowColorHex: String) {
        _uiState.update { current ->
            val positions = current.elementPositions.toMutableMap()
            val pos = positions[elementType] ?: CardElementPosition(elementType = elementType)
            positions[elementType] = pos.copy(glowColorHex = glowColorHex)
            current.copy(elementPositions = positions, isDirty = true)
        }
    }

    fun updateQrStyleConfig(
        frameStyle: QrFrameStyle = _uiState.value.qrStyleConfig.frameStyle,
        darkColorHex: String = _uiState.value.qrStyleConfig.darkColorHex,
        lightColorHex: String = _uiState.value.qrStyleConfig.lightColorHex,
        cornerRadiusDp: Int = _uiState.value.qrStyleConfig.cornerRadiusDp
    ) {
        _uiState.update { current ->
            val updatedQr = current.qrStyleConfig.copy(
                frameStyle = frameStyle,
                darkColorHex = darkColorHex,
                lightColorHex = lightColorHex,
                cornerRadiusDp = cornerRadiusDp
            )
            current.copy(qrStyleConfig = updatedQr, isDirty = true)
        }
    }

    // =========================================================
    // 4. Pre-built Template Repository Integration
    // =========================================================

    /**
     * تطبيق قالب مجهز مسبقاً بشكل فوري على الكانفاس بجميع أبعاده وألوانه ومواضعه
     */
    fun applyTemplatePreset(preset: CardTemplatePreset) {
        _uiState.update { current ->
            current.copy(
                activeTemplateId = preset.id,
                templateName = preset.nameArabic,
                dimensions = preset.dimensions,
                gridConfig = A4GridConfig(cardDimensions = preset.dimensions),
                selectedTheme = preset.theme,
                elementPositions = preset.positions,
                qrStyleConfig = preset.qrConfig,
                isDirty = false
            )
        }
    }

    fun selectTheme(theme: CardThemeStyle) {
        _uiState.update { current ->
            current.copy(selectedTheme = theme, isDirty = true)
        }
    }

    fun updateCustomThemeColors(
        backgroundStart: androidx.compose.ui.graphics.Color = _uiState.value.selectedTheme.backgroundStart,
        backgroundEnd: androidx.compose.ui.graphics.Color = _uiState.value.selectedTheme.backgroundEnd,
        textColor: androidx.compose.ui.graphics.Color = _uiState.value.selectedTheme.textColor,
        accentColor: androidx.compose.ui.graphics.Color = _uiState.value.selectedTheme.accentColor,
        badgeBgColor: androidx.compose.ui.graphics.Color = _uiState.value.selectedTheme.badgeBgColor,
        codeBoxBgColor: androidx.compose.ui.graphics.Color = _uiState.value.selectedTheme.codeBoxBgColor,
        codeBoxBorderColor: androidx.compose.ui.graphics.Color = _uiState.value.selectedTheme.codeBoxBorderColor
    ) {
        _uiState.update { current ->
            val updatedTheme = current.selectedTheme.copy(
                backgroundStart = backgroundStart,
                backgroundEnd = backgroundEnd,
                textColor = textColor,
                accentColor = accentColor,
                badgeBgColor = badgeBgColor,
                codeBoxBgColor = codeBoxBgColor,
                codeBoxBorderColor = codeBoxBorderColor
            )
            current.copy(selectedTheme = updatedTheme, isDirty = true)
        }
    }

    // =========================================================
    // 5. Element Drag & Layout Manipulation
    // =========================================================

    fun selectElement(elementType: CardElementType?) {
        _uiState.update { it.copy(selectedElement = elementType) }
    }

    fun onElementDragged(elementType: CardElementType, deltaXPercent: Float, deltaYPercent: Float) {
        _uiState.update { current ->
            val positions = current.elementPositions.toMutableMap()
            val pos = positions[elementType] ?: CardElementPosition(elementType = elementType)

            val newX = (pos.xPercent + deltaXPercent).coerceIn(0.0f, (100.0f - pos.widthPercent).coerceAtLeast(0.0f))
            val newY = (pos.yPercent + deltaYPercent).coerceIn(0.0f, (100.0f - pos.heightPercent).coerceAtLeast(0.0f))

            positions[elementType] = pos.copy(xPercent = newX, yPercent = newY)
            current.copy(elementPositions = positions, isDirty = true)
        }
    }

    fun setElementPositionExact(elementType: CardElementType, xPercent: Float, yPercent: Float) {
        _uiState.update { current ->
            val positions = current.elementPositions.toMutableMap()
            val pos = positions[elementType] ?: CardElementPosition(elementType = elementType)
            val newX = xPercent.coerceIn(0.0f, (100.0f - pos.widthPercent).coerceAtLeast(0.0f))
            val newY = yPercent.coerceIn(0.0f, (100.0f - pos.heightPercent).coerceAtLeast(0.0f))
            positions[elementType] = pos.copy(xPercent = newX, yPercent = newY)
            current.copy(elementPositions = positions, isDirty = true)
        }
    }

    fun setElementFontSize(elementType: CardElementType, fontSizeSp: Float) {
        _uiState.update { current ->
            val positions = current.elementPositions.toMutableMap()
            val pos = positions[elementType] ?: CardElementPosition(elementType = elementType)
            positions[elementType] = pos.copy(fontSizeSp = fontSizeSp.coerceIn(6.0f, 36.0f))
            current.copy(elementPositions = positions, isDirty = true)
        }
    }

    fun setElementVisibility(elementType: CardElementType, isVisible: Boolean) {
        _uiState.update { current ->
            val positions = current.elementPositions.toMutableMap()
            val pos = positions[elementType] ?: CardElementPosition(elementType = elementType)
            positions[elementType] = pos.copy(isVisible = isVisible)
            current.copy(elementPositions = positions, isDirty = true)
        }
    }

    fun resetToDefaultLayout() {
        _uiState.update { current ->
            current.copy(
                elementPositions = CardStudioDefaults.defaultPositions(),
                selectedTheme = CardThemesLibrary.EMERALD_PRO,
                dimensions = CardDimensions.STANDARD_63_19,
                gridConfig = A4GridConfig(cardDimensions = CardDimensions.STANDARD_63_19),
                qrStyleConfig = QrStyleConfig(),
                isDirty = false
            )
        }
    }

    fun calculatePreviewScaleFactor(
        containerWidthPx: Float,
        containerHeightPx: Float,
        dpi: Int = 300
    ): Float {
        val dims = _uiState.value.dimensions
        val cardWidthPx = dims.toPixelsWidth(dpi)
        val cardHeightPx = dims.toPixelsHeight(dpi)

        if (cardWidthPx <= 0f || cardHeightPx <= 0f || containerWidthPx <= 0f || containerHeightPx <= 0f) {
            return 1.0f
        }

        val scaleX = containerWidthPx / cardWidthPx
        val scaleY = containerHeightPx / cardHeightPx

        return kotlin.math.min(scaleX, scaleY)
    }
}
