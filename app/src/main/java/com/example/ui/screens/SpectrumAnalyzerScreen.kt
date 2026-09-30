package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BestChannelRecommendation
import com.example.data.model.ChannelRating
import com.example.data.model.ConnectedWifiInfo
import com.example.data.model.ScannedWifiNetwork
import com.example.data.model.SpectrumAnalysisReport
import com.example.data.model.WifiBand
import com.example.ui.MainViewModel
import com.example.util.SpectrumAnalysisEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

@Composable
fun SpectrumAnalyzerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var selectedBand by remember { mutableStateOf(WifiBand.BAND_2_4_GHZ) }
    var isScanning by remember { mutableStateOf(false) }
    var report by remember { mutableStateOf<SpectrumAnalysisReport?>(null) }
    var hasPermission by remember { mutableStateOf(SpectrumAnalysisEngine.hasWifiScanPermissions(context)) }
    var selectedNetworkForAudit by remember { mutableStateOf<ScannedWifiNetwork?>(null) }

    // Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val wifiGranted = permissions[Manifest.permission.ACCESS_WIFI_STATE] ?: false
        hasPermission = fineLocationGranted || wifiGranted

        // Run scan immediately after granting
        scope.launch {
            isScanning = true
            delay(1000)
            report = SpectrumAnalysisEngine.performSpectrumScan(context)
            isScanning = false
        }
    }

    // Trigger initial scan
    LaunchedEffect(Unit) {
        isScanning = true
        delay(600)
        report = SpectrumAnalysisEngine.performSpectrumScan(context)
        isScanning = false
    }

    fun triggerScan() {
        scope.launch {
            if (!hasPermission) {
                val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_WIFI_STATE,
                        Manifest.permission.CHANGE_WIFI_STATE,
                        Manifest.permission.NEARBY_WIFI_DEVICES
                    )
                } else {
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_WIFI_STATE,
                        Manifest.permission.CHANGE_WIFI_STATE
                    )
                }
                permissionLauncher.launch(perms)
            } else {
                isScanning = true
                delay(1000)
                report = SpectrumAnalysisEngine.performSpectrumScan(context)
                isScanning = false
                Toast.makeText(context, "اكتمل تحليل الجو والترددات الحقيقي بنجاح", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val currentRatings = if (selectedBand == WifiBand.BAND_2_4_GHZ) {
        report?.channelRatings24 ?: emptyList()
    } else {
        report?.channelRatings5 ?: emptyList()
    }

    val bestRecommendation = if (selectedBand == WifiBand.BAND_2_4_GHZ) {
        report?.bestRecommendation24
    } else {
        report?.bestRecommendation5
    }

    val activeBandNetworks = report?.networks?.filter { it.band == selectedBand } ?: emptyList()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070D18))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header Banner & Live Scanner Trigger
        item {
            SpectrumHeaderCard(
                isScanning = isScanning,
                isHardware = report?.isHardwareScan == true,
                onScanClick = { triggerScan() }
            )
        }

        // 2. Hardware Alerts (Wi-Fi disabled or Location disabled)
        if (report != null && !report!!.wifiEnabled) {
            item {
                HardwareWarningBanner(
                    icon = Icons.Default.WifiOff,
                    title = "الواي فاي مغلق في الهاتف",
                    message = "يرجى تشغيل الواي فاي لتمكين التطبيق من فحص الإشارات وقراءة القنوات المحيطة بدقة.",
                    actionLabel = "تفعيل الواي فاي",
                    onAction = { openWifiSettings(context) }
                )
            }
        }

        if (report != null && !report!!.locationServicesEnabled) {
            item {
                HardwareWarningBanner(
                    icon = Icons.Default.LocationOn,
                    title = "خدمة الموقع (GPS) غير مفعلة",
                    message = "يشترط نظام أندرويد تفعيل خدمة الموقع لإتاحة نتائج فحص قنوات الواي فاي الحقيقية للتطبيقات.",
                    actionLabel = "تفعيل الموقع",
                    onAction = { openLocationSettings(context) }
                )
            }
        }

        // 3. Real Connected Network Card (عندما أرتبط بالشبكة مباشرة يحلل الجو والقنوات)
        item {
            LiveConnectedWifiStatusCard(
                connectedInfo = report?.connectedWifi,
                bestRecommendation = bestRecommendation,
                onSelectMatchingBand = { band ->
                    selectedBand = band
                }
            )
        }

        // 4. Band Switcher (2.4 GHz توجي vs 5 GHz فايف جي)
        item {
            BandSelectorPills(
                selectedBand = selectedBand,
                onSelectBand = { selectedBand = it },
                detectedCount = activeBandNetworks.size
            )
        }

        // 5. Golden Channel Advisor Card (المقترح لاختيار القناة الأفضل)
        item {
            if (bestRecommendation != null) {
                GoldenChannelRecommendationHeroCard(
                    recommendation = bestRecommendation,
                    connectedWifi = report?.connectedWifi,
                    onCopyCommand = { cmd ->
                        clipboardManager.setText(AnnotatedString(cmd))
                        Toast.makeText(context, "تم نسخ أمر ميكروتك إلى الحافظة", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        // 6. Live Visual Spectrum Waveform (رسم بياني حقيقي لمنحنيات الطيف)
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1726)),
                border = BorderStroke(1.dp, Color(0xFF1E3252)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "مخطط الطيف الراديوي الحي (Parabolic RF Waves)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Text(
                            text = "${selectedBand.displayName} • ${activeBandNetworks.size} إشارة حقيقية",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }

                    Text(
                        text = "يوضح التداخل والاصطدام بين إشارات الأكسسات وقوة الإرسال الحقيقية (dBm)",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )

                    if (activeBandNetworks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF070E1A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Wifi, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "لم يتم رصد إشارات في هذا النطاق حالياً من حساس الواي فاي بالهاتف",
                                    color = Color(0xFF64748B),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    } else {
                        // The Canvas Waveform
                        LiveSpectrumWaveformCanvas(
                            networks = activeBandNetworks,
                            band = selectedBand,
                            connectedBssid = report?.connectedWifi?.bssid,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Networks legend pills
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(activeBandNetworks) { net ->
                                val isCurrentConnected = report?.connectedWifi?.bssid?.equals(net.bssid, ignoreCase = true) == true
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isCurrentConnected) Color(0xFF0C2E4E) else Color(0xFF132238),
                                    border = BorderStroke(1.dp, if (isCurrentConnected) Color(0xFF38BDF8) else net.waveColor.copy(alpha = 0.5f)),
                                    onClick = { selectedNetworkForAudit = net }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(net.waveColor)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isCurrentConnected) "${net.ssid} (شبكتك الحالية • ق${net.channel})" else "${net.ssid} (ق${net.channel} • ${net.rssi}dBm)",
                                            color = if (isCurrentConnected) Color(0xFF38BDF8) else Color.White,
                                            fontWeight = if (isCurrentConnected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 10.5.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 7. Channel Ranking List & Cleanliness Meters
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Speed,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ترتيب القنوات الفعلي حسب النقاء وعدم التشويش",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Text(
                    text = if (selectedBand == WifiBand.BAND_2_4_GHZ) "القنوات القياسية: 1، 6، 11" else "ترددات غير متداخلة",
                    color = Color(0xFF38BDF8),
                    fontSize = 11.sp
                )
            }
        }

        items(currentRatings) { rating ->
            ChannelEvaluationRowCard(
                rating = rating,
                onCopyScript = {
                    clipboardManager.setText(AnnotatedString(rating.routerOsCommand))
                    Toast.makeText(context, "تم نسخ أمر ضبط القناة ${rating.channelNumber}", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // 8. RouterOS Spectral Scan Guide for MikroTik Hardware
        item {
            MikroTikSpectralHardwareCard(
                onCopy = { cmd ->
                    clipboardManager.setText(AnnotatedString(cmd))
                    Toast.makeText(context, "تم نسخ أمر ميكروتك", Toast.LENGTH_SHORT).show()
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ================= COMPONENT: HEADER CARD =================
@Composable
fun SpectrumHeaderCard(
    isScanning: Boolean,
    isHardware: Boolean,
    onScanClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val radarRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF0D1B2E),
        border = BorderStroke(1.dp, Color(0xFF1E385C)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                            .background(Color(0xFF0284C7).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Radar,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier
                                .size(22.dp)
                                .then(if (isScanning) Modifier.rotate(radarRotation) else Modifier)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "محلل الطيف والقنوات والتشويش الحقيقي",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isHardware) Color(0xFF10B981) else Color(0xFF38BDF8))
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isHardware) "بث حي مباشر من حساس راديو الهاتف" else "جاهز لفحص ترددات الهواء المحيطة",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Button(
                    onClick = onScanClick,
                    enabled = !isScanning,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("جاري الفحص...", fontSize = 11.sp, color = Color.White)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("فحص الهواء", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ================= COMPONENT: LIVE CONNECTED NETWORK CARD =================
@Composable
fun LiveConnectedWifiStatusCard(
    connectedInfo: ConnectedWifiInfo?,
    bestRecommendation: BestChannelRecommendation?,
    onSelectMatchingBand: (WifiBand) -> Unit
) {
    if (connectedInfo == null || !connectedInfo.isConnected) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF334155)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.WifiOff, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "الهاتف غير متصل بأي شبكة واي فاي حالياً",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "عندما تتصل بشبكة الراوتر مباشرة، سيقوم المحلل بفحص قناتها الحالية فورياً وتقديم المقترح الأفضل لنقائها.",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }
            }
        }
        return
    }

    val isBand24 = connectedInfo.band == WifiBand.BAND_2_4_GHZ
    val bandText = if (isBand24) "2.4 GHz (توجي)" else "5 GHz (فايف جي)"
    val bandBadgeColor = if (isBand24) Color(0xFF0284C7) else Color(0xFF8B5CF6)

    val currentScore = connectedInfo.currentChannelCleanliness ?: 80
    val scoreColor = when {
        currentScore >= 80 -> Color(0xFF10B981)
        currentScore >= 55 -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1E38)),
        border = BorderStroke(1.5.dp, Color(0xFF1E4976)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Connected Indicator + Band Pill
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
                            .background(Color(0xFF0284C7).copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Wifi, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "شبكتك الحالية المتصل بها مباشرة ⚡",
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Text(
                            text = connectedInfo.ssid ?: "شبكة واي فاي متصلة",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = bandBadgeColor,
                    modifier = Modifier.clickable {
                        connectedInfo.band?.let { onSelectMatchingBand(it) }
                    }
                ) {
                    Text(
                        text = bandText,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stats Matrix: Operating Channel, Frequency, RSSI, Gateway IP
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Channel Box
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0E2544))
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("القناة العاملة", color = Color(0xFF94A3B8), fontSize = 10.sp)
                    Text("قناة ${connectedInfo.channel ?: "-"}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("${connectedInfo.frequency ?: 0} MHz", color = Color(0xFF38BDF8), fontSize = 9.5.sp)
                }

                // Signal / Link Speed Box
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0E2544))
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("قوة الإشارة", color = Color(0xFF94A3B8), fontSize = 10.sp)
                    Text("${connectedInfo.rssi ?: 0} dBm", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("${connectedInfo.linkSpeedMbps ?: 0} Mbps", color = Color(0xFF10B981), fontSize = 9.5.sp)
                }

                // Cleanliness Score Box
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0E2544))
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("نقاء القناة", color = Color(0xFF94A3B8), fontSize = 10.sp)
                    Text("$currentScore%", color = scoreColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("${connectedInfo.competingApsOnChannel} متداخل", color = Color(0xFF94A3B8), fontSize = 9.5.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Router Gateway & BSSID Info Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "آي بي الراوتر: ${connectedInfo.gatewayIp ?: "192.168.88.1"}",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "الماك: ${connectedInfo.bssid ?: ""}",
                    color = Color(0xFF64748B),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Real-time Warning or Optimization Message
            if (!connectedInfo.channelWarningMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (currentScore < 60) Color(0xFF7F1D1D).copy(alpha = 0.35f) else Color(0xFF78350F).copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, if (currentScore < 60) Color(0xFFEF4444) else Color(0xFFF59E0B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (currentScore < 60) Icons.Default.Warning else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (currentScore < 60) Color(0xFFEF4444) else Color(0xFFF59E0B),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = connectedInfo.channelWarningMessage,
                            color = Color.White,
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

// ================= COMPONENT: HARDWARE WARNING BANNER =================
@Composable
fun HardwareWarningBanner(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF271B0B)),
        border = BorderStroke(1.dp, Color(0xFFB45309)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = title, color = Color(0xFFFDE68A), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(text = message, color = Color(0xFFCBD5E1), fontSize = 10.5.sp, lineHeight = 14.sp)
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB45309)),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(actionLabel, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ================= COMPONENT: BAND SELECTOR PILLS =================
@Composable
fun BandSelectorPills(
    selectedBand: WifiBand,
    onSelectBand: (WifiBand) -> Unit,
    detectedCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        WifiBand.values().forEach { band ->
            val isSelected = selectedBand == band
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) Color(0xFF0F2E4F) else Color(0xFF0C1726),
                border = BorderStroke(1.5.dp, if (isSelected) Color(0xFF0284C7) else Color(0xFF1A2A40)),
                onClick = { onSelectBand(band) },
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (band == WifiBand.BAND_2_4_GHZ) "2.4 GHz (توجي)" else "5 GHz (فايف جي)",
                            color = if (isSelected) Color.White else Color(0xFF94A3B8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = if (band == WifiBand.BAND_2_4_GHZ) "تغطية أوسع واختراق جدران" else "سرعة فائقة وخلو من التشويش",
                            color = Color(0xFF64748B),
                            fontSize = 9.5.sp
                        )
                    }
                    if (isSelected) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF0284C7),
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.padding(3.dp))
                        }
                    }
                }
            }
        }
    }
}

