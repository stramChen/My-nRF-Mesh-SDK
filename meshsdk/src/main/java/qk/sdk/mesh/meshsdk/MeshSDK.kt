package qk.sdk.mesh.meshsdk

import android.bluetooth.BluetoothAdapter
import android.content.Context
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.weyye.hipermission.PermissionCallback
import no.nordicsemi.android.meshprovisioner.ApplicationKey
import no.nordicsemi.android.meshprovisioner.UnprovisionedBeacon
import no.nordicsemi.android.meshprovisioner.models.GenericOnOffServerModel
import no.nordicsemi.android.meshprovisioner.models.VendorModel
import no.nordicsemi.android.meshprovisioner.transport.*
import no.nordicsemi.android.meshprovisioner.utils.MeshParserUtils
import qk.sdk.mesh.meshsdk.bean.CallbackMsg
import qk.sdk.mesh.meshsdk.bean.CommonErrorMsg
import qk.sdk.mesh.meshsdk.bean.ExtendedBluetoothDevice
import qk.sdk.mesh.meshsdk.callback.*
import qk.sdk.mesh.meshsdk.mesh.BleMeshManager
import qk.sdk.mesh.meshsdk.util.*
import qk.sdk.mesh.meshsdk.util.Constants.ConnectState
import java.lang.Thread.sleep
import java.util.concurrent.atomic.AtomicBoolean

object MeshSDK {
    private val TAG = "MeshSDK"
    private var mContext: Context? = null
    private var mExtendedBluetoothDeviceMap = HashMap<String, ExtendedBluetoothDevice>()

    const val VENDOR_MODELID = 153223168

    var mMeshCallbacks = ArrayList<ConnectCallback?>(5)

    var mConnectCallbacks = HashMap<String, Any>(20)

    private const val PUBLISH_INTERVAL = 88 //25秒
    private const val PUBLISH_TTL = 5

    /**
     * opcode
     */
    const val VENDOR_MSG_ATTR_GET = "10"
    const val VENDOR_MSG_ATTR_SET = "11"
    const val VENDOR_MSG_ATTR_SET_UNACKED = "12"
    const val VENDOR_MSG_ATTR_STATUS = "13"
    const val VENDOR_MSG_HB = "14"

    /**
     * 通用attr type
     */
    const val ATTR_TYPE_COMMON_GET_STATUS = "0100" //设备上报属性
    const val ATTR_TYPE_COMMON_SET_PROTOCAL_VERSION = "0200" //设置协议版本
    const val ATTR_TYPE_COMMON_GET_QUADRUPLES = "0300" // 获取设备五元组信息

    /**
     * 灯的attr type
     */
    const val ATTR_TYPE_LIGHT_ON_OFF = "0001" //开关
    const val ATTR_TYPE_LIGHT_BRIGHTNESS = "2101" //亮度
    const val ATTR_TYPE_LIGHT_TEMPRETURE = "2201" //色温
    const val ATTR_TYPE_LIGHT_HSV = "2301" //颜色hsv

    //callback name
    const val CALLBACK_GET_IDENTITY = "getDeviceIdentityKeys"

    // 初始化 mesh
    fun init(context: Context) {
        mContext = context
        Utils.mContext = context
        mContext?.apply {
            if (!Utils.isServiceExisted(this, "qk.sdk.mesh.meshsdk.MeshHelper\$MeshProxyService")) {
                MeshHelper.restartService(applicationContext, object : MapCallback {
                    override fun onResult(result: HashMap<String, Any>) {
                        result.forEach {
                            Utils.printLog(TAG, "key:${it.key},value:${it.value}")
                        }
                    }
                })
            }
            LogFileUtil.deleteLog(mContext)
        }
    }

    // 初始化 mesh
    fun init(context: Context, callback: BooleanCallback) {
        mContext = context
        Utils.mContext = context
        mContext?.apply {
            if (!Utils.isServiceExisted(this, "qk.sdk.mesh.meshsdk.MeshHelper\$MeshProxyService")) {
                MeshHelper.restartService(applicationContext, object : MapCallback {
                    override fun onResult(result: HashMap<String, Any>) {
                        result.forEach {
                            Utils.printLog(TAG, "key:${it.key},value:${it.value}")
                        }

                        result["code"]?.apply {
                            if (this is Int && this == ConnectState.SERVICE_CREATED.code) {
                                callback.onResult(true)
                            }
                        }
                    }
                })
            } else {
                callback.onResult(true)
            }
            LogFileUtil.deleteLog(mContext)
        }
    }

    fun checkPermission(callback: StringCallback) {
        if (mContext == null) {
            callback.onResultMsg(ConnectState.SDK_NOT_INIT.msg)
            return
        }

        PermissionUtil.checkMeshPermission(mContext!!, object : PermissionCallback {
            override fun onFinish() {
                callback.onResultMsg(Constants.PERMISSION_GRANTED)
            }

            override fun onGuarantee(permission: String?, position: Int) {
                callback.onResultMsg(Constants.PERMISSION_GRANTED)
            }

            override fun onDeny(permission: String?, position: Int) {
                callback.onResultMsg(Constants.PERMISSION_DENIED)
            }

            override fun onClose() {
                callback.onResultMsg(Constants.PERMISSION_DENIED)
            }
        })
    }

