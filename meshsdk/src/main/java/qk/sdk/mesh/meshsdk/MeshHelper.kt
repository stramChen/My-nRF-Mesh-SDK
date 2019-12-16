package qk.sdk.mesh.meshsdk

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.joker.api.wrapper.ListenerWrapper
import no.nordicsemi.android.meshprovisioner.ApplicationKey
import no.nordicsemi.android.meshprovisioner.MeshNetwork
import no.nordicsemi.android.meshprovisioner.NetworkKey
import no.nordicsemi.android.meshprovisioner.models.VendorModel
import no.nordicsemi.android.meshprovisioner.transport.*
import no.nordicsemi.android.meshprovisioner.utils.MeshAddress
import no.nordicsemi.android.meshprovisioner.utils.MeshParserUtils
import qk.sdk.mesh.meshsdk.bean.ExtendedBluetoothDevice
import qk.sdk.mesh.meshsdk.callbak.*
import qk.sdk.mesh.meshsdk.service.BaseMeshService
import qk.sdk.mesh.meshsdk.util.PermissionUtil
import qk.sdk.mesh.meshsdk.util.Utils
import java.util.*
import kotlin.collections.ArrayList

object MeshHelper {
    private val TAG = "MeshHelper"
    private var mProvisionCallback: ProvisionCallback? = null

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

    fun addConnectCallback(callback: ConnectCallback) {
        MeshProxyService.mMeshProxyService?.addConnectCallback(callback)
    }

    fun getConnectedDevice(): ExtendedBluetoothDevice? {
        return MeshProxyService.mMeshProxyService?.getConnectingDevice()
    }

    fun disConnect() {
        MeshProxyService.mMeshProxyService?.disConnect()
    }

    fun stopConnect() {
        MeshProxyService.mMeshProxyService?.stopConnect()
    }

    fun clearMeshCallback() {
        MeshProxyService.mMeshProxyService?.clearMeshCallback()
    }

    fun startProvision(device: ExtendedBluetoothDevice, callback: BaseCallback) {
        MeshProxyService.mMeshProxyService?.startProvisioning(device, callback)
    }

