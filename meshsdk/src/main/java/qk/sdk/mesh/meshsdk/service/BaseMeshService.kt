package qk.sdk.mesh.meshsdk.service

import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.Observer
import com.google.gson.Gson
import no.nordicsemi.android.meshprovisioner.MeshManagerApi
import no.nordicsemi.android.meshprovisioner.MeshNetwork
import no.nordicsemi.android.meshprovisioner.NetworkKey
import no.nordicsemi.android.meshprovisioner.transport.*
import no.nordicsemi.android.meshprovisioner.utils.AuthenticationOOBMethods
import qk.sdk.mesh.meshsdk.MeshHandler
import qk.sdk.mesh.meshsdk.MeshHelper
import qk.sdk.mesh.meshsdk.MeshSDK
import qk.sdk.mesh.meshsdk.bean.*
import qk.sdk.mesh.meshsdk.callback.*
import qk.sdk.mesh.meshsdk.mesh.BleMeshManager
import qk.sdk.mesh.meshsdk.mesh.NrfMeshManager
import qk.sdk.mesh.meshsdk.util.*
import rx.Observable
import rx.Subscription
import rx.schedulers.Schedulers
import java.lang.RuntimeException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap
import qk.sdk.mesh.meshsdk.bean.DeviceConstantsCode as DC;

open class BaseMeshService : LifecycleService() {
    private val TAG = "BaseMeshService"
    private var isProvisioningStarted = false

    var mNrfMeshManager: NrfMeshManager? = null
    var mConnectCallback: ConnectCallback? = null
    var mScanCallback: ScanCallback? = null
    var mCurrentNetworkKey: NetworkKey? = null

    private val mHearBeanLock: Any = Any();

    companion object {
        //心跳OpCode
        private const val OPCODE_HEART_BEAT: Int = 0x14;

        const val DOWNSTREAM_CALLBACK = "subscribeStatus";

        //状态上报回调用，一个app从头到尾应该只会持有一个这样的回调用
        var mDownStreamCallback: IDeviceStatusCallBack? = null;
    }

    private var mHeatBeatSubscription: Subscription? = null;

    //心跳修改状态值记录
    var mHartBeanStatusMarkTime: Long = 0;

    //离线状态value 分为 00，01，10，11，低位为上次状态，高位为本次状态
    internal var mHeartBeatMap: ConcurrentHashMap<String?, Int?> =
            ConcurrentHashMap()


    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onCreate() {
        super.onCreate()
        mNrfMeshManager = NrfMeshManager(this, MeshManagerApi(this), BleMeshManager(this))
        setObserver()
    }

    val PERMISSION_BLUETOOTH_REQUEST_CODE = 1000
    val PERMISSION_BLUETOOTH_ADMIN_REQUEST_CODE = 1001
    val PERMISSION_ACCESS_FINE_LOCATION_REQUEST_CODE = 1002

    //开始扫描
    internal fun startScan(filterUuid: UUID, scanCallback: ScanCallback?, networkKey: String = "") {
        // 获取扫描状态结果
        mNrfMeshManager?.getScannerState()?.observe(this, Observer {
            if (!it.isBluetoothEnabled)
                mScanCallback?.onError(
                        CallbackMsg(
                                Constants.ConnectState.BLE_NOT_AVAILABLE.code,
                                Constants.ConnectState.BLE_NOT_AVAILABLE.msg
                        )
                )
        })

        var netKey: NetworkKey? = null
        if (networkKey.isNotEmpty()) {
            mNrfMeshManager?.meshManagerApi?.meshNetwork?.netKeys?.forEach {
                if (ByteUtil.bytesToHexString(it.key) == networkKey)
                    netKey = it
            }
        }

        scanCallback?.apply {
            mNrfMeshManager?.startScan(filterUuid, this, netKey)
        }
    }

    //停止扫描
    internal fun stopScan() {
        mScanCallback = null
        mNrfMeshManager?.stopScan()
        mNrfMeshManager?.getScannerResults()?.removeObservers(this)
        // 获取扫描状态结果
        mNrfMeshManager?.getScannerState()?.removeObservers(this)
    }

    internal fun stopConnect() {
        mConnectCallback = null
        mNrfMeshManager?.isDeviceReady?.removeObservers(this)
        mConnectCallback = null

    }

    internal fun getConnectingDevice(): ExtendedBluetoothDevice? {
        return mNrfMeshManager?.mConnectDevice
    }

    internal fun addConnectCallback(callback: ConnectCallback) {
        mConnectCallback = callback
        setConnectObserver()
    }

