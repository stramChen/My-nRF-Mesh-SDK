package qk.sdk.mesh.meshsdk.callbak

import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode

interface ProvisionCallback : BaseCallback {
    fun onProvisionedNodes(nodes: ArrayList<ProvisionedMeshNode>)
    fun onNodeDeleted(isDeleted: Boolean, node: ProvisionedMeshNode)
}