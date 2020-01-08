package qk.sdk.mesh.meshsdk

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.lifecycle.Observer
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
import qk.sdk.mesh.meshsdk.util.*
import rx.android.schedulers.AndroidSchedulers
import java.util.*
import kotlin.collections.ArrayList

object MeshHelper {
    private val TAG = "MeshHelper"
    private var mProvisionCallback: ProvisionCallback? = null

    // 初始化 mesh
    fun initMesh(context: Context) {
        context.startService(Intent(context, MeshProxyService::class.java))
        LocalPreferences.init(context)
    }

    // 检查蓝牙权限
    // 希望不需要传递 Activity
    fun checkPermission(activity: Activity, listener: ListenerWrapper.PermissionRequestListener) {
        PermissionUtil.checkMeshPermission(activity, listener)
    }

    // 扫描蓝牙节点
    // TODO: UUID 定义为一个通用的字符串，标明扫描到的设备类型（已加入 mesh 网络的，未加入的）
    // TODO: scanCallback -> 2 个方法，onStatusChange, onScanResult
    fun startScan(filterUuid: UUID, scanCallback: ScanCallback?, networkKey: String = "") {
        rx.Observable.create<String> {
        }.subscribeOn(AndroidSchedulers.mainThread()).doOnSubscribe {
            MeshProxyService.mMeshProxyService?.startScan(filterUuid, scanCallback, networkKey)
        }.subscribeOn(AndroidSchedulers.mainThread()).subscribe()
    }

    // 停止蓝牙扫描
    fun stopScan() {
        rx.Observable.create<String> {
        }.subscribeOn(AndroidSchedulers.mainThread()).doOnSubscribe {
            MeshProxyService.mMeshProxyService?.stopScan()
        }.subscribeOn(AndroidSchedulers.mainThread()).subscribe()
    }

    // ⚠️ Q: 此处是否仅建立蓝牙连接？
    // 建立连接
    // TODO: device 修改为传递唯一标识符 string | number
    // TODO: 能多函数多函数，参数最好是基础类型
    fun connect(
        device: ExtendedBluetoothDevice,
        connectToNetwork: Boolean,
        callback: ConnectCallback?
    ) {
        rx.Observable.create<String> {
        }.subscribeOn(AndroidSchedulers.mainThread()).doOnSubscribe {
            MeshProxyService.mMeshProxyService?.connect(device, connectToNetwork, callback)
        }.subscribeOn(AndroidSchedulers.mainThread()).subscribe()
    }

    // 添加蓝牙连接回调
    // 当前连接的 mesh 代理节点状态变化时，回调通知应用层
    // TODO: 合并 callback 为一个方法，定义并给出 callback 参数定义 - 连接、断开、连接中，等等
    // TODO: addConnectStatusChangedCallback
    fun addConnectCallback(callback: ConnectCallback) {
        rx.Observable.create<String> {
        }.subscribeOn(AndroidSchedulers.mainThread()).doOnSubscribe {
            MeshProxyService.mMeshProxyService?.addConnectCallback(callback)
        }.subscribeOn(AndroidSchedulers.mainThread()).subscribe()
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
        rx.Observable.create<String> {
        }.subscribeOn(AndroidSchedulers.mainThread()).doOnSubscribe {
            MeshProxyService.mMeshProxyService?.disConnect()
        }.subscribeOn(AndroidSchedulers.mainThread()).subscribe()
    }

    // 停止蓝牙连接 - 正在连接的时候
    fun stopConnect() {
        rx.Observable.create<String> {
        }.subscribeOn(AndroidSchedulers.mainThread()).doOnSubscribe {
            MeshProxyService.mMeshProxyService?.stopConnect()
        }.subscribeOn(AndroidSchedulers.mainThread()).subscribe()
    }

    // 清除 mesh 回调，清除 mesh 业务逻辑中所有设定的 callback
    fun clearMeshCallback() {
        rx.Observable.create<String> {
        }.subscribeOn(AndroidSchedulers.mainThread()).doOnSubscribe {
            MeshProxyService.mMeshProxyService?.clearMeshCallback()
        }.subscribeOn(AndroidSchedulers.mainThread()).subscribe()
    }

