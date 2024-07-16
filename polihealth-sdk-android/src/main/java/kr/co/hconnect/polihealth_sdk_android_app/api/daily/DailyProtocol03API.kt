package kr.co.hconnect.polihealth_sdk_android_app.api.daily

import android.os.Build
import androidx.annotation.RequiresApi
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.util.AttributeKey
import kotlinx.coroutines.runBlocking
import kr.co.hconnect.polihealth_sdk_android_app.PoliClient
import kr.co.hconnect.polihealth_sdk_android_app.api.dto.request.HRSpO2
import kr.co.hconnect.polihealth_sdk_android_app.api.dto.request.HRSpO2Request
import kr.co.hconnect.polihealth_sdk_android_app.api.dto.response.SleepResponse
import kr.co.hconnect.polihealth_sdk_android_app.api.dto.response.toSleepCommResponse
import kr.co.hconnect.polihealth_sdk_android_app.api.sleep.SleepSessionAPI

object DailyProtocol03API {
    /**
     * TODO: 심박수와 산소포화도를 서버로 전송하는 API
     *
     * @param reqDate ex) 20240704054513 (yyyyMMddHHmmss)
     * @param hrspo2
     * */
    suspend fun requestPost(
        reqDate: String,
        hrSpO2: HRSpO2
    ): SleepResponse.SleepCommResponse {

        val requestBody = HRSpO2Request(
            reqDate = reqDate,
            userSno = SleepSessionAPI.userSno,
            data = HRSpO2Request.Data(
                oxygenVal = hrSpO2.spo2,
                heartRateVal = hrSpO2.heartRate
            )
        )

        val response = PoliClient.client.post("poli/day/protocol3") {
            setBody(requestBody)
        }.call.attributes[AttributeKey("body")].toString().toSleepCommResponse()

        return response
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    fun testPost(
        hrSpO2: HRSpO2
    ) = runBlocking {
        try {
            val requestBody = HRSpO2Request(
                reqDate = "20240704054513",
                userSno = SleepSessionAPI.userSno,
                data = HRSpO2Request.Data(
                    oxygenVal = hrSpO2.spo2,
                    heartRateVal = hrSpO2.heartRate
                )
            )

            PoliClient.client.post("poli/day/protocol3") {
                setBody(requestBody)
            }.call.attributes[AttributeKey("body")].toString()


        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}