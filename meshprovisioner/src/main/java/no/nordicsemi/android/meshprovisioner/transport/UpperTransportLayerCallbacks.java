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

package no.nordicsemi.android.meshprovisioner.transport;

import java.util.UUID;

import androidx.annotation.Nullable;

import no.nordicsemi.android.meshprovisioner.NetworkKey;

/**
 * Upper transport layer call backs
 */
public interface UpperTransportLayerCallbacks {

    /**
     * Callback to get the mesh node from the list of provisioned mesh node.
     *
     * @param unicastAddress unicast address of the mesh node
     */
    ProvisionedMeshNode getNode(final int unicastAddress);

    /**
     * Returns the IV Index of the mesh network
     */
    byte[] getIvIndex();

    /**
     * Returns the application key with the specific application key identifier
     *
     * @param aid application key identifier
     */
    byte[] getApplicationKey(NetworkKey networkKey,final int aid);


    /**
     * Returns the label UUID of the destination address
     *
     * @param address Virtual address
     * @return The label uuid if it's known to the provisioner or null otherwise
     */
    @Nullable
    UUID getLabel(final int address);
}
