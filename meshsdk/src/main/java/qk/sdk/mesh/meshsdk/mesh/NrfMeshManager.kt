package qk.sdk.mesh.meshsdk.mesh

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.ParcelUuid
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

import java.io.File

//import no.nordicsemi.android.log.Logger
import no.nordicsemi.android.meshprovisioner.ApplicationKey
import no.nordicsemi.android.meshprovisioner.Group
import no.nordicsemi.android.meshprovisioner.MeshManagerApi
import no.nordicsemi.android.meshprovisioner.MeshManagerCallbacks
import no.nordicsemi.android.meshprovisioner.MeshNetwork
import no.nordicsemi.android.meshprovisioner.MeshProvisioningStatusCallbacks
import no.nordicsemi.android.meshprovisioner.MeshStatusCallbacks
import no.nordicsemi.android.meshprovisioner.NetworkKey
import no.nordicsemi.android.meshprovisioner.Provisioner
import no.nordicsemi.android.meshprovisioner.UnprovisionedBeacon
import no.nordicsemi.android.meshprovisioner.models.SigModelParser
import no.nordicsemi.android.meshprovisioner.provisionerstates.ProvisioningState
import no.nordicsemi.android.meshprovisioner.provisionerstates.UnprovisionedMeshNode
import no.nordicsemi.android.meshprovisioner.utils.MeshAddress
import qk.sdk.mesh.meshsdk.bean.provision.ProvisioningStatusLiveData
import qk.sdk.mesh.meshsdk.bean.provision.TransactionStatus

import no.nordicsemi.android.meshprovisioner.MeshManagerApi.MESH_PROXY_UUID
import no.nordicsemi.android.meshprovisioner.transport.*
import no.nordicsemi.android.meshprovisioner.utils.ByteUtil
import no.nordicsemi.android.support.v18.scanner.*
import qk.sdk.mesh.meshsdk.bean.*
import qk.sdk.mesh.meshsdk.bean.scan.ScannerLiveData
import qk.sdk.mesh.meshsdk.bean.scan.ScannerStateLiveData
import qk.sdk.mesh.meshsdk.util.*
import qk.sdk.mesh.meshsdk.util.Constants.ConnectState.*
import java.util.*
import kotlin.collections.ArrayList

