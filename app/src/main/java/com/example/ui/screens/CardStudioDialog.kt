package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.cards.CardGenerationEngine
import com.example.data.cards.CardPdfPrintManager
import com.example.data.cards.CardTemplateConfig
import com.example.data.cards.CardThemesLibrary
import com.example.data.local.entity.CardEntity
import com.example.ui.theme.MikroTikNavy
import com.example.ui.theme.MikroTikPrimary

@Composable
fun CardStudioDialog(
    sampleCards: List<CardEntity>,
    initialCard: CardEntity? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("معاينة وتصميم القالب", "سكربت ميكروتك (CLI)", "تنظيف الكروت المنتهية")

    // Template Configuration State
    var selectedTheme by remember { mutableStateOf(CardThemesLibrary.CYBER_NEON) }
    var networkTitle by remember { mutableStateOf("شبكة سام ميكروتك الذكية") }
    var supportPhone by remember { mutableStateOf("777-123456") }
    var loginDomain by remember { mutableStateOf("wifi.net") }
    var showQrCode by remember { mutableStateOf(true) }
    var showPriceBadge by remember { mutableStateOf(true) }
    var showPasswordOrPin by remember { mutableStateOf(true) }
    var cornerRadiusDp by remember { mutableIntStateOf(12) }

    val currentConfig = CardTemplateConfig(
        templateId = selectedTheme.id,
        theme = selectedTheme,
        networkTitle = networkTitle,
        supportPhone = supportPhone,
        loginDomain = loginDomain,
        showQrCode = showQrCode,
        showPriceBadge = showPriceBadge,
        showPasswordOrPin = showPasswordOrPin,
        cornerRadiusDp = cornerRadiusDp
    )

    // Fallback sample card if database is empty
    val previewCard = initialCard ?: sampleCards.firstOrNull() ?: CardEntity(
        id = 1,
        batchId = 1,
        username = "sam74921",
        password = "4819",
        categoryName = "باقة السوبر 4GB / 24H",
        retailPrice = 500.0,
        wholesalePrice = 450.0,
        status = "AVAILABLE"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.98f),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Palette, contentDescription = null, tint = MikroTikPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("استوديو القوالب وتصدير ميكروتك", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("تصميم كروت احترافية وطباعة A4 ومزامنة الأوامر", fontSize = 11.sp, color = Color.Gray)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontSize = 11.sp, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                when (selectedTab) {
                    0 -> TemplateDesignTab(
                        previewCard = previewCard,
                        config = currentConfig,
                        selectedTheme = selectedTheme,
                        onSelectTheme = { selectedTheme = it },
                        networkTitle = networkTitle,
                        onNetworkTitleChange = { networkTitle = it },
                        supportPhone = supportPhone,
                        onSupportPhoneChange = { supportPhone = it },
                        loginDomain = loginDomain,
                        onLoginDomainChange = { loginDomain = it },
                        showQrCode = showQrCode,
                        onShowQrCodeChange = { showQrCode = it },
                        showPriceBadge = showPriceBadge,
                        onShowPriceBadgeChange = { showPriceBadge = it },
                        showPasswordOrPin = showPasswordOrPin,
                        onShowPasswordOrPinChange = { showPasswordOrPin = it },
                        cornerRadiusDp = cornerRadiusDp,
                        onCornerRadiusChange = { cornerRadiusDp = it },
                        onPrintA4 = {
                            val cardsToPrint = if (sampleCards.isNotEmpty()) sampleCards else listOf(previewCard)
                            CardPdfPrintManager.printCardsSheet(context, cardsToPrint, currentConfig)
                            Toast.makeText(context, "جاري تجهيز صفحة الطباعة A4...", Toast.LENGTH_SHORT).show()
                        }
                    )
                    1 -> MikroTikScriptTab(
                        cards = sampleCards.ifEmpty { listOf(previewCard) },
                        onCopy = { script ->
                            clipboard.setText(AnnotatedString(script))
                            Toast.makeText(context, "تم نسخ سكريبت ميكروتك إلى الحافظة بنجاح ✓", Toast.LENGTH_SHORT).show()
                        }
                    )
                    2 -> ExpiredCleanupTab(
                        onCopy = { script ->
                            clipboard.setText(AnnotatedString(script))
                            Toast.makeText(context, "تم نسخ سكريبت التنظيف إلى الحافظة ✓", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MikroTikPrimary)
            ) {
                Text("إغلاق الاستوديو")
            }
        }
    )
}

@Composable
fun TemplateDesignTab(
    previewCard: CardEntity,
    config: CardTemplateConfig,
    selectedTheme: com.example.data.cards.CardThemeStyle,
    onSelectTheme: (com.example.data.cards.CardThemeStyle) -> Unit,
    networkTitle: String,
    onNetworkTitleChange: (String) -> Unit,
    supportPhone: String,
    onSupportPhoneChange: (String) -> Unit,
    loginDomain: String,
    onLoginDomainChange: (String) -> Unit,
    showQrCode: Boolean,
    onShowQrCodeChange: (Boolean) -> Unit,
    showPriceBadge: Boolean,
    onShowPriceBadgeChange: (Boolean) -> Unit,
    showPasswordOrPin: Boolean,
    onShowPasswordOrPinChange: (Boolean) -> Unit,
    cornerRadiusDp: Int,
    onCornerRadiusChange: (Int) -> Unit,
    onPrintA4: () -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().height(420.dp)
    ) {
        // Live Interactive Preview
        item {
            Text("المعاينة المباشرة للكرت (Live Card Preview):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            InteractiveCardPreview(card = previewCard, templateConfig = config)
        }

        // Action: Print / PDF
        item {
            Button(
                onClick = onPrintA4,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
            ) {
                Icon(Icons.Default.Print, contentDescription = null, tint = Color(0xFF38BDF8))
                Spacer(modifier = Modifier.width(8.dp))
                Text("طباعة الكروت على ورقة A4 (24 كرت)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        // Theme Palette Selector
        item {
            Text("اختر ثيم وبصريات الكرت الجاهزة:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(CardThemesLibrary.ALL_THEMES) { theme ->
                    val isSelected = selectedTheme.id == theme.id
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MikroTikPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .clickable { onSelectTheme(theme) }
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MikroTikPrimary else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(theme.accentColor)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(theme.nameArabic, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Custom Info Inputs
        item {
            OutlinedTextField(
                value = networkTitle,
                onValueChange = onNetworkTitleChange,
                label = { Text("اسم الشبكة بالكرت") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = supportPhone,
                    onValueChange = onSupportPhoneChange,
                    label = { Text("هاتف الدعم") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = loginDomain,
                    onValueChange = onLoginDomainChange,
                    label = { Text("دومين الدخول") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }

        // Feature Toggles
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("إظهار رمز QR الذكي (تسجيل الدخول التلقائي)", fontSize = 11.sp)
                    Switch(
                        checked = showQrCode,
                        onCheckedChange = onShowQrCodeChange,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("إظهار شارة السعر (Price Badge)", fontSize = 11.sp)
                    Switch(
                        checked = showPriceBadge,
                        onCheckedChange = onShowPriceBadgeChange,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("إظهار حقل PIN كلمة المرور", fontSize = 11.sp)
                    Switch(
                        checked = showPasswordOrPin,
                        onCheckedChange = onShowPasswordOrPinChange,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MikroTikScriptTab(
    cards: List<CardEntity>,
    onCopy: (String) -> Unit
) {
    var scriptType by remember { mutableStateOf("hotspot") } // "hotspot" or "userman"
    var profileName by remember { mutableStateOf("default") }
    var serverName by remember { mutableStateOf("all") }

    val generatedScript = remember(cards, scriptType, profileName, serverName) {
        if (scriptType == "hotspot") {
            CardGenerationEngine.generateRouterOsHotspotScript(
                cards = cards,
                profileName = profileName,
                serverName = serverName
            )
        } else {
            CardGenerationEngine.generateUserManagerV7Script(
                cards = cards,
                userGroup = profileName
            )
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().height(420.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { scriptType = "hotspot" },
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (scriptType == "hotspot") MikroTikPrimary.copy(alpha = 0.1f) else Color.Transparent
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text("سيرفر Hotspot العادي", fontSize = 11.sp)
            }
            OutlinedButton(
                onClick = { scriptType = "userman" },
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (scriptType == "userman") MikroTikPrimary.copy(alpha = 0.1f) else Color.Transparent
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text("User Manager v7", fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = profileName,
                onValueChange = { profileName = it },
                label = { Text("اسم البروفايل (Profile)") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            if (scriptType == "hotspot") {
                OutlinedTextField(
                    value = serverName,
                    onValueChange = { serverName = it },
                    label = { Text("السيرفر (Server)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF071322))
                .padding(10.dp)
        ) {
            LazyColumn {
                item {
                    Text(
                        text = generatedScript,
                        color = Color(0xFF38BDF8),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { onCopy(generatedScript) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MikroTikPrimary)
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("نسخ السكربت الجاهز للتيرمنال (RouterOS CLI)", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ExpiredCleanupTab(
    onCopy: (String) -> Unit
) {
    var excludeTags by remember { mutableStateOf("admin,keep_admin,vip,bypass") }
    val cleanupScript = remember(excludeTags) {
        CardGenerationEngine.generateExpiredUsersCleanupScript(excludeTags)
    }

    Column(
        modifier = Modifier.fillMaxWidth().height(420.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "تنظيف الكروت المنتهية آلياً (Auto Cleanup Expired Users):",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "هذا السكربت يفحص كروت الهوتسبوت في راوتر الميكروتك ويحذف أي كرت استهلك وقته أو رصيده بالكامل لتفريغ الذاكرة وحماية أداء الراوتر.",
            fontSize = 11.sp,
            color = Color.Gray
        )

        OutlinedTextField(
            value = excludeTags,
            onValueChange = { excludeTags = it },
            label = { Text("استثناء الكلمات الدلالية (Commas)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0F172A))
                .padding(10.dp)
        ) {
            LazyColumn {
                item {
                    Text(
                        text = cleanupScript,
                        color = Color(0xFF34D399),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Button(
            onClick = { onCopy(cleanupScript) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("نسخ سكربت التنظيف وحماية أداء الراوتر", fontWeight = FontWeight.Bold)
        }
    }
}
