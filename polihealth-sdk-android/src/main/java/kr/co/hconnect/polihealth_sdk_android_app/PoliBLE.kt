package kr.co.hconnect.polihealth_sdk_android_app

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattService
import android.bluetooth.le.ScanResult
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kr.co.hconnect.bluetoothlib.HCBle
import kr.co.hconnect.polihealth_sdk_android_app.api.dto.request.HRSpO2
import kr.co.hconnect.polihealth_sdk_android_app.api.dto.response.SleepResponse
import kr.co.hconnect.polihealth_sdk_android_app.api.sleep.SleepProtocol06API
import kr.co.hconnect.polihealth_sdk_android_app.api.sleep.SleepProtocol07API
import kr.co.hconnect.polihealth_sdk_android_app.api.sleep.SleepProtocol08API
import kr.co.hconnect.polihealth_sdk_android_app.service.sleep.SleepApiService

object PoliBLE {
    private const val TAG = "PoliBLE"
    fun init(context: Context) {
        HCBle.init(context)
    }

    fun startScan(scanDevice: (ScanResult) -> Unit) {
        HCBle.scanLeDevice { device ->
            scanDevice.invoke(device)
        }
    }

    fun stopScan() {
        HCBle.scanStop()
    }

    fun connectDevice(
        context: Context? = null, // bin파일 저장을 위한 임시 컨텍스트
        device: BluetoothDevice,
        onConnState: (state: Int) -> Unit,
        onGattServiceState: (gatt: Int) -> Unit,
        onBondState: (bondState: Int) -> Unit,
        onSubscriptionState: (state: Boolean) -> Unit,
        onReceive: (type: ProtocolType, response: SleepResponse?) -> Unit
    ) {
        HCBle.connectToDevice(
            device = device,
            onConnState = { state ->
                onConnState.invoke(state)
            },
            onGattServiceState = { gatt ->
                onGattServiceState.invoke(gatt)
            },
            onBondState = { bondState ->
                onBondState.invoke(bondState)
            },
            onSubscriptionState = { state ->
                onSubscriptionState.invoke(state)
            },
            onCharacteristicChanged = { byteArray ->
                byteArray?.let {

                    when (it[0]) {
                        0x04.toByte() -> {
                            onReceive.invoke(ProtocolType.PROTOCOL_4_SLEEP_START, null)
                        }

                        0x05.toByte() -> {
                            onReceive.invoke(ProtocolType.PROTOCOL_5_SLEEP_END, null)
                        }

                        0x06.toByte() -> {
                            SleepProtocol06API.addByte(removeFrontTwoBytes(it, 2))
                        }

                        0x07.toByte() -> {
                            CoroutineScope(Dispatchers.IO).launch {
                                val response: SleepResponse.SleepCommResponse? =
                                    SleepApiService().sendProtocol08(context)
                                response?.let {
                                    onReceive.invoke(
                                        ProtocolType.PROTOCOL_8,
                                        response
                                    )
                                }

                                SleepProtocol07API.addByte(removeFrontTwoBytes(it, 2))
                            }
                        }

                        0x08.toByte() -> {
                            CoroutineScope(Dispatchers.IO).launch {
                                val response = SleepApiService().sendProtocol06(context)
                                response?.let {
                                    onReceive.invoke(
                                        ProtocolType.PROTOCOL_6,
                                        response
                                    )
                                }
                                SleepProtocol08API.addByte(removeFrontTwoBytes(it, 2))
                            }
                        }

                        0x09.toByte() -> {
                            CoroutineScope(Dispatchers.IO).launch {
                                val deferProtocol07 = async {
                                    SleepApiService().sendProtocol07(context)
                                }
                                val hrSpO2: HRSpO2 =
                                    HRSpO2Parser.asciiToHRSpO2(removeFrontTwoBytes(it, 1))
                                val deferProtocol09 = async {
                                    SleepApiService().sendProtocol09(hrSpO2)
                                }

                                val responseProtocol07 = deferProtocol07.await()
                                onReceive.invoke(ProtocolType.PROTOCOL_7, responseProtocol07)

                                val responseProtocol09 = deferProtocol09.await()
                                onReceive.invoke(
                                    ProtocolType.PROTOCOL_9_HR_SpO2,
                                    responseProtocol09
                                )
                            }
                        else -> {
                            Log.e(TAG, "Unknown Protocol")
                        }
                    }
                } ?: run {
                    Log.e("PoliBLE", "byteArray is null")
                }
                val hexString =
                    byteArray?.joinToString(separator = " ") { byte -> "%02x".format(byte) }
                Log.d("GATTService", "onCharacteristicChanged: $hexString")
            }
        )
    }


    private fun removeFrontTwoBytes(byteArray: ByteArray, size: Int): ByteArray {
        // 배열의 길이가 2 이상인 경우에만 앞의 2바이트를 제거
        if (byteArray.size > size) {
            return byteArray.copyOfRange(size, byteArray.size)
        }
        // 배열의 길이가 2 이하인 경우 빈 배열 반환
        return ByteArray(0)
    }

    fun disconnectDevice() {
        HCBle.disconnect()
    }

    /**
     * TODO: GATT Service 리스트를 반환합니다.
     * 블루투스가 연결되어 onServicesDiscovered 콜백이 호출 돼야 사용가능합니다.
     * @return
     */
    fun getGattServiceList(): List<BluetoothGattService> {
        return HCBle.getGattServiceList()
    }

    /**
     * TODO: 서비스 UUID를 설정합니다.
     * 사용 하고자 하는 서비스 UUID를 설정합니다.
     * @param uuid
     */
    fun setServiceUUID(uuid: String) {
        HCBle.setServiceUUID(uuid)
    }

    /**
     * TODO: 캐릭터리스틱 UUID를 설정합니다.
     * 사용 하고자 하는 캐릭터리스틱 UUID를 설정합니다.
     * @param characteristicUUID
     */
    fun setCharacteristicUUID(characteristicUUID: String) {
        HCBle.setCharacteristicUUID(characteristicUUID)
    }

    /**
     * TODO: 캐릭터리스틱을 읽습니다.
     * setCharacteristicUUID로 설정된 캐릭터리스틱을 읽습니다.
     */
    fun readCharacteristic() {
        HCBle.readCharacteristic()
    }

    /**
     * TODO: 캐릭터리스틱을 쓰기합니다.
     * setCharacteristicUUID로 설정된 캐릭터리스틱에 데이터를 쓰기합니다.
     * @param data
     */
    fun writeCharacteristic(data: ByteArray) {
        HCBle.writeCharacteristic(data)
    }

    /**
     * TODO: 캐릭터리스틱 알림을 설정합니다.
     * setCharacteristicUUID로 설정된 캐릭터리스틱에 알림을 설정합니다.
     * @param isEnable
     */
    fun setCharacteristicNotification(isEnable: Boolean) {
        HCBle.setCharacteristicNotification(isEnable)
    }

    fun getBondedDevices(): List<BluetoothDevice> {
        return HCBle.getBondedDevices()
    }
}