package qk.sdk.mesh.meshsdk.callback

import qk.sdk.mesh.meshsdk.bean.CallbackMsg
import qk.sdk.mesh.meshsdk.util.Utils

abstract class MapCallback :BaseCallback{
    abstract fun onResult(result: HashMap<String, Any>)

    override fun onError(msg: CallbackMsg) {
        Utils.printLog("StringCallback--error", "code:${msg.code}   msg${msg.msg}")
    }
}