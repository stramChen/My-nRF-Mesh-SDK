package qk.sdk.mesh.meshsdk

import android.os.Handler
import android.os.HandlerThread
import qk.sdk.mesh.meshsdk.bean.CallbackMsg
import qk.sdk.mesh.meshsdk.bean.CommonErrorMsg
import qk.sdk.mesh.meshsdk.bean.MeshMsgSender
import qk.sdk.mesh.meshsdk.callback.BaseCallback
import qk.sdk.mesh.meshsdk.callback.MeshCallback
import qk.sdk.mesh.meshsdk.callback.StringCallback
import qk.sdk.mesh.meshsdk.util.Utils
import java.util.concurrent.ConcurrentHashMap

object MeshHandler {
    const val TAG = "MeshHandler"

    private var mHandler: Handler
    private val handlerThread = HandlerThread("BLEMeshModule")

    private val requestMaps = ConcurrentHashMap<String, BaseCallback>(64)
    private val runnableMaps = ConcurrentHashMap<String, TimeOutRunnable>(20)
    private val TIME_OUT_MILLS = 3 * 1000L

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
            runnableMaps.remove(meshMsgSender.key)
            Utils.printLog(TAG, "removeRunnable:$method")
        }
    }

    @Synchronized
    fun addRunnable(meshMsgSender: MeshMsgSender) {
        if (meshMsgSender.callback == null || meshMsgSender.key.isEmpty())
            return

        removeRunnable(meshMsgSender.key)
        requestMaps[meshMsgSender.key] = meshMsgSender.callback!!

        if (meshMsgSender.timeout) {
            val runnable = TimeOutRunnable(meshMsgSender)
            runnableMaps[meshMsgSender.key] = runnable
            mHandler.postDelayed(runnable, TIME_OUT_MILLS)
            Utils.printLog(TAG, "addRunnable method:${meshMsgSender.key}")
        }
    }

    fun getCallback(method: String): Any? {
        return requestMaps[method]
    }

    fun getAllCallback(): MutableCollection<BaseCallback> {
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
                removeRunnable(meshMsgSender.key)
            } else {
                if (meshMsgSender.message != null)
                    MeshHelper.sendMessage(
                        "",
                        meshMsgSender.dst ?: 0,
                        meshMsgSender.message!!,
                        null
                    )
                meshMsgSender.retry = false
                Utils.printLog(TAG, "retry:${meshMsgSender.key}")
                mHandler.postDelayed(this, TIME_OUT_MILLS)
            }
        }
    }
}