    /**
     * @param type Int 扫描类型：unProvisioned for 未配对节点，provisioned for 已配对节点
     * @param callback Callback RN回调callback
     */
    fun startScan(
        type: String,
        scanResultCallback: ArrayMapCallback,
        errCallback: IntCallback
    ) {
        Utils.printLog(TAG, "startScan startScan")
        disConnect()
        var scanCallback: ScanCallback = object :
            ScanCallback {
            override fun onScanResult(
                devices: List<ExtendedBluetoothDevice>,
                updatedIndex: Int?
            ) {
                var resultArray = ArrayList<HashMap<String, Any>>()
                devices.forEach {
                    Utils.printLog(TAG, "scan result:${it.getAddress()}")
                    it.beacon?.apply {
                        val unprovisionedBeacon = UnprovisionedBeacon(this.beaconData)
                        var map = HashMap<String, Any>()
                        map.put("mac", it.getAddress())
                        map.put("uuid", unprovisionedBeacon.uuid.toString())
                        map.put("rssi", it.rssi ?: 0)
                        map.put("name", it.name ?: "")
                        resultArray.add(map)
                        mExtendedBluetoothDeviceMap.put(unprovisionedBeacon.uuid.toString(), it)
                    }

                }
                scanResultCallback.onResult(resultArray)
            }

            override fun onError(msg: CallbackMsg) {
                errCallback.onResultMsg(msg.code)
            }
        }

        Utils.printLog(
            TAG,
            "startScan uuid:${if (type == Constants.SCAN_UNPROVISIONED) BleMeshManager.MESH_PROVISIONING_UUID else BleMeshManager.MESH_PROXY_UUID}"
        )
        MeshHelper.startScan(
            if (type == Constants.SCAN_UNPROVISIONED) BleMeshManager.MESH_PROVISIONING_UUID else BleMeshManager.MESH_PROXY_UUID,
            scanCallback
        )
    }

    fun stopScan() {
        MeshHelper.stopScan()
    }

    /**
     * 设备认证，先建立proxy连接，再启动配置邀请
     * 注意：android的networkKey都必须是大写的
     */
    fun provision(uuid: String, networkKey: String, callback: MapCallback) {
        mContext?.apply {
            init(this, object : BooleanCallback {
                override fun onResult(boolean: Boolean) {
                    if (boolean) {

                        var map = HashMap<String, Any>()
                        doBaseCheck(uuid, map, callback)
                        if (networkKey.isEmpty() || getAllNetworkKey().size <= 0 || !getAllNetworkKey().contains(
                                networkKey.toUpperCase()
                            )
                        ) {
                            map.put(Constants.KEY_MESSAGE, ConnectState.NET_KEY_IS_NULL.msg)
                            map.put(Constants.KEY_CODE, ConnectState.NET_KEY_IS_NULL.code)
                            callback.onResult(map)
                            return
                        }
                        setCurrentNetworkKey(networkKey.toUpperCase())
                        var hasProvisioned = false
                        var hasProvisionStart = false
                        var needReconnect = true

                        mContext?.let { _ ->
                            mExtendedBluetoothDeviceMap.get(uuid)?.let { extendedBluetoothDevice ->
                                Utils.printLog(
                                    TAG,
                                    "extendedBluetoothDevice:${extendedBluetoothDevice.getAddress()}"
                                )
                                var connectCallback = object : ConnectCallback {
                                    override fun onConnect() {
                                        //开始启动配置邀请
                                        MeshHelper.startProvision(mExtendedBluetoothDeviceMap[uuid]!!,
                                            MeshHelper.getCurrentNetworkKey()!!,
                                            object : BaseCallback {
                                                override fun onError(msg: CallbackMsg) {
                                                    map.clear()
                                                    map.put(Constants.KEY_MESSAGE, msg.msg)
                                                    map.put(Constants.KEY_CODE, msg.code)
                                                    callback.onResult(map)
                                                }
                                            })
                                    }

                                    override fun onConnectStateChange(msg: CallbackMsg) {
                                        Utils.printLog(
                                            TAG,
                                            "provision onConnectStateChange code:${msg.code} ,msg:${msg.msg}"
                                        )

                                        when (msg.code) {
                                            ConnectState.PROVISION_SUCCESS.code -> {
                                                map.clear()
                                                doMapCallback(
                                                    map,
                                                    callback,
                                                    CallbackMsg(
                                                        ConnectState.PROVISION_SUCCESS.code,
                                                        ConnectState.PROVISION_SUCCESS.msg
                                                    )
                                                )
                                            }
                                            ConnectState.DISCONNECTED.code,
                                            ConnectState.CONNECT_BLE_RESOURCE_FAILED.code -> {
                                                if (ConnectState.CONNECT_BLE_RESOURCE_FAILED.code == msg.code) {
                                                    disConnect()
                                                    MeshHelper.clearGatt()
                                                }

                                                if (hasProvisioned && needReconnect) {
                                                    MeshHelper.unRegisterConnectListener()
                                                    reConnect(callback)
                                                    connect(networkKey, callback)
                                                } else if (ConnectState.DISCONNECTED.code == msg.code && !hasProvisionStart) {
                                                    doMapCallback(map, callback, msg)
                                                } else {
                                                    MeshHelper.unRegisterConnectListener()
                                                    doMapCallback(
                                                        map,
                                                        callback,
                                                        CallbackMsg(
                                                            ConnectState.PROVISION_FAILED.code,
                                                            ConnectState.PROVISION_FAILED.msg
                                                        )
                                                    )
                                                }
                                            }
                                            else -> {
                                                hasProvisionStart = true
                                                doMapCallback(map, callback, msg)
                                            }
                                        }
                                    }

                                    override fun onError(msg: CallbackMsg) {
                                        if (msg.code == ConnectState.STOP_CONNECT.code) {

                                        }
                                        map.clear()
                                        map.put(Constants.KEY_MESSAGE, msg.msg)
                                        map.put(Constants.KEY_CODE, msg.code)
                                        callback.onResult(map)
                                        needReconnect = false
                                    }
                                }
                                MeshHelper.connect(
                                    extendedBluetoothDevice,
                                    false, connectCallback
                                )
                                mMeshCallbacks.add(connectCallback)
                            }
                        }
                    }
                }
            })
        }
    }

    /**
     * 重连
     */
    private fun reConnect(callback: MapCallback) {
        mContext?.apply {
            init(this.applicationContext, object : BooleanCallback {
                override fun onResult(boolean: Boolean) {
                    if (boolean) {
                        var map = HashMap<String, Any>()
                        mConnectCallbacks.forEach { connectStateCallback ->
                            if (connectStateCallback is MapCallback) {
                                doMapCallback(
                                    map, callback,
                                    CallbackMsg(
                                        CommonErrorMsg.DISCONNECTED.code,
                                        CommonErrorMsg.DISCONNECTED.msg
                                    )
                                )
                            } else if (connectStateCallback is BooleanCallback) {
                                connectStateCallback.onResult(false)
                            }
                        }
                        mConnectCallbacks.clear()
                        MeshHelper.unRegisterConnectListener()
                    }
                }
            })
        }
    }

