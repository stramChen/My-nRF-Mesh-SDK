package qk.sdk.mesh.meshsdk

import android.content.Context
import android.content.Intent
import android.util.Log
import no.nordicsemi.android.meshprovisioner.*
import no.nordicsemi.android.meshprovisioner.transport.*
import no.nordicsemi.android.meshprovisioner.utils.AddressArray
import no.nordicsemi.android.meshprovisioner.utils.MeshAddress
import no.nordicsemi.android.meshprovisioner.utils.MeshParserUtils
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
//    fun checkPermission(activity: Activity, listener: ListenerWrapper.PermissionRequestListener) {
//        PermissionUtil.checkMeshPermission(activity, listener)
//    }

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
        MeshProxyService.mMeshProxyService?.stopScan()
    }

    // 建立连接
    fun connect(
            device: ExtendedBluetoothDevice,
            connectToNetwork: Boolean,
            callback: ConnectCallback?
    ) {
//        rx.Observable.create<String> {
//        }.subscribeOn(AndroidSchedulers.mainThread()).doOnSubscribe {
//        unRegisterConnectListener()
        MeshProxyService.mMeshProxyService?.connect(device, connectToNetwork, callback)
//        }.subscribeOn(AndroidSchedulers.mainThread()).sendSubscribeMsg()
    }

    // 添加蓝牙连接回调
    // 当前连接的 mesh 代理节点状态变化时，回调通知应用层
    fun addConnectCallback(callback: ConnectCallback) {
        rx.Observable.create<String> {
        }.subscribeOn(AndroidSchedulers.mainThread()).doOnSubscribe {
            MeshProxyService.mMeshProxyService?.setConnectCallback(callback)
        }.subscribeOn(AndroidSchedulers.mainThread()).subscribe()
    }

    // 获取当前已连接的蓝牙设备
    fun getConnectedDevice(): ExtendedBluetoothDevice? {
        return MeshProxyService.mMeshProxyService?.getConnectedDevice()
    }

    // 断开当前蓝牙连接
    fun disConnect() {
        MeshProxyService.mMeshProxyService?.disConnect()
    }

    // 断开当前蓝牙连接
    fun innerDisConnect() {
        MeshProxyService.mMeshProxyService?.innerDisConnect()
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

    /**
     * 向设备添加appKey
     */
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
            Utils.printLog(TAG, "addAppKeys() applicationKey is null!")
        }

        applicationKey?.apply {
            val networkKey = getNetworkKey(this.boundNetKeyIndex)
            Utils.printLog(
                    TAG,
                    "networkKey.keyIndex:${networkKey?.keyIndex},applicationKey.boundNetKeyIndex:${this.boundNetKeyIndex}"
            )
            if (networkKey == null || networkKey.keyIndex != this.boundNetKeyIndex) {
                Utils.printLog(TAG, "addAppKeys() networkKey is null!")
            } else {
                val node = getSelectedMeshNode()
                if (node != null) {
                    val isNodeKeyAdd = MeshParserUtils.isNodeKeyExists(
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
     * 在绑定好appkey之后，获取当前节点的element、model列表
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

    fun createNetworkKey(key: String) {
//        rx.Observable.create<String> {
//        }.subscribeOn(AndroidSchedulers.mainThread()).doOnSubscribe {
        var netKey =
                MeshProxyService.mMeshProxyService?.mNrfMeshManager
                        ?.meshManagerApi?.meshNetwork?.createNetworkKey()
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

    /**
     * 向设备发送指令
     * @param message 组成：$Opcode$TID$AttrType$AttrValue
     */
    fun sendMessage(
            key: String,
            dst: Int,
            message: MeshMessage,
            callback: BaseCallback?,
            timeOut: Boolean = false,
            retry: Boolean = false
    ) {
        try {
            sendMeshPdu(key, dst, message, callback, timeOut, retry)
        } catch (ex: IllegalArgumentException) {
            ex.printStackTrace()
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
            key: String,
            dst: Int,
            message: MeshMessage,
            callback: BaseCallback?,
            timeOut: Boolean = false,
            retry: Boolean = false
    ) {
//        rx.Observable.create<String> {
//        }.subscribeOn(AndroidSchedulers.mainThread()).doOnSubscribe {
        Utils.printLog(TAG, "===>-mesh- sendMeshPdu，method name：${key}")
        MeshProxyService.mMeshProxyService?.sendMeshPdu(
                key,
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

    fun unRegisterConnectListener() {
        MeshProxyService.mMeshProxyService?.unRegisterConnectListener()
    }

    fun exportMeshNetwork(callback: NetworkExportUtils.NetworkExportCallbacks) {
        MeshProxyService.mMeshProxyService?.exportMeshNetwork(callback)
    }

    fun importMeshNetwork(json: String, callback: StringCallback) {
            disConnect()
            MeshProxyService.mMeshProxyService?.importMeshNetworkJson(json, callback)
    }

    fun createGroup(groupName: String, groupAdd: Int = 0): Boolean {
        Utils.printLog(TAG, "groupName:$groupName,groupAdd:$groupAdd")
        //0xC000是起始地址，小于起始地址我们直接返回
        if (groupAdd != 0 && groupAdd < 0xC000) {
            return false
        }

        try {
            var group: Group? = null
            var groups  = getGroupByName(groupName)
            if(groups.size >0){
                group = groups[0]
            }
            if (group != null) {
                addPoxyFilter(MeshAddress.formatAddress(group.address, false))
                return true
            }

            val network = MeshProxyService.mMeshProxyService?.getMeshNetwork()
            val provisioner = network?.getSelectedProvisioner();
            //直接给当前provisioner分配最大组播地址0xC000-0FEFF(0FEFF-0FFFF是保留地址不做分配)
            //因为目前我们只支持一个组网里面只有一个provisioner,所以可以给他直接分配最大地址
            var range: AllocatedGroupRange? = AllocatedGroupRange(
                    "C000".toInt(16),
                    "FEFF".toInt(16)
            )
            if (range != null) {
                provisioner?.addRange(range)
            }

            group = if (groupAdd == 0)
                provisioner?.let {
                    network?.createGroup(
                            it,
                            groupName
                    )
                } else provisioner?.let {
                network?.createGroup(
                        it,
                        groupName,
                        groupAdd
                )
            }

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
        getGroupByName(groupName).forEach {
            it?.apply {
                MeshProxyService.mMeshProxyService?.getMeshNetwork()?.let {
                    it.removeGroup(this)
                    Log.d(TAG,"===>-mesh-删除脏数据成功:${groupName}")
                }
            }
        }
    }

    private fun addPoxyFilter(groupAdd: String): Boolean {
        val address = MeshParserUtils.toByteArray(groupAdd)
        Utils.printLog(TAG, "addPoxyFilter address bytes:${ByteUtil.bytesToHexString(address)}")

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

    fun getGroupByName(groupName: String): ArrayList<Group> {
        var groups: ArrayList<Group> = ArrayList<Group>();
        getGroup().forEach {
            if (groupName == it.name) {
                groups.add(it)
            }
        }
        return groups
    }

    fun getGroupByAddress(groupAddr: Int): Group? {
        getGroup().forEach {
            if (groupAddr == it.address) {
                return it
            }
        }
        return null
    }

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

    fun clearGatt() {
        MeshProxyService.mMeshProxyService?.clearGatt()
    }

    fun restartService(context: Context, callback: MapCallback) {
        context.stopService(Intent(context, MeshProxyService::class.java))
        mMeshProxyServiceCallback = callback
        initMesh(context)
    }

    /**
     * 生成一个hash键，但有时候它并不唯一，因此后面会优化
     */
    fun generatePrimaryKey(
            sequence: Int? = null,
            uuid: String? = null
    ) = sequence.toString() + ":" + uuid?.toUpperCase()


    private var mMeshProxyServiceCallback: MapCallback? = null

    internal class MeshProxyService : BaseMeshService() {
        companion object {
            var mMeshProxyService: MeshProxyService? = null
                private set
        }

        override fun onCreate() {
            super.onCreate()
            Utils.printLog(TAG, "service created")

            mNrfMeshManager?.mNetworkImportState?.observe(this, androidx.lifecycle.Observer {
                mMeshProxyServiceCallback?.onResult(
                        hashMapOf(
                                "code" to Constants.ConnectState.SERVICE_CREATED.code,
                                "msg" to Constants.ConnectState.SERVICE_CREATED.msg
                        )
                )
                sServiceLifecycleLintener?.onCreate()
            })

            mMeshProxyService = this
        }
    }
}