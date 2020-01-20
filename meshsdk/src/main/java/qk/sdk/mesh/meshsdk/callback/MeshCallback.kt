package qk.sdk.mesh.meshsdk.callback

import no.nordicsemi.android.meshprovisioner.transport.MeshMessage

interface MeshCallback : BaseCallback {
    fun onReceive(msg: MeshMessage)
}