    /**
     * 是否建立proxy连接
     */
    fun isConnectedToProxy(callback: BooleanCallback) {
        callback.onResult(MeshHelper.isConnectedToProxy())
    }

    fun getAllNetworkKey(): ArrayList<String> {
        var keyList = ArrayList<String>()
        MeshHelper.getAllNetworkKey()?.forEach {
            keyList.add(ByteUtil.bytesToHexString(it.key))
        }
        return keyList
    }

    fun setCurrentNetworkKey(networkKey: String) {
        MeshHelper.setCurrentNetworkKey(networkKey.toUpperCase())
    }

    fun getCurrentNetworkKey(callback: StringCallback) {
        callback.onResultMsg(MeshHelper.getCurrentNetworkKeyStr() ?: "")
    }

    fun createNetworkKey(networkKey: String): Boolean {
        if (networkKey.isEmpty() || getAllNetworkKey().contains(networkKey.toUpperCase())) {
            return false
        }
        MeshHelper.createNetworkKey(networkKey)
        return true
    }

    fun removeNetworkKey(key: String, callback: MapCallback) {
        MeshHelper.removeNetworkKey(key.toUpperCase(), callback)
    }

    fun createApplicationKey(networkKey: String): String {
        return MeshHelper.createApplicationKey(networkKey.toUpperCase())
    }

    fun getAllApplicationKey(networkKey: String, callback: ArrayStringCallback) {
        MeshHelper.getAllApplicationKey(networkKey.toUpperCase(), callback)
    }

    fun removeApplicationKey(appKey: String, callback: IntCallback) {
        MeshHelper.removeApplicationKey(appKey, callback)
    }

    /**
     * 获取所有已配对过的节点
     */
    fun getProvisionedNodes(callback: ArrayMapCallback) {
        var data = ArrayList<HashMap<String, Any>>()
        MeshHelper.getProvisionNode()?.forEach {
            var map = HashMap<String, Any>()
            map.put("uuid", it.uuid)
            map.put("name", it.nodeName)
//            map.put("productId", ByteUtil.getPIdFromUUID(it.beacon!!.getBeaconData()))
        }
        callback.onResult(data)
    }

    fun removeProvisionedNode(uuid: String) {
        MeshHelper.deleteProvisionNode(MeshHelper.getProvisionedNodeByUUID(uuid))
    }

    const val ADD_APPKEYS = "addAppkeys"
    const val GET_COMPOSITION_DATA = "getCompositionData"

    /**
     * 添加appkey，获取Compositio nData并自动绑定所有的element下的所有model
     */
    fun addApplicationKeyForNode(uuid: String, appKey: String, callback: MapCallback) {
        mContext?.apply {
            init(this, object : BooleanCallback {
                override fun onResult(boolean: Boolean) {
                    if (boolean) {
                        var map = HashMap<String, Any>()
                        doBaseCheck(uuid, map, callback)

                        MeshHelper.getAppkeyByKeyName(appKey)?.let { applicationKey ->
                            MeshHelper.getProvisionNode()?.forEach { node ->
                                if (node.uuid == uuid) {
                                    MeshHelper.setSelectedMeshNode(node)
                                }
                            }

                            if (MeshHelper.isConnectedToProxy()) {
                                var meshCallback = object :
                                    MeshCallback {
                                    override fun onReceive(msg: MeshMessage) {
                                        if (msg is ConfigAppKeyStatus) {
                                            if (msg.isSuccessful) {//添加appkey成功
                                                Utils.printLog(TAG, "add app key success!")
                                                MeshHandler.removeRunnable(ADD_APPKEYS)
                                                if ((MeshHelper.getSelectedMeshNode()?.elements?.size
                                                        ?: 0) <= 0
                                                ) {
                                                    MeshHelper.getCompositionData(
                                                        GET_COMPOSITION_DATA,
                                                        this
                                                    )
                                                }
                                            } else {
                                                Utils.printLog(
                                                    TAG,
                                                    "add app key failed,because ${msg.statusCodeName}!"
                                                )
                                                map.clear()
                                                doMapCallback(
                                                    map, callback,
                                                    CallbackMsg(
                                                        ConnectState.BIND_APP_KEY_FOR_NODE_FAILED.code,
                                                        msg.statusCodeName
                                                    )
                                                )
                                            }
                                        } else if (msg is ConfigCompositionDataStatus) {
                                            Utils.printLog(TAG, "get getCompositionData success!")

                                            //获取到CompositionData之后默认设备端自己已绑定好key，app端自己异步去将本地数据更新
                                            MeshHandler.removeRunnable(GET_COMPOSITION_DATA)
                                            bindKey(applicationKey.keyIndex)
                                            doMapCallback(
                                                map, callback,
                                                CallbackMsg(
                                                    ConnectState.BIND_APP_KEY_SUCCESS.code,
                                                    ConnectState.BIND_APP_KEY_SUCCESS.msg
                                                )
                                            )
                                        }
                                    }

                                    override fun onError(msg: CallbackMsg) {
                                        doMapCallback(
                                            map, callback,
                                            CallbackMsg(
                                                ConnectState.BIND_APP_KEY_FOR_NODE_FAILED.code,
                                                ConnectState.BIND_APP_KEY_FOR_NODE_FAILED.msg
                                            )
                                        )
                                    }
                                }

                                //添加appkey
                                MeshHelper.addAppkeys(
                                    ADD_APPKEYS,
                                    applicationKey.keyIndex,
                                    meshCallback,
                                    true,
                                    true
                                )
                            } else {
                                map.clear()
                                map.put(
                                    Constants.KEY_MESSAGE,
                                    ConnectState.CONNECT_NOT_EXIST.msg
                                )
                                map.put(
                                    Constants.KEY_CODE,
                                    ConnectState.CONNECT_NOT_EXIST.code
                                )
                            }
                        }
                    }
                }
            })
        }
    }

    /**
     * 遍历绑定所有element下所有model
     */
    private fun bindKey(appKeyIndex: Int) {
        MeshHelper.getSelectedMeshNode()?.let { node ->
            node.elements?.forEach { eleArrd, element ->
                element.meshModels?.forEach { modelId, model ->
                    model?.setBoundAppKeyIndex(appKeyIndex)
                }
            }
        }
    }