    // 开启启动网络配置
    // TODO: ExtendedBluetoothDevice 转换为 HashMap
    // TODO: callback 同上
    fun startProvision(device: ExtendedBluetoothDevice, callback: BaseCallback) {
        rx.Observable.create<String> {
        }.subscribeOn(AndroidSchedulers.mainThread()).doOnSubscribe {
            MeshProxyService.mMeshProxyService?.startProvisioning(device, callback)
        }.subscribeOn(AndroidSchedulers.mainThread()).subscribe()
    }

    // 开启启动网络配置
    fun startProvision(
        device: ExtendedBluetoothDevice,
        networkKey: NetworkKey,
        callback: BaseCallback
    ) {
        rx.Observable.create<String> {
        }.subscribeOn(AndroidSchedulers.mainThread()).doOnSubscribe {
            MeshProxyService.mMeshProxyService?.startProvisioning(device, networkKey, callback)
        }.subscribeOn(AndroidSchedulers.mainThread()).subscribe()
    }

    // ??? 获取已经配置的网络的节点列表？
    // TODO: callback 同上
    // TODO: getProvisionedDeviceListForCurrentMeshNetwork()
    fun getProvisionedNodeByCallback(callback: ProvisionCallback) {
        rx.Observable.create<String> {
        }.subscribeOn(AndroidSchedulers.mainThread()).doOnSubscribe {
            mProvisionCallback = callback
            MeshProxyService.mMeshProxyService?.getProvisionedNodes(callback)
        }.subscribeOn(AndroidSchedulers.mainThread()).subscribe()
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
    fun deleteProvisionNode(node: ProvisionedMeshNode?, callback: ProvisionCallback? = null) {
        rx.Observable.create<String> {
        }.subscribeOn(AndroidSchedulers.mainThread()).doOnSubscribe {
            node?.let {
                MeshProxyService.mMeshProxyService?.deleteNode(node, callback)
            }
        }.subscribeOn(AndroidSchedulers.mainThread()).subscribe()
    }

    fun getProvisionedNodeByUUID(uuid: String): ProvisionedMeshNode? {
        getProvisionNode()?.forEach {
            if (it.uuid == uuid) {
                return it
            }
        }
        return null
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

    fun getAppkeyByKeyName(key: String): ApplicationKey? {
        MeshProxyService.mMeshProxyService?.mNrfMeshManager?.meshManagerApi?.meshNetwork?.appKeys?.forEach {
            if (key == ByteUtil.bytesToHexString(it.key))
                return it
        }

        return null
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
        rx.Observable.create<String> {
        }.subscribeOn(AndroidSchedulers.mainThread()).doOnSubscribe {
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

        }.subscribeOn(AndroidSchedulers.mainThread()).subscribe()
    }

    //给RN用
    fun addAppkeys(index: Int, meshCallback: MeshCallback?) {
        val applicationKey = getAppKeys()?.get(index)
        if (applicationKey != null) {
            val networkKey = getNetworkKey(applicationKey.boundNetKeyIndex)
            Utils.printLog(
                TAG,
                "networkKey.keyIndex:${networkKey?.keyIndex},applicationKey.boundNetKeyIndex:${applicationKey.boundNetKeyIndex}"
            )
            if (networkKey == null || networkKey.keyIndex != applicationKey.boundNetKeyIndex) {
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
//        getAppKeys()?.get(index)?.let { applicationKey ->
//            getNetworkKey(applicationKey.boundNetKeyIndex)?.let { networkKey ->
//                val node = getSelectedMeshNode()
//                var isNodeKeyAdd: Boolean
//                if (node != null) {
//                    isNodeKeyAdd = MeshParserUtils.isNodeKeyExists(
//                        node.addedAppKeys,
//                        applicationKey.keyIndex
//                    )
//                    val meshMessage: MeshMessage
//                    if (!isNodeKeyAdd) {
//                        meshMessage = ConfigAppKeyAdd(getCurrentNetworkKey()!!, applicationKey)
//                    } else {
//                        meshMessage = ConfigAppKeyDelete(getCurrentNetworkKey()!!, applicationKey)
//                    }
//                    sendMessage(node.unicastAddress, meshMessage, meshCallback)
//                }
//            }
//        }
    }

    /**
     * 在绑定好appkey之后，获取当前节点的元素列表
     */
    fun getCompositionData() {
        rx.Observable.create<String> {
        }.subscribeOn(AndroidSchedulers.mainThread()).doOnSubscribe {
            val configCompositionDataGet = ConfigCompositionDataGet()
            val node = MeshHelper.getSelectedMeshNode()
            node?.let {
                sendMessage(it.unicastAddress, configCompositionDataGet)
            }
        }.subscribeOn(AndroidSchedulers.mainThread()).subscribe()
    }

    // 绑定 application key
    fun bindAppKey(meshCallback: MeshCallback?) {
        getSelectedMeshNode()?.let {
            val element = getSelectedElement()
            if (element != null) {
                Utils.printLog(TAG, "getSelectedElement")
                val model = getSelectedModel()
                if (model != null) {
                    Utils.printLog(TAG, "getSelectedModel")
                    val configModelAppUnbind =
                        ConfigModelAppBind(element.elementAddress, model.modelId, 0)
                    sendMessage(it.unicastAddress, configModelAppUnbind, meshCallback)
                }
            }
        }
    }

    fun bindAppKey(appKeyIndex: Int, meshCallback: MeshCallback?) {
        getSelectedMeshNode()?.let {
            val element = getSelectedElement()
            if (element != null) {
                val model = getSelectedModel()
                if (model != null) {
                    Utils.printLog(TAG, "getSelectedModel")
                    val configModelAppUnbind =
                        ConfigModelAppBind(element.elementAddress, model.modelId, appKeyIndex)
                    sendMessage(it.unicastAddress, configModelAppUnbind, meshCallback)
                }
            }
        }
    }

    fun createNetworkKey(key: String) {
//        rx.Observable.create<String> {
//        }.subscribeOn(AndroidSchedulers.mainThread()).doOnSubscribe {
        var netKey =
            MeshProxyService.mMeshProxyService?.mNrfMeshManager?.meshManagerApi?.meshNetwork?.createNetworkKey()
        netKey?.key = ByteUtil.hexStringToBytes(key)
        netKey?.let {
            MeshProxyService.mMeshProxyService?.mNrfMeshManager?.meshManagerApi?.meshNetwork?.addNetKey(
                netKey
            )
        }
//        }.subscribeOn(AndroidSchedulers.mainThread()).subscribe()
    }

    fun removeNetworkKey(key: String, callback: IntCallback) {
        var netKey: NetworkKey? = null
        getAllNetworkKey()?.forEach {
            if (ByteUtil.bytesToHexString(it.key) == key) {
                netKey = it
            }
        }
        netKey?.let {
            if (!(MeshProxyService.mMeshProxyService?.mNrfMeshManager?.meshManagerApi?.meshNetwork?.removeNetKey(
                    it
                ) ?: false)
            ) {
                callback.onResultMsg(Constants.ConnectState.NET_KEY_DELETE_FAILED.code)
            } else {
                callback.onResultMsg(Constants.ConnectState.COMMON_SUCCESS.code)
                if (LocalPreferences.getCurrentNetKey() == ByteUtil.bytesToHexString(it.key)) {
                    LocalPreferences.setCurrentNetKey("")
                }
            }
        }
    }

    fun getAllNetworkKey(): List<NetworkKey>? {
        return MeshProxyService.mMeshProxyService?.mNrfMeshManager?.meshNetworkLiveData?.networkKeys
    }

    fun setCurrentNetworkKey(networkKey: String) {
        MeshProxyService.mMeshProxyService?.setCurrentNetworkKey(networkKey)
    }

    fun getPrimaryNetKey() {

    }

    fun getCurrentNetworkKey(): NetworkKey? {
        return MeshProxyService.mMeshProxyService?.getCurrentNetworkKey()
    }

    fun getCurrentNetworkKeyStr(): String? {
        return MeshProxyService.mMeshProxyService?.getCurrentNetworkKeyStr()
    }

    fun createApplicationKey(networkKey: String): String {
        getAllNetworkKey()?.forEach { netKey ->
            if (ByteUtil.bytesToHexString(netKey.key) == networkKey) {
                var appKey =
                    MeshProxyService.mMeshProxyService?.mNrfMeshManager?.meshManagerApi?.meshNetwork?.createAppKey()
                if (appKey != null) {
                    Utils.printLog(TAG, "")
                    appKey.boundNetKeyIndex = netKey.keyIndex
                    MeshProxyService.mMeshProxyService?.mNrfMeshManager?.meshManagerApi?.meshNetwork?.let { network ->
                        network.addAppKey(appKey)
                        return ByteUtil.bytesToHexString(appKey.key)
                    }
                }
            }
        }

        return ""
    }

    fun getNetKeyByKeyName(networkKey: String): NetworkKey? {
        getAllNetworkKey()?.forEach { netKey ->
            if (ByteUtil.bytesToHexString(netKey.key) == networkKey) {
                return netKey
            }
        }
        return null
    }

    fun getAllApplicationKey(networkKey: String, callback: ArrayStringCallback) {
        var appKeys = ArrayList<String>()
        var netKey = getNetKeyByKeyName(networkKey)
        getAppKeys()?.forEach {
            if (it.boundNetKeyIndex == netKey?.keyIndex) {
                appKeys.add(ByteUtil.bytesToHexString(it.key))
            }
        }
        callback.onResult(appKeys)
    }

    fun removeApplicationKey(appKey: String, callback: IntCallback) {
        var applicationKey: ApplicationKey? = null
        getAppKeys()?.forEach {
            if (ByteUtil.bytesToHexString(it.key) == appKey) {
                applicationKey = it
            }
        }

        applicationKey?.let {
            if (!(MeshProxyService.mMeshProxyService?.mNrfMeshManager?.meshManagerApi?.meshNetwork?.removeAppKey(
                    it
                ) ?: false)
            ) {
                callback.onResultMsg(Constants.ConnectState.APP_KEY_DELETE_FAILED.code)
            } else {
                callback.onResultMsg(Constants.ConnectState.COMMON_SUCCESS.code)
                if (LocalPreferences.getCurrentNetKey() == ByteUtil.bytesToHexString(it.key)) {
                    LocalPreferences.setCurrentNetKey("")
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
        rx.Observable.create<String> {
        }.subscribeOn(AndroidSchedulers.mainThread()).doOnSubscribe {
            MeshProxyService.mMeshProxyService?.sendMeshPdu(dst, message, callback)
        }.subscribeOn(AndroidSchedulers.mainThread()).subscribe()
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

    // 设定开关状态？
    fun sendGenericOnOff(state: Boolean, delay: Int?, meshCallback: MeshCallback?) {
        if (meshCallback == null)
            Utils.printLog(TAG, "")
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
    fun sendVendorModelMessage(
        opcode: Int,
        parameters: ByteArray?,
        acknowledged: Boolean,
        callback: MeshCallback? = null
    ) {
        val element = getSelectedElement()
        if (element != null) {
            val model = getSelectedModel()
            if (model != null && model is VendorModel) {
                if (model.boundAppKeyIndexes.size > 0) {
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
                            sendMessage(element.elementAddress, message, callback)
                        } else {
                            message = VendorModelMessageUnacked(
                                appKey,
                                model.modelId,
                                model.companyIdentifier,
                                opcode,
                                parameters
                            )
                            sendMessage(element.elementAddress, message, callback)
                        }
                    }
                } else {
                    //todo
                    Utils.printLog(TAG, "model don't boundAppKey")
                }
            }
        }
    }

    fun unRegisterMeshMsg() {
        MeshProxyService.mMeshProxyService?.unRegisterMeshMsg()
    }

    fun unRegisterConnectListener() {
        MeshProxyService.mMeshProxyService?.unRegisterConnectListener()
    }

    fun exportMeshNetwork(callback: NetworkExportUtils.NetworkExportCallbacks) {
        MeshProxyService.mMeshProxyService?.exportMeshNetwork(callback)
    }

    fun importMeshNetwork(path: String) {
        disConnect()
        MeshProxyService.mMeshProxyService?.importMeshNetwork(path)
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