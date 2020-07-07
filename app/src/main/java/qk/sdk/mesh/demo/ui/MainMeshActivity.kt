package qk.sdk.mesh.demo.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
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

    var meshNetwork = MeshHelper.getMeshNetwork()
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


        mNodeAdapter?.setOnItemClickListener(object : OnItemClickListener<ProvisionedMeshNode> {
            override fun onItemClick(data: ProvisionedMeshNode, position: Int) {
//                MeshHelper.setSelectedMeshNode(data)
//                bindModel(data)
//                startActivity(Intent(this@MainMeshActivity, ConnectMeshActivity::class.java))
                meshNetwork = MeshHelper.getMeshNetwork()
//                MeshSDK.connect(ByteUtil.bytesToHexString(
//                    MeshHelper.getAllNetworkKey()
//                        ?.get(MeshHelper.getAllNetworkKey()!!.size - 1)?.key
//                ),
                MeshSDK.connect("9ACC2A8BC1374FC0BC29DC105FA2E308",
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

        MeshSDK.connect(ByteUtil.bytesToHexString(
            MeshHelper.getAllNetworkKey()?.get(1)?.key
        ),
//        MeshSDK.connect("758DB07ED6CE6FEE180DFE199EC65BCF",
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

    var meshJson ="{\"nodes\":[{\"sequenceNumber\":350,\"unicastAddress\":\"0001\",\"deviceKey\":\"F14CED1981A72D1D8301B52DEACFF98A\",\"configComplete\":true,\"features\":{\"proxy\":2,\"relay\":2,\"friend\":2,\"lowPower\":2},\"security\":\"low\",\"blacklisted\":false,\"elements\":[{\"models\":[{\"bind\":[],\"modelId\":\"0001\",\"subscribe\":[]}],\"name\":\"Element: 0x0001\",\"index\":0,\"location\":\"0000\"}],\"name\":\"nRF Mesh Provisioner\",\"defaultTTL\":5,\"UUID\":\"6B153DC375C94C9EAE973E98321470EB\",\"appKeys\":[{\"index\":0,\"updated\":false}],\"netKeys\":[{\"index\":0,\"updated\":false}]},{\"sequenceNumber\":738,\"crpl\":\"0032\",\"unicastAddress\":\"0002\",\"deviceKey\":\"071F7F7CD8AE152E5921E483565912AE\",\"pid\":\"0000\",\"configComplete\":false,\"vid\":\"0000\",\"features\":{\"proxy\":1,\"relay\":1,\"friend\":1,\"lowPower\":1},\"security\":\"low\",\"blacklisted\":false,\"elements\":[{\"models\":[{\"bind\":[],\"modelId\":\"0000\",\"subscribe\":[]},{\"bind\":[2],\"modelId\":\"0002\",\"subscribe\":[]},{\"bind\":[2],\"modelId\":\"1000\",\"subscribe\":[\"C002\"]},{\"bind\":[2],\"modelId\":\"005D0001\",\"subscribe\":[]}],\"name\":\"Element: 0x0002\",\"index\":0,\"location\":\"0000\"}],\"name\":\"MXCHIP Mesh light\",\"defaultTTL\":5,\"UUID\":\"01003A1CD0047863D1322B0000000000\",\"appKeys\":[{\"index\":2,\"updated\":false}],\"netKeys\":[{\"index\":1,\"updated\":false}],\"cid\":\"005D\"},{\"sequenceNumber\":31,\"crpl\":\"0032\",\"unicastAddress\":\"0003\",\"deviceKey\":\"27FBF9F568ECBBAFD84032A5D31851CB\",\"pid\":\"0000\",\"configComplete\":false,\"vid\":\"0000\",\"features\":{\"proxy\":1,\"relay\":1,\"friend\":1,\"lowPower\":1},\"security\":\"low\",\"blacklisted\":false,\"elements\":[{\"models\":[{\"bind\":[],\"modelId\":\"0000\",\"subscribe\":[]},{\"bind\":[2],\"modelId\":\"0002\",\"subscribe\":[]},{\"bind\":[2],\"modelId\":\"1000\",\"subscribe\":[\"C002\"]},{\"bind\":[2],\"modelId\":\"005D0001\",\"subscribe\":[]}],\"name\":\"Element: 0x0003\",\"index\":0,\"location\":\"0000\"}],\"name\":\"MXCHIP Mesh light\",\"defaultTTL\":5,\"UUID\":\"01003A1CD0047863D132360000000000\",\"appKeys\":[{\"index\":2,\"updated\":false}],\"netKeys\":[{\"index\":1,\"updated\":false}],\"cid\":\"005D\"}],\"\$schema\":\"http://json-schema.org/draft-04/schema#\",\"provisioners\":[{\"allocatedGroupRange\":[{\"lowAddress\":\"C000\",\"highAddress\":\"CC9A\"}],\"provisionerName\":\"nRF Mesh Provisioner\",\"UUID\":\"6B153DC375C94C9EAE973E98321470EB\",\"allocatedUnicastRange\":[{\"lowAddress\":\"0001\",\"highAddress\":\"199A\"}],\"allocatedSceneRange\":[{\"firstScene\":\"0001\",\"lastScene\":\"3333\"}]}],\"scenes\":[],\"groups\":[{\"address\":\"C000\",\"name\":\"49152\",\"parentAddress\":\"0000\"},{\"address\":\"C002\",\"name\":\"49154\",\"parentAddress\":\"0000\"}],\"meshName\":\"nRF Mesh Network\",\"id\":\"TBD\",\"appKeys\":[{\"name\":\"Application Key 1\",\"index\":0,\"boundNetKey\":0,\"key\":\"B73BDB0D4A18084430D2DC219F3BB369\"},{\"name\":\"Application Key 2\",\"index\":1,\"boundNetKey\":0,\"key\":\"776DA52A40BB390344241A718BE373E5\"},{\"name\":\"Application Key 3\",\"index\":2,\"boundNetKey\":1,\"key\":\"C25BA639F4D34F649040304E8E66AEC7\"}],\"netKeys\":[{\"phase\":0,\"isCurrent\":1,\"minSecurity\":\"high\",\"name\":\"Network Key 2\",\"index\":1,\"key\":\"9ACC2A8BC1374FC0BC29DC105FA2E308\",\"timestamp\":\"2020-06-30T13:42:38+0800\"},{\"phase\":0,\"isCurrent\":0,\"minSecurity\":\"high\",\"name\":\"Network Key 1\",\"index\":0,\"key\":\"2A3BF3B63DF0FC2F8223887115446C2D\",\"timestamp\":\"2020-06-30T13:42:38+0800\"}],\"meshUUID\":\"5C9BA6BF77DE49D5899C34D720849365\",\"version\":\"1.0\",\"timestamp\":\"2020-06-30T13:42:55+0800\"}"

    override fun onClick(v: View) {
        when (v.id) {
            R.id.tv_add -> {
                meshNetwork = MeshHelper.getMeshNetwork()
                startActivity(Intent(this, ScanTestActivity::class.java))
            }
            R.id.tv_export -> {
                Thread(Runnable {
                    MeshSDK.exportMeshNetwork(object : StringCallback {
                        override fun onResultMsg(msg: String) {
                            meshJson = msg
                            Utils.printLog(TAG, "mesh json:${JsonParser().parse(meshJson)}")

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
                Thread(Runnable {
                    MeshSDK.importMeshNetwork(meshJson, object :
                        StringCallback {
                        override fun onResultMsg(msg: String) {
                            Utils.printLog(TAG, "import result:$msg")
                        }
                    })
                }).start()
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
