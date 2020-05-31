package qk.sdk.mesh.meshsdk

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.lifecycle.LiveData
import com.joker.api.wrapper.ListenerWrapper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.nordicsemi.android.meshprovisioner.ApplicationKey
import no.nordicsemi.android.meshprovisioner.Group
import no.nordicsemi.android.meshprovisioner.MeshNetwork
import no.nordicsemi.android.meshprovisioner.NetworkKey
import no.nordicsemi.android.meshprovisioner.models.VendorModel
import no.nordicsemi.android.meshprovisioner.transport.*
import no.nordicsemi.android.meshprovisioner.utils.AddressArray
import no.nordicsemi.android.meshprovisioner.utils.MeshAddress
import no.nordicsemi.android.meshprovisioner.utils.MeshParserUtils
import no.nordicsemi.android.meshprovisioner.utils.ProxyFilterType
import qk.sdk.mesh.meshsdk.bean.ExtendedBluetoothDevice
import qk.sdk.mesh.meshsdk.callback.*
import qk.sdk.mesh.meshsdk.service.BaseMeshService
import qk.sdk.mesh.meshsdk.util.*
import rx.android.schedulers.AndroidSchedulers
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

object MeshHelper {
    private val TAG = "MeshHelper"
    private var mProvisionCallback: ProvisionCallback? = null


    fun getVersion(): String {
        return "0.8.5"
    }

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
    fun startScan(filterUuid: UUID, scanCallback: ScanCallback?, networkKey: String = "") {
//        rx.Observable.create<String> {
//        }.subscribeOn(AndroidSchedulers.mainThread()).doOnSubscribe {
        MeshProxyService.mMeshProxyService?.startScan(
            filterUuid,
            scanCallback,
            networkKey.toUpperCase()
        )
//        }.subscribeOn(AndroidSchedulers.mainThread()).sendSubscribeMsg()
    }

    // 停止蓝牙扫描
    fun stopScan() {
        rx.Observable.create<String> {
        }.subscribeOn(AndroidSchedulers.mainThread()).doOnSubscribe {
            MeshProxyService.mMeshProxyService?.stopScan()
        }.subscribeOn(AndroidSchedulers.mainThread()).subscribe()
    }

    // 建立连接
    fun connect(
        device: ExtendedBluetoothDevice,
        connectToNetwork: Boolean,
        callback: ConnectCallback?
    ) {
//        rx.Observable.create<String> {
//        }.subscribeOn(AndroidSchedulers.mainThread()).doOnSubscribe {
        unRegisterConnectListener()
        MeshProxyService.mMeshProxyService?.connect(device, connectToNetwork, callback)
//        }.subscribeOn(AndroidSchedulers.mainThread()).sendSubscribeMsg()
    }

    // 添加蓝牙连接回调
    // 当前连接的 mesh 代理节点状态变化时，回调通知应用层
    fun addConnectCallback(callback: ConnectCallback) {
        rx.Observable.create<String> {
        }.subscribeOn(AndroidSchedulers.mainThread()).doOnSubscribe {
            MeshProxyService.mMeshProxyService?.addConnectCallback(callback)
        }.subscribeOn(AndroidSchedulers.mainThread()).subscribe()
    }

    // 获取当前已连接的蓝牙设备
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
//    fun clearMeshCallback() {
//        rx.Observable.create<String> {
//        }.subscribeOn(AndroidSchedulers.mainThread()).doOnSubscribe {
//            MeshProxyService.mMeshProxyService?.clearMeshCallback()
//        }.subscribeOn(AndroidSchedulers.mainThread()).sendSubscribeMsg()
//    }

