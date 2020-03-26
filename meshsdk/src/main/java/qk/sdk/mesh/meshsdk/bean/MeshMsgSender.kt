package qk.sdk.mesh.meshsdk.bean

import no.nordicsemi.android.meshprovisioner.transport.MeshMessage
import qk.sdk.mesh.meshsdk.callback.MeshCallback
import qk.sdk.mesh.meshsdk.util.ByteUtil

data class MeshMsgSender(
    var method: String,
    var dst: Int?,
    var message: MeshMessage?,
    var callback: MeshCallback?,
    var timeout: Boolean = false,
    var retry: Boolean = false
) {
    override fun toString(): String {
        return "method:$method,dst:$dst,message:${ByteUtil.bytesToHexString(message?.parameter)},timeout:$timeout,retry:$retry"
    }
}