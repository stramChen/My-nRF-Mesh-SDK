package qk.sdk.mesh.demo.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.styd.crm.adapter.base.BaseAdapter
import com.styd.crm.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.activity_scan.*
import kotlinx.android.synthetic.main.item_scan_device.view.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.nordicsemi.android.meshprovisioner.models.GenericOnOffServerModel
import no.nordicsemi.android.meshprovisioner.transport.Element
import no.nordicsemi.android.meshprovisioner.transport.MeshModel
import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode
import qk.sdk.mesh.demo.R
import qk.sdk.mesh.demo.base.BaseMeshActivity
import qk.sdk.mesh.demo.widget.base.OnItemClickListener
import qk.sdk.mesh.meshsdk.MeshHelper
import qk.sdk.mesh.meshsdk.bean.CallbackMsg
import qk.sdk.mesh.meshsdk.bean.ExtendedBluetoothDevice
import qk.sdk.mesh.meshsdk.bean.connect.ConnectState
import qk.sdk.mesh.meshsdk.callbak.BaseCallback
import qk.sdk.mesh.meshsdk.callbak.ConnectCallback
import qk.sdk.mesh.meshsdk.mesh.BleMeshManager
import qk.sdk.mesh.meshsdk.callbak.ScanCallback
import qk.sdk.mesh.meshsdk.util.Utils
import kotlin.collections.ArrayList

class ScanMeshActivity : BaseMeshActivity(),
    OnItemClickListener<ExtendedBluetoothDevice> {
    //    private val INTENT_EXTRA = "scan_mode"
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

    override fun onStart() {
        super.onStart()
        MeshHelper.stopConnect()
        MeshHelper.disConnect()
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

            override fun onScanStateChange() {// 扫描状态变化，若需要一直扫描，需要在此调用startScan
            }

            override fun onError(callbackMsg: CallbackMsg) {

            }
        })
    }

    fun startConnect(data: ExtendedBluetoothDevice) {
        MeshHelper.connect(this, data, false, object : ConnectCallback {
            override fun onConnect() {
                MeshHelper.startProvision(data, object : BaseCallback {
                    override fun onError(callbackMsg: CallbackMsg) {

                    }
                })
            }

            override fun onConnectStateChange(msg: CallbackMsg) {
                if (msg.msg == ConnectState.DISCONNECTED.msg) {
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

    private fun bindModel(node: ProvisionedMeshNode) {
        var elementsMap = node.elements.values
        var elements = ArrayList<Element>()
        elements.addAll(elementsMap)
        if (elements.size > 0)
            elements[0].let { element ->
                var modelsMap = element.meshModels.values
                var models = ArrayList<MeshModel>()
                models.addAll(modelsMap)
                models.forEach { model ->
                    if (model is GenericOnOffServerModel) {
                        MeshHelper.setSelectedModel(element, model)
                    }
                }
            }
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
    }

}
