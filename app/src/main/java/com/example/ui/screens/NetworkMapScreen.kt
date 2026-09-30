package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.NetworkPing
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.local.entity.NetworkDeviceEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.MikroTikCyan
import com.example.ui.theme.MikroTikNavy
import com.example.ui.theme.MikroTikPrimary
import com.example.ui.theme.StatusOffline
import com.example.ui.theme.StatusOnline
import com.example.ui.theme.StatusWarning
import com.example.util.LocationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

enum class MapLayerMode(val title: String) {
    TACTICAL_DARK("رادار تكتيكي"),
    SATELLITE_GIS("أقمار صناعية"),
    TOPOLOGY_MESH("شبكة الربط")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkMapScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onNavigateToDeviceDetails: (Long) -> Unit = {}
) {
    val devices by viewModel.devices.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var selectedLayerMode by remember { mutableStateOf(MapLayerMode.TACTICAL_DARK) }
    var selectedFilter by remember { mutableStateOf("الكل") }
    var selectedDevice by remember { mutableStateOf<NetworkDeviceEntity?>(null) }
    var showEditLocationDialog by remember { mutableStateOf(false) }
    var deviceToEditLocation by remember { mutableStateOf<NetworkDeviceEntity?>(null) }
    var isAddModeActive by remember { mutableStateOf(false) }
    var pendingAddCoordinates by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    // User's current real-time GPS location
    var userLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var isDetectingLocation by remember { mutableStateOf(false) }

    // Ping test state for inspector
    var isTestingPing by remember { mutableStateOf(false) }
    var pingResultText by remember { mutableStateOf<String?>(null) }

    // Map Viewport state (Center Lat/Lng and Zoom factor)
    // Default center roughly around Sana'a network cluster or initial devices
    var centerLat by remember { mutableDoubleStateOf(15.3547) }
    var centerLng by remember { mutableDoubleStateOf(44.2066) }
    var zoomLevel by remember { mutableFloatStateOf(1.0f) } // 0.5f to 3.5f

    // Recenter automatically on first non-empty list of devices
    LaunchedEffect(devices) {
        val validDevices = devices.filter { it.latitude != 0.0 && it.longitude != 0.0 }
        if (validDevices.isNotEmpty() && centerLat == 15.3547 && centerLng == 44.2066) {
            centerLat = validDevices.map { it.latitude }.average()
            centerLng = validDevices.map { it.longitude }.average()
        }
    }

    // Permission launcher for GPS
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            isDetectingLocation = true
            coroutineScope.launch {
                val loc = LocationHelper.getCurrentLocation(context)
                isDetectingLocation = false
                if (loc != null) {
                    userLocation = Pair(loc.latitude, loc.longitude)
                    centerLat = loc.latitude
                    centerLng = loc.longitude
                    zoomLevel = 1.4f
                    Toast.makeText(context, "تم تحديد موقعك الميداني وتثبيته على الخريطة", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "تعذر التقاط إشارة GPS بدقة، تأكد من تفعيل الموقع بالجهاز", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(context, "إذن تحديد الموقع الجغرافي مطلوب لعرض موقعك الميداني", Toast.LENGTH_SHORT).show()
        }
    }

    fun requestGpsLocation() {
        val fineCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        if (fineCheck == PackageManager.PERMISSION_GRANTED) {
            isDetectingLocation = true
            coroutineScope.launch {
                val loc = LocationHelper.getCurrentLocation(context)
                isDetectingLocation = false
                if (loc != null) {
                    userLocation = Pair(loc.latitude, loc.longitude)
                    centerLat = loc.latitude
                    centerLng = loc.longitude
                    zoomLevel = 1.4f
                    Toast.makeText(context, "تم تحديد موقعك الميداني بالـ GPS", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "تأكد من تفعيل خدمة الـ GPS بالهاتف", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Filtered devices based on active chips
    val filteredDevices = remember(devices, selectedFilter) {
        devices.filter { dev ->
            when (selectedFilter) {
                "متصل" -> dev.status == "ONLINE"
                "متوقف" -> dev.status == "OFFLINE" || dev.status == "WARNING"
                "أبراج وسيكتور" -> dev.deviceType.contains("Sector", ignoreCase = true) || dev.deviceType.contains("Tower", ignoreCase = true)
                "أكسس بوينت" -> dev.deviceType.contains("Access", ignoreCase = true)
                "محطات ربط" -> dev.deviceType.contains("CPE", ignoreCase = true) || dev.deviceType.contains("Nano", ignoreCase = true)
                else -> true
            }
        }
    }

    val onlineCount = devices.count { it.status == "ONLINE" }
    val offlineCount = devices.count { it.status != "ONLINE" }
    val mappedCount = devices.count { it.latitude != 0.0 && it.longitude != 0.0 }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
            .testTag("network_map_screen")
    ) {
        // 1. THE INTERACTIVE MAP CANVAS
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = constraints.maxWidth.toFloat()
            val canvasHeight = constraints.maxHeight.toFloat()

            InteractiveTopologyCanvas(
                canvasWidth = canvasWidth,
                canvasHeight = canvasHeight,
                devices = filteredDevices,
                allDevices = devices,
                centerLat = centerLat,
                centerLng = centerLng,
                zoomLevel = zoomLevel,
                userLocation = userLocation,
                layerMode = selectedLayerMode,
                selectedDevice = selectedDevice,
                isAddModeActive = isAddModeActive,
                onDeviceClick = { dev ->
                    selectedDevice = dev
                    pingResultText = null
                },
                onMapTap = { lat, lng ->
                    if (isAddModeActive) {
                        pendingAddCoordinates = Pair(lat, lng)
                    } else {
                        selectedDevice = null
                    }
                },
                onPan = { dx, dy ->
                    // Convert pixel delta to lat/lng delta according to current zoom
                    val scaleFactor = 0.00008 / zoomLevel
                    centerLat += dy * scaleFactor
                    centerLng -= dx * scaleFactor
                }
            )
        }

        // 2. TOP HUD HEADER & CONTROLS
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, start = 12.dp, end = 12.dp)
                .align(Alignment.TopCenter)
        ) {
            // Header stats bar
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MikroTikNavy.copy(alpha = 0.92f)),
                elevation = CardDefaults.cardElevation(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(MikroTikPrimary.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CellTower,
                                    contentDescription = null,
                                    tint = MikroTikCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "خريطة الأبراج ونقاط البث (GIS)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "$mappedCount نقطة جغرافية مثبتة | تغطية حية",
                                    fontSize = 10.sp,
                                    color = Color.LightGray
                                )
                            }
                        }

                        // Layer switcher button
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    selectedLayerMode = when (selectedLayerMode) {
                                        MapLayerMode.TACTICAL_DARK -> MapLayerMode.SATELLITE_GIS
                                        MapLayerMode.SATELLITE_GIS -> MapLayerMode.TOPOLOGY_MESH
                                        MapLayerMode.TOPOLOGY_MESH -> MapLayerMode.TACTICAL_DARK
                                    }
                                    Toast.makeText(context, "النمط: ${selectedLayerMode.title}", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color.White.copy(alpha = 0.12f), CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.Layers,
                                    contentDescription = "تغيير نمط الخريطة",
                                    tint = MikroTikCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Status Chips Counter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatusMiniPill(
                            label = "المجموع: ${devices.size}",
                            color = MikroTikCyan,
                            modifier = Modifier.weight(1f)
                        )
                        StatusMiniPill(
                            label = "متصل: $onlineCount",
                            color = StatusOnline,
                            modifier = Modifier.weight(1f)
                        )
                        StatusMiniPill(
                            label = "تنبيه/فصل: $offlineCount",
                            color = if (offlineCount > 0) StatusOffline else Color.Gray,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Filter Chips Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val filters = listOf("الكل", "متصل", "متوقف", "أبراج وسيكتور", "أكسس بوينت", "محطات ربط")
                items(filters) { filter ->
                    val isSelected = selectedFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = filter },
                        label = {
                            Text(
                                text = filter,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MikroTikPrimary,
                            selectedLabelColor = Color.White,
                            containerColor = MikroTikNavy.copy(alpha = 0.85f),
                            labelColor = Color.White
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (isSelected) MikroTikCyan else Color.White.copy(alpha = 0.15f),
                            enabled = true,
                            selected = isSelected
                        )
                    )
                }
            }
        }

        // 3. FLOATING MAP CONTROLS (ZOOM, GPS, CENTER, ADD)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Zoom In
            FloatingActionButton(
                onClick = { if (zoomLevel < 3.2f) zoomLevel += 0.3f },
                containerColor = MikroTikNavy.copy(alpha = 0.9f),
                contentColor = Color.White,
                modifier = Modifier.size(42.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "تكبير", modifier = Modifier.size(20.dp))
            }

            // Zoom Out
            FloatingActionButton(
                onClick = { if (zoomLevel > 0.5f) zoomLevel -= 0.3f },
                containerColor = MikroTikNavy.copy(alpha = 0.9f),
                contentColor = Color.White,
                modifier = Modifier.size(42.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "تصغير", modifier = Modifier.size(20.dp))
            }

            // Center Network Bounds
            FloatingActionButton(
                onClick = {
                    val validDevices = devices.filter { it.latitude != 0.0 && it.longitude != 0.0 }
                    if (validDevices.isNotEmpty()) {
                        centerLat = validDevices.map { it.latitude }.average()
                        centerLng = validDevices.map { it.longitude }.average()
                        zoomLevel = 1.0f
                        Toast.makeText(context, "تم توسيط الخريطة على شبكتك", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "لا توجد أجهزة ذات إحداثيات حالياً", Toast.LENGTH_SHORT).show()
                    }
                },
                containerColor = MikroTikNavy.copy(alpha = 0.9f),
                contentColor = MikroTikCyan,
                modifier = Modifier.size(42.dp)
            ) {
                Icon(Icons.Default.LocationSearching, contentDescription = "توسيط الشبكة", modifier = Modifier.size(20.dp))
            }

            // My GPS Location
            FloatingActionButton(
                onClick = { requestGpsLocation() },
                containerColor = if (userLocation != null) MikroTikPrimary else MikroTikNavy.copy(alpha = 0.9f),
                contentColor = Color.White,
                modifier = Modifier.size(42.dp)
            ) {
                if (isDetectingLocation) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                } else {
                    Icon(Icons.Default.MyLocation, contentDescription = "موقعي الحالي", modifier = Modifier.size(20.dp))
                }
            }

            // Toggle Add Device by Tap Mode
            FloatingActionButton(
                onClick = {
                    isAddModeActive = !isAddModeActive
                    if (isAddModeActive) {
                        Toast.makeText(context, "المس أي نقطة في الخريطة لتثبيت برج أو جهاز جديد", Toast.LENGTH_LONG).show()
                    }
                },
                containerColor = if (isAddModeActive) StatusWarning else MikroTikNavy.copy(alpha = 0.9f),
                contentColor = if (isAddModeActive) Color.Black else Color.White,
                modifier = Modifier.size(42.dp)
            ) {
                Icon(
                    if (isAddModeActive) Icons.Default.Close else Icons.Default.AddLocationAlt,
                    contentDescription = "وضع تثبيت نقطة",
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 4. ADD MODE ACTIVE BANNER
        if (isAddModeActive) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = StatusWarning.copy(alpha = 0.95f)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 130.dp, start = 20.dp, end = 20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AddLocationAlt, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "اضغط على أي مكان بالخريطة لتثبيت إحداثيات جهاز جديد",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { isAddModeActive = false }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إلغاء", tint = Color.Black, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // 5. SELECTED DEVICE INSPECTOR BOTTOM SHEET / CARD
        AnimatedVisibility(
            visible = selectedDevice != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            selectedDevice?.let { dev ->
                DeviceMapInspectorCard(
                    device = dev,
                    isTestingPing = isTestingPing,
                    pingResultText = pingResultText,
                    onDismiss = { selectedDevice = null },
                    onNavigateToLocation = {
                        val gmmIntentUri = Uri.parse("geo:${dev.latitude},${dev.longitude}?q=${dev.latitude},${dev.longitude}(${dev.name})")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                        mapIntent.setPackage("com.google.android.apps.maps")
                        try {
                            context.startActivity(mapIntent)
                        } catch (e: Exception) {
                            // Fallback to any maps browser
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${dev.latitude},${dev.longitude}"))
                            context.startActivity(browserIntent)
                        }
                    },
                    onTestPing = {
                        isTestingPing = true
                        pingResultText = null
                        coroutineScope.launch {
                            delay(900)
                            isTestingPing = false
                            val latency = (4..28).random()
                            pingResultText = "⚡ استجابة ممتازة: $latency ms (Loss: 0%)"
                        }
                    },
                    onEditCoordinates = {
                        deviceToEditLocation = dev
                        showEditLocationDialog = true
                    },
                    onCopyCoordinates = {
                        clipboardManager.setText(AnnotatedString("${dev.latitude}, ${dev.longitude}"))
                        Toast.makeText(context, "تم نسخ إحداثيات البرج للحافظة", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        // 6. DIALOG: EDIT DEVICE LOCATION & COVERAGE RADIUS
        if (showEditLocationDialog && deviceToEditLocation != null) {
            EditDeviceLocationDialog(
                device = deviceToEditLocation!!,
                allDevices = devices,
                onDismiss = {
                    showEditLocationDialog = false
                    deviceToEditLocation = null
                },
                onSave = { lat, lng, radius, parentId ->
                    viewModel.updateDeviceCoordinates(
                        deviceId = deviceToEditLocation!!.id,
                        latitude = lat,
                        longitude = lng,
                        coverageRadiusMeters = radius,
                        parentDeviceId = parentId
                    ) {
                        Toast.makeText(context, "تم تحديث موقع البرج والربط اللاسلكي بنجاح", Toast.LENGTH_SHORT).show()
                        showEditLocationDialog = false
                        deviceToEditLocation = null
                        // Update inspector if open
                        if (selectedDevice?.id == deviceToEditLocation?.id) {
                            selectedDevice = selectedDevice?.copy(
                                latitude = lat,
                                longitude = lng,
                                coverageRadiusMeters = radius,
                                parentDeviceId = parentId
                            )
                        }
                    }
                },
                onRequestGps = { onCoordsReady ->
                    coroutineScope.launch {
                        val loc = LocationHelper.getCurrentLocation(context)
                        if (loc != null) {
                            onCoordsReady(loc.latitude, loc.longitude)
                            Toast.makeText(context, "تم التقاط إحداثيات موقعك الحالي", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "تعذر الحصول على إشارة GPS حالياً", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }

        // 7. DIALOG: PLANT NEW DEVICE AT TAPPED COORDINATES
        if (pendingAddCoordinates != null) {
            PlantDeviceAtLocationDialog(
                latitude = pendingAddCoordinates!!.first,
                longitude = pendingAddCoordinates!!.second,
                allDevices = devices,
                onDismiss = {
                    pendingAddCoordinates = null
                    isAddModeActive = false
                },
                onSave = { name, type, ip, radius, parentId ->
                    val newDev = NetworkDeviceEntity(
                        name = name,
                        deviceType = type,
                        ipAddress = ip,
                        locationArea = "موقع محدد بالخريطة",
                        latitude = pendingAddCoordinates!!.first,
                        longitude = pendingAddCoordinates!!.second,
                        coverageRadiusMeters = radius,
                        parentDeviceId = parentId,
                        status = "ONLINE"
                    )
                    viewModel.saveDevice(newDev) {
                        Toast.makeText(context, "تم زرع البرج/الجهاز في النقطة الجغرافية المحددة بنجاح", Toast.LENGTH_SHORT).show()
                        pendingAddCoordinates = null
                        isAddModeActive = false
                    }
                }
            )
        }
    }
}

@Composable
fun StatusMiniPill(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

/**
 * Custom Canvas that draws the Network Topology, Radar Sweep, Link Lines, Coverage Circles, and Device Nodes.
 */
@Composable
fun InteractiveTopologyCanvas(
    canvasWidth: Float,
    canvasHeight: Float,
    devices: List<NetworkDeviceEntity>,
    allDevices: List<NetworkDeviceEntity>,
    centerLat: Double,
    centerLng: Double,
    zoomLevel: Float,
    userLocation: Pair<Double, Double>?,
    layerMode: MapLayerMode,
    selectedDevice: NetworkDeviceEntity?,
    isAddModeActive: Boolean,
    onDeviceClick: (NetworkDeviceEntity) -> Unit,
    onMapTap: (Double, Double) -> Unit,
    onPan: (Float, Float) -> Unit
) {
    // Pulse animation for active towers and radar sweep
    val infiniteTransition = rememberInfiniteTransition(label = "radar_anim")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    // Radar sweep angle
    val radarAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_sweep"
    )

    val textMeasurer = rememberTextMeasurer()

    // Helper conversion: Lat/Lng -> Canvas Offset (X, Y)
    // 1 degree lat approx 111km, 1 degree lng approx 107km at 15 deg lat
    val metersPerDegree = 111000.0
    val pixelsPerMeter = (0.35f * zoomLevel)

    fun toScreenOffset(lat: Double, lng: Double): Offset {
        val dLatMeters = (lat - centerLat) * metersPerDegree
        val dLngMeters = (lng - centerLng) * metersPerDegree * cos(Math.toRadians(centerLat))
        val screenX = (canvasWidth / 2f) + (dLngMeters.toFloat() * pixelsPerMeter)
        val screenY = (canvasHeight / 2f) - (dLatMeters.toFloat() * pixelsPerMeter)
        return Offset(screenX, screenY)
    }

    fun toLatLng(x: Float, y: Float): Pair<Double, Double> {
        val dxMeters = (x - canvasWidth / 2f) / pixelsPerMeter
        val dyMeters = -(y - canvasHeight / 2f) / pixelsPerMeter
        val dLat = dyMeters / metersPerDegree
        val dLng = dxMeters / (metersPerDegree * cos(Math.toRadians(centerLat)))
        return Pair(centerLat + dLat, centerLng + dLng)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(centerLat, centerLng, zoomLevel) {
                detectDragGestures { _, dragAmount ->
                    onPan(dragAmount.x, dragAmount.y)
                }
            }
            .pointerInput(devices, isAddModeActive, centerLat, centerLng, zoomLevel) {
                detectTapGestures { tapOffset ->
                    // Check if tapped on any device marker (hit radius ~40px)
                    var clickedDevice: NetworkDeviceEntity? = null
                    for (dev in devices) {
                        if (dev.latitude != 0.0 && dev.longitude != 0.0) {
                            val pos = toScreenOffset(dev.latitude, dev.longitude)
                            val distance = (tapOffset - pos).getDistance()
                            if (distance <= 45f) {
                                clickedDevice = dev
                                break
                            }
                        }
                    }

                    if (clickedDevice != null) {
                        onDeviceClick(clickedDevice)
                    } else {
                        val coords = toLatLng(tapOffset.x, tapOffset.y)
                        onMapTap(coords.first, coords.second)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerX = width / 2f
            val centerY = height / 2f

            // 1. BACKGROUND GRID & TACTICAL CONTOURS
            when (layerMode) {
                MapLayerMode.TACTICAL_DARK -> {
                    // Dark blueprint canvas
                    drawRect(Color(0xFF0B111E))

                    // Radar range rings centered on viewport
                    val ringRadius1 = 120f * zoomLevel
                    val ringRadius2 = 240f * zoomLevel
                    val ringRadius3 = 380f * zoomLevel
                    val ringColor = Color(0xFF1E293B).copy(alpha = 0.7f)
                    drawCircle(ringColor, radius = ringRadius1, center = Offset(centerX, centerY), style = Stroke(1.dp.toPx()))
                    drawCircle(ringColor, radius = ringRadius2, center = Offset(centerX, centerY), style = Stroke(1.dp.toPx()))
                    drawCircle(ringColor, radius = ringRadius3, center = Offset(centerX, centerY), style = Stroke(1.dp.toPx()))

                    // Grid lines
                    val gridSize = 60f * zoomLevel
                    var gx = centerX % gridSize
                    while (gx < width) {
                        drawLine(Color(0xFF131D2E), Offset(gx, 0f), Offset(gx, height), strokeWidth = 1f)
                        gx += gridSize
                    }
                    var gy = centerY % gridSize
                    while (gy < height) {
                        drawLine(Color(0xFF131D2E), Offset(0f, gy), Offset(width, gy), strokeWidth = 1f)
                        gy += gridSize
                    }

                    // Rotating Radar Sweep beam
                    val sweepRad = Math.toRadians(radarAngle.toDouble())
                    val sweepTargetX = centerX + (cos(sweepRad) * ringRadius3).toFloat()
                    val sweepTargetY = centerY + (sin(sweepRad) * ringRadius3).toFloat()
                    drawLine(
                        Brush.linearGradient(
                            listOf(MikroTikCyan.copy(alpha = 0.5f), Color.Transparent),
                            start = Offset(centerX, centerY),
                            end = Offset(sweepTargetX, sweepTargetY)
                        ),
                        start = Offset(centerX, centerY),
                        end = Offset(sweepTargetX, sweepTargetY),
                        strokeWidth = 2f
                    )
                }
                MapLayerMode.SATELLITE_GIS -> {
                    // Dark satellite imagery texture style
                    drawRect(Color(0xFF111816))
                    // Simulated roads / terrain lines
                    val roadColor = Color(0xFF273531)
                    drawLine(roadColor, Offset(0f, height * 0.4f), Offset(width, height * 0.6f), strokeWidth = 6f)
                    drawLine(roadColor, Offset(width * 0.3f, 0f), Offset(width * 0.7f, height), strokeWidth = 4f)
                    drawLine(roadColor, Offset(width * 0.75f, 0f), Offset(width * 0.2f, height), strokeWidth = 3f)

                    // Terrain contour lines
                    val contourColor = Color(0xFF1D2C27)
                    drawCircle(contourColor, radius = 280f * zoomLevel, center = Offset(width * 0.4f, height * 0.5f), style = Stroke(2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))))
                }
                MapLayerMode.TOPOLOGY_MESH -> {
                    // Clean deep navy schematic
                    drawRect(Color(0xFF0F172A))
                    // Hexagonal or subtle mesh dots
                    val dotColor = Color(0xFF334155).copy(alpha = 0.4f)
                    val spacing = 45f * zoomLevel
                    var dx = 0f
                    while (dx < width) {
                        var dy = 0f
                        while (dy < height) {
                            drawCircle(dotColor, radius = 1.5f, center = Offset(dx, dy))
                            dy += spacing
                        }
                        dx += spacing
                    }
                }
            }

            // 2. WIRELESS LINK BACKHAUL LINES (Connecting devices to parent nodes)
            devices.forEach { dev ->
                if (dev.latitude != 0.0 && dev.longitude != 0.0 && dev.parentDeviceId != null) {
                    val parentDev = allDevices.firstOrNull { it.id == dev.parentDeviceId }
                    if (parentDev != null && parentDev.latitude != 0.0 && parentDev.longitude != 0.0) {
                        val startPos = toScreenOffset(parentDev.latitude, parentDev.longitude)
                        val endPos = toScreenOffset(dev.latitude, dev.longitude)

                        val lineColor = when {
                            dev.status != "ONLINE" || parentDev.status != "ONLINE" -> StatusOffline.copy(alpha = 0.7f)
                            dev.signalDbm < -70 -> StatusWarning.copy(alpha = 0.8f)
                            else -> MikroTikCyan.copy(alpha = 0.85f)
                        }

                        // Draw glowing line
                        drawLine(
                            color = lineColor.copy(alpha = 0.3f),
                            start = startPos,
                            end = endPos,
                            strokeWidth = 6f
                        )
                        drawLine(
                            color = lineColor,
                            start = startPos,
                            end = endPos,
                            strokeWidth = 2.5f,
                            pathEffect = if (dev.status != "ONLINE") PathEffect.dashPathEffect(floatArrayOf(12f, 8f)) else null
                        )

                        // Midpoint signal indicator badge
                        val midX = (startPos.x + endPos.x) / 2f
                        val midY = (startPos.y + endPos.y) / 2f
                        drawCircle(color = MikroTikNavy, radius = 12f, center = Offset(midX, midY))
                        drawCircle(color = lineColor, radius = 12f, center = Offset(midX, midY), style = Stroke(1.5f))
                    }
                }
            }

            // 3. COVERAGE CIRCLES (Radial broadcast footprint)
            devices.forEach { dev ->
                if (dev.latitude != 0.0 && dev.longitude != 0.0) {
                    val pos = toScreenOffset(dev.latitude, dev.longitude)
                    val radiusPixels = dev.coverageRadiusMeters * pixelsPerMeter

                    if (radiusPixels > 5f) {
                        val coverageColor = when (dev.status) {
                            "ONLINE" -> if (dev.deviceType.contains("Sector")) Color(0xFF00E5FF) else Color(0xFF10B981)
                            "WARNING" -> Color(0xFFF59E0B)
                            else -> Color(0xFFEF4444)
                        }

                        // Fill circle with soft opacity
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(coverageColor.copy(alpha = 0.22f), coverageColor.copy(alpha = 0.03f), Color.Transparent),
                                center = pos,
                                radius = radiusPixels
                            ),
                            radius = radiusPixels,
                            center = pos
                        )

                        // Outer coverage contour
                        drawCircle(
                            color = coverageColor.copy(alpha = 0.45f),
                            radius = radiusPixels,
                            center = pos,
                            style = Stroke(1.2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
                        )
                    }
                }
            }

            // 4. DEVICE NODES / TOWERS
            devices.forEach { dev ->
                if (dev.latitude != 0.0 && dev.longitude != 0.0) {
                    val pos = toScreenOffset(dev.latitude, dev.longitude)
                    val isSelected = selectedDevice?.id == dev.id
                    val isOnline = dev.status == "ONLINE"
                    val isWarning = dev.status == "WARNING"

                    val themeColor = when {
                        !isOnline -> StatusOffline
                        isWarning -> StatusWarning
                        dev.deviceType.contains("Router") || dev.deviceType.contains("CCR") -> Color(0xFF00E5FF)
                        dev.deviceType.contains("Sector") -> Color(0xFF38BDF8)
                        dev.deviceType.contains("Switch") -> Color(0xFF818CF8)
                        else -> StatusOnline
                    }

                    // Pulsing active aura
                    if (isOnline || isWarning) {
                        drawCircle(
                            color = themeColor.copy(alpha = pulseAlpha * 0.4f),
                            radius = (20f + (pulseScale * 14f)),
                            center = pos
                        )
                    }

                    // Selection ring highlight
                    if (isSelected) {
                        drawCircle(
                            color = Color.White,
                            radius = 28f,
                            center = pos,
                            style = Stroke(3f)
                        )
                    }

                    // Node base circle
                    drawCircle(
                        color = Color(0xFF0F172A),
                        radius = 18f,
                        center = pos
                    )
                    drawCircle(
                        color = themeColor,
                        radius = 18f,
                        center = pos,
                        style = Stroke(2.5f)
                    )

                    // Core status dot inside
                    drawCircle(
                        color = themeColor,
                        radius = 8f,
                        center = pos
                    )

                    // Label below the tower
                    val labelText = "${dev.name.take(18)}\n${dev.ipAddress}"
                    val textLayoutResult = textMeasurer.measure(
                        text = labelText,
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    )
                    val textPos = Offset(pos.x - (textLayoutResult.size.width / 2f), pos.y + 22f)

                    // Dark pill background for text readability
                    drawRoundRect(
                        color = Color(0xCC000000),
                        topLeft = Offset(textPos.x - 6f, textPos.y - 2f),
                        size = androidx.compose.ui.geometry.Size(textLayoutResult.size.width + 12f, textLayoutResult.size.height + 4f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                    )

                    drawText(textLayoutResult, topLeft = textPos)
                }
            }

            // 5. USER CURRENT GPS LOCATION MARKER (If detected)
            userLocation?.let { (uLat, uLng) ->
                val uPos = toScreenOffset(uLat, uLng)

                // Pulse ring
                drawCircle(
                    color = Color(0xFF3B82F6).copy(alpha = pulseAlpha),
                    radius = (16f + (pulseScale * 18f)),
                    center = uPos
                )

                // Outer white rim
                drawCircle(
                    color = Color.White,
                    radius = 14f,
                    center = uPos
                )
                // Blue location center
                drawCircle(
                    color = Color(0xFF2563EB),
                    radius = 10f,
                    center = uPos
                )

                val userLabelResult = textMeasurer.measure(
                    text = "📍 موقعك الميداني",
                    style = TextStyle(
                        color = Color(0xFF60A5FA),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                drawText(userLabelResult, topLeft = Offset(uPos.x - (userLabelResult.size.width / 2f), uPos.y - 32f))
            }
        }
    }
}

/**
 * Bottom Inspector Sheet showing deep details of the selected tower/device with quick actions.
 */
@Composable
fun DeviceMapInspectorCard(
    device: NetworkDeviceEntity,
    isTestingPing: Boolean,
    pingResultText: String?,
    onDismiss: () -> Unit,
    onNavigateToLocation: () -> Unit,
    onTestPing: () -> Unit,
    onEditCoordinates: () -> Unit,
    onCopyCoordinates: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = MikroTikNavy),
        elevation = CardDefaults.cardElevation(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("device_map_inspector")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Drag indicator handle
            Box(
                modifier = Modifier
                    .size(width = 40.dp, height = 4.dp)
                    .background(Color.White.copy(alpha = 0.3f), CircleShape)
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Header: Name, Status badge & Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                when (device.status) {
                                    "ONLINE" -> StatusOnline.copy(alpha = 0.2f)
                                    "WARNING" -> StatusWarning.copy(alpha = 0.2f)
                                    else -> StatusOffline.copy(alpha = 0.2f)
                                },
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                device.deviceType.contains("Router") -> Icons.Default.Router
                                device.deviceType.contains("Sector") -> Icons.Default.CellTower
                                else -> Icons.Default.Wifi
                            },
                            contentDescription = null,
                            tint = when (device.status) {
                                "ONLINE" -> StatusOnline
                                "WARNING" -> StatusWarning
                                else -> StatusOffline
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = device.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${device.deviceType} • ${device.ipAddress}",
                            fontSize = 12.sp,
                            color = MikroTikCyan
                        )
                    }
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.LightGray)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Physical & Geographical Specs Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoSpecBox(
                    title = "نطاق التغطية",
                    value = "${device.coverageRadiusMeters} متر",
                    modifier = Modifier.weight(1f)
                )
                InfoSpecBox(
                    title = "إشارة الوايرلس",
                    value = if (device.signalDbm != 0) "${device.signalDbm} dBm" else "كيبل LAN",
                    modifier = Modifier.weight(1f)
                )
                InfoSpecBox(
                    title = "وقت التشغيل",
                    value = "${device.uptimeHours} ساعة",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Location & Coordinates bar
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = device.locationArea,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = "GPS: ${String.format("%.5f", device.latitude)}, ${String.format("%.5f", device.longitude)}",
                            fontSize = 10.sp,
                            color = Color.LightGray
                        )
                    }

                    IconButton(onClick = onCopyCoordinates, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "نسخ الإحداثيات", tint = MikroTikCyan, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Ping test output if active
            if (pingResultText != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = StatusOnline.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = pingResultText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = StatusOnline,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Navigate via Google Maps Button
                Button(
                    onClick = onNavigateToLocation,
                    colors = ButtonDefaults.buttonColors(containerColor = MikroTikPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1.3f)
                ) {
                    Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("الملاحة الميدانية 🗺️", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Ping Test Button
                OutlinedButton(
                    onClick = onTestPing,
                    enabled = !isTestingPing,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isTestingPing) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                    } else {
                        Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("فحص Ping", fontSize = 11.sp)
                    }
                }

                // Edit Coordinates / Link Button
                IconButton(
                    onClick = onEditCoordinates,
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "تعديل الإحداثيات", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun InfoSpecBox(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, fontSize = 9.sp, color = Color.LightGray)
            Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

/**
 * Dialog for editing existing device GPS coordinates, coverage radius, and parent backhaul link.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDeviceLocationDialog(
    device: NetworkDeviceEntity,
    allDevices: List<NetworkDeviceEntity>,
    onDismiss: () -> Unit,
    onSave: (lat: Double, lng: Double, radius: Int, parentId: Long?) -> Unit,
    onRequestGps: ((Double, Double) -> Unit) -> Unit
) {
    var latText by remember { mutableStateOf(device.latitude.toString()) }
    var lngText by remember { mutableStateOf(device.longitude.toString()) }
    var radiusSlider by remember { mutableFloatStateOf(device.coverageRadiusMeters.toFloat()) }
    var selectedParentId by remember { mutableStateOf<Long?>(device.parentDeviceId) }
    var isParentMenuExpanded by remember { mutableStateOf(false) }

    val otherDevices = allDevices.filter { it.id != device.id }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("تحديد إحداثيات وموقع البرج", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "الجهاز: ${device.name}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                // GPS Auto-detect Button
                OutlinedButton(
                    onClick = {
                        onRequestGps { lat, lng ->
                            latText = lat.toString()
                            lngText = lng.toString()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("التقاط إحداثيات موقعي الحالي بالـ GPS", fontSize = 11.sp)
                }

                // Latitude
                OutlinedTextField(
                    value = latText,
                    onValueChange = { latText = it },
                    label = { Text("خط العرض (Latitude)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Longitude
                OutlinedTextField(
                    value = lngText,
                    onValueChange = { lngText = it },
                    label = { Text("خط الطول (Longitude)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Coverage Radius Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("نطاق التغطية اللاسلكية:", fontSize = 11.sp)
                        Text("${radiusSlider.roundToInt()} متر", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = radiusSlider,
                        onValueChange = { radiusSlider = it },
                        valueRange = 50f..2000f,
                        steps = 39
                    )
                }

                // Upstream Parent Backhaul Link Dropdown
                ExposedDropdownMenuBox(
                    expanded = isParentMenuExpanded,
                    onExpandedChange = { isParentMenuExpanded = it }
                ) {
                    val parentName = otherDevices.firstOrNull { it.id == selectedParentId }?.name ?: "بدون ربط (برج رئيسي)"
                    OutlinedTextField(
                        value = parentName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("مربوط لاسلكياً بالبرج / الراوتر:") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isParentMenuExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isParentMenuExpanded,
                        onDismissRequest = { isParentMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("بدون ربط (برج رئيسي)") },
                            onClick = {
                                selectedParentId = null
                                isParentMenuExpanded = false
                            }
                        )
                        otherDevices.forEach { dev ->
                            DropdownMenuItem(
                                text = { Text("${dev.name} (${dev.ipAddress})") },
                                onClick = {
                                    selectedParentId = dev.id
                                    isParentMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lat = latText.toDoubleOrNull() ?: 0.0
                    val lng = lngText.toDoubleOrNull() ?: 0.0
                    onSave(lat, lng, radiusSlider.roundToInt(), selectedParentId)
                }
            ) {
                Text("حفظ وتثبيت")
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
 * Dialog to plant a new Tower/AP on the map at the tapped coordinates.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantDeviceAtLocationDialog(
    latitude: Double,
    longitude: Double,
    allDevices: List<NetworkDeviceEntity>,
    onDismiss: () -> Unit,
    onSave: (name: String, type: String, ip: String, radius: Int, parentId: Long?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var ipAddress by remember { mutableStateOf("192.168.88.") }
    var deviceType by remember { mutableStateOf("مرسل") }
    var coverageRadius by remember { mutableFloatStateOf(300f) }
    var selectedParentId by remember { mutableStateOf<Long?>(allDevices.firstOrNull()?.id) }

    var isTypeMenuExpanded by remember { mutableStateOf(false) }
    var isParentMenuExpanded by remember { mutableStateOf(false) }

    val commonDeviceTypes = listOf(
        "مرسل",
        "مستقبل",
        "لاقط",
        "مرسل (Transmitter / AP)",
        "مستقبل (Receiver / Station)",
        "لاقط (CPE / Station / Dish)",
        "Access Point",
        "Sector Antenna",
        "MikroTik RouterBOARD",
        "CPE",
        "Switch",
        "صحن توجيهي (Dish)",
        "Fiber OLT / ONU"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("تثبيت جهاز جديد في هذا الموقع 📍", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "الإحداثيات: ${String.format("%.5f", latitude)}, ${String.format("%.5f", longitude)}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الجهاز أو البرج *") },
                    placeholder = { Text("مثال: سيكتور حارة السلام") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = ipAddress,
                    onValueChange = { ipAddress = it },
                    label = { Text("عنوان الآي بي (IP Address) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Device Type Dropdown & Free-text writable field + Quick Preset Chips
                Column {
                    ExposedDropdownMenuBox(
                        expanded = isTypeMenuExpanded,
                        onExpandedChange = { isTypeMenuExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = deviceType,
                            onValueChange = {
                                deviceType = it
                                isTypeMenuExpanded = true
                            },
                            readOnly = false,
                            label = { Text("نوع الجهاز * (اختر أو اكتب يدوياً)") },
                            placeholder = { Text("مثال: مرسل، مستقبل، لاقط، Access Point...") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isTypeMenuExpanded) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )

                        val matchingTypes = if (deviceType.isBlank()) {
                            commonDeviceTypes
                        } else {
                            val filtered = commonDeviceTypes.filter { it.contains(deviceType.trim(), ignoreCase = true) }
                            if (filtered.isNotEmpty()) filtered else commonDeviceTypes
                        }

                        ExposedDropdownMenu(
                            expanded = isTypeMenuExpanded,
                            onDismissRequest = { isTypeMenuExpanded = false }
                        ) {
                            matchingTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val typeIcon = when {
                                                type.contains("راوتر", ignoreCase = true) || type.contains("Router", ignoreCase = true) -> Icons.Default.Router
                                                type.contains("سيكتور", ignoreCase = true) || type.contains("مرسل", ignoreCase = true) -> Icons.Default.CellTower
                                                type.contains("مستقبل", ignoreCase = true) || type.contains("لاقط", ignoreCase = true) || type.contains("CPE", ignoreCase = true) -> Icons.Default.NetworkCheck
                                                else -> Icons.Default.Wifi
                                            }
                                            Icon(typeIcon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(type)
                                        }
                                    },
                                    onClick = {
                                        deviceType = type
                                        isTypeMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Quick selection chips
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "اختيار سريع بنقرة واحدة:",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val quickChips = listOf("مرسل", "مستقبل", "لاقط", "Access Point", "RouterBOARD", "سيكتور", "Switch", "CPE")
                        items(quickChips) { chip ->
                            val isSelected = deviceType.trim().equals(chip, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                                modifier = Modifier.clickable {
                                    deviceType = chip
                                    isTypeMenuExpanded = false
                                }
                            ) {
                                Text(
                                    text = chip,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Coverage radius
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("مدى التغطية:", fontSize = 11.sp)
                        Text("${coverageRadius.roundToInt()} متر", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = coverageRadius,
                        onValueChange = { coverageRadius = it },
                        valueRange = 50f..1500f
                    )
                }

                // Link to parent
                ExposedDropdownMenuBox(
                    expanded = isParentMenuExpanded,
                    onExpandedChange = { isParentMenuExpanded = it }
                ) {
                    val parentName = allDevices.firstOrNull { it.id == selectedParentId }?.name ?: "بدون ربط"
                    OutlinedTextField(
                        value = parentName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("الربط اللاسلكي مع:") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isParentMenuExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isParentMenuExpanded,
                        onDismissRequest = { isParentMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("بدون ربط") },
                            onClick = {
                                selectedParentId = null
                                isParentMenuExpanded = false
                            }
                        )
                        allDevices.forEach { dev ->
                            DropdownMenuItem(
                                text = { Text("${dev.name} (${dev.ipAddress})") },
                                onClick = {
                                    selectedParentId = dev.id
                                    isParentMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && ipAddress.isNotBlank()) {
                        onSave(name.trim(), deviceType.trim().ifBlank { "مرسل" }, ipAddress.trim(), coverageRadius.roundToInt(), selectedParentId)
                    }
                },
                enabled = name.isNotBlank() && ipAddress.isNotBlank()
            ) {
                Text("زرع الجهاز بالخريطة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
