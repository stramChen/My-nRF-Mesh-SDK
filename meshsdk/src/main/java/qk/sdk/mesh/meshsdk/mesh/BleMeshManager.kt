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

package qk.sdk.mesh.meshsdk.mesh

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.util.Log
import no.nordicsemi.android.ble.Request
import no.nordicsemi.android.ble.callback.DataReceivedCallback
import no.nordicsemi.android.ble.callback.DataSentCallback
import qk.sdk.mesh.meshsdk.util.Utils
import java.util.*

//class BleMeshManager @Inject
class BleMeshManager
constructor(context: Context) : LoggableBleManager<BleMeshManagerCallbacks>(context) {
    private val TAG = "BleMeshManager"

    companion object {
        private val MTU_SIZE_DEFAULT = 23
        private val MTU_SIZE_MAX = 517

        /**
         * Mesh provisioning service UUID
         */
        val MESH_PROVISIONING_UUID = UUID.fromString("00001827-0000-1000-8000-00805F9B34FB")

        /**
         * Mesh provisioning data in characteristic UUID
         */
        private val MESH_PROVISIONING_DATA_IN =
            UUID.fromString("00002ADB-0000-1000-8000-00805F9B34FB")

        /**
         * Mesh provisioning data out characteristic UUID
         */
        private val MESH_PROVISIONING_DATA_OUT =
            UUID.fromString("00002ADC-0000-1000-8000-00805F9B34FB")

        /**
         * Mesh provisioning service UUID
         */
        val MESH_PROXY_UUID = UUID.fromString("00001828-0000-1000-8000-00805F9B34FB")

        /**
         * Mesh provisioning data in characteristic UUID
         */
        private val MESH_PROXY_DATA_IN = UUID.fromString("00002ADD-0000-1000-8000-00805F9B34FB")

        /**
         * Mesh provisioning data out characteristic UUID
         */
        private val MESH_PROXY_DATA_OUT = UUID.fromString("00002ADE-0000-1000-8000-00805F9B34FB")
    }

    var mMeshProvisioningDataInCharacteristic: BluetoothGattCharacteristic? = null
    var mMeshProvisioningDataOutCharacteristic: BluetoothGattCharacteristic? = null
    var mMeshProxyDataInCharacteristic: BluetoothGattCharacteristic? = null
    var mMeshProxyDataOutCharacteristic: BluetoothGattCharacteristic? = null

    var isProvisioningComplete: Boolean = false
    var isDeviceReady: Boolean = false
    var mNodeReset: Boolean = false
    private var mBluetoothGatt: BluetoothGatt? = null

    /**
     * BluetoothGatt callbacks for connection/disconnection, service discovery, receiving notifications, etc.
     */
    private val mGattCallback = object : BleManagerGattCallback() {

        override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
            val meshProvisioningService = gatt.getService(MESH_PROVISIONING_UUID)
            if (meshProvisioningService != null) {
                isProvisioningComplete = false
                mMeshProvisioningDataInCharacteristic =
                    meshProvisioningService.getCharacteristic(MESH_PROVISIONING_DATA_IN)
                mMeshProvisioningDataOutCharacteristic =
                    meshProvisioningService.getCharacteristic(MESH_PROVISIONING_DATA_OUT)

                val meshProxyService = gatt.getService(MESH_PROXY_UUID)
                if (meshProxyService != null) {
                    isProvisioningComplete = true
                    mMeshProxyDataInCharacteristic =
                        meshProxyService.getCharacteristic(MESH_PROXY_DATA_IN)
                    mMeshProxyDataOutCharacteristic =
                        meshProxyService.getCharacteristic(MESH_PROXY_DATA_OUT)

                    return mMeshProxyDataInCharacteristic != null &&
                            mMeshProxyDataOutCharacteristic != null &&
                            hasNotifyProperty(mMeshProxyDataOutCharacteristic!!) &&
                            hasWriteNoResponseProperty(mMeshProxyDataInCharacteristic!!)
                }
            }

            return false
        }

        override fun initialize() {
            requestMtu(MTU_SIZE_MAX).enqueue()

            // This callback will be called each time a notification is received.
            val onDataReceived = DataReceivedCallback { device, data ->
                mCallbacks.onDataReceived(
                    device,
                    getMaximumPacketSize(),
                    data.getValue() ?: byteArrayOf(0x00)
                )
            }

            // Set the notification callback and enable notification on Data In characteristic.
            val characteristic = mMeshProvisioningDataOutCharacteristic
            val proxyCharacteristic = mMeshProxyDataOutCharacteristic

            setNotificationCallback(characteristic).with(onDataReceived)
            setNotificationCallback(proxyCharacteristic).with(onDataReceived)
            enableNotifications(characteristic).enqueue()
            enableNotifications(proxyCharacteristic).enqueue()
        }

        protected override fun initGatt(gatt: BluetoothGatt): Deque<Request> {
            mBluetoothGatt = gatt
            val requests = LinkedList<Request>()
            requests.add(Request.newMtuRequest(MTU_SIZE_MAX))
            if (isProvisioningComplete) {
                requests.add(Request.newReadRequest(mMeshProxyDataOutCharacteristic))
                requests.add(Request.newReadRequest(mMeshProxyDataInCharacteristic))
                requests.add(Request.newEnableNotificationsRequest(mMeshProxyDataOutCharacteristic))
            }
            requests.add(Request.newReadRequest(mMeshProvisioningDataInCharacteristic))
            requests.add(Request.newReadRequest(mMeshProvisioningDataOutCharacteristic))
            requests.add(
                Request.newEnableNotificationsRequest(
                    mMeshProvisioningDataOutCharacteristic
                )
            )
            return requests
        }

        override fun onDeviceDisconnected() {
            //We reset the MTU to 23 upon disconnection
            Log.d(TAG, "===>-mesh- 设备断开连接了")
            overrideMtu(MTU_SIZE_DEFAULT)
            isDeviceReady = false
            isProvisioningComplete = false
            mMeshProvisioningDataInCharacteristic = null
            mMeshProvisioningDataOutCharacteristic = null
            mMeshProxyDataInCharacteristic = null
            mMeshProxyDataOutCharacteristic = null
        }

        override fun onDeviceReady() {
            isDeviceReady = true
            super.onDeviceReady()
        }
    }

    fun getMaximumPacketSize(): Int {
        return super.getMtu() - 3
    }

    override fun getGattCallback(): BleManagerGattCallback {
        return mGattCallback
    }

    override fun shouldClearCacheWhenDisconnected(): Boolean {
        // This is to make sure that Android will discover the services as the the mesh node will
        // change the provisioning service to a proxy service.
        val result = !isProvisioningComplete || mNodeReset
        mNodeReset = false
        return result
    }

    /**
     * After calling this method the device cache will be cleared upon next disconnection.
     */
    fun setClearCacheRequired() {
        mNodeReset = true
    }

    /**
     * Sends the mesh pdu.
     *
     *
     * The function will chunk the pdu to fit in to the mtu size supported by the node.
     *
     * @param pdu mesh pdu.
     */
    fun sendPdu(pdu: ByteArray) {
        Utils.printLog(TAG, "sendPdu isDeviceReady:$isDeviceReady")
        if (!isDeviceReady)
            return

        // This callback will be called each time the data were sent.
        val callback = DataSentCallback { device, data ->
            mCallbacks.onDataSent(
                device,
                getMaximumPacketSize(),
                data.value ?: byteArrayOf(0x00)
            )
        }

        // Write the right characteristic.
        val characteristic = (if (pdu[0].toInt() == 0x03)
            mMeshProvisioningDataInCharacteristic
        else
            mMeshProxyDataInCharacteristic)
            ?: return

        writeCharacteristic(characteristic, pdu)
            .split()
            .with(callback)
            .enqueue()
    }

    private fun hasWriteNoResponseProperty(characteristic: BluetoothGattCharacteristic): Boolean {
        return characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0
    }

    private fun hasNotifyProperty(characteristic: BluetoothGattCharacteristic): Boolean {
        return characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0
    }

    fun clearGatt() {
        mBluetoothGatt?.disconnect()
        mBluetoothGatt?.close()
    }

}
