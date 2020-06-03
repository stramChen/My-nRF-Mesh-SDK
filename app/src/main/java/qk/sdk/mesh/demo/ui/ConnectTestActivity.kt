package qk.sdk.mesh.demo.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.activity_connect.*
import qk.sdk.mesh.demo.R
import qk.sdk.mesh.demo.base.BaseMeshActivity
import qk.sdk.mesh.demo.util.Constants
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

        tv_proxy_address.setOnClickListener {
            //            MeshSDK.subscribeStatus(mUUID, object : MapCallback {
//                override fun onResult(result: HashMap<String, Any>) {
//                    result.forEach { t, u ->
//                        Utils.printLog(TAG, "subscribeStatus,key:$t,value:$u")
//                    }
//                }
//            })
            MeshSDK.sendMeshMessage(mUUID, 0, 0, "15", "00", object : BooleanCallback {
                override fun onResult(boolean: Boolean) {
                    Log.e(TAG, "reboot :$boolean")
                }
            })
        }

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

        switch_mode.setOnCheckedChangeListener { buttonView, isChecked ->
            var map = HashMap<String, Any>()
            map["LightMode"] = if (isChecked) 1 else 0
            MeshSDK.modifyLightStatus(mUUID, map, object :
                BooleanCallback {
                override fun onResult(boolean: Boolean) {
                    Utils.printLog(TAG, "switch_mode result:$boolean")
                }
            })
        }

        switch_on_off.setOnCheckedChangeListener { buttonView, isChecked ->
            MeshSDK.setGenericOnOff(mUUID, isChecked, 0, object :
                BooleanCallback {
                override fun onResult(boolean: Boolean) {
                    Utils.printLog(TAG, "setGenericOnOff result:$boolean")
                }
            })
//            var map = HashMap<String, Any>()
//            map["LightSwitch"] = if (isChecked) 1 else 0
//            MeshSDK.modifyLightStatus(mUUID, map, object :
//                BooleanCallback {
//                override fun onResult(boolean: Boolean) {
//                    Utils.printLog(TAG, "setGenericOnOff result:$boolean")
//                }
//            })
        }

        btn_send_vendor.setOnClickListener {
            var c = sb_vendor_c.progress * 360 / 100
            var w = sb_vendor_w.progress
            var r = sb_vendor_r.progress
            var g = sb_vendor_g.progress
            var b = sb_vendor_b.progress * 3800 / 100 + 2700
            Utils.printLog(TAG, "h:$c,s:$w,v:$r,b:$g,t:$b")
//            MeshSDK.setLightProperties(mUUID, c, w, r, g, b, object :
//                BooleanCallback {
//                override fun onResult(boolean: Boolean) {
//                    Utils.printLog(TAG, "setLightProperties result:$boolean")
//                }
//            })
            var map = HashMap<String, Any>()
            var callback = object : BooleanCallback {
                override fun onResult(boolean: Boolean) {
                    Utils.printLog(TAG, "modifyLightStatus result:$boolean")
                }
            }
            var vendorMap = HashMap<String, Int>()
            if (g != 0) {
                map["Brightness"] = g
            } else if (b != 2700) {
//                vendorMap["Temperature"] = b
                map["Temperature"] = b
            } else {
                vendorMap["hue"] = c
                vendorMap["saturation"] = w
                vendorMap["value"] = r
                map["HSVColor"] = vendorMap
            }
            MeshSDK.modifyLightStatus(mUUID, map, callback)
        }

        btn_set_publication.setOnClickListener {
            //            MeshSDK.resetNode(mUUID)
            MeshSDK.setPublication(mUUID, Constants.TEST_GROUP, 0xC000, object : MapCallback {
                override fun onResult(result: HashMap<String, Any>) {
                    result.forEach { t, u ->
                        Utils.printLog(TAG, "set publication 0xC000,key:$t,value:$u")
                    }
                    MeshSDK.setPublication(
                        mUUID,
                        Constants.TEST_GROUP_PIR,
                        0xC002,
                        object : MapCallback {
                            override fun onResult(result: HashMap<String, Any>) {
                                result.forEach { t, u ->
                                    Utils.printLog(TAG, "set publication 0xC002,key:$t,value:$u")
                                }
                            }
                        })
                }
            })

        }

        btn_subscribe.setOnClickListener {
//            if (MeshHelper.getGroupByName(Constants.TEST_GROUP) == null)
            MeshSDK.createGroup(Constants.TEST_GROUP, object : BooleanCallback {
                override fun onResult(boolean: Boolean) {
                    Utils.printLog(TAG, "createGroup:$boolean")
                }
            }, 0xC000)

            MeshSDK.createGroup(Constants.TEST_GROUP_PIR, object : BooleanCallback {
                override fun onResult(boolean: Boolean) {
                    Utils.printLog(TAG, "createGroup TEST_GROUP_PIR:$boolean")
                }
            }, 0xC002)

//            MeshSDK.setPublication(mUUID, Constants.TEST_GROUP, object : MapCallback {
//                override fun onResult(result: HashMap<String, Any>) {
//                    result.forEach { t, u ->
//                        Utils.printLog(TAG, " setPublication,key:$t,value:$u")
//                    }
//                }
//            })
            MeshSDK.subscribeStatus(mUUID, object : MapCallback {
                override fun onResult(result: HashMap<String, Any>) {
                    result.forEach { t, u ->
                        Utils.printLog(TAG, "set sendSubscribeMsg,key:$t,value:$u")
                    }
                }
            })
//            MeshSDK.sendSubscribeMsg(mUUID, 0xC002, object : MapCallback {
//                override fun onResult(result: HashMap<String, Any>) {
//                    result.forEach { t, u ->
//                        Utils.printLog(TAG, "set sendSubscribeMsg,key:$t,value:$u")
//                    }
//                }
//            })
//            var map = HashMap<String, Any>()
//            var hsvMap = HashMap<String, Int>()
//            map["HSVColor"] = hsvMap
//            hsvMap["Hue"] = 100
//            hsvMap["Saturation"] = 100
//            hsvMap["Value"] = 100
//            MeshSDK.lightControl(mUUID, map, object : MapCallback {
//                override fun onResult(result: HashMap<String, Any>) {
//
//                }
//            })
        }

        tv_ping.setOnClickListener {
            MeshSDK.getDeviceIdentityKeys(mUUID, object : MapCallback {
                override fun onResult(result: HashMap<String, Any>) {
                    result.forEach { t, u ->
                        Log.e(TAG, "key:$t,value:$u")
                    }
                }
            })
//            MeshSDK.sendMeshMessage(mUUID,0,0,"16","00",object :BooleanCallback{
//                override fun onResult(boolean: Boolean) {
//
//                }
//            })
//            MeshSDK.fetchLightCurrentStatus(mUUID, object : MapCallback {
//                override fun onResult(result: HashMap<String, Any>) {
//                    result.forEach { t, u ->
//                        Utils.printLog(TAG, "key:$t,value:$u")
//                    }
//                }
//            })
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
    override fun onDestroy() {
        super.onDestroy()
        MeshSDK.unSubscribeLightStatus()
    }
}