// ================= COMPONENT: GOLDEN CHANNEL RECOMMENDATION HERO CARD =================
@Composable
fun GoldenChannelRecommendationHeroCard(
    recommendation: BestChannelRecommendation,
    connectedWifi: ConnectedWifiInfo?,
    onCopyCommand: (String) -> Unit
) {
    var selectedCommandType by remember { mutableStateOf(0) } // 0: Legacy RouterOS, 1: WifiWave2 / WiFi v7

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF06231C),
        border = BorderStroke(1.5.dp, Color(0xFF059669)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                            .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "المقترح لاختيار القناة الأفضل 🏆",
                            color = Color(0xFF6EE7B7),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "القناة ${recommendation.recommendedChannel} (${recommendation.frequencyMhz} MHz)",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                    }
                }

                // Cleanliness Score Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF059669)
                ) {
                    Text(
                        text = "نقاء ${recommendation.cleanlinessScore}%",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Reasoning Text
            Text(
                text = "💡 التحليل الهندسي: ${recommendation.reasoning}",
                color = Color(0xFFD1FAE5),
                fontSize = 11.5.sp,
                lineHeight = 17.sp
            )

            // Current Channel Comparison if user is connected
            if (!recommendation.comparisonWithCurrent.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF064E3B).copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = recommendation.comparisonWithCurrent,
                        color = Color(0xFFA7F3D0),
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFF064E3B))
            Spacer(modifier = Modifier.height(10.dp))

            // MikroTik Command Selector Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "أمر ضبط الراوتر التلقائي:",
                    color = Color(0xFFA7F3D0),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (selectedCommandType == 0) Color(0xFF059669) else Color(0xFF0C2B22),
                        modifier = Modifier.clickable { selectedCommandType = 0 }
                    ) {
                        Text(
                            text = "RouterOS v6",
                            color = Color.White,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (selectedCommandType == 1) Color(0xFF059669) else Color(0xFF0C2B22),
                        modifier = Modifier.clickable { selectedCommandType = 1 }
                    ) {
                        Text(
                            text = "WiFi v7",
                            color = Color.White,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            val activeCommand = if (selectedCommandType == 0) recommendation.routerOsCommand else recommendation.routerOsWifiWave2Command

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF041913))
                    .border(0.5.dp, Color(0xFF059669), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = activeCommand,
                        color = Color(0xFF6EE7B7),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.5.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = { onCopyCommand(activeCommand) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("نسخ الأمر", color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ================= COMPONENT: LIVE SPECTRUM CANVAS (PARABOLIC WAVES) =================
@Composable
fun LiveSpectrumWaveformCanvas(
    networks: List<ScannedWifiNetwork>,
    band: WifiBand,
    connectedBssid: String?,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val bottomMargin = 30f
        val leftMargin = 35f
        val chartWidth = width - leftMargin
        val chartHeight = height - bottomMargin

        // Draw Background Grid Lines (dBm levels: -30, -50, -70, -90)
        val dbmSteps = listOf(-30, -50, -70, -90)
        dbmSteps.forEach { dbm ->
            val normalizedY = (dbm - (-100f)) / 70f
            val y = chartHeight - (normalizedY * chartHeight)

            drawLine(
                color = Color(0xFF1E293B),
                start = Offset(leftMargin, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )

            drawText(
                textMeasurer = textMeasurer,
                text = "${dbm}dBm",
                topLeft = Offset(2f, y - 8f),
                style = TextStyle(color = Color(0xFF64748B), fontSize = 8.sp)
            )
        }

        // Draw Channel frequency scale
        val channelRange = if (band == WifiBand.BAND_2_4_GHZ) {
            (1..13).toList()
        } else {
            listOf(36, 40, 44, 48, 52, 56, 60, 64, 100, 104, 108, 112, 116, 132, 136, 140, 149, 153, 157, 161, 165)
        }

        val stepX = chartWidth / (channelRange.size + 1)

        channelRange.forEachIndexed { idx, ch ->
            val x = leftMargin + (stepX * (idx + 1))

            // Channel tick
            drawLine(
                color = if (ch == 1 || ch == 6 || ch == 11) Color(0xFF0284C7) else Color(0xFF1E293B),
                start = Offset(x, chartHeight),
                end = Offset(x, chartHeight + 6f),
                strokeWidth = 1.5f
            )

            // Channel label
            drawText(
                textMeasurer = textMeasurer,
                text = "$ch",
                topLeft = Offset(x - 5f, chartHeight + 8f),
                style = TextStyle(
                    color = if (ch == 1 || ch == 6 || ch == 11) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                    fontSize = 9.sp,
                    fontWeight = if (ch == 1 || ch == 6 || ch == 11) FontWeight.Bold else FontWeight.Normal
                )
            )
        }

        // Draw Parabolic Waves for each detected network
        networks.forEach { net ->
            val chIndex = channelRange.indexOf(net.channel)
            if (chIndex >= 0) {
                val centerX = leftMargin + (stepX * (chIndex + 1))
                val clampedRssi = max(-95f, min(-30f, net.rssi.toFloat()))
                val normalizedLevel = (clampedRssi - (-100f)) / 70f
                val peakY = chartHeight - (normalizedLevel * chartHeight)

                val halfSpread = stepX * (if (net.channelWidth >= 40) 2.2f else 1.2f)
                val startX = centerX - halfSpread
                val endX = centerX + halfSpread

                val isConnectedNet = connectedBssid?.equals(net.bssid, ignoreCase = true) == true
                val waveColor = if (isConnectedNet) Color(0xFF38BDF8) else net.waveColor

                val path = Path().apply {
                    moveTo(startX, chartHeight)
                    cubicTo(
                        startX + (halfSpread * 0.4f), chartHeight,
                        centerX - (halfSpread * 0.2f), peakY,
                        centerX, peakY
                    )
                    cubicTo(
                        centerX + (halfSpread * 0.2f), peakY,
                        endX - (halfSpread * 0.4f), chartHeight,
                        endX, chartHeight
                    )
                    close()
                }

                // Fill with subtle gradient
                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        colors = listOf(waveColor.copy(alpha = if (isConnectedNet) 0.65f else 0.4f), waveColor.copy(alpha = 0.05f)),
                        startY = peakY,
                        endY = chartHeight
                    )
                )

                // Draw curve outline
                drawPath(
                    path = path,
                    color = waveColor,
                    style = Stroke(width = if (isConnectedNet) 3.5f else 2f, cap = StrokeCap.Round)
                )

                // Draw SSID label above peak
                val truncatedSsid = if (net.ssid.length > 12) net.ssid.take(10) + ".." else net.ssid
                val labelText = if (isConnectedNet) "★ $truncatedSsid\n${net.rssi}dBm" else "$truncatedSsid\n${net.rssi}dBm"
                drawText(
                    textMeasurer = textMeasurer,
                    text = labelText,
                    topLeft = Offset(centerX - 24f, max(4f, peakY - 26f)),
                    style = TextStyle(
                        color = if (isConnectedNet) Color(0xFF38BDF8) else Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

// ================= COMPONENT: CHANNEL EVALUATION ROW CARD =================
@Composable
fun ChannelEvaluationRowCard(
    rating: ChannelRating,
    onCopyScript: () -> Unit
) {
    val progressColor = when {
        rating.cleanlinessScore >= 80 -> Color(0xFF10B981)
        rating.cleanlinessScore >= 55 -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF0C1726),
        border = BorderStroke(
            1.5.dp,
            when {
                rating.isCurrentConnectedChannel -> Color(0xFF38BDF8)
                rating.isRecommended -> Color(0xFF059669)
                else -> Color(0xFF1A2A40)
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Channel Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when {
                            rating.isCurrentConnectedChannel -> Color(0xFF0284C7)
                            rating.isRecommended -> Color(0xFF059669)
                            else -> Color(0xFF1E293B)
                        }
                    ) {
                        Text(
                            text = "قناة ${rating.channelNumber}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "${rating.frequencyMhz} MHz",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )

                    if (rating.isCurrentConnectedChannel) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF0284C7).copy(alpha = 0.25f)
                        ) {
                            Text("قناتك الحالية 📍", color = Color(0xFF38BDF8), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                        }
                    }

                    if (rating.isRecommended) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF059669).copy(alpha = 0.25f)
                        ) {
                            Text("المقترحة 🏆", color = Color(0xFF34D399), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                        }
                    }
                }

                // Cleanliness Score
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${rating.cleanlinessScore}%",
                        color = progressColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(onClick = onCopyScript, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "نسخ أمر الراوتر", tint = Color.LightGray, modifier = Modifier.size(14.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { rating.cleanlinessScore / 100f },
                color = progressColor,
                trackColor = Color(0xFF1E293B),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Diagnostic notes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = rating.recommendationTitle,
                    color = progressColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "الأجهزة المتنافسة: ${rating.competingApCount} (نفس القناة: ${rating.coChannelCount})",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp
                )
            }
        }
    }
}

// ================= COMPONENT: MIKROTIK ROUTEROS HARDWARE SPECTRAL CARD =================
@Composable
fun MikroTikSpectralHardwareCard(
    onCopy: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0B1727),
        border = BorderStroke(1.dp, Color(0xFF1E3350)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Router, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "أدوات ميكروتك لفحص طيف الراديو من الهوائي (Spectral Scan)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "إذا كان جهاز البث (BaseBox أو NetMetal أو mANTBox) مثبتاً على برج مرتفع، يمكنك تشغيل فحص الضوضاء والتشويش المباشر من منظور الهوائي نفسه بالأمر التالي:",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF070E1A))
                    .border(0.5.dp, Color(0xFF1E3252), RoundedCornerShape(6.dp))
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "/interface wireless frequency-monitor wlan1 duration=10s",
                        color = Color(0xFF38BDF8),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { onCopy("/interface wireless frequency-monitor wlan1 duration=10s") },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

// Helpers
fun openWifiSettings(context: Context) {
    try {
        context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    } catch (e: Exception) {
        Toast.makeText(context, "تعذر فتح إعدادات الواي فاي", Toast.LENGTH_SHORT).show()
    }
}

fun openLocationSettings(context: Context) {
    try {
        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    } catch (e: Exception) {
        Toast.makeText(context, "تعذر فتح إعدادات الموقع", Toast.LENGTH_SHORT).show()
    }
}