    //开始连接
    internal fun connect(
            device: ExtendedBluetoothDevice,
            connectToNetwork: Boolean, callback: ConnectCallback?
    ) {
        mConnectCallback = callback
        setConnectObserver()
        mNrfMeshManager?.connect(this, device, connectToNetwork)
    }

    /**
     * 移除心跳检查线程
     */
    fun stopHeatBeanCheck() {
        mHeatBeatSubscription?.unsubscribe();
        mHeatBeatSubscription = null;

    }

    /**
     * 开始心跳检查线程，这里只检查离线，在线放在上报的时候检查，这样可以加快上线的速度
     */
    fun startHeartBeatCheck() {
        synchronized(this) {
            if (mHeatBeatSubscription != null) return;

            val nodes = mNrfMeshManager?.nodes?.value
            if (nodes != null) {
                for (meshNode in nodes) {
                    mHeartBeatMap[meshNode.uuid.toUpperCase()] = 0;
                }
                mHeatBeatSubscription =
                        Observable.interval(0, 35 * 1000 + 100, TimeUnit.MILLISECONDS)
                                .subscribeOn(Schedulers.computation())
                                .subscribe {
                                    for (uuid in mHeartBeatMap.keys()) {
                                        synchronized(mHearBeanLock) {
                                            var heartBeanTag = mHeartBeatMap[uuid];
                                            if (heartBeanTag != null) {
                                                //设备一直离线，就让它离线吧
                                                if (heartBeanTag == 0) {
                                                    //do nothing
                                                }
                                                //设备离线了 10,feedback
                                                else if (heartBeanTag == 2) {
                                                    mHeartBeatMap[uuid] = heartBeanTag and 0
                                                    mDownStreamCallback?.onCommand(
                                                            Gson()
                                                                    .toJson(
                                                                            DeviceNode<Any>(
                                                                                    DeviceNode.STATUS_OFFLINE,
                                                                                    uuid
                                                                            )
                                                                    )
                                                    )
                                                }
                                                //设备上线了 01，feedback
                                                else if (heartBeanTag == 1) {
                                                    mHeartBeatMap[uuid] = heartBeanTag shl 1
                                                }
                                                //设备一直在线 11，改为10
                                                else if (heartBeanTag == 3) {
                                                    mHeartBeatMap[uuid] = heartBeanTag and 2
                                                    //记录一下修改时间，以鉴别状态为10，但是时间小于30s的时候
                                                    //还是认为是在线状态
                                                    mHartBeanStatusMarkTime = System.currentTimeMillis();
                                                }

                                            }
                                        }
                                    }
                                    checkUpdateHeartBeatNode();
                                }
            }
        }
    }

    /**
     * 检查节点是否有更新
     */
    fun checkUpdateHeartBeatNode() {
        val nodes = mNrfMeshManager?.nodes?.value
        if (nodes != null) {
            if (nodes.size != mHeartBeatMap.size) {
                mHeartBeatMap.clear();
                for (meshNode in nodes) {
                    mHeartBeatMap[meshNode.uuid.toUpperCase()] = 0;
                }
            }
        }
    }

    /**
     * 检查设备是否在线
     */
    private fun MeshMessage.checkDeviceOnline() {
        var uuid: String? = MeshHelper
                .getMeshNetwork()?.getNode(src)?.uuid?.toUpperCase();
        synchronized(mHearBeanLock) {
            //设备一直处于离线状态，赶紧通知它上线吧！
            if ((mHeartBeatMap[uuid] ?: 0) == 0) {
                mDownStreamCallback?.onCommand(
                        Gson()
                                .toJson(DeviceNode<Any>(DeviceNode.STATUS_ONLINE, uuid))
                )
            }
            //让高位为1,表示当前它肯定是在线的
            mHeartBeatMap[uuid] = (mHeartBeatMap[uuid] ?: 0) or 1;
        }
    }

