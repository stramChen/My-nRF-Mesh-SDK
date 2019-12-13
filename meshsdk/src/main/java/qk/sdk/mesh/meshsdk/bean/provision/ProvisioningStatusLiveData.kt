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

package qk.sdk.mesh.meshsdk.bean.provision

import androidx.lifecycle.LiveData

import java.util.ArrayList

import qk.sdk.mesh.meshsdk.util.ProvisionerStates

class ProvisioningStatusLiveData : LiveData<ProvisioningStatusLiveData>() {

    val stateList = ArrayList<ProvisionerProgress>()


    val provisionerProgress: ProvisionerProgress?
        get() = if (stateList.size == 0) null else stateList[stateList.size - 1]

    fun clear() {
        stateList.clear()
        postValue(this)
    }

    fun onMeshNodeStateUpdated(state: ProvisionerStates) {
        val provisioningProgress: ProvisionerProgress
        when (state) {
            ProvisionerStates.PROVISIONING_INVITE -> {
                provisioningProgress = ProvisionerProgress(state, "Sending provisioning invite...")
                stateList.add(provisioningProgress)
            }
            ProvisionerStates.PROVISIONING_CAPABILITIES -> {
                provisioningProgress =
                    ProvisionerProgress(state, "Provisioning capabilities received...")
                stateList.add(provisioningProgress)
            }
            ProvisionerStates.PROVISIONING_START -> {
                provisioningProgress = ProvisionerProgress(state, "Sending provisioning start...")
                stateList.add(provisioningProgress)
            }
            ProvisionerStates.PROVISIONING_PUBLIC_KEY_SENT -> {
                provisioningProgress =
                    ProvisionerProgress(state, "Sending provisioning public key...")
                stateList.add(provisioningProgress)
            }
            ProvisionerStates.PROVISIONING_PUBLIC_KEY_RECEIVED -> {
                provisioningProgress =
                    ProvisionerProgress(state, "Provisioning public key received...")
                stateList.add(provisioningProgress)
            }
            ProvisionerStates.PROVISIONING_AUTHENTICATION_STATIC_OOB_WAITING, ProvisionerStates.PROVISIONING_AUTHENTICATION_OUTPUT_OOB_WAITING, ProvisionerStates.PROVISIONING_AUTHENTICATION_INPUT_OOB_WAITING -> {
                provisioningProgress =
                    ProvisionerProgress(state, "Waiting for user authentication input...")
                stateList.add(provisioningProgress)
            }
            ProvisionerStates.PROVISIONING_AUTHENTICATION_INPUT_ENTERED -> {
                provisioningProgress = ProvisionerProgress(state, "OOB authentication entered...")
                stateList.add(provisioningProgress)
            }
            ProvisionerStates.PROVISIONING_INPUT_COMPLETE -> {
                provisioningProgress =
                    ProvisionerProgress(state, "Provisioning input complete received...")
                stateList.add(provisioningProgress)
            }
            ProvisionerStates.PROVISIONING_CONFIRMATION_SENT -> {
                provisioningProgress =
                    ProvisionerProgress(state, "Sending provisioning confirmation...")
                stateList.add(provisioningProgress)
            }
            ProvisionerStates.PROVISIONING_CONFIRMATION_RECEIVED -> {
                provisioningProgress =
                    ProvisionerProgress(state, "Provisioning confirmation received...")
                stateList.add(provisioningProgress)
            }
            ProvisionerStates.PROVISIONING_RANDOM_SENT -> {
                provisioningProgress = ProvisionerProgress(state, "Sending provisioning random...")
                stateList.add(provisioningProgress)
            }
            ProvisionerStates.PROVISIONING_RANDOM_RECEIVED -> {
                provisioningProgress = ProvisionerProgress(state, "Provisioning random received...")
                stateList.add(provisioningProgress)
            }
            ProvisionerStates.PROVISIONING_DATA_SENT -> {
                provisioningProgress = ProvisionerProgress(state, "Sending provisioning data...")
                stateList.add(provisioningProgress)
            }
            ProvisionerStates.PROVISIONING_COMPLETE -> {
                provisioningProgress =
                    ProvisionerProgress(state, "Provisioning complete received...")
                stateList.add(provisioningProgress)
            }
            ProvisionerStates.PROVISIONING_FAILED -> {
                provisioningProgress = ProvisionerProgress(state, "Provisioning failed received...")
                stateList.add(provisioningProgress)
            }
            ProvisionerStates.COMPOSITION_DATA_GET_SENT -> {
                provisioningProgress = ProvisionerProgress(state, "Sending composition data get...")
                stateList.add(provisioningProgress)
            }
            ProvisionerStates.COMPOSITION_DATA_STATUS_RECEIVED -> {
                provisioningProgress =
                    ProvisionerProgress(state, "Composition data status received...")
                stateList.add(provisioningProgress)
            }
            ProvisionerStates.SENDING_DEFAULT_TTL_GET -> {
                provisioningProgress = ProvisionerProgress(state, "Sending default TLL get...")
                stateList.add(provisioningProgress)
            }
            ProvisionerStates.DEFAULT_TTL_STATUS_RECEIVED -> {
                provisioningProgress = ProvisionerProgress(state, "Default TTL status received...")
                stateList.add(provisioningProgress)
            }
            ProvisionerStates.SENDING_APP_KEY_ADD -> {
                provisioningProgress = ProvisionerProgress(state, "Sending app key add...")
                stateList.add(provisioningProgress)
            }
            ProvisionerStates.APP_KEY_STATUS_RECEIVED -> {
                provisioningProgress = ProvisionerProgress(state, "App key status received...")
                stateList.add(provisioningProgress)
            }
            ProvisionerStates.SENDING_NETWORK_TRANSMIT_SET -> {
                provisioningProgress = ProvisionerProgress(state, "Sending network transmit set...")
                stateList.add(provisioningProgress)
            }
            ProvisionerStates.NETWORK_TRANSMIT_STATUS_RECEIVED -> {
                provisioningProgress =
                    ProvisionerProgress(state, "Network transmit status received...")
                stateList.add(provisioningProgress)
            }
            ProvisionerStates.SENDING_BLOCK_ACKNOWLEDGEMENT -> {
                provisioningProgress = ProvisionerProgress(state, "Sending block acknowledgements")
                stateList.add(provisioningProgress)
            }
            ProvisionerStates.BLOCK_ACKNOWLEDGEMENT_RECEIVED -> {
                provisioningProgress =
                    ProvisionerProgress(state, "Receiving block acknowledgements")
                stateList.add(provisioningProgress)
            }
            ProvisionerStates.PROVISIONER_UNASSIGNED -> {
                provisioningProgress = ProvisionerProgress(state, "Provisioner unassigned...")
                stateList.add(provisioningProgress)
            }
            else -> {
            }
        }
        postValue(this)
    }
}
