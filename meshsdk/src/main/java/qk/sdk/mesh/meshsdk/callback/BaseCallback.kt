package qk.sdk.mesh.meshsdk.callback

import qk.sdk.mesh.meshsdk.bean.CallbackMsg

 interface BaseCallback {
    fun onError(msg: CallbackMsg)
}