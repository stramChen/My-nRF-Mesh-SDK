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

package qk.sdk.mesh.meshsdk.bean.scan

import androidx.lifecycle.LiveData

import java.util.ArrayList

import no.nordicsemi.android.meshprovisioner.UnprovisionedBeacon
import no.nordicsemi.android.support.v18.scanner.ScanResult
import qk.sdk.mesh.meshsdk.bean.ExtendedBluetoothDevice
import qk.sdk.mesh.meshsdk.util.Utils

/**
 * This class keeps the current list of discovered Bluetooth LE devices matching filter.
 * If a new device has been found it is added to the list and the LiveData in observers are
 * notified. If a packet from a device that's already in the list is found, the RSSI and name
 * are updated and observers are also notified. Observer may check [.getUpdatedDeviceIndex]
 * to find out the index of the updated device.
 */
class ScannerLiveData internal constructor() : LiveData<ScannerLiveData>() {
    private val mDevices = ArrayList<ExtendedBluetoothDevice>()
    private var mUpdatedDeviceIndex: Int? = null

    /**
     * Returns the list of devices.
     *
     * @return current list of devices discovered
     */
    val devices: ArrayList<ExtendedBluetoothDevice>
        get() = mDevices

    /**
     * Returns null if a new device was added, or an index of the updated device.
     */
    val updatedDeviceIndex: Int?
        get() {
            val i = mUpdatedDeviceIndex
            mUpdatedDeviceIndex = null
            return i
        }

    /**
     * Returns whether the list is empty.
     */
    val isEmpty: Boolean
        get() = mDevices.isEmpty()

    internal fun deviceDiscovered(result: ScanResult):ScannerLiveData {
        val device: ExtendedBluetoothDevice

        val index = indexOf(result)
        if (index == -1) {
            device = ExtendedBluetoothDevice(result)
            mDevices.add(device)
            mUpdatedDeviceIndex = null
        } else {
            device = mDevices[index]
            mUpdatedDeviceIndex = index
        }
        // Update RSSI and name
        device.rssi = result.rssi
        device.name = result.scanRecord!!.deviceName

        postValue(this)
        return this
    }

    internal fun deviceDiscovered(result: ScanResult, beacon: MeshBeacon?): ScannerLiveData {
        var device: ExtendedBluetoothDevice

        val index = indexOf(result)
        Utils.printLog(
            "ScannerLiveData",
            "beacon is UnprovisionedBeacon:${beacon is UnprovisionedBeacon}，index：$index"
        )
        if (index == -1) {
            device = ExtendedBluetoothDevice(result, beacon)
            mDevices.add(device)
            mUpdatedDeviceIndex = null
        } else {
            device = mDevices[index]
            mUpdatedDeviceIndex = index
        }
        // Update RSSI and name
        device.rssi = result.rssi
        device.name = result.scanRecord?.deviceName
        device.beacon = beacon

        postValue(this)
        Utils.printLog("ScannerLiveData", "postValue")
        return this
    }

    /**
     * Finds the index of existing devices on the scan results list.
     *
     * @param result scan result
     * @return index of -1 if not found
     */
    private fun indexOf(result: ScanResult): Int {
        var i = 0
        for (device in mDevices) {
            if (device.matches(result))
                return i
            i++
        }
        return -1
    }

    internal fun startScanning() {
        mDevices.clear()
        postValue(this)
    }
}
