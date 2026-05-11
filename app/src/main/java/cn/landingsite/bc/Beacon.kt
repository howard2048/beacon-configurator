package cn.landingsite.bc

open class Beacon(
    val name: String,
    val mac: String,
    val rssi: Int,
    val bytes: ByteArray,
    val connectable: Boolean
)