    // 开启启动网络配置
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
//        rx.Observable.create<String> {
//        }.subscribeOn(AndroidSchedulers.mainThread()).doOnSubscribe {
        MeshProxyService.mMeshProxyService?.startProvisioning(device, networkKey, callback)
//        }.subscribeOn(AndroidSchedulers.mainThread()).sendSubscribeMsg()
    }

    // ??? 获取已经配置的网络的节点列表？
    fun getProvisionedNodeByCallback(callback: ProvisionCallback) {
//        rx.Observable.create<String> {
//        }.subscribeOn(AndroidSchedulers.mainThread()).doOnSubscribe {
        mProvisionCallback = callback
        MeshProxyService.mMeshProxyService?.getProvisionedNodes(callback)
//        }.subscribeOn(AndroidSchedulers.mainThread()).sendSubscribeMsg()
    }

    // ??? 获取已经配置的网络的节点列表？—— 和上面函数的区别？
    fun getProvisionNode(): ArrayList<ProvisionedMeshNode>? {
        return MeshProxyService.mMeshProxyService?.mNrfMeshManager?.nodes?.value
    }

    // 移除已经配置的 mesh 网络节点
    fun deleteProvisionNode(node: ProvisionedMeshNode?, callback: ProvisionCallback? = null) {
        rx.Observable.create<String> {
        }.subscribeOn(AndroidSchedulers.mainThread()).doOnSubscribe {
            node?.let {
                MeshProxyService.mMeshProxyService?.deleteNode(node, callback)
            }
        }.subscribeOn(AndroidSchedulers.mainThread()).subscribe()
    }

    fun getProvisionedNodeByUUID(uuid: String): ProvisionedMeshNode? {
        for (node: ProvisionedMeshNode in getProvisionNode() ?: ArrayList()) {
            if (node.uuid.toUpperCase() == uuid.toUpperCase()) {
                return node
            }
        }
        return null
    }

    // ？？？
    // 设置要操作的节点
    fun setSelectedMeshNode(node: ProvisionedMeshNode?) {
        if (node == null)
            return

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
                        sendMessage(
                            "addAppKeys",
                            node.unicastAddress,
                            meshMessage,
                            meshCallback,
                            true,
                            true
                        )
                    }
                }
            } else {
                //todo 日志记录
                Utils.printLog(TAG, "addAppKeys() applicationKey is null!")
            }

        }.subscribeOn(AndroidSchedulers.mainThread()).subscribe()
    }

    //给RN用
    fun addAppkeys(
        method: String,
        index: Int,
        meshCallback: MeshCallback?,
        timeOut: Boolean,
        retry: Boolean
    ) {
        var applicationKey: ApplicationKey? = null
        getAppKeys()?.forEach {
            if (it.keyIndex == index) {
                applicationKey = it
            }
        }
//        val applicationKey = getAppKeys()?.get(index)

        if (applicationKey == null) {
            //todo 日志记录
            Utils.printLog(TAG, "addAppKeys() applicationKey is null!")
        }

        applicationKey?.apply {
            val networkKey = getNetworkKey(this.boundNetKeyIndex)
            Utils.printLog(
                TAG,
                "networkKey.keyIndex:${networkKey?.keyIndex},applicationKey.boundNetKeyIndex:${this.boundNetKeyIndex}"
            )
            if (networkKey == null || networkKey.keyIndex != this.boundNetKeyIndex) {
                //todo 日志记录
                Utils.printLog(TAG, "addAppKeys() networkKey is null!")
            } else {
                val node = getSelectedMeshNode()
                var isNodeKeyAdd: Boolean
                if (node != null) {
                    isNodeKeyAdd = MeshParserUtils.isNodeKeyExists(
                        node.addedAppKeys,
                        this.keyIndex
                    )
                    val meshMessage: MeshMessage
                    if (!isNodeKeyAdd) {
                        meshMessage = ConfigAppKeyAdd(networkKey, this)
                    } else {
                        meshMessage = ConfigAppKeyDelete(networkKey, this)
                    }
                    sendMessage(
                        method,
                        node.unicastAddress,
                        meshMessage,
                        meshCallback,
                        timeOut,
                        retry
                    )
                }
            }
        }

    }

    /**
     * 在绑定好appkey之后，获取当前节点的元素列表
     */
    fun getCompositionData(method: String, callback: MeshCallback) {
        rx.Observable.create<String> {
        }.subscribeOn(AndroidSchedulers.mainThread()).doOnSubscribe {
            val configCompositionDataGet = ConfigCompositionDataGet()
            val node = getSelectedMeshNode()
            node?.let {
                sendMessage(
                    method,
                    it.unicastAddress,
                    configCompositionDataGet,
                    callback,
                    true,
                    true
                )
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
                    sendMessage(
                        "bindAppKey",
                        it.unicastAddress,
                        configModelAppUnbind,
                        meshCallback,
                        true,
                        true
                    )
                }
            }
        }
    }

    fun bindAppKey(method: String, appKeyIndex: Int, meshCallback: MeshCallback?) {
        getSelectedMeshNode()?.let {
            val element = getSelectedElement()
            if (element != null) {
                val model = getSelectedModel()
                if (model != null) {
                    Utils.printLog(
                        TAG,
                        "bindAppKey getSelectedEle:${element.elementAddress},getSelectedModel:${model.modelId}"
                    )
                    val configModelAppUnbind =
                        ConfigModelAppBind(element.elementAddress, model.modelId, appKeyIndex)
                    sendMessage(
                        method,
                        it.unicastAddress,
                        configModelAppUnbind,
                        meshCallback,
                        true,
                        true
                    )
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
//        }.subscribeOn(AndroidSchedulers.mainThread()).sendSubscribeMsg()
    }

    fun removeNetworkKey(key: String, callback: MapCallback) {
        var netKey: NetworkKey? = null
        var map = HashMap<String, Any>()
        getAllNetworkKey()?.forEach {
            if (ByteUtil.bytesToHexString(it.key) == key) {
                netKey = it
            }
        }

        if (netKey == null) {
            map.put("code", Constants.ConnectState.NET_KEY_NOT_EXIST.code)
            map.put("message", Constants.ConnectState.NET_KEY_NOT_EXIST.msg)
            callback.onResult(map)
            return
        }
        netKey?.let {
            if (!(MeshProxyService.mMeshProxyService?.mNrfMeshManager?.meshManagerApi?.meshNetwork?.removeNetKey(
                    it
                ) ?: false)
            ) {
                map.put("code", Constants.ConnectState.NET_KEY_DELETE_FAILED.code)
                map.put("message", Constants.ConnectState.NET_KEY_DELETE_FAILED.msg)
                callback.onResult(map)
            } else {
                map.put("code", Constants.ConnectState.COMMON_SUCCESS.code)
                map.put("message", Constants.ConnectState.COMMON_SUCCESS.msg)
                callback.onResult(map)
                if (LocalPreferences.getCurrentNetKey() == ByteUtil.bytesToHexString(it.key)) {
                    LocalPreferences.setCurrentNetKey("")
                }
                //删除对应的appkey
                removeApplicationKey(key)
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
            if (ByteUtil.bytesToHexString(netKey.key) == networkKey.toUpperCase()) {
                var appKey =
                    MeshProxyService.mMeshProxyService?.mNrfMeshManager?.meshManagerApi?.meshNetwork?.createAppKey()
                if (appKey != null) {
                    appKey.boundNetKeyIndex = netKey.keyIndex
                    Utils.printLog(
                        TAG,
                        "appKey.boundNetKeyIndex:${appKey.boundNetKeyIndex},appkey:${ByteUtil.bytesToHexString(
                            appKey.key
                        )}"
                    )
                    MeshProxyService.mMeshProxyService?.mNrfMeshManager?.meshManagerApi?.meshNetwork?.let { network ->
                        network.addAppKey(appKey)
                        Utils.printLog(TAG, "appKey.boundNetKeyIndex:${appKey.boundNetKeyIndex}")
                        return ByteUtil.bytesToHexString(appKey.key)
                    }
                } else {
                    Utils.printLog(TAG, "create appkey is null")
                }
            }
        }

        return ""
    }

    fun getNetKeyByKeyName(networkKey: String): NetworkKey? {
        getAllNetworkKey()?.forEach { netKey ->
            if (ByteUtil.bytesToHexString(netKey.key) == networkKey.toUpperCase()) {
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

    fun removeApplicationKey(appKey: String, callback: IntCallback? = null) {
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
                callback?.onResultMsg(Constants.ConnectState.APP_KEY_DELETE_FAILED.code)
            } else {
                if (LocalPreferences.getCurrentNetKey() == ByteUtil.bytesToHexString(it.key)) {
                    LocalPreferences.setCurrentNetKey("")
                }
                callback?.onResultMsg(Constants.ConnectState.COMMON_SUCCESS.code)
            }
        }
    }

    // 向设备发送指令
    fun sendMessage(
        method: String,
        dst: Int,
        message: MeshMessage,
        callback: MeshCallback?,
        timeOut: Boolean = false,
        retry: Boolean = false
    ) {
        try {
            sendMeshPdu(method, dst, message, callback, timeOut, retry)
        } catch (ex: IllegalArgumentException) {
            ex.printStackTrace()
            //todo 日志记录
        }
    }

    // 获取当前 mesh 网络的 network key
    fun getNetworkKey(index: Int): NetworkKey? {
        MeshProxyService.mMeshProxyService?.getMeshNetwork()?.netKeys?.forEach {
            if (it.keyIndex == index) {
                return it
            }
        }
        return null
    }

    // 传送 mesh 数据包
    // 传递控制参数给 mesh 设备
    fun sendMeshPdu(
        method: String,
        dst: Int,
        message: MeshMessage,
        callback: MeshCallback?,
        timeOut: Boolean = false,
        retry: Boolean = false
    ) {
//        rx.Observable.create<String> {
//        }.subscribeOn(AndroidSchedulers.mainThread()).doOnSubscribe {
        Utils.printLog(TAG, "sendMeshPdu")
        MeshProxyService.mMeshProxyService?.sendMeshPdu(
            method,
            dst,
            message,
            callback,
            timeOut,
            retry
        )
//        }.subscribeOn(AndroidSchedulers.mainThread()).subscribe()
    }

    // 获取选中的 model
    fun getSelectedModel(): MeshModel? {
        return MeshProxyService.mMeshProxyService?.getSelectedModel()
    }

    fun getSelectedElement(): Element? {
        return MeshProxyService.mMeshProxyService?.getSelectedElement()
    }

    // 是否已经成功连接代理节点
    fun isConnectedToProxy(): Boolean {
        return MeshProxyService.mMeshProxyService?.isConnectedToProxy() ?: false
    }

    fun sendGenericOnOffGet(meshCallback: MeshCallback?) {
        val element = getSelectedElement()
        if (element != null) {
            val model = getSelectedModel()
            if (model != null) {
                if (model.boundAppKeyIndexes.isNotEmpty()) {
                    val appKeyIndex = model.boundAppKeyIndexes[0]
                    val appKey =
                        getMeshNetwork()?.getAppKey(appKeyIndex)

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
                        sendMessage("sendGenericOnOffGet", address, genericOnOffSet, meshCallback)
                    }
                } else {
                    //todo 日志记录
                    Utils.printLog(TAG, "sendGenericOnOffGet failed!")
                }
            }
        }
    }

    fun sendGenericOnOff(state: Boolean, delay: Int) {
        getSelectedMeshNode()?.let { node ->
            node.ttl = 16
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
                            sendMessage("sendGenericOnOff", address, genericOnOffSet, null)
                        }
                    } else {
                        Utils.printLog(TAG, "boundAppKeyIndexes is null!")
                    }
                }
            }
        }
    }

    fun sendUnacknowledgedGenericOnOff(state: Boolean, delay: Int) {
        getSelectedMeshNode()?.let { node ->
            getSelectedElement()?.let { element ->
                getSelectedModel()?.let { model ->
                    if (model.boundAppKeyIndexes.isNotEmpty()) {
                        val appKeyIndex = model.boundAppKeyIndexes[0]
                        val appKey =
                            getMeshNetwork()?.getAppKey(appKeyIndex)
                        val address = element.elementAddress
                        if (appKey != null) {
                            val genericOnOffSet = GenericOnOffSetUnacknowledged(
                                appKey,
                                state,
                                node.sequenceNumber,
                                0,
                                0,
                                delay
                            )
                            sendMessage("sendGenericOnOff", address, genericOnOffSet, null)
                        }
                    } else {
                        Utils.printLog(TAG, "boundAppKeyIndexes is null!")
                    }
                }
            }
        }
    }