    /**
     * 断开gatt连接
     */
    fun disConnect() {
        MeshHelper.disConnect()
        clearConnectCallbacks()
        for (callback in mMeshCallbacks)
            callback?.onError(
                CallbackMsg(
                    ConnectState.STOP_CONNECT.code,
                    ConnectState.STOP_CONNECT.msg
                )
            )
        mMeshCallbacks.clear()
    }

    private fun clearConnectCallbacks() {
        mConnectCallbacks.forEach { connectStateCallback ->
            if (connectStateCallback is MapCallback) {
                var map = HashMap<String, Any>()
                doMapCallback(
                    map, connectStateCallback,
                    CallbackMsg(
                        CommonErrorMsg.DISCONNECTED.code,
                        CommonErrorMsg.DISCONNECTED.msg
                    )
                )
            } else if (connectStateCallback is BooleanCallback) {
                connectStateCallback.onResult(false)
            }
        }

        mConnectCallbacks.clear()
    }

    private fun doBaseCheck(uuid: String?, map: HashMap<String, Any>, callback: MapCallback) {
        if (mContext == null) {//判断sdk是否被初始化
            map.put(Constants.KEY_MESSAGE, Constants.SDK_NOT_INIT_MSG)
            map.put(Constants.KEY_CODE, Constants.SDK_NOT_INIT_CODE)
            callback.onResult(map)
            return
        }

        if (!BluetoothAdapter.getDefaultAdapter().isEnabled) {
            map.put(Constants.KEY_MESSAGE, ConnectState.BLE_NOT_AVAILABLE.msg)
            map.put(Constants.KEY_CODE, ConnectState.BLE_NOT_AVAILABLE.code)
            callback.onResult(map)
            return
        }

        if (null != uuid && mExtendedBluetoothDeviceMap[uuid] == null) {//判断是否存在此设备
            map.put(
                Constants.KEY_MESSAGE,
                ConnectState.CANNOT_FIND_DEVICE_BY_MAC.msg
            )
            map.put(
                Constants.KEY_CODE,
                ConnectState.CANNOT_FIND_DEVICE_BY_MAC.code
            )
            callback.onResult(map)
            return
        }
    }

    /**
     * 协议v2.0：开关
     */
    fun setGenericOnOff(
        uuid: String, onOff: Boolean,
        eleIndex: Int, callback: BooleanCallback
    ) {
        if (!MeshHelper.isConnectedToProxy()) {
            callback.onResult(false)
            return
        }

        mConnectCallbacks["setGenericOnOff"] = callback
        try {
            if (MeshHelper.getSelectedMeshNode()?.uuid != uuid) {
                MeshHelper.getProvisionNode()?.forEach { node ->
                    if (node.uuid.toUpperCase() == uuid.toUpperCase()) {
                        MeshHelper.setSelectedMeshNode(node)
                    }
                }
            }

            MeshHelper.setSelectedModel(
                MeshHelper.getSelectedMeshNode()?.elements?.values?.elementAt(eleIndex),
                MeshHelper.getSelectedElement()?.meshModels?.get(VENDOR_MODELID)
            )

            sendMeshMessage(
                uuid,
                eleIndex,
                VENDOR_MSG_ATTR_SET,
                "$ATTR_TYPE_LIGHT_ON_OFF${if (onOff) "01" else "00"}",
                callback
            )

            mConnectCallbacks.remove("setGenericOnOff")
            callback.onResult(true)
        } catch (e: Exception) {
            e.printStackTrace()
            callback.onResult(false)
        }
    }

//    fun setUnacknowledgedGenericOnOff(
//        uuid: String, onOff: Boolean,
//        eleIndex: Int, callback: BooleanCallback
//    ) {
//        if (!MeshHelper.isConnectedToProxy()) {
//            callback.onResult(false)
//            return
//        }
//
//        mConnectCallbacks["setGenericOnOff"] = callback
//        try {
//            if (MeshHelper.getSelectedMeshNode()?.uuid != uuid) {
//                MeshHelper.getProvisionNode()?.forEach { node ->
//                    if (node.uuid.toUpperCase() == uuid.toUpperCase()) {
//                        MeshHelper.setSelectedMeshNode(node)
//                    }
//                }
//            }
//
//            MeshHelper.setSelectedModel(
//                MeshHelper.getSelectedMeshNode()?.elements?.values?.elementAt(eleIndex),
//                MeshHelper.getSelectedElement()?.meshModels?.get(ON_OFF_MODELID)
//            )
//            MeshHelper.sendUnacknowledgedGenericOnOff(onOff, 0)
//
//            mConnectCallbacks.remove("setGenericOnOff")
//            callback.onResult(true)
//        } catch (e: Exception) {
//            e.printStackTrace()
//            callback.onResult(false)
//        }
//    }

