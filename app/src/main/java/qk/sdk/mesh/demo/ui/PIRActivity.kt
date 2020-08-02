package qk.sdk.mesh.demo.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import qk.sdk.mesh.demo.R
import qk.sdk.mesh.meshsdk.MeshHelper
import qk.sdk.mesh.meshsdk.MeshSDK
import qk.sdk.mesh.meshsdk.callback.BooleanCallback

class PIRActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pir)

        initPIRListener()
    }

    fun initPIRListener() {
        MeshHelper.getGroupByAddress(0XC002)?.apply {
            MeshSDK.createGroup("${this.name}", object : BooleanCallback() {
                override fun onResult(boolean: Boolean) {
//                    MeshSDK.subscribeStatus("", object : MapCallback {
//                        override fun onResult(result: HashMap<String, Any>) {
//                            result.forEach { key, value ->
//                                Utils.printLog("TAG", "key:$key,value:$value")
//                            }
//                        }
//                    })
                }
            })
        }

    }
}