    fun setConnectObserver() {
        Utils.printLog(TAG, "setConnectObserver")
        mNrfMeshManager?.isDeviceReady?.observe(this, Observer {
            if (mNrfMeshManager?.bleMeshManager?.isDeviceReady == true) {
                mConnectCallback?.onConnect()
            } else {
                Utils.printLog(
                        TAG, "connect result:" +
                        "${mNrfMeshManager?.bleMeshManager?.isDeviceReady}"
                )
            }
        })
        mNrfMeshManager?.connectionState?.observe(this, Observer {
            mConnectCallback?.onConnectStateChange(it)
            Utils.printLog(TAG, " mNrfMeshManager?.connectionState:${it.msg}")
            LogFileUtil.writeLogToInnerFile(
                    this@BaseMeshService,
                    "${it.msg}",
                    LogFileUtil.getInnerFileName(Constants.MESH_LOG_FILE_NAME)
            )
        })

        Utils.printLog(
                TAG,
                " mNrfMeshManager?.connectionState has active observers:${mNrfMeshManager?.connectionState?.hasActiveObservers()}"
        )

        mNrfMeshManager?.provisionedNodes?.observe(this, Observer {
            mConnectCallback?.onConnectStateChange(
                    CallbackMsg(
                            CommonErrorMsg.CONNECT_PROVISIONED_NODE_UPDATE.code,
                            CommonErrorMsg.CONNECT_PROVISIONED_NODE_UPDATE.msg
                    )
            )
        })
    }

    internal fun disConnect() {
        stopConnect()
        mNrfMeshManager?.unprovisionedMeshNode?.removeObservers(this)
//        mNrfMeshManager?.mExtendedMeshNode?.removeObservers(this)
        mNrfMeshManager?.disconnect()
        isProvisioningStarted = false
    }

//    internal fun clearMeshCallback() {
//        mNrfMeshManager?.meshMessageLiveData?.removeObservers(this)
//    }

