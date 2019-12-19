package qk.sdk.mesh.meshsdk.callbak

import qk.sdk.mesh.meshsdk.bean.CallbackMsg

 interface BaseCallback {
    fun onError(msg: CallbackMsg)
}