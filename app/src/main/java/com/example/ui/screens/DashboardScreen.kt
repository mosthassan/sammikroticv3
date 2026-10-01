package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import android.widget.Toast
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.NetworkRepository
import com.example.ui.MainViewModel
import com.example.ui.theme.AssetPurple
import com.example.ui.theme.EquityBlue
import com.example.ui.theme.InvestmentGold
import com.example.ui.theme.MikroTikCyan
import com.example.ui.theme.MikroTikNavy
import com.example.ui.theme.MikroTikPrimary
import com.example.ui.theme.PaymentRed
import com.example.ui.theme.ProfitEmerald
import com.example.ui.theme.ReceiptGreen
import com.example.ui.theme.StatusDistributed
import com.example.ui.theme.StatusOnline
import com.example.ui.theme.StatusWarning

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.graphics.Brush
import com.example.data.local.entity.UserEntity
import com.example.ui.GoogleSignInUiState
import com.example.ui.theme.CairoFontFamily
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberDarkCardElevated
import com.example.ui.theme.CyberDarkSurface
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToTab: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onSignInWithGoogle: () -> Unit = {},
    onSignOutGoogle: () -> Unit = {},
    onResetToProduction: () -> Unit = {}
) {
    val devices by viewModel.devices.collectAsState()
    val retailers by viewModel.retailers.collectAsState()
    val availableCards by viewModel.availableCardsCount.collectAsState()
    val vouchers by viewModel.vouchers.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val networkIdentity by viewModel.networkIdentity.collectAsState()
    val partners by viewModel.partners.collectAsState()
    val assets by viewModel.assets.collectAsState()
    val totalInvestedCapital by viewModel.totalInvestedCapital.collectAsState()
    val totalAssetPurchaseCost by viewModel.totalAssetPurchaseCost.collectAsState()
    val totalReceipts by viewModel.totalReceipts.collectAsState()
    val totalPayments by viewModel.totalPayments.collectAsState()
    val distributors by viewModel.distributors.collectAsState()
    val isProductionMode by viewModel.isProductionMode.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()

    var selectedDashboardTab by remember { mutableIntStateOf(0) }
    var showEmailAuthDialog by remember { mutableStateOf(false) }
    var showFirebaseConfigDialog by remember { mutableStateOf(false) }

    val totalDebt = retailers.sumOf { it.balanceOwedDouble }
    val onlineDevicesCount = devices.count { it.status == "ONLINE" }
    val receiptsVal = totalReceipts ?: 0.0
    val paymentsVal = totalPayments ?: 0.0
    val netProfitVal = receiptsVal - paymentsVal

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen")
    ) {
        // 2026 Sleek Segmented Capsule Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(CyberDarkSurface)
                .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                Triple(0, "المؤشرات", Icons.Default.Dashboard),
                Triple(1, "الموزعين (${distributors.size})", Icons.Default.TwoWheeler),
                Triple(2, "هوية الشبكة", Icons.Default.Wifi)
            ).forEach { (tabIndex, title, icon) ->
                val isSelected = selectedDashboardTab == tabIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) Brush.horizontalGradient(
                                listOf(Color(0xFF0284C7), Color(0xFF0369A1))
                            ) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                        )
                        .clickable { selectedDashboardTab = tabIndex }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) Color.White else TextSecondaryDark,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = title,
                            color = if (isSelected) Color.White else TextSecondaryDark,
                            fontFamily = CairoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        if (selectedDashboardTab == 1) {
            DistributorsManagementScreen(
                viewModel = viewModel,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            )
        } else if (selectedDashboardTab == 2) {
            NetworkIdentityScreen(
                viewModel = viewModel,
                onNavigateToAi = { onNavigateToTab(5) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Welcome Hero Banner
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                        border = BorderStroke(1.dp, CyberBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (NetworkRepository.isSuperAdminEmail(currentUser?.email)) {
                                            "مرحباً بك، ${currentUser?.fullName ?: "المهندس حسن"} 👑"
                                        } else {
                                            "مرحباً بك، ${currentUser?.fullName ?: "مدير الشبكة"}"
                                        },
                                        color = TextPrimaryDark,
                                        fontFamily = CairoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = "${networkIdentity.networkName} • نطاق ${networkIdentity.approvedDeviceSubnet}",
                                        color = TextSecondaryDark,
                                        fontFamily = CairoFontFamily,
                                        fontSize = 11.5.sp,
                                        maxLines = 1
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(CyberDarkCardElevated)
                                            .border(1.dp, CyberBorder, RoundedCornerShape(10.dp))
                                            .clickable { selectedDashboardTab = 2 }
                                            .padding(horizontal = 8.dp, vertical = 5.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Settings, contentDescription = null, tint = MikroTikCyan, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "الهوية",
                                                color = MikroTikCyan,
                                                fontFamily = CairoFontFamily,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isProductionMode) Color(0xFF059669).copy(alpha = 0.2f) else StatusOnline.copy(alpha = 0.15f))
                                            .border(
                                                1.dp,
                                                if (isProductionMode) Color(0xFF10B981).copy(alpha = 0.4f) else StatusOnline.copy(alpha = 0.4f),
                                                RoundedCornerShape(10.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = if (isProductionMode) "الوضع الفعلي 🚀" else "مستقر 100%",
                                            color = if (isProductionMode) Color(0xFF34D399) else StatusOnline,
                                            fontFamily = CairoFontFamily,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // IP Anti-Collision Security Widget
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CyberDarkCardElevated)
                                    .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MikroTikCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "نظام الحماية من تعارض الآي بي (IP Conflict Guard)",
                                        color = TextPrimaryDark,
                                        fontFamily = CairoFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "جميع عناوين الـ IP (${devices.size} جهاز) مفحوصة ومؤمنة دون أي تكرار",
                                        color = TextSecondaryDark,
                                        fontFamily = CairoFontFamily,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }

        // Google Cloud Account & Remote Backup Card (Matching the modern native security & backup card)
        item {
            val googleSignInState by viewModel.googleSignInState.collectAsState()
            val isConnected = currentUser?.isGoogleUser == true || !googleSignInState.activeFirebaseEmail.isNullOrBlank()
            val activeEmail = currentUser?.email ?: googleSignInState.activeFirebaseEmail ?: ""

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.dp, if (isConnected) Color(0xFF10B981) else Color(0xFF1E3A8A)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_google_cloud_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header Row: Title + Subtitle + Google Logo Circle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "الحساب السحابي والنسخ الاحتياطي",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "اربط جهازك بحساب Google لتأمين ونسخ بياناتك وسجلاتك سحابياً",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "G",
                                color = Color(0xFF2563EB),
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (!isConnected) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CyberDarkCardElevated,
                            border = BorderStroke(1.dp, CyberBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.CloudQueue,
                                    contentDescription = null,
                                    tint = MikroTikCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "يعمل النظام محلياً بشكل كامل ومستقل. اربط حسابك السحابي لتفعيل النسخ الاحتياطي والمزامنة اللامركزية التلقائية.",
                                    color = TextSecondaryDark,
                                    fontFamily = CairoFontFamily,
                                    fontSize = 11.5.sp,
                                    lineHeight = 17.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Full-Width Google Sign-In Button (Primary 1-Tap Trigger)
                        Button(
                            onClick = onSignInWithGoogle,
                            enabled = !googleSignInState.isLoading,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_google_one_tap_dashboard")
                        ) {
                            if (googleSignInState.isLoading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "جاري الاتصال بخدمات Google...",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "G",
                                        color = Color(0xFF2563EB),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تسجيل الدخول بحساب Google",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Alternative Sign-in & Setup Options
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showEmailAuthDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MikroTikCyan.copy(alpha = 0.6f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MikroTikCyan),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .testTag("btn_direct_email_auth")
                            ) {
                                Icon(
                                    Icons.Default.Email,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "الدخول بالبريد",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = CairoFontFamily
                                )
                            }

                            OutlinedButton(
                                onClick = { showFirebaseConfigDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFF64748B)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCBD5E1)),
                                modifier = Modifier
                                    .height(42.dp)
                                    .testTag("btn_firebase_config_info")
                            ) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "بيانات SHA-1",
                                    fontSize = 11.5.sp,
                                    fontFamily = CairoFontFamily
                                )
                            }
                        }
                    } else {
                        // Connected State: Real-Time Synced Google Account View
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF064E3B).copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, Color(0xFF10B981)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "متصل بحساب Google ومزامن سحابياً ✅",
                                        color = Color(0xFFA7F3D0),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = activeEmail,
                                        color = Color.White,
                                        fontSize = 11.5.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Sync Status Message Box
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CyberDarkCardElevated.copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CloudQueue,
                                    contentDescription = null,
                                    tint = MikroTikCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = syncStatus,
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.triggerCloudSync() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "مزامنة ثنائية (رفع وسحب)",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = { viewModel.pullDataFromCloud() },
                                colors = ButtonDefaults.buttonColors(containerColor = MikroTikCyan.copy(alpha = 0.2f)),
                                border = BorderStroke(1.dp, MikroTikCyan),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "سحب السحابة ⬇️",
                                    color = MikroTikCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedButton(
                                onClick = onSignOutGoogle,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF87171)),
                                border = BorderStroke(1.dp, Color(0xFFF87171)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("خروج", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Production Environment Initialization & Factory Reset Card (يظهر لكل مستخدم جديد لتهيئة بيئة عمله النظيفة)
        if (!isProductionMode) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                    border = BorderStroke(1.dp, PaymentRed.copy(alpha = 0.45f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("production_reset_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(PaymentRed.copy(alpha = 0.15f))
                                    .border(1.dp, PaymentRed.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RocketLaunch,
                                    contentDescription = null,
                                    tint = PaymentRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "تهيئة بيئة العمل وتخصيص شبكتك الجديدة",
                                        color = TextPrimaryDark,
                                        fontFamily = CairoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = PaymentRed.copy(alpha = 0.18f),
                                        border = BorderStroke(1.dp, PaymentRed.copy(alpha = 0.35f))
                                    ) {
                                        Text(
                                            text = "إعداد أولي 🚀",
                                            color = PaymentRed,
                                            fontFamily = CairoFontFamily,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "حذف كل البيانات الافتراضية وتصفير النظام لبدء تشغيل شبكتك الخاصة كمنتج مستقل",
                                    color = TextSecondaryDark,
                                    fontFamily = CairoFontFamily,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = onResetToProduction,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PaymentRed),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .testTag("dashboard_reset_production_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "تهيئة التطبيق وإزالة كافة البيانات الافتراضية",
                                    fontFamily = CairoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4 KPI Summary Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardStatCard(
                        title = "أجهزة الشبكة",
                        value = "${devices.size} أجهزة",
                        subtitle = "$onlineDevicesCount متصل أونلاين",
                        color = MikroTikPrimary,
                        icon = Icons.Default.Router,
                        onClick = { onNavigateToTab(1) },
                        modifier = Modifier.weight(1f)
                    )
                    DashboardStatCard(
                        title = "كروت المستودع",
                        value = "$availableCards كرت",
                        subtitle = "جاهزة للتوزيع",
                        color = StatusOnline,
                        icon = Icons.Default.ConfirmationNumber,
                        onClick = { onNavigateToTab(2) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardStatCard(
                        title = "نقاط التوزيع (البقالات)",
                        value = "${retailers.size} بقالة",
                        subtitle = "ديون: ${totalDebt.toInt()} ريال",
                        color = StatusWarning,
                        icon = Icons.Default.Store,
                        onClick = { onNavigateToTab(3) },
                        modifier = Modifier.weight(1f)
                    )
                    DashboardStatCard(
                        title = "السندات المالية",
                        value = "${vouchers.size} سندات",
                        subtitle = "حركات قبض وصرف",
                        color = ReceiptGreen,
                        icon = Icons.Default.Receipt,
                        onClick = { onNavigateToTab(4) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Authorized Distributors & Field Sales Team Overview Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, CyberBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedDashboardTab = 1 }
                    .testTag("dashboard_distributors_overview_card")
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
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF38BDF8).copy(alpha = 0.15f))
                                    .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.TwoWheeler,
                                    contentDescription = null,
                                    tint = MikroTikCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "فريق الموزعين ونقاط التوزيع الميدانية",
                                    color = TextPrimaryDark,
                                    fontFamily = CairoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "إضافة وتفويض الموزعين عبر البريد الإلكتروني لتوزيع الكروت والبقالات",
                                    color = TextSecondaryDark,
                                    fontFamily = CairoFontFamily,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0284C7).copy(alpha = 0.2f))
                                .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${distributors.size} موزع معتمد",
                                color = MikroTikCyan,
                                fontFamily = CairoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = CyberDarkCardElevated,
                            border = BorderStroke(1.dp, CyberBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("الموزعين النشطين", fontSize = 10.sp, fontFamily = CairoFontFamily, color = TextSecondaryDark)
                                Text(
                                    text = "${distributors.count { it.isActive }} موزع",
                                    fontSize = 13.sp,
                                    fontFamily = CairoFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ProfitEmerald
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = CyberDarkCardElevated,
                            border = BorderStroke(1.dp, CyberBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("نقاط البيع (البقالات)", fontSize = 10.sp, fontFamily = CairoFontFamily, color = TextSecondaryDark)
                                Text(
                                    text = "${retailers.size} بقالة",
                                    fontSize = 13.sp,
                                    fontFamily = CairoFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = StatusWarning
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = CyberDarkCardElevated,
                            border = BorderStroke(1.dp, CyberBorder),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("الصلاحية الممنوحة", fontSize = 10.sp, fontFamily = CairoFontFamily, color = TextSecondaryDark)
                                Text(
                                    text = "توزيع + بقالات + سندات",
                                    fontSize = 10.5.sp,
                                    fontFamily = CairoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = MikroTikCyan
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "إدارة فريق الموزعين وإضافة موزع جديد ←",
                            color = MikroTikCyan,
                            fontFamily = CairoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Investment, Partners & CAPEX Project Overview Banner
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, CyberBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToTab(4) }
                    .testTag("dashboard_investment_overview_card")
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
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(InvestmentGold.copy(alpha = 0.15f))
                                    .border(1.dp, InvestmentGold.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = InvestmentGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "نظام الاستثمار والشركاء والأصول",
                                    color = TextPrimaryDark,
                                    fontFamily = CairoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "إدارة حصص الشركاء، أصول الشبكة (CAPEX)، والأرباح",
                                    color = TextSecondaryDark,
                                    fontFamily = CairoFontFamily,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(ProfitEmerald.copy(alpha = 0.18f))
                                .border(1.dp, ProfitEmerald.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "مشروع استثماري",
                                color = ProfitEmerald,
                                fontFamily = CairoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Partner Capital
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
                            border = BorderStroke(1.dp, CyberBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Group, contentDescription = null, tint = InvestmentGold, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("رأس المال", color = TextSecondaryDark, fontFamily = CairoFontFamily, fontSize = 10.sp)
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "${String.format(java.util.Locale.US, "%,.0f", totalInvestedCapital ?: 0.0)} ر.ي",
                                    color = InvestmentGold,
                                    fontFamily = CairoFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Fixed Assets (CAPEX)
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
                            border = BorderStroke(1.dp, CyberBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Devices, contentDescription = null, tint = AssetPurple, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("الأصول (${assets.size})", color = TextSecondaryDark, fontFamily = CairoFontFamily, fontSize = 10.sp)
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "${String.format(java.util.Locale.US, "%,.0f", totalAssetPurchaseCost ?: 0.0)} ر.ي",
                                    color = AssetPurple,
                                    fontFamily = CairoFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Net Profit
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
                            border = BorderStroke(1.dp, CyberBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = ProfitEmerald, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("صافي الربح", color = TextSecondaryDark, fontFamily = CairoFontFamily, fontSize = 10.sp)
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "${String.format(java.util.Locale.US, "%,.0f", netProfitVal)} ر.ي",
                                    color = if (netProfitVal >= 0) ProfitEmerald else PaymentRed,
                                    fontFamily = CairoFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Network GIS Map Card
        item {
            val mappedDevicesCount = devices.count { it.latitude != 0.0 && it.longitude != 0.0 }
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, CyberBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToTab(1) }
                    .testTag("dashboard_gis_map_card")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0D9488).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFF0D9488).copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CellTower,
                            contentDescription = null,
                            tint = Color(0xFF2DD4BF),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "خريطة الأبراج والتغطية اللاسلكية GIS",
                                color = TextPrimaryDark,
                                fontFamily = CairoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF0D9488).copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, Color(0xFF0D9488).copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = "خريطة تفاعلية",
                                    color = Color(0xFF2DD4BF),
                                    fontFamily = CairoFontFamily,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "$mappedDevicesCount أبراج وسواري محددة على الخريطة من أصل ${devices.size} جهاز",
                            color = TextSecondaryDark,
                            fontFamily = CairoFontFamily,
                            fontSize = 11.sp
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0D9488),
                        modifier = Modifier.clickable { onNavigateToTab(1) }
                    ) {
                        Text(
                            text = "عرض 🗺️",
                            color = Color.White,
                            fontFamily = CairoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Quick Actions Section Header
        item {
            Text(
                text = "الإجراءات والعمليات السريعة",
                fontFamily = CairoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextPrimaryDark
            )
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionButton(
                    title = "إضافة جهاز وفحص IP",
                    icon = Icons.Default.Lan,
                    color = MikroTikCyan,
                    onClick = { onNavigateToTab(1) },
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    title = "توليد كروت جديدة",
                    icon = Icons.Default.ConfirmationNumber,
                    color = StatusOnline,
                    onClick = { onNavigateToTab(2) },
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    title = "تسليم كروت لبقالة",
                    icon = Icons.Default.Send,
                    color = StatusDistributed,
                    onClick = { onNavigateToTab(3) },
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    title = "مستشار سام AI",
                    icon = Icons.Default.AutoAwesome,
                    color = Color(0xFFA78BFA),
                    onClick = { onNavigateToTab(5) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Recent Vouchers Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "آخر السندات المالية المقيدة",
                    fontFamily = CairoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimaryDark
                )

                Text(
                    text = "عرض الكل ←",
                    fontFamily = CairoFontFamily,
                    fontSize = 12.sp,
                    color = MikroTikCyan,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigateToTab(4) }
                )
            }
        }

        if (vouchers.isEmpty()) {
            item {
                Text(
                    text = "لا توجد حركات مالية مسجلة بعد",
                    fontFamily = CairoFontFamily,
                    color = TextSecondaryDark,
                    fontSize = 12.sp
                )
            }
        } else {
            items(vouchers.take(4), key = { it.id }) { voucher ->
                val isReceipt = voucher.voucherType == "RECEIPT"
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                    border = BorderStroke(1.dp, CyberBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToTab(4) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isReceipt) ReceiptGreen.copy(alpha = 0.15f) else PaymentRed.copy(alpha = 0.15f))
                                    .border(1.dp, if (isReceipt) ReceiptGreen.copy(alpha = 0.35f) else PaymentRed.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isReceipt) Icons.Default.LocalAtm else Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = if (isReceipt) ReceiptGreen else PaymentRed,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = voucher.partyName,
                                    fontFamily = CairoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = TextPrimaryDark
                                )
                                Text(
                                    text = "${voucher.voucherNumber} • ${voucher.category}",
                                    fontFamily = CairoFontFamily,
                                    fontSize = 10.5.sp,
                                    color = TextSecondaryDark
                                )
                            }
                        }

                        Text(
                            text = "${if (isReceipt) "+" else "-"}${voucher.amount.toInt()} ريال",
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = if (isReceipt) ReceiptGreen else PaymentRed
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
    }

    if (showEmailAuthDialog) {
        EmailAuthDialog(
            viewModel = viewModel,
            onDismiss = { showEmailAuthDialog = false }
        )
    }

    if (showFirebaseConfigDialog) {
        FirebaseConfigDialog(
            onDismiss = { showFirebaseConfigDialog = false }
        )
    }
    }
}

@Composable
fun DashboardStatCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brush.linearGradient(listOf(color.copy(alpha = 0.22f), color.copy(alpha = 0.05f))))
                        .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                fontSize = 17.sp,
                fontFamily = CairoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(
                text = title,
                fontSize = 12.sp,
                fontFamily = CairoFontFamily,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimaryDark
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                fontFamily = CairoFontFamily,
                color = TextSecondaryDark
            )
        }
    }
}

@Composable
fun QuickActionButton(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
        border = BorderStroke(1.dp, CyberBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(color.copy(alpha = 0.22f), color.copy(alpha = 0.05f))))
                    .border(1.dp, color.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 10.sp,
                fontFamily = CairoFontFamily,
                fontWeight = FontWeight.SemiBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = TextPrimaryDark,
                maxLines = 2
            )
        }
    }
}

