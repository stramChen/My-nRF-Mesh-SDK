package qk.sdk.mesh.meshsdk.callback

import qk.sdk.mesh.meshsdk.MeshSDK
import qk.sdk.mesh.meshsdk.bean.CallbackMsg
import qk.sdk.mesh.meshsdk.util.Utils

abstract class StringCallback : BaseCallback {
    abstract fun onResultMsg(msg: String)

    override fun onError(msg: CallbackMsg) {
        Utils.printLog("StringCallback--error", "code:${msg.code}   msg${msg.msg}")
    }
}