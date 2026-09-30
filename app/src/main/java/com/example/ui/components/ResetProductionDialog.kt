package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.CairoFontFamily
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberDarkCardElevated
import com.example.ui.theme.CyberDarkSurface
import com.example.ui.theme.PaymentRed
import com.example.ui.theme.StatusOnline
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

@Composable
fun ResetProductionDialog(
    onDismissRequest: () -> Unit,
    onConfirmReset: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = CyberDarkSurface,
            tonalElevation = 0.dp,
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .border(1.5.dp, PaymentRed.copy(alpha = 0.6f), RoundedCornerShape(22.dp))
                .testTag("reset_production_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                // Header with Alert Icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(PaymentRed.copy(alpha = 0.18f))
                            .border(1.dp, PaymentRed.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            tint = PaymentRed,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "تهيئة بيئة العمل الفعلية",
                            color = TextPrimaryDark,
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "حذف كافة البيانات التجريبية والافتراضية",
                            color = StatusWarning,
                            fontFamily = CairoFontFamily,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "سيقوم هذا الإجراء بتفريغ وتنظيف قاعدة بيانات التطبيق تماماً ليصبح جاهزاً للعمل الميداني الفعلي والربط الحقيقي مع شبكتك الخاصة من الصفر، وسيختفي زر التهيئة نهائياً:",
                    color = TextPrimaryDark,
                    fontFamily = CairoFontFamily,
                    fontSize = 12.5.sp,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Detailed Checklist of What Will Be Cleared
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CyberDarkCardElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ResetInfoRow(
                            icon = Icons.Default.Warning,
                            iconColor = PaymentRed,
                            text = "حذف جميع كروت الهوتسبوت التجريبية ودفعاتها السابقة"
                        )
                        ResetInfoRow(
                            icon = Icons.Default.Warning,
                            iconColor = PaymentRed,
                            text = "حذف نقاط البيع والبقالات الوهمية وتصفير المديونيات"
                        )
                        ResetInfoRow(
                            icon = Icons.Default.Warning,
                            iconColor = PaymentRed,
                            text = "حذف سندات القبض والصرف التجريبية المسجلة مسبقاً"
                        )
                        ResetInfoRow(
                            icon = Icons.Default.Warning,
                            iconColor = PaymentRed,
                            text = "حذف أجهزة وسيرفرات الشبكة الافتراضية"
                        )
                        ResetInfoRow(
                            icon = Icons.Default.Done,
                            iconColor = StatusOnline,
                            text = "الإبقاء على حساب المالك الحقيقي (أو حساب جوجل النشط) كمدير عام"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Confirmation Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = TextSecondaryDark
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("cancel_reset_button")
                    ) {
                        Text(
                            text = "إلغاء",
                            fontFamily = CairoFontFamily,
                            color = TextSecondaryDark,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        onClick = onConfirmReset,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PaymentRed),
                        modifier = Modifier
                            .weight(2f)
                            .height(46.dp)
                            .testTag("confirm_reset_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.RocketLaunch,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "تفريغ والتحول للوضع الفعلي",
                                fontFamily = CairoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResetInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = TextPrimaryDark,
            fontFamily = CairoFontFamily,
            fontSize = 11.5.sp
        )
    }
}
