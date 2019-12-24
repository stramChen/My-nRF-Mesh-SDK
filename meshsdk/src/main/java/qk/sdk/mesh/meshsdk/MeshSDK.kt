package qk.sdk.mesh.meshsdk

import android.content.Context
import android.view.View
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.weyye.hipermission.PermissionCallback
import no.nordicsemi.android.meshprovisioner.UnprovisionedBeacon
import no.nordicsemi.android.meshprovisioner.models.GenericOnOffServerModel
import no.nordicsemi.android.meshprovisioner.models.VendorModel
import no.nordicsemi.android.meshprovisioner.transport.*
import qk.sdk.mesh.meshsdk.bean.CallbackMsg
import qk.sdk.mesh.meshsdk.bean.ExtendedBluetoothDevice
import qk.sdk.mesh.meshsdk.callbak.*
import qk.sdk.mesh.meshsdk.mesh.BleMeshManager
import qk.sdk.mesh.meshsdk.util.ByteUtil
import qk.sdk.mesh.meshsdk.util.Constants
import qk.sdk.mesh.meshsdk.util.PermissionUtil
import qk.sdk.mesh.meshsdk.util.Utils
import java.lang.StringBuilder
import kotlin.collections.HashMap

object MeshSDK {
    private val TAG = "MeshSDK"
    private var mContext: Context? = null
    private var mExtendedBluetoothDeviceMap = HashMap<String, ExtendedBluetoothDevice>()
    const val CWRGB_MODELID = 6094849

    // 初始化 mesh
    fun init(context: Context) {
        mContext = context
        if (mContext != null)
            MeshHelper.initMesh(mContext!!)
    }