    fun sendMeshMessage(
        uuid: String,
        elementIndex: Int,
        opcode: String,
        param: String,
        callback: Any,
        method: String = "",
        timeout: Boolean = false,
        retry: Boolean = false
    ) {
        mContext?.apply {
            init(this, object : BooleanCallback {
                override fun onResult(boolean: Boolean) {
                    if (boolean) {
                        if (!MeshHelper.isConnectedToProxy()) {
                            doVendorCallback(
                                callback,
                                false,
                                CallbackMsg(
                                    CommonErrorMsg.DISCONNECTED.code,
                                    CommonErrorMsg.DISCONNECTED.msg
                                )
                            )
                            return
                        }

                        mConnectCallbacks[if (method.isEmpty()) "sendMeshMessage" else method] =
                            callback

                        if (MeshHelper.getSelectedMeshNode()?.uuid != uuid) {
                            MeshHelper.getProvisionNode()?.forEach { node ->
                                if (node.uuid == uuid) {
                                    MeshHelper.setSelectedMeshNode(node)
                                }
                            }
                        }

                        var selElement =
                            MeshHelper.getSelectedMeshNode()?.elements?.values?.elementAt(
                                elementIndex
                            )
//        selElement?.meshModels?.forEach { key, model ->
//            if (model is VendorModel) {
//
//            }
//        }
                        MeshHelper.setSelectedModel(
                            selElement,
                            MeshHelper.getSelectedMeshNode()?.elements?.values?.elementAt(
                                elementIndex
                            )?.meshModels?.get(VENDOR_MODELID)
                        )

                        var newParam = ""
                        //如果消息有参数，消息参数需加上tid，规则：秒级时间戳余255
                        if (param.isNotEmpty()) {
                            var timeCuts = System.currentTimeMillis() / 1000 % 255
                            newParam =
                                "${ByteUtil.bytesToHexString(byteArrayOf(timeCuts.toByte()))}$param"
                        }

                        var msgIndex = -1
                        val element = MeshHelper.getSelectedElement()
                        if (element != null) {
                            val model = MeshHelper.getSelectedModel()
                            if (model != null && model is VendorModel) {
                                if (model.boundAppKeyIndexes.size > 0) {
                                    val appKeyIndex = model.boundAppKeyIndexes[0]
                                    val appKey = MeshHelper.getMeshNetwork()?.getAppKey(appKeyIndex)
                                    val message: MeshMessage
                                    if (appKey != null) {
                                        message = VendorModelMessageUnacked(
                                            appKey,
                                            model.modelId,
                                            model.companyIdentifier,
                                            Integer.valueOf(opcode, 16),
                                            ByteUtil.hexStringToBytes(newParam)
                                        )
                                        var meshCallback = object : MeshCallback {
                                            override fun onReceive(msg: MeshMessage) {
                                                Utils.printLog(
                                                    TAG,
                                                    "vendor msg:${ByteUtil.bytesToHexString(msg.parameter)}"
                                                )
                                                if (!MeshHelper.isConnectedToProxy()) {
                                                    Utils.printLog(
                                                        TAG,
                                                        "disconnect"
                                                    )
                                                    doVendorCallback(
                                                        callback, false,
                                                        CallbackMsg(
                                                            CommonErrorMsg.DISCONNECTED.code,
                                                            CommonErrorMsg.DISCONNECTED.msg

                                                        )
                                                    )
                                                }
                                                Utils.printLog(
                                                    TAG,
                                                    "send opcode:$opcode,param:${String(msg.parameter)}"
                                                )

                                                synchronized(msgIndex) {
                                                    when (opcode) {
                                                        "02" -> {//重启网关
                                                            if (callback is BooleanCallback) {
                                                                callback.onResult(true)
                                                            }

                                                            mConnectCallbacks.remove(if (method.isEmpty()) "sendMeshMessage" else method)
                                                        }
                                                        "04" -> {//set cwrgb
                                                            if (callback is MapCallback && msg.parameter.size == 5) {
                                                                var map = HashMap<String, Any>()

                                                                var c = msg.parameter[0].toInt()
                                                                var w = msg.parameter[1].toInt()
                                                                var r = msg.parameter[2].toInt()
                                                                var g = msg.parameter[3].toInt()
                                                                var b = msg.parameter[4].toInt()
                                                                map.put("c", c)
                                                                map.put("w", w)
                                                                map.put("r", r)
                                                                map.put("g", g)
                                                                map.put("b", b)
                                                                map.put(
                                                                    "isOn",
                                                                    if (c == 0 && w == 0 && r == 0 && g == 0 && b == 0) false else true
                                                                )
                                                                callback.onResult(map)
                                                                mConnectCallbacks.remove(if (method.isEmpty()) "sendMeshMessage" else method)
                                                                msgIndex = 0
                                                            } else {
                                                                //todo log
                                                            }
                                                        }
                                                        "05" -> {//get cwrgb
                                                            if (msgIndex < 0 && callback is BooleanCallback) {
                                                                callback.onResult(true)
                                                                mConnectCallbacks.remove(if (method.isEmpty()) "sendMeshMessage" else method)
                                                                msgIndex = 0
                                                            } else {
                                                                //todo log
                                                            }
                                                        }
                                                        "0D", "0E", "0F", "11" -> {//set HSV
                                                            if (msgIndex < 0 && callback is BooleanCallback) {
                                                                callback.onResult(true)

                                                                mConnectCallbacks.remove(if (method.isEmpty()) "sendMeshMessage" else method)
                                                                msgIndex = 0
                                                            } else {
                                                                //todo log
                                                            }
                                                        }
                                                        else -> {

                                                        }
                                                    }
                                                }

                                            }

                                            override fun onError(msg: CallbackMsg) {
                                                doVendorCallback(callback, false, msg)
                                            }
                                        }

                                        MeshHelper.sendMessage(
                                            method,
                                            element.elementAddress,
                                            message,
                                            meshCallback, timeout, retry
                                        )
                                    }
                                } else {
                                    //todo
                                    Utils.printLog(TAG, "model don't boundAppKey")
                                }
                            }
                        }
                    }
                }
            })
        }
    }

    private fun doVendorCallback(callback: Any, result: Boolean, msg: CallbackMsg?) {
        if (callback is BooleanCallback) {
            callback.onResult(result)
        } else if (callback is MapCallback && msg != null) {
            var map = HashMap<String, Any>()
            doMapCallback(map, callback, msg)
        }
        return
    }

    /**
     * 协议v2 .0:获取设备四元组
     */
    fun getDeviceQuadruples(uuid: String, callback: MapCallback) {
        Utils.printLog(TAG, "start getDeviceIdentityKeys")
        sendMeshMessage(
            uuid,
            0,
            VENDOR_MSG_ATTR_GET,
            "$ATTR_TYPE_COMMON_GET_QUADRUPLES",
            callback,
            CALLBACK_GET_IDENTITY,
            true,
            true
        )
    }

    fun resetNode(uuid: String) {
        MeshHelper.getProvisionNode()?.forEach { node ->
            if (node.meshUuid == uuid) {
                MeshHelper.setSelectedMeshNode(node)
            }
        }

        val configNodeReset = ConfigNodeReset()
        MeshHelper.sendMessage(
            "",
            MeshHelper.getSelectedMeshNode()?.unicastAddress ?: 0,
            configNodeReset, null
        )
    }

    var isReconnect: AtomicBoolean = AtomicBoolean(false)
    var needReconnect = true