    //开始provisioning
    internal fun startProvisioning(device: ExtendedBluetoothDevice, callback: BaseCallback) {
//        isProvisioningStarted = false
        mNrfMeshManager?.unprovisionedMeshNode?.observe(this, Observer {
            if (it != null) {
                var capibilities = it.provisioningCapabilities
                if (capibilities != null) {
                    var network = mNrfMeshManager?.meshNetworkLiveData?.meshNetwork
                    if (network != null) {
                        try {
                            val elementCount = capibilities.numberOfElements.toInt()
                            val provisioner = network.selectedProvisioner
                            val unicast =
                                    network.nextAvailableUnicastAddress(elementCount, provisioner)
                            network.assignUnicastAddress(unicast)
                            if (!isProvisioningStarted) {
                                var node = mNrfMeshManager?.unprovisionedMeshNode?.value
                                if (node != null && node.provisioningCapabilities.availableOOBTypes.size == 1 && node.provisioningCapabilities.availableOOBTypes[0] == AuthenticationOOBMethods.NO_OOB_AUTHENTICATION) {
                                    node.nodeName =
                                            mNrfMeshManager?.meshNetworkLiveData?.nodeName
                                    mNrfMeshManager?.meshManagerApi?.startProvisioning(node)
                                    Utils.printLog(TAG, "开始provisioning")
                                    isProvisioningStarted = true
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            //todo  记录日志
                            if (e.message == ERROR_MSG_UNICAST_UNABLED) {
                                callback.onError(
                                        CallbackMsg(
                                                CommonErrorMsg.PROVISION_UNICAST_UNABLED.code,
                                                CommonErrorMsg.PROVISION_UNICAST_UNABLED.msg
                                        )
                                )
                            }
                        }
                    }
                }
            }
        })
        mNrfMeshManager?.identifyNode(device)
    }

    //开始provisioning
    internal fun startProvisioning(
            device: ExtendedBluetoothDevice,
            networkKey: NetworkKey,
            callback: BaseCallback
    ) {
        mNrfMeshManager?.unprovisionedMeshNode?.observe(this, Observer {
            if (it != null) {
                var capibilities = it.provisioningCapabilities
                if (capibilities != null) {
                    var network = mNrfMeshManager?.meshNetworkLiveData?.meshNetwork
                    if (network != null) {
                        try {
                            val elementCount = capibilities.numberOfElements.toInt()
                            val provisioner = network.selectedProvisioner
                            val unicast =
                                    network.nextAvailableUnicastAddress(elementCount, provisioner)
                            network.assignUnicastAddress(unicast)
                            if (!isProvisioningStarted) {
                                var node = mNrfMeshManager?.unprovisionedMeshNode?.value
                                if (node != null && node.provisioningCapabilities.availableOOBTypes.size > 0
                                        && node.provisioningCapabilities.availableOOBTypes[0] == AuthenticationOOBMethods.NO_OOB_AUTHENTICATION
                                ) {
                                    node.nodeName =
                                            mNrfMeshManager?.meshNetworkLiveData?.nodeName
                                    mNrfMeshManager?.meshManagerApi?.startProvisioning(node)
                                    Utils.printLog(TAG, "开始provisioning")
                                    LogFileUtil.writeLogToInnerFile(
                                            this@BaseMeshService,
                                            "开始provisioning",
                                            LogFileUtil.getInnerFileName(Constants.MESH_LOG_FILE_NAME)
                                    )
                                    isProvisioningStarted = true
                                }
                            }
                        } catch (e: IllegalArgumentException) {
                            mNrfMeshManager?.meshManagerApi?.generateProvisioner()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            //todo  记录日志
                            if (e.message == ERROR_MSG_UNICAST_UNABLED) {
                                callback.onError(
                                        CallbackMsg(
                                                CommonErrorMsg.PROVISION_UNICAST_UNABLED.code,
                                                CommonErrorMsg.PROVISION_UNICAST_UNABLED.msg
                                        )
                                )
                            }
                        } finally {

                        }
                    }
                }
            }
        })
        mNrfMeshManager?.identifyNode(device, networkKey)
    }

    //获取provisioned nodes
    internal fun getProvisionedNodes(callback: ProvisionCallback) {
        if ((mNrfMeshManager?.nodes?.value?.size ?: 0) > 0) {
            callback.onProvisionedNodes(mNrfMeshManager?.nodes?.value!!)
        }
        mNrfMeshManager?.nodes?.observe(this, Observer {
            callback.onProvisionedNodes(it)
        })
    }

    internal fun deleteNode(node: ProvisionedMeshNode, callback: ProvisionCallback?) {
        var deleteResult =
                mNrfMeshManager?.meshNetworkLiveData?.meshNetwork?.deleteNode(node) ?: false
        callback?.onNodeDeleted(
                deleteResult, node
        )
    }

    internal fun setSelectedNode(node: ProvisionedMeshNode) {
        mNrfMeshManager?.setSelectedMeshNode(node)
    }

    internal fun getSelectedNode(): ProvisionedMeshNode? {
        return mNrfMeshManager?.mExtendedMeshNode
    }

    internal fun getMeshNetwork(): MeshNetwork? {
        return mNrfMeshManager?.meshNetworkLiveData?.meshNetwork
    }

    internal fun sendMeshPdu(
            method: String,
            dst: Int,
            message: MeshMessage,
            callback: BaseCallback?,
            timeOut: Boolean = false,
            retry: Boolean = false
    ) {
        Utils.printLog(TAG, "sendMeshPdu")
        mNrfMeshManager?.meshManagerApi?.createMeshPdu(dst, message)
        MeshHandler.addRunnable(MeshMsgSender(method, dst, message, callback, timeOut, retry))
    }

//    internal fun unRegisterMeshMsg() {
//        mNrfMeshManager?.meshMessageLiveData?.removeObservers(this)
//    }

    internal fun unRegisterConnectListener() {
//        mNrfMeshManager?.connectionState?.removeObservers(this)
        mConnectCallback = null
    }

    internal fun setSelectedModel(
            element: Element?,
            model: MeshModel?
    ) {
        mNrfMeshManager?.setSelectedElement(element)
        mNrfMeshManager?.setSelectedModel(model)
    }

    internal fun getSelectedModel(): MeshModel? {
        return mNrfMeshManager?.mSelectedModel
    }

    internal fun getSelectedElement(): Element? {
        return mNrfMeshManager?.mSelectedElement
    }

    internal fun isConnectedToProxy(): Boolean? {
        return mNrfMeshManager?.isConnected
    }

    internal fun setCurrentNetworkKey(networkKey: String) {
        MeshHelper.MeshProxyService.mMeshProxyService?.mNrfMeshManager?.meshNetworkLiveData?.networkKeys?.forEach {
            if (ByteUtil.bytesToHexString(it.key) == networkKey) {
                mCurrentNetworkKey = it
                it.isCurrent = 1
                LocalPreferences.setCurrentNetKey(networkKey)
            } else {
                it.isCurrent = 0
            }
            Utils.printLog(
                    TAG,
                    "setCurrentNetworkKey:${ByteUtil.bytesToHexString(it.key)},isCurrent:${it.isCurrent}"
            )
            MeshHelper.MeshProxyService.mMeshProxyService?.mNrfMeshManager?.meshManagerApi?.meshNetwork?.let { meshNetwork ->
                meshNetwork.updateNetKey(it)
            }
        }
    }

    internal fun getCurrentNetworkKey(): NetworkKey? {
        if (mCurrentNetworkKey != null)
            return mCurrentNetworkKey!!

        mNrfMeshManager?.meshNetworkLiveData?.networkKeys?.forEach {
            if (ByteUtil.bytesToHexString(it.key) == LocalPreferences.getCurrentNetKey()) {
                mCurrentNetworkKey = it
                return it
            }
        }

        return null
    }

    internal fun getCurrentNetworkKeyStr(): String? {
        if (mCurrentNetworkKey != null)
            return ByteUtil.bytesToHexString(mCurrentNetworkKey!!.key)
        else {
            var key = LocalPreferences.getCurrentNetKey()
            mNrfMeshManager?.meshNetworkLiveData?.networkKeys?.forEach {
                if (key == ByteUtil.bytesToHexString(it.key)) {
                    mCurrentNetworkKey = it
                    setCurrentNetworkKey(key)
                }
            }
            return key
        }
    }

    internal fun exportMeshNetwork(callback: NetworkExportUtils.NetworkExportCallbacks) {
        mNrfMeshManager?.exportMeshNetwork(callback)
    }

    internal fun importMeshNetworkJson(json: String, mapCallback: StringCallback) {
        mNrfMeshManager?.importMeshNetworkJson(json)
        mNrfMeshManager?.mNetworkImportState?.observe(this, Observer {
            mapCallback.onResultMsg(it)
        })
    }

    internal fun subscribeLightStatus(callback: MeshCallback) {
//        MeshHandler.addRunnable(SUBSCRIBE_VENDOR_MODEL, false, false, callback)
        MeshHandler.addRunnable(
                MeshMsgSender(
                        DOWNSTREAM_CALLBACK,
                        null,
                        null,
                        callback,
                        false,
                        false
                )
        )
    }

    internal fun unSubscribeLightStatus() {
//        MeshHandler.removeRunnable(DOWNSTREAM_CALLBACK)
    }

    /**
     * 1.监听livedata
     * 2.callback回调
     * 3.retry + timeout处理
     */
    internal fun setObserver() {
        //接收到的mesh消息
        mNrfMeshManager?.meshMessageLiveData?.observe(this, Observer { meshMsg ->
            meshMsg?.apply {
                Utils.printLog(
                        TAG, """===>[mesh] meshMessageLiveDataResult:
                        |${meshMsg}""".trimMargin()
                )

                //因为第一心跳包要在30S左右才能收到，为了尽快让设备上线，这是只要接受到设备的消息就当作上线算
                //原本需要---meshMsg.opCode ==OPCODE_HEART_BEAT
                checkDeviceOnline()

                //根据opCode分发消息
                dispatchMsgByOpCode(meshMsg)

                if (parameter.isNotEmpty() && parameter.size > 2) {
                    var connectCallbacksIterator = MeshSDK.mConnectCallbacks.iterator()
                    while (connectCallbacksIterator.hasNext()) {
                        var callbackIterator = connectCallbacksIterator.next()

                        if (callbackIterator.value is MapCallback
                                && meshMsg is VendorModelMessageStatus
                        ) {
                            var attrType =
                                    ByteUtil.bytesToHexString(byteArrayOf(parameter[1], parameter[2]))
                            when (attrType) {
                                ATTR_TYPE_COMMON_GET_QUADRUPLES -> {//获取四元组，pk、ps、dn、ds、pid
                                    decodeDevicePrimaryInfoAndFeedback(
                                            callbackIterator,
                                            connectCallbacksIterator
                                    )
                                }
                                else -> {
                                    var map = HashMap<String, Any>()
                                    map["params"] = ByteUtil.bytesToHexString(parameter)
                                    map["opcode"] = "${meshMsg.opCode}"
                                    meshMsg.mMessage.apply {
                                        if (meshMsg.mMessage is AccessMessage) {
                                            var pdus = (meshMsg.mMessage as AccessMessage).accessPdu
                                            Utils.printLog(
                                                    TAG,
                                                    "mesh msg opcode:${meshMsg.opCode},pus:${ByteUtil.bytesToHexString(
                                                            pdus
                                                    )}"
                                            )
                                            map["accessPDU"] = ByteUtil.bytesToHexString(pdus)
                                        }
                                    }
                                    if (callbackIterator.value is MapCallback) {
                                        (callbackIterator.value as MapCallback).onResult(map)
                                    }
                                    connectCallbacksIterator.remove()
                                }
                            }
                        }
                    }
                }
            }
        })
    }

