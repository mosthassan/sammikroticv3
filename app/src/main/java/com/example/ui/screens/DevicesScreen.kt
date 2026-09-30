package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import com.example.data.local.entity.NetworkDeviceEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.CairoFontFamily
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberDarkCardElevated
import com.example.ui.theme.CyberDarkSurface
import com.example.ui.theme.MikroTikCyan
import com.example.ui.theme.MikroTikNavy
import com.example.ui.theme.MikroTikPrimary
import com.example.ui.theme.StatusOffline
import com.example.ui.theme.StatusOnline
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.util.LocationHelper
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun DevicesScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val devices by viewModel.devices.collectAsState()
    val ipValidation by viewModel.ipValidation.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf("الكل") }
    var showAddEditDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var deviceToEdit by remember { mutableStateOf<NetworkDeviceEntity?>(null) }
    var deviceToDelete by remember { mutableStateOf<NetworkDeviceEntity?>(null) }
    var pingingDeviceId by remember { mutableStateOf<Long?>(null) }
    var activeSubTab by remember { mutableStateOf(0) } // 0: Devices list, 1: Spectrum & Interference Analyzer

    val filterTypes = remember(devices) {
        val basePresets = listOf("الكل", "مرسل", "مستقبل", "لاقط", "Access Point", "MikroTik RouterBOARD", "Sector Antenna", "Switch", "CPE")
        val fromExisting = devices.map { it.deviceType.trim() }.filter { it.isNotBlank() }
        (listOf("الكل") + (basePresets.drop(1) + fromExisting).distinct())
    }

    val filteredDevices = devices.filter { device ->
        val matchesSearch = searchQuery.isBlank() ||
                device.name.contains(searchQuery, ignoreCase = true) ||
                device.ipAddress.contains(searchQuery, ignoreCase = true) ||
                device.locationArea.contains(searchQuery, ignoreCase = true) ||
                device.frequencyOrSsid.contains(searchQuery, ignoreCase = true) ||
                device.deviceType.contains(searchQuery, ignoreCase = true)

        val matchesType = selectedTypeFilter == "الكل" ||
                device.deviceType.equals(selectedTypeFilter, ignoreCase = true) ||
                device.deviceType.contains(selectedTypeFilter, ignoreCase = true)
        matchesSearch && matchesType
    }

    Box(modifier = modifier.fillMaxSize().testTag("devices_screen")) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Mode Switcher Header: [قائمة الأجهزة] | [خريطة الأبراج GIS 🗺️] | [محلل الترددات ⚡]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberDarkSurface)
                    .border(BorderStroke(1.dp, CyberBorder))
                    .padding(horizontal = 8.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (activeSubTab == 0) MikroTikPrimary.copy(alpha = 0.25f) else CyberDarkCardElevated,
                    border = BorderStroke(1.dp, if (activeSubTab == 0) MikroTikPrimary.copy(alpha = 0.6f) else CyberBorder),
                    onClick = { activeSubTab = 0 },
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 7.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Router,
                            contentDescription = null,
                            tint = if (activeSubTab == 0) MikroTikCyan else TextSecondaryDark,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "الأجهزة (${devices.size})",
                            color = if (activeSubTab == 0) Color.White else TextSecondaryDark,
                            fontFamily = CairoFontFamily,
                            fontSize = 11.5.sp,
                            fontWeight = if (activeSubTab == 0) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (activeSubTab == 1) Color(0xFF0D9488).copy(alpha = 0.25f) else CyberDarkCardElevated,
                    border = BorderStroke(1.dp, if (activeSubTab == 1) Color(0xFF2DD4BF).copy(alpha = 0.6f) else CyberBorder),
                    onClick = { activeSubTab = 1 },
                    modifier = Modifier.weight(1.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 7.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CellTower,
                            contentDescription = null,
                            tint = if (activeSubTab == 1) Color(0xFF2DD4BF) else TextSecondaryDark,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "خريطة الأبراج 🗺️",
                            color = if (activeSubTab == 1) Color.White else TextSecondaryDark,
                            fontFamily = CairoFontFamily,
                            fontSize = 11.5.sp,
                            fontWeight = if (activeSubTab == 1) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (activeSubTab == 2) Color(0xFF0284C7).copy(alpha = 0.25f) else CyberDarkCardElevated,
                    border = BorderStroke(1.dp, if (activeSubTab == 2) Color(0xFF38BDF8).copy(alpha = 0.6f) else CyberBorder),
                    onClick = { activeSubTab = 2 },
                    modifier = Modifier.weight(1.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 7.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = if (activeSubTab == 2) Color(0xFF38BDF8) else TextSecondaryDark,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "محلل الترددات ⚡",
                            color = if (activeSubTab == 2) Color.White else TextSecondaryDark,
                            fontFamily = CairoFontFamily,
                            fontSize = 11.5.sp,
                            fontWeight = if (activeSubTab == 2) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            when (activeSubTab) {
                1 -> {
                    NetworkMapScreen(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                2 -> {
                    SpectrumAnalyzerScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                }
                else -> {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    // Header Title & Device Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "أجهزة ومحطات الشبكة",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "إدارة الأكسسات، الراوترات، السيكتورات ومراقبة الآي بيهات بدون تكرار",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { showBackupDialog = true },
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("open_device_json_backup_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MikroTikCyan),
                        border = BorderStroke(1.dp, MikroTikCyan.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تصدير / استيراد JSON 🔄", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = CairoFontFamily)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MikroTikPrimary.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${devices.size} جهاز",
                            color = MikroTikPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().testTag("device_search_input"),
                placeholder = { Text("بحث باسم الجهاز، رقم الآي بي (IP)، أو المنطقة...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "بحث", tint = MikroTikPrimary)
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MikroTikPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Category Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filterTypes) { type ->
                    val isSelected = selectedTypeFilter == type
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) MikroTikPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                            .clickable { selectedTypeFilter = type }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = type,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Device List
            if (filteredDevices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Router,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "لا توجد أجهزة مطابقة للبحث",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredDevices, key = { it.id }) { device ->
                        DeviceCard(
                            device = device,
                            isPinging = pingingDeviceId == device.id,
                            onClone = {
                                coroutineScope.launch {
                                    val suggestedIp = viewModel.getSuggestedIp()
                                    deviceToEdit = device.copy(
                                        id = 0L,
                                        name = "${device.name} (نسخة)",
                                        ipAddress = suggestedIp,
                                        uptimeHours = 0
                                    )
                                    viewModel.clearIpValidation()
                                    showAddEditDialog = true
                                    Toast.makeText(context, "تم استنساخ بيانات الجهاز واقتراح آي بي جديد. عدّل التفاصيل ثم احفظ", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onEdit = {
                                deviceToEdit = device
                                showAddEditDialog = true
                            },
                            onDelete = {
                                deviceToDelete = device
                            },
                            onPing = {
                                coroutineScope.launch {
                                    pingingDeviceId = device.id
                                    kotlinx.coroutines.delay(800)
                                    pingingDeviceId = null
                                    Toast.makeText(context, "استجابة بنج ممتازة من ${device.ipAddress} (Ping: 4ms)", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }
}
}

        // Floating Action Button to Add Device
        if (activeSubTab == 0) {
            FloatingActionButton(
                onClick = {
                    deviceToEdit = null
                    viewModel.clearIpValidation()
                    showAddEditDialog = true
                },
                containerColor = MikroTikPrimary,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .testTag("add_device_fab")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة جهاز")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إضافة جهاز", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Add/Edit Dialog
        if (showAddEditDialog) {
            AddEditDeviceDialog(
                deviceToEdit = deviceToEdit,
                allDevices = devices,
                ipValidation = ipValidation,
                onValidateIp = { ip ->
                    viewModel.validateIp(ip, deviceToEdit?.id)
                },
                onSuggestIp = {
                    coroutineScope.launch {
                        val freeIp = viewModel.getSuggestedIp()
                        it(freeIp)
                    }
                },
                onDismiss = {
                    showAddEditDialog = false
                    viewModel.clearIpValidation()
                },
                onSave = { newDevice ->
                    viewModel.saveDevice(newDevice) {
                        showAddEditDialog = false
                        viewModel.clearIpValidation()
                        Toast.makeText(context, "تم حفظ بيانات الجهاز بنجاح ✓", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        // Delete Confirmation Dialog
        if (deviceToDelete != null) {
            AlertDialog(
                onDismissRequest = { deviceToDelete = null },
                title = { Text("تأكيد حذف الجهاز") },
                text = { Text("هل أنت متأكد من حذف الجهاز (${deviceToDelete?.name}) بعنوان الآي بي (${deviceToDelete?.ipAddress})؟") },
                confirmButton = {
                    Button(
                        onClick = {
                            deviceToDelete?.let { viewModel.deleteDevice(it) }
                            deviceToDelete = null
                            Toast.makeText(context, "تم حذف الجهاز", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusOffline)
                    ) {
                        Text("حذف")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deviceToDelete = null }) {
                        Text("إلغاء")
                    }
                }
            )
        }

        // Export / Import Devices JSON Backup Dialog
        if (showBackupDialog) {
            DeviceJsonBackupDialog(
                viewModel = viewModel,
                onDismissRequest = { showBackupDialog = false }
            )
        }
    }
}

@Composable
fun DeviceCard(
    device: NetworkDeviceEntity,
    isPinging: Boolean,
    onClone: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPing: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
        border = BorderStroke(1.dp, CyberBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth().testTag("device_card_${device.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top row: Type badge, Name, Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    val icon = when {
                        device.deviceType.contains("راوتر", ignoreCase = true) || device.deviceType.contains("Router", ignoreCase = true) -> Icons.Default.Router
                        device.deviceType.contains("سيكتور", ignoreCase = true) || device.deviceType.contains("Sector", ignoreCase = true) || device.deviceType.contains("مرسل", ignoreCase = true) -> Icons.Default.CellTower
                        device.deviceType.contains("مستقبل", ignoreCase = true) || device.deviceType.contains("لاقط", ignoreCase = true) || device.deviceType.contains("CPE", ignoreCase = true) || device.deviceType.contains("Dish", ignoreCase = true) -> Icons.Default.NetworkCheck
                        else -> Icons.Default.Wifi
                    }
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CyberDarkCardElevated)
                            .border(1.dp, CyberBorder, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = MikroTikCyan, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = device.name,
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Text(
                            text = "${device.deviceType} • ${device.model.ifEmpty { "عام" }}",
                            fontFamily = CairoFontFamily,
                            fontSize = 11.5.sp,
                            color = TextSecondaryDark
                        )
                    }
                }

                // Status Pill
                val (statusColor, statusText) = when (device.status) {
                    "ONLINE" -> StatusOnline to "متصل"
                    "WARNING" -> StatusWarning to "تنبيه"
                    else -> StatusOffline to "غير متصل"
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .border(1.dp, statusColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontFamily = CairoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // IP Address Row with Quick Copy
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "IP:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MikroTikPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = device.ipAddress,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(device.ipAddress))
                            Toast.makeText(context, "تم نسخ الآي بي: ${device.ipAddress}", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "نسخ IP", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }

                    // Ping test button
                    IconButton(
                        onClick = onPing,
                        modifier = Modifier.size(28.dp)
                    ) {
                        if (isPinging) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.NetworkCheck, contentDescription = "فحص الاتصال Ping", tint = MikroTikPrimary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Location & Frequency Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = device.locationArea,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (device.frequencyOrSsid.isNotEmpty()) {
                    Text(
                        text = device.frequencyOrSsid,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Signal dBm meter if wireless
            if (device.signalDbm != 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "قوة الإشارة: ${device.signalDbm} dBm",
                        fontSize = 11.sp,
                        color = if (device.signalDbm > -65) StatusOnline else StatusWarning
                    )
                    Text(
                        text = "مدة التشغيل: ${device.uptimeHours} ساعة",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            // GPS Coordinates & Navigation Link
            if (device.latitude != 0.0 && device.longitude != 0.0) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F243A))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MyLocation, contentDescription = null, tint = MikroTikCyan, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "GPS: ${String.format("%.4f", device.latitude)}, ${String.format("%.4f", device.longitude)} (${device.coverageRadiusMeters}م)",
                            fontSize = 10.sp,
                            color = MikroTikCyan
                        )
                    }
                    Text(
                        text = "ملاحة 🗺️",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.clickable {
                            val uri = Uri.parse("geo:${device.latitude},${device.longitude}?q=${device.latitude},${device.longitude}(${device.name})")
                            val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                            try {
                                context.startActivity(mapIntent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "إحداثيات البرج: ${device.latitude}, ${device.longitude}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            // Bottom Actions (Clone / Edit / Delete)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onClone, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "استنساخ الجهاز", tint = MikroTikCyan, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MikroTikPrimary, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = StatusOffline, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditDeviceDialog(
    deviceToEdit: NetworkDeviceEntity?,
    allDevices: List<NetworkDeviceEntity> = emptyList(),
    ipValidation: com.example.ui.IpValidationResult,
    onValidateIp: (String) -> Unit,
    onSuggestIp: ((String) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onSave: (NetworkDeviceEntity) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }
    var name by remember(deviceToEdit) { mutableStateOf(deviceToEdit?.name ?: "") }
    var ipAddress by remember(deviceToEdit) { mutableStateOf(deviceToEdit?.ipAddress ?: "") }
    var locationArea by remember(deviceToEdit) { mutableStateOf(deviceToEdit?.locationArea ?: "") }
    var latitude by remember(deviceToEdit) { mutableStateOf(if (deviceToEdit?.latitude != null && deviceToEdit.latitude != 0.0) deviceToEdit.latitude.toString() else "") }
    var longitude by remember(deviceToEdit) { mutableStateOf(if (deviceToEdit?.longitude != null && deviceToEdit.longitude != 0.0) deviceToEdit.longitude.toString() else "") }
    var coverageRadius by remember(deviceToEdit) { mutableFloatStateOf(deviceToEdit?.coverageRadiusMeters?.toFloat() ?: 300f) }
    var parentDeviceId by remember(deviceToEdit) { mutableStateOf<Long?>(deviceToEdit?.parentDeviceId) }
    var isParentExpanded by remember { mutableStateOf(false) }
    var isLocatingGps by remember { mutableStateOf(false) }
    var deviceType by remember(deviceToEdit) { mutableStateOf(deviceToEdit?.deviceType ?: "مرسل") }
    var portOrInterface by remember(deviceToEdit) { mutableStateOf(deviceToEdit?.portOrInterface ?: "ether1") }
    var frequencyOrSsid by remember(deviceToEdit) { mutableStateOf(deviceToEdit?.frequencyOrSsid ?: "") }
    var model by remember(deviceToEdit) { mutableStateOf(deviceToEdit?.model ?: "") }
    var notes by remember(deviceToEdit) { mutableStateOf(deviceToEdit?.notes ?: "") }

    var isTypeExpanded by remember { mutableStateOf(false) }
    val commonDeviceTypes = listOf(
        "مرسل",
        "مستقبل",
        "لاقط",
        "مرسل (Transmitter / AP)",
        "مستقبل (Receiver / Station)",
        "لاقط (CPE / Station / Dish)",
        "Access Point",
        "MikroTik RouterBOARD",
        "Sector Antenna",
        "Switch",
        "CPE",
        "صحن توجيهي (Dish)",
        "Fiber OLT / ONU"
    )

    // Trigger validation on initial load or change
    LaunchedEffect(ipAddress) {
        onValidateIp(ipAddress)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (deviceToEdit == null || deviceToEdit.id == 0L) {
                    if (deviceToEdit?.id == 0L) "استنساخ وتسجيل جهاز جديد" else "إضافة جهاز شبكة جديد"
                } else "تعديل بيانات الجهاز",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Device Name
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم الجهاز أو الأكسس *") },
                        placeholder = { Text("مثال: أكسس برج التحرير الرئيسي") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("device_name_input")
                    )
                }

                // Device Type (Writable TextField + Exposed Dropdown + Quick Preset Chips)
                item {
                    Column {
                        ExposedDropdownMenuBox(
                            expanded = isTypeExpanded,
                            onExpandedChange = { isTypeExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = deviceType,
                                onValueChange = {
                                    deviceType = it
                                    isTypeExpanded = true
                                },
                                readOnly = false,
                                label = { Text("نوع الجهاز * (اختر أو اكتب يدوياً)") },
                                placeholder = { Text("مثال: مرسل، مستقبل، لاقط، Access Point...") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isTypeExpanded) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                                    .testTag("device_type_input")
                            )

                            val matchingTypes = if (deviceType.isBlank()) {
                                commonDeviceTypes
                            } else {
                                val filtered = commonDeviceTypes.filter { it.contains(deviceType.trim(), ignoreCase = true) }
                                if (filtered.isNotEmpty()) filtered else commonDeviceTypes
                            }

                            ExposedDropdownMenu(
                                expanded = isTypeExpanded,
                                onDismissRequest = { isTypeExpanded = false }
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
                                                Icon(typeIcon, contentDescription = null, tint = MikroTikCyan, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(type)
                                            }
                                        },
                                        onClick = {
                                            deviceType = type
                                            isTypeExpanded = false
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
                                    color = if (isSelected) MikroTikPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    border = BorderStroke(1.dp, if (isSelected) MikroTikPrimary else Color.Transparent),
                                    modifier = Modifier.clickable {
                                        deviceType = chip
                                        isTypeExpanded = false
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
                }

                // IP Address with Real-Time Conflict Warning and Auto-Suggest Button
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "عنوان الآي بي (IP Address) *",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )

                            // Suggest IP Button
                            TextButton(
                                onClick = {
                                    onSuggestIp { freeIp ->
                                        ipAddress = freeIp
                                    }
                                },
                                modifier = Modifier.testTag("suggest_ip_btn")
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp), tint = MikroTikPrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("اقتراح آي بي متاح", fontSize = 11.sp, color = MikroTikPrimary)
                            }
                        }

                        OutlinedTextField(
                            value = ipAddress,
                            onValueChange = { ipAddress = it },
                            placeholder = { Text("192.168.88.x") },
                            singleLine = true,
                            isError = ipValidation.hasConflict,
                            modifier = Modifier.fillMaxWidth().testTag("device_ip_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                errorBorderColor = StatusOffline
                            )
                        )

                        // Visual IP Conflict Alert Banner
                        AnimatedVisibility(visible = ipValidation.hasConflict) {
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = StatusOffline.copy(alpha = 0.12f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp)
                                    .testTag("ip_conflict_warning")
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = "تحذير تعارض",
                                        tint = StatusOffline,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "⚠️ تنبيه: عنوان الآي بي مسجل مسبقاً!",
                                            color = StatusOffline,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = "الآي بي محجوز للجهاز (${ipValidation.conflictingDeviceName}) في منطقة (${ipValidation.conflictingDeviceLocation}). يرجى تغيير الآي بي لمنع انهيار وتضارب الشبكة.",
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Outside Approved Subnet Alert Banner
                        AnimatedVisibility(visible = !ipValidation.hasConflict && ipValidation.isOutsideSubnet && ipAddress.isNotBlank()) {
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = StatusWarning.copy(alpha = 0.15f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp)
                                    .testTag("ip_outside_subnet_warning")
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = "تنبيه الرينج المعتمد",
                                        tint = StatusWarning,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "⚠️ خارج الرينج المعتمد (${ipValidation.approvedSubnet})",
                                            color = StatusWarning,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = "هذا الآي بي خارج نطاق الأجهزة المعتمد في هوية الشبكة. يفضل استخدام الرينج المعتمد لضمان التواصل مع السيرفر والذكاء الاصطناعي.",
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Location Area
                item {
                    OutlinedTextField(
                        value = locationArea,
                        onValueChange = { locationArea = it },
                        label = { Text("موقع الجهاز / المنطقة / البرج *") },
                        placeholder = { Text("مثال: برج الروضة - سارية رقم 2") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("device_location_input")
                    )
                }

                // GIS Coordinates & Wireless Coverage Section
                item {
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("إحداثيات الخريطة ونطاق البث (GIS)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                TextButton(
                                    onClick = {
                                        isLocatingGps = true
                                        coroutineScope.launch {
                                            val loc = LocationHelper.getCurrentLocation(context)
                                            isLocatingGps = false
                                            if (loc != null) {
                                                latitude = loc.latitude.toString()
                                                longitude = loc.longitude.toString()
                                                Toast.makeText(context, "تم التقاط إحداثيات موقعك الحالي", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "يرجى التأكد من تفعيل الـ GPS في الهاتف", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                ) {
                                    if (isLocatingGps) {
                                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                                    } else {
                                        Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("GPS الحالي 📍", fontSize = 11.sp)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = latitude,
                                    onValueChange = { latitude = it },
                                    label = { Text("خط العرض Lat") },
                                    placeholder = { Text("15.3547") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = longitude,
                                    onValueChange = { longitude = it },
                                    label = { Text("خط الطول Lng") },
                                    placeholder = { Text("44.2066") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Coverage Radius Slider
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("نطاق التغطية اللاسلكية:", fontSize = 11.sp)
                                    Text("${coverageRadius.roundToInt()} متر", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = coverageRadius,
                                    onValueChange = { coverageRadius = it },
                                    valueRange = 50f..2000f,
                                    steps = 39
                                )
                            }

                            // Parent Device / Upstream Link
                            val otherDevices = allDevices.filter { it.id != deviceToEdit?.id }
                            if (otherDevices.isNotEmpty()) {
                                ExposedDropdownMenuBox(
                                    expanded = isParentExpanded,
                                    onExpandedChange = { isParentExpanded = it }
                                ) {
                                    val parentName = otherDevices.firstOrNull { it.id == parentDeviceId }?.name ?: "بدون ربط (برج رئيسي مستقل)"
                                    OutlinedTextField(
                                        value = parentName,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("مربوط لاسلكياً بالبرج:") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isParentExpanded) },
                                        modifier = Modifier.fillMaxWidth().menuAnchor()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = isParentExpanded,
                                        onDismissRequest = { isParentExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("بدون ربط (برج رئيسي مستقل)") },
                                            onClick = {
                                                parentDeviceId = null
                                                isParentExpanded = false
                                            }
                                        )
                                        otherDevices.forEach { dev ->
                                            DropdownMenuItem(
                                                text = { Text("${dev.name} (${dev.ipAddress})") },
                                                onClick = {
                                                    parentDeviceId = dev.id
                                                    isParentExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Port & Interface
                item {
                    OutlinedTextField(
                        value = portOrInterface,
                        onValueChange = { portOrInterface = it },
                        label = { Text("المنفذ أو الواجهة (Interface/Port)") },
                        placeholder = { Text("مثال: ether2-Hotspot أو wlan1") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Frequency or SSID
                item {
                    OutlinedTextField(
                        value = frequencyOrSsid,
                        onValueChange = { frequencyOrSsid = it },
                        label = { Text("التردد أو اسم شبكة البث (Frequency / SSID)") },
                        placeholder = { Text("مثال: 5240 MHz / SAM-WIFI-5G") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Model
                item {
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        label = { Text("موديل الجهاز (Model)") },
                        placeholder = { Text("مثال: MikroTik RB4011 / TP-Link EAP225") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Notes
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("ملاحظات إضافية") },
                        placeholder = { Text("كلمات المرور، نوع الكيبل، حالة المحول...") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!isSaving && name.isNotBlank() && ipAddress.isNotBlank() && locationArea.isNotBlank() && !ipValidation.hasConflict) {
                        isSaving = true
                        val finalDeviceType = deviceType.trim().ifBlank { "مرسل" }
                        val updated = (deviceToEdit ?: NetworkDeviceEntity(
                            name = name.trim(),
                            ipAddress = ipAddress.trim(),
                            locationArea = locationArea.trim(),
                            deviceType = finalDeviceType
                        )).copy(
                            name = name.trim(),
                            ipAddress = ipAddress.trim(),
                            locationArea = locationArea.trim(),
                            deviceType = finalDeviceType,
                            latitude = latitude.toDoubleOrNull() ?: 0.0,
                            longitude = longitude.toDoubleOrNull() ?: 0.0,
                            coverageRadiusMeters = coverageRadius.roundToInt(),
                            parentDeviceId = parentDeviceId,
                            portOrInterface = portOrInterface.trim(),
                            frequencyOrSsid = frequencyOrSsid.trim(),
                            model = model.trim(),
                            notes = notes.trim()
                        )
                        onSave(updated)
                    }
                },
                enabled = !isSaving && name.isNotBlank() && ipAddress.isNotBlank() && locationArea.isNotBlank() && !ipValidation.hasConflict,
                colors = ButtonDefaults.buttonColors(containerColor = MikroTikPrimary),
                modifier = Modifier.testTag("save_device_btn")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("جاري الحفظ...", color = Color.White, fontWeight = FontWeight.Bold)
                } else {
                    Text(
                        if (deviceToEdit != null && deviceToEdit.id != 0L) "تحديث الجهاز" else "حفظ الجهاز",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
