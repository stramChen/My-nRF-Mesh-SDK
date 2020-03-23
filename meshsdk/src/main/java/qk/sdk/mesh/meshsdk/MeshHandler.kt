package qk.sdk.mesh.meshsdk

import qk.sdk.mesh.meshsdk.bean.CommonErrorMsg
import qk.sdk.mesh.meshsdk.callback.MapCallback
import qk.sdk.mesh.meshsdk.util.Utils
import java.util.HashMap
import java.util.concurrent.ConcurrentHashMap

object MeshHandler {
    const val TAG = "MeshHandler"


    private val requestMaps = ConcurrentHashMap<String, TimeOutRunnable>()

    private fun addRunnable(method: String, callback: Any): TimeOutRunnable {
        val runnable = TimeOutRunnable(method, callback)
        requestMaps.put(method, runnable)
        return runnable
    }

    private class TimeOutRunnable internal constructor(
        private val method: String,
        private var callback: Any?
    ) : Runnable {

        override fun run() {
            if (callback is MapCallback) {
                val map = HashMap<String, Any>()
                map["code"] = CommonErrorMsg.DISCONNECTED.code
                map["message"] = CommonErrorMsg.DISCONNECTED.msg
                Utils.printLog(TAG, "time out callback")
                (callback as MapCallback).onResult(map)
                callback = null
                requestMaps.remove(method)
            }
        }
    }
}