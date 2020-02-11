package qk.sdk.mesh.demo.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.activity_connect.*
import qk.sdk.mesh.demo.R
import qk.sdk.mesh.demo.base.BaseMeshActivity
import qk.sdk.mesh.meshsdk.MeshHelper
import qk.sdk.mesh.meshsdk.MeshSDK
import qk.sdk.mesh.meshsdk.callback.ArrayStringCallback
import qk.sdk.mesh.meshsdk.callback.BooleanCallback
import qk.sdk.mesh.meshsdk.callback.MapCallback
import qk.sdk.mesh.meshsdk.callback.StringCallback
import qk.sdk.mesh.meshsdk.util.Utils

class ConnectTestActivity : BaseMeshActivity() {
    private val TAG = "ConnectTestActivity"

    private val MODEL_TYPE_GENERIC = 1
    private val MODEL_TYPE_VENDOR = 2

    private var mUUID = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun init() {
        mUUID = intent.getStringExtra("uuid") ?: ""
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
                Thread(Runnable {
                    MeshSDK.getCurrentNetworkKey(object :
                        StringCallback {
                        override fun onResultMsg(msg: String) {
                            MeshSDK.getAllApplicationKey(msg, object :
                                ArrayStringCallback {
                                override fun onResult(result: ArrayList<String>) {
                                    if (result.size > 0) {
                                        MeshSDK.bindApplicationKeyForNode(
                                            mUUID,
                                            result.get(0),
                                            object : MapCallback {
                                                override fun onResult(result: HashMap<String, Any>) {
                                                }
                                            })
                                    }
                                }
                            })
                        }
                    })
                }).start()
            } else {
                Utils.printLog(TAG, "isn't Connected To Proxy")
            }
        }

        switch_on_off.setOnCheckedChangeListener { buttonView, isChecked ->
            MeshSDK.setGenericOnOff(mUUID, isChecked, object :
                BooleanCallback {
                override fun onResult(boolean: Boolean) {
                    Utils.printLog(TAG, "setGenericOnOff result:$boolean")
                }
            })
        }

        btn_send_vendor.setOnClickListener {
            var c = sb_vendor_c.progress * 255 / 100
            var w = sb_vendor_w.progress * 255 / 100
            var r = sb_vendor_r.progress * 255 / 100
            var g = sb_vendor_g.progress * 255 / 100
            var b = sb_vendor_b.progress * 255 / 100
            MeshSDK.setLightProperties(mUUID, c, w, r, g, b, object :
                BooleanCallback {
                override fun onResult(boolean: Boolean) {
                    Utils.printLog(TAG, "setLightProperties result:$boolean")
                }
            })
        }

        btn_reset.setOnClickListener {
            MeshSDK.resetNode(mUUID)
        }

        tv_ping.setOnClickListener {
            MeshSDK.getDeviceIdentityKeys(mUUID,object : MapCallback {
                override fun onResult(result: HashMap<String, Any>) {
                    result.forEach { t, u ->
                        Log.e(TAG,"key:$t,value:$u")
                    }
                }
            })
//            MeshSDK.sendMeshMessage(mUUID,0,0,"05","0016000000",object :BooleanCallback{
//                override fun onResult(boolean: Boolean) {
//
//                }
//            })
//            MeshSDK.sendMeshMessage(mUUID,0,0,"00","",object :BooleanCallback{
//                override fun onResult(boolean: Boolean) {
//
//                }
//            })
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
