package qk.sdk.mesh.demo.ui

import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_connect.*
import no.nordicsemi.android.meshprovisioner.models.GenericOnOffServerModel
import no.nordicsemi.android.meshprovisioner.models.VendorModel
import no.nordicsemi.android.meshprovisioner.transport.*
import qk.sdk.mesh.demo.R
import qk.sdk.mesh.demo.base.BaseMeshActivity
import qk.sdk.mesh.meshsdk.MeshHelper
import qk.sdk.mesh.meshsdk.bean.CallbackMsg
import qk.sdk.mesh.meshsdk.bean.ExtendedBluetoothDevice
import qk.sdk.mesh.meshsdk.callbak.ConnectCallback
import qk.sdk.mesh.meshsdk.callbak.MeshCallback
import qk.sdk.mesh.meshsdk.callbak.ScanCallback
import qk.sdk.mesh.meshsdk.mesh.BleMeshManager
import qk.sdk.mesh.meshsdk.util.ByteUtil
import qk.sdk.mesh.meshsdk.util.Constants
import qk.sdk.mesh.meshsdk.util.Utils
import java.lang.StringBuilder

class ConnectMeshActivity : BaseMeshActivity() {
    private val TAG = "ConnectMeshActivity"

    private val MODEL_TYPE_GENERIC = 1
    private val MODEL_TYPE_VENDOR = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun init() {
        initView()
        if (!MeshHelper.isConnectedToProxy()) {
            startScan(BleMeshManager.MESH_PROXY_UUID, scanCallback)
        } else {
            switch_on_off.visibility = View.VISIBLE
            tv_state.text = getString(R.string.connect_success)
            MeshHelper.addConnectCallback(connectCallback!!)
        }
    }

    override fun setLayoutId(): Int = R.layout.activity_connect

    fun initView() {
        btn_add_app_key.isEnabled = false
        tv_address.text = MeshHelper.getSelectedMeshNode()?.uuid ?: ""
        tv_proxy_address.text = MeshHelper.getConnectedDevice()?.getAddress() ?: ""
        tv_ttl.text = "${MeshHelper.getSelectedMeshNode()?.ttl ?: ""}"
        btn_add_app_key.setOnClickListener {
            if (MeshHelper.isConnectedToProxy()) {
                MeshHelper.addAppKeys(meshCallback)
            } else {
                Utils.printLog(TAG, "isn't Connected To Proxy")
            }
        }
        switch_on_off.setOnCheckedChangeListener { buttonView, isChecked ->
            if (!(MeshHelper.getSelectedModel() is GenericOnOffServerModel)) {
                bindModel(MODEL_TYPE_GENERIC)
            }
            if (MeshHelper.getSelectedModel()?.boundAppKeyIndexes?.isNotEmpty() ?: false) {
                MeshHelper.sendGenericOnOff(isChecked, 0)
            } else {
                MeshHelper.bindAppKey(meshCallback)
            }

        }

        btn_send_vendor.setOnClickListener {
            MeshHelper.getSelectedModel()?.let {
                if (!(it is VendorModel)) {
                    bindModel(MODEL_TYPE_VENDOR)
                }
                return@let MeshHelper.getSelectedModel()
            }?.let {
                Utils.printLog(TAG, "model is vendor${it is VendorModel}")
            }
            if (MeshHelper.getSelectedModel() is VendorModel && MeshHelper.getSelectedModel()?.boundAppKeyIndexes?.isNotEmpty() ?: false) {
                Utils.printLog(TAG, "${sb_vendor_c.progress}")
                var c = sb_vendor_c.progress * 255 / 100
                var w = sb_vendor_w.progress * 255 / 100
                var r = sb_vendor_r.progress * 255 / 100
                var g = sb_vendor_g.progress * 255 / 100
                var b = sb_vendor_b.progress * 255 / 100
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
            } else if (MeshHelper.getSelectedModel() is VendorModel) {
                MeshHelper.bindAppKey(meshCallback)
            }
        }

        btn_reset.setOnClickListener {
            val configNodeReset = ConfigNodeReset()
            sendMessage(MeshHelper.getSelectedMeshNode()?.unicastAddress ?: 0, configNodeReset)
        }

        tv_ping.setOnClickListener {
            MeshHelper.sendGenericOnOffGet(meshCallback)
        }
    }

