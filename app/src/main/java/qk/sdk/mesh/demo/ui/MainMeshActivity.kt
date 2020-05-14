package qk.sdk.mesh.demo.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.JsonObject
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
import qk.sdk.mesh.meshsdk.bean.auto.*
import qk.sdk.mesh.meshsdk.callback.MapCallback
import qk.sdk.mesh.meshsdk.callback.ProvisionCallback
import qk.sdk.mesh.meshsdk.callback.StringCallback
import qk.sdk.mesh.meshsdk.util.ByteUtil
import qk.sdk.mesh.meshsdk.util.Constants
import qk.sdk.mesh.meshsdk.util.LogFileUtil
import qk.sdk.mesh.meshsdk.util.Utils

class MainMeshActivity : BaseMeshActivity(), View.OnClickListener {
    private val TAG = "MainMeshActivity"
    private var mNodeAdapter: NodeAdapter? = null
    private var mNodesCallback: ProvisionCallback = object :
        ProvisionCallback {
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
        tv_groups.setOnClickListener(this)

        rv_provisioned_nodes.layoutManager = LinearLayoutManager(this)
        mNodeAdapter = NodeAdapter(
            this,
            MeshHelper.getProvisionNode() ?: ArrayList()
        )
        rv_provisioned_nodes.adapter = mNodeAdapter
        mNodeAdapter?.setOnItemLongClickListener(object :
            OnItemLongClickListener<ProvisionedMeshNode> {
            override fun onItemLongClick(data: ProvisionedMeshNode, position: Int): Boolean {
//                MeshHelper.deleteProvisionNode(data, mNodesCallback)
                MeshSDK.removeProvisionedNode(data.uuid)
                return true
            }
        })

        var meshNetwork = MeshHelper.getMeshNetwork()

        mNodeAdapter?.setOnItemClickListener(object : OnItemClickListener<ProvisionedMeshNode> {
            override fun onItemClick(data: ProvisionedMeshNode, position: Int) {
//                MeshHelper.setSelectedMeshNode(data)
//                bindModel(data)
//                startActivity(Intent(this@MainMeshActivity, ConnectMeshActivity::class.java))
                meshNetwork = MeshHelper.getMeshNetwork()
                MeshSDK.connect(ByteUtil.bytesToHexString(
                    MeshHelper.getAllNetworkKey()?.get(1)?.key
                ),
//                MeshSDK.connect("758DB07ED6CE6FEE180DFE199EC65BCF",
                    object : MapCallback {
                        override fun onResult(result: HashMap<String, Any>) {
                            Utils.printLog(TAG, "connect result:${result.get("code")}")
                            if (MeshHelper.createGroup("01029012901920")) {
//                                MeshSDK.subscribeStatus(data.uuid, object : MapCallback {
//                                    override fun onResult(result: HashMap<String, Any>) {
//                                        result.forEach { key, value ->
//                                            Utils.printLog(TAG, "key:$key,value:$value")
//                                        }
//                                    }
//                                })
                            }
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

    var meshJson =
        "{\"nodes\":[{\"features\":{\"proxy\":2,\"relay\":2,\"friend\":2,\"lowPower\":2},\"security\":\"low\",\"blacklisted\":false,\"unicastAddress\":\"0001\",\"elements\":[{\"models\":[{\"bind\":[],\"modelId\":\"0001\",\"subscribe\":[]}],\"name\":\"Element: 0x0001\",\"index\":0,\"location\":\"0000\"}],\"name\":\"nRF Mesh Provisioner\",\"defaultTTL\":5,\"deviceKey\":\"11B9654442FB85C22E98DE8BE1CE2094\",\"configComplete\":true,\"UUID\":\"55EDDB4513864CE2864554E256ACBD8F\",\"netKeys\":[{\"index\":0,\"updated\":false}],\"appKeys\":[{\"index\":0,\"updated\":false},{\"index\":1,\"updated\":false},{\"index\":2,\"updated\":false}]},{\"crpl\":\"0064\",\"unicastAddress\":\"0002\",\"deviceKey\":\"BC2D8B73B7EA0B34EAACBDD68CB4AADB\",\"pid\":\"0000\",\"configComplete\":false,\"vid\":\"0000\",\"features\":{\"proxy\":1,\"relay\":1,\"friend\":1,\"lowPower\":1},\"security\":\"low\",\"blacklisted\":false,\"elements\":[{\"models\":[{\"bind\":[],\"modelId\":\"0000\",\"subscribe\":[]},{\"bind\":[3],\"modelId\":\"0002\",\"subscribe\":[]},{\"bind\":[3],\"modelId\":\"1000\",\"subscribe\":[]},{\"bind\":[3],\"modelId\":\"005D0001\",\"subscribe\":[]}],\"name\":\"Element: 0x0002\",\"index\":0,\"location\":\"0000\"}],\"name\":\"MXCHIP Mesh light\",\"defaultTTL\":5,\"UUID\":\"01003A1CD0047863D0F1B30000000000\",\"netKeys\":[{\"index\":1,\"updated\":false}],\"appKeys\":[{\"index\":3,\"updated\":false}],\"cid\":\"005D\"}],\"\$schema\":\"http://json-schema.org/draft-04/schema#\",\"provisioners\":[{\"allocatedGroupRange\":[{\"lowAddress\":\"C000\",\"highAddress\":\"CC9A\"}],\"provisionerName\":\"nRF Mesh Provisioner\",\"UUID\":\"55EDDB4513864CE2864554E256ACBD8F\",\"allocatedUnicastRange\":[{\"lowAddress\":\"0001\",\"highAddress\":\"199A\"}],\"allocatedSceneRange\":[{\"firstScene\":\"0001\",\"lastScene\":\"3333\"}]}],\"meshName\":\"nRFMeshNetwork\",\"groups\":[{\"address\":\"C000\",\"name\":\"26744ad63e9e44baaed7ea61dd095d15\",\"parentAddress\":\"0000\"}],\"id\":\"TBD\",\"meshUUID\":\"D724FC1AE223427EB80041B33B1D9616\",\"netKeys\":[{\"phase\":0,\"isCurrent\":1,\"minSecurity\":\"high\",\"name\":\"Network Key 2\",\"index\":1,\"key\":\"525E52D87A4511EABF8D0242AC48000A\",\"timestamp\":\"1970-01-01T08:00:00+0800\"}],\"appKeys\":[{\"name\":\"Application Key 4\",\"index\":3,\"boundNetKey\":1,\"key\":\"0F93F348AA78DF56D4621E11BEDC9043\"}],\"version\":\"1.0\",\"timestamp\":\"1970-01-01T08:00:00+0800\"}"

    override fun onClick(v: View) {
        when (v.id) {
            R.id.tv_add -> {
                startActivity(Intent(this, ScanTestActivity::class.java))
            }
            R.id.tv_export -> {
                Thread(Runnable {
                    MeshSDK.exportMeshNetwork(object : StringCallback {
                        override fun onResultMsg(msg: String) {
                            meshJson = msg
                            Utils.printLog(TAG, "mesh json:$meshJson")
                            LogFileUtil.writeLogToInnerFile(
                                this@MainMeshActivity,
                                meshJson,
                                Constants.FOLDER_MXCHIP,
                                "mesh_json.text",
                                false,
                                true
                            )
                        }
                    })
                }).start()
            }
            R.id.tv_import -> {
//                Thread(Runnable {
//                    MeshSDK.importMeshNetwork(meshJson, object :
//                        StringCallback {
//                        override fun onResultMsg(msg: String) {
//                            Utils.printLog(TAG, "import result:$msg")
//                        }
//                    })
//                }).start()
                var autoOperations = ArrayList<AutoOperation>()
                var autoOperation = AutoOperation(
                    ByteUtil.bytesToHexString(byteArrayOf(0x00, 0x03)),
                    ByteUtil.bytesToHexString(byteArrayOf(0x82.toByte(), 0x02)),
                    1,
                    ByteUtil.bytesToHexString(byteArrayOf(0))
                )
                autoOperations.add(autoOperation)

                var action = AutoAction("01", 0, autoOperations);
                var actions = ArrayList<AutoAction>()
                actions.add(action)

                var logic = AutoLogic("52", 0, actions)
                var logics = ArrayList<AutoLogic>()
                logics.add(logic)

                var rule = Rule("0002", 0, logics);
                var rules = Rules(arrayListOf(rule))

                Utils.printLog(TAG, "rules:${ByteUtil.bytesToHexString(rules.getBytes())}")
            }
//            R.id.tv_update -> {
//                Thread(Runnable {
//                    MeshSDK.updateDeviceImg("01003510-8C04-7863-D0F1-410000000000","/storage/emulated/0/mxchip_light-b7a7b10b9b23db4d04d91291448af183.bin",object :
//                        MapCallback {
//                        override fun onResult(result: HashMap<String, Any>) {
//
//                        }
//                    })
//                }).start()
//            }
            R.id.tv_groups -> {
                startActivity(Intent(this, GroupActivity::class.java))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        MeshHelper.getProvisionedNodeByCallback(mNodesCallback)
//        MeshHelper.subscribeStatus(object : MeshCallback {
//            override fun onReceive(msg: MeshMessage) {
//
//            }
//
//            override fun onError(msg: CallbackMsg) {
//
//            }
//        })
    }

}
