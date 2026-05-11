package cn.landingsite.bc

data class IBeacon(
//    val mac: String,
    val uuid: String,
    val major: Int,
    val minor: Int,
    val txPower: Int,
    val mac: String,
    val rssi: Int
)