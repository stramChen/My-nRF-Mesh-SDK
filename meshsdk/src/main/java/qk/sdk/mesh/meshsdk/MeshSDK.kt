package qk.sdk.mesh.meshsdk

import android.content.Context
import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.weyye.hipermission.PermissionCallback
import no.nordicsemi.android.meshprovisioner.UnprovisionedBeacon
import no.nordicsemi.android.meshprovisioner.models.VendorModel
import no.nordicsemi.android.meshprovisioner.transport.*
import no.nordicsemi.android.meshprovisioner.utils.MeshParserUtils
import qk.sdk.mesh.meshsdk.bean.CallbackMsg
import qk.sdk.mesh.meshsdk.bean.CommonErrorMsg
import qk.sdk.mesh.meshsdk.bean.ExtendedBluetoothDevice
import qk.sdk.mesh.meshsdk.callback.*
import qk.sdk.mesh.meshsdk.mesh.BleMeshManager
import qk.sdk.mesh.meshsdk.util.*
import java.lang.StringBuilder
import kotlin.collections.HashMap
import qk.sdk.mesh.meshsdk.util.Constants.ConnectState
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import java.lang.Exception

object MeshSDK {
    private val TAG = "MeshSDK"
    private var mContext: Context? = null
    private var mExtendedBluetoothDeviceMap = HashMap<String, ExtendedBluetoothDevice>()

    const val VENDOR_MODELID = 6094849
    const val ON_OFF_MODELID = 4096
//    const val ELEMENT_ADDRESS = 2

    const val LEAST_MODEL_COUNT = 4

