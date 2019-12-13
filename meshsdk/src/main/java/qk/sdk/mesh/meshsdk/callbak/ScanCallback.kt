package qk.sdk.mesh.meshsdk.callbak

import qk.sdk.mesh.meshsdk.bean.ExtendedBluetoothDevice

interface ScanCallback : BaseCallback {
    fun onScanResult(devices: List<ExtendedBluetoothDevice>, updatedIndex: Int?)
    fun onScanStateChange()
}