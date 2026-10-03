package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import com.example.data.repository.NetworkRepository
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material.icons.filled.TwoWheeler
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.UserEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.InvestmentGold
import com.example.ui.theme.MikroTikNavy
import com.example.ui.theme.MikroTikPrimary
import com.example.ui.theme.PaymentRed
import com.example.ui.theme.ProfitEmerald
import com.example.ui.theme.StatusDistributed
import com.example.ui.theme.StatusOnline

@Composable
fun DistributorsManagementScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val distributors by viewModel.distributors.collectAsState()
    val allUsers by viewModel.users.collectAsState()
    val retailers by viewModel.retailers.collectAsState()
    val distributedCardsCount by viewModel.distributedCardsCount.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingDistributor by remember { mutableStateOf<UserEntity?>(null) }
    var deletingDistributor by remember { mutableStateOf<UserEntity?>(null) }

    val activeDistributors = distributors.count { it.isActive }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("distributors_management_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header & Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF38BDF8).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TwoWheeler,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "إدارة الموزعين والوكلاء",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Text(
                        text = "إضافة موزع وتفويضه عبر بريده الإلكتروني لتوزيع الكروت والبقالات",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("add_distributor_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إضافة موزع", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 2. Statistical KPI Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DistributorKpiCard(
                    title = "إجمالي الموزعين",
                    value = "${distributors.size}",
                    subtitle = "$activeDistributors نشط حالياً",
                    color = Color(0xFF0284C7),
                    icon = Icons.Default.Group,
                    modifier = Modifier.weight(1f)
                )

                DistributorKpiCard(
                    title = "نقاط البيع",
                    value = "${retailers.size}",
                    subtitle = "بقالات مسجلة",
                    color = ProfitEmerald,
                    icon = Icons.Default.Store,
                    modifier = Modifier.weight(1f)
                )

                DistributorKpiCard(
                    title = "كروت موزعة",
                    value = "$distributedCardsCount",
                    subtitle = "قيد التوزيع والبيع",
                    color = StatusDistributed,
                    icon = Icons.Default.Receipt,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3. Informational Explanation Banner
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1D33)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF38BDF8).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "كيف تعمل صلاحية الموزع المعتمد؟",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "عندما يثبت الموزع التطبيق ويسجل الدخول ببريده الإلكتروني المعتمد، يفتح له التطبيق فوراً واجهة الموزع الخاصة: توزيع الكروت وتسليمها للبقالات، إضافة بقالات جديدة، وتحصيل السندات النقدية. ويتم حجب إعدادات راوترات الميكروتك وإدارة الشركاء لحماية أمن واستقرار شبكتك.",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // 4. Distributors List Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "سجل الموزعين والوكلاء المعتمدين (${distributors.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when {
                        currentUser?.role == "DISTRIBUTOR" -> InvestmentGold.copy(alpha = 0.2f)
                        currentUser != null -> Color(0xFF1E293B)
                        else -> Color(0xFF0F172A)
                    }
                ) {
                    Text(
                        text = when {
                            currentUser?.role == "DISTRIBUTOR" -> "أنت تتصفح حالياً بدور: موزع معتمد 🛵"
                            currentUser?.role == "OWNER" || NetworkRepository.isSuperAdminEmail(currentUser?.email ?: "") -> "أنت تتصفح بصلاحية: المدير العام 👑"
                            currentUser != null -> "أنت تتصفح بحساب: ${currentUser?.fullName} (${currentUser?.role})"
                            else -> "وضع المعاينة (غير مسجل الدخول) 👤"
                        },
                        color = if (currentUser?.role == "DISTRIBUTOR") InvestmentGold else Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // 5. Distributors List Items
        if (distributors.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Group,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "لم يتم إضافة أي موزع معتمد حتى الآن",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "اضغط على زر 'إضافة موزع' لتعيين أول موزع وربط بريده الإلكتروني",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        } else {
            items(distributors, key = { it.id }) { distributor ->
                DistributorCardItem(
                    distributor = distributor,
                    isCurrentSession = currentUser?.id == distributor.id,
                    onSwitchToThisUser = {
                        viewModel.selectUser(distributor)
                        Toast.makeText(context, "تم التحويل إلى حساب الموزع: ${distributor.fullName} للتجربة والتحقق", Toast.LENGTH_LONG).show()
                    },
                    onToggleActive = {
                        viewModel.toggleDistributorActive(distributor)
                    },
                    onEdit = {
                        editingDistributor = distributor
                    },
                    onDelete = {
                        deletingDistributor = distributor
                    },
                    onShareInvite = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                """
                                مرحباً ${distributor.fullName}،
                                تم اعتمادك كموزع رسمي لشبكة طلقة نت / سام تك 🛵📶.
                                يمكنك تحميل التطبيق وتسجيل الدخول عبر بريدك الإلكتروني المعتمد:
                                📧 ${distributor.email}
                                
                                الصلاحيات المتاحة لك:
                                - توزيع كروت الشحن واستلام دفعاتها
                                - إضافة بقالات ومحلات جديدة كوكلاء
                                - تحصيل السندات المالية وتسجيلها فوراً
                                """.trimIndent()
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "مشاركة بيانات اعتماد الموزع"))
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // Add / Edit Dialog
    if (showAddDialog || editingDistributor != null) {
        DistributorFormDialog(
            initialUser = editingDistributor,
            onDismiss = {
                showAddDialog = false
                editingDistributor = null
            },
            onSave = { updatedUser ->
                viewModel.saveDistributor(updatedUser) {
                    Toast.makeText(context, "تم حفظ بيانات الموزع ومزامنتها بنجاح", Toast.LENGTH_SHORT).show()
                    showAddDialog = false
                    editingDistributor = null
                }
            }
        )
    }

    // Delete Confirmation Dialog
    deletingDistributor?.let { dist ->
        AlertDialog(
            onDismissRequest = { deletingDistributor = null },
            title = { Text("حذف الموزع", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من حذف الموزع '${dist.fullName}'؟ سيتم إلغاء وصوله للتطبيق وإزالة ترخيصه السحابي.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDistributor(dist) {
                            Toast.makeText(context, "تم حذف الموزع بنجاح", Toast.LENGTH_SHORT).show()
                            deletingDistributor = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PaymentRed)
                ) {
                    Text("حذف نهائي")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingDistributor = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun DistributorKpiCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(color.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                softWrap = false,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text(text = subtitle, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun DistributorCardItem(
    distributor: UserEntity,
    isCurrentSession: Boolean,
    onSwitchToThisUser: () -> Unit,
    onToggleActive: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShareInvite: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentSession) Color(0xFF0D253F) else MaterialTheme.colorScheme.surface
        ),
        border = if (isCurrentSession) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF38BDF8)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Avatar, Name, Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (distributor.isActive) Color(0xFF0284C7).copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = if (distributor.isActive) Color(0xFF0284C7) else Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = distributor.fullName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isCurrentSession) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            if (isCurrentSession) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF0284C7)
                                ) {
                                    Text(
                                        text = "حسابك النشط الآن ✓",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = if (distributor.assignedArea.isNotBlank()) "نطاق: ${distributor.assignedArea}" else "موزع ميداني عام",
                            fontSize = 11.sp,
                            color = if (isCurrentSession) Color(0xFF94A3B8) else Color.Gray
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (distributor.isActive) ProfitEmerald.copy(alpha = 0.15f) else PaymentRed.copy(alpha = 0.15f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (distributor.isActive) ProfitEmerald else PaymentRed)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (distributor.isActive) "حساب نشط" else "معطل",
                            color = if (distributor.isActive) ProfitEmerald else PaymentRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Details info grid
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isCurrentSession) Color.White.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Email (Authentication key)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "البريد الإلكتروني للدخول:",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = distributor.email.ifBlank { "لم يُحدد بعد" },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isCurrentSession) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Phone & WhatsApp
                    if (distributor.phone.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Phone,
                                contentDescription = null,
                                tint = ProfitEmerald,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "الهاتف:", fontSize = 10.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = distributor.phone,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isCurrentSession) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Commission Rate
                    if (distributor.commissionRate > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Percent,
                                contentDescription = null,
                                tint = InvestmentGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "نسبة العمولة / الحافز:", fontSize = 10.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${distributor.commissionRate}% من قيمة الكروت",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = InvestmentGold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Switch / Test role button
                OutlinedButton(
                    onClick = onSwitchToThisUser,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isCurrentSession) Color(0xFF38BDF8) else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.SwitchAccount, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isCurrentSession) "الحساب نشط" else "دخول بحسابه",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Share invite
                IconButton(
                    onClick = onShareInvite,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ProfitEmerald.copy(alpha = 0.12f))
                ) {
                    Icon(Icons.Default.Share, contentDescription = "مشاركة الدعوة", tint = ProfitEmerald, modifier = Modifier.size(16.dp))
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Edit
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0284C7).copy(alpha = 0.12f))
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = Color(0xFF0284C7), modifier = Modifier.size(16.dp))
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Delete
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(PaymentRed.copy(alpha = 0.12f))
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = PaymentRed, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun DistributorFormDialog(
    initialUser: UserEntity?,
    onDismiss: () -> Unit,
    onSave: (UserEntity) -> Unit
) {
    var fullName by remember { mutableStateOf(initialUser?.fullName ?: "") }
    var email by remember { mutableStateOf(initialUser?.email ?: "") }
    var phone by remember { mutableStateOf(initialUser?.phone ?: "") }
    var assignedArea by remember { mutableStateOf(initialUser?.assignedArea ?: "") }
    var commissionRateText by remember { mutableStateOf(initialUser?.commissionRate?.takeIf { it > 0 }?.toString() ?: "") }
    var notes by remember { mutableStateOf(initialUser?.notes ?: "") }
    var isActive by remember { mutableStateOf(initialUser?.isActive ?: true) }

    var emailError by remember { mutableStateOf<String?>(null) }
    var nameError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.TwoWheeler,
                    contentDescription = null,
                    tint = Color(0xFF0284C7),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (initialUser == null) "إضافة موزع معتمد جديد" else "تعديل بيانات الموزع",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "أدخل بيانات الموزع بدقة. سيتم استخدام البريد الإلكتروني للتحقق التلقائي عند دخوله للتطبيق:",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                // Full Name
                item {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = {
                            fullName = it
                            if (it.isNotBlank()) nameError = null
                        },
                        label = { Text("الاسم الكامل للموزع *") },
                        placeholder = { Text("مثال: فيصل العواضي") },
                        isError = nameError != null,
                        supportingText = nameError?.let { { Text(it, color = PaymentRed) } },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("distributor_fullname_input")
                    )
                }

                // Email Address
                item {
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            if (it.contains("@")) emailError = null
                        },
                        label = { Text("البريد الإلكتروني للموزع (Google/Firebase) *") },
                        placeholder = { Text("مثال: faisal.dist@gmail.com") },
                        isError = emailError != null,
                        supportingText = emailError?.let { { Text(it, color = PaymentRed) } } ?: {
                            Text("البريد الذي سيستخدمه الموزع لتسجيل الدخول من هاتفه", fontSize = 10.sp, color = Color.Gray)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("distributor_email_input")
                    )
                }

                // Phone
                item {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("رقم الهاتف / الواتساب") },
                        placeholder = { Text("مثال: 771234567") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("distributor_phone_input")
                    )
                }

                // Assigned Area
                item {
                    OutlinedTextField(
                        value = assignedArea,
                        onValueChange = { assignedArea = it },
                        label = { Text("منطقة أو مربع التوزيع المخصص") },
                        placeholder = { Text("مثال: حي الروضة - البقالات الغربية") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Commission Rate %
                item {
                    OutlinedTextField(
                        value = commissionRateText,
                        onValueChange = { commissionRateText = it },
                        label = { Text("نسبة العمولة أو الحافز (%)") },
                        placeholder = { Text("مثال: 5.0") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        leadingIcon = { Icon(Icons.Default.Percent, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Notes
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("ملاحظات إضافية") },
                        placeholder = { Text("مثال: مسؤول عن توزيع كروت الفئات 500 و 1000") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Status Switch
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("تفعيل صلاحية الموزع", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("السماح للموزع بالدخول والعمل على الشبكة", fontSize = 11.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = isActive,
                            onCheckedChange = { isActive = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = ProfitEmerald)
                        )
                    }
                }

                // Permissions Overview Box
                item {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0F243A),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "الصلاحيات الممنوحة تلقائياً للموزع:",
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = ProfitEmerald, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("توزيع كروت الشبكة وتسليمها للبقالات وبيعها", color = Color.White, fontSize = 11.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = ProfitEmerald, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إضافة بقالات ونقاط بيع جديدة وتحديد مواقعها", color = Color.White, fontSize = 11.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = ProfitEmerald, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تحصيل السندات النقدية وقيد سندات القبض", color = Color.White, fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = PaymentRed, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("حجب إعدادات راوترات الميكروتك والأصول والشركاء", color = Color(0xFFFCA5A5), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    var hasError = false
                    if (fullName.isBlank()) {
                        nameError = "يرجى كتابة اسم الموزع"
                        hasError = true
                    }
                    if (email.isBlank() || !email.contains("@")) {
                        emailError = "يرجى كتابة بريد إلكتروني صالح"
                        hasError = true
                    }

                    if (!hasError) {
                        val rate = commissionRateText.toDoubleOrNull() ?: 0.0
                        val user = initialUser?.copy(
                            fullName = fullName.trim(),
                            email = email.trim().lowercase(),
                            phone = phone.trim(),
                            assignedArea = assignedArea.trim(),
                            commissionRate = rate,
                            notes = notes.trim(),
                            isActive = isActive
                        ) ?: UserEntity(
                            username = email.substringBefore("@").trim(),
                            fullName = fullName.trim(),
                            role = "DISTRIBUTOR",
                            email = email.trim().lowercase(),
                            phone = phone.trim(),
                            assignedArea = assignedArea.trim(),
                            commissionRate = rate,
                            notes = notes.trim(),
                            isActive = isActive
                        )
                        onSave(user)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                modifier = Modifier.testTag("submit_distributor_button")
            ) {
                Text("حفظ الموزع")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
