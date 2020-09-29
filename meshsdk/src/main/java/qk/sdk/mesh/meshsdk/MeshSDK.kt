package qk.sdk.mesh.meshsdk

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.Observer
import com.google.gson.Gson
import me.weyye.hipermission.PermissionCallback
import no.nordicsemi.android.meshprovisioner.Group
import no.nordicsemi.android.meshprovisioner.UnprovisionedBeacon
import no.nordicsemi.android.meshprovisioner.models.GenericOnOffServerModel
import no.nordicsemi.android.meshprovisioner.models.VendorModel
import no.nordicsemi.android.meshprovisioner.transport.*
import qk.sdk.mesh.meshsdk.bean.*
import qk.sdk.mesh.meshsdk.callback.*
import qk.sdk.mesh.meshsdk.mesh.BleMeshManager
import qk.sdk.mesh.meshsdk.service.BaseMeshService
import qk.sdk.mesh.meshsdk.util.*
import qk.sdk.mesh.meshsdk.util.Constants.ConnectState
import java.lang.Thread.sleep
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import qk.sdk.mesh.meshsdk.bean.DeviceConstantsCode as DC


object MeshSDK{
    private val TAG = "MeshSDK"
    private var mContext: Context? = null
    private var mExtendedBluetoothDeviceMap = HashMap<String, ExtendedBluetoothDevice>()

    const val VENDOR_MODELID = 153223168
    const val VENDOR_MODEL_COMPANYIDENTIFIER = 2338

    var mGattConnectCallbacks = ArrayList<MapCallback?>(20)

    var mConnectCallbacks = HashMap<String, Any>(20)

    private const val PUBLISH_INTERVAL = 88 //25秒
    private const val PUBLISH_TTL = 5

    private var isSubscribeDeviceStatusSuccessfully = false;

    //这里记录一下全局的手动点击的时间戳，外层的key是uuid，内层的key是属性名,value是时间戳
    var mDeviceTouchTimeStamp = java.util.HashMap<String, HashMap<String, Long>>()


    //callback name
    const val CALLBACK_GET_IDENTITY = "getDeviceIdentityKeys"