//    fun sendGenericOnOff(state: Boolean, delay: Int?, meshCallback: MeshCallback?) {
//        if (meshCallback == null)
//            Utils.printLog(TAG, "")
//        getSelectedMeshNode()?.let { node ->
//            getSelectedElement()?.let { element ->
//                getSelectedModel()?.let { model ->
//                    if (model.boundAppKeyIndexes.isNotEmpty()) {
//                        val appKeyIndex = model.boundAppKeyIndexes[0]
//                        val appKey =
//                            getMeshNetwork()?.getAppKey(appKeyIndex)
//                        val address = element.elementAddress
//                        if (appKey != null) {
//                            val genericOnOffSet = GenericOnOffSet(
//                                appKey,
//                                state,
//                                node.sequenceNumber,
//                                0,
//                                0,
//                                delay
//                            )
//                            sendMessage(address, genericOnOffSet)
//                        }
//                    } else {
//                        Utils.printLog(TAG, "boundAppKeyIndexes is null!")
//                    }
//                }
//            }
//        }
//    }

    /**
     * Send vendor model acknowledged message
     *
     * @param opcode     opcode of the message
     * @param parameters parameters of the message
     */
    // 私有协议 opcode, value
    fun sendVendorModelMessage(
        method: String,
        opcode: Int,
        parameters: ByteArray?,
        acknowledged: Boolean,
        callback: MeshCallback? = null,
        timeout: Boolean,
        retry: Boolean
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
                            sendMessage(
                                method,
                                element.elementAddress,
                                message,
                                callback,
                                timeout,
                                retry
                            )
                        } else {
                            message = VendorModelMessageUnacked(
                                appKey,
                                model.modelId,
                                model.companyIdentifier,
                                opcode,
                                parameters
                            )
                            sendMessage(
                                method,
                                element.elementAddress,
                                message,
                                callback,
                                timeout,
                                retry
                            )
                        }
                    }
                } else {
                    //todo
                    Utils.printLog(TAG, "model don't boundAppKey")
                }
            }
        }
    }

