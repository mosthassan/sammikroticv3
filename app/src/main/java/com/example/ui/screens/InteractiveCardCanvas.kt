package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.cards.CardElementType
import com.example.data.cards.QrCodeGenerator
import com.example.ui.cards.CardStudioUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * مكون الكانفاس التفاعلي لعرض وسحب وتنسيق عناصر كرت الإنترنت
 * - يتأقلم ديناميكياً مع أبعاد الكرت (CardDimensions)
 * - يدعم إيماءات اللمس والسحب (Touch & Drag Gestures) لتحريك العناصر بالنسبة المئوية (0-100%)
 * - يُظهر مكبر وهالة مضيئة عند تحديد أو سحب أي عنصر
 */
@Composable
fun InteractiveCardCanvas(
    uiState: CardStudioUiState,
    onElementSelected: (CardElementType) -> Unit,
    onElementDragged: (CardElementType, deltaXPercent: Float, deltaYPercent: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = uiState.selectedTheme
    val dims = uiState.dimensions
    val density = LocalDensity.current

    // QR Code Bitmap generation
    var qrBitmap by remember(uiState.loginDomain, uiState.sampleCode) {
        mutableStateOf<Bitmap?>(null)
    }

    LaunchedEffect(uiState.loginDomain, uiState.sampleCode) {
        withContext(Dispatchers.Default) {
            val url = "http://${uiState.loginDomain}/login?username=${uiState.sampleCode}"
            qrBitmap = QrCodeGenerator.generateQrBitmap(url, sizePx = 200, darkColor = android.graphics.Color.BLACK)
        }
    }

    // Canvas Container Box
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 12.dp,
            color = Color.Transparent,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(dims.aspectRatio)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    width = 1.5.dp,
                    color = theme.borderColor.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(theme.backgroundStart, theme.backgroundEnd)
                        )
                    )
            ) {
                val canvasWidthPx = constraints.maxWidth.toFloat()
                val canvasHeightPx = constraints.maxHeight.toFloat()

                // Draw each card element based on elementPositions
                CardElementType.values().forEach { elementType ->
                    val pos = uiState.elementPositions[elementType] ?: return@forEach
                    if (!pos.isVisible) return@forEach

                    val isSelected = uiState.selectedElement == elementType
                    var isDraggingThis by remember { mutableStateOf(false) }

                    val scaleFactor by animateFloatAsState(
                        targetValue = if (isDraggingThis) 1.08f else (if (isSelected) 1.03f else 1.0f),
                        label = "scaleFactor"
                    )

                    // Calculate pixel offsets and dimensions
                    val leftPx = (pos.clampedX() / 100.0f) * canvasWidthPx
                    val topPx = (pos.clampedY() / 100.0f) * canvasHeightPx
                    val widthPx = (pos.widthPercent / 100.0f) * canvasWidthPx
                    val heightPx = (pos.heightPercent / 100.0f) * canvasHeightPx

                    val leftDp = with(density) { leftPx.toDp() }
                    val topDp = with(density) { topPx.toDp() }
                    val widthDp = with(density) { widthPx.toDp() }
                    val heightDp = with(density) { heightPx.toDp() }

                    // Element Box container with touch & drag handlers
                    Box(
                        modifier = Modifier
                            .offset(x = leftDp, y = topDp)
                            .size(width = widthDp, height = heightDp)
                            .scale(scaleFactor)
                            .pointerInput(elementType) {
                                detectTapGestures {
                                    onElementSelected(elementType)
                                }
                            }
                            .pointerInput(elementType, canvasWidthPx, canvasHeightPx) {
                                detectDragGestures(
                                    onDragStart = {
                                        onElementSelected(elementType)
                                        isDraggingThis = true
                                    },
                                    onDragEnd = { isDraggingThis = false },
                                    onDragCancel = { isDraggingThis = false },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        if (canvasWidthPx > 0f && canvasHeightPx > 0f) {
                                            val deltaXPercent = (dragAmount.x / canvasWidthPx) * 100.0f
                                            val deltaYPercent = (dragAmount.y / canvasHeightPx) * 100.0f
                                            onElementDragged(elementType, deltaXPercent, deltaYPercent)
                                        }
                                    }
                                )
                            }
                            .then(
                                if (isSelected) {
                                    Modifier
                                        .border(
                                            width = 1.5.dp,
                                            color = parseHexColor(theme.accentColor.toStringHex(), Color(0xFF00F0FF)),
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                                } else Modifier
                            )
                    ) {
                        // Render element content based on elementType
                        when (elementType) {
                            CardElementType.NETWORK_NAME -> {
                                CanvasNetworkNameElement(
                                    title = uiState.networkTitle,
                                    textColor = parseHexColor(pos.textColorHex, theme.textColor),
                                    fontSizeSp = pos.fontSizeSp,
                                    isBold = pos.isBold
                                )
                            }
                            CardElementType.PRICE_BADGE -> {
                                CanvasPriceBadgeElement(
                                    priceText = "YER ${uiState.samplePrice.toInt()}",
                                    badgeBg = parseHexColor(pos.backgroundColorHex, theme.badgeBgColor),
                                    textColor = parseHexColor(pos.textColorHex, theme.badgeTextColor),
                                    fontSizeSp = pos.fontSizeSp,
                                    shape = pos.shape,
                                    visualStyle = pos.visualStyle,
                                    glowColor = parseHexColor(pos.glowColorHex, theme.accentColor)
                                )
                            }
                            CardElementType.CODE_PIN_BOX -> {
                                CanvasCodeBoxElement(
                                    code = uiState.sampleCode,
                                    boxBg = parseHexColor(pos.backgroundColorHex, theme.codeBoxBgColor),
                                    borderColor = parseHexColor(pos.borderColorHex, theme.codeBoxBorderColor),
                                    textColor = parseHexColor(pos.textColorHex, theme.codeBoxTextColor),
                                    fontSizeSp = pos.fontSizeSp,
                                    shape = pos.shape,
                                    visualStyle = pos.visualStyle,
                                    glowColor = parseHexColor(pos.glowColorHex, theme.accentColor)
                                )
                            }
                            CardElementType.QR_CODE -> {
                                CanvasQrCodeElement(
                                    bitmap = qrBitmap,
                                    qrConfig = uiState.qrStyleConfig
                                )
                            }
                            CardElementType.VALIDITY -> {
                                CanvasValidityElement(
                                    quotaText = uiState.sampleQuota,
                                    validityText = uiState.sampleValidity,
                                    textColor = parseHexColor(pos.textColorHex, theme.textColor.copy(alpha = 0.85f)),
                                    fontSizeSp = pos.fontSizeSp
                                )
                            }
                            CardElementType.BATCH_NO -> {
                                CanvasBatchNoElement(
                                    batchNo = uiState.sampleBatchNo,
                                    textColor = parseHexColor(pos.textColorHex, theme.textColor.copy(alpha = 0.7f)),
                                    fontSizeSp = pos.fontSizeSp
                                )
                            }
                            CardElementType.EXPIRY_DATE -> {
                                CanvasExpiryDateElement(
                                    expiryDate = uiState.sampleExpiryDate,
                                    textColor = parseHexColor(pos.textColorHex, theme.textColor.copy(alpha = 0.7f)),
                                    fontSizeSp = pos.fontSizeSp
                                )
                            }
                        }

                        // Corner drag handles indicator when selected
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00F0FF))
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00F0FF))
                            )
                        }
                    }
                }
            }
        }
    }
}

