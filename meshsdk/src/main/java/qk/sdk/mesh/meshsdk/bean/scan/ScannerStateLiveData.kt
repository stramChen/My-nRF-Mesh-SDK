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

class ScannerStateLiveData internal constructor(
    bluetoothEnabled: Boolean,
    private var mLocationEnabled: Boolean
) : LiveData<ScannerStateLiveData>() {
    /**
     * Returns whether scanning is in progress.
     */
    var isScanning: Boolean = false
        private set
    /**
     * Returns whether Bluetooth adapter is enabled.
     */
    var isBluetoothEnabled: Boolean = false
        private set
    private var mDeviceFound: Boolean = false

    val isEmpty: Boolean
        get() = !mDeviceFound

    /**
     * Returns whether Location is enabled.
     */
    var isLocationEnabled: Boolean
        get() = mLocationEnabled
        internal set(enabled) {
            mLocationEnabled = enabled
            postValue(this)
        }

    init {
        isScanning = false
        mDeviceFound = false
        isBluetoothEnabled = bluetoothEnabled
        postValue(this)
    }

    fun startScanning() {
        postValue(this)
    }

    internal fun scanningStarted() {
        isScanning = true
    }

    internal fun scanningStopped() {
        isScanning = false
        mDeviceFound = false
        postValue(this)
    }

    internal fun bluetoothEnabled() {
        isBluetoothEnabled = true
        postValue(this)
    }

    internal fun bluetoothDisabled() {
        isBluetoothEnabled = false
        postValue(this)
    }

    internal fun deviceFound() {
        if (!mDeviceFound) {
            mDeviceFound = true
            postValue(this)
        }
    }

    fun getScanState(): Int {
        return if (isBluetoothEnabled) 1 else if (isLocationEnabled) 2 else if (isScanning) 3 else if (mDeviceFound) 4 else 5
    }
}