    fun checkPermission(callback: StringCallback) {
        if (mContext == null) {
            callback.onResultMsg(Constants.ConnectState.SDK_NOT_INIT.msg)
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
                        val unprovisionedBeacon = UnprovisionedBeacon(it.beacon!!.beaconData)
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
        MeshHelper.startScan(
            if (type == Constants.SCAN_UNPROVISIONED) BleMeshManager.MESH_PROVISIONING_UUID else BleMeshManager.MESH_PROXY_UUID,
            scanCallback
        )
    }

    fun stopScan() {
        MeshHelper.stopScan()
    }

    fun provision(uuid: String, callback: MapCallback) {
        var map = HashMap<Any, Any>()
        doBaseCheck(uuid, map, callback)

        mContext?.let { context ->
            mExtendedBluetoothDeviceMap.get(uuid)?.let { extendedBluetoothDevice ->
                MeshHelper.connect(
                    context,
                    extendedBluetoothDevice,
                    false,
                    object : ConnectCallback {
                        override fun onConnect() {
                            MeshHelper.startProvision(mExtendedBluetoothDeviceMap.get(uuid)!!,
                                object : BaseCallback {
                                    override fun onError(msg: CallbackMsg) {
                                        map.clear()
                                        map.put(msg.code, msg.msg)
                                        callback.onResult(map)
                                    }
                                })
                        }

                        override fun onConnectStateChange(msg: CallbackMsg) {
                            map.clear()
                            map.put(msg.code, msg.msg)
                            callback.onResult(map)
                            Utils.printLog(TAG, "onConnectStateChange:${msg.msg}")
                            if (msg.code == Constants.ConnectState.DISCONNECTED.code) {
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
                                                                    ?.let { selectedNode ->
                                                                        if (mExtendedBluetoothDeviceMap.get(
                                                                                uuid
                                                                            )!!.getAddress() == device.getAddress()
                                                                        ) {
                                                                            stopScan()
                                                                            MeshHelper.connect(
                                                                                context,
                                                                                device,
                                                                                true,
                                                                                object :
                                                                                    ConnectCallback {
                                                                                    override fun onConnect() {
                                                                                        map.clear()
                                                                                        map.put(
                                                                                            Constants.ConnectState.PROVISION_SUCCESS.code,
                                                                                            Constants.ConnectState.PROVISION_SUCCESS.msg
                                                                                        )
                                                                                        callback.onResult(
                                                                                            map
                                                                                        )
                                                                                    }

                                                                                    override fun onConnectStateChange(
                                                                                        msg: CallbackMsg
                                                                                    ) {
                                                                                        map.clear()
                                                                                        map.put(
                                                                                            msg.code,
                                                                                            msg.msg
                                                                                        )
                                                                                        callback.onResult(
                                                                                            map
                                                                                        )
                                                                                    }

                                                                                    override fun onError(
                                                                                        msg: CallbackMsg
                                                                                    ) {

                                                                                    }
                                                                                })
                                                                        }
                                                                    }
                                                            }
                                                        }

                                                        override fun onError(msg: CallbackMsg) {
                                                            map.clear()
                                                            map.put(
                                                                msg.code,
                                                                msg.msg
                                                            )
                                                            callback.onResult(map)
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
                            map.put(msg.code, msg.msg)
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

    fun getAllNetworkKey(callback: ArrayStringCallback) {
        var keyList = ArrayList<String>()
        MeshHelper.getAllNetworkKey()?.forEach {
            keyList.add(ByteUtil.bytesToHexString(it.key))
        }
        callback.onResult(keyList)
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

    fun createApplicationKey(networkKey: String) {
        MeshHelper.createApplicationKey(networkKey)
    }

    fun getAllApplicationKey(networkKey: String, callback: ArrayStringCallback) {
        MeshHelper.getAllApplicationKey(networkKey, callback)
    }

    fun removeApplicationKey(appKey: String, networkKey: String, callback: IntCallback) {
        MeshHelper.removeApplicationKey(appKey, networkKey, callback)
    }

    fun bindApplicationKeyForNode(uuid: String, appKey: String, callback: MapCallback) {
        var map = HashMap<Any, Any>()
        doBaseCheck(uuid, map, callback)
        MeshHelper.getAppkeyByKeyName(appKey)?.let { applicationKey ->
            MeshHelper.getProvisionNode()?.forEach { node ->
                if (node.uuid == uuid) {
                    MeshHelper.setSelectedMeshNode(node)
                }
            }
            if (MeshHelper.isConnectedToProxy()) {
                var bindedIndex = 0
                MeshHelper.addAppkeys(applicationKey.keyIndex, object : MeshCallback {
                    override fun onReceive(msg: MeshMessage) {
                        if (msg is ConfigAppKeyStatus) {
                            if (msg.isSuccessful) {//添加appkey成功
                                Utils.printLog(TAG, "add app key success!")
                                MeshHelper.getCompositionData()
                            } else {
                                Utils.printLog(
                                    TAG,
                                    "add app key failed,because ${msg.statusCodeName}!"
                                )
                                map.clear()
                                map.put(
                                    Constants.ConnectState.BIND_APP_KEY_FOR_NODE_FAILED.code,
                                    msg.message
                                )
                                callback.onResult(map)
                            }
                        } else if (msg is ConfigModelAppStatus) {
                            if (msg.isSuccessful) {//bind appkey成功
                                Utils.printLog(TAG, "$bindedIndex,bindAppKey success!")
                            } else {
                                Utils.printLog(
                                    TAG,
                                    "$bindedIndex,bindAppKey failed:${msg.statusCodeName}"
                                )
                                map.clear()
                                map.put(
                                    Constants.ConnectState.BIND_APP_KEY_FOR_NODE_FAILED.code,
                                    Constants.ConnectState.BIND_APP_KEY_FOR_NODE_FAILED.msg
                                )
                            }
                            bindedIndex++
                            if (bindedIndex < (MeshHelper.getSelectedElement()?.meshModels?.size
                                    ?: 0)
                            ) {
                                MeshHelper.getSelectedModel()?.let {
                                    MeshHelper.setSelectedModel(
                                        MeshHelper.getSelectedElement(),
                                        MeshHelper.getSelectedElement()!!.meshModels!![bindedIndex]
                                    )
                                    MeshHelper.bindAppKey(this)
                                }
//                            Utils.printLog(TAG, "bindedIndex:$bindedIndex")
                            } else {
                                if (map.size > 0) {
                                    callback.onResult(map)
                                } else {
                                    map.put(
                                        Constants.ConnectState.PROVISION_SUCCESS.code,
                                        ""
                                    )
                                    callback.onResult(map)
                                }

                            }
                        } else if (msg is GenericOnOffStatus) {
                            Utils.printLog(TAG, "get on off status")
                        } else if (msg is ConfigCompositionDataStatus) {
                            Utils.printLog(TAG, "get getCompositionData success!")
                            bindedIndex = 0
                            MeshHelper.getSelectedMeshNode()?.let { node ->
                                node.elements?.forEach { eleKey, eleValue ->
                                    eleValue.meshModels?.forEach bindModel@{
                                        if (it.key == CWRGB_MODELID) {
                                            it.value.let { modelValue ->
                                                if (modelValue.boundAppKeyIndexes?.size ?: 0 <= 0) {
                                                    MeshHelper.setSelectedModel(
                                                        eleValue,
                                                        modelValue
                                                    )
                                                    MeshHelper.bindAppKey(this)
//                                                Utils.printLog(TAG, "bindedIndex:$bindedIndex")
                                                    return@bindModel
                                                }
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
                    Constants.ConnectState.CONNECT_NOT_EXIST.code,
                    Constants.ConnectState.CONNECT_NOT_EXIST.msg
                )
            }
        }
    }

    fun disConnect() {
        MeshHelper.disConnect()
    }

    private fun doBaseCheck(uuid: String, map: HashMap<Any, Any>, callback: MapCallback) {
        if (mContext == null) {//判断sdk是否被初始化
            map.put(Constants.SDK_NOT_INIT_CODE, Constants.SDK_NOT_INIT_MSG)
            callback.onResult(map)
            return
        }

        if (mExtendedBluetoothDeviceMap.get(uuid) == null) {//判断是否存在此设备
            map.put(
                Constants.ConnectState.CANNOT_FIND_DEVICE_BY_MAC.code,
                Constants.ConnectState.CANNOT_FIND_DEVICE_BY_MAC.msg
            )
            callback.onResult(map)
            return
        }
    }

    fun setGenericOnOff(uuid: String, onOff: Boolean, callback: BooleanCallback) {
// todo       MeshHelper.getProvisionNode()?.forEach { node ->
//            if (node.meshUuid == uuid) {
//                MeshHelper.setSelectedMeshNode(node)
//            }
//        }

        MeshHelper.setSelectedModel(
            MeshHelper.getSelectedMeshNode()?.elements?.get(2),
            MeshHelper.getSelectedElement()?.meshModels?.get(4096)
        )
        MeshHelper.sendGenericOnOff(onOff, 0)
    }

    fun setLightProperties(
        uuid: String, c: Int, w: Int, r: Int,
        g: Int, b: Int, callback: BooleanCallback
    ) {
        // todo       MeshHelper.getProvisionNode()?.forEach { node ->
//            if (node.meshUuid == uuid) {
//                MeshHelper.setSelectedMeshNode(node)
//            }
//        }

        MeshHelper.setSelectedModel(
            MeshHelper.getSelectedMeshNode()?.elements?.get(2),
            MeshHelper.getSelectedElement()?.meshModels?.get(CWRGB_MODELID)
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
    }

    fun resetNode(uuid: String) {
        // todo       MeshHelper.getProvisionNode()?.forEach { node ->
//            if (node.meshUuid == uuid) {
//                MeshHelper.setSelectedMeshNode(node)
//            }
//        }

        val configNodeReset = ConfigNodeReset()
        MeshHelper.sendMessage(
            MeshHelper.getSelectedMeshNode()?.unicastAddress ?: 0,
            configNodeReset
        )
    }
//    fun getWritableMap(map: HashMap<String, Object>): WritableNativeMap {
//        var nativeMap = WritableNativeMap()
//        map.forEach { key, value ->
//            var valueClass = value.javaClass
//            if (valueClass == Int.javaClass) {
//                nativeMap.putInt(key, value as Int)
//            } else if (valueClass == Boolean::class.java) {
//                nativeMap.putBoolean(key, value as Boolean)
//            } else if (valueClass is WritableArray) {
//                nativeMap.putArray(key, value as WritableArray)
//            } else if (valueClass == Double::class.java) {
//                nativeMap.putDouble(key, value as Double)
//            } else if (valueClass == String::class.java) {
//                nativeMap.putString(key, value as String)
//            } else if (valueClass is WritableMap) {
//                nativeMap.putMap(key, value as WritableMap)
//            }
//        }
//        return nativeMap
//    }

//    fun getWritableArray(map: HashMap<String, Object>): WritableNativeArray {
//
//    }

}