    /**
     * 根据 opcode来进行消息分发
     */
    private fun dispatchMsgByOpCode(meshMsg: MeshMessage?) {
        if (meshMsg is VendorModelMessageStatus &&
                meshMsg.opCode == VENDOR_MSG_OPCODE_ATTR_RECEIVE.toInt(16)) {
            decodeMessageAndFeedBack(meshMsg);
        }

        if (meshMsg is ConfigAppKeyStatus &&
                meshMsg.opCode == VENDOR_MSG_ADD_APP_KEY.toInt(16)
        ) {
            (MeshHandler.getCallback(ADD_APPKEYS)
                    as MeshCallback).onReceive(meshMsg);
        }

        if (meshMsg is ConfigCompositionDataStatus &&
                meshMsg.opCode == VENDOR_MSG_GET_COMPOSITION_DATA.toInt(16)
        ) {
            (MeshHandler.getCallback(GET_COMPOSITION_DATA)
                    as MeshCallback).onReceive(meshMsg);
        }
    }

    fun decodeMessageAndFeedBack(message: VendorModelMessageStatus) {
        val uuid = MeshHelper.getMeshNetwork()?.getNode(message.src)?.uuid
        val pid: String? = uuid?.let { MxMeshUtil.getProductIdByUUID(it).toString() }
        val opCode = message.opCode
        //TODO 这里要区分一下回的是组播还是单播

        if (message.parameter.isEmpty() || message.parameter.size < 2) {
            throw RuntimeException("received message params can not be null")
        }
        Utils.printLog(
                TAG, "vendor msg:${ByteUtil.bytesToHexString(message.parameter)}"
        )
        //根据产品型号来解码对应的参数，参数以key&value&key&value形式来区分,key占两个字节，value占k个字节
        var res = "";
        when (pid) {
            DC.lightCons[PRODUCT_ID] -> {
                res = decodeLightParam(pid, uuid, message)
            }
            DC.socketCons[PRODUCT_ID] -> {
                res = decodeSocketParams(pid, uuid, message)
            }
            DC.pirSensorCons[PRODUCT_ID] -> {
                res = decodePirSensorParams(pid, uuid, message)
            }
        }
        val key = MeshHelper.generatePrimaryKey(uuid);
        val callback = MeshHandler.getCallback(key)

        if (null != callback) {
            if (callback is StringCallback) {
                callback.onResultMsg(res);
            }
            //为了兼容前同事的工作成果，和减少改动，这里暂时保留BoooleanCallBack
            if (callback is BooleanCallback) {
                callback.onResult(true);
            }
            MeshHandler.removeRunnable(key)
        }
    }

