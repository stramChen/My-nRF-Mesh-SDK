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

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.os.Parcel
import android.os.Parcelable

import no.nordicsemi.android.meshprovisioner.MeshBeacon
import no.nordicsemi.android.support.v18.scanner.ScanResult

@SuppressLint("ParcelCreator")
class ExtendedBluetoothDevice(
    var scanResult: ScanResult?,
    var beacon: MeshBeacon? = null,
    var device: BluetoothDevice? = scanResult?.device,
    var name: String? = scanResult?.scanRecord?.deviceName,
    var rssi: Int? = scanResult?.rssi
) : Parcelable {
    constructor(source: Parcel) : this(
        source.readParcelable<ScanResult>(ScanResult::class.java.classLoader),
        source.readParcelable<MeshBeacon>(MeshBeacon::class.java.classLoader),
        source.readParcelable<BluetoothDevice>(BluetoothDevice::class.java.classLoader),
        source.readString(),
        source.readInt()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeParcelable(device, 0)
        writeParcelable(scanResult, 0)
        writeString(name)
        writeInt(rssi ?: 0)
        writeParcelable(beacon, 0)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<ExtendedBluetoothDevice> =
            object : Parcelable.Creator<ExtendedBluetoothDevice> {
                override fun createFromParcel(source: Parcel): ExtendedBluetoothDevice =
                    ExtendedBluetoothDevice(source)

                override fun newArray(size: Int): Array<ExtendedBluetoothDevice?> =
                    arrayOfNulls(size)
            }
    }

    fun matches(scanResult: ScanResult): Boolean {
        return device?.address == scanResult.device.address
    }

//    override fun equals(o: Any?): Boolean {
//        if (o is ExtendedBluetoothDevice) {
//            val that = o as ExtendedBluetoothDevice?
//            return device?.address == that?.device?.address
//        }
//        return super.equals(o)
//    }

    fun getAddress(): String = device?.address ?: ""

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ExtendedBluetoothDevice

        if (scanResult != other.scanResult) return false
        if (beacon != other.beacon) return false
        if (device != other.device) return false
        if (name != other.name) return false
        if (rssi != other.rssi) return false
        if (device?.address != other.device!!.address) return false

        return true
    }

    override fun hashCode(): Int {
        var result = scanResult?.hashCode() ?: 0
        result = 31 * result + (beacon?.hashCode() ?: 0)
        result = 31 * result + (device?.hashCode() ?: 0)
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (rssi ?: 0)
        return result
    }
}
