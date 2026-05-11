package cn.landingsite.bc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * Beacon 全屏配置页
 * 适配 Navigation 全屏跳转，替代小 Dialog / BottomSheet
 */
@Composable
fun BeaconConfigPage(
    onBackClick: () -> Unit,
    onSaveClick: (String, String, String, String) -> Unit
) {
    // 内部状态
    var uuid by remember { mutableStateOf("") }
    var major by remember { mutableStateOf("") }
    var minor by remember { mutableStateOf("") }
    var txPower by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "Beacon 详细配置", modifier = Modifier.padding(top = 10.dp))

        // UUID 长文本输入，支持多行粘贴
        OutlinedTextField(
            value = uuid,
            onValueChange = { uuid = it },
            label = { Text("UUID") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 4,
            singleLine = false,
            placeholder = { Text("请输入/粘贴完整 UUID") }
        )

        // Major + Minor 同行布局
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = major,
                onValueChange = { major = it },
                label = { Text("Major") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = minor,
                onValueChange = { minor = it },
                label = { Text("Minor") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        // TX Power
        OutlinedTextField(
            value = txPower,
            onValueChange = { txPower = it },
            label = { Text("TX Power") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        // 返回 + 保存按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onBackClick,
                modifier = Modifier.weight(1f)
            ) {
                Text("返回")
            }
            Button(
                onClick = {
                    onSaveClick(uuid, major, minor, txPower)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("保存配置")
            }
        }
    }
}