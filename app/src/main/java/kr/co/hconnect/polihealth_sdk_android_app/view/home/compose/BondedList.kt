package kr.co.hconnect.polihealth_sdk_android_app.view.home.compose

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kr.co.hconnect.polihealth_sdk_android_app.PoliBLE
import kr.co.hconnect.polihealth_sdk_android_app.viewmodel.BondedDevicesViewModel
import kr.co.hconnect.polihealth_sdk_android_app.viewmodel.DeviceViewModel

@Composable
fun BondedList(
    bondedDeviceViewModel: BondedDevicesViewModel = viewModel(),
    deviceViewModel: DeviceViewModel = viewModel(),
    navController: NavController
) {
    val bondedList = bondedDeviceViewModel.bondedDevices
    LazyColumn(contentPadding = PaddingValues(horizontal = 15.dp)) {
        items(bondedList.value.size) { index ->
            BondedItem(
                device = bondedList.value[index],
                onClick = {
                    deviceViewModel.device.value = bondedList.value[index]
                    PoliBLE.stopScan()
                    navController.navigate("deviceDetail")
                }
            )
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun BondedItem(
    device: BluetoothDevice,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(Color.Cyan)
            .clickable {
                onClick()
            },
    ) {
        Text(
            text = device.name ?: "Unknown Device",
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )
    }
}