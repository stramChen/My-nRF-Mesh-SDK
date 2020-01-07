package qk.sdk.mesh.meshsdk

import android.content.Context
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.weyye.hipermission.PermissionCallback
import no.nordicsemi.android.meshprovisioner.UnprovisionedBeacon
import no.nordicsemi.android.meshprovisioner.transport.*
import qk.sdk.mesh.meshsdk.bean.CallbackMsg
import qk.sdk.mesh.meshsdk.bean.ExtendedBluetoothDevice
import qk.sdk.mesh.meshsdk.bean.MXQuadruples
import qk.sdk.mesh.meshsdk.callbak.*
import qk.sdk.mesh.meshsdk.mesh.BleMeshManager
import qk.sdk.mesh.meshsdk.util.ByteUtil
import qk.sdk.mesh.meshsdk.util.Constants
import qk.sdk.mesh.meshsdk.util.PermissionUtil
import qk.sdk.mesh.meshsdk.util.Utils
import java.lang.StringBuilder
import kotlin.collections.HashMap
import qk.sdk.mesh.meshsdk.util.Constants.ConnectState
import rx.Observable
import rx.android.schedulers.AndroidSchedulers

object MeshSDK {
    private val TAG = "MeshSDK"
    private var mContext: Context? = null
    private var mExtendedBluetoothDeviceMap = HashMap<String, ExtendedBluetoothDevice>()

    const val CWRGB_MODELID = 6094849
    const val ON_OFF_MODELID = 4096
//    const val ELEMENT_ADDRESS = 2

    const val LEAST_MODEL_COUNT = 4

