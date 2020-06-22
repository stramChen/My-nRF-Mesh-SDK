package qk.sdk.mesh.meshsdk

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonNull
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.weyye.hipermission.PermissionCallback
import no.nordicsemi.android.meshprovisioner.UnprovisionedBeacon
import no.nordicsemi.android.meshprovisioner.models.GenericOnOffServerModel
import no.nordicsemi.android.meshprovisioner.models.VendorModel
import no.nordicsemi.android.meshprovisioner.transport.*
import no.nordicsemi.android.meshprovisioner.utils.MeshParserUtils
import qk.sdk.mesh.meshsdk.bean.CallbackMsg
import qk.sdk.mesh.meshsdk.bean.CommonErrorMsg
import qk.sdk.mesh.meshsdk.bean.ExtendedBluetoothDevice
import qk.sdk.mesh.meshsdk.bean.auto.AutoLogic
import qk.sdk.mesh.meshsdk.callback.*
import qk.sdk.mesh.meshsdk.mesh.BleMeshManager
import qk.sdk.mesh.meshsdk.mesh.NrfMeshManager
import qk.sdk.mesh.meshsdk.util.*
import java.lang.StringBuilder
import kotlin.collections.HashMap
import qk.sdk.mesh.meshsdk.util.Constants.ConnectState
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import java.lang.Exception
import java.lang.Thread.sleep
import java.util.concurrent.atomic.AtomicBoolean

object MeshSDK {
    private val TAG = "MeshSDK"
    private var mContext: Context? = null
    private var mExtendedBluetoothDeviceMap = HashMap<String, ExtendedBluetoothDevice>()

    const val VENDOR_MODELID = 6094849
    const val ON_OFF_MODELID = 4096
//    const val ELEMENT_ADDRESS = 2

    const val LEAST_MODEL_COUNT = 1

    var mMeshCallbacks = ArrayList<ConnectCallback?>(5)

    var mConnectCallbacks = HashMap<String, Any>(20)

    private const val PUBLISH_INTERVAL = 88
    private const val PUBLISH_TTL = 5