    // 初始化 mesh
    fun init(context: Context, callback: BooleanCallback? = null) {
        mContext = context
        Utils.mContext = context
        mContext?.apply {
            if (!Utils.isServiceExisted(this, "qk.sdk.mesh.meshsdk.MeshHelper\$MeshProxyService")) {
                MeshHelper.restartService(applicationContext, object : MapCallback() {
                    override fun onResult(result: HashMap<String, Any>) {
                        result.forEach {
                            Utils.printLog(TAG, "key:${it.key},value:${it.value}")
                        }

                        result["code"]?.apply {
                            if (this is Int && this == ConnectState.SERVICE_CREATED.code) {
                                callback?.onResult(true)
                                MeshHelper.MeshProxyService.mMeshProxyService?.tryStartHeartBeatCheck();
                            }
                        }
                    }
                })
            } else {
                callback?.onResult(true)
                MeshHelper.MeshProxyService.mMeshProxyService?.tryStartHeartBeatCheck();
            }
//            LogFileUtil.deleteLog(mContext)
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
        if (isScanning()) {
            Log.d(TAG,"===>-mesh-扫描前，发现正在扫描,现在先停止扫描")
            stopScan()
        }
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
                        val uuidUpCase = unprovisionedBeacon.uuid.toString().toUpperCase()
                        var map = HashMap<String, Any>()
                        map.put("mac", it.getAddress())
                        map.put("uuid", uuidUpCase)
                        map.put("rssi", it.rssi ?: 0)
                        map.put("name", it.name ?: "")
                        resultArray.add(map)
                        mExtendedBluetoothDeviceMap.put(uuidUpCase, it)
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
                "===>-mesh-开始扫描 startScan uuid:${if (type == Constants.SCAN_UNPROVISIONED) BleMeshManager.MESH_PROVISIONING_UUID else BleMeshManager.MESH_PROXY_UUID}"
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
    fun startProvision(uuid: String, networkKey: String, callback: MapCallback) {
        if(getGattConnectStatus() != BluetoothGatt.STATE_DISCONNECTED){
            //发现偶然会有在进入配网的时候连接还没断
            if(getGattConnectStatus() == BluetoothGatt.STATE_CONNECTED
                    || getGattConnectStatus() == BluetoothGatt.STATE_CONNECTING){
                Log.d(TAG,"===>-mesh-配网前发现Gatt正在连接或者已连接，现在去断开连接")
                innerDisConnect()
            }
            Log.d(TAG,"===>-mesh-配网前发现Gatt并没有断开连接,现在开始等待Gatt断开连接然后去配网")
            MeshHelper.MeshProxyService.mMeshProxyService?.mNrfMeshManager
            ?.connectionState?.observe(MeshHelper.MeshProxyService.mMeshProxyService!!
                            , object : Observer<CallbackMsg> {
                        override fun onChanged(msg: CallbackMsg?) {
                            if(ConnectState.DISCONNECTED.code == msg?.code){
                                Log.d(TAG,"===>-mesh-已经断开连接回调,现在要真的去配网了")
                                MeshHelper.MeshProxyService.mMeshProxyService?.mNrfMeshManager
                                        ?.connectionState?.removeObserver(this)
                                realStartProvision(uuid, callback, networkKey)
                            }
                        }
                    })
        }else{
            realStartProvision(uuid, callback, networkKey)
        }
    }


    /**
     * 真的要开始配网了
     */
    private fun realStartProvision(uuid: String, callback: MapCallback, networkKey: String) {
        val uuidUpcase = uuid.toUpperCase()
        mContext?.apply {
            init(this, object : BooleanCallback() {
                override fun onResult(boolean: Boolean) {
                    if (boolean) {
                        var map = HashMap<String, Any>()
                        doBaseCheck(uuidUpcase, map, callback)
                        if (networkKey.isEmpty() || getAllNetworkKey().size <= 0
                                || !getAllNetworkKey().contains(networkKey.toUpperCase())
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
                            mExtendedBluetoothDeviceMap.get(uuidUpcase)?.let { extendedBluetoothDevice ->
                                Utils.printLog(
                                        TAG,
                                        "===>-mesh-extendedBluetoothDevice:${extendedBluetoothDevice.getAddress()}"
                                )
                                var connectCallback = object : ConnectCallback {
                                    override fun onConnect() {
                                        notifyAllGattConnectCallback(
                                                CallbackMsg(
                                                        ConnectState.COMMON_SUCCESS.code,
                                                        ConnectState.COMMON_SUCCESS.msg
                                                )
                                        )
                                        isConnecting.set(false)
                                        //开始启动配置邀请
                                        MeshHelper.startProvision(mExtendedBluetoothDeviceMap[uuidUpcase]!!,
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
                                                "===>-mesh-provision onConnectStateChange code:${msg.code} ,msg:${msg.msg}"
                                        )

                                        when (msg.code) {
                                            ConnectState.PROVISION_SUCCESS.code -> {
                                                hasProvisioned = true
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
                                            ConnectState.DISCONNECTED.code -> {
                                                //连接断开，自动寻找代理节点重连
                                                if (MeshSDK.needReconnect && !isReconnect.get()) {
                                                    tryReconnect(networkKey)
                                                }
                                            }
                                            ConnectState.CONNECT_BLE_RESOURCE_FAILED.code -> {
                                                isConnecting.set(false)
                                                if (ConnectState.CONNECT_BLE_RESOURCE_FAILED.code == msg.code) {
                                                    disConnect()
                                                    MeshHelper.clearGatt()
                                                }
                                                //如果已经配网成功之后，突然断连接，则可能是网关配网成功后的
                                                //蓝牙重启操作，因此需要进行重连尝试
                                                if (hasProvisioned && needReconnect) {
                                                    MeshHelper.unRegisterConnectListener()
                                                    tryReconnectAfterProvison(networkKey, map, callback)
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
                                        isConnecting.set(false)
                                        if (msg.code == ConnectState.STOP_CONNECT.code) {

                                        }
                                        map.clear()
                                        map.put(Constants.KEY_MESSAGE, msg.msg)
                                        map.put(Constants.KEY_CODE, msg.code)
                                        callback.onResult(map)
                                        needReconnect = false
                                    }
                                }
                                synchronized(isConnecting) {
                                    if (!isConnecting.get()) {
                                        isConnecting.set(true)
                                        MeshHelper.connect(
                                                extendedBluetoothDevice,
                                                false, connectCallback
                                        )
                                    }
                                }

                            }
                        }
                    }
                }
            })
        }
    }

    private fun tryReconnectAfterProvison(networkKey: String, map: HashMap<String, Any>, callback: MapCallback) {
        reConnect(networkKey, object : MapCallback() {
            override fun onResult(result: HashMap<String, Any>) {
                for ((key, value) in result) {
                    if (value is Int && value == 200) {
                        doMapCallback(
                                map,
                                callback,
                                CallbackMsg(
                                        ConnectState.DEVICE_CONNECTED.code,
                                        ConnectState.DEVICE_CONNECTED.msg
                                )
                        )
                    }
                }
            }
        })
    }

    /**
     * 回调所有的Gatt连接
     */
    private fun notifyAllGattConnectCallback(msg: CallbackMsg) {

        if (null == mGattConnectCallbacks || mGattConnectCallbacks.size == 0) return;

        Log.d(TAG, "===>-mesh-返回所有Gatt连接监听:${msg.code}:${msg.msg}")
        synchronized(mGattConnectCallbacks) {
            var map = HashMap<String, Any>()
            map.put("code", msg.code)
            map.put("message", msg.msg)
            mGattConnectCallbacks.forEach {
                it?.onResult(map);
            }
            mGattConnectCallbacks.clear()
        }
    }

    /**
     * 重连
     */
    private fun reConnect(networkKey: String, callback: MapCallback) {
        mContext?.apply {
            init(this.applicationContext, object : BooleanCallback() {
                override fun onResult(boolean: Boolean) {
                    if (boolean) {
                        connect(applicationContext, networkKey, callback)
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
            data.add(map)
        }
        callback.onResult(data)
    }

    fun removeProvisionedNode(uuid: String) {
        MeshHelper.deleteProvisionNode(MeshHelper.getProvisionedNodeByUUID(uuid))
    }

    fun removeProvisionedNode(uuid: String, callBack: ProvisionCallback? = null) {
        var provisionedMeshNode: ProvisionedMeshNode? = MeshHelper.getProvisionedNodeByUUID(uuid);
        if (null == provisionedMeshNode) {
            Log.d(TAG, "===>-mesh-设备没有找到，直接返回删除成功")
            callBack?.onNodeDeleted(true, null)
        } else {
            MeshHelper.deleteProvisionNode(provisionedMeshNode, callBack)
        }
    }

    /**
     * 添加appkey，获取CompositionData并自动绑定所有的element下的所有model
     */
    fun addApplicationKeyForNode(uuid: String, appKey: String, callback: MapCallback) {
        mContext?.apply {
            init(this, object : BooleanCallback() {
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
//                                                if ((MeshHelper.getSelectedMeshNode()?.elements?.size
//                                                        ?: 0) <= 0
//                                                ) {
                                                MeshHelper.getCompositionData(
                                                        GET_COMPOSITION_DATA,
                                                        this
                                                )
//                                                }
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
                                        } else {
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
        isConnecting.set(false)
        isSubscribeDeviceStatusSuccessfully = false
        MeshHelper.MeshProxyService.mMeshProxyService?.mConnectCallback = null
        MeshHelper.disConnect()
        clearAllCallbacks()
        Utils.printLog(TAG, "===>-mesh- 断开蓝牙连接")
    }

    /**
     * 内部断开gatt连接，不清除回调
     */
    private fun innerDisConnect() {
        isConnecting.set(false)
        isSubscribeDeviceStatusSuccessfully = false
        MeshHelper.innerDisConnect()
        Utils.printLog(TAG, "===>-mesh- 内部断开蓝牙连接，但不返回callback")
    }

    /**
     * 连接前需要检查一下，资源变的情况
     */
    fun checkResourceBeforeConnect() {
        //订阅需要释放
        if (isSubscribeDeviceStatusSuccessfully) {
            Log.d(TAG, "===>-mesh-发现订阅状态没释放,现在去释放")
            isSubscribeDeviceStatusSuccessfully = false
        }
    }

    private fun clearAllCallbacks() {
        mConnectCallbacks.forEach { requestCallback ->
            if (requestCallback is MapCallback) {
                var map = HashMap<String, Any>()
                doMapCallback(
                        map, requestCallback,
                        CallbackMsg(
                                CommonErrorMsg.DISCONNECTED.code,
                                CommonErrorMsg.DISCONNECTED.msg
                        )
                )
            }

            if (requestCallback is BooleanCallback) {
                requestCallback.onResult(false)
            }

            if (requestCallback is StringCallback) {
                requestCallback.onResultMsg("");
            }
        }
        notifyAllGattConnectCallback(
                CallbackMsg(
                        CommonErrorMsg.DISCONNECTED.code,
                        CommonErrorMsg.DISCONNECTED.msg
                )
        )
        mConnectCallbacks.clear()
        mGattConnectCallbacks.clear()
        MeshHelper.unRegisterConnectListener()
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
     * 协议v2.0：灯开关
     */
    fun setGenericOnOff(
            uuid: String, onOff: Boolean,
            eleIndex: Int, callback: BooleanCallback
    ) {
        if (!MeshHelper.isConnectedToProxy()) {
            callback.onResult(false)
            return
        }

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

            mDeviceTouchTimeStamp[uuid] = hashMapOf(SWITCH to System.currentTimeMillis())

            sendMeshMessage(
                    uuid,
                    eleIndex,
                    VENDOR_MSG_OPCODE_ATTR_SET,
                    listOf(Pair(DC.lightCons[SWITCH], if (onOff) "01" else "00")),
                    callback
            )

        } catch (e: Exception) {
            e.printStackTrace()
            callback.onResult(false)
        }
    }

    /**
     * 协议v2.0：单火开关
     */
    fun setSocketOnOff(
            uuid: String, onOff: Boolean,
            eleIndex: Int, callback: BooleanCallback
    ) {
        if (!MeshHelper.isConnectedToProxy()) {
            callback.onResult(false)
            return
        }

        try {
            var list = ArrayList<Pair<String?, String?>?>();
            if (eleIndex == 0) {
                list.add(Pair(DC.socketCons[SWITCH], if (onOff) "01" else "00"))
            }
            if (eleIndex == 1) {
                list.add(Pair(DC.socketCons[SWITCH_SECOND], if (onOff) "01" else "00"))
            }
            if (eleIndex == 2) {
                list.add(Pair(DC.socketCons[SWITCH_THIRD], if (onOff) "01" else "00"))
            }

            sendMeshMessage(
                    uuid,
                    0,
                    VENDOR_MSG_OPCODE_ATTR_SET,
                    list,
                    callback
            )

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

    /**
     * 将Type和value结合起来
     */
    fun combineAttTypeAndAttValue(params: List<Pair<String?, String?>?>?): String {
        val res = StringBuilder();
        params?.forEach {
            res.append(it?.first ?: "")
            res.append(it?.second ?: "")
        }
        return res.toString();
    }

    /**
     * 发送单播消息
     * Pair存在放<attrType,attrValue>
     */
    fun sendMeshMessage(
            uuid: String,
            elementIndex: Int,
            opcode: String,
            param: List<Pair<String?, String?>?>?,
            callback: BaseCallback,
            key: String = "",
            timeout: Boolean = false,
            retry: Boolean = false
    ) {
        //检查一下数据库数据的完整性，避免有脏数据
        if(!MeshHelper.checkDeviceDataIntegrity(uuid)){
            return
        }

        //拼接参数
        val param = combineAttTypeAndAttValue(param);
        Utils.printLog(
                TAG, "===>-mesh- 准备发送数据 uuid:${uuid}" +
                "===opCode:${opcode}===param:${param}"
        )

        mContext?.apply {
            init(this, object : BooleanCallback() {
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

                        mConnectCallbacks[if (key.isEmpty()) "sendMeshMessage" else key] =
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
                        var sequence: Int = 0;
                        if (param.isNotEmpty()) {
                            sequence = MxMeshUtil.generateTid()
                            Log.d(TAG, "===>-mesh- 发送数据前 生成的sequence= $sequence")
                            newParam =
                                    "${ByteUtil.bytesToHexString(byteArrayOf(sequence.toByte()))}$param"
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
                                                //Set the opCode to a 3-bit opCode
                                                Integer.valueOf(opcode, 16),
                                                ByteUtil.hexStringToBytes(newParam)
                                        )

                                        MeshHelper.sendMessage(
                                                MeshHelper.generatePrimaryKey(sequence, uuid),
                                                element.elementAddress,
                                                message,
                                                callback, timeout, retry
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
        } else if (callback is StringCallback) {
            callback.onResultMsg(Gson().toJson(msg))
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
                VENDOR_MSG_OPCODE_ATTR_GET,
                listOf(Pair(ATTR_TYPE_COMMON_GET_QUADRUPLES, null)),
                callback,
                CALLBACK_GET_IDENTITY,
                true,
                true
        )
    }

    /**
     * 协议v2 .0:获取某个设备的属性
     * @param uuid 设备唯一id
     * @param properties 需要获取属性的集合
     */
    fun getDeviceCurrentStatus(uuid: String, properties: List<String?>?, callback: StringCallback) {
        val productId = MxMeshUtil.getProductIdByUUID(uuid).toString()
        var param: ArrayList<Pair<String?, String?>?>? = ArrayList()
        properties?.forEach {
            it?.let {
                when (productId) {
                    PRODUCT_ID_LIGHT_2,
                    PRODUCT_ID_LIGHT_5 -> {
                        param!!.add(Pair(DC.lightCons[it], null))
                    }
                    PRODUCT_ID_SOCKKET_SINGLE,
                    PRODUCT_ID_SOCKKET_DOBULE,
                    PRODUCT_ID_SOCKKET_TRIPLE -> {
                        param!!.add(Pair(DC.socketCons[it], null))
                    }
                    PRODUCT_ID_PIR_SENSOR -> {
                        param!!.add(Pair(DC.pirSensorCons[it], null))
                    }
                    else -> {
                    }
                }
            }
        }
        sendMeshMessage(
                uuid,
                0,
                VENDOR_MSG_OPCODE_ATTR_GET,
                param,
                callback,
                CALLBACK_GET_IDENTITY,
                true,
                true
        )
    }

    private fun deleyDisconnectDevice() {
        Handler(Looper.getMainLooper()).postDelayed(object : Runnable {
            override fun run() {
                innerDisConnect()
            }
        }, 1000)
    }

    fun resetNode(uuid: String) {
        MeshHelper.getProvisionNode()?.forEach { node ->
            if (node.uuid == uuid) {
                MeshHelper.setSelectedMeshNode(node)
                if (getGattDirectConnectNode()?.uuid == node.uuid) {
                    deleyDisconnectDevice()
                }
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

    //判断是否正在连接,为了避免多个连接请求调用的时候出现连接的问题，当有多个连接同时请求的时候
    //mGattConnectCallbacks会将请求缓存起来，当连接成功的时候，一并返回
    var isConnecting: AtomicBoolean = AtomicBoolean(false)
    var needReconnect = true

    /**
     * 建立gatt连接
     */
    fun connect(context: Context, networkKey: String, callback: MapCallback) {
        init(context, object : BooleanCallback() {
            override fun onResult(isInit: Boolean) {

                if(!isInit){
                    Log.d(TAG, "===>-发现初始化失败,连接结束")
                    return
                }

                //如果有节点才去连接gatt
                if (null == MeshHelper.getProvisionNode()
                        || MeshHelper.getProvisionNode()!!.size <= 0) {
                    Log.d(TAG, "===>-发现没有节点,连接结束")
                    return
                }

                if (isInit) {
                    var map = HashMap<String, Any>()
                    doBaseCheck(null, map, callback)
                    //进行一下包含判断，因为重连的时候也会走这个
                    if (!mGattConnectCallbacks.contains(callback)) {
                        mGattConnectCallbacks.add(callback)
                    }

                    if (!MeshHelper.isConnectedToProxy() && MeshHelper.getProvisionNode()?.size ?: 0 > 0) {
                        synchronized(isConnecting) {
                            if (isConnecting.get()) {
                                Log.d(TAG, "===>Mesh SDK 层 isConnecting 状态是true,这次请求被搁置")
                                return
                            }
                            isConnecting.set(true)
                            //做一下容错超时机制，防止回调不回来，下次无法重连
                            Handler().postDelayed(object : Runnable {
                                override fun run() {
                                    isConnecting.set(false)
                                }
                            }, 8000)
                        }

                        //连接前检查一下需要释放的资源
                        checkResourceBeforeConnect()

                        Utils.printLog(TAG, "===>-mesh- connect start scan")
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
                                            "===>-mesh- 扫描到待配对节点 " +
                                                    "connect onScanResult:${devices[0].getAddress()}"
                                    )
                                    var connectCallback = object :
                                            ConnectCallback {
                                        override fun onConnect() {
                                            stopScan()
                                            isReconnect.set(false)
                                            isConnecting.set(false)
                                            notifyAllGattConnectCallback(
                                                    CallbackMsg(
                                                            ConnectState.COMMON_SUCCESS.code,
                                                            ConnectState.COMMON_SUCCESS.msg
                                                    )
                                            )
                                        }

                                        override fun onConnectStateChange(msg: CallbackMsg) {
                                            Utils.printLog(
                                                    TAG,
                                                    "===>-mesh-connect onConnectStateChange:${msg.msg}" +
                                                            ",needReconnect:$needReconnect" +
                                                            ",isReconnect:$isReconnect"
                                            )
                                            if (msg.code == ConnectState.DISCONNECTED.code && needReconnect && !isReconnect.get()) {//连接断开，自动寻找代理节点重连
                                                tryReconnect(networkKey)
                                            }
                                        }

                                        override fun onError(msg: CallbackMsg) {
                                            isConnecting.set(false)
                                            Utils.printLog(
                                                    TAG, "===>-mesh-connect连接出现异常。${msg.code}:${msg.msg}"
                                            )
                                            if (msg.code == ConnectState.STOP_CONNECT.code)
                                                needReconnect = false
                                            notifyAllGattConnectCallback(msg)
                                        }
                                    }
                                    MeshHelper.connect(devices[0], true, connectCallback)
                                }
                            }

                            override fun onError(msg: CallbackMsg) {
                                notifyAllGattConnectCallback(msg)
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

    private fun tryReconnect(networkKey: String) {
        Utils.printLog(
                TAG,
                "===>-mesh-开始尝试重新连接"
        )
        isConnecting.set(false)
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            Utils.printLog(
                    TAG,
                    "===>蓝牙开关关闭了,无法进行重连"
            )
            return
        }
        if (!isReconnect.get()) {
            isReconnect.set(true)
            reconnectAndSubscribeDeviceStatus(networkKey)
        }
    }

    /**
     * 获取Gatt连接的节点
     */
    fun getGattDirectConnectNode(): ProvisionedMeshNode? {
        var macAddress: String? = MeshHelper.MeshProxyService.mMeshProxyService
                ?.getConnectedDevice()?.getAddress()
        MeshHelper.getProvisionNode()?.forEach {
            var nodeMacAddress = MxMeshUtil.getMacAddressIdByUUID(it.uuid)
            if (nodeMacAddress == macAddress) return it;
        }
        return null;
    }

    /**
     * 重连并且订阅设备状态
     */
    private fun reconnectAndSubscribeDeviceStatus(networkKey: String) {
        reConnect(networkKey, object : MapCallback() {
            override fun onResult(result: HashMap<String, Any>) {
                for ((key, value) in result) {
                    //重连完成要自己订阅设备状态
                    if (value is Int && value == 200) {
                        subscribeThreeGroups()
                    }
                }
            }
        })
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

    fun checkOldGroupExist() {
        MeshHelper.removeGroup("New Group")
        MeshHelper.removeGroup("0xD000")
        MeshHelper.removeGroup("0xD002")
    }

    /**
     * 订阅状态上报
     */
    fun subscribeDeviceStatus(callback: IDeviceStatusCallBack) {
        checkOldGroupExist();
        subscribeThreeGroups()
        BaseMeshService.mDownStreamCallback = callback
    }

    /**
     * 订阅两个组播地址D000,D002,D003
     */
    private fun subscribeThreeGroups() {
        createGroup(SUBSCRIBE_ALL_DEVICE, object : BooleanCallback() {
            override fun onResult(boolean: Boolean) {
                isSubscribeDeviceStatusSuccessfully = boolean
                Utils.printLog(TAG, "===>-mesh- createGroup:$SUBSCRIBE_ALL_DEVICE:$boolean")
            }
        }, SUBSCRIBE_ALL_DEVICE_ADDR)
        //创建一个组播地址为了同步设备状态
        createGroup(ALL_DEVICE_SYNC, object : BooleanCallback() {
            override fun onResult(boolean: Boolean) {
                isSubscribeDeviceStatusSuccessfully = boolean
                Utils.printLog(TAG, "===>-mesh- createGroup:$ALL_DEVICE_SYNC:$boolean")
            }
        }, ALL_DEVICE_SYNC_ADDR)
        //为本地自动化添加独特的组播地址
        createGroup(LOCAL_LINKAGE, object : BooleanCallback() {
            override fun onResult(boolean: Boolean) {
                isSubscribeDeviceStatusSuccessfully = boolean
                Utils.printLog(TAG, "===>-mesh- createGroup:$LOCAL_LINKAGE:$boolean")
            }
        }, LOCAL_LINKAGE_ADDR)
    }

    /**
     * 检查是否已经订阅设备状态成功
     */
    fun isSubscribleDeviceStatusSuccess(): Boolean {
        return isSubscribeDeviceStatusSuccessfully
    }

    /**
     * 发一次获取所有设备主属性的同步请求，接口通过{@see #subscribeDeviceStatus}的回调统一返回
     * 因此你在调用此方法的时候，必须进行{@see #subscribeDeviceStatus}订阅操作。
     */
    fun fetchAllDeviceStatus() {
        MeshHelper.getGroupByAddress(ALL_DEVICE_SYNC_ADDR)?.let { group ->
            val networkKey =
                    MeshHelper.MeshProxyService.mMeshProxyService?.getCurrentNetworkKeyStr();
            getAllApplicationKey(networkKey!!, object : ArrayStringCallback {
                override fun onResult(result: ArrayList<String>) {
//                    var newParam = ""
                    var newParam = "0100" + "010100"
                    //如果消息有参数，消息参数需加上tid，规则：秒级时间戳余255
                    var timeCuts = MxMeshUtil.generateTid()
                    newParam =
                            "${ByteUtil.bytesToHexString(byteArrayOf(timeCuts.toByte()))}${newParam}"
                    var message = MeshHelper.getAppkeyByKeyName(result[0])?.let {
                        VendorModelMessageUnacked(
                                it, VENDOR_MODELID, VENDOR_MODEL_COMPANYIDENTIFIER,
                                VENDOR_MSG_OPCODE_SYNC,
                                ByteUtil.hexStringToBytes(newParam)
//                        VendorModelMessageUnackedState(
//                        VendorModelMessageUnackedState(
//                                    it, VENDOR_MODEL_COMPANYIDENTIFIER)
                        )
                    }
                    if (message != null) {
                        MeshHelper.sendMessage("", group.address, message, null)
                    }
                }
            })
        }
    }

    /**
     * 获取设备在线状态
     */
    fun isDeviceOnline(uuid: String): Boolean {
        var status: Int =
                MeshHelper.MeshProxyService.mMeshProxyService?.mHeartBeatMap?.get(uuid.toUpperCase())
                        ?: 0
        var heartCheckTime: Long =
                MeshHelper.MeshProxyService.mMeshProxyService?.mHartBeanStatusMarkTime ?: 0
        //当设备状态是11或者01,或者10且未超过心态时间的时候，考虑它是在线状态
        if (status and 1 == 1
                || status == 1
                || (status == 2 && System.currentTimeMillis() - heartCheckTime <
                        BaseMeshService.HEART_BEAT_CHECK_PERIOD * 1000)
        ) return true;
        return false;
    }

    /**
     * 取消状态上报
     */
    fun unRegisterDownStreamListener() {
        MeshHelper.removeGroup(SUBSCRIBE_ALL_DEVICE);
    }


    fun subscribeStatus(uuid: String, callback: MapCallback) {
        var meshCallback = object : MeshCallback {
            override fun onReceive(msg: MeshMessage) {
                var node = MeshHelper.getMeshNetwork()?.getNode(msg.src)
                Utils.printLog(TAG, " uuid:${node?.uuid} ,sendSubscribeMsg uuid:$uuid")
                if (uuid.isNotEmpty() && node?.uuid?.toUpperCase() != uuid.toUpperCase()) {
                    return
                }

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
//                        parseLightStatus(msg.parameter, callback, map)
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
                                "propertyId:${ByteUtil.bytesToHexString(sensorData.propertyId)},value:${
                                    ByteUtil.bytesToHexString(
                                            sensorData.value
                                    )
                                }"
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
            var group: Group? = null
            var groups = MeshHelper.getGroupByName(groupName)
            if (groups.size > 0) {
                group = groups[0]
            }
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

    fun setPublication(uuid: String, groupName: String, callback: MapCallback) {
        var map = HashMap<String, Any>()
        if (doProxyCheck(uuid, map, callback)) {
            //通过groupName获取group
            var group: Group? = null
            var groups = MeshHelper.getGroupByName(groupName)
            if (groups.size > 0) {
                group = groups[0]
            }
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
                                    eleValue.elementAddress,
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

    //    fun setPublication(uuid: String, groupName: String, groupAddr: Int, callback: MapCallback) {
//        var map = HashMap<String, Any>()
//        if (doProxyCheck(uuid, map, callback)) {
//            if (MeshHelper.getGroupByAddress(groupAddr) == null) {
//                MeshHelper.createGroup(groupName, groupAddr)
//            }
//
//            //获取provisioned节点
//            var node = MeshHelper.getProvisionedNodeByUUID(uuid)
//            if (node == null) {
//                doMapCallback(
//                    map,
//                    callback,
//                    CallbackMsg(ConnectState.NODE_NOT_EXIST.code, ConnectState.NODE_NOT_EXIST.msg)
//                )
//                return
//            }
//
//            var publishAddress = groupAddr
//            var modelTotalSize = 0
//            var index = 0
//            node.elements.values.forEach { eleValue ->
//                modelTotalSize = modelTotalSize.plus(eleValue.meshModels?.values?.size ?: 0)
//                eleValue.meshModels?.values?.forEach { meshModel ->
//                    if (meshModel.boundAppKeyIndexes?.size ?: 0 > 0 && (meshModel is GenericOnOffServerModel || meshModel is VendorModel)) {
//                        runBlocking {
//                            launch {
//                                delay(500)
//                                var meshMsg = ConfigModelPublicationSet(
//                                    eleValue.elementAddress
//                                    ,
//                                    publishAddress,
//                                    meshModel.boundAppKeyIndexes[0],
//                                    false,
//                                    PUBLISH_TTL,
//                                    PUBLISH_INTERVAL,
//                                    0,
//                                    0,
//                                    0,
//                                    meshModel.modelId
//                                )
//
//                                try {
//                                    MeshHelper.MeshProxyService.mMeshProxyService?.mNrfMeshManager?.meshManagerApi
//                                        ?.createMeshPdu(node.unicastAddress, meshMsg)
//                                    index++
//                                } catch (ex: IllegalArgumentException) {
//                                    ex.printStackTrace()
//                                }
//                            }
//                        }
//                    } else {
//                        index++
//                    }
//                }
//            }
//
//            if (index == modelTotalSize) {
//                doMapCallback(
//                    map,
//                    callback,
//                    CallbackMsg(ConnectState.COMMON_SUCCESS.code, ConnectState.COMMON_SUCCESS.msg)
//                )
//            } else {
//                doMapCallback(
//                    map,
//                    callback,
//                    CallbackMsg(ConnectState.PUBLISH_FAILED.code, ConnectState.PUBLISH_FAILED.msg)
//                )
//            }
//        }
//
//    }

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

        if (hsv != null) {
            mDeviceTouchTimeStamp[uuid] = hashMapOf(COLOR to System.currentTimeMillis())
            var vendorMap = hsv as HashMap<String, Any>
            var h = vendorMap["Hue"]
            var s = vendorMap["Saturation"]
            var v = vendorMap["Value"]

            var paramType = Utils.getNumberType(h)
            if (paramType < 0 || h == null || s == null || v == null) {
                callback.onResult(false)
                return
            }
            var value =
                    "${
                        ByteUtil.bytesToHexString(
                                ByteUtil.shortToByte(
                                        if (paramType == 1) (h as Int).toShort()
                                        else if (paramType == 2) (h as Double).toShort()
                                        else (h as Float).toShort()
                                )
                        )
                    }${
                        ByteUtil.bytesToHexString(
                                byteArrayOf(
                                        (if (paramType == 1) (v as Int).toByte()
                                        else if (paramType == 2) (v as Double).toByte()
                                        else (v as Float).toByte())
                                )
                        )
                    }${
                        ByteUtil.bytesToHexString(
                                byteArrayOf(
                                        (if (paramType == 1) (s as Int).toByte()
                                        else if (paramType == 2) (s as Double).toByte()
                                        else (s as Float).toByte())
                                )
                        )
                    }"
            sendMeshMessage(
                    uuid,
                    0,
                    VENDOR_MSG_OPCODE_ATTR_SET,
                    listOf(Pair(DC.lightCons[COLOR], value)),
                    callback
            )
        } else if (bright != null && Utils.getNumberType(bright) > 0) {
            mDeviceTouchTimeStamp[uuid] = hashMapOf(LIGHTNESS_LEVEL to System.currentTimeMillis())
            var briType = Utils.getNumberType(bright)
            var value = ByteUtil.bytesToHexString(
                    ByteUtil.shortToByte(
                            if (briType == 1) (bright as Int).toShort()
                            else if (briType == 2) (bright as Double).toShort()
                            else (bright as Float).toShort()
                    )
            )
            sendMeshMessage(
                    uuid,
                    0,
                    VENDOR_MSG_OPCODE_ATTR_SET,
                    listOf(Pair(DC.lightCons[LIGHTNESS_LEVEL], value)),
                    callback
            )
        } else if (temperature != null && Utils.getNumberType(temperature) > 0) {
            mDeviceTouchTimeStamp[uuid] = hashMapOf(COLOR_TEMPERATURE to System.currentTimeMillis())
            var temType = Utils.getNumberType(temperature)
            var temValue = ByteUtil.bytesToHexString(
                    ByteUtil.shortToByte(
                            (if (temType == 1) (temperature as Int).toShort()
                            else if (temType == 2) (temperature as Double).toShort()
                            else (temperature as Float).toShort())
                    )
            )
            sendMeshMessage(
                    uuid,
                    0,
                    VENDOR_MSG_OPCODE_ATTR_SET,
                    listOf(Pair(DC.lightCons[COLOR_TEMPERATURE], temValue)),
                    callback
            )
        } else if (mode != null) {
            mDeviceTouchTimeStamp[uuid] = hashMapOf(MODE_NUMBER to System.currentTimeMillis())
            var value = ByteUtil.bytesToHexString(
                    byteArrayOf(("$mode".toDouble().toInt()).toByte())
            )
            sendMeshMessage(
                    uuid,
                    0,
                    VENDOR_MSG_OPCODE_ATTR_SET,
                    listOf(Pair(DC.lightCons[MODE_NUMBER], value)),
                    callback
            )
        } else if (onOff != null && Utils.getNumberType(onOff) >= 0) {
            mDeviceTouchTimeStamp[uuid] = hashMapOf(SWITCH to System.currentTimeMillis())
            var onOffType = Utils.getNumberType(onOff)
            var onOffParam =
                    if (onOffType == 1) onOff as Int else if (onOffType == 2) (onOff as Double).toInt() else (onOff as Float).toInt()
            setGenericOnOff(uuid, onOffParam == 1, 0, callback)
        } else {
            callback.onResult(false)
        }
    }

    /**
     * 获取设备当前状态:亮度、色温、开关
     */
    fun fetchLightCurrentStatus(uuid: String, callback: StringCallback) {
        Utils.printLog(TAG, "fetchLightCurrentStatus")
        var productID = MxMeshUtil.getProductIdByUUID(uuid)
        var params = arrayListOf(
                Pair(DC.lightCons[LIGHTNESS_LEVEL], null),
                Pair(DC.lightCons[COLOR_TEMPERATURE], null),
                Pair(DC.lightCons[SWITCH], null)
        )
        if (productID.toString() == PRODUCT_ID_LIGHT_5) {
            params.addAll(
                    listOf(
                            Pair(DC.lightCons[COLOR], null),
                            Pair(DC.lightCons[MODE_NUMBER], null)
                    )
            )

        }
        sendMeshMessage(
                uuid,
                0,
                VENDOR_MSG_OPCODE_ATTR_GET,
                params, callback
        )
    }

    fun unSubscribeLightStatus() {
        mConnectCallbacks.remove("subscribeStatus")
        MeshHelper.unSubscribeLightStatus()
    }

    fun doMapCallback(
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
    fun getDeviceVersion(uuid: String, callback: StringCallback) {
        sendMeshMessage(
                uuid, 0, VENDOR_MSG_OPCODE_ATTR_GET, listOf(Pair(ATTR_TYPE_GET_VERSION, null)), callback
        )
    }

    /**
     * 虚拟按钮
     */
//    fun sendGroupMsg(groupAddr: Int, vid: Int) {
//        MeshHelper.getGroupByAddress(groupAddr)?.let { group ->
//            MeshHelper.getMeshNetwork()?.let { network ->
//                network.getModels()
//                network.getModels(group)?.forEach foreach@{ model ->
//                    if (MeshParserUtils.isVendorModel(model.modelId)) {
//                        model.boundAppKeyIndexes?.let { boundedAppkeys ->
//                            if (boundedAppkeys.size == 1) {
//                                val appKey: ApplicationKey =
//                                        network.getAppKey(boundedAppkeys[0])
//                                val message = VendorModelMessageUnacked(
//                                        appKey,
//                                        model.modelId,
//                                        (model as VendorModel).companyIdentifier,
//                                        0x18,
//                                        byteArrayOf(vid.toByte())
//                                )
//                                MeshHelper.sendMessage("", group.address, message, null)
//                                return@foreach
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }

    /**
     * 虚拟按钮
     */
    fun sendGroupMsg(vid: Int) {
        MeshHelper.getGroupByAddress(LOCAL_LINKAGE_ADDR)?.let { group ->
            val networkKey =
                    MeshHelper.MeshProxyService.mMeshProxyService?.getCurrentNetworkKeyStr();
            getAllApplicationKey(networkKey!!, object : ArrayStringCallback {
                override fun onResult(result: ArrayList<String>) {
//                    var newParam = ""
                    var newParam = ATTR_TYPE_VIRTUAL_BUTTON +
                            ByteUtil.bytesToHexString(byteArrayOf(vid.toByte()))
                    //如果消息有参数，消息参数需加上tid，规则：秒级时间戳余255
                    var timeCuts = ByteUtil.bytesToHexString(
                            byteArrayOf(MxMeshUtil.generateTid().toByte())
                    )
                    newParam = "${timeCuts}${newParam}"
                    var message = MeshHelper.getAppkeyByKeyName(result[0])?.let {
                        VendorModelMessageUnacked(
                                it, VENDOR_MODELID, VENDOR_MODEL_COMPANYIDENTIFIER, VENDOR_MSG_OPCODE_STATUS, ByteUtil.hexStringToBytes(newParam)
//                        VendorModelMessageUnackedState(
//                        VendorModelMessageUnackedState(
//                                    it, VENDOR_MODEL_COMPANYIDENTIFIER)
                        )
                    }
                    if (message != null) {
                        MeshHelper.sendMessage("", group.address, message, null)
                    }
                }
            })
        }
    }

    /**
     * 订阅设备消息
     */
    fun getAllMeshDeviceStatusOneByOne(callback: StringCallback) {
        Utils.printLog(TAG, "===>-mesh- 开始 获取所有节点状态");
        getProvisionedNodes(object : ArrayMapCallback {
            override fun onResult(result: ArrayList<HashMap<String, Any>>) {
                result.forEach {
                    val productId = MxMeshUtil.getProductIdByUUID(it["uuid"] as String).toString();
                    var param: List<String?>? = null
                    when (productId) {
                        PRODUCT_ID_LIGHT_2,
                        PRODUCT_ID_LIGHT_5 -> {
                            param = listOf(SWITCH)
                        }
                        PRODUCT_ID_SOCKKET_SINGLE,
                        PRODUCT_ID_SOCKKET_DOBULE,
                        PRODUCT_ID_SOCKKET_TRIPLE -> {
                            param = listOf(SWITCH)
                        }
                        PRODUCT_ID_PIR_SENSOR -> {
                            param = listOf(REMAINING_ELECTRICITY)
                        }
                    }
                    getDeviceCurrentStatus(it["uuid"] as String,
                            param, object : StringCallback() {
                        override fun onResultMsg(msg: String) {
                            callback.onResultMsg(msg)
                        }
                    })
                }
            }
        })
    }

    /**
     * 清除手机所有mesh配置
     */
    fun clearAllMesh(booleanCallback: BooleanCallback?) {
        MeshHelper.MeshProxyService.mMeshProxyService?.deleteCurrentMeshNetwork(booleanCallback)
    }

    fun isScanning(): Boolean {
        if (null != MeshHelper.MeshProxyService.mMeshProxyService) {
            return MeshHelper.MeshProxyService.mMeshProxyService!!.isScanning()
        }
        return false
    }

    fun notifyAllDeviceOnline() {
        MeshHelper.MeshProxyService.mMeshProxyService?.notifyAllDeviceOnline()
    }

    fun notifyAllDeviceOffline() {
        MeshHelper.MeshProxyService.mMeshProxyService?.notifyAllDeviceOffline()
    }

    /**
     * 这里添加一个方法给默认的provisoner分配最大的单播地址范围
     */
    fun allocateDefaultProvisionerBigAddressRange() {
        MeshHelper.MeshProxyService.mMeshProxyService?.allocateDefaultProvisionerBigAddressRange()
    }

    /**
     * 设置gatt连接状态监听
     */
    fun setBleConnectStatusListener(bleStatusCallbacks: BleStatusCallbacks){
        MeshHelper.MeshProxyService.mMeshProxyService?.mNrfMeshManager
                ?.setBleConnectListener(bleStatusCallbacks)
    }

    /**
     * 获取蓝牙连接状态
     */
    fun getGattConnectStatus(): Int {
        return MeshHelper.MeshProxyService.mMeshProxyService?.mNrfMeshManager
                ?.bleMeshManager?.connectionState!!
    }

}