class NrfMeshManager(
    internal var mContext: Context,
    internal val meshManagerApi: MeshManagerApi,
    internal var bleMeshManager: BleMeshManager?
) : MeshProvisioningStatusCallbacks, MeshStatusCallbacks, MeshManagerCallbacks,
    BleMeshManagerCallbacks {
    private val TAG = "NrfMeshManager"

    // Connection States Connecting, Connected, Disconnecting, Disconnected etc.
    private val mIsConnectedToProxy = MutableLiveData<Boolean>()

    // Live data flag containing connected state.
    private var mIsConnected: Boolean = false

    // LiveData to notify when device is ready
    private val mOnDeviceReady = MutableLiveData<Void>()

    // Updates the connection state while connecting to a peripheral
    private val mConnectionState = MutableLiveData<CallbackMsg>()

    // Flag to determine if a reconnection is in the progress when provisioning has completed
    private val mIsReconnecting = SingleLiveData<Boolean>()
    private val mUnprovisionedMeshNodeLiveData = MutableLiveData<UnprovisionedMeshNode>()
    private val mProvisionedMeshNodeLiveData = MutableLiveData<ProvisionedMeshNode>()
    private val mConnectedProxyAddress = SingleLiveData<Int>()

    internal var isProvisioningComplete = false
        private set // Flag to determine if provisioning was completed

    // Holds the selected MeshNode to configure
    internal var mExtendedMeshNode: ProvisionedMeshNode? = null

    // Holds the selected Element to configure
    internal var mSelectedElement: Element? = null

    // Holds the selected mesh model to configure
    internal var mSelectedModel: MeshModel? = null

    // Holds the selected app key to configure
    private val mSelectedNetKey = MutableLiveData<NetworkKey>()

    // Holds the selected app key to configure
    private val mSelectedAppKey = MutableLiveData<ApplicationKey>()

    // Holds the selected provisioner when adding/editing
    private val mSelectedProvisioner = MutableLiveData<Provisioner>()

    private val mSelectedGroupLiveData = MutableLiveData<Group>()

    // Composition data status
    internal val mCompositionDataStatus = SingleLiveData<ConfigCompositionDataStatus>()

    // App key add status
    internal val mAppKeyStatus = SingleLiveData<ConfigAppKeyStatus>()

    //Contains the MeshNetwork
    internal val meshNetworkLiveData = MeshNetworkLiveData()
    public val mNetworkImportState = MutableLiveData<String>()
    private val mMeshMessageLiveData = MutableLiveData<MeshMessage>()

    // Contains the provisioned nodes
    private val mProvisionedNodes = MutableLiveData<ArrayList<ProvisionedMeshNode>>()
    //    private final MutableLiveData<ProvisionedMeshNode> mSelectedProvisionedNode = new MutableLiveData<>();

    internal val mGroups = MutableLiveData<ArrayList<Group>>()

    private val mTransactionStatus = SingleLiveData<TransactionStatus>()

    /**
     * Returns the ble mesh manager
     *
     * @return [BleMeshManager]
     */
//    internal var bleMeshManager: BleMeshManager? = null
//        private set
    private val mHandler: Handler
    private var mUnprovisionedMeshNode: UnprovisionedMeshNode? = null
    private var mProvisionedMeshNode: ProvisionedMeshNode? = null
    private var mIsReconnectingFlag: Boolean = false
    private var mIsScanning: Boolean = false
    private var mSetupProvisionedNode: Boolean = false
    internal var provisioningState: ProvisioningStatusLiveData? = null
        private set
    private var mMeshNetwork: MeshNetwork? = null
    internal var isCompositionDataStatusReceived: Boolean = false
        private set
    internal var isDefaultTtlReceived: Boolean = false
        private set
    internal var isAppKeyAddCompleted: Boolean = false
        private set
    internal var isNetworkRetransmitSetCompleted: Boolean = false
        private set
    private val uri: Uri? = null

    internal var mConnectDevice: ExtendedBluetoothDevice? = null

    /**
     * scan
     */

    private var mNetworkId: String? = null

    /**
     * MutableLiveData containing the scanner state to notify MainActivity.
     */
    private var mScannerLiveData: ScannerLiveData = ScannerLiveData()
    private var mScannerStateLiveData: ScannerStateLiveData =
        ScannerStateLiveData(Utils.isBleEnabled, Utils.isLocationEnabled(mContext))

    private var mFilterUuid: UUID? = null

    private val mReconnectRunnable = Runnable { this.startScan() }

    private val mScannerTimeout = {
        stopScan()
        mIsReconnecting.postValue(false)
    }

    /**
     * Returns [SingleLiveData] containing the device ready state.
     */
    internal val isDeviceReady: LiveData<Void>
        get() = mOnDeviceReady

    /**
     * Returns [SingleLiveData] containing the device ready state.
     */
    internal val connectionState: LiveData<CallbackMsg>
        get() = mConnectionState

    /**
     * Returns [SingleLiveData] containing the device ready state.
     */
    internal val isConnected: Boolean
        get() = mIsConnected

    /**
     * Returns [SingleLiveData] containing the device ready state.
     */
    internal val isConnectedToProxy: LiveData<Boolean>
        get() = mIsConnectedToProxy

    internal val isReconnecting: LiveData<Boolean>
        get() = mIsReconnecting

    internal val nodes: LiveData<ArrayList<ProvisionedMeshNode>>
        get() = mProvisionedNodes

    internal val networkLoadState: LiveData<String>
        get() = mNetworkImportState

    internal val transactionStatus: LiveData<TransactionStatus>
        get() = mTransactionStatus

    internal val provisionedNodes: LiveData<ProvisionedMeshNode>
        get() = mProvisionedMeshNodeLiveData

    /**
     * Returns the [MeshMessageLiveData] live data object containing the mesh message
     */
    internal val meshMessageLiveData: LiveData<MeshMessage>
        get() = mMeshMessageLiveData

    internal val selectedGroup: LiveData<Group>
        get() = mSelectedGroupLiveData

    internal val unprovisionedMeshNode: LiveData<UnprovisionedMeshNode>
        get() = mUnprovisionedMeshNodeLiveData

    internal val connectedProxyAddress: LiveData<Int>
        get() = mConnectedProxyAddress

    /**
     * Returns the selected mesh node
     */
//    internal val selectedMeshNode: LiveData<ProvisionedMeshNode>
//        get() = mExtendedMeshNode

    /**
     * Returns the selected element
     */
//    internal val selectedElement: Element?
//        get() = mSelectedElement

    /**
     * Returns the selected mesh model
     */
    internal val selectedAppKey: LiveData<ApplicationKey>
        get() = mSelectedAppKey

    /**
     * Returns the selected [Provisioner]
     */
    internal val selectedProvisioner: LiveData<Provisioner>
        get() = mSelectedProvisioner

    /**
     * Returns the selected mesh model
     */
//    internal val selectedModel: LiveData<MeshModel>
//        get() = mSelectedModel

    init {
        this.meshManagerApi.setMeshManagerCallbacks(this)
        this.meshManagerApi.setProvisioningStatusCallbacks(this)
        this.meshManagerApi.setMeshStatusCallbacks(this)
        this.meshManagerApi.loadMeshNetwork()
        //Initialize the ble manager
//        this.bleMeshManager = bleMeshManager
        this.bleMeshManager?.setGattCallbacks(this)
        mHandler = Handler()
    }//Initialize the mesh api

    internal fun clearInstance() {
        bleMeshManager = null
    }

    /**
     * Clears the transaction status
     */
    internal fun clearTransactionStatus() {
        if (mTransactionStatus.value != null) {
            mTransactionStatus.postValue(null)
        }
    }

    /**
     * Reset mesh network
     */
    internal fun resetMeshNetwork() {
        disconnect()
        meshManagerApi.resetMeshNetwork()
    }

    /**
     * Connect to peripheral
     *
     * @param context          Context
     * @param device           [ExtendedBluetoothDevice] device
     * @param connectToNetwork True if connecting to an unprovisioned node or proxy node
     */
    internal fun connect(
        device: ExtendedBluetoothDevice,
        connectToNetwork: Boolean
    ) {
        meshNetworkLiveData.nodeName = device.name
        isProvisioningComplete = false
        isCompositionDataStatusReceived = false
        isDefaultTtlReceived = false
        isAppKeyAddCompleted = false
        isNetworkRetransmitSetCompleted = false
        mConnectDevice = device
        val bluetoothDevice = device.device
        Utils.printLog(TAG, "connect $connectToNetwork")
        initIsConnectedLiveData()
        mConnectionState.postValue(
            CallbackMsg(
                CONNECTING.code,
                CONNECTING.msg
            )
        )
        //Added a 1 second delay for connection, mostly to wait for a disconnection to complete before connecting.
        if (bluetoothDevice != null) {
            mHandler.postDelayed({
                bleMeshManager?.connect(bluetoothDevice)?.retry(3, 200)?.enqueue()
            }, 1000)
        } else {
            //todo 记录日志
        }
    }

    /**
     * Connect to peripheral
     *
     * @param device bluetooth device
     */
    private fun connectToProxy(device: ExtendedBluetoothDevice) {
        initIsConnectedLiveData()
        mConnectionState.postValue(
            CallbackMsg(
                CONNECTING.code,
                CONNECTING.msg
            )
        )
        if (device.device != null) {
            bleMeshManager?.connect(device.device!!)?.retry(3, 200)?.enqueue()
        } else {
            //todo 记录日志

        }
    }

    private fun initIsConnectedLiveData() {
        mIsConnected = false
    }

    /**
     * Disconnects from peripheral
     */
    internal fun disconnect() {
        clearProvisioningLiveData()
        isProvisioningComplete = false
        bleMeshManager?.disconnect()?.enqueue()
    }

    internal fun clearProvisioningLiveData() {
        mHandler.removeCallbacks(mReconnectRunnable)
        mSetupProvisionedNode = false
        mIsReconnectingFlag = false
        mUnprovisionedMeshNodeLiveData.value = null
        mProvisionedMeshNodeLiveData.value = null
    }

    private fun removeCallbacks() {
        mHandler.removeCallbacksAndMessages(null)
    }

    fun identifyNode(device: ExtendedBluetoothDevice) {
        val beacon = device.beacon as UnprovisionedBeacon?
        if (beacon != null) {
            meshManagerApi.identifyNode(beacon.uuid, ATTENTION_TIMER)
        } else if (device.scanResult != null) {
            val serviceData =
                Utils.getServiceData(device.scanResult!!, BleMeshManager.MESH_PROVISIONING_UUID)
            if (serviceData != null) {
                val uuid = meshManagerApi.getDeviceUuid(serviceData)
                meshManagerApi.identifyNode(uuid, ATTENTION_TIMER)
            }
        }
    }

    fun identifyNode(device: ExtendedBluetoothDevice, networkKey: NetworkKey) {
        if (device.beacon is UnprovisionedBeacon) {
            val beacon = device.beacon as UnprovisionedBeacon
            meshManagerApi.identifyNode(beacon.uuid, ATTENTION_TIMER, networkKey)
        } else {
            val serviceData =
                Utils.getServiceData(device.scanResult!!, BleMeshManager.MESH_PROVISIONING_UUID)
            if (serviceData != null) {
                val uuid = meshManagerApi.getDeviceUuid(serviceData)
                meshManagerApi.identifyNode(uuid, ATTENTION_TIMER, networkKey)
            }
        }
    }

    private fun clearExtendedMeshNode() {
        mExtendedMeshNode = null
    }

    /**
     * Sets the mesh node to be configured
     *
     * @param node provisioned mesh node
     */
    internal fun setSelectedMeshNode(node: ProvisionedMeshNode) {
        mExtendedMeshNode = (node)
    }

    /**
     * Set the selected [Element] to be configured
     *
     * @param element element
     */
    internal fun setSelectedElement(element: Element?) {
        mSelectedElement = element
    }

    /**
     * Set the selected model to be configured
     *
     * @param appKey mesh model
     */
    internal fun setSelectedAppKey(appKey: ApplicationKey) = mSelectedAppKey.postValue(appKey)

    /**
     * Selects provisioner for editing or adding
     *
     * @param provisioner [Provisioner]
     */
    internal fun setSelectedProvisioner(provisioner: Provisioner) =
        mSelectedProvisioner.postValue(provisioner)

    /**
     * Set the selected model to be configured
     *
     * @param model mesh model
     */
    internal fun setSelectedModel(model: MeshModel?) {
        mSelectedModel = model
    }

    override fun onDataReceived(bluetoothDevice: BluetoothDevice, mtu: Int, pdu: ByteArray) {
        meshManagerApi.handleNotifications(mtu, pdu)
//        Utils.printLog(TAG, "onDataReceived:${ByteUtil.bytesToHexString(pdu)}")
    }

    override fun onDataSent(device: BluetoothDevice, mtu: Int, pdu: ByteArray) {
        meshManagerApi.handleWriteCallbacks(mtu, pdu)
    }

    override fun onDeviceConnecting(device: BluetoothDevice) {
        mConnectionState.postValue(
            CallbackMsg(
                CONNECTING.code,
                CONNECTING.msg
            )
        )
    }

    override fun onDeviceConnected(device: BluetoothDevice) {
        mIsConnected = true
        mConnectionState.postValue(
            CallbackMsg(
                DISCOVERING_SERVICE.code,
                DISCOVERING_SERVICE.msg
            )
        )
        mIsConnectedToProxy.postValue(true)
    }

    override fun onDeviceDisconnecting(device: BluetoothDevice) {
        Utils.printLog(
            TAG,
            "Disconnecting...，connect devicd:${mConnectDevice?.getAddress()},disConnect device:${device.address}"
        )
        mIsConnectedToProxy.postValue(false)
        mIsConnected = false
        if (mIsReconnectingFlag) {
            mConnectionState.postValue(
                CallbackMsg(
                    RECONNETCING.code,
                    RECONNETCING.msg
                )
            )
        } else {
            mConnectionState.postValue(
                CallbackMsg(
                    DISCONNECTING.code,
                    DISCONNECTING.msg
                )
            )
        }
    }

    override fun onDeviceDisconnected(device: BluetoothDevice) {
        Utils.printLog(TAG, "Disconnected")
        mConnectionState.postValue(
            CallbackMsg(
                DISCONNECTED.code,
                DISCONNECTED.msg
            )
        )
        if (mIsReconnectingFlag) {
            mIsReconnectingFlag = false
            mIsReconnecting.postValue(false)
            mIsConnected = false
            mIsConnectedToProxy.postValue(false)
        } else {
            mIsConnected = false
            mIsConnectedToProxy.postValue(false)
            if (mConnectedProxyAddress.value != null) {
                val network = meshManagerApi.meshNetwork
                network!!.proxyFilter = null

            }
            //clearExtendedMeshNode();
        }
        mSetupProvisionedNode = false
        mConnectedProxyAddress.postValue(null)
    }

    override fun onLinkLossOccurred(device: BluetoothDevice) {
        Utils.printLog(TAG, "Link loss occurred")
        mIsConnected = false
    }

    override fun onServicesDiscovered(device: BluetoothDevice, optionalServicesFound: Boolean) {
        mConnectionState.postValue(
            CallbackMsg(
                INITIALIZING.code,
                INITIALIZING.msg
            )
        )
    }

    override fun onDeviceReady(device: BluetoothDevice) {
        mOnDeviceReady.postValue(null)
        mConnectionState.postValue(CallbackMsg(COMMON_SUCCESS.code, COMMON_SUCCESS.msg))

        if (bleMeshManager!!.isProvisioningComplete) {
            if (mSetupProvisionedNode) {
                if (mMeshNetwork!!.selectedProvisioner!!.provisionerAddress != null) {
                    mHandler.postDelayed({
                        //Adding a slight delay here so we don't send anything before we receive the mesh beacon message
                        val node = mProvisionedMeshNodeLiveData.value
                        if (node != null) {
                            val compositionDataGet = ConfigCompositionDataGet()
                            meshManagerApi.createMeshPdu(node.unicastAddress, compositionDataGet)
                        }
                    }, 2000)
                } else {
                    mSetupProvisionedNode = false
                    provisioningState?.onMeshNodeStateUpdated(ProvisionerStates.PROVISIONER_UNASSIGNED)
                    clearExtendedMeshNode()
                }
            }
            mIsConnectedToProxy.postValue(true)
        }
    }

    override fun onBondingRequired(device: BluetoothDevice) {
        // Empty.
    }

    override fun onBonded(device: BluetoothDevice) {
        // Empty.
    }

    override fun onBondingFailed(device: BluetoothDevice) {
        // Empty.
    }

    override fun onError(device: BluetoothDevice, message: String, errorCode: Int) {
        Utils.printLog(TAG, message + " (code: " + errorCode + "), device: " + device.address)
        if (errorCode == 133) {
            bleMeshManager?.clearGatt()
        }
        mConnectionState.postValue(CallbackMsg(errorCode, message))
    }

    override fun onDeviceNotSupported(device: BluetoothDevice) {

    }

    override fun onNetworkLoaded(meshNetwork: MeshNetwork) {
        loadNetwork(meshNetwork)
        loadGroups()
    }

    override fun onNetworkUpdated(meshNetwork: MeshNetwork) {
        loadNetwork(meshNetwork)
        loadGroups()
        updateSelectedGroup()
    }

    override fun onNetworkLoadFailed(error: String) {
        mNetworkImportState.postValue(error)
    }

    override fun onNetworkImported(meshNetwork: MeshNetwork) {
        //We can delete the old network after the import has been successful!
        //But let's make sure we don't delete the same network in case someone imports the same network ;)
        val oldNet = mMeshNetwork
        if (oldNet!!.meshUUID != meshNetwork.meshUUID) {
            meshManagerApi.deleteMeshNetworkFromDb(oldNet)
        }
        loadNetwork(meshNetwork)
        loadGroups()
//        mNetworkImportState.postValue(
//            meshNetwork.meshName + " has been successfully imported.\n" +
//                    "In order to start sending messages to this network, please change the provisioner address. " +
//                    "Using the same provisioner address will cause messages to be discarded due to the usage of incorrect sequence numbers " +
//                    "for this address. However if the network does not contain any nodes you do not need to change the address"
//        )
        mNetworkImportState.postValue(COMMON_SUCCESS.msg)
    }

    override fun onNetworkImportFailed(error: String) {
        mNetworkImportState.postValue(error)
    }

    override fun sendProvisioningPdu(meshNode: UnprovisionedMeshNode, pdu: ByteArray) {
        bleMeshManager?.sendPdu(pdu)
    }

    override fun onMeshPduCreated(pdu: ByteArray) {
        bleMeshManager?.sendPdu(pdu)
    }

    override fun getMtu(): Int {
        return bleMeshManager?.getMaximumPacketSize() ?: 20
    }

    @Synchronized
    override fun onProvisioningStateChanged(
        meshNode: UnprovisionedMeshNode,
        state: ProvisioningState.States,
        data: ByteArray
    ) {
        mUnprovisionedMeshNode = meshNode
        mUnprovisionedMeshNodeLiveData.postValue(meshNode)
        when (state) {
            ProvisioningState.States.PROVISIONING_INVITE -> provisioningState =
                ProvisioningStatusLiveData()
            ProvisioningState.States.PROVISIONING_FAILED -> isProvisioningComplete = false
            else -> {
            }
        }
        provisioningState?.onMeshNodeStateUpdated(ProvisionerStates.fromStatusCode(state.state))
        Utils.printLog(TAG, "provisioningState:${state.state}")
    }

    override fun onProvisioningFailed(
        meshNode: UnprovisionedMeshNode,
        state: ProvisioningState.States,
        data: ByteArray
    ) {
        mUnprovisionedMeshNode = meshNode
        mUnprovisionedMeshNodeLiveData.postValue(meshNode)
        if (state == ProvisioningState.States.PROVISIONING_FAILED) {
            isProvisioningComplete = false
        }
        provisioningState?.onMeshNodeStateUpdated(ProvisionerStates.fromStatusCode(state.state))
    }

    override fun onProvisioningCompleted(
        meshNode: ProvisionedMeshNode,
        state: ProvisioningState.States,
        data: ByteArray
    ) {
        mProvisionedMeshNode = meshNode
        mExtendedMeshNode = meshNode
        mUnprovisionedMeshNodeLiveData.postValue(null)
        mProvisionedMeshNodeLiveData.postValue(meshNode)
        if (state == ProvisioningState.States.PROVISIONING_COMPLETE) {
            onProvisioningCompleted(meshNode)
        }
        provisioningState?.onMeshNodeStateUpdated(ProvisionerStates.fromStatusCode(state.state))
    }

    private fun onProvisioningCompleted(node: ProvisionedMeshNode) {
        isProvisioningComplete = true
        mProvisionedMeshNode = node
        mIsReconnecting.postValue(true)
//        bleMeshManager!!.disconnect().enqueue()
        Utils.printLog(TAG, "onProvisioningCompleted disconnect ")
        mConnectionState.postValue(CallbackMsg(PROVISION_SUCCESS.code, PROVISION_SUCCESS.msg))
        loadNodes()
//        mHandler.post { mConnectionState.postValue("Scanning for provisioned node") }
//        mHandler.postDelayed(
//            mReconnectRunnable,
//            1000
//        ) //Added a slight delay to disconnect and refresh the cache
    }

    /**
     * Here we load all nodes except the current provisioner. This may contain other provisioner nodes if available
     */
    private fun loadNodes() {
        val nodes = ArrayList<ProvisionedMeshNode>()
        mMeshNetwork?.nodes?.apply {
            for (node in this) {
                if (!node.uuid.equals(
                        mMeshNetwork?.selectedProvisioner?.provisionerUuid,
                        ignoreCase = true
                    )
                ) {
                    nodes.add(node)
                }
            }
            mProvisionedNodes.postValue(nodes)
        }
    }

    override fun onTransactionFailed(dst: Int, hasIncompleteTimerExpired: Boolean) {
        mProvisionedMeshNode = mMeshNetwork!!.getNode(dst)
        mTransactionStatus.postValue(TransactionStatus(dst, hasIncompleteTimerExpired))
    }

    override fun onUnknownPduReceived(src: Int, accessPayload: ByteArray) {
        val node = mMeshNetwork!!.getNode(src)
        if (node != null) {
            mProvisionedMeshNode = node
            updateNode(node)
        }
    }

    override fun onBlockAcknowledgementProcessed(dst: Int, message: ControlMessage) {
        val node = mMeshNetwork?.getNode(dst)
        if (node != null) {
            mProvisionedMeshNode = node
            if (mSetupProvisionedNode) {
                mProvisionedMeshNodeLiveData.postValue(mProvisionedMeshNode)
                provisioningState?.onMeshNodeStateUpdated(ProvisionerStates.SENDING_BLOCK_ACKNOWLEDGEMENT)
            }
        }
    }

    override fun onBlockAcknowledgementReceived(src: Int, message: ControlMessage) {
        val node = mMeshNetwork?.getNode(src)
        if (node != null) {
            mProvisionedMeshNode = node
            if (mSetupProvisionedNode) {
                mProvisionedMeshNodeLiveData.postValue(node)
                provisioningState?.onMeshNodeStateUpdated(ProvisionerStates.BLOCK_ACKNOWLEDGEMENT_RECEIVED)
            }
        }
    }

    override fun onMeshMessageProcessed(dst: Int, meshMessage: MeshMessage) {
        val node = mMeshNetwork?.getNode(dst)
        if (node != null) {
            mProvisionedMeshNode = node
            if (meshMessage is ConfigCompositionDataGet) {
                if (mSetupProvisionedNode) {
                    mProvisionedMeshNodeLiveData.postValue(node)
                    provisioningState?.onMeshNodeStateUpdated(ProvisionerStates.COMPOSITION_DATA_GET_SENT)
                }
            } else if (meshMessage is ConfigDefaultTtlGet) {
                if (mSetupProvisionedNode) {
                    mProvisionedMeshNodeLiveData.postValue(node)
                    provisioningState?.onMeshNodeStateUpdated(ProvisionerStates.SENDING_DEFAULT_TTL_GET)
                }
            } else if (meshMessage is ConfigAppKeyAdd) {
                if (mSetupProvisionedNode) {
                    mProvisionedMeshNodeLiveData.postValue(node)
                    provisioningState?.onMeshNodeStateUpdated(ProvisionerStates.SENDING_APP_KEY_ADD)
                }
            } else if (meshMessage is ConfigNetworkTransmitSet) {
                if (mSetupProvisionedNode) {
                    mProvisionedMeshNodeLiveData.postValue(node)
                    provisioningState?.onMeshNodeStateUpdated(ProvisionerStates.SENDING_NETWORK_TRANSMIT_SET)
                }
            }
        }
    }

    override fun onMeshMessageReceived(src: Int, meshMessage: MeshMessage) {
        val node = mMeshNetwork?.getNode(src)
        if (node != null)
            if (meshMessage is ProxyConfigFilterStatus) {
                mProvisionedMeshNode = node
                setSelectedMeshNode(node)
                val unicastAddress = meshMessage.src
                Utils.printLog(
                    TAG,
                    "Proxy configuration source: " + MeshAddress.formatAddress(
                        meshMessage.src,
                        false
                    )
                )
                mConnectedProxyAddress.postValue(unicastAddress)
                mMeshMessageLiveData.postValue(meshMessage)
            } else if (meshMessage is ConfigCompositionDataStatus) {
                if (mSetupProvisionedNode) {
                    isCompositionDataStatusReceived = true
                    mProvisionedMeshNodeLiveData.postValue(node)
                    mConnectedProxyAddress.postValue(node.unicastAddress)
                    provisioningState?.onMeshNodeStateUpdated(ProvisionerStates.COMPOSITION_DATA_STATUS_RECEIVED)
                    mHandler.postDelayed({
                        val configDefaultTtlGet = ConfigDefaultTtlGet()
                        meshManagerApi.createMeshPdu(node.unicastAddress, configDefaultTtlGet)
                    }, 500)
                    mMeshMessageLiveData.postValue(meshMessage)
                } else {
                    updateNode(node)
                }
            } else if (meshMessage is ConfigDefaultTtlStatus) {
                if (mSetupProvisionedNode) {
                    isDefaultTtlReceived = true
                    mProvisionedMeshNodeLiveData.postValue(node)
                    provisioningState?.onMeshNodeStateUpdated(ProvisionerStates.DEFAULT_TTL_STATUS_RECEIVED)
//                    mHandler.postDelayed({
//                        val appKey = meshNetworkLiveData.getSelectedAppKey()
//                        val index = node.addedNetKeys[0].index
//                        val networkKey = mMeshNetwork!!.netKeys[index]
//                        val configAppKeyAdd = ConfigAppKeyAdd(networkKey, appKey!!)
//                        meshManagerApi.createMeshPdu(node.unicastAddress, configAppKeyAdd)
//                    }, 1500)
                } else {
                    updateNode(node)
                    mMeshMessageLiveData.postValue(meshMessage)
                }
            } else if (meshMessage is ConfigAppKeyStatus) {
                if (mSetupProvisionedNode) {
                    if (meshMessage.isSuccessful) {
                        isAppKeyAddCompleted = true
                        mProvisionedMeshNodeLiveData.postValue(node)
                        provisioningState?.onMeshNodeStateUpdated(ProvisionerStates.APP_KEY_STATUS_RECEIVED)
                        mHandler.postDelayed({
                            val networkTransmitSet = ConfigNetworkTransmitSet(2, 1)
                            meshManagerApi.createMeshPdu(node.unicastAddress, networkTransmitSet)
                        }, 1500)
                    }
                } else {
                    updateNode(node)
                    mMeshMessageLiveData.postValue(meshMessage)
                }
            } else if (meshMessage is ConfigNetworkTransmitStatus) {
                if (mSetupProvisionedNode) {
                    mSetupProvisionedNode = false
                    isNetworkRetransmitSetCompleted = true
                    provisioningState?.onMeshNodeStateUpdated(ProvisionerStates.NETWORK_TRANSMIT_STATUS_RECEIVED)
                } else {
                    updateNode(node)
                    mMeshMessageLiveData.postValue(meshMessage)
                }
            } else if (meshMessage is ConfigModelAppStatus) {
                mMeshMessageLiveData.postValue(meshMessage)
                if (updateNode(node)) {
                    val element = node.elements[meshMessage.elementAddress]
                    if (node.elements.containsKey(meshMessage.elementAddress)) {
                        mSelectedElement = element
                        val model = element?.meshModels?.get(meshMessage.modelIdentifier)
                        mSelectedModel = model
                    }
                }

            } else if (meshMessage is ConfigModelPublicationStatus) {

                if (updateNode(node)) {
                    if (node.elements.containsKey(meshMessage.elementAddress)) {
                        val element = node.elements[meshMessage.elementAddress]
                        mSelectedElement = element
                        val model = element?.meshModels?.get(meshMessage.modelIdentifier)
                        mSelectedModel = model
                    }
                }

            } else if (meshMessage is ConfigModelSubscriptionStatus) {

                if (updateNode(node)) {
                    if (node.elements.containsKey(meshMessage.elementAddress)) {
                        val element = node.elements[meshMessage.elementAddress]
                        mSelectedElement = element
                        val model = element?.meshModels?.get(meshMessage.modelIdentifier)
                        mSelectedModel = model
                    }
                }

            } else if (meshMessage is ConfigNodeResetStatus) {
                bleMeshManager?.setClearCacheRequired()
                mExtendedMeshNode = null
                loadNodes()
                mMeshMessageLiveData.postValue(meshMessage)

            } else if (meshMessage is ConfigRelayStatus) {
                if (updateNode(node)) {
                    mMeshMessageLiveData.postValue(meshMessage)
                }

            } else if (meshMessage is ConfigProxyStatus) {
                if (updateNode(node)) {
                    mMeshMessageLiveData.postValue(meshMessage)
                }

            } else if (meshMessage is GenericOnOffStatus) {
                if (updateNode(node)) {
                    if (node.elements.containsKey(meshMessage.srcAddress)) {
                        val element = node.elements[meshMessage.srcAddress]
                        mSelectedElement = element
                        val model =
                            element?.meshModels?.get(SigModelParser.GENERIC_ON_OFF_SERVER.toInt())
                        mSelectedModel = model
                    }
                }
            } else if (meshMessage is GenericLevelStatus) {

                if (updateNode(node)) {
                    if (node.elements.containsKey(meshMessage.srcAddress)) {
                        val element = node.elements[meshMessage.srcAddress]
                        mSelectedElement = element
                        val model =
                            element?.meshModels?.get(SigModelParser.GENERIC_LEVEL_SERVER.toInt())
                        mSelectedModel = model
                    }
                }

            } else if (meshMessage is VendorModelMessageStatus) {

                if (updateNode(node)) {
                    if (node.elements.containsKey(meshMessage.srcAddress)) {
                        val element = node.elements[meshMessage.srcAddress]
                        mSelectedElement = element
                        val model = element?.meshModels?.get(meshMessage.modelIdentifier)
                        mSelectedModel = model
                    }
                }
            } else if (meshMessage is SensorStatus) {
                if (updateNode(node)) {
                    if (node.elements.containsKey(meshMessage.src)) {
                        val element = node.elements[meshMessage.src]
                        mSelectedElement = element
                    }
                }
            } else if (meshMessage is SensorBatteryStatus) {
                if (updateNode(node)) {
                    if (node.elements.containsKey(meshMessage.src)) {
                        val element = node.elements[meshMessage.src]
                        mSelectedElement = element
                    }
                }
            }

        if (mMeshMessageLiveData.hasActiveObservers()) {
            mMeshMessageLiveData.postValue(meshMessage)
        }

        //Refresh mesh network live data
        meshManagerApi.meshNetwork?.apply {
            meshNetworkLiveData.refresh(this)
        }
    }

    override fun onMessageDecryptionFailed(meshLayer: String, errorMessage: String) {
        Log.e(TAG, "Decryption failed in $meshLayer : $errorMessage")
    }

    /**
     * Loads the network that was loaded from the db or imported from the mesh cdb
     *
     * @param meshNetwork mesh network that was loaded
     */
    private fun loadNetwork(meshNetwork: MeshNetwork) {
        mMeshNetwork = meshNetwork
        mMeshNetwork?.apply {
            if (!this.isProvisionerSelected) {
                meshNetwork.provisioners?.apply {
                    if (this.size > 0) {
                        val provisioner = this[0]
                        provisioner.isLastSelected = true
                        mMeshNetwork!!.selectProvisioner(provisioner)
                    }
                }
            }
            //Load live data with mesh network
            meshNetworkLiveData.loadNetworkInformation(meshNetwork)
            //Load live data with provisioned nodes
            loadNodes()

//            val node = mExtendedMeshNode.value
//            if (node != null) {
            mExtendedMeshNode = this.getNode(mExtendedMeshNode?.uuid)
//            }
        }
    }

    /**
     * We should only update the selected node, since sending messages to group address will notify with nodes that is not on the UI
     */
    private fun updateNode(node: ProvisionedMeshNode): Boolean {
        if (mProvisionedMeshNode?.unicastAddress == node.unicastAddress) {
            mProvisionedMeshNode = node
            mExtendedMeshNode = node
            return true
        }
        return false
    }

    /**
     * Starts reconnecting to the device
     */
    private fun startScan() {
        try {
            if (mIsScanning)
                return

            mIsScanning = true
            // Scanning settings
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                // Refresh the devices list every second
                .setReportDelay(0)
                // Hardware filtering has some issues on selected devices
                .setUseHardwareFilteringIfSupported(false)
                // Samsung S6 and S6 Edge report equal value of RSSI for all devices. In this app we ignore the RSSI.
                /*.setUseHardwareBatchingIfSupported(false)*/
                .build()

            // Let's use the filter to scan only for Mesh devices
            val filters = ArrayList<ScanFilter>()
            filters.add(ScanFilter.Builder().setServiceUuid(ParcelUuid(MESH_PROXY_UUID)).build())

            val scanner = BluetoothLeScannerCompat.getScanner()
            scanner.startScan(filters, settings, mScanCallbacks)
            Log.v(TAG, "Scan started")
            mHandler.postDelayed(mScannerTimeout, 20000)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            //todo 记录日志
        }
    }

    /**
     * stop scanning for bluetooth devices.
     */
    internal fun stopScan() {
        Utils.printLog(TAG, "stopScan")
        mHandler.removeCallbacks(mScannerTimeout)
        var scanner = BluetoothLeScannerCompat.getScanner()
        scanner.stopScan(mScanCallbacks)
        mIsScanning = false
        mScannerStateLiveData.scanningStopped()

//        unregisterBroadcastReceivers()
    }

    private fun onProvisionedDeviceFound(
        node: ProvisionedMeshNode?,
        device: ExtendedBluetoothDevice
    ) {
        mSetupProvisionedNode = true
        mProvisionedMeshNode = node
        mIsReconnectingFlag = true
        //Added an extra delay to ensure reconnection
        mHandler.postDelayed({ connectToProxy(device) }, 2000)
    }

    /**
     * Generates the groups based on the addresses each models have subscribed to
     */
    private fun loadGroups() {
        mGroups.postValue(mMeshNetwork!!.groups)
    }

    private fun updateSelectedGroup() {
        val selectedGroup = mSelectedGroupLiveData.value
        if (selectedGroup != null) {
            mSelectedGroupLiveData.postValue(mMeshNetwork!!.getGroup(selectedGroup.address))
        }
    }

    /**
     * Sets the group that was selected from the GroupAdapter.
     */
    internal fun setSelectedGroup(address: Int) {
        val group = mMeshNetwork!!.getGroup(address)
        if (group != null) {
            mSelectedGroupLiveData.postValue(group)
        }
    }

    companion object {

        private val TAG = NrfMeshManager::class.java.simpleName
        private val ATTENTION_TIMER = 5
        val EXPORT_PATH = Environment.getExternalStorageDirectory().absolutePath + File.separator +
                "mxchip" + File.separator + "nRFMesh" + File.separator
        private val EXPORTED_PATH =
            "sdcard" + File.separator + "mxchip" + File.separator + "nRFMesh" + File.separator
    }

    /************************ scan ******************************/
    private val mScanCallbacks = object : ScanCallback() {

        override fun onScanResult(callbackType: Int, result: ScanResult) {
            try {
                Utils.printLog(
                    TAG,
                    "onScanResult:$mFilterUuid"
                )
                if (!mIsScanning)
                    return

                if (mFilterUuid == BleMeshManager.MESH_PROVISIONING_UUID) {
                    // If the packet has been obtained while Location was disabled, mark Location as not required
                    if (Utils.isLocationRequired(mContext) && !Utils.isLocationEnabled(mContext))
                        Utils.markLocationNotRequired(mContext)

                    updateScannerLiveData(result)
                } else if (mFilterUuid == BleMeshManager.MESH_PROXY_UUID) {
                    val scanRecord = result.scanRecord
                    val serviceData = Utils.getServiceData(result, MESH_PROXY_UUID)
                    if (scanRecord != null) {
                        if (serviceData != null) {
                            if (meshManagerApi.isAdvertisedWithNodeIdentity(serviceData) && mProvisionedMeshNode != null) {
                                val node = mProvisionedMeshNode
                                if (meshManagerApi.nodeIdentityMatches(node!!, serviceData)) {
                                    stopScan()
                                    //TODO
                                    mConnectionState.postValue(
                                        CallbackMsg(
                                            0,
                                            "Provisioned node found"
                                        )
                                    )
                                    onProvisionedDeviceFound(node, ExtendedBluetoothDevice(result))
                                    return
                                }
                            }
                        }
                    } else {
                        Utils.printLog(TAG, "scanRecord is null")
                    }

                    if (meshManagerApi.isAdvertisingWithNetworkIdentity(serviceData) && mNetworkId != null) {
                        if (meshManagerApi.networkIdMatches(mNetworkId!!, serviceData)) {
                            updateScannerLiveData(result)
                        }
                    } else if (meshManagerApi.isAdvertisedWithNodeIdentity(serviceData) && serviceData != null) {
                        if (checkIfNodeIdentityMatches(serviceData)) {
                            updateScannerLiveData(result)
                        }
                    } else {
                        //todo 做日志记录
                    }
                }
            } catch (ex: Exception) {
                Log.e(TAG, "Error: " + ex.message)
            }

        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            // Batch scan is disabled (report delay = 0)
        }

        override fun onScanFailed(errorCode: Int) {
            mScannerStateLiveData.scanningStopped()
        }
    }

    /**
     * Broadcast receiver to monitor the changes in the location provider
     */
    private val mLocationProviderChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val enabled = Utils.isLocationEnabled(context)
            mScannerStateLiveData.isLocationEnabled = enabled
        }
    }

    /**
     * Broadcast receiver to monitor the changes in the bluetooth adapter
     */
    private val mBluetoothStateBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)
            val previousState = intent.getIntExtra(
                BluetoothAdapter.EXTRA_PREVIOUS_STATE,
                BluetoothAdapter.STATE_OFF
            )

            when (state) {
                BluetoothAdapter.STATE_ON -> mScannerStateLiveData.bluetoothEnabled()
                BluetoothAdapter.STATE_TURNING_OFF, BluetoothAdapter.STATE_OFF -> if (previousState != BluetoothAdapter.STATE_TURNING_OFF && previousState != BluetoothAdapter.STATE_OFF) {
                    stopScan()
                    mScannerStateLiveData.bluetoothDisabled()
                }
            }
        }
    }

    fun getScannerState(): ScannerStateLiveData? {
        return mScannerStateLiveData
    }

    fun getScannerResults(): ScannerLiveData? {
        return mScannerLiveData
    }

    private fun updateScannerLiveData(result: ScanResult) {
        val scanRecord = result.scanRecord
        if (scanRecord != null) {
            if (scanRecord.bytes != null) {
                Utils.printLog(
                    TAG,
                    "scanRecord have data,address:${result.device.address}"
                )
                if (mFilterUuid == BleMeshManager.MESH_PROXY_UUID) {
                    mScanDataCallback?.apply {
                        var scannerData = mScannerLiveData.deviceDiscovered(result)
                        this.onScanResult(
                            scannerData.devices,
                            scannerData.updatedDeviceIndex
                        )
                    }
                } else {
                    scanRecord.bytes?.apply {
                        val beaconData = meshManagerApi.getMeshBeaconData(this)
                        if (beaconData != null) {
                            mScanDataCallback?.apply {
                                Utils.printLog(
                                    TAG,
                                    "beaconData:${qk.sdk.mesh.meshsdk.util.ByteUtil.bytesToHexString(
                                        beaconData
                                    )}"
                                )

                                var scannerData = mScannerLiveData.deviceDiscovered(
                                    result,
                                    meshManagerApi.getMeshBeacon(beaconData)
                                )
                                this.onScanResult(
                                    scannerData.devices,
                                    scannerData.updatedDeviceIndex
                                )
                            }
                        }
                    }
                }
                mScannerStateLiveData.deviceFound()
            }
        }
    }

    /**
     * Register for required broadcast receivers.
     */
    internal fun registerBroadcastReceivers() {
        mContext.registerReceiver(
            mBluetoothStateBroadcastReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        )
        if (Utils.isMarshmallowOrAbove) {
            mContext.registerReceiver(
                mLocationProviderChangedReceiver,
                IntentFilter(LocationManager.MODE_CHANGED_ACTION)
            )
        }
    }

    /**
     * Unregister for required broadcast receivers.
     */
    internal fun unregisterBroadcastReceivers() {
        try {
            mContext.unregisterReceiver(mBluetoothStateBroadcastReceiver)
            if (Utils.isMarshmallowOrAbove) {
                mContext.unregisterReceiver(mLocationProviderChangedReceiver)
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Start scanning for Bluetooth devices.
     *
     * @param filterUuid UUID to filter scan results with
     */
    fun startScan(
        filterUuid: UUID,
        networkKey: NetworkKey? = null
    ) {
        try {
            if (mIsScanning)
                return

            mIsScanning = true

            mFilterUuid = filterUuid

            if (mScannerStateLiveData.isScanning) {
                return
            }
            mScannerLiveData.startScanning()

            if (mFilterUuid == BleMeshManager.MESH_PROXY_UUID) {
                if (networkKey != null) {
                    mNetworkId = meshManagerApi.generateNetworkId(networkKey.key)
                }
            }

            mScannerStateLiveData.scanningStarted()
            //Scanning settings
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                // Refresh the devices list every second
                .setReportDelay(0)
                // Hardware filtering has some issues on selected devices
                .setUseHardwareFilteringIfSupported(false)
                // Samsung S6 and S6 Edge report equal value of RSSI for all devices. In this app we ignore the RSSI.
                /*.setUseHardwareBatchingIfSupported(false)*/
                .build()

            //Let's use the filter to scan only for unprovisioned mesh nodes.
            val filters = ArrayList<ScanFilter>()

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {//若版本号小于26，统一都用BluetoothLeScannerImplJB，否则解析不到beacon包
                //反射修改BluetoothLeScannerCompat.getScanner()
                var clazz =
                    Class.forName("no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat")
                Utils.printLog(TAG, "clazz.name:${clazz.simpleName}")
                var instance = clazz.getDeclaredField("instance")
                instance.isAccessible = true
                //反射获取BluetoothLeScannerImplJB实例
                var JBClazz =
                    Class.forName("no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerImplJB")
                var JBConstructor = JBClazz.getDeclaredConstructor()
                JBConstructor.isAccessible = true
                instance.set(BluetoothLeScannerCompat.getScanner(), JBConstructor.newInstance())
            }

            filters.add(ScanFilter.Builder().setServiceUuid(ParcelUuid(filterUuid)).build())
            val scanner = BluetoothLeScannerCompat.getScanner()
            scanner.startScan(filters, settings, mScanCallbacks)
            Utils.printLog(TAG, "start scan")
//            registerBroadcastReceivers()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            mScannerStateLiveData.bluetoothDisabled()
        }
    }

    /**
     * Start scanning for Bluetooth devices.
     *
     * @param filterUuid UUID to filter scan results with
     */
    var mScanDataCallback: qk.sdk.mesh.meshsdk.callback.ScanCallback? = null

    fun startScan(
        filterUuid: UUID,
        scanCallback: qk.sdk.mesh.meshsdk.callback.ScanCallback,
        networkKey: NetworkKey? = null
    ) {
        try {
            if (mIsScanning)
                return

            mIsScanning = true

            mFilterUuid = filterUuid

            if (mScannerStateLiveData.isScanning) {
                return
            }
            mScannerLiveData.startScanning()

            if (mFilterUuid == BleMeshManager.MESH_PROXY_UUID) {
                if (networkKey != null) {
                    mNetworkId = meshManagerApi.generateNetworkId(networkKey.key)
                }
            }

            mScannerStateLiveData.scanningStarted()
            //Scanning settings
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                // Refresh the devices list every second
                .setReportDelay(0)
                // Hardware filtering has some issues on selected devices
                .setUseHardwareFilteringIfSupported(false)
                // Samsung S6 and S6 Edge report equal value of RSSI for all devices. In this app we ignore the RSSI.
                /*.setUseHardwareBatchingIfSupported(false)*/
                .build()

            //Let's use the filter to scan only for unprovisioned mesh nodes.
            val filters = ArrayList<ScanFilter>()

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {//若版本号小于26，统一都用BluetoothLeScannerImplJB，否则解析不到beacon包
                //反射修改BluetoothLeScannerCompat.getScanner()
                var clazz =
                    Class.forName("no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat")
                Utils.printLog(TAG, "clazz.name:${clazz.simpleName}")
                var instance = clazz.getDeclaredField("instance")
                instance.isAccessible = true
                //反射获取BluetoothLeScannerImplJB实例
                var JBClazz =
                    Class.forName("no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerImplJB")
                var JBConstructor = JBClazz.getDeclaredConstructor()
                JBConstructor.isAccessible = true
                instance.set(BluetoothLeScannerCompat.getScanner(), JBConstructor.newInstance())
            }

            filters.add(ScanFilter.Builder().setServiceUuid(ParcelUuid(filterUuid)).build())
            val scanner = BluetoothLeScannerCompat.getScanner()
            mScanDataCallback = scanCallback
            scanner.startScan(filters, settings, mScanCallbacks)
            Utils.printLog(TAG, "start scan mScanDataCallback：${mScanDataCallback == null}")
//            registerBroadcastReceivers()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            mScannerStateLiveData.bluetoothDisabled()
        }
    }

    /**
     * Check if node identity matches
     *
     * @param serviceData service data received from the advertising data
     * @return true if the node identity matches or false otherwise
     */
    private fun checkIfNodeIdentityMatches(serviceData: ByteArray): Boolean {
        val network = meshManagerApi.meshNetwork
        if (network != null) {
            for (node in network.nodes) {
                if (meshManagerApi.nodeIdentityMatches(node, serviceData)) {
                    return true
                }
            }
        }
        return false
    }

    internal fun exportMeshNetwork(callback: NetworkExportUtils.NetworkExportCallbacks) {
        NetworkExportUtils.exportMeshNetwork(
            meshManagerApi,
            EXPORT_PATH,
            "meshJson.json",
            callback
        )
    }

    internal fun importMeshNetworkJson(json: String) {
        mMeshNetwork?.apply {
            meshManagerApi.importMeshNetworkJson(this, json)
        }
    }
}