    // 初始化 mesh
    fun init(context: Context) {
        mContext = context
        Utils.mContext = context
        mContext?.apply {
            MeshHelper.initMesh(this)
        }
        LogFileUtil.deleteLog(mContext)
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
        Utils.printLog(TAG, "startScan startScan")
        disConnect()
        var scanCallback: ScanCallback = object :
            ScanCallback {
            override fun onScanResult(devices: List<ExtendedBluetoothDevice>, updatedIndex: Int?) {
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
//                        map.put("productId", ByteUtil.getPIdFromUUID(it.beacon!!.getBeaconData()))
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
                        Log.e(
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

    private fun reConnect(callback: MapCallback) {
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
//        disConnect()
        MeshHelper.unRegisterConnectListener()
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

    const val ADD_APPKEYS = "addAppkeys"
    const val GET_COMPOSITION_DATA = "getCompositionData"
    const val BIND_APP_KEY = "bindAppKey"
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
                    var bindedModelIndex = -1
                    var bindedEleIndex = -1
                    var currentModel: MeshModel? = null
                    var currentElement: Element? = null
                    var currentNode: ProvisionedMeshNode? = null
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
                                        MeshHelper.getCompositionData(GET_COMPOSITION_DATA, this)
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
                                synchronized(bindedModelIndex) {
                                    if (bindedModelIndex >= 0 && msg.modelIdentifier == currentModel?.modelId && (MeshHelper.getSelectedElement()?.meshModels?.size
                                            ?: 0) > bindedModelIndex && msg.elementAddress == currentElement?.elementAddress
                                    ) {
                                        if (!msg.isSuccessful) {//bind appkey失败ßßß
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
                                            return@synchronized
                                        }
                                        var models = MeshHelper.getSelectedElement()?.meshModels
                                        for (model in models!!.values) {
                                            if (model.modelId == currentModel?.modelId) {
                                                if (model.boundAppKeyIndexes.size > 0) {//当前model绑定成功
                                                    bindedModelIndex++
                                                    Utils.printLog(
                                                        TAG,
                                                        "当前model绑定成功 eleAdd:${msg.elementAddress} ,modelId:${model.modelId},bindedIndex:$bindedModelIndex,bindedEleIndex:$bindedEleIndex"
                                                    )
                                                    if (bindedModelIndex < models.size) {
                                                        MeshHelper.setSelectedModel(
                                                            MeshHelper.getSelectedElement(),
                                                            MeshHelper.getSelectedElement()?.meshModels?.values?.elementAt(
                                                                bindedModelIndex
                                                            )
                                                        )
                                                        currentModel =
                                                            MeshHelper.getSelectedElement()
                                                                ?.meshModels?.values?.elementAt(
                                                                    bindedModelIndex
                                                                )
                                                        MeshHelper.bindAppKey(
                                                            BIND_APP_KEY,
                                                            applicationKey.keyIndex, this
                                                        )
                                                    } else {//绑定全部model成功,继续轮询绑定其他element的model
                                                        bindedEleIndex++
                                                        if (bindedEleIndex < currentNode?.elements?.values?.size ?: 0) {
                                                            currentNode?.elements?.values?.elementAt(
                                                                bindedEleIndex
                                                            )?.let { eleValue ->
                                                                //若有多个model，默认跳过第一个model，从第二个开始bind
                                                                bindedModelIndex =
                                                                    if (eleValue.meshModels?.size ?: 0 == 1) 0 else 1

                                                                currentModel =
                                                                    eleValue.meshModels?.values?.elementAt(
                                                                        bindedModelIndex
                                                                    )
                                                                MeshHelper.setSelectedModel(
                                                                    eleValue,
                                                                    currentModel
                                                                )
                                                                currentElement = eleValue

                                                                MeshHelper.bindAppKey(
                                                                    BIND_APP_KEY,
                                                                    applicationKey.keyIndex,
                                                                    this
                                                                )
//                                                                }
                                                            }
                                                        } else {
                                                            MeshHandler.removeRunnable(BIND_APP_KEY)
                                                            Utils.printLog(
                                                                TAG,
                                                                "finish bind success"
                                                            )
                                                            doMapCallback(
                                                                map, callback,
                                                                CallbackMsg(
                                                                    ConnectState.COMMON_SUCCESS.code,
                                                                    ConnectState.COMMON_SUCCESS.msg
                                                                )
                                                            )
                                                        }
                                                    }
                                                    return
                                                } else {//当前model绑定失败
                                                    Utils.printLog(
                                                        TAG,
                                                        "当前model绑定失败:${model.modelId}"
                                                    )
                                                    bindedModelIndex = -1
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

                                        bindedModelIndex = -1
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
//                                        }
                                    }
                                }

                            } else if (msg is ConfigCompositionDataStatus) {
                                Utils.printLog(TAG, "get getCompositionData success!")
                                MeshHandler.removeRunnable(GET_COMPOSITION_DATA)
                                synchronized(bindedModelIndex) {
                                    if (bindedModelIndex == -1) {
                                        MeshHelper.getSelectedMeshNode()?.let { node ->
                                            currentNode = node
                                            node.elements?.values?.elementAt(0)?.let { eleValue ->
                                                if (eleValue.meshModels?.size ?: 0 >= LEAST_MODEL_COUNT && eleValue.meshModels?.values?.elementAt(
                                                        if (eleValue.meshModels?.size ?: 0 == 1) 0 else 1
                                                    ) != null
                                                ) {//默认跳过第一个model，从第二个开始bind
                                                    bindedModelIndex = 1
                                                    bindedEleIndex = -1
                                                    Utils.printLog(
                                                        TAG,
                                                        "get getCompositionData bindAppKey!"
                                                    )
                                                    currentModel =
                                                        eleValue.meshModels?.values?.elementAt(
                                                            if (eleValue.meshModels?.size ?: 0 == 1) 0 else 1
                                                        )
                                                    MeshHelper.setSelectedModel(
                                                        eleValue,
                                                        currentModel
                                                    )
                                                    currentElement = eleValue

                                                    MeshHelper.bindAppKey(
                                                        BIND_APP_KEY,
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
                    }
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
        }.subscribeOn(AndroidSchedulers.mainThread()).subscribe()
    }

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
                MeshHelper.getSelectedElement()?.meshModels?.get(ON_OFF_MODELID)
            )
            MeshHelper.sendGenericOnOff(onOff, 0)

            mConnectCallbacks.remove("setGenericOnOff")
            callback.onResult(true)
        } catch (e: Exception) {
            e.printStackTrace()
            callback.onResult(false)
        }
    }

    fun setUnacknowledgedGenericOnOff(
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
                MeshHelper.getSelectedElement()?.meshModels?.get(ON_OFF_MODELID)
            )
            MeshHelper.sendUnacknowledgedGenericOnOff(onOff, 0)

            mConnectCallbacks.remove("setGenericOnOff")
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
        elementIndex: Int,
        modelIndex: Int,
        opcode: String,
        value: String,
        callback: Any,
        method: String = "",
        timeout: Boolean = false,
        retry: Boolean = false
    ) {
        if (!MeshHelper.isConnectedToProxy()) {
            doVendorCallback(
                callback,
                false,
                CallbackMsg(CommonErrorMsg.DISCONNECTED.code, CommonErrorMsg.DISCONNECTED.msg)
            )
            return
        }

        mConnectCallbacks[if (method.isEmpty()) "sendMeshMessage" else method] = callback

        if (MeshHelper.getSelectedMeshNode()?.uuid != uuid) {
            MeshHelper.getProvisionNode()?.forEach { node ->
                if (node.uuid == uuid) {
                    MeshHelper.setSelectedMeshNode(node)
                }
            }
        }

        var selElement = MeshHelper.getSelectedMeshNode()?.elements?.values?.elementAt(elementIndex)
        selElement?.meshModels?.forEach { key, model ->
            if (model is VendorModel) {
                MeshHelper.setSelectedModel(
                    selElement,
                    if (modelIndex == 0) model else MeshHelper.getSelectedMeshNode()?.elements?.values?.elementAt(
                        elementIndex
                    )?.meshModels?.get(modelIndex)
                )
            }
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
                            ByteUtil.hexStringToBytes(value)
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
                                    "send opcode:$opcode"
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

                                            if (msgIndex < 0 && msg.parameter.size >= 40) {

                                                var preIndex = 0
                                                var quadrupleIndex = 0
                                                var map = HashMap<String, Any>()
                                                for (index in 0 until msg.parameter.size) {
                                                    if (msg.parameter[index] == 0x00.toByte() || msg.parameter[index] == 0x20.toByte() || index == msg.parameter.size - 1) {
                                                        when (quadrupleIndex) {
                                                            0 -> {//pk
                                                                var pkBytes =
                                                                    ByteArray(index - preIndex)
                                                                System.arraycopy(
                                                                    msg.parameter,
                                                                    preIndex,
                                                                    pkBytes,
                                                                    0,
                                                                    pkBytes.size
                                                                )
                                                                map.put("pk", String(pkBytes))
                                                                quadrupleIndex++
                                                                preIndex = index + 1
                                                            }
                                                            1 -> {//ps
                                                                var psBytes =
                                                                    ByteArray(index - preIndex)
                                                                System.arraycopy(
                                                                    msg.parameter,
                                                                    preIndex,
                                                                    psBytes,
                                                                    0,
                                                                    psBytes.size
                                                                )
                                                                map.put("ps", String(psBytes))
                                                                quadrupleIndex++
                                                                preIndex = index + 1
                                                            }
                                                            2 -> {//dn
                                                                var dnBytes =
                                                                    ByteArray(index - preIndex)
                                                                System.arraycopy(
                                                                    msg.parameter,
                                                                    preIndex,
                                                                    dnBytes,
                                                                    0,
                                                                    dnBytes.size
                                                                )
                                                                map.put("dn", String(dnBytes))
                                                                quadrupleIndex++
                                                                preIndex = index + 1
                                                            }
                                                            3 -> {//ds
                                                                var dsBytes =
                                                                    ByteArray(index - preIndex)
                                                                System.arraycopy(
                                                                    msg.parameter,
                                                                    preIndex,
                                                                    dsBytes,
                                                                    0,
                                                                    dsBytes.size
                                                                )
                                                                map.put("ds", String(dsBytes))
                                                                quadrupleIndex++
                                                                preIndex = index + 1
                                                            }
                                                            4 -> {//product_id
                                                                var pidBytes =
                                                                    ByteArray(index - preIndex)
                                                                System.arraycopy(
                                                                    msg.parameter,
                                                                    preIndex,
                                                                    pidBytes,
                                                                    0,
                                                                    pidBytes.size
                                                                )
                                                                map.put("pid", String(pidBytes))
                                                                quadrupleIndex++
                                                                preIndex = index + 1
                                                            }
                                                        }
                                                    }
                                                }
                                                map.put(
                                                    "code",
                                                    ConnectState.COMMON_SUCCESS.code
                                                )
                                                if (callback is MapCallback) {
                                                    map.forEach { t, u ->
                                                        Log.e(TAG, "key:$t,value:$u")
                                                    }
                                                    callback.onResult(map)
                                                    MeshHandler.removeRunnable(method)
                                                }
                                                mConnectCallbacks.remove("sendMeshMessage")
                                                msgIndex = 0
                                            } else {
                                                //todo log
                                            }
                                        }
                                        "02" -> {//重启网关
                                            if (callback is BooleanCallback) {
                                                callback.onResult(true)
                                            }

                                            mConnectCallbacks.remove("sendMeshMessage")
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
                                                mConnectCallbacks.remove("sendMeshMessage")
                                                msgIndex = 0
                                            } else {
                                                //todo log
                                            }
                                        }
                                        "05" -> {//get cwrgb
                                            if (msgIndex < 0 && callback is BooleanCallback) {
                                                callback.onResult(true)
                                                mConnectCallbacks.remove("sendMeshMessage")
                                                msgIndex = 0
                                            } else {
                                                //todo log
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

                                                    mConnectCallbacks.remove("sendMeshMessage")
                                                    msgIndex = 0
                                                } else {
                                                    //todo log
                                                }
                                            } else {
                                                //todo log
                                            }
                                        }
                                        "0D", "0E", "0F", "11" -> {//set HSV
                                            if (msgIndex < 0 && callback is BooleanCallback) {
                                                callback.onResult(true)

                                                mConnectCallbacks.remove("sendMeshMessage")
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
        sendMeshMessage(uuid, 0, 0, "00", "", callback, "getDeviceIdentityKeys", true, true)
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

    fun connect(networkKey: String, callback: MapCallback) {
        if (MeshHelper.isConnectedToProxy() || !BluetoothAdapter.getDefaultAdapter().isEnabled) {
            return
        }

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
                        Utils.printLog(TAG, "connect onScanResult:${devices[0].getAddress()}")
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
                CallbackMsg(ConnectState.COMMON_SUCCESS.code, ConnectState.COMMON_SUCCESS.msg)
            )
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
            var modelTotalSize = 0
            var index = 0
            Thread(Runnable {
                node.elements.values.forEach { eleValue ->
                    modelTotalSize = modelTotalSize.plus(eleValue.meshModels?.values?.size ?: 0)
                    eleValue.meshModels?.values?.forEach { meshModel ->
                        if (meshModel.boundAppKeyIndexes?.size ?: 0 > 0 && (meshModel is GenericOnOffServerModel || meshModel is VendorModel)) {
                            sleep(500)
                            var meshMsg = ConfigModelPublicationSet(
                                eleValue.elementAddress
                                ,
                                publishAddress,
                                meshModel.boundAppKeyIndexes[0],
                                false,
                                PUBLISH_TTL,
                                PUBLISH_INTERVAL,
                                0,
                                0,
                                0,
                                meshModel.modelId
                            )

                            try {
                                MeshHelper.MeshProxyService.mMeshProxyService?.mNrfMeshManager?.meshManagerApi
                                    ?.createMeshPdu(node.unicastAddress, meshMsg)
                                index++
                            } catch (ex: IllegalArgumentException) {
                                ex.printStackTrace()
                            }
                        } else {
                            index++
                        }
                    }
                }

                if (index == modelTotalSize) {
                    doMapCallback(
                        map,
                        callback,
                        CallbackMsg(
                            ConnectState.COMMON_SUCCESS.code,
                            ConnectState.COMMON_SUCCESS.msg
                        )
                    )
                } else {
                    doMapCallback(
                        map,
                        callback,
                        CallbackMsg(
                            ConnectState.PUBLISH_FAILED.code,
                            ConnectState.PUBLISH_FAILED.msg
                        )
                    )
                }
            }).start()
        }

    }

    fun setPublication(uuid: String, groupName: String, groupAddr: Int, callback: MapCallback) {
        var map = HashMap<String, Any>()
        if (doProxyCheck(uuid, map, callback)) {
            if (MeshHelper.getGroupByAddress(groupAddr) == null) {
                MeshHelper.createGroup(groupName, groupAddr)
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

            var publishAddress = groupAddr
            var modelTotalSize = 0
            var index = 0
            node.elements.values.forEach { eleValue ->
                modelTotalSize = modelTotalSize.plus(eleValue.meshModels?.values?.size ?: 0)
                eleValue.meshModels?.values?.forEach { meshModel ->
                    if (meshModel.boundAppKeyIndexes?.size ?: 0 > 0 && (meshModel is GenericOnOffServerModel || meshModel is VendorModel)) {
                        runBlocking {
                            launch {
                                delay(500)
                                var meshMsg = ConfigModelPublicationSet(
                                    eleValue.elementAddress
                                    ,
                                    publishAddress,
                                    meshModel.boundAppKeyIndexes[0],
                                    false,
                                    PUBLISH_TTL,
                                    PUBLISH_INTERVAL,
                                    0,
                                    0,
                                    0,
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
            }

            if (index == modelTotalSize) {
                doMapCallback(
                    map,
                    callback,
                    CallbackMsg(ConnectState.COMMON_SUCCESS.code, ConnectState.COMMON_SUCCESS.msg)
                )
            } else {
                doMapCallback(
                    map,
                    callback,
                    CallbackMsg(ConnectState.PUBLISH_FAILED.code, ConnectState.PUBLISH_FAILED.msg)
                )
            }
        }

    }

    fun sendSubscribeMsg(uuid: String, groupAddr: Int, callback: MapCallback) {
        var map = HashMap<String, Any>()
        if (doProxyCheck(uuid, map, callback)) {

            if (MeshHelper.getGroupByAddress(groupAddr) == null) {
                doMapCallback(
                    map,
                    callback,
                    CallbackMsg(ConnectState.GROUP_NOT_EXIST.code, ConnectState.GROUP_NOT_EXIST.msg)
                )
            }

            //获取provisioned节点
            var node = MeshHelper.getProvisionedNodeByUUID(uuid)

            var index = 0
            var modelTotal = 0
            node?.elements?.values?.forEach { eleValue ->
                modelTotal += eleValue.meshModels?.size ?: 0
                eleValue?.meshModels?.values?.forEach { model ->
                    if (model is VendorModel || model is GenericOnOffServerModel) {
                        runBlocking {
                            launch {
                                delay(1000)
                                val modelIdentifier = model.getModelId()
                                val configModelSubscriptionAdd: MeshMessage
                                var elementAddress = eleValue.elementAddress
                                configModelSubscriptionAdd =
                                    ConfigModelSubscriptionAdd(
                                        elementAddress,
                                        groupAddr,
                                        modelIdentifier
                                    )
                                MeshHelper.sendMessage(
                                    "sendSubscribeMsg",
                                    node.unicastAddress,
                                    configModelSubscriptionAdd,
                                    null
                                )
                                index++
                            }
                        }
                    }
                }
            }

            if (index == modelTotal) {
                doMapCallback(
                    map,
                    callback,
                    CallbackMsg(ConnectState.COMMON_SUCCESS.code, ConnectState.COMMON_SUCCESS.msg)
                )
            }
        }
    }

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
                    CallbackMsg(ConnectState.GROUP_NOT_EXIST.code, ConnectState.GROUP_NOT_EXIST.msg)
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

        if (hsv != null) {
            var vendorMap = hsv as HashMap<String, Any>
            var h = vendorMap["Hue"]
            var s = vendorMap["Saturation"]
            var v = vendorMap["Value"]

            var paramType = getNumberType(h)
            if (paramType < 0 || h == null || s == null || v == null) {
                callback.onResult(false)
                return
            }

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
            if (isAcknowledged)
                setGenericOnOff(uuid, if (onOffParam == 0) false else true, 0, callback)
            else
                setUnacknowledgedGenericOnOff(
                    uuid,
                    if (onOffParam == 0) false else true,
                    0,
                    callback
                )
        } else {
            callback.onResult(false)
        }
    }

    private fun getNumberType(number: Any?): Int {
        if (number == null)
            return -2
        return if (number is Int) 1 else if (number is Double) 2 else if (number is Float) 3 else -1
    }

    fun fetchLightCurrentStatus(uuid: String, callback: MapCallback) {
        Utils.printLog(TAG, "fetchLightCurrentStatus")
        sendMeshMessage(uuid, 0, 0, "0C", "", callback)
    }

    fun subscribeStatus(uuid: String, callback: MapCallback) {
        var meshCallback = object : MeshCallback {
            override fun onReceive(msg: MeshMessage) {
                var node = MeshHelper?.getMeshNetwork()?.getNode(msg.src)
                Utils.printLog(TAG, " uuid:${node?.uuid} ,sendSubscribeMsg uuid:$uuid")
//                if (uuid.isNotEmpty() && node?.uuid?.toUpperCase() != uuid.toUpperCase()) {
//                    return
//                }

                var map = HashMap<String, Any>()
                map["uuid"] = node?.uuid ?: ""
                if (msg is GenericOnOffStatus) {
                    var switchMap = HashMap<String, Int>()
                    var param = msg.parameter
                    if (param.size == 1) {
                        var eleIndex =
                            node?.elements?.values?.indexOf(node?.elements?.get(msg.src)) ?: 0
                        switchMap["$eleIndex"] = param[0].toInt()
                        map["OnOffSwitch"] = switchMap
                        Utils.printLog(
                            TAG,
                            "onreceive node:${node?.uuid?.toUpperCase()}, eleIndex:$eleIndex,isOn:${switchMap["$eleIndex"]}"
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
                    when (msg.opCode) {
                        0x5D -> {//透传数据
                            var switchMap = HashMap<String, Int>()
                            for (index in 0 until msg.parameter.size) {
                                switchMap["$index"] = msg.parameter[index].toInt()
                            }

                            map["OnOffSwitch"] = switchMap

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
                } else if (msg is SensorStatus) {
                    msg.msensorData.forEach { sensorData ->
                        map.put(ByteUtil.bytesToHexString(sensorData.propertyId), sensorData.value)

                        Utils.printLog(
                            TAG,
                            "propertyId:${ByteUtil.bytesToHexString(sensorData.propertyId)},value:${ByteUtil.bytesToHexString(
                                sensorData.value
                            )}"
                        )
                    }

                    doMapCallback(
                        map,
                        callback,
                        CallbackMsg(
                            ConnectState.COMMON_SUCCESS.code,
                            ConnectState.COMMON_SUCCESS.msg
                        )
                    )
                } else if (msg is SensorBatteryStatus) {
                    map.put("battery", msg.battery)
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

            override fun onError(msg: CallbackMsg) {

            }
        }
        mConnectCallbacks["subscribeStatus"] = meshCallback
        MeshHelper.subscribeLightStatus(meshCallback)
    }

    fun unSubscribeLightStatus() {
        mConnectCallbacks.remove("subscribeStatus")
        MeshHelper.unSubscribeLightStatus()
    }

    fun parseLightStatus(
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
        var isOn = modeBits[7].toInt()

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

        var switchMap = HashMap<String, Int>()
        switchMap["0"] = isOn

        lightStatus["LightMode"] = mode
        lightStatus["Brightness"] = b
        lightStatus["ColorTemperature"] = t
        lightStatus["OnOffSwitch"] = switchMap

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

//    private fun addConnectStateListner(callback: Any) {
//        if (callback is MapCallback || callback is BooleanCallback)
//            mConnectCallbacks.add(callback)
//    }

//    private fun deleteConnectStateKListener(callback: Any) {
//        mConnectCallbacks.remove(callback)
//    }

    fun setCurrentNode(uuid: String) {
        MeshHelper.setSelectedMeshNode(MeshHelper.getProvisionedNodeByUUID(uuid))
    }

    fun getCurrentNode(callback: MapCallback) {
        MeshHelper.MeshProxyService.mMeshProxyService?.mNrfMeshManager?.mExtendedMeshNode?.let { node ->
            var map = HashMap<String, Any>()
            map.put("uuid", node.uuid)
            var elementsMap = HashMap<String, Any>()
            var elementsArr = ArrayList<HashMap<String, Any>>()

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

    fun getDeviceVersion(uuid: String, callback: MapCallback) {
        sendMeshMessage(uuid, 0, VENDOR_MODELID, "0A", "", callback)
    }

}