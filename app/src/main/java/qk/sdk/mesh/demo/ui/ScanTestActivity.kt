package qk.sdk.mesh.demo.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_scan.*
import kotlinx.android.synthetic.main.item_scan_device.view.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import qk.sdk.mesh.demo.R
import qk.sdk.mesh.demo.base.BaseMeshActivity
import qk.sdk.mesh.demo.widget.base.BaseAdapter
import qk.sdk.mesh.demo.widget.base.BaseViewHolder
import qk.sdk.mesh.demo.widget.base.OnItemClickListener
import qk.sdk.mesh.meshsdk.MeshHelper
import qk.sdk.mesh.meshsdk.MeshSDK
import qk.sdk.mesh.meshsdk.callbak.ArrayMapCallback
import qk.sdk.mesh.meshsdk.callbak.IntCallback
import qk.sdk.mesh.meshsdk.callbak.MapCallback
import qk.sdk.mesh.meshsdk.callbak.StringCallback
import qk.sdk.mesh.meshsdk.util.Constants
import qk.sdk.mesh.meshsdk.util.Utils

class ScanTestActivity : BaseMeshActivity(),
    OnItemClickListener<HashMap<String, Any>> {
    private val TAG = "ScanTestActivity"
    private var mDeviceAdapter: DevicesAdapter? = null
    private var mDevice: ArrayList<HashMap<String, Any>> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun setLayoutId(): Int = R.layout.activity_scan
    override fun init() {
        initView()
    }

    fun initView() {
        mDeviceAdapter = DevicesAdapter(this)
        rv_devices.layoutManager = LinearLayoutManager(this)
        rv_devices.adapter = mDeviceAdapter
        mDeviceAdapter?.setOnItemClickListener(this)

        Thread(Runnable {
            MeshHelper.getProvisionNode()
        }).start()

        startScan(Constants.SCAN_UNPROVISIONED, object :
            ArrayMapCallback {
            override fun onResult(result: ArrayList<HashMap<String, Any>>) {
                mDevice = result
                mDeviceAdapter?.setData(result)
            }
        })

        btn_manage_keys.setOnClickListener {
            startActivity(Intent(this, NetKeyActivity::class.java))
        }
    }


    private fun startScan(type: String, callback: ArrayMapCallback) {
        MeshSDK.checkPermission(object : StringCallback {
            override fun onResultMsg(msg: String) {
                if (msg == Constants.PERMISSION_GRANTED) {
                    MeshSDK.startScan(type, callback, object : IntCallback {
                        override fun onResultMsg(code: Int) {
                            Utils.printLog(TAG, "scan error:$code")
                        }
                    })
                } else {
                    Utils.printLog(TAG, "PERMISSION:$msg")
                }
            }
        })

    }


    override fun onItemClick(data: HashMap<String, Any>, position: Int) {
        MeshSDK.stopScan()

        var netKey = "${System.currentTimeMillis()}0000011111111010101"

        MeshSDK.getCurrentNetworkKey(object : StringCallback {
            override fun onResultMsg(msg: String) {
                if (msg.isEmpty()) {
                    MeshSDK.createNetworkKey(netKey)
                    MeshSDK.createApplicationKey(netKey)
                    MeshSDK.setCurrentNetworkKey(netKey)
                }
            }
        })

        MeshSDK.provision(data.get("uuid") as String, object : MapCallback {
            override fun onResult(msg: HashMap<String, Any>) {
                tv_status.visibility = View.VISIBLE
                msg.forEach { key, value ->
                    Utils.printLog(TAG, "$value")
                    tv_status.text = "$value"
                    if (value == Constants.ConnectState.PROVISION_SUCCESS.msg) {//配对成功
                        Utils.printLog(TAG, "$value")

                        startActivity(
                            Intent(
                                this@ScanTestActivity,
                                ConnectTestActivity::class.java
                            ).putExtra("mac", data.get("mac") as String)
                        )
                        finish()
                    }
                }
            }
        })
    }

    inner class DevicesAdapter(context: Context) :
        BaseAdapter<HashMap<String, Any>>(context, mDevice, R.layout.item_scan_device) {

        override fun bindData(
            holder: BaseViewHolder,
            data: HashMap<String, Any>,
            position: Int
        ) {
            holder.itemView.tv_device_address.text = mDevice[position].get("uuid") as String
            holder.itemView.tv_device_name.text = mDevice[position].get("mac") as String
        }

        fun notify(updatedIndex: Int?) {
            if (updatedIndex != null && (mDevice.size) > 0) {
                notifyItemChanged(updatedIndex)
            } else {
                setData(mDevice)
                notifyDataSetChanged()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        MeshSDK.stopScan()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