    // 初始化 mesh
    fun init(context: Context) {
        mContext = context
        if (mContext != null) {
            MeshHelper.initMesh(mContext!!)
//            DfuHelper.getInstance(context)
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
    fun startScan(type: String, scanResultCallback: ArrayMapCallback, errCallback: IntCallback) {
        if (MeshHelper.isConnectedToProxy())
            disConnect()
        var scanCallback: ScanCallback = object :
            ScanCallback {
            override fun onScanResult(devices: List<ExtendedBluetoothDevice>, updatedIndex: Int?) {
                var resultArray = ArrayList<HashMap<String, Any>>()
                devices.forEach {
                    if (it.beacon != null) {
                        Utils.printLog(TAG, "scan result:${it.getAddress()}")
                        val unprovisionedBeacon = UnprovisionedBeacon(it.beacon!!.getBeaconData())
                        var map = HashMap<String, Any>()
                        map.put("mac", it.getAddress())
                        map.put("uuid", unprovisionedBeacon.uuid.toString())
                        map.put("rssi", it.rssi ?: 0)
                        map.put("name", it.name ?: "")
                        map.put("productId", ByteUtil.getPIdFromUUID(it.beacon!!.getBeaconData()))
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
        MeshHelper.startScan(
            if (type == Constants.SCAN_UNPROVISIONED) BleMeshManager.MESH_PROVISIONING_UUID else BleMeshManager.MESH_PROXY_UUID,
            scanCallback
        )
    }

    fun stopScan() {
        MeshHelper.stopScan()
    }

    fun provision(uuid: String, networkKey: String, callback: MapCallback) {
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

        mContext?.let { _ ->
            mExtendedBluetoothDeviceMap.get(uuid)?.let { extendedBluetoothDevice ->
                MeshHelper.connect(
                    extendedBluetoothDevice,
                    false,
                    object : ConnectCallback {
                        override fun onConnect() {
                            MeshHelper.startProvision(mExtendedBluetoothDeviceMap.get(uuid)!!,
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
                            //todo 日志管理
                            Log.e(TAG, "onConnectStateChange:${msg.msg}")
                            if (msg.code == ConnectState.PROVISION_SUCCESS.code) {
                                map.clear()
                                doMapCallback(
                                    map,
                                    callback,
                                    CallbackMsg(
                                        ConnectState.PROVISION_SUCCESS.code,
                                        ConnectState.PROVISION_SUCCESS.msg
                                    )
                                )
                                MeshHelper.unRegisterConnectListener()
                            } else {
                                doMapCallback(map, callback, msg)
                            }
                        }

                        override fun onError(msg: CallbackMsg) {
                            map.clear()
                            map.put(Constants.KEY_MESSAGE, msg.msg)
                            map.put(Constants.KEY_CODE, msg.code)
                            callback.onResult(map)
                        }
                    })
            }
        }
    }

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

    fun bindApplicationKeyForNode(uuid: String, appKey: String, callback: MapCallback) {
        Observable.create<String> {}.subscribeOn(AndroidSchedulers.mainThread()).doOnSubscribe {
            var map = HashMap<String, Any>()
            doBaseCheck(uuid, map, callback)
            MeshHelper.getAppkeyByKeyName(appKey)?.let { applicationKey ->
                MeshHelper.getProvisionNode()?.forEach { node ->
                    if (node.uuid == uuid) {
                        MeshHelper.setSelectedMeshNode(node)
                    }
                }
                if (MeshHelper.isConnectedToProxy()) {
                    var bindedIndex = -1
                    var currentModel: MeshModel? = null
                    MeshHelper.addAppkeys(applicationKey.keyIndex, object :
                        MeshCallback {
                        override fun onReceive(msg: MeshMessage) {
                            if (msg is ConfigAppKeyStatus) {
                                if (msg.isSuccessful) {//添加appkey成功
                                    Utils.printLog(TAG, "add app key success!")
                                    if ((MeshHelper.getSelectedMeshNode()?.elements?.size
                                            ?: 0) <= 0
                                    ) {
                                        MeshHelper.getCompositionData()
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
                            } else if (msg is ConfigModelAppStatus) {
                                synchronized(bindedIndex) {
                                    if (bindedIndex > 0 && msg.modelIdentifier == currentModel?.modelId) {
                                        if (msg.isSuccessful) {//bind appkey成功
                                            Utils.printLog(TAG, "bindAppKey success!")
                                        } else {
                                            Utils.printLog(
                                                TAG,
                                                "bindAppKey failed:${msg.statusCodeName}"
                                            )
                                            map.clear()
                                            map.put(
                                                Constants.KEY_MESSAGE,
                                                ConnectState.BIND_APP_KEY_FOR_NODE_FAILED.msg
                                            )
                                            map.put(
                                                Constants.KEY_CODE,
                                                ConnectState.BIND_APP_KEY_FOR_NODE_FAILED.code
                                            )
                                        }
                                        var models = MeshHelper.getSelectedElement()?.meshModels
                                        if (models?.size ?: 0 >= LEAST_MODEL_COUNT) {
                                            for (model in models!!.values) {
                                                if (model.modelId == currentModel?.modelId) {
                                                    if (model.boundAppKeyIndexes.size > 0) {//当前model绑定成功
                                                        Utils.printLog(
                                                            TAG,
                                                            "当前model绑定成功:${model.modelId}"
                                                        )
                                                        bindedIndex++
                                                        if (bindedIndex < models.size) {
                                                            MeshHelper.setSelectedModel(
                                                                MeshHelper.getSelectedElement(),
                                                                MeshHelper.getSelectedElement()?.meshModels?.values?.elementAt(
                                                                    bindedIndex
                                                                )
                                                            )
                                                            currentModel =
                                                                MeshHelper.getSelectedElement()
                                                                    ?.meshModels?.values?.elementAt(
                                                                    bindedIndex
                                                                )
                                                            MeshHelper.bindAppKey(
                                                                applicationKey.keyIndex, this
                                                            )
                                                        } else {//绑定全部model成功
                                                            doMapCallback(
                                                                map, callback,
                                                                CallbackMsg(
                                                                    ConnectState.COMMON_SUCCESS.code,
                                                                    ConnectState.COMMON_SUCCESS.msg
                                                                )
                                                            )
                                                        }
                                                        return
                                                    } else {//当前model绑定失败
                                                        Utils.printLog(
                                                            TAG,
                                                            "当前model绑定失败:${model.modelId}"
                                                        )
                                                        bindedIndex = -1
                                                        doMapCallback(
                                                            map, callback,
                                                            CallbackMsg(
                                                                ConnectState.BIND_APP_KEY_FOR_NODE_FAILED.code,
                                                                ConnectState.BIND_APP_KEY_FOR_NODE_FAILED.msg
                                                            )
                                                        )
                                                        return
                                                    }
                                                }
                                            }

                                            bindedIndex = -1
                                            doMapCallback(
                                                map, callback,
                                                CallbackMsg(
                                                    ConnectState.BIND_APP_KEY_FOR_NODE_FAILED.code,
                                                    ConnectState.BIND_APP_KEY_FOR_NODE_FAILED.msg
                                                )
                                            )
                                            Utils.printLog(
                                                TAG,
                                                "node绑定失败:${currentModel?.modelId}"
                                            )
                                        }
                                    }
                                }

                            } else if (msg is ConfigCompositionDataStatus) {
                                Utils.printLog(TAG, "get getCompositionData success!")
                                synchronized(bindedIndex) {
                                    if (bindedIndex == -1) {
                                        MeshHelper.getSelectedMeshNode()?.let { node ->
                                            node.elements?.values?.elementAt(0)?.let { eleValue ->
                                                if (eleValue.meshModels?.size ?: 0 >= LEAST_MODEL_COUNT && eleValue.meshModels?.values?.elementAt(
                                                        1
                                                    ) != null
                                                ) {//默认跳过第一个model，从第二个开始bind过程
                                                    bindedIndex = 1
                                                    Utils.printLog(
                                                        TAG,
                                                        "get getCompositionData bindAppKey!"
                                                    )
                                                    MeshHelper.setSelectedModel(
                                                        eleValue,
                                                        eleValue.meshModels?.values?.elementAt(1)
                                                    )
                                                    currentModel =
                                                        eleValue.meshModels?.values?.elementAt(1)
                                                    MeshHelper.bindAppKey(
                                                        applicationKey.keyIndex, this
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                            }
                        }

                        override fun onError(msg: CallbackMsg) {

                        }
                    })
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
        }.subscribeOn(AndroidSchedulers.mainThread()).subscribe()
    }

    fun disConnect() {
        MeshHelper.disConnect()
    }

    private fun doBaseCheck(uuid: String?, map: HashMap<String, Any>, callback: MapCallback) {
        if (mContext == null) {//判断sdk是否被初始化
            map.put(Constants.KEY_MESSAGE, Constants.SDK_NOT_INIT_MSG)
            map.put(Constants.KEY_CODE, Constants.SDK_NOT_INIT_CODE)
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

    fun setGenericOnOff(uuid: String, onOff: Boolean, callback: BooleanCallback) {
        if (!MeshHelper.isConnectedToProxy()) {
            callback.onResult(false)
            return
        }

        try {
            if (MeshHelper.getSelectedMeshNode()?.uuid != uuid) {
                MeshHelper.getProvisionNode()?.forEach { node ->
                    if (node.uuid == uuid) {
                        MeshHelper.setSelectedMeshNode(node)
                    }
                }
            }

            MeshHelper.setSelectedModel(
                MeshHelper.getSelectedMeshNode()?.elements?.values?.elementAt(0),
                MeshHelper.getSelectedElement()?.meshModels?.get(ON_OFF_MODELID)
            )
            MeshHelper.sendGenericOnOff(onOff, 0)
            callback.onResult(true)
        } catch (e: Exception) {
            e.printStackTrace()
            callback.onResult(false)
        }
    }

    fun setLightProperties(
        uuid: String, c: Int, w: Int, r: Int,
        g: Int, b: Int, callback: BooleanCallback
    ) {
        var params = StringBuilder(
            "${ByteUtil.rgbtoHex(c)}${ByteUtil.rgbtoHex(w)}${ByteUtil.rgbtoHex(r)}${ByteUtil.rgbtoHex(
                g
            )}${ByteUtil.rgbtoHex(b)}"
        )
        sendMeshMessage(uuid, 0, VENDOR_MODELID, "05", params.toString(), callback)
    }

    fun getLightProperties(uuid: String, callback: MapCallback) {
        sendMeshMessage(uuid, 0, VENDOR_MODELID, "04", "", callback)
    }

    fun sendMeshMessage(
        uuid: String,
        element: Int,
        modelId: Int,
        opcode: String,
        value: String,
        callback: Any
    ) {
        if (!MeshHelper.isConnectedToProxy()) {
            doVendorCallback(
                callback,
                false,
                CallbackMsg(CommonErrorMsg.DISCONNECTED.code, CommonErrorMsg.DISCONNECTED.msg)
            )
            return
        }

        if (MeshHelper.getSelectedMeshNode()?.uuid != uuid) {
            MeshHelper.getProvisionNode()?.forEach { node ->
                if (node.uuid == uuid) {
                    MeshHelper.setSelectedMeshNode(node)
                }
            }
        }

        MeshHelper.setSelectedModel(
            MeshHelper.getSelectedMeshNode()?.elements?.values?.elementAt(element),
            MeshHelper.getSelectedMeshNode()?.elements?.values?.elementAt(element)?.meshModels?.get(
                if (modelId == 0) VENDOR_MODELID else modelId
            )
        )

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
                            ByteUtil.hexStringToBytes(value)
                        )
                        Utils.printLog(
                            TAG,
                            "value hex:$value,value bytes:${ByteUtil.hexStringToBytes(value)}"
                        )
                        MeshHelper.sendMessage(
                            element.elementAddress,
                            message,
                            object : MeshCallback {
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
                                        "opcode:$opcode"
                                    )
                                    synchronized(msgIndex) {
                                        when (opcode) {
                                            "00" -> {//四元组
                                                Utils.printLog(
                                                    TAG,
                                                    "quadruple size:${msg.parameter.size} ,content：${String(
                                                        msg.parameter
                                                    )}"
                                                )
                                                var infos = ByteUtil.bytesToHexString(msg.parameter)
                                                    .split("00")
                                                if (msgIndex < 0 && msg.parameter.size >= 40) {
                                                    var map = HashMap<String, Any>()
                                                    map.put(
                                                        "pk",
                                                        String(ByteUtil.hexStringToBytes(infos[0]))
                                                    )
                                                    map.put(
                                                        "ps",
                                                        String(ByteUtil.hexStringToBytes(infos[1]))
                                                    )
                                                    map.put(
                                                        "dn",
                                                        String(ByteUtil.hexStringToBytes(infos[2]))
                                                    )
                                                    map.put(
                                                        "ds",
                                                        String(ByteUtil.hexStringToBytes(infos[3]))
                                                    )
                                                    map.put(
                                                        "code",
                                                        ConnectState.COMMON_SUCCESS.code
                                                    )
                                                    if (callback is MapCallback) {
                                                        map.forEach { t, u ->
                                                            Log.e(TAG, "key:$t,value:$u")
                                                        }
                                                        callback.onResult(map)
                                                    }
                                                    MeshHelper.unRegisterMeshMsg()
                                                    msgIndex = 0
                                                }
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
                                                    msgIndex = 0
                                                }
                                            }
                                            "05" -> {//get cwrgb
                                                if (msgIndex < 0 && callback is BooleanCallback) {
                                                    callback.onResult(true)
                                                    msgIndex = 0
                                                }
                                            }
                                            "0C" -> {//获取灯当前状态，会返回所有属性
                                                if (msg.parameter.size >= 8 && msgIndex < 0) {
                                                    if (callback is MapCallback) {
                                                        parseLightStatus(
                                                            msg.parameter,
                                                            callback,
                                                            HashMap<String, Any>()
                                                        )
                                                        msgIndex = 0
                                                    }
                                                }
                                            }
                                            "0D", "0E", "0F", "11" -> {//set HSV
                                                if (msgIndex < 0 && callback is BooleanCallback) {
                                                    callback.onResult(true)
                                                    msgIndex = 0
                                                }
                                            }
//                                            "0E" -> {//set bright
//
//                                            }
//                                            "0F" -> {//set temperature
//
//                                            }
//                                            "11" -> {//set light mode
//
//                                            }
                                        }
                                    }

                                }

                                override fun onError(msg: CallbackMsg) {
                                    doVendorCallback(callback, false, msg)
                                }
                            })
                    }
                } else {
                    //todo
                    Utils.printLog(TAG, "model don't boundAppKey")
                }
            }
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

    fun getDeviceIdentityKeys(uuid: String, callback: MapCallback) {
        sendMeshMessage(uuid, 0, 0, "00", "", callback)
    }

    fun resetNode(uuid: String) {
        MeshHelper.getProvisionNode()?.forEach { node ->
            if (node.meshUuid == uuid) {
                MeshHelper.setSelectedMeshNode(node)
            }
        }

        val configNodeReset = ConfigNodeReset()
        MeshHelper.sendMessage(
            MeshHelper.getSelectedMeshNode()?.unicastAddress ?: 0,
            configNodeReset
        )
    }

    fun connect(networkKey: String, callback: MapCallback) {
        var map = HashMap<String, Any>()
        doBaseCheck(null, map, callback)
        if (!MeshHelper.isConnectedToProxy()) {
            MeshHelper.startScan(BleMeshManager.MESH_PROXY_UUID, object :
                ScanCallback {
                override fun onScanResult(
                    devices: List<ExtendedBluetoothDevice>,
                    updatedIndex: Int?
                ) {
                    if (devices.isNotEmpty()) {
                        MeshHelper.stopScan()
                        Utils.printLog(TAG, "onScanResult:${devices[0].getAddress()}")
                        MeshHelper.connect(devices[0], true, object :
                            ConnectCallback {
                            override fun onConnect() {
                                doMapCallback(
                                    map, callback,
                                    CallbackMsg(
                                        ConnectState.COMMON_SUCCESS.code,
                                        ConnectState.COMMON_SUCCESS.msg
                                    )
                                )
                            }

                            override fun onConnectStateChange(msg: CallbackMsg) {
                                if (msg.code == ConnectState.DISCONNECTED.code) {//连接断开，自动寻找代理节点重连
                                    disConnect()
                                    MeshHelper.unRegisterConnectListener()
                                    connect(networkKey.toUpperCase(), callback)
                                }
                            }

                            override fun onError(msg: CallbackMsg) {
                                doMapCallback(map, callback, msg)
                            }
                        })
                    }
                }

                override fun onError(msg: CallbackMsg) {
                    doMapCallback(map, callback, msg)
                }
            }, networkKey.toUpperCase())
        } else {
            doMapCallback(
                map, callback,
                CallbackMsg(ConnectState.COMMON_SUCCESS.code, ConnectState.COMMON_SUCCESS.msg)
            )
        }
    }

    fun exportMeshNetwork(callback: StringCallback) {
        MeshHelper.exportMeshNetwork(object : NetworkExportUtils.NetworkExportCallbacks {
            override fun onNetworkExported(json: String) {
                callback.onResultMsg(json)
            }

            override fun onNetworkExportFailed(error: String?) {

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

    fun createGroup(groupName: String, callback: BooleanCallback) {
        callback.onResult(MeshHelper.createGroup(groupName))
    }

    fun setPublication(uuid: String, groupName: String, callback: MapCallback) {
        var map = HashMap<String, Any>()
        if (doProxyCheck(uuid, map, callback)) {
            //通过groupName获取group
            var group = MeshHelper.getGroupByName(groupName)
            if (group == null) {
                doMapCallback(
                    map,
                    callback,
                    CallbackMsg(ConnectState.GROUP_NOT_EXIST.code, ConnectState.GROUP_NOT_EXIST.msg)
                )
                return
            }

            //获取provisioned节点
            var node = MeshHelper.getProvisionedNodeByUUID(uuid)
            if (node == null) {
                doMapCallback(
                    map,
                    callback,
                    CallbackMsg(ConnectState.NODE_NOT_EXIST.code, ConnectState.NODE_NOT_EXIST.msg)
                )
                return
            }

            var publishAddress = group.address
            var index = 0
            node.elements.values.elementAt(0).meshModels?.values?.forEach { meshModel ->
                if (meshModel.boundAppKeyIndexes?.size ?: 0 > 0) {
                    runBlocking {
                        launch {
                            delay(1000)
                            var meshMsg = ConfigModelPublicationSet(
                                node.elements.values.elementAt(0).elementAddress
                                ,
                                publishAddress,
                                meshModel.boundAppKeyIndexes!!.get(0),
                                false,
                                MeshParserUtils.USE_DEFAULT_TTL
                                ,
                                53,
                                0,
                                1,
                                1,
                                meshModel.modelId
                            )

                            try {
                                MeshHelper.MeshProxyService.mMeshProxyService?.mNrfMeshManager?.meshManagerApi
                                    ?.createMeshPdu(node.unicastAddress, meshMsg)
                                index++
                            } catch (ex: IllegalArgumentException) {
                                ex.printStackTrace()
                            }
                        }
                    }
                } else {
                    index++
                }
            }

            if (index == node.elements.values.elementAt(0).meshModels?.values?.size) {
                doMapCallback(
                    map,
                    callback,
                    CallbackMsg(ConnectState.COMMON_SUCCESS.code, ConnectState.COMMON_SUCCESS.msg)
                )
            }
        }

    }

    fun subscribe(uuid: String, groupName: String, callback: MapCallback) {
        var map = HashMap<String, Any>()
        if (doProxyCheck(uuid, map, callback)) {
            //通过uuid获取group
            var group = MeshHelper.getGroupByName(groupName)
            if (group == null) {
                doMapCallback(
                    map,
                    callback,
                    CallbackMsg(ConnectState.GROUP_NOT_EXIST.code, ConnectState.GROUP_NOT_EXIST.msg)
                )
                return
            }

            //获取provisioned节点
            var node = MeshHelper.getProvisionedNodeByUUID(uuid)

            var index = 0
            node?.elements?.values?.elementAt(0)?.meshModels?.values?.forEach { model ->
                runBlocking {
                    launch {
                        delay(1000)
                        val modelIdentifier = model.getModelId()
                        val configModelSubscriptionAdd: MeshMessage
                        var elementAddress = node.elements.values.elementAt(0).elementAddress
                        if (group.addressLabel == null) {
                            configModelSubscriptionAdd =
                                ConfigModelSubscriptionAdd(
                                    elementAddress,
                                    group.getAddress(),
                                    modelIdentifier
                                )
                        } else {
                            configModelSubscriptionAdd = ConfigModelSubscriptionVirtualAddressAdd(
                                elementAddress,
                                group.getAddressLabel()!!,
                                modelIdentifier
                            )
                        }
                        MeshHelper.sendMessage(node.unicastAddress, configModelSubscriptionAdd)
                        index++
                    }
                }
            }

            if (index == node?.elements?.values?.elementAt(0)?.meshModels?.values?.size) {
                doMapCallback(
                    map,
                    callback,
                    CallbackMsg(ConnectState.COMMON_SUCCESS.code, ConnectState.COMMON_SUCCESS.msg)
                )
            }
        }
    }

    fun modifyLightStatus(uuid: String, params: HashMap<String, Any>, callback: BooleanCallback) {
        var hsv = params["HSVColor"]
        var bright = params["Brightness"]
        var temperature = params["Temperature"]
        var mode = params["LightMode"]
        var onOff = params["LightSwitch"]
        Utils.printLog(TAG, "Brightness:$bright,Temperature:$temperature,mode:$mode,onOff:$onOff")

        if (hsv != null) {
            var vendorMap = hsv as HashMap<String, Any>
            var h = vendorMap["Hue"]
            var s = vendorMap["Saturation"]
            var v = vendorMap["Value"]

            var paramType = getNumberType(h)
            if (paramType < 0 || h == null || s == null || v == null)
                callback.onResult(false)

            sendMeshMessage(
                uuid,
                0,
                0,
                "0D",
                "${ByteUtil.bytesToHexString(
                    ByteUtil.shortToByte(
                        if (paramType == 1) (h as Int).toShort() else if (paramType == 2) (h as Double).toShort() else (h as Float).toShort()
                    )
                )}${ByteUtil.bytesToHexString(
                    byteArrayOf((if (paramType == 1) (s as Int).toByte() else if (paramType == 2) (s as Double).toByte() else (s as Float).toByte()))
                )}${ByteUtil.bytesToHexString(
                    byteArrayOf((if (paramType == 1) (v as Int).toByte() else if (paramType == 2) (v as Double).toByte() else (v as Float).toByte()))
                )}",
                callback
            )
        } else if (bright != null && getNumberType(bright) > 0) {
            var briType = getNumberType(bright)

            sendMeshMessage(
                uuid,
                0,
                0,
                "0E",
                "${ByteUtil.bytesToHexString(
                    byteArrayOf((if (briType == 1) (bright as Int).toByte() else if (briType == 2) (bright as Double).toByte() else (bright as Float).toByte()))
                )}",
                callback
            )
        } else if (temperature != null && getNumberType(temperature) > 0) {
            var temType = getNumberType(temperature)
            sendMeshMessage(
                uuid,
                0,
                0,
                "0F",
                "${ByteUtil.bytesToHexString(
                    ByteUtil.shortToByte((if (temType == 1) (temperature as Int).toShort() else if (temType == 2) (temperature as Double).toShort() else (temperature as Float).toShort()))
                )}",
                callback
            )
        } else if (mode != null) {
            sendMeshMessage(
                uuid,
                0,
                0,
                "11",
                "${ByteUtil.bytesToHexString(
                    byteArrayOf(("$mode".toDouble().toInt()).toByte())
                )}",
                callback
            )
        } else if (onOff != null && getNumberType(onOff) >= 0) {
            var onOffType = getNumberType(onOff)
            var onOffParam =
                if (onOffType == 1) onOff as Int else if (onOffType == 2) (onOff as Double).toInt() else (onOff as Float).toInt()
            setGenericOnOff(uuid, if (onOffParam == 0) false else true, callback)
        }
    }

    private fun getNumberType(number: Any?): Int {
        if (number == null)
            return -2
        return if (number is Int) 1 else if (number is Double) 2 else if (number is Float) 3 else -1
    }

    fun fetchLightCurrentStatus(uuid: String, callback: MapCallback) {
        sendMeshMessage(uuid, 0, 0, "0C", "", callback)
    }

    fun subscribeLightStatus(uuid: String, callback: MapCallback) {
        MeshHelper.subscribeLightStatus(object : MeshCallback {
            override fun onReceive(msg: MeshMessage) {
                var node = MeshHelper?.getMeshNetwork()?.getNode(msg.src)
                if (uuid.isNotEmpty() && node?.uuid?.toUpperCase() != uuid.toUpperCase()) {
                    return
                }

                var map = HashMap<String, Any>()
                map["uuid"] = node?.uuid ?: ""
                if (msg is GenericOnOffStatus) {
                    var param = msg.parameter
                    if (param.size == 1) {
                        map["isOn"] = if (param[0].toInt() == 0) false else true
                        Utils.printLog(
                            TAG,
                            "onreceive node:${node?.uuid?.toUpperCase()}, isOn:${map["isOn"]}"
                        )
                        callback.onResult(map)
                    }
                } else if (msg is VendorModelMessageStatus) {
                    if (msg.parameter.size >= 8) {
                        parseLightStatus(msg.parameter, callback, map)
                        Utils.printLog(
                            TAG,
                            "onreceive node:${node?.uuid?.toUpperCase()}, isOn:${map["isOn"]}"
                        )
                    }
                }
            }

            override fun onError(msg: CallbackMsg) {

            }
        })
    }

    fun unSubscribeLightStatus() {
        MeshHelper.unSubscribeLightStatus()
    }

    private fun parseLightStatus(
        params: ByteArray,
        callback: MapCallback,
        map: HashMap<String, Any>
    ) {
        var modeByte = params[0]
        var modeBits = ByteUtil.byteTobitArray(modeByte)
        var modeBitString = ByteUtil.byteTobitString(modeByte)
        Utils.printLog(
            TAG,
            "mode Int:${modeByte.toInt()},modeBitString:$modeBitString,statuHex:${ByteUtil.bytesToHexString(
                params
            )}"
        )
        var mode = ByteUtil.byteToShort(byteArrayOf(modeBits[6], modeBits[5])).toInt()
        var isOn = modeBits[7].toShort()

        var h = ByteUtil.byteToShort(
            byteArrayOf(
                params[2],
                params[1]
            )
        )
        var s = params[3].toInt()
        var v = params[4].toInt()
        var b = params[5].toInt()
        var t = ByteUtil.byteArrayToInt(
            byteArrayOf(
                0x00,
                0x00,
                params[6],
                params[7]
            )
        )
        Utils.printLog(TAG, "h:$h,s:$s,v:$v,b:$b,t:$t")
        map["code"] = 200
        var lightStatus = HashMap<String, Any>()
        lightStatus["LightMode"] = mode
        lightStatus["Brightness"] = b
        lightStatus["Temperature"] = t
        lightStatus["LightSwitch"] = isOn

        var HSVColor = HashMap<String, Int>()
        HSVColor["Hue"] = h.toInt()
        HSVColor["Saturation"] = s
        HSVColor["Value"] = v
        lightStatus["HSVColor"] = HSVColor
        map["data"] = lightStatus


        if (callback is MapCallback) {
            doMapCallback(
                map,
                callback,
                CallbackMsg(
                    ConnectState.COMMON_SUCCESS.code,
                    ConnectState.COMMON_SUCCESS.msg
                )
            )
        }
    }

//    fun updateDeviceImg(uuid: String, path: String, callback: MapCallback) {
//        var map = HashMap<String, Any>()
//        doBaseCheck(uuid, map, callback)
//        MeshHelper.updateDeviceImg(mContext!!,uuid,path,object :DfuAdapter.DfuHelperCallback(){
//            override fun onStateChanged(p0: Int) {
//                super.onStateChanged(p0)
//            }
//
//            override fun onProcessStateChanged(p0: Int, p1: Throughput?) {
//                super.onProcessStateChanged(p0, p1)
//            }
//
//            override fun onProgressChanged(p0: DfuProgressInfo?) {
//                super.onProgressChanged(p0)
//            }
//
//            override fun onError(p0: Int, p1: Int) {
//                super.onError(p0, p1)
//            }
//        })
//    }

    private fun doMapCallback(map: HashMap<String, Any>, callback: MapCallback, msg: CallbackMsg) {
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
}