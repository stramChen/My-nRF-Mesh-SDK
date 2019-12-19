package qk.sdk.mesh.meshsdk

import android.content.Context
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.weyye.hipermission.PermissionCallback
import qk.sdk.mesh.meshsdk.bean.CallbackMsg
import qk.sdk.mesh.meshsdk.bean.ExtendedBluetoothDevice
import qk.sdk.mesh.meshsdk.callbak.*
import qk.sdk.mesh.meshsdk.mesh.BleMeshManager
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
        if (mContext == null) {
            map.put(Constants.SDK_NOT_INIT_CODE, Constants.SDK_NOT_INIT_MSG)
            callback.onResult(map)
        }

        if (mExtendedBluetoothDeviceMap.get(mac) == null) {
            map.put(
                Constants.ConnectState.CANNOT_FIND_DEVICE_BY_MAC.code,
                Constants.ConnectState.CANNOT_FIND_DEVICE_BY_MAC.msg
            )
            callback.onResult(map)
        }

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