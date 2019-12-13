package qk.sdk.mesh.meshsdk

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.joker.api.wrapper.ListenerWrapper
import no.nordicsemi.android.meshprovisioner.ApplicationKey
import no.nordicsemi.android.meshprovisioner.MeshNetwork
import no.nordicsemi.android.meshprovisioner.NetworkKey
import no.nordicsemi.android.meshprovisioner.transport.Element
import no.nordicsemi.android.meshprovisioner.transport.MeshMessage
import no.nordicsemi.android.meshprovisioner.transport.MeshModel
import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode
import qk.sdk.mesh.meshsdk.bean.ExtendedBluetoothDevice
import qk.sdk.mesh.meshsdk.callbak.*
import qk.sdk.mesh.meshsdk.service.BaseMeshService
import qk.sdk.mesh.meshsdk.util.PermissionUtil
import java.util.*
import kotlin.collections.ArrayList

object MeshHelper {

    fun initMesh(context: Context) {
        context.startService(Intent(context, MeshProxyService::class.java))
    }

    fun checkPermission(activity: Activity, listener: ListenerWrapper.PermissionRequestListener) {
        PermissionUtil.checkMeshPermission(activity, listener)
    }

    fun startScan(filterUuid: UUID, scanCallback: ScanCallback?) {
        MeshProxyService.mMeshProxyService?.startScan(filterUuid, scanCallback)
    }

    fun stopScan() {
        MeshProxyService.mMeshProxyService?.stopScan()
    }

    fun connect(
        context: Context,
        device: ExtendedBluetoothDevice,
        connectToNetwork: Boolean,
        callback: ConnectCallback?
    ) {
        MeshProxyService.mMeshProxyService?.connect(context, device, connectToNetwork, callback)
    }

    fun disConnect() {
        MeshProxyService.mMeshProxyService?.disConnect()
    }

    fun clearMeshCallback() {
        MeshProxyService.mMeshProxyService?.clearMeshCallback()
    }

    fun startProvision(device: ExtendedBluetoothDevice, callback: BaseCallback) {
        MeshProxyService.mMeshProxyService?.startProvisioning(device, callback)
    }

    fun getProvisionedNodeByCallback(callback: ProvisionCallback) {
        MeshProxyService.mMeshProxyService?.getProvisionedNodes(callback)
    }

    fun getProvisionNode(): ArrayList<ProvisionedMeshNode>? {
        return MeshProxyService.mMeshProxyService?.mNrfMeshManager?.nodes?.value
    }

    fun deleteProvisionNode(node: ProvisionedMeshNode, callback: ProvisionCallback) {
        MeshProxyService.mMeshProxyService?.deleteNode(node, callback)
    }

    fun setSelectedMeshNode(node: ProvisionedMeshNode) {
        MeshProxyService.mMeshProxyService?.setSelectedNode(node)
    }

    fun getSelectedMeshNode(): ProvisionedMeshNode? {
        return MeshProxyService.mMeshProxyService?.getSelectedNode()
    }

    fun getMeshNetwork(): MeshNetwork? {
        return MeshProxyService.mMeshProxyService?.getMeshNetwork()
    }

    fun getAppKeys(): List<ApplicationKey>? {
        return MeshProxyService.mMeshProxyService?.getMeshNetwork()?.appKeys
    }

    fun getNetworkKey(index: Int): NetworkKey? {
        return MeshProxyService.mMeshProxyService?.getMeshNetwork()?.netKeys?.get(index)
    }

    fun sendMeshPdu(dst: Int, message: MeshMessage, callback: MeshCallback?) {
        MeshProxyService.mMeshProxyService?.sendMeshPdu(dst, message, callback)
    }

    fun setSelectedModel(
        element: Element,
        model: MeshModel
    ) {
        MeshProxyService.mMeshProxyService?.setSelectedModel(element, model)
    }

    fun getSelectedModel(): MeshModel? {
        return MeshProxyService.mMeshProxyService?.getSelectedModel()
    }

    fun getSelectedElement(): Element? {
        return MeshProxyService.mMeshProxyService?.getSelectedElement()
    }

    fun isConnectedToProxy(): Boolean {
        return MeshProxyService.mMeshProxyService?.isConnectedToProxy() ?: false
    }

    internal class MeshProxyService : BaseMeshService() {
        companion object {
            var mMeshProxyService: MeshProxyService? = null
                private set
        }

        override fun onCreate() {
            super.onCreate()
            mMeshProxyService = this

        }
    }
}