    /**
     * 解析Pir传感器参数
     */
    private fun decodePirSensorParams(
        pid: String?,
        uuid: String?,
        message: VendorModelMessageStatus
    ):String {
        var res = "";
        var k = 2;
        val pirSensor = PirSensor();
        pirSensor.productID = pid
        pirSensor.uuid = uuid
        for (i in message.parameter.indices step 2 + k) {
            val attrType: String = message.parameter[i].toString(16);
            val attrValue: String = message.parameter[i + 1].toString(16)
            when (attrType) {
                DC.pirSensorCons[BIO_SENSER] -> {
                    pirSensor.bioSenser =
                        if (attrValue == DC.CODE_SWITCH_ON) BIO_SENSER_ON else BIO_SENSER_OFF
                    k = 2
                }
                DC.pirSensorCons[REMAINING_ELECTRICITY] -> {
                    pirSensor.remainingElectricity = ""
                    k = 2
                }
                DC.pirSensorCons[SWITCH_THIRD] -> {
                    pirSensor.event = ""
                    k = 2
                }
                DC.pirSensorCons[EVENT] -> {
                    k = 2
                }
            }
        }
        res = Gson().toJson(pirSensor);
        return res;
    }

    /**
     * 解析插座，开关
     */
    private fun decodeSocketParams(
        pid: String?,
        uuid: String?,
        message: VendorModelMessageStatus
    ):String {
        var res = "";
        var k = 2
        val socketBean = SocketBean();
        socketBean.productID = pid
        socketBean.uuid = uuid
        for (i in message.parameter.indices step 2 + k) {
            val attrType: String = message.parameter[i].toString(16);
            val attrValue: String = message.parameter[i + 1].toString(16);
            when (attrType) {
                DC.socketCons[SWITCH] -> {
                    socketBean.switch =
                        if (attrValue == DC.CODE_SWITCH_ON) SWITCH_ON else SWITCH_OFF
                    k = 2
                }
                DC.socketCons[SWITCH_SECOND] -> {
                    socketBean.switchSecond =
                        if (attrValue == DC.CODE_SWITCH_ON) SWITCH_ON else SWITCH_OFF
                    k = 2
                }
                DC.socketCons[SWITCH_THIRD] -> {
                    socketBean.switchThird =
                        if (attrValue == DC.CODE_SWITCH_ON) SWITCH_ON else SWITCH_OFF
                    k = 2
                }
                DC.socketCons[EVENT] -> {
                    k = 2
                }
            }
        }
        res = Gson().toJson(socketBean);
        return res;
    }