    fun getProvisionedNodeByCallback(callback: ProvisionCallback) {
        mProvisionCallback = callback
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

    fun addAppKeys(meshCallback: MeshCallback?) {
        val applicationKey = getAppKeys()?.get(0)
        if (applicationKey != null) {
            val networkKey = getNetworkKey(applicationKey.boundNetKeyIndex)
            if (networkKey == null) {
                //todo 日志记录
                Utils.printLog(TAG, "addAppKeys() networkKey is null!")
            } else {
                val node = MeshHelper.getSelectedMeshNode()
                var isNodeKeyAdd = false
                if (node != null) {
                    isNodeKeyAdd = MeshParserUtils.isNodeKeyExists(
                        node.addedAppKeys,
                        applicationKey.keyIndex
                    )
                    val meshMessage: MeshMessage
                    if (!isNodeKeyAdd) {
                        meshMessage = ConfigAppKeyAdd(networkKey, applicationKey)
                    } else {
                        meshMessage = ConfigAppKeyDelete(networkKey, applicationKey)
                    }
                    sendMessage(node.unicastAddress, meshMessage, meshCallback)
                }
            }
        } else {
            //todo 日志记录
            Utils.printLog(TAG, "addAppKeys() applicationKey is null!")
        }
    }

    fun bindAppKey(meshCallback: MeshCallback?) {
        getSelectedMeshNode()?.let {
            val element = MeshHelper.getSelectedElement()
            if (element != null) {
                Utils.printLog(TAG, "getSelectedElement")
                val model = MeshHelper.getSelectedModel()
                if (model != null) {
                    Utils.printLog(TAG, "getSelectedModel")
                    val configModelAppUnbind =
                        ConfigModelAppBind(element.elementAddress, model.modelId, 0)
                    sendMessage(it.unicastAddress, configModelAppUnbind, meshCallback)
                }
            }
        }
    }

    fun sendMessage(address: Int, message: MeshMessage, callback: MeshCallback? = null) {
        try {
            sendMeshPdu(address, message, callback)
        } catch (ex: IllegalArgumentException) {
            ex.printStackTrace()
            //todo 日志记录
        }
    }

    fun getNetworkKey(index: Int): NetworkKey? {
        return MeshProxyService.mMeshProxyService?.getMeshNetwork()?.netKeys?.get(index)
    }

    fun sendMeshPdu(dst: Int, message: MeshMessage, callback: MeshCallback?) {
        MeshProxyService.mMeshProxyService?.sendMeshPdu(dst, message, callback)
    }

    fun setSelectedModel(
        element: Element?,
        model: MeshModel?
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

    fun sendGenericOnOffGet(meshCallback: MeshCallback?) {
        val element = MeshHelper.getSelectedElement()
        if (element != null) {
            val model = MeshHelper.getSelectedModel()
            if (model != null) {
                if (model.boundAppKeyIndexes.isNotEmpty()) {
                    val appKeyIndex = model.boundAppKeyIndexes[0]
                    val appKey =
                        MeshHelper.getMeshNetwork()?.getAppKey(appKeyIndex)

                    appKey?.let {
                        val address = element.elementAddress
                        Utils.printLog(
                            TAG,
                            "Sending message to element's unicast address: " + MeshAddress.formatAddress(
                                address,
                                true
                            )
                        )

                        val genericOnOffSet = GenericOnOffGet(appKey)
                        sendMessage(address, genericOnOffSet, meshCallback)
                    }
                } else {
                    //todo 日志记录
                    Utils.printLog(TAG, "sendGenericOnOffGet failed!")
                }
            }
        }
    }

    fun sendGenericOnOff(state: Boolean, delay: Int?) {
        getSelectedMeshNode()?.let { node ->
            getSelectedElement()?.let { element ->
                getSelectedModel()?.let { model ->
                    if (model.boundAppKeyIndexes.isNotEmpty()) {
                        val appKeyIndex = model.boundAppKeyIndexes[0]
                        val appKey =
                            getMeshNetwork()?.getAppKey(appKeyIndex)
                        val address = element.elementAddress
                        if (appKey != null) {
                            val genericOnOffSet = GenericOnOffSet(
                                appKey,
                                state,
                                node.sequenceNumber,
                                0,
                                0,
                                delay
                            )
                            sendMessage(address, genericOnOffSet)
                        }
                    } else {
                        Utils.printLog(TAG, "boundAppKeyIndexes is null!")
                    }
                }
            }
        }
    }

    /**
     * Send vendor model acknowledged message
     *
     * @param opcode     opcode of the message
     * @param parameters parameters of the message
     */
    fun sendVendorModelMessage(opcode: Int, parameters: ByteArray?, acknowledged: Boolean) {
        val element = MeshHelper.getSelectedElement()
        if (element != null) {
            val model = MeshHelper.getSelectedModel() as VendorModel
            if (model != null) {
                val appKeyIndex = model.boundAppKeyIndexes[0]
                val appKey = MeshHelper.getMeshNetwork()?.getAppKey(appKeyIndex)
                val message: MeshMessage
                if (appKey != null) {
                    if (acknowledged) {
                        message = VendorModelMessageAcked(
                            appKey,
                            model.modelId,
                            model.companyIdentifier,
                            opcode,
                            parameters!!
                        )
                        sendMessage(element.elementAddress, message)
                    } else {
                        message = VendorModelMessageUnacked(
                            appKey,
                            model.modelId,
                            model.companyIdentifier,
                            opcode,
                            parameters
                        )
                        sendMessage(element.elementAddress, message)
                    }
                }
            }
        }
    }

    internal class MeshProxyService : BaseMeshService() {
        companion object {
            var mMeshProxyService: MeshProxyService? = null
                private set
        }

        override fun onCreate() {
            super.onCreate()
            mMeshProxyService = this
            if (mProvisionCallback != null)
                getProvisionedNodes(mProvisionCallback!!)
        }
    }
}