package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.InventoryItemEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.MikroTikPrimary

@Composable
fun InventoryScreen(viewModel: MainViewModel, onNavigateToStudio: () -> Unit, onNavigateToBatches: () -> Unit) {
    val items by viewModel.inventoryItems.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var itemToClone by remember { mutableStateOf<InventoryItemEntity?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "مخزن الكروت (الرصيد الفعلي)",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("إدارة كروت المستودع الجاهزة", fontSize = 12.sp, color = Color.Gray)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = onNavigateToStudio,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("استوديو الكروت", color = MikroTikPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onNavigateToBatches,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("سجل الدفعات", color = MikroTikPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("المخزن فارغ حالياً. أضف كروت للبدء.", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(items) { item ->
                        InventoryItemCard(
                            item = item,
                            onClone = {
                                itemToClone = item
                                showAddDialog = true
                            }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                itemToClone = null
                showAddDialog = true
            },
            containerColor = MikroTikPrimary,
            contentColor = Color.White,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "إضافة للمخزن")
        }
    }

    if (showAddDialog) {
        AddInventoryDialog(
            itemToClone = itemToClone,
            onDismiss = {
                showAddDialog = false
                itemToClone = null
            },
            onConfirm = { packageName, qty, wholesale, retail ->
                viewModel.saveInventoryItem(packageName, qty, wholesale, retail)
                showAddDialog = false
                itemToClone = null
            }
        )
    }
}

@Composable
fun InventoryItemCard(
    item: InventoryItemEntity,
    onClone: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.packageName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("سعر الجملة: ${item.wholesalePrice.toInt()} ريال", fontSize = 12.sp, color = Color.Gray)
                Text("سعر الجمهور: ${item.retailPrice.toInt()} ريال", fontSize = 12.sp, color = Color.Gray)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onClone) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "استنساخ الباقة وتفاصيلها",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("الرصيد", fontSize = 10.sp, color = Color.White)
                        Text("${item.quantityAvailable}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun AddInventoryDialog(
    itemToClone: InventoryItemEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (packageName: String, qty: Int, wholesale: Double, retail: Double) -> Unit
) {
    var isSaving by remember { mutableStateOf(false) }
    var packageName by remember(itemToClone) { mutableStateOf(itemToClone?.packageName ?: "") }
    var quantity by remember(itemToClone) { mutableStateOf(if (itemToClone != null) "100" else "") }
    var wholesalePrice by remember(itemToClone) { mutableStateOf(itemToClone?.let { it.wholesalePrice.toInt().toString() } ?: "") }
    var retailPrice by remember(itemToClone) { mutableStateOf(itemToClone?.let { it.retailPrice.toInt().toString() } ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (itemToClone != null) "استنساخ باقة كروت وإضافتها للمخزن" else "إضافة كروت للمخزن",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = packageName,
                    onValueChange = { packageName = it },
                    label = { Text("الباقة (مثال: فئة 200 ريال)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("عدد الكروت المضافة") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = wholesalePrice,
                    onValueChange = { wholesalePrice = it },
                    label = { Text("سعر الجملة (ريال)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = retailPrice,
                    onValueChange = { retailPrice = it },
                    label = { Text("سعر البيع للجمهور (ريال)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantity.toIntOrNull() ?: 0
                    val wPrice = wholesalePrice.toDoubleOrNull() ?: 0.0
                    val rPrice = retailPrice.toDoubleOrNull() ?: 0.0
                    if (!isSaving && packageName.isNotBlank() && qty > 0) {
                        isSaving = true
                        onConfirm(packageName.trim(), qty, wPrice, rPrice)
                    }
                },
                enabled = !isSaving && packageName.isNotBlank() && (quantity.toIntOrNull() ?: 0) > 0,
                colors = ButtonDefaults.buttonColors(containerColor = MikroTikPrimary)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("جاري الحفظ والإضافة...", color = Color.White, fontWeight = FontWeight.Bold)
                } else {
                    Text(if (itemToClone != null) "حفظ الباقة المستنسخة" else "إضافة للمخزن", fontWeight = FontWeight.Bold)
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
