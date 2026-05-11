package cn.landingsite.bc

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.UUID

class MainActivity : ComponentActivity() {

    private val TAG = javaClass.simpleName

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var leScanCallback: ScanCallback? = null

    private val uuids = listOf(
        UUID.fromString("0000B0FF-0000-1000-8000-00805F9B34FB")
    )

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate.......")
        setContent {
            AppNavHost()
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

    fun startBleScan(onFound: (Beacon) -> Unit) {
        Log.i(TAG, "starting to scan..")
        val adapter = bluetoothAdapter ?: return
        if (!adapter.isEnabled) return
        Log.i(TAG, "BLE adapter is enabled")

        val filters = mutableListOf<ScanFilter>()
        for (uuid in uuids) {
            val filter = ScanFilter.Builder().setServiceUuid(ParcelUuid(uuid)).build()
            filters.add(filter)
        }
        val settings =
            ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

        leScanCallback = object : ScanCallback() {
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                result?.also { result ->
                    val device = result.device;
                    val mac = device.address;
                    if (mac.equals("CF:2F:3C:9E:1F:63", true)) {
//                        Log.d(TAG, "mac: " + device.address + ", rssi:" + result.rssi);
                        val scanned = result.scanRecord?.bytes
                        scanned?.let { bytes ->
                            val hex = bytes.joinToString("") {
                                String.format("%02X", it)
                            }
//                            Log.i(TAG, hex)
                        }
                    }
                } ?: return
                val device = result.device;
                val name = device?.name ?: ""
                val mac = device.address;
                val rssi = result.rssi
                val bytes = result.scanRecord?.bytes ?: byteArrayOf()
                val connectable = result.isConnectable
                if (rssi >= -80) {
                    onFound(Beacon(name, mac, rssi, bytes, connectable))
                }
            }
        }
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "no permission");
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