//    fun unRegisterMeshMsg() {
//        MeshProxyService.mMeshProxyService?.unRegisterMeshMsg()
//    }

    fun unRegisterConnectListener() {
        MeshProxyService.mMeshProxyService?.unRegisterConnectListener()
    }

    fun exportMeshNetwork(callback: NetworkExportUtils.NetworkExportCallbacks) {
        MeshProxyService.mMeshProxyService?.exportMeshNetwork(callback)
    }

    fun importMeshNetwork(json: String, callback: StringCallback) {
        rx.Observable.create<String> {
        }.subscribeOn(AndroidSchedulers.mainThread()).doOnSubscribe {
            disConnect()
            MeshProxyService.mMeshProxyService?.importMeshNetworkJson(json, callback)
        }.subscribeOn(AndroidSchedulers.mainThread()).subscribe()
    }

//    fun updateDeviceImg(
//        context: Context,
//        uuid: String,
//        path: String,
//        callback: DfuAdapter.DfuHelperCallback
//    ) {
//        if (uuid.isEmpty() || path.isEmpty()) {
//            callback.onError(
//                Constants.ERROR_TYPE_FILE_ERROR,
//                Constants.ConnectState.DFU_PARAM_ERROR.code
//            )
//        }
//
//        if (!File(path).exists()) {
//            callback.onError(
//                Constants.ERROR_TYPE_PARAM_ERROR,
//                Constants.ConnectState.DFU_FILE_NOT_EXIST.code
//            )
//        }
//
//        mDfuHelper = DfuHelper.getInstance(context)
//        if (mDfuHelper != null && getDfuConfig() != null) {
//            mDfuHelper.addDfuHelperCallback(callback)
//            initialize(context, Utils.getMacFromUUID(uuid))
//            BackgroundScanAutoConnected.getInstance()
//                .scanLeDevice(false, BackgroundScanAutoConnected.SCAN_PROXY)
//            BackgroundScanAutoConnected.getInstance().gattLayerInstance
//                .setIs_OTA_SERVICE_On(true)
//            if (BackgroundScanAutoConnected.getInstance().isConnect()) {
//                connectRemoteDevice(
//                    BackgroundScanAutoConnected.getInstance().getGattLayerInstance().getBluetoothDevice(
//                        Utils.getMacFromUUID(uuid)
//                    ), false
//                )
//            }
//
//            getDfuConfig()?.otaWorkMode = Constants.DFU_WORK_MODE_SILENCE//设置更新模式为静默更新
//            getDfuConfig()?.filePath = path//设置bin文件路径
//            getDfuConfig()?.address = Utils.getMacFromUUID(uuid)//设置更新设备
//            try {
//                var binInfo = BinFactory.loadImageBinInfo(
//                    path,
//                    mDfuHelper.otaDeviceInfo, false
//                )
//                if (checkFileContent(mDfuHelper, getDfuConfig(), binInfo)) {
////                getDfuConfig()?.setAutomaticActiveEnabled(SettingsHelper.getInstance().isAutomaticActiveEnabled())
//                    getDfuConfig()?.isBatteryCheckEnabled = false
//                    getDfuConfig()?.isVersionCheckEnabled = false
//
////                        getDfuConfig().setSpeedControlEnabled(true)
////                        getDfuConfig().setControlSpeed(speed)
//                    getDfuConfig()?.isSpeedControlEnabled = (false)
//                    getDfuConfig()?.controlSpeed = (0)
//                    getDfuConfig()?.address = getDfuConfig()?.address
//
//                    val ret = mDfuHelper.startOtaProcess(getDfuConfig())
//                    if (!ret) {
//                        //todo
//                        Utils.printLog(TAG, "开始ota失败")
//                    }
//                } else {
//                    //todo
//                    Utils.printLog(TAG, "校验bin文件失败")
//                }
//            }catch (e:DfuException){
//                e.printStackTrace()
//                //todo
//            }
//        } else {
//            //todo
//            Utils.printLog(TAG, "dfu 未初始化！")
//        }
//    }


    fun createGroup(groupName: String, groupAdd: Int = 0): Boolean {
        Utils.printLog(TAG, "groupName:$groupName,groupAdd:$groupAdd")
        if (groupAdd != 0 && groupAdd < 0xC000) {
            return false
        }

        try {
            var group: Group? = getGroupByName(groupName)
            if (group != null) {
                addPoxyFilter(MeshAddress.formatAddress(group.address, false))
                return true
            }

            val network = MeshProxyService.mMeshProxyService?.getMeshNetwork()
            group = if (groupAdd == 0) network?.createGroup(
                network.getSelectedProvisioner(),
                groupName
            ) else network?.createGroup(
                network.getSelectedProvisioner(),
                groupName,
                groupAdd
            )

            if (group != null) {
                if (network?.addGroup(group) ?: false) {
                    addPoxyFilter(MeshAddress.formatAddress(group.address, false))
                    return true
                }
            }
        } catch (ex: IllegalArgumentException) {
            ex.printStackTrace()
        }
        return false
    }

    fun removeGroup(groupName: String) {
        getGroupByName(groupName)?.apply {
            MeshProxyService.mMeshProxyService?.getMeshNetwork()?.let {
                it.removeGroup(this)
            }
        }
    }

    private fun addPoxyFilter(groupAdd: String): Boolean {
        val address = MeshParserUtils.toByteArray(groupAdd)
        Utils.printLog(TAG, "address bytes:${ByteUtil.bytesToHexString(address)}")

        val addAddressToFilter = ProxyConfigAddAddressToFilter(
            arrayListOf(
                AddressArray(
                    address[0],
                    address[1]
                )
            )
        )

        try {
            MeshProxyService.mMeshProxyService?.mNrfMeshManager?.meshManagerApi?.createMeshPdu(
                MeshAddress.UNASSIGNED_ADDRESS,
                addAddressToFilter
            )
            return true
        } catch (ex: IllegalArgumentException) {
            ex.printStackTrace()
        }
        return false
    }

    fun getGroup(): ArrayList<Group> {
        return MeshProxyService.mMeshProxyService?.mNrfMeshManager?.mGroups?.value ?: ArrayList()
    }

    fun getGroupByName(groupName: String): Group? {
        getGroup().forEach {
            if (groupName == it.name) {
                return it
            }
        }
        return null
    }

    fun getGroupByAddress(groupAddr: Int): Group? {
        getGroup().forEach {
            if (groupAddr == it.address) {
                return it
            }
        }
        return null
    }

