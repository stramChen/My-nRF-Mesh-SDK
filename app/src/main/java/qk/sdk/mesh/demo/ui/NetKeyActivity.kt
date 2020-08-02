package qk.sdk.mesh.demo.ui

import android.content.Context
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_net_key.*
import kotlinx.android.synthetic.main.item_key.view.*
import kotlinx.android.synthetic.main.item_scan_device.view.*
import qk.sdk.mesh.demo.R
import qk.sdk.mesh.demo.base.BaseMeshActivity
import qk.sdk.mesh.demo.widget.base.BaseAdapter
import qk.sdk.mesh.demo.widget.base.BaseViewHolder
import qk.sdk.mesh.demo.widget.base.OnItemLongClickListener
import qk.sdk.mesh.meshsdk.MeshSDK
import qk.sdk.mesh.meshsdk.callback.ArrayStringCallback
import qk.sdk.mesh.meshsdk.callback.IntCallback
import qk.sdk.mesh.meshsdk.callback.MapCallback
import qk.sdk.mesh.meshsdk.util.Utils

class NetKeyActivity : BaseMeshActivity() {
    private val TAG = "NetKeyActivity"

    class KeysAdapter(context: Context, data: ArrayList<String>) :
        BaseAdapter<String>(context, data, R.layout.item_key) {

        override fun bindData(holder: BaseViewHolder, data: String, position: Int) {
            holder.itemView.tv_title.text = data
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun setLayoutId(): Int = R.layout.activity_net_key

    override fun init() {
        initView()
    }

    fun initView() {
        var adapter: KeysAdapter
        rv_net_key.layoutManager=LinearLayoutManager(this)

        adapter = KeysAdapter(applicationContext, MeshSDK.getAllNetworkKey())
        rv_net_key.adapter = adapter
        adapter.setOnItemLongClickListener(object : OnItemLongClickListener<String> {
            override fun onItemLongClick(data: String, position: Int): Boolean {
                MeshSDK.removeNetworkKey(data, object : MapCallback() {
                    override fun onResult(result: HashMap<String, Any>) {
                        Utils.printLog(TAG, "remove net key:${result.get("code")}")
                    }
                })
                return true
            }
        })
    }
}
