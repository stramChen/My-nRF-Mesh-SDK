package qk.sdk.mesh.demo.ui

import android.os.Bundle
import kotlinx.android.synthetic.main.activity_connect.*
import kotlinx.android.synthetic.main.activity_connect.btn_add_app_key
import kotlinx.android.synthetic.main.activity_on_off_mesh.*
import qk.sdk.mesh.demo.R
import qk.sdk.mesh.demo.base.BaseMeshActivity
import qk.sdk.mesh.meshsdk.MeshHelper
import qk.sdk.mesh.meshsdk.MeshSDK
import qk.sdk.mesh.meshsdk.callback.ArrayStringCallback
import qk.sdk.mesh.meshsdk.callback.BooleanCallback
import qk.sdk.mesh.meshsdk.callback.MapCallback
import qk.sdk.mesh.meshsdk.callback.StringCallback
import qk.sdk.mesh.meshsdk.util.Utils

class OnOffMeshActivity : BaseMeshActivity() {
    private val TAG = "OnOffMeshActivity"

    private var mUUID = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun setLayoutId(): Int = R.layout.activity_on_off_mesh

    override fun init() {
        mUUID = intent.getStringExtra("uuid") ?: ""
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

        var btn1 = 0
        var btn2 = 0
        var btn3 = 0
        btn_ele_fir.setOnClickListener {
            MeshSDK.setGenericOnOff(
                mUUID,
                if (btn1 % 2 == 0) true else false,
                0,
                object : BooleanCallback {
                    override fun onResult(boolean: Boolean) {
                        btn1++
                        Utils.printLog(TAG, "setGenericOnOff result:$boolean")
                    }
                })
        }

        btn_ele_sec.setOnClickListener {
            MeshSDK.setGenericOnOff(
                mUUID,
                if (btn2 % 2 == 0) true else false,
                1,
                object : BooleanCallback {
                    override fun onResult(boolean: Boolean) {
                        btn2++
                        Utils.printLog(TAG, "setGenericOnOff result:$boolean")
                    }
                })
        }

        btn_ele_thir.setOnClickListener {
            MeshSDK.setGenericOnOff(
                mUUID,
                if (btn3 % 2 == 0) true else false,
                2,
                object : BooleanCallback {
                    override fun onResult(boolean: Boolean) {
                        btn3++
                        Utils.printLog(TAG, "setGenericOnOff result:$boolean")
                    }
                })
        }
    }
}
