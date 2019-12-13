package qk.sdk.mesh.meshsdk.callbak

import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode
import qk.sdk.mesh.meshsdk.bean.ErrorMsg

interface ConnectCallback : BaseCallback {
    fun onConnect()
    fun onConnectStateChange(msg: ErrorMsg)
    fun onSelectMeshNodeChange(node: ProvisionedMeshNode)
}