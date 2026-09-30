package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.cards.A4GridConfig
import com.example.data.cards.A4Orientation
import com.example.ui.cards.CardStudioUiState

/**
 * مكون معاينة صفحة A4 الكاملة (210 × 297 مم) مع التوزيع التلقائي للكروت
 * - يدعم إيماءات التكبير والتصغير المزدوجة بإنشين (Pinch-to-Zoom & Pan Scrolling)
 * - يرسم خطوط القص المقطعة (Dashed Cut Lines) للطباعة
 */
@Composable
fun A4SheetPreviewComposable(
    uiState: CardStudioUiState,
    modifier: Modifier = Modifier
) {
    val gridConfig = uiState.gridConfig
    val dims = uiState.dimensions

    // Pinch-to-zoom and Pan state
    var scale by remember { mutableFloatStateOf(1.0f) }
    var offsetX by remember { mutableFloatStateOf(0.0f) }
    var offsetY by remember { mutableFloatStateOf(0.0f) }

    // Page aspect ratio
    val a4Ratio = if (gridConfig.orientation == A4Orientation.PORTRAIT) (210.0f / 297.0f) else (297.0f / 210.0f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.6f, 3.5f)
                    offsetX += pan.x
                    offsetY += pan.y
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = Color.White,
            shadowElevation = 10.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .aspectRatio(a4Ratio)
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    // Header Bar on paper
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (gridConfig.showCutLines) "✂️ خطوط القص المحددة • صفحة 1 من 1" else "صفحة 1 من 1",
                            color = Color.DarkGray,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = "${uiState.networkTitle} • A4 Sheet (${gridConfig.cardsPerPage} كرت)",
                            color = Color.Black,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(color = Color.LightGray)
                    Spacer(modifier = Modifier.height(6.dp))

                    // Grid Layout of Cards
                    val cols = gridConfig.columns
                    val rows = gridConfig.rows
                    val totalCards = gridConfig.cardsPerPage

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        repeat(rows) { rowIndex ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                repeat(cols) { colIndex ->
                                    val cardIndex = rowIndex * cols + colIndex
                                    if (cardIndex < totalCards) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxSize()
                                        ) {
                                            MiniCardPreviewBox(uiState = uiState)

                                            // Draw dashed cut lines around the card
                                            if (gridConfig.showCutLines) {
                                                DashedCutLinesOverlay()
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
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

@Composable
private fun MiniCardPreviewBox(uiState: CardStudioUiState) {
    val theme = uiState.selectedTheme
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = theme.backgroundStart,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, theme.borderColor.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxSize()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "YER ${uiState.samplePrice.toInt()}",
                        color = theme.badgeTextColor,
                        fontSize = 6.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(theme.badgeBgColor, RoundedCornerShape(2.dp))
                            .padding(horizontal = 2.dp)
                    )

                    Text(
                        text = uiState.networkTitle.take(12),
                        color = theme.textColor,
                        fontSize = 6.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(2.dp),
                    color = theme.codeBoxBgColor,
                    border = androidx.compose.foundation.BorderStroke(0.3.dp, theme.codeBoxBorderColor)
                ) {
                    Text(
                        text = uiState.sampleCode,
                        color = theme.codeBoxTextColor,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = uiState.sampleQuota,
                        color = theme.textColor.copy(alpha = 0.8f),
                        fontSize = 5.5.sp
                    )

                    Text(
                        text = uiState.sampleValidity,
                        color = theme.textColor.copy(alpha = 0.8f),
                        fontSize = 5.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DashedCutLinesOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val stroke = Stroke(
            width = 1.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
        )
        drawRect(
            color = Color.Red.copy(alpha = 0.45f),
            style = stroke
        )
    }
}
