package qk.sdk.mesh.demo.ui

import android.content.Context
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_group.*
import kotlinx.android.synthetic.main.item_group.view.*
import no.nordicsemi.android.meshprovisioner.Group
import no.nordicsemi.android.meshprovisioner.utils.MeshAddress
import qk.sdk.mesh.demo.R
import qk.sdk.mesh.demo.base.BaseMeshActivity
import qk.sdk.mesh.demo.widget.base.BaseAdapter
import qk.sdk.mesh.demo.widget.base.BaseViewHolder
import qk.sdk.mesh.demo.widget.base.OnItemLongClickListener
import qk.sdk.mesh.meshsdk.MeshHelper
import qk.sdk.mesh.meshsdk.MeshSDK
import qk.sdk.mesh.meshsdk.callback.BooleanCallback
import qk.sdk.mesh.meshsdk.util.Utils

class GroupActivity : BaseMeshActivity() {
    private val TAG = "GroupActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun setLayoutId(): Int = R.layout.activity_group

    override fun init() {
        btn_add_group.setOnClickListener {
            MeshSDK.createGroup(
                "01029012901920",
                object : BooleanCallback {
                    override fun onResult(boolean: Boolean) {
                        Utils.printLog(TAG, "createGroup result:$boolean")
                    }
                })
        }

        initAdapter()
    }

    fun initAdapter() {
        var adapter = GroupAdapter(this, MeshHelper.getGroup())
        rv_groups.layoutManager = LinearLayoutManager(this)
        rv_groups.adapter = adapter
        adapter.setOnItemLongClickListener(object : OnItemLongClickListener<Group> {
            override fun onItemLongClick(data: Group, position: Int): Boolean {
                val network = MeshHelper.getMeshNetwork()
                val group = network?.getGroups()?.get(position)
                if (network?.getModels(group)?.size == 0) {
                    network?.removeGroup(group!!)
                }
                return true
            }
        })
    }

    inner class GroupAdapter(context: Context, groups: ArrayList<Group>) :
        BaseAdapter<Group>(context, groups, R.layout.item_group) {

        override fun bindData(
            holder: BaseViewHolder,
            data: Group,
            position: Int
        ) {
            holder.itemView.tv_group_name.text = data.name
            holder.itemView.tv_group_address.text =
                MeshAddress.formatAddress(data.address, true)
            holder.itemView.tv_device_count.text =
                "${MeshHelper.getMeshNetwork()?.getModels(data)?.size} 个"
        }
    }
}
