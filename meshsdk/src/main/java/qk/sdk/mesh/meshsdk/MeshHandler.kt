package qk.sdk.mesh.meshsdk

import android.os.Handler
import android.os.HandlerThread
import qk.sdk.mesh.meshsdk.bean.CallbackMsg
import qk.sdk.mesh.meshsdk.bean.CommonErrorMsg
import qk.sdk.mesh.meshsdk.bean.MeshMsgSender
import qk.sdk.mesh.meshsdk.callback.BooleanCallback
import qk.sdk.mesh.meshsdk.callback.MapCallback
import qk.sdk.mesh.meshsdk.callback.MeshCallback
import qk.sdk.mesh.meshsdk.util.LogFileUtil
import qk.sdk.mesh.meshsdk.util.Utils
import java.util.HashMap
import java.util.concurrent.ConcurrentHashMap

object MeshHandler {
    const val TAG = "MeshHandler"

    private var mHandler: Handler
    private val handlerThread = HandlerThread("BLEMeshModule")

    private val requestMaps = ConcurrentHashMap<String, MeshCallback>(20)
    private val runnableMaps = ConcurrentHashMap<String, TimeOutRunnable>(20)

    init {
        handlerThread.start()
        mHandler = Handler(handlerThread.looper)
    }

    fun removeRunnable(method: String) {
        requestMaps.remove(method)
        runnableMaps.remove(method)
    }

    fun addRunnable(meshMsgSender: MeshMsgSender) {
        if (meshMsgSender.callback == null || meshMsgSender.method.isEmpty())
            return

        requestMaps[meshMsgSender.method] = meshMsgSender.callback!!

        if (meshMsgSender.timeOut) {
            val runnable = TimeOutRunnable(meshMsgSender)
            runnableMaps[meshMsgSender.method] = runnable
            mHandler.postDelayed(runnable, 3 * 1000)
        }
    }

    fun getCallback(method: String): Any? {
        return requestMaps[method]
    }

    fun getAllCallback(): MutableCollection<MeshCallback> {
        return requestMaps.values
    }

    private class TimeOutRunnable constructor(
        var meshMsgSender: MeshMsgSender
    ) : Runnable {
        override fun run() {
            if (!meshMsgSender.retry) {
                meshMsgSender.callback?.onError(
                    CallbackMsg(
                        CommonErrorMsg.TIME_OUT.code,
                        CommonErrorMsg.TIME_OUT.msg
                    )
                )
            } else {
                if (meshMsgSender.message != null)
                    MeshHelper.sendMessage(
                        "",
                        meshMsgSender.dst ?: 0,
                        meshMsgSender.message!!,
                        null
                    )
                meshMsgSender.retry = false
            }
        }
    }
}