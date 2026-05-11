package cn.landingsite.bc

import android.Manifest
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


object BleConnectManager {
    private const val TAG = "BleConnectManager"

    private val _gattReady = MutableStateFlow(false)
    val gattReady: StateFlow<Boolean> = _gattReady.asStateFlow()

    private var bluetoothGatt: BluetoothGatt? = null

    private val gattCallback = object : BluetoothGattCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "BLE Connected")
                gatt?.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.e(TAG, "BLE Disconnected")
                _gattReady.value = false
                closeGatt()
            }
        }

        // 服务发现完成
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "GATT success, 可以开始读写特征值做配置")
                bluetoothGatt = gatt
                _gattReady.value = true
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connectDevice(context: Context, macAddr: String): Boolean {
        if (macAddr.isBlank()) return false

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE)
                as? BluetoothManager ?: run {
            Log.e(TAG, "获取蓝牙管理器失败")
            return false
        }
        val bluetoothAdapter = bluetoothManager.adapter

        if (!bluetoothAdapter.isEnabled) {
            Log.e(TAG, "蓝牙未开启")
            return false
        }

        return try {
            val device = bluetoothAdapter.getRemoteDevice(macAddr)
            closeGatt()
            bluetoothGatt = device.connectGatt(context, false, gattCallback)
            Log.i(TAG, "开始连接设备：$macAddr")
            true
        } catch (e: Exception) {
            Log.e(TAG, "连接异常：${e.message}")
            false
        }
    }

    // 关闭连接
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun closeGatt() {
        bluetoothGatt?.close()
        bluetoothGatt = null
        Log.i(TAG, "已关闭GATT连接")
    }

    // 获取当前Gatt对象，后续配置读写用
    fun getGatt(): BluetoothGatt? = bluetoothGatt
}