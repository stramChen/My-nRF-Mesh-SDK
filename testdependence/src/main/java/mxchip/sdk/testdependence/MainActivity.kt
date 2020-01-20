package mxchip.sdk.testdependence

import android.app.Activity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : Activity() {
    private val TAG = "MainActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        MeshSDK.init(this)

        test()
    }

    fun test() {
//        tv_hello.setOnClickListener {
//            MeshSDK.startScan("unProvisioned", object : ArrayMapCallback {
//                override fun onResult(result: ArrayList<HashMap<String, Any>>) {
//                    Log.e(TAG, "onResult:${result.size}")
//                }
//            }, object : IntCallback {
//                override fun onResultMsg(code: Int) {
//                    Log.e(TAG, "onErrorMsg:$code")
//                }
//            })
//        }
    }

    override fun onStop() {
        super.onStop()
//        MeshSDK.stopScan()
    }
}
