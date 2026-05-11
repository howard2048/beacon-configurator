package cn.landingsite.bc

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeaconListPage(
    onItemGoConfig: () -> Unit
) {
    val activity = LocalContext.current as MainActivity
    val beacons = remember { mutableStateListOf<Beacon>() }
    var isScanning by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    val refreshState = rememberPullToRefreshState()
    val scope = rememberCoroutineScope()

    var scanCountdownJob by remember { mutableStateOf<Job?>(null) }
    val scanDuration = 5000L

    val colorScheme = MaterialTheme.colorScheme

    val isGattReady by BleConnectManager.gattReady.collectAsStateWithLifecycle()
    if (isGattReady) {
        Log.i(TAG, "gatt is ready........")
    }

    val handleConnectCLick = {

    }
    MaterialTheme(
        colorScheme = darkColorScheme()
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Beacon Configurator",
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {}) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    },
                )
            }
        ) { innerPadding ->
            PullToRefreshBox(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                isRefreshing = isRefreshing,
                onRefresh = {
                    scope.launch {
                        Log.i(TAG, "refreshing...")
                        if (isScanning) {
                            scanCountdownJob?.cancel()
                        } else {
                            isScanning = true
                            Log.i(TAG, "scan for ${scanDuration / 1000} seconds ")
                            beacons.clear()
                            activity.startBleScan { beacon ->
                                val pos = beacons.indexOfFirst { it.mac == beacon.mac }
                                if (pos == -1) {
                                    beacons.add(beacon)
                                } else {
                                    beacons[pos] = beacon
                                }
//                        beacons.sortByDescending { it.rssi }
                            }
                        }
                        scanCountdownJob = scope.launch {
                            delay(scanDuration)
                            Log.i(TAG, "stop scan")
                            activity.stopBleScan()
                            isScanning = false
                        }
                        isRefreshing = false
                        refreshState.animateToHidden()
                    }


//            if (!isScanning.value) {
//                beacons.clear()
//                activity.startBleScan { beacon ->
//                    // 先找列表里有没有同MAC设备
//                    val pos = beacons.indexOfFirst { it.mac == beacon.mac }
//                    if (pos == -1) {
//                        // 没有：新增
//                        beacons.add(beacon)
//                    } else {
//                        // 已有：直接覆盖更新最新RSSI和信息
//                        beacons[pos] = beacon
//                    }
////                        beacons.sortByDescending { it.rssi }
//                }
//                isScanning.value = true
//            } else {
////                    beacons.clear()
//                activity.stopBleScan()
//                isScanning.value = false
//            }
                },
                state = refreshState
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    items(beacons) { item ->
                        BeaconListItem(beacon = item, onConnectClick = { beacon ->
                            if (ActivityCompat.checkSelfPermission(
                                    activity,
                                    Manifest.permission.BLUETOOTH_CONNECT
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                return@BeaconListItem
                            }
//                    val result = BleConnectManager.connectDevice(activity, beacon.mac)
                            handleConnectCLick()
                            onItemGoConfig()
                        })
                    }
                }

            }
        }
    }


}

const val TAG = "X"

@Composable
fun BeaconListItem(beacon: Beacon, onConnectClick: (Beacon) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(0.7f)) {
                Text(
                    text = beacon.name.ifBlank { "N/A" },
                    color = if (beacon.name.isBlank()) Color.Gray else Color.Unspecified,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(text = beacon.mac)
                Text(text = "RSSI: ${beacon.rssi}", fontSize = 12.sp)
            }
            Button(
                onClick = {
                    onConnectClick(beacon)
                },
                enabled = beacon.connectable,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(text = "Connect", fontSize = 12.sp)
            }
        }
    }
}