@Composable
fun EmailAuthDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val googleSignInState by viewModel.googleSignInState.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = MikroTikCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isSignUp) "إنشاء حساب سحابي (Firebase)" else "تسجيل الدخول بالبريد (Firebase)",
                    fontFamily = CairoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "مصادقة حقيقية عبر Firebase Authentication لتأمين ومزامنة شبكتك سحابياً.",
                    fontFamily = CairoFontFamily,
                    fontSize = 11.5.sp,
                    color = TextSecondaryDark,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Toggle between Sign In and Sign Up
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberDarkCardElevated)
                        .padding(4.dp)
                ) {
                    Button(
                        onClick = { isSignUp = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isSignUp) MikroTikPrimary else Color.Transparent,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("تسجيل دخول", fontSize = 12.sp, fontFamily = CairoFontFamily)
                    }

                    Button(
                        onClick = { isSignUp = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSignUp) MikroTikPrimary else Color.Transparent,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("حساب جديد", fontSize = 12.sp, fontFamily = CairoFontFamily)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isSignUp) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("الاسم الكامل", fontFamily = CairoFontFamily, fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MikroTikCyan) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("البريد الإلكتروني", fontFamily = CairoFontFamily, fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = MikroTikCyan) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("كلمة المرور (6 أحرف أو أكثر)", fontFamily = CairoFontFamily, fontSize = 12.sp) },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MikroTikCyan) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8)
                            )
                        }
                    }
                )

                if (googleSignInState.isLoading) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MikroTikCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("جاري التحقق عبر Firebase...", color = MikroTikCyan, fontSize = 12.sp, fontFamily = CairoFontFamily)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isSignUp) {
                        viewModel.signUpWithEmail(email, password, name) { success ->
                            if (success) onDismiss()
                        }
                    } else {
                        viewModel.signInWithEmail(email, password) { success ->
                            if (success) onDismiss()
                        }
                    }
                },
                enabled = !googleSignInState.isLoading && email.isNotBlank() && password.length >= 6,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (isSignUp) "إنشاء الحساب والمزامنة" else "تسجيل الدخول",
                    fontFamily = CairoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = Color(0xFF94A3B8), fontFamily = CairoFontFamily)
            }
        },
        containerColor = CyberDarkSurface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun FirebaseConfigDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val packageName = "com.samtecai.sammikrotic"
    val sha1 = "64:FB:BF:3E:DD:50:75:13:D3:B8:A7:F5:B7:F2:52:23:19:E1:B9:EF"
    val sha256 = "D5:E9:39:11:E1:C1:FF:A3:A1:D1:DC:5E:18:24:0D:A5:4E:02:A8:21:6E:E0:7D:FD:8B:1A:CA:E6:51:28:A9:46"
    val webClientId = "668455931031-burt9863pi64rshdlmgenejnj27ep0d0.apps.googleusercontent.com"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = MikroTikCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "بيانات ربط Firebase و Google",
                    fontFamily = CairoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "لضمان عمل تسجيل الدخول بنقرة واحدة من Google على الأجهزة الحقيقية، يجب إضافة بصمة SHA-1 أدناه في إعدادات تطبيق Android في Firebase Console.",
                    fontFamily = CairoFontFamily,
                    fontSize = 11.5.sp,
                    color = TextSecondaryDark,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Package Name
                Text("اسم الحزمة (Package Name):", color = Color(0xFF94A3B8), fontSize = 11.sp, fontFamily = CairoFontFamily)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = CyberDarkCardElevated,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text(packageName, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(8.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // SHA-1 Fingerprint with copy button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("بصمة الشهادة (SHA-1):", color = Color(0xFF94A3B8), fontSize = 11.sp, fontFamily = CairoFontFamily)
                    TextButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(sha1))
                            Toast.makeText(context, "تم نسخ SHA-1 إلى الحافظة", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = MikroTikCyan, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("نسخ", color = MikroTikCyan, fontSize = 11.sp, fontFamily = CairoFontFamily)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = CyberDarkCardElevated,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                ) {
                    Text(sha1, color = Color(0xFFA7F3D0), fontSize = 10.5.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(8.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Web Client ID
                Text("معرّف Web Client ID:", color = Color(0xFF94A3B8), fontSize = 11.sp, fontFamily = CairoFontFamily)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = CyberDarkCardElevated,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text(webClientId, color = Color(0xFFE2E8F0), fontSize = 9.5.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(8.dp))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MikroTikPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("إغلاق", fontFamily = CairoFontFamily, fontSize = 12.sp)
            }
        },
        containerColor = CyberDarkSurface,
        shape = RoundedCornerShape(16.dp)
    )
}
