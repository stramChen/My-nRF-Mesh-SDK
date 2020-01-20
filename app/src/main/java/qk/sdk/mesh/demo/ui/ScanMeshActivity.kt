package qk.sdk.mesh.demo.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_scan.*
import kotlinx.android.synthetic.main.item_scan_device.view.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import qk.sdk.mesh.demo.R
import qk.sdk.mesh.demo.base.BaseMeshActivity
import qk.sdk.mesh.demo.widget.base.BaseAdapter
import qk.sdk.mesh.demo.widget.base.BaseViewHolder
import qk.sdk.mesh.demo.widget.base.OnItemClickListener
import qk.sdk.mesh.meshsdk.MeshHelper
import qk.sdk.mesh.meshsdk.bean.CallbackMsg
import qk.sdk.mesh.meshsdk.bean.ExtendedBluetoothDevice
import qk.sdk.mesh.meshsdk.callback.BaseCallback
import qk.sdk.mesh.meshsdk.callback.ConnectCallback
import qk.sdk.mesh.meshsdk.mesh.BleMeshManager
import qk.sdk.mesh.meshsdk.callback.ScanCallback
import qk.sdk.mesh.meshsdk.util.Constants
import qk.sdk.mesh.meshsdk.util.Utils
import kotlin.collections.ArrayList

class ScanMeshActivity : BaseMeshActivity(),
    OnItemClickListener<ExtendedBluetoothDevice> {
    private val TAG = "ScanMeshActivity"

    private var mDeviceAdapter: DevicesAdapter? = null
    private var mDevice: ArrayList<ExtendedBluetoothDevice> = ArrayList()

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

        startScan(BleMeshManager.MESH_PROVISIONING_UUID, object :
            ScanCallback {
            override fun onScanResult(
                devices: List<ExtendedBluetoothDevice>,
                updatedIndex: Int?
            ) {//扫描设备列表回调
                if (devices is ArrayList) {
                    mDevice = devices
                    mDeviceAdapter?.setData(devices)
                }
            }

            override fun onError(callbackMsg: CallbackMsg) {

            }
        })
    }

    fun startConnect(data: ExtendedBluetoothDevice) {
//        MeshHelper.disConnect()
        MeshHelper.stopScan()
        MeshHelper.connect( data, false, object : ConnectCallback {
            override fun onConnect() {
                MeshHelper.startProvision(data, object : BaseCallback {
                    override fun onError(callbackMsg: CallbackMsg) {

                    }
                })
            }

            override fun onConnectStateChange(msg: CallbackMsg) {
                tv_status.visibility = View.VISIBLE
                tv_status.text = msg.msg
                if (msg.msg == Constants.ConnectState.DISCONNECTED.msg) {
                    var node = MeshHelper.getProvisionNode()
                    node?.let {
                        node.forEach {
                            if (Utils.isUUIDEqualsMac(
                                    Utils.getMacFromUUID(it.uuid),
                                    data.getAddress()
                                )
                            ) {
                                MeshHelper.stopScan()
                                runBlocking {
                                    launch {
                                        delay(1000)
                                        MeshHelper.setSelectedMeshNode(it)
                                        MeshHelper.setSelectedModel(null, null)
//                                        bindModel(it)
                                        startActivity(
                                            Intent(
                                                this@ScanMeshActivity,
                                                ConnectMeshActivity::class.java
                                            )
                                        )
                                        finish()
                                    }
                                }
                            }
                        }
                    }
                }
            }

            override fun onError(callbackMsg: CallbackMsg) {


            }
        })

    }

    override fun onItemClick(data: ExtendedBluetoothDevice, position: Int) {
        MeshHelper.stopScan()
        startConnect(data)
    }

    override fun onStop() {
        super.onStop()
        MeshHelper.stopScan()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        MeshHelper.disConnect()
    }


    inner class DevicesAdapter(context: Context) :
        BaseAdapter<ExtendedBluetoothDevice>(context, mDevice, R.layout.item_scan_device) {

        override fun bindData(
            holder: BaseViewHolder,
            data: ExtendedBluetoothDevice,
            position: Int
        ) {
            holder.itemView.tv_device_address.text = mDevice[position].getAddress()
            holder.itemView.tv_device_name.text = mDevice[position].name
        }

        fun notify(updatedIndex: Int?) {
            if (updatedIndex != null && (mDevice.size) > 0) {
                notifyItemChanged(updatedIndex)
            } else {
                setData(mDevice)
                notifyDataSetChanged()
            }
        }

        fun search(address: String) {
            mDevice?.forEach {
                if (it.getAddress().endsWith(address)) {

                }
            }
        }
    }

}
