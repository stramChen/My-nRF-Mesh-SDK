package qk.sdk.mesh.meshsdk.callback

import qk.sdk.mesh.meshsdk.bean.ExtendedBluetoothDevice

interface ScanCallback : BaseCallback {
    fun onScanResult(devices: List<ExtendedBluetoothDevice>, updatedIndex: Int?)
//    fun onScanStateChange()
}