    /**
     * 建立gatt连接
     */
    fun connect(networkKey: String, callback: MapCallback) {
        mContext?.apply {
            init(this.applicationContext, object : BooleanCallback {
                override fun onResult(boolean: Boolean) {
                    if (boolean) {
                        var map = HashMap<String, Any>()
                        doBaseCheck(null, map, callback)
                        if (!MeshHelper.isConnectedToProxy() && MeshHelper.getProvisionNode()?.size ?: 0 > 0) {
                            Utils.printLog(TAG, "connect start scan")
                            setCurrentNetworkKey(networkKey)
                            MeshHelper.startScan(BleMeshManager.MESH_PROXY_UUID, object :
                                ScanCallback {
                                override fun onScanResult(
                                    devices: List<ExtendedBluetoothDevice>,
                                    updatedIndex: Int?
                                ) {
                                    if (devices.isNotEmpty()) {
                                        MeshHelper.stopScan()
                                        Utils.printLog(
                                            TAG,
                                            "connect onScanResult:${devices[0].getAddress()}"
                                        )
                                        var connectCallback = object :
                                            ConnectCallback {
                                            override fun onConnect() {
                                                stopScan()
                                                doMapCallback(
                                                    map, callback,
                                                    CallbackMsg(
                                                        ConnectState.COMMON_SUCCESS.code,
                                                        ConnectState.COMMON_SUCCESS.msg
                                                    )
                                                )
                                                isReconnect = AtomicBoolean(false)
                                            }

                                            override fun onConnectStateChange(msg: CallbackMsg) {
                                                Utils.printLog(
                                                    TAG,
                                                    "connect onConnectStateChange:${msg.msg},needReconnect:$needReconnect,isReconnect:$isReconnect"
                                                )
                                                if (msg.code == ConnectState.DISCONNECTED.code && needReconnect && !isReconnect.get()) {//连接断开，自动寻找代理节点重连
                                                    Utils.printLog(
                                                        TAG,
                                                        "connect onConnectStateChange start reConnect"
                                                    )
                                                    if (!isReconnect.get()) {
                                                        isReconnect = AtomicBoolean(true)
                                                        reConnect(callback)
                                                        connect(networkKey, callback)
                                                    }
                                                }
                                            }

                                            override fun onError(msg: CallbackMsg) {
                                                if (msg.code == ConnectState.STOP_CONNECT.code)
                                                    needReconnect = false
                                                doMapCallback(map, callback, msg)
                                            }
                                        }
                                        MeshHelper.connect(devices[0], true, connectCallback)
                                        mMeshCallbacks.add(connectCallback)
                                    }
                                }

                                override fun onError(msg: CallbackMsg) {
                                    doMapCallback(map, callback, msg)
                                }
                            }, networkKey.toUpperCase())
                        } else {
                            doMapCallback(
                                map, callback,
                                CallbackMsg(
                                    ConnectState.COMMON_SUCCESS.code,
                                    ConnectState.COMMON_SUCCESS.msg
                                )
                            )
                        }
                    }
                }
            })
        }
    }

    fun exportMeshNetwork(callback: StringCallback) {
        MeshHelper.exportMeshNetwork(object : NetworkExportUtils.NetworkExportCallbacks {
            override fun onNetworkExported() {
//                callback.onResultMsg(NrfMeshManager.EXPORT_PATH + "meshJson.json")
            }

            override fun onNetworkExportFailed(error: String?) {

            }

            override fun onNetworkExported(json: String) {
                callback.onResultMsg(json)
            }
        })
    }

    fun importMeshNetwork(json: String, callback: StringCallback) {
        if (json.isEmpty()) {
            callback.onResultMsg(ConnectState.IMPORT_MESH_JSON_EMPTY_ERR.msg)
            return
        }
        MeshHelper.importMeshNetwork(json, callback)
    }

    fun createGroup(groupName: String, callback: BooleanCallback, groupAddr: Int = 0) {
        callback.onResult(MeshHelper.createGroup(groupName, groupAddr))
    }


    fun createGroup(groupName: String, callback: BooleanCallback) {
        callback.onResult(MeshHelper.createGroup(groupName))
    }

//    fun sendSubscribeMsg(uuid: String, groupAddr: Int, callback: MapCallback) {
//        var map = HashMap<String, Any>()
//        if (doProxyCheck(uuid, map, callback)) {
//
//            if (MeshHelper.getGroupByAddress(groupAddr) == null) {
//                doMapCallback(
//                    map,
//                    callback,
//                    CallbackMsg(ConnectState.GROUP_NOT_EXIST.code, ConnectState.GROUP_NOT_EXIST.msg)
//                )
//            }
//
//            //获取provisioned节点
//            var node = MeshHelper.getProvisionedNodeByUUID(uuid)
//
//            var index = 0
//            var modelTotal = 0
//
//            node?.elements?.values?.forEach { eleValue ->
//                modelTotal += eleValue.meshModels?.size ?: 0
//                eleValue?.meshModels?.values?.forEach { model ->
//                    if (model is VendorModel || model is GenericOnOffServerModel) {
//                        val modelIdentifier = model.getModelId()
//                        val configModelSubscriptionAdd: MeshMessage
//                        var elementAddress = eleValue.elementAddress
//                        configModelSubscriptionAdd =
//                            ConfigModelSubscriptionAdd(
//                                elementAddress,
//                                groupAddr,
//                                modelIdentifier
//                            )
//                        MeshHelper.sendMessage(
//                            "sendSubscribeMsg",
//                            node.unicastAddress,
//                            configModelSubscriptionAdd,
//                            null
//                        )
//                        index++
//                    }
//                }
//            }
//
//            if (index == modelTotal) {
//                doMapCallback(
//                    map,
//                    callback,
//                    CallbackMsg(ConnectState.COMMON_SUCCESS.code, ConnectState.COMMON_SUCCESS.msg)
//                )
//            }
//        }
//    }