    /**
     * 解析灯的参数
     */
    private fun decodeLightParam(
        pid: String?,
        uuid: String?,
        message: VendorModelMessageStatus
    ): String {
        var k = 2;
        var res = "";
        val lightBean = LightBean();
        lightBean.productID = pid
        lightBean.uuid = uuid

        for (i in 1 until message.parameter.size step 2 + k) {
            val attrType: String = ByteUtil.bytesToHexString(
                byteArrayOf(message.parameter[i], message.parameter[i + 1])
            )
            var attrValue: String = ""
            when (attrType) {
                DC.lightCons[SWITCH] -> {
                    //开关只占一个字节
                    attrValue = ByteUtil.bytesToHexString(
                        byteArrayOf(message.parameter[2 + i])
                    )

                    lightBean.switch =
                        if (attrValue == DC.CODE_SWITCH_ON) SWITCH_ON else SWITCH_OFF
                    k = 1;
                }
                DC.lightCons[COLOR] -> {
                    var h = ByteUtil.byteToShort(
                        byteArrayOf(
                            message.parameter[i + 1]
                            , message.parameter[i]
                        )
                    )
                    lightBean.color = 0
                    k = 3;
                }
                DC.lightCons[LIGHTNESS_LEVEL] -> {
                    lightBean.lightnessLevel = ByteUtil.byteArrayToInt(
                        byteArrayOf(
                            message.parameter[2 + i],
                            message.parameter[2 + i + 1]
                        )
                    )
                    k = 2
                }
                DC.lightCons[COLOR_TEMPERATURE] -> {
                    lightBean.colorTemperature = ByteUtil.byteArrayToInt(
                        byteArrayOf(
                            message.parameter[2 + i],
                            message.parameter[2 + i + 1]
                        )
                    )
                    k = 2
                }
                DC.lightCons[MODE_NUMBER] -> {
                    var l = message.parameter[i].toInt()
                    lightBean.modeNumber = ""
                    k = 2
                }
                DC.lightCons[EVENT] -> {

                }
            }
        }
        res = Gson().toJson(lightBean);
        return res
    }

