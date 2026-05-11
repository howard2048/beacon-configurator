package cn.landingsite.bc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 完整封装的 Beacon 配置底部弹窗
 * @param show 是否显示
 * @param onDismiss 关闭回调
 * @param onSave 保存回调：UUID, Major, Minor, TxPower
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeaconConfigModalBottomSheet(
    show: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 控制弹出/收起
    LaunchedEffect(show) {
        if (show) sheetState.expand() else sheetState.hide()
    }

    if (sheetState.isVisible) {
        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { /* 禁止外部关闭 */ }
        ) {
            // 内部自有状态，不外露
            var uuidText by remember { mutableStateOf("") }
            var majorText by remember { mutableStateOf("") }
            var minorText by remember { mutableStateOf("") }
            var txPowerText by remember { mutableStateOf("") }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = "Beacon 配置",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                // UUID 多行适配长文本
                OutlinedTextField(
                    value = uuidText,
                    onValueChange = { uuidText = it },
                    label = { Text("UUID") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    singleLine = false
                )

                // Major + Minor 同行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = majorText,
                        onValueChange = { majorText = it },
                        label = { Text("Major") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = minorText,
                        onValueChange = { minorText = it },
                        label = { Text("Minor") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                // Tx Power
                OutlinedTextField(
                    value = txPowerText,
                    onValueChange = { txPowerText = it },
                    label = { Text("TX Power") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // 取消 / 保存
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("取消")
                    }
                    Button(
                        onClick = {
                            onSave(uuidText, majorText, minorText, txPowerText)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }
}