/*
 * Copyright (c) 2018, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package qk.sdk.mesh.demo.ui.adapter

import android.content.Context
import kotlinx.android.synthetic.main.item_main_node.view.*

import no.nordicsemi.android.meshprovisioner.transport.Element
import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode
import no.nordicsemi.android.meshprovisioner.utils.CompanyIdentifiers
import qk.sdk.mesh.demo.R
import qk.sdk.mesh.demo.widget.base.BaseAdapter
import qk.sdk.mesh.demo.widget.base.BaseViewHolder
import java.lang.Exception

class NodeAdapter(context: Context, data: ArrayList<ProvisionedMeshNode>) :
    BaseAdapter<ProvisionedMeshNode>(context, data, R.layout.item_main_node) {

    override fun bindData(holder: BaseViewHolder, data: ProvisionedMeshNode, position: Int) {
        try {
            holder.itemView.tv_node_name.text = data.nodeName
            holder.itemView.tv_unicast.text = data.uuid
            holder.itemView.tv_company_identifier.text =
                CompanyIdentifiers.getCompanyName(data.companyIdentifier.toShort())
            holder.itemView.elements.text = "${data.elements.size}"
            holder.itemView.models.text = "${getModels(data.elements)}"
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getModels(elements: Map<Int, Element>): Int {
        var models = 0
        for (element in elements.values) {
            models += element.meshModels.size
        }
        return models
    }
}
