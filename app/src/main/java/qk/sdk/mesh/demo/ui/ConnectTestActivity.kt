package qk.sdk.mesh.demo.ui

import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_connect.*
import qk.sdk.mesh.demo.R
import qk.sdk.mesh.demo.base.BaseMeshActivity
import qk.sdk.mesh.meshsdk.MeshHelper
import qk.sdk.mesh.meshsdk.MeshSDK
import qk.sdk.mesh.meshsdk.callback.*
import qk.sdk.mesh.meshsdk.util.Utils
import qk.sdk.mesh.meshsdk.bean.*
import qk.sdk.mesh.meshsdk.util.LongLog

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
        Utils.printLog(TAG, "mesh uuid:${mUUID}")
        initView()
    }

    override fun setLayoutId(): Int = R.layout.activity_connect

    fun initView() {
        btn_add_app_key.isEnabled = true
        btn_add_app_key.visibility = View.VISIBLE
        tv_address.text = MeshHelper.getSelectedMeshNode()?.uuid ?: ""
        tv_proxy_address.text = MeshHelper.getConnectedDevice()?.getAddress() ?: mUUID
        tv_ttl.text = "${MeshHelper.getSelectedMeshNode()?.ttl ?: ""}"

        tv_proxy_address.setOnClickListener {
            MeshSDK.fetchLightCurrentStatus(mUUID, object : StringCallback() {
                override fun onResultMsg(msg: String) {
                        Utils.printLog(TAG, msg)
                }
            })
        }

        btn_add_app_key.setOnClickListener {
            if (MeshHelper.isConnectedToProxy()) {
                Thread(Runnable {
                    MeshSDK.getCurrentNetworkKey(object :
                        StringCallback() {
                        override fun onResultMsg(msg: String) {
                            MeshSDK.getAllApplicationKey(msg, object :
                                ArrayStringCallback {
                                override fun onResult(result: ArrayList<String>) {
                                    if (result.size > 0) {
                                        MeshSDK.addApplicationKeyForNode(
                                            mUUID,
                                            result.get(0),
                                            object : MapCallback() {
                                                override fun onResult(result: HashMap<String, Any>) {
                                                    exportConfig()
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
                BooleanCallback() {
                override fun onResult(boolean: Boolean) {
                    Utils.printLog(TAG, "switch_mode result:$boolean")
                }
            })

//            var ruleMsg = "00010cd85d00080106000282030101"
//            var meshMsg =
//                VendorModelMessageUnacked(null, 0, 0, 18, ByteUtil.hexStringToBytes(ruleMsg))
//            MeshHelper.sendMeshPdu("", 0xC002, meshMsg, null)
        }

        switch_on_off.setOnCheckedChangeListener { buttonView, isChecked ->
            MeshSDK.setGenericOnOff(mUUID, isChecked, 0, object :
                BooleanCallback() {
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
            var h = sb_vendor_c.progress * 360 / 100
            var s = sb_vendor_w.progress
            var v = sb_vendor_r.progress
            var b = sb_vendor_g.progress
            var t = sb_vendor_b.progress * 192 + 800
            Utils.printLog(TAG, "h:$h,s:$s,v:$v,b:$b,t:$t")
            var map = HashMap<String, Any>()
            var callback = object : BooleanCallback() {
                override fun onResult(boolean: Boolean) {
                    Utils.printLog(TAG, "modifyLightStatus result:$boolean")
                }
            }
            var vendorMap = HashMap<String, Int>()
            if (b != 0) {
                map["Brightness"] = b
            } else if (t != 800) {
                map["ColorTemperature"] = t
            } else {
                vendorMap["hue"] = h
                vendorMap["saturation"] = s
                vendorMap["value"] = v
                map["HSVColor"] = vendorMap
            }
            MeshSDK.modifyLightStatus(mUUID, map, callback)
        }

        btn_set_publication.setOnClickListener {
//                        MeshSDK.resetNode(mUUID)
//            MeshSDK.setPublication(mUUID, Constants.TEST_GROUP, 0xD000, object : MapCallback {
//                override fun onResult(result: HashMap<String, Any>) {
//                    result.forEach { t, u ->
//                        Utils.printLog(TAG, "set publication 0xC000,key:$t,value:$u")
//                    }
//                    MeshSDK.setPublication(
//                        mUUID,
//                        Constants.TEST_GROUP_PIR,
//                        0xC002,
//                        object : MapCallback {
//                            override fun onResult(result: HashMap<String, Any>) {
//                                result.forEach { t, u ->
//                                    Utils.printLog(TAG, "set publication 0xC002,key:$t,value:$u")
//                                }
//                            }
//                        })
//                }
//            })

        }

        btn_subscribe.setOnClickListener {
            MeshSDK.getCurrentNode(object : MapCallback() {
                override fun onResult(result: HashMap<String, Any>) {
                    MeshSDK.getDeviceCurrentStatus(result["uuid"] as String,
                        listOf(SWITCH),
                        object : StringCallback() {
                            override fun onResultMsg(msg: String) {
                                Utils.printLog(TAG, "result:$msg")
                            }
                        })
                }
            })


//            MeshSDK.getAllDeviceStatus();

//            if (MeshHelper.getGroupByName(Constants.TEST_GROUP) == null)
//            MeshSDK.createGroup("0xD000", object : BooleanCallback {
//                override fun onResult(boolean: Boolean) {
//                    Utils.printLog(TAG, "createGroup:$boolean")
//                }
//            }, 0xD000)

//            MeshSDK.createGroup("0xC001", object : BooleanCallback {
//                override fun onResult(boolean: Boolean) {
//                    Utils.printLog(TAG, "createGroup TEST_GROUP_PIR:$boolean")
//                }
//            }, 0xC001)
//
//            MeshSDK.createGroup(Constants.TEST_GROUP_PIR, object : BooleanCallback {
//                override fun onResult(boolean: Boolean) {
//                    Utils.printLog(TAG, "createGroup TEST_GROUP_PIR:$boolean")
//                }
//            }, 0xC002)

//            MeshSDK.setPublication(mUUID, Constants.TEST_GROUP, object : MapCallback {
//                override fun onResult(result: HashMap<String, Any>) {
//                    result.forEach { t, u ->
//                        Utils.printLog(TAG, " setPublication,key:$t,value:$u")
//                    }
//                }
//            })
//            MeshSDK.subscribeStatus(mUUID, object : MapCallback {
//                override fun onResult(result: HashMap<String, Any>) {
//                    result.forEach { t, u ->
//                        Utils.printLog(TAG, "set sendSubscribeMsg,key:$t,value:$u")
//                    }
//                }
//            })
//            MeshSDK.sendSubscribeMsg(mUUID, Constants.TEST_GROUP, object : MapCallback {
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
            MeshSDK.getDeviceQuadruples(mUUID, object : MapCallback() {
                override fun onResult(result: HashMap<String, Any>) {
                    result.forEach { key, value ->
                        Utils.printLog(TAG, "$key:$value")
                    }
                }
            })
//            MeshSDK.getDeviceIdentityKeys(mUUID, object : MapCallback {
//                override fun onResult(result: HashMap<String, Any>) {
//                    result.forEach { t, u ->
//                        Log.e(TAG, "key:$t,value:$u")
//                    }
//                }
//            })
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

//            MeshSDK.getDeviceVersion(mUUID, object : MapCallback {
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

    private fun exportConfig() {
        Thread{
            MeshSDK.exportMeshNetwork(object : StringCallback() {
                override fun onResultMsg(msg: String) {
                    LongLog.d(TAG, "mesh rule->$msg")
                }
            })
        }.run()

    }

    override fun onDestroy() {
        super.onDestroy()
        MeshSDK.unSubscribeLightStatus()
    }
}
