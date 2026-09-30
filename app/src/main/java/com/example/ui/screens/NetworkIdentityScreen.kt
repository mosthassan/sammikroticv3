package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.NetworkIdentityEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.InvestmentGold
import com.example.ui.theme.MikroTikCyan
import com.example.ui.theme.MikroTikNavy
import com.example.ui.theme.MikroTikPrimary
import com.example.ui.theme.ProfitEmerald
import com.example.ui.theme.ReceiptGreen
import com.example.ui.theme.StatusOnline
import com.example.ui.theme.StatusWarning
import com.example.util.CurrencyHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NetworkIdentityScreen(
    viewModel: MainViewModel,
    onNavigateToAi: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val identityFromDb by viewModel.networkIdentity.collectAsState()
    val devices by viewModel.devices.collectAsState()

    var networkName by remember(identityFromDb) { mutableStateOf(identityFromDb.networkName) }
    var ownerName by remember(identityFromDb) { mutableStateOf(identityFromDb.ownerName) }
    var supportPhone by remember(identityFromDb) { mutableStateOf(identityFromDb.supportPhone) }
    var supportWhatsapp by remember(identityFromDb) { mutableStateOf(identityFromDb.supportWhatsapp) }
    var supportEmail by remember(identityFromDb) { mutableStateOf(identityFromDb.supportEmail) }
    var networkLocation by remember(identityFromDb) { mutableStateOf(identityFromDb.networkLocation) }
    var routerModel by remember(identityFromDb) { mutableStateOf(identityFromDb.routerModel) }
    var routerOsVersion by remember(identityFromDb) { mutableStateOf(identityFromDb.routerOsVersion) }
    var approvedDeviceSubnet by remember(identityFromDb) { mutableStateOf(identityFromDb.approvedDeviceSubnet) }
    var gatewayIp by remember(identityFromDb) { mutableStateOf(identityFromDb.gatewayIp) }
    var ipRangeStart by remember(identityFromDb) { mutableStateOf(identityFromDb.ipRangeStart) }
    var ipRangeEnd by remember(identityFromDb) { mutableStateOf(identityFromDb.ipRangeEnd) }
    var hotspotSubnet by remember(identityFromDb) { mutableStateOf(identityFromDb.hotspotSubnet) }
    var hotspotGatewayIp by remember(identityFromDb) { mutableStateOf(identityFromDb.hotspotGatewayIp) }
    var dnsServers by remember(identityFromDb) { mutableStateOf(identityFromDb.dnsServers) }
    var welcomeNotice by remember(identityFromDb) { mutableStateOf(identityFromDb.welcomeNotice) }

    // Multi-Currency & Exchange Rates State
    var defaultCurrency by remember(identityFromDb) { mutableStateOf(identityFromDb.defaultCurrency.ifBlank { "YER" }) }
    var isRatesEditing by remember { mutableStateOf(false) }
    var sarToYerRateText by remember { mutableStateOf((if (identityFromDb.sarToYerRate > 0) identityFromDb.sarToYerRate else 140.0).toString()) }
    var usdToYerRateText by remember { mutableStateOf((if (identityFromDb.usdToYerRate > 0) identityFromDb.usdToYerRate else 530.0).toString()) }
    var usdToSarRateText by remember { mutableStateOf((if (identityFromDb.usdToSarRate > 0) identityFromDb.usdToSarRate else 3.79).toString()) }

    LaunchedEffect(identityFromDb) {
        if (!isRatesEditing) {
            sarToYerRateText = (if (identityFromDb.sarToYerRate > 0) identityFromDb.sarToYerRate else 140.0).toString()
            usdToYerRateText = (if (identityFromDb.usdToYerRate > 0) identityFromDb.usdToYerRate else 530.0).toString()
            usdToSarRateText = (if (identityFromDb.usdToSarRate > 0) identityFromDb.usdToSarRate else 3.79).toString()
        }
    }

    var isReconciling by remember { mutableStateOf(false) }

    // Quick currency calculator
    var calcAmountText by remember { mutableStateOf("100") }
    var calcCurrency by remember { mutableStateOf("USD") }

    var suggestedIpPreview by remember { mutableStateOf<String?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showScriptDialog by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // Calculate how many devices are within the approved subnet
    val subnetPrefix = remember(approvedDeviceSubnet) {
        val clean = approvedDeviceSubnet.substringBefore("/").trim()
        val parts = clean.split(".")
        if (parts.size >= 3) "${parts[0]}.${parts[1]}.${parts[2]}." else "192.168.88."
    }
    val devicesInSubnetCount = devices.count { it.ipAddress.trim().startsWith(subnetPrefix) }
    val devicesOutsideSubnetCount = devices.size - devicesInSubnetCount

    val lastUpdatedDateStr = remember(identityFromDb.updatedAt) {
        if (identityFromDb.updatedAt > 0) {
            val sdf = SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale.getDefault())
            sdf.format(Date(identityFromDb.updatedAt))
        } else {
            "إعدادات أولية معتمدة"
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("network_identity_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Identity Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MikroTikNavy),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth().testTag("network_identity_hero")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MikroTikPrimary.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Wifi,
                                    contentDescription = null,
                                    tint = MikroTikCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = networkName.ifBlank { "هوية الشبكة الرسمية" },
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "المالك: ${ownerName.ifBlank { "المهندس المشرف" }}",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(StatusOnline.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(StatusOnline)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "معتمد لدى AI",
                                    color = StatusOnline,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Live Info Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Router Model Badge
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF172A45)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Router, contentDescription = null, tint = MikroTikCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text("السيرفر الرئيسي", color = Color(0xFF94A3B8), fontSize = 9.sp)
                                    Text(routerModel, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                }
                            }
                        }

                        // Subnet Badge
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF172A45)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.VpnKey, contentDescription = null, tint = MikroTikCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text("رينج الأجهزة", color = Color(0xFF94A3B8), fontSize = 9.sp)
                                    Text(approvedDeviceSubnet, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Notice / Welcome message
                    if (welcomeNotice.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F1E33))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MikroTikCyan, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = welcomeNotice,
                                color = Color(0xFFE2E8F0),
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "آخر تحديث للهوية: $lastUpdatedDateStr",
                        color = Color(0xFF64748B),
                        fontSize = 9.sp,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }

        // Section 1: Basic Identity Information
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().testTag("section_basic_identity")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Business, contentDescription = null, tint = MikroTikPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "بيانات الهوية والعلامة التجارية",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = networkName,
                        onValueChange = { networkName = it },
                        label = { Text("اسم الشبكة الرسمي *") },
                        placeholder = { Text("مثال: شبكة سام ميكروتك الذكية") },
                        leadingIcon = { Icon(Icons.Default.Wifi, contentDescription = null, tint = MikroTikPrimary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_network_name")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = ownerName,
                        onValueChange = { ownerName = it },
                        label = { Text("اسم مالك الشبكة أو المهندس المسؤول *") },
                        placeholder = { Text("مثال: المهندس سام الشبواني") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MikroTikPrimary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_owner_name")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = networkLocation,
                        onValueChange = { networkLocation = it },
                        label = { Text("الموقع الجغرافي ومقر السيرفرات") },
                        placeholder = { Text("مثال: اليمن - صنعاء - السبعين") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = MikroTikPrimary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_network_location")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = welcomeNotice,
                        onValueChange = { welcomeNotice = it },
                        label = { Text("رسالة الترحيب / الإعلان للمشتركين") },
                        placeholder = { Text("أهلاً بكم في شبكتنا - إنترنت فائق السرعة") },
                        singleLine = false,
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth().testTag("input_welcome_notice")
                    )
                }
            }
        }

        // Section 2: Contact & Technical Support Channels
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().testTag("section_contact_support")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Call, contentDescription = null, tint = MikroTikPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "عناوين التواصل والدعم الفني",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = supportPhone,
                        onValueChange = { supportPhone = it },
                        label = { Text("رقم هاتف الدعم الفني وخدمة المشتركين") },
                        placeholder = { Text("770000001") },
                        leadingIcon = { Icon(Icons.Default.Call, contentDescription = null, tint = MikroTikPrimary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_support_phone")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = supportWhatsapp,
                        onValueChange = { supportWhatsapp = it },
                        label = { Text("رقم الواتساب الرسمي (مع الرمز الدولي)") },
                        placeholder = { Text("967770000001") },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = ReceiptGreen) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_support_whatsapp")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = supportEmail,
                        onValueChange = { supportEmail = it },
                        label = { Text("البريد الإلكتروني للإدارة") },
                        placeholder = { Text("support@sam-mikrotic.ye") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = MikroTikPrimary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_support_email")
                    )
                }
            }
        }

        // Section 3: Main Router & System Specifications
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().testTag("section_router_specs")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Router, contentDescription = null, tint = MikroTikPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "السيرفر المركزي ونظام التشغيل",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = routerModel,
                            onValueChange = { routerModel = it },
                            label = { Text("موديل الراوتر الرئيسي") },
                            placeholder = { Text("MikroTik CCR2004") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("input_router_model")
                        )

                        OutlinedTextField(
                            value = routerOsVersion,
                            onValueChange = { routerOsVersion = it },
                            label = { Text("إصدار RouterOS") },
                            placeholder = { Text("v7.15") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("input_routeros_version")
                        )
                    }
                }
            }
        }

        // Section 4: Approved IP Range & Subnet Settings (CRITICAL)
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().testTag("section_ip_ranges")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VpnKey, contentDescription = null, tint = MikroTikPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "رينج أجهزة الشبكة المعتمد",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Status chip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MikroTikPrimary.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "$devicesInSubnetCount أجهزة مسجلة",
                                color = MikroTikPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "هذا النطاق هو المرجع الأساسي لمساعد الذكاء الاصطناعي (Gemini) ولنظام اقتراح وفحص عناوين الآي بي للأجهزة الجديدة لمنع التعارض.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = approvedDeviceSubnet,
                            onValueChange = { approvedDeviceSubnet = it },
                            label = { Text("رينج الشبكة (CIDR) *") },
                            placeholder = { Text("192.168.88.0/24") },
                            singleLine = true,
                            modifier = Modifier.weight(1.2f).testTag("input_approved_subnet")
                        )

                        OutlinedTextField(
                            value = gatewayIp,
                            onValueChange = { gatewayIp = it },
                            label = { Text("بوابة الراوتر *") },
                            placeholder = { Text("192.168.88.1") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("input_gateway_ip")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = ipRangeStart,
                            onValueChange = { ipRangeStart = it },
                            label = { Text("بداية التوزيع") },
                            placeholder = { Text("192.168.88.2") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("input_range_start")
                        )

                        OutlinedTextField(
                            value = ipRangeEnd,
                            onValueChange = { ipRangeEnd = it },
                            label = { Text("نهاية التوزيع") },
                            placeholder = { Text("192.168.88.254") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("input_range_end")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Subnet Health & Live IP Suggestion Test Box
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MikroTikPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "فحص جاهزية الرينج واقتراح الآي بي الشاغر",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            suggestedIpPreview = viewModel.getSuggestedIp()
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MikroTikPrimary),
                                    modifier = Modifier.height(32.dp).testTag("test_suggest_ip_btn")
                                ) {
                                    Text("تجربة الاقتراح", fontSize = 10.sp)
                                }
                            }

                            if (suggestedIpPreview != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(StatusOnline.copy(alpha = 0.15f))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusOnline, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "الآي بي التالي المتاح فوراً: ",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = suggestedIpPreview ?: "",
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = StatusOnline
                                    )
                                }
                            }

                            if (devicesOutsideSubnetCount > 0) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = StatusWarning, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "تنبيه: يوجد $devicesOutsideSubnetCount جهاز مسجل خارج هذا الرينج حالياً.",
                                        color = StatusWarning,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 5: Hotspot Network & DNS Settings
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().testTag("section_hotspot_dns")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Wifi, contentDescription = null, tint = MikroTikPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "شبكة الهوتسبوت والمشتركين و DNS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = hotspotSubnet,
                            onValueChange = { hotspotSubnet = it },
                            label = { Text("رينج الهوتسبوت (CIDR)") },
                            placeholder = { Text("10.5.50.0/24") },
                            singleLine = true,
                            modifier = Modifier.weight(1.2f).testTag("input_hotspot_subnet")
                        )

                        OutlinedTextField(
                            value = hotspotGatewayIp,
                            onValueChange = { hotspotGatewayIp = it },
                            label = { Text("بوابة الهوتسبوت") },
                            placeholder = { Text("10.5.50.1") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("input_hotspot_gw")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = dnsServers,
                        onValueChange = { dnsServers = it },
                        label = { Text("خوادم أسماء النطاقات (DNS Servers)") },
                        placeholder = { Text("8.8.8.8, 1.1.1.1") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_dns_servers")
                    )
                }
            }
        }

        // Section: Multi-Currency & Exchange Rates Management (قسم العملات وأسعار الصرف)
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, InvestmentGold.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth().testTag("section_currencies_settings")
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
                                    .background(InvestmentGold.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CurrencyExchange,
                                    contentDescription = null,
                                    tint = InvestmentGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "إدارة العملات وأسعار الصرف",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "ريال يمني (YER) • ريال سعودي (SAR) • دولار أمريكي (USD)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Base currency badge & Edit Button
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MikroTikPrimary.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "العملة الأساسية: YER",
                                    color = MikroTikPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    if (isRatesEditing) {
                                        val newSar = sarToYerRateText.toDoubleOrNull() ?: 140.0
                                        val newUsd = usdToYerRateText.toDoubleOrNull() ?: 530.0
                                        val newUsdSar = usdToSarRateText.toDoubleOrNull() ?: 3.79

                                        val updatedIdentity = identityFromDb.copy(
                                            sarToYerRate = newSar,
                                            usdToYerRate = newUsd,
                                            usdToSarRate = newUsdSar,
                                            updatedAt = System.currentTimeMillis()
                                        )
                                        viewModel.saveNetworkIdentity(updatedIdentity) {
                                            Toast.makeText(context, "تم تثبيت وحفظ أسعار الصرف بنجاح ✓", Toast.LENGTH_SHORT).show()
                                        }
                                        isRatesEditing = false
                                    } else {
                                        isRatesEditing = true
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (isRatesEditing) ProfitEmerald else MikroTikCyan),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(
                                    imageVector = if (isRatesEditing) Icons.Default.Check else Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = if (isRatesEditing) ProfitEmerald else MikroTikCyan,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (isRatesEditing) "حفظ وتثبيت" else "تعديل",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isRatesEditing) ProfitEmerald else MikroTikCyan
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isRatesEditing) "حالة التعديل: مفتوحة ✏️ (قم بإدخال أسعار الصرف الجديدة ثم اضغط حفظ وتثبيت)" else "حالة أسعار الصرف: مثبتة ومحفوظة 🔒 (اضغط زر تعديل للتغيير والحفظ)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isRatesEditing) ProfitEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Exchange rates inputs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = sarToYerRateText,
                            onValueChange = { sarToYerRateText = it },
                            enabled = isRatesEditing,
                            label = { Text("1 ريال سعودي = (ريال يمني)") },
                            placeholder = { Text("140") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("input_sar_to_yer")
                        )

                        OutlinedTextField(
                            value = usdToYerRateText,
                            onValueChange = { usdToYerRateText = it },
                            enabled = isRatesEditing,
                            label = { Text("1 دولار أمريكي = (ريال يمني)") },
                            placeholder = { Text("530") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("input_usd_to_yer")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = usdToSarRateText,
                        onValueChange = { usdToSarRateText = it },
                        enabled = isRatesEditing,
                        label = { Text("1 دولار أمريكي = (ريال سعودي)") },
                        placeholder = { Text("3.79") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_usd_to_sar")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Live Currency Converter Mini-Widget
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Calculate, contentDescription = null, tint = MikroTikCyan, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("حاسبة التحويل السريع المباشرة", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf("USD" to "دولار", "SAR" to "سعودي").forEach { (cKey, cLabel) ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (calcCurrency == cKey) MikroTikPrimary else Color.Transparent)
                                                .clickable { calcCurrency = cKey }
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = cLabel,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (calcCurrency == cKey) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = calcAmountText,
                                    onValueChange = { calcAmountText = it },
                                    label = { Text("المبلغ بـ ($calcCurrency)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )

                                val inputAmount = calcAmountText.toDoubleOrNull() ?: 0.0
                                val curSarRate = sarToYerRateText.toDoubleOrNull() ?: 430.0
                                val curUsdRate = usdToYerRateText.toDoubleOrNull() ?: 1630.0
                                val convertedYer = if (calcCurrency == "USD") inputAmount * curUsdRate else inputAmount * curSarRate

                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text("المعادل بالريال اليمني:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "${String.format(Locale.US, "%,.0f", convertedYer)} ر.ي",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = ProfitEmerald
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Reconcile Accounting Ledger Button
                    OutlinedButton(
                        onClick = {
                            isReconciling = true
                            viewModel.reconcileAccountingLedger {
                                isReconciling = false
                                Toast.makeText(context, "تمت التسوية المحاسبية ومطابقة كافة التبويبات بنجاح ✓", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp).testTag("btn_reconcile_ledger")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isReconciling) "جارٍ التسوية والمطابقة التامة..." else "مزامنة وتسوية الحسابات بين كافة التبويبات",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Cloud Sync to Firebase (sam-mikrotic)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.triggerCloudSync {
                                    Toast.makeText(context, "تمت مزامنة ورفع كافة البيانات إلى السحابة بنجاح ✓", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ProfitEmerald),
                            modifier = Modifier.weight(1f).height(38.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("رفع للسحابة Firebase", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.pullDataFromCloud {
                                    Toast.makeText(context, "تم استيراد البيانات من السحابة بنجاح ✓", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(38.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("سحب من السحابة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Section 6: AI Awareness & Integration Box
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B).copy(alpha = 0.5f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth().testTag("section_ai_awareness")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFA5B4FC), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "تكامل مساعد الذكاء الاصطناعي (Gemini AI)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }

                        Button(
                            onClick = {
                                onNavigateToAi()
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("فتح المستشار", fontSize = 10.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "المساعد الذكي (SAM AI) يستخدم الآن بيانات $networkName ورينج الأجهزة المعتمد ($approvedDeviceSubnet) كمرجع أساسي للإجابة على جميع الاستشارات التقنية وتوليد سكربتات RouterOS الدقيقة.",
                        color = Color(0xFFCBD5E1),
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick consultation chip buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.consultAi("حلل لي رينج الأجهزة المعتمد $approvedDeviceSubnet والبوابة $gatewayIp وكيف أحمي الشبكة من التعارض في راوتر $routerModel")
                                onNavigateToAi()
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(34.dp)
                        ) {
                            Text("استشارة الرينج والتعارض", fontSize = 10.sp, color = Color(0xFFA5B4FC))
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.consultAi("أعطني سكربت تهيئة متكامل للهوتسبوت على رينج $hotspotSubnet و كيو السرعات لراوتر $routerModel")
                                onNavigateToAi()
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(34.dp)
                        ) {
                            Text("سكربت الهوتسبوت", fontSize = 10.sp, color = Color(0xFFA5B4FC))
                        }
                    }
                }
            }
        }

        // Section 7: Action Buttons (Save, Script Export, Reset)
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Primary Save Button
                Button(
                    onClick = {
                        if (networkName.isBlank()) {
                            Toast.makeText(context, "يرجى إدخال اسم الشبكة", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (approvedDeviceSubnet.isBlank() || gatewayIp.isBlank()) {
                            Toast.makeText(context, "يرجى إدخال رينج الأجهزة المعتمد وبوابة الراوتر", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        isSaving = true
                        val updated = NetworkIdentityEntity(
                            id = 1L,
                            networkName = networkName.trim(),
                            ownerName = ownerName.trim(),
                            supportPhone = supportPhone.trim(),
                            supportWhatsapp = supportWhatsapp.trim(),
                            supportEmail = supportEmail.trim(),
                            networkLocation = networkLocation.trim(),
                            routerModel = routerModel.trim(),
                            routerOsVersion = routerOsVersion.trim(),
                            approvedDeviceSubnet = approvedDeviceSubnet.trim(),
                            gatewayIp = gatewayIp.trim(),
                            ipRangeStart = ipRangeStart.trim(),
                            ipRangeEnd = ipRangeEnd.trim(),
                            hotspotSubnet = hotspotSubnet.trim(),
                            hotspotGatewayIp = hotspotGatewayIp.trim(),
                            dnsServers = dnsServers.trim(),
                            welcomeNotice = welcomeNotice.trim(),
                            defaultCurrency = defaultCurrency.trim().ifBlank { "YER" },
                            sarToYerRate = sarToYerRateText.toDoubleOrNull() ?: 140.0,
                            usdToYerRate = usdToYerRateText.toDoubleOrNull() ?: 530.0,
                            usdToSarRate = usdToSarRateText.toDoubleOrNull() ?: 3.79,
                            updatedAt = System.currentTimeMillis()
                        )

                        viewModel.saveNetworkIdentity(updated) {
                            isSaving = false
                            Toast.makeText(context, "تم حفظ هوية الشبكة وأسعار الصرف وتحديث سياق الذكاء الاصطناعي بنجاح ✓", Toast.LENGTH_LONG).show()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MikroTikPrimary),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_network_identity_btn")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSaving) "جارٍ الحفظ والمزامنة السحابية..." else "حفظ هوية الشبكة ومزامنتها",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Export RouterOS Script Dialog Button
                    OutlinedButton(
                        onClick = { showScriptDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(42.dp).testTag("export_routeros_script_btn")
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("سكربت ميكروتك", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Reset to Recommended Defaults
                    OutlinedButton(
                        onClick = { showResetDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusWarning),
                        modifier = Modifier.weight(1f).height(42.dp).testTag("reset_identity_defaults_btn")
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("القيم الموصى بها", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Reset Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text("استعادة إعدادات الهوية النموذجية", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("هل ترغب في إعادة ضبط هوية الشبكة ورينج الأجهزة إلى القيم النموذجية الموصى بها (192.168.88.0/24)؟")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetNetworkIdentityToDefault {
                            showResetDialog = false
                            Toast.makeText(context, "تمت استعادة الإعدادات النموذجية بنجاح", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MikroTikPrimary)
                ) {
                    Text("نعم، استعادة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // RouterOS Script Modal Dialog
    if (showScriptDialog) {
        val generatedScript = """
            # ==============================================================
            # سكريبت تهيئة شبكة: $networkName
            # المالك: $ownerName | الموقع: $networkLocation
            # راوتر: $routerModel ($routerOsVersion)
            # تم التوليد بواسطة: تطبيق سام ميكروتك الذكي
            # ==============================================================
            
            # 1. تسمية السيرفر وهوية الراوتر
            /system identity set name="$networkName"
            
            # 2. إعداد رينج أجهزة الشبكة والبوابة
            /ip address
            add address=$gatewayIp/24 interface=bridge comment="SAM-Core-Gateway"
            
            # 3. بركة عناوين الآي بي لأجهزة البنية التحتية
            /ip pool
            add name=pool-sam-infra ranges=$ipRangeStart-$ipRangeEnd
            
            # 4. إعدادات خوادم أسماء النطاقات (DNS)
            /ip dns
            set allow-remote-requests=yes servers=$dnsServers
            
            # 5. حماية الرينج ومنع تكرار الآي بي (Anti-ARP Spoofing)
            /ip dhcp-server set [find] add-arp=yes
            /interface bridge set [find] arp=reply-only
            
            # 6. إعداد رينج الهوتسبوت والمشتركين
            /ip address
            add address=$hotspotGatewayIp/24 interface=bridge comment="SAM-Hotspot-Subnet"
            /ip pool
            add name=pool-sam-hotspot ranges=10.5.50.10-10.5.50.250
            
            # 7. ملاحظة الدعم الفني
            /system note set note="Network: $networkName | Support Phone: $supportPhone | WhatsApp: $supportWhatsapp"
        """.trimIndent()

        AlertDialog(
            onDismissRequest = { showScriptDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Code, contentDescription = null, tint = MikroTikPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("سكربت RouterOS Terminal", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    Text(
                        text = "انسخ هذا السكربت والصقه في موجه الأوامر (Terminal) في برنامج Winbox لضبط الراوتر بنفس الهوية والرينج المعتمد:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        modifier = Modifier.fillMaxWidth().height(260.dp)
                    ) {
                        LazyColumn(modifier = Modifier.padding(10.dp)) {
                            item {
                                Text(
                                    text = generatedScript,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = Color(0xFF38BDF8)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("RouterOS Script", generatedScript)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "تم نسخ السكربت إلى الحافظة بنجاح ✓", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MikroTikPrimary)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("نسخ السكربت")
                }
            },
            dismissButton = {
                TextButton(onClick = { showScriptDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }
}
