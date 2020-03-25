package qk.sdk.mesh.meshsdk.bean

import no.nordicsemi.android.meshprovisioner.transport.MeshMessage
import qk.sdk.mesh.meshsdk.callback.MeshCallback

data class MeshMsgSender(
    var method: String,
    var dst: Int?,
    var message: MeshMessage?,
    var callback: MeshCallback?,
    var timeOut: Boolean = false,
    var retry: Boolean = false
)