package cn.landingsite.bc

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private var leScanCallback: ScanCallback? = null
    private val TAG = javaClass.simpleName

    private fun requestBlePermissions() {
        val list = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            list.add(Manifest.permission.BLUETOOTH_SCAN)
            list.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        // 用启动器发起请求，不用老的 requestPermissions
        btPermissionLauncher.launch(list.toTypedArray())
    }

    // 蓝牙权限请求启动器
    private val btPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            // 遍历所有权限，看是不是全部允许
            val allGranted = result.all { it.value }
            if (allGranted) {
                Log.i("BT_PERM", "所有蓝牙权限已授权")
                // 在这里 再初始化 BluetoothManager、拿 Adapter
            } else {
                Log.e("BT_PERM", "蓝牙权限有被拒绝")
            }
        }
    private var bluetoothAdapter: BluetoothAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate.......")
        setContent {
            BeaconScanPage()
        }
        Log.i(TAG, "requesting BLE permissions")
        requestBlePermissions();
        Log.i(TAG, "init Bluetooth")
        val bluetoothManager =
            ContextCompat.getSystemService(this, BluetoothManager::class.java)
        if (bluetoothManager != null) {
            Log.i(TAG, "got bluetoothManager")
            bluetoothAdapter = bluetoothManager.adapter
            Log.i(TAG, "got bluetoothAdapter: " + (bluetoothAdapter != null))
        } else {
            Log.w(TAG, "XX");
        }

    }

    fun startBleScan(onFound: (IBeacon) -> Unit) {
        Log.i(TAG, "starting to scan..")
        val adapter = bluetoothAdapter ?: return
        if (!adapter.isEnabled) return
        Log.i(TAG, "BLE adapter is enabled")
        val settings =
            ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

        leScanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                result?.also { result ->
                    val device = result.device;
                    val mac = device.address;
                    if (mac.equals("CF:2F:3C:9E:1F:63", true)) {
                        Log.d(TAG, "mac: " + device.address + ", rssi:" + result.rssi);
                        val scanned = result.scanRecord?.bytes
                        scanned?.let { bytes ->
                            val hex = bytes.joinToString("") {
                                String.format("%02X", it)
                            }
                            Log.i(TAG, hex)
                        }
                    }
                } ?: return
                val device = result.device;
                val mac = device.address;
                val rssi = result.rssi
                val scanned = result.scanRecord?.bytes
                if (scanned != null) {
                    val beacon = parseIBeacon(scanned, mac, rssi)?.let { onFound(it) }
                }
            }
        }
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "no permission");
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        bluetoothAdapter?.bluetoothLeScanner?.startScan(null, settings, leScanCallback)
    }

    fun stopBleScan() {
        Log.i(TAG, "stopping BLE scan");
        leScanCallback?.let {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "no permission2");
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(it)
        }
    }

    fun parseIBeacon(rawBytes: ByteArray, mac: String, rssi: Int): IBeacon? {
        // iBeacon 特征：Apple 0x004C + 0215
        val pattern = byteArrayOf(0x4C, 0x00, 0x02, 0x15)

        // 遍历寻找特征头位置
        for (i in 0 until rawBytes.size - pattern.size) {
            var match = true
            for (j in pattern.indices) {
                if (rawBytes[i + j] != pattern[j]) {
                    match = false
                    break
                }
            }
            if (!match) continue

            // 特征头命中，往后读21字节：16UUID + 2Major + 2Minor + 1TxPower
            val base = i + pattern.size
            if (base + 21 > rawBytes.size) return null

            // 16字节 UUID
            val uuidBuf = rawBytes.copyOfRange(base, base + 16)
            val uuid = uuidBuf.joinToString("") { String.format("%02X", it) }
                .replace(Regex("(.{8})(.{4})(.{4})(.{4})(.{12})"), "$1-$2-$3-$4-$5")

            // Major 2字节
            val major = (rawBytes[base + 16].toInt() and 0xFF) shl 8 or
                    (rawBytes[base + 17].toInt() and 0xFF)
            // Minor 2字节
            val minor = (rawBytes[base + 18].toInt() and 0xFF) shl 8 or
                    (rawBytes[base + 19].toInt() and 0xFF)

            // TxPower 1字节
            val txPower = rawBytes[base + 20].toInt()

            return IBeacon(uuid, major, minor, txPower, mac, rssi)
        }
        // 没找到iBeacon特征
        return null
    }

    private fun bytesToUuid(bytes: ByteArray): String {
        val hex = bytes.joinToString("") { "%02x".format(it) }
        return "${hex.take(8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-${
            hex.substring(
                16, 20
            )
        }-${hex.drop(20)}"
    }

    override fun onDestroy() {
        super.onDestroy()
        stopBleScan()
    }
}

@Composable
fun BeaconScanPage() {
    val activity = LocalContext.current as MainActivity
    val beaconList = remember { mutableStateListOf<IBeacon>() }
    val scanning = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(
            onClick = {
                if (!scanning.value) {
                    beaconList.clear()
                    activity.startBleScan { beacon ->
                        if (beaconList.none { it.mac == beacon.mac }) {
                            beaconList.add(beacon)
                        }
                    }
                    scanning.value = true
                } else {
                    activity.stopBleScan()
                    scanning.value = false
                }
            }, modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (scanning.value) "停止扫描" else "开始扫描 Beacon")
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp)
        ) {
            items(beaconList) { item ->
                BeaconListItem(item)
            }
        }
    }
}

@Composable
fun BeaconListItem(beacon: IBeacon) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("MAC: ${beacon.mac}")
            Text("UUID: ${beacon.uuid}")
            Text("Major: ${beacon.major}  Minor: ${beacon.minor}")
            Text("RSSI: ${beacon.rssi}")
        }
    }
}