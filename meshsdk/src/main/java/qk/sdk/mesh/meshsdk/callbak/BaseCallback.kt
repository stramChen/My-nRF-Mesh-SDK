package qk.sdk.mesh.meshsdk.callbak

import qk.sdk.mesh.meshsdk.bean.ErrorMsg

 interface BaseCallback {
    fun onError(errorMsg: ErrorMsg)
}