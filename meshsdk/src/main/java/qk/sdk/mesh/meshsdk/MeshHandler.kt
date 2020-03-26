package qk.sdk.mesh.meshsdk

import android.os.Handler
import android.os.HandlerThread
import qk.sdk.mesh.meshsdk.bean.CallbackMsg
import qk.sdk.mesh.meshsdk.bean.CommonErrorMsg
import qk.sdk.mesh.meshsdk.bean.MeshMsgSender
import qk.sdk.mesh.meshsdk.callback.MeshCallback
import qk.sdk.mesh.meshsdk.util.Utils
import java.util.concurrent.ConcurrentHashMap

object MeshHandler {
    const val TAG = "MeshHandler"

    private var mHandler: Handler
    private val handlerThread = HandlerThread("BLEMeshModule")

    private val requestMaps = ConcurrentHashMap<String, MeshCallback>(20)
    private val runnableMaps = ConcurrentHashMap<String, TimeOutRunnable>(20)
    private val TIME_OUT_MILLS = 5 * 1000L

    init {
        handlerThread.start()
        mHandler = Handler(handlerThread.looper)
    }

    @Synchronized
    fun removeRunnable(method: String) {
        requestMaps.remove(method)
        runnableMaps[method]?.apply {
            mHandler.removeCallbacks(this)
            meshMsgSender.retry = false
            runnableMaps.remove(meshMsgSender.method)
            Utils.printLog(TAG, "removeRunnable:$method")
        }
    }

    @Synchronized
    fun addRunnable(meshMsgSender: MeshMsgSender) {
        if (meshMsgSender.callback == null || meshMsgSender.method.isEmpty())
            return

        requestMaps[meshMsgSender.method] = meshMsgSender.callback!!

        if (meshMsgSender.timeout) {
            val runnable = TimeOutRunnable(meshMsgSender)
            runnableMaps[meshMsgSender.method] = runnable
            mHandler.postDelayed(runnable, TIME_OUT_MILLS)
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
                removeRunnable(meshMsgSender.method)
            } else {
                if (meshMsgSender.message != null)
                    MeshHelper.sendMessage(
                        "",
                        meshMsgSender.dst ?: 0,
                        meshMsgSender.message!!,
                        null
                    )
                meshMsgSender.retry = false
                Utils.printLog(TAG, "retry:${meshMsgSender.method}")
                mHandler.postDelayed(this, TIME_OUT_MILLS)
            }
        }
    }
}