    var scanCallback: ScanCallback? = object : ScanCallback {
        override fun onScanResult(
            devices: List<ExtendedBluetoothDevice>,
            updatedIndex: Int?
        ) {
            // 若是已配对过的节点,则可通过代理节点连接
            if (MeshHelper.getSelectedModel()?.boundAppKeyIndexes?.isNotEmpty() ?: false) {
//                MeshHelper.getProvisionNode()?.forEach forEach@{ node ->
//                    devices?.forEach deviceForeach@{ device ->
//                        if (Utils.isUUIDEqualsMac(
//                                Utils.getMacFromUUID(node.uuid),
//                                device.getAddress()
//                            )
//                        ) {
                if (devices.isNotEmpty()) {
                    startConnect(devices[0])
                    MeshHelper.stopScan()
                }
//                            return@forEach
//                        }
//                    }
//                }
            } else {
                devices.forEach { device ->
                    var selectedNode = MeshHelper.getSelectedMeshNode()
                    selectedNode?.let { selectedNode ->
                        if (Utils.isUUIDEqualsMac(
                                Utils.getMacFromUUID(selectedNode.uuid),
                                device.getAddress()
                            )
                        ) {
                            startConnect(device)
                            MeshHelper.stopScan()
                        }
                        tv_state.text = getString(R.string.connecting)
                    }
                }
            }
        }

        override fun onError(callbackMsg: CallbackMsg) {
        }
    }

    var connectCallback: ConnectCallback? = object : ConnectCallback {
        override fun onConnect() {
            tv_proxy_address.text = MeshHelper.getConnectedDevice()?.getAddress()
            if (MeshHelper.getSelectedModel() == null && MeshHelper.getSelectedElement() == null) {
                btn_add_app_key.isEnabled = true
                btn_add_app_key.visibility = View.VISIBLE
            } else {
                switch_on_off.visibility = View.VISIBLE
            }
            tv_state.text = getString(R.string.connect_success)
        }

        override fun onConnectStateChange(msg: CallbackMsg) {
            if (msg.msg == Constants.ConnectState.DISCONNECTED.msg) {//连接断开，自动寻找代理节点重连
                startScan(BleMeshManager.MESH_PROXY_UUID, scanCallback)
            }
            tv_state.text = msg.msg
        }

        override fun onError(callbackMsg: CallbackMsg) {
            tv_state.text = getString(R.string.connect_failed)
        }
    }

    var meshCallback: MeshCallback? = object : MeshCallback {
        override fun onReceive(msg: MeshMessage) {
            if (msg is ConfigAppKeyStatus) {
                if (msg.isSuccessful) {//添加appkey成功
                    getCompositionData()
                    if (switch_on_off.visibility != View.VISIBLE || MeshHelper.getSelectedElement() == null) {
                        switch_on_off.visibility = View.VISIBLE
                    }

                    Utils.printLog(
                        TAG,
                        "switch_on_off.visibility:${switch_on_off.visibility},add app key success!"
                    )
                    btn_add_app_key.visibility = View.GONE
                    MeshHelper.bindAppKey(this)
                } else {
                    Utils.printLog(TAG, "add app key failed!")
                }
            } else if (msg is ConfigModelAppStatus) {
                if (msg.isSuccessful) {//bind appkey成功
//                    Utils.printLog(TAG, "bindAppKey success!")
                    //todo 轮询model列表，
                } else {
                    Utils.printLog(TAG, "bindAppKey failed:${msg.statusCodeName}")
                }
            } else if (msg is GenericOnOffStatus) {
                tv_ping.text = "${System.currentTimeMillis() - mPingMills} ms"
                Utils.printLog(TAG, "get on off status")
            }
        }

        override fun onError(callbackMsg: CallbackMsg) {

        }
    }

    fun startConnect(data: ExtendedBluetoothDevice) {
        MeshHelper.connect(data, true, connectCallback)
    }

    /**
     * 在获取到appkey之后，获取当前节点的元素列表
     */
    private fun getCompositionData() {
        val configCompositionDataGet = ConfigCompositionDataGet()
        val node = MeshHelper.getSelectedMeshNode()
        node?.let {
            sendMessage(it.unicastAddress, configCompositionDataGet)
        }
    }

    private fun bindModel(type: Int) {
        var mNode = MeshHelper.getSelectedMeshNode()
        mNode?.let { node ->
            var elementsMap = node.elements.values
            var elements = ArrayList<Element>()
            elements.addAll(elementsMap)
            if (elements.size > 0)
                elements[0].let { element ->
                    var modelsMap = element.meshModels.values
                    var models = ArrayList<MeshModel>()
                    models.addAll(modelsMap)
                    models.forEach { model ->
                        when (type) {
                            MODEL_TYPE_GENERIC -> {
                                if (model is GenericOnOffServerModel) {
                                    MeshHelper.setSelectedModel(element, model)
                                }
                            }
                            MODEL_TYPE_VENDOR -> {
                                if (model is VendorModel) {
                                    MeshHelper.setSelectedModel(element, model)
                                    Utils.printLog(TAG, "set selected vendor model!")
                                }
                            }
                        }
                    }
                }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scanCallback = null
        connectCallback = null
        meshCallback = null
        MeshHelper.stopConnect()
        MeshHelper.stopScan()
        MeshHelper.clearMeshCallback()
    }
}