//    fun setPublication(groupName: String) {
//        //通过uuid获取group
//        var group = getGroupByName(groupName)
//        if (group == null) {
//            Utils.printLog(TAG, "setPublication group is null")
//            return
//        }
//
//        //获取provisioned节点
//        var node = getProvisionedNodeByUUID(groupName)
//        if (node == null) {
//            Utils.printLog(TAG, "setPublication node is null")
//            return
//        }
//
//        var publishAddress = group.address
//        node.elements.values.elementAt(0).meshModels?.values?.forEach { meshModel ->
//            if (meshModel.boundAppKeyIndexes?.size ?: 0 > 0) {
//                runBlocking {
//                    launch {
//                        delay(1000)
//                        var meshMsg = ConfigModelPublicationSet(
//                            node.elements.values.elementAt(0).elementAddress
//                            ,
//                            publishAddress,
//                            meshModel.boundAppKeyIndexes?.get(0) ?: 0,
//                            false,
//                            MeshParserUtils.USE_DEFAULT_TTL
//                            ,
//                            53,
//                            0,
//                            1,
//                            1,
//                            meshModel.modelId
//                        )
//
//                        try {
//                            MeshProxyService.mMeshProxyService?.mNrfMeshManager?.meshManagerApi
//                                ?.createMeshPdu(node.unicastAddress, meshMsg)
//                        } catch (ex: IllegalArgumentException) {
//                            ex.printStackTrace()
//                        }
//                    }
//                }
//
//            }
//
//        }
//
//    }
//
//    fun sendSubscribeMsg(uuid: String) {
//        var node = getProvisionedNodeByUUID(uuid)
//        if (node == null) {
//            Utils.printLog(TAG, "sendSubscribeMsg node is null")
//            return
//        }
//
//        var group = getGroupByName(uuid)
//        if (group == null) {
//            Utils.printLog(TAG, "sendSubscribeMsg group is null")
//            return
//        }
//        node.elements.values.elementAt(0).meshModels?.values?.forEach { model ->
//            runBlocking {
//                launch {
//                    delay(1000)
//                    val modelIdentifier = model.getModelId()
//                    val configModelSubscriptionAdd: MeshMessage
//                    var elementAddress = node.elements.values.elementAt(0).elementAddress
//                    if (group.addressLabel == null) {
//                        configModelSubscriptionAdd =
//                            ConfigModelSubscriptionAdd(
//                                elementAddress,
//                                group.getAddress(),
//                                modelIdentifier
//                            )
//                    } else {
//                        configModelSubscriptionAdd = ConfigModelSubscriptionVirtualAddressAdd(
//                            elementAddress,
//                            group.getAddressLabel()!!,
//                            modelIdentifier
//                        )
//                    }
//                    sendMessage(node.unicastAddress, configModelSubscriptionAdd)
//
//                }
//            }
//        }
//    }

    fun subscribeLightStatus(meshCallback: MeshCallback) {
        rx.Observable.create<String> {
        }.subscribeOn(AndroidSchedulers.mainThread()).doOnSubscribe {
            MeshProxyService.mMeshProxyService?.subscribeLightStatus(meshCallback)
        }.subscribeOn(AndroidSchedulers.mainThread()).subscribe()
    }

    fun unSubscribeLightStatus() {
        MeshProxyService.mMeshProxyService?.unSubscribeLightStatus()
    }

    fun clear() {
        getMeshNetwork()?.apply {
            this.nodes?.clear()
            this.appKeys?.forEach { applicationKey ->
                if (applicationKey.boundNetKeyIndex == 0)
                    this.appKeys.remove(applicationKey)
            }
            this.netKeys?.forEach { netKey ->
                if (netKey.keyIndex != 0) {
                    this.netKeys.remove(netKey)
                }
            }
            this.groups?.forEach { group ->
                this.removeGroup(group)
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
            if (mMeshProxyService == null)
                mMeshProxyService = this

            mProvisionCallback?.apply {
                getProvisionedNodes(this)
            }
        }
    }
}