    // 初始化 mesh
    fun init(context: Context) {
        mContext = context
        if (mContext != null)
            MeshHelper.initMesh(mContext!!)
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
        var scanCallback: ScanCallback = object : ScanCallback {
            override fun onScanResult(devices: List<ExtendedBluetoothDevice>, updatedIndex: Int?) {
                var resultArray = ArrayList<HashMap<String, Any>>()
                devices.forEach {
                    if (it.beacon != null) {
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

    fun provision(uuid: String, callback: MapCallback) {
        var map = HashMap<String, Any>()
        doBaseCheck(uuid, map, callback)
        if (MeshHelper.getCurrentNetworkKey() == null) {
            map.put(Constants.KEY_MESSAGE, ConnectState.NOT_SET_CURRENT_NET_KEY.msg)
            map.put(Constants.KEY_CODE, ConnectState.NOT_SET_CURRENT_NET_KEY.code)
            callback.onResult(map)
            return
        }

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
                            map.clear()
                            map.put(Constants.KEY_MESSAGE, msg.msg)
                            map.put(Constants.KEY_CODE, msg.code)
                            callback.onResult(map)
                            Utils.printLog(TAG, "onConnectStateChange:${msg.msg}")
                            if (msg.code == ConnectState.DISCONNECTED.code) {
                                var node = MeshHelper.getProvisionNode()
                                Utils.printLog(TAG, "getProvisionNode:${node?.size}")
                                node?.forEach {
                                    if (it.uuid == uuid) {
                                        stopScan()
                                        runBlocking {
                                            launch {
                                                delay(1000)
                                                MeshHelper.setSelectedMeshNode(it)
                                                MeshHelper.setSelectedModel(null, null)
                                                MeshHelper.startScan(BleMeshManager.MESH_PROXY_UUID,
                                                    object : ScanCallback {
                                                        override fun onScanResult(
                                                            devices: List<ExtendedBluetoothDevice>,
                                                            updatedIndex: Int?
                                                        ) {
                                                            devices.forEach { device ->
                                                                MeshHelper.getSelectedMeshNode()
                                                                    ?.let {
                                                                        if (mExtendedBluetoothDeviceMap.get(
                                                                                uuid
                                                                            )!!.getAddress() == device.getAddress()
                                                                        ) {
                                                                            stopScan()
                                                                            MeshHelper.connect(
                                                                                device,
                                                                                true,
                                                                                object :
                                                                                    ConnectCallback {
                                                                                    override fun onConnect() {
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
                                                                                    }

                                                                                    override fun onConnectStateChange(
                                                                                        msg: CallbackMsg
                                                                                    ) {

                                                                                    }

                                                                                    override fun onError(
                                                                                        msg: CallbackMsg
                                                                                    ) {
                                                                                        doMapCallback(
                                                                                            map,
                                                                                            callback,
                                                                                            msg
                                                                                        )
                                                                                    }
                                                                                })
                                                                        }
                                                                    }
                                                            }
                                                        }

                                                        override fun onError(msg: CallbackMsg) {
                                                            map.clear()
                                                            callback.onResult(map)
                                                            doMapCallback(map,callback,msg)
                                                        }
                                                    })
                                            }
                                        }
                                    }
                                }
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

    fun stopProvision() {
        //todo
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
        MeshHelper.setCurrentNetworkKey(networkKey)
    }

    fun getCurrentNetworkKey(callback: StringCallback) {
        callback.onResultMsg(MeshHelper.getCurrentNetworkKeyStr() ?: "")
    }

    fun createNetworkKey(networkKey: String) {
        Utils.printLog(TAG, "createNetworkKey:${networkKey}")
        MeshHelper.createNetworkKey(networkKey)
    }

    fun removeNetworkKey(key: String, callback: IntCallback) {
        MeshHelper.removeNetworkKey(key, callback)
    }

    fun createApplicationKey(networkKey: String): String {
        return MeshHelper.createApplicationKey(networkKey)
    }

    fun getAllApplicationKey(networkKey: String, callback: ArrayStringCallback) {
        MeshHelper.getAllApplicationKey(networkKey, callback)
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
                    MeshHelper.addAppkeys(applicationKey.keyIndex, object : MeshCallback {
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
                                    map.put(
                                        Constants.KEY_MESSAGE,
                                        msg.statusCodeName
                                    )
                                    map.put(
                                        Constants.KEY_CODE,
                                        ConnectState.BIND_APP_KEY_FOR_NODE_FAILED.code
                                    )
                                    callback.onResult(map)
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
    }

    fun setLightProperties(
        uuid: String, c: Int, w: Int, r: Int,
        g: Int, b: Int, callback: BooleanCallback
    ) {
        if (MeshHelper.getSelectedMeshNode()?.uuid != uuid) {
            MeshHelper.getProvisionNode()?.forEach { node ->
                if (node.uuid == uuid) {
                    MeshHelper.setSelectedMeshNode(node)
                }
            }
        }
        MeshHelper.setSelectedModel(
            MeshHelper.getSelectedMeshNode()?.elements?.values?.elementAt(0),
            MeshHelper.getSelectedMeshNode()?.elements?.values?.elementAt(0)?.meshModels?.get(
                CWRGB_MODELID
            )
        )

        var params = StringBuilder(
            "${ByteUtil.rgbtoHex(c)}${ByteUtil.rgbtoHex(w)}${ByteUtil.rgbtoHex(r)}${ByteUtil.rgbtoHex(
                g
            )}${ByteUtil.rgbtoHex(b)}"
        )
        MeshHelper.sendVendorModelMessage(
            Integer.valueOf("05", 16),
            ByteUtil.hexStringToBytes(params.toString()),
            false
        )
        callback.onResult(true)
    }

    fun getDeviceIdentityKeys(uuid: String, callback: MapCallback) {
        var map = HashMap<String, Any>()
        if (MeshHelper.getSelectedMeshNode()?.uuid != uuid) {
            MeshHelper.getProvisionNode()?.forEach { node ->
                if (node.uuid == uuid) {
                    MeshHelper.setSelectedMeshNode(node)
                }
            }
        }
        MeshHelper.setSelectedModel(
            MeshHelper.getSelectedMeshNode()?.elements?.values?.elementAt(0),
            MeshHelper.getSelectedMeshNode()?.elements?.values?.elementAt(0)?.meshModels?.get(
                CWRGB_MODELID
            )
        )
        var msgIndex = -1
        MeshHelper.sendVendorModelMessage(
            Integer.valueOf("00", 16),
            null,
            false, object : MeshCallback {
                override fun onReceive(msg: MeshMessage) {
                    Utils.printLog(TAG, "param:${String(msg.parameter)}")
                    if (msg.parameter.size >= 83) {
                        synchronized(msgIndex) {
                            if (msgIndex < 0) {
                                var pk = ByteArray(11)
                                var ps = ByteArray(16)
                                var dn = ByteArray(20)
                                var ds = ByteArray(32)
                                System.arraycopy(msg.parameter, 0, pk, 0, 11)
                                System.arraycopy(msg.parameter, 12, ps, 0, 16)
                                System.arraycopy(msg.parameter, 29, dn, 0, 20)
                                System.arraycopy(msg.parameter, 50, ds, 0, 32)
                                Utils.printLog(
                                    TAG,
                                    "pk:${String(pk)},ps:${String(ps)},dn:${String(dn)},ds:${String(
                                        ds
                                    )}"
                                )
                                map.put("pk", String(pk))
                                map.put("ps", String(ps))
                                map.put("dn", String(dn))
                                map.put("ds", String(ds))
                                map.put("code", ConnectState.COMMON_SUCCESS.code)
                                callback.onResult(map)
                                MeshHelper.unRegisterMeshMsg()
                                msgIndex = 0
                            }
                        }
                    }
                }

                override fun onError(msg: CallbackMsg) {
                    doMapCallback(map, callback, msg)
                }
            }
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
            MeshHelper.getSelectedMeshNode()?.unicastAddress ?: 0,
            configNodeReset
        )
    }

    fun connect(networkKey: String, callback: MapCallback) {
        var map = HashMap<String, Any>()
        doBaseCheck(null, map, callback)
        if (!MeshHelper.isConnectedToProxy()) {
            MeshHelper.startScan(BleMeshManager.MESH_PROXY_UUID, object : ScanCallback {
                override fun onScanResult(
                    devices: List<ExtendedBluetoothDevice>,
                    updatedIndex: Int?
                ) {
                    if (devices.isNotEmpty()) {
                        MeshHelper.stopScan()
                        MeshHelper.connect(devices[0], true, object : ConnectCallback {
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
                                    connect(networkKey, callback)
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
            }, networkKey)
        } else {
            doMapCallback(
                map, callback,
                CallbackMsg(ConnectState.COMMON_SUCCESS.code, ConnectState.COMMON_SUCCESS.msg)
            )
        }
    }

    private fun doMapCallback(map: HashMap<String, Any>, callback: MapCallback, msg: CallbackMsg) {
        map.put("code", msg.code)
        map.put("message", msg.msg)
        callback.onResult(map)
    }
}