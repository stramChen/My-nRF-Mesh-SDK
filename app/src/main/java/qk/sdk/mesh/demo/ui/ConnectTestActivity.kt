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
import qk.sdk.mesh.meshsdk.MeshSDK
import qk.sdk.mesh.meshsdk.bean.CallbackMsg
import qk.sdk.mesh.meshsdk.bean.ExtendedBluetoothDevice
import qk.sdk.mesh.meshsdk.callbak.*
import qk.sdk.mesh.meshsdk.mesh.BleMeshManager
import qk.sdk.mesh.meshsdk.util.ByteUtil
import qk.sdk.mesh.meshsdk.util.Constants
import qk.sdk.mesh.meshsdk.util.Utils
import java.lang.StringBuilder

class ConnectTestActivity : BaseMeshActivity() {
    private val TAG = "ConnectMeshActivity"

    private val MODEL_TYPE_GENERIC = 1
    private val MODEL_TYPE_VENDOR = 2

    private var mMac = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun init() {
        mMac = intent.getStringExtra("mac") ?: ""
        if (mMac.isEmpty()) {
            finish()
            return
        }
        initView()
//        if (!MeshSDK.isConnectedToProxy()) {
////            startScan(Constants.SCAN_PROVISIONED, scanCallback)
//
//        } else {
//
//            MeshHelper.addConnectCallback(connectCallback!!)
//        }
    }

    override fun setLayoutId(): Int = R.layout.activity_connect

    fun initView() {
        btn_add_app_key.isEnabled = true
        btn_add_app_key.visibility = View.VISIBLE
        tv_address.text = MeshHelper.getSelectedMeshNode()?.uuid ?: ""
        tv_proxy_address.text = MeshHelper.getConnectedDevice()?.getAddress() ?: ""
        tv_ttl.text = "${MeshHelper.getSelectedMeshNode()?.ttl ?: ""}"
        btn_add_app_key.setOnClickListener {
            if (MeshHelper.isConnectedToProxy()) {
                MeshSDK.bindApplicationKeyForNode(mMac, object : MapCallback {
                    override fun onResult(result: HashMap<Any, Any>) {

                    }
                })
            } else {
                Utils.printLog(TAG, "isn't Connected To Proxy")
            }
        }
        
        switch_on_off.setOnCheckedChangeListener { buttonView, isChecked ->
            MeshSDK.setGenericOnOff(mMac, isChecked, object : BooleanCallback {
                override fun onResult(boolean: Boolean) {

                }
            })
        }
//
//        btn_send_vendor.setOnClickListener {
//            MeshHelper.getSelectedModel()?.let {
//                if (!(it is VendorModel)) {
//                    bindModel(MODEL_TYPE_VENDOR)
//                }
//                return@let MeshHelper.getSelectedModel()
//            }?.let {
//                Utils.printLog(TAG, "model is vendor${it is VendorModel}")
//            }
//            if (MeshHelper.getSelectedModel() is VendorModel && MeshHelper.getSelectedModel()?.boundAppKeyIndexes?.isNotEmpty() ?: false) {
//                Utils.printLog(TAG, "${sb_vendor_c.progress}")
//                var c = sb_vendor_c.progress * 255 / 100
//                var w = sb_vendor_w.progress * 255 / 100
//                var r = sb_vendor_r.progress * 255 / 100
//                var g = sb_vendor_g.progress * 255 / 100
//                var b = sb_vendor_b.progress * 255 / 100
//                var params = StringBuilder(
//                    "${ByteUtil.rgbtoHex(c)}${ByteUtil.rgbtoHex(w)}${ByteUtil.rgbtoHex(r)}${ByteUtil.rgbtoHex(
//                        g
//                    )}${ByteUtil.rgbtoHex(b)}"
//                )
//                MeshHelper.sendVendorModelMessage(
//                    Integer.valueOf("05", 16),
//                    ByteUtil.hexStringToBytes(params.toString()),
//                    false
//                )
//            } else if (MeshHelper.getSelectedModel() is VendorModel) {
////                MeshHelper.bindAppKey(meshCallback)
//            }
//        }

        btn_reset.setOnClickListener {
            val configNodeReset = ConfigNodeReset()
            sendMessage(MeshHelper.getSelectedMeshNode()?.unicastAddress ?: 0, configNodeReset)
        }

        tv_ping.setOnClickListener {
            //            MeshHelper.sendGenericOnOffGet(meshCallback)
        }
    }
//
//    /**
//     * 在获取到appkey之后，获取当前节点的元素列表
//     */
//    private fun getCompositionData() {
//        val configCompositionDataGet = ConfigCompositionDataGet()
//        val node = MeshHelper.getSelectedMeshNode()
//        node?.let {
//            sendMessage(it.unicastAddress, configCompositionDataGet)
//        }
//    }
//
//    private fun bindModel(type: Int) {
//        var mNode = MeshHelper.getSelectedMeshNode()
//        mNode?.let { node ->
//            var elementsMap = node.elements.values
//            var elements = ArrayList<Element>()
//            elements.addAll(elementsMap)
//            if (elements.size > 0)
//                elements[0].let { element ->
//                    var modelsMap = element.meshModels.values
//                    var models = ArrayList<MeshModel>()
//                    models.addAll(modelsMap)
//                    models.forEach { model ->
//                        when (type) {
//                            MODEL_TYPE_GENERIC -> {
//                                if (model is GenericOnOffServerModel) {
//                                    MeshHelper.setSelectedModel(element, model)
//                                }
//                            }
//                            MODEL_TYPE_VENDOR -> {
//                                if (model is VendorModel) {
//                                    MeshHelper.setSelectedModel(element, model)
//                                    Utils.printLog(TAG, "set selected vendor model!")
//                                }
//                            }
//                        }
//                    }
//                }
//        }
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        scanCallback = null
//        connectCallback = null
//        meshCallback = null
//        MeshHelper.stopConnect()
//        MeshHelper.stopScan()
//        MeshHelper.clearMeshCallback()
//    }
}
