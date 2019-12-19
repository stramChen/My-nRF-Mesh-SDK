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

    // 初始化 mesh
    fun initMesh(context: Context) {
        context.startService(Intent(context, MeshProxyService::class.java))
    }

    // 检查蓝牙权限
    // 希望不需要传递 Activity
    fun checkPermission(activity: Activity, listener: ListenerWrapper.PermissionRequestListener) {
        PermissionUtil.checkMeshPermission(activity, listener)
    }

    // 扫描蓝牙节点
    // TODO: UUID 定义为一个通用的字符串，标明扫描到的设备类型（已加入 mesh 网络的，未加入的）
    // TODO: scanCallback -> 2 个方法，onStatusChange, onScanResult
    fun startScan(filterUuid: UUID, scanCallback: ScanCallback?) {
        MeshProxyService.mMeshProxyService?.startScan(filterUuid, scanCallback)
    }

    // 停止蓝牙扫描
    fun stopScan() {
        MeshProxyService.mMeshProxyService?.stopScan()
    }

    // ⚠️ Q: 此处是否仅建立蓝牙连接？
    // 建立连接
    // TODO: device 修改为传递唯一标识符 string | number
    // TODO: 能多函数多函数，参数最好是基础类型
    fun connect(
        context: Context,
        device: ExtendedBluetoothDevice,
        connectToNetwork: Boolean,
        callback: ConnectCallback?
    ) {
        MeshProxyService.mMeshProxyService?.connect(context, device, connectToNetwork, callback)
    }

    // 添加蓝牙连接回调
    // 当前连接的 mesh 代理节点状态变化时，回调通知应用层
    // TODO: 合并 callback 为一个方法，定义并给出 callback 参数定义 - 连接、断开、连接中，等等
    // TODO: addConnectStatusChangedCallback
    fun addConnectCallback(callback: ConnectCallback) {
        MeshProxyService.mMeshProxyService?.addConnectCallback(callback)
    }

    // 获取当前已连接的蓝牙设备
    // TODO: getCurrentConnectedDevice()
    // TODO: ExtendedBluetoothDevice 转换为 HashMap
    // TODO: ExtendedBluetoothDevice 包含属性（暂定）
    // TODO: UUID, mac(address), name, rssi
    fun getConnectedDevice(): ExtendedBluetoothDevice? {
        return MeshProxyService.mMeshProxyService?.getConnectingDevice()
    }

    // 断开当前蓝牙连接
    fun disConnect() {
        MeshProxyService.mMeshProxyService?.disConnect()
    }

    // 停止蓝牙连接 - 正在连接的时候
    fun stopConnect() {
        MeshProxyService.mMeshProxyService?.stopConnect()
    }

    // 清除 mesh 回调，清除 mesh 业务逻辑中所有设定的 callback
    fun clearMeshCallback() {
        MeshProxyService.mMeshProxyService?.clearMeshCallback()
    }

    // 开启启动网络配置
    // TODO: ExtendedBluetoothDevice 转换为 HashMap
    // TODO: callback 同上
    fun startProvision(device: ExtendedBluetoothDevice, callback: BaseCallback) {
        MeshProxyService.mMeshProxyService?.startProvisioning(device, callback)
    }

    // ??? 获取已经配置的网络的节点列表？
    // TODO: callback 同上
    // TODO: getProvisionedDeviceListForCurrentMeshNetwork()
    fun getProvisionedNodeByCallback(callback: ProvisionCallback) {
        mProvisionCallback = callback
        MeshProxyService.mMeshProxyService?.getProvisionedNodes(callback)
    }

    // ??? 获取已经配置的网络的节点列表？—— 和上面函数的区别？
    // Warning!!!!!!!!!!!!!!!!!!!!!!!! 最好 React Native 层不要用这个方法
    fun getProvisionNode(): ArrayList<ProvisionedMeshNode>? {
        return MeshProxyService.mMeshProxyService?.mNrfMeshManager?.nodes?.value
    }

    // 移除已经配置的 mesh 网络节点
    // TODO: ProvisionedMeshNode 同上
    // TODO: callback 同上
    // TODO: deleteProvisionNodeFormLocalMeshNetworkDataBase
    fun deleteProvisionNode(node: ProvisionedMeshNode, callback: ProvisionCallback) {
        MeshProxyService.mMeshProxyService?.deleteNode(node, callback)
    }

    // ？？？
    // 设置要操作的节点
    // TODO: node -> string MAC 地址
    fun setSelectedMeshNode(node: ProvisionedMeshNode) {
        MeshProxyService.mMeshProxyService?.setSelectedNode(node)
    }

    // ？？？
    fun getSelectedMeshNode(): ProvisionedMeshNode? {
        return MeshProxyService.mMeshProxyService?.getSelectedNode()
    }

    // ？？？
    fun getMeshNetwork(): MeshNetwork? {
        return MeshProxyService.mMeshProxyService?.getMeshNetwork()
    }

    // 获取 mesh 网络中已有的 application key
    fun getAppKeys(): List<ApplicationKey>? {
        return MeshProxyService.mMeshProxyService?.getMeshNetwork()?.appKeys
    }

    // 选择要操作的元素和 model
    fun setSelectedModel(
        element: Element?,
        model: MeshModel?
    ) {
        MeshProxyService.mMeshProxyService?.setSelectedModel(element, model)
    }

    // 在当前 mesh 网络中创建一个新的 application key，并存储
    // TODO: 传入 APP Key 序号
    fun addAppKeys(meshCallback: MeshCallback?) {
        val applicationKey = getAppKeys()?.get(0)
        if (applicationKey != null) {
            val networkKey = getNetworkKey(applicationKey.boundNetKeyIndex)
            if (networkKey == null) {
                //todo 日志记录
                Utils.printLog(TAG, "addAppKeys() networkKey is null!")
            } else {
                val node = getSelectedMeshNode()
                var isNodeKeyAdd: Boolean
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

    // 绑定 application key
    // TODO: 传入 APP Key 序号
    // TODO: 在这个过程中对目标 modelxN 进行 bindAppKey 操作，直接遍历所有 model 并绑定
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

    // 向设备发送指令
    fun sendMessage(address: Int, message: MeshMessage, callback: MeshCallback? = null) {
        try {
            sendMeshPdu(address, message, callback)
        } catch (ex: IllegalArgumentException) {
            ex.printStackTrace()
            //todo 日志记录
        }
    }

    // 获取当前 mesh 网络的 network key
    fun getNetworkKey(index: Int): NetworkKey? {
        return MeshProxyService.mMeshProxyService?.getMeshNetwork()?.netKeys?.get(index)
    }

    // 传送 mesh 数据包
    // 传递控制参数给 mesh 设备
    // TODO: 修改为原始类型数据
    // TODO: sendMeshPDU
    fun sendMeshPdu(dst: Int, message: MeshMessage, callback: MeshCallback?) {
        MeshProxyService.mMeshProxyService?.sendMeshPdu(dst, message, callback)
    }

    // 获取选中的 model
    // TODO: 修改为原始类型数据
    fun getSelectedModel(): MeshModel? {
        return MeshProxyService.mMeshProxyService?.getSelectedModel()
    }

    // TODO: 修改为原始类型数据
    fun getSelectedElement(): Element? {
        return MeshProxyService.mMeshProxyService?.getSelectedElement()
    }

    // 是否已经成功连接代理节点
    fun isConnectedToProxy(): Boolean {
        return MeshProxyService.mMeshProxyService?.isConnectedToProxy() ?: false
    }

    // 设定通用开关状态 ？？？GET？？？
    // Android 封装好的方法
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

    // 设定开关状态？
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
    // 私有协议 opcode, value
    fun sendVendorModelMessage(opcode: Int, parameters: ByteArray?, acknowledged: Boolean) {
        val element = getSelectedElement()
        if (element != null) {
            val model = getSelectedModel()
            if (model != null && model is VendorModel) {
                val appKeyIndex = model.boundAppKeyIndexes[0]
                val appKey = getMeshNetwork()?.getAppKey(appKeyIndex)
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