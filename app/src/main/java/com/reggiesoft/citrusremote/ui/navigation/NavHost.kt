package com.reggiesoft.citrusremote.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.reggiesoft.citrusremote.ui.control.ControlScreen
import com.reggiesoft.citrusremote.ui.discovery.DiscoveryScreen
import com.reggiesoft.citrusremote.ui.discovery.DiscoveryViewModel
import com.reggiesoft.citrusremote.ui.pairing.PairingScreen
import com.reggiesoft.citrusremote.ui.splash.SplashScreen
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object Routes {
    const val SPLASH = "splash"
    const val DISCOVERY = "discovery"
    const val PAIRING = "pairing/{deviceIp}/{deviceName}"
    const val CONTROL = "control/{deviceIp}/{deviceName}"

    fun pairingRoute(deviceIp: String, deviceName: String): String =
        "pairing/${deviceIp.urlEncoded()}/${deviceName.urlEncoded()}"

    fun controlRoute(deviceIp: String, deviceName: String): String =
        "control/${deviceIp.urlEncoded()}/${deviceName.urlEncoded()}"
}

@Composable
fun CitrusRemoteNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        modifier = modifier
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onAnimationComplete = {
                    navController.navigate(Routes.DISCOVERY) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.DISCOVERY) {
            val discoveryViewModel: DiscoveryViewModel = hiltViewModel()
            DiscoveryScreen(
                viewModel = discoveryViewModel,
                onDeviceSelected = { device ->
                    if (discoveryViewModel.hasUsableCredentials(device.address)) {
                        navController.navigate(Routes.controlRoute(device.address, device.name))
                    } else {
                        navController.navigate(Routes.pairingRoute(device.address, device.name))
                    }
                }
            )
        }

        composable(
            route = Routes.PAIRING,
            arguments = listOf(
                navArgument("deviceIp") { type = NavType.StringType },
                navArgument("deviceName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val deviceIp = backStackEntry.decodedArgument("deviceIp")
            val deviceName = backStackEntry.decodedArgument("deviceName")
            PairingScreen(
                deviceIp = deviceIp,
                deviceName = deviceName,
                onPairSuccess = {
                    navController.navigate(Routes.controlRoute(deviceIp, deviceName)) {
                        popUpTo(Routes.DISCOVERY)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.CONTROL,
            arguments = listOf(
                navArgument("deviceIp") { type = NavType.StringType },
                navArgument("deviceName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val deviceIp = backStackEntry.decodedArgument("deviceIp")
            val deviceName = backStackEntry.decodedArgument("deviceName")
            ControlScreen(
                deviceIp = deviceIp,
                deviceName = deviceName,
                onBack = {
                    navController.popBackStack(Routes.DISCOVERY, inclusive = false)
                },
                onDeviceForgotten = {
                    navController.popBackStack(Routes.DISCOVERY, inclusive = false)
                }
            )
        }
    }
}

private fun NavBackStackEntry.decodedArgument(key: String): String {
    val raw = arguments?.getString(key).orEmpty()
    return URLDecoder.decode(raw, StandardCharsets.UTF_8.name())
}

private fun String.urlEncoded(): String = URLEncoder.encode(this, StandardCharsets.UTF_8.name())