    /**
     * 设置设备订阅地址
     * @param uuid 设备uuid
     * @param groupName 订阅地址的groupName
     * @param callback 订阅结果回调
     * @param isSubsAll 是否遍历为所有model都添加订阅（默认只添加onOff和vendor两个model的订阅）
     */
    fun sendSubscribeMsg(
        uuid: String,
        groupName: String,
        callback: MapCallback,
        isSubsAll: Boolean = false
    ) {
        var map = HashMap<String, Any>()
        if (doProxyCheck(uuid, map, callback)) {
            //通过uuid获取group
            var group = MeshHelper.getGroupByName(groupName)
            if (group == null) {
                doMapCallback(
                    map,
                    callback,
                    CallbackMsg(
                        ConnectState.GROUP_NOT_EXIST.code,
                        ConnectState.GROUP_NOT_EXIST.msg
                    )
                )
                return
            }

            //获取provisioned节点
            var node = MeshHelper.getProvisionedNodeByUUID(uuid)

            var index = 0
            var modelTotal = 0
            Thread(Runnable {
                node?.elements?.values?.forEach { eleValue ->
                    modelTotal += eleValue.meshModels?.size ?: 0
                    eleValue?.meshModels?.values?.forEach { model ->
                        if (model is VendorModel || model is GenericOnOffServerModel || isSubsAll) {
                            sleep(1000)
                            val modelIdentifier = model.getModelId()
                            val configModelSubscriptionAdd: MeshMessage
                            var elementAddress = eleValue.elementAddress
                            if (group.addressLabel == null) {
                                configModelSubscriptionAdd =
                                    ConfigModelSubscriptionAdd(
                                        elementAddress,
                                        group.address,
                                        modelIdentifier
                                    )
                            } else {
                                configModelSubscriptionAdd =
                                    ConfigModelSubscriptionVirtualAddressAdd(
                                        elementAddress,
                                        group.getAddressLabel()!!,
                                        modelIdentifier
                                    )
                            }
                            MeshHelper.sendMessage(
                                "sendSubscribeMsg",
                                node.unicastAddress,
                                configModelSubscriptionAdd,
                                null
                            )
                        }
                        index++
                    }
                }

                if (index == modelTotal) {
                    doMapCallback(
                        map,
                        callback,
                        CallbackMsg(
                            ConnectState.COMMON_SUCCESS.code,
                            ConnectState.COMMON_SUCCESS.msg
                        )
                    )
                }
            }).start()

        }
    }

    /**
     * 协议v2.0：设置灯的hsvbt属性
     */
    fun modifyLightStatus(
        uuid: String,
        params: HashMap<String, Any>,
        callback: BooleanCallback,
        isAcknowledged: Boolean = true
    ) {
        var hsv = params["HSVColor"]
        var bright = params["Brightness"]
        var temperature = params["ColorTemperature"]
        var mode = params["LightMode"]
        var onOff = params["LightSwitch"]
        Utils.printLog(
            TAG,
            "Brightness:$bright,ColorTemperature:$temperature,mode:$mode,onOff:$onOff"
        )

        if (hsv != null) {// todo mxchip 待调试
            var vendorMap = hsv as HashMap<String, Any>
            var h = vendorMap["Hue"]
            var s = vendorMap["Saturation"]
            var v = vendorMap["Value"]

            var paramType = Utils.getNumberType(h)
            if (paramType < 0 || h == null || s == null || v == null) {
                callback.onResult(false)
                return
            }

            sendMeshMessage(
                uuid,
                0,
                VENDOR_MSG_ATTR_SET,
                "$ATTR_TYPE_LIGHT_HSV${ByteUtil.bytesToHexString(
                    byteArrayOf((if (paramType == 1) (v as Int).toByte() else if (paramType == 2) (v as Double).toByte() else (v as Float).toByte()))
                )}${ByteUtil.bytesToHexString(
                    ByteUtil.shortToByte(
                        if (paramType == 1) (h as Int).toShort() else if (paramType == 2) (h as Double).toShort() else (h as Float).toShort()
                    )
                )}${ByteUtil.bytesToHexString(
                    byteArrayOf((if (paramType == 1) (s as Int).toByte() else if (paramType == 2) (s as Double).toByte() else (s as Float).toByte()))
                )}",
                callback
            )
        } else if (bright != null && Utils.getNumberType(bright) > 0) {
            var briType = Utils.getNumberType(bright)

            sendMeshMessage(
                uuid,
                0,
                VENDOR_MSG_ATTR_SET,
                "$ATTR_TYPE_LIGHT_BRIGHTNESS${ByteUtil.bytesToHexString(
                    ByteUtil.shortToByte(if (briType == 1) (bright as Int).toShort() else if (briType == 2) (bright as Double).toShort() else (bright as Float).toShort())
                )}",
                callback
            )
        } else if (temperature != null && Utils.getNumberType(temperature) > 0) {
            var temType = Utils.getNumberType(temperature)
            sendMeshMessage(
                uuid,
                0,
                VENDOR_MSG_ATTR_SET,
                "$ATTR_TYPE_LIGHT_TEMPRETURE${ByteUtil.bytesToHexString(
                    ByteUtil.shortToByte((if (temType == 1) (temperature as Int).toShort() else if (temType == 2) (temperature as Double).toShort() else (temperature as Float).toShort()))
                )}",
                callback
            )
        } else if (mode != null) {//TODO mxchip 白/彩灯模式未调试
            sendMeshMessage(
                uuid,
                0,
                "11",
                "${ByteUtil.bytesToHexString(
                    byteArrayOf(("$mode".toDouble().toInt()).toByte())
                )}",
                callback
            )
        } else if (onOff != null && Utils.getNumberType(onOff) >= 0) {
            var onOffType = Utils.getNumberType(onOff)
            var onOffParam =
                if (onOffType == 1) onOff as Int else if (onOffType == 2) (onOff as Double).toInt() else (onOff as Float).toInt()
            setGenericOnOff(uuid, onOffParam == 0, 0, callback)
        } else {
            callback.onResult(false)
        }
    }

    /**
     * 获取设备当前状态:亮度、色温、开关、颜色
     */
    fun fetchLightCurrentStatus(uuid: String, callback: MapCallback) {
        Utils.printLog(TAG, "fetchLightCurrentStatus")
        sendMeshMessage(
            uuid,
            0,
            VENDOR_MSG_ATTR_GET,
            "$ATTR_TYPE_LIGHT_BRIGHTNESS$ATTR_TYPE_LIGHT_TEMPRETURE$ATTR_TYPE_LIGHT_ON_OFF$ATTR_TYPE_LIGHT_HSV",
            callback
        )
    }

    fun unSubscribeLightStatus() {
        mConnectCallbacks.remove("subscribeStatus")
        MeshHelper.unSubscribeLightStatus()
    }

