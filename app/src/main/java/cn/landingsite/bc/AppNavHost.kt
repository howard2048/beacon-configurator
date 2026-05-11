package cn.landingsite.bc

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavHost() {
    val navCtrl = rememberNavController()

    NavHost(
        navController = navCtrl,
        startDestination = "beacon_list"
    ) {
        // 列表页
        composable("beacon_list") {
            BeaconListPage(
                onItemGoConfig = {
                    // 跳转配置页
                    navCtrl.navigate("beacon_config")
                }
            )
        }

        // 配置页
        composable("beacon_config") {
            BeaconConfigPage(
                onBackClick = {
                    // 返回列表页
                    navCtrl.popBackStack()
                },
                onSaveClick = { uuid, major, minor, txPower ->
                    // 这里写你保存配置的逻辑
                    // 保存完自动返回列表页
                    navCtrl.popBackStack()
                }
            )
        }
    }
}