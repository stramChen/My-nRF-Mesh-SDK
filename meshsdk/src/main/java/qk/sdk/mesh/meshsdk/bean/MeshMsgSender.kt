package qk.sdk.mesh.meshsdk.bean

import no.nordicsemi.android.meshprovisioner.transport.MeshMessage
import qk.sdk.mesh.meshsdk.callback.BaseCallback
import qk.sdk.mesh.meshsdk.callback.MeshCallback
import qk.sdk.mesh.meshsdk.callback.StringCallback
import qk.sdk.mesh.meshsdk.util.ByteUtil

data class MeshMsgSender(
    var key: String,
    var dst: Int?,
    var message: MeshMessage?,
    var callback: BaseCallback?,
    var timeout: Boolean = false,
    var retry: Boolean = false
) {
    override fun toString(): String {
        return "key:$key,dst:$dst,message:${ByteUtil.bytesToHexString(message?.parameter)},timeout:$timeout,retry:$retry"
    }
}

//data class LocalAutoRule(address:Short,)