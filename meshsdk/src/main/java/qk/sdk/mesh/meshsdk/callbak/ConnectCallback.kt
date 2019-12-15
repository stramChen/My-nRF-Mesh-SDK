package qk.sdk.mesh.meshsdk.callbak

import qk.sdk.mesh.meshsdk.bean.CallbackMsg

interface ConnectCallback : BaseCallback {
    fun onConnect()
    fun onConnectStateChange(msg: CallbackMsg)
//    fun onSelectMeshNodeChange(node: ProvisionedMeshNode)
}