//    fun parseLightStatus(
//        params: ByteArray,
//        callback: MapCallback,
//        map: HashMap<String, Any>
//    ) {
//        var modeByte = params[0]
//        var modeBits = ByteUtil.byteTobitArray(modeByte)
//        var modeBitString = ByteUtil.byteTobitString(modeByte)
//        Utils.printLog(
//            TAG,
//            "mode Int:${modeByte.toInt()},modeBitString:$modeBitString,statuHex:${ByteUtil.bytesToHexString(
//                params
//            )}"
//        )
//        var mode = ByteUtil.byteToShort(byteArrayOf(modeBits[6], modeBits[5])).toInt()
//        var isOn = modeBits[7].toInt()
//
//        var h = ByteUtil.byteToShort(
//            byteArrayOf(
//                params[2],
//                params[1]
//            )
//        )
//        var s = params[3].toInt()
//        var v = params[4].toInt()
//        var b = params[5].toInt()
//        var t = ByteUtil.byteArrayToInt(
//            byteArrayOf(
//                0x00,
//                0x00,
//                params[6],
//                params[7]
//            )
//        )
//        Utils.printLog(TAG, "h:$h,s:$s,v:$v,b:$b,t:$t")
//        map["code"] = 200
//        var lightStatus = HashMap<String, Any>()
//
//        var switchMap = HashMap<String, Int>()
//        switchMap["0"] = isOn
//
//        lightStatus["LightMode"] = mode
//        lightStatus["Brightness"] = b
//        lightStatus["ColorTemperature"] = t
//        lightStatus["OnOffSwitch"] = switchMap
//
//        var HSVColor = HashMap<String, Int>()
//        HSVColor["Hue"] = h.toInt()
//        HSVColor["Saturation"] = s
//        HSVColor["Value"] = v
//        lightStatus["HSVColor"] = HSVColor
//        map["data"] = lightStatus
//
//
//        if (callback is MapCallback) {
//            doMapCallback(
//                map,
//                callback,
//                CallbackMsg(
//                    ConnectState.COMMON_SUCCESS.code,
//                    ConnectState.COMMON_SUCCESS.msg
//                )
//            )
//        }
//    }

    private fun doMapCallback(
        map: HashMap<String, Any>,
        callback: MapCallback,
        msg: CallbackMsg
    ) {
        map.put("code", msg.code)
        map.put("message", msg.msg)
        callback.onResult(map)
    }

    private fun doProxyCheck(
        uuid: String,
        map: HashMap<String, Any>,
        callback: MapCallback
    ): Boolean {
        doBaseCheck(uuid, map, callback)
        var node = MeshHelper.getProvisionedNodeByUUID(uuid)

        if (!MeshHelper.isConnectedToProxy()) {
            doMapCallback(
                map, callback,
                CallbackMsg(CommonErrorMsg.DISCONNECTED.code, CommonErrorMsg.DISCONNECTED.msg)
            )
            return false
        }

        if (node == null) {
            doMapCallback(
                map, callback,
                CallbackMsg(ConnectState.NODE_NOT_EXIST.code, ConnectState.NODE_NOT_EXIST.msg)
            )

            return false
        }

        return true
    }

    fun setCurrentNode(uuid: String) {
        MeshHelper.setSelectedMeshNode(MeshHelper.getProvisionedNodeByUUID(uuid))
    }

    fun getCurrentNode(callback: MapCallback) {
        MeshHelper.MeshProxyService.mMeshProxyService?.mNrfMeshManager?.mExtendedMeshNode?.let { node ->
            var map = HashMap<String, Any>()
            map.put("uuid", node.uuid)
            var elementsMap = HashMap<String, Any>()

            node.elements.forEach { key, element ->
                var eleMap = HashMap<String, Any>()
                var modelsMap = HashMap<String, Any>()
//                eleMap.put(eleMap.)
//                elementsMap.put(key, eleMap)
            }
            map.put("element", elementsMap)
            callback.onResult(map)
        }
    }

    /**
     * 获取节点的mesh address即第一个element的element address
     */
    fun getMeshAddress(uuid: String, callback: MapCallback) {
        var map = HashMap<String, Any>()
        var node = MeshHelper.getProvisionedNodeByUUID(uuid)
        if (node == null) {
            doMapCallback(
                map,
                callback,
                CallbackMsg(ConnectState.NODE_NOT_EXIST.code, ConnectState.NODE_NOT_EXIST.msg)
            )
            return
        }

        if (node?.elements?.values?.size ?: 0 > 0) {
            map["address"] = "${node?.elements?.values?.elementAt(0)?.elementAddress}"
            callback.onResult(map)
        }
    }

    /**
     * 获取设备版本号
     */
//    fun getDeviceVersion(uuid: String, callback: MapCallback) {
//        sendMeshMessage(uuid, 0, "0A", "", callback, "getDeviceVersion")
//    }

    fun sendGroupMsg(groupAddr: Int, vid: Int) {
        MeshHelper.getGroupByAddress(groupAddr)?.let { group ->
            MeshHelper.getMeshNetwork()?.let { network ->
                network.getModels(group)?.forEach foreach@{ model ->
                    if (MeshParserUtils.isVendorModel(model.modelId)) {
                        model.boundAppKeyIndexes?.let { boundedAppkeys ->
                            if (boundedAppkeys.size == 1) {
                                val appKey: ApplicationKey =
                                    network.getAppKey(boundedAppkeys[0])
                                val message = VendorModelMessageUnacked(
                                    appKey,
                                    model.modelId,
                                    (model as VendorModel).companyIdentifier,
                                    0x18,
                                    byteArrayOf(vid.toByte())
                                )
                                MeshHelper.sendMessage("", group.address, message, null)
                                return@foreach
                            }
                        }
                    }
                }
            }
        }
    }

}