    fun parseLightStatus(
            params: ByteArray,
            callback: MapCallback,
            map: HashMap<String, Any>
    ) {
//        var modeByte = params[0]
//        var modeBits = ByteUtil.byteTobitArray(modeByte)
//        var modeBitString = ByteUtil.byteTobitString(modeByte)
//        Utils.printLog(
//                TAG,
//                "mode Int:${modeByte.toInt()},modeBitString:$modeBitString,statuHex:${ByteUtil.bytesToHexString(
//                        params
//                )}"
//        )
//        var mode = ByteUtil.byteToShort(byteArrayOf(modeBits[6], modeBits[5])).toInt()
//        var isOn = modeBits[7].toInt()
//
//        var h = ByteUtil.byteToShort(
//                byteArrayOf(
//                        params[2],
//                        params[1]
//                )
//        )
//        var s = params[3].toInt()
//        var v = params[4].toInt()
//        var b = params[5].toInt()
//        var t = ByteUtil.byteArrayToInt(
//                byteArrayOf(
//                        0x00,
//                        0x00,
//                        params[6],
//                        params[7]
//                )
//        )
//        Utils.printLog(TAG, "h:$h,s:$s,v:$v,b:$b,t:$t")
//        map["code"] = 200
//        var lightStatus = HashMap<String, Any>()
//
//        var switchMap = HashMap<String, Int>()
//        switchMap["0"] = isOn

//        lightStatus["LightMode"] = mode
//        lightStatus["Brightness"] = b
//        lightStatus["ColorTemperature"] = t
//        lightStatus["OnOffSwitch"] = switchMap

//        var HSVColor = HashMap<String, Int>()
//        HSVColor["Hue"] = h.toInt()
//        HSVColor["Saturation"] = s
//        HSVColor["Value"] = v
//        lightStatus["HSVColor"] = HSVColor
//        map["data"] = lightStatus


//        if (callback is MapCallback) {
//            MeshSDK.doMapCallback(
//                    map,
//                    callback,
//                    CallbackMsg(
//                            Constants.ConnectState.COMMON_SUCCESS.code,
//                            Constants.ConnectState.COMMON_SUCCESS.msg
//                    )
//            )
//        }
    }


//        if (!MeshHelper.isConnectedToProxy()) {
//            Utils.printLog(TAG, "disconnect")
//            MeshSDK.doVendorCallback(
//                callback, false,
//                CallbackMsg(
//                    CommonErrorMsg.DISCONNECTED.code,
//                    CommonErrorMsg.DISCONNECTED.msg
//                )
//            )
//        }
//        Utils.printLog(
//            TAG,
//            "send opcode:$opcode,param:${String(msg.parameter)}"
//        )

//        when (opcode) {
//            "02" -> {//重启网关
//                if (callback is BooleanCallback) {
//                    callback.onResuslt(true)
//                }
//
//                MeshSDK.mConnectCallbacks.remove(if (key.isEmpty()) "sendMeshMessage" else key)
//            }
//            "04" -> {//set cwrgb
//                if (callback is MapCallback && msg.parameter.size == 5) {
//                    var map = HashMap<String, Any>()
//
//                    var c = msg.parameter[0].toInt()
//                    var w = msg.parameter[1].toInt()
//                    var r = msg.parameter[2].toInt()
//                    var g = msg.parameter[3].toInt()
//                    var b = msg.parameter[4].toInt()
//                    map.put("c", c)
//                    map.put("w", w)
//                    map.put("r", r)
//                    map.put("g", g)
//                    map.put("b", b)
//                    map.put(
//                        "isOn",
//                        if (c == 0 && w == 0 && r == 0 && g == 0 && b == 0) false else true
//                    )
//                    g.onResult(map)
//                    MeshSDK.mConnectCallbacks.remove(if (key.isEmpty()) "sendMeshMessage" else key)
//                    msgIndex = 0
//                } else {
//                    //todo log
//                }
//            }
//            "05" -> {//get cwrgb
//                if (msgIndex < 0 && callback is BooleanCallback) {
//                    callback.onResult(true)
//                    MeshSDK.mConnectCallbacks.remove(if (key.isEmpty()) "sendMeshMessage" else key)
//                    msgIndex = 0
//                } else {
//                    //todo log
//                }
//            }
//            "0D", "0E", "0F", "11" -> {//set HSV
//                if (msgIndex < 0 && callback is BooleanCallback) {
//                    callback.onResult(true)
//
//                    MeshSDK.mConnectCallbacks.remove(if (key.isEmpty()) "sendMeshMessage" else key)
//                    msgIndex = 0
//                } else {
//                    //todo log
//                }
//            }
//            else -> {
//
//            }
//        }

//            override fun onError(msg: CallbackMsg) {
//                MeshSDK.doVendorCallback(callback, false, msg)
//            }
//}

    /**
     * 解析四元组
     */
    private fun MeshMessage.decodeDevicePrimaryInfoAndFeedback(
            callbackIterator: MutableMap.MutableEntry<String, Any>,
            connectCallbacksIterator: MutableIterator<MutableMap.MutableEntry<String, Any>>
    ) {
        Utils.printLog(
                TAG,
                "quadruple size:${parameter.size} ,content：${String(
                        parameter
                )}"
        )

        if (parameter.size >= 40 && callbackIterator.key == MeshSDK.CALLBACK_GET_IDENTITY) {
            var preIndex = 3
            var quadrupleIndex = 0
            var map = HashMap<String, Any>()
            for (index in 3 until parameter.size) {
                if (parameter[index] == 0x20.toByte() || index == parameter.size - 1) {
                    when (quadrupleIndex) {
                        0 -> {//pk
                            var pkBytes =
                                    ByteArray(index - preIndex)
                            System.arraycopy(
                                    parameter,
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
                                    parameter,
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
                                    parameter,
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
                                    parameter,
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
                                    parameter,
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
                    Constants.ConnectState.COMMON_SUCCESS.code
            )
            map.forEach { (t, u) ->
                Log.e(TAG, "key:$t,value:$u")
            }
            (callbackIterator.value as MapCallback).onResult(map)
            MeshHandler.removeRunnable(MeshSDK.CALLBACK_GET_IDENTITY)
            connectCallbacksIterator.remove()
        } else {
            //todo log
        }
    }

    internal fun clearGatt() {
        mNrfMeshManager?.clearGatt()
    }
}