// ================= ELEMENT CANVAS RENDERERS =================

@Composable
private fun CanvasNetworkNameElement(
    title: String,
    textColor: Color,
    fontSizeSp: Float,
    isBold: Boolean
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        Text(
            text = title,
            color = textColor,
            fontSize = fontSizeSp.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(3.dp))
        Icon(
            imageVector = Icons.Default.Router,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size((fontSizeSp * 1.1f).dp)
        )
    }
}

private fun getShapeForElement(shape: com.example.data.cards.ElementShape): androidx.compose.ui.graphics.Shape {
    return when (shape) {
        com.example.data.cards.ElementShape.ROUNDED_RECTANGLE -> RoundedCornerShape(6.dp)
        com.example.data.cards.ElementShape.CAPSULE -> CircleShape
        com.example.data.cards.ElementShape.PILL -> RoundedCornerShape(12.dp)
        com.example.data.cards.ElementShape.SHARP_RECTANGLE -> RoundedCornerShape(0.dp)
    }
}

@Composable
private fun CanvasPriceBadgeElement(
    priceText: String,
    badgeBg: Color,
    textColor: Color,
    fontSizeSp: Float,
    shape: com.example.data.cards.ElementShape = com.example.data.cards.ElementShape.PILL,
    visualStyle: com.example.data.cards.VisualStyleType = com.example.data.cards.VisualStyleType.SOLID,
    glowColor: Color = Color(0xFF00F0FF)
) {
    val composeShape = getShapeForElement(shape)
    val modifier = when (visualStyle) {
        com.example.data.cards.VisualStyleType.SOLID -> Modifier.background(badgeBg, composeShape)
        com.example.data.cards.VisualStyleType.OUTLINE -> Modifier
            .background(Color.Transparent, composeShape)
            .border(1.5.dp, badgeBg, composeShape)
        com.example.data.cards.VisualStyleType.GLASSMORPHISM -> Modifier
            .background(badgeBg.copy(alpha = 0.35f), composeShape)
            .border(1.dp, Color.White.copy(alpha = 0.4f), composeShape)
        com.example.data.cards.VisualStyleType.NEON_GLOW -> Modifier
            .background(badgeBg, composeShape)
            .border(1.5.dp, glowColor, composeShape)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = priceText,
            color = textColor,
            fontSize = fontSizeSp.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CanvasCodeBoxElement(
    code: String,
    boxBg: Color,
    borderColor: Color,
    textColor: Color,
    fontSizeSp: Float,
    shape: com.example.data.cards.ElementShape = com.example.data.cards.ElementShape.CAPSULE,
    visualStyle: com.example.data.cards.VisualStyleType = com.example.data.cards.VisualStyleType.NEON_GLOW,
    glowColor: Color = Color(0xFF00F0FF)
) {
    val composeShape = getShapeForElement(shape)
    val modifier = when (visualStyle) {
        com.example.data.cards.VisualStyleType.SOLID -> Modifier
            .background(boxBg, composeShape)
            .border(1.dp, borderColor, composeShape)
        com.example.data.cards.VisualStyleType.OUTLINE -> Modifier
            .background(Color.Transparent, composeShape)
            .border(1.5.dp, borderColor, composeShape)
        com.example.data.cards.VisualStyleType.GLASSMORPHISM -> Modifier
            .background(boxBg.copy(alpha = 0.4f), composeShape)
            .border(1.dp, Color.White.copy(alpha = 0.35f), composeShape)
        com.example.data.cards.VisualStyleType.NEON_GLOW -> Modifier
            .background(boxBg, composeShape)
            .border(1.5.dp, glowColor, composeShape)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            code.take(8).forEach { char ->
                Box(
                    modifier = Modifier
                        .size((fontSizeSp * 1.2f).dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.Black.copy(alpha = 0.3f))
                        .border(0.5.dp, borderColor.copy(alpha = 0.6f), RoundedCornerShape(3.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = char.toString(),
                        color = textColor,
                        fontSize = (fontSizeSp * 0.9f).sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun CanvasQrCodeElement(
    bitmap: Bitmap?,
    qrConfig: com.example.data.cards.QrStyleConfig = com.example.data.cards.QrStyleConfig()
) {
    val frameShape = RoundedCornerShape(qrConfig.cornerRadiusDp.dp)
    val frameModifier = when (qrConfig.frameStyle) {
        com.example.data.cards.QrFrameStyle.NONE -> Modifier.background(Color.Transparent)
        com.example.data.cards.QrFrameStyle.ROUNDED_BORDER -> Modifier
            .background(Color.White, frameShape)
            .border(1.dp, Color.Gray, frameShape)
        com.example.data.cards.QrFrameStyle.WHITE_CARD -> Modifier.background(Color.White, frameShape)
        com.example.data.cards.QrFrameStyle.NEON_BORDER -> Modifier
            .background(Color.White, frameShape)
            .border(1.5.dp, parseHexColor("#00F0FF"), frameShape)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(frameModifier),
        contentAlignment = Alignment.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(2.dp)
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "QR Code",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    Icons.Default.QrCode2,
                    contentDescription = null,
                    tint = Color.DarkGray,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun CanvasValidityElement(
    quotaText: String,
    validityText: String,
    textColor: Color,
    fontSizeSp: Float
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            Icons.Default.Speed,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(fontSizeSp.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = "$quotaText • $validityText",
            color = textColor,
            fontSize = fontSizeSp.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CanvasBatchNoElement(
    batchNo: String,
    textColor: Color,
    fontSizeSp: Float
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = batchNo,
            color = textColor,
            fontSize = fontSizeSp.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CanvasExpiryDateElement(
    expiryDate: String,
    textColor: Color,
    fontSizeSp: Float
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.CenterEnd
    ) {
        Text(
            text = "انتهاء: $expiryDate",
            color = textColor,
            fontSize = fontSizeSp.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// Helper to convert Compose Color to Hex string
private fun Color.toStringHex(): String {
    val red = (this.red * 255).toInt()
    val green = (this.green * 255).toInt()
    val blue = (this.blue * 255).toInt()
    return String.format("#%02X%02X%02X", red, green, blue)
}

// Helper to parse Hex color to Compose Color
private fun parseHexColor(hex: String, fallback: Color = Color.White): Color {
    return try {
        val clean = hex.removePrefix("#")
        val colorInt = when (clean.length) {
            6 -> android.graphics.Color.parseColor("#$clean")
            8 -> android.graphics.Color.parseColor("#$clean")
            else -> android.graphics.Color.parseColor("#$clean")
        }
        Color(colorInt)
    } catch (e: Exception) {
        fallback
    }
}
