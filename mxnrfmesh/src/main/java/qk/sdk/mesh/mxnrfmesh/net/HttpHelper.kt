package qk.sdk.mesh.mxnrfmesh.net

import android.content.Context
import androidx.collection.ArrayMap
import mxchip.sdk.http.RetrofitManager

object HttpHelper {

    private val KEY_HEADER_AUTHORIZATION = "Authorization"
    private val KEY_HEADER_DEVICE_TYPE = "device-type"

    /**
     * 公共参数
     */
    private val params = ArrayMap<String, String>()

    /**
     * 公共请求头参数
     */
    private val headers = ArrayMap<String, String>()

    init {
        headers[KEY_HEADER_AUTHORIZATION] = ""
        headers[KEY_HEADER_DEVICE_TYPE] = "2"
    }

    fun init(context: Context, baseUrl: String) {
//        RetrofitManager.initManager(context.applicationContext, baseUrl, params, headers, CheckTokenValidityInterceptor())
    }

//    fun refreshUserToken(): Boolean {
//        val token = StydPreference.token
//        RetrofitManager.setPublicHeader(KEY_HEADER_AUTHORIZATION, StydPreference.token)
//        //RetrofitManager.setPublicHeader(KEY_HEADER_AUTHORIZATION, "W2d34PyZGtTstHUE8fipuEqyBL1Ve6pd")
//        return !token.isEmpty()
//    }

    fun getApi(): Api {
        return RetrofitManager.create(Api::class.java)!!
    }

}