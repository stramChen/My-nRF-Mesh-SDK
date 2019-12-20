package qk.sdk.mesh.meshsdk

import android.content.Context
import android.view.View
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.weyye.hipermission.PermissionCallback
import no.nordicsemi.android.meshprovisioner.transport.ConfigAppKeyStatus
import no.nordicsemi.android.meshprovisioner.transport.ConfigModelAppStatus
import no.nordicsemi.android.meshprovisioner.transport.GenericOnOffStatus
import no.nordicsemi.android.meshprovisioner.transport.MeshMessage
import qk.sdk.mesh.meshsdk.bean.CallbackMsg
import qk.sdk.mesh.meshsdk.bean.ExtendedBluetoothDevice
import qk.sdk.mesh.meshsdk.callbak.*
import qk.sdk.mesh.meshsdk.mesh.BleMeshManager
import qk.sdk.mesh.meshsdk.util.ByteUtil
import qk.sdk.mesh.meshsdk.util.Constants
import qk.sdk.mesh.meshsdk.util.PermissionUtil
import qk.sdk.mesh.meshsdk.util.Utils
import kotlin.collections.HashMap

object MeshSDK {
    private val TAG = "MeshSDK"
    private var mContext: Context? = null
    private var mExtendedBluetoothDeviceMap = HashMap<String, ExtendedBluetoothDevice>()

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
                    var map = HashMap<String, Any>()
                    map.put("mac", it.getAddress())
                    map.put("rssi", it.rssi ?: 0)
                    map.put("name", it.name ?: "")

                    resultArray.add(map)
                    mExtendedBluetoothDeviceMap.put(it.getAddress(), it)
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

    fun provision(mac: String, callback: MapCallback) {
        var map = HashMap<Any, Any>()
        doBaseCheck(mac, map, callback)

        mContext?.let { context ->
            mExtendedBluetoothDeviceMap.get(mac)?.let { extendedBluetoothDevice ->
                MeshHelper.connect(
                    context,
                    extendedBluetoothDevice,
                    false,
                    object : ConnectCallback {
                        override fun onConnect() {
                            MeshHelper.startProvision(mExtendedBluetoothDeviceMap.get(mac)!!,
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
                                Utils.printLog(TAG, "provision:${node?.size}")
                                node?.let {
                                    node.forEach {
                                        if (Utils.isUUIDEqualsMac(
                                                Utils.getMacFromUUID(it.uuid),
                                                mac
                                            )
                                        ) {
                                            MeshHelper.setSelectedMeshNode(it)
                                            MeshHelper.setSelectedModel(null, null)
                                            MeshHelper.startScan(BleMeshManager.MESH_PROXY_UUID,
                                                object : ScanCallback {
                                                    override fun onScanResult(
                                                        devices: List<ExtendedBluetoothDevice>,
                                                        updatedIndex: Int?
                                                    ) {
                                                        devices.forEach { device ->
                                                            runBlocking {
                                                                launch {
                                                                    delay(1000)
                                                                    var selectedMeshNode =
                                                                        MeshHelper.getSelectedMeshNode()
                                                                    selectedMeshNode?.let { selectedNode ->
                                                                        if (Utils.isUUIDEqualsMac(
                                                                                Utils.getMacFromUUID(
                                                                                    selectedNode.uuid
                                                                                ),
                                                                                device.getAddress()
                                                                            )
                                                                        ) {
                                                                            MeshHelper.stopScan()
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

                        override fun onError(msg: CallbackMsg) {
                            map.clear()
                            map.put(msg.code, msg.msg)
                            callback.onResult(map)
                        }
                    })
            }
        }
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
        MeshHelper.getCurrentNetworkKey()?.let {
            callback.onResultMsg(ByteUtil.bytesToHexString(it.key))
        }
        callback.onResultMsg("")
    }

    fun createNetworkKey(networkKey: String) {
        MeshHelper.createNetworkKey(networkKey)
    }

    fun createApplicationKey(networkKey: String) {
        MeshHelper.createApplicationKey(networkKey)
    }

    fun bindApplicationKeyForNode(mac: String, callback: MapCallback) {
        var map = HashMap<Any, Any>()
        doBaseCheck(mac, map, callback)
        if (MeshHelper.isConnectedToProxy()) {
            MeshHelper.addAppkeys(object : MeshCallback {
                override fun onReceive(msg: MeshMessage) {
                    if (msg is ConfigAppKeyStatus) {
                        if (msg.isSuccessful) {//添加appkey成功
                            MeshHelper.getCompositionData()
                            Utils.printLog(TAG, "add app key success!")
                            MeshHelper.bindAppKey(this)
                        } else {
                            Utils.printLog(TAG, "add app key failed!")
                        }
                    } else if (msg is ConfigModelAppStatus) {
                        if (msg.isSuccessful) {//bind appkey成功
                            Utils.printLog(TAG, "bindAppKey success!")
                            //todo 轮询model列表，
                        } else {
                            Utils.printLog(TAG, "bindAppKey failed:${msg.statusCodeName}")
                        }
                    } else if (msg is GenericOnOffStatus) {
                        Utils.printLog(TAG, "get on off status")
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

    fun bindApplicationKeyForBaseModel(mac: String, callback: MapCallback) {

    }

    fun disConnect() {
        MeshHelper.disConnect()
    }

    private fun doBaseCheck(mac: String, map: HashMap<Any, Any>, callback: MapCallback) {
        if (mContext == null) {//判断sdk是否被初始化
            map.put(Constants.SDK_NOT_INIT_CODE, Constants.SDK_NOT_INIT_MSG)
            callback.onResult(map)
        }

        if (mExtendedBluetoothDeviceMap.get(mac) == null) {//判断是否存在此设备
            map.put(
                Constants.ConnectState.CANNOT_FIND_DEVICE_BY_MAC.code,
                Constants.ConnectState.CANNOT_FIND_DEVICE_BY_MAC.msg
            )
            callback.onResult(map)
        }
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