package qk.sdk.mesh.demo.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_main.*
import no.nordicsemi.android.meshprovisioner.models.GenericOnOffServerModel
import no.nordicsemi.android.meshprovisioner.transport.Element
import no.nordicsemi.android.meshprovisioner.transport.MeshModel
import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode
import qk.sdk.mesh.demo.R
import qk.sdk.mesh.demo.base.BaseMeshActivity
import qk.sdk.mesh.demo.ui.adapter.NodeAdapter
import qk.sdk.mesh.demo.widget.base.OnItemClickListener
import qk.sdk.mesh.demo.widget.base.OnItemLongClickListener
import qk.sdk.mesh.meshsdk.MeshHelper
import qk.sdk.mesh.meshsdk.MeshSDK
import qk.sdk.mesh.meshsdk.bean.CallbackMsg
import qk.sdk.mesh.meshsdk.callbak.MapCallback
import qk.sdk.mesh.meshsdk.callbak.ProvisionCallback
import qk.sdk.mesh.meshsdk.util.ByteUtil
import qk.sdk.mesh.meshsdk.util.Utils

class MainMeshActivity : BaseMeshActivity(), View.OnClickListener {
    private val TAG = "MainMeshActivity"
    private var mNodeAdapter: NodeAdapter? = null
    private var mNodesCallback: ProvisionCallback = object : ProvisionCallback {
        override fun onProvisionedNodes(nodes: ArrayList<ProvisionedMeshNode>) {
            mNodeAdapter?.setData(nodes)
        }

        override fun onNodeDeleted(isDeleted: Boolean, node: ProvisionedMeshNode) {
            mNodeAdapter?.mData?.remove(node)
            mNodeAdapter?.notifyDataSetChanged()
        }

        override fun onError(callbackMsg: CallbackMsg) {

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun setLayoutId(): Int = R.layout.activity_main
    override fun init() {
        initView()
    }

    fun initView() {
        tv_add.setOnClickListener(this)
        tv_export.setOnClickListener(this)
        tv_import.setOnClickListener(this)

        rv_provisioned_nodes.layoutManager = LinearLayoutManager(this)
        mNodeAdapter = NodeAdapter(
            this,
            MeshHelper.getProvisionNode() ?: ArrayList()
        )
        rv_provisioned_nodes.adapter = mNodeAdapter
        mNodeAdapter?.setOnItemLongClickListener(object :
            OnItemLongClickListener<ProvisionedMeshNode> {
            override fun onItemLongClick(data: ProvisionedMeshNode, position: Int): Boolean {
                MeshHelper.deleteProvisionNode(data, mNodesCallback)
                return true
            }
        })

        mNodeAdapter?.setOnItemClickListener(object : OnItemClickListener<ProvisionedMeshNode> {
            override fun onItemClick(data: ProvisionedMeshNode, position: Int) {
//                MeshHelper.setSelectedMeshNode(data)
//                bindModel(data)
//                startActivity(Intent(this@MainMeshActivity, ConnectMeshActivity::class.java))
                MeshSDK.connect(ByteUtil.bytesToHexString(MeshHelper.getAllNetworkKey()?.get(3)?.key),
//                MeshSDK.connect("758DB07ED6CE6FEE180DFE199EC65BCF",
                    object : MapCallback {
                        override fun onResult(result: HashMap<String, Any>) {
                            Utils.printLog(TAG, "connect result:${result.get("code")}")
                        }
                    })
                startActivity(
                    Intent(
                        this@MainMeshActivity,
                        ConnectTestActivity::class.java
                    ).putExtra("uuid", data.uuid)
                )
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

    override fun onClick(v: View) {
        when (v.id) {
            R.id.tv_add -> {
                startActivity(Intent(this, ScanTestActivity::class.java))
            }
            R.id.tv_export -> {
                MeshSDK.exportMeshNetwork()
            }
            R.id.tv_import->{
                MeshSDK.importMeshNetwork()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        MeshHelper.getProvisionedNodeByCallback(mNodesCallback)
    }

}
