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

package qk.sdk.mesh.meshsdk.bean

import android.text.TextUtils
import androidx.lifecycle.LiveData

import no.nordicsemi.android.meshprovisioner.ApplicationKey
import no.nordicsemi.android.meshprovisioner.MeshNetwork
import no.nordicsemi.android.meshprovisioner.NetworkKey
import no.nordicsemi.android.meshprovisioner.Provisioner

/**
 * LiveData class for storing [MeshNetwork]
 */
class MeshNetworkLiveData internal constructor() : LiveData<MeshNetworkLiveData>() {

    var meshNetwork: MeshNetwork? = null
        private set
    private var selectedAppKey: ApplicationKey? = null
    /**
     * Returns the node name
     */
    /**
     * Sets the node name
     *
     * @param nodeName node name
     */
    var nodeName: String? = null
        set(nodeName) {
            if (!TextUtils.isEmpty(nodeName)) {
                field = nodeName
                postValue(this)
            }
        }

    val networkKeys: List<NetworkKey>
        get() = meshNetwork!!.netKeys

    /**
     * Returns the app keys list
     */
    val appKeys: List<ApplicationKey>
        get() = meshNetwork!!.appKeys

    /**
     * Returns the list of [Provisioner]
     */
    val provisioners: List<Provisioner>
        get() = meshNetwork!!.provisioners

    val provisioner: Provisioner?
        get() = meshNetwork!!.selectedProvisioner

    /**
     * Returns the network name
     */
    /**
     * Set the network name of the mesh network
     *
     * @param name network name
     */
    var networkName: String
        get() = meshNetwork!!.meshName
        set(name) {
            meshNetwork!!.meshName = name
            postValue(this)
        }

    /**
     * Loads the mesh network information in to live data
     *
     * @param meshNetwork provisioning settings
     */
    fun loadNetworkInformation(meshNetwork: MeshNetwork) {
        this.meshNetwork = meshNetwork
        postValue(this)
    }

    /**
     * Refreshes the mesh network information
     *
     * @param meshNetwork provisioning settings
     */
     fun refresh(meshNetwork: MeshNetwork) {
        this.meshNetwork = meshNetwork
        postValue(this)
    }

    /**
     * Return the selected app key to be added during the provisioning process.
     *
     * @return app key
     */
    fun getSelectedAppKey(): ApplicationKey? {
        if (selectedAppKey == null)
            selectedAppKey = meshNetwork!!.appKeys[0]
        return selectedAppKey
    }

    /**
     * Set the selected app key to be added during the provisioning process.
     */
    fun setSelectedAppKey(appKey: ApplicationKey) {
        this.selectedAppKey = appKey
        postValue(this)
    }

    fun resetSelectedAppKey() {
        this.selectedAppKey = null
    }
}
