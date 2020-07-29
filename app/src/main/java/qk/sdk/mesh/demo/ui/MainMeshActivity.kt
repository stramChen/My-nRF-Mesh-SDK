package qk.sdk.mesh.demo.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
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
import qk.sdk.mesh.meshsdk.MeshHelper.getProvisionNode
import qk.sdk.mesh.meshsdk.MeshSDK
import qk.sdk.mesh.meshsdk.bean.CallbackMsg
import qk.sdk.mesh.meshsdk.callback.*
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
        //初始化完成才能去查询设备
        MeshSDK.init(this, object : BooleanCallback {
            override fun onResult(boolean: Boolean) {
                MeshHelper.getProvisionedNodeByCallback(mNodesCallback)
                val nodes = getProvisionNode()
                Utils.printLog(TAG, "mesh===>获取已配网设备:${nodes?.size}")
                if (nodes == null || nodes.size == 0) {
                    return
                }
                //先通过gatt协议连接到mesh
                connectDevice()
                //初始化设备列表
                initRecycleView(nodes)
            }
        });

    }

    private fun initRecycleView(nodes: ArrayList<ProvisionedMeshNode>) {
        rv_provisioned_nodes.layoutManager = LinearLayoutManager(this)
        mNodeAdapter = NodeAdapter(
            this,
            nodes
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
                MeshSDK.connect("A5A052659EA049C4AC2D30D292C0E040",
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

    private fun connectDevice() {
        MeshSDK.connect(ByteUtil.bytesToHexString(
            MeshHelper.getAllNetworkKey()?.get(1)?.key
        ),
            object : MapCallback {
                override fun onResult(result: HashMap<String, Any>) {
                    Utils.printLog(TAG, "connect result:${result.get("code")}")
                    //订阅消息通知
                    MeshSDK.subscribeDeviceStatus(object : IDeviceStatusCallBack {
                        override fun onCommand(message: String?) {
                            Utils.printLog(TAG, "downstream:$message");
                        }
                    })
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

    var meshJson ="{\"nodes\":[{\"sequenceNumber\":7926,\"unicastAddress\":\"0001\",\"deviceKey\":\"F14CED1981A72D1D8301B52DEACFF98A\",\"configComplete\":true,\"features\":{\"proxy\":2,\"relay\":2,\"friend\":2,\"lowPower\":2},\"security\":\"low\",\"blacklisted\":false,\"elements\":[{\"models\":[{\"bind\":[],\"modelId\":\"0001\",\"subscribe\":[]}],\"name\":\"Element: 0x0001\",\"index\":0,\"location\":\"0000\"}],\"name\":\"nRF Mesh Provisioner\",\"defaultTTL\":5,\"UUID\":\"6B153DC375C94C9EAE973E98321470EB\",\"appKeys\":[{\"index\":0,\"updated\":false}],\"netKeys\":[{\"index\":0,\"updated\":false}]},{\"sequenceNumber\":1804,\"crpl\":\"0064\",\"unicastAddress\":\"0006\",\"deviceKey\":\"2C3F69CA7E761CA3DA64B4801C2D4ACC\",\"pid\":\"0000\",\"configComplete\":false,\"vid\":\"0000\",\"features\":{\"proxy\":1,\"relay\":1,\"friend\":1,\"lowPower\":1},\"security\":\"low\",\"blacklisted\":false,\"elements\":[{\"models\":[{\"bind\":[],\"modelId\":\"0000\",\"subscribe\":[]},{\"bind\":[4],\"modelId\":\"0002\",\"subscribe\":[]},{\"bind\":[4],\"modelId\":\"1100\",\"subscribe\":[]},{\"bind\":[4],\"modelId\":\"100C\",\"subscribe\":[]},{\"bind\":[4],\"modelId\":\"005D0001\",\"subscribe\":[\"C002\"],\"publish\":{\"period\":1537,\"address\":\"C002\",\"credentials\":0,\"index\":4,\"ttl\":0,\"retransmit\":{\"count\":0,\"interval\":0}}}],\"name\":\"Element: 0x0006\",\"index\":0,\"location\":\"0000\"}],\"name\":\"MXCHIP Mesh PIR\",\"defaultTTL\":5,\"UUID\":\"01003CD975047863D131D40000000000\",\"appKeys\":[{\"index\":4,\"updated\":false}],\"netKeys\":[{\"index\":3,\"updated\":false}],\"cid\":\"005D\"},{\"sequenceNumber\":247,\"crpl\":\"0064\",\"unicastAddress\":\"0004\",\"deviceKey\":\"ABF0FB476D2723BB9CABBF8A337D6109\",\"pid\":\"0000\",\"configComplete\":false,\"vid\":\"0000\",\"features\":{\"proxy\":1,\"relay\":1,\"friend\":1,\"lowPower\":1},\"security\":\"low\",\"blacklisted\":false,\"elements\":[{\"models\":[{\"bind\":[],\"modelId\":\"0000\",\"subscribe\":[]},{\"bind\":[4],\"modelId\":\"0002\",\"subscribe\":[]},{\"bind\":[4],\"modelId\":\"1000\",\"subscribe\":[],\"publish\":{\"period\":1537,\"address\":\"C002\",\"credentials\":0,\"index\":4,\"ttl\":0,\"retransmit\":{\"count\":0,\"interval\":0}}},{\"bind\":[4],\"modelId\":\"005D0001\",\"subscribe\":[\"C002\"]}],\"name\":\"Element: 0x0004\",\"index\":0,\"location\":\"0000\"}],\"name\":\"MXCHIP light_0004\",\"defaultTTL\":5,\"UUID\":\"01003A1CD01000000000040000000000\",\"appKeys\":[{\"index\":4,\"updated\":false}],\"netKeys\":[{\"index\":3,\"updated\":false}],\"cid\":\"005D\"}],\"\$schema\":\"http://json-schema.org/draft-04/schema#\",\"provisioners\":[{\"allocatedGroupRange\":[{\"lowAddress\":\"C000\",\"highAddress\":\"CC9A\"}],\"provisionerName\":\"nRF Mesh Provisioner\",\"UUID\":\"6B153DC375C94C9EAE973E98321470EB\",\"allocatedUnicastRange\":[{\"lowAddress\":\"0001\",\"highAddress\":\"199A\"}],\"allocatedSceneRange\":[{\"firstScene\":\"0001\",\"lastScene\":\"3333\"}]}],\"scenes\":[],\"groups\":[{\"address\":\"C000\",\"name\":\"49152\",\"parentAddress\":\"0000\"},{\"address\":\"C002\",\"name\":\"49154\",\"parentAddress\":\"0000\"}],\"meshName\":\"nRF Mesh Network\",\"id\":\"TBD\",\"appKeys\":[{\"name\":\"Application Key 1\",\"index\":0,\"boundNetKey\":0,\"key\":\"B73BDB0D4A18084430D2DC219F3BB369\"},{\"name\":\"Application Key 2\",\"index\":1,\"boundNetKey\":0,\"key\":\"776DA52A40BB390344241A718BE373E5\"},{\"name\":\"Application Key 3\",\"index\":2,\"boundNetKey\":1,\"key\":\"C25BA639F4D34F649040304E8E66AEC7\"},{\"name\":\"Application Key 4\",\"index\":3,\"boundNetKey\":2,\"key\":\"98B8693811BE9CC8158EA0DF1D4E4227\"},{\"name\":\"Application Key 5\",\"index\":4,\"boundNetKey\":3,\"key\":\"E51E0453B2070F7C29A1BD95DAF39B3C\"}],\"netKeys\":[{\"phase\":0,\"isCurrent\":0,\"minSecurity\":\"high\",\"name\":\"Network Key 2\",\"index\":1,\"key\":\"9ACC2A8BC1374FC0BC29DC105FA2E308\",\"timestamp\":\"2020-07-13T10:12:11+0800\"},{\"phase\":0,\"isCurrent\":0,\"minSecurity\":\"high\",\"name\":\"Network Key 3\",\"index\":2,\"key\":\"096BD18FF85C4584835DE0138516F2F0\",\"timestamp\":\"2020-07-13T10:12:11+0800\"},{\"phase\":0,\"isCurrent\":1,\"minSecurity\":\"high\",\"name\":\"Network Key 4\",\"index\":3,\"key\":\"A5A052659EA049C4AC2D30D292C0E040\",\"timestamp\":\"2020-07-13T10:12:11+0800\"}],\"meshUUID\":\"5C9BA6BF77DE49D5899C34D720849365\",\"version\":\"1.0\",\"timestamp\":\"2020-07-13T10:09:22+0800\"}"

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
        //获取已配对设备节点
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

    override fun onDestroy() {
        Utils.printLog(TAG,"clear gatt");
        MeshHelper.clearGatt()
        super.